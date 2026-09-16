package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 确认正文后是否生成配图请求
 * <p>
 * 正文生成完成后，用户选择是否生成配图；生成时需指定配图方式列表。
 */
@Data
public class ArticleConfirmContentRequest implements Serializable {

    /** 任务 ID */
    private String taskId;

    /** 是否生成配图 */
    private Boolean generateImages;

    /** 启用的配图方式（generateImages 为 true 时生效；空表示默认 Pexels） */
    private List<String> enabledImageMethods;

    private static final long serialVersionUID = 1L;
}
