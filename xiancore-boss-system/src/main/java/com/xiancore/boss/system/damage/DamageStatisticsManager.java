package com.xiancore.boss.system.damage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 伤害统计管理器
 * 管理和查询 Boss 的伤害统计数据
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface DamageStatisticsManager {

    /**
     * 记录一次伤害
     *
     * @param bossUUID Boss UUID
     * @param playerUUID 玩家 UUID
     * @param damage 伤害量
     */
    void recordDamage(@NotNull UUID bossUUID, @NotNull UUID playerUUID, double damage);

    /**
     * 获取玩家对 Boss 造成的总伤害
     *
     * @param bossUUID Boss UUID
     * @param playerUUID 玩家 UUID
     * @return 总伤害，如果没有记录则返回 0
     */
    double getPlayerDamage(@NotNull UUID bossUUID, @NotNull UUID playerUUID);

    /**
     * 获取 Boss 的总伤害统计
     *
     * @param bossUUID Boss UUID
     * @return 总伤害，如果没有记录则返回 0
     */
    double getTotalDamage(@NotNull UUID bossUUID);

    /**
     * 获取 Boss 的伤害排行（前 N 位）
     *
     * @param bossUUID Boss UUID
     * @param limit 显示的排行数量
     * @return 伤害排行列表
     */
    @NotNull
    List<Map<String, Object>> getDamageRankings(@NotNull UUID bossUUID, int limit);

    /**
     * 获取 Boss 的所有伤害统计信息
     *
     * @param bossUUID Boss UUID
     * @return 伤害统计 Map，key 为玩家 UUID，value 为伤害量
     */
    @NotNull
    Map<UUID, Double> getAllDamageStatistics(@NotNull UUID bossUUID);

    /**
     * 保存 Boss 的伤害记录
     *
     * @param damageRecord 伤害记录
     * @return 是否成功保存
     */
    boolean saveDamageRecord(@NotNull DamageRecord damageRecord);

    /**
     * 清除指定 Boss 的伤害统计
     *
     * @param bossUUID Boss UUID
     */
    void clearDamageStatistics(@NotNull UUID bossUUID);

    /**
     * 获取历史伤害记录
     *
     * @return 所有伤害记录列表
     */
    @NotNull
    List<DamageRecord> getHistoryRecords();

    /**
     * 获取历史伤害记录（分页）
     *
     * @param page 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 伤害记录列表
     */
    @NotNull
    List<DamageRecord> getHistoryRecords(int page, int pageSize);
}
