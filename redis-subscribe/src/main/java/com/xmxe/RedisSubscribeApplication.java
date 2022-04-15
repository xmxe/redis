package com.xmxe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class RedisSubscribeApplication {
	private static String CHANNEL = "didispace";

	public static void main(String[] args) {
		SpringApplication.run(RedisSubscribeApplication.class, args);
	}


}
