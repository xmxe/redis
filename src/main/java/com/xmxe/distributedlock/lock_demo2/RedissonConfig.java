package com.xmxe.distributedlock.lock_demo2;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
/**
 * @ClassName:  RedissonConfig   
 * @Description:(双端检索单例工厂模式--》创建 redissonClient 交由 spring IOC管理)   
 */
@Configuration
public class RedissonConfig {
	private static RedissonClient redissonClient;
	private static final String host = "redis.host";
	private static final String port = "redis.port";
	private static final String password = "redis.pass";
	private static final String database = "redis.database";
	private static final String timeout = "cachePool.timeout";
	
	@Bean
	public static RedissonClient getRedisson() {
		if (redissonClient == null) {
			synchronized (RedissonConfig.class) {
				if (redissonClient == null) {
					redissonClient = getAndSetRedisson();
				}
			}
		}
		return redissonClient;
	}
	
	public static RedissonClient getAndSetRedisson() {
		try {
			System.out.println("获取redissonClient......");
			if (redissonClient == null) {
				System.out.println("redissonClient为空,创建中......");
				synchronized (RedissonConfig.class) {
					if (redissonClient == null) {
						Config config = new Config();
						config.useSingleServer().setAddress("redis://"+host+":"+port)
												.setPassword(password)
//												.setConnectionPoolSize(64)//连接池最大连接数，默认 64
//								                .setConnectionMinimumIdleSize(10)//最小空闲连接数，默认10
								                .setConnectTimeout(5000) //默认值：10000 同节点建立连接时的等待超时。时间单位是毫秒
								                .setTimeout(3000) //默认值：3000 等待节点回复命令的时间。该时间从命令发送成功时开始计时
								                .setDatabase(Integer.valueOf(database));
						redissonClient = Redisson.create(config);
						System.out.println("redissonClient,创建成功......");
					}
				}
			}
		} catch (Exception e) {
			System.out.println("redissonClient,创建异常......");
			e.printStackTrace();
		}
		return redissonClient;
	}
 
}
