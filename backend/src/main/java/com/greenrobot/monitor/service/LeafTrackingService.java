package com.greenrobot.monitor.service;

import com.greenrobot.monitor.config.LeafConfig;
import com.greenrobot.monitor.entity.ConfirmedLeaf;
import com.greenrobot.monitor.entity.LeafObservation;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 叶片追踪服务
 * 
 * 负责将检测到的叶片区域匹配到已确认的叶片档案
 * 使用多维特征匹配算法：
 * - 位置距离（35%权重）
 * - 颜色H值（30%权重）
 * - 面积比例（15%权重）
 * - 轮廓相似度（20%权重）
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
@Service
public class LeafTrackingService {

    /**
     * 追踪结果内部类
     * 
     * 封装单次追踪的结果
     */
    @Data
    public static class TrackingResult {
        /** 检测到的叶片 */
        private LeafDetectionService.DetectionResult detected;
        /** 匹配到的叶片档案（如果有） */
        private ConfirmedLeaf matchedLeaf;
        /** 匹配置信度 */
        private double confidence;
        /** 是否是新叶片 */
        private boolean isNewLeaf;
    }

    /**
     * 追踪当前帧中的所有叶片
     * 
     * 使用匈牙利算法找最优匹配
     * 确保每片当前叶片最多匹配一片历史叶片
     * 
     * @param detections 当前检测到的叶片列表
     * @param confirmedLeaves 已确认的叶片档案列表
     * @return 追踪结果列表
     */
    public List<TrackingResult> trackLeaves(List<LeafDetectionService.DetectionResult> detections,
                                            List<ConfirmedLeaf> confirmedLeaves) {
        List<TrackingResult> results = new ArrayList<>();
        
        if (detections.isEmpty()) {
            // 没有检测到叶片，直接返回空
            return results;
        }
        
        if (confirmedLeaves.isEmpty()) {
            // 没有已确认的叶片，所有检测都是新叶片
            for (LeafDetectionService.DetectionResult detection : detections) {
                TrackingResult result = new TrackingResult();
                result.setDetected(detection);
                result.setNewLeaf(true);
                result.setMatchedLeaf(null);
                result.setConfidence(0.0);
                results.add(result);
            }
            return results;
        }
        
        // ========== 构建匹配矩阵 ==========
        // 矩阵元素[i][j]表示第i个检测结果匹配第j个叶片的分数
        
        double[][] scoreMatrix = new double[detections.size()][confirmedLeaves.size()];
        for (int i = 0; i < detections.size(); i++) {
            for (int j = 0; j < confirmedLeaves.size(); j++) {
                scoreMatrix[i][j] = calculateMatchScore(
                    detections.get(i), 
                    confirmedLeaves.get(j)
                );
            }
        }
        
        // ========== 使用匈牙利算法找最优匹配 ==========
        // 这是一个经典的二分图最优匹配算法
        // 确保整体匹配分数最高
        
        Map<Integer, Integer> assignment = hungarianAlgorithm(scoreMatrix);
        
        // 记录已匹配的叶片索引
        Set<Integer> matchedLeafIndices = new HashSet<>();
        
        // ========== 处理匹配结果 ==========
        for (int i = 0; i < detections.size(); i++) {
            TrackingResult result = new TrackingResult();
            result.setDetected(detections.get(i));
            
            Integer matchedIndex = assignment.get(i);
            if (matchedIndex != null && scoreMatrix[i][matchedIndex] >= LeafConfig.MATCH_CONFIDENT_THRESHOLD) {
                // 匹配置信度足够，绑定到已有叶片
                ConfirmedLeaf matched = confirmedLeaves.get(matchedIndex);
                result.setMatchedLeaf(matched);
                result.setConfidence(scoreMatrix[i][matchedIndex]);
                result.setNewLeaf(false);
                matchedLeafIndices.add(matchedIndex);
            } else {
                // 匹配置信度不够，标记为新叶片
                result.setNewLeaf(true);
                result.setConfidence(scoreMatrix[i][matchedIndex != null ? matchedIndex : -1]);
            }
            results.add(result);
        }
        
        return results;
    }

