package com.xmxe.business;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Boot + Redis 实现延时队列 (https://mp.weixin.qq.com/s/7hUHW7rwnJY7XCM0JFDXpQ)
 * 源码(https://gitee.com/daifyutils/springboot-samples/blob/master/base-redis-delay)
 *
 * 业务流程:
 * 1.用户提交任务。首先将任务推送至延迟队列中。
 * 2.延迟队列接收到任务后，首先将任务推送至job pool中，然后计算其执行时间。
 * 3.然后生成延迟任务（仅仅包含任务id）放入某个桶中
 * 4.时间组件时刻轮询各个桶，当时间到达的时候从job pool中获得任务元信息。
 * 5.监测任务的合法性如果已经删除则pass。继续轮询。如果任务合法则再次计算时间
 * 6.如果合法则计算时间，如果时间合法：根据topic将任务放入对应的ready queue，然后从bucket中移除。如果时间不合法，则重新计算时间再次放入bucket，并移除之前的bucket中的内容
 * 7.消费端轮询对应topic的ready queue。获取job后做自己的业务逻辑。与此同时，服务端将已经被消费端获取的job按照其设定的TTR，重新计算执行时间，并将其放入bucket。
 * 8.完成消费后，发送finish消息，服务端根据job id删除对应信息。
 *
 * 对象
 * 我们现在可以了解到中间存在的几个组件
 * 延迟队列，为Redis延迟队列。实现消息传递
 * Job pool 任务池保存job元信息。根据文章描述使用K/V的数据结构，key为ID，value为job
 * Delay Bucket 用来保存业务的延迟任务。文章中描述使用轮询方式放入某一个Bucket可以知道其并没有使用topic来区分，个人这里默认使用顺序插入
 * Timer 时间组件，负责扫描各个Bucket。根据文章描述存在多个Timer，但是同一个Timer同一时间只能扫描一个Bucket
 * Ready Queue 负责存放需要被完成的任务，但是根据描述根据Topic的不同存在多个Ready Queue。
 * 其中Timer负责轮询，Job pool、Delay Bucket、Ready Queue都是不同职责的集合。
 *
 * 任务状态
 * ready：可执行状态，
 * delay：不可执行状态，等待时钟周期。
 * reserved：已被消费者读取，但没有完成消费。
 * deleted：已被消费完成或者已被删除。
 *
 * 对外提供的接口
 * add 添加任务
 * pop 取出待等待任务
 * finish 完成任务
 * delete 删除任务
 *
 * 额外的内容
 * 首先根据状态状态描述，finish和delete操作都是将任务设置成deleted状态。
 * 根据文章描述的操作，在执行finish或者delete的操作的时候任务已经从元数据中移除，此时deleted状态可能只存在极短时间，所以实际实现中就直接删除了。
 * 文章中并没有说明响应超时后如何处理，所以个人现在将其重新投入了待处理队列。
 * 文章中因为使用了集群，所以使用redis的setnx锁来保证多个时间循环处理多个桶的时候不会出现重复循环。这里因为是简单的实现，所以就很简单的每个桶设置一个时间队列处理。也是为了方便简单处理。关于分布式锁可以看我之前的文章里面有描述。
 */

public class SpringBootRedisDelayQueue {
}

/**
 * 任务对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class Job implements Serializable {

	/**
	 * 延迟任务的唯一标识，用于检索任务
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	/**
	 * 任务类型（具体业务类型）
	 */
	private String topic;

	/**
	 * 任务的延迟时间
	 */
	private long delayTime;

	/**
	 * 任务的执行超时时间
	 */
	private long ttrTime;

	/**
	 * 任务具体的消息内容，用于处理具体业务逻辑用
	 */
	private String message;

	/**
	 * 重试次数
	 */
	private int retryCount;
	/**
	 * 任务状态
	 */
	private JobStatus status;
}

/**
 * 任务引用对象 / 延迟任务
 */
@Data
@AllArgsConstructor
class DelayJob implements Serializable {


	/**
	 * 延迟任务的唯一标识
	 */
	private long jodId;

	/**
	 * 任务的执行时间
	 */
	private long delayDate;

	/**
	 * 任务类型（具体业务类型）
	 */
	private String topic;


	public DelayJob(Job job) {
		this.jodId = job.getId();
		this.delayDate = System.currentTimeMillis() + job.getDelayTime();
		this.topic = job.getTopic();
	}

	public DelayJob(Object value, Double score) {
		this.jodId = Long.parseLong(String.valueOf(value));
		this.delayDate = System.currentTimeMillis() + score.longValue();
	}
}
/**
 * 任务状态
 **/
@Getter
enum JobStatus {

	/**
	 * 可执行状态，等待消费
	 */
	READY,
	/**
	 * 不可执行状态，等待时钟周期
	 */
	DELAY,
	/**
	 * 已被消费者读取，但还未得到消费者的响应
	 */
	RESERVED,
	/**
	 * 已被消费完成或者已被删除
	 */
	DELETED;
}
/**
 * 容器
 * 目前我们需要完成三个容器的创建，Job任务池、延迟任务容器、待完成任务容器。
 */

/**
 * job任务池，为普通的K/V结构，提供基础的操作
 */
@Component
@Slf4j
class JobPool {

	@Autowired
	private RedisTemplate redisTemplate;

	private String NAME = "job.pool";

	private BoundHashOperations getPool () {
		BoundHashOperations ops = redisTemplate.boundHashOps(NAME);
		return ops;
	}

	/**
	 * 添加任务
	 * @param job
	 */
	public void addJob (Job job) {
		log.info("任务池添加任务：{}", JSON.toJSONString(job));
		getPool().put(job.getId(),job);
		return ;
	}

	/**
	 * 获得任务
	 * @param jobId
	 * @return
	 */
	public Job getJob(Long jobId) {
		Object o = getPool().get(jobId);
		if (o instanceof Job) {
			return (Job) o;
		}
		return null;
	}

	/**
	 * 移除任务
	 * @param jobId
	 */
	public void removeDelayJob (Long jobId) {
		log.info("任务池移除任务：{}",jobId);
		// 移除任务
		getPool().delete(jobId);
	}
}

/**
 * 延迟任务，使用可排序的ZSet保存数据，提供取出最小值等操作
 * 延时处理队列
 */
@Slf4j
@Component
class DelayBucket {

	@Autowired
	private RedisTemplate redisTemplate;

	private static AtomicInteger index = new AtomicInteger(0);

	@Value("${thread.size}")
	private int bucketsSize;

	private List<String> bucketNames = new ArrayList<>();

	@Bean
	public List <String> createBuckets() {
		for (int i = 0; i < bucketsSize; i++) {
			bucketNames.add("bucket" + i);
		}
		return bucketNames;
	}

	/**
	 * 获得桶的名称
	 * @return
	 */
	private String getThisBucketName() {
		int thisIndex = index.addAndGet(1);
		int i1 = thisIndex % bucketsSize;
		return bucketNames.get(i1);
	}

	/**
	 * 获得桶集合
	 * @param bucketName
	 * @return
	 */
	private BoundZSetOperations getBucket(String bucketName) {
		return redisTemplate.boundZSetOps(bucketName);
	}

	/**
	 * 放入延时任务
	 * @param job
	 */
	public void addDelayJob(DelayJob job) {
		log.info("添加延迟任务:{}", JSON.toJSONString(job));
		String thisBucketName = getThisBucketName();
		BoundZSetOperations bucket = getBucket(thisBucketName);
		bucket.add(job,job.getDelayDate());
	}

	/**
	 * 获得最新的延期任务
	 * @return
	 */
	public DelayJob getFirstDelayTime(Integer index) {
		String name = bucketNames.get(index);
		BoundZSetOperations bucket = getBucket(name);
		Set<ZSetOperations.TypedTuple> set = bucket.rangeWithScores(0, 1);
		if (CollectionUtils.isEmpty(set)) {
			return null;
		}
		ZSetOperations.TypedTuple typedTuple = (ZSetOperations.TypedTuple) set.toArray()[0];
		Object value = typedTuple.getValue();
		if (value instanceof DelayJob) {
			return (DelayJob) value;
		}
		return null;
	}

	/**
	 * 移除延时任务
	 * @param index
	 * @param delayJob
	 */
	public void removeDelayTime(Integer index,DelayJob delayJob) {
		String name = bucketNames.get(index);
		BoundZSetOperations bucket = getBucket(name);
		bucket.remove(delayJob);
	}

}

/**
 * 待完成任务 内部使用topic进行细分 每个topic对应一个list集合
 */
@Component
@Slf4j
class ReadyQueue {

	@Autowired
	private RedisTemplate redisTemplate;

	private String NAME = "process.queue";

	private String getKey(String topic) {
		return NAME + topic;
	}

	/**
	 * 获得队列
	 * @param topic
	 * @return
	 */
	private BoundListOperations getQueue (String topic) {
		BoundListOperations ops = redisTemplate.boundListOps(getKey(topic));
		return ops;
	}

	/**
	 * 设置任务
	 * @param delayJob
	 */
	public void pushJob(DelayJob delayJob) {
		log.info("执行队列添加任务:{}",delayJob);
		BoundListOperations listOperations = getQueue(delayJob.getTopic());
		listOperations.leftPush(delayJob);
	}

	/**
	 * 移除并获得任务
	 * @param topic
	 * @return
	 */
	public DelayJob popJob(String topic) {
		BoundListOperations listOperations = getQueue(topic);
		Object o = listOperations.leftPop();
		if (o instanceof DelayJob) {
			log.info("执行队列取出任务:{}", JSON.toJSONString((DelayJob) o));
			return (DelayJob) o;
		}
		return null;
	}

}

/**
 * 轮询处理
 * 设置了线程池为每个bucket设置一个轮询操作
 */
@Component
class DelayTimer implements ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private DelayBucket delayBucket;
	@Autowired
	private JobPool     jobPool;
	@Autowired
	private ReadyQueue  readyQueue;

	@Value("${thread.size}")
	private int length;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		ExecutorService executorService = new ThreadPoolExecutor(
				length,
				length,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());

		for (int i = 0; i < length; i++) {
			executorService.execute(
					new DelayJobHandler(
							delayBucket,
							jobPool,
							readyQueue,
							i));
		}

	}
}
@Slf4j
@Data
@AllArgsConstructor
class DelayJobHandler implements Runnable{

