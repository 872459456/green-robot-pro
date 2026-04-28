package com.greenrobot.monitor.controller;

import com.greenrobot.monitor.dto.DetectionResponseDTO;
import com.greenrobot.monitor.dto.ModelStatusDTO;
import com.greenrobot.monitor.service.YoloDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * YOLO目标检测REST API控制器
 * 
 * 提供叶片检测相关的API接口
 * 
 * @author 狼群团队
 * @version 4.0.0
 * @since 2026-04-28
 */
@RestController
@RequestMapping("/detect")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DetectionController {
    
    /** YOLO检测服务 */
    private final YoloDetectionService yoloService;
    
    /**
     * 执行目标检测
     * 
     * POST /api/detect
     * 
     * 对指定图片进行YOLO检测
     * 
     * @param imagePath 图片路径（相对或绝对路径）
     * @return 检测结果
     */
    @PostMapping
    public ResponseEntity<DetectionResponseDTO> detect(
            @RequestParam String imagePath) {
        
        log.info("收到检测请求: imagePath={}", imagePath);
        
        // 处理相对路径
        String fullPath = resolveImagePath(imagePath);
        
        DetectionResponseDTO result = yoloService.detect(fullPath);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 获取模型状态
     * 
     * GET /api/detect/status
     * 
     * @return 模型当前状态
     */
    @GetMapping("/status")
    public ResponseEntity<ModelStatusDTO> getStatus() {
        ModelStatusDTO status = yoloService.getStatus();
        return ResponseEntity.ok(status);
    }
    
    /**
     * 批量检测
     * 
     * POST /api/detect/batch
     * 
     * 对多个图片进行批量检测
     * 
     * @param imagePaths 图片路径列表（逗号分隔）
     * @return 批量检测结果
     */
    @PostMapping("/batch")
    public ResponseEntity<?> detectBatch(
            @RequestParam String imagePaths) {
        
        log.info("收到批量检测请求: {}", imagePaths);
        
        String[] paths = imagePaths.split(",");
        StringBuilder results = new StringBuilder();
        
        for (String path : paths) {
            String trimmedPath = path.trim();
            String fullPath = resolveImagePath(trimmedPath);
            
            DetectionResponseDTO result = yoloService.detect(fullPath);
            
            results.append(trimmedPath).append(": ");
            if (result.isSuccess()) {
                results.append(result.getTotalDetections()).append(" detections, ");
                results.append(String.format("%.2fms", result.getInferenceTimeMs()));
            } else {
                results.append("ERROR: ").append(result.getErrorMessage());
            }
            results.append("\n");
        }
        
        return ResponseEntity.ok(results.toString());
    }
    
    /**
     * 检测并返回带标注的图片
     * 
     * POST /api/detect/annotate
     * 
     * 对图片进行检测并在图片上绘制检测框后返回
     * 
     * @param imagePath 图片路径
     * @return 带标注的图片
     */
    @PostMapping(value = "/annotate", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> detectAndAnnotate(
            @RequestParam String imagePath) {
        
        String fullPath = resolveImagePath(imagePath);
        
        try {
            // 执行检测
            DetectionResponseDTO result = yoloService.detect(fullPath);
            
            if (!result.isSuccess()) {
                return ResponseEntity.badRequest().build();
            }
            
            // 读取原图
            File imageFile = new File(fullPath);
            BufferedImage image = ImageIO.read(imageFile);
            
            if (image == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 在图片上绘制检测框
            BufferedImage annotated = drawDetections(image, result);
            
            // 转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(annotated, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imageBytes);
                    
        } catch (Exception e) {
            log.error("标注图片失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 在图片上绘制检测框
     * 
     * @param image 原图
     * @param result 检测结果
     * @return 带标注的图片
     */
    private BufferedImage drawDetections(BufferedImage image, DetectionResponseDTO result) {
        BufferedImage output = new BufferedImage(
                image.getWidth(), 
                image.getHeight(), 
                BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        
        // 设置绘制样式
        g2d.setStroke(new BasicStroke(3));
        
        // 遍历检测结果绘制框
        for (var detection : result.getDetections()) {
            // 根据类别选择颜色
            Color color = getColorForClass(detection.getClassName());
            g2d.setColor(color);
            
            // 绘制矩形框
            int x = (int) detection.getX();
            int y = (int) detection.getY();
            int w = (int) detection.getWidth();
            int h = (int) detection.getHeight();
            
            g2d.drawRect(x, y, w, h);
            
            // 绘制标签
            String label = String.format("%s %.2f", 
                    detection.getClassName(), 
                    detection.getConfidence());
            
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            
            // 绘制背景
            g2d.drawString(label, x, y - 5);
        }
        
        g2d.dispose();
        
        return output;
    }
    
    /**
     * 根据类别获取颜色
     */
    private Color getColorForClass(String className) {
        return switch (className) {
            case "healthy_leaf" -> new Color(82, 196, 26);  // 绿色
            case "yellow_leaf" -> new Color(250, 173, 20);   // 黄色
            case "wilt_leaf" -> new Color(255, 77, 79);      // 红色
            default -> new Color(24, 144, 255);              // 蓝色
        };
    }
    
    /**
     * 解析图片路径
     * 
     * 处理相对路径，转换为绝对路径
     * 
     * @param imagePath 输入路径
     * @return 绝对路径
     */
    private String resolveImagePath(String imagePath) {
        if (imagePath.startsWith("/") || imagePath.matches("^[A-Za-z]:.*")) {
            // 已经是绝对路径
            return imagePath;
        }
        
        // 相对路径，转换为绝对路径
        // 假设相对于 data/captures 目录
        String basePath = System.getProperty("user.dir");
        if (imagePath.startsWith("data/captures/")) {
            return basePath + File.separator + imagePath;
        }
        
        return basePath + File.separator + imagePath;
    }
}