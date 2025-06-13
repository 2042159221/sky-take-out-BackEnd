package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Component
@ConfigurationProperties(prefix = "sky.alioss")
@Data
@Slf4j
public class AliOssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    
    @PostConstruct
    public void validateProperties() {
        log.info("验证OSS配置信息");
        if (endpoint == null || endpoint.isEmpty()) {
            log.error("OSS配置错误: endpoint为空");
        }
        if (accessKeyId == null || accessKeyId.isEmpty()) {
            log.error("OSS配置错误: accessKeyId为空");
        }
        if (accessKeySecret == null || accessKeySecret.isEmpty()) {
            log.error("OSS配置错误: accessKeySecret为空");
        }
        if (bucketName == null || bucketName.isEmpty()) {
            log.error("OSS配置错误: bucketName为空");
        }
        
        log.info("OSS配置验证完成 - endpoint: {}, bucketName: {}, accessKeyId: {}",
                endpoint,
                bucketName,
                accessKeyId != null ? accessKeyId.substring(0, Math.min(3, accessKeyId.length())) + "******" : "null");
    }
}
