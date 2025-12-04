package com.xiancore.systems.boss.location;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;

/**
 * 位置安全性分析器
 * 分析位置的安全性、可用性等
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class SafetyAnalyzer {

    // ==================== 常量 ====================

    private static final int CHECK_RADIUS = 10;  // 检查半径
    private static final int MIN_HEIGHT = 10;    // 最低高度
    private static final int MAX_HEIGHT = 250;   // 最高高度

    // ==================== 不安全方块 ====================

    private static final Set<Material> DANGEROUS_BLOCKS = new HashSet<>();

    static {
        DANGEROUS_BLOCKS.add(Material.LAVA);
        DANGEROUS_BLOCKS.add(Material.MAGMA_BLOCK);
        DANGEROUS_BLOCKS.add(Material.FIRE);
        DANGEROUS_BLOCKS.add(Material.WATER);
        DANGEROUS_BLOCKS.add(Material.VOID_AIR);
    }

    // ==================== 分析方法 ====================

    /**
     * 计算位置的安全性分数 (0.0-1.0)
     *
     * @param location 位置
     * @return 安全性分数
     */
    public double calculateSafetyScore(Location location) {
        if (location == null || location.getWorld() == null) {
            return 0.0;
        }

        double score = 1.0;

        // 检查高度
        if (!isHeightValid(location)) {
            score -= 0.3;
        }

        // 检查危险方块
        if (hasDangerousBlocks(location)) {
            score -= 0.4;
        }

        // 检查空间
        if (!hasEnoughSpace(location)) {
            score -= 0.2;
        }

        // 检查悬崖
        if (isOnCliff(location)) {
            score -= 0.1;
        }

        return Math.max(0.0, score);
    }

    /**
     * 判断位置是否安全
     *
     * @param location 位置
     * @return 是否安全
     */
    public boolean isSafeLocation(Location location) {
        return calculateSafetyScore(location) >= 0.5;
    }

    /**
     * 过滤不安全的位置
     *
     * @param candidates 候选位置列表
     * @return 过滤后的位置列表
     */
    public List<Location> filterUnsafeLocations(List<Location> candidates) {
        if (candidates == null) {
            return new ArrayList<>();
        }

        List<Location> safe = new ArrayList<>();
        for (Location loc : candidates) {
            if (isSafeLocation(loc)) {
                safe.add(loc);
            }
        }
        return safe;
    }

    // ==================== 具体检查方法 ====================

    /**
     * 检查高度是否有效
     */
    private boolean isHeightValid(Location location) {
        int y = location.getBlockY();
        return y >= MIN_HEIGHT && y <= MAX_HEIGHT;
    }

    /**
     * 检查周围是否有危险方块
     */
    private boolean hasDangerousBlocks(Location location) {
        if (location.getBlock().getType() == Material.LAVA ||
            location.getBlock().getType() == Material.MAGMA_BLOCK) {
            return true;
        }

        // 检查下方2格
        for (int i = 0; i < 2; i++) {
            Block below = location.getBlock().getRelative(0, -i, 0);
            if (DANGEROUS_BLOCKS.contains(below.getType())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查是否有足够的空间
     */
    private boolean hasEnoughSpace(Location location) {
        Block block = location.getBlock();

        // 需要至少2格高的空间
        Block above1 = block.getRelative(0, 1, 0);
        Block above2 = block.getRelative(0, 2, 0);

        return !above1.getType().isSolid() && !above2.getType().isSolid();
    }

    /**
     * 检查是否在悬崖边缘
     */
    private boolean isOnCliff(Location location) {
        // 检查周围8个方向是否有下陷
        int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1},
                              {1,1}, {1,-1}, {-1,1}, {-1,-1}};

        int baseY = location.getBlockY();

        for (int[] dir : directions) {
            Block neighbor = location.getBlock().getRelative(dir[0], 0, dir[1]);
            Block below = neighbor.getRelative(0, -1, 0);

            // 如果相邻块是空的或液体，可能是悬崖
            if (neighbor.getType() == Material.AIR && below.getType() == Material.AIR) {
                return true;
            }
        }

        return false;
    }

    // ==================== 工具方法 ====================

    /**
     * 获取位置详细的安全性报告
     */
    public String getSafetyReport(Location location) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 安全性报告 ===\n");
        sb.append(String.format("位置: (%d, %d, %d)\n",
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()));
        sb.append(String.format("安全性分数: %.2f\n", calculateSafetyScore(location)));
        sb.append(String.format("高度有效: %s\n", isHeightValid(location) ? "是" : "否"));
        sb.append(String.format("危险方块: %s\n", hasDangerousBlocks(location) ? "有" : "无"));
        sb.append(String.format("空间足够: %s\n", hasEnoughSpace(location) ? "是" : "否"));
        sb.append(String.format("悬崖边缘: %s\n", isOnCliff(location) ? "是" : "否"));
        sb.append(String.format("安全: %s\n", isSafeLocation(location) ? "是" : "否"));

        return sb.toString();
    }

    /**
     * 计算到最近安全位置的调整
     */
    public Location findNearestSafeLocation(Location location, int radius) {
        if (isSafeLocation(location)) {
            return location;
        }

        // 逐层向外扩展搜索
        for (int r = 1; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) == r || Math.abs(z) == r) {  // 只检查外围
                        Location candidate = location.clone().add(x, 0, z);
                        if (isSafeLocation(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * 获取分析统计
     */
    public Map<String, Object> getAnalysisStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("checkRadius", CHECK_RADIUS);
        stats.put("minHeight", MIN_HEIGHT);
        stats.put("maxHeight", MAX_HEIGHT);
        stats.put("dangerousBlocks", DANGEROUS_BLOCKS.size());
        return stats;
    }
}
