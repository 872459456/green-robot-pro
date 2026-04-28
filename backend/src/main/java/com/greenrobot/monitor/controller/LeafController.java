package com.greenrobot.monitor.controller;

import com.greenrobot.monitor.entity.ConfirmedLeaf;
import com.greenrobot.monitor.entity.LeafObservation;
import com.greenrobot.monitor.service.LeafService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 叶片管理REST API控制器
 * 
 * 提供叶片的增删改查接口
 * 
 * @author 狼群团队
 * @version 3.1.0
 * @since 2026-04-28
 */
@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LeafController {

    private final LeafService leafService;

    /**
     * 获取所有已确认的叶片
     * 
     * GET /api/leaves
     * 
     * @return 所有叶片列表
     */
    @GetMapping
    public ResponseEntity<List<ConfirmedLeaf>> getAllLeaves() {
        return ResponseEntity.ok(leafService.getAllConfirmedLeaves());
    }

    /**
     * 根据ID获取叶片详情
     * 
     * GET /api/leaves/{leafId}
     * 
     * @param leafId 叶片ID
     * @return 叶片详情
     */
    @GetMapping("/{leafId}")
    public ResponseEntity<ConfirmedLeaf> getLeafById(@PathVariable String leafId) {
        return leafService.getLeafById(leafId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取需要关注的叶片
     * 
     * GET /api/leaves/attention
     * 
     * 返回健康状态异常的叶片（黄叶、枯萎、预警状态）
     * 
     * @return 需要关注的叶片列表
     */
    @GetMapping("/attention")
    public ResponseEntity<List<ConfirmedLeaf>> getLeavesNeedingAttention() {
        return ResponseEntity.ok(leafService.getLeavesNeedingAttention());
    }

    /**
     * 确认新叶片
     * 
     * POST /api/leaves/confirm
     * 
     * 将检测到的叶片加入已确认档案
     * 
     * @param observation 观测记录
     * @return 新创建的叶片档案
     */
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmedLeaf> confirmLeaf(@RequestBody LeafObservation observation) {
        ConfirmedLeaf newLeaf = leafService.confirmLeaf(observation);
        return ResponseEntity.ok(newLeaf);
    }

    /**
     * 更新叶片信息
     * 
     * PUT /api/leaves/{leafId}
     * 
     * 根据最新观测记录更新叶片档案
     * 
     * @param leafId 叶片ID
     * @param observation 最新观测记录
     * @return 更新后的叶片档案
     */
    @PutMapping("/{leafId}")
    public ResponseEntity<ConfirmedLeaf> updateLeaf(
            @PathVariable String leafId,
            @RequestBody LeafObservation observation) {
        try {
            ConfirmedLeaf updated = leafService.updateLeaf(leafId, observation);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除叶片
     * 
     * DELETE /api/leaves/{leafId}
     * 
     * 级联删除：该叶片的所有观测记录也会被删除
     * 
     * @param leafId 叶片ID
     * @return 204 No Content
     */
    @DeleteMapping("/{leafId}")
    public ResponseEntity<Void> deleteLeaf(@PathVariable String leafId) {
        leafService.deleteLeaf(leafId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取叶片的历史观测记录
     * 
     * GET /api/leaves/{leafId}/observations
     * 
     * @param leafId 叶片ID
     * @return 观测记录列表（按时间倒序）
     */
    @GetMapping("/{leafId}/observations")
    public ResponseEntity<List<LeafObservation>> getLeafObservations(@PathVariable String leafId) {
        return ResponseEntity.ok(leafService.getLeafObservations(leafId));
    }

    /**
     * 获取叶片最近的观测记录
     * 
     * GET /api/leaves/{leafId}/observations/recent?limit=10
     * 
     * @param leafId 叶片ID
     * @param limit 返回数量，默认10
     * @return 最近的N条记录
     */
    @GetMapping("/{leafId}/observations/recent")
    public ResponseEntity<List<LeafObservation>> getRecentObservations(
            @PathVariable String leafId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leafService.getRecentObservations(leafId, limit));
    }

    /**
     * 获取叶片统计信息
     * 
     * GET /api/leaves/statistics
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<LeafService.LeafStatistics> getStatistics() {
        return ResponseEntity.ok(leafService.getStatistics());
    }
}
