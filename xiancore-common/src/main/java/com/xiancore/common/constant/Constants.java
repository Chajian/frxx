package com.xiancore.common.constant;

/**
 * 应用常量定义
 */
public class Constants {

    // ==================== 应用配置 ====================

    /** 默认Web服务URL */
    public static final String DEFAULT_WEB_SERVICE_URL = "http://localhost:8080";

    /** API基础路径 */
    public static final String API_BASE_PATH = "/api";

    /** WebSocket基础路径 */
    public static final String WEBSOCKET_BASE_PATH = "/ws";

    // ==================== Boss状态 ====================

    /** Boss状态: 已生成 */
    public static final String BOSS_STATUS_SPAWNED = "SPAWNED";

    /** Boss状态: 存活 */
    public static final String BOSS_STATUS_ALIVE = "ALIVE";

    /** Boss状态: 已死亡 */
    public static final String BOSS_STATUS_DEAD = "DEAD";

    /** Boss状态: 已消失 */
    public static final String BOSS_STATUS_DESPAWNED = "DESPAWNED";

    // ==================== 难度等级 ====================

    /** 难度: 微不足道 */
    public static final String DIFFICULTY_TRIVIAL = "TRIVIAL";

    /** 难度: 简单 */
    public static final String DIFFICULTY_EASY = "EASY";

    /** 难度: 普通 */
    public static final String DIFFICULTY_NORMAL = "NORMAL";

    /** 难度: 困难 */
    public static final String DIFFICULTY_HARD = "HARD";

    /** 难度: 炼狱 */
    public static final String DIFFICULTY_INSANE = "INSANE";

    // ==================== 伤害类型 ====================

    /** 伤害类型: 物理伤害 */
    public static final String DAMAGE_TYPE_PHYSICAL = "PHYSICAL";

    /** 伤害类型: 魔法伤害 */
    public static final String DAMAGE_TYPE_MAGICAL = "MAGICAL";

    /** 伤害类型: 真实伤害 */
    public static final String DAMAGE_TYPE_TRUE = "TRUE_DAMAGE";

    // ==================== API端点 ====================

    /** API端点: 获取所有Boss */
    public static final String API_GET_BOSSES = API_BASE_PATH + "/bosses";

    /** API端点: 创建Boss */
    public static final String API_CREATE_BOSS = API_BASE_PATH + "/bosses";

    /** API端点: 更新Boss */
    public static final String API_UPDATE_BOSS = API_BASE_PATH + "/bosses/{id}";

    /** API端点: 删除Boss */
    public static final String API_DELETE_BOSS = API_BASE_PATH + "/bosses/{id}";

    /** API端点: 记录伤害 */
    public static final String API_RECORD_DAMAGE = API_BASE_PATH + "/damage";

    /** API端点: 获取统计 */
    public static final String API_GET_STATS = API_BASE_PATH + "/stats";

    // ==================== WebSocket Topic ====================

    /** WebSocket Topic: Boss事件 */
    public static final String WS_TOPIC_BOSS_EVENTS = "/topic/boss-events";

    /** WebSocket Topic: 击杀事件 */
    public static final String WS_TOPIC_KILL_EVENTS = "/topic/kill-events";

    /** WebSocket Topic: 统计更新 */
    public static final String WS_TOPIC_STATS_UPDATE = "/topic/stats-update";

    /** WebSocket Topic: 系统告警 */
    public static final String WS_TOPIC_ALERTS = "/topic/alerts";

    /** WebSocket Topic: 系统状态 */
    public static final String WS_TOPIC_SYSTEM_STATUS = "/topic/system-status";

    // ==================== 超时时间 ====================

    /** HTTP请求超时时间 (毫秒) */
    public static final int HTTP_TIMEOUT_MS = 5000;

    /** 数据库连接超时时间 (毫秒) */
    public static final int DB_TIMEOUT_MS = 10000;

    /** 缓存过期时间 (秒) */
    public static final int CACHE_TTL_SECONDS = 300;

    // ==================== 错误码 ====================

    /** 错误码: 成功 */
    public static final Integer ERROR_CODE_SUCCESS = 0;

    /** 错误码: 通用错误 */
    public static final Integer ERROR_CODE_GENERAL = 1;

    /** 错误码: 参数错误 */
    public static final Integer ERROR_CODE_INVALID_PARAM = 400;

    /** 错误码: 未找到 */
    public static final Integer ERROR_CODE_NOT_FOUND = 404;

    /** 错误码: 服务器错误 */
    public static final Integer ERROR_CODE_SERVER_ERROR = 500;

    // ==================== 日志配置 ====================

    /** 日志: 插件启动 */
    public static final String LOG_PLUGIN_STARTED = "XianCore插件已启动";

    /** 日志: Web服务启动 */
    public static final String LOG_WEB_STARTED = "XianCore Web服务已启动";

    /** 日志: 网络连接失败 */
    public static final String LOG_NETWORK_ERROR = "网络连接失败";
}
