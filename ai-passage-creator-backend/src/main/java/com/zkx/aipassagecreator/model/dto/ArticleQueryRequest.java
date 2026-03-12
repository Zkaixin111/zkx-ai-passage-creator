package com.zkx.aipassagecreator.model.dto;

import com.zkx.aipassagecreator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文章分页查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleQueryRequest extends PageRequest implements Serializable {

    /** 用户 ID（筛选自己的文章） */
    private Long userId;

    /** 状态：PENDING/PROCESSING/COMPLETED/FAILED */
    private String status;

    private static final long serialVersionUID = 1L;
}
