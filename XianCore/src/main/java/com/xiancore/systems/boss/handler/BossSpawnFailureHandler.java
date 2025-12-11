package com.xiancore.systems.boss.handler;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import com.xiancore.systems.boss.spawner.MobSpawner;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.logging.Logger;

/**
 * Boss生成失败处理器 - 处理生成失败的情况
 *
 * 职责:
 * - 检测生成失败
 * - 记录失败原因
 * - 触发重试机制
 * - 降级处理（使用备用方案）
 * - 发送警告通知
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-16
 */
public class BossSpawnFailureHandler {

    private final XianCore plugin;
    private final Logger logger;
    private final BossRefreshManager bossManager;
    private final MobSpawner mobSpawner;

    // 失败统计
    private volatile int totalSpawnAttempts = 0;
    private volatile int totalSpawnFailures = 0;
    private volatile int consecutiveFailures = 0;
    private volatile long lastFailureTime = 0;

    // 重试配置
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100; // 毫秒
    private static final long FAILURE_THRESHOLD_MS = 60000; // 1分钟内失败达到阈值则警告

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param bossManager Boss管理器
     * @param mobSpawner 怪物生成器
     */
    public BossSpawnFailureHandler(XianCore plugin, BossRefreshManager bossManager,
                                   MobSpawner mobSpawner) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.bossManager = bossManager;
        this.mobSpawner = mobSpawner;
    }

    /**
     * 处理Boss生成失败
     * 带重试机制的生成处理
     *
     * @param mobType MythicMobs怪物类型
     * @param location 生成位置
     * @param spawnPoint 刷新点
     * @return 生成成功返回LivingEntity，失败返回null
     */
    public LivingEntity handleSpawnWithRetry(String mobType, Location location, BossSpawnPoint spawnPoint) {
        totalSpawnAttempts++;

        LivingEntity entity = null;
        Exception lastException = null;

        // 尝试最多MAX_RETRIES次
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                entity = mobSpawner.spawn(mobType, location);

                if (entity != null) {
                    // 生成成功，重置失败计数
                    consecutiveFailures = 0;
                    logger.info("✓ Boss生成成功 (第 " + attempt + " 次尝试): " + mobType);
                    return entity;
                }

                // entity为null，继续重试
                if (attempt < MAX_RETRIES) {
                    logger.warning("⚠ Boss生成返回null，准备重试 (第 " + attempt + " 次): " + mobType);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

            } catch (Exception e) {
                lastException = e;
                logger.warning("⚠ Boss生成异常 (第 " + attempt + " 次): " + e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 所有重试都失败了
        return handleSpawnFailure(mobType, location, spawnPoint, lastException);
    }

    /**
     * 处理Boss生成失败的情况
     *
     * @param mobType MythicMobs怪物类型
     * @param location 生成位置
     * @param spawnPoint 刷新点
     * @param exception 异常信息
     * @return 总是返回null（生成失败）
     */
    protected LivingEntity handleSpawnFailure(String mobType, Location location,
                                            BossSpawnPoint spawnPoint, Exception exception) {
        totalSpawnFailures++;
        consecutiveFailures++;
        long currentTime = System.currentTimeMillis();

        // 记录失败信息
        logger.severe("✗ Boss生成最终失败: " + mobType);
        logger.severe("  - 刷新点: " + (spawnPoint != null ? spawnPoint.getId() : "Unknown"));
        logger.severe("  - 位置: " + (location != null ? location.toString() : "null"));
        logger.severe("  - 失败原因: " + (exception != null ? exception.getMessage() : "未知原因"));
        logger.severe("  - 连续失败次数: " + consecutiveFailures);

        // 检查是否需要警告
        if (currentTime - lastFailureTime < FAILURE_THRESHOLD_MS) {
            if (consecutiveFailures >= 5) {
                logger.severe("⚠⚠⚠ 警告: 1分钟内Boss生成失败 " + consecutiveFailures + " 次!");
                logger.severe("⚠⚠⚠ 请检查MythicMobs配置或服务器日志!");

                // 可以在这里添加通知管理员的机制
                notifyAdminsOfSpawnFailure(mobType, consecutiveFailures);
            }
        } else {
            // 重置失败计时
            lastFailureTime = currentTime;
            consecutiveFailures = 1;
        }

        // 打印异常堆栈
        if (exception != null) {
            exception.printStackTrace();
        }

        return null;
    }

    /**
     * 通知管理员生成失败
     *
     * @param mobType 怪物类型
     * @param failureCount 失败次数
     */
    private void notifyAdminsOfSpawnFailure(String mobType, int failureCount) {
        String message = "§c[Boss系统] Boss生成失败过于频繁! 怪物类型: " + mobType + ", 失败次数: " + failureCount;

        // 通知在线的管理员
        plugin.getServer().getOnlinePlayers().stream()
            .filter(p -> p.hasPermission("boss.admin"))
            .forEach(p -> p.sendMessage(message));
    }

    /**
     * 检查 MobSpawner 是否可用
     *
     * @return 是否可用
     */
    public boolean isMythicIntegrationAvailable() {
        return mobSpawner != null && mobSpawner.isAvailable();
    }

    /**
     * 验证生成参数
     *
     * @param mobType 怪物类型
     * @param location 位置
     * @return 是否有效
     */
    public boolean validateSpawnParameters(String mobType, Location location) {
        if (mobType == null || mobType.isEmpty()) {
            logger.warning("✗ 怪物类型为空");
            return false;
        }

        if (location == null) {
            logger.warning("✗ 生成位置为null");
            return false;
        }

        if (location.getWorld() == null) {
            logger.warning("✗ 生成位置所在世界不存在");
            return false;
        }

        // 检查怪物类型是否存在
        if (!mobSpawner.hasMobType(mobType)) {
            logger.warning("✗ 怪物类型不存在: " + mobType);
            return false;
        }

        return true;
    }

    /**
     * 获取生成成功率
     *
     * @return 成功率百分比 (0-100)
     */
    public double getSpawnSuccessRate() {
        if (totalSpawnAttempts == 0) {
            return 100.0;
        }
        return ((double) (totalSpawnAttempts - totalSpawnFailures) / totalSpawnAttempts) * 100;
    }

    /**
     * 获取统计信息
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        return String.format(
            "Boss生成统计: 总尝试=%d, 成功=%d, 失败=%d, 成功率=%.1f%%, 连续失败=%d",
            totalSpawnAttempts,
            totalSpawnAttempts - totalSpawnFailures,
            totalSpawnFailures,
            getSpawnSuccessRate(),
            consecutiveFailures
        );
    }

    /**
     * 重置失败统计
     */
    public void resetStatistics() {
        totalSpawnAttempts = 0;
        totalSpawnFailures = 0;
        consecutiveFailures = 0;
        lastFailureTime = 0;
        logger.info("✓ Boss生成统计已重置");
    }

    // ==================== Getter 方法 ====================

    public int getTotalSpawnAttempts() {
        return totalSpawnAttempts;
    }

    public int getTotalSpawnFailures() {
        return totalSpawnFailures;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }
}
