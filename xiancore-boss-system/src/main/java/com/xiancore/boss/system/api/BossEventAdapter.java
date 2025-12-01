package com.xiancore.boss.system.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Boss 事件适配器
 * XianCore 需要实现此接口以接收 Boss 系统的事件通知
 *
 * 新模块会在重要事件发生时调用这些方法
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface BossEventAdapter {

    /**
     * Boss 生成事件
     * 当 Boss 被生成时调用
     *
     * @param bossUUID Boss 的唯一 ID
     * @param bossType Boss 类型（如 MythicMobs ID）
     * @param tier Boss 等级（1-4）
     * @param data 额外的事件数据
     */
    void onBossSpawned(@NotNull UUID bossUUID,
                      @NotNull String bossType,
                      int tier,
                      @NotNull Map<String, Object> data);

    /**
     * Boss 击杀事件
     * 当 Boss 被击杀时调用
     *
     * @param bossUUID Boss 的唯一 ID
     * @param killerUUID 最后击杀者的 UUID
     * @param totalDamage 总伤害量
     * @param participantCount 参与人数
     * @param data 额外的事件数据
     */
    void onBossKilled(@NotNull UUID bossUUID,
                     @NotNull UUID killerUUID,
                     double totalDamage,
                     int participantCount,
                     @NotNull Map<String, Object> data);

    /**
     * Boss 消失事件
     * 当 Boss 消失（不是被击杀）时调用
     *
     * @param bossUUID Boss 的唯一 ID
     * @param reason 消失原因（如 "timeout", "reload"）
     * @param data 额外的事件数据
     */
    void onBossDespawned(@NotNull UUID bossUUID,
                        @NotNull String reason,
                        @NotNull Map<String, Object> data);

    /**
     * Boss 伤害记录事件
     * 当有玩家伤害 Boss 时调用
     *
     * @param bossUUID Boss 的唯一 ID
     * @param playerUUID 玩家 UUID
     * @param damage 伤害量
     */
    void onBossDamaged(@NotNull UUID bossUUID,
                      @NotNull UUID playerUUID,
                      double damage);

    /**
     * Boss 属性更新事件
     * 当 Boss 的属性发生变化时调用（如难度调整）
     *
     * @param bossUUID Boss 的唯一 ID
     * @param attributeType 属性类型（如 "health", "armor"）
     * @param newValue 新值
     */
    void onBossAttributeUpdated(@NotNull UUID bossUUID,
                               @NotNull String attributeType,
                               double newValue);

    /**
     * Boss 系统错误事件
     * 当 Boss 系统发生错误时调用
     *
     * @param errorType 错误类型
     * @param message 错误消息
     * @param exception 异常（可能为 null）
     */
    void onBossSystemError(@NotNull String errorType,
                          @NotNull String message,
                          Exception exception);
}
