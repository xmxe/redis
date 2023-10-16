package com.xmxe.controller;

import com.xmxe.anno.ClearAndReloadCache;
import com.xmxe.config.User;
import com.xmxe.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制层
 */
@RequestMapping("/user")
@RestController
public class UserController {
	@Autowired
	private UserService userService;

	@GetMapping("/get/{id}")
	//@Cacheable(cacheNames = {"get"})
	public String get(@PathVariable("id") Integer id){
		return userService.get(id);
	}

	@PostMapping("/updateData")
	@ClearAndReloadCache(name = "get method")
	public String updateData(@RequestBody User user){
		return userService.update(user);
	}

	@PostMapping("/insert")
	public String insert(@RequestBody User user){
		return userService.insert(user);
	}

	@DeleteMapping("/delete/{id}")
	public String delete(@PathVariable("id") Integer id){
		return userService.delete(id);
	}
}