package com.zkx.aipassagecreator.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 文章视图对象（与前端 API.ArticleVO 对齐）
 */
@Data
public class ArticleVO implements Serializable {

    /** 文章 ID（雪花 ID，序列化为字符串避免前端 JS 精度丢失） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 任务 ID */
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

    /** 标题方案列表 */
    private List<TitleOption> titleOptions;

    /** 大纲列表 */
    private List<OutlineItem> outline;

    /** 正文（Markdown） */
    private String content;

    /** 完整图文（Markdown，含配图） */
    private String fullContent;

    /** 封面图 URL */
    private String coverImage;

    /** 配图列表 */
    private List<ImageItem> images;

    /** 状态：PENDING/PROCESSING/COMPLETED/FAILED */
    private String status;

    /** 当前阶段 */
    private String phase;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private String createTime;

    /** 完成时间 */
    private String completedTime;

    private static final long serialVersionUID = 1L;
}
