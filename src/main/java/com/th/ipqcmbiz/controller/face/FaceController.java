package com.th.ipqcmbiz.controller.face;

import com.th.ipqcmbiz.controller.common.BaseController;
import com.th.ipqcmbiz.entity.common.Result;
import com.th.ipqcmbiz.service.face.FaceService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName FaceController
 * @Description 人脸影响controller
 * @Author 杨兴明
 * @Date 2026/3/25 14:15
 * @Version 1.0
 */
@RestController
@RequestMapping("/face")
public class FaceController extends BaseController {

    @Resource
    private FaceService faceService;

    @GetMapping("/frame")
    public void frame(@RequestParam String userName,@RequestParam String userId, HttpServletResponse response) throws IOException {
        response.setContentType("image/jpeg");
        BufferedImage img = faceService.getFrame(userId,userName);
        if (img != null) ImageIO.write(img, "jpg", response.getOutputStream());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> map = new HashMap<>();
        map.put("progress", faceService.getProgress());
        map.put("direction", faceService.getDirection());
        map.put("msg", faceService.getProgress() >= 10 ? "完成" : "采集中");
        return map;
    }

    @GetMapping("/releaseCamera")
    public void releaseCamera() {
        faceService.releaseCamera();
    }

    // 新增：手动重试初始化的方法（供前端/监控调用）
    @GetMapping("/reinit") // 新增接口，允许手动触发重新初始化
    public Result reinitCamera() {
        return success(faceService.reinitCamera());
    }
}
