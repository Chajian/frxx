package com.xiancore.systems.boss.event;

import com.xiancore.systems.boss.BossEventBus;
import com.xiancore.systems.boss.entity.BossEntity;

import java.util.UUID;

/**
 * Boss消失事件
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class BossDespawnedEvent extends BossEventBus.BossEvent {
    private final BossEntity boss;
    private final String reason;  // 消失原因: despawned, killed, timeout等
    private final UUID bossUUID;

    public BossDespawnedEvent(BossEntity boss, String reason) {
        this(boss.getBossUUID(), boss, reason);
    }

    public BossDespawnedEvent(UUID bossUUID, BossEntity boss, String reason) {
        this.bossUUID = bossUUID;
        this.boss = boss;
        this.reason = reason;
    }

    public UUID getBossUUID() {
        return bossUUID;
    }

    public BossEntity getBoss() {
        return boss;
    }

    public String getReason() {
        return reason;
    }
}
