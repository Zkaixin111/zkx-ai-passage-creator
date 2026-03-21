package com.zkx.aipassagecreator.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录用户视图对象（脱敏）
 * <p>
 * 返回给客户端，不包含密码等敏感字段。
 */
@Data
public class LoginUserVO implements Serializable {
    /** 用户 ID（雪花 ID，序列化为字符串避免前端 JS 精度丢失） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    /** 剩余配额（MVP 阶段返回大值放行，前端 hasQuota 恒为 true） */
    private Long quota;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
