package com.greenrobot.monitor.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 监控状态数据传输对象
 * 
 * 用于返回监控系统当前状态
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@Data
public class MonitorStatusDTO {
    
    /** 监控系统是否在线 */
    private boolean online;
    
    /** 当前使用的摄像头索引 */
    private Integer cameraIndex;
    
    /** 最后一次拍摄时间 */
    private LocalDateTime lastCapture;
    
    /** 今日拍摄次数 */
    private Integer captureCount;
    
    /** 当前监控状态描述 */
    private String status;
    
    /** 备注信息 */
    private String message;
    
    /**
     * 创建在线状态对象
     */
    public static MonitorStatusDTO online(int cameraIndex, LocalDateTime lastCapture, int captureCount) {
        MonitorStatusDTO dto = new MonitorStatusDTO();
        dto.setOnline(true);
        dto.setCameraIndex(cameraIndex);
        dto.setLastCapture(lastCapture);
        dto.setCaptureCount(captureCount);
        dto.setStatus("RUNNING");
        dto.setMessage("监控系统运行正常");
        return dto;
    }
    
    /**
     * 创建离线状态对象
     */
    public static MonitorStatusDTO offline(String message) {
        MonitorStatusDTO dto = new MonitorStatusDTO();
        dto.setOnline(false);
        dto.setStatus("OFFLINE");
        dto.setMessage(message != null ? message : "监控系统未启动");
        return dto;
    }
}
