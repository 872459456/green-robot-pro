package com.greenrobot.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 叶片观测记录实体
 * 
 * 每次叶片追踪分析产生一条观测记录
 * 记录某片叶子在特定时间的面积、颜色、位置等状态
 * 用于追踪叶片成长历史和健康变化趋势
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
@Data
@Entity
@Table(name = "leaf_observations")
public class LeafObservation {

    /** 观测记录ID，自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的叶片ID，外键指向confirmed_leaves表 */
    @Column(name = "leaf_id", length = 20)
    private String leafId;

    /** 观测时间（拍摄时间） */
    @Column(name = "observation_time")
    private LocalDateTime observationTime;

    /** 原始图片路径 */
    @Column(name = "image_path")
    private String imagePath;

    /** 叶片面积（像素数） */
    @Column(name = "area")
    private Double area;

    /** HSV颜色H值 - 色相，0-180范围 */
    @Column(name = "color_h")
    private Double colorH;

    /** HSV颜色S值 - 饱和度 */
    @Column(name = "color_s")
    private Double colorS;

    /** HSV颜色V值 - 亮度 */
    @Column(name = "color_v")
    private Double colorV;

    /** 叶片中心X坐标 */
    @Column(name = "centroid_x")
    private Double centroidX;

    /** 叶片中心Y坐标 */
    @Column(name = "centroid_y")
    private Double centroidY;

    /** 叶片健康状态: HEALTHY/YELLOW_LEAF/WILT/WILT_WARNING/TREND_YELLOW/TREND_WILT */
    @Column(name = "status")
    private String status;

    /** 匹配置信度 - 0到1，越高表示匹配越准确 */
    @Column(name = "match_confidence")
    private Double matchConfidence;

    /** 捕获时间（系统记录时间） */
    @Column(name = "capture_time")
    private LocalDateTime captureTime;

    /** 标注图路径 */
    @Column(name = "annotated_image_path")
    private String annotatedImagePath;

    /** 叶片位置X（用于历史对比） */
    @Column(name = "position_x")
    private Integer positionX;

    /** 叶片位置Y（用于历史对比） */
    @Column(name = "position_y")
    private Integer positionY;

    /** 数据来源: DETECTED（自动检测）/ MANUAL（手动确认） */
    @Column(name = "source")
    private String source;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // 自动设置创建时间和捕获时间
        createdAt = LocalDateTime.now();
        if (captureTime == null) {
            captureTime = LocalDateTime.now();
        }
        if (observationTime == null) {
            observationTime = LocalDateTime.now();
        }
    }
}
