package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.UploadPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 功法插画上传接口：
 *   POST /api/manuals/upload
 *        form-data: file=<图片文件>
 *        成功 200 -> { url: "/images/manuals/<文件名>" }
 *        失败 400 -> 非图片 / 超过大小限制；500 -> 落盘失败
 *
 * 文件保存到 UploadPaths.DIR（static/images/manuals），URL 与种子图保持一致，
 * 前端详情页 coverOf() 可直接用 API_BASE + image 拼接展示。
 */
@RestController
@RequestMapping("/api/manuals")
public class UploadController {

    private static final long MAX_SIZE = 10L * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未选择图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 jpg / png / gif / webp 图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片体积不得超过 10MB");
        }

        String original = file.getOriginalFilename();
        String ext = "jpg";
        if (original != null && original.contains(".")) {
            String e = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
            if (ALLOWED_EXT.contains(e)) {
                ext = e.equals("jpeg") ? "jpg" : e;
            }
        }
        String fileName = "m_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        try {
            Files.createDirectories(UploadPaths.DIR);
            file.transferTo(UploadPaths.DIR.resolve(fileName).toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败：" + e.getMessage());
        }

        Map<String, String> resp = new LinkedHashMap<>(1);
        resp.put("url", UploadPaths.URL_PREFIX + "/" + fileName);
        return resp;
    }
}
