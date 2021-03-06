package com.xmxe.distributedlock;


import com.xmxe.distributedlock.redistemplateimpl.RedisTemplateDistributedLock;
import com.xmxe.distributedlock.redissonimpl.RedissonUtil;
import com.xmxe.distributedlock.jedisimpl.JedisDistributedLock;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class RedisDistributedLockTest {
    
    @Autowired
    private static RedissonUtil redissonUtils;

    @Autowired
    RedisTemplate<?,?> redisTemplate;

    public static void main(String[] args){
        RedisDistributedLockTest test = new RedisDistributedLockTest();
        test.redisTemplateTest();
        test.redissonTest();
        test.seckillTest();
    }

    public void redisTemplateTest(){
        String lockId = "";
        try{
            lockId = RedisTemplateDistributedLock.lockFailThrowException(redisTemplate, "addCustomerAdviser", 10);
        }finally{
            RedisTemplateDistributedLock.unlock(redisTemplate, "addCustomerAdviser", lockId);
        }
    }


    public void redissonTest(){
        RLock fairLock = null; 
        try {
            fairLock=redissonUtils.getLock("teacherListQueueLock_AFP");
            // 业务逻辑
        
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fairLock.unlock();
        }
    }
    
    public void seckillTest(){
        for (int i = 0; i < 50; i++) {
            Thread thread = new Thread(()->{
                seckill();
            });
            thread.start();
        }
    }

    /**
     * 秒杀
     */
    public void seckill() {
        JedisPool pool = null;
        JedisPoolConfig config = new JedisPoolConfig();
        // 设置最大连接数
        config.setMaxTotal(200);
        // 设置最大空闲数
        config.setMaxIdle(8);
        // 设置最大等待时间
        config.setMaxWaitMillis(1000 * 100);
        // 在borrow一个jedis实例时，是否需要验证，若为true，则所有jedis实例均是可用的
        config.setTestOnBorrow(true);
        pool = new JedisPool(config, "127.0.0.1", 6379, 3000);
        JedisDistributedLock lock = new JedisDistributedLock(pool);
        int n = 500;
        // 返回锁的value值，供释放锁时候进行判断
        String indentifier = lock.lockWithTimeout("resource", 5000, 1000);
        System.out.println(Thread.currentThread().getName() + "获得了锁");
        System.out.println(--n);
        lock.releaseLock("resource", indentifier);
    }
}
