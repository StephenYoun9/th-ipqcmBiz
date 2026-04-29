package com.th.ipqcmbiz.mapper.exception;

import com.th.ipqcmbiz.entity.po.ExceptionLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ExceptionLogMapper {

    List<ExceptionLogDO> select(@Param("keyword") String keyword,
                                 @Param("startTime") Date startTime,
                                 @Param("endTime") Date endTime,
                                 @Param("operatorType") String operatorType,
                                 @Param("exceptionOnly") Boolean exceptionOnly,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    Long count(@Param("keyword") String keyword,
               @Param("startTime") Date startTime,
               @Param("endTime") Date endTime,
               @Param("operatorType") String operatorType,
               @Param("exceptionOnly") Boolean exceptionOnly);

    Long countPending(@Param("startTime") Date startTime,
                      @Param("endTime") Date endTime);

    Long countAllExceptions(@Param("startTime") Date startTime,
                             @Param("endTime") Date endTime);
}