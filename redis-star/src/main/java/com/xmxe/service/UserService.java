package com.xmxe.service;


import com.xmxe.dataobject.UserInfo;
import com.xmxe.dto.ExperienceDTO;
import com.xmxe.dto.RoleDTO;
import com.xmxe.dto.UserInfoDTO;

import java.util.List;

public interface UserService {
    /**
     * 注册
     */
    UserInfoDTO registerByEmail(UserInfoDTO userInfo);

    /**
     * 更新个人信息
     */
    UserInfo updateInfo(UserInfo userInfo);

    /**
     * 用邮箱登录
     */
    UserInfoDTO loginByEmail(String email, String password);

    /**
     * 用电话登录
     */
    UserInfo loginByTel(String tel, String password);

    /**
     * 注销登录
     */
    void logout(String userId);

    /**
     * 通过id查询用户
     */
    UserInfo findById(String id);

    /**
     * 重置/修改 密码
     */
    UserInfo resetPassword(String id, String oldPassword, String newPassword);

    /**
     * 查询所有用户信息
     */
    List<UserInfo> findAll(Integer page, Integer size, Integer sort);

    /**
     * 通过角色筛选该角色下的所有用户
     */
    List<UserInfo> findAllByRole(Integer page, Integer size, Integer sort, Integer role);

    /**
     * 通过经验筛选符合条件的用户
     *
     * @param experience 用户经验
     */
    List<UserInfo> findAllByExperience(Integer page, Integer size, Integer sort, Integer experience);

    /**
     * 通过角色和经验双重筛选符合条件的用户
     *
     * @param role       用户角色
     * @param experience 用户经验
     */
    List<UserInfo> findAllByRoleAndExperience(Integer page, Integer size, Integer sort, Integer role, Integer experience);

    /**
     * 获取所有的角色信息
     */
    List<RoleDTO> findAllRoles();

    /**
     * 获取所有的经验信息
     */
    List<ExperienceDTO> findAllExperience();
}