package com.th.ipqcmbiz.entity.vo.output;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "操作日志响应VO")
public class OperationLogRespVO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作人ID")
    private String operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "操作时间")
    private Date operateTime;

    @Schema(description = "操作类型: borrow-借工具, return-还工具, employee-员工管理, tool-工具管理")
    private String operatorType;

    @Schema(description = "操作类型名称")
    private String operatorTypeName;

    @Schema(description = "操作详情")
    private String detail;

    @Schema(description = "是否有异常")
    private Boolean hasException;

    @Schema(description = "异常描述")
    private String exceptionDesc;

    @Schema(description = "关联工具编号")
    private String toolCode;

    @Schema(description = "关联工具名称")
    private String toolName;

    @Schema(description = "关联员工编号")
    private String userId;
}