package com.th.ipqcmbiz.controller.employee;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.BorrowRecordDO;
import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import com.th.ipqcmbiz.service.tool.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 员工端-工具借还控制器
 */
@Tag(name = "员工端-工具借还")
@RestController
@RequestMapping("/employee/tool")
public class ToolBorrowController extends BaseController {

    @Resource
    private ToolService toolService;

    @Operation(summary = "获取可借工具列表（分页）")
    @GetMapping("/available")
    public Result<PageInfo<ToolInfoDO>> getAvailableTools(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String toolType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return success(toolService.getAvailableToolsPaged(keyword, toolType, pageNum, pageSize));
    }

    @Operation(summary = "借工具")
    @PostMapping("/borrow")
    public Result<Boolean> borrowTool(@RequestParam String toolCode) {
        return success(toolService.borrowTool(toolCode));
    }

    @Operation(summary = "获取我的未还工具列表（分页）")
    @GetMapping("/my-borrowed")
    public Result<PageInfo<BorrowRecordDO>> getMyBorrowedTools(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return success(toolService.getMyBorrowedTools(pageNum, pageSize));
    }

    @Operation(summary = "还工具")
    @PostMapping("/return")
    public Result<Boolean> returnTool(@RequestParam String toolCode) {
        return success(toolService.returnTool(toolCode));
    }
}