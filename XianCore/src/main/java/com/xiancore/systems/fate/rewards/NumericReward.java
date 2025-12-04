package com.xiancore.systems.fate.rewards;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;

/**
 * 数值奖励（修为、灵石、功法点等）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class NumericReward extends FateReward {

    private long minAmount;
    private long maxAmount;
    private final Random random = new Random();

    public NumericReward(XianCore plugin, String type, long minAmount, long maxAmount) {
        super(plugin, type);
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    @Override
    public String give(Player player) {
        // 概率判定
        if (!shouldGive()) {
            return null;
        }

        try {
            // 计算随机奖励数量
            long amount = minAmount;
            if (maxAmount > minAmount) {
                amount = minAmount + random.nextLong(maxAmount - minAmount + 1);
            }

            // 获取玩家数据
            PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
            if (data == null) {
                plugin.getLogger().warning("[奇遇系统] 无法加载玩家数据: " + player.getName());
                return null;
            }

            // 根据类型发放奖励
            String message = switch (type.toLowerCase()) {
                case "qi" -> {
                    data.addQi(amount);
                    yield "§b修为 +" + amount;
                }

                case "spirit-stones" -> {
                    data.addSpiritStones(amount);
                    yield "§6灵石 +" + amount;
                }

                case "skill-points" -> {
                    data.addSkillPoints((int) amount);
                    yield "§5功法点 +" + amount;
                }

                case "contribution" -> {
                    data.addContribution((int) amount);
                    yield "§a贡献点 +" + amount;
                }

                case "active-qi" -> {
                    data.addActiveQi(amount);
                    yield "§e活跃灵气 +" + amount;
                }

                case "level" -> {
                    data.addLevel((int) amount);
                    yield "§d等级 +" + amount;
                }

                default -> {
                    plugin.getLogger().warning("[奇遇系统] 未知的数值奖励类型: " + type);
                    yield null;
                }
            };

            // 保存玩家数据
            if (message != null) {
                plugin.getDataManager().savePlayerData(data);
            }

            return message;

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 发放数值奖励失败 (" + type + "): " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 从配置Map创建数值奖励
     *
     * @param map    配置Map
     * @param plugin 插件实例
     * @param type   奖励类型
     * @return 数值奖励对象
     */
    public static NumericReward fromMap(Map<String, Object> map, XianCore plugin, String type) {
        // 支持多种配置格式:
        // 1. amount: 100
        // 2. amount: "50-100"
        // 3. min-amount: 50, max-amount: 100

        long minAmount = 0;
        long maxAmount = 0;

        try {
            if (map.containsKey("amount")) {
                Object amountObj = map.get("amount");
                
                if (amountObj instanceof Number) {
                    // 格式1: amount: 100
                    minAmount = maxAmount = ((Number) amountObj).longValue();
                } else if (amountObj instanceof String) {
                    String amountStr = (String) amountObj;
                    
                    if (amountStr.contains("-")) {
                        // 格式2: amount: "50-100"
                        String[] parts = amountStr.split("-");
                        if (parts.length == 2) {
                            minAmount = Long.parseLong(parts[0].trim());
                            maxAmount = Long.parseLong(parts[1].trim());
                        } else {
                            throw new IllegalArgumentException("无效的数量范围格式: " + amountStr);
                        }
                    } else {
                        // 格式: amount: "100"
                        minAmount = maxAmount = Long.parseLong(amountStr.trim());
                    }
                } else {
                    throw new IllegalArgumentException("amount 字段类型无效: " + amountObj.getClass().getName());
                }
            } else if (map.containsKey("min-amount") || map.containsKey("max-amount")) {
                // 格式3: min-amount: 50, max-amount: 100
                minAmount = getLong(map, "min-amount", 0);
                maxAmount = getLong(map, "max-amount", minAmount);
            } else {
                throw new IllegalArgumentException("数值奖励缺少 amount 或 min-amount/max-amount 字段");
            }

            // 验证数值范围
            if (minAmount < 0) {
                throw new IllegalArgumentException("最小值不能为负数: " + minAmount);
            }
            if (maxAmount < minAmount) {
                plugin.getLogger().warning("[奇遇系统] 最大值小于最小值，已自动调整: " + 
                                         minAmount + " - " + maxAmount);
                maxAmount = minAmount;
            }

            return new NumericReward(plugin, type, minAmount, maxAmount);

        } catch (Exception e) {
            throw new IllegalArgumentException("解析数值奖励配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从Map中读取Long值
     */
    private static long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object obj = map.get(key);
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * 获取奖励类型
     */
    public String getRewardType() {
        return type;
    }

    /**
     * 获取最小值（用于调试）
     */
    public long getMinAmount() {
        return minAmount;
    }

    /**
     * 获取最大值（用于调试）
     */
    public long getMaxAmount() {
        return maxAmount;
    }
}

