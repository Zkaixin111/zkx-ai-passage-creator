package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求 DTO
 */
@Data
public class UserRegisterRequest implements Serializable {
    private String userAccount;
    private String userPassword;
    private String checkPassword;
}
