package com.greenrobot.monitor.service;

import com.greenrobot.monitor.dto.CaptureResultDTO;
import com.greenrobot.monitor.dto.MonitorStatusDTO;
import com.greenrobot.monitor.entity.ConfirmedLeaf;
import com.greenrobot.monitor.entity.LeafObservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 监控服务
 * 
 * 负责摄像头控制、图像拍摄、叶片检测
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@Slf4j
@Service
public class MonitorService {
    
    /** 摄像头索引，默认0 */
    @Value("${camera.index:0}")
    private int cameraIndex;
    
    /** 拍摄图片存储目录 */
    @Value("${camera.image-dir:../data/captures}")
    private String imageDir;
    
    /** 拍摄是否启用 */
    @Value("${camera.enabled:true}")
    private boolean cameraEnabled;
    
    /** 今日拍摄计数器 */
    private final AtomicInteger todayCaptureCount = new AtomicInteger(0);
    
    /** 最后拍摄时间 */
    private LocalDateTime lastCaptureTime;
    
    /** 最后拍摄路径 */
    private String lastCapturePath;
    
    /** 标注图路径 */
    private String lastAnnotatedPath;
    
    /** 监控系统运行状态 */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    /** 日期格式化器 */
    private static final DateTimeFormatter FILE_DATE_FORMAT = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 获取监控系统状态
     * 
     * @return 监控状态DTO
     */
    public MonitorStatusDTO getStatus() {
        // 检查是否在线
        if (!cameraEnabled) {
            return MonitorStatusDTO.offline("摄像头功能未启用");
        }
        
        // 检查是否有模拟数据或真实摄像头
        if (lastCapturePath != null) {
            // 有过成功拍摄，认为在线
            return MonitorStatusDTO.online(
                    cameraIndex,
                    lastCaptureTime,
                    todayCaptureCount.get()
            );
        } else {
            // 首次启动，返回就绪状态
            MonitorStatusDTO dto = new MonitorStatusDTO();
            dto.setOnline(true);
            dto.setCameraIndex(cameraIndex);
            dto.setStatus("READY");
            dto.setMessage("监控系统就绪，等待首次拍摄");
            return dto;
        }
    }
    
