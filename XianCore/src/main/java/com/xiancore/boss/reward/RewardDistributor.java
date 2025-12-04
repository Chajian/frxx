package com.xiancore.boss.reward;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Boss 奖励分发系统
 * 负责击杀Boss后的奖励分配（金币、经验、道具等）
 *
 * @author XianCore
 * @version 1.0
 */
public class RewardDistributor {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, RewardConfig> rewardConfigs; // mobType -> RewardConfig
    private final Map<UUID, Long> lastRewardTime; // playerUuid -> lastRewardTime

    /**
     * 奖励配置类
     */
    public static class RewardConfig {
        public String mobType;
        public double baseExp;
        public double baseGold;
        public List<String> dropItems; // 物品ID列表
        public double tierMultiplier; // 根据Tier等级的倍数
        public int cooldownSeconds; // 重复击杀冷却时间

        public RewardConfig(String mobType, double baseExp, double baseGold, int cooldownSeconds) {
            this.mobType = mobType;
            this.baseExp = baseExp;
            this.baseGold = baseGold;
            this.dropItems = new ArrayList<>();
            this.tierMultiplier = 1.0;
            this.cooldownSeconds = cooldownSeconds;
        }

        public void addDropItem(String itemId) {
            if (itemId != null && !itemId.isEmpty()) {
                dropItems.add(itemId);
            }
        }

        public void setTierMultiplier(double multiplier) {
            this.tierMultiplier = Math.max(0.5, Math.min(5.0, multiplier)); // 限制范围 0.5-5.0
        }
    }

    /**
     * 玩家贡献数据类
     */
    public static class PlayerContribution {
        public UUID playerUuid;
        public String playerName;
        public double damageDealt;
        public long lastHitTime;
        public double contributionPercentage;

