package com.xiancore.web.service;

import com.xiancore.common.dto.PlayerStatsDTO;
import com.xiancore.web.entity.PlayerStats;
import com.xiancore.web.repository.PlayerStatsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 玩家统计 业务服务类
 * 处理所有与玩家统计相关的业务逻辑
 */
@Service
@Slf4j
@Transactional
public class PlayerStatsService {

    @Autowired
    private PlayerStatsRepository statsRepository;

    /**
     * 获取或创建玩家统计
     */
    public PlayerStats getOrCreatePlayerStats(String playerId, String playerName) {
        log.debug("获取或创建玩家统计: {} - {}", playerId, playerName);

        Optional<PlayerStats> existing = statsRepository.findByPlayerId(playerId);
        if (existing.isPresent()) {
            return existing.get();
        }

        PlayerStats newStats = PlayerStats.builder()
                .id(UUID.randomUUID().toString())
                .playerId(playerId)
                .playerName(playerName)
                .bossKills(0)
                .totalDamage(0.0)
                .totalBattles(0)
                .balance(0.0)
                .totalEarned(0.0)
                .totalSpent(0.0)
                .build();

        newStats.initializeDefaults();
        PlayerStats saved = statsRepository.save(newStats);
        log.info("创建新的玩家统计: {}", playerId);
        return saved;
    }

    /**
     * 获取玩家统计
     */
    public Optional<PlayerStats> getPlayerStats(String playerId) {
        log.debug("查询玩家统计: {}", playerId);
        return statsRepository.findByPlayerId(playerId);
    }

    /**
     * 增加玩家的Boss击杀数
     */
    public PlayerStats addBossKill(String playerId, Double reward) {
        log.info("增加玩家的Boss击杀数: {}", playerId);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.addBossKill();
        if (reward != null && reward > 0) {
            stats.addEarnings(reward);
        }

        return statsRepository.save(stats);
    }

    /**
     * 增加玩家的伤害统计
     */
    public PlayerStats addPlayerDamage(String playerId, Double damage) {
        log.debug("增加玩家伤害: {} - {}", playerId, damage);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.addDamage(damage);
        return statsRepository.save(stats);
    }

    /**
     * 增加玩家参与的战斗计数
     */
    public PlayerStats addBattle(String playerId) {
        log.debug("增加玩家战斗计数: {}", playerId);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.addBattle();
        return statsRepository.save(stats);
    }

    /**
     * 增加玩家收入
     */
    public PlayerStats addEarnings(String playerId, Double amount) {
        log.info("增加玩家收入: {} - {}", playerId, amount);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.addEarnings(amount);
        return statsRepository.save(stats);
    }

    /**
     * 增加玩家支出
     */
    public PlayerStats addSpending(String playerId, Double amount) {
        log.info("增加玩家支出: {} - {}", playerId, amount);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.addSpending(amount);
        return statsRepository.save(stats);
    }

    /**
     * 增加玩家余额
     */
    public PlayerStats addBalance(String playerId, Double amount) {
        log.debug("增加玩家余额: {} - {}", playerId, amount);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.addBalance(amount);
        return statsRepository.save(stats);
    }

    /**
     * 减少玩家余额
     */
    public PlayerStats subtractBalance(String playerId, Double amount) {
        log.debug("减少玩家余额: {} - {}", playerId, amount);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.subtractBalance(amount);
        return statsRepository.save(stats);
    }

    /**
     * 获取Boss击杀排名
     */
    public Page<PlayerStats> getKillRanking(Pageable pageable) {
        log.debug("查询Boss击杀排名");
        return statsRepository.getKillRanking(pageable);
    }

    /**
     * 获取财富排名
     */
    public Page<PlayerStats> getWealthRanking(Pageable pageable) {
        log.debug("查询财富排名");
        return statsRepository.getWealthRanking(pageable);
    }

    /**
     * 获取伤害排名
     */
    public Page<PlayerStats> getDamageRanking(Pageable pageable) {
        log.debug("查询伤害排名");
        return statsRepository.getDamageRanking(pageable);
    }

    /**
     * 获取所有玩家，按击杀数排序
     */
    public List<PlayerStats> getAllPlayersByKills() {
        log.debug("查询所有玩家 - 按击杀数排序");
        return statsRepository.findAllByOrderByBossKillsDesc();
    }

    /**
     * 获取所有玩家，按余额排序
     */
    public List<PlayerStats> getAllPlayersByWealth() {
        log.debug("查询所有玩家 - 按余额排序");
        return statsRepository.findAllByOrderByBalanceDesc();
    }

    /**
     * 获取所有玩家，按伤害排序
     */
    public List<PlayerStats> getAllPlayersByDamage() {
        log.debug("查询所有玩家 - 按伤害排序");
        return statsRepository.findAllByOrderByTotalDamageDesc();
    }

