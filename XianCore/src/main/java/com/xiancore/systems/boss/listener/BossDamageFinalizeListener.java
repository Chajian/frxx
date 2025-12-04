package com.xiancore.systems.boss.listener;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.damage.DamageStatisticsManager;
import com.xiancore.systems.boss.entity.BossEntity;
import com.xiancore.systems.boss.event.BossDespawnedEvent;
import com.xiancore.systems.boss.event.BossKilledEvent;
import com.xiancore.systems.boss.BossRefreshManager;
import org.bukkit.event.Listener;

public class BossDamageFinalizeListener implements Listener {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;
    private final DamageStatisticsManager damageManager;

    public BossDamageFinalizeListener(XianCore plugin, BossRefreshManager bossManager, DamageStatisticsManager damageManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.damageManager = damageManager;

        // 订阅内部事件总线
        bossManager.getEventBus().subscribe(BossKilledEvent.class, this::onBossKilled);
        bossManager.getEventBus().subscribe(BossDespawnedEvent.class, this::onBossDespawned);
    }

    public void onBossKilled(BossKilledEvent event) {
        BossEntity boss = event.getBoss();
        if (boss == null) return;
        damageManager.finalizeBossDamage(boss.getBossUUID());
    }

    public void onBossDespawned(BossDespawnedEvent event) {
        BossEntity boss = event.getBoss();
        if (boss == null) return;
        damageManager.finalizeBossDamage(boss.getBossUUID());
    }
}
