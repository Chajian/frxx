package com.xiancore.systems.boss.spawner;

import com.xiancore.integration.mythic.MythicIntegration;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * MythicMobs 怪物生成器实现
 * 封装 MythicIntegration 的生成逻辑，实现 MobSpawner 接口
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class MythicMobsSpawner implements MobSpawner {

    private static final String NAME = "MythicMobs";

    private final MythicIntegration mythicIntegration;

    public MythicMobsSpawner(MythicIntegration mythicIntegration) {
        this.mythicIntegration = mythicIntegration;
    }

    @Override
    public LivingEntity spawn(String mobType, Location location) {
        if (!isAvailable()) {
            return null;
        }
        return mythicIntegration.spawnMythicMob(mobType, location);
    }

    @Override
    public boolean hasMobType(String mobType) {
        if (!isAvailable()) {
            return false;
        }
        return mythicIntegration.hasMythicMobType(mobType);
    }

    @Override
    public boolean isAvailable() {
        return mythicIntegration != null
                && mythicIntegration.isInitialized()
                && mythicIntegration.isEnabled();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