	/**
	 * 延迟队列
	 */
	private DelayBucket delayBucket;
	/**
	 * 任务池
	 */
	private JobPool jobPool;

	private ReadyQueue readyQueue;
	/**
	 * 索引
	 */
	private int index;

	/**
	 */
	@Override
	public void run() {
		log.info("定时任务开始执行");
		while (true) {
			try {
				DelayJob delayJob = delayBucket.getFirstDelayTime(index);
				//没有任务
				if (delayJob == null) {
					sleep();
					continue;
				}
				// 发现延时任务
				// 延迟时间没到
				if (delayJob.getDelayDate() > System.currentTimeMillis()) {
					sleep();
					continue;
				}
				Job job = jobPool.getJob(delayJob.getJodId());

				//延迟任务元数据不存在
				if (job == null) {
					log.info("移除不存在任务:{}", JSON.toJSONString(delayJob));
					delayBucket.removeDelayTime(index,delayJob);
					continue;
				}

				JobStatus status = job.getStatus();
				if (JobStatus.RESERVED.equals(status)) {
					log.info("处理超时任务:{}", JSON.toJSONString(job));
					// 超时任务
					processTtrJob(delayJob,job);
				} else {
					log.info("处理延时任务:{}", JSON.toJSONString(job));
					// 延时任务
					processDelayJob(delayJob,job);
				}
			} catch (Exception e) {
				log.error("扫描DelayBucket出错：",e.getStackTrace());
				sleep();
			}
		}
	}

