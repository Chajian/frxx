package com.xiancore.systems.boss.event;

import com.xiancore.systems.boss.BossEventBus;
import com.xiancore.systems.boss.entity.BossEntity;

import java.util.UUID;

/**
 * Boss生成事件
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class BossSpawnedEvent extends BossEventBus.BossEvent {
    private final BossEntity boss;
    private final UUID bossUUID;
    private final String spawnPointId;

    public BossSpawnedEvent(BossEntity boss) {
        this(boss.getBossUUID(), boss, "unknown");
    }

    public BossSpawnedEvent(UUID bossUUID, BossEntity boss, String spawnPointId) {
        this.bossUUID = bossUUID;
        this.boss = boss;
        this.spawnPointId = spawnPointId;
    }

    public UUID getBossUUID() {
        return bossUUID;
    }

    public BossEntity getBoss() {
        return boss;
    }

    public String getSpawnPointId() {
        return spawnPointId;
    }
}
