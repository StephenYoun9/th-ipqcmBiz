package com.th.ipqcmbiz.mapper;

import com.th.ipqcmbiz.entity.po.ToolCabinetSnapshotDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 工具柜快照Mapper
 */
@Mapper
public interface ToolCabinetSnapshotMapper {

    /**
     * 插入快照记录
     */
    int insert(ToolCabinetSnapshotDO snapshot);

    /**
     * 批量插入快照记录
     */
    int batchInsert(@Param("list") List<ToolCabinetSnapshotDO> snapshots);

    /**
     * 根据借还记录ID查询快照
     */
    List<ToolCabinetSnapshotDO> selectByBorrowRecordId(@Param("borrowRecordId") Long borrowRecordId);

    /**
     * 根据借还记录ID和快照类型查询
     */
    List<ToolCabinetSnapshotDO> selectByBorrowRecordIdAndType(
            @Param("borrowRecordId") Long borrowRecordId,
            @Param("snapshotType") String snapshotType);

    /**
     * 删除借还记录关联的快照
     */
    int deleteByBorrowRecordId(@Param("borrowRecordId") Long borrowRecordId);
}
