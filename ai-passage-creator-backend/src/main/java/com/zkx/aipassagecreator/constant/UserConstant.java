package com.zkx.aipassagecreator.constant;

/**
 * 用户模块常量
 * <p>
 * 定义 Session 键、角色常量等全局使用的值。
 */
public interface UserConstant {
    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";
  
    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";
  
    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 会员类型：免费
     */
    String VIP_TYPE_NONE = "NONE";

    /**
     * 会员类型：永久 VIP
     */
    String VIP_TYPE_PERMANENT = "PERMANENT";

    /**
     * 免费用户默认创作配额（次）
     */
    int DEFAULT_QUOTA_TOTAL = 5;
}