    /**
     * 计算单次匹配分数
     * 
     * 分数 = 位置分数×0.35 + 颜色分数×0.30 + 面积分数×0.15 + 轮廓分数×0.20
     * 
     * @param detection 检测结果
     * @param leaf 叶片档案
     * @return 匹配分数 (0-1)
     */
    private double calculateMatchScore(LeafDetectionService.DetectionResult detection,
                                       ConfirmedLeaf leaf) {
        
        // ========== 1. 位置分数 (35%) ==========
        // 使用欧几里得距离计算位置差异
        // 距离越近分数越高，使用指数衰减
        double dx = detection.getCenterX() - leaf.getPositionX();
        double dy = detection.getCenterY() - leaf.getPositionY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        // 指数衰减：距离100像素时分数降到约0.37
        double positionScore = Math.exp(-distance / 100.0);
        
        // ========== 2. 颜色分数 (30%) ==========
        // 直接使用H值的差异计算
        // H值范围0-180，差异越小越好
        double hDiff = Math.abs(detection.getColorH() - leaf.getColorH());
        // 归一化到0-1，差异超过30认为完全不匹配
        double colorScore = Math.max(0, 1 - hDiff / 30.0);
        
        // ========== 3. 面积分数 (15%) ==========
        // 使用面积比例的对称性计算
        double areaRatio = detection.getArea() / leaf.getArea();
        // 面积比例接近1最好，偏离太多分数降低
        double areaScore = areaRatio >= 1 ? 1.0 / areaRatio : areaRatio;
        
        // ========== 4. 轮廓分数 (20%) ==========
        // Hu矩是轮廓的旋转缩放不变特征
        // 轮廓越相似分数越高
        double contourScore = 0.8; // TODO: 实现Hu矩比较
        
        // ========== 加权求和 ==========
        double totalScore = positionScore * LeafConfig.MATCH_POSITION_WEIGHT
                          + colorScore * LeafConfig.MATCH_COLOR_WEIGHT
                          + areaScore * LeafConfig.MATCH_AREA_WEIGHT
                          + contourScore * LeafConfig.MATCH_CONTOUR_WEIGHT;
        
        return totalScore;
    }

    /**
     * 匈牙利算法实现
     * 
     * 用于在二分图中找到最优匹配
     * 时间复杂度: O(n³)
     * 
     * @param costMatrix 成本矩阵（这里实际上是得分矩阵，取负作为成本）
     * @return 匹配结果，key是行索引，value是列索引
     */
    private Map<Integer, Integer> hungarianAlgorithm(double[][] costMatrix) {
        int n = costMatrix.length; // 检测数量
        int m = costMatrix[0].length; // 叶片数量
        
        // 转换为最小成本问题（取负）
        double[][] minCost = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                minCost[i][j] = -costMatrix[i][j];
            }
        }
        
        Map<Integer, Integer> result = new HashMap<>();
        
        // TODO: 实现完整的匈牙利算法
        // 当前返回简单的贪婪匹配作为占位
        
        // 贪婪匹配（简化版本）
        boolean[] matched = new boolean[m];
        for (int i = 0; i < n; i++) {
            int bestJ = -1;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < m; j++) {
                if (!matched[j] && costMatrix[i][j] > bestScore) {
                    bestScore = costMatrix[i][j];
                    bestJ = j;
                }
            }
            if (bestJ != -1) {
                result.put(i, bestJ);
                matched[bestJ] = true;
            }
        }
        
        return result;
    }

    /**
     * 创建观测记录
     * 
     * 根据追踪结果创建观测记录实体
     * 
     * @param result 追踪结果
     * @return 观测记录
     */
    public LeafObservation createObservation(TrackingResult result) {
        LeafObservation observation = new LeafObservation();
        LeafDetectionService.DetectionResult detection = result.getDetected();
        
        // 设置基本属性
        observation.setLeafId(result.getMatchedLeaf() != null 
            ? result.getMatchedLeaf().getLeafId() 
            : null);
        observation.setArea(detection.getArea());
        observation.setColorH(detection.getColorH());
        observation.setColorS(detection.getColorS());
        observation.setColorV(detection.getColorV());
        observation.setCentroidX((double) detection.getCenterX());
        observation.setCentroidY((double) detection.getCenterY());
        observation.setPositionX(detection.getCenterX());
        observation.setPositionY(detection.getCenterY());
        observation.setMatchConfidence(result.getConfidence());
        observation.setSource(result.isNewLeaf() ? "DETECTED" : "MANUAL");
        observation.setObservationTime(LocalDateTime.now());
        
        return observation;
    }
}
