package com.th.ipqcmbiz.controller.admin;

import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.input.OperationLogReqVO;
import com.th.ipqcmbiz.entity.vo.output.OperationLogPageRespVO;
import com.th.ipqcmbiz.service.operationlog.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 操作日志管理控制器
 */
@Tag(name = "管理员-日志审计")
@RestController
@RequestMapping("/admin/log")
public class OperationLogController extends BaseController {

    @Resource
    private OperationLogService operationLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/list")
    public Result<OperationLogPageRespVO> queryLogs(
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime,
            @RequestParam(required = false) String operatorType,
            @RequestParam(required = false) Boolean exceptionOnly,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        OperationLogReqVO reqVO = new OperationLogReqVO();
        reqVO.setOperatorKeyword(operatorKeyword);
        reqVO.setStartTime(startTime);
        reqVO.setEndTime(endTime);
        reqVO.setOperatorType(operatorType);
        reqVO.setExceptionOnly(exceptionOnly);
        reqVO.setPageNum(pageNum);
        reqVO.setPageSize(pageSize);

        return success(operationLogService.queryLogs(reqVO));
    }

    @Operation(summary = "导出操作日志")
    @GetMapping("/export")
    public void exportLogs(
            @RequestParam(required = false) String operatorKeyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endTime,
            @RequestParam(required = false) String operatorType,
            @RequestParam(required = false) Boolean exceptionOnly,
            HttpServletResponse response) throws IOException {

        OperationLogReqVO reqVO = new OperationLogReqVO();
        reqVO.setOperatorKeyword(operatorKeyword);
        reqVO.setStartTime(startTime);
        reqVO.setEndTime(endTime);
        reqVO.setOperatorType(operatorType);
        reqVO.setExceptionOnly(exceptionOnly);

        String csvContent = operationLogService.exportLogs(reqVO);

        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode("操作日志_" + System.currentTimeMillis() + ".csv", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

        PrintWriter writer = response.getWriter();
        writer.write(csvContent);
        writer.flush();
    }

    @Operation(summary = "获取未处理异常数量")
    @GetMapping("/exception/count")
    public Result<Long> getExceptionCount() {
        OperationLogReqVO reqVO = new OperationLogReqVO();
        reqVO.setExceptionOnly(true);
        OperationLogPageRespVO result = operationLogService.queryLogs(reqVO);
        return success(result.getExceptionCount());
    }
}