package com.xmxe.distributedlock.redissonimpl;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
/**
 * redisson工具类
 */
@Component
public class RedissonUtil {

	@Autowired
	private RedissonClient redissonClient;

	/**
	 * 获取分布式锁
	 */
	public RLock getLock(String key) {
		RLock fairLock = null;
		try {
			fairLock = redissonClient.getLock(key);
			//等待60秒，获取锁后会在10后释放锁
			fairLock.tryLock(60,10,TimeUnit.SECONDS);
		} catch (Exception e) {
			System.out.println("distribute lock key: " +key +" ,获取锁异常");
			e.printStackTrace();
		}
        return fairLock;
	}

	/**
	 * 释放锁
	 */
	public void unLock(RLock rLock) {
		try {
			if (rLock != null) {
				rLock.unlock();
			} else {
				System.out.println("distribute lock 不存在");
			}
		} catch (Exception e) {
			System.out.println("distribute lock key: " + rLock.getName() + " ,释放锁异常");
			e.printStackTrace();
		}
	}
}