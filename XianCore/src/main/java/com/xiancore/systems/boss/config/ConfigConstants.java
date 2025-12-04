package com.xiancore.systems.boss.config;

/**
 * Boss 系统配置常量定义
 * 定义所有配置相关的常量，便于维护和修改
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class ConfigConstants {

    // ==================== 文件相关常量 ====================

    /** 配置文件名称 */
    public static final String CONFIG_FILE_NAME = "boss-refresh.yml";

    /** 配置文件根路径 */
    public static final String CONFIG_ROOT_PATH = "boss-refresh";

    // ==================== 全局配置路径 ====================

    /** 全局配置节点 */
    public static final String GLOBAL_CONFIG_PATH = CONFIG_ROOT_PATH + ".global";

    /** 检查间隔路径 */
    public static final String CHECK_INTERVAL_PATH = GLOBAL_CONFIG_PATH + ".check-interval";

    /** 最大 Boss 数路径 */
    public static final String MAX_ACTIVE_BOSSES_PATH = GLOBAL_CONFIG_PATH + ".max-active-bosses";

    /** 最少玩家数路径 */
    public static final String MIN_ONLINE_PLAYERS_PATH = GLOBAL_CONFIG_PATH + ".min-online-players";

    /** 启用状态路径 */
    public static final String ENABLED_PATH = GLOBAL_CONFIG_PATH + ".enabled";

    // ==================== 刷新点配置路径 ====================

    /** 刷新点根路径 */
    public static final String SPAWN_POINTS_PATH = CONFIG_ROOT_PATH + ".spawn-points";

    // ==================== 默认值 ====================

    /** 默认检查间隔 (秒) */
    public static final int DEFAULT_CHECK_INTERVAL = 30;

    /** 默认最大 Boss 数 */
    public static final int DEFAULT_MAX_ACTIVE_BOSSES = 10;

    /** 默认最少玩家数 */
    public static final int DEFAULT_MIN_ONLINE_PLAYERS = 3;

    /** 默认启用状态 */
    public static final boolean DEFAULT_ENABLED = true;

    /** 默认 Boss 等级 */
    public static final int DEFAULT_TIER = 1;

    /** 默认冷却时间 (秒) */
    public static final long DEFAULT_COOLDOWN_SECONDS = 3600;

    /** 默认最大数量 */
    public static final int DEFAULT_MAX_COUNT = 1;

    // ==================== 验证范围 ====================

    /** 最小检查间隔 (秒) */
    public static final int MIN_CHECK_INTERVAL = 5;

    /** 最大检查间隔 (秒) */
    public static final int MAX_CHECK_INTERVAL = 3600;

    /** 最小 Boss 数 */
    public static final int MIN_ACTIVE_BOSSES = 1;

    /** 最大 Boss 数 */
    public static final int MAX_ACTIVE_BOSSES = 100;

    /** 最小玩家数 */
    public static final int MIN_ONLINE_PLAYERS_LIMIT = 1;

    /** 最大玩家数 */
    public static final int MAX_ONLINE_PLAYERS_LIMIT = 50;

    /** 最小冷却时间 (秒) */
    public static final long MIN_COOLDOWN_SECONDS = 60;

    /** 最小 Tier */
    public static final int MIN_TIER = 1;

    /** 最大 Tier */
    public static final int MAX_TIER = 4;

    /** 最小最大数量 */
    public static final int MIN_MAX_COUNT = 1;

    // ==================== 私有构造函数 ====================

    /**
     * 私有构造函数，防止实例化
     */
    private ConfigConstants() {
        throw new UnsupportedOperationException("无法实例化常量类");
    }
}
