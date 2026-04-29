package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ToolMapper {

    List<ToolInfoDO> selectList(@Param("keyword") String keyword);

    List<ToolInfoDO> selectAvailableList(@Param("keyword") String keyword, @Param("toolType") String toolType, @Param("offset") int offset, @Param("limit") int limit);

    Long countAvailableList(@Param("keyword") String keyword, @Param("toolType") String toolType);

    ToolInfoDO selectByCode(@Param("toolCode") String toolCode);

    int insert(ToolInfoDO tool);

    int update(ToolInfoDO tool);

    int deleteByCode(@Param("toolCode") String toolCode);

    int updateStatus(@Param("toolCode") String toolCode, @Param("status") String status);
}