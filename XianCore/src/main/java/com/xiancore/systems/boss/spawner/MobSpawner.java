package com.xiancore.systems.boss.spawner;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * 怪物生成器接口
 * 解耦 Boss 系统与具体怪物插件的依赖
 *
 * <p>设计目的：
 * <ul>
 *   <li>遵循依赖倒置原则 (DIP)，上层模块依赖抽象而非具体实现</li>
 *   <li>支持未来扩展其他怪物插件（如 ModelEngine、Citizens 等）</li>
 *   <li>便于单元测试时提供 Mock 实现</li>
 * </ul>
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface MobSpawner {

    /**
     * 生成怪物
     *
     * @param mobType  怪物类型标识（如 MythicMobs 的 mob id）
     * @param location 生成位置
     * @return 生成的 LivingEntity，如果失败返回 null
     */
    LivingEntity spawn(String mobType, Location location);

    /**
     * 检查怪物类型是否存在/支持
     *
     * @param mobType 怪物类型标识
     * @return true 如果该类型存在且可生成
     */
    boolean hasMobType(String mobType);

    /**
     * 检查生成器是否可用
     * 用于判断依赖的插件是否已加载和初始化
     *
     * @return true 如果生成器可用
     */
    boolean isAvailable();

    /**
     * 获取生成器名称
     * 用于日志和调试
     *
     * @return 生成器名称（如 "MythicMobs", "Vanilla" 等）
     */
    String getName();
}
