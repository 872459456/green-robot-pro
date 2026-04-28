package com.greenrobot.monitor.controller;

import com.greenrobot.monitor.dto.CaptureResultDTO;
import com.greenrobot.monitor.dto.MonitorStatusDTO;
import com.greenrobot.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * 监控REST API控制器
 * 
 * 提供摄像头控制、图像拍摄相关接口
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MonitorController {
    
    private final MonitorService monitorService;
    
    /**
     * 获取监控系统状态
     * 
     * GET /api/monitor/status
     * 
     * @return 监控状态
     */
    @GetMapping("/status")
    public ResponseEntity<MonitorStatusDTO> getStatus() {
        MonitorStatusDTO status = monitorService.getStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 触发一次拍摄
     * 
     * POST /api/monitor/capture
     * 
     * @param force 是否强制拍摄（忽略模糊检测）
     * @return 拍摄结果
     */
    @PostMapping("/capture")
    public ResponseEntity<CaptureResultDTO> capture(
            @RequestParam(defaultValue = "false") boolean force) {
        CaptureResultDTO result = monitorService.capture(force);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 获取最新拍摄图片
     * 
     * GET /api/monitor/latest
     * 
     * 返回最新的JPEG图片
     */
    @GetMapping(value = "/latest", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getLatestImage() {
        File imageFile = monitorService.getLatestImage();
        
        if (imageFile == null || !imageFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            // 读取图片文件
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 转换为JPEG字节数组
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取视频流（MJPEG格式）
     * 
     * GET /api/monitor/stream
     * 
     * 返回Motion-JPEG格式的视频流
     * 
     * 注意：由于Java实现复杂，目前返回静态图片
     * 后续版本将实现真正的MJPEG流
     */
    @GetMapping(value = "/stream", produces = "multipart/x-mixed-replace")
    public ResponseEntity<byte[]> getStream() {
        File imageFile = monitorService.getLatestImage();
        
        if (imageFile == null || !imageFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
