package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员修改用户请求
 */
@Data
public class UserUpdateByAdminRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
}
