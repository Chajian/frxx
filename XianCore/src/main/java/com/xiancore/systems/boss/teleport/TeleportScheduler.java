package com.xiancore.systems.boss.teleport;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;

/**
 * 传送调度器
 * 管理玩家的冷却时间和倒计时
 *
 * 职责:
 * - 冷却时间管理
 * - 传送倒计时
 * - 异步处理
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
public class TeleportScheduler {

    // ==================== 常量 ====================

    /** 默认冷却时间 (秒) */
    private static final long DEFAULT_COOLDOWN = 60;

    /** VIP冷却时间 (秒) */
    private static final long VIP_COOLDOWN = 30;

    // ==================== 内部状态 ====================

    /** 冷却映射: UUID -> 冷却结束时间 */
    private final Map<UUID, Long> cooldownMap;

    /** 倒计时任务映射: UUID -> ScheduledFuture */
    private final Map<UUID, ScheduledFuture<?>> countdownTasks;

    /** 定时执行器 */
    private final ScheduledExecutorService executor;

    /** 是否已启用 */
    private volatile boolean enabled;

    // ==================== 统计 ====================

    /** 总传送次数 */
    private volatile long totalTeleports;

    // ==================== 构造函数 ====================

    public TeleportScheduler() {
        this.cooldownMap = new ConcurrentHashMap<>();
        this.countdownTasks = new ConcurrentHashMap<>();
        this.enabled = false;
        this.totalTeleports = 0;

        // 创建定时执行器
        this.executor = Executors.newScheduledThreadPool(1,
            r -> {
                Thread t = new Thread(r, "BossTeleportScheduler");
                t.setDaemon(true);
                return t;
            });
    }

    // ==================== 生命周期 ====================

    /**
     * 启用调度器
     */
    public void start() {
        if (enabled) {
            return;
        }

        enabled = true;

        // 启动冷却过期检查任务
        executor.scheduleAtFixedRate(
            this::cleanupExpiredCooldowns,
            1,     // 1秒后开始
            5,     // 每5秒检查一次
            TimeUnit.SECONDS
        );
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        if (!enabled) {
            return;
        }

        enabled = false;

        // 取消所有倒计时任务
        countdownTasks.values().forEach(future -> future.cancel(false));
        countdownTasks.clear();

        // 关闭执行器
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        cooldownMap.clear();
    }

    // ==================== 冷却管理 ====================

    /**
     * 添加冷却
     *
     * @param playerUUID 玩家UUID
     * @param isVIP 是否为VIP
     */
    public void addCooldown(UUID playerUUID, boolean isVIP) {
        long cooldownSeconds = isVIP ? VIP_COOLDOWN : DEFAULT_COOLDOWN;
        addCooldown(playerUUID, cooldownSeconds);
    }

    /**
     * 添加指定时长的冷却
     *
     * @param playerUUID 玩家UUID
     * @param cooldownSeconds 冷却秒数
     */
    public void addCooldown(UUID playerUUID, long cooldownSeconds) {
        if (playerUUID == null || cooldownSeconds <= 0) {
            return;
        }

        long cooldownEndTime = System.currentTimeMillis() + (cooldownSeconds * 1000);
        cooldownMap.put(playerUUID, cooldownEndTime);
    }

    /**
     * 检查是否在冷却中
     *
     * @param playerUUID 玩家UUID
     * @return 是否在冷却中
     */
    public boolean isInCooldown(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }

        Long cooldownEndTime = cooldownMap.get(playerUUID);
        if (cooldownEndTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= cooldownEndTime) {
            cooldownMap.remove(playerUUID);
            return false;
        }

        return true;
    }

    /**
     * 获取剩余冷却时间 (秒)
     *
     * @param playerUUID 玩家UUID
     * @return 剩余秒数，0表示无冷却
     */
    public long getRemainingCooldown(UUID playerUUID) {
        if (playerUUID == null) {
            return 0;
        }

        Long cooldownEndTime = cooldownMap.get(playerUUID);
        if (cooldownEndTime == null) {
            return 0;
        }

        long remaining = cooldownEndTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldownMap.remove(playerUUID);
            return 0;
        }

        return remaining / 1000;
    }

    /**
     * 移除冷却
     *
     * @param playerUUID 玩家UUID
     */
    public void removeCooldown(UUID playerUUID) {
        if (playerUUID != null) {
            cooldownMap.remove(playerUUID);
        }
    }

    /**
     * 清理过期的冷却
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        cooldownMap.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }

    // ==================== 倒计时 ====================

    /**
     * 调度倒计时
     *
     * @param player 玩家
     * @param countdownSeconds 倒计时秒数
     * @param callback 回调接口
     */
    public void scheduleCountdown(Player player, int countdownSeconds, CountdownCallback callback) {
        if (player == null || !enabled || countdownSeconds <= 0) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        // 取消之前的倒计时
        cancelCountdown(playerUUID);

        // 创建新的倒计时任务
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            () -> {
                // 倒计时逻辑
                callback.onCountdown(player, countdownSeconds);
            },
            0,
            1,
            TimeUnit.SECONDS
        );

        countdownTasks.put(playerUUID, future);
    }

    /**
     * 简化的倒计时调度 (仅通知)
     *
     * @param player 玩家
     * @param countdownSeconds 倒计时秒数
     */
    public void scheduleSimpleCountdown(Player player, int countdownSeconds) {
        scheduleCountdown(player, countdownSeconds, (p, seconds) -> {
            // 在实际应用中会显示倒计时
        });
    }

    /**
     * 取消倒计时
     *
     * @param playerUUID 玩家UUID
     */
    public void cancelCountdown(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        ScheduledFuture<?> future = countdownTasks.remove(playerUUID);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 取消玩家的倒计时
     *
     * @param player 玩家
     */
    public void cancelCountdown(Player player) {
        if (player != null) {
            cancelCountdown(player.getUniqueId());
        }
    }

    /**
     * 检查是否有活跃的倒计时
     *
     * @param playerUUID 玩家UUID
     * @return 是否有倒计时
     */
    public boolean hasActiveCountdown(UUID playerUUID) {
        if (playerUUID == null) {
            return false;
        }

        ScheduledFuture<?> future = countdownTasks.get(playerUUID);
        if (future == null) {
            return false;
        }

        if (future.isDone() || future.isCancelled()) {
            countdownTasks.remove(playerUUID);
            return false;
        }

        return true;
    }

    // ==================== 统计 ====================

    /**
     * 记录一次传送
     */
    public void recordTeleport() {
        totalTeleports++;
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format(
            "传送调度: 总传送%d次, 冷却中%d个, 倒计时中%d个",
            totalTeleports,
            cooldownMap.size(),
            countdownTasks.size()
        );
    }

    /**
     * 获取冷却数量
     */
    public int getCooldownCount() {
        return cooldownMap.size();
    }

    /**
     * 获取倒计时数量
     */
    public int getCountdownCount() {
        return countdownTasks.size();
    }

    // ==================== 倒计时回调接口 ====================

    /**
     * 倒计时回调接口
     */
    public interface CountdownCallback {
        /**
         * 倒计时回调
         *
         * @param player 玩家
         * @param remainingSeconds 剩余秒数
         */
        void onCountdown(Player player, int remainingSeconds);
    }
}
