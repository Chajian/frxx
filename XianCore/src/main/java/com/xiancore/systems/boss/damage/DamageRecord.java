package com.xiancore.systems.boss.damage;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss 伤害记录
 * 记录单个 Boss 的所有伤害数据
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class DamageRecord {

    // ==================== 基础信息 ====================
    /** Boss UUID */
    private final UUID bossUUID;

    /** 开始记录时间 */
    private final long startTime;

    // ==================== 伤害数据 ====================
    /** 玩家伤害记录 (玩家UUID -> 伤害值) */
    private final Map<UUID, Double> playerDamage = new ConcurrentHashMap<>();

    /** 玩家命中次数 (玩家UUID -> 次数) */
    private final Map<UUID, Integer> playerHitCounts = new ConcurrentHashMap<>();

    /** 玩家最后伤害时间 (玩家UUID -> 时间戳) */
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();

    /** 总伤害 */
    private volatile double totalDamage = 0.0;

    /** 总伤害次数 */
    private volatile int totalDamageCount = 0;

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param bossUUID Boss UUID
     */
    public DamageRecord(UUID bossUUID) {
        this.bossUUID = bossUUID;
        this.startTime = System.currentTimeMillis();
    }

    // ==================== 伤害记录方法 ====================

    /**
     * 记录伤害
     *
     * @param player 玩家
     * @param damage 伤害值
     */
    public void recordDamage(Player player, double damage) {
        recordDamage(player.getUniqueId(), damage);
    }

    /**
     * 记录伤害
     *
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     */
    public void recordDamage(UUID playerUUID, double damage) {
        if (damage <= 0) return;

        // 更新玩家伤害
        playerDamage.merge(playerUUID, damage, Double::sum);

        // 更新命中次数
        playerHitCounts.merge(playerUUID, 1, Integer::sum);

        // 更新最后伤害时间
        lastDamageTime.put(playerUUID, System.currentTimeMillis());

        // 更新总伤害
        totalDamage += damage;
        totalDamageCount++;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取玩家伤害
     *
     * @param playerUUID 玩家UUID
     * @return 伤害值
     */
    public double getPlayerDamage(UUID playerUUID) {
        return playerDamage.getOrDefault(playerUUID, 0.0);
    }

    /**
     * 获取玩家命中次数
     *
     * @param playerUUID 玩家UUID
     * @return 命中次数
     */
    public int getHitCount(UUID playerUUID) {
        return playerHitCounts.getOrDefault(playerUUID, 0);
    }

    /**
     * 获取玩家最后伤害时间
     *
     * @param playerUUID 玩家UUID
     * @return 时间戳，如果没有记录则返回0
     */
    public long getLastDamageTime(UUID playerUUID) {
        return lastDamageTime.getOrDefault(playerUUID, 0L);
    }

    /**
     * 获取参与者列表
     *
     * @return 参与者UUID列表
     */
    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(playerDamage.keySet());
    }

    /**
     * 获取玩家伤害映射 (不可变)
     *
     * @return Map<玩家UUID, 伤害总量>
     */
    public Map<UUID, Double> getPlayerDamageMap() {
        return Collections.unmodifiableMap(playerDamage);
    }

    /**
     * 获取参与者数量
     *
     * @return 参与者数量
     */
    public int getParticipantCount() {
        return playerDamage.size();
    }

    /**
     * 获取总伤害
     *
     * @return 总伤害
     */
    public double getTotalDamage() {
        return totalDamage;
    }

    /**
     * 获取总伤害次数
     *
     * @return 总伤害次数
     */
    public int getTotalDamageCount() {
        return totalDamageCount;
    }

    /**
     * 获取玩家伤害占比
     *
     * @param playerUUID 玩家UUID
     * @return 伤害占比 (0.0-1.0)
     */
    public double getDamagePercentage(UUID playerUUID) {
        if (totalDamage <= 0) return 0.0;
        double damage = getPlayerDamage(playerUUID);
        return damage / totalDamage;
    }

    /**
     * 获取排序后的伤害列表
     *
     * @return 按伤害降序排列的玩家UUID列表
     */
    public List<UUID> getSortedPlayers() {
        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(playerDamage.entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        List<UUID> result = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : entries) {
            result.add(entry.getKey());
        }
        return result;
    }

    /**
     * 获取前N名伤害玩家
     *
     * @param n 数量
     * @return 玩家UUID列表
     */
    public List<UUID> getTopPlayers(int n) {
        List<UUID> sorted = getSortedPlayers();
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    /**
     * 获取玩家伤害排名
     *
     * @param playerUUID 玩家UUID
     * @return 排名 (1-based)，如果不存在则返回-1
     */
    public int getPlayerRank(UUID playerUUID) {
        List<UUID> sorted = getSortedPlayers();
        int rank = sorted.indexOf(playerUUID);
        return rank >= 0 ? rank + 1 : -1;
    }

    /**
     * 清空所有记录
     */
    public void clear() {
        playerDamage.clear();
        playerHitCounts.clear();
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
        return String.format("DamageRecord{boss=%s, participants=%d, totalDamage=%.1f}",
            bossUUID.toString().substring(0, 8), getParticipantCount(), totalDamage);
    }

    /**
     * 获取详细信息
     *
     * @return 详细信息字符串
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Boss伤害记录 ===\n");
        sb.append(String.format("Boss UUID: %s\n", bossUUID));
        sb.append(String.format("总伤害: %.1f\n", totalDamage));
        sb.append(String.format("总次数: %d\n", totalDamageCount));
        sb.append(String.format("参与者: %d人\n", getParticipantCount()));
        sb.append(String.format("记录时长: %ds\n", (System.currentTimeMillis() - startTime) / 1000));
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }
}
