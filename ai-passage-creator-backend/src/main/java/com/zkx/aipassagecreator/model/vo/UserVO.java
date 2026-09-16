package com.zkx.aipassagecreator.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图对象（管理端用，含完整信息但不含密码）
 */
@Data
public class UserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户 ID（雪花 ID，序列化为字符串避免前端 JS 精度丢失） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private LocalDateTime editTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
