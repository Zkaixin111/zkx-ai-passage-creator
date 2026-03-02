package com.zkx.aipassagecreator.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.zkx.aipassagecreator.model.dto.*;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.vo.LoginUserVO;
import com.zkx.aipassagecreator.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取加密密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 修改个人信息
     */
    boolean updateUser(UserUpdateRequest request, HttpServletRequest httpRequest);

    /**
     * 修改密码
     */
    boolean changePassword(UserPasswordRequest request, HttpServletRequest httpRequest);

    /**
     * 分页查询用户列表（管理员）
     */
    Page<UserVO> queryUserPage(UserQueryRequest request);

    /**
     * 删除用户（管理员）
     */
    boolean deleteUser(long id);

    /**
     * 管理员修改用户信息
     */
    boolean updateUserByAdmin(UserUpdateByAdminRequest request);

    /**
     * 管理员添加用户
     */
    long addUser(UserAddRequest request);

    /**
     * 判断当前用户是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);
}
