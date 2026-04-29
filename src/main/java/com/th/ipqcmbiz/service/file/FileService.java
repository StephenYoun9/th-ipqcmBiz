package com.th.ipqcmbiz.service.file;

import com.th.ipqcmbiz.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${tool.image.upload-path:/tmp/tool-images}")
    private String uploadPath;

    public String uploadImage(MultipartFile file) {
        validateImageFile(file);
        String fileName = generateFileName(file);
        saveFile(file, fileName);
        return "/tool-images/" + fileName;
    }

    public String uploadAttachment(MultipartFile file) {
        validateAttachmentFile(file);
        String fileName = generateFileName(file);
        saveFile(file, fileName);
        return "/tool-images/" + fileName;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new BusinessException(400, "文件URL不能为空");
        }
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        Path filePath = Paths.get(uploadPath, fileName);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", e.getMessage());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "图片不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "只能上传图片文件");
        }
    }

    private void validateAttachmentFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(400, "无法识别文件类型");
        }
        if (!contentType.startsWith("image/") && !contentType.startsWith("video/")) {
            throw new BusinessException(400, "只能上传图片或视频文件");
        }
    }

    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + ext;
    }

    private void saveFile(MultipartFile file, String fileName) {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            log.error("保存文件失败: {}", e.getMessage());
            throw new BusinessException(500, "上传失败");
        }
    }
}