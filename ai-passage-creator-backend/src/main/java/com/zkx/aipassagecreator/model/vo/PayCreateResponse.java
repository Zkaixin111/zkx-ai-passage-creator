package com.zkx.aipassagecreator.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建支付订单响应
 * <p>
 * payUrl 为支付宝页面表单 HTML，前端渲染后自动跳转到支付宝完成支付。
 * status=PAID 表示该商品已购买过，无需再次支付。
 */
@Data
public class PayCreateResponse implements Serializable {

    /** 业务订单号（雪花 ID 场景下仍以字符串返回） */
    private String orderNo;

    /** 支付渠道 */
    private String channel;

    /** 商品类型 */
    private String productType;

    /** 商品标题 */
    private String subject;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 订单状态：PENDING/PAID */
    private String status;

    /** 支付参数：支付宝表单 HTML */
    private String payUrl;

    /** 支付类型提示：ALIPAY_FORM(表单) */
    private String payType;
}
