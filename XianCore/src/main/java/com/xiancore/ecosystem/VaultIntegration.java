package com.xiancore.ecosystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Vault经济系统集成 - 与经济插件无缝集成
 * Vault Economy Integration - Seamless Integration with Economy Plugins
 *
 * @author XianCore
 * @version 1.0
 */
public class VaultIntegration {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, PlayerEconomy> playerEconomies = new ConcurrentHashMap<>();
    private boolean vaultEnabled = false;
    private double rewardMultiplier = 1.0;

    /**
     * 玩家经济信息
     */
    public static class PlayerEconomy {
        public String playerName;
        public String uuid;
        public double balance;
        public double totalEarned;      // 总赚取
        public double totalSpent;       // 总花费
        public int bossKills;          // Boss击杀数
        public double earnRate;        // 赚取速率 (金币/秒)
        public Map<String, Double> bossBounties; // 按Boss类型计算的赏金

        public PlayerEconomy(String playerName, String uuid) {
            this.playerName = playerName;
            this.uuid = uuid;
            this.balance = 0;
            this.totalEarned = 0;
            this.totalSpent = 0;
            this.bossKills = 0;
            this.earnRate = 0;
            this.bossBounties = new ConcurrentHashMap<>();
        }

        public void deposit(double amount) {
            balance += amount;
            totalEarned += amount;
        }

        public boolean withdraw(double amount) {
            if (balance >= amount) {
                balance -= amount;
                totalSpent += amount;
                return true;
            }
            return false;
        }

        public double getNetWorth() {
            return balance;
        }
    }

    /**
     * Boss赏金表
     */
    public static class BossBounty {
        public String bossType;
        public String bossName;
        public int minTier;
        public int maxTier;
        public double baseReward;
        public double rewardPerTier;   // 每等级额外奖励
        public boolean enabled;

        public BossBounty(String bossType, String bossName, double baseReward) {
            this.bossType = bossType;
            this.bossName = bossName;
            this.minTier = 1;
            this.maxTier = 5;
            this.baseReward = baseReward;
            this.rewardPerTier = baseReward * 0.2;
            this.enabled = true;
        }

        public double calculateReward(int tier) {
            if (!enabled) return 0;
            int clampedTier = Math.max(minTier, Math.min(maxTier, tier));
            return baseReward + (rewardPerTier * (clampedTier - minTier));
        }
    }

    /**
     * 经济交易记录
     */
    public static class EconomyTransaction {
        public String transactionId;
        public String playerName;
        public TransactionType type;
        public double amount;
        public String reason;          // 交易原因
        public long timestamp;
        public Map<String, Object> metadata;

        public enum TransactionType {
            BOSS_KILL_REWARD,  // Boss击杀奖励
            PURCHASE,          // 购买
            PENALTY,           // 惩罚
            ADMIN_COMMAND,     // 管理员命令
            TRANSFER           // 转账
        }

        public EconomyTransaction(String playerName, TransactionType type, double amount, String reason) {
            this.transactionId = UUID.randomUUID().toString();
            this.playerName = playerName;
            this.type = type;
            this.amount = amount;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }
    }

    /**
     * 构造函数
     */
    public VaultIntegration() {
        logger.info("✓ VaultIntegration已初始化");
    }

    /**
     * 初始化Vault
     */
    public void initializeVault(boolean enabled) {
        this.vaultEnabled = enabled;
        if (enabled) {
            logger.info("✓ Vault经济系统已启用");
        } else {
            logger.info("⚠ Vault经济系统已禁用，使用本地经济系统");
        }
    }

    /**
     * 获取或创建玩家经济信息
     */
    public PlayerEconomy getPlayerEconomy(String playerName, String uuid) {
        return playerEconomies.computeIfAbsent(playerName, k ->
                new PlayerEconomy(playerName, uuid));
    }

