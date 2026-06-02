package com.th.ipqcmbiz.controller;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.ToolCabinetInventoryDO;
import com.th.ipqcmbiz.service.toolinventory.ToolInventoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工具柜库存控制器
 */
@RestController
@RequestMapping("/tool/inventory")
@Slf4j
public class ToolInventoryController {

    @Resource
    private ToolInventoryService inventoryService;

    /**
     * 获取工具柜库存列表
     */
    @GetMapping("/{cabinetNo}")
    public Result getInventory(@PathVariable String cabinetNo) {
        List<ToolCabinetInventoryDO> inventories = inventoryService.getCabinetInventory(cabinetNo);
        return Result.success(inventories);
    }

    /**
     * 初始化工具柜库存
     */
    @PostMapping("/init")
    public Result initInventory(@RequestBody Map<String, Object> params) {
        String cabinetNo = (String) params.get("cabinetNo");
        @SuppressWarnings("unchecked")
        List<String> toolCodes = (List<String>) params.get("toolCodes");
        @SuppressWarnings("unchecked")
        List<String> positions = (List<String>) params.get("positions");

        if (cabinetNo == null || toolCodes == null || positions == null) {
            return Result.error(400, "参数不完整");
        }

        inventoryService.initCabinetInventory(cabinetNo, toolCodes, positions);
        return Result.success("库存初始化成功");
    }

    /**
     * 保存借出前快照
     */
    @PostMapping("/snapshot/before")
    public Result saveSnapshotBefore(@RequestBody Map<String, Object> params) {
        String cabinetNo = (String) params.get("cabinetNo");
        Long borrowRecordId = ((Number) params.get("borrowRecordId")).longValue();

        if (cabinetNo == null || borrowRecordId == null) {
            return Result.error(400, "参数不完整");
        }

        inventoryService.saveSnapshotBeforeBorrow(cabinetNo, borrowRecordId);
        return Result.success("借出前快照已保存");
    }

    /**
     * 保存归还后快照
     */
    @PostMapping("/snapshot/after")
    public Result saveSnapshotAfter(@RequestBody Map<String, Object> params) {
        String cabinetNo = (String) params.get("cabinetNo");
        Long borrowRecordId = ((Number) params.get("borrowRecordId")).longValue();

        if (cabinetNo == null || borrowRecordId == null) {
            return Result.error(400, "参数不完整");
        }

        inventoryService.saveSnapshotAfterReturn(cabinetNo, borrowRecordId);
        return Result.success("归还后快照已保存");
    }

    /**
     * 对比快照，获取工具变化
     */
    @GetMapping("/snapshot/compare/{borrowRecordId}")
    public Result compareSnapshot(@PathVariable Long borrowRecordId) {
        List<String> changes = inventoryService.compareSnapshot(borrowRecordId);
        return Result.success(changes);
    }
}
