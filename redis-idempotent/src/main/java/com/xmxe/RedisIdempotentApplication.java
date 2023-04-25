package com.xmxe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisIdempotentApplication {
	public static void main(String[] args) {
		SpringApplication.run(RedisIdempotentApplication.class, args);
	}
}