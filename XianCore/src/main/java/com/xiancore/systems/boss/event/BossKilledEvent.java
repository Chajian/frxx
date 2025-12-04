package com.xiancore.systems.boss.event;

import com.xiancore.systems.boss.BossEventBus;
import com.xiancore.systems.boss.entity.BossEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Boss击杀事件
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class BossKilledEvent extends BossEventBus.BossEvent {
    private final BossEntity boss;
    private final Player killer;
    private final double totalDamage;
    private final UUID bossUUID;

    public BossKilledEvent(BossEntity boss, Player killer) {
        this(boss.getBossUUID(), boss, killer, boss.getTotalDamage());
    }

    public BossKilledEvent(UUID bossUUID, BossEntity boss, Player killer, double totalDamage) {
        this.bossUUID = bossUUID;
        this.boss = boss;
        this.killer = killer;
        this.totalDamage = totalDamage;
    }

    public UUID getBossUUID() {
        return bossUUID;
    }

    public BossEntity getBoss() {
        return boss;
    }

    public Player getKiller() {
        return killer;
    }

    public double getTotalDamage() {
        return totalDamage;
    }
}
