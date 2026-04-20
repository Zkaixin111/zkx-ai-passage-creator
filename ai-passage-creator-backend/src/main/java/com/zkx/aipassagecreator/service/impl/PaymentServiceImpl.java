package com.zkx.aipassagecreator.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.mybatisflex.core.query.QueryWrapper;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.zkx.aipassagecreator.config.PaymentProperties;
import com.zkx.aipassagecreator.constant.UserConstant;
import com.zkx.aipassagecreator.exception.BusinessException;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.mapper.PaymentRecordMapper;
import com.zkx.aipassagecreator.mapper.UserMapper;
import com.zkx.aipassagecreator.model.entity.PaymentRecord;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.enums.PaymentChannelEnum;
import com.zkx.aipassagecreator.model.enums.PaymentStatusEnum;
import com.zkx.aipassagecreator.model.enums.ProductTypeEnum;
import com.zkx.aipassagecreator.model.vo.PayCreateResponse;
import com.zkx.aipassagecreator.service.PaymentService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现（微信 Native + 支付宝）
 * <p>
 * 下单幂等：同一用户 VIP 商品存在未支付订单时复用；回调幂等：uk_channelOrderNo 唯一 + 状态机
 * PENDING→PAID 单向流转，已入账的重复回调直接返回成功。
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentRecordMapper paymentRecordMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PaymentProperties paymentProperties;

    @Resource
    private TransactionTemplate transactionTemplate;

    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public PayCreateResponse createOrder(Long userId, String channel) {
        PaymentChannelEnum channelEnum = PaymentChannelEnum.getEnumByValue(channel);
        if (channelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的支付渠道");
        }
        // 幂等：查询该用户 VIP 商品最新订单
        QueryWrapper qw = new QueryWrapper();
        qw.eq("userId", userId);
        qw.eq("productType", ProductTypeEnum.VIP_PERMANENT.getValue());
        qw.orderBy("createTime", false);
        PaymentRecord exist = paymentRecordMapper.selectOneByQuery(qw);

        PaymentRecord order;
        if (exist != null && PaymentStatusEnum.PAID.getValue().equals(exist.getStatus())) {
            // 已购买过：直接返回已支付，前端提示已开通
            return buildResponse(exist, null, null);
        }
        if (exist != null && PaymentStatusEnum.PENDING.getValue().equals(exist.getStatus())) {
            // 存在未支付订单：复用，重新生成支付参数
            order = exist;
        } else {
            // 新建订单（orderNo 唯一保证下单幂等）
            order = PaymentRecord.builder()
                    .orderNo(generateOrderNo())
                    .userId(userId)
                    .channel(channelEnum.getValue())
                    .productType(ProductTypeEnum.VIP_PERMANENT.getValue())
                    .subject("VIP 永久会员")
                    .amount(paymentProperties.getVipPrice())
                    .currency("CNY")
                    .status(PaymentStatusEnum.PENDING.getValue())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .isDelete(0)
                    .build();
            paymentRecordMapper.insert(order);
        }

        // 按渠道生成支付参数
        if (channelEnum == PaymentChannelEnum.WECHAT) {
            String codeUrl = wechatPrepay(order);
            if (StrUtil.isBlank(codeUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信支付下单失败");
            }
            return buildResponse(order, codeUrl, "WECHAT_NATIVE");
        }
        String form = alipayPagePay(order);
        if (StrUtil.isBlank(form)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "支付宝下单失败");
        }
        return buildResponse(order, form, "ALIPAY_FORM");
    }

    @Override
    public String handleWechatNotify(String body, Map<String, String> headers) {
        try {
            RSAAutoCertificateConfig config = wechatConfig();
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(headers.get("Wechatpay-Serial"))
                    .timestamp(headers.get("Wechatpay-Timestamp"))
                    .nonce(headers.get("Wechatpay-Nonce"))
                    .signature(headers.get("Wechatpay-Signature"))
                    .signType("WECHATPAY2-SHA256-RSA2048")
                    .body(body)
                    .build();
            NotificationParser parser = new NotificationParser(config);
            Transaction transaction = parser.parse(requestParam, Transaction.class);

            Integer totalFen = transaction.getAmount() == null ? null : transaction.getAmount().getTotal();
            boolean ok = settleOrder(transaction.getOutTradeNo(),
                    transaction.getTransactionId(),
                    totalFen == null ? null : BigDecimal.valueOf(totalFen, 2),
                    body);
            return ok ? "{\"code\":\"SUCCESS\",\"message\":\"成功\"}" : "{\"code\":\"FAIL\",\"message\":\"处理失败\"}";
        } catch (Exception e) {
            log.error("微信支付回调处理失败: {}", e.getMessage(), e);
            return "{\"code\":\"FAIL\",\"message\":\"处理失败\"}";
        }
    }

    @Override
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            PaymentProperties.Alipay alipay = paymentProperties.getAlipay();
            // 验签（验签失败抛异常）
            boolean signOk = AlipaySignature.rsaCheckV1(params, alipay.getAlipayPublicKey(), "UTF-8", "RSA2");
            if (!signOk) {
                log.warn("支付宝回调验签失败: {}", params.get("out_trade_no"));
                return "failure";
            }
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                return "success"; // 非最终态不处理，返回 success 避免重复推送
            }
            boolean ok = settleOrder(params.get("out_trade_no"),
                    params.get("trade_no"),
                    StrUtil.isBlank(params.get("total_amount")) ? null : new BigDecimal(params.get("total_amount")),
                    params.toString());
            return ok ? "success" : "failure";
        } catch (Exception e) {
            log.error("支付宝回调处理失败: {}", e.getMessage(), e);
            return "failure";
        }
    }

    // ==================== 私有辅助 ====================

    /**
     * 幂等入账 + 开通 VIP（事务内执行）
     */
    public boolean settleOrder(String orderNo, String channelOrderNo, BigDecimal amount, String notifyData) {
        if (StrUtil.isBlank(orderNo)) {
            return false;
        }
        Boolean ok = transactionTemplate.execute(status -> {
            QueryWrapper qw = new QueryWrapper();
            qw.eq("orderNo", orderNo);
            PaymentRecord order = paymentRecordMapper.selectOneByQuery(qw);
            if (order == null) {
                log.warn("回调订单不存在: orderNo={}", orderNo);
                return false;
            }
            // 幂等：已支付直接返回成功（重复回调/并发回调只入账一次）
            if (PaymentStatusEnum.PAID.getValue().equals(order.getStatus())) {
                return true;
            }
            if (!PaymentStatusEnum.PENDING.getValue().equals(order.getStatus())) {
                log.warn("订单状态不可入账: orderNo={}, status={}", orderNo, order.getStatus());
                return false;
            }
            // 金额校验（防御性：回调金额须与订单一致）
            if (amount != null && amount.compareTo(order.getAmount()) != 0) {
                log.warn("回调金额与订单不一致: orderNo={}, expect={}, actual={}", orderNo, order.getAmount(), amount);
                return false;
            }
            // 入账
            order.setStatus(PaymentStatusEnum.PAID.getValue());
            order.setChannelOrderNo(channelOrderNo);
            order.setPayTime(LocalDateTime.now());
            order.setNotifyData(notifyData);
            order.setUpdateTime(LocalDateTime.now());
            paymentRecordMapper.update(order);

            // 开通 VIP（永久）
            User user = userMapper.selectOneById(order.getUserId());
            if (user == null) {
                log.warn("订单用户不存在: userId={}", order.getUserId());
                return false;
            }
            user.setVipType(UserConstant.VIP_TYPE_PERMANENT);
            user.setVipStartTime(LocalDateTime.now());
            user.setVipExpireTime(null); // 永久会员长期有效
            user.setUpdateTime(LocalDateTime.now());
            userMapper.update(user);
            log.info("VIP 开通成功: userId={}, orderNo={}", user.getId(), orderNo);
            return true;
        });
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 微信 Native 下单，返回 code_url（二维码内容）
     */
    private String wechatPrepay(PaymentRecord order) {
        try {
            RSAAutoCertificateConfig config = wechatConfig();
            NativePayService service = new NativePayService.Builder().config(config).build();

            PrepayRequest req = new PrepayRequest();
            req.setAppid(paymentProperties.getWechat().getAppid());
            req.setMchid(paymentProperties.getWechat().getMchid());
            req.setDescription(order.getSubject());
            req.setOutTradeNo(order.getOrderNo());
            req.setNotifyUrl(buildNotifyUrl("wechat"));
            Amount amount = new Amount();
            amount.setTotal(yuanToFen(order.getAmount()));
            amount.setCurrency("CNY");
            req.setAmount(amount);

            return service.prepay(req).getCodeUrl();
        } catch (Exception e) {
            log.error("微信 Native 下单失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 支付宝电脑网站支付，返回页面表单 HTML
     */
    private String alipayPagePay(PaymentRecord order) {
        try {
            PaymentProperties.Alipay alipay = paymentProperties.getAlipay();
            AlipayClient client = new DefaultAlipayClient(
                    alipay.getGateway(), alipay.getAppId(), alipay.getPrivateKey(),
                    "json", "UTF-8", alipay.getAlipayPublicKey(), "RSA2");
            AlipayTradePagePayRequest req = new AlipayTradePagePayRequest();
            req.setNotifyUrl(buildNotifyUrl("alipay"));
            req.setBizContent("{\"out_trade_no\":\"" + order.getOrderNo()
                    + "\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\",\"total_amount\":\"" + order.getAmount()
                    + "\",\"subject\":\"" + order.getSubject() + "\"}");
            return client.pageExecute(req).getBody();
        } catch (Exception e) {
            log.error("支付宝下单失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private RSAAutoCertificateConfig wechatConfig() throws Exception {
        PaymentProperties.Wechat wechat = paymentProperties.getWechat();
        if (StrUtil.isBlank(wechat.getAppid()) || StrUtil.isBlank(wechat.getMchid())
                || StrUtil.isBlank(wechat.getApiV3Key()) || StrUtil.isBlank(wechat.getMerchantSerialNo())
                || StrUtil.isBlank(wechat.getPrivateKeyPath())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "微信支付未配置，请先在 application.yaml 填写 pay.wechat 商户参数");
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(wechat.getMchid())
                .privateKeyFromPath(wechat.getPrivateKeyPath())
                .merchantSerialNumber(wechat.getMerchantSerialNo())
                .apiV3Key(wechat.getApiV3Key())
                .build();
    }

    private String buildNotifyUrl(String channel) {
        return StrUtil.removeSuffix(paymentProperties.getNotifyBaseUrl(), "/")
                + "/pay/notify/" + channel;
    }

    private String generateOrderNo() {
        return "VIP" + LocalDateTime.now().format(ORDER_TIME_FORMATTER)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
    }

    private PayCreateResponse buildResponse(PaymentRecord order, String payUrl, String payType) {
        PayCreateResponse resp = new PayCreateResponse();
        resp.setOrderNo(order.getOrderNo());
        resp.setChannel(order.getChannel());
        resp.setProductType(order.getProductType());
        resp.setSubject(order.getSubject());
        resp.setAmount(order.getAmount());
        resp.setStatus(order.getStatus());
        resp.setPayUrl(payUrl);
        resp.setPayType(payType);
        return resp;
    }

    /** 元 -> 分（微信金额单位，int） */
    private int yuanToFen(BigDecimal yuan) {
        return yuan == null ? 0 : yuan.multiply(BigDecimal.valueOf(100)).intValue();
    }
}
