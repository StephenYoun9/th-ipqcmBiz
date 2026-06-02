package com.th.ipqcmbiz.mapper.borrow;

import com.github.pagehelper.Page;
import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.vo.output.BorrowRecordRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BorrowRecordMapper {

    BorrowRecordDO selectById(@Param("id") Long id);

    List<BorrowRecordDO> selectByUserId(@Param("userId") String userId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    Long countByUserId(@Param("userId") String userId);

    List<BorrowRecordDO> selectBorrowedByUserId(@Param("userId") String userId,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    Long countBorrowedByUserId(@Param("userId") String userId);

    Long countBorrowed();

    Integer countBorrowedByToolCode(@Param("toolCode") String toolCode);

    int insert(BorrowRecordDO record);

    int updateReturn(@Param("toolCode") String toolCode, @Param("returnOperatorId") String returnOperatorId);

    BorrowRecordDO selectBorrowedByToolCode(@Param("toolCode") String toolCode);

    Page<BorrowRecordRespVO> selectWithDetailByUserId(@Param("userId") String userId,
                                                       @Param("offset") int offset,
                                                       @Param("limit") int limit);

    List<BorrowRecordDO> selectForOperationLog(@Param("keyword") String keyword,
                                               @Param("startTime") java.util.Date startTime,
                                               @Param("endTime") java.util.Date endTime,
                                               @Param("operatorType") String operatorType,
                                               @Param("offset") Integer offset,
                                               @Param("limit") Integer limit);

    Long countForOperationLog(@Param("keyword") String keyword,
                              @Param("startTime") java.util.Date startTime,
                              @Param("endTime") java.util.Date endTime,
                              @Param("operatorType") String operatorType);
}