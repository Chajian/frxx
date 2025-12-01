package com.xiancore.web.repository;

import com.xiancore.web.entity.Boss;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Boss Repository 接口
 * 处理Boss实体的数据库操作
 */
@Repository
public interface BossRepository extends JpaRepository<Boss, String> {

    /**
     * 根据Boss状态查询
     */
    List<Boss> findByStatus(String status);

    /**
     * 根据Boss所在世界查询
     */
    List<Boss> findByWorld(String world);

    /**
     * 根据Boss状态和ID查询
     */
    Optional<Boss> findByIdAndStatus(String id, String status);

    /**
     * 根据生成时间范围查询Boss
     */
    List<Boss> findBySpawnedTimeBetween(Long startTime, Long endTime);

    /**
     * 分页查询指定状态的Boss
     */
    @Query("SELECT b FROM Boss b WHERE b.status = :status ORDER BY b.spawnedTime DESC")
    Page<Boss> findActiveByStatus(@Param("status") String status, Pageable pageable);

    /**
     * 查询所有存活的Boss
     */
    @Query("SELECT b FROM Boss b WHERE b.status = 'ALIVE' ORDER BY b.spawnedTime DESC")
    List<Boss> findAllAliveBosses();

    /**
     * 查询指定世界的所有存活Boss
     */
    @Query("SELECT b FROM Boss b WHERE b.status = 'ALIVE' AND b.world = :world ORDER BY b.spawnedTime DESC")
    List<Boss> findAllAliveBossesByWorld(@Param("world") String world);

    /**
     * 查询已击杀的Boss数量
     */
    @Query("SELECT COUNT(b) FROM Boss b WHERE b.status = 'DEAD'")
    Long countKilledBosses();

    /**
     * 查询指定玩家击杀的Boss数量
     */
    @Query("SELECT COUNT(b) FROM Boss b WHERE b.status = 'DEAD' AND b.killerPlayerId = :playerId")
    Long countBossesKilledByPlayer(@Param("playerId") String playerId);

    /**
     * 查询指定玩家击杀的所有Boss
     */
    @Query("SELECT b FROM Boss b WHERE b.status = 'DEAD' AND b.killerPlayerId = :playerId ORDER BY b.killedTime DESC")
    List<Boss> findBossesKilledByPlayer(@Param("playerId") String playerId);

    /**
     * 查询最近击杀的N个Boss
     */
    @Query("SELECT b FROM Boss b WHERE b.status = 'DEAD' ORDER BY b.killedTime DESC")
    Page<Boss> findRecentlyKilledBosses(Pageable pageable);

    /**
     * 查询受伤害最多的Boss
     */
    @Query("SELECT b FROM Boss b WHERE b.status = 'DEAD' ORDER BY b.totalDamage DESC")
    Page<Boss> findMostDamagedBosses(Pageable pageable);

    /**
     * 查询难度最高的已击杀Boss
     */
    @Query("SELECT b FROM Boss b WHERE b.status = 'DEAD' ORDER BY b.difficultyLevel DESC")
    Page<Boss> findHighestDifficultyBosses(Pageable pageable);

    /**
     * 查询特定难度等级的Boss
     */
    List<Boss> findByDifficultyLevel(Integer difficultyLevel);

    /**
     * 查询指定类型的所有Boss
     */
    List<Boss> findByType(String type);
}
