package com.xiancore.spawn;

import java.util.*;
import java.util.logging.Logger;

/**
 * 位置生成器 - 安全位置选择和生成
 * Location Generator - Safe Location Selection and Generation
 *
 * @author XianCore
 * @version 1.0
 */
public class LocationGenerator {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Set<String> usedLocations = new HashSet<>();
    private final int worldWidth;
    private final int worldHeight;

    /**
     * 位置信息
     */
    public static class Location {
        public String world;
        public double x, y, z;
        public float yaw, pitch;
        public LocationType type;
        public double safetyScore;   // 0-100安全评分
        public boolean isOccupied;

        public enum LocationType {
            PLATFORM,        // 平台
            ARENA,          // 竞技场
            CAVE,           // 洞穴
            MOUNTAIN,       // 山顶
            FLOATING_ISLE   // 浮岛
        }

        public Location(String world, double x, double y, double z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = 0;
            this.pitch = 0;
            this.type = LocationType.PLATFORM;
            this.safetyScore = 50.0;
            this.isOccupied = false;
        }

        public String getKey() {
            return String.format("%s:%.0f:%.0f:%.0f", world, x, y, z);
        }

        @Override
        public String toString() {
            return String.format("%s (%.1f, %.1f, %.1f) - Safety: %.0f%%", world, x, y, z, safetyScore);
        }
    }

    /**
     * 安全检查结果
     */
    public static class SafetyAssessment {
        public double score;               // 0-100
        public List<String> issues;        // 安全问题列表
        public List<String> warnings;      // 警告信息
        public boolean isSafe;

        public SafetyAssessment() {
            this.score = 100.0;
            this.issues = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.isSafe = true;
        }
    }

    /**
     * 构造函数
     */
    public LocationGenerator(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        logger.info("✓ LocationGenerator已初始化 (世界: " + worldWidth + "x" + worldHeight + ")");
    }

    /**
     * 生成随机安全位置
     */
    public Location generateSafeLocation(String world, int attempts) {
        for (int i = 0; i < attempts; i++) {
            double x = (Math.random() - 0.5) * worldWidth;
            double y = Math.random() * (worldHeight - 64) + 64;  // 最小Y=64
            double z = (Math.random() - 0.5) * worldWidth;

            Location location = new Location(world, x, y, z);
            SafetyAssessment assessment = assessSafety(location);

            if (assessment.isSafe && assessment.score > 70) {
                location.safetyScore = assessment.score;
                usedLocations.add(location.getKey());
                logger.info("✓ 安全位置已生成: " + location);
                return location;
            }
        }

        logger.warning("⚠ 无法生成安全位置，使用备选位置");
        return generateFallbackLocation(world);
    }

    /**
     * 生成竞技场位置
     */
    public Location generateArenaLocation(String world, int arenaRadius) {
        double centerX = (Math.random() - 0.5) * (worldWidth - arenaRadius * 2);
        double centerY = 64.0;
        double centerZ = (Math.random() - 0.5) * (worldWidth - arenaRadius * 2);

        Location location = new Location(world, centerX, centerY, centerZ);
        location.type = Location.LocationType.ARENA;
        location.safetyScore = 85.0;

        usedLocations.add(location.getKey());
        logger.info("✓ 竞技场位置已生成 (半径: " + arenaRadius + ")");

        return location;
    }

    /**
     * 生成平台位置
     */
    public Location generatePlatformLocation(String world, int platformSize) {
        double x = (Math.random() - 0.5) * worldWidth;
        double y = 100.0 + Math.random() * 50;  // Y: 100-150
        double z = (Math.random() - 0.5) * worldWidth;

        Location location = new Location(world, x, y, z);
        location.type = Location.LocationType.PLATFORM;
        location.safetyScore = 80.0;

        usedLocations.add(location.getKey());
        logger.info("✓ 平台位置已生成");

        return location;
    }

    /**
     * 生成洞穴位置
     */
    public Location generateCaveLocation(String world) {
        double x = (Math.random() - 0.5) * worldWidth;
        double y = 32.0 + Math.random() * 32;  // Y: 32-64 (地下)
        double z = (Math.random() - 0.5) * worldWidth;

        Location location = new Location(world, x, y, z);
        location.type = Location.LocationType.CAVE;
        location.safetyScore = 50.0;  // 洞穴较危险

        usedLocations.add(location.getKey());
        logger.info("✓ 洞穴位置已生成");

        return location;
    }

