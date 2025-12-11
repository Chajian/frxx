package com.xiancore.systems.boss.location;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

/**
 * 位置评分器
 * 负责计算位置的综合评分，用于智能选择最佳生成位置
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class LocationScorer {

    private final XianCore plugin;
    private final Logger logger;

    public LocationScorer(XianCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 计算综合位置评分（智能评分系统）
     * 综合考虑：开阔度、生物群系匹配、灵气浓度、玩家密集度
     *
     * @param location 待评分的位置
     * @param point    刷新点配置
     * @return 综合评分 (0.0-1.0)
     */
    public double calculateSmartScore(Location location, BossSpawnPoint point) {
        // 如果未启用智能评分，直接使用简单开阔度评分
        if (!point.isEnableSmartScoring()) {
            return calculateOpennessScore(location);
        }

        // 各维度评分
        double opennessScore = calculateOpennessScore(location);
        double biomeScore = calculateBiomeMatchScore(location, point);
        double spiritualScore = calculateSpiritualEnergyScore(location);
        double playerDensityScore = calculatePlayerDensityScore(location);

        // 获取权重
        double w1 = point.getOpennessWeight();
        double w2 = point.getBiomeWeight();
        double w3 = point.getSpiritualEnergyWeight();
        double w4 = point.getPlayerDensityWeight();

        // 权重归一化（确保总和为1.0）
        double totalWeight = w1 + w2 + w3 + w4;
        if (totalWeight > 0) {
            w1 /= totalWeight;
            w2 /= totalWeight;
            w3 /= totalWeight;
            w4 /= totalWeight;
        }

        // 计算加权平均分
        double finalScore = opennessScore * w1 +
                biomeScore * w2 +
                spiritualScore * w3 +
                playerDensityScore * w4;

        logger.fine(String.format(
                "智能评分详情: 总分=%.2f (开阔度=%.2f×%.2f, 生物群系=%.2f×%.2f, 灵气=%.2f×%.2f, 玩家密度=%.2f×%.2f)",
                finalScore, opennessScore, w1, biomeScore, w2, spiritualScore, w3, playerDensityScore, w4
        ));

        return finalScore;
    }

    /**
     * 计算位置的开阔度评分（0.0-1.0）
     * 检查周围是否有足够的空间，避免在洞穴或建筑内生成
     *
     * @param location 要评分的位置
     * @return 开阔度分数，1.0=完全开阔，0.0=完全封闭
     */
    public double calculateOpennessScore(Location location) {
        if (location == null) return 0.0;

        World world = location.getWorld();
        if (world == null) return 0.0;

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        int totalChecks = 0;
        int openChecks = 0;

        // 1. 检查头顶是否露天（能看到天空）
        boolean canSeeSky = world.getHighestBlockYAt(x, z) <= y;
        if (canSeeSky) {
            openChecks += 5; // 露天加5分
        }
        totalChecks += 5;

        // 2. 检查水平8个方向，半径5格内是否开阔
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},  // 东西南北
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}  // 四个对角
        };

        for (int[] dir : directions) {
            boolean isOpen = true;
            for (int dist = 1; dist <= 5; dist++) {
                int checkX = x + dir[0] * dist;
                int checkZ = z + dir[1] * dist;

                // 检查该位置及上方2格是否为空气
                for (int dy = 0; dy <= 2; dy++) {
                    Block block = world.getBlockAt(checkX, y + dy, checkZ);
                    if (block.getType().isSolid()) {
                        isOpen = false;
                        break;
                    }
                }
                if (!isOpen) break;
            }

            if (isOpen) {
                openChecks++;
            }
            totalChecks++;
        }

        // 3. 计算评分
        double score = totalChecks > 0 ? (double) openChecks / totalChecks : 0.0;

        logger.fine("位置开阔度评分: " + String.format("%.2f", score) +
                " (露天: " + canSeeSky + ", 开阔方向: " + (openChecks - (canSeeSky ? 5 : 0)) + "/8)");

        return score;
    }

    /**
     * 计算生物群系匹配评分
     *
     * @param location 位置
     * @param point    刷新点配置
     * @return 匹配评分 (0.0-1.0)
     */
    public double calculateBiomeMatchScore(Location location, BossSpawnPoint point) {
        List<String> preferredBiomes = point.getPreferredBiomes();

        // 如果没有配置偏好生物群系，返回中性分数
        if (preferredBiomes == null || preferredBiomes.isEmpty()) {
            return 0.5;
        }

        // 获取当前位置的生物群系
        Biome currentBiome = location.getBlock().getBiome();
        String biomeName = currentBiome.name();

        // 完全匹配：1.0分
        for (String preferred : preferredBiomes) {
            if (biomeName.equalsIgnoreCase(preferred)) {
                return 1.0;
            }
        }

        // 部分匹配：检查生物群系类别（如FOREST, DESERT等）
        for (String preferred : preferredBiomes) {
            if (biomeName.contains(preferred.toUpperCase()) ||
                    preferred.toUpperCase().contains(biomeName)) {
                return 0.7; // 类别匹配给0.7分
            }
        }

        // 不匹配：0.3分（不完全拒绝）
        return 0.3;
    }

    /**
     * 计算灵气浓度评分
     * 基于生物群系类型和环境特征计算灵气浓度
     *
     * @param location 位置
     * @return 灵气评分 (0.0-1.0)
     */
    public double calculateSpiritualEnergyScore(Location location) {
        Biome biome = location.getBlock().getBiome();
        String biomeName = biome.name();
        double baseScore = 0.5; // 默认基础分

        // 高灵气生物群系（神秘、稀有）
        if (biomeName.contains("MUSHROOM")) {
            baseScore = 0.95; // 蘑菇岛 - 极高灵气
        } else if (biomeName.contains("JUNGLE")) {
            baseScore = 0.85; // 丛林 - 很高灵气
        } else if (biomeName.contains("BAMBOO")) {
            baseScore = 0.80; // 竹林 - 高灵气
        } else if (biomeName.contains("DARK_FOREST") || biomeName.contains("DARK_OAK")) {
            baseScore = 0.75; // 黑森林 - 高灵气
        } else if (biomeName.contains("FOREST")) {
            baseScore = 0.70; // 森林 - 较高灵气
        } else if (biomeName.contains("MOUNTAIN") || biomeName.contains("PEAKS")) {
            baseScore = 0.75; // 山脉 - 高灵气
        } else if (biomeName.contains("TAIGA")) {
            baseScore = 0.65; // 针叶林 - 中等偏高
        } else if (biomeName.contains("SWAMP")) {
            baseScore = 0.60; // 沼泽 - 中等
        } else if (biomeName.contains("RIVER") || biomeName.contains("OCEAN")) {
            baseScore = 0.55; // 水域 - 中等
        } else if (biomeName.contains("PLAINS")) {
            baseScore = 0.50; // 平原 - 中等
        } else if (biomeName.contains("DESERT")) {
            baseScore = 0.45; // 沙漠 - 较低
        } else if (biomeName.contains("SAVANNA")) {
            baseScore = 0.40; // 热带草原 - 较低
        } else if (biomeName.contains("BADLANDS") || biomeName.contains("MESA")) {
            baseScore = 0.35; // 恶地 - 低
        } else if (biomeName.contains("NETHER")) {
            baseScore = 0.80; // 下界 - 特殊高灵气（魔法能量）
        } else if (biomeName.contains("END")) {
            baseScore = 0.90; // 末地 - 极高灵气
        }

        // 环境加成：高度修正（高山获得加成）
        int y = location.getBlockY();
        if (y > 120) {
            baseScore += 0.05; // 高处+5%
        } else if (y < 40) {
            baseScore -= 0.05; // 低处-5%
        }

        // 确保评分在0.0-1.0范围内
        return Math.max(0.0, Math.min(1.0, baseScore));
    }

    /**
     * 计算玩家密集度评分
     * 评分逻辑：适度玩家密集度最佳（太多或太少都不好）
     *
     * @param location 位置
     * @return 密集度评分 (0.0-1.0)
     */
    public double calculatePlayerDensityScore(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return 0.5;
        }

        // 统计附近玩家数量
        int nearbyPlayers = 0;
        int checkRadius = 100; // 检查半径100格

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(location) <= checkRadius) {
                nearbyPlayers++;
            }
        }

        // 评分逻辑：
        // 0人：0.3分（太冷清）
        // 1-2人：0.8分（适中偏少）
        // 3-5人：1.0分（最佳）
        // 6-10人：0.7分（适中偏多）
        // 10+人：0.4分（太拥挤）

        if (nearbyPlayers == 0) {
            return 0.3;
        } else if (nearbyPlayers <= 2) {
            return 0.8;
        } else if (nearbyPlayers <= 5) {
            return 1.0;
        } else if (nearbyPlayers <= 10) {
            return 0.7;
        } else {
            return 0.4;
        }
    }

    /**
     * 从候选列表中选择最佳位置
     *
     * @param candidates 候选位置列表
     * @param point      刷新点配置
     * @return 最佳位置，如果所有候选都不合格则返回null
     */
    public Location selectBestLocation(List<Location> candidates, BossSpawnPoint point) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        double bestScore = -1;
        Location bestLocation = null;

        for (Location candidate : candidates) {
            double score = calculateSmartScore(candidate, point);

            logger.info("  候选点: (" +
                    candidate.getBlockX() + ", " +
                    candidate.getBlockY() + ", " +
                    candidate.getBlockZ() + ") - 评分: " +
                    String.format("%.2f", score));

            if (score > bestScore) {
                bestScore = score;
                bestLocation = candidate;
            }

            // 如果找到足够好的位置，提前结束
            double minThreshold = point.isEnableSmartScoring() ? point.getMinScore() : 0.7;
            if (score >= minThreshold && score >= 0.7) {
                logger.info("  ✓ 找到优质位置，提前结束搜索");
                return candidate;
            }
        }

        // 检查最佳候选是否达到最低分数要求
        double minAcceptableScore = point.isEnableSmartScoring() ? point.getMinScore() : 0.3;
        if (bestLocation != null && bestScore >= minAcceptableScore) {
            return bestLocation;
        }

        logger.warning("✗ 所有候选点评分均低于最低要求 " + minAcceptableScore);
        return null;
    }
}
