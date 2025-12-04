package com.xiancore.listeners;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.cultivation.QiRewardCalculator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 实体击杀监听器
 * 处理玩家击杀怪物获得修为的逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class EntityKillListener implements Listener {

    private final XianCore plugin;
    private final QiRewardCalculator rewardCalculator;

    public EntityKillListener(XianCore plugin) {
        this.plugin = plugin;
        this.rewardCalculator = new QiRewardCalculator(plugin);
    }

    /**
     * 处理实体死亡事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        // 检查功能是否启用
        if (!isQiFromKillsEnabled()) {
            return;
        }

        LivingEntity killedEntity = event.getEntity();
        Player killer = killedEntity.getKiller();

        // 必须是玩家击杀
        if (killer == null) {
            return;
        }

        // 不处理玩家PVP（可选）
        if (killedEntity instanceof Player && !isPvpQiEnabled()) {
            return;
        }

        // 计算修为奖励
        QiRewardCalculator.QiRewardResult result = rewardCalculator.calculateQiReward(killer, killedEntity);

        // 处理结果
        handleRewardResult(killer, killedEntity, result);
    }

    /**
     * 处理奖励结果
     */
    private void handleRewardResult(Player player, LivingEntity killedEntity, QiRewardCalculator.QiRewardResult result) {
        if (result.isSuccess()) {
            // 成功获得修为
            PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
            if (data != null) {
                data.addQi(result.getQiAmount());
                plugin.getDataManager().savePlayerData(data);
                
                // 发送奖励消息
                sendRewardMessage(player, killedEntity, result);
                
                // 调试日志
                if (isDebugEnabled()) {
                    plugin.getLogger().info(String.format("[击杀修为] 玩家 %s 击杀 %s(%s) 获得 %d 修为", 
                        player.getName(), 
                        getEntityDisplayName(killedEntity),
                        result.getMobRealm(),
                        result.getQiAmount()));
                }
            }
        } else {
            // 处理失败情况（冷却、限额等）
            handleFailedReward(player, result);
        }
    }

    /**
     * 发送奖励消息
     */
    private void sendRewardMessage(Player player, LivingEntity killedEntity, QiRewardCalculator.QiRewardResult result) {
        FileConfiguration rewardConfig = plugin.getConfigManager().getConfig("cultivation_rewards");
        if (rewardConfig == null) {
            return;
        }
        
        String entityName = getEntityDisplayName(killedEntity);
        String bonusInfo = result.getExtraInfo();
        
        if (bonusInfo != null && !bonusInfo.isEmpty()) {
            // 有加成信息的消息
            String message = rewardConfig.getString("messages.reward-with-bonus", 
                "&a[修为] &f击败了 &e{mob_name} &f获得 &b+{qi_amount}修为 &7({bonus_info})");
            
            message = message.replace("{mob_name}", entityName)
                           .replace("{qi_amount}", String.valueOf(result.getQiAmount()))
                           .replace("{bonus_info}", bonusInfo);
            
            player.sendMessage(message.replace("&", "§"));
        } else {
            // 普通奖励消息
            String message = rewardConfig.getString("messages.reward-message", 
                "&a[修为] &f击败了 &e{mob_name} &f获得 &b+{qi_amount}修为");
            
            message = message.replace("{mob_name}", entityName)
                           .replace("{qi_amount}", String.valueOf(result.getQiAmount()));
            
            player.sendMessage(message.replace("&", "§"));
        }
        
        // 调试计算过程（如果启用）
        if (isDebugEnabled() && isShowCalculationEnabled()) {
            String debugMessage = rewardConfig.getString("messages.debug-calculation",
                "&7[调试] 基础:{base} × 境界:{realm_gap} × 特殊:{special} = {final}");
            
            debugMessage = debugMessage.replace("{base}", String.valueOf(result.getBaseReward()))
                                     .replace("{realm_gap}", String.format("%.2f", result.getTotalMultiplier()))
                                     .replace("{special}", "1.00")
                                     .replace("{final}", String.valueOf(result.getQiAmount()));
            
            player.sendMessage(debugMessage.replace("&", "§"));
        }
    }

    /**
     * 处理获取奖励失败的情况
     */
    private void handleFailedReward(Player player, QiRewardCalculator.QiRewardResult result) {
        FileConfiguration rewardConfig = plugin.getConfigManager().getConfig("cultivation_rewards");
        if (rewardConfig == null) {
            return;
        }
        
        String reason = result.getReason();
        String extraInfo = result.getExtraInfo();
        
        if (reason.equals("冷却中")) {
            // 冷却消息
            Long remainingCooldown = result.getRemainingCooldown();
            if (remainingCooldown != null) {
                String message = rewardConfig.getString("messages.cooldown-message",
                    "&e[修为] &7击败{realm}怪物冷却中，剩余 {remaining}秒");
                
                String realm = result.getMobRealm() != null ? result.getMobRealm() : "相同境界";
                message = message.replace("{realm}", realm)
                               .replace("{remaining}", String.valueOf(remainingCooldown));
                
                player.sendMessage(message.replace("&", "§"));
            }
        } else if (reason.contains("每日") || reason.contains("上限")) {
            // 每日限额消息
            if (extraInfo != null) {
                String message = rewardConfig.getString("messages.daily-limit-message",
                    "&e[修为] &7今日击杀修为已达上限 {limit_info}");
                
                message = message.replace("{limit_info}", extraInfo);
                player.sendMessage(message.replace("&", "§"));
            }
        } else if (reason.contains("区域") || reason.contains("递减")) {
            // 区域惩罚消息
            String message = rewardConfig.getString("messages.area-penalty-message",
                "&e[修为] &7区域内重复击杀，奖励减半");
            
            player.sendMessage(message.replace("&", "§"));
        }
        
        // 调试信息
        if (isDebugEnabled()) {
            player.sendMessage("§7[调试] 获取修为失败: " + reason);
        }
    }

    /**
     * 获取实体显示名称
     */
    private String getEntityDisplayName(LivingEntity entity) {
        String customName = entity.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        
        // 获取MythicMobs显示名称
        if (plugin.getMythicIntegration() != null && plugin.getMythicIntegration().isEnabled()) {
            try {
                var mythicMobs = io.lumine.mythic.bukkit.MythicBukkit.inst();
                if (mythicMobs != null && mythicMobs.getMobManager() != null) {
                    var activeMobOptional = mythicMobs.getMobManager().getActiveMob(entity.getUniqueId());
                    if (activeMobOptional.isPresent()) {
                        var activeMob = activeMobOptional.get();
                        String displayName = activeMob.getDisplayName();
                        if (displayName != null && !displayName.isEmpty()) {
                            return displayName;
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略MythicMobs API异常
            }
        }
        
        // 返回默认类型名称
        return getEntityTypeName(entity.getType());
    }

    /**
     * 获取实体类型的中文名称
     */
    private String getEntityTypeName(org.bukkit.entity.EntityType type) {
        return switch (type) {
            case ZOMBIE -> "僵尸";
            case SKELETON -> "骷髅";
            case SPIDER -> "蜘蛛";
            case CREEPER -> "苦力怕";
            case ENDERMAN -> "末影人";
            case BLAZE -> "烈焰人";
            case WITCH -> "女巫";
            case WITHER_SKELETON -> "凋灵骷髅";
            case ELDER_GUARDIAN -> "远古守卫者";
            case WITHER -> "凋灵";
            case ENDER_DRAGON -> "末影龙";
            default -> type.name().toLowerCase().replace("_", " ");
        };
    }

    // ========== 配置检查方法 ==========

    private boolean isQiFromKillsEnabled() {
        return plugin.getConfig().getBoolean("qi-from-kills.enabled", true);
    }

    private boolean isPvpQiEnabled() {
        return plugin.getConfig().getBoolean("qi-from-kills.pvp-enabled", false);
    }

    private boolean isDebugEnabled() {
        FileConfiguration rewardConfig = plugin.getConfigManager().getConfig("cultivation_rewards");
        return rewardConfig != null && rewardConfig.getBoolean("debug.enabled", false);
    }

    private boolean isShowCalculationEnabled() {
        FileConfiguration rewardConfig = plugin.getConfigManager().getConfig("cultivation_rewards");
        return rewardConfig != null && rewardConfig.getBoolean("debug.show-calculation", false);
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        rewardCalculator.loadConfig();
    }
}








