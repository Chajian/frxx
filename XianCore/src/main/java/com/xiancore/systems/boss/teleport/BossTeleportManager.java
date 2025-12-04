package com.xiancore.systems.boss.teleport;

import com.xiancore.systems.boss.permission.BossPermission;
import com.xiancore.systems.boss.permission.BossPermissionManager;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss传送管理器
 * 统一管理所有传送点和玩家传送操作
 *
 * 职责:
 * - 管理传送点
 * - 处理玩家传送请求
 * - 费用计算和扣除
 * - 冷却管理
 * - 权限验证
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
public class BossTeleportManager {

    // ==================== 内部状态 ====================

    /** 传送点映射: ID -> BossTeleportPoint */
    private final Map<String, BossTeleportPoint> teleportPoints;

    /** 费用计算器 */
    private final PricingCalculator pricingCalculator;

    /** 传送调度器 */
    private final TeleportScheduler scheduler;

    /** 权限管理器 */
    private BossPermissionManager permissionManager;

    /** 是否已初始化 */
    private volatile boolean initialized;

    /** 是否启用 */
    private volatile boolean enabled;

    // ==================== 统计 ====================

    /** 总传送次数 */
    private volatile long totalTeleports;

    /** 总费用 */
    private volatile double totalCost;

    // ==================== 常量 ====================

    private static final int DEFAULT_COUNTDOWN = 3; // 秒
    private static final int MAX_TELEPORT_POINTS = 50;

    // ==================== 构造函数 ====================

    public BossTeleportManager() {
        this.teleportPoints = new ConcurrentHashMap<>();
        this.pricingCalculator = new PricingCalculator();
        this.scheduler = new TeleportScheduler();
        this.permissionManager = null;
        this.initialized = false;
        this.enabled = false;
        this.totalTeleports = 0;
        this.totalCost = 0;
    }

