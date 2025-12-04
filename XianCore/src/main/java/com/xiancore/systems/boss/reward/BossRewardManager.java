package com.xiancore.systems.boss.reward;

import com.xiancore.XianCore;
import com.xiancore.integration.mythic.MythicIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Boss奖励管理器
 * 负责加载配置、管理奖励池、发放奖励
 * 
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
public class BossRewardManager {
    
    private final XianCore plugin;
    private final MythicIntegration mythicIntegration;
    private final Logger logger;
    
    // 奖励池：tier -> rank -> RewardPool
    private final Map<Integer, Map<Integer, RewardPool>> rewardPools;
    
    // 默认奖励配置
    private boolean enableRewards = true;
    private boolean enableMoneyRewards = true;
    private boolean broadcastRewards = true;
    private int maxRewardRanks = 10;
    
    public BossRewardManager(XianCore plugin, MythicIntegration mythicIntegration) {
        this.plugin = plugin;
        this.mythicIntegration = mythicIntegration;
        this.logger = plugin.getLogger();
        this.rewardPools = new HashMap<>();
    }
    
    /**
     * 初始化奖励系统
     */
    public void initialize() {
        logger.info("§e初始化Boss奖励系统...");
        
        // 初始化Vault集成
        if (VaultIntegration.setup()) {
            logger.info("§a✓ Vault经济集成已启用");
        } else {
            logger.warning("§eVault未启用，金钱奖励将不可用");
            enableMoneyRewards = false;
        }
        
        // 加载配置
        loadConfig();
        
        logger.info("§a✓ Boss奖励系统初始化完成");
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "boss-rewards.yml");
        
        // 如果配置文件不存在，创建默认配置
        if (!configFile.exists()) {
            logger.info("boss-rewards.yml 不存在，正在生成默认配置...");
            plugin.saveResource("boss-rewards.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // 加载全局设置
        enableRewards = config.getBoolean("settings.enable-rewards", true);
        enableMoneyRewards = config.getBoolean("settings.enable-money-rewards", true);
        broadcastRewards = config.getBoolean("settings.broadcast-rewards", true);
        maxRewardRanks = config.getInt("settings.max-reward-ranks", 10);
        
        // 清空现有奖励池
        rewardPools.clear();
        
        // 加载各等级Boss的奖励配置
        ConfigurationSection tiersSection = config.getConfigurationSection("tiers");
        if (tiersSection != null) {
            for (String tierKey : tiersSection.getKeys(false)) {
                int tier = Integer.parseInt(tierKey);
                loadTierRewards(tier, tiersSection.getConfigurationSection(tierKey));
            }
        }
        
        logger.info("§a✓ 奖励配置加载完成 (" + rewardPools.size() + " 个等级)");
    }
    
    /**
     * 加载某个等级的奖励配置
     */
    private void loadTierRewards(int tier, ConfigurationSection tierSection) {
        if (tierSection == null) return;
        
        Map<Integer, RewardPool> rankPools = new HashMap<>();
        
        // 加载排名奖励
        ConfigurationSection ranksSection = tierSection.getConfigurationSection("ranks");
        if (ranksSection != null) {
            for (String rankKey : ranksSection.getKeys(false)) {
                int rank = Integer.parseInt(rankKey);
                ConfigurationSection rankSection = ranksSection.getConfigurationSection(rankKey);
                
                if (rankSection != null) {
                    double multiplier = rankSection.getDouble("multiplier", 1.0);
                    RewardPool pool = new RewardPool("Tier" + tier + "-Rank" + rank, multiplier);
                    
                    // 加载奖励列表
                    loadRewardList(pool, rankSection);
                    
                    rankPools.put(rank, pool);
                }
            }
        }
        
        rewardPools.put(tier, rankPools);
    }
    
    /**
     * 加载奖励列表
     */
    private void loadRewardList(RewardPool pool, ConfigurationSection section) {
        ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection == null) return;
        
        for (String rewardKey : rewardsSection.getKeys(false)) {
            ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(rewardKey);
            if (rewardSection == null) continue;
            
            String type = rewardSection.getString("type", "experience");
            double chance = rewardSection.getDouble("chance", 1.0);
            String displayName = rewardSection.getString("display-name", "");
            
            Reward reward = parseReward(type, rewardSection, chance, displayName);
            if (reward != null) {
                pool.addReward(reward);
            }
        }
    }
    
