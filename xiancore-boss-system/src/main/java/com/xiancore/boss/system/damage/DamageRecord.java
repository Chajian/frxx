package com.xiancore.boss.system.damage;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 伤害记录
 * 记录单个Boss的所有伤害数据
 *
 * 职责:
 * - 记录玩家对Boss的伤害
 * - 维护伤害统计数据
 * - 提供伤害查询方法
 * - 计算伤害相关统计
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
@Setter
public class DamageRecord {

    // ==================== 基础信息 ====================
    /** Boss UUID */
    private UUID bossUUID;

    /** Boss类型 (如 "SkeletonKing") */
    private String bossType = "Unknown";

    /** Boss等级 (1-4) */
    private int bossTier = 1;

    /** 记录创建时间 */
    private long recordTime = System.currentTimeMillis();

    // ==================== 伤害数据 ====================
    /** 玩家伤害记录 (玩家UUID -> 伤害值) */
    private final Map<UUID, Double> playerDamage = new ConcurrentHashMap<>();

    /** 玩家伤害次数 (玩家UUID -> 伤害次数) */
    private final Map<UUID, Integer> damageCount = new ConcurrentHashMap<>();

    /** 玩家最后伤害时间 (玩家UUID -> 时间戳) */
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();

    /** 总伤害 */
    private volatile double totalDamage = 0.0;

    /** 总伤害次数 */
    private volatile int totalDamageCount = 0;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public DamageRecord() {
    }

    /**
     * 构造函数 (指定Boss UUID)
     *
     * @param bossUUID Boss UUID
     */
    public DamageRecord(UUID bossUUID) {
        this.bossUUID = bossUUID;
    }

    /**
     * 构造函数 (指定Boss信息)
     *
     * @param bossUUID Boss UUID
     * @param bossType Boss类型
     * @param bossTier Boss等级
     */
    public DamageRecord(UUID bossUUID, String bossType, int bossTier) {
        this.bossUUID = bossUUID;
        this.bossType = bossType;
        this.bossTier = bossTier;
    }

    // ==================== 伤害记录方法 ====================

    /**
     * 记录玩家伤害
     *
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     * @param timestamp 时间戳
     */
    public void recordDamage(UUID playerUUID, double damage, long timestamp) {
        if (playerUUID == null || damage <= 0) {
            return;
        }

        // 记录伤害
        playerDamage.merge(playerUUID, damage, Double::sum);
        totalDamage += damage;

        // 记录伤害次数
        damageCount.merge(playerUUID, 1, Integer::sum);
        totalDamageCount++;

        // 记录最后伤害时间
        lastDamageTime.put(playerUUID, timestamp);
    }

    /**
     * 记录玩家伤害 (便捷版本，使用当前时间)
     *
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     */
    public void recordDamage(UUID playerUUID, double damage) {
        recordDamage(playerUUID, damage, System.currentTimeMillis());
    }

    // ==================== 伤害查询方法 ====================

    /**
     * 获取玩家对Boss的伤害
     *
     * @param playerUUID 玩家UUID
     * @return 伤害值
     */
    public double getPlayerDamage(UUID playerUUID) {
        return playerDamage.getOrDefault(playerUUID, 0.0);
    }

    /**
     * 获取玩家的伤害次数
     *
     * @param playerUUID 玩家UUID
     * @return 伤害次数
     */
    public int getPlayerDamageCount(UUID playerUUID) {
        return damageCount.getOrDefault(playerUUID, 0);
    }

    /**
     * 获取玩家的伤害百分比
     *
     * @param playerUUID 玩家UUID
     * @return 伤害百分比 (0.0-1.0)
     */
    public double getPlayerDamagePercentage(UUID playerUUID) {
        if (totalDamage <= 0) {
            return 0.0;
        }

        return getPlayerDamage(playerUUID) / totalDamage;
    }

    /**
     * 获取前N名伤害者
     *
     * @param limit 限制数量
     * @return 排行榜 (玩家UUID列表，从高到低)
     */
    public List<UUID> getTopDamagers(int limit) {
        return playerDamage.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 获取前N名伤害者 (详细信息)
     *
     * @param limit 限制数量
     * @return 包含伤害值的排行列表
     */
    public List<Map.Entry<UUID, Double>> getTopDamagersDetailed(int limit) {
        return playerDamage.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有参与伤害的玩家
     *
     * @return 玩家UUID集合
     */
    public Set<UUID> getParticipants() {
        return new HashSet<>(playerDamage.keySet());
    }

    /**
     * 获取参与人数
     *
     * @return 参与人数
     */
    public int getParticipantCount() {
        return playerDamage.size();
    }

    // ==================== 统计方法 ====================

    /**
     * 获取玩家的平均单次伤害
     *
     * @param playerUUID 玩家UUID
     * @return 平均伤害
     */
    public double getPlayerAverageDamage(UUID playerUUID) {
        int count = getPlayerDamageCount(playerUUID);
        if (count == 0) {
            return 0.0;
        }

        return getPlayerDamage(playerUUID) / count;
    }

    /**
     * 获取平均每玩家伤害
     *
     * @return 平均伤害
     */
    public double getAverageDamagePerPlayer() {
        if (playerDamage.isEmpty()) {
            return 0.0;
        }

        return totalDamage / playerDamage.size();
    }

    /**
     * 获取最高伤害玩家
     *
     * @return 最高伤害玩家UUID，如果无数据则返回null
     */
    public UUID getTopDamager() {
        return playerDamage.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * 获取最高单次伤害
     *
     * @return 最高伤害值
     */
    public double getMaxDamage() {
        return playerDamage.values().stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
    }

    /**
     * 清空所有伤害数据
     */
    public void clear() {
        playerDamage.clear();
        damageCount.clear();
        lastDamageTime.clear();
        totalDamage = 0.0;
        totalDamageCount = 0;
    }

    // ==================== 信息方法 ====================

    /**
     * 获取简要信息
     *
     * @return 信息字符串
     */
    public String getSimpleInfo() {
        return String.format(
            "DamageRecord{type=%s, tier=%d, totalDamage=%.1f, players=%d}",
            bossType, bossTier, totalDamage, playerDamage.size()
        );
    }

    /**
     * 获取详细信息
     *
     * @return 详细信息字符串
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("DamageRecord{\n");
        sb.append("  Boss UUID: ").append(bossUUID).append("\n");
        sb.append("  类型: ").append(bossType).append("\n");
        sb.append("  等级: ").append(bossTier).append("\n");
        sb.append("  总伤害: ").append(String.format("%.1f", totalDamage)).append("\n");
        sb.append("  伤害次数: ").append(totalDamageCount).append("\n");
        sb.append("  参与人数: ").append(playerDamage.size()).append("\n");
        sb.append("  平均伤害: ").append(String.format("%.1f", getAverageDamagePerPlayer())).append("\n");
        sb.append("  最高伤害者: ");

        UUID topDamager = getTopDamager();
        if (topDamager != null) {
            sb.append(topDamager).append(" (").append(String.format("%.1f", getPlayerDamage(topDamager))).append(")\n");
        } else {
            sb.append("无\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DamageRecord that = (DamageRecord) o;
        return Objects.equals(bossUUID, that.bossUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bossUUID);
    }
}
