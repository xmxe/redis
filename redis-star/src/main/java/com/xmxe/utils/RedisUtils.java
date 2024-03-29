package com.xmxe.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 操作Redis的工具类
 */
@Component
public class RedisUtils {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public void setString(String key, String value){
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public void setString(String key, String value, Long expired, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, value, expired, timeUnit);
    }

    public void deleteString(String key){
        stringRedisTemplate.delete(key);
    }
}