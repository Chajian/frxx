package com.xiancore.integration.residence;

import lombok.Getter;
import org.bukkit.Location;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 建筑位系统
 * 管理宗门领地内的建筑位置和效果范围
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class BuildingSlotManager {

    // 建筑位存储（宗门ID -> 建筑位列表）
    private final Map<Integer, List<BuildingSlot>> buildingSlots = new ConcurrentHashMap<>();

    // 建筑位效果范围缓存（宗门ID -> 效果范围）
    private final Map<Integer, SectionEffectRange> effectRanges = new ConcurrentHashMap<>();

    /**
     * 添加建筑位
     */
    public boolean addBuildingSlot(int sectId, String slotType, Location location, int level) {
        buildingSlots.putIfAbsent(sectId, Collections.synchronizedList(new ArrayList<>()));
        List<BuildingSlot> slots = buildingSlots.get(sectId);
        slots.add(new BuildingSlot(slotType, location, level, System.currentTimeMillis()));
        updateEffectRange(sectId);
        return true;
    }

    /**
     * 移除建筑位
     */
    public boolean removeBuildingSlot(int sectId, int index) {
        List<BuildingSlot> slots = buildingSlots.get(sectId);
        if (slots != null && index >= 0 && index < slots.size()) {
            slots.remove(index);
            updateEffectRange(sectId);
            return true;
        }
        return false;
    }

    /**
     * 获取宗门的建筑位
     */
    public List<BuildingSlot> getBuildingSlots(int sectId) {
        return new ArrayList<>(buildingSlots.getOrDefault(sectId, new ArrayList<>()));
    }

    /**
     * 获取指定位置的建筑位效果
     */
    public double getEffectMultiplier(int sectId, Location location, String effectType) {
        SectionEffectRange range = effectRanges.get(sectId);
        if (range == null) {
            return 1.0;
        }

        double multiplier = 1.0;
        List<BuildingSlot> slots = buildingSlots.getOrDefault(sectId, new ArrayList<>());

        for (BuildingSlot slot : slots) {
            double distance = slot.getLocation().distance(location);
            double slotEffect = slot.getEffectMultiplier(distance, effectType);
            multiplier *= slotEffect;
        }

        return multiplier;
    }

    /**
     * 更新效果范围
     */
    private void updateEffectRange(int sectId) {
        List<BuildingSlot> slots = buildingSlots.get(sectId);
        if (slots == null || slots.isEmpty()) {
            effectRanges.remove(sectId);
            return;
        }

        SectionEffectRange range = new SectionEffectRange();
        for (BuildingSlot slot : slots) {
            range.updateFromSlot(slot);
        }
        effectRanges.put(sectId, range);
    }

    /**
     * 清空所有建筑位
     */
    public void clearAllSlots(int sectId) {
        buildingSlots.remove(sectId);
        effectRanges.remove(sectId);
    }

    /**
     * 获取建筑位统计
     */
    public String getStatistics(int sectId) {
        List<BuildingSlot> slots = buildingSlots.getOrDefault(sectId, new ArrayList<>());
        Map<String, Integer> typeCounts = new HashMap<>();

        for (BuildingSlot slot : slots) {
            typeCounts.put(slot.getSlotType(), typeCounts.getOrDefault(slot.getSlotType(), 0) + 1);
        }

        StringBuilder sb = new StringBuilder("建筑位统计:\n");
        sb.append("  总数: ").append(slots.size()).append("\n");
        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 建筑位类
     */
    public static class BuildingSlot {
        @Getter
        private final String slotType;
        @Getter
        private final Location location;
        @Getter
        private final int level;
        @Getter
        private final long createdTime;

        // 效果范围（方块）
        private static final int EFFECT_RANGE = 32;

        public BuildingSlot(String slotType, Location location, int level, long createdTime) {
            this.slotType = slotType;
            this.location = location;
            this.level = level;
            this.createdTime = createdTime;
        }

        /**
         * 获取该建筑位对指定距离的效果倍数
         * 基于距离和建筑位等级计算
         */
        public double getEffectMultiplier(double distance, String effectType) {
            // 超过范围，无效果
            if (distance > EFFECT_RANGE) {
                return 1.0;
            }

            // 距离衰减系数
            double distanceDecay = 1.0 - (distance / EFFECT_RANGE) * 0.5;

            // 等级加成
            double levelBonus = 1.0 + (level - 1) * 0.1;

            // 根据效果类型调整
            double typeMultiplier = getTypeMultiplier(effectType);

            return distanceDecay * levelBonus * typeMultiplier;
        }

        /**
         * 根据效果类型获取倍数
         */
        private double getTypeMultiplier(String effectType) {
            return switch (effectType) {
                case "EXPERIENCE" -> 1.2;  // 经验加成20%
                case "DROP_RATE" -> 1.15; // 掉落率加成15%
                case "SPEED" -> 1.1;      // 速度加成10%
                case "DEFENSE" -> 1.05;   // 防御加成5%
                default -> 1.0;
            };
        }

        /**
         * 获取建筑位描述
         */
        public String getDescription() {
            return String.format("§7[%s] 等级: %d, 位置: %d,%d,%d",
                slotType, level,
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    /**
     * 宗门效果范围
     */
    public static class SectionEffectRange {
        private double minX = Double.MAX_VALUE;
        private double maxX = Double.MIN_VALUE;
        private double minY = Double.MAX_VALUE;
        private double maxY = Double.MIN_VALUE;
        private double minZ = Double.MAX_VALUE;
        private double maxZ = Double.MIN_VALUE;

        public void updateFromSlot(BuildingSlot slot) {
            Location loc = slot.getLocation();
            int range = 32;

            minX = Math.min(minX, loc.getX() - range);
            maxX = Math.max(maxX, loc.getX() + range);
            minY = Math.min(minY, loc.getY() - range);
            maxY = Math.max(maxY, loc.getY() + range);
            minZ = Math.min(minZ, loc.getZ() - range);
            maxZ = Math.max(maxZ, loc.getZ() + range);
        }

        public boolean isInRange(Location location) {
            return location.getX() >= minX && location.getX() <= maxX &&
                   location.getY() >= minY && location.getY() <= maxY &&
                   location.getZ() >= minZ && location.getZ() <= maxZ;
        }
    }
}
