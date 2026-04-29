package com.th.ipqcmbiz.controller.admin;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.input.AnnouncementReqVO;
import com.th.ipqcmbiz.entity.vo.output.AnnouncementRespVO;
import com.th.ipqcmbiz.service.announcement.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统公告管理控制器
 */
@Tag(name = "管理员-系统公告")
@RestController
@RequestMapping("/admin/announcement")
public class AnnouncementController extends BaseController {

    @Resource
    private AnnouncementService announcementService;

    @Operation(summary = "发布公告")
    @PostMapping
    public Result<Boolean> publishAnnouncement(
            @Valid @RequestBody AnnouncementReqVO reqVO,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        if (userId == null || userId.isEmpty()) {
            userId = "admin";
            userName = "管理员";
        }
        return success(announcementService.publishAnnouncement(reqVO, userId, userName));
    }

    @Operation(summary = "获取生效中的公告列表")
    @GetMapping("/active")
    public Result<List<AnnouncementRespVO>> getActiveAnnouncements() {
        return success(announcementService.getActiveAnnouncements());
    }

    @Operation(summary = "获取所有公告列表（分页）")
    @GetMapping("/list")
    public Result<PageInfo<AnnouncementRespVO>> getAllAnnouncements(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "5") int pageSize) {
        return success(announcementService.getAllAnnouncementsPaged(pageNum, pageSize));
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteAnnouncement(@PathVariable Long id) {
        return success(announcementService.deleteAnnouncement(id));
    }

    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    public Result<Boolean> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementReqVO reqVO) {
        return success(announcementService.updateAnnouncement(id, reqVO));
    }
}