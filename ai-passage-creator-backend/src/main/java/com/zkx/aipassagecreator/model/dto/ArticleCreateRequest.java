package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建文章任务请求 DTO
 * <p>
 * 前端 ArticleCreatePage.vue 的 startCreate 方法构造此对象，
 * POST 到 /article/create 接口。
 */
@Data
public class ArticleCreateRequest implements Serializable {

    /** 选题，必填，如"AI 对教育的影响" */
    private String topic;

    /** 文章风格，可选，如"科技风格"/"武侠"/"科普"，直接填入 prompt 的 {style} 占位符 */
    private String style;

    /** 目标字数，可选，空或 <=0 时 Service 层用默认值 1200 字 */
    private Integer wordCount;

    /** 启用的配图方式，可选，空表示全部启用，如 ["pexels","ai_image","mermaid"] */
    private List<String> enabledImageMethods;

    private static final long serialVersionUID = 1L;
}
