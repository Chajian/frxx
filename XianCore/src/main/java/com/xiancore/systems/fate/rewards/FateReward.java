package com.xiancore.systems.fate.rewards;

import com.xiancore.XianCore;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 奇遇奖励抽象基类
 * 所有奖励类型都继承此类
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public abstract class FateReward {

    protected final XianCore plugin;
    protected String type;
    protected double chance;  // 奖励触发概率 (0.0-1.0)

    public FateReward(XianCore plugin, String type) {
        this.plugin = plugin;
        this.type = type;
        this.chance = 1.0;  // 默认100%概率
    }

    /**
     * 发放奖励给玩家
     *
     * @param player 玩家
     * @return 奖励描述消息，null表示发放失败或被概率判定跳过
     */
    public abstract String give(Player player);

    /**
     * 从配置Map创建奖励对象
     *
     * @param map    配置Map
     * @param plugin 插件实例
     * @return 奖励对象
     * @throws IllegalArgumentException 如果配置无效
     */
    public static FateReward fromMap(Map<String, Object> map, XianCore plugin) {
        // 读取奖励类型
        String type = (String) map.get("type");
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("奖励配置缺少 type 字段");
        }

        // 根据类型创建对应的奖励对象
        FateReward reward = switch (type.toLowerCase()) {
            // 数值奖励
            case "qi" -> NumericReward.fromMap(map, plugin, "qi");
            case "spirit-stones" -> NumericReward.fromMap(map, plugin, "spirit-stones");
            case "skill-points" -> NumericReward.fromMap(map, plugin, "skill-points");
            case "contribution" -> NumericReward.fromMap(map, plugin, "contribution");
            case "active-qi" -> NumericReward.fromMap(map, plugin, "active-qi");
            case "level" -> NumericReward.fromMap(map, plugin, "level");

            // 物品奖励
            case "item" -> ItemReward.fromMap(map, plugin);

            // 功法书奖励
            case "skill-book" -> SkillBookReward.fromMap(map, plugin);

            // 装备奖励
            case "equipment" -> EquipmentReward.fromMap(map, plugin);
            // case "embryo" -> EmbryoReward.fromMap(map, plugin);  // 可选，暂不实现

            // 命令奖励
            case "command" -> CommandReward.fromMap(map, plugin);

            // 任务奖励（将在 Day 5 实现，可选）
            // case "task" -> TaskReward.fromMap(map, plugin);

            default -> {
                plugin.getLogger().warning("[奇遇系统] 未知的奖励类型: " + type);
                yield null;
            }
        };

        // 读取通用属性
        if (reward != null && map.containsKey("chance")) {
            try {
                Object chanceObj = map.get("chance");
                if (chanceObj instanceof Number) {
                    reward.chance = ((Number) chanceObj).doubleValue();
                } else if (chanceObj instanceof String) {
                    reward.chance = Double.parseDouble((String) chanceObj);
                }

                // 确保概率在有效范围内
                if (reward.chance < 0.0 || reward.chance > 1.0) {
                    plugin.getLogger().warning("[奇遇系统] 奖励概率超出范围 [0-1]: " + reward.chance + "，已修正");
                    reward.chance = Math.max(0.0, Math.min(1.0, reward.chance));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[奇遇系统] 解析奖励概率失败，使用默认值 1.0");
            }
        }

        return reward;
    }

    /**
     * 是否应该发放奖励（概率判定）
     */
    protected boolean shouldGive() {
        return Math.random() < chance;
    }

    /**
     * 获取奖励类型
     */
    public String getType() {
        return type;
    }

    /**
     * 获取触发概率
     */
    public double getChance() {
        return chance;
    }

    /**
     * 设置触发概率
     */
    public void setChance(double chance) {
        this.chance = Math.max(0.0, Math.min(1.0, chance));
    }
}


