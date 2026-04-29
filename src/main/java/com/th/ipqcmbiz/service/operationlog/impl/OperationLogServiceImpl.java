package com.th.ipqcmbiz.service.operationlog.impl;

import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.po.ExceptionLogDO;
import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import com.th.ipqcmbiz.entity.po.UserInfoDO;
import com.th.ipqcmbiz.entity.vo.input.OperationLogReqVO;
import com.th.ipqcmbiz.entity.vo.output.OperationLogPageRespVO;
import com.th.ipqcmbiz.entity.vo.output.OperationLogRespVO;
import com.th.ipqcmbiz.mapper.ToolMapper;
import com.th.ipqcmbiz.mapper.UserInfoMapper;
import com.th.ipqcmbiz.mapper.borrow.BorrowRecordMapper;
import com.th.ipqcmbiz.mapper.exception.ExceptionLogMapper;
import com.th.ipqcmbiz.service.operationlog.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 操作日志查询服务实现类
 */
@Service
@Slf4j
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private ToolMapper toolMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private ExceptionLogMapper exceptionLogMapper;

    /**
     * 操作类型映射
     */
    private static final String TYPE_BORROW = "borrow";
    private static final String TYPE_RETURN = "return";
    private static final String TYPE_EMPLOYEE = "employee";
    private static final String TYPE_TOOL = "tool";

    /**
     * 操作类型名称映射
     */
    private static final String TYPE_NAME_BORROW = "借工具";
    private static final String TYPE_NAME_RETURN = "还工具";
    private static final String TYPE_NAME_EMPLOYEE = "员工管理";
    private static final String TYPE_NAME_TOOL = "工具管理";

    @Override
    public OperationLogPageRespVO queryLogs(OperationLogReqVO reqVO) {
        // 设置默认值
        if (reqVO.getPageNum() == null || reqVO.getPageNum() < 1) {
            reqVO.setPageNum(1);
        }
        if (reqVO.getPageSize() == null || reqVO.getPageSize() < 1) {
            reqVO.setPageSize(10);
        }

        // 计算偏移量
        int offset = (reqVO.getPageNum() - 1) * reqVO.getPageSize();

        List<OperationLogRespVO> resultList = new ArrayList<>();
        Long totalCount = 0L;

        // 如果仅看异常，查询异常表
        if (Boolean.TRUE.equals(reqVO.getExceptionOnly())) {
            List<ExceptionLogDO> exceptionLogs = exceptionLogMapper.select(
                    reqVO.getOperatorKeyword(),
                    reqVO.getStartTime(),
                    adjustEndTime(reqVO.getEndTime()),
                    reqVO.getOperatorType(),
                    true,
                    offset,
                    reqVO.getPageSize()
            );
            totalCount = exceptionLogMapper.count(
                    reqVO.getOperatorKeyword(),
                    reqVO.getStartTime(),
                    adjustEndTime(reqVO.getEndTime()),
                    reqVO.getOperatorType(),
                    true
            );

            for (ExceptionLogDO exceptionLog : exceptionLogs) {
                resultList.add(convertExceptionToResp(exceptionLog));
            }
        } else if (reqVO.getOperatorType() != null && !reqVO.getOperatorType().isEmpty()) {
            // 如果指定了操作类型，按类型查询对应表
            if (TYPE_BORROW.equals(reqVO.getOperatorType()) || TYPE_RETURN.equals(reqVO.getOperatorType())) {
                // 借还记录
                List<BorrowRecordDO> borrowRecords = borrowRecordMapper.selectForOperationLog(
                        reqVO.getOperatorKeyword(),
                        reqVO.getStartTime(),
                        adjustEndTime(reqVO.getEndTime()),
                        reqVO.getOperatorType(),
                        offset,
                        reqVO.getPageSize()
                );
                totalCount = borrowRecordMapper.countForOperationLog(
                        reqVO.getOperatorKeyword(),
                        reqVO.getStartTime(),
                        adjustEndTime(reqVO.getEndTime()),
                        reqVO.getOperatorType()
                );

                for (BorrowRecordDO record : borrowRecords) {
                    resultList.add(convertBorrowToResp(record));
                }
            } else {
                // 员工管理或工具管理类型，查询异常表
                List<ExceptionLogDO> exceptionLogs = exceptionLogMapper.select(
                        reqVO.getOperatorKeyword(),
                        reqVO.getStartTime(),
                        adjustEndTime(reqVO.getEndTime()),
                        reqVO.getOperatorType(),
                        false,
                        offset,
                        reqVO.getPageSize()
                );
                totalCount = exceptionLogMapper.count(
                        reqVO.getOperatorKeyword(),
                        reqVO.getStartTime(),
                        adjustEndTime(reqVO.getEndTime()),
                        reqVO.getOperatorType(),
                        false
                );

                for (ExceptionLogDO exceptionLog : exceptionLogs) {
                    resultList.add(convertExceptionToResp(exceptionLog));
                }
            }
        } else {
            // 查询所有日志（合并借还记录和异常记录）

            // 查询借还记录
            List<BorrowRecordDO> borrowRecords = borrowRecordMapper.selectForOperationLog(
                    reqVO.getOperatorKeyword(),
                    reqVO.getStartTime(),
                    adjustEndTime(reqVO.getEndTime()),
                    null,
                    0,
                    1000
            );

            // 查询异常记录
            List<ExceptionLogDO> exceptionLogs = exceptionLogMapper.select(
                    reqVO.getOperatorKeyword(),
                    reqVO.getStartTime(),
                    adjustEndTime(reqVO.getEndTime()),
                    null,
                    false,
                    0,
                    1000
            );

            // 合并并转换
            List<OperationLogRespVO> mergedList = new ArrayList<>();
            for (BorrowRecordDO record : borrowRecords) {
                mergedList.add(convertBorrowToResp(record));
            }
            for (ExceptionLogDO exceptionLog : exceptionLogs) {
                mergedList.add(convertExceptionToResp(exceptionLog));
            }

            // 按时间倒序排序
            mergedList.sort((a, b) -> {
                if (a.getOperateTime() == null && b.getOperateTime() == null) return 0;
                if (a.getOperateTime() == null) return 1;
                if (b.getOperateTime() == null) return -1;
                return b.getOperateTime().compareTo(a.getOperateTime());
            });

            // 分页
            totalCount = (long) mergedList.size();
            int fromIndex = offset;
            int toIndex = Math.min(offset + reqVO.getPageSize(), mergedList.size());
            if (fromIndex < mergedList.size()) {
                resultList = mergedList.subList(fromIndex, toIndex);
            }
        }

        // 统计异常数
        Long exceptionCount = exceptionLogMapper.countAllExceptions(reqVO.getStartTime(), adjustEndTime(reqVO.getEndTime()));

        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalCount / reqVO.getPageSize());

        return OperationLogPageRespVO.builder()
                .total(totalCount)
                .totalPages(totalPages)
                .pageNum(reqVO.getPageNum())
                .pageSize(reqVO.getPageSize())
                .list(resultList)
                .exceptionCount(exceptionCount)
                .build();
    }

    @Override
    public String exportLogs(OperationLogReqVO reqVO) {
        // 导出时不分页，设置较大的pageSize
        reqVO.setPageSize(10000);
        reqVO.setPageNum(1);

        OperationLogPageRespVO pageResult = queryLogs(reqVO);
        List<OperationLogRespVO> logs = pageResult.getList();

        // 构建CSV内容
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM for UTF-8
        csv.append("操作人,工号,操作时间,操作类型,操作详情,是否异常\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (OperationLogRespVO log : logs) {
            csv.append(escapeCsv(log.getOperatorName())).append(",");
            csv.append(escapeCsv(log.getOperatorId())).append(",");
            csv.append(log.getOperateTime() != null ? escapeCsv(sdf.format(log.getOperateTime())) : "").append(",");
            csv.append(escapeCsv(log.getOperatorTypeName())).append(",");
            csv.append(escapeCsv(log.getDetail())).append(",");
            csv.append(log.getHasException() != null && log.getHasException() ? "异常" : "正常").append("\n");
        }

        return csv.toString();
    }

    /**
     * 将借还记录转换为响应VO
     */
    private OperationLogRespVO convertBorrowToResp(BorrowRecordDO record) {
        UserInfoDO user = userInfoMapper.selectByUserId(record.getUserId());
        ToolInfoDO tool = toolMapper.selectByCode(record.getToolCode());
        String operatorName = user != null ? user.getUserName() : null;
        String toolName = tool != null ? tool.getToolName() : null;

        String operatorTypeName = TYPE_NAME_BORROW;
        String detail = "";
        boolean hasException = false;

        if (TYPE_BORROW.equals(record.getOperatorType())) {
            operatorTypeName = TYPE_NAME_BORROW;
            detail = "借取" + (toolName != null ? toolName : record.getToolCode()) + "，" + record.getToolCode() + "柜门开启";
        } else if (TYPE_RETURN.equals(record.getOperatorType())) {
            operatorTypeName = TYPE_NAME_RETURN;
            detail = "归还" + (toolName != null ? toolName : record.getToolCode()) + "，" + record.getToolCode();
        }

        return OperationLogRespVO.builder()
                .id(record.getId())
                .operatorId(record.getUserId())
                .operatorName(operatorName != null ? operatorName + "（" + record.getUserId() + "）" : record.getUserId())
                .operateTime(TYPE_BORROW.equals(record.getOperatorType()) ? record.getBorrowTime() : record.getReturnTime())
                .operatorType(record.getOperatorType())
                .operatorTypeName(operatorTypeName)
                .detail(detail)
                .hasException(hasException)
                .toolCode(record.getToolCode())
                .toolName(toolName)
                .userId(record.getUserId())
                .build();
    }

    /**
     * 将异常记录转换为响应VO
     */
    private OperationLogRespVO convertExceptionToResp(ExceptionLogDO exceptionLog) {
        UserInfoDO user = userInfoMapper.selectByUserId(exceptionLog.getUserId());
        ToolInfoDO tool = toolMapper.selectByCode(exceptionLog.getToolCode());
        String operatorName = user != null ? user.getUserName() : null;
        String toolName = tool != null ? tool.getToolName() : null;

        String operatorTypeName = getOperatorTypeName(exceptionLog.getExceptionType());

        return OperationLogRespVO.builder()
                .id(exceptionLog.getId())
                .operatorId(exceptionLog.getUserId())
                .operatorName(operatorName != null ? operatorName + "（" + exceptionLog.getUserId() + "）" : exceptionLog.getUserId())
                .operateTime(exceptionLog.getCreateTime())
                .operatorType(exceptionLog.getExceptionType())
                .operatorTypeName(operatorTypeName)
                .detail(exceptionLog.getDescription())
                .hasException(true)
                .exceptionDesc(exceptionLog.getDescription())
                .toolCode(exceptionLog.getToolCode())
                .toolName(toolName)
                .userId(exceptionLog.getUserId())
                .build();
    }

    /**
     * 获取操作类型名称
     */
    private String getOperatorTypeName(String exceptionType) {
        if (exceptionType == null) return "未知";
        switch (exceptionType) {
            case TYPE_BORROW:
                return TYPE_NAME_BORROW;
            case TYPE_RETURN:
                return TYPE_NAME_RETURN;
            case TYPE_EMPLOYEE:
                return TYPE_NAME_EMPLOYEE;
            case TYPE_TOOL:
                return TYPE_NAME_TOOL;
            default:
                return exceptionType;
        }
    }

    /**
     * 调整结束时间（设置为当天23:59:59）
     */
    private java.util.Date adjustEndTime(java.util.Date endTime) {
        if (endTime == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(endTime);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTime();
    }

    /**
     * CSV内容转义
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}