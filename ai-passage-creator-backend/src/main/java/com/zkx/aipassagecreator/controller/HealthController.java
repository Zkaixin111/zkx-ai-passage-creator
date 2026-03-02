package com.zkx.aipassagecreator.controller;

import com.zkx.aipassagecreator.common.BaseResponse;
import com.zkx.aipassagecreator.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 健康检查
 * <p>
 * 用于监控和负载均衡检测服务是否正常运行。
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
