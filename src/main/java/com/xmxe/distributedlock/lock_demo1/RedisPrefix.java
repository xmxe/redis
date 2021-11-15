package com.xmxe.distributedlock.lock_demo1;

/**
 * redis key 前缀
 * 默认key规范 数据的意义_系统编码_唯一的标识
 */

public class RedisPrefix {
    /**
     * 鉴权信息 key 前缀
     */
    public static final String AUTH_REDIS_PREFIX = "UNIF_AUTH_";

    /**
     * 缓存信息 key 前缀
     */
    public static final String CACHE_REDIS_PREFIX = "UNIF_CACHE_";

    /**
     * 分布式锁信息 key 前缀
     */
    public static final String LOCK_REDIS_PREFIX = "UNIF_LOCK_";

}
