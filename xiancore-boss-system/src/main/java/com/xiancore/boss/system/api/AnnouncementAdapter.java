package com.xiancore.boss.system.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * 公告处理适配器
 * 用于向玩家发送 Boss 系统相关的公告和消息
 *
 * XianCore 需要实现此接口以支持自定义公告样式
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface AnnouncementAdapter {

    /**
     * 发送全服公告
     *
     * @param title 标题
     * @param message 消息内容
     * @param type 公告类型（如 "boss_spawn", "boss_kill", "boss_warning"）
     */
    void broadcastAnnouncement(@NotNull String title,
                              @NotNull String message,
                              @NotNull String type);

    /**
     * 向指定玩家发送公告
     *
     * @param playerUUID 玩家 UUID
     * @param title 标题
     * @param message 消息内容
     * @param type 公告类型
     */
    void sendAnnouncement(@NotNull UUID playerUUID,
                         @NotNull String title,
                         @NotNull String message,
                         @NotNull String type);

    /**
     * 发送 Boss 生成公告
     *
     * @param bossType Boss 类型（MythicMobs ID）
     * @param tier Boss 等级
     * @param locationName 生成位置名称
     */
    void announceBossSpawned(@NotNull String bossType,
                            int tier,
                            @NotNull String locationName);

    /**
     * 发送 Boss 击杀公告
     *
     * @param bossType Boss 类型
     * @param killerName 击杀者名称
     * @param tier Boss 等级
     */
    void announceBossKilled(@NotNull String bossType,
                           @NotNull String killerName,
                           int tier);

    /**
     * 发送 Boss 警告消息（如 Boss 即将死亡）
     *
     * @param bossType Boss 类型
     * @param message 警告消息
     * @param warningLevel 警告级别（1-3，3 为最严重）
     */
    void sendBossWarning(@NotNull String bossType,
                        @NotNull String message,
                        int warningLevel);

    /**
     * 发送伤害排行信息
     *
     * @param bossType Boss 类型
     * @param rankings 伤害排行数据（通常为 JSON 格式或 Map）
     */
    void announceDamageRankings(@NotNull String bossType,
                               @NotNull String rankings);
}
