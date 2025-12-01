package com.xiancore.web.repository;

import com.xiancore.web.entity.PlayerStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PlayerStats Repository 接口
 * 处理玩家统计数据的数据库操作
 */
@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, String> {

    /**
     * 根据玩家ID查询玩家统计
     */
    Optional<PlayerStats> findByPlayerId(String playerId);

    /**
     * 查询所有玩家，按Boss击杀数排序
     */
    List<PlayerStats> findAllByOrderByBossKillsDesc();

    /**
     * 查询所有玩家，按余额排序 (最富有)
     */
    List<PlayerStats> findAllByOrderByBalanceDesc();

    /**
     * 查询所有玩家，按伤害统计排序
     */
    List<PlayerStats> findAllByOrderByTotalDamageDesc();

    /**
     * 分页查询Boss击杀排名
     */
    @Query("SELECT p FROM PlayerStats p WHERE p.bossKills > 0 ORDER BY p.bossKills DESC, p.totalDamage DESC")
    Page<PlayerStats> getKillRanking(Pageable pageable);

    /**
     * 分页查询财富排名
     */
    @Query("SELECT p FROM PlayerStats p ORDER BY p.balance DESC")
    Page<PlayerStats> getWealthRanking(Pageable pageable);

    /**
     * 分页查询伤害排名
     */
    @Query("SELECT p FROM PlayerStats p ORDER BY p.totalDamage DESC")
    Page<PlayerStats> getDamageRanking(Pageable pageable);

    /**
     * 统计总玩家数
     */
    @Query("SELECT COUNT(p) FROM PlayerStats p")
    Long countTotalPlayers();

    /**
     * 统计至少击杀过一个Boss的玩家数
     */
    @Query("SELECT COUNT(p) FROM PlayerStats p WHERE p.bossKills > 0")
    Long countPlayersWithKills();

    /**
     * 获取总伤害值
     */
    @Query("SELECT SUM(p.totalDamage) FROM PlayerStats p")
    Double getTotalDamageAcrossAllPlayers();

    /**
     * 获取平均Boss击杀数
     */
    @Query("SELECT AVG(p.bossKills) FROM PlayerStats p WHERE p.bossKills > 0")
    Double getAverageBossKills();

    /**
     * 获取平均伤害值
     */
    @Query("SELECT AVG(p.totalDamage) FROM PlayerStats p WHERE p.totalDamage > 0")
    Double getAverageTotalDamage();

    /**
     * 获取最高Boss击杀数
     */
    @Query("SELECT MAX(p.bossKills) FROM PlayerStats p")
    Integer getMaxBossKills();

    /**
     * 获取最高余额
     */
    @Query("SELECT MAX(p.balance) FROM PlayerStats p")
    Double getMaxBalance();

    /**
     * 按击杀数获取玩家排名
     */
    @Query(value = "SELECT ROW_NUMBER() OVER (ORDER BY boss_kills DESC, total_damage DESC) as ranking, " +
            "player_id, player_name, boss_kills FROM player_stats",
            nativeQuery = true)
    List<Object[]> getPlayerKillRankingWithRank();

    /**
     * 查询指定排名范围的玩家
     */
    @Query("SELECT p FROM PlayerStats p WHERE p.killRanking BETWEEN :startRank AND :endRank " +
            "ORDER BY p.killRanking ASC")
    List<PlayerStats> findPlayersInKillRankRange(@Param("startRank") Integer startRank, @Param("endRank") Integer endRank);

    /**
     * 查询富人榜 (余额前N名)
     */
    @Query("SELECT p FROM PlayerStats p WHERE p.balance > 0 ORDER BY p.balance DESC")
    Page<PlayerStats> findRichestPlayers(Pageable pageable);

    /**
     * 查询最活跃玩家 (伤害最多)
     */
    @Query("SELECT p FROM PlayerStats p WHERE p.totalDamage > 0 ORDER BY p.totalDamage DESC")
    Page<PlayerStats> findMostActivePlayers(Pageable pageable);

    /**
     * 查询新玩家 (最近加入的玩家)
     */
    @Query("SELECT p FROM PlayerStats p ORDER BY p.createdAt DESC")
    Page<PlayerStats> findNewPlayers(Pageable pageable);

    /**
     * 按玩家名称模糊查询
     */
    List<PlayerStats> findByPlayerNameContaining(String playerName);

    /**
     * 查询指定名称的玩家
     */
    Optional<PlayerStats> findByPlayerName(String playerName);

    /**
     * 统计参与过战斗的玩家数
     */
    @Query("SELECT COUNT(p) FROM PlayerStats p WHERE p.totalBattles > 0")
    Long countPlayersInBattles();

    /**
     * 获取玩家的伤害贡献度排名
     */
    @Query(value = "SELECT player_name, total_damage, " +
            "ROUND((total_damage * 100.0) / (SELECT SUM(total_damage) FROM player_stats), 2) as contribution_percent " +
            "FROM player_stats WHERE total_damage > 0 ORDER BY total_damage DESC",
            nativeQuery = true)
    List<Object[]> getPlayerDamageContribution();
}
