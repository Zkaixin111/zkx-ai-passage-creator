package com.zkx.aipassagecreator.model.dto;

import com.zkx.aipassagecreator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户列表查询请求（管理员）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userName;
    private String userAccount;
}
