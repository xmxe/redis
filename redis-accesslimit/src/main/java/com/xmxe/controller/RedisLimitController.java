package com.xmxe.controller;

import com.xmxe.anno.AccessLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class RedisLimitController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	/**
	 * 限流测试
	 */
	@GetMapping
	@AccessLimit(maxCount = 3,second = 60)
	public String limit(HttpServletRequest request) {
		logger.error("Access Limit Test");
		return "限流测试";
	}
}