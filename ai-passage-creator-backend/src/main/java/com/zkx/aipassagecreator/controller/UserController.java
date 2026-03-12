package com.zkx.aipassagecreator.controller;

import com.mybatisflex.core.paginate.Page;
import com.zkx.aipassagecreator.annotation.AuthCheck;
import com.zkx.aipassagecreator.common.BaseResponse;
import com.zkx.aipassagecreator.common.ResultUtils;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.exception.ThrowUtils;
import com.zkx.aipassagecreator.model.dto.*;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.vo.LoginUserVO;
import com.zkx.aipassagecreator.model.vo.UserVO;
import com.zkx.aipassagecreator.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 用户模块控制器
 * <p>
 * 提供用户注册、登录、信息管理功能。
 * 管理员接口通过 @AuthCheck 注解进行权限校验，无需在方法内手写判断逻辑。
 */
@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    // ==================== 公开接口 ====================

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(userId);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    // ==================== 需要登录的接口 ====================

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public BaseResponse<LoginUserVO> getCurrentUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUserAccount(user.getUserAccount());
        loginUserVO.setUserName(user.getUserName());
        loginUserVO.setUserAvatar(user.getUserAvatar());
        loginUserVO.setUserProfile(user.getUserProfile());
        loginUserVO.setUserRole(user.getUserRole());
        // MVP 阶段不做配额限制，返回大值使前端 hasQuota 放行
        loginUserVO.setQuota(Long.MAX_VALUE);
        loginUserVO.setCreateTime(user.getCreateTime());
        loginUserVO.setUpdateTime(user.getUpdateTime());
        return ResultUtils.success(loginUserVO);
    }

    @Operation(summary = "用户注销")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public BaseResponse<Boolean> changePassword(@RequestBody UserPasswordRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.changePassword(request, httpRequest);
        return ResultUtils.success(result);
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/profile")
    public BaseResponse<Boolean> updateProfile(@RequestBody UserUpdateRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.updateUser(request, httpRequest);
        return ResultUtils.success(result);
    }

    // ==================== 管理员接口 ====================

    @AuthCheck(mustRole = "admin")
    @Operation(summary = "用户列表查询（管理员）")
    @GetMapping("/list")
    public BaseResponse<Page<UserVO>> listUsers(UserQueryRequest request) {
        Page<UserVO> page = userService.queryUserPage(request);
        return ResultUtils.success(page);
    }

    @AuthCheck(mustRole = "admin")
    @Operation(summary = "删除用户（管理员）")
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteUser(@PathVariable long id) {
        boolean result = userService.deleteUser(id);
        return ResultUtils.success(result);
    }

    @AuthCheck(mustRole = "admin")
    @Operation(summary = "修改用户信息（管理员）")
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateUserByAdmin(@PathVariable long id, @RequestBody UserUpdateByAdminRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        request.setId(id);
        boolean result = userService.updateUserByAdmin(request);
        return ResultUtils.success(result);
    }

    @AuthCheck(mustRole = "admin")
    @Operation(summary = "添加用户（管理员）")
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.addUser(request);
        return ResultUtils.success(userId);
    }
}
