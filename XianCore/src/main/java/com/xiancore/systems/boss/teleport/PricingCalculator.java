package com.xiancore.systems.boss.teleport;

import lombok.Getter;
import org.bukkit.Location;

import java.util.*;

/**
 * 传送费用计算器
 * 计算玩家从一个位置到另一位置的传送费用
 *
 * 支持:
 * - 基于距离的动态价格
 * - VIP折扣
 * - 经济验证
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
public class PricingCalculator {

    // ==================== 配置常量 ====================

    /** 基础费用 */
    private static final double BASE_COST = 50.0;

    /** 每格距离的费用 */
    private static final double COST_PER_BLOCK = 0.1;

    /** VIP折扣 (30%) */
    private static final double VIP_DISCOUNT = 0.3;

    // ==================== 内部状态 ====================

    /** 经济系统引用 (模拟) */
    private final EconomySimulator economy;

    /** 费用记录 */
    private final Map<UUID, Long> lastCostMap;

    // ==================== 构造函数 ====================

    public PricingCalculator() {
        this.economy = new EconomySimulator();
        this.lastCostMap = new HashMap<>();
    }

    // ==================== 费用计算方法 ====================

    /**
     * 计算传送费用 (基于传送点)
     *
     * @param from 源位置
     * @param point 目标传送点
     * @param isVIP 是否为VIP
     * @return 费用
     */
    public double calculateCost(Location from, BossTeleportPoint point, boolean isVIP) {
        if (point == null) {
            return 0;
        }

        double cost;

        // 使用传送点的基础费用或自动计算
        if (point.isAutoCalculatePrice() && from != null) {
            cost = calculateCostByDistance(from, point.getLocation());
        } else {
            cost = point.getBaseCost();
        }

        // 应用VIP折扣
        if (isVIP) {
            cost = applyVIPDiscount(cost);
        }

        return Math.max(1, cost);  // 最小费用1
    }

    /**
     * 根据距离计算费用
     *
     * @param from 源位置
     * @param to 目标位置
     * @return 费用
     */
    public double calculateCostByDistance(Location from, Location to) {
        if (from == null || to == null) {
            return BASE_COST;
        }

        // 不同世界不能传送
        if (!from.getWorld().equals(to.getWorld())) {
            return Double.MAX_VALUE;
        }

        // 计算距离
        double distance = from.distance(to);

        // 费用 = 基础 + 距离 × 每格费用
        double cost = BASE_COST + (distance * COST_PER_BLOCK);

        return Math.max(BASE_COST, cost);
    }

    /**
     * 应用VIP折扣
     *
     * @param originalCost 原始费用
     * @return 折扣后费用
     */
    public double applyVIPDiscount(double originalCost) {
        // VIP折扣: 原价 × (1 - 30%) = 原价 × 0.7
        return originalCost * (1 - VIP_DISCOUNT);
    }

    /**
     * 计算VIP可节省的费用
     *
     * @param originalCost 原始费用
     * @return 节省的费用
     */
    public double calculateVIPSavings(double originalCost) {
        return originalCost * VIP_DISCOUNT;
    }

    // ==================== 验证方法 ====================

    /**
     * 验证玩家是否有足够的余额
     *
     * @param playerUUID 玩家UUID
     * @param cost 费用
     * @return 是否足够
     */
    public boolean canAfford(UUID playerUUID, double cost) {
        return economy.hasBalance(playerUUID, cost);
    }

    /**
     * 获取玩家的账户余额
     *
     * @param playerUUID 玩家UUID
     * @return 余额
     */
    public double getBalance(UUID playerUUID) {
        return economy.getBalance(playerUUID);
    }

    // ==================== 经济操作 ====================

    /**
     * 从玩家账户扣除费用
     *
     * @param playerUUID 玩家UUID
     * @param cost 费用
     * @return 是否成功
     */
    public boolean deductCost(UUID playerUUID, double cost) {
        if (!canAfford(playerUUID, cost)) {
            return false;
        }

        economy.withdraw(playerUUID, cost);
        recordLastCost(playerUUID, (long) cost);
        return true;
    }

    /**
     * 向玩家账户增加金币 (退款)
     *
     * @param playerUUID 玩家UUID
     * @param amount 金额
     */
    public void refund(UUID playerUUID, double amount) {
        economy.deposit(playerUUID, amount);
    }

    // ==================== 统计方法 ====================

    /**
     * 记录最后的费用
     */
    private void recordLastCost(UUID playerUUID, long cost) {
        lastCostMap.put(playerUUID, cost);
    }

    /**
     * 获取玩家最后的费用
     */
    public long getLastCost(UUID playerUUID) {
        return lastCostMap.getOrDefault(playerUUID, 0L);
    }

    /**
     * 获取费用字符串
     */
    public String getCostString(double cost) {
        return String.format("%.0f枚", cost);
    }

    /**
     * 获取费用比较信息
     */
    public String getCostComparisonInfo(double originalCost, boolean isVIP) {
        if (!isVIP) {
            return getCostString(originalCost);
        }

        double discountedCost = applyVIPDiscount(originalCost);
        double savings = calculateVIPSavings(originalCost);

        return String.format("&8[&a%.0f&8/&c%.0f&8] &7(省%.0f)",
            discountedCost, originalCost, savings);
    }

    /**
     * 模拟经济系统
     * 在实际应用中，这应该与真实的经济插件集成
     */
    private static class EconomySimulator {
        private final Map<UUID, Double> balances = new HashMap<>();

        public boolean hasBalance(UUID player, double amount) {
            return getBalance(player) >= amount;
        }

        public double getBalance(UUID player) {
            return balances.getOrDefault(player, 1000.0);  // 默认1000枚
        }

        public void withdraw(UUID player, double amount) {
            if (hasBalance(player, amount)) {
                balances.put(player, getBalance(player) - amount);
            }
        }

        public void deposit(UUID player, double amount) {
            balances.put(player, getBalance(player) + amount);
        }
    }
}
