package com.xiancore.listeners;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 奇遇触发监听器
 * 监听玩家活动以触发奇遇事件
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class FateTriggerListener implements Listener {

    private final XianCore plugin;

    // 玩家移动计数器（用于检测活跃度）
    private final Map<UUID, Integer> moveCounter = new HashMap<>();

    // 奇遇检查间隔
    private int checkInterval = 100;

    public FateTriggerListener(XianCore plugin) {
        this.plugin = plugin;
        // 从配置读取检查间隔
        this.checkInterval = plugin.getConfigManager().getConfig("config").getInt("fate.check-interval", 100);
    }

    /**
     * 玩家移动事件
     * 累积活跃度并定期触发奇遇检查
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 检查是否有实际移动（防止视角旋转触发）
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        try {
            // 增加移动计数
            int count = moveCounter.getOrDefault(uuid, 0) + 1;
            moveCounter.put(uuid, count);

            // 每移动 checkInterval 次检查一次奇遇
            if (count >= checkInterval) {
                moveCounter.put(uuid, 0);

                // 增加活跃灵气值
                PlayerData data = plugin.getDataManager().loadPlayerData(uuid);
                if (data != null) {
                    data.addActiveQi(1); // 每次检查增加1点活跃灵气
                    plugin.getDataManager().savePlayerData(data);

                    // 触发奇遇检查
                    plugin.getFateSystem().triggerChance(player, "MOVEMENT");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("处理玩家移动事件时出错: " + e.getMessage());
        }
    }

    /**
     * 实体死亡事件
     * 击杀怪物可触发奇遇
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // 必须是玩家击杀
        if (killer == null) {
            return;
        }

        try {
            PlayerData data = plugin.getDataManager().loadPlayerData(killer.getUniqueId());
            if (data == null) {
                return;
            }

            // 根据怪物类型给予不同的活跃灵气
            int activeQiGain = calculateActiveQiFromKill(entity);
            data.addActiveQi(activeQiGain);

            // Boss 击杀有更高的奇遇触发率
            boolean isBoss = isBossEntity(entity);
            if (isBoss) {
                data.addActiveQi(activeQiGain * 3); // Boss 额外奖励

                // Boss 击杀必定触发奇遇检查
                plugin.getFateSystem().triggerChance(killer, "BOSS_KILL");
            } else {
                // 普通怪物有概率触发奇遇检查
                if (Math.random() < 0.1) { // 10% 概率
                    plugin.getFateSystem().triggerChance(killer, "MONSTER_KILL");
                }
            }

            plugin.getDataManager().savePlayerData(data);

        } catch (Exception e) {
            plugin.getLogger().warning("处理实体死亡事件时出错: " + e.getMessage());
        }
    }

    /**
     * 根据击杀的实体计算活跃灵气获得量
     */
    private int calculateActiveQiFromKill(LivingEntity entity) {
        // 根据实体类型和生命值计算
        double maxHealth = entity.getMaxHealth();

        return switch (entity.getType()) {
            case ZOMBIE, SKELETON, SPIDER, CREEPER -> 2;
            case ENDERMAN, BLAZE, WITCH -> 5;
            case WITHER_SKELETON, PIGLIN_BRUTE -> 8;
            case ENDER_DRAGON, WITHER -> 50;
            default -> (int) Math.max(1, maxHealth / 10);
        };
    }

    /**
     * 判断是否是 Boss 实体
     */
    private boolean isBossEntity(LivingEntity entity) {
        // 检查是否是原版 Boss
        switch (entity.getType()) {
            case ENDER_DRAGON:
            case WITHER:
            case ELDER_GUARDIAN:
            case WARDEN:
                return true;
        }

        // 检查是否是 MythicMobs Boss
        if (plugin.getMythicIntegration() != null) {
            // 调用 MythicMobs API 检查是否是 Mythic Boss
            try {
                // 使用 MythicMobs 5.6.1 API 检查
                io.lumine.mythic.bukkit.MythicBukkit mythicBukkit = io.lumine.mythic.bukkit.MythicBukkit.inst();
                if (mythicBukkit != null && mythicBukkit.getMobManager() != null) {
                    // 检查实体是否是活跃的 Mythic Mob
                    if (mythicBukkit.getMobManager().isActiveMob(entity.getUniqueId())) {
                        if (plugin.isDebugMode()) {
                            plugin.getLogger().fine("[奇遇] 检测到 MythicMobs Boss: " + entity.getName());
                        }
                        return true;
                    }
                }
            } catch (Exception e) {
                // MythicMobs API 可能不可用或版本不兼容
                if (plugin.isDebugMode()) {
                    plugin.getLogger().warning("[奇遇] MythicMobs API 检查失败: " + e.getMessage());
                }
            }
        }

        // 检查实体名称是否包含 Boss 标识
        if (entity.getCustomName() != null) {
            String name = entity.getCustomName().toLowerCase();
            return name.contains("boss") || name.contains("首领") || name.contains("王");
        }

        return false;
    }

    /**
     * 清理离线玩家的计数器
     */
    public void cleanupOfflinePlayerCounters() {
        moveCounter.entrySet().removeIf(entry ->
            plugin.getServer().getPlayer(entry.getKey()) == null
        );
    }
}
