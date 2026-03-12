package com.zkx.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 大纲章节（请求/响应通用，结构与 OutlineItem 一致）
 */
@Data
public class OutlineSection implements Serializable {

    /** 章节序号 */
    private Integer section;

    /** 章节标题 */
    private String title;

    /** 章节要点 */
    private List<String> points;

    private static final long serialVersionUID = 1L;
}
