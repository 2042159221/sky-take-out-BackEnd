package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        // 标记上传是否成功
        boolean uploadSuccess = false;

        try {
            log.info("开始上传文件到OSS，bucket:{}, objectName:{}, endpoint:{}", bucketName, objectName, endpoint);
            
            // 创建上传Object的元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            
            // 根据文件后缀设置contentType
            String suffix = objectName.substring(objectName.lastIndexOf(".")).toLowerCase();
            if (suffix.equals(".jpg") || suffix.equals(".jpeg")) {
                metadata.setContentType("image/jpeg");
            } else if (suffix.equals(".png")) {
                metadata.setContentType("image/png");
            } else if (suffix.equals(".gif")) {
                metadata.setContentType("image/gif");
            } else if (suffix.equals(".bmp")) {
                metadata.setContentType("image/bmp");
            } else if (suffix.equals(".webp")) {
                metadata.setContentType("image/webp");
            } else {
                metadata.setContentType("application/octet-stream");
            }
            
            // 设置Content-Disposition为inline
            metadata.setContentDisposition("inline");
            
            // 上传文件并设置元数据
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes), metadata);
            
            // 上传完成后，设置文件的访问权限为公共读
            ossClient.setObjectAcl(bucketName, objectName, CannedAccessControlList.PublicRead);
            
            uploadSuccess = true;
            log.info("文件上传成功并设置为公共读取权限");
        } catch (OSSException oe) {
            log.error("OSS服务异常，上传文件失败: ErrorMessage:{}, ErrorCode:{}, RequestId:{}, HostId:{}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            throw new RuntimeException("OSS服务异常，上传文件失败", oe);
        } catch (ClientException ce) {
            log.error("OSS客户端异常，上传文件失败: ErrorMessage:{}", ce.getMessage());
            throw new RuntimeException("OSS客户端异常，上传文件失败", ce);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // 只有上传成功才生成并返回URL
        if (uploadSuccess) {
            // 阿里云OSS文件访问路径格式: https://BucketName.Endpoint/ObjectName
            // 注意：endpoint中可能已经包含了https://，所以需要处理一下
            String ossEndpoint = endpoint;
            if (ossEndpoint.startsWith("https://")) {
                ossEndpoint = ossEndpoint.substring(8);
            } else if (ossEndpoint.startsWith("http://")) {
                ossEndpoint = ossEndpoint.substring(7);
            }
            
            StringBuilder stringBuilder = new StringBuilder("https://");
            stringBuilder
                    .append(bucketName)
                    .append(".")
                    .append(ossEndpoint)
                    .append("/")
                    .append(objectName);
    
            log.info("文件访问URL:{}", stringBuilder.toString());
            return stringBuilder.toString();
        } else {
            return null;
        }
    }
    
    /**
     * 生成可在浏览器中预览的URL（带有适当的Content-Disposition头）
     * @param objectName
     * @return
     */
    public String generatePreviewUrl(String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        try {
            // 设置URL过期时间为1小时
            Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
            
            // 生成签名URL请求
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName);
            request.setExpiration(expiration);
            
            // 设置响应头
            ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
            responseHeaders.setContentDisposition("inline");
            
            // 根据文件后缀设置contentType
            String suffix = objectName.substring(objectName.lastIndexOf(".")).toLowerCase();
            if (suffix.equals(".jpg") || suffix.equals(".jpeg")) {
                responseHeaders.setContentType("image/jpeg");
            } else if (suffix.equals(".png")) {
                responseHeaders.setContentType("image/png");
            } else if (suffix.equals(".gif")) {
                responseHeaders.setContentType("image/gif");
            } else if (suffix.equals(".bmp")) {
                responseHeaders.setContentType("image/bmp");
            } else if (suffix.equals(".webp")) {
                responseHeaders.setContentType("image/webp");
            }
            
            request.setResponseHeaders(responseHeaders);
            
            // 生成签名URL
            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            log.error("生成预览URL失败: {}", e.getMessage());
            throw new RuntimeException("生成预览URL失败", e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
