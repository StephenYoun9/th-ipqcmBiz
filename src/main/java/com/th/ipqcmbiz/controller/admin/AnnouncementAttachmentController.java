package com.th.ipqcmbiz.controller.admin;

import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 公告附件上传控制器
 */
@Tag(name = "管理员-公告附件")
@RestController
@RequestMapping("/admin/announcement")
public class AnnouncementAttachmentController extends BaseController {

    @Resource
    private FileService fileService;

    @Operation(summary = "上传公告附件")
    @PostMapping("/attachment")
    public Result<String> uploadAttachment(@RequestParam("file") MultipartFile file) {
        return success(fileService.uploadAttachment(file));
    }

    @Operation(summary = "删除公告附件")
    @DeleteMapping("/attachment")
    public Result<Boolean> deleteAttachment(@RequestParam String fileUrl) {
        fileService.deleteFile(fileUrl);
        return success(true);
    }
}