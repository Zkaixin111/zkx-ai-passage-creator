package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 确认标题请求
 */
@Data
public class ArticleConfirmTitleRequest implements Serializable {

    /** 任务 ID */
    private String taskId;

    /** 选中的主标题 */
    private String selectedMainTitle;

    /** 选中的副标题 */
    private String selectedSubTitle;

    /** 用户补充描述 */
    private String userDescription;

    private static final long serialVersionUID = 1L;
}
