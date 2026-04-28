package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ToolMapper {

    List<ToolInfoDO> selectList(@Param("keyword") String keyword);

    ToolInfoDO selectByCode(@Param("toolCode") String toolCode);

    int insert(ToolInfoDO tool);

    int update(ToolInfoDO tool);

    int deleteByCode(@Param("toolCode") String toolCode);

    int updateStatus(@Param("toolCode") String toolCode, @Param("status") String status);
}