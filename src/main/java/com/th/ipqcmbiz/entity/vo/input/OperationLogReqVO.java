package com.th.ipqcmbiz.entity.vo.input;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@Schema(description = "操作日志查询请求VO")
public class OperationLogReqVO {

    @Schema(description = "操作人ID或姓名")
    private String operatorKeyword;

    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endTime;

    @Schema(description = "操作类型: borrow-借工具, return-还工具, employee-员工管理, tool-工具管理")
    private String operatorType;

    @Schema(description = "仅看异常")
    private Boolean exceptionOnly;

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}