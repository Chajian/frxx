package com.xiancore.systems.boss.entity;

import lombok.Getter;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss生成历史记录
 * 记录一个Boss的完整生命周期和相关数据
 *
 * 职责:
 * - 记录Boss的生成时间和位置
 * - 记录Boss的击杀者和伤害分布
 * - 存储Boss的统计数据
 * - 用于数据持久化和统计分析
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossSpawnHistory {

    // ==================== 基础信息 ====================
    /** Boss记录ID (数据库ID) */
    private String bossId;

    /** Boss UUID (唯一标识) */
    private UUID bossUUID;

    /** MythicMobs类型 */
    private String mythicMobType;

    /** Boss等级 (1-4) */
    private int tier;

    // ==================== 时间信息 ====================
    /** 生成时间 (毫秒时间戳) */
    private long spawnTime;

    /** 死亡时间 (毫秒时间戳) */
    private long deathTime;

    /** 存活时长 (毫秒) */
    private long aliveDuration;

    // ==================== 位置信息 ====================
    /** 生成位置 */
    private Location spawnLocation;

    // ==================== 击杀信息 ====================
    /** 击杀者UUID */
    private UUID killerUUID;

    /** 击杀者名字 */
    private String killerName;

    /** 击杀者等级 */
    private int killerLevel;

    // ==================== 伤害数据 ====================
    /** 总伤害 */
    private double totalDamage;

    /** 伤害分布 (玩家UUID -> 伤害值) */
    private final Map<UUID, Double> damageDistribution = new ConcurrentHashMap<>();

    /** 参与击杀的玩家数 */
    private int participantCount;

    // ==================== 奖励数据 ====================
    /** 总奖励经验 */
    private double totalExperience;

    /** 总奖励灵石 */
    private long totalSpiritStone;

    /** 奖励分布 (玩家UUID -> 奖励经验) */
    private final Map<UUID, Double> experienceDistribution = new ConcurrentHashMap<>();

    /** 奖励分布 (玩家UUID -> 奖励灵石) */
    private final Map<UUID, Long> spiritStoneDistribution = new ConcurrentHashMap<>();

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public BossSpawnHistory() {
    }

    /**
     * 从BossEntity创建历史记录
     *
     * @param boss Boss实体
     */
    public static BossSpawnHistory fromBossEntity(BossEntity boss) {
        BossSpawnHistory history = new BossSpawnHistory();
        history.bossUUID = boss.getBossUUID();
        history.mythicMobType = boss.getMythicMobType();
        history.tier = boss.getTier();
        history.spawnTime = boss.getSpawnTime();
        history.spawnLocation = boss.getSpawnLocation().clone();
        history.totalDamage = boss.getTotalDamage();
        history.participantCount = boss.getParticipants().size();

        // 复制伤害分布
        for (UUID playerUUID : boss.getParticipants()) {
            history.damageDistribution.put(playerUUID, boss.getPlayerDamage(playerUUID));
        }

        return history;
    }

    // ==================== 时间相关方法 ====================

    /**
     * 标记为已击杀
     *
     * @param killer 击杀者
     */
    public void markAsKilled(org.bukkit.entity.Player killer) {
        this.deathTime = System.currentTimeMillis();
        this.aliveDuration = deathTime - spawnTime;
        this.killerUUID = killer.getUniqueId();
        this.killerName = killer.getName();
    }

    /**
     * 标记为已消失
     */
    public void markAsDespawned() {
        this.deathTime = System.currentTimeMillis();
        this.aliveDuration = deathTime - spawnTime;
    }

    /**
     * 获取存活时长 (秒)
     */
    public long getAliveDurationSeconds() {
        return aliveDuration / 1000;
    }

    /**
     * 获取存活时长 (分钟)
     */
    public double getAliveDurationMinutes() {
        return (double) aliveDuration / 60000;
    }

    /**
     * 获取是否被击杀 (vs 自然消失)
     */
    public boolean wasKilled() {
        return killerUUID != null;
    }

    // ==================== 伤害相关方法 ====================

    /**
     * 添加伤害记录
     *
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     */
    public void addDamage(UUID playerUUID, double damage) {
        damageDistribution.merge(playerUUID, damage, Double::sum);
        totalDamage += damage;
    }

    /**
     * 获取玩家的伤害
     */
    public double getPlayerDamage(UUID playerUUID) {
        return damageDistribution.getOrDefault(playerUUID, 0.0);
    }

    /**
     * 获取玩家的伤害百分比
     */
    public double getPlayerDamagePercentage(UUID playerUUID) {
        if (totalDamage <= 0) {
            return 0.0;
        }
        return getPlayerDamage(playerUUID) / totalDamage;
    }

    /**
     * 获取伤害排行
     *
     * @param limit 限制数量
     * @return 伤害排行列表 (UUID列表，按伤害从高到低)
     */
    public List<UUID> getDamageRanking(int limit) {
        return damageDistribution.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * 获取前N名伤害者
     */
    public List<Map.Entry<UUID, Double>> getTopDamagers(int limit) {
        return damageDistribution.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .toList();
    }

    // ==================== 奖励相关方法 ====================

    /**
     * 添加经验奖励
     *
     * @param playerUUID 玩家UUID
     * @param experience 经验值
     */
    public void addExperienceReward(UUID playerUUID, double experience) {
        experienceDistribution.merge(playerUUID, experience, Double::sum);
        totalExperience += experience;
    }

    /**
     * 添加灵石奖励
     *
     * @param playerUUID 玩家UUID
     * @param spiritStone 灵石数量
     */
    public void addSpiritStoneReward(UUID playerUUID, long spiritStone) {
        spiritStoneDistribution.merge(playerUUID, spiritStone, Long::sum);
        totalSpiritStone += spiritStone;
    }

    /**
     * 获取玩家的经验奖励
     */
    public double getPlayerExperience(UUID playerUUID) {
        return experienceDistribution.getOrDefault(playerUUID, 0.0);
    }

    /**
     * 获取玩家的灵石奖励
     */
    public long getPlayerSpiritStone(UUID playerUUID) {
        return spiritStoneDistribution.getOrDefault(playerUUID, 0L);
    }

    // ==================== 统计方法 ====================

    /**
     * 获取平均每玩家的伤害
     */
    public double getAverageDamagePerPlayer() {
        if (participantCount == 0) {
            return 0.0;
        }
        return totalDamage / participantCount;
    }

    /**
     * 获取平均每玩家的伤害时间 (秒)
     */
    public double getAverageDamageTimePerPlayer() {
        if (participantCount == 0) {
            return 0.0;
        }
        return (double) getAliveDurationSeconds() / participantCount;
    }

    /**
     * 获取DPS (每秒伤害)
     */
    public double getDPS() {
        if (aliveDuration == 0) {
            return 0.0;
        }
        return totalDamage / getAliveDurationSeconds();
    }

    /**
     * 计算难度指数 (综合考虑等级、时长、伤害)
     */
    public double calculateDifficultyIndex() {
        // 基础难度: 根据等级
        double baseDifficulty = tier * 25.0;

        // 时间影响: 更长的时间=更高的难度
        double timeFactor = Math.log1p(getAliveDurationSeconds() / 60.0);  // 按分钟计算

        // 伤害影响: 需要的总伤害
        double damageFactor = Math.log1p(totalDamage / 100.0);

        return baseDifficulty * (1 + timeFactor * 0.1 + damageFactor * 0.1);
    }

    // ==================== 信息方法 ====================

    /**
     * 获取简要信息
     */
    public String getSimpleInfo() {
        return String.format(
            "BossHistory{type=%s, tier=%d, duration=%ds, damage=%.1f, killer=%s}",
            mythicMobType, tier, getAliveDurationSeconds(), totalDamage, killerName
        );
    }

    /**
     * 获取详细信息
     */
    public String getDetailedInfo() {
        return String.format(
            "BossSpawnHistory{\n" +
            "  类型: %s (Lv.%d)\n" +
            "  ID: %s\n" +
            "  生成时间: %d\n" +
            "  死亡时间: %d\n" +
            "  存活时长: %d秒 (%.1f分钟)\n" +
            "  生成位置: %s\n" +
            "  击杀者: %s (%s)\n" +
            "  总伤害: %.1f\n" +
            "  参与人数: %d\n" +
            "  DPS: %.1f\n" +
            "  难度指数: %.1f\n" +
            "  总奖励: 经验%.1f + 灵石%d\n" +
            "}",
            mythicMobType, tier,
            bossUUID,
            spawnTime, deathTime,
            getAliveDurationSeconds(), getAliveDurationMinutes(),
            spawnLocation,
            killerName, killerUUID,
            totalDamage,
            participantCount,
            getDPS(),
            calculateDifficultyIndex(),
            totalExperience, totalSpiritStone
        );
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BossSpawnHistory that = (BossSpawnHistory) o;
        return Objects.equals(bossUUID, that.bossUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bossUUID);
    }
}
