package com.xmxe.util;

import org.apache.logging.log4j.util.Base64Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
public class IdempotenceUtil {

	@Autowired
	private RedisTemplateUtil redisTemplate;
	/**
	 * 生成幂等号
	 */
	public String generateId() {
		String uuid = UUID.randomUUID().toString();
		String uId= Base64Util.encode(uuid).toLowerCase();
		redisTemplate.setEx(uId,"1",1800L);
		return uId;
	}

	/**
	 * 从Header里面获取幂等号
	 */
	public String getHeaderIdempotenceId(){
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = attributes.getRequest();
		String idempotenceId=request.getHeader("idempotenceId");
		return idempotenceId;
	}
}