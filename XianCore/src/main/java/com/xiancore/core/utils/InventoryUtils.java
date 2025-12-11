package com.xiancore.core.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * 背包工具类
 * 提供统一的物品统计、扣除等操作
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public final class InventoryUtils {

    private InventoryUtils() {
        // 工具类禁止实例化
    }

    /**
     * 统计玩家背包中符合条件的物品数量
     *
     * @param player 玩家
     * @param filter 过滤条件
     * @return 物品总数量
     */
    public static int countItems(Player player, Predicate<ItemStack> filter) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && filter.test(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 统计玩家背包中指定材料的物品数量
     *
     * @param player   玩家
     * @param material 材料类型
     * @return 物品总数量
     */
    public static int countItems(Player player, Material material) {
        return countItems(player, item -> item.getType() == material);
    }

    /**
     * 从玩家背包移除指定数量的物品
     *
     * @param player   玩家
     * @param material 材料类型
     * @param amount   移除数量
     * @return 是否成功移除（背包中物品足够）
     */
    public static boolean removeItems(Player player, Material material, int amount) {
        // 先检查数量是否足够
        if (countItems(player, material) < amount) {
            return false;
        }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        return true;
    }

    /**
     * 从玩家背包移除符合条件的物品
     *
     * @param player 玩家
     * @param filter 过滤条件
     * @param amount 移除数量
     * @return 是否成功移除
     */
    public static boolean removeItems(Player player, Predicate<ItemStack> filter, int amount) {
        // 先检查数量是否足够
        if (countItems(player, filter) < amount) {
            return false;
        }

        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && filter.test(item)) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        return true;
    }

    /**
     * 检查玩家背包是否有空位
     *
     * @param player 玩家
     * @return 是否有空位
     */
    public static boolean hasEmptySlot(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }

    /**
     * 获取玩家背包空位数量
     *
     * @param player 玩家
     * @return 空位数量
     */
    public static int getEmptySlots(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 安全地给予玩家物品（如果背包满则掉落）
     *
     * @param player 玩家
     * @param item   物品
     */
    public static void giveItemSafely(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            // 背包满了，掉落在地上
            leftover.values().forEach(dropItem ->
                    player.getWorld().dropItemNaturally(player.getLocation(), dropItem)
            );
        }
    }
}
