package com.xmxe.controller;

import com.xmxe.anno.AutoIdempotent;
import com.xmxe.service.TokenService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
/**
 * 模拟业务请求类，首先我们需要通过/get/token路径通过getToken()方法去获取具体的token，然后我们调用testIdempotence方法，
 * 这个方法上面注解了@AutoIdempotent，拦截器会拦截所有的请求，当判断到处理的方法上面有该注解的时候，就会调用TokenService中的checkToken()方法，如果捕获到异常会将异常抛出调用者，
 * 下面我们来模拟请求一下：
 */
@RestController
public class ReSubmitController {


	@Resource
	private TokenService tokenService;

	@PostMapping("/get/token")
	public String getToken() {
		String token = tokenService.createToken();

		return token;
	}


	@AutoIdempotent
	@PostMapping("/test/Idempotence")
	public String testIdempotence() {
		return "";
	}
}