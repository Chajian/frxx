package com.xiancore.boss.system.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 伤害数据持久化适配器
 * 用于保存和读取 Boss 伤害统计数据
 *
 * XianCore 需要实现此接口以支持数据库操作
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface DamageDataAdapter {

    /**
     * 保存 Boss 伤害记录
     * 应保存的数据包括：Boss UUID、击杀者 UUID、总伤害、参与者数量、时间戳等
     *
     * @param bossUUID Boss 的唯一 ID
     * @param killerUUID 击杀者的 UUID
     * @param totalDamage 总伤害量
     * @param participantCount 参与人数
     * @param durationSeconds Boss 存活时间（秒）
     * @param data 其他数据（可能包含伤害排行等信息）
     * @return 记录是否成功保存
     */
    boolean saveDamageRecord(@NotNull UUID bossUUID,
                            @NotNull UUID killerUUID,
                            double totalDamage,
                            int participantCount,
                            long durationSeconds,
                            @NotNull Map<String, Object> data);

    /**
     * 获取 Boss 的伤害记录
     *
     * @param bossUUID Boss 的唯一 ID
     * @return 伤害记录数据，如果不存在则返回 null
     */
    @Nullable
    Map<String, Object> getDamageRecord(@NotNull UUID bossUUID);

    /**
     * 获取所有伤害历史记录
     *
     * @return 伤害记录列表
     */
    @NotNull
    List<Map<String, Object>> getAllDamageRecords();

    /**
     * 获取伤害历史记录（分页）
     *
     * @param page 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 伤害记录列表
     */
    @NotNull
    List<Map<String, Object>> getDamageRecords(int page, int pageSize);

    /**
     * 保存玩家伤害统计
     *
     * @param bossUUID Boss 的唯一 ID
     * @param playerUUID 玩家 UUID
     * @param damage 伤害量
     * @param timestamp 时间戳
     * @return 是否成功保存
     */
    boolean saveDamageStatistics(@NotNull UUID bossUUID,
                                @NotNull UUID playerUUID,
                                double damage,
                                long timestamp);

    /**
     * 获取玩家对特定 Boss 的伤害统计
     *
     * @param bossUUID Boss 的唯一 ID
     * @param playerUUID 玩家 UUID
     * @return 伤害统计数据
     */
    @Nullable
    Map<String, Object> getPlayerDamage(@NotNull UUID bossUUID,
                                        @NotNull UUID playerUUID);

    /**
     * 删除过期的伤害记录
     *
     * @param daysOld 删除超过多少天的记录
     * @return 删除的记录数量
     */
    int deleteDamageRecordsOlderThan(int daysOld);

    /**
     * 清空所有伤害记录（谨慎使用）
     *
     * @return 是否成功清空
     */
    boolean clearAllDamageRecords();
}
