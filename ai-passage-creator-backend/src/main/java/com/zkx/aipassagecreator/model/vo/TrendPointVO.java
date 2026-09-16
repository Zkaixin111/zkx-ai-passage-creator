package com.zkx.aipassagecreator.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 每日创作趋势点
 */
@Data
public class TrendPointVO implements Serializable {

    /** 日期（yyyy-MM-dd） */
    private String date;

    /** 当日创作数量 */
    private Long count;

    private static final long serialVersionUID = 1L;
}
