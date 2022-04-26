package com.xmxe.service.impl;

import com.xmxe.consts.RedisConsts;
import com.xmxe.dataobject.UserInfo;
import com.xmxe.dto.ExperienceDTO;
import com.xmxe.dto.RoleDTO;
import com.xmxe.dto.UserInfoDTO;
import com.xmxe.enums.ExperienceEnum;
import com.xmxe.enums.ResultEnum;
import com.xmxe.enums.RoleEnum;
import com.xmxe.enums.TalentsSortEnum;
import com.xmxe.exception.UserException;
import com.xmxe.repository.UserInfoRepository;
import com.xmxe.service.UserService;
import com.xmxe.utils.KeyUtils;
import com.xmxe.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    UserInfoRepository userRepository;

    @Autowired
    RedisUtils redisUtils;

    @Autowired
    StringRedisTemplate stringRedisTemplate;


    @Override
    @Transactional
    public UserInfoDTO registerByEmail(UserInfoDTO userInfo) {
        //校验邮箱是否被注册过
        UserInfo info = userRepository.findUserInfoByEmail(userInfo.getEmail());
        if (info != null) {
            //已经注册过了,抛出已注册异常
            throw new UserException(ResultEnum.EMAIL_ALREADY_REGISTER);
        }
        //没有被注册过，则存入数据库
        UserInfo user = new UserInfo();
        BeanUtils.copyProperties(userInfo, user);
        //赋值唯一的id
        String id = KeyUtils.genUniqueKey();
        user.setId(id);

        UserInfo result = userRepository.save(user);
        if (result == null) {
            throw new UserException(ResultEnum.REGISTER_FAIL);
        }

        UserInfoDTO dto = new UserInfoDTO();
        BeanUtils.copyProperties(result, dto);
        return dto;
    }

    @Override
    @Transactional
    public UserInfo updateInfo(UserInfo userInfo) {
        UserInfo result = userRepository.save(userInfo);
        return result;
    }

    @Override
    public UserInfoDTO loginByEmail(String email, String password) {
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(password)){
            throw new UserException(ResultEnum.EMAIL_PASSWORD_EMPTY);
        }

        UserInfo user = userRepository.findUserInfoByEmail(email);
        if (user == null) {
            throw new UserException(ResultEnum.EMAIL_NOT_EXIST);
        }
        if (!user.getPassword().equals(password)) {
            throw new UserException(ResultEnum.PASSWORD_ERROR);
        }

        //生成 token 并保存在 Redis 中
        String token = KeyUtils.genUniqueKey();
        //将token存储在 Redis 中。键是 TOKEN_用户id, 值是token
        redisUtils.setString(String.format(RedisConsts.TOKEN_TEMPLATE, user.getId()), token, 2l, TimeUnit.HOURS);

        UserInfoDTO dto = new UserInfoDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setToken(token);

        return dto;
    }

    @Override
    public UserInfo loginByTel(String tel, String password) {
        return null;
    }

    @Override
    public void logout(String userId) {
        // 从redis中删除当前用户对应的key
        String key = String.format(RedisConsts.TOKEN_TEMPLATE, userId);
        redisUtils.deleteString(key);
    }

    @Override
    public UserInfo findById(String id) {
        return userRepository.findUserInfoById(id);
    }

    @Override
    @Transactional
    public UserInfo resetPassword(String id, String oldPassword, String newPassword) {
        UserInfo userInfo = userRepository.findUserInfoById(id);
        String password = userInfo.getPassword();
        // 先检验旧密码
        if (!password.equals(oldPassword)){
            throw new UserException(ResultEnum.PASSWORD_ERROR);
        }
        // 密码检验通过保存新密码
        userInfo.setPassword(newPassword);
        UserInfo info = updateInfo(userInfo);
        if (info == null){
            throw new UserException(ResultEnum.RESET_PASSWORD_FAIL);
        }
        return info;
    }

    @Override
    public List<UserInfo> findAll(Integer page, Integer size, Integer sort) {
        // return userRepository.findAllByRole();
        return null;
    }

    @Override
    public List<UserInfo> findAllByRole(Integer page, Integer size, Integer sort, Integer role) {
        return null;
    }

    @Override
    public List<UserInfo> findAllByExperience(Integer page, Integer size, Integer sort, Integer experience) {
        return null;
    }

    @Override
    public List<UserInfo> findAllByRoleAndExperience(Integer page, Integer size, Integer sort, Integer role, Integer experience) {
        return null;
    }

    @Override
    public List<RoleDTO> findAllRoles() {
        RoleEnum[] roleEnums = RoleEnum.values();
        ArrayList<RoleDTO> dtos = new ArrayList<>();
        for (RoleEnum role : roleEnums){
            if (role.getCode() != 0){
                RoleDTO dto = new RoleDTO(role.getCode(), role.getMsg());
                dtos.add(dto);
            }
        }
        return dtos;
    }

    @Override
    public List<ExperienceDTO> findAllExperience() {
        ExperienceEnum[] experienceEnums = ExperienceEnum.values();
        ArrayList<ExperienceDTO> dtos = new ArrayList<>();
        for (ExperienceEnum experience : experienceEnums) {
            dtos.add(new ExperienceDTO(experience.getCode(), experience.getMsg()));
        }
        // Arrays.stream(experienceEnums).map()
        return dtos;
    }

    /**
     * 根据传入的排序标记生成带排序的 Pageable 对象
     *
     * @param page
     * @param size
     * @param sort
     * @return
     */
    private Pageable genSortedPageable(Integer page, Integer size, Integer sort) {
        //sort 为空则返回原列表
        if (sort == null) {
            return PageRequest.of(page, size);
        }
        if (sort.equals(TalentsSortEnum.DEFAULT.getCode())) {
            //默认排序，返回原列表
            return PageRequest.of(page, size);
        } else if (sort.equals(TalentsSortEnum.SORT_BY_EXPERIENCE.getCode())) {
            //根据经验排序.如果经验相同则比较影响力
            Sort orders = Sort.by(Sort.Direction.DESC, "experience", "influence");
            return PageRequest.of(page, size, orders);
        } else if (sort.equals(TalentsSortEnum.SORT_BY_INFLUENCE.getCode())) {
            //根据影响力排序.如果影响力相同则比较经验
            Sort orders = Sort.by(Sort.Direction.DESC, "influence", "experience");
            return PageRequest.of(page, size, orders);
        }
        return PageRequest.of(page, size);
    }
}
