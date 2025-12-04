package com.xiancore.systems.fate.rewards;

import com.xiancore.XianCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 物品奖励
 * 发放指定材质和数量的物品给玩家
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ItemReward extends FateReward {

    private final Material material;
    private final int amount;
    private String displayName;
    private List<String> lore;

    public ItemReward(XianCore plugin, Material material, int amount) {
        super(plugin, "item");
        this.material = material;
        this.amount = amount;
        this.lore = new ArrayList<>();
    }

    @Override
    public String give(Player player) {
        // 概率判定
        if (!shouldGive()) {
            return null;
        }

        try {
            // 创建物品
            ItemStack item = createItemStack();
            if (item == null) {
                plugin.getLogger().warning("[奇遇系统] 创建物品失败: " + material);
                return null;
            }

            // 检查背包空间
            if (player.getInventory().firstEmpty() == -1) {
                // 背包满，掉落到地面
                player.sendMessage("§c背包已满！物品掉落在地上");
                player.getWorld().dropItem(player.getLocation(), item);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().fine("[奇遇系统] 玩家 " + player.getName() + " 背包满，物品已掉落");
                }
            } else {
                // 背包有空间，直接添加
                player.getInventory().addItem(item);
            }

            // 返回奖励消息
            String itemName = displayName != null ? displayName : 
                             material.toString().replace("_", " ");
            return "§f" + itemName + " §7×" + amount;

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 发放物品奖励失败 (" + material + "): " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 创建物品
     */
    private ItemStack createItemStack() {
        try {
            ItemStack item = new ItemStack(material, amount);

            // 如果有自定义名称或Lore，应用到物品
            if (displayName != null || (lore != null && !lore.isEmpty())) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (displayName != null) {
                        meta.setDisplayName(displayName);
                    }
                    if (lore != null && !lore.isEmpty()) {
                        meta.setLore(lore);
                    }
                    item.setItemMeta(meta);
                }
            }

            return item;

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 创建物品失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从配置Map创建物品奖励
     *
     * @param map    配置Map
     * @param plugin 插件实例
     * @return 物品奖励对象
     */
    public static ItemReward fromMap(Map<String, Object> map, XianCore plugin) {
        // 读取材质（必需）
        String materialStr = (String) map.get("material");
        if (materialStr == null || materialStr.isEmpty()) {
            throw new IllegalArgumentException("物品奖励缺少 material 字段");
        }

        // 验证并转换材质
        Material material;
        try {
            material = Material.valueOf(materialStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的材质: " + materialStr);
        }

        // 读取数量（可选，默认1）
        int amount = 1;
        if (map.containsKey("amount")) {
            Object amountObj = map.get("amount");
            if (amountObj instanceof Number) {
                amount = ((Number) amountObj).intValue();
            } else if (amountObj instanceof String) {
                try {
                    amount = Integer.parseInt((String) amountObj);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[奇遇系统] 物品数量格式错误: " + amountObj + "，使用默认值1");
                    amount = 1;
                }
            }
        }

        // 验证数量范围
        if (amount <= 0) {
            plugin.getLogger().warning("[奇遇系统] 物品数量无效: " + amount + "，使用默认值1");
            amount = 1;
        } else if (amount > 64) {
            plugin.getLogger().warning("[奇遇系统] 物品数量超过64: " + amount + "，已修正为64");
            amount = 64;
        }

        ItemReward reward = new ItemReward(plugin, material, amount);

        // 读取显示名称（可选）
        if (map.containsKey("display-name")) {
            Object nameObj = map.get("display-name");
            if (nameObj != null) {
                reward.displayName = nameObj.toString();
            }
        }

        // 读取Lore描述（可选）
        if (map.containsKey("lore")) {
            Object loreObj = map.get("lore");
            if (loreObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> loreList = (List<String>) loreObj;
                reward.lore = new ArrayList<>(loreList);
            } else if (loreObj instanceof String) {
                // 如果是单行字符串，也支持
                reward.lore = new ArrayList<>();
                reward.lore.add(loreObj.toString());
            }
        }

        return reward;
    }

    /**
     * 获取材质（用于调试和验证）
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * 获取数量（用于调试和验证）
     */
    public int getAmount() {
        return amount;
    }

    /**
     * 获取显示名称（用于调试和验证）
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置显示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 设置Lore
     */
    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    /**
     * 添加一行Lore
     */
    public void addLore(String line) {
        if (this.lore == null) {
            this.lore = new ArrayList<>();
        }
        this.lore.add(line);
    }
}


