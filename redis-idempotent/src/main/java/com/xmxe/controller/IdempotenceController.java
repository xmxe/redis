package com.xmxe.controller;

import com.xmxe.anno.IdempotenceRequired;
import com.xmxe.util.IdempotenceUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/idempotence")
public class IdempotenceController {

	@Resource
	IdempotenceUtil idempotenceUtil;
	/**
	 * 生成幂等号
	 */
	@GetMapping("/generateId")
	public String generateId(){
		String uId = idempotenceUtil.generateId();
		System.out.println("生成成功");
		return uId;
	}

	@IdempotenceRequired
	@PostMapping("/getUsers")
	public String getUsers(){
		//执行正常业务逻辑
        // ...
		return null;
	}
	/*
	 * 使用
	 * 服务端:不是所有的方法都需要切面拦截,只有IdempotenceRequired注解的方法才会被拦截。
	 * 在开发幂等接口时，只需要在方法上简单增加一个IdempotenceRequired注解即可。
	 *
	 * 客户端:服务端处理好后，在客户端访问接口的时候需要执行以下步骤：
	 * 需要先获取幂等号,然后将幂等号添加到请求头中
	 * 1.获取幂等号 http://服务地址/idempotence/generateId
	 * 2.请求调用,往header中添加幂等号
	 */
}