	/**
	 * 处理ttr的任务
	 */
	private void processTtrJob(DelayJob delayJob,Job job) {
		job.setStatus(JobStatus.DELAY);
		// 修改任务池状态
		jobPool.addJob(job);
		// 移除delayBucket中的任务
		delayBucket.removeDelayTime(index,delayJob);
		Long delayDate = System.currentTimeMillis() + job.getDelayTime();
		delayJob.setDelayDate(delayDate);
		// 再次添加到任务中
		delayBucket.addDelayJob(delayJob);
	}

	/**
	 * 处理延时任务
	 */
	private void processDelayJob(DelayJob delayJob,Job job) {
		job.setStatus(JobStatus.READY);
		// 修改任务池状态
		jobPool.addJob(job);
		// 设置到待处理任务
		readyQueue.pushJob(delayJob);
		// 移除delayBucket中的任务
		delayBucket.removeDelayTime(index,delayJob);
	}

	private void sleep(){
		try {
			Thread.sleep(DelayConfig.SLEEP_TIME);
		} catch (InterruptedException e){
			log.error("",e);
		}
	}
}
/**
 *
 **/
class DelayConfig {
	/**
	 * 睡眠时间
	 */
	public static Long SLEEP_TIME = 1000L;

	/**
	 * 重试次数
	 */
	public static Integer RETRY_COUNT = 5;

