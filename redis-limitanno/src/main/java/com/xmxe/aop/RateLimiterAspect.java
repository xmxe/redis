package com.xmxe.aop;

import com.xmxe.anno.RateLimiter;
import com.xmxe.enums.LimitType;
import com.xmxe.util.IpUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.service.spi.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

@Aspect
@Component
public class RateLimiterAspect {
	private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

	@Autowired
	private RedisTemplate<Object, Object> redisTemplate;

	@Autowired
	private RedisScript<Long> limitScript;

	@Before("@annotation(rateLimiter)")
	public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable {
		String key = rateLimiter.key();
		int time = rateLimiter.time();
		int count = rateLimiter.count();

		String combineKey = getCombineKey(rateLimiter, point);
		List<Object> keys = Collections.singletonList(combineKey);
		try {
			Long number = redisTemplate.execute(limitScript, keys, count, time);
			if (number==null || number.intValue() > count) {
				throw new ServiceException("访问过于频繁，请稍候再试");
			}
			log.info("限制请求'{}',当前请求'{}',缓存key'{}'", count, number.intValue(), key);
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("服务器限流异常，请稍候再试");
		}
	}

	/**
	 * 把用户IP和接口方法名拼接成redis的key
	 * @param point 切入点
	 * @return 组合key
	 */
	public String getCombineKey(RateLimiter rateLimiter, JoinPoint point) {
		StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());
		if (rateLimiter.limitType() == LimitType.IP) {
			stringBuffer.append(IpUtils.getIpAddr(((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest())).append("-");
		}
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();
		Class<?> targetClass = method.getDeclaringClass();
		stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
		return stringBuffer.toString();
	}
}
/**
 * 首先获取到注解中的key、time以及count三个参数。
 * 获取一个组合的key，所谓的组合的key，就是在注解的key属性基础上，再加上方法的完整路径，如果是IP模式的话，就再加上IP地址。以IP模式为例，最终生成的key类似这样：rate_limit:127.0.0.1-org.javaboy.ratelimiter.controller.HelloController-hello（如果不是IP模式，那么生成的key中就不包含IP地址）。
 * 将生成的key放到集合中。
 * 通过redisTemplate.execute方法取执行一个Lua脚本，第一个参数是脚本所封装的对象，第二个参数是key，对应了脚本中的KEYS，后面是可变长度的参数，对应了脚本中的ARGV。
 * 将Lua脚本执行的结果与count进行比较，如果大于count，就说明过载了，抛异常就行了。
 */