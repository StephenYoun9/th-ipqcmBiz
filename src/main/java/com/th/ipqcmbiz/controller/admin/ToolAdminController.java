package com.th.ipqcmbiz.controller.admin;

import com.th.ipqcmbiz.entity.po.ToolInfoDO;
import com.th.ipqcmbiz.entity.vo.input.ToolInfoReqVO;
import com.th.ipqcmbiz.service.tool.ToolService;
import com.th.ipqcmbiz.entity.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Tag(name = "管理员-工具管理")
@RestController
@RequestMapping("/admin/tool")
@Slf4j
public class ToolAdminController {

    @Autowired
    private ToolService toolService;

    @Value("${tool.image.upload-path:/tmp/tool-images}")
    private String uploadPath;

    @Operation(summary = "获取工具列表")
    @GetMapping("/list")
    public Result<List<ToolInfoDO>> getToolList(@RequestParam(required = false) String keyword) {
        return Result.success(toolService.getToolList(keyword));
    }

    @Operation(summary = "根据编号获取工具")
    @GetMapping("/{toolCode}")
    public Result<ToolInfoDO> getToolByCode(@PathVariable String toolCode) {
        return Result.success(toolService.getToolByCode(toolCode));
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
        return Result.success(toolService.addTool(tool));
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
        return Result.success(toolService.updateTool(tool));
    }

    @Operation(summary = "下架工具")
    @DeleteMapping("/{toolCode}")
    public Result<Boolean> deleteTool(@PathVariable String toolCode) {
        return Result.success(toolService.deleteTool(toolCode));
    }

    @Operation(summary = "更新工具状态")
    @PutMapping("/status/{toolCode}")
    public Result<Boolean> updateToolStatus(@PathVariable String toolCode, @RequestParam String status) {
        return Result.success(toolService.updateToolStatus(toolCode, status));
    }

    @Operation(summary = "上传工具图片")
    @PostMapping("/image/{toolCode}")
    public Result<String> uploadImage(@PathVariable String toolCode, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "图片不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "只能上传图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + ext;

        try {
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            Path filePath = Paths.get(uploadPath, fileName);
            Files.copy(file.getInputStream(), filePath);

            String imageUrl = "/tool-images/" + fileName;
            toolService.updateToolImage(toolCode, imageUrl);

            return Result.success(imageUrl);
        } catch (IOException e) {
            log.error("上传图片失败: {}", e.getMessage());
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除工具图片")
    @DeleteMapping("/image/{toolCode}")
    public Result<Boolean> deleteImage(@PathVariable String toolCode) {
        ToolInfoDO tool = toolService.getToolByCode(toolCode);
        if (tool == null) {
            return Result.error(404, "工具不存在");
        }

        String imageUrl = tool.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadPath, fileName);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("删除图片文件失败: {}", e.getMessage());
            }
            toolService.updateToolImage(toolCode, null);
        }

        return Result.success(true);
    }
}