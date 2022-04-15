package com.xmxe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
public class RedisStockApplication {
	public static void main(String[] args) {
		SpringApplication.run(RedisStockApplication.class, args);
	}
}
