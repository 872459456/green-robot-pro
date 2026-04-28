package com.greenrobot.monitor.dto;

import lombok.Data;

/**
 * 单个检测结果数据传输对象
 * 
 * 表示YOLO检测到的单个目标
 * 
 * @author 狼群团队
 * @version 4.0.0
 * @since 2026-04-28
 */
@Data
public class DetectionResultDTO {
    
    /** 类别ID */
    private int classId;
    
    /** 类别名称 */
    private String className;
    
    /** 置信度 (0-1) */
    private float confidence;
    
    /** 检测框X坐标 */
    private float x;
    
    /** 检测框Y坐标 */
    private float y;
    
    /** 检测框宽度 */
    private float width;
    
    /** 检测框高度 */
    private float height;
    
    /**
     * 创建检测结果
     */
    public static DetectionResultDTO create(int classId, String className, 
            float confidence, float x, float y, float width, float height) {
        DetectionResultDTO dto = new DetectionResultDTO();
        dto.setClassId(classId);
        dto.setClassName(className);
        dto.setConfidence(confidence);
        dto.setX(x);
        dto.setY(y);
        dto.setWidth(width);
        dto.setHeight(height);
        return dto;
    }
}
