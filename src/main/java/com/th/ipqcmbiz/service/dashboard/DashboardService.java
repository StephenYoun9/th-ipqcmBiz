package com.th.ipqcmbiz.service.dashboard;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.vo.output.DashboardStatsRespVO;
import com.th.ipqcmbiz.entity.po.ExceptionLogDO;

public interface DashboardService {
    DashboardStatsRespVO getDashboardStats();

    PageInfo<ExceptionLogDO> getPendingAlertsPaged(int pageNum, int pageSize);
}