package com.th.ipqcmbiz.controller.employee;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.output.BorrowRecordRespVO;
import com.th.ipqcmbiz.service.tool.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工端接口控制器
 */
@Tag(name = "员工端-个人中心")
@RestController
@RequestMapping("/employee")
public class EmployeeController extends BaseController {

    @Resource
    private ToolService toolService;

    @Operation(summary = "获取我的借还记录（分页）")
    @PostMapping("/my-borrow-records")
    public Result<PageInfo<BorrowRecordRespVO>> getMyBorrowRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "5") int pageSize) {
        return success(toolService.getMyBorrowRecords(pageNum, pageSize));
    }
}