    /**
     * 查询新玩家
     */
    public Page<PlayerStats> getNewPlayers(Pageable pageable) {
        log.debug("查询新玩家");
        return statsRepository.findNewPlayers(pageable);
    }

    /**
     * 查询富人榜
     */
    public Page<PlayerStats> getRichestPlayers(Pageable pageable) {
        log.debug("查询富人榜");
        return statsRepository.findRichestPlayers(pageable);
    }

    /**
     * 查询最活跃玩家
     */
    public Page<PlayerStats> getMostActivePlayers(Pageable pageable) {
        log.debug("查询最活跃玩家");
        return statsRepository.findMostActivePlayers(pageable);
    }

    /**
     * 按名称搜索玩家
     */
    public List<PlayerStats> searchPlayerByName(String playerName) {
        log.debug("按名称搜索玩家: {}", playerName);
        return statsRepository.findByPlayerNameContaining(playerName);
    }

    /**
     * 统计总玩家数
     */
    public Long countTotalPlayers() {
        log.debug("统计总玩家数");
        return statsRepository.countTotalPlayers();
    }

    /**
     * 统计至少击杀过一个Boss的玩家数
     */
    public Long countPlayersWithKills() {
        log.debug("统计击杀过Boss的玩家数");
        return statsRepository.countPlayersWithKills();
    }

    /**
     * 获取总伤害值
     */
    public Double getTotalDamageAcrossAllPlayers() {
        log.debug("获取所有玩家的总伤害");
        Double total = statsRepository.getTotalDamageAcrossAllPlayers();
        return total != null ? total : 0.0;
    }

    /**
     * 获取平均Boss击杀数
     */
    public Double getAverageBossKills() {
        log.debug("获取平均Boss击杀数");
        Double average = statsRepository.getAverageBossKills();
        return average != null ? average : 0.0;
    }

    /**
     * 获取平均伤害值
     */
    public Double getAverageTotalDamage() {
        log.debug("获取平均伤害值");
        Double average = statsRepository.getAverageTotalDamage();
        return average != null ? average : 0.0;
    }

    /**
     * 获取最高Boss击杀数
     */
    public Integer getMaxBossKills() {
        log.debug("获取最高Boss击杀数");
        Integer max = statsRepository.getMaxBossKills();
        return max != null ? max : 0;
    }

    /**
     * 获取最高余额
     */
    public Double getMaxBalance() {
        log.debug("获取最高余额");
        Double max = statsRepository.getMaxBalance();
        return max != null ? max : 0.0;
    }

    /**
     * 获取玩家的平均每次战斗伤害
     */
    public Double getPlayerAverageDamagePerBattle(String playerId) {
        log.debug("获取玩家的平均每次战斗伤害: {}", playerId);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        return stats.getAverageDamagePerBattle();
    }

    /**
     * 获取玩家的平均每次击杀伤害
     */
    public Double getPlayerAverageDamagePerKill(String playerId) {
        log.debug("获取玩家的平均每次击杀伤害: {}", playerId);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        return stats.getAverageDamagePerKill();
    }

    /**
     * 获取玩家的财富等级描述
     */
    public String getPlayerWealthLevel(String playerId) {
        log.debug("获取玩家的财富等级: {}", playerId);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        return stats.getWealthLevel();
    }

    /**
     * 重置玩家统计 (管理员操作)
     */
    public PlayerStats resetPlayerStats(String playerId) {
        log.warn("重置玩家统计: {}", playerId);

        PlayerStats stats = statsRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalArgumentException("玩家统计不存在: " + playerId));

        stats.setBossKills(0);
        stats.setTotalDamage(0.0);
        stats.setTotalBattles(0);
        stats.setBalance(0.0);
        stats.setTotalEarned(0.0);
        stats.setTotalSpent(0.0);
        stats.setKillRanking(null);
        stats.setWealthRanking(null);

        return statsRepository.save(stats);
    }

    /**
     * 获取玩家的伤害贡献度
     */
    public List<Object[]> getPlayerDamageContribution() {
        log.debug("获取所有玩家的伤害贡献度");
        return statsRepository.getPlayerDamageContribution();
    }

    /**
     * 更新所有玩家的排名 (可定期运行)
     */
    public void updateAllPlayerRankings() {
        log.info("更新所有玩家的排名");

        // 更新击杀排名
        List<PlayerStats> killRanking = statsRepository.findAllByOrderByBossKillsDesc();
        for (int i = 0; i < killRanking.size(); i++) {
            killRanking.get(i).setKillRanking(i + 1);
        }

        // 更新财富排名
        List<PlayerStats> wealthRanking = statsRepository.findAllByOrderByBalanceDesc();
        for (int i = 0; i < wealthRanking.size(); i++) {
            wealthRanking.get(i).setWealthRanking(i + 1);
        }

        statsRepository.saveAll(killRanking);
        statsRepository.saveAll(wealthRanking);
        log.info("排名更新完成");
    }
}