    /**
     * 记录Boss击杀奖励
     */
    public EconomyTransaction recordBossKillReward(String playerName, String bossType, int bossTier, double baseReward) {
        PlayerEconomy economy = playerEconomies.get(playerName);
        if (economy == null) {
            logger.warning("⚠ 玩家经济信息不存在: " + playerName);
            return null;
        }

        // 计算奖励
        double finalReward = baseReward * rewardMultiplier;
        if (bossTier > 1) {
            finalReward *= (1 + (bossTier - 1) * 0.25);  // 等级加成
        }

        economy.deposit(finalReward);
        economy.bossKills++;
        economy.bossBounties.put(bossType,
                economy.bossBounties.getOrDefault(bossType, 0.0) + finalReward);

        EconomyTransaction transaction = new EconomyTransaction(playerName,
                EconomyTransaction.TransactionType.BOSS_KILL_REWARD,
                finalReward, "击杀Boss: " + bossType + " (T" + bossTier + ")");

        logger.info("✓ Boss击杀奖励已记录: " + playerName + " +$" + String.format("%.2f", finalReward));

        return transaction;
    }

    /**
     * 处理玩家购买
     */
    public boolean processPurchase(String playerName, String itemName, double cost, String reason) {
        PlayerEconomy economy = playerEconomies.get(playerName);
        if (economy == null) return false;

        if (economy.withdraw(cost)) {
            logger.info("✓ 购买已处理: " + playerName + " -$" + String.format("%.2f", cost) + " (" + itemName + ")");
            return true;
        } else {
            logger.warning("⚠ 玩家金币不足: " + playerName + " (需要: $" + String.format("%.2f", cost) +
                    ", 拥有: $" + String.format("%.2f", economy.balance) + ")");
            return false;
        }
    }

    /**
     * 设置奖励倍数
     */
    public void setRewardMultiplier(double multiplier) {
        this.rewardMultiplier = Math.max(0.1, multiplier);
        logger.info("✓ 奖励倍数已设置: " + String.format("%.2f", rewardMultiplier) + "x");
    }

    /**
     * 获取玩家余额
     */
    public double getPlayerBalance(String playerName) {
        PlayerEconomy economy = playerEconomies.get(playerName);
        return economy != null ? economy.balance : 0;
    }

    /**
     * 获取所有玩家经济信息
     */
    public Collection<PlayerEconomy> getAllPlayerEconomies() {
        return playerEconomies.values();
    }

    /**
     * 获取富豪排行榜
     */
    public List<PlayerEconomy> getRichestPlayers(int limit) {
        return playerEconomies.values().stream()
                .sorted((a, b) -> Double.compare(b.balance, a.balance))
                .limit(limit)
                .toList();
    }

    /**
     * 获取杀手排行榜
     */
    public List<PlayerEconomy> getTopKillers(int limit) {
        return playerEconomies.values().stream()
                .sorted((a, b) -> Integer.compare(b.bossKills, a.bossKills))
                .limit(limit)
                .toList();
    }

    /**
     * 重置玩家经济
     */
    public void resetPlayerEconomy(String playerName) {
        playerEconomies.remove(playerName);
        logger.info("✓ 玩家经济已重置: " + playerName);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("vault_enabled", vaultEnabled);
        stats.put("reward_multiplier", String.format("%.2fx", rewardMultiplier));
        stats.put("total_players", playerEconomies.size());

        double totalBalance = playerEconomies.values().stream()
                .mapToDouble(e -> e.balance)
                .sum();
        stats.put("total_balance", String.format("$%.2f", totalBalance));

        double totalEarned = playerEconomies.values().stream()
                .mapToDouble(e -> e.totalEarned)
                .sum();
        stats.put("total_earned", String.format("$%.2f", totalEarned));

        int totalKills = (int) playerEconomies.values().stream()
                .mapToInt(e -> e.bossKills)
                .sum();
        stats.put("total_boss_kills", totalKills);

        return stats;
    }

    /**
     * 重置系统
     */
    public void reset() {
        playerEconomies.clear();
        logger.info("✓ Vault经济系统已重置");
    }
}