        public PlayerContribution(UUID playerUuid, String playerName, double damageDealt) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.damageDealt = damageDealt;
            this.lastHitTime = System.currentTimeMillis();
            this.contributionPercentage = 0.0;
        }
    }

    /**
     * 分发奖励结果类
     */
    public static class RewardResult {
        public UUID playerId;
        public String playerName;
        public double expReward;
        public double goldReward;
        public List<String> itemsReceived;
        public long timestamp;

        public RewardResult(UUID playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.expReward = 0.0;
            this.goldReward = 0.0;
            this.itemsReceived = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 构造函数
     */
    public RewardDistributor(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.rewardConfigs = new ConcurrentHashMap<>();
        this.lastRewardTime = new ConcurrentHashMap<>();
    }

    /**
     * 注册奖励配置
     */
    public void registerRewardConfig(RewardConfig config) {
        if (config == null || config.mobType == null) {
            logger.warning("✗ 无法注册奖励配置: 参数为 null");
            return;
        }

        rewardConfigs.put(config.mobType, config);
        logger.info("✓ 已注册奖励配置: " + config.mobType + " (基础经验: " + config.baseExp + ", 基础金币: " + config.baseGold + ")");
    }

    /**
     * 获取奖励配置
     */
    public RewardConfig getRewardConfig(String mobType) {
        return rewardConfigs.getOrDefault(mobType, getDefaultRewardConfig(mobType));
    }

    /**
     * 创建默认奖励配置
     */
    private RewardConfig getDefaultRewardConfig(String mobType) {
        return new RewardConfig(mobType, 100.0, 50.0, 3600); // 默认值
    }

    /**
     * 分发奖励给所有玩家
     */
    public Map<UUID, RewardResult> distributeRewards(String mobType, int bossLevel,
                                                     List<PlayerContribution> contributions) {
        Map<UUID, RewardResult> results = new HashMap<>();

        if (contributions == null || contributions.isEmpty()) {
            logger.warning("✗ 没有玩家贡献数据，无法分发奖励");
            return results;
        }

        try {
            RewardConfig config = getRewardConfig(mobType);

            // 计算总伤害
            double totalDamage = contributions.stream()
                    .mapToDouble(c -> c.damageDealt)
                    .sum();

            if (totalDamage <= 0) {
                logger.warning("✗ 总伤害为0，无法分发奖励");
                return results;
            }

            // 计算等级倍数 (Tier等级影响奖励)
            double levelMultiplier = 1.0 + (bossLevel * 0.1);

            // 为每个玩家计算和分发奖励
            for (PlayerContribution contribution : contributions) {
                // 计算贡献百分比
                contribution.contributionPercentage = contribution.damageDealt / totalDamage;

                RewardResult result = new RewardResult(contribution.playerUuid, contribution.playerName);

                // 检查冷却时间
                if (isRewardCoolingDown(contribution.playerUuid, config.cooldownSeconds)) {
                    logger.info("⏳ 玩家 " + contribution.playerName + " 仍在冷却时间内，减少奖励");
                    result.expReward = config.baseExp * contribution.contributionPercentage * config.tierMultiplier * levelMultiplier * 0.5;
                    result.goldReward = config.baseGold * contribution.contributionPercentage * config.tierMultiplier * levelMultiplier * 0.5;
                } else {
                    // 完整奖励
                    result.expReward = config.baseExp * contribution.contributionPercentage * config.tierMultiplier * levelMultiplier;
                    result.goldReward = config.baseGold * contribution.contributionPercentage * config.tierMultiplier * levelMultiplier;
                    recordRewardTime(contribution.playerUuid);
                }

                // 添加掉落物品
                if (!config.dropItems.isEmpty()) {
                    // 随机选择掉落的物品
                    int dropIndex = new Random().nextInt(config.dropItems.size());
                    result.itemsReceived.add(config.dropItems.get(dropIndex));
                }

                results.put(contribution.playerUuid, result);

                logger.info("✓ 玩家奖励: " + contribution.playerName +
                           " | 经验: " + String.format("%.1f", result.expReward) +
                           " | 金币: " + String.format("%.1f", result.goldReward) +
                           " | 贡献: " + String.format("%.1f%%", contribution.contributionPercentage * 100));
            }

        } catch (Exception e) {
            logger.severe("✗ 分发奖励异常: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * 分发单个玩家奖励
     */
    public RewardResult distributePlayerReward(Player player, String mobType, int bossLevel, double damageDealt) {
        if (player == null || mobType == null) {
            logger.warning("✗ 无法分发奖励: 参数为 null");
            return null;
        }

        try {
            PlayerContribution contribution = new PlayerContribution(player.getUniqueId(), player.getName(), damageDealt);
            List<PlayerContribution> contributions = Collections.singletonList(contribution);

            Map<UUID, RewardResult> results = distributeRewards(mobType, bossLevel, contributions);
            return results.getOrDefault(player.getUniqueId(), null);

        } catch (Exception e) {
            logger.severe("✗ 分发单个玩家奖励异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 检查是否在奖励冷却时间内
     */
    private boolean isRewardCoolingDown(UUID playerUuid, int cooldownSeconds) {
        Long lastReward = lastRewardTime.get(playerUuid);
        if (lastReward == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        return (currentTime - lastReward) < cooldownMillis;
    }

    /**
     * 记录玩家获得奖励的时间
     */
    void recordRewardTime(UUID playerUuid) {
        lastRewardTime.put(playerUuid, System.currentTimeMillis());
    }

    /**
     * 获取玩家上次获得奖励的时间
     */
    public long getLastRewardTime(UUID playerUuid) {
        return lastRewardTime.getOrDefault(playerUuid, 0L);
    }

    /**
     * 计算玩家的贡献百分比
     */
    public double calculateContributionPercentage(double playerDamage, double totalDamage) {
        if (totalDamage <= 0) {
            return 0.0;
        }
        return playerDamage / totalDamage;
    }

    /**
     * 计算带有Tier等级倍数的经验奖励
     */
    public double calculateExpReward(double baseExp, double contributionPercentage, int tierLevel) {
        double levelMultiplier = 1.0 + (tierLevel * 0.1);
        return baseExp * contributionPercentage * levelMultiplier;
    }

    /**
     * 计算带有Tier等级倍数的金币奖励
     */
    public double calculateGoldReward(double baseGold, double contributionPercentage, int tierLevel) {
        double levelMultiplier = 1.0 + (tierLevel * 0.1);
        return baseGold * contributionPercentage * levelMultiplier;
    }

    /**
     * 清除过期的冷却时间
     */
    public void clearExpiredCooldowns(int maxAgeSeconds) {
        long currentTime = System.currentTimeMillis();
        long maxAgeMillis = maxAgeSeconds * 1000L;

        lastRewardTime.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > maxAgeMillis
        );

        logger.info("✓ 已清除过期的奖励冷却记录");
    }

    /**
     * 获取所有已注册的奖励配置
     */
    public Set<String> getAllRegisteredMobs() {
        return rewardConfigs.keySet();
    }

    /**
     * 打印奖励配置信息
     */
    public void printRewardConfig(String mobType) {
        RewardConfig config = rewardConfigs.get(mobType);
        if (config == null) {
            logger.info("✗ 未找到该Boss类型的奖励配置: " + mobType);
            return;
        }

        logger.info("=== " + mobType + " 的奖励配置 ===");
        logger.info("  基础经验: " + config.baseExp);
        logger.info("  基础金币: " + config.baseGold);
        logger.info("  Tier倍数: " + config.tierMultiplier);
        logger.info("  冷却时间: " + config.cooldownSeconds + "秒");
        logger.info("  掉落物品数: " + config.dropItems.size());
        for (int i = 0; i < config.dropItems.size(); i++) {
            logger.info("    [" + (i + 1) + "] " + config.dropItems.get(i));
        }
    }

    /**
     * 清除所有缓存
     */
    public void clear() {
        rewardConfigs.clear();
        lastRewardTime.clear();
        logger.info("✓ 已清除所有奖励分发数据");
    }
}
