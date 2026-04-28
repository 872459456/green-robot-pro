package com.greenrobot.monitor.dto;

import lombok.Data;

/**
 * 观测记录数据传输对象
 * 
 * 用于创建观测记录的请求数据
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@Data
public class ObservationDTO {
    
    /** 关联的叶片ID */
    private String leafId;
    
    /** 叶片面积 */
    private Double area;
    
    /** HSV颜色H值 */
    private Double colorH;
    
    /** HSV颜色S值 */
    private Double colorS;
    
    /** HSV颜色V值 */
    private Double colorV;
    
    /** 叶片中心X坐标 */
    private Integer positionX;
    
    /** 叶片中心Y坐标 */
    private Integer positionY;
    
    /** 健康状态 */
    private String status;
    
    /** 匹配置信度 */
    private Double matchConfidence;
    
    /** 原始图片路径 */
    private String imagePath;
    
    /** 标注图路径 */
    private String annotatedImagePath;
}
