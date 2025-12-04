package com.xiancore.systems.boss.teleport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * Boss传送点数据类
 * 表示一个可传送的目标位置
 *
 * 包含:
 * - 位置和世界信息
 * - 传送费用
 * - 权限和等级限制
 * - 使用统计
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
@Setter
public class BossTeleportPoint {

    // ==================== 基本信息 ====================

    /** 传送点唯一ID */
    private String pointId;

    /** 显示名称 */
    private String displayName;

    /** 目标位置 */
    private Location location;

    /** 是否启用 */
    private boolean enabled;

    // ==================== 访问控制 ====================

    /** 所需等级 */
    private int requiredLevel;

    /** 所需权限节点 */
    private String permission;

    /** VIP专用 */
    private boolean vipOnly;

    // ==================== 费用配置 ====================

    /** 传送基础费用 */
    private double baseCost;

    /** 是否自动计算费用 (基于距离) */
    private boolean autoCalculatePrice;

    // ==================== 时间和统计 ====================

    /** 创建时间 (毫秒) */
    private long createdTime;

    /** 上次使用时间 */
    private long lastUsedTime;

    /** 总使用次数 */
    private long usageCount;

    /** 总收益 (所有传送费用之和) */
    private double totalRevenue;

    // ==================== 描述信息 ====================

    /** 传送点描述 */
    private String description;

    /** 相关的Boss名称 */
    private String relatedBossName;

    /** 推荐人数 */
    private int recommendedGroupSize;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public BossTeleportPoint() {
        this.pointId = UUID.randomUUID().toString().substring(0, 8);
        this.enabled = true;
        this.createdTime = System.currentTimeMillis();
        this.usageCount = 0;
        this.totalRevenue = 0;
        this.vipOnly = false;
        this.autoCalculatePrice = true;
        this.baseCost = 50;
        this.requiredLevel = 0;
    }

    /**
     * 带参数的构造函数
     */
    public BossTeleportPoint(String pointId, String displayName, Location location, double baseCost) {
        this();
        this.pointId = pointId;
        this.displayName = displayName;
        this.location = location;
        this.baseCost = baseCost;
    }

    // ==================== 业务方法 ====================

    /**
     * 获取世界
     */
    public World getWorld() {
        return location != null ? location.getWorld() : null;
    }

    /**
     * 获取世界名称
     */
    public String getWorldName() {
        World world = getWorld();
        return world != null ? world.getName() : "unknown";
    }

    /**
     * 获取X坐标
     */
    public double getX() {
        return location != null ? location.getX() : 0;
    }

    /**
     * 获取Y坐标
     */
    public double getY() {
        return location != null ? location.getY() : 0;
    }

    /**
     * 获取Z坐标
     */
    public double getZ() {
        return location != null ? location.getZ() : 0;
    }

