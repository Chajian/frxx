package com.xiancore.systems.boss.damage;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 伤害统计信息
 * 存储Boss的伤害统计数据
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
@Setter
public class DamageStatistics {

    // ==================== 基础统计 ====================
    /** 总伤害 */
    private double totalDamage = 0.0;

    /** 平均单次伤害 */
    private double averageSingleDamage = 0.0;

    /** 平均伤害 (别名) */
    private double averageDamage = 0.0;

    /** 最大单次伤害 */
    private double maxSingleDamage = 0.0;

    /** 最小单次伤害 */
    private double minSingleDamage = Double.MAX_VALUE;

    // ==================== 性能统计 ====================
    /** 每秒伤害 (DPS) */
    private double dps = 0.0;

    /** 平均每秒伤害 */
    private double averageDpsPerPlayer = 0.0;

    // ==================== 参与人数和次数 ====================
    /** 总参与玩家数 */
    private int totalPlayers = 0;

    /** 总伤害次数 */
    private int totalDamageCount = 0;

    // ==================== 时间统计 ====================
    /** 开始时间 (毫秒时间戳) */
    private long startTime = System.currentTimeMillis();

    /** 结束时间 (毫秒时间戳) */
    private long endTime = System.currentTimeMillis();

    /** 持续时间 (秒) */
    private long durationSeconds = 0;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public DamageStatistics() {
    }

    /**
     * 从DamageRecord计算统计
     *
     * @param record 伤害记录
     */
    public DamageStatistics(DamageRecord record) {
        calculate(record);
    }

    // ==================== 计算方法 ====================

    /**
     * 从DamageRecord计算统计信息
     *
     * @param record 伤害记录
     */
    public void calculate(DamageRecord record) {
        if (record == null) {
            return;
        }

        this.totalDamage = record.getTotalDamage();
        this.totalDamageCount = record.getTotalDamageCount();
        this.totalPlayers = record.getParticipantCount();

        // 计算平均伤害
        if (totalDamageCount > 0) {
            this.averageSingleDamage = totalDamage / totalDamageCount;
        }

        // 计算最大和最小伤害
        if (!record.getPlayerDamageMap().isEmpty()) {
            this.maxSingleDamage = record.getPlayerDamageMap().values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
            this.minSingleDamage = record.getPlayerDamageMap().values().stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
        }

        // 计算平均每玩家伤害
        if (totalPlayers > 0) {
            this.averageDpsPerPlayer = totalDamage / totalPlayers;
        }

        // 计算持续时间
        if (durationSeconds > 0) {
            this.dps = totalDamage / durationSeconds;
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取中等伤害 (用于分析)
     *
     * @param record 伤害记录
     * @return 中等伤害值
     */
    public double getMedianDamage(DamageRecord record) {
        if (record == null || record.getPlayerDamageMap().isEmpty()) {
            return 0.0;
        }

        List<Double> damages = new ArrayList<>(record.getPlayerDamageMap().values());
        damages.sort(Double::compareTo);

        int size = damages.size();
        if (size % 2 == 0) {
            return (damages.get(size / 2 - 1) + damages.get(size / 2)) / 2;
        } else {
            return damages.get(size / 2);
        }
    }

    /**
     * 获取伤害分布 (用于可视化)
     *
     * @param record 伤害记录
     * @param buckets 桶数
     * @return 伤害分布 (范围 -> 个数)
     */
    public Map<String, Integer> getDistribution(DamageRecord record, int buckets) {
        if (record == null || record.getPlayerDamageMap().isEmpty() || buckets <= 0) {
            return new LinkedHashMap<>();
        }

        double min = record.getPlayerDamageMap().values().stream()
            .mapToDouble(Double::doubleValue)
            .min()
            .orElse(0.0);

        double max = record.getPlayerDamageMap().values().stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);
        double range = max - min;
        if (range == 0) {
            Map<String, Integer> result = new LinkedHashMap<>();
            result.put(String.format("%.1f", min), record.getParticipantCount());
            return result;
        }

        Map<String, Integer> distribution = new LinkedHashMap<>();
        double bucketSize = range / buckets;

        for (int i = 0; i < buckets; i++) {
            double rangeStart = min + i * bucketSize;
            double rangeEnd = rangeStart + bucketSize;
            String key = String.format("%.1f-%.1f", rangeStart, rangeEnd);
            distribution.put(key, 0);
        }

        for (double damage : record.getPlayerDamageMap().values()) {
            int bucketIndex = (int) ((damage - min) / bucketSize);
            if (bucketIndex >= buckets) {
                bucketIndex = buckets - 1;
            }

            String key = distribution.keySet().stream()
                .skip(bucketIndex)
                .findFirst()
                .orElse(null);

            if (key != null) {
                distribution.put(key, distribution.get(key) + 1);
            }
        }

        return distribution;
    }

    // ==================== 转换方法 ====================

    /**
     * 转换为Map (用于序列化)
     *
     * @return 数据映射
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalDamage", totalDamage);
        map.put("averageSingleDamage", averageSingleDamage);
        map.put("maxSingleDamage", maxSingleDamage);
        map.put("minSingleDamage", minSingleDamage);
        map.put("dps", dps);
        map.put("averageDpsPerPlayer", averageDpsPerPlayer);
        map.put("totalPlayers", totalPlayers);
        map.put("totalDamageCount", totalDamageCount);
        map.put("durationSeconds", durationSeconds);
        return map;
    }

    // ==================== 信息方法 ====================

    /**
     * 获取简要信息
     *
     * @return 信息字符串
     */
    public String getSimpleInfo() {
        return String.format(
            "DamageStats{total=%.1f, dps=%.1f, players=%d, count=%d}",
            totalDamage, dps, totalPlayers, totalDamageCount
        );
    }

    /**
     * 获取详细信息
     *
     * @return 详细信息字符串
     */
    public String getDetailedInfo() {
        return String.format(
            "DamageStatistics{\\n" +
            "  总伤害: %.1f\\n" +
            "  平均单次伤害: %.1f\\n" +
            "  最大单次伤害: %.1f\\n" +
            "  最小单次伤害: %.1f\\n" +
            "  DPS: %.1f\\n" +
            "  平均DPS/玩家: %.1f\\n" +
            "  参与玩家: %d\\n" +
            "  伤害次数: %d\\n" +
            "  持续时间: %d秒\\n" +
            "}",
            totalDamage, averageSingleDamage, maxSingleDamage, minSingleDamage,
            dps, averageDpsPerPlayer, totalPlayers, totalDamageCount, durationSeconds
        );
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }
}
