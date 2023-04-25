package com.xmxe.Idempotence;

import com.xmxe.util.RedisTemplateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisIdempotence implements Idempotence {

	@Autowired
	private RedisTemplateUtil redisTemplate;

	@Override
	public boolean check(String idempotenceId) {
		return redisTemplate.exists(idempotenceId);
	}

	@Override
	public void record(String idempotenceId) {
		redisTemplate.set(idempotenceId,"1");
	}

	@Override
	public void record(String idempotenceId,Integer time) {
		redisTemplate.setEx(idempotenceId,"1", Long.valueOf(time));
	}

	@Override
	public void delete(String idempotenceId) {
		redisTemplate.del(idempotenceId);
	}
}