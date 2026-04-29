package com.th.ipqcmbiz.entity.vo.output;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "操作日志分页响应VO")
public class OperationLogPageRespVO {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Integer totalPages;

    @Schema(description = "当前页码")
    private Integer pageNum;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "日志列表")
    private List<OperationLogRespVO> list;

    @Schema(description = "异常记录数")
    private Long exceptionCount;
}