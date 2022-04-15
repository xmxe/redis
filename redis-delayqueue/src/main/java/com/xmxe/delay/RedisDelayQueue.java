package com.xmxe.delay;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

import java.util.Calendar;
import java.util.Set;

/**
 * 利用redis实现延时队列
 * 如 生成订单30分钟未支付，则自动取消
 * 生成订单60秒后,给用户发短信
 * demo1
 *
 */
public class RedisDelayQueue {
	private static final String ADDR = "127.0.0.1";
	private static final int PORT = 6379;
	private static JedisPool jedisPool = new JedisPool(ADDR, PORT);

	public static Jedis getJedis() {
		return jedisPool.getResource();
	}

	//生产者,生成5个订单放进去
	public void productionDelayMessage(){
		for(int i=0;i<5;i++){
			//延迟3秒
			Calendar cal1 = Calendar.getInstance();
			cal1.add(Calendar.SECOND, 3);
			int second3later = (int) (cal1.getTimeInMillis() / 1000);
			RedisDelayQueue.getJedis().zadd("OrderId",second3later,"OID0000001"+i);
			System.out.println(System.currentTimeMillis()+"ms:redis生成了一个订单任务：订单ID为"+"OID0000001"+i);
		}
	}

	//消费者，取订单
	public void consumerDelayMessage(){
		Jedis jedis = RedisDelayQueue.getJedis();
		while(true){
			Set<Tuple> items = jedis.zrangeWithScores("OrderId", 0, 1);
			if(items == null || items.isEmpty()){
				System.out.println("当前没有等待的任务");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			}
			int  score = (int) ((Tuple)items.toArray()[0]).getScore();
			Calendar cal = Calendar.getInstance();
			int nowSecond = (int) (cal.getTimeInMillis() / 1000);
			/*if(nowSecond >= score){
				String orderId = ((Tuple)items.toArray()[0]).getElement();
				jedis.zrem("OrderId", orderId);
				System.out.println(System.currentTimeMillis() +"ms:redis消费了一个任务：消费的订单OrderId为"+orderId);
			}*/
			if(nowSecond >= score) {
				String orderId = ((Tuple) items.toArray()[0]).getElement();
				Long num = jedis.zrem("OrderId", orderId);
				if (num != null && num > 0) {
					System.out.println(System.currentTimeMillis() + "ms:redis消费了一个任务：消费的订单OrderId为" + orderId);
				}
			}
		}
	}

	public static void main(String[] args) {
		RedisDelayQueue redisDelayQueue = new RedisDelayQueue();
		redisDelayQueue.productionDelayMessage();
		redisDelayQueue.consumerDelayMessage();

//		然而，这一版存在一个致命的硬伤，在高并发条件下，多消费者会取到同一个订单号，我们上测试代码ThreadTest
//		class ThreadTest {
//			private static final int threadNum = 10;
//			private CountDownLatch cdl = new CountDownLatch(threadNum);
//
//			class DelayMessage implements Runnable {
//				public void run() {
//					try {
//						cdl.await();
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					RedisDelayQueue redisDelayQueue1 = new RedisDelayQueue();
//					redisDelayQueue1.consumerDelayMessage();
//				}
//			}
//			void main(String[] args) {
//				RedisDelayQueue appTest =new RedisDelayQueue();
//				appTest.productionDelayMessage();
//				for(int i=0;i<threadNum;i++){
//					new Thread(new DelayMessage()).start();
//					cdl.countDown();
//				}
//			}
//
//		}
		/*解决方案：
		(1)用分布式锁，但是用分布式锁，性能下降了，该方案不细说。
		(2)对ZREM的返回值进行判断，只有大于0的时候，才消费数据，于是将consumerDelayMessage()方法里的
		if(nowSecond >= score){
			String orderId = ((Tuple)items.toArray()[0]).getElement();
			jedis.zrem("OrderId", orderId);
			System.out.println(System.currentTimeMillis()+"ms:redis消费了一个任务：消费的订单OrderId为"+orderId);
		}
		修改为
		if(nowSecond >= score){
			String orderId = ((Tuple)items.toArray()[0]).getElement();
			Long num = jedis.zrem("OrderId", orderId);
			if( num != null && num>0){
				System.out.println(System.currentTimeMillis()+"ms:redis消费了一个任务：消费的订单OrderId为"+orderId);
			}
		}*/

	}

}


