package com.th.ipqcmbiz.service.toolinventory.impl;

import com.th.ipqcmbiz.entity.po.ToolCabinetInventoryDO;
import com.th.ipqcmbiz.entity.po.ToolCabinetSnapshotDO;
import com.th.ipqcmbiz.mapper.ToolCabinetInventoryMapper;
import com.th.ipqcmbiz.mapper.ToolCabinetSnapshotMapper;
import com.th.ipqcmbiz.service.toolinventory.ToolInventoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工具柜库存服务实现
 */
@Service
@Slf4j
public class ToolInventoryServiceImpl implements ToolInventoryService {

    @Resource
    private ToolCabinetInventoryMapper inventoryMapper;

    @Resource
    private ToolCabinetSnapshotMapper snapshotMapper;

    private static final String STATUS_IN_CABINET = "in_cabinet";
    private static final String STATUS_EMPTY = "empty";
    private static final String SNAPSHOT_BEFORE = "before";
    private static final String SNAPSHOT_AFTER = "after";

    @Override
    public List<ToolCabinetInventoryDO> getCabinetInventory(String cabinetNo) {
        return inventoryMapper.selectByCabinetNo(cabinetNo);
    }

    @Override
    @Transactional
    public void initCabinetInventory(String cabinetNo, List<String> toolCodes, List<String> positions) {
        // 删除现有库存
        inventoryMapper.deleteByCabinetNo(cabinetNo);

        // 批量插入新库存
        List<ToolCabinetInventoryDO> inventories = new ArrayList<>();
        for (int i = 0; i < toolCodes.size(); i++) {
            ToolCabinetInventoryDO inv = new ToolCabinetInventoryDO();
            inv.setCabinetNo(cabinetNo);
            inv.setToolCode(toolCodes.get(i));
            inv.setPosition(positions.get(i));
            inv.setStatus(STATUS_IN_CABINET);
            inventories.add(inv);
        }

        if (!inventories.isEmpty()) {
            inventoryMapper.batchInsert(inventories);
        }

        log.info("工具柜 {} 库存初始化完成，共 {} 个位置", cabinetNo, inventories.size());
    }

    @Override
    @Transactional
    public void updateInventoryStatus(Long id, String status) {
        inventoryMapper.updateStatus(id, status);
    }

    @Override
    @Transactional
    public void saveSnapshotBeforeBorrow(String cabinetNo, Long borrowRecordId) {
        saveSnapshot(cabinetNo, SNAPSHOT_BEFORE, borrowRecordId);
    }

    @Override
    @Transactional
    public void saveSnapshotAfterReturn(String cabinetNo, Long borrowRecordId) {
        saveSnapshot(cabinetNo, SNAPSHOT_AFTER, borrowRecordId);
    }

    private void saveSnapshot(String cabinetNo, String snapshotType, Long borrowRecordId) {
        List<ToolCabinetInventoryDO> inventories = inventoryMapper.selectByCabinetNoAndStatus(cabinetNo, STATUS_IN_CABINET);

        List<ToolCabinetSnapshotDO> snapshots = inventories.stream()
                .map(inv -> {
                    ToolCabinetSnapshotDO snapshot = new ToolCabinetSnapshotDO();
                    snapshot.setCabinetNo(cabinetNo);
                    snapshot.setToolCode(inv.getToolCode());
                    snapshot.setPosition(inv.getPosition());
                    snapshot.setSnapshotType(snapshotType);
                    snapshot.setBorrowRecordId(borrowRecordId);
                    return snapshot;
                })
                .collect(Collectors.toList());

        if (!snapshots.isEmpty()) {
            snapshotMapper.batchInsert(snapshots);
        }

        log.info("保存快照: cabinetNo={}, type={}, count={}", cabinetNo, snapshotType, snapshots.size());
    }

    @Override
    public List<String> compareSnapshot(Long borrowRecordId) {
        // 获取借出前快照
        List<ToolCabinetSnapshotDO> beforeSnapshots = snapshotMapper.selectByBorrowRecordIdAndType(
                borrowRecordId, SNAPSHOT_BEFORE);

        // 获取归还后快照
        List<ToolCabinetSnapshotDO> afterSnapshots = snapshotMapper.selectByBorrowRecordIdAndType(
                borrowRecordId, SNAPSHOT_AFTER);

        // 提取工具编号集合
        Set<String> beforeTools = beforeSnapshots.stream()
                .map(ToolCabinetSnapshotDO::getToolCode)
                .collect(Collectors.toSet());

        Set<String> afterTools = afterSnapshots.stream()
                .map(ToolCabinetSnapshotDO::getToolCode)
                .collect(Collectors.toSet());

        // 计算变化
        Set<String> added = new HashSet<>(afterTools);
        added.removeAll(beforeTools);

        Set<String> removed = new HashSet<>(beforeTools);
        removed.removeAll(afterTools);

        // 返回变化：借出返回减少的工具，归还返回增加的工具
        List<String> changes = new ArrayList<>();
        changes.addAll(removed);  // 借出时减少的工具
        changes.addAll(added);    // 归还时增加的工具

        log.info("快照对比: borrowRecordId={}, before={}, after={}, changes={}",
                borrowRecordId, beforeTools, afterTools, changes);

        return changes;
    }
}
