package com.xmxe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RedisStarApplication {
	public static void main(String[] args) {
		SpringApplication.run(RedisStarApplication.class, args);
	}
}
