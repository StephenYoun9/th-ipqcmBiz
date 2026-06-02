package com.th.ipqcmbiz.controller;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.po.ToolCabinetInventoryDO;
import com.th.ipqcmbiz.entity.vo.ToolDetection;
import com.th.ipqcmbiz.mapper.ToolCabinetVOConverter;
import com.th.ipqcmbiz.mapper.ToolCabinetInventoryMapper;
import com.th.ipqcmbiz.service.toolborrow.ToolBorrowService;
import com.th.ipqcmbiz.service.toolrecognition.ToolVideoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工具柜控制器
 * 核心流程：
 * 1. 初始化柜中工具
 * 2. 打开柜门（假实现）
 * 3. 确认拿完，对比记录借走的工具
 */
@RestController
@RequestMapping("/tool/cabinet")
@Slf4j
public class ToolCabinetController {

    @Autowired
    private ToolBorrowService toolBorrowService;

    @Autowired
    private ToolVideoService toolVideoService;

    @Autowired
    private ToolCabinetInventoryMapper inventoryMapper;

    /**
     * 获取当前柜中库存
     * @param statusOnly 如果为true，只返回在柜中的工具；否则返回所有工具
     */
    @GetMapping("/inventory")
    public Result getCurrentInventory(@RequestParam(required = false, defaultValue = "false") Boolean statusOnly) {
        List<ToolCabinetInventoryDO> inventories;
        if (statusOnly) {
            inventories = toolBorrowService.getCurrentInventory();
        } else {
            inventories = inventoryMapper.selectByCabinetNo("001");
        }
        return Result.success(ToolCabinetVOConverter.INSTANCE.toVOList(inventories));
    }

    /**
     * 初始化柜中工具（管理员操作）
     */
    @PostMapping("/init")
    public Result initCabinet(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> toolCodes = (List<String>) params.get("toolCodes");
        @SuppressWarnings("unchecked")
        List<String> positions = (List<String>) params.get("positions");

        if (toolCodes == null || positions == null || toolCodes.size() != positions.size()) {
            return Result.error(400, "工具编号和位置数量不匹配");
        }

        toolBorrowService.initCabinetWithTools(toolCodes, positions);
        return Result.success("柜子初始化成功");
    }

    /**
     * 获取已登记的工具编号列表
     */
    @GetMapping("/available-tools")
    public Result getAvailableToolCodes() {
        List<String> toolCodes = toolBorrowService.getAvailableToolCodes();
        return Result.success(toolCodes);
    }

    /**
     * 打开柜门（假实现）
     */
    @PostMapping("/open-door")
    public Result openDoor() {
        boolean success = toolBorrowService.openCabinetDoor();
        if (success) {
            return Result.success("柜门已打开");
        } else {
            return Result.error(500, "柜门打开失败");
        }
    }

    /**
     * 开始借出流程
     * 保存借出前快照（不创建借出记录）
     */
    @PostMapping("/start")
    public Result startBorrow() {
        toolBorrowService.startBorrowProcess();
        return Result.success(null);
    }

    /**
     * 更新柜中库存（根据YOLO检测结果）
     */
    @PostMapping("/update-inventory")
    public Result updateInventoryByDetection(@RequestBody List<String> detectedToolCodes) {
        toolBorrowService.updateInventoryByDetection(detectedToolCodes);
        return Result.success("库存已更新");
    }

    /**
     * 确认借出完成
     * 为每个工具创建独立的借出记录，并更新库存状态
     */
    @PostMapping("/confirm")
    public Result confirmBorrowCompleted(@RequestBody Map<String, Object> params) {
        log.info("confirmBorrowCompleted called, params: {}", params);

        @SuppressWarnings("unchecked")
        List<String> borrowedTools = (List<String>) params.get("detectedTools");

        log.info("borrowedTools: {}", borrowedTools);

        // 确认借出完成（为每个工具创建独立记录）
        List<Long> recordIds = toolBorrowService.confirmBorrowCompleted(borrowedTools);

        // 根据被借走的工具更新库存状态为"已借出"
        if (borrowedTools != null && !borrowedTools.isEmpty()) {
            toolBorrowService.updateInventoryStatusToEmpty(borrowedTools);
        }

        return Result.success(recordIds);
    }

    /**
     * 处理借还混合操作
     * 根据初始柜中工具和最终检测到的工具，智能判断借了哪些、还了哪些
     */
    @PostMapping("/process-borrow-return")
    public Result processBorrowReturn(@RequestBody Map<String, Object> params) {
        log.info("processBorrowReturn called, params: {}", params);

        @SuppressWarnings("unchecked")
        List<String> initialTools = (List<String>) params.get("initialTools");

        @SuppressWarnings("unchecked")
        List<String> finalTools = (List<String>) params.get("finalTools");

        log.info("initialTools: {}, finalTools: {}", initialTools, finalTools);

        Map<String, Object> result = toolBorrowService.processBorrowReturn(initialTools, finalTools);

        return Result.success(result);
    }

    /**
     * 获取当前视频帧和检测结果
     */
    @GetMapping("/frame-info")
    public Result getFrameInfo() {
        var frameInfo = toolVideoService.getLatestFrameWithInfo();
        if (frameInfo == null) {
            return Result.error(500, "无视频帧");
        }
        return Result.success(frameInfo);
    }

    /**
     * 启动摄像头
     */
    @PostMapping("/camera/start")
    public Result startCamera() {
        return toolVideoService.reinitCamera();
    }

    /**
     * 释放摄像头
     */
    @PostMapping("/camera/release")
    public Result releaseCamera() {
        toolVideoService.releaseCamera();
        return Result.success("摄像头已释放");
    }

    /**
     * 获取我的借出记录
     */
    @GetMapping("/my-records")
    public Result getMyRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        List<BorrowRecordDO> records = toolBorrowService.getMyBorrowRecords(pageNum, pageSize);
        PageInfo<BorrowRecordDO> pageInfo = new PageInfo<>(records);
        return Result.success(pageInfo);
    }
}