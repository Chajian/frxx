package com.xiancore.gui.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI 工具类
 * 提供创建物品的便捷方法
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    /**
     * 创建物品构建器
     */
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    /**
     * 创建物品构建器（指定数量）
     */
    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
    }

    /**
     * 设置显示名称
     */
    public ItemBuilder name(String name) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(name);
        }
        return this;
    }

    /**
     * 设置物品数量
     */
    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * 设置 Lore（描述）
     */
    public ItemBuilder lore(String... lore) {
        if (itemMeta != null) {
            itemMeta.setLore(Arrays.asList(lore));
        }
        return this;
    }

    /**
     * 设置 Lore（列表形式）
     */
    public ItemBuilder lore(List<String> lore) {
        if (itemMeta != null) {
            itemMeta.setLore(lore);
        }
        return this;
    }

    /**
     * 添加单行 Lore
     */
    public ItemBuilder addLoreLine(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
            lore.add(line);
            itemMeta.setLore(lore);
        }
        return this;
    }

    /**
     * 添加附魔效果
     */
    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    /**
     * 添加发光效果（无实际附魔）
     */
    public ItemBuilder glow() {
        if (itemMeta != null) {
            itemMeta.addEnchant(Enchantment.LURE, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    /**
     * 设置不可破坏
     */
    public ItemBuilder unbreakable() {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(true);
            itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        return this;
    }

    /**
     * 隐藏所有标志
     */
    public ItemBuilder hideFlags() {
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    /**
     * 设置自定义模型数据
     */
    public ItemBuilder customModelData(int data) {
        if (itemMeta != null) {
            itemMeta.setCustomModelData(data);
        }
        return this;
    }

    /**
     * 构建最终物品
     */
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    /**
     * 快捷方法：创建一个简单的物品
     */
    public static ItemStack create(Material material, String name, String... lore) {
        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build();
    }

    /**
     * 快捷方法：创建一个发光的物品
     */
    public static ItemStack createGlowing(Material material, String name, String... lore) {
        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .glow()
                .build();
    }
}