    /**
     * 生成浮岛位置
     */
    public Location generateFloatingIsleLocation(String world) {
        double x = (Math.random() - 0.5) * worldWidth;
        double y = 150.0 + Math.random() * 100;  // Y: 150-250 (高空)
        double z = (Math.random() - 0.5) * worldWidth;

        Location location = new Location(world, x, y, z);
        location.type = Location.LocationType.FLOATING_ISLE;
        location.safetyScore = 60.0;  // 易坠落

        usedLocations.add(location.getKey());
        logger.info("✓ 浮岛位置已生成 (高度: " + String.format("%.0f", y) + ")");

        return location;
    }

    /**
     * 评估位置安全性
     */
    public SafetyAssessment assessSafety(Location location) {
        SafetyAssessment assessment = new SafetyAssessment();

        // 检查边界
        if (Math.abs(location.x) > worldWidth / 2 || Math.abs(location.z) > worldWidth / 2) {
            assessment.issues.add("位置超出世界边界");
            assessment.isSafe = false;
            assessment.score -= 30;
        }

        // 检查高度
        if (location.y < 0 || location.y > worldHeight) {
            assessment.issues.add("Y坐标超出范围");
            assessment.isSafe = false;
            assessment.score -= 40;
        } else if (location.y < 10) {
            assessment.warnings.add("接近世界底部");
            assessment.score -= 10;
        } else if (location.y > worldHeight - 10) {
            assessment.warnings.add("接近世界顶部");
            assessment.score -= 10;
        }

        // 检查是否已被占用
        if (isLocationOccupied(location)) {
            assessment.warnings.add("位置可能已被占用");
            assessment.score -= 20;
        }

        // 检查位置类型安全性
        assessment.score = Math.min(100, Math.max(0, assessment.score));
        assessment.isSafe = assessment.issues.isEmpty() && assessment.score > 50;

        return assessment;
    }

    /**
     * 检查位置是否被占用
     */
    public boolean isLocationOccupied(Location location) {
        String key = location.getKey();
        return usedLocations.contains(key);
    }

    /**
     * 标记位置为占用
     */
    public void markLocationOccupied(Location location) {
        usedLocations.add(location.getKey());
    }

    /**
     * 标记位置为可用
     */
    public void markLocationFree(Location location) {
        usedLocations.remove(location.getKey());
    }

    /**
     * 生成备选位置
     */
    private Location generateFallbackLocation(String world) {
        Location location = new Location(world, 0, 100, 0);
        location.type = Location.LocationType.PLATFORM;
        location.safetyScore = 70.0;
        return location;
    }

    /**
     * 计算两个位置间距离
     */
    public double getDistance(Location loc1, Location loc2) {
        if (!loc1.world.equals(loc2.world)) {
            return Double.MAX_VALUE;
        }

        double dx = loc1.x - loc2.x;
        double dy = loc1.y - loc2.y;
        double dz = loc1.z - loc2.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 找到最近的安全位置
     */
    public Location findNearestSafeLocation(Location center, double searchRadius) {
        // 在给定半径内生成候选位置
        List<Location> candidates = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double radius = Math.random() * searchRadius;

            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y;

            Location candidate = new Location(center.world, x, y, z);
            SafetyAssessment assessment = assessSafety(candidate);

            if (assessment.isSafe) {
                candidates.add(candidate);
            }
        }

        if (candidates.isEmpty()) {
            return center;
        }

        // 返回安全评分最高的位置
        return candidates.stream()
                .max(Comparator.comparingDouble(l -> assessSafety(l).score))
                .orElse(center);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("used_locations", usedLocations.size());
        stats.put("world_dimensions", worldWidth + "x" + worldHeight);
        stats.put("available_space", worldWidth * worldHeight - usedLocations.size());

        return stats;
    }

    /**
     * 清空所有位置占用标记
     */
    public void clearOccupied() {
        usedLocations.clear();
        logger.info("✓ 所有位置占用标记已清空");
    }

    /**
     * 重置生成器
     */
    public void reset() {
        usedLocations.clear();
        logger.info("✓ 位置生成器已重置");
    }
}
