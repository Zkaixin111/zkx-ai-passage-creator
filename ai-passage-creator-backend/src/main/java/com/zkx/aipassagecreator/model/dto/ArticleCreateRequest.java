package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建文章任务请求
 */
@Data
public class ArticleCreateRequest implements Serializable {

    /** 选题 */
    private String topic;

    /** 文章风格（默认/科技/情感/教育/幽默） */
    private String style;

    /** 启用的配图方式（空表示全部） */
    private List<String> enabledImageMethods;

    private static final long serialVersionUID = 1L;
}
