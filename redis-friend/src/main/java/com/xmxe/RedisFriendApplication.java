package com.xmxe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RedisFriendApplication {
	public static void main(String[] args) {
		SpringApplication.run(RedisFriendApplication.class, args);
	}
}