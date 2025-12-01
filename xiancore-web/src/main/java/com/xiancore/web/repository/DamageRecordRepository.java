package com.xiancore.web.repository;

import com.xiancore.web.entity.DamageRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DamageRecord Repository 接口
 * 处理伤害记录的数据库操作
 */
@Repository
public interface DamageRecordRepository extends JpaRepository<DamageRecord, String> {

    /**
     * 查询指定Boss的所有伤害记录
     */
    List<DamageRecord> findByBossId(String bossId);

    /**
     * 查询指定玩家造成的所有伤害记录
     */
    List<DamageRecord> findByPlayerId(String playerId);

    /**
     * 查询指定Boss的伤害记录，按时间倒序
     */
    List<DamageRecord> findByBossIdOrderByDamageTimeDesc(String bossId);

    /**
     * 查询指定Boss的伤害记录，按时间倒序并分页
     */
    Page<DamageRecord> findByBossIdOrderByDamageTimeDesc(String bossId, Pageable pageable);

    /**
     * 查询指定玩家对特定Boss的伤害记录
     */
    List<DamageRecord> findByBossIdAndPlayerId(String bossId, String playerId);

    /**
     * 获取Boss的总伤害值
     */
    @Query("SELECT SUM(d.damage) FROM DamageRecord d WHERE d.boss.id = :bossId")
    Double getTotalDamageForBoss(@Param("bossId") String bossId);

    /**
     * 获取玩家对Boss的总伤害值
     */
    @Query("SELECT SUM(d.damage) FROM DamageRecord d WHERE d.boss.id = :bossId AND d.playerId = :playerId")
    Double getTotalDamageByPlayer(@Param("bossId") String bossId, @Param("playerId") String playerId);

    /**
     * 获取Boss的伤害排名 (玩家名称, 伤害值)
     */
    @Query(value = "SELECT d.player_name, SUM(d.damage) as totalDamage " +
            "FROM damage_records d " +
            "WHERE d.boss_id = :bossId " +
            "GROUP BY d.player_id " +
            "ORDER BY totalDamage DESC",
            nativeQuery = true)
    List<Object[]> getDamageRankingByBoss(@Param("bossId") String bossId);

    /**
     * 获取Boss的伤害排名，分页
     */
    @Query(value = "SELECT d.player_name, SUM(d.damage) as totalDamage " +
            "FROM damage_records d " +
            "WHERE d.boss_id = :bossId " +
            "GROUP BY d.player_id " +
            "ORDER BY totalDamage DESC",
            nativeQuery = true)
    Page<Object[]> getDamageRankingByBoss(@Param("bossId") String bossId, Pageable pageable);

    /**
     * 获取特定时间范围内的伤害记录
     */
    List<DamageRecord> findByDamageTimeBetween(Long startTime, Long endTime);

    /**
     * 获取指定伤害类型的记录
     */
    List<DamageRecord> findByDamageType(String damageType);

    /**
     * 统计Boss被伤害的次数
     */
    @Query("SELECT COUNT(d) FROM DamageRecord d WHERE d.boss.id = :bossId")
    Long countDamageRecordsByBoss(@Param("bossId") String bossId);

    /**
     * 获取玩家造成的总伤害值
     */
    @Query("SELECT SUM(d.damage) FROM DamageRecord d WHERE d.playerId = :playerId")
    Double getTotalDamageByPlayerId(@Param("playerId") String playerId);

    /**
     * 获取玩家参与过的Boss击杀次数
     */
    @Query("SELECT COUNT(DISTINCT d.boss.id) FROM DamageRecord d WHERE d.playerId = :playerId AND d.boss.status = 'DEAD'")
    Long countBossesDamagedByPlayer(@Param("playerId") String playerId);

    /**
     * 获取最高单次伤害记录
     */
    @Query("SELECT d FROM DamageRecord d ORDER BY d.damage DESC")
    Page<DamageRecord> findHighestDamageRecords(Pageable pageable);

    /**
     * 获取指定玩家对所有Boss的伤害统计
     */
    @Query(value = "SELECT d.boss_id, d.boss.name, SUM(d.damage) as totalDamage " +
            "FROM damage_records d " +
            "WHERE d.player_id = :playerId " +
            "GROUP BY d.boss_id " +
            "ORDER BY totalDamage DESC",
            nativeQuery = true)
    List<Object[]> getPlayerDamageStatistics(@Param("playerId") String playerId);
}
