package com.greenrobot.monitor.service;

import com.greenrobot.monitor.entity.ConfirmedLeaf;
import com.greenrobot.monitor.entity.LeafObservation;
import com.greenrobot.monitor.repository.ConfirmedLeafRepository;
import com.greenrobot.monitor.repository.LeafObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 叶片管理服务
 * 
 * 提供叶片的增删改查等管理功能
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@Service
@RequiredArgsConstructor
public class LeafService {

    private final ConfirmedLeafRepository confirmedLeafRepository;
    private final LeafObservationRepository observationRepository;

    /**
     * 获取所有已确认的叶片
     * 
     * @return 按确认时间倒序的叶片列表
     */
    public List<ConfirmedLeaf> getAllConfirmedLeaves() {
        return confirmedLeafRepository.findAllByOrderByConfirmedAtDesc();
    }

    /**
     * 根据ID获取叶片
     * 
     * @param leafId 叶片ID
     * @return 叶片信息
     */
    public Optional<ConfirmedLeaf> getLeafById(String leafId) {
        return confirmedLeafRepository.findById(leafId);
    }

    /**
     * 获取需要关注的叶片（健康状态异常）
     * 
     * @return 需要关注的叶片列表
     */
    public List<ConfirmedLeaf> getLeavesNeedingAttention() {
        return confirmedLeafRepository.findLeavesNeedingAttention();
    }

    /**
     * 确认新叶片
     * 
     * 将检测到的叶片加入已确认档案
     * 自动生成唯一的叶片ID（如L0001）
     * 
     * @param observation 观测记录
     * @return 新创建的叶片档案
     */
    @Transactional
    public ConfirmedLeaf confirmLeaf(LeafObservation observation) {
        // ========== 生成唯一的叶片ID ==========
        // 格式: L + 4位序号，如L0001, L0002...
        String newLeafId = generateLeafId();
        
        // ========== 创建叶片档案 ==========
        ConfirmedLeaf leaf = new ConfirmedLeaf();
        leaf.setLeafId(newLeafId);
        leaf.setPositionX(observation.getPositionX());
        leaf.setPositionY(observation.getPositionY());
        leaf.setArea(observation.getArea());
        leaf.setColorH(observation.getColorH());
        leaf.setColorS(observation.getColorS());
        leaf.setColorV(observation.getColorV());
        leaf.setImagePath(observation.getImagePath());
        leaf.setAnnotatedImagePath(observation.getAnnotatedImagePath());
        leaf.setConfirmedAt(observation.getObservationTime());
        leaf.setHealthStatus(observation.getStatus());
        leaf.setLastObservationTime(observation.getObservationTime());
        leaf.setGrowthPercentage(0.0); // 首次确认，成长为0
        
        return confirmedLeafRepository.save(leaf);
    }

    /**
     * 更新叶片信息
     * 
     * @param leafId 叶片ID
     * @param observation 最新观测记录
     * @return 更新后的叶片档案
     */
    @Transactional
    public ConfirmedLeaf updateLeaf(String leafId, LeafObservation observation) {
        ConfirmedLeaf leaf = confirmedLeafRepository.findById(leafId)
                .orElseThrow(() -> new RuntimeException("叶片不存在: " + leafId));
        
        // ========== 更新位置和形态特征（取最新值） ==========
        leaf.setPositionX(observation.getPositionX());
        leaf.setPositionY(observation.getPositionY());
        
        // ========== 更新颜色特征 ==========
        leaf.setColorH(observation.getColorH());
        leaf.setColorS(observation.getColorS());
        leaf.setColorV(observation.getColorV());
        
        // ========== 计算成长百分比 ==========
        // 成长% = (当前面积 / 首次确认面积 - 1) × 100%
        if (leaf.getArea() > 0) {
            double growthPercent = (observation.getArea() / leaf.getArea() - 1) * 100;
            leaf.setGrowthPercentage(growthPercent);
        }
        
        // ========== 更新面积 ==========
        leaf.setArea(observation.getArea());
        
        // ========== 更新健康状态 ==========
        leaf.setHealthStatus(observation.getStatus());
        
        // ========== 更新最后观测时间 ==========
        leaf.setLastObservationTime(observation.getObservationTime());
        
        return confirmedLeafRepository.save(leaf);
    }

    /**
     * 删除叶片
     * 
     * 级联删除：同时删除该叶片的所有观测记录
     * 
     * @param leafId 叶片ID
     */
    @Transactional
    public void deleteLeaf(String leafId) {
        // 1. 先删除所有关联的观测记录
        List<LeafObservation> observations = observationRepository.findByLeafIdOrderByObservationTimeDesc(leafId);
        observationRepository.deleteAll(observations);
        
        // 2. 再删除叶片档案
        confirmedLeafRepository.deleteById(leafId);
    }

