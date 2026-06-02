package com.th.ipqcmbiz.controller.common;

import com.github.pagehelper.PageInfo;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.vo.output.AnnouncementRespVO;
import com.th.ipqcmbiz.service.announcement.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共接口控制器
 */
@Tag(name = "公共接口")
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    @Autowired
    private AnnouncementService announcementService;

    /**
     * 获取当前生效的系统公告（所有用户可访问）
     */
    @Operation(summary = "获取系统公告")
    @GetMapping("/announcement")
    public Result<List<AnnouncementRespVO>> getAnnouncements() {
        List<AnnouncementRespVO> list = announcementService.getActiveAnnouncements();
        return Result.success(list);
    }

    /**
     * 获取当前生效的系统公告（分页）
     */
    @Operation(summary = "获取系统公告（分页）")
    @GetMapping("/announcement/paged")
    public Result<PageInfo<AnnouncementRespVO>> getAnnouncementsPaged(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "5") int pageSize) {
        PageInfo<AnnouncementRespVO> pageInfo = announcementService.getActiveAnnouncementsPaged(pageNum, pageSize);
        return Result.success(pageInfo);
    }
}