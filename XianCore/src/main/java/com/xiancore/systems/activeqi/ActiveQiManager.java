package com.xiancore.systems.activeqi;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 活跃灵气管理器
 * 管理活跃灵气的消耗、加成和衰减
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class ActiveQiManager {

    private final XianCore plugin;

    // 活跃灵气加成状态（玩家UUID -> 加成类型 -> 使用时间）
    private final Map<UUID, Map<ActiveQiBoostType, Long>> activeBoosts;

    // 每日礼包领取记录（玩家UUID -> 最后领取时间）
    private final Map<UUID, Long> dailyGiftClaims;

    // 活跃灵气消耗配置
    private static final int BREAKTHROUGH_BOOST_COST = 30;      // 突破加成消耗
    private static final double BREAKTHROUGH_BOOST_RATE = 0.05;  // 突破加成5%

    private static final int FORGE_BOOST_COST = 25;             // 炼制加成消耗
    private static final double FORGE_BOOST_RATE = 0.03;         // 炼制加成3%

    private static final int DAILY_GIFT_COST = 100;             // 每日礼包消耗
    private static final long DAILY_GIFT_COOLDOWN = 24 * 60 * 60 * 1000L; // 24小时

    public ActiveQiManager(XianCore plugin) {
        this.plugin = plugin;
        this.activeBoosts = new HashMap<>();
        this.dailyGiftClaims = new HashMap<>();
    }

    /**
     * 使用活跃灵气购买突破加成
     *
     * @param player 玩家
     * @return 是否成功
     */
    public boolean purchaseBreakthroughBoost(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查活跃灵气是否足够
        if (data.getActiveQi() < BREAKTHROUGH_BOOST_COST) {
            player.sendMessage("§c活跃灵气不足!");
            player.sendMessage("§7需要: §e" + BREAKTHROUGH_BOOST_COST + " §7当前: §f" + data.getActiveQi());
            return false;
        }

        // 检查是否已经有加成
        if (hasActiveBoost(player.getUniqueId(), ActiveQiBoostType.BREAKTHROUGH)) {
            player.sendMessage("§c你已经拥有突破加成效果!");
            return false;
        }

        // 扣除活跃灵气
        if (!data.removeActiveQi(BREAKTHROUGH_BOOST_COST)) {
            player.sendMessage("§c扣除活跃灵气失败!");
            return false;
        }

        // 激活加成
        activateBoost(player.getUniqueId(), ActiveQiBoostType.BREAKTHROUGH);

        // 保存数据
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("§a§l========== 突破加成激活 ==========");
        player.sendMessage("§e效果: §f突破成功率 §a+5%");
        player.sendMessage("§e持续: §f本次突破");
        player.sendMessage("§e消耗: §f" + BREAKTHROUGH_BOOST_COST + " 活跃灵气");
        player.sendMessage("§a§l================================");

        return true;
    }

    /**
     * 使用活跃灵气购买炼制加成
     *
     * @param player 玩家
     * @return 是否成功
     */
    public boolean purchaseForgeBoost(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查活跃灵气是否足够
        if (data.getActiveQi() < FORGE_BOOST_COST) {
            player.sendMessage("§c活跃灵气不足!");
            player.sendMessage("§7需要: §e" + FORGE_BOOST_COST + " §7当前: §f" + data.getActiveQi());
            return false;
        }

        // 检查是否已经有加成
        if (hasActiveBoost(player.getUniqueId(), ActiveQiBoostType.FORGE)) {
            player.sendMessage("§c你已经拥有炼制加成效果!");
            return false;
        }

        // 扣除活跃灵气
        if (!data.removeActiveQi(FORGE_BOOST_COST)) {
            player.sendMessage("§c扣除活跃灵气失败!");
            return false;
        }

        // 激活加成
        activateBoost(player.getUniqueId(), ActiveQiBoostType.FORGE);

        // 保存数据
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("§a§l========== 炼制加成激活 ==========");
        player.sendMessage("§e效果: §f炼制成功率 §a+3%");
        player.sendMessage("§e持续: §f本次炼制");
        player.sendMessage("§e消耗: §f" + FORGE_BOOST_COST + " 活跃灵气");
        player.sendMessage("§a§l================================");

        return true;
    }

    /**
     * 领取每日礼包
     *
     * @param player 玩家
     * @return 是否成功
     */
    public boolean claimDailyGift(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerData data = plugin.getDataManager().loadPlayerData(playerId);

        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查活跃灵气是否足够
        if (data.getActiveQi() < DAILY_GIFT_COST) {
            player.sendMessage("§c活跃灵气不足!");
            player.sendMessage("§7需要: §e" + DAILY_GIFT_COST + " §7当前: §f" + data.getActiveQi());
            return false;
        }

        // 检查冷却时间
        Long lastClaim = dailyGiftClaims.get(playerId);
        long currentTime = System.currentTimeMillis();

        if (lastClaim != null) {
            long timeSinceLast = currentTime - lastClaim;
            if (timeSinceLast < DAILY_GIFT_COOLDOWN) {
                long remainingTime = (DAILY_GIFT_COOLDOWN - timeSinceLast) / 1000 / 60 / 60; // 小时
                player.sendMessage("§c每日礼包冷却中!");
                player.sendMessage("§7剩余时间: §f" + remainingTime + " §7小时");
                return false;
            }
        }

        // 扣除活跃灵气
        if (!data.removeActiveQi(DAILY_GIFT_COST)) {
            player.sendMessage("§c扣除活跃灵气失败!");
            return false;
        }

        // 生成随机奖励
        DailyGift gift = generateDailyGift(data);

        // 发放奖励
        if (gift.spiritStones > 0) {
            data.addSpiritStones(gift.spiritStones);
        }
        if (gift.skillPoints > 0) {
            data.addSkillPoints(gift.skillPoints);
        }
        if (gift.contributionPoints > 0) {
            data.addContribution(gift.contributionPoints);
        }

        // 记录领取时间
        dailyGiftClaims.put(playerId, currentTime);

        // 保存数据
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("§a§l========== 每日礼包 ==========");
        player.sendMessage("§e消耗: §f" + DAILY_GIFT_COST + " 活跃灵气");
        player.sendMessage("");
        player.sendMessage("§e奖励:");
        if (gift.spiritStones > 0) {
            player.sendMessage("§7  - 灵石: §6+" + gift.spiritStones);
        }
        if (gift.skillPoints > 0) {
            player.sendMessage("§7  - 功法点: §d+" + gift.skillPoints);
        }
        if (gift.contributionPoints > 0) {
            player.sendMessage("§7  - 贡献点: §b+" + gift.contributionPoints);
        }
        player.sendMessage("§a§l============================");

        return true;
    }

    /**
     * 检查玩家是否有指定类型的加成
     *
     * @param playerId 玩家UUID
     * @param type 加成类型
     * @return 是否有加成
     */
    public boolean hasActiveBoost(UUID playerId, ActiveQiBoostType type) {
        Map<ActiveQiBoostType, Long> boosts = activeBoosts.get(playerId);
        return boosts != null && boosts.containsKey(type);
    }

    /**
     * 获取突破加成比率
     *
     * @param playerId 玩家UUID
     * @return 加成比率（例如：0.05 表示5%）
     */
    public double getBreakthroughBoost(UUID playerId) {
        if (hasActiveBoost(playerId, ActiveQiBoostType.BREAKTHROUGH)) {
            return BREAKTHROUGH_BOOST_RATE;
        }
        return 0.0;
    }

    /**
     * 获取炼制加成比率
     *
     * @param playerId 玩家UUID
     * @return 加成比率（例如：0.03 表示3%）
     */
    public double getForgeBoost(UUID playerId) {
        if (hasActiveBoost(playerId, ActiveQiBoostType.FORGE)) {
            return FORGE_BOOST_RATE;
        }
        return 0.0;
    }

    /**
     * 激活加成
     *
     * @param playerId 玩家UUID
     * @param type 加成类型
     */
    private void activateBoost(UUID playerId, ActiveQiBoostType type) {
        Map<ActiveQiBoostType, Long> boosts = activeBoosts.computeIfAbsent(playerId, k -> new HashMap<>());
        boosts.put(type, System.currentTimeMillis());
    }

    /**
     * 消耗加成（使用后清除）
     *
     * @param playerId 玩家UUID
     * @param type 加成类型
     */
    public void consumeBoost(UUID playerId, ActiveQiBoostType type) {
        Map<ActiveQiBoostType, Long> boosts = activeBoosts.get(playerId);
        if (boosts != null) {
            boosts.remove(type);
            if (boosts.isEmpty()) {
                activeBoosts.remove(playerId);
            }
        }
    }

    /**
     * 生成每日礼包奖励
     *
     * @param data 玩家数据
     * @return 礼包内容
     */
    private DailyGift generateDailyGift(PlayerData data) {
        DailyGift gift = new DailyGift();

        // 根据境界调整奖励
        int realmLevel = getRealmLevel(data.getRealm());

        // 灵石奖励（100-500，根据境界）
        gift.spiritStones = 100 + (realmLevel * 50);

        // 功法点奖励（1-3）
        gift.skillPoints = Math.min(3, 1 + (realmLevel / 3));

        // 贡献点奖励（50-200）
        gift.contributionPoints = 50 + (realmLevel * 20);

        return gift;
    }

    /**
     * 获取境界等级
     *
     * @param realmName 境界名称
     * @return 境界等级（1-8）
     */
    private int getRealmLevel(String realmName) {
        String[] realms = {"炼气期", "筑基期", "结丹期", "元婴期", "化神期", "炼虚期", "合体期", "大乘期"};
        for (int i = 0; i < realms.length; i++) {
            if (realms[i].equals(realmName)) {
                return i + 1;
            }
        }
        return 1;
    }

    /**
     * 每日礼包数据类
     */
    private static class DailyGift {
        int spiritStones = 0;
        int skillPoints = 0;
        int contributionPoints = 0;
    }

    /**
     * 活跃灵气加成类型
     */
    public enum ActiveQiBoostType {
        BREAKTHROUGH,  // 突破加成
        FORGE         // 炼制加成
    }

    /**
     * 获取每日礼包剩余冷却时间（小时）
     *
     * @param playerId 玩家UUID
     * @return 剩余冷却时间（小时），如果没有冷却返回 0
     */
    public long getDailyGiftRemainingCooldown(UUID playerId) {
        Long lastClaim = dailyGiftClaims.get(playerId);
        if (lastClaim == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceLast = currentTime - lastClaim;

        if (timeSinceLast >= DAILY_GIFT_COOLDOWN) {
            return 0;
        }

        return (DAILY_GIFT_COOLDOWN - timeSinceLast) / 1000 / 60 / 60; // 小时
    }

    /**
     * 检查每日礼包是否在冷却中
     *
     * @param playerId 玩家UUID
     * @return 是否在冷却中
     */
    public boolean isDailyGiftOnCooldown(UUID playerId) {
        return getDailyGiftRemainingCooldown(playerId) > 0;
    }
}
