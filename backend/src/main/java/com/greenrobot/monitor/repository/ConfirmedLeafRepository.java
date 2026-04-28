package com.greenrobot.monitor.repository;

import com.greenrobot.monitor.entity.ConfirmedLeaf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 已确认叶片档案数据访问层
 * 
 * 提供叶片档案的数据库操作方法
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
@Repository
public interface ConfirmedLeafRepository extends JpaRepository<ConfirmedLeaf, String> {

    /**
     * 查询所有叶片，按确认时间倒序
     * 
     * @return 按确认时间倒序的叶片列表
     */
    List<ConfirmedLeaf> findAllByOrderByConfirmedAtDesc();

    /**
     * 查询特定健康状态的叶片
     * 
     * @param status 健康状态
     * @return 该状态的所有叶片
     */
    List<ConfirmedLeaf> findByHealthStatusOrderByConfirmedAtDesc(String status);

    /**
     * 查询需要关注的叶片（黄叶、枯萎、预警状态）
     * 
     * @return 需要关注的叶片列表
     */
    @Query("SELECT l FROM ConfirmedLeaf l WHERE l.healthStatus IN ('YELLOW_LEAF', 'WILT', 'WILT_WARNING', 'TREND_YELLOW', 'TREND_WILT') ORDER BY l.confirmedAt DESC")
    List<ConfirmedLeaf> findLeavesNeedingAttention();

    /**
     * 根据位置范围查询叶片（用于位置分桶）
     * 
     * @param minX 最小X坐标
     * @param maxX 最大X坐标
     * @param minY 最小Y坐标
     * @param maxY 最大Y坐标
     * @return 该范围内的叶片
     */
    @Query("SELECT l FROM ConfirmedLeaf l WHERE l.positionX BETWEEN :minX AND :maxX AND l.positionY BETWEEN :minY AND :maxY")
    List<ConfirmedLeaf> findByPositionRange(Integer minX, Integer maxX, Integer minY, Integer maxY);

    /**
     * 统计各状态的叶片数量
     * 
     * @return 各状态的叶片数量
     */
    @Query("SELECT l.healthStatus, COUNT(l) FROM ConfirmedLeaf l GROUP BY l.healthStatus")
    List<Object[]> countByHealthStatus();
}
