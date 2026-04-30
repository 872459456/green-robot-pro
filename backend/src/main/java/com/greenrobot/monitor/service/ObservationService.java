package com.greenrobot.monitor.service;

import com.greenrobot.monitor.dto.CaptureResultDTO;
import com.greenrobot.monitor.entity.ConfirmedLeaf;
import com.greenrobot.monitor.entity.HealthStatus;
import com.greenrobot.monitor.entity.LeafObservation;
import com.greenrobot.monitor.repository.ConfirmedLeafRepository;
import com.greenrobot.monitor.repository.LeafObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 观测记录服务
 * 
 * 负责观测记录的创建、查询和与叶片的关联
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObservationService {
    
    /** 观测记录仓库 */
    private final LeafObservationRepository observationRepository;
    
    /** 叶片档案仓库 */
    private final ConfirmedLeafRepository leafRepository;
    
    /** 叶片检测服务 */
    private final LeafDetectionService detectionService;
    
    /** 飞书通知服务 */
    private final FeishuNotificationService feishuService;
    
    /**
     * 从拍摄结果创建观测记录
     * 
     * 拍摄成功后，将检测到的叶片信息保存为观测记录
     * 如果叶片已确认，则更新叶片状态
     * 如果发现异常叶片，则发送飞书预警
     * 
     * @param captureResult 拍摄结果
     */
    @Transactional
    public void createObservationsFromCapture(CaptureResultDTO captureResult) {
        log.info("创建观测记录: 叶片数={}", captureResult.getLeafCount());
        
        // TODO: 实际部署时，这里应该使用真实的叶片检测结果
        // 目前使用模拟数据
        
        // 模拟创建3个观测记录（对应3片模拟叶片）
        for (int i = 0; i < captureResult.getLeafCount(); i++) {
            String simulatedLeafId = "L" + String.format("%04d", i + 1);
            
            // 检查叶片是否已确认
            List<ConfirmedLeaf> existingLeaves = leafRepository.findAll();
            boolean isNewLeaf = existingLeaves.stream()
                    .noneMatch(l -> l.getLeafId().equals(simulatedLeafId));
            
            if (isNewLeaf) {
                // 新叶片，跳过（需要先确认才能创建观测记录）
                log.info("发现新叶片候选: {}, 等待用户确认", simulatedLeafId);
                continue;
            }
            
            // 已确认的叶片，创建观测记录
            LeafObservation observation = new LeafObservation();
            observation.setLeafId(simulatedLeafId);
            observation.setArea(1200.0 + Math.random() * 300); // 模拟面积
            observation.setColorH(40.0 + Math.random() * 10);  // 模拟H值
            observation.setColorS(120.0 + Math.random() * 30);   // 模拟S值
            observation.setColorV(140.0 + Math.random() * 20);   // 模拟V值
            observation.setPositionX(100 + i * 150);
            observation.setPositionY(100 + i * 50);
            observation.setCentroidX(observation.getPositionX().doubleValue());
            observation.setCentroidY(observation.getPositionY().doubleValue());
            observation.setStatus("HEALTHY");
            observation.setMatchConfidence(0.85);
            observation.setImagePath(captureResult.getImagePath());
            observation.setAnnotatedImagePath(captureResult.getAnnotatedImagePath());
            observation.setSource("DETECTED");
            observation.setObservationTime(LocalDateTime.now());
            
            // 保存观测记录
            observationRepository.save(observation);
            log.info("保存观测记录: 叶片={}", simulatedLeafId);
            
            // 更新叶片状态
            updateLeafFromObservation(simulatedLeafId, observation);
        }
    }
    
    /**
     * 从观测记录更新叶片档案
     * 
     * 根据最新观测记录更新叶片的位置、颜色等信息
     * 如果状态异常，发送飞书预警
     * 
     * @param leafId 叶片ID
     * @param observation 最新观测记录
     */
    @Transactional
    public void updateLeafFromObservation(String leafId, LeafObservation observation) {
        leafRepository.findById(leafId).ifPresent(leaf -> {
            // 更新位置
            leaf.setPositionX(observation.getPositionX());
            leaf.setPositionY(observation.getPositionY());
            
            // 更新颜色
            leaf.setColorH(observation.getColorH());
            leaf.setColorS(observation.getColorS());
            leaf.setColorV(observation.getColorV());
            
            // 更新面积
            if (observation.getArea() != null) {
                // 计算成长百分比
                double growthPercent = (observation.getArea() / leaf.getArea() - 1) * 100;
                leaf.setGrowthPercentage(growthPercent);
                leaf.setArea(observation.getArea());
            }
            
            // 更新状态
            leaf.setHealthStatus(observation.getStatus());
            leaf.setLastObservationTime(observation.getObservationTime());
            
            leafRepository.save(leaf);
            log.info("更新叶片状态: {} -> {}", leafId, observation.getStatus());
            
            // 如果状态异常，发送预警
            if (observation.getStatus() != null && 
                    !observation.getStatus().equals("HEALTHY")) {
                feishuService.sendLeafAlert(leaf, observation);
            }
        });
    }
    
    /**
     * 根据HSV值判断健康状态
     * 
     * @param h H值
     * @param s S值
     * @param v V值
     * @return 健康状态
     */
    public String determineHealthStatus(double h, double s, double v) {
        // 使用叶片检测服务的判断逻辑
        HealthStatus status = detectionService.determineHealthStatus(h, s, v, null, null, null);
        return status.name();
    }
    
    /**
     * 获取叶片的所有观测记录
     * 
     * @param leafId 叶片ID
     * @return 观测记录列表（按时间倒序）
     */
    public List<LeafObservation> getObservations(String leafId) {
        return observationRepository.findByLeafIdOrderByObservationTimeDesc(leafId);
    }
    
    /**
     * 获取叶片最近的N条观测记录
     * 
     * @param leafId 叶片ID
     * @param limit 数量限制
     * @return 观测记录列表
     */
    public List<LeafObservation> getRecentObservations(String leafId, int limit) {
        return observationRepository.findRecentObservations(leafId, limit);
    }
}
