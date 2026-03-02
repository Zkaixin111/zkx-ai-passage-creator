package com.zkx.aipassagecreator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.zkx.aipassagecreator.exception.BusinessException;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.mapper.UserMapper;
import com.zkx.aipassagecreator.model.dto.*;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.enums.UserRoleEnum;
import com.zkx.aipassagecreator.model.vo.LoginUserVO;
import com.zkx.aipassagecreator.model.vo.UserVO;
import com.zkx.aipassagecreator.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.zkx.aipassagecreator.constant.UserConstant.ADMIN_ROLE;
import static com.zkx.aipassagecreator.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现
 * <p>
 * 覆盖用户注册、登录、鉴权、信息修改等完整业务逻辑。
 * 密码使用 MD5 + 盐值加密存储，登录态通过 Redis Session 管理。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 用户注册
     * <p>
     * 校验参数 → 查重 → 加密密码 → 插入数据库，返回新用户 ID。
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 创建用户，插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     * 用户登录
     * <p>
     * 校验参数 → 加密密码 → 查询匹配 → 写入 Session → 返回脱敏 VO。
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 返回脱敏的用户信息
        return getUserVO(user);
    }

    /**
     * 获取当前登录用户
     * <p>
     * 从 Session 中获取用户 ID，再从数据库查最新数据返回。
     * 这样既保证实时性，也避免 Session 中的旧数据影响后续操作。
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询当前用户信息（保证数据最新）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 用户注销
     * <p>
     * 清除 Session 中的用户登录态。
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * MD5 + 盐值加密
     * <p>
     * 对用户密码进行单向加密存储，防止明文泄露。
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "zkx";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 修改个人信息
     * <p>
     * 只更新请求中非空的字段（name/avatar/profile 可选），不修改其他内容。
     */
    @Override
    public boolean updateUser(UserUpdateRequest request, HttpServletRequest httpRequest) {
        User loginUser = getLoginUser(httpRequest);

        // 只更新不为空的字段
        if (StrUtil.isNotBlank(request.getUserName())) {
            loginUser.setUserName(request.getUserName());
        }
        if (StrUtil.isNotBlank(request.getUserAvatar())) {
            loginUser.setUserAvatar(request.getUserAvatar());
        }
        if (StrUtil.isNotBlank(request.getUserProfile())) {
            loginUser.setUserProfile(request.getUserProfile());
        }

        loginUser.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改失败");
        }
        return true;
    }

    /**
     * 修改密码
     * <p>
     * 校验参数 → 验证旧密码 → 更新为新密码 → 清除登录态（强制重新登录）。
     */
    @Override
    public boolean changePassword(UserPasswordRequest request, HttpServletRequest httpRequest) {
        // 1. 校验参数
        if (StrUtil.hasBlank(request.getOldPassword(), request.getNewPassword(), request.getCheckNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (request.getNewPassword().length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度过短");
        }
        if (!request.getNewPassword().equals(request.getCheckNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }

        // 2. 验证旧密码
        User loginUser = getLoginUser(httpRequest);
        String oldEncryptPassword = getEncryptPassword(request.getOldPassword());
        if (!loginUser.getUserPassword().equals(oldEncryptPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }

        // 3. 更新密码
        String newEncryptPassword = getEncryptPassword(request.getNewPassword());
        loginUser.setUserPassword(newEncryptPassword);
        loginUser.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "密码修改失败");
        }

        // 4. 清除登录态，要求重新登录
        httpRequest.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 分页查询用户列表
     * <p>
     * 支持按用户名模糊搜索、按账号精确匹配、自定义排序字段。
     * 排序字段通过白名单校验防止 SQL 注入。
     */
    @Override
    public Page<UserVO> queryUserPage(UserQueryRequest request) {
        QueryWrapper queryWrapper = new QueryWrapper();

        // 模糊查询用户名
        if (StrUtil.isNotBlank(request.getUserName())) {
            queryWrapper.like("userName", request.getUserName());
        }
        // 账号精确匹配
        if (StrUtil.isNotBlank(request.getUserAccount())) {
            queryWrapper.eq("userAccount", request.getUserAccount());
        }

        // 排序处理
        String sortField = request.getSortField();
        String sortOrder = request.getSortOrder();
        if (StrUtil.isNotBlank(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            // 防止 SQL 注入：sortField 只能是已知的字段名
            if (isSafeField(sortField)) {
                queryWrapper.orderBy(sortField, isAsc);
            }
        } else {
            // 默认按创建时间降序
            queryWrapper.orderBy("createTime", false);
        }

        Page<User> userPage = this.mapper.paginate(request.getPageNum(), request.getPageSize(), queryWrapper);

        // 转换为 UserVO 分页对象
        Page<UserVO> voPage = new Page<>();
        voPage.setTotalRow(userPage.getTotalRow());
        voPage.setPageNumber(userPage.getPageNumber());
        voPage.setPageSize(userPage.getPageSize());
        voPage.setRecords(userPage.getRecords().stream().map(this::convertToUserVO).toList());

        return voPage;
    }

    /**
     * 删除用户（逻辑删除）
     * <p>
     * MyBatis-Flex 的 @Column(isLogicDelete = true) 注解自动将 delete 操作转为 UPDATE isDelete。
     */
    @Override
    public boolean deleteUser(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 无效");
        }
        // 使用逻辑删除（MyBatis-Flex @Column(isLogicDelete = true) 自动处理）
        boolean result = this.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败，用户不存在");
        }
        return true;
    }

    /**
     * 管理员修改用户信息
     * <p>
     * 可修改昵称、头像、简介、角色。只更新非空字段，且需校验角色值的合法性。
     */
    @Override
    public boolean updateUserByAdmin(UserUpdateByAdminRequest request) {
        if (request.getId() == null || request.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 无效");
        }

        User user = this.getById(request.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 只更新不为空的字段
        if (StrUtil.isNotBlank(request.getUserName())) {
            user.setUserName(request.getUserName());
        }
        if (StrUtil.isNotBlank(request.getUserAvatar())) {
            user.setUserAvatar(request.getUserAvatar());
        }
        if (StrUtil.isNotBlank(request.getUserProfile())) {
            user.setUserProfile(request.getUserProfile());
        }
        if (StrUtil.isNotBlank(request.getUserRole())) {
            // 校验角色合法性
            UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(request.getUserRole());
            if (roleEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色值无效");
            }
            user.setUserRole(request.getUserRole());
        }

        user.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改失败");
        }
        return true;
    }

    /**
     * 管理员添加用户
     * <p>
     * 校验参数 → 查重 → 加密密码 → 插入数据库。
     * 与注册逻辑类似，但管理员可以指定用户角色。
     */
    @Override
    public long addUser(UserAddRequest request) {
        if (StrUtil.hasBlank(request.getUserAccount(), request.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }
        if (request.getUserAccount().length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (request.getUserPassword().length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }

        // 检查账号重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", request.getUserAccount());
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        // 加密密码
        String encryptPassword = getEncryptPassword(request.getUserPassword());

        User user = new User();
        user.setUserAccount(request.getUserAccount());
        user.setUserPassword(encryptPassword);
        user.setUserName(StrUtil.isNotBlank(request.getUserName()) ? request.getUserName() : "无名");

        // 校验角色合法性
        if (StrUtil.isNotBlank(request.getUserRole())) {
            UserRoleEnum roleEnum = UserRoleEnum.getEnumByValue(request.getUserRole());
            if (roleEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "角色值无效");
            }
            user.setUserRole(request.getUserRole());
        } else {
            user.setUserRole(UserRoleEnum.USER.getValue());
        }

        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加失败");
        }
        return user.getId();
    }

    /**
     * 判断当前用户是否为管理员
     * <p>
     * 先调用 getLoginUser 确保已登录，再比对角色。
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User user = getLoginUser(request);
        return ADMIN_ROLE.equals(user.getUserRole());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * User 实体转 LoginUserVO（脱敏视图）
     */
    private LoginUserVO getUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserName(user.getUserName());
        loginUserVO.setUserAvatar(user.getUserAvatar());
        loginUserVO.setUserProfile(user.getUserProfile());
        loginUserVO.setUserRole(user.getUserRole());
        loginUserVO.setCreateTime(user.getCreateTime());
        loginUserVO.setUpdateTime(user.getUpdateTime());
        return loginUserVO;
    }

    /**
     * User 实体转 UserVO（管理端完整视图，不含密码）
     */
    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    /**
     * 校验排序字段是否安全（防止 SQL 注入）
     */
    private boolean isSafeField(String field) {
        return "id".equals(field)
                || "userAccount".equals(field)
                || "userName".equals(field)
                || "userRole".equals(field)
                || "createTime".equals(field)
                || "updateTime".equals(field)
                || "editTime".equals(field);
    }
}
