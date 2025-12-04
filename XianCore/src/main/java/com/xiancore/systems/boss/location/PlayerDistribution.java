package com.xiancore.systems.boss.location;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 玩家分布分析器
 * 分析在线玩家的分布情况并选择最优位置
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class PlayerDistribution {

    // ==================== 常量 ====================

    private static final double DENSITY_CHECK_RADIUS = 50.0;  // 密度检查半径

    // ==================== 分析方法 ====================

    /**
     * 计算玩家分布分数 (0.0-1.0)
     * 位置远离玩家聚集地的分数越高
     *
     * @param location 位置
     * @param players 在线玩家列表
     * @return 分布分数
     */
    public double calculateDistributionScore(Location location, List<Player> players) {
        if (location == null || players == null || players.isEmpty()) {
            return 0.5;
        }

        // 计算到最近玩家的距离
        double minDistance = Double.MAX_VALUE;
        for (Player player : players) {
            if (player.getWorld().equals(location.getWorld())) {
                double distance = player.getLocation().distance(location);
                minDistance = Math.min(minDistance, distance);
            }
        }

        // 距离越远，分数越高 (最远200格为满分)
        double score = Math.min(1.0, minDistance / 200.0);
        return score;
    }

    /**
     * 计算位置的玩家密度分数 (0.0-1.0)
     * 半径内玩家越少，分数越高
     *
     * @param location 位置
     * @param players 在线玩家列表
     * @return 密度分数 (高分=玩家少)
     */
    public double calculateDensityScore(Location location, List<Player> players) {
        if (location == null || players == null) {
            return 1.0;
        }

        // 计算半径内的玩家数量
        int playerCount = 0;
        for (Player player : players) {
            if (player.getWorld().equals(location.getWorld())) {
                if (player.getLocation().distance(location) <= DENSITY_CHECK_RADIUS) {
                    playerCount++;
                }
            }
        }

        // 玩家数越少，分数越高 (0个玩家满分)
        return Math.max(0.0, 1.0 - (playerCount / 10.0));
    }

    /**
     * 获取玩家中心位置
     *
     * @param players 在线玩家列表
     * @return 中心位置
     */
    public Location getPlayerCenter(List<Player> players) {
        if (players == null || players.isEmpty()) {
            return null;
        }

        double totalX = 0, totalY = 0, totalZ = 0;
        int validCount = 0;

        for (Player player : players) {
            if (player.getLocation() != null) {
                Location loc = player.getLocation();
                totalX += loc.getX();
                totalY += loc.getY();
                totalZ += loc.getZ();
                validCount++;
            }
        }

        if (validCount == 0) {
            return null;
        }

        Location center = players.get(0).getWorld().getSpawnLocation();
        center.setX(totalX / validCount);
        center.setY(totalY / validCount);
        center.setZ(totalZ / validCount);

        return center;
    }

    /**
     * 计算到最近玩家的距离
     *
     * @param location 位置
     * @param players 在线玩家列表
     * @return 距离
     */
    public double getDistanceToNearestPlayer(Location location, List<Player> players) {
        if (location == null || players == null || players.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double minDistance = Double.MAX_VALUE;
        for (Player player : players) {
            if (player.getWorld().equals(location.getWorld())) {
                double distance = player.getLocation().distance(location);
                minDistance = Math.min(minDistance, distance);
            }
        }

        return minDistance;
    }

    /**
     * 获取最优分散位置 (远离玩家)
     *
     * @param candidates 候选位置
     * @param players 在线玩家列表
     * @return 最优位置列表
     */
    public List<Location> getOptimalDistributionLocations(List<Location> candidates, List<Player> players) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // 按分布分数排序
        List<Location> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> Double.compare(
            calculateDistributionScore(b, players),
            calculateDistributionScore(a, players)
        ));

        return sorted;
    }

    /**
     * 获取分析报告
     */
    public String getAnalysisReport(Location location, List<Player> players) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 玩家分布分析 ===\n");
        sb.append(String.format("分布分数: %.2f\n", calculateDistributionScore(location, players)));
        sb.append(String.format("密度分数: %.2f\n", calculateDensityScore(location, players)));
        sb.append(String.format("最近玩家距离: %.1f\n", getDistanceToNearestPlayer(location, players)));
        sb.append(String.format("在线玩家数: %d\n", players != null ? players.size() : 0));

        if (players != null && !players.isEmpty()) {
            Location center = getPlayerCenter(players);
            if (center != null) {
                sb.append(String.format("玩家中心: (%.0f, %.0f, %.0f)\n", center.getX(), center.getY(), center.getZ()));
                sb.append(String.format("到玩家中心距离: %.1f\n", location.distance(center)));
            }
        }

        return sb.toString();
    }
}
