package com.th.ipqcmbiz.mapper.dashboard;

import com.th.ipqcmbiz.entity.po.ExceptionLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardMapper {
    Long countEmployee();

    Long countTool();

    Long countTodayBorrow();

    Long countPendingException();

    List<ExceptionLogDO> getPendingAlerts();
}