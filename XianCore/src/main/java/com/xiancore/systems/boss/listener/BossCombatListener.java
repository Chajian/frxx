package com.xiancore.systems.boss.listener;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.damage.DamageStatisticsManager;
import com.xiancore.systems.boss.entity.BossEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class BossCombatListener implements Listener {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;
    private final DamageStatisticsManager damageManager;

    public BossCombatListener(XianCore plugin, BossRefreshManager bossManager, DamageStatisticsManager damageManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.damageManager = damageManager;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity victim = (LivingEntity) event.getEntity();
        BossEntity boss = bossManager.getBossEntityByMythicMob(victim);
        if (boss == null) {
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            ProjectileSource src = projectile.getShooter();
            if (src instanceof Player) {
                attacker = (Player) src;
            }
        }

        if (attacker == null) {
            return;
        }

        double damage = event.getFinalDamage();
        if (damage <= 0) {
            return;
        }

        UUID bossUUID = boss.getBossUUID();
        damageManager.recordDamage(bossUUID, attacker, damage);
    }
}
