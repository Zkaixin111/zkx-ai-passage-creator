package com.zkx.aipassagecreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建支付订单请求
 */
@Data
public class PayCreateRequest implements Serializable {

    /** 支付渠道：ALIPAY */
    private String channel;

    private static final long serialVersionUID = 1L;
}
