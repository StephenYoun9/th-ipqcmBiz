package com.th.ipqcmbiz.controller.face;

import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.entity.po.UserInfoDO;
import com.th.ipqcmbiz.mapper.UserInfoMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/finger")
@Slf4j
public class FingerController {

    @Resource
    private UserInfoMapper userInfoMapper;

    @PostMapping("/enroll/start")
    public Result enrollStart(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        if (userId == null || userId.isEmpty()) {
            return Result.error(400, "用户ID不能为空");
        }
        try {
            UserInfoDO user = UserInfoDO.builder().userId(userId).fingerRegistered("Y").build();
            userInfoMapper.updateFingerEnrolled(user);
            log.info("指纹录入完成: userId={}", userId);
            return Result.success("指纹录入成功");
        } catch (Exception e) {
            log.error("指纹录入失败: {}", e.getMessage(), e);
            return Result.error(500, "指纹录入失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result deleteFinger(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        if (userId == null || userId.isEmpty()) {
            return Result.error(400, "用户ID不能为空");
        }
        try {
            UserInfoDO user = UserInfoDO.builder().userId(userId).fingerRegistered("N").build();
            userInfoMapper.updateFingerEnrolled(user);
            log.info("删除指纹: userId={}", userId);
            return Result.success("指纹数据已删除");
        } catch (Exception e) {
            log.error("删除指纹失败: {}", e.getMessage(), e);
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }
}
