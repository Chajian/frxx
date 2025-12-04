package com.xiancore.systems.fate.rewards;

import com.xiancore.XianCore;
import com.xiancore.systems.forge.items.Equipment;
import com.xiancore.systems.forge.items.EquipmentType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 装备奖励
 * 发放随机装备给玩家
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class EquipmentReward extends FateReward {

    private String quality;           // 装备品质
    private EquipmentType equipmentType;  // 装备类型
    private int minLevel;             // 最小等级（强化等级）
    private int maxLevel;             // 最大等级（强化等级）
    private final Random random = new Random();

    public EquipmentReward(XianCore plugin) {
        super(plugin, "equipment");
        this.quality = "凡品";  // 默认品质
        this.minLevel = 0;
        this.maxLevel = 0;
    }

    @Override
    public String give(Player player) {
        // 概率判定
        if (!shouldGive()) {
            return null;
        }

        try {
            // 创建装备
            Equipment equipment = createEquipment();
            if (equipment == null) {
                plugin.getLogger().warning("[奇遇系统] 创建装备失败");
                return null;
            }

            // 转换为 ItemStack（带 PDC 数据）
            ItemStack item = equipment.toItemStack(plugin);
            if (item == null) {
                plugin.getLogger().warning("[奇遇系统] 装备转换为物品失败");
                return null;
            }

            // 检查背包空间
            if (player.getInventory().firstEmpty() == -1) {
                // 背包满，掉落到地面
                player.sendMessage("§c背包已满！装备掉落在地上");
                player.getWorld().dropItem(player.getLocation(), item);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().fine("[奇遇系统] 玩家 " + player.getName() + " 背包满，装备已掉落");
                }
            } else {
                // 背包有空间，直接添加
                player.getInventory().addItem(item);
            }

            // 返回奖励消息
            String enhanceStr = equipment.getEnhanceLevel() > 0 ? " §7(+" + equipment.getEnhanceLevel() + ")" : "";
            return "§d装备: §e" + quality + equipment.getType().getDisplayName() + enhanceStr;

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 发放装备奖励失败: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 创建装备对象
     */
    private Equipment createEquipment() {
        try {
            Equipment equipment = new Equipment();

            // 生成UUID
            equipment.setUuid(UUID.randomUUID().toString());

            // 设置装备类型
            equipment.setType(equipmentType != null ? equipmentType : getRandomEquipmentType());

            // 设置品质
            equipment.setQuality(quality);

            // 设置五行属性（随机）
            String[] elements = {"金", "木", "水", "火", "土"};
            equipment.setElement(elements[random.nextInt(elements.length)]);

            // 根据品质计算基础属性
            int baseValue = getQualityBaseValue(quality);
            EquipmentType type = equipment.getType();

            int attackBonus = type.isWeapon() ? 2 : 1;
            int defenseBonus = type.isArmor() ? 2 : 1;
            int hpBonus = type.isArmor() ? 2 : 1;
            int qiBonus = type.isAccessory() ? 2 : 1;

            equipment.setBaseAttack(applyRandomVariance(baseValue * attackBonus));
            equipment.setBaseDefense(applyRandomVariance(baseValue * defenseBonus));
            equipment.setBaseHp(applyRandomVariance(baseValue * 10 * hpBonus));
            equipment.setBaseQi(applyRandomVariance(baseValue * 5 * qiBonus));

            // 设置强化等级
            int enhanceLevel = minLevel;
            if (maxLevel > minLevel) {
                enhanceLevel = minLevel + random.nextInt(maxLevel - minLevel + 1);
            }
            equipment.setEnhanceLevel(enhanceLevel);

            // 设置耐久
            equipment.setDurability(100);

            return equipment;

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 创建装备对象失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据品质获取基础属性值
     */
    private int getQualityBaseValue(String quality) {
        return switch (quality) {
            case "神品" -> 100;
            case "仙品" -> 80;
            case "宝品" -> 60;
            case "灵品" -> 40;
            default -> 20;  // 凡品
        };
    }

    /**
     * 获取随机装备类型
     */
    private EquipmentType getRandomEquipmentType() {
        EquipmentType[] allTypes = EquipmentType.values();
        return allTypes[random.nextInt(allTypes.length)];
    }

    /**
     * 应用随机波动（±10%）
     */
    private int applyRandomVariance(int baseValue) {
        if (baseValue == 0) {
            return 0;
        }
        double variance = 0.9 + (random.nextDouble() * 0.2);
        return (int)(baseValue * variance);
    }

    /**
     * 从配置Map创建装备奖励
     *
     * @param map    配置Map
     * @param plugin 插件实例
     * @return 装备奖励对象
     */
    public static EquipmentReward fromMap(Map<String, Object> map, XianCore plugin) {
        EquipmentReward reward = new EquipmentReward(plugin);

        // 读取品质（可选）
        if (map.containsKey("quality")) {
            String qualityStr = (String) map.get("quality");
            if (qualityStr != null && !qualityStr.isEmpty()) {
                // 支持随机品质
                if ("random".equalsIgnoreCase(qualityStr)) {
                    String[] qualities = {"凡品", "灵品", "宝品", "仙品", "神品"};
                    reward.quality = qualities[new Random().nextInt(qualities.length)];
                } else {
                    reward.quality = qualityStr;
                }
            }
        }

        // 读取装备类型（可选）
        if (map.containsKey("equipment-type")) {
            String typeStr = (String) map.get("equipment-type");
            try {
                reward.equipmentType = EquipmentType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[奇遇系统] 无效的装备类型: " + typeStr + "，将随机选择");
            }
        }

        // 读取等级范围（可选）
        if (map.containsKey("level")) {
            Object levelObj = map.get("level");
            
            if (levelObj instanceof Number) {
                // 固定等级
                reward.minLevel = reward.maxLevel = ((Number) levelObj).intValue();
            } else if (levelObj instanceof String) {
                String levelStr = (String) levelObj;
                if (levelStr.contains("-")) {
                    // 范围格式: "10-20"
                    String[] parts = levelStr.split("-");
                    if (parts.length == 2) {
                        try {
                            reward.minLevel = Integer.parseInt(parts[0].trim());
                            reward.maxLevel = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("[奇遇系统] 等级范围格式错误: " + levelStr);
                        }
                    }
                } else {
                    // 单个数值字符串
                    try {
                        reward.minLevel = reward.maxLevel = Integer.parseInt(levelStr.trim());
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("[奇遇系统] 等级格式错误: " + levelStr);
                    }
                }
            }
        } else if (map.containsKey("min-level") || map.containsKey("max-level")) {
            // 分别指定
            reward.minLevel = getInt(map, "min-level", 0);
            reward.maxLevel = getInt(map, "max-level", reward.minLevel);
        }

        // 验证等级范围
        if (reward.minLevel < 0) reward.minLevel = 0;
        if (reward.maxLevel < reward.minLevel) reward.maxLevel = reward.minLevel;
        if (reward.maxLevel > 30) {
            plugin.getLogger().warning("[奇遇系统] 装备等级过高: " + reward.maxLevel + "，已修正为30");
            reward.maxLevel = 30;
        }

        return reward;
    }

    /**
     * 从Map中读取int值
     */
    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object obj = map.get(key);
        if (obj == null) {
            return defaultValue;
        }

        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }
}


