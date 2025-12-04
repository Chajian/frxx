package com.xiancore.systems.boss.config;

import com.xiancore.systems.boss.entity.BossSpawnPoint;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss 刷新系统配置
 * 管理所有 Boss 系统的配置数据
 *
 * 包含:
 * - 全局设置 (检查间隔、最大Boss数等)
 * - 刷新点列表
 * - 验证方法
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
@Setter
public class BossRefreshConfig {

    // ==================== 全局设置 ====================

    /** Boss 系统是否启用 */
    private boolean enabled = ConfigConstants.DEFAULT_ENABLED;

    /** 刷新检查间隔 (秒) */
    private int checkIntervalSeconds = ConfigConstants.DEFAULT_CHECK_INTERVAL;

    /** 最多同时活跃的 Boss 数量 */
    private int maxActiveBosses = ConfigConstants.DEFAULT_MAX_ACTIVE_BOSSES;

    /** 最少需要的在线玩家数 */
    private int minOnlinePlayers = ConfigConstants.DEFAULT_MIN_ONLINE_PLAYERS;

    // ==================== 刷新点列表 ====================

    /** 所有刷新点的列表 */
    private List<BossSpawnPoint> spawnPoints = new ArrayList<>();

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public BossRefreshConfig() {
    }

    /**
     * 完整构造函数
     */
    public BossRefreshConfig(boolean enabled, int checkIntervalSeconds,
                            int maxActiveBosses, int minOnlinePlayers) {
        this.enabled = enabled;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.maxActiveBosses = maxActiveBosses;
        this.minOnlinePlayers = minOnlinePlayers;
    }

    /**
     * 获取刷新检查间隔（秒）
     */
    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    /**
     * 获取最多同时活跃的Boss数量
     */
    public int getMaxActiveBosses() {
        return maxActiveBosses;
    }

    /**
     * 获取最少需要的在线玩家数
     */
    public int getMinOnlinePlayers() {
        return minOnlinePlayers;
    }

    /**
     * 获取所有刷新点的列表
     */
    public List<BossSpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    // ==================== 验证方法 ====================

    /**
     * 验证配置的完整性和正确性
     *
     * @return 错误消息列表，如果配置有效则为空列表
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // 验证全局设置
        validateGlobalSettings(errors);

        // 验证刷新点
        validateSpawnPoints(errors);

        return errors;
    }

    /**
     * 验证全局设置
     */
    private void validateGlobalSettings(List<String> errors) {
        // 检查间隔验证
        if (checkIntervalSeconds < ConfigConstants.MIN_CHECK_INTERVAL ||
            checkIntervalSeconds > ConfigConstants.MAX_CHECK_INTERVAL) {
            errors.add(String.format("检查间隔必须在 %d-%d 秒之间，当前值: %d",
                ConfigConstants.MIN_CHECK_INTERVAL,
                ConfigConstants.MAX_CHECK_INTERVAL,
                checkIntervalSeconds));
        }

        // 最大 Boss 数验证
        if (maxActiveBosses < ConfigConstants.MIN_ACTIVE_BOSSES ||
            maxActiveBosses > ConfigConstants.MAX_ACTIVE_BOSSES) {
            errors.add(String.format("最大 Boss 数必须在 %d-%d 之间，当前值: %d",
                ConfigConstants.MIN_ACTIVE_BOSSES,
                ConfigConstants.MAX_ACTIVE_BOSSES,
                maxActiveBosses));
        }

        // 最少玩家数验证
        if (minOnlinePlayers < ConfigConstants.MIN_ONLINE_PLAYERS_LIMIT ||
            minOnlinePlayers > ConfigConstants.MAX_ONLINE_PLAYERS_LIMIT) {
            errors.add(String.format("最少玩家数必须在 %d-%d 之间，当前值: %d",
                ConfigConstants.MIN_ONLINE_PLAYERS_LIMIT,
                ConfigConstants.MAX_ONLINE_PLAYERS_LIMIT,
                minOnlinePlayers));
        }
    }

    /**
     * 验证所有刷新点
     */
    private void validateSpawnPoints(List<String> errors) {
        if (spawnPoints.isEmpty()) {
            errors.add("至少需要配置一个刷新点");
            return;
        }

        for (BossSpawnPoint point : spawnPoints) {
            List<String> pointErrors = point.getValidationErrors();
            if (!pointErrors.isEmpty()) {
                errors.addAll(pointErrors);
            }
        }
    }

    // ==================== 刷新点操作方法 ====================

    /**
     * 添加刷新点
     *
     * @param point 要添加的刷新点
     * @return 是否添加成功
     */
    public boolean addSpawnPoint(BossSpawnPoint point) {
        if (point == null) {
            return false;
        }

        // 检查 ID 是否已存在
        for (BossSpawnPoint existing : spawnPoints) {
            if (existing.getId().equals(point.getId())) {
                return false;
            }
        }

        return spawnPoints.add(point);
    }

    /**
     * 移除刷新点
     *
     * @param id 刷新点 ID
     * @return 是否移除成功
     */
    public boolean removeSpawnPoint(String id) {
        return spawnPoints.removeIf(point -> point.getId().equals(id));
    }

    /**
     * 根据 ID 获取刷新点
     *
     * @param id 刷新点 ID
     * @return 刷新点，如果不存在则返回 null
     */
    public BossSpawnPoint getSpawnPoint(String id) {
        for (BossSpawnPoint point : spawnPoints) {
            if (point.getId().equals(id)) {
                return point;
            }
        }
        return null;
    }

    /**
     * 检查是否存在指定 ID 的刷新点
     *
     * @param id 刷新点 ID
     * @return 是否存在
     */
    public boolean hasSpawnPoint(String id) {
        return getSpawnPoint(id) != null;
    }

    // ==================== 信息方法 ====================

    /**
     * 获取简要信息
     */
    public String getSimpleInfo() {
        return String.format(
            "BossRefreshConfig{enabled=%s, checkInterval=%ds, maxBosses=%d, minPlayers=%d, points=%d}",
            enabled, checkIntervalSeconds, maxActiveBosses, minOnlinePlayers, spawnPoints.size()
        );
    }

    /**
     * 获取详细信息
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("BossRefreshConfig {\n");
        sb.append("  enabled: ").append(enabled).append("\n");
        sb.append("  checkInterval: ").append(checkIntervalSeconds).append(" 秒\n");
        sb.append("  maxActiveBosses: ").append(maxActiveBosses).append("\n");
        sb.append("  minOnlinePlayers: ").append(minOnlinePlayers).append("\n");
        sb.append("  spawnPoints (\n").append(spawnPoints.size()).append(" 个):\n");

        for (BossSpawnPoint point : spawnPoints) {
            sb.append("    - ").append(point.getSimpleInfo()).append("\n");
        }

        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }

    /**
     * 创建默认配置
     *
     * @return 默认配置实例
     */
    public static BossRefreshConfig loadDefault() {
        return new BossRefreshConfig(
            ConfigConstants.DEFAULT_ENABLED,
            ConfigConstants.DEFAULT_CHECK_INTERVAL,
            ConfigConstants.DEFAULT_MAX_ACTIVE_BOSSES,
            ConfigConstants.DEFAULT_MIN_ONLINE_PLAYERS
        );
    }
}