    /**
     * 解析单个奖励
     */
    private Reward parseReward(String typeString, ConfigurationSection section, double chance, String displayName) {
        RewardType type = RewardType.fromConfigKey(typeString);
        if (type == null) {
            logger.warning("未知的奖励类型: " + typeString);
            return null;
        }
        
        switch (type) {
            case EXPERIENCE:
                int exp = section.getInt("amount", 0);
                return new Reward.Builder()
                    .type(RewardType.EXPERIENCE)
                    .value(exp)
                    .chance(chance)
                    .displayName(displayName.isEmpty() ? "经验 +" + exp : displayName)
                    .build();
                    
            case MONEY:
                if (!enableMoneyRewards || !VaultIntegration.isEnabled()) {
                    return null;
                }
                double money = section.getDouble("amount", 0.0);
                return new Reward.Builder()
                    .type(RewardType.MONEY)
                    .value(money)
                    .chance(chance)
                    .displayName(displayName.isEmpty() ? "金钱 +" + money : displayName)
                    .build();
                    
            case ITEM:
                String materialName = section.getString("material", "DIAMOND");
                int amount = section.getInt("amount", 1);
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    ItemStack item = new ItemStack(material, amount);
                    return new Reward.Builder()
                        .type(RewardType.ITEM)
                        .value(item)
                        .chance(chance)
                        .displayName(displayName.isEmpty() ? material.name() + " x" + amount : displayName)
                        .build();
                } catch (IllegalArgumentException e) {
                    logger.warning("无效的材料类型: " + materialName);
                    return null;
                }
                
            case MYTHIC_ITEM:
                String itemId = section.getString("item-id", "");
                int mythicAmount = section.getInt("amount", 1);
                return new Reward.Builder()
                    .type(RewardType.MYTHIC_ITEM)
                    .value(itemId + ":" + mythicAmount)
                    .chance(chance)
                    .displayName(displayName.isEmpty() ? "神话物品 x" + mythicAmount : displayName)
                    .build();
                    
            case COMMAND:
                String command = section.getString("command", "");
                return new Reward.Builder()
                    .type(RewardType.COMMAND)
                    .value(command)
                    .chance(chance)
                    .displayName(displayName.isEmpty() ? "特殊奖励" : displayName)
                    .build();
                    
            default:
                return null;
        }
    }
    
    /**
     * 给予玩家奖励
     * 
     * @param player 玩家
     * @param tier Boss等级
     * @param rank 排名
     * @param damagePercent 伤害占比
     * @return 发放的奖励列表
     */
    public List<Reward> giveRewards(Player player, int tier, int rank, double damagePercent) {
        if (!enableRewards || player == null || !player.isOnline()) {
            return Collections.emptyList();
        }
        
        // 获取奖励池
        RewardPool pool = getRewardPool(tier, rank);
        if (pool == null || pool.isEmpty()) {
            // 使用默认奖励
            return giveDefaultRewards(player, tier, rank, damagePercent);
        }
        
        // 获取缩放后的奖励
        List<Reward> rewards = pool.getScaledRewards(damagePercent);
        
        // 发放奖励
        List<Reward> givenRewards = new ArrayList<>();
        for (Reward reward : rewards) {
            if (giveReward(player, reward)) {
                givenRewards.add(reward);
            }
        }
        
        return givenRewards;
    }
    
    /**
     * 发放单个奖励
     */
    private boolean giveReward(Player player, Reward reward) {
        switch (reward.getType()) {
            case EXPERIENCE:
                player.giveExp(reward.getIntValue());
                return true;
                
            case MONEY:
                if (VaultIntegration.isEnabled()) {
                    return VaultIntegration.giveMoney(player, reward.getDoubleValue());
                }
                return false;
                
            case ITEM:
                ItemStack item = reward.getItemStack();
                if (item != null) {
                    player.getInventory().addItem(item);
                    return true;
                }
                return false;
                
            case MYTHIC_ITEM:
                return giveMythicItem(player, reward.getStringValue());
                
            case COMMAND:
                return executeRewardCommand(player, reward.getStringValue());
                
            default:
                return false;
        }
    }
    
    /**
     * 给予MythicMobs物品
     */
    private boolean giveMythicItem(Player player, String itemData) {
        try {
            String[] parts = itemData.split(":");
            String itemId = parts[0];
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            
            if (mythicIntegration != null && mythicIntegration.isEnabled()) {
                // 使用MythicMobs API给予物品
                var mythicMobs = mythicIntegration.getMythicMobs();
                if (mythicMobs != null && mythicMobs.getItemManager() != null) {
                    var itemStack = mythicMobs.getItemManager().getItemStack(itemId);
                    if (itemStack != null) {
                        itemStack.setAmount(amount);
                        player.getInventory().addItem(itemStack);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("给予神话物品失败: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * 执行奖励命令
     */
    private boolean executeRewardCommand(Player player, String command) {
        try {
            String cmd = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return true;
        } catch (Exception e) {
            logger.warning("执行奖励命令失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取奖励池
     */
    private RewardPool getRewardPool(int tier, int rank) {
        Map<Integer, RewardPool> rankPools = rewardPools.get(tier);
        if (rankPools == null) return null;
        return rankPools.get(rank);
    }
    
    /**
     * 给予默认奖励（当配置不存在时）
     */
    private List<Reward> giveDefaultRewards(Player player, int tier, int rank, double damagePercent) {
        List<Reward> rewards = new ArrayList<>();
        
        // 默认排名倍率
        double rankMultiplier = switch (rank) {
            case 1 -> 2.0;
            case 2 -> 1.5;
            case 3 -> 1.2;
            case 4, 5 -> 1.0;
            default -> 0.8;
        };
        
        double finalMultiplier = rankMultiplier * (0.5 + damagePercent);
        
        // 经验奖励
        int expAmount = (int) (50.0 * tier * finalMultiplier);
        Reward expReward = Reward.experience(expAmount);
        player.giveExp(expAmount);
        rewards.add(expReward);
        
        // 金钱奖励（如果启用）
        if (enableMoneyRewards && VaultIntegration.isEnabled()) {
            double moneyAmount = 100.0 * tier * finalMultiplier;
            Reward moneyReward = Reward.money(moneyAmount);
            if (VaultIntegration.giveMoney(player, moneyAmount)) {
                rewards.add(moneyReward);
            }
        }
        
        return rewards;
    }
    
    /**
     * 格式化奖励信息
     */
    public String formatReward(Reward reward) {
        switch (reward.getType()) {
            case EXPERIENCE:
                return "§a✔ 经验: §f+" + reward.getIntValue();
            case MONEY:
                return "§a✔ 金钱: §f+" + VaultIntegration.format(reward.getDoubleValue());
            case ITEM:
                ItemStack item = reward.getItemStack();
                return item != null ? "§a✔ 物品: §f" + item.getType().name() + " x" + item.getAmount() : "";
            case MYTHIC_ITEM:
                return "§a✔ " + reward.getDisplayName();
            case COMMAND:
                return "§a✔ " + reward.getDisplayName();
            default:
                return "";
        }
    }
    
    // Getters
    
    public boolean isEnableRewards() {
        return enableRewards;
    }
    
    public boolean isEnableMoneyRewards() {
        return enableMoneyRewards && VaultIntegration.isEnabled();
    }
    
    public boolean isBroadcastRewards() {
        return broadcastRewards;
    }
    
    public int getMaxRewardRanks() {
        return maxRewardRanks;
    }
}
