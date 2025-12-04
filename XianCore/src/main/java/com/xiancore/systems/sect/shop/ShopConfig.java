package com.xiancore.systems.sect.shop;

import org.bukkit.Material;

import java.util.*;

/**
 * 商店配置
 * 定义所有可兑换的物品
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ShopConfig {

    private static final List<ShopItem> SHOP_ITEMS = new ArrayList<>();

    static {
        initializeShopItems();
    }

    /**
     * 初始化商店物品
     */
    private static void initializeShopItems() {
        // ========== 资源类 ==========
        SHOP_ITEMS.add(new ShopItem("spirit_stone_small", "\u00a7e\u5c0f\u578b\u7075\u77f3",
                Material.EMERALD, 10, 50, "resource")
                .addLore("\u00a77\u57fa\u7840\u4fee\u70bc\u8d44\u6e90")
                .addLore("\u00a77\u6570\u91cf: 10\u4e2a"));

        SHOP_ITEMS.add(new ShopItem("spirit_stone_medium", "\u00a7b\u4e2d\u578b\u7075\u77f3",
                Material.DIAMOND, 5, 100, "resource")
                .addLore("\u00a77\u9ad8\u7ea7\u4fee\u70bc\u8d44\u6e90")
                .addLore("\u00a77\u6570\u91cf: 5\u4e2a"));

        SHOP_ITEMS.add(new ShopItem("exp_bottle", "\u00a7a\u7ecf\u9a8c\u74f6",
                Material.EXPERIENCE_BOTTLE, 16, 30, "resource")
                .addLore("\u00a77\u63d0\u4f9b\u7ecf\u9a8c\u503c")
                .addLore("\u00a77\u6570\u91cf: 16\u4e2a"));

        // ========== 材料类 ==========
        SHOP_ITEMS.add(new ShopItem("iron_ingot", "\u00a7f\u94c1\u952d",
                Material.IRON_INGOT, 32, 20, "material")
                .addLore("\u00a77\u57fa\u7840\u5236\u4f5c\u6750\u6599")
                .addLore("\u00a77\u6570\u91cf: 32\u4e2a"));

        SHOP_ITEMS.add(new ShopItem("gold_ingot", "\u00a7e\u91d1\u952d",
                Material.GOLD_INGOT, 16, 40, "material")
                .addLore("\u00a77\u7a00\u6709\u5236\u4f5c\u6750\u6599")
                .addLore("\u00a77\u6570\u91cf: 16\u4e2a"));

        SHOP_ITEMS.add(new ShopItem("diamond", "\u00a7b\u94bb\u77f3",
                Material.DIAMOND, 8, 80, "material")
                .addLore("\u00a77\u73cd\u8d35\u5236\u4f5c\u6750\u6599")
                .addLore("\u00a77\u6570\u91cf: 8\u4e2a"));

        SHOP_ITEMS.add(new ShopItem("netherite_ingot", "\u00a75\u4e0b\u754c\u5408\u91d1\u952d",
                Material.NETHERITE_INGOT, 1, 200, "material")
                .addLore("\u00a77\u6700\u9ad8\u7ea7\u5236\u4f5c\u6750\u6599")
                .addLore("\u00a77\u6570\u91cf: 1\u4e2a"));

        // ========== 装备类 ==========
        SHOP_ITEMS.add(new ShopItem("iron_armor_set", "\u00a7f\u94c1\u5957\u88c5",
                Material.IRON_CHESTPLATE, 1, 150, "equipment")
                .addLore("\u00a77\u5305\u542b\u5168\u5957\u94c1\u88c5\u5907")
                .addLore("\u00a7c\u6ce8\u610f: \u9700\u8981\u80cc\u5305\u7a7a\u4f4d"));

        SHOP_ITEMS.add(new ShopItem("diamond_sword", "\u00a7b\u94bb\u77f3\u5251",
                Material.DIAMOND_SWORD, 1, 120, "equipment")
                .addLore("\u00a77\u9ad8\u7ea7\u6b66\u5668")
                .addLore("\u00a77\u9549\u5229 V"));

        // ========== 特殊类 ==========
        SHOP_ITEMS.add(new ShopItem("totem_of_undying", "\u00a7e\u4e0d\u6b7b\u56fe\u817e",
                Material.TOTEM_OF_UNDYING, 1, 500, "special")
                .addLore("\u00a77\u6781\u5176\u73cd\u8d35\u7684\u7269\u54c1")
                .addLore("\u00a77\u53ef\u4ee5\u6297\u62d2\u4e00\u6b21\u81f4\u547d\u4f24\u5bb3"));

        SHOP_ITEMS.add(new ShopItem("elytra", "\u00a7d\u978b\u7fc5",
                Material.ELYTRA, 1, 800, "special")
                .addLore("\u00a77\u6781\u5176\u73cd\u8d35\u7684\u7269\u54c1")
                .addLore("\u00a77\u5141\u8bb8\u6ed1\u7fd4\u98de\u884c"));

        SHOP_ITEMS.add(new ShopItem("enchanted_golden_apple", "\u00a76\u9644\u9b54\u91d1\u82f9\u679c",
                Material.ENCHANTED_GOLDEN_APPLE, 1, 300, "special")
                .addLore("\u00a77\u795e\u5947\u7684\u6cbb\u7597\u7269\u54c1")
                .addLore("\u00a77\u63d0\u4f9b\u5f3a\u5927\u7684\u589e\u76ca\u6548\u679c"));

        SHOP_ITEMS.add(new ShopItem("beacon", "\u00a7e\u4fe1\u6807",
                Material.BEACON, 1, 1000, "special")
                .addLore("\u00a77\u7ec8\u6781\u5b9d\u7269")
                .addLore("\u00a77\u63d0\u4f9b\u8303\u56f4\u589e\u76ca\u6548\u679c"));
    }

    /**
     * 获取所有商店物品
     */
    public static List<ShopItem> getAllItems() {
        return new ArrayList<>(SHOP_ITEMS);
    }

    /**
     * 根据ID获取物品
     */
    public static ShopItem getItemById(String id) {
        for (ShopItem item : SHOP_ITEMS) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 根据分类获取物品
     */
    public static List<ShopItem> getItemsByCategory(String category) {
        List<ShopItem> items = new ArrayList<>();
        for (ShopItem item : SHOP_ITEMS) {
            if (item.getCategory().equals(category)) {
                items.add(item);
            }
        }
        return items;
    }
}