    /**
     * 获取坐标字符串
     */
    public String getCoordinatesString() {
        if (location == null) {
            return "unknown";
        }
        return String.format("(%d, %d, %d)",
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }

    /**
     * 计算到另一位置的距离
     *
     * @param from 源位置
     * @return 距离 (格)
     */
    public double calculateDistance(Location from) {
        if (location == null || from == null) {
            return 0;
        }

        // 不同世界不能计算距离
        if (!location.getWorld().equals(from.getWorld())) {
            return Double.MAX_VALUE;
        }

        return location.distance(from);
    }

    /**
     * 记录一次使用
     *
     * @param cost 实际花费
     */
    public void recordUsage(double cost) {
        this.usageCount++;
        this.lastUsedTime = System.currentTimeMillis();
        this.totalRevenue += cost;
    }

    /**
     * 获取使用统计
     */
    public String getUsageStats() {
        return String.format("使用%d次, 总收益%.2f", usageCount, totalRevenue);
    }

    /**
     * 检查玩家是否可以访问此传送点
     *
     * @param hasPermission 玩家是否有权限
     * @param playerLevel 玩家等级
     * @param playerIsVIP 玩家是否VIP
     * @return 是否可以访问
     */
    public boolean canAccess(boolean hasPermission, int playerLevel, boolean playerIsVIP) {
        // 权限检查
        if (permission != null && !permission.isEmpty() && !hasPermission) {
            return false;
        }

        // 等级检查
        if (playerLevel < requiredLevel) {
            return false;
        }

        // VIP检查
        if (vipOnly && !playerIsVIP) {
            return false;
        }

        // 启用状态检查
        return enabled;
    }

    /**
     * 获取不可访问的原因
     *
     * @param hasPermission 玩家是否有权限
     * @param playerLevel 玩家等级
     * @param playerIsVIP 玩家是否VIP
     * @return 原因字符串
     */
    public String getAccessDeniedReason(boolean hasPermission, int playerLevel, boolean playerIsVIP) {
        if (!enabled) {
            return "传送点已禁用";
        }

        if (permission != null && !permission.isEmpty() && !hasPermission) {
            return "权限不足";
        }

        if (playerLevel < requiredLevel) {
            return String.format("等级不足 (需要: %d, 当前: %d)", requiredLevel, playerLevel);
        }

        if (vipOnly && !playerIsVIP) {
            return "仅限VIP";
        }

        return "访问被拒绝";
    }

    /**
     * 获取完整的信息描述
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 传送点信息 ===\n");
        sb.append(String.format("名称: %s\n", displayName));
        sb.append(String.format("ID: %s\n", pointId));
        sb.append(String.format("位置: %s %s\n", getWorldName(), getCoordinatesString()));
        sb.append(String.format("状态: %s\n", enabled ? "启用" : "禁用"));

        if (relatedBossName != null && !relatedBossName.isEmpty()) {
            sb.append(String.format("相关Boss: %s\n", relatedBossName));
        }

        sb.append(String.format("费用: %.0f\n", baseCost));

        if (requiredLevel > 0) {
            sb.append(String.format("所需等级: %d\n", requiredLevel));
        }

        if (vipOnly) {
            sb.append("VIP专用: 是\n");
        }

        sb.append(String.format("统计: %s\n", getUsageStats()));

        if (description != null && !description.isEmpty()) {
            sb.append(String.format("描述: %s\n", description));
        }

        return sb.toString();
    }

    /**
     * 获取简单的显示信息
     */
    public String getDisplayInfo() {
        return String.format("&6%s &8[&e%.0f枚&8] &8(%s)",
            displayName, baseCost, getCoordinatesString());
    }

    /**
     * 转换为Map格式 (用于序列化)
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("pointId", pointId);
        map.put("displayName", displayName);
        map.put("enabled", enabled);

        if (location != null) {
            map.put("world", getWorldName());
            map.put("x", getX());
            map.put("y", getY());
            map.put("z", getZ());
        }

        map.put("baseCost", baseCost);
        map.put("requiredLevel", requiredLevel);
        map.put("vipOnly", vipOnly);

        map.put("usageCount", usageCount);
        map.put("totalRevenue", totalRevenue);

        return map;
    }

    /**
     * 克隆传送点
     */
    @Override
    public BossTeleportPoint clone() {
        BossTeleportPoint point = new BossTeleportPoint();
        point.pointId = this.pointId;
        point.displayName = this.displayName;
        point.location = this.location != null ? this.location.clone() : null;
        point.enabled = this.enabled;
        point.requiredLevel = this.requiredLevel;
        point.permission = this.permission;
        point.vipOnly = this.vipOnly;
        point.baseCost = this.baseCost;
        point.autoCalculatePrice = this.autoCalculatePrice;
        point.createdTime = this.createdTime;
        point.lastUsedTime = this.lastUsedTime;
        point.usageCount = this.usageCount;
        point.totalRevenue = this.totalRevenue;
        point.description = this.description;
        point.relatedBossName = this.relatedBossName;
        point.recommendedGroupSize = this.recommendedGroupSize;
        return point;
    }

    /**
     * 比较两个传送点是否相同
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BossTeleportPoint)) {
            return false;
        }

        BossTeleportPoint other = (BossTeleportPoint) obj;
        return this.pointId.equals(other.pointId);
    }

    /**
     * 哈希码
     */
    @Override
    public int hashCode() {
        return pointId.hashCode();
    }

    /**
     * 字符串表示
     */
    @Override
    public String toString() {
        return String.format("BossTeleportPoint{%s: %s @ %s}",
            pointId, displayName, getCoordinatesString());
    }
}
