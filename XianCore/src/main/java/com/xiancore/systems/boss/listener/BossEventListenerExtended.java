package com.xiancore.systems.boss.listener;

import com.xiancore.XianCore;
import com.xiancore.boss.damage.DamageTracker;
import com.xiancore.boss.reward.RewardDistributor;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.entity.BossEntity;
import com.xiancore.systems.boss.event.BossDespawnedEvent;
import com.xiancore.systems.boss.event.BossKilledEvent;
import com.xiancore.systems.boss.event.BossSpawnedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Boss事件监听器（扩展版本）- 处理Boss相关事件，包括奖励和伤害追踪
 *
 * 职责:
 * - 监听EntityDeathEvent处理Boss死亡
 * - 监听自定义BossSpawnedEvent处理Boss生成
 * - 监听自定义BossKilledEvent处理Boss击杀
 * - 监听自定义BossDespawnedEvent处理Boss消失
 * - 集成RewardDistributor分发奖励
 * - 集成DamageTracker追踪伤害
 * - 协调Manager和事件系统
 *
 * @author XianCore Team
 * @version 1.1
 * @since 2025-11-16
 */
public class BossEventListenerExtended implements Listener {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;
    private final RewardDistributor rewardDistributor;
    private final DamageTracker damageTracker;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param bossManager Boss刷新管理器
     * @param rewardDistributor 奖励分发系统
     * @param damageTracker 伤害追踪系统
     */
    public BossEventListenerExtended(XianCore plugin, BossRefreshManager bossManager,
                           RewardDistributor rewardDistributor, DamageTracker damageTracker) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.rewardDistributor = rewardDistributor;
        this.damageTracker = damageTracker;
    }

    /**
     * 监听Boss生成事件
     * 创建伤害追踪记录
     */
    @EventHandler
    public void onBossSpawned(BossSpawnedEvent event) {
        try {
            BossEntity boss = event.getBoss();
            UUID bossUuid = boss.getBossUUID();
            String bossType = boss.getMythicMobType();

            plugin.getLogger().info("§a✓ Boss已生成: " + bossType);
            plugin.getLogger().info("  - UUID: " + bossUuid);
            plugin.getLogger().info("  - 等级: " + boss.getTier());
            plugin.getLogger().info("  - 位置: " + boss.getSpawnLocation());

            // 创建伤害追踪记录
            damageTracker.createBossRecord(bossUuid, bossType);

        } catch (Exception e) {
            plugin.getLogger().warning("处理Boss生成事件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 监听Boss击杀事件
     * 分发奖励，保存伤害统计
     */
    @EventHandler
    public void onBossKilled(BossKilledEvent event) {
        try {
            BossEntity boss = event.getBoss();
            Player killer = event.getKiller();
            UUID bossUuid = boss.getBossUUID();
            String bossType = boss.getMythicMobType();
            int bossLevel = boss.getTier();

            String killerName = killer != null ? killer.getName() : "Unknown";

            plugin.getLogger().info("§c✓ Boss已击杀: " + bossType);
            plugin.getLogger().info("  - 击杀者: " + killerName);
            plugin.getLogger().info("  - 等级: " + bossLevel);

            // 结束伤害追踪
            DamageTracker.BossDamageRecord damageRecord = damageTracker.finishTracking(bossUuid);

            // 分发奖励
            if (damageRecord != null && !damageRecord.playerDamages.isEmpty()) {
                List<RewardDistributor.PlayerContribution> contributions =
                    convertDamageToContribution(damageRecord);

                Map<UUID, RewardDistributor.RewardResult> rewards =
                    rewardDistributor.distributeRewards(bossType, bossLevel, contributions);

                // 给玩家发送奖励并记录统计
                for (Map.Entry<UUID, RewardDistributor.RewardResult> entry : rewards.entrySet()) {
                    UUID playerId = entry.getKey();
                    RewardDistributor.RewardResult result = entry.getValue();

                    // 可以在这里添加具体的奖励发放逻辑
                    plugin.getLogger().info("  [奖励] " + result.playerName +
                                           " | 经验: " + String.format("%.1f", result.expReward) +
                                           " | 金币: " + String.format("%.1f", result.goldReward));

                    // 将奖励信息保存到数据库（如果有）
                    // dao.saveRewardRecord(result);
                }
            } else {
                plugin.getLogger().warning("⚠ 没有玩家伤害数据，无法分发奖励");
            }

            // 打印伤害统计
            if (damageRecord != null) {
                damageTracker.printDamageStatistics(bossUuid);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("处理Boss击杀事件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 监听Boss消失事件
     * 清除伤害追踪记录
     */
    @EventHandler
    public void onBossDespawned(BossDespawnedEvent event) {
        try {
            BossEntity boss = event.getBoss();
            UUID bossUuid = boss.getBossUUID();
            String bossType = boss.getMythicMobType();
            String reason = event.getReason();

            plugin.getLogger().info("§e✓ Boss已消失: " + bossType);
            plugin.getLogger().info("  - 原因: " + reason);
            plugin.getLogger().info("  - 等级: " + boss.getTier());

            // 删除伤害追踪记录
            damageTracker.deleteBossRecord(bossUuid);

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
     * 记录玩家对Boss的伤害
     * 这个方法应该在受伤害事件中调用
     */
    public void recordBossDamage(UUID bossId, Player player, double damageDealt) {
        try {
            damageTracker.recordDamage(bossId, player, damageDealt);
        } catch (Exception e) {
            plugin.getLogger().warning("记录Boss伤害失败: " + e.getMessage());
        }
    }

    /**
     * 获取Boss的伤害排名
     */
    public List<DamageTracker.DamageRecord> getBossDamageRankings(UUID bossId, int limit) {
        return damageTracker.getTopDamageDealer(bossId, limit);
    }

    /**
     * 获取玩家对Boss的伤害统计
     */
    public DamageTracker.DamageRecord getPlayerBossDamage(UUID bossId, UUID playerId) {
        return damageTracker.getPlayerDamage(bossId, playerId);
    }

    /**
     * 清除过期的伤害和奖励记录
     */
    public void cleanupExpiredRecords() {
        damageTracker.clearExpiredRecords();
        rewardDistributor.clearExpiredCooldowns(86400); // 24小时
        plugin.getLogger().info("✓ 已清除过期的记录");
    }

    /**
     * 将伤害记录转换为贡献数据
     */
    private List<RewardDistributor.PlayerContribution> convertDamageToContribution(
            DamageTracker.BossDamageRecord damageRecord) {
        return damageRecord.playerDamages.values().stream()
                .map(dmg -> new RewardDistributor.PlayerContribution(
                        dmg.playerId,
                        dmg.playerName,
                        dmg.damageDealt
                ))
                .collect(Collectors.toList());
    }

    /**
     * 注册事件监听器
     *
     * @param plugin 插件实例
     * @param bossManager Boss刷新管理器
     * @param rewardDistributor 奖励分发系统
     * @param damageTracker 伤害追踪系统
     */
    public static void register(XianCore plugin, BossRefreshManager bossManager,
                               RewardDistributor rewardDistributor, DamageTracker damageTracker) {
        BossEventListenerExtended listener = new BossEventListenerExtended(plugin, bossManager, rewardDistributor, damageTracker);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("§a✓ Boss事件监听器已注册（扩展版本）");
    }
}
