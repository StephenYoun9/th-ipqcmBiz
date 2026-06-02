package com.th.ipqcmbiz.service.toolborrow;

import com.th.ipqcmbiz.context.UserContext;
import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.po.ToolCabinetInventoryDO;
import com.th.ipqcmbiz.entity.po.ToolCabinetSnapshotDO;
import com.th.ipqcmbiz.exception.BusinessException;
import com.th.ipqcmbiz.mapper.borrow.BorrowRecordMapper;
import com.th.ipqcmbiz.mapper.ToolCabinetInventoryMapper;
import com.th.ipqcmbiz.mapper.ToolCabinetSnapshotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 工具借用服务
 * 核心流程：
 * 1. 管理员初始化柜中工具（设置初始库存快照）
 * 2. 用户打开柜门（假实现）
 * 3. 用户拿工具，摄像头检测当前状态
 * 4. 对比初始状态和当前状态，记录借走的工具
 */
@Service
@Slf4j
public class ToolBorrowService {

    @Autowired
    private ToolCabinetInventoryMapper inventoryMapper;

    @Autowired
    private ToolCabinetSnapshotMapper snapshotMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    private static final String CABINET_NO_DEFAULT = "001";
    private static final String STATUS_IN_CABINET = "0";
    private static final String STATUS_BORROWED = "1";
    private static final String STATUS_MAINTENANCE = "2";
    private static final String SNAPSHOT_TYPE_INITIAL = "0";
    private static final String SNAPSHOT_TYPE_AFTER = "1";

    /**
     * 获取当前柜中库存（只返回在柜中的工具）
     */
    public List<ToolCabinetInventoryDO> getCurrentInventory() {
        return inventoryMapper.selectByCabinetNoAndStatus(CABINET_NO_DEFAULT, STATUS_IN_CABINET);
    }

    /**
     * 初始化柜中工具（管理员操作）
     * 将工具设置到指定位置，作为初始状态
     */
    @Transactional
    public void initCabinetWithTools(List<String> toolCodes, List<String> positions) {
        if (toolCodes == null || positions == null || toolCodes.size() != positions.size()) {
            throw new BusinessException(400, "工具编号和位置数量不匹配");
        }

        // 清除现有库存
        inventoryMapper.deleteByCabinetNo(CABINET_NO_DEFAULT);

        // 插入新库存
        List<ToolCabinetInventoryDO> inventories = new ArrayList<>();
        for (int i = 0; i < toolCodes.size(); i++) {
            ToolCabinetInventoryDO inv = new ToolCabinetInventoryDO();
            inv.setId(System.currentTimeMillis() + i);
            inv.setCabinetNo(CABINET_NO_DEFAULT);
            inv.setToolCode(toolCodes.get(i));
            inv.setPosition(positions.get(i));
            inv.setStatus(STATUS_IN_CABINET);
            inventories.add(inv);
        }

        if (!inventories.isEmpty()) {
            inventoryMapper.batchInsert(inventories);
        }

        // 保存初始快照（作为基准）
        saveSnapshot(SNAPSHOT_TYPE_INITIAL, null);

        log.info("柜子 {} 初始化完成，位置数: {}", CABINET_NO_DEFAULT, inventories.size());
    }

    /**
     * 获取当前柜中已存在的工具列表（用于初始化）
     */
    public List<String> getAvailableToolCodes() {
        List<ToolCabinetInventoryDO> inventories = inventoryMapper.selectByCabinetNo(CABINET_NO_DEFAULT);
        return inventories.stream()
                .map(ToolCabinetInventoryDO::getToolCode)
                .collect(Collectors.toList());
    }

    /**
     * 打开柜门（假实现）
     * 返回成功即可，实际柜门控制由硬件实现
     */
    public boolean openCabinetDoor() {
        log.info("柜门已打开（模拟）");
        return true;
    }

    /**
     * 开始借出流程
     * 保存借出前快照（不创建借出记录，等用户确认借出后再创建）
     */
    @Transactional
    public void startBorrowProcess() {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }

        // 保存借出前快照（用userId作为临时标识，等确认时再关联到具体记录）
        saveSnapshot(SNAPSHOT_TYPE_INITIAL, null);

