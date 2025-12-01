package com.xiancore.boss.system.reward;

import lombok.Getter;

import java.util.*;

/**
 * Boss奖励计算器
 * 计算和分配Boss击杀的奖励
 *
 * 职责:
 * - 计算基础奖励 (经验/精魄)
 * - 应用品质倍数
 * - 应用难度倍数
 * - 按伤害百分比分配奖励
 * - 计算掉落物品
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossRewardCalculator {

    // ==================== 配置常量 ====================

    /** 每个Tier等级的基础经验 */
    private static final double BASE_EXPERIENCE_PER_TIER = 100.0;

    /** 每个Tier等级的基础精魄 */
    private static final double BASE_SPIRITS_PER_TIER = 10.0;

    /** 击杀者额外奖励倍数 */
    private static final double KILL_STRIKE_BONUS = 0.2; // 额外20%

    /** 最小伤害阈值 (低于此数值的玩家不获得奖励) */
    private static final double MIN_DAMAGE_THRESHOLD = 0.01;

    // ==================== 内部状态 ====================

    /** Boss UUID */
    private final UUID bossUUID;

    /** Boss等级 */
    private final int bossLevel;

    /** 基础经验值 */
    private volatile double baseExperience = 0;

    /** 基础精魄值 */
    private volatile double baseSpirits = 0;

    // ==================== 构造函数 ====================

    public BossRewardCalculator(UUID bossUUID, int bossLevel) {
        this.bossUUID = bossUUID;
        this.bossLevel = bossLevel;
        this.baseExperience = BASE_EXPERIENCE_PER_TIER * bossLevel;
        this.baseSpirits = BASE_SPIRITS_PER_TIER * bossLevel;
    }

    // ==================== 核心计算方法 ====================

    /**
     * 计算最终奖励
     *
     * @param baseReward 基础奖励
     * @param qualityMultiplier 品质倍数
     * @param difficultyMultiplier 难度倍数
     * @return 最终奖励
     */
    public double calculateFinalReward(double baseReward,
                                      double qualityMultiplier,
                                      double difficultyMultiplier) {
        return baseReward * qualityMultiplier * difficultyMultiplier;
    }

    /**
     * 计算经验奖励
     *
     * @param qualityMultiplier 品质倍数
     * @param difficultyMultiplier 难度倍数
     * @return 经验值
     */
    public double calculateExperience(double qualityMultiplier, double difficultyMultiplier) {
        return calculateFinalReward(baseExperience, qualityMultiplier, difficultyMultiplier);
    }

    /**
     * 计算精魄奖励
     *
     * @param qualityMultiplier 品质倍数
     * @param difficultyMultiplier 难度倍数
     * @return 精魄值
     */
    public double calculateSpirits(double qualityMultiplier, double difficultyMultiplier) {
        return calculateFinalReward(baseSpirits, qualityMultiplier, difficultyMultiplier);
    }

    /**
     * 计算掉落倍数
     *
     * @param qualityMultiplier 品质倍数
     * @return 掉落倍数
     */
    public double calculateDropMultiplier(double qualityMultiplier) {
        return qualityMultiplier;
    }

    /**
     * 为单个玩家计算奖励
     *
     * @param totalExperience 总经验值
     * @param damagePercentage 该玩家的伤害百分比 (0-1)
     * @param isKiller 是否是击杀者
     * @return 玩家应获得的经验
     */
    public double calculatePlayerExperience(double totalExperience,
                                           double damagePercentage,
                                           boolean isKiller) {
        // 基础奖励: 按伤害百分比分配
        double playerExperience = totalExperience * damagePercentage;

        // 击杀加成: 最后一击的玩家获得额外20%
        if (isKiller) {
            playerExperience *= (1.0 + KILL_STRIKE_BONUS);
        }

        return playerExperience;
    }

    /**
     * 为单个玩家计算精魄奖励
     *
     * @param totalSpirits 总精魄值
     * @param damagePercentage 该玩家的伤害百分比 (0-1)
     * @param isKiller 是否是击杀者
     * @return 玩家应获得的精魄
     */
    public double calculatePlayerSpirits(double totalSpirits,
                                        double damagePercentage,
                                        boolean isKiller) {
        return calculatePlayerExperience(totalSpirits, damagePercentage, isKiller);
    }

    /**
     * 分配奖励给多个玩家
     *
     * 参数:
     * @param totalExperience 总经验值
     * @param playerDamages 玩家UUID -> 伤害值 映射
     * @param killerUUID 击杀者UUID (可为null)
     * @return 玩家UUID -> 经验奖励 映射
     */
    public Map<UUID, Double> distributeExperience(double totalExperience,
                                                  Map<UUID, Double> playerDamages,
                                                  UUID killerUUID) {
        Map<UUID, Double> rewards = new HashMap<>();

        if (playerDamages.isEmpty()) {
            return rewards;
        }

        // 计算总伤害
        double totalDamage = playerDamages.values()
            .stream()
            .mapToDouble(Double::doubleValue)
            .sum();

        if (totalDamage <= 0) {
            return rewards;
        }

        // 为每个玩家计算奖励
        for (Map.Entry<UUID, Double> entry : playerDamages.entrySet()) {
            UUID playerUUID = entry.getKey();
            double playerDamage = entry.getValue();

            // 跳过伤害太低的玩家
            if (playerDamage < (totalDamage * MIN_DAMAGE_THRESHOLD)) {
                continue;
            }

            // 计算伤害百分比
            double damagePercentage = playerDamage / totalDamage;

            // 检查是否是击杀者
            boolean isKiller = playerUUID.equals(killerUUID);

            // 计算奖励
            double experience = calculatePlayerExperience(totalExperience, damagePercentage, isKiller);
            rewards.put(playerUUID, experience);
        }

        return rewards;
    }

    /**
     * 分配精魄给多个玩家
     */
    public Map<UUID, Double> distributeSpirits(double totalSpirits,
                                              Map<UUID, Double> playerDamages,
                                              UUID killerUUID) {
        return distributeExperience(totalSpirits, playerDamages, killerUUID);
    }

    /**
     * 计算掉落物品数量
     *
     * @param baseDropCount 基础掉落数量
     * @param dropMultiplier 掉落倍数
     * @return 调整后的掉落数量
     */
    public int calculateDropCount(int baseDropCount, double dropMultiplier) {
        return Math.max(1, (int) (baseDropCount * dropMultiplier));
    }

    /**
     * 计算稀有物品掉落概率
     *
     * @param baseRarity 基础稀有度 (0-1)
     * @param qualityMultiplier 品质倍数
     * @return 最终掉落概率
     */
    public double calculateRarityProbability(double baseRarity, double qualityMultiplier) {
        return Math.min(1.0, baseRarity * qualityMultiplier);
    }

    /**
     * 获取奖励信息字符串
     */
    public String getRewardInfo(double qualityMultiplier, double difficultyMultiplier) {
        double exp = calculateExperience(qualityMultiplier, difficultyMultiplier);
        double spirits = calculateSpirits(qualityMultiplier, difficultyMultiplier);

        return String.format(
            "基础奖励 - 经验: %.0f, 精魄: %.0f (品质%.1fx, 难度%.1fx)",
            exp, spirits, qualityMultiplier, difficultyMultiplier
        );
    }

    /**
     * 获取详细奖励报告
     */
    public String getDetailedRewardReport(double totalExperience,
                                         double totalSpirits,
                                         Map<UUID, Double> playerRewards,
                                         UUID killerUUID) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 奖励分配报告 ===\n");
        sb.append(String.format("总经验: %.0f, 总精魄: %.0f\n", totalExperience, totalSpirits));
        sb.append(String.format("参与玩家: %d人\n", playerRewards.size()));

        int[] rank = {1};
        playerRewards.entrySet()
            .stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(10)
            .forEach(entry -> {
                UUID playerUUID = entry.getKey();
                double reward = entry.getValue();
                String marker = playerUUID.equals(killerUUID) ? " [击杀者]" : "";
                sb.append(String.format("%d. %s: %.0f经验%s\n",
                    rank[0]++, playerUUID.toString().substring(0, 8), reward, marker));
            });

        return sb.toString();
    }

    /**
     * 重置奖励计算器
     */
    public void reset() {
        this.baseExperience = BASE_EXPERIENCE_PER_TIER * bossLevel;
        this.baseSpirits = BASE_SPIRITS_PER_TIER * bossLevel;
    }
}
