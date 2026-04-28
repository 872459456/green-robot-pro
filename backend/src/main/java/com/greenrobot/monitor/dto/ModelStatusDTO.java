package com.greenrobot.monitor.dto;

import lombok.Data;

/**
 * 模型状态数据传输对象
 * 
 * 用于返回YOLO模型的状态信息
 * 
 * @author 狼群团队
 * @version 4.0.0
 * @since 2026-04-28
 */
@Data
public class ModelStatusDTO {
    
    /** 模型是否已加载 */
    private boolean modelLoaded;
    
    /** 模型名称 */
    private String modelName;
    
    /** 模型文件路径 */
    private String modelPath;
    
    /** 推理总次数 */
    private long inferenceCount;
    
    /** 平均推理耗时（毫秒） */
    private double avgInferenceTimeMs;
    
    /** 模型版本 */
    private String modelVersion;
    
    /** 最后一次推理时间 */
    private String lastInferenceTime;
    
    /** 状态描述 */
    private String status;
    
    /** 备注信息 */
    private String message;
    
    /**
     * 创建模型就绪状态
     */
    public static ModelStatusDTO ready(String modelName, String modelPath) {
        ModelStatusDTO dto = new ModelStatusDTO();
        dto.setModelLoaded(true);
        dto.setModelName(modelName);
        dto.setModelPath(modelPath);
        dto.setStatus("READY");
        dto.setMessage("模型加载成功，可以进行检测");
        return dto;
    }
    
    /**
     * 创建模型未加载状态
     */
    public static ModelStatusDTO notLoaded(String message) {
        ModelStatusDTO dto = new ModelStatusDTO();
        dto.setModelLoaded(false);
        dto.setStatus("NOT_LOADED");
        dto.setMessage(message);
        return dto;
    }
}
