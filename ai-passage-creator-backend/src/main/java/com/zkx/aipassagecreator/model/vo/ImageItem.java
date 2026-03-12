package com.zkx.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 配图条目
 */
@Data
public class ImageItem implements Serializable {

    /** 插入位置（章节序号） */
    private Integer position;

    /** 图片 URL */
    private String url;

    /** 配图方式（PEXELS/PICSUM 等） */
    private String method;

    /** 英文搜索关键词 */
    private String keywords;

    /** 对应章节标题 */
    private String sectionTitle;

    /** 图片描述 */
    private String description;

    private static final long serialVersionUID = 1L;
}
