package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.po.ExceptionLogDO;
import com.th.ipqcmbiz.entity.vo.output.BorrowRecordRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface OperationLogMapper {

    /**
     * 查询借还记录（作为操作日志来源）
     */
    List<BorrowRecordDO> selectBorrowRecords(@Param("keyword") String keyword,
                                              @Param("startTime") Date startTime,
                                              @Param("endTime") Date endTime,
                                              @Param("operatorType") String operatorType,
                                              @Param("exceptionOnly") Boolean exceptionOnly,
                                              @Param("offset") Integer offset,
                                              @Param("limit") Integer limit);

    /**
     * 查询借还记录总数
     */
    Long countBorrowRecords(@Param("keyword") String keyword,
                            @Param("startTime") Date startTime,
                            @Param("endTime") Date endTime,
                            @Param("operatorType") String operatorType,
                            @Param("exceptionOnly") Boolean exceptionOnly);

    /**
     * 查询异常记录（作为操作日志来源）
     */
    List<ExceptionLogDO> selectExceptionLogs(@Param("keyword") String keyword,
                                            @Param("startTime") Date startTime,
                                            @Param("endTime") Date endTime,
                                            @Param("operatorType") String operatorType,
                                            @Param("exceptionOnly") Boolean exceptionOnly,
                                            @Param("offset") Integer offset,
                                            @Param("limit") Integer limit);

    /**
     * 查询异常记录总数
     */
    Long countExceptionLogs(@Param("keyword") String keyword,
                            @Param("startTime") Date startTime,
                            @Param("endTime") Date endTime,
                            @Param("operatorType") String operatorType,
                            @Param("exceptionOnly") Boolean exceptionOnly);

    /**
     * 统计异常记录数
     */
    Long countAllExceptions(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 根据工具编号查询工具名称
     */
    String selectToolNameByCode(@Param("toolCode") String toolCode);

    /**
     * 根据用户ID查询用户姓名
     */
    String selectUserNameById(@Param("userId") String userId);

    /**
     * 根据用户ID查询借还记录
     */
    List<BorrowRecordDO> selectBorrowRecordsByUserId(@Param("userId") String userId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    /**
     * 根据用户ID查询借还记录（带工具名称和用户姓名）
     */
    List<BorrowRecordRespVO> selectBorrowRecordsWithDetailByUserId(@Param("userId") String userId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);

    /**
     * 根据用户ID查询借还记录总数
     */
    Long countBorrowRecordsByUserId(@Param("userId") String userId);

    /**
     * 插入借记录
     */
    int insertBorrowRecord(@Param("toolCode") String toolCode, @Param("userId") String userId, @Param("operatorType") String operatorType);

    /**
     * 更新借还记录（还工具）
     */
    int updateBorrowRecordReturn(@Param("toolCode") String toolCode, @Param("userId") String userId);

    /**
     * 查询用户未归还的工具
     */
    List<BorrowRecordDO> selectBorrowedByUserId(@Param("userId") String userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询用户未归还的工具总数
     */
    Long countBorrowedByUserId(@Param("userId") String userId);
}