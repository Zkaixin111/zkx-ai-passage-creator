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

/**
 * 文章实体
 * <p>
 * 对应 article 表（字段名为驼峰，无需下划线转换）。
 * JSON 字段（titleOptions/outline/images）在实体中存为字符串，由 Service 层用 Jackson 序列化/反序列化。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article", camelToUnderline = false)
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文章 ID，雪花算法生成 */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /** 任务 ID（UUID） */
    private String taskId;

    /** 用户 ID */
    private Long userId;

    /** 选题 */
    private String topic;

    /** 用户补充描述 */
    private String userDescription;

    /** 主标题 */
    private String mainTitle;

    /** 副标题 */
    private String subTitle;

    /** 标题方案列表（JSON 字符串） */
    private String titleOptions;

    /** 大纲（JSON 字符串） */
    private String outline;

    /** 正文（Markdown 格式） */
    private String content;

    /** 完整图文（Markdown 格式，含配图） */
    private String fullContent;

    /** 封面图 URL */
    private String coverImage;

    /** 配图列表（JSON 字符串） */
    private String images;

    /** 状态：PENDING/PROCESSING/COMPLETED/FAILED */
    private String status;

    /** 当前阶段 */
    private String phase;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}
