package com.xmxe.controller;

import com.xmxe.service.FollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 关注/取关控制层
 */
@RestController
public class FollowController {

	@Resource
	private FollowService followService;
	@Resource
	private HttpServletRequest request;

	/**
	 * 关注/取关
	 *
	 * @param followUserId 关注的用户ID
	 * @param isFollowed    是否关注 1=关注 0=取消
	 * @param access_token  登录用户token
	 * @return
	 */
	@PostMapping("/{followUserId}")
	public Map<String,Object> follow(@PathVariable Integer followUserId,
	                                 @RequestParam int isFollowed,
	                                 String access_token) {
		Map<String,Object> resultInfo = followService.follow(followUserId,
				isFollowed, access_token, request.getServletPath());
		return resultInfo;
	}

	/**
	 * 共同关注列表
	 *
	 * @param userId
	 * @param access_token
	 * @return
	 */
	@GetMapping("commons/{userId}")
	public void findCommonsFriends(@PathVariable Integer userId,
	                                     String access_token) {
		followService.findCommonsFriends(userId, access_token, request.getServletPath());
	}


}