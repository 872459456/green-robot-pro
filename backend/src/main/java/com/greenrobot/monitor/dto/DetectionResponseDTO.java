package com.greenrobot.monitor.dto;

import lombok.Data;
import java.util.List;

/**
 * 检测结果响应数据传输对象
 * 
 * 包含YOLO模型检测的完整结果
 * 
 * @author 狼群团队
 * @version 4.0.0
 * @since 2026-04-28
 */
@Data
public class DetectionResponseDTO {
    
    /** 检测是否成功 */
    private boolean success;
    
    /** 检测结果列表 */
    private List<DetectionResultDTO> detections;
    
    /** 推理耗时（毫秒） */
    private double inferenceTimeMs;
    
    /** 总检测数量 */
    private int totalDetections;
    
    /** 错误信息（如果失败） */
    private String errorMessage;
    
    /** 使用的模型名称 */
    private String modelName;
    
    /**
     * 创建成功响应
     */
    public static DetectionResponseDTO success(List<DetectionResultDTO> detections, 
            double inferenceTimeMs, String modelName) {
        DetectionResponseDTO dto = new DetectionResponseDTO();
        dto.setSuccess(true);
        dto.setDetections(detections);
        dto.setInferenceTimeMs(inferenceTimeMs);
        dto.setTotalDetections(detections.size());
        dto.setModelName(modelName);
        return dto;
    }
    
    /**
     * 创建失败响应
     */
    public static DetectionResponseDTO failure(String errorMessage) {
        DetectionResponseDTO dto = new DetectionResponseDTO();
        dto.setSuccess(false);
        dto.setErrorMessage(errorMessage);
        dto.setDetections(null);
        dto.setTotalDetections(0);
        return dto;
    }
}
