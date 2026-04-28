package com.greenrobot.monitor.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 拍摄结果数据传输对象
 * 
 * 用于返回拍摄操作的结果
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@Data
public class CaptureResultDTO {
    
    /** 拍摄是否成功 */
    private boolean success;
    
    /** 拍摄的图片路径 */
    private String imagePath;
    
    /** 标注图路径 */
    private String annotatedImagePath;
    
    /** 检测到的叶片数量 */
    private Integer leafCount;
    
    /** 新发现的叶片数量 */
    private Integer newLeafCount;
    
    /** 是否发送了预警 */
    private Boolean alertSent;
    
    /** 拍摄时间 */
    private LocalDateTime captureTime;
    
    /** 错误信息（如果失败） */
    private String errorMessage;
    
    /** 绿色覆盖率 */
    private Double greenCoverage;
    
    /** 健康叶片数量 */
    private Integer healthyCount;
    
    /** 异常叶片数量 */
    private Integer attentionCount;
    
    /**
     * 创建成功结果
     */
    public static CaptureResultDTO success(String imagePath, String annotatedPath, 
            int leafCount, int newLeafCount, boolean alertSent,
            Double greenCoverage, int healthyCount, int attentionCount) {
        CaptureResultDTO dto = new CaptureResultDTO();
        dto.setSuccess(true);
        dto.setImagePath(imagePath);
        dto.setAnnotatedImagePath(annotatedPath);
        dto.setLeafCount(leafCount);
        dto.setNewLeafCount(newLeafCount);
        dto.setAlertSent(alertSent);
        dto.setCaptureTime(LocalDateTime.now());
        dto.setGreenCoverage(greenCoverage);
        dto.setHealthyCount(healthyCount);
        dto.setAttentionCount(attentionCount);
        return dto;
    }
    
    /**
     * 创建失败结果
     */
    public static CaptureResultDTO failure(String errorMessage) {
        CaptureResultDTO dto = new CaptureResultDTO();
        dto.setSuccess(false);
        dto.setErrorMessage(errorMessage);
        dto.setCaptureTime(LocalDateTime.now());
        return dto;
    }
}
