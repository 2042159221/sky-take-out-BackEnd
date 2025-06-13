package com.sky.controller.admin;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Tag(name = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;
    
    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}", file);
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null) {
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String objectName = UUID.randomUUID().toString() + extension;
                
                // 1. 先上传文件
                String filePath = aliOssUtil.upload(file.getBytes(), objectName);
                
                // 检查返回的filePath是否为null，如果是null说明上传失败
                if (filePath == null) {
                    log.error("文件上传失败：filePath为null");
                    return Result.error(MessageConstant.UPLOAD_FAILED);
                }
                
                // 2. 直接返回标准URL (后续如果出现预览问题可替换为下面的方式)
                return Result.success(filePath);
                
                // 如果需要返回预览URL:
                // String previewUrl = aliOssUtil.generatePreviewUrl(objectName);
                // return Result.success(previewUrl);
            } else {
                return Result.error(MessageConstant.UPLOAD_FAILED);
            }
        } catch (IOException e) {
            log.error("文件读取失败：{}", e.getMessage());
            return Result.error(MessageConstant.UPLOAD_FAILED);
        } catch (RuntimeException e) {
            log.error("OSS上传失败：{}", e.getMessage());
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }
    }

}
