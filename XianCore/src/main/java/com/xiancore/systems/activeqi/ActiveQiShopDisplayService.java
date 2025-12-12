package com.xiancore.systems.activeqi;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 活跃灵气商店显示服务
 * 负责活跃灵气商店GUI的业务逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ActiveQiShopDisplayService {

    private final XianCore plugin;

    // 加成消耗常量
    public static final int BREAKTHROUGH_BOOST_COST = 30;
    public static final int FORGE_BOOST_COST = 25;
    public static final int DAILY_GIFT_COST = 100;

    // 加成效果常量
    public static final String BREAKTHROUGH_BOOST_EFFECT = "+5% 突破成功率";
    public static final String FORGE_BOOST_EFFECT = "+3% 炼制成功率";

    public ActiveQiShopDisplayService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取玩家活跃灵气
     */
    public long getActiveQi(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        return data != null ? data.getActiveQi() : 0L;
    }

    /**
     * 检查是否拥有突破加成
     */
    public boolean hasBreakthroughBoost(UUID playerId) {
        return plugin.getActiveQiManager().hasActiveBoost(playerId, ActiveQiManager.ActiveQiBoostType.BREAKTHROUGH);
    }

    /**
     * 检查是否拥有炼制加成
     */
    public boolean hasForgeBoost(UUID playerId) {
        return plugin.getActiveQiManager().hasActiveBoost(playerId, ActiveQiManager.ActiveQiBoostType.FORGE);
    }

    /**
     * 检查每日礼包是否在冷却中
     */
    public boolean isDailyGiftOnCooldown(UUID playerId) {
        return plugin.getActiveQiManager().isDailyGiftOnCooldown(playerId);
    }

    /**
     * 获取每日礼包剩余冷却时间（小时）
     */
    public long getDailyGiftRemainingCooldown(UUID playerId) {
        return plugin.getActiveQiManager().getDailyGiftRemainingCooldown(playerId);
    }

    /**
     * 购买突破加成
     */
    public boolean purchaseBreakthroughBoost(Player player) {
        return plugin.getActiveQiManager().purchaseBreakthroughBoost(player);
    }

    /**
     * 购买炼制加成
     */
    public boolean purchaseForgeBoost(Player player) {
        return plugin.getActiveQiManager().purchaseForgeBoost(player);
    }

    /**
     * 领取每日礼包
     */
    public boolean claimDailyGift(Player player) {
        return plugin.getActiveQiManager().claimDailyGift(player);
    }

    /**
     * 获取商店物品信息
     */
    public ShopItemInfo getBreakthroughBoostInfo(UUID playerId) {
        boolean hasBoost = hasBreakthroughBoost(playerId);
        return new ShopItemInfo(
                "★ 突破加成",
                BREAKTHROUGH_BOOST_COST,
                BREAKTHROUGH_BOOST_EFFECT,
                "本次突破",
                hasBoost,
                hasBoost ? "已拥有此加成" : "点击购买"
        );
    }

    /**
     * 获取炼制加成信息
     */
    public ShopItemInfo getForgeBoostInfo(UUID playerId) {
        boolean hasBoost = hasForgeBoost(playerId);
        return new ShopItemInfo(
                "⚏ 炼制加成",
                FORGE_BOOST_COST,
                FORGE_BOOST_EFFECT,
                "本次炼制/强化/融合",
                hasBoost,
                hasBoost ? "已拥有此加成" : "点击购买"
        );
    }

    /**
     * 获取每日礼包信息
     */
    public DailyGiftInfo getDailyGiftInfo(UUID playerId) {
        boolean onCooldown = isDailyGiftOnCooldown(playerId);
        long remainingHours = onCooldown ? getDailyGiftRemainingCooldown(playerId) : 0;

        return new DailyGiftInfo(
                "❤ 每日礼包",
                DAILY_GIFT_COST,
                onCooldown,
                remainingHours,
                onCooldown ? "冷却中 (" + remainingHours + " 小时)" : "点击领取"
        );
    }

    /**
     * 商店物品信息
     */
    public static class ShopItemInfo {
        private final String name;
        private final int cost;
        private final String effect;
        private final String duration;
        private final boolean owned;
        private final String actionText;

        public ShopItemInfo(String name, int cost, String effect, String duration, boolean owned, String actionText) {
            this.name = name;
            this.cost = cost;
            this.effect = effect;
            this.duration = duration;
            this.owned = owned;
            this.actionText = actionText;
        }

        public String getName() { return name; }
        public int getCost() { return cost; }
        public String getEffect() { return effect; }
        public String getDuration() { return duration; }
        public boolean isOwned() { return owned; }
        public String getActionText() { return actionText; }
    }

    /**
     * 每日礼包信息
     */
    public static class DailyGiftInfo {
        private final String name;
        private final int cost;
        private final boolean onCooldown;
        private final long remainingHours;
        private final String actionText;

        public DailyGiftInfo(String name, int cost, boolean onCooldown, long remainingHours, String actionText) {
            this.name = name;
            this.cost = cost;
            this.onCooldown = onCooldown;
            this.remainingHours = remainingHours;
            this.actionText = actionText;
        }

        public String getName() { return name; }
        public int getCost() { return cost; }
        public boolean isOnCooldown() { return onCooldown; }
        public long getRemainingHours() { return remainingHours; }
        public String getActionText() { return actionText; }
    }
}
