package com.greenrobot.monitor.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 对象存储服务（MinIO/S3兼容）
 * 
 * 封装文件上传、下载、删除、URL生成等操作
 * 支持内网 MinIO 对象存储
 * 
 * @author 狼群团队
 * @version 4.1.0
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OssService {
    
    /** S3客户端 */
    private S3Client s3Client;
    
    /** 存储桶名称 */
    @Value("${oss.bucket:green-robot-captures}")
    private String bucketName;
    
    /** MinIO endpoint */
    @Value("${oss.endpoint:http://localhost:9000}")
    private String endpoint;
    
    /** Access Key */
    @Value("${oss.access-key:admin}")
    private String accessKey;
    
    /** Secret Key */
    @Value("${oss.secret-key:greenrobot123}")
    private String secretKey;
    
    /** 是否启用OSS */
    @Value("${oss.enabled:true}")
    private boolean ossEnabled;
    
    /** 本地文件存储目录（备用） */
    @Value("${oss.local-dir:../data/captures}")
    private String localDir;
    
    /** 日期格式化器 */
    private static final DateTimeFormatter FILE_DATE_FORMAT = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 初始化S3客户端
     */
    @PostConstruct
    public void init() {
        if (!ossEnabled) {
            log.info("OSS功能已禁用，使用本地文件存储");
            return;
        }
        
        try {
            log.info("========== 初始化 OSS 客户端 ==========");
            log.info("Endpoint: {}", endpoint);
            log.info("Bucket: {}", bucketName);
            
            // 创建 S3 客户端（MinIO 兼容）
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    accessKey, 
                    secretKey
            );
            
            s3Client = S3Client.builder()
                    .endpointOverride(java.net.URI.create(endpoint))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(true)  // MinIO 需要这个
                    .build();
            
            // 验证连接
            try {
                s3Client.headBucket(HeadBucketRequest.builder()
                        .bucket(bucketName)
                        .build());
                log.info("OSS 连接成功！Bucket: {}", bucketName);
            } catch (NoSuchBucketException e) {
                log.warn("Bucket 不存在，创建中: {}", bucketName);
                s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build());
            }
            
            log.info("========== OSS 初始化完成 ==========");
            
        } catch (Exception e) {
            log.error("OSS 初始化失败: {}", e.getMessage(), e);
            log.warn("降级到本地文件存储模式");
            ossEnabled = false;
        }
    }
    
    /**
     * 关闭S3客户端
     */
    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
            log.info("OSS 客户端已关闭");
        }
    }
    
    /**
     * 上传文件
     * 
     * @param key 对象存储中的路径 (如: captures/20260430_123456.jpg)
     * @param file 要上传的文件
     * @return 上传后的访问URL
     */
    public String uploadFile(String key, File file) throws IOException {
        if (ossEnabled && s3Client != null) {
            return uploadToS3(key, file);
        } else {
            return saveLocally(key, file);
        }
    }
    
    /**
     * 上传字节数组
     * 
     * @param key 对象存储中的路径
     * @param data 字节数据
     * @return 上传后的访问URL
     */
    public String uploadBytes(String key, byte[] data) throws IOException {
        if (ossEnabled && s3Client != null) {
            return uploadBytesToS3(key, data);
        } else {
            return saveBytesLocally(key, data);
        }
    }
    
    /**
     * 上传到S3 (MinIO)
     */
    private String uploadToS3(String key, File file) throws IOException {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    RequestBody.fromFile(file)
            );
            
            log.debug("上传成功: {} ({} bytes)", key, file.length());
            return generatePresignedUrl(key);
            
        } catch (Exception e) {
            log.error("S3上传失败: {} - {}", key, e.getMessage());
            throw new IOException("S3上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 上传字节数组到S3
     */
    private String uploadBytesToS3(String key, byte[] data) throws IOException {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(detectContentType(key))
                            .build(),
                    RequestBody.fromBytes(data)
            );
            
            log.debug("上传成功: {} ({} bytes)", key, data.length);
            return generatePresignedUrl(key);
            
        } catch (Exception e) {
            log.error("S3上传失败: {} - {}", key, e.getMessage());
            throw new IOException("S3上传失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 下载文件到本地
     * 
     * @param key 对象存储中的路径
     * @return 下载的字节数组
     */
    public byte[] downloadFile(String key) throws IOException {
        if (ossEnabled && s3Client != null) {
            return downloadFromS3(key);
        } else {
            return readLocalFile(key);
        }
    }
    
    /**
     * 从S3下载
     */
    private byte[] downloadFromS3(String key) throws IOException {
        try {
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
            
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = s3Object.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            
            log.debug("下载成功: {} ({} bytes)", key, buffer.size());
            return buffer.toByteArray();
            
        } catch (Exception e) {
            log.error("S3下载失败: {} - {}", key, e.getMessage());
            throw new IOException("S3下载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 删除文件
     * 
     * @param key 对象存储中的路径
     */
    public void deleteFile(String key) {
        if (ossEnabled && s3Client != null) {
            deleteFromS3(key);
        } else {
            deleteLocalFile(key);
        }
    }
    
    /**
     * 从S3删除
     */
    private void deleteFromS3(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
            );
            log.debug("删除成功: {}", key);
        } catch (Exception e) {
            log.error("S3删除失败: {} - {}", key, e.getMessage());
        }
    }
    
    /**
     * 生成预签名URL（用于公开访问）
     * 
     * @param key 对象存储中的路径
     * @return 预签名访问URL
     */
    public String generatePresignedUrl(String key) {
        if (ossEnabled && s3Client != null) {
            try {
                String url = s3Client.utilities()
                        .getUrl(GetUrlRequest.builder()
                                .bucket(bucketName)
                                .key(key)
                                .build())
                        .toString();
                return url;
            } catch (Exception e) {
                log.error("生成URL失败: {} - {}", key, e.getMessage());
            }
        }
        // 降级到本地路径
        return "/api/files/" + key;
    }
    
    /**
     * 生成带过期时间的预签名URL
     * 
     * @param key 对象存储中的路径
     * @param expirationMinutes 过期时间（分钟）
     * @return 预签名访问URL
     */
    public String generatePresignedUrl(String key, int expirationMinutes) {
        if (ossEnabled && s3Client != null) {
            try {
                // MinIO 直接返回内网地址
                return endpoint + "/" + bucketName + "/" + key;
            } catch (Exception e) {
                log.error("生成URL失败: {} - {}", key, e.getMessage());
            }
        }
        return "/api/files/" + key;
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param key 对象存储中的路径
     * @return 是否存在
     */
    public boolean exists(String key) {
        if (ossEnabled && s3Client != null) {
            try {
                s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
                return true;
            } catch (NoSuchKeyException e) {
                return false;
            } catch (Exception e) {
                log.error("检查文件存在失败: {} - {}", key, e.getMessage());
                return false;
            }
        } else {
            File localFile = new File(localDir, key);
            return localFile.exists();
        }
    }
    
    /**
     * 获取文件列表
     * 
     * @param prefix 前缀路径
     * @return 文件key列表
     */
    public java.util.List<String> listFiles(String prefix) {
        java.util.List<String> files = new java.util.ArrayList<>();
        
        if (ossEnabled && s3Client != null) {
            try {
                ListObjectsV2Response response = s3Client.listObjectsV2(
                        ListObjectsV2Request.builder()
                                .bucket(bucketName)
                                .prefix(prefix)
                                .build()
                );
                
                for (S3Object obj : response.contents()) {
                    files.add(obj.key());
                }
            } catch (Exception e) {
                log.error("列出文件失败: {} - {}", prefix, e.getMessage());
            }
        } else {
            File dir = new File(localDir, prefix);
            if (dir.exists() && dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    if (f.isFile()) {
                        files.add(f.getName());
                    }
                }
            }
        }
        
        return files;
    }
    
    // ========== 本地文件存储备用方法 ==========
    
    /**
     * 保存到本地
     */
    private String saveLocally(String key, File file) throws IOException {
        File localFile = new File(localDir, key);
        localFile.getParentFile().mkdirs();
        
        try (FileInputStream fis = new FileInputStream(file);
             FileOutputStream fos = new FileOutputStream(localFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
        
        log.debug("本地保存成功: {}", localFile.getAbsolutePath());
        return "/api/files/" + key;
    }
    
    /**
     * 保存字节到本地
     */
    private String saveBytesLocally(String key, byte[] data) throws IOException {
        File localFile = new File(localDir, key);
        localFile.getParentFile().mkdirs();
        
        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            fos.write(data);
        }
        
        log.debug("本地保存成功: {}", localFile.getAbsolutePath());
        return "/api/files/" + key;
    }
    
    /**
     * 读取本地文件
     */
    private byte[] readLocalFile(String key) throws IOException {
        File localFile = new File(localDir, key);
        try (FileInputStream fis = new FileInputStream(localFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
    
    /**
     * 删除本地文件
     */
    private void deleteLocalFile(String key) {
        File localFile = new File(localDir, key);
        if (localFile.exists()) {
            localFile.delete();
            log.debug("本地删除成功: {}", localFile.getAbsolutePath());
        }
    }
    
    /**
     * 根据文件扩展名检测Content-Type
     */
    private String detectContentType(String key) {
        String lowerKey = key.toLowerCase();
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerKey.endsWith(".png")) {
            return "image/png";
        } else if (lowerKey.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerKey.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerKey.endsWith(".avi")) {
            return "video/x-msvideo";
        }
        return "application/octet-stream";
    }
    
    /**
     * OSS是否启用
     */
    public boolean isOssEnabled() {
        return ossEnabled && s3Client != null;
    }
    
    /**
     * 获取Bucket名称
     */
    public String getBucketName() {
        return bucketName;
    }
}
