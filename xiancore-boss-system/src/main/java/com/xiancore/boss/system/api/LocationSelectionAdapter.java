package com.xiancore.boss.system.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 位置选择适配器
 * 用于选择 Boss 生成的位置
 *
 * XianCore 需要实现此接口以提供 Boss 生成地点
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface LocationSelectionAdapter {

    /**
     * 选择 Boss 生成位置
     * 应返回一个包含以下信息的 Map：
     * - "world": 世界名称 (String)
     * - "x": X 坐标 (Double)
     * - "y": Y 坐标 (Double)
     * - "z": Z 坐标 (Double)
     * - "tier": Boss 等级 (Integer)
     *
     * @param spawnType 生成类型（如 "normal", "event", "raid"）
     * @param difficultySuggestion 建议的难度（供参考）
     * @return 包含位置信息的 Map，如果没有合适的位置则返回 null
     */
    @Nullable
    Map<String, Object> selectSpawnLocation(@NotNull String spawnType,
                                           double difficultySuggestion);

    /**
     * 检查位置是否安全可用
     *
     * @param world 世界名称
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @return 是否安全可用
     */
    boolean isLocationSafe(@NotNull String world, double x, double y, double z);

    /**
     * 获取指定位置附近的玩家数量
     *
     * @param world 世界名称
     * @param x X 坐标
     * @param y Y 坐标
     * @param z Z 坐标
     * @param radius 检查半径（方块）
     * @return 玩家数量
     */
    int getNearbyPlayerCount(@NotNull String world, double x, double y, double z, double radius);

    /**
     * 获取推荐的生成点信息
     * 返回系统配置中定义的生成点列表
     *
     * @return 生成点信息列表，每个元素是一个包含位置信息的 Map
     */
    @NotNull
    java.util.List<Map<String, Object>> getSpawnPoints();
}
