package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.ToolCabinetInventoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工具柜库存Mapper
 */
@Mapper
public interface ToolCabinetInventoryMapper {

    /**
     * 根据柜号查询库存列表
     */
    List<ToolCabinetInventoryDO> selectByCabinetNo(@Param("cabinetNo") String cabinetNo);

    /**
     * 根据柜号和状态查询库存
     */
    List<ToolCabinetInventoryDO> selectByCabinetNoAndStatus(
            @Param("cabinetNo") String cabinetNo,
            @Param("status") String status);

    /**
     * 根据工具编号查询库存
     */
    ToolCabinetInventoryDO selectByToolCode(@Param("toolCode") String toolCode);

    /**
     * 根据柜号和位置查询库存
     */
    ToolCabinetInventoryDO selectByCabinetNoAndPosition(
            @Param("cabinetNo") String cabinetNo,
            @Param("position") String position);

    /**
     * 插入库存记录
     */
    int insert(ToolCabinetInventoryDO inventory);

    /**
     * 批量插入库存记录
     */
    int batchInsert(@Param("list") List<ToolCabinetInventoryDO> inventories);

    /**
     * 更新库存状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 根据工具编号更新库存状态
     */
    int updateStatusByToolCode(@Param("toolCode") String toolCode, @Param("status") String status);

    /**
     * 根据柜号删除所有库存记录
     */
    int deleteByCabinetNo(@Param("cabinetNo") String cabinetNo);
}