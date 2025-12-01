package com.xiancore.boss.system.api;

import com.xiancore.boss.system.damage.DamageRecord;
import com.xiancore.boss.system.damage.DamageStatisticsManager;
import com.xiancore.boss.system.lifecycle.BossLifecycleData;
import com.xiancore.boss.system.lifecycle.BossLifecycleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boss 系统 API 门面接口
 * 所有对 Boss 系统的操作都通过此接口进行
 *
 * 新模块的公开 API，不暴露内部实现细节
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface BossSystemAPI {

    // ==================== 初始化 ====================

    /**
     * 初始化 Boss 系统
     * 必须在使用其他方法前调用
     *
     * @param eventAdapter 事件适配器（由 XianCore 实现）
     * @param entityAdapter 实体适配器（由 XianCore 实现）
     */
    void initialize(@NotNull BossEventAdapter eventAdapter,
                    @NotNull BossEntityAdapter entityAdapter);

    /**
     * 设置伤害数据持久化接口
     *
     * @param database 伤害数据库实现
     */
    void setDamageDatabase(@NotNull DamageDataAdapter database);

    /**
     * 设置位置选择器
     *
     * @param selector 位置选择实现
     */
    void setLocationSelector(@NotNull LocationSelectionAdapter selector);

    /**
     * 设置公告处理器
     *
     * @param announcer 公告适配器实现
     */
    void setAnnouncementHandler(@NotNull AnnouncementAdapter announcer);

    // ==================== 管理器访问 ====================

    /**
     * 获取生命周期管理器
     * 用于管理 Boss 的生命周期状态
     *
     * @return 生命周期管理器
     */
    @NotNull
    BossLifecycleManager getLifecycleManager();

    /**
     * 获取伤害统计管理器
     * 用于管理和查询伤害统计
     *
     * @return 伤害统计管理器
     */
    @NotNull
    DamageStatisticsManager getDamageManager();

    // ==================== 数据查询 ====================

    /**
     * 获取 Boss 的生命周期数据
     *
     * @param bossUUID Boss UUID
     * @return 生命周期数据，如果不存在则返回 null
     */
    @Nullable
    BossLifecycleData getLifecycleData(@NotNull UUID bossUUID);

    /**
     * 获取 Boss 的伤害记录
     *
     * @param bossUUID Boss UUID
     * @return 伤害记录，如果不存在则返回 null
     */
    @Nullable
    DamageRecord getDamageRecord(@NotNull UUID bossUUID);

    /**
     * 获取伤害历史记录
     *
     * @return 历史伤害记录列表
     */
    @NotNull
    List<DamageRecord> getDamageHistory();

    /**
     * 获取伤害历史记录（分页）
     *
     * @param page 页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 历史伤害记录列表
     */
    @NotNull
    List<DamageRecord> getDamageHistory(int page, int pageSize);

    // ==================== 计算 ====================

    /**
     * 计算 Boss 难度
     *
     * @param bossUUID Boss UUID
     * @param participantCount 参与人数
     * @param playerPower 玩家平均战力
     * @param bossPower Boss 战力
     * @return 难度系数（1.0 = 标准难度）
     */
    double calculateDifficulty(@NotNull UUID bossUUID,
                              int participantCount,
                              double playerPower,
                              double bossPower);

    /**
     * 计算击杀 Boss 的奖励
     *
     * @param bossUUID Boss UUID
     * @param killerUUID 最后击杀者的 UUID
     * @param durationSeconds Boss 持续存在的时间（秒）
     * @param deathCount 玩家死亡次数
     * @return 奖励数据映射
     */
    @NotNull
    Map<String, Object> calculateRewards(@NotNull UUID bossUUID,
                                        @NotNull UUID killerUUID,
                                        long durationSeconds,
                                        int deathCount);

    /**
     * 计算 Boss 属性倍数
     *
     * @param baseTier 基础等级
     * @param participantCount 参与人数
     * @return 属性倍数
     */
    double calculateAttributeMultiplier(int baseTier, int participantCount);

    // ==================== 状态检查 ====================

    /**
     * 检查 Boss 系统是否已初始化
     *
     * @return 是否已初始化
     */
    boolean isInitialized();

    /**
     * 检查 Boss 系统是否启用
     *
     * @return 是否启用
     */
    boolean isEnabled();

    /**
     * 获取系统版本
     *
     * @return 版本字符串
     */
    @NotNull
    String getVersion();

    /**
     * 获取系统统计信息
     *
     * @return 统计信息 Map
     */
    @NotNull
    Map<String, Object> getStatistics();
}
