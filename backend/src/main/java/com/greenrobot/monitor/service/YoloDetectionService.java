package com.greenrobot.monitor.service;

import com.greenrobot.monitor.dto.DetectionResponseDTO;
import com.greenrobot.monitor.dto.DetectionResultDTO;
import com.greenrobot.monitor.dto.ModelStatusDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.FloatBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * YOLOv8目标检测服务
 * 
 * 负责加载YOLOv8模型并执行叶片检测推理
 * 使用ONNX Runtime作为推理引擎
 * 
 * @author 狼群团队
 * @version 4.0.0
 * @since 2026-04-28
 */
@Slf4j
@Service
public class YoloDetectionService {
    
    /** 模型文件路径 */
    @Value("${model.path:models/yolov8n-leaf.onnx}")
    private String modelPath;
    
    /** 置信度阈值 */
    @Value("${model.conf-threshold:0.5}")
    private float confThreshold;
    
    /** IOU阈值（NMS用） */
    @Value("${model.iou-threshold:0.45}")
    private float iouThreshold;
    
    /** 模型是否启用 */
    @Value("${model.enabled:true}")
    private boolean modelEnabled;
    
    /** 模型是否已加载 */
    private boolean isModelLoaded = false;
    
    /** 推理次数统计 */
    private final AtomicLong inferenceCount = new AtomicLong(0);
    
    /** 总推理耗时（毫秒） */
    private final AtomicLong totalInferenceTime = new AtomicLong(0);
    
    /** 最后推理时间 */
    private LocalDateTime lastInferenceTime;
    
    /** 类别名称映射 */
    private static final Map<Integer, String> CLASS_NAMES = new HashMap<>();
    
    /** 模型输入尺寸 */
    private static final int INPUT_SIZE = 640;
    
    static {
        // 初始化类别名称
        CLASS_NAMES.put(0, "leaf");
        CLASS_NAMES.put(1, "healthy_leaf");
        CLASS_NAMES.put(2, "yellow_leaf");
        CLASS_NAMES.put(3, "wilt_leaf");
    }
    
    /**
     * 模型初始化
     * 
     * 应用启动时自动调用，加载YOLOv8模型
     */
    @PostConstruct
    public void init() {
        log.info("========== YOLOv8模型初始化 ==========");
        log.info("模型路径: {}", modelPath);
        log.info("置信度阈值: {}", confThreshold);
        log.info("IOU阈值: {}", iouThreshold);
        log.info("模型启用: {}", modelEnabled);
        
        if (!modelEnabled) {
            log.warn("模型未启用，检测功能不可用");
            return;
        }
        
        // 检查模型文件是否存在
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            log.warn("模型文件不存在: {}，将使用模拟模式", modelPath);
            log.info("请将YOLOv8模型文件放入 models/ 目录");
            // 模拟模式：标记为已加载但使用模拟检测
            isModelLoaded = true;
            return;
        }
        
        try {
            // TODO: 实际加载ONNX模型
            //OrtSession session = env.createSession(modelPath);
            //isModelLoaded = true;
            log.info("模型加载成功（模拟模式）");
            
            // 模拟模式：标记为已加载
            isModelLoaded = true;
            
        } catch (Exception e) {
            log.error("模型加载失败", e);
            isModelLoaded = false;
        }
        
