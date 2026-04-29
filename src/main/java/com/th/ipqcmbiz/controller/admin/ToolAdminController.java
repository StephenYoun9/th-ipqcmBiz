package com.th.ipqcmbiz.controller.admin;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import com.th.ipqcmbiz.entity.vo.input.ToolInfoReqVO;
import com.th.ipqcmbiz.service.file.FileService;
import com.th.ipqcmbiz.service.tool.ToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "管理员-工具管理")
@RestController
@RequestMapping("/admin/tool")
public class ToolAdminController extends BaseController {

    @Resource
    private ToolService toolService;

    @Resource
    private FileService fileService;

    @Operation(summary = "获取工具列表")
    @GetMapping("/list")
    public Result<List<ToolInfoDO>> getToolList(@RequestParam(required = false) String keyword) {
        return success(toolService.getToolList(keyword));
    }

    @Operation(summary = "获取工具列表（分页）")
    @GetMapping("/list/paged")
    public Result<PageInfo<ToolInfoDO>> getToolListPaged(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "15") int pageSize) {
        return success(toolService.getToolListPaged(keyword, pageNum, pageSize));
    }

    @Operation(summary = "根据编号获取工具")
    @GetMapping("/{toolCode}")
    public Result<ToolInfoDO> getToolByCode(@PathVariable String toolCode) {
        return success(toolService.getToolByCode(toolCode));
    }

    @Operation(summary = "上架新工具")
    @PostMapping
    public Result<Boolean> addTool(@RequestBody ToolInfoReqVO reqVO) {
        ToolInfoDO tool = ToolInfoDO.builder()
                .toolCode(reqVO.getToolCode())
                .toolName(reqVO.getToolName())
                .toolType(reqVO.getToolType())
                .cabinetNo(reqVO.getCabinetNo())
                .status(reqVO.getStatus())
                .specification(reqVO.getSpecification())
                .imageUrl(reqVO.getImageUrl())
                .build();
        return success(toolService.addTool(tool));
    }

    @Operation(summary = "更新工具信息")
    @PutMapping
    public Result<Boolean> updateTool(@RequestBody ToolInfoReqVO reqVO) {
        ToolInfoDO tool = ToolInfoDO.builder()
                .toolCode(reqVO.getToolCode())
                .toolName(reqVO.getToolName())
                .toolType(reqVO.getToolType())
                .cabinetNo(reqVO.getCabinetNo())
                .status(reqVO.getStatus())
                .specification(reqVO.getSpecification())
                .imageUrl(reqVO.getImageUrl())
                .build();
        return success(toolService.updateTool(tool));
    }

    @Operation(summary = "下架工具")
    @DeleteMapping("/{toolCode}")
    public Result<Boolean> deleteTool(@PathVariable String toolCode) {
        return success(toolService.deleteTool(toolCode));
    }

    @Operation(summary = "更新工具状态")
    @PutMapping("/status/{toolCode}")
    public Result<Boolean> updateToolStatus(@PathVariable String toolCode, @RequestParam String status) {
        return success(toolService.updateToolStatus(toolCode, status));
    }

    @Operation(summary = "上传工具图片")
    @PostMapping("/image/{toolCode}")
    public Result<String> uploadImage(@PathVariable String toolCode, @RequestParam("file") MultipartFile file) {
        String imageUrl = fileService.uploadImage(file);
        toolService.updateToolImage(toolCode, imageUrl);
        return success(imageUrl);
    }

    @Operation(summary = "删除工具图片")
    @DeleteMapping("/image/{toolCode}")
    public Result<Boolean> deleteImage(@PathVariable String toolCode) {
        ToolInfoDO tool = toolService.getToolByCode(toolCode);
        if (tool != null && tool.getImageUrl() != null) {
            fileService.deleteFile(tool.getImageUrl());
            toolService.updateToolImage(toolCode, null);
        }
        return success(true);
    }
}