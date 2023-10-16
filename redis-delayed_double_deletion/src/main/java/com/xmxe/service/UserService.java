package com.xmxe.service;

import com.xmxe.config.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {



	public String get(Integer id){
		// LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
		// wrapper.eq(User::getId,id);
		// User user = userMapper.selectOne(wrapper);
		return "success";
	}

	public String insert(User user){
		// int line = userMapper.insert(user);
		// if(line > 0)
			return "success";
		// return "操作数据库失败";
	}

	public String delete(Integer id) {
		// LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
		// wrapper.eq(User::getId, id);
		// int line = userMapper.delete(wrapper);
		// if (line > 0)
		// 	return "success";
		return "操作数据库失败";
	}

	public String update(User user){
		int i = userMapper.updateById(user);
		if(i > 0)
			return "success";
		return "操作数据库失败";
	}
}