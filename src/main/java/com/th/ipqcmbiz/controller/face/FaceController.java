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

import java.io.OutputStream;
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
    public void getFrame(
            @RequestParam String userId,
            @RequestParam String userName,
            HttpServletResponse response
    ) {
        try {
            // 先初始化用户信息
            faceService.getFrame(userId, userName);
            // 直接拿预压缩好的JPEG字节数组
            byte[] jpegBytes = faceService.getLatestJpegFrame();
            if (jpegBytes == null || jpegBytes.length == 0) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            // 设置响应头，浏览器直接渲染
            response.setContentType("image/jpeg");
            response.setContentLength(jpegBytes.length);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");

            // 直接写出，零处理延迟
            try (OutputStream os = response.getOutputStream()) {
                os.write(jpegBytes);
                os.flush();
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {

        Map<String, Object> map = new HashMap<>();
        map.put("progress", faceService.getProgress());
        map.put("direction", faceService.getDirection());
        map.put("msg", faceService.getProgress() >= 10 ? "完成" : "采集中");
        return Result.success(map);
    }

    @GetMapping("/releaseCamera")
    public Result release() {
        faceService.releaseCamera();
        return Result.success("已释放");
    }

    @GetMapping("/reinit")
    public Result reinit() {
        return faceService.reinitCamera();
    }
}