    /**
     * 触发一次拍摄
     * 
     * 拍摄流程：
     * 1. 从摄像头捕获一帧
     * 2. 检测是否模糊
     * 3. 执行叶片检测
     * 4. 生成标注图
     * 5. 存储结果
     * 
     * @param force 强制拍摄（忽略模糊检测）
     * @return 拍摄结果
     */
    public CaptureResultDTO capture(boolean force) {
        log.info("开始拍摄，force={}", force);
        
        try {
            // ========== 1. 模拟摄像头捕获 ==========
            // 由于Java OpenCV集成复杂，目前使用模拟数据
            // 后续版本将集成真实的OpenCV摄像头捕获
            
            BufferedImage capturedImage = createSimulatedImage();
            
            if (capturedImage == null) {
                return CaptureResultDTO.failure("摄像头捕获失败");
            }
            
            // ========== 2. 模糊检测 ==========
            if (!force && isBlur(capturedImage)) {
                return CaptureResultDTO.failure("图片模糊，已自动丢弃");
            }
            
            // ========== 3. 生成文件名 ==========
            String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
            String imagePath = saveImage(capturedImage, timestamp, false);
            String annotatedPath = saveImage(capturedImage, timestamp, true);
            
            // ========== 4. 更新状态 ==========
            lastCaptureTime = LocalDateTime.now();
            lastCapturePath = imagePath;
            lastAnnotatedPath = annotatedPath;
            todayCaptureCount.incrementAndGet();
            isRunning.set(true);
            
            // ========== 5. 计算覆盖率（模拟） ==========
            Double greenCoverage = calculateGreenCoverage(capturedImage);
            
            // ========== 6. 检测叶片（模拟） ==========
            int leafCount = 3; // 模拟检测到3片
            int newLeafCount = 0;
            int healthyCount = 2;
            int attentionCount = 1;
            
            log.info("拍摄成功: {}, 叶片数: {}", imagePath, leafCount);
            
            return CaptureResultDTO.success(
                    imagePath,
                    annotatedPath,
                    leafCount,
                    newLeafCount,
                    false,
                    greenCoverage,
                    healthyCount,
                    attentionCount
            );
            
        } catch (Exception e) {
            log.error("拍摄异常", e);
            return CaptureResultDTO.failure("拍摄异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取最新拍摄图片
     * 
     * @return 最新图片文件
     */
    public File getLatestImage() {
        if (lastCapturePath == null) {
            return null;
        }
        File file = new File(lastCapturePath);
        return file.exists() ? file : null;
    }
    
    /**
     * 创建模拟图像
     * 
     * 用于测试和演示
     * 实际部署时将替换为真实摄像头捕获
     * 
     * @return 模拟的 BufferedImage
     */
    private BufferedImage createSimulatedImage() {
        try {
            // 创建一个640x480的绿色图像模拟绿萝
            int width = 640;
            int height = 480;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            Graphics2D g2d = image.createGraphics();
            
            // 填充浅绿色背景（模拟绿萝叶子）
            g2d.setColor(new Color(120, 180, 80));
            g2d.fillRect(0, 0, width, height);
            
            // 添加一些深绿色斑块模拟叶子
            g2d.setColor(new Color(60, 140, 40));
            for (int i = 0; i < 10; i++) {
                int x = (int) (Math.random() * width);
                int y = (int) (Math.random() * height);
                int w = 50 + (int) (Math.random() * 100);
                int h = 30 + (int) (Math.random() * 80);
                g2d.fillOval(x, y, w, h);
            }
            
            // 添加一些棕色斑块模拟枯萎
            g2d.setColor(new Color(180, 140, 80));
            for (int i = 0; i < 3; i++) {
                int x = (int) (Math.random() * width);
                int y = (int) (Math.random() * height);
                int w = 20 + (int) (Math.random() * 40);
                int h = 15 + (int) (Math.random() * 30);
                g2d.fillOval(x, y, w, h);
            }
            
            g2d.dispose();
            return image;
            
        } catch (Exception e) {
            log.error("创建模拟图像失败", e);
            return null;
        }
    }
    
    /**
     * 检测图片是否模糊
     * 
     * 使用简单的方差法检测
     * 
     * @param image 输入图片
     * @return true表示模糊
     */
    private boolean isBlur(BufferedImage image) {
        // 简化实现：检测图像边缘是否足够清晰
        // 后续将使用拉普拉斯方差法
        return false;
    }
    
    /**
     * 计算绿色覆盖率
     * 
     * 统计图像中绿色像素的比例
     * 
     * @param image 输入图像
     * @return 覆盖率 (0-100)
     */
    private Double calculateGreenCoverage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int greenCount = 0;
        int total = width * height;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb);
                
                // 简单判断是否为绿色（基于RGB）
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                
                if (g > r && g > b && g > 100) {
                    greenCount++;
                }
            }
        }
        
        return (greenCount * 100.0) / total;
    }
    
    /**
     * 保存图片到文件
     * 
     * @param image 图片
     * @param timestamp 时间戳
     * @param annotated 是否为标注图
     * @return 保存的文件路径
     */
    private String saveImage(BufferedImage image, String timestamp, boolean annotated) {
        try {
            // 确保目录存在
            File dir = new File(imageDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 构建文件名
            String prefix = annotated ? "annotated_" : "capture_";
            String filename = prefix + timestamp + ".jpg";
            File file = new File(dir, filename);
            
            // 绘制标注信息
            if (annotated) {
                Graphics2D g2d = image.createGraphics();
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(3));
                
                // 模拟标注框
                g2d.drawRect(100, 100, 150, 120);
                g2d.drawRect(300, 200, 180, 140);
                g2d.drawRect(450, 100, 120, 100);
                
                // 添加标签
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("L0001", 105, 95);
                g2d.drawString("L0002", 305, 195);
                g2d.drawString("L0003", 455, 95);
                
                g2d.dispose();
            }
            
            // 保存为JPEG
            ImageIO.write(image, "jpg", file);
            
            log.info("图片保存成功: {}", file.getAbsolutePath());
            return file.getAbsolutePath();
            
        } catch (IOException e) {
            log.error("图片保存失败", e);
            return null;
        }
    }
    
    /**
     * 执行叶片检测
     * 
     * TODO: 集成 OpenCV 进行真实叶片检测
     * 
     * @param image 输入图像
     * @return 检测结果列表
     */
    private List<LeafDetectionService.DetectionResult> detectLeaves(BufferedImage image) {
        // TODO: 实现真实的叶片检测
        // 目前返回空列表，模拟检测结果
        return List.of();
    }
}
