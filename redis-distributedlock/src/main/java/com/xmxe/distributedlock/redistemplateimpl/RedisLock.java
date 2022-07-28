package com.xmxe.distributedlock.redistemplateimpl;

import lombok.Data;

@Data
public class RedisLock {

	// 锁的key
	private String key;

	// 锁的值
	private String value;

	public RedisLock(String key, String value) {
		this.key = key;
		this.value = value;
	}
}