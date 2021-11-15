package com.xmxe.distributedlock.lock_demo1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RedisLockUtil {
    private static Logger log = LoggerFactory.getLogger(RedisLockUtil.class);
    /**
     * 默认轮休获取锁间隔时间， 单位：毫秒
     */
    private static final int DEFAULT_ACQUIRE_RESOLUTION_MILLIS = 100;

    private static final String UNLOCK_LUA;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if redis.call(\"get\",KEYS[1]) == ARGV[1] ");
        sb.append("then ");
        sb.append("    return redis.call(\"del\",KEYS[1]) ");
        sb.append("else ");
        sb.append("    return 0 ");
        sb.append("end ");
        UNLOCK_LUA = sb.toString();
    }

    /**
     * 获取锁，没有获取到则一直等待，异常情况则返回null
     *
     * @param redisTemplate redis连接
     * @param key           redis key
     * @param expire        锁过期时间, 单位 秒
     * @return 当前锁唯一id，如果没有获取到，返回 null
     */
    public static String lock(RedisTemplate<?, ?> redisTemplate, final String key, long expire) {
        return lock(redisTemplate, key, expire, -1);
    }

    /**
     * 获取锁，acquireTimeout时间内没有获取到，则返回null，异常情况返回null
     *
     * @param redisTemplate  redis连接
     * @param key            redis key
     * @param expire         锁过期时间, 单位 秒
     * @param acquireTimeout 获取锁超时时间, -1代表永不超时, 单位 秒
     * @return 当前锁唯一id，如果没有获取到，返回 null
     */
    public static String lock(RedisTemplate<?, ?> redisTemplate, final String key, long expire, long acquireTimeout) {
        try {
            return acquireLock(redisTemplate, key, expire, acquireTimeout);
        } catch (Exception e) {
            log.error("acquire lock exception", e);
        }
        return null;
    }

    /**
     * 获取锁，没有获取到则一直等待，没有获取到则抛出异常
     *
     * @param redisTemplate redis连接
     * @param key           redis key
     * @param expire        锁过期时间, 单位 秒
     * @return 当前锁唯一id，如果没有获取到，返回 null
     */
    public static String lockFailThrowException(RedisTemplate<?, ?> redisTemplate, final String key, long expire) {
        return lockFailThrowException(redisTemplate, key, expire, -1);
    }

    /**
     * 获取锁，到达超时时间时没有获取到，则抛出异常
     *
     * @param redisTemplate  redis连接
     * @param key            redis key
     * @param expire         锁过期时间, 单位 秒
     * @param acquireTimeout 获取锁超时时间, -1代表永不超时, 单位 秒
     * @return 当前锁唯一id，如果没有获取到，返回 null
     */
    public static String lockFailThrowException(RedisTemplate<?, ?> redisTemplate, final String key, long expire,
            long acquireTimeout) {
        try {
            String lockId = acquireLock(redisTemplate, key, expire, acquireTimeout);
            if (lockId != null) {
                return lockId;
            }
            throw new RuntimeException("acquire lock fail");
        } catch (Exception e) {
            throw new RuntimeException("acquire lock exception", e);
        }
    }

    private static String acquireLock(RedisTemplate<?, ?> redisTemplate, String key, long expire, long acquireTimeout)
            throws InterruptedException {
        long acquireTime = -1;
        if (acquireTimeout != -1) {
            acquireTime = acquireTimeout * 1000 + System.currentTimeMillis();
        }
        synchronized (key) {
            String lockId = UUID.randomUUID().toString();
            while (true) {

                if (acquireTime != -1 && acquireTime < System.currentTimeMillis()) {
                    break;
                }
                // 调用tryLock
                boolean hasLock = tryLock(redisTemplate, key, expire, lockId);

                // 获取锁成功
                if (hasLock) {
                    return lockId;
                }
                Thread.sleep(DEFAULT_ACQUIRE_RESOLUTION_MILLIS);
            }
        }
        return null;
    }

    /**
     * 释放锁
     *
     * @param redisTemplate redis连接
     * @param key           redis key
     * @param lockId        当前锁唯一id
     */
    public static void unlock(RedisTemplate<?, ?> redisTemplate, String key, String lockId) {
        try {
            RedisCallback<Boolean> callback = (connection) -> connection.eval(
                    UNLOCK_LUA.getBytes(StandardCharsets.UTF_8), ReturnType.BOOLEAN, 1,
                    (com.xmxe.distributedlock.lock_demo1.RedisPrefix.LOCK_REDIS_PREFIX + key).getBytes(StandardCharsets.UTF_8),
                    lockId.getBytes(StandardCharsets.UTF_8));
            redisTemplate.execute(callback);
        } catch (Exception e) {
            log.error("release lock exception", e);
        }
    }

    /**
     * 获取当前锁的id
     *
     * @param key redis key
     * @return 当前锁唯一id
     */
    public static String get(RedisTemplate<?, ?> redisTemplate, String key) {
        try {
            RedisCallback<String> callback = (connection) -> {
                byte[] bytes = connection.get((com.xmxe.distributedlock.lock_demo1.RedisPrefix.LOCK_REDIS_PREFIX + key).getBytes(StandardCharsets.UTF_8));
                if (bytes != null) {
                    return new String(bytes, StandardCharsets.UTF_8);
                }
                return null;
            };
            return (String) redisTemplate.execute(callback);
        } catch (Exception e) {
            log.error("get lock id exception", e);
        }
        return null;
    }

    private static boolean tryLock(RedisTemplate<?, ?> redisTemplate, String key, long expire, String lockId) {
        RedisCallback<Boolean> callback = (connection) -> connection.set(
                (com.xmxe.distributedlock.lock_demo1.RedisPrefix.LOCK_REDIS_PREFIX + key).getBytes(StandardCharsets.UTF_8),
                lockId.getBytes(StandardCharsets.UTF_8), Expiration.seconds(expire),
                RedisStringCommands.SetOption.SET_IF_ABSENT);
        return (Boolean) redisTemplate.execute(callback);
    }

}
