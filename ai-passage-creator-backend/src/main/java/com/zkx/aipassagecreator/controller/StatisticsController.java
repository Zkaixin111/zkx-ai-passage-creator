package com.zkx.aipassagecreator.controller;

import com.zkx.aipassagecreator.annotation.AuthCheck;
import com.zkx.aipassagecreator.common.BaseResponse;
import com.zkx.aipassagecreator.common.ResultUtils;
import com.zkx.aipassagecreator.model.vo.StatisticsVO;
import com.zkx.aipassagecreator.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统统计分析控制器（仅管理员）
 */
@Tag(name = "系统统计")
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @AuthCheck(mustRole = "admin")
    @Operation(summary = "获取系统统计总览（管理员）")
    @GetMapping("/overview")
    public BaseResponse<StatisticsVO> getOverview() {
        return ResultUtils.success(statisticsService.getOverview());
    }
}
