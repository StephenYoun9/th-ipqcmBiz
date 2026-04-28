package com.th.ipqcmbiz.controller.dashboard;

import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.output.DashboardStatsRespVO;
import com.th.ipqcmbiz.entity.po.ExceptionLogDO;
import com.th.ipqcmbiz.service.dashboard.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping(value = "/dashboard", name = "数据概览")
public class DashboardController extends BaseController {
    @Resource
    private DashboardService dashboardService;

    @Operation(summary = "获取统计数据", description = "获取在册员工数、工具总数、今日借还次数，未处理异常日志")
    @PostMapping(value = "/stats")
    public Result<DashboardStatsRespVO> getStats() {
        return success(dashboardService.getDashboardStats());
    }

    @Operation(summary = "获取系统告警", description = "获取未处理的异常告警列表")
    @PostMapping(value = "/alerts")
    public Result<List<ExceptionLogDO>> getAlerts() {
        return success(dashboardService.getPendingAlerts());
    }
}