    /**
     * 设置权限管理器
     *
     * @param permissionManager 权限管理器
     */
    public void setPermissionManager(BossPermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    // ==================== 生命周期 ====================

    /**
     * 初始化管理器
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            scheduler.start();
            enabled = true;
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            enabled = false;
            scheduler.shutdown();
            teleportPoints.clear();
            initialized = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 传送点管理 ====================

    /**
     * 注册传送点
     *
     * @param point 传送点对象
     * @return 是否成功
     */
    public boolean registerTeleportPoint(BossTeleportPoint point) {
        if (!enabled || point == null) {
            return false;
        }

        if (teleportPoints.size() >= MAX_TELEPORT_POINTS) {
            return false;  // 超过最大数量
        }

        if (teleportPoints.containsKey(point.getPointId())) {
            return false;  // 已存在
        }

        teleportPoints.put(point.getPointId(), point);
        return true;
    }

    /**
     * 注销传送点
     *
     * @param pointId 传送点ID
     * @return 是否成功
     */
    public boolean unregisterTeleportPoint(String pointId) {
        return teleportPoints.remove(pointId) != null;
    }

    /**
     * 获取所有传送点
     *
     * @return 传送点列表
     */
    public List<BossTeleportPoint> getAllTeleportPoints() {
        return new ArrayList<>(teleportPoints.values());
    }

    /**
     * 获取指定的传送点
     *
     * @param pointId 传送点ID
     * @return 传送点对象
     */
    public BossTeleportPoint getTeleportPoint(String pointId) {
        return teleportPoints.get(pointId);
    }

    /**
     * 获取玩家可以访问的传送点
     *
     * @param player 玩家
     * @return 可访问的传送点列表
     */
    public List<BossTeleportPoint> getAccessibleTeleportPoints(Player player) {
        if (player == null || !enabled) {
            return new ArrayList<>();
        }

        List<BossTeleportPoint> accessible = new ArrayList<>();
        boolean isVIP = player.hasPermission("boss.teleport.vip");
        int playerLevel = getPlayerLevel(player);  // 假设方法存在

        for (BossTeleportPoint point : teleportPoints.values()) {
            if (point.canAccess(
                player.hasPermission(point.getPermission() != null ? point.getPermission() : "boss.teleport"),
                playerLevel,
                isVIP)) {
                accessible.add(point);
            }
        }

        return accessible;
    }

    // ==================== 传送操作 ====================

    /**
     * 执行玩家传送
     *
     * @param player 玩家
     * @param pointId 目标传送点ID
     * @return 传送结果
     */
    public TeleportResult teleportPlayer(Player player, String pointId) {
        if (player == null || pointId == null || !enabled) {
            return TeleportResult.FAILED;
        }

        // 检查基础传送权限
        if (permissionManager != null && !permissionManager.hasPermission(player, BossPermission.TELEPORT)) {
            return TeleportResult.NO_PERMISSION;
        }

        BossTeleportPoint point = getTeleportPoint(pointId);
        if (point == null) {
            return TeleportResult.POINT_NOT_FOUND;
        }

        // 检查各种条件
        TeleportResult checkResult = checkCanTeleport(player, point);
        if (checkResult != TeleportResult.SUCCESS) {
            return checkResult;
        }

        // 检查是否免费传送
        boolean hasFreeTP = permissionManager != null && 
            permissionManager.hasPermission(player, BossPermission.TELEPORT_FREE);

        // 计算费用
        double cost = hasFreeTP ? 0 : pricingCalculator.calculateCost(
            player.getLocation(),
            point,
            permissionManager != null && permissionManager.hasPermission(player, BossPermission.TELEPORT)
        );

        // 检查余额
        if (!pricingCalculator.canAfford(player.getUniqueId(), cost)) {
            return TeleportResult.INSUFFICIENT_FUNDS;
        }

        // 执行传送
        try {
            // 扣除费用
            pricingCalculator.deductCost(player.getUniqueId(), cost);

            // 执行传送
            player.teleport(point.getLocation());

            // 记录统计
            totalTeleports++;
            totalCost += cost;
            point.recordUsage(cost);
            scheduler.recordTeleport();
            
            // 只有没有无冷却权限的玩家才添加冷却
            boolean hasNoCooldown = permissionManager != null && 
                permissionManager.hasPermission(player, BossPermission.TELEPORT_NO_COOLDOWN);
            
            if (!hasNoCooldown) {
                scheduler.addCooldown(player.getUniqueId(), 
                    permissionManager != null && permissionManager.hasPermission(player, BossPermission.TELEPORT));
            }

            return TeleportResult.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            // 退款
            pricingCalculator.refund(player.getUniqueId(), cost);
            return TeleportResult.ERROR;
        }
    }

    // ==================== 检查方法 ====================

    /**
     * 检查玩家是否可以传送
     *
     * @param player 玩家
     * @param point 传送点
     * @return 检查结果
     */
    private TeleportResult checkCanTeleport(Player player, BossTeleportPoint point) {
        // 检查传送点启用状态
        if (!point.isEnabled()) {
            return TeleportResult.POINT_DISABLED;
        }

        // 检查冷却（有无冷却权限可以跳过）
        boolean hasNoCooldown = permissionManager != null && 
            permissionManager.hasPermission(player, BossPermission.TELEPORT_NO_COOLDOWN);
        
        if (!hasNoCooldown && scheduler.isInCooldown(player.getUniqueId())) {
            return TeleportResult.IN_COOLDOWN;
        }

        // 检查权限
        boolean isVIP = player.hasPermission("boss.teleport.vip");
        int playerLevel = getPlayerLevel(player);

        if (!point.canAccess(
            player.hasPermission(point.getPermission() != null ? point.getPermission() : "boss.teleport"),
            playerLevel,
            isVIP)) {
            return TeleportResult.ACCESS_DENIED;
        }

        return TeleportResult.SUCCESS;
    }

    /**
     * 获取玩家的等级 (模拟方法)
     */
    private int getPlayerLevel(Player player) {
        // 在实际应用中，这应该从真实的数据库或插件获取
        return 1;
    }

    /**
     * 获取玩家不能传送的原因
     *
     * @param player 玩家
     * @param point 传送点
     * @return 原因字符串
     */
    public String getFailureReason(Player player, BossTeleportPoint point) {
        if (player == null || point == null) {
            return "参数错误";
        }

        TeleportResult result = checkCanTeleport(player, point);
        return switch (result) {
            case SUCCESS -> "可以传送";
            case NO_PERMISSION -> "你没有传送权限 (需要: boss.teleport)";
            case POINT_NOT_FOUND -> "传送点不存在";
            case POINT_DISABLED -> "传送点已禁用";
            case IN_COOLDOWN -> String.format("传送冷却中 (%d秒)", scheduler.getRemainingCooldown(player.getUniqueId()));
            case ACCESS_DENIED -> point.getAccessDeniedReason(
                player.hasPermission(point.getPermission() != null ? point.getPermission() : "boss.teleport"),
                getPlayerLevel(player),
                player.hasPermission("boss.teleport.vip")
            );
            case INSUFFICIENT_FUNDS -> "余额不足";
            case ERROR -> "传送出错";
            default -> "未知错误";
        };
    }

    // ==================== 统计方法 ====================

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format(
            "传送系统: 总传送%d次, 总费用%.0f, 传送点%d个, %s",
            totalTeleports,
            totalCost,
            teleportPoints.size(),
            scheduler.getStatistics()
        );
    }

    /**
     * 获取传送点数量
     */
    public int getTeleportPointCount() {
        return teleportPoints.size();
    }

    // ==================== 传送结果枚举 ====================

    /**
     * 传送结果
     */
    public enum TeleportResult {
        SUCCESS("成功"),
        NO_PERMISSION("没有权限"),
        POINT_NOT_FOUND("传送点不存在"),
        POINT_DISABLED("传送点已禁用"),
        IN_COOLDOWN("在冷却中"),
        ACCESS_DENIED("访问被拒绝"),
        INSUFFICIENT_FUNDS("余额不足"),
        ERROR("执行出错"),
        FAILED("传送失败");

        public final String description;

        TeleportResult(String description) {
            this.description = description;
        }

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }
}
