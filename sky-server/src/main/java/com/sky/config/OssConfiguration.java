package com.sky.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 配置类，用于创建AliOssUtil对象
 */
@Configuration
@Slf4j
public class OssConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){
        log.info("开始创建阿里云文件上传工具类对象");
        log.info("OSS配置信息 - endpoint: {}, bucketName: {}, accessKeyId: {}", 
                aliOssProperties.getEndpoint(), 
                aliOssProperties.getBucketName(),
                aliOssProperties.getAccessKeyId() != null ? aliOssProperties.getAccessKeyId().substring(0, 3) + "******" : "null");
                
        if (aliOssProperties.getEndpoint() == null || aliOssProperties.getBucketName() == null ||
            aliOssProperties.getAccessKeyId() == null || aliOssProperties.getAccessKeySecret() == null) {
            log.error("OSS配置信息不完整，请检查application-dev.yml中的配置");
        }
        
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(), 
                aliOssProperties.getAccessKeySecret(), 
                aliOssProperties.getBucketName()
        );
    }
}
