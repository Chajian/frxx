package com.xiancore.boss.system.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Boss 实体适配器
 * 用于在 Bukkit 世界中创建和管理 Boss 实体
 *
 * XianCore 需要实现此接口以在游戏中生成 Boss
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface BossEntityAdapter {

    /**
     * 创建并生成 Boss 实体
     *
     * @param bossUUID Boss 的唯一 ID
     * @param mythicMobType MythicMobs ID（如 "ElderDragon"）
     * @param x 世界坐标 X
     * @param y 世界坐标 Y
     * @param z 世界坐标 Z
     * @param worldName 世界名称
     * @return 是否成功生成
     */
    boolean spawnBoss(@NotNull UUID bossUUID,
                      @NotNull String mythicMobType,
                      double x, double y, double z,
                      @NotNull String worldName);

    /**
     * 移除 Boss 实体
     *
     * @param bossUUID Boss 的唯一 ID
     * @return 是否成功移除
     */
    boolean despawnBoss(@NotNull UUID bossUUID);

    /**
     * 检查 Boss 是否存在
     *
     * @param bossUUID Boss 的唯一 ID
     * @return 是否存在
     */
    boolean isBossAlive(@NotNull UUID bossUUID);

    /**
     * 获取 Boss 当前生命值
     *
     * @param bossUUID Boss 的唯一 ID
     * @return 当前生命值，如果 Boss 不存在则返回 null
     */
    @Nullable
    Double getBossHealth(@NotNull UUID bossUUID);

    /**
     * 设置 Boss 生命值
     *
     * @param bossUUID Boss 的唯一 ID
     * @param health 新的生命值
     */
    void setBossHealth(@NotNull UUID bossUUID, double health);

    /**
     * 获取 Boss 最大生命值
     *
     * @param bossUUID Boss 的唯一 ID
     * @return 最大生命值，如果 Boss 不存在则返回 null
     */
    @Nullable
    Double getBossMaxHealth(@NotNull UUID bossUUID);

    /**
     * 对 Boss 造成伤害
     *
     * @param bossUUID Boss 的唯一 ID
     * @param damage 伤害量
     * @param damagerUUID 造成伤害的实体 UUID（通常为玩家）
     */
    void damageBoss(@NotNull UUID bossUUID, double damage, @NotNull UUID damagerUUID);

    /**
     * 设置 Boss 属性倍数（用于难度调整）
     *
     * @param bossUUID Boss 的唯一 ID
     * @param healthMultiplier 生命值倍数
     * @param damageMultiplier 伤害倍数
     * @param armorMultiplier 护甲倍数
     */
    void setBossMultipliers(@NotNull UUID bossUUID,
                           double healthMultiplier,
                           double damageMultiplier,
                           double armorMultiplier);
}
