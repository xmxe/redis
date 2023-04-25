package com.xmxe.controller;

import com.alibaba.fastjson.JSON;
import com.xmxe.bean.DelayJob;
import com.xmxe.bean.Job;
import com.xmxe.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

/**
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
 * Job pool任务池保存job元信息。根据文章描述使用K/V的数据结构，key为ID，value为job
 * Delay Bucket用来保存业务的延迟任务。文章中描述使用轮询方式放入某一个Bucket可以知道其并没有使用topic来区分，个人这里默认使用顺序插入
 * Timer时间组件，负责扫描各个Bucket。根据文章描述存在多个Timer，但是同一个Timer同一时间只能扫描一个Bucket
 * Ready Queue负责存放需要被完成的任务，但是根据描述根据Topic的不同存在多个Ready Queue。
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


/**
 * 测试
 * 添加延迟任务
 * 通过postman请求：localhost:8000/delay/add 此时这条延时任务被添加进了线程池中 根据设置10秒钟之后任务会被添加至ReadyQueue中
 * 获得任务
 * 这时候我们请求localhost:8000/delay/pop 这个时候任务被响应，修改状态的同时设置其超时时间，然后放置在DelayBucket中 按照设计在30秒后，任务假如没有被消费将会重新放置在ReadyQueue中
 * 任务的删除/消费
 * 现在我们请求：localhost:8000/delay/delete 此时在Job pool中此任务将会被移除，此时元数据已经不存在，但任务还在DelayBucket中循环，然而在循环中当检测到元数据已经不存的话此延时任务会被移除。
 */

@RestController
@RequestMapping("delay")
public class DelayController {

    @Autowired
    private JobService jobService;

    private final static AtomicInteger index = new AtomicInteger(0);

    private final static String[] tag = new String[]{"test","web","java"};


    /**
     * 添加 测试的时候使用
     */
    @RequestMapping(value = "addTest",method = RequestMethod.POST)
    public String addDefJobTest() {
        Job request = new Job();
        int i = index.addAndGet(1);
        Long aLong = Long.valueOf(i);
        request.setId(aLong);
        int num = i%3;
        request.setTopic(tag[num]);
        request.setMessage("tag:" + tag[num] + "id:" + i);
        request.setDelayTime(10000);
        request.setTtrTime(10000);
        DelayJob delayJob = jobService.addDefJob(request);
        return JSON.toJSONString(delayJob);
    }

    /**
     * 添加
     * @param request
     */
    @RequestMapping(value = "add",method = RequestMethod.POST)
    public String addDefJob(Job request) {
        DelayJob delayJob = jobService.addDefJob(request);
        return JSON.toJSONString(delayJob);
    }

    /**
     * 获取
     */
    @RequestMapping(value = "pop/{topic}",method = RequestMethod.GET)
    public String getProcessJob(@PathVariable("topic") String topic) {
        Job process = jobService.getProcessJob(topic);
        return JSON.toJSONString(process);
    }

    /**
     * 完成一个执行的任务
     * @param jobId
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