package com.xiancore.systems.boss.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss 系统配置验证器
 * 验证配置的完整性、正确性和一致性
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class ConfigValidator {

    /**
     * 验证完整的配置
     *
     * @param config 要验证的配置
     * @return 错误消息列表，如果配置有效则为空列表
     */
    public List<String> validateConfig(BossRefreshConfig config) {
        if (config == null) {
            List<String> errors = new ArrayList<>();
            errors.add("配置对象不能为空");
            return errors;
        }

        return config.validate();
    }

    /**
     * 验证检查间隔设置
     *
     * @param checkIntervalSeconds 检查间隔 (秒)
     * @return 错误消息，如果有效则返回 null
     */
    public String validateCheckInterval(int checkIntervalSeconds) {
        if (checkIntervalSeconds < ConfigConstants.MIN_CHECK_INTERVAL) {
            return String.format("检查间隔过小: %d (最小: %d)",
                checkIntervalSeconds, ConfigConstants.MIN_CHECK_INTERVAL);
        }

        if (checkIntervalSeconds > ConfigConstants.MAX_CHECK_INTERVAL) {
            return String.format("检查间隔过大: %d (最大: %d)",
                checkIntervalSeconds, ConfigConstants.MAX_CHECK_INTERVAL);
        }

        return null;
    }

    /**
     * 验证最大 Boss 数设置
     *
     * @param maxActiveBosses 最大 Boss 数
     * @return 错误消息，如果有效则返回 null
     */
    public String validateMaxActiveBosses(int maxActiveBosses) {
        if (maxActiveBosses < ConfigConstants.MIN_ACTIVE_BOSSES) {
            return String.format("最大 Boss 数过小: %d (最小: %d)",
                maxActiveBosses, ConfigConstants.MIN_ACTIVE_BOSSES);
        }

        if (maxActiveBosses > ConfigConstants.MAX_ACTIVE_BOSSES) {
            return String.format("最大 Boss 数过大: %d (最大: %d)",
                maxActiveBosses, ConfigConstants.MAX_ACTIVE_BOSSES);
        }

        return null;
    }

    /**
     * 验证最少玩家数设置
     *
     * @param minOnlinePlayers 最少玩家数
     * @return 错误消息，如果有效则返回 null
     */
    public String validateMinOnlinePlayers(int minOnlinePlayers) {
        if (minOnlinePlayers < ConfigConstants.MIN_ONLINE_PLAYERS_LIMIT) {
            return String.format("最少玩家数过小: %d (最小: %d)",
                minOnlinePlayers, ConfigConstants.MIN_ONLINE_PLAYERS_LIMIT);
        }

        if (minOnlinePlayers > ConfigConstants.MAX_ONLINE_PLAYERS_LIMIT) {
            return String.format("最少玩家数过大: %d (最大: %d)",
                minOnlinePlayers, ConfigConstants.MAX_ONLINE_PLAYERS_LIMIT);
        }

        return null;
    }

    /**
     * 验证 Boss 等级
     *
     * @param tier Boss 等级
     * @return 错误消息，如果有效则返回 null
     */
    public String validateTier(int tier) {
        if (tier < ConfigConstants.MIN_TIER || tier > ConfigConstants.MAX_TIER) {
            return String.format("Boss 等级无效: %d (范围: %d-%d)",
                tier, ConfigConstants.MIN_TIER, ConfigConstants.MAX_TIER);
        }
        return null;
    }

    /**
     * 验证冷却时间
     *
     * @param cooldownSeconds 冷却时间 (秒)
     * @return 错误消息，如果有效则返回 null
     */
    public String validateCooldown(long cooldownSeconds) {
        if (cooldownSeconds < ConfigConstants.MIN_COOLDOWN_SECONDS) {
            return String.format("冷却时间过短: %d (最小: %d)",
                cooldownSeconds, ConfigConstants.MIN_COOLDOWN_SECONDS);
        }
        return null;
    }

    /**
     * 验证最大数量
     *
     * @param maxCount 最大数量
     * @return 错误消息，如果有效则返回 null
     */
    public String validateMaxCount(int maxCount) {
        if (maxCount < ConfigConstants.MIN_MAX_COUNT) {
            return String.format("最大数量无效: %d (最小: %d)",
                maxCount, ConfigConstants.MIN_MAX_COUNT);
        }
        return null;
    }

    /**
     * 验证位置字符串格式
     *
     * @param location 位置字符串 (格式: world,x,y,z)
     * @return 错误消息，如果有效则返回 null
     */
    public String validateLocationString(String location) {
        if (location == null || location.isEmpty()) {
            return "位置不能为空";
        }

        String[] parts = location.split(",");
        if (parts.length != 4) {
            return String.format("位置格式错误: %s (期望: world,x,y,z)", location);
        }

        String world = parts[0].trim();
        if (world.isEmpty()) {
            return "世界名称不能为空";
        }

        try {
            Integer.parseInt(parts[1].trim());
            Integer.parseInt(parts[2].trim());
            Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException e) {
            return String.format("位置坐标格式错误: %s", location);
        }

        return null;
    }

    /**
     * 验证 MythicMobs ID
     *
     * @param mythicMobId MythicMobs ID
     * @return 错误消息，如果有效则返回 null
     */
    public String validateMythicMobId(String mythicMobId) {
        if (mythicMobId == null || mythicMobId.isEmpty()) {
            return "MythicMobs ID 不能为空";
        }

        // 检查是否包含非法字符
        if (!mythicMobId.matches("[a-zA-Z0-9_]+")) {
            return String.format("MythicMobs ID 包含非法字符: %s", mythicMobId);
        }

        return null;
    }

    /**
     * 验证刷新点 ID
     *
     * @param id 刷新点 ID
     * @return 错误消息，如果有效则返回 null
     */
    public String validateSpawnPointId(String id) {
        if (id == null || id.isEmpty()) {
            return "刷新点 ID 不能为空";
        }

        // 检查是否包含非法字符
        if (!id.matches("[a-zA-Z0-9_-]+")) {
            return String.format("刷新点 ID 包含非法字符: %s", id);
        }

        return null;
    }
}
