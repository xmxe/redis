package com.xmxe.distributedlock.lock_demo3;

import com.xmxe.util.JedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.UUID;

public class DistributedLock2 {
	private Logger log = LoggerFactory.getLogger(DistributedLock2.class);

	private static final String LOCK_SUCCESS = "OK";
	private static final Long RELEASE_SUCCESS = 1L;
	private static final String SET_IF_NOT_EXIST = "NX";
	private static final String SET_WITH_EXPIRE_TIME = "PX";
	private static Jedis jedis = JedisUtil.getRedisUtil().getJedis();
	// 设置获取锁的超时时间，超过这个时间放弃获取锁
	public long acquireTimeout = 30000;
	// 设置锁的过期时间
	public long expireTime = 20000;
	// 设置key
	private final String lockKey = "lock";

	/**
	 * 获取锁
	 * @return
	 */
	public String acquire() {
		try {
			// 获取锁的超时时间，超过这个时间则放弃获取锁
			long end = System.currentTimeMillis() + acquireTimeout;
			// 随机生成一个 value
			String requireToken = UUID.randomUUID().toString();
			while (System.currentTimeMillis() < end) {
				SetParams setParams = SetParams.setParams().nx().px(expireTime);
				// String result = jedis.set(lockKey, requireToken, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);
				String result = jedis.set(lockKey, requireToken, setParams);
				if (LOCK_SUCCESS.equals(result)) {
					return requireToken;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		} catch (Exception e) {
			log.error("acquire lock due to error", e);
		}

		return null;
	}

	/**
	 * 释放锁
	 * @param identify
	 * @return
	 */
	public boolean release(String identify) {
		if (identify == null) {
			return false;
		}

		String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
		Object result = new Object();
		try {
			result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(identify));
			if (RELEASE_SUCCESS.equals(result)) {
				log.info("release lock success, requestToken:{}", identify);
				return true;
			}
		} catch (Exception e) {
			log.error("release lock due to error", e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}

		log.info("release lock failed, requestToken:{}, result:{}", identify, result);
		return false;
	}
}
