package com.xiancore.boss.system.lifecycle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Boss 生命周期管理器
 * 管理 Boss 的生成、存活和死亡过程
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface BossLifecycleManager {

    /**
     * 创建并跟踪一个新的 Boss
     *
     * @param bossUUID Boss UUID
     * @param bossType Boss 类型（MythicMobs ID）
     * @param tier Boss 等级
     * @return 生命周期数据
     */
    @NotNull
    BossLifecycleData createBossLifecycle(@NotNull UUID bossUUID,
                                         @NotNull String bossType,
                                         int tier);

    /**
     * 获取 Boss 的生命周期数据
     *
     * @param bossUUID Boss UUID
     * @return 生命周期数据，如果不存在则返回 null
     */
    @Nullable
    BossLifecycleData getLifecycleData(@NotNull UUID bossUUID);

    /**
     * 更新 Boss 状态
     *
     * @param bossUUID Boss UUID
     * @param newStatus 新状态（SPAWNED, FIGHTING, DYING, DEAD, DESPAWNED）
     */
    void updateBossStatus(@NotNull UUID bossUUID, @NotNull String newStatus);

    /**
     * 更新 Boss 的当前生命值
     *
     * @param bossUUID Boss UUID
     * @param health 新的生命值
     */
    void updateBossHealth(@NotNull UUID bossUUID, double health);

    /**
     * 记录 Boss 被击杀
     *
     * @param bossUUID Boss UUID
     * @param killerUUID 击杀者 UUID
     */
    void markBossDead(@NotNull UUID bossUUID, @NotNull UUID killerUUID);

    /**
     * 记录 Boss 消失
     *
     * @param bossUUID Boss UUID
     * @param reason 消失原因（timeout, reload, etc）
     */
    void markBossDespawned(@NotNull UUID bossUUID, @NotNull String reason);

    /**
     * 获取所有活跃的 Boss
     *
     * @return 活跃 Boss 的生命周期数据列表
     */
    @NotNull
    List<BossLifecycleData> getActiveBosses();

    /**
     * 获取指定类型的活跃 Boss 数量
     *
     * @param bossType Boss 类型
     * @return 数量
     */
    int getActiveBossCount(@NotNull String bossType);

    /**
     * 清除已死亡的 Boss 数据
     *
     * @param bossUUID Boss UUID
     */
    void removeBoss(@NotNull UUID bossUUID);

    /**
     * 获取总活跃 Boss 数量
     *
     * @return 数量
     */
    int getTotalActiveBossCount();
}
