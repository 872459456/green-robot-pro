package com.greenrobot.monitor.service;

import com.greenrobot.monitor.entity.ConfirmedLeaf;
import com.greenrobot.monitor.entity.LeafObservation;
import com.greenrobot.monitor.repository.ConfirmedLeafRepository;
import com.greenrobot.monitor.repository.LeafObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 定时任务服务
 * 
 * 负责系统的自动拍摄定时任务
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class ScheduledTaskService {
    
    /** 监控服务 */
    private final MonitorService monitorService;
    
    /** 叶片追踪服务 */
    private final LeafTrackingService leafTrackingService;
    
    /** 叶片服务 */
    private final LeafService leafService;
    
    /** 叶片档案仓库 */
    private final ConfirmedLeafRepository leafRepository;
    
    /** 观测记录仓库 */
    private final LeafObservationRepository observationRepository;
    
    /** 飞书通知服务 */
    private final FeishuNotificationService feishuService;
    
    /**
     * 定时拍摄任务
     * 
     * 每小时自动执行一次拍摄和叶片检测
     * 
     * cron表达式: 0 0 * * * ? = 每小时第0分第0秒执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void scheduledCapture() {
        log.info("========== 定时拍摄任务开始 ==========");
        
        try {
            // ========== 1. 触发拍摄 ==========
            var captureResult = monitorService.capture(false);
            
            if (!captureResult.isSuccess()) {
                log.warn("定时拍摄失败: {}", captureResult.getErrorMessage());
                return;
            }
            
            log.info("拍摄成功: 叶片数={}, 新叶片={}", 
                    captureResult.getLeafCount(), 
                    captureResult.getNewLeafCount());
            
            // ========== 2. TODO: 执行叶片检测 ==========
            // 目前使用模拟数据，实际部署时将集成OpenCV
            // detectAndTrackLeaves();
            
            log.info("========== 定时拍摄任务完成 ==========");
            
        } catch (Exception e) {
            log.error("定时拍摄任务异常", e);
        }
    }
    
    /**
     * 每日健康检查
     * 
     * 每天早上9点执行，检查所有叶片的健康状态
     * 对需要关注的叶片发送飞书通知
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void dailyHealthCheck() {
        log.info("========== 每日健康检查开始 ==========");
        
        try {
            // 获取所有需要关注的叶片
            List<ConfirmedLeaf> attentionLeaves = leafService.getLeavesNeedingAttention();
            
            if (attentionLeaves.isEmpty()) {
                log.info("所有叶片健康，无异常");
                return;
            }
            
            log.info("发现{}片需要关注的叶片", attentionLeaves.size());
            
            // 遍历并发送通知
            for (ConfirmedLeaf leaf : attentionLeaves) {
                // 获取最新观测记录
                List<LeafObservation> observations = 
                        observationRepository.findTop1ByLeafIdOrderByObservationTimeDesc(leaf.getLeafId());
                
                if (!observations.isEmpty()) {
                    LeafObservation latestObs = observations.get(0);
                    // 发送飞书预警
                    feishuService.sendLeafAlert(leaf, latestObs);
                }
            }
            
            log.info("========== 每日健康检查完成 ==========");
            
        } catch (Exception e) {
            log.error("每日健康检查异常", e);
        }
    }
    
    /**
     * 执行叶片检测和追踪
     * 
     * TODO: 集成OpenCV实现真实的叶片检测
     * 
     * @param captureResult 拍摄结果
     */
    @Transactional
    private void detectAndTrackLeaves(var captureResult) {
        // TODO: 实现真实的叶片检测和追踪逻辑
        // 1. 读取拍摄图片
        // 2. HSV分割提取绿色区域
        // 3. 轮廓分析获取叶片区域
        // 4. 与已确认叶片匹配
        // 5. 保存观测记录
        // 6. 更新叶片状态
    }
}
