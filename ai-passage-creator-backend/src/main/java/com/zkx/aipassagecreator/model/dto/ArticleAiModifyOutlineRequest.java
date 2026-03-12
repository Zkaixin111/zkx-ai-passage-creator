package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 修改大纲请求
 */
@Data
public class ArticleAiModifyOutlineRequest implements Serializable {

    /** 任务 ID */
    private String taskId;

    /** 修改建议 */
    private String modifySuggestion;

    private static final long serialVersionUID = 1L;
}
