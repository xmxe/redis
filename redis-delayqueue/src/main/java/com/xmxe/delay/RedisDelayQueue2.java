package com.xmxe.delay;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * 利用redis实现延时队列
 * 如生成订单30分钟未支付，则自动取消,生成订单60秒后给用户发短信
 * demo2
 *
 * 首先在在redis.conf中，加入一条配置
 * notify-keyspace-events Ex
 * 运行如下代码
 */
public class RedisDelayQueue2{
	private static final String ADDR = "127.0.0.1";
	private static final int PORT = 6379;
	private static JedisPool jedis = new JedisPool(ADDR, PORT);
	private static RedisSub sub = new RedisSub();

	public static void init() {
		new Thread(new Runnable() {
			public void run() {
				jedis.getResource().subscribe(sub, "__keyevent@0__:expired");
			}
		}).start();
	}

	public static void main(String[] args) throws InterruptedException {
		init();
		for(int i =0;i<10;i++){
			String orderId = "OID000000"+i;
			jedis.getResource().setex(orderId, 3, orderId);
			System.out.println(System.currentTimeMillis()+"ms:"+orderId+"订单生成");
		}
	}

	static class RedisSub extends JedisPubSub {
		@Override
		public void onMessage(String channel, String message) {
			System.out.println(System.currentTimeMillis()+"ms:"+message+"订单取消");
		}
	}

	/*
	 * ps:redis的pub/sub机制存在一个硬伤，官网内容如下
	 * 原:Because Redis Pub/Sub is fire and forget currently there is no way to use this feature if your application demands reliable notification of events, that is, if your Pub/Sub client disconnects, and reconnects later, all the events delivered during the time the client was disconnected are lost.
	 * 翻: Redis的发布/订阅目前是即发即弃(fire and forget)模式的，因此无法实现事件的可靠通知。也就是说，如果发布/订阅的客户端断链之后又重连，则在客户端断链期间的所有事件都丢失了。因此，方案二不是太推荐。当然，如果你对可靠性要求不高，可以使用。
	 */
}