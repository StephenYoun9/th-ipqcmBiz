package com.th.ipqcmbiz.service.operationlog;

import com.th.ipqcmbiz.entity.vo.input.OperationLogReqVO;
import com.th.ipqcmbiz.entity.vo.output.OperationLogPageRespVO;

/**
 * 操作日志查询服务接口
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志
     * @param reqVO 查询条件
     * @return 分页结果
     */
    OperationLogPageRespVO queryLogs(OperationLogReqVO reqVO);

    /**
     * 导出操作日志（返回CSV格式的字符串）
     * @param reqVO 查询条件
     * @return CSV格式的日志数据
     */
    String exportLogs(OperationLogReqVO reqVO);
}