package com.xmxe.controller;

import com.xmxe.anno.RateLimiter;
import com.xmxe.enums.LimitType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class RedisLimitController {

	@GetMapping("/hello")
	// 每一个 IP 地址，在 5 秒内只能访问 3 次。
	@RateLimiter(time = 5,count = 3,limitType = LimitType.IP)
	public String hello() {
		return "hello>>>"+new Date();
	}
}
