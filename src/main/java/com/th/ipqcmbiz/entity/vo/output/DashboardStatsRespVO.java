package com.th.ipqcmbiz.entity.vo.output;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsRespVO {
    private Long employeeCount;
    private Long toolCount;
    private Long todayBorrowCount;
    private Long pendingExceptionCount;
}