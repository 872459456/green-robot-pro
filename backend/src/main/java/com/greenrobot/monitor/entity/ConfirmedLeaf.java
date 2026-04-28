package com.greenrobot.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 已确认叶片档案实体
 * 
 * 记录用户确认过的有效叶片
 * 每片叶子有唯一的leafId作为主键
 * 包含首次发现时的位置、形态、颜色等核心特征
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
@Data
@Entity
@Table(name = "confirmed_leaves")
public class ConfirmedLeaf {

    /** 叶片唯一标识，主键，如"L0001" */
    @Id
    @Column(name = "leaf_id", nullable = false, length = 20)
    private String leafId;

    /** 首次发现时的中心X坐标，用于初步位置匹配 */
    @Column(name = "position_x")
    private Integer positionX;

    /** 首次发现时的中心Y坐标，用于初步位置匹配 */
    @Column(name = "position_y")
    private Integer positionY;

    /** 首次发现时的面积（像素数），用于面积变化监测 */
    @Column(name = "area")
    private Double area;

    /** 首次发现时的HSV颜色H值，核心健康指标 */
    @Column(name = "color_h")
    private Double colorH;

    /** 首次发现时的HSV颜色S值，饱和度指标 */
    @Column(name = "color_s")
    private Double colorS;

    /** 首次发现时的HSV颜色V值，亮度指标 */
    @Column(name = "color_v")
    private Double colorV;

    /** 叶片圆形度 = 4π×面积/周长²，取值0-1，越接近1越圆 */
    @Column(name = "circularity")
    private Double circularity;

    /** 叶片首次发现的图片路径 */
    @Column(name = "image_path")
    private String imagePath;

    /** 叶片确认时间 */
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    /** 用户备注信息 */
    @Column(name = "notes")
    private String notes;

    /** Hu矩轮廓特征 - 7维特征向量，旋转缩放不变 */
    @Column(name = "contour_points")
    private String contourPoints;

    /** 标注图路径 - 用于前端展示检测结果 */
    @Column(name = "annotated_image_path")
    private String annotatedImagePath;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 叶片当前健康状态: HEALTHY/YELLOW_LEAF/WILT/WILT_WARNING */
    @Column(name = "health_status")
    private String healthStatus;

    /** 最后一次预警时间，用于冷却控制 */
    @Column(name = "last_alert_time")
    private LocalDateTime lastAlertTime;

    /** 最后一次观测时间 */
    @Column(name = "last_observation_time")
    private LocalDateTime lastObservationTime;

    /** 叶片累计成长百分比 */
    @Column(name = "growth_percentage")
    private Double growthPercentage;

    @PrePersist
    protected void onCreate() {
        // 首次创建时自动设置创建时间和更新时间
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (confirmedAt == null) {
            // 如果没有指定确认时间，使用当前时间
            confirmedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // 更新时自动刷新更新时间
        updatedAt = LocalDateTime.now();
    }
}
