package com.zkx.aipassagecreator.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "user", camelToUnderline = false)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID，使用雪花算法生成 */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 登录账号 */
    private String userAccount;

    /** 加密后的密码 */
    private String userPassword;

    /** 用户昵称 */
    private String userName;

    /** 头像 URL */
    private String userAvatar;

    /** 个人简介 */
    private String userProfile;

    /** 角色：user / admin */
    private String userRole;

    /** 编辑时间 */
    private LocalDateTime editTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}
