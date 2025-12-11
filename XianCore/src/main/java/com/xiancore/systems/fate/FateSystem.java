package com.xiancore.systems.fate;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Random;

/**
 * 奇遇系统
 * 负责管理玩家的奇遇触发和奖励
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class FateSystem implements Listener {

    private final XianCore plugin;
    private final Random random = new Random();
    private boolean initialized = false;

    // 奇遇配置
    private double baseChance = 0.0001;  // 基础触发概率
    private int checkInterval = 100;      // 检查间隔（移动次数）
    
    // 奖励配置管理器
    private FateRewardConfig rewardConfig;

    public FateSystem(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化奇遇系统
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 注册事件监听器
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // 加载配置
        loadConfig();

        initialized = true;
        plugin.getLogger().info("  §a✓ 奇遇系统初始化完成");
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        // 从配置文件加载奇遇相关配置
        org.bukkit.configuration.Configuration config = plugin.getConfigManager().getConfig("config");

        if (config != null) {
            // 加载基础触发概率
            double configBaseChance = config.getDouble("fate.base-chance", 0.0001);
            this.baseChance = configBaseChance;

            // 加载检查间隔
            int configCheckInterval = config.getInt("fate.check-interval", 100);
            this.checkInterval = configCheckInterval;

            // 输出配置加载日志
            if (plugin.isDebugMode()) {
                plugin.getLogger().fine("[奇遇系统] 配置已加载");
                plugin.getLogger().fine("  - 基础触发概率: " + baseChance);
                plugin.getLogger().fine("  - 检查间隔: " + checkInterval);
            }
        } else {
            // 使用默认配置
            plugin.getLogger().warning("[奇遇系统] 配置文件未找到，使用默认配置");
        }
        
        // 初始化并加载奖励配置
        rewardConfig = new FateRewardConfig(plugin);
        rewardConfig.load();
    }

    /**
     * 触发奇遇检查
     *
     * @param player 玩家
     */
    public void triggerChance(Player player, Object context) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        // 计算触发概率
        double chance = calculateFateChance(data);

        // 随机判定
        if (random.nextDouble() < chance) {
            // 触发奇遇
            triggerFate(player, data);
        }
    }

    /**
     * 计算奇遇触发概率
     *
     * @param data 玩家数据
     * @return 触发概率
     */
    private double calculateFateChance(PlayerData data) {
        // 基础概率
        double chance = baseChance;

        // 活跃灵气加成（最高3倍）
        double activeQiMultiplier = Math.min(3.0, 1.0 + (data.getActiveQi() / 50.0));
        chance *= activeQiMultiplier;

        // VIP 加成
        // if (player.hasPermission("xiancore.vip")) {
        //     chance *= 1.5;
        // }

        return chance;
    }

    /**
     * 触发奇遇
     *
     * @param player 玩家
     * @param data   玩家数据
     */
    private void triggerFate(Player player, PlayerData data) {
        // 确定奇遇类型
        FateType fateType = determineFateType();

        // 触发对应的奇遇
        switch (fateType) {
            case SMALL -> triggerSmallFate(player, data);
            case MEDIUM -> triggerMediumFate(player, data);
            case LARGE -> triggerLargeFate(player, data);
            case DESTINY -> triggerDestinyFate(player, data);
        }

        // 更新奇遇统计
        data.setFateCount(data.getFateCount() + 1);
        data.setLastFateTime(System.currentTimeMillis());
        plugin.getDataManager().savePlayerData(data);
    }

    /**
     * 确定奇遇类型
     *
     * @return 奇遇类型
     */
    private FateType determineFateType() {
        double roll = random.nextDouble();

        if (roll < 0.005) return FateType.DESTINY;      // 0.5%
        if (roll < 0.05) return FateType.LARGE;         // 4.5%
        if (roll < 0.30) return FateType.MEDIUM;        // 25%
        return FateType.SMALL;                          // 70%
    }

    /**
     * 触发小奇遇
     */
    private void triggerSmallFate(Player player, PlayerData data) {
        player.sendMessage("§e🌿 你触发了一次小奇遇!");
        rewardConfig.giveRewards(player, FateType.SMALL);
    }

    /**
     * 触发中奇遇
     */
    private void triggerMediumFate(Player player, PlayerData data) {
        player.sendMessage("§6✨ 你触发了一次中奇遇!");
        rewardConfig.giveRewards(player, FateType.MEDIUM);
    }

    /**
     * 触发大奇遇
     */
    private void triggerLargeFate(Player player, PlayerData data) {
        player.sendMessage("§d🌟 你触发了一次大奇遇!");
        rewardConfig.giveRewards(player, FateType.LARGE);
    }

    /**
     * 触发命运奇遇
     */
    private void triggerDestinyFate(Player player, PlayerData data) {
        // 全服广播
        plugin.getServer().broadcastMessage("§c§l🌠 玩家 " + player.getName() + " 触发了命运奇遇!");
        player.sendMessage("§c§l⭐ 你触发了命运奇遇! 这是千载难逢的机缘!");
        
        // 发放奖励
        rewardConfig.giveRewards(player, FateType.DESTINY);
    }

    /**
     * 奇遇类型枚举
     */
    public enum FateType {
        SMALL,      // 小奇遇
        MEDIUM,     // 中奇遇
        LARGE,      // 大奇遇
        DESTINY     // 命运奇遇
    }
}