    /**
     * 获取叶片的历史观测记录
     * 
     * @param leafId 叶片ID
     * @return 按时间倒序的观测记录
     */
    public List<LeafObservation> getLeafObservations(String leafId) {
        return observationRepository.findByLeafIdOrderByObservationTimeDesc(leafId);
    }

    /**
     * 获取叶片最近的N条观测记录
     * 
     * @param leafId 叶片ID
     * @param limit 记录数量
     * @return 最近的N条记录
     */
    public List<LeafObservation> getRecentObservations(String leafId, int limit) {
        return observationRepository.findRecentObservations(leafId, limit);
    }

    /**
     * 获取叶片的历史最大面积
     * 
     * @param leafId 叶片ID
     * @return 历史最大面积
     */
    public Double getMaxArea(String leafId) {
        return observationRepository.findMaxAreaByLeafId(leafId);
    }

    /**
     * 生成唯一的叶片ID
     * 
     * 格式: L + 4位序号
     * 如: L0001, L0002, ... L9999
     * 
     * @return 新的叶片ID
     */
    private String generateLeafId() {
        // 获取当前最大的叶片ID
        List<ConfirmedLeaf> allLeaves = confirmedLeafRepository.findAll();
        
        int maxNum = 0;
        for (ConfirmedLeaf leaf : allLeaves) {
            String leafId = leaf.getLeafId();
            if (leafId != null && leafId.startsWith("L")) {
                try {
                    int num = Integer.parseInt(leafId.substring(1));
                    maxNum = Math.max(maxNum, num);
                } catch (NumberFormatException ignored) {
                    // 忽略格式错误的ID
                }
            }
        }
        
        // 下一个ID
        return String.format("L%04d", maxNum + 1);
    }

    /**
     * 获取叶片统计信息
     *
     * @return 统计信息
     */
    public LeafStatistics getStatistics() {
        LeafStatistics stats = new LeafStatistics();

        // 总叶片数
        stats.setTotalLeaves(confirmedLeafRepository.count());

        // 各状态数量
        List<Object[]> statusCounts = confirmedLeafRepository.countByHealthStatus();
        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            stats.getStatusCounts().put(status, count);
        }

        // 需要关注的叶片数
        stats.setAttentionCount(confirmedLeafRepository.findLeavesNeedingAttention().size());

        return stats;
    }

    /**
     * 创建观测记录
     *
     * 将检测到的叶片数据保存为观测记录
     *
     * @param leafId 叶片ID
     * @param area 面积
     * @param colorH H值
     * @param colorS S值
     * @param colorV V值
     * @param positionX X坐标
     * @param positionY Y坐标
     * @param status 健康状态
     * @param confidence 匹配置信度
     * @param imagePath 图片路径
     * @param annotatedPath 标注图路径
     * @return 创建的观测记录
     */
    @Transactional
    public LeafObservation createObservation(String leafId, Double area, Double colorH,
            Double colorS, Double colorV, Integer positionX, Integer positionY,
            String status, Double confidence, String imagePath, String annotatedPath) {
        LeafObservation observation = new LeafObservation();
        observation.setLeafId(leafId);
        observation.setArea(area);
        observation.setColorH(colorH);
        observation.setColorS(colorS);
        observation.setColorV(colorV);
        observation.setPositionX(positionX);
        observation.setPositionY(positionY);
        observation.setCentroidX(positionX != null ? positionX.doubleValue() : null);
        observation.setCentroidY(positionY != null ? positionY.doubleValue() : null);
        observation.setStatus(status);
        observation.setMatchConfidence(confidence);
        observation.setImagePath(imagePath);
        observation.setAnnotatedImagePath(annotatedPath);
        observation.setSource("DETECTED");
        observation.setObservationTime(LocalDateTime.now());

        return observationRepository.save(observation);
    }

    /**
     * 更新叶片的健康状态
     *
     * 根据最新观测记录更新叶片的健康状态
     *
     * @param leafId 叶片ID
     * @param newStatus 新的健康状态
     */
    @Transactional
    public void updateLeafHealthStatus(String leafId, String newStatus) {
        confirmedLeafRepository.findById(leafId).ifPresent(leaf -> {
            leaf.setHealthStatus(newStatus);
            leaf.setLastObservationTime(LocalDateTime.now());
            confirmedLeafRepository.save(leaf);
        });
    }

    /**
     * 获取叶片的历史最大面积
     *
     * @param leafId 叶片ID
     * @return 历史最大面积
     */
    public Double getHistoricalMaxArea(String leafId) {
        return observationRepository.findMaxAreaByLeafId(leafId);
    }

    /**
     * 叶片统计信息内部类
     */
    @Data
    public static class LeafStatistics {
        private long totalLeaves;
        private long attentionCount;
        private java.util.Map<String, Long> statusCounts = new java.util.HashMap<>();
    }
}
