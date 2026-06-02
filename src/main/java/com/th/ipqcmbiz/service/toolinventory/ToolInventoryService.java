package com.th.ipqcmbiz.service.toolinventory;

import com.th.ipqcmbiz.entity.po.ToolCabinetInventoryDO;

import java.util.List;

/**
 * 工具柜库存服务接口
 */
public interface ToolInventoryService {

    /**
     * 获取工具柜库存列表
     */
    List<ToolCabinetInventoryDO> getCabinetInventory(String cabinetNo);

    /**
     * 初始化工具柜库存
     */
    void initCabinetInventory(String cabinetNo, List<String> toolCodes, List<String> positions);

    /**
     * 更新库存状态
     */
    void updateInventoryStatus(Long id, String status);

    /**
     * 保存借出前快照
     */
    void saveSnapshotBeforeBorrow(String cabinetNo, Long borrowRecordId);

    /**
     * 保存归还后快照
     */
    void saveSnapshotAfterReturn(String cabinetNo, Long borrowRecordId);

    /**
     * 对比快照，判断工具变化
     * @return 变化的工具列表
     */
    List<String> compareSnapshot(Long borrowRecordId);
}
