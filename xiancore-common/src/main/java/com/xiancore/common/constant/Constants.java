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
}