        log.info("开始借出流程: userId={}", userId);
    }

    /**
     * 确认借出完成
     * 为每个工具创建一条借出记录
     */
    @Transactional
    public List<Long> confirmBorrowCompleted(List<String> borrowedTools) {
        log.info("confirmBorrowCompleted service called, borrowedTools={}", borrowedTools);

        String userId = UserContext.getUserId();
        List<Long> recordIds = new ArrayList<>();

        if (borrowedTools == null || borrowedTools.isEmpty()) {
            log.info("没有借出工具");
            return recordIds;
        }

        // 为每个工具创建一条独立的借出记录
        for (String toolCode : borrowedTools) {
            BorrowRecordDO record = BorrowRecordDO.builder()
                    .userId(userId)
                    .toolCode(toolCode)
                    .operatorType("borrow")
                    .cabinetNo(CABINET_NO_DEFAULT)
                    .build();
            borrowRecordMapper.insert(record);
            recordIds.add(record.getId());
            log.debug("创建借出记录: id={}, toolCode={}, userId={}", record.getId(), toolCode, userId);
        }

        // 保存借后快照
        if (!recordIds.isEmpty()) {
            saveSnapshot(SNAPSHOT_TYPE_AFTER, recordIds.get(0));
        }

        log.info("借出完成: recordIds={}, userId={}, 借走工具={}", recordIds, userId, borrowedTools);
        return recordIds;
    }

    /**
     * 处理借还混合操作
     * @param initialTools 初始柜中工具列表
     * @param finalTools 最终识别到的工具列表
     * @return 操作结果，包含借走的工具和归还的工具
     */
    @Transactional
    public Map<String, Object> processBorrowReturn(List<String> initialTools, List<String> finalTools) {
        String operatorId = UserContext.getUserId();
        log.info("处理借还操作: operatorId={}, initial={}, final={}", operatorId, initialTools, finalTools);

        Set<String> initialSet = new HashSet<>(initialTools);
        Set<String> finalSet = new HashSet<>(finalTools);

        // 借走的工具 = 初始有但最终没有的
        Set<String> borrowedSet = new HashSet<>(initialSet);
        borrowedSet.removeAll(finalSet);

        // 归还的工具 = 最终有但初始没有的
        Set<String> returnedSet = new HashSet<>(finalSet);
        returnedSet.removeAll(initialSet);

        List<String> borrowedList = new ArrayList<>(borrowedSet);
        List<String> returnedList = new ArrayList<>(returnedSet);

        log.info("计算结果: borrowed={}, returned={}", borrowedList, returnedList);

        // 处理借出：为每把借走的工具创建借出记录
        for (String toolCode : borrowedList) {
            BorrowRecordDO record = BorrowRecordDO.builder()
                    .userId(operatorId)
                    .toolCode(toolCode)
                    .borrowTime(new Date())
                    .status("BORROWED")
                    .operatorType("borrow")
                    .cabinetNo(CABINET_NO_DEFAULT)
                    .build();
            borrowRecordMapper.insert(record);

            // 更新库存状态为已借出
            inventoryMapper.updateStatusByToolCode(toolCode, STATUS_BORROWED);
            log.debug("借出工具: toolCode={}, userId={}", toolCode, operatorId);
        }

        // 处理归还：为每把归还的工具更新记录
        for (String toolCode : returnedList) {
            // 查找该工具的未还借出记录
            BorrowRecordDO borrowRecord = borrowRecordMapper.selectBorrowedByToolCode(toolCode);
            if (borrowRecord != null) {
                // 更新归还信息
                borrowRecordMapper.updateReturn(toolCode, operatorId);

                // 更新库存状态为在柜中
                inventoryMapper.updateStatusByToolCode(toolCode, STATUS_IN_CABINET);

                log.debug("归还工具: toolCode={}, 原借用人={}, 还操作人={}",
                        toolCode, borrowRecord.getUserId(), operatorId);
            } else {
                log.warn("归还工具 {} 时未找到对应的未还借出记录", toolCode);
                // 即使没有借出记录，也更新库存状态为在柜中
                inventoryMapper.updateStatusByToolCode(toolCode, STATUS_IN_CABINET);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("borrowed", borrowedList);
        result.put("returned", returnedList);
        result.put("borrowedCount", borrowedList.size());
        result.put("returnedCount", returnedList.size());

        log.info("借还操作完成: {}", result);
        return result;
    }

    /**
     * 根据YOLO检测更新柜中库存
     * 摄像头检测到当前柜中有哪些工具，据此更新库存状态
     */
    @Transactional
    public void updateInventoryByDetection(List<String> detectedToolCodes) {
        List<ToolCabinetInventoryDO> currentInventory = inventoryMapper.selectByCabinetNo(CABINET_NO_DEFAULT);

        Set<String> detectedSet = new HashSet<>(detectedToolCodes);

        for (ToolCabinetInventoryDO inv : currentInventory) {
            if (STATUS_IN_CABINET.equals(inv.getStatus())) {
                // 如果这个位置的工具有检测到，保持in_cabinet
                if (detectedSet.contains(inv.getToolCode())) {
                    detectedSet.remove(inv.getToolCode());
                }
                // 如果没检测到，说明工具被拿走了，标记为已借出
                else {
                    inv.setStatus(STATUS_BORROWED);
                    inventoryMapper.updateStatus(inv.getId(), STATUS_BORROWED);
                    log.debug("位置 {} 工具 {} 已被取走", inv.getPosition(), inv.getToolCode());
                }
            }
        }

        // 如果检测到新工具（不在库存中的），可能是放回去的
        for (String detectedCode : detectedSet) {
            log.info("检测到未登记工具: {}", detectedCode);
            // 可以选择自动添加或记录异常
        }
    }

    /**
     * 根据被借走的工具列表更新库存状态为已借出
     */
    @Transactional
    public void updateInventoryStatusToEmpty(List<String> borrowedToolCodes) {
        if (borrowedToolCodes == null || borrowedToolCodes.isEmpty()) {
            return;
        }

        Set<String> borrowedSet = new HashSet<>(borrowedToolCodes);
        List<ToolCabinetInventoryDO> currentInventory = inventoryMapper.selectByCabinetNo(CABINET_NO_DEFAULT);

        for (ToolCabinetInventoryDO inv : currentInventory) {
            if (borrowedSet.contains(inv.getToolCode())) {
                inv.setStatus(STATUS_BORROWED);
                inventoryMapper.updateStatus(inv.getId(), STATUS_BORROWED);
                log.info("借出完成，位置 {} 工具 {} 状态更新为已借出", inv.getPosition(), inv.getToolCode());
            }
        }
    }

    /**
     * 获取借走的工具列表（对比快照）
     */
    private List<String> getBorrowedTools(Long borrowRecordId) {
        List<ToolCabinetSnapshotDO> beforeSnapshots = snapshotMapper.selectByBorrowRecordIdAndType(
                borrowRecordId, SNAPSHOT_TYPE_INITIAL);
        List<ToolCabinetSnapshotDO> afterSnapshots = snapshotMapper.selectByBorrowRecordIdAndType(
                borrowRecordId, SNAPSHOT_TYPE_AFTER);

        Set<String> beforeTools = beforeSnapshots.stream()
                .map(ToolCabinetSnapshotDO::getToolCode)
                .collect(Collectors.toSet());

        Set<String> afterTools = afterSnapshots.stream()
                .map(ToolCabinetSnapshotDO::getToolCode)
                .collect(Collectors.toSet());

        // 借走的工具 = 初始有的 - 现在还有的
        Set<String> borrowed = new HashSet<>(beforeTools);
        borrowed.removeAll(afterTools);

        return new ArrayList<>(borrowed);
    }

    /**
     * 保存快照
     */
    private void saveSnapshot(String snapshotType, Long borrowRecordId) {
        List<ToolCabinetInventoryDO> inventories = inventoryMapper.selectByCabinetNo(CABINET_NO_DEFAULT);

        final long baseId = System.currentTimeMillis();
        final Long finalBorrowRecordId = borrowRecordId;

        List<ToolCabinetSnapshotDO> snapshots = IntStream.range(0, inventories.size())
                .mapToObj(idx -> {
                    ToolCabinetInventoryDO inv = inventories.get(idx);
                    ToolCabinetSnapshotDO snapshot = new ToolCabinetSnapshotDO();
                    snapshot.setId(baseId + idx);
                    snapshot.setCabinetNo(CABINET_NO_DEFAULT);
                    snapshot.setToolCode(inv.getToolCode());
                    snapshot.setPosition(inv.getPosition());
                    snapshot.setSnapshotType(snapshotType);
                    snapshot.setBorrowRecordId(finalBorrowRecordId == null ? 0L : finalBorrowRecordId);
                    return snapshot;
                })
                .collect(Collectors.toList());

        if (!snapshots.isEmpty()) {
            snapshotMapper.batchInsert(snapshots);
        }

        log.info("保存快照: type={}, count={}", snapshotType, snapshots.size());
    }

    /**
     * 获取借出记录
     */
    public List<BorrowRecordDO> getMyBorrowRecords(int pageNum, int pageSize) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        int offset = (pageNum - 1) * pageSize;
        return borrowRecordMapper.selectByUserId(userId, offset, pageSize);
    }
}