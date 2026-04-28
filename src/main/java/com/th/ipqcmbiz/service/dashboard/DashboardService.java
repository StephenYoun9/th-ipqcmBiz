package com.th.ipqcmbiz.service.dashboard;

import com.th.ipqcmbiz.entity.vo.output.DashboardStatsRespVO;
import com.th.ipqcmbiz.entity.po.ExceptionLogDO;
import java.util.List;

public interface DashboardService {
    DashboardStatsRespVO getDashboardStats();

    List<ExceptionLogDO> getPendingAlerts();
}