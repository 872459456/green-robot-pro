package com.greenrobot.monitor.repository;

import com.greenrobot.monitor.entity.LeafObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 叶片观测记录数据访问层
 * 
 * 提供叶片观测记录的数据库操作方法
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
@Repository
public interface LeafObservationRepository extends JpaRepository<LeafObservation, Long> {

    /**
     * 根据叶片ID查询所有观测记录，按时间倒序
     * 
     * @param leafId 叶片ID
     * @return 该叶片的所有观测记录
     */
    List<LeafObservation> findByLeafIdOrderByObservationTimeDesc(String leafId);

    /**
     * 查询某叶片最近N条观测记录
     * 
     * @param leafId 叶片ID
     * @param limit 返回记录数
     * @return 最近的N条记录
     */
    @Query("SELECT o FROM LeafObservation o WHERE o.leafId = :leafId ORDER BY o.observationTime DESC LIMIT :limit")
    List<LeafObservation> findRecentObservations(@org.springframework.data.repository.query.Param("leafId") String leafId, @org.springframework.data.repository.query.Param("limit") int limit);

    /**
     * 查询特定状态的观测记录
     * 
     * @param status 健康状态
     * @return 该状态的所有观测记录
     */
    List<LeafObservation> findByStatusOrderByObservationTimeDesc(String status);

    /**
     * 查询某时间范围内的观测记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 该时间范围内的记录
     */
    List<LeafObservation> findByObservationTimeBetweenOrderByObservationTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询某叶片在特定时间的观测记录
     * 
     * @param leafId 叶片ID
     * @param observationTime 观测时间
     * @return 该时间的记录
     */
    List<LeafObservation> findByLeafIdAndObservationTime(String leafId, LocalDateTime observationTime);

    /**
     * 统计某叶片的历史最大面积
     * 
     * @param leafId 叶片ID
     * @return 最大面积
     */
    @Query("SELECT MAX(o.area) FROM LeafObservation o WHERE o.leafId = :leafId")
    Double findMaxAreaByLeafId(String leafId);

    /**
     * 查询某叶片的最新观测记录
     * 
     * @param leafId 叶片ID
     * @return 最新记录
     */
    List<LeafObservation> findTop1ByLeafIdOrderByObservationTimeDesc(String leafId);
}
