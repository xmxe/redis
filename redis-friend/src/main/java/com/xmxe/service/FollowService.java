package com.xmxe.service;

import com.xmxe.entity.Follow;
import com.xmxe.mapper.FollowMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 关注/取关业务逻辑层
 */
@Service
public class FollowService {

	@Resource
	private RestTemplate restTemplate;
	@Resource
	private FollowMapper followMapper;
	@Resource
	private RedisTemplate redisTemplate;


	/**
	 * 关注/取关
	 *
	 * @param followUserId  关注的食客ID
	 * @param isFollowed    是否关注 1=关注 0=取关
	 * @param accessToken   登录用户token
	 * @param path          访问地址
	 * @return
	 */
	public Map<String,Object> follow(Integer followUserId, int isFollowed,
	                         String accessToken, String path) {
		// 是否选择了关注对象
		Assert.isTrue(followUserId == null || followUserId < 1,
				"请选择要关注的人");
		// 获取登录用户信息 (封装方法)
		Map<String,Object> dinerInfo = loadSignInDinerInfo(accessToken);
		// 获取当前登录用户与需要关注用户的关注信息
		Follow follow = followMapper.selectFollow((int)dinerInfo.get("id"), followUserId);

		// 如果没有关注信息，且要进行关注操作 -- 添加关注
		if (follow == null && isFollowed == 1) {
			// 添加关注信息
			int count = followMapper.save((int)dinerInfo.get("id"), followUserId);
			// 添加关注列表到Redis
			if (count == 1) {
				addToRedisSet((int)dinerInfo.get("id"), followUserId);
			}

			return Map.of("200","关注成功", path, "关注成功");
			// return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,"关注成功", path, "关注成功");
		}

		// 如果有关注信息，且目前处于关注状态，且要进行取关操作 --取关关注
		if (follow != null && follow.getIs_valid() == 1 && isFollowed == 0) {
			// 取关
			int count = followMapper.update(follow.getId(), isFollowed);
			// 移除Redis关注列表
			if (count == 1) {
				removeFromRedisSet((int)dinerInfo.get("id"), followUserId);
			}

			return Map.of("200","成功取关", path, "成功取关");
			// return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,"成功取关", path, "成功取关");
		}

		// 如果有关注信息，且目前处于取关状态，且要进行关注操作 -- 重新关注
		if (follow != null && follow.getIs_valid() == 0 && isFollowed == 1) {
			// 重新关注
			int count = followMapper.update(follow.getId(), isFollowed);
			// 添加关注列表到Redis
			if (count == 1) {
				addToRedisSet((int)dinerInfo.get("id"), followUserId);
			}
			return Map.of("200","关注成功", path, "关注成功");
			// return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,"关注成功", path, "关注成功");
		}

		return Map.of(path,"操作成功");
		// return ResultInfoUtil.buildSuccess(path, "操作成功");
	}

	/**
	 * 添加关注列表到 Redis
	 *
	 * @param dinerId
	 * @param followUserId
	 */
	private void addToRedisSet(Integer dinerId, Integer followUserId) {
		redisTemplate.opsForSet().add("_" + dinerId, followUserId);
		redisTemplate.opsForSet().add("_" + followUserId, dinerId);
	}

	/**
	 * 移除 Redis 关注列表
	 *
	 * @param dinerId
	 * @param followUserId
	 */
	private void removeFromRedisSet(Integer dinerId, Integer followUserId) {
		redisTemplate.opsForSet().remove("_" + dinerId, followUserId);
		redisTemplate.opsForSet().remove("_" + followUserId, dinerId);
	}

	/**
	 * 获取登录用户信息
	 *
	 * @param accessToken
	 * @return
	 */
	private Map<String,Object> loadSignInDinerInfo(String accessToken) {
		return new HashMap<String,Object>();
	}

	/**
	 * 共同关注列表
	 *
	 * @param userId
	 * @param accessToken
	 * @param path
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	public void findCommonsFriends(Integer userId, String accessToken, String path) {
		// 是否选择了查看对象
		Assert.isTrue(userId == null || userId < 1,
				"请选择要查看的人");
		// 获取登录用户信息
		Map<String,Object> userInfo = loadSignInDinerInfo(accessToken);
		// 获取登录用户的关注信息
		String loginuserKey = "_" + userInfo.get("id");
		// 获取登录用户查看对象的关注信息
		String userKey = "_" + userId;
		// 计算交集
		Set<Integer> userIds = redisTemplate.opsForSet().intersect(loginuserKey, userKey);
		// 没有
		if (userIds == null || userIds.isEmpty()) {
			// return ResultInfoUtil.buildSuccess(path, new ArrayList<ShortUserInfo>());

		}
		// // 调用食客服务根据ids查询食客信息
		// Map<String,Object> resultInfo = restTemplate.getForObject(usersServerName + "findByIds?access_token={accessToken}&ids={ids}",
		// 		ResultInfo.class, accessToken, StrUtil.join(",", userIds));
		// if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
		// 	resultInfo.setPath(path);
		// 	return resultInfo;
		// }
		// 处理结果集
		// List<LinkedHashMap> dinnerInfoMaps = (ArrayList) resultInfo.getData();
		// List<ShortUserInfo> userInfos = dinnerInfoMaps.stream()
		// 		.map(user -> BeanUtil.fillBeanWithMap(user, new ShortUserInfo(), true))
		// 		.collect(Collectors.toList());

		// return ResultInfoUtil.buildSuccess(path, userInfos);
	}

}