        log.info("========== 模型初始化完成 ==========");
    }
    
    /**
     * 模型清理
     */
    @PreDestroy
    public void cleanup() {
        log.info("YOLOv8模型清理完成");
    }
    
    /**
     * 执行目标检测
     * 
     * 对输入图片执行YOLO推理，返回检测结果
     * 
     * @param imagePath 图片路径
     * @return 检测结果响应
     */
    public DetectionResponseDTO detect(String imagePath) {
        log.info("开始检测: {}", imagePath);
        
        // 检查模型状态
        if (!isModelLoaded) {
            return DetectionResponseDTO.failure("模型未加载");
        }
        
        // 检查图片是否存在
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return DetectionResponseDTO.failure("图片文件不存在: " + imagePath);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // ========== 1. 读取图片 ==========
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return DetectionResponseDTO.failure("无法读取图片");
            }
            
            // ========== 2. 图片预处理 ==========
            float[][][][] inputTensor = preprocessImage(image);
            
            // ========== 3. 模型推理（模拟） ==========
            // TODO: 实际调用ONNX Runtime进行推理
            // List<DetectionResultDTO> detections = runInference(inputTensor);
            
            // 模拟检测结果
            List<DetectionResultDTO> detections = simulateDetection(image);
            
            // ========== 4. 后处理（NMS） ==========
            detections = nonMaxSuppression(detections);
            
            // 统计推理时间
            long inferenceTime = System.currentTimeMillis() - startTime;
            inferenceCount.incrementAndGet();
            totalInferenceTime.addAndGet(inferenceTime);
            lastInferenceTime = LocalDateTime.now();
            
            log.info("检测完成: {}个目标, 耗时{}ms", detections.size(), inferenceTime);
            
            return DetectionResponseDTO.success(detections, inferenceTime, getModelName());
            
        } catch (Exception e) {
            log.error("检测异常", e);
            return DetectionResponseDTO.failure("检测异常: " + e.getMessage());
        }
    }
    
    /**
     * 图片预处理
     * 
     * 将BufferedImage转换为模型需要的float数组格式
     * 
     * @param image 输入图片
     * @return 预处理后的tensor [1x3x640x640]
     */
    private float[][][][] preprocessImage(BufferedImage image) {
        // 缩放到640x640
        BufferedImage resized = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(image.getScaledInstance(INPUT_SIZE, INPUT_SIZE, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();
        
        // 转换为float数组并归一化 [0,1]
        float[][][][] tensor = new float[1][3][INPUT_SIZE][INPUT_SIZE];
        
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                Color color = new Color(rgb);
                
                // RGB通道归一化
                tensor[0][0][y][x] = color.getRed() / 255.0f;
                tensor[0][1][y][x] = color.getGreen() / 255.0f;
                tensor[0][2][y][x] = color.getBlue() / 255.0f;
            }
        }
        
        return tensor;
    }
    
    /**
     * 模拟检测结果
     * 
     * 在没有真实模型时，返回模拟的检测结果
     * 用于测试和演示
     * 
     * @param image 输入图片
     * @return 模拟检测结果
     */
    private List<DetectionResultDTO> simulateDetection(BufferedImage image) {
        List<DetectionResultDTO> detections = new ArrayList<>();
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // 模拟检测3-5个叶片
        Random random = new Random();
        int count = 3 + random.nextInt(3);
        
        for (int i = 0; i < count; i++) {
            // 随机位置和大小
            float x = random.nextInt(width - 100) + 50;
            float y = random.nextInt(height - 100) + 50;
            float w = 80 + random.nextInt(80);
            float h = 60 + random.nextInt(60);
            
            // 随机类别
            int classId = random.nextInt(4);
            
            // 随机置信度
            float confidence = 0.7f + random.nextFloat() * 0.25f;
            
            detections.add(DetectionResultDTO.create(
                    classId,
                    CLASS_NAMES.getOrDefault(classId, "unknown"),
                    confidence,
                    x, y, w, h
            ));
        }
        
        return detections;
    }
    
    /**
     * 非极大值抑制（NMS）
     * 
     * 去除重叠的检测框，保留最准确的框
     * 
     * @param detections 原始检测结果
     * @return NMS处理后的结果
     */
    private List<DetectionResultDTO> nonMaxSuppression(List<DetectionResultDTO> detections) {
        if (detections.isEmpty()) {
            return detections;
        }
        
        // 按置信度排序
        detections.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));
        
        List<DetectionResultDTO> result = new ArrayList<>();
        
        for (DetectionResultDTO det : detections) {
            boolean shouldKeep = true;
            
            // 检查与已有框的IOU
            for (DetectionResultDTO kept : result) {
                if (det.getClassId() == kept.getClassId()) {
                    float iou = calculateIoU(det, kept);
                    if (iou > iouThreshold) {
                        shouldKeep = false;
                        break;
                    }
                }
            }
            
            if (shouldKeep) {
                result.add(det);
            }
        }
        
        return result;
    }
    
    /**
     * 计算两个检测框的IOU
     * 
     * @param a 检测框A
     * @param b 检测框B
     * @return IOU值 (0-1)
     */
    private float calculateIoU(DetectionResultDTO a, DetectionResultDTO b) {
        float x1 = Math.max(a.getX(), b.getX());
        float y1 = Math.max(a.getY(), b.getY());
        float x2 = Math.min(a.getX() + a.getWidth(), b.getX() + b.getWidth());
        float y2 = Math.min(a.getY() + a.getHeight(), b.getY() + b.getHeight());
        
        float intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float union = a.getWidth() * a.getHeight() + b.getWidth() * b.getHeight() - intersection;
        
        return union > 0 ? intersection / union : 0;
    }
    
    /**
     * 获取模型状态
     * 
     * @return 模型状态DTO
     */
    public ModelStatusDTO getStatus() {
        ModelStatusDTO dto = new ModelStatusDTO();
        dto.setModelLoaded(isModelLoaded);
        dto.setModelName(getModelName());
        dto.setModelPath(modelPath);
        dto.setInferenceCount(inferenceCount.get());
        
        // 计算平均推理时间
        long count = inferenceCount.get();
        if (count > 0) {
            dto.setAvgInferenceTimeMs((double) totalInferenceTime.get() / count);
        } else {
            dto.setAvgInferenceTimeMs(0);
        }
        
        if (lastInferenceTime != null) {
            dto.setLastInferenceTime(lastInferenceTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        dto.setModelVersion("YOLOv8n");
        dto.setStatus(isModelLoaded ? "RUNNING" : "STOPPED");
        dto.setMessage(isModelLoaded ? "模型运行中" : "模型未加载");
        
        return dto;
    }
    
    /**
     * 获取模型名称
     */
    private String getModelName() {
        return modelPath.contains("yolov8n") ? "YOLOv8n" : 
               modelPath.contains("yolov8s") ? "YOLOv8s" : "YOLOv8";
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        inferenceCount.set(0);
        totalInferenceTime.set(0);
        log.info("统计信息已重置");
    }
}