	/**
	 * 默认超时时间
	 */
	public static Long PROCESS_TIME = 5000L;
}

/**
 * @author daify
 * @date 2019-07-28
 */
@Component
class JobService {

	@Autowired
	private DelayBucket delayBucket;

	@Autowired
	private ReadyQueue readyQueue;

	@Autowired
	private JobPool jobPool;


	public DelayJob addDefJob(Job job) {
		job.setStatus(JobStatus.DELAY);
		jobPool.addJob(job);
		DelayJob delayJob = new DelayJob(job);
		delayBucket.addDelayJob(delayJob);
		return delayJob;
	}

	/**
	 * 获取
	 * @return
	 */
	public Job getProcessJob(String topic) {
		// 拿到任务
		DelayJob delayJob = readyQueue.popJob(topic);
		if (delayJob == null || delayJob.getJodId() == 0L) {
			return new Job();
		}
		Job job = jobPool.getJob(delayJob.getJodId());
		// 元数据已经删除，则取下一个
		if (job == null) {
			job = getProcessJob(topic);
		} else {
			job.setStatus(JobStatus.RESERVED);
			delayJob.setDelayDate(System.currentTimeMillis() + job.getTtrTime());

			jobPool.addJob(job);
			delayBucket.addDelayJob(delayJob);
		}
		return job;
	}

	/**
	 * 完成一个执行的任务
	 * @param jobId
	 * @return
	 */
	public void finishJob(Long jobId) {
		jobPool.removeDelayJob(jobId);
	}

	/**
	 * 完成一个执行的任务
	 * @param jobId
	 * @return
	 */
	public void deleteJob(Long jobId) {
		jobPool.removeDelayJob(jobId);
	}

}
/**
 * 测试用请求
 **/
@RestController
@RequestMapping("delay")
class DelayController {

	@Autowired
	private JobService jobService;
	/**
	 * 添加
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "add",method = RequestMethod.POST)
	public String addDefJob(Job request) {
		DelayJob delayJob = jobService.addDefJob(request);
		return JSON.toJSONString(delayJob);
	}

	/**
	 * 获取
	 * @return
	 */
	@RequestMapping(value = "pop",method = RequestMethod.GET)
	public String getProcessJob(String topic) {
		Job process = jobService.getProcessJob(topic);
		return JSON.toJSONString(process);
	}

	/**
	 * 完成一个执行的任务
	 * @param jobId
	 * @return
	 */
	@RequestMapping(value = "finish",method = RequestMethod.DELETE)
	public String finishJob(Long jobId) {
		jobService.finishJob(jobId);
		return "success";
	}

	@RequestMapping(value = "delete",method = RequestMethod.DELETE)
	public String deleteJob(Long jobId) {
		jobService.deleteJob(jobId);
		return "success";
	}

}
/**
 * 测试
 * 添加延迟任务
 * 通过postman请求：localhost:8000/delay/add 此时这条延时任务被添加进了线程池中 根据设置10秒钟之后任务会被添加至ReadyQueue中
 * 获得任务
 * 这时候我们请求localhost:8000/delay/pop 这个时候任务被响应，修改状态的同时设置其超时时间，然后放置在DelayBucket中 按照设计在30秒后，任务假如没有被消费将会重新放置在ReadyQueue中
 * 任务的删除/消费
 * 现在我们请求：localhost:8000/delay/delete 此时在Job pool中此任务将会被移除，此时元数据已经不存在，但任务还在DelayBucket中循环，然而在循环中当检测到元数据已经不存的话此延时任务会被移除。
 */