package com.xmxe.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class RedisUtil {

	private static final Long SUCCESS = 1L;

	@Autowired
	private RedisTemplate<Object, Object> redisTemplate;
	//   1. RedisTemplate<Object, Object>
	//   2. StringRedisTemplate<String, String>

	/**
	 * 获取锁
	 * @param lockKey
	 * @param value
	 * @param expireTime：单位-秒
	 * @return
	 */
	public boolean getLock(String lockKey, Object value, int expireTime) {
		try {
			log.info("添加分布式锁key={},expireTime={}",lockKey,expireTime);
			String script = "if redis.call('setNx',KEYS[1],ARGV[1]) then if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('expire',KEYS[1],ARGV[2]) else return 0 end end";
			RedisScript<String> redisScript = new DefaultRedisScript<>(script, String.class);
			Object result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), value, expireTime);
			if (SUCCESS.equals(result)) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 释放锁
	 * @param lockKey
	 * @param value
	 * @return
	 */
	public boolean releaseLock(String lockKey, String value) {
		String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
		RedisScript<String> redisScript = new DefaultRedisScript<>(script, String.class);
		Object result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), value);
		if (SUCCESS.equals(result)) {
			return true;
		}
		return false;
	}

	/**
	 * 判断是否存在key
	 * @param key
	 * @return
	 */
	public boolean hasKey(String key){
		return redisTemplate.hasKey(key);
	}

	/**
	 * 将key-value的值+1
	 */
	public long incr(String key, long delta) {
		if (delta < 0) {
			throw new RuntimeException("递增因子必须大于0");
		}
		return redisTemplate.opsForValue().increment(key, delta);
	}

}