package com.xiancore.systems.forge.enhance;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.activeqi.ActiveQiManager;
import com.xiancore.systems.forge.items.EquipmentParser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 装备强化服务
 * 负责强化相关的业务逻辑，与 GUI 分离
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class EnhanceService {

    private final XianCore plugin;
    private final Random random = new Random();

    // 并发控制锁
    private final ConcurrentHashMap<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> processingFlags = new ConcurrentHashMap<>();

    // 常量配置
    public static final int MAX_ENHANCE_LEVEL = 20;
    public static final int BASE_COST = 100;
    public static final int COST_PER_LEVEL = 50;
    public static final int BASE_SUCCESS_RATE = 90;
    public static final int RATE_DECREASE_PER_LEVEL = 3;
    public static final int MIN_SUCCESS_RATE = 20;
    public static final int DOWNGRADE_CHANCE = 10;

    public EnhanceService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 计算强化消耗
     *
     * @param currentLevel 当前等级
     * @return 消耗的灵石数量
     */
    public int calculateCost(int currentLevel) {
        return BASE_COST + currentLevel * COST_PER_LEVEL;
    }

    /**
     * 计算基础成功率
     *
     * @param currentLevel 当前等级
     * @return 基础成功率 (0-100)
     */
    public int calculateBaseSuccessRate(int currentLevel) {
        return Math.max(MIN_SUCCESS_RATE, BASE_SUCCESS_RATE - currentLevel * RATE_DECREASE_PER_LEVEL);
    }

    /**
     * 计算最终成功率（包含所有加成）
     *
     * @param player 玩家
     * @param currentLevel 当前强化等级
     * @return 最终成功率信息
     */
    public EnhanceRateInfo calculateFinalSuccessRate(Player player, int currentLevel) {
        int baseRate = calculateBaseSuccessRate(currentLevel);

        // 活跃灵气加成
        double forgeBoost = plugin.getActiveQiManager().getForgeBoost(player.getUniqueId());
        int activeQiBonus = forgeBoost > 0 ? (int)(forgeBoost * 100) : 0;

        // 宗门设施加成
        int sectBonus = 0;
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data != null && data.getSectId() != null) {
            sectBonus = (int) plugin.getSectSystem().getFacilityManager()
                    .getForgeSuccessBonus(data.getSectId());
        }

        int finalRate = Math.min(100, baseRate + activeQiBonus + sectBonus);

        return new EnhanceRateInfo(baseRate, activeQiBonus, sectBonus, finalRate);
    }

    /**
     * 检查是否可以强化
     *
     * @param player 玩家
     * @param item 装备物品
     * @return 检查结果
     */
    public EnhanceCheckResult checkCanEnhance(Player player, ItemStack item) {
        // 检查物品是否存在
        if (item == null || item.getType().isAir()) {
            return EnhanceCheckResult.failure("物品不存在");
        }

        // 检查是否是装备
        if (!EquipmentParser.isEquipment(item)) {
            return EnhanceCheckResult.failure("该物品不是装备");
        }

        // 检查等级
        int currentLevel = EquipmentParser.getEnhanceLevel(item);
        if (currentLevel >= MAX_ENHANCE_LEVEL) {
            return EnhanceCheckResult.failure("装备已达最大强化等级");
        }

        // 检查灵石
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return EnhanceCheckResult.failure("玩家数据加载失败");
        }

        int cost = calculateCost(currentLevel);
        if (data.getSpiritStones() < cost) {
            return EnhanceCheckResult.failure("灵石不足，需要 " + cost + " 灵石");
        }

        return EnhanceCheckResult.success(currentLevel, cost, data.getSpiritStones());
    }

    /**
     * 执行强化操作
     *
     * @param player 玩家
     * @param item 装备物品
     * @param selectedSlot 装备所在槽位
     * @return 强化结果
     */
    public EnhanceResult performEnhance(Player player, ItemStack item, int selectedSlot) {
        UUID playerId = player.getUniqueId();

        // 获取并发锁
        ReentrantLock lock = playerLocks.computeIfAbsent(playerId, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            return EnhanceResult.failure("正在处理中，请稍候...");
        }

        // 检查是否正在处理
        if (processingFlags.getOrDefault(playerId, false)) {
            lock.unlock();
            return EnhanceResult.failure("正在强化中，请勿重复点击!");
        }

        processingFlags.put(playerId, true);

        try {
            // 重新验证物品
            ItemStack currentItem = player.getInventory().getItem(selectedSlot);
            if (currentItem == null || currentItem.getType().isAir()) {
                return EnhanceResult.failure("物品已移除");
            }

            if (!EquipmentParser.isEquipment(currentItem)) {
                return EnhanceResult.failure("选中的物品不是装备");
            }

            // 获取玩家数据
            PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
            if (data == null) {
                return EnhanceResult.failure("数据加载失败");
            }

            // 获取当前等级
            int currentLevel = EquipmentParser.getEnhanceLevel(currentItem);
            if (currentLevel >= MAX_ENHANCE_LEVEL) {
                return EnhanceResult.failure("装备已达最大强化等级");
            }

            // 计算消耗
            int cost = calculateCost(currentLevel);
            if (data.getSpiritStones() < cost) {
                return EnhanceResult.failure("灵石不足");
            }

            // 计算成功率
            EnhanceRateInfo rateInfo = calculateFinalSuccessRate(player, currentLevel);

            // 检查是否有活跃灵气加成
            double forgeBoost = plugin.getActiveQiManager().getForgeBoost(playerId);

            // 执行随机判定
            boolean success = random.nextInt(100) < rateInfo.getFinalRate();

            EnhanceResult result;

            if (success) {
                // 强化成功
                data.removeSpiritStones(cost);

                int newLevel = currentLevel + 1;
                EquipmentParser.setEnhanceLevel(currentItem, newLevel);
                EquipmentParser.updateEnhanceStats(currentItem, newLevel);
                player.getInventory().setItem(selectedSlot, currentItem);

                // 增加活跃灵气
                int activeQiGain = Math.min(10, 3 + newLevel / 5);
                data.addActiveQi(activeQiGain);

                result = EnhanceResult.success(currentLevel, newLevel, cost, activeQiGain);

            } else {
                // 强化失败
                int refund = cost / 2;
                data.removeSpiritStones(cost - refund);  // 只扣除一半

                // 10%概率降级
                boolean downgraded = false;
                int newLevel = currentLevel;

                if (random.nextInt(100) < DOWNGRADE_CHANCE && currentLevel > 0) {
                    newLevel = currentLevel - 1;
                    EquipmentParser.setEnhanceLevel(currentItem, newLevel);
                    EquipmentParser.updateEnhanceStats(currentItem, newLevel);
                    player.getInventory().setItem(selectedSlot, currentItem);
                    downgraded = true;
                }

                result = EnhanceResult.fail(currentLevel, newLevel, cost - refund, downgraded);
            }

            // 消耗活跃灵气加成
            if (forgeBoost > 0) {
                plugin.getActiveQiManager().consumeBoost(playerId, ActiveQiManager.ActiveQiBoostType.FORGE);
            }

            // 保存数据
            plugin.getDataManager().savePlayerData(data);

            return result;

        } finally {
            processingFlags.put(playerId, false);
            lock.unlock();
        }
    }

    /**
     * 强化成功率信息
     */
    public static class EnhanceRateInfo {
        private final int baseRate;
        private final int activeQiBonus;
        private final int sectBonus;
        private final int finalRate;

        public EnhanceRateInfo(int baseRate, int activeQiBonus, int sectBonus, int finalRate) {
            this.baseRate = baseRate;
            this.activeQiBonus = activeQiBonus;
            this.sectBonus = sectBonus;
            this.finalRate = finalRate;
        }

        public int getBaseRate() { return baseRate; }
        public int getActiveQiBonus() { return activeQiBonus; }
        public int getSectBonus() { return sectBonus; }
        public int getFinalRate() { return finalRate; }
    }

    /**
     * 强化前检查结果
     */
    public static class EnhanceCheckResult {
        private final boolean canEnhance;
        private final String failReason;
        private final int currentLevel;
        private final int cost;
        private final long playerSpiritStones;

        private EnhanceCheckResult(boolean canEnhance, String failReason, int currentLevel, int cost, long playerSpiritStones) {
            this.canEnhance = canEnhance;
            this.failReason = failReason;
            this.currentLevel = currentLevel;
            this.cost = cost;
            this.playerSpiritStones = playerSpiritStones;
        }

        public static EnhanceCheckResult success(int currentLevel, int cost, long playerSpiritStones) {
            return new EnhanceCheckResult(true, null, currentLevel, cost, playerSpiritStones);
        }

        public static EnhanceCheckResult failure(String reason) {
            return new EnhanceCheckResult(false, reason, 0, 0, 0);
        }

        public boolean canEnhance() { return canEnhance; }
        public String getFailReason() { return failReason; }
        public int getCurrentLevel() { return currentLevel; }
        public int getCost() { return cost; }
        public long getPlayerSpiritStones() { return playerSpiritStones; }
    }

    /**
     * 强化结果
     */
    public static class EnhanceResult {
        private final boolean success;
        private final boolean executed;
        private final String errorMessage;
        private final int previousLevel;
        private final int newLevel;
        private final int costPaid;
        private final int activeQiGain;
        private final boolean downgraded;

        private EnhanceResult(boolean success, boolean executed, String errorMessage,
                              int previousLevel, int newLevel, int costPaid, int activeQiGain, boolean downgraded) {
            this.success = success;
            this.executed = executed;
            this.errorMessage = errorMessage;
            this.previousLevel = previousLevel;
            this.newLevel = newLevel;
            this.costPaid = costPaid;
            this.activeQiGain = activeQiGain;
            this.downgraded = downgraded;
        }

        public static EnhanceResult success(int previousLevel, int newLevel, int costPaid, int activeQiGain) {
            return new EnhanceResult(true, true, null, previousLevel, newLevel, costPaid, activeQiGain, false);
        }

        public static EnhanceResult fail(int previousLevel, int newLevel, int costPaid, boolean downgraded) {
            return new EnhanceResult(false, true, null, previousLevel, newLevel, costPaid, 0, downgraded);
        }

        public static EnhanceResult failure(String errorMessage) {
            return new EnhanceResult(false, false, errorMessage, 0, 0, 0, 0, false);
        }

        public boolean isSuccess() { return success; }
        public boolean isExecuted() { return executed; }
        public String getErrorMessage() { return errorMessage; }
        public int getPreviousLevel() { return previousLevel; }
        public int getNewLevel() { return newLevel; }
        public int getCostPaid() { return costPaid; }
        public int getActiveQiGain() { return activeQiGain; }
        public boolean isDowngraded() { return downgraded; }
    }
}
