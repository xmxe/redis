package com.xmxe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RedisLimitAnnoApplication {
	public static void main(String[] args) {
		SpringApplication.run(RedisLimitAnnoApplication.class, args);
	}
}
