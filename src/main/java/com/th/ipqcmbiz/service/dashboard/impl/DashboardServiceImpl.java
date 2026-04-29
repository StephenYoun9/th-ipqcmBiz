package com.th.ipqcmbiz.service.dashboard.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.vo.output.DashboardStatsRespVO;
import com.th.ipqcmbiz.entity.po.ExceptionLogDO;
import com.th.ipqcmbiz.mapper.dashboard.DashboardMapper;
import com.th.ipqcmbiz.service.dashboard.DashboardService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Resource
    private DashboardMapper dashboardMapper;

    @Override
    public DashboardStatsRespVO getDashboardStats() {
        return DashboardStatsRespVO.builder()
                .employeeCount(dashboardMapper.countEmployee())
                .toolCount(dashboardMapper.countTool())
                .todayBorrowCount(dashboardMapper.countTodayBorrow())
                .pendingExceptionCount(dashboardMapper.countPendingException())
                .build();
    }

    @Override
    public PageInfo<ExceptionLogDO> getPendingAlertsPaged(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        PageInfo<ExceptionLogDO> pageInfo = new PageInfo<>(dashboardMapper.getPendingAlerts());
        return pageInfo;
    }
}