package com.xiancore.systems.boss.listener;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.entity.BossEntity;
import com.xiancore.systems.boss.event.BossDespawnedEvent;
import com.xiancore.systems.boss.event.BossKilledEvent;
import com.xiancore.systems.boss.event.BossSpawnedEvent;
import com.xiancore.systems.boss.announcement.BossAnnouncementManager;
import com.xiancore.systems.boss.lifecycle.BossLifecycleData;
import com.xiancore.boss.system.model.BossTier;
import com.xiancore.systems.boss.reward.BossRewardManager;
import com.xiancore.systems.boss.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Boss事件监听器 - 处理Boss相关事件
 *
 * 职责:
 * - 监听EntityDeathEvent处理Boss死亡
 * - 监听自定义BossSpawnedEvent处理Boss生成
 * - 监听自定义BossKilledEvent处理Boss击杀
 * - 监听自定义BossDespawnedEvent处理Boss消失
 * - 协调Manager和事件系统
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-16
 */
public class BossEventListener implements Listener {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param bossManager Boss刷新管理器
     */
    public BossEventListener(XianCore plugin, BossRefreshManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    /**
     * 监听Boss生成事件
     */
    public void onBossSpawned(BossSpawnedEvent event) {
        try {
            BossEntity boss = event.getBoss();
            plugin.getLogger().info("§a✓ Boss已生成: " + boss.getMythicMobType());
            plugin.getLogger().info("  - UUID: " + boss.getBossUUID());
            plugin.getLogger().info("  - 等级: " + boss.getTier());
            plugin.getLogger().info("  - 位置: " + boss.getSpawnLocation());

            // 发送公告
            BossAnnouncementManager announcementManager = bossManager.getAnnouncementManager();
            if (announcementManager != null) {
                BossTier bossTier = convertToBoTier(boss.getTier());
                BossLifecycleData data = createLifecycleData(boss);
                int nearbyPlayers = 0;
                if (boss.getSpawnLocation() != null && boss.getSpawnLocation().getWorld() != null) {
                    for (Player p : boss.getSpawnLocation().getWorld().getPlayers()) {
                        if (p.getLocation().distanceSquared(boss.getSpawnLocation()) <= 100 * 100) {
                            nearbyPlayers++;
                        }
                    }
                }
                announcementManager.announceBossSpawn(data, boss.getSpawnLocation(), nearbyPlayers);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("处理Boss生成事件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 监听Boss击杀事件
     */
    public void onBossKilled(BossKilledEvent event) {
        try {
            BossEntity boss = event.getBoss();
            Player killer = event.getKiller();
            String killerName = killer != null ? killer.getName() : "Unknown";

            plugin.getLogger().info("§c✓ Boss已击杀: " + boss.getMythicMobType());
            plugin.getLogger().info("  - 击杀者: " + killerName);
            plugin.getLogger().info("  - 等级: " + boss.getTier());

            // 发送公告
            BossAnnouncementManager announcementManager = bossManager.getAnnouncementManager();
            if (announcementManager != null && killer != null) {
                BossLifecycleData data = createLifecycleData(boss);
                long duration = System.currentTimeMillis() - boss.getSpawnTime();
                announcementManager.announceBossKilled(
                    data,
                    killer.getUniqueId(),
                    killer.getName(),
                    100, // 品质分数 - 待实现
                    "S", // 品质等级 - 待实现
                    50.0 * boss.getTier(), // 经验奖励
                    duration,
                    0 // 死亡人数 - 待实现
                );
            }

            distributeRewards(boss, killer);
        } catch (Exception e) {
            plugin.getLogger().warning("处理Boss击杀事件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void distributeRewards(BossEntity boss, Player killer) {
        try {
            Set<UUID> participants = boss.getParticipants();
            if (participants.isEmpty()) {
                plugin.getLogger().info("  §7没有玩家参与击杀，不分配奖励");
                return;
            }

            // 获取奖励管理器
            BossRewardManager rewardManager = bossManager.getRewardManager();
            if (rewardManager == null || !rewardManager.isEnableRewards()) {
                plugin.getLogger().warning("  §e奖励系统未启用，使用默认奖励");
                distributeDefaultRewards(boss, killer);
                return;
            }

            int tier = boss.getTier();
            List<UUID> topDamagers = boss.getTopDamagers(rewardManager.getMaxRewardRanks());
            int totalParticipants = participants.size();

            plugin.getLogger().info("  §e=== 奖励分发 ===");
            plugin.getLogger().info("  §7参与玩家: " + totalParticipants + " | 前" + topDamagers.size() + "名获得奖励");

            int rank = 1;
            for (UUID playerUUID : topDamagers) {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player == null || !player.isOnline()) {
                    rank++;
                    continue;
                }

                double damagePercent = boss.getPlayerDamagePercentage(playerUUID);

                // 使用奖励管理器发放奖励
                List<Reward> rewards = rewardManager.giveRewards(player, tier, rank, damagePercent);

                // 向玩家发送奖励信息
                String rankSymbol = getRankSymbol(rank);
                player.sendMessage("§6§l[Boss击杀奖励]");
                player.sendMessage("§e" + rankSymbol + " 排名: §f第" + rank + "名 §7(伤害: " + String.format("%.1f%%", damagePercent * 100) + ")");
                
                for (Reward reward : rewards) {
                    player.sendMessage(rewardManager.formatReward(reward));
                }

                plugin.getLogger().info("  §a" + rank + ". " + player.getName() + " - 获得" + rewards.size() + "种奖励");

                rank++;
            }

            plugin.getLogger().info("  §e================");
        } catch (Exception e) {
            plugin.getLogger().warning("分发奖励失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 分发默认奖励（奖励系统未启用时的备用方案）
     */
    private void distributeDefaultRewards(BossEntity boss, Player killer) {
        try {
            int tier = boss.getTier();
            double baseExp = 50.0 * tier;

            List<UUID> topDamagers = boss.getTopDamagers(10);

            int rank = 1;
            for (UUID playerUUID : topDamagers) {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player == null || !player.isOnline()) {
                    continue;
                }

                double damagePercent = boss.getPlayerDamagePercentage(playerUUID);
                double rewardMultiplier = getRankMultiplier(rank);

                int expReward = (int) (baseExp * rewardMultiplier * (0.5 + damagePercent));

                player.giveExp(expReward);

                String rankSymbol = getRankSymbol(rank);
                player.sendMessage("§6§l[Boss击杀奖励]");
                player.sendMessage("§e" + rankSymbol + " 排名: §f第" + rank + "名 §7(伤害: " + String.format("%.1f%%", damagePercent * 100) + ")");
                player.sendMessage("§a✔ 经验: §f+" + expReward);

                rank++;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("分发默认奖励失败: " + e.getMessage());
        }
    }

    private double getRankMultiplier(int rank) {
        return switch (rank) {
            case 1 -> 2.0;
            case 2 -> 1.5;
            case 3 -> 1.2;
            case 4, 5 -> 1.0;
            case 6, 7, 8 -> 0.8;
            default -> 0.5;
        };
    }

    private String getRankSymbol(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "§7⭐";
        };
    }

    private com.xiancore.boss.system.model.BossTier convertToBoTier(int tier) {
        return com.xiancore.boss.system.model.BossTier.fromLevel(tier);
    }

    private BossLifecycleData createLifecycleData(BossEntity boss) {
        BossLifecycleData data = new BossLifecycleData();
        data.setBossUUID(boss.getBossUUID());
        data.setBossType(boss.getMythicMobType());
        data.setBossTier(convertToBoTier(boss.getTier()));
        return data;
    }

    /**
     * 监听Boss消失事件
     */
    public void onBossDespawned(BossDespawnedEvent event) {
        try {
            BossEntity boss = event.getBoss();
            String reason = event.getReason();

            plugin.getLogger().info("§e✓ Boss已消失: " + boss.getMythicMobType());
            plugin.getLogger().info("  - 原因: " + reason);
            plugin.getLogger().info("  - 等级: " + boss.getTier());
        } catch (Exception e) {
            plugin.getLogger().warning("处理Boss消失事件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 监听玩家击杀Boss (Bukkit原生事件)
     * 当实体死亡时触发，检查是否为Boss
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        try {
            LivingEntity deadEntity = event.getEntity();

            // 获取击杀者 (如果有)
            if (deadEntity.getKiller() != null) {
                // 检查是否为Boss
                BossEntity boss = bossManager.getBossEntityByMythicMob(deadEntity);
                if (boss != null) {
                    // 调用onBossKilled回调
                    bossManager.onBossKilled(boss, deadEntity.getKiller());
                }
            } else {
                // 没有击杀者（自然死亡、摔死等）
                BossEntity boss = bossManager.getBossEntityByMythicMob(deadEntity);
                if (boss != null) {
                    // 调用onBossDespawned回调
                    bossManager.onBossDespawned(boss);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("处理实体死亡事件失败: " + e.getMessage());
            // 不打印堆栈跟踪，因为这个事件可能频繁触发
        }
    }

    /**
     * 注册事件监听器
     *
     * @param plugin 插件实例
     */
    public static void register(XianCore plugin, BossRefreshManager bossManager) {
        BossEventListener listener = new BossEventListener(plugin, bossManager);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        bossManager.getEventBus().subscribe(BossSpawnedEvent.class, listener::onBossSpawned);
        bossManager.getEventBus().subscribe(BossKilledEvent.class, listener::onBossKilled);
        bossManager.getEventBus().subscribe(BossDespawnedEvent.class, listener::onBossDespawned);
        plugin.getLogger().info("§a✓ Boss事件监听器已注册");
    }
}
