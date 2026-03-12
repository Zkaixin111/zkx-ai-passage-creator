package com.zkx.aipassagecreator.model.dto;

import com.zkx.aipassagecreator.model.vo.OutlineSection;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 确认大纲请求
 */
@Data
public class ArticleConfirmOutlineRequest implements Serializable {

    /** 任务 ID */
    private String taskId;

    /** 确认后的大纲 */
    private List<OutlineSection> outline;

    private static final long serialVersionUID = 1L;
}
