package com.xiancore.systems.boss.location;

import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Location;

import java.util.List;

/**
 * 位置选择策略接口
 * 定义 Boss 生成位置的选择算法
 * <p>
 * 使用策略模式，不同的生成模式（固定位置、玩家附近、区域随机）
 * 通过实现此接口来提供不同的位置选择逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public interface LocationSelectionStrategy {

    /**
     * 获取策略名称（用于配置匹配）
     *
     * @return 策略名称，如 "fixed", "player-nearby", "region"
     */
    String getName();

    /**
     * 生成候选位置列表
     *
     * @param point      刷新点配置
     * @param maxCandidates 最大候选位置数量
     * @return 候选位置列表
     */
    List<Location> generateCandidates(BossSpawnPoint point, int maxCandidates);

    /**
     * 检查此策略是否适用于给定的刷新点
     *
     * @param point 刷新点配置
     * @return 是否适用
     */
    default boolean isApplicable(BossSpawnPoint point) {
        return getName().equalsIgnoreCase(point.getSpawnMode());
    }
}
