package com.xiancore.systems.cultivation;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 修炼系统
 * 负责管理玩家的修炼、境界突破等
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class CultivationSystem {

    private final XianCore plugin;
    private boolean initialized = false;

    // 突破冷却时间管理 (玩家UUID -> 冷却结束时间)
    private final Map<UUID, Long> breakthroughCooldowns = new HashMap<>();

    // 正在修炼的玩家集合
    private final Set<UUID> cultivatingPlayers = new HashSet<>();

    // 突破冷却时间（秒）
    private static final long BREAKTHROUGH_COOLDOWN = 300; // 5分钟

    public CultivationSystem(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化修炼系统
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 注册修炼相关监听器
        // 启动修炼任务调度器
        startCultivationTask();

        initialized = true;
        plugin.getLogger().info("  §a✓ 修炼系统初始化完成");
    }

    /**
     * 开始打坐修炼
     */
    public void startCultivation(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }
        data.setCultivating(true);
        cultivatingPlayers.add(player.getUniqueId());
        plugin.getDataManager().savePlayerData(data);
        player.sendMessage("§a开始打坐修炼...");
    }

    /**
     * 暂停打坐修炼
     */
    public void pauseCultivation(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }
        data.setCultivating(false);
        cultivatingPlayers.remove(player.getUniqueId());
        plugin.getDataManager().savePlayerData(data);
        player.sendMessage("§e已暂停修炼。");
    }

    /**
     * 是否正在修炼
     */
    public boolean isCultivating(UUID playerId) {
        return cultivatingPlayers.contains(playerId);
    }

    /**
     * 每分钟修为增长（实时计算）
     */
    public long calculateQiGainPerMinute(Player player, PlayerData data) {
        double L = data.getSpiritualRoot();           // 灵根
        double P = data.getTechniqueAdaptation();     // 功法适配度
        double E = calculateEnvironmentQi(player.getLocation()); // 环境灵气
        double G = data.getComprehension();           // 悟性

        // 宗门灵脉加成（百分比）
        double sectSpeedBonus = 0.0;
        if (data.getSectId() != null) {
            try {
                double bonus = plugin.getSectSystem().getFacilityManager().getCultivationSpeedBonus(data.getSectId());
                sectSpeedBonus = Math.max(0.0, bonus) / 100.0;
            } catch (Exception ignored) {
            }
        }

        // 基础速率（可后续从配置读取）
        double base = 100.0;

        double rate = base * L * P * E * G * (1.0 + sectSpeedBonus);
        return Math.max(1L, Math.round(rate));
    }

    /**
     * 启动被动修炼定时任务（每分钟）
     */
    public void startCultivationTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (cultivatingPlayers.isEmpty()) {
                return;
            }
            // 遍历正在修炼的在线玩家
            for (Player online : Bukkit.getOnlinePlayers()) {
                UUID id = online.getUniqueId();
                if (!cultivatingPlayers.contains(id)) {
                    continue;
                }
                PlayerData data = plugin.getDataManager().loadPlayerData(id);
                if (data == null || !data.isCultivating()) {
                    cultivatingPlayers.remove(id);
                    continue;
                }

                long gain = calculateQiGainPerMinute(online, data);
                data.addQi(gain);
                plugin.getDataManager().savePlayerData(data);

                // 可选：记录修炼任务进度（按分钟）
                try {
                    if (plugin.getSectSystem() != null && plugin.getSectSystem().getProgressTracker() != null) {
                        plugin.getSectSystem().getProgressTracker().trackCultivation(id, 1);
                    }
                } catch (Exception ignored) {
                }
            }
        }, 20L * 60, 20L * 60); // 每分钟执行一次
    }

    /**
     * 尝试突破境界
     *
     * @param player 玩家
     * @return 是否成功
     */
    public boolean attemptBreakthrough(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        // 计算突破成功率
        double successRate = calculateBreakthroughRate(player, data);

        // 应用活跃灵气加成
        double activeQiBoost = plugin.getActiveQiManager().getBreakthroughBoost(player.getUniqueId());
        if (activeQiBoost > 0) {
            successRate += activeQiBoost;
            player.sendMessage("§a§l⚡ 活跃灵气加成: +5% 成功率!");
        }

        // 应用宗门灵脉加成
        if (data.getSectId() != null) {
            double sectBonus = plugin.getSectSystem().getFacilityManager()
                    .getCultivationSpeedBonus(data.getSectId());
            if (sectBonus > 0) {
                successRate += (sectBonus / 100.0); // 转换为小数
                player.sendMessage("§b§l✦ 宗门灵脉加成: +" + (int)sectBonus + "% 成功率!");
            }
        }

        // 随机判定
        boolean success = Math.random() < successRate;

        // 消耗活跃灵气加成（无论成功失败）
        if (activeQiBoost > 0) {
            plugin.getActiveQiManager().consumeBoost(player.getUniqueId(),
                    com.xiancore.systems.activeqi.ActiveQiManager.ActiveQiBoostType.BREAKTHROUGH);
        }

        // 记录突破尝试
        data.recordBreakthroughAttempt(success);

        if (success) {
            // 突破成功
            advanceRealm(data);

            // 突破成功增加大量活跃灵气
            data.addActiveQi(30);

            // 根据境界给予额外奖励
            int skillPointReward = getBreakthroughSkillPointReward(data.getRealm());
            if (skillPointReward > 0) {
                data.addSkillPoints(skillPointReward);
                player.sendMessage("§e获得 " + skillPointReward + " 功法点!");
            }

            player.sendMessage("§a§l⚡ 突破成功! 恭喜进入" + data.getFullRealmName() + "!");
            player.sendMessage("§7活跃灵气 +30");
        } else {
            // 突破失败
            applyBreakthroughFailurePenalty(player, data);

            // 突破失败也增加少量活跃灵气（鼓励尝试）
            data.addActiveQi(5);

            player.sendMessage("§c§l💀 突破失败! 修为受损...");
            player.sendMessage("§7活跃灵气 +5");
        }

        // 设置突破冷却（无论成功或失败都设置）
        setBreakthroughCooldown(player.getUniqueId());

        plugin.getDataManager().savePlayerData(data);
        return success;
    }

    /**
     * 计算突破成功率
     */
    private double calculateBreakthroughRate(Player player, PlayerData data) {
        // 根据架构文档中的公式计算
        // P_突破 = 1 - e^(-α * L * P * E * S * G / D)

        double L = data.getSpiritualRoot();           // 灵根
        double P = data.getTechniqueAdaptation();     // 功法适配度
        double E = calculateEnvironmentQi(player.getLocation());  // 环境灵气浓度
        double S = calculateResourceInput(player);    // 资源投入
        double G = data.getComprehension();           // 悟性
        double D = getRealmDifficulty(data.getRealm()); // 境界难度

        double alpha = 1.5; // 经验系数

        return 1 - Math.exp(-alpha * L * P * E * S * G / D);
    }

    /**
     * 计算环境灵气浓度
     * 根据生物群系来判断灵气浓度
     */
    private double calculateEnvironmentQi(org.bukkit.Location location) {
        org.bukkit.block.Biome biome = location.getBlock().getBiome();
        String biomeName = biome.name();

        // 灵气丰富的生物群系 (0.8-0.9)
        if (biomeName.contains("OCEAN") || biomeName.contains("JUNGLE") ||
            biomeName.contains("LUSH") || biomeName.contains("MUSHROOM") ||
            biomeName.contains("FOREST")) {
            return 0.8;
        }

        // 灵气普通的生物群系 (0.5)
        if (biomeName.contains("PLAINS") || biomeName.contains("TAIGA") ||
            biomeName.contains("RIVER") || biomeName.contains("BEACH") ||
            biomeName.contains("MEADOW") || biomeName.contains("SAVANNA")) {
            return 0.5;
        }

        // 灵气稀薄的生物群系 (0.2-0.4)
        if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS") ||
            biomeName.contains("THE_END")) {
            return 0.3;
        }

        // 地狱生物群系 (0.6)
        if (biomeName.contains("NETHER")) {
            return 0.6;
        }

        // 深暗生物群系 (0.4)
        if (biomeName.contains("DEEP_DARK")) {
            return 0.4;
        }

        // 其他默认为普通
        return 0.5;
    }

    /**
     * 计算资源投入系数
     * 根据玩家背包中的灵石数量计算
     */
    private double calculateResourceInput(Player player) {
        // 简化实现：根据玩家等级和背包来判断
        // 可以扩展为读取自定义NBT数据中的灵石数量

        // 默认基础投入
        double baseInput = 0.5;

        // 玩家等级越高，资源投入能力越强
        double levelBonus = Math.min(0.3, (player.getLevel() - 1) * 0.01);

        // 检查背包中的"灵石"物品（如果有的话）
        // 这里简化为根据玩家的饱食度和血量来估算
        double resourceBonus = 0.1 * (player.getHealth() / player.getMaxHealth());

        return baseInput + levelBonus + resourceBonus;
    }

    /**
     * 获取境界难度系数
     */
    private double getRealmDifficulty(String realm) {
        return switch (realm) {
            case "炼气期" -> 1.0;
            case "筑基期" -> 2.0;
            case "结丹期" -> 5.0;
            case "元婴期" -> 10.0;
            case "化神期" -> 20.0;
            default -> 1.0;
        };
    }

    /**
     * 提升境界
     */
    private void advanceRealm(PlayerData data) {
        int currentStage = data.getRealmStage();

        if (currentStage < 3) {
            // 小境界突破（初期→中期，中期→后期）
            data.setRealmStage(currentStage + 1);
            // 小境界突破增加5级
            data.addLevel(5);
        } else {
            // 大境界突破（后期→下一个境界初期）
            String nextRealm = getNextRealm(data.getRealm());
            if (nextRealm != null) {
                data.setRealm(nextRealm);
                data.setRealmStage(1);
                // 大境界突破增加15级
                data.addLevel(15);
            }
        }
    }

    /**
     * 获取下一个境界
     */
    private String getNextRealm(String currentRealm) {
        return switch (currentRealm) {
            case "炼气期" -> "筑基期";
            case "筑基期" -> "结丹期";
            case "结丹期" -> "元婴期";
            case "元婴期" -> "化神期";
            case "化神期" -> "炼虚期";
            case "炼虚期" -> "合体期";
            case "合体期" -> "大乘期";
            default -> null;
        };
    }

    /**
     * 检查玩家是否在突破冷却中
     */
    public boolean isOnBreakthroughCooldown(UUID playerId) {
        Long cooldownEnd = breakthroughCooldowns.get(playerId);
        if (cooldownEnd == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEnd) {
            // 冷却已结束，移除记录
            breakthroughCooldowns.remove(playerId);
            return false;
        }

        return true;
    }

    /**
     * 获取剩余突破冷却时间（秒）
     */
    public long getRemainingBreakthroughCooldown(UUID playerId) {
        Long cooldownEnd = breakthroughCooldowns.get(playerId);
        if (cooldownEnd == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEnd) {
            breakthroughCooldowns.remove(playerId);
            return 0;
        }

        return (cooldownEnd - currentTime) / 1000;
    }

    /**
     * 设置突破冷却
     */
    public void setBreakthroughCooldown(UUID playerId) {
        long cooldownEnd = System.currentTimeMillis() + (BREAKTHROUGH_COOLDOWN * 1000);
        breakthroughCooldowns.put(playerId, cooldownEnd);
    }

    /**
     * 获取突破成功的功法点奖励
     */
    private int getBreakthroughSkillPointReward(String realm) {
        return switch (realm) {
            case "炼气期" -> 0;
            case "筑基期" -> 5;
            case "结丹期" -> 10;
            case "元婴期" -> 15;
            case "化神期" -> 20;
            case "炼虚期" -> 30;
            case "合体期" -> 40;
            case "大乘期" -> 50;
            default -> 0;
        };
    }

    /**
     * 应用突破失败惩罚
     */
    private void applyBreakthroughFailurePenalty(Player player, PlayerData data) {
        // 修为倒退
        long qiLoss = data.getQi() / 10;  // 损失10%修为
        data.removeQi(qiLoss);

        // 应用 debuff 效果（虚弱和缓慢）
        int debuffDuration = 600; // 30秒 (600 ticks = 30 seconds)
        int debuffLevel = 1;

        // 虚弱效果（WEAKNESS）- 攻击力下降
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS,
            debuffDuration,
            debuffLevel
        ));

        // 缓慢效果（SLOW/SLOWNESS）- 移动速度下降
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOW,
            debuffDuration,
            debuffLevel
        ));

        // 失明效果（BLINDNESS）- 视线受损
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.BLINDNESS,
            300, // 15秒
            0
        ));

        // 向玩家输出惩罚信息
        player.sendMessage("§c你陷入了虚弱状态，30秒内无法进行剧烈活动...");
        player.sendMessage("§c修为损失: §6" + qiLoss + " 灵力");
    }
}
