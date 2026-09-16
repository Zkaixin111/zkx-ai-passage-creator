package com.zkx.aipassagecreator.controller;

import com.zkx.aipassagecreator.annotation.AuthCheck;
import com.zkx.aipassagecreator.common.BaseResponse;
import com.zkx.aipassagecreator.common.ResultUtils;
import com.zkx.aipassagecreator.exception.ErrorCode;
import com.zkx.aipassagecreator.exception.ThrowUtils;
import com.zkx.aipassagecreator.model.dto.PayCreateRequest;
import com.zkx.aipassagecreator.model.entity.User;
import com.zkx.aipassagecreator.model.vo.PayCreateResponse;
import com.zkx.aipassagecreator.service.PaymentService;
import com.zkx.aipassagecreator.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付模块控制器
 * <p>
 * 下单、支付宝回调（回调接口不校验登录，由渠道服务器调用）、订单查询。
 */
@Tag(name = "支付模块")
@RestController
@RequestMapping("/pay")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @Resource
    private UserService userService;

    @AuthCheck
    @Operation(summary = "创建支付订单（支付宝）")
    @PostMapping("/create")
    public BaseResponse<PayCreateResponse> createOrder(@RequestBody PayCreateRequest request,
                                                       HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        PayCreateResponse resp = paymentService.createOrder(loginUser.getId(), request.getChannel());
        return ResultUtils.success(resp);
    }

    @Operation(summary = "支付宝回调（支付宝服务器调用，勿加鉴权）")
    @PostMapping("/notify/alipay")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            params.put(name, request.getParameter(name));
        }
        return paymentService.handleAlipayNotify(params);
    }
}
