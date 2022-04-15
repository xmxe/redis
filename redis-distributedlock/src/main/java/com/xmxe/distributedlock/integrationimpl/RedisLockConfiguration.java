package com.xmxe.distributedlock.integrationimpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

/**
 * 分布式锁配置类
 */
@Configuration
public class RedisLockConfiguration {

	@Bean
	public RedisLockRegistry redisLockRegistry(RedisConnectionFactory redisConnectionFactory){
		return new RedisLockRegistry(redisConnectionFactory, "redis-lock");
	}

}