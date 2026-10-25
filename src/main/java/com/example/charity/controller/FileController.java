package com.example.charity.controller;

import com.example.charity.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        try {
            // 1. 确保目录存在
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 2. 生成唯一文件名 (防止重名覆盖)
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + suffix;

            // 3. 保存文件
            File dest = new File(uploadPath + newFileName);
            file.transferTo(dest);

            // 4. 返回相对路径（注意 /images/ 与 WebMvcConfig 的映射一致）
            //    不要写死 http://localhost:8080，否则换域名/IP 部署后图片全废；
            //    前端同源请求 /images/**，由 nginx 反代到后端（开发环境由 vite proxy 转发）
            String url = "/images/" + newFileName;
            return Result.success(url);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败：" + e.getMessage());
        }
    }
}