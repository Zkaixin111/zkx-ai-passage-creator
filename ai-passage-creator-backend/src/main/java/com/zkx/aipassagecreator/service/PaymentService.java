package com.zkx.aipassagecreator.service;

import com.zkx.aipassagecreator.model.vo.PayCreateResponse;

import java.util.Map;

/**
 * 支付服务
 * <p>
 * 统一封装微信/支付宝的下单、回调处理（幂等入账 + 开通 VIP）。
 */
public interface PaymentService {

    /**
     * 创建支付订单（幂等：同一用户 VIP 订单未支付时复用，已支付直接返回）
     *
     * @param userId  用户 ID
     * @param channel 支付渠道：WECHAT / ALIPAY
     * @return 订单与支付参数（微信 code_url / 支付宝表单）
     */
    PayCreateResponse createOrder(Long userId, String channel);

    /**
     * 处理微信支付回调（验签 + 解密 + 幂等入账 + 开通 VIP）
     *
     * @param body    回调原始 JSON
     * @param headers 回调请求头（验签用 Wechatpay-* 头）
     * @return 微信要求的应答 JSON（{"code":"SUCCESS","message":"成功"}）
     */
    String handleWechatNotify(String body, Map<String, String> headers);

    /**
     * 处理支付宝回调（验签 + 幂等入账 + 开通 VIP）
     *
     * @param params 回调表单参数
     * @return "success" / "failure"
     */
    String handleAlipayNotify(Map<String, String> params);
}
