package com.xiancore.systems.boss.reward;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个奖励项
 * 
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
public class Reward {
    
    private final RewardType type;
    private final Object value;
    private final double chance; // 0.0-1.0
    private final String displayName;
    
    public Reward(RewardType type, Object value, double chance, String displayName) {
        this.type = type;
        this.value = value;
        this.chance = Math.max(0.0, Math.min(1.0, chance));
        this.displayName = displayName;
    }
    
    public RewardType getType() {
        return type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public double getChance() {
        return chance;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取整数值（用于经验奖励）
     */
    public int getIntValue() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    /**
     * 获取双精度浮点值（用于金钱奖励）
     */
    public double getDoubleValue() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    /**
     * 获取字符串值（用于命令、MythicMobs物品ID）
     */
    public String getStringValue() {
        return value != null ? value.toString() : "";
    }
    
    /**
     * 获取物品栈（用于物品奖励）
     */
    public ItemStack getItemStack() {
        if (value instanceof ItemStack) {
            return (ItemStack) value;
        }
        return null;
    }
    
    /**
     * 是否触发（基于概率）
     */
    public boolean shouldGive() {
        return Math.random() < chance;
    }
    
    /**
     * Builder 模式创建奖励
     */
    public static class Builder {
        private RewardType type;
        private Object value;
        private double chance = 1.0;
        private String displayName;
        
        public Builder type(RewardType type) {
            this.type = type;
            return this;
        }
        
        public Builder value(Object value) {
            this.value = value;
            return this;
        }
        
        public Builder chance(double chance) {
            this.chance = chance;
            return this;
        }
        
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Reward build() {
            if (type == null) {
                throw new IllegalArgumentException("Reward type cannot be null");
            }
            if (displayName == null) {
                displayName = type.getDisplayName();
            }
            return new Reward(type, value, chance, displayName);
        }
    }
    
    /**
     * 快速创建经验奖励
     */
    public static Reward experience(int amount) {
        return new Builder()
            .type(RewardType.EXPERIENCE)
            .value(amount)
            .displayName("经验 +" + amount)
            .build();
    }
    
    /**
     * 快速创建金钱奖励
     */
    public static Reward money(double amount) {
        return new Builder()
            .type(RewardType.MONEY)
            .value(amount)
            .displayName(String.format("金钱 +%.2f", amount))
            .build();
    }
    
    /**
     * 快速创建物品奖励
     */
    public static Reward item(ItemStack item) {
        return new Builder()
            .type(RewardType.ITEM)
            .value(item)
            .displayName("物品 x" + item.getAmount())
            .build();
    }
    
    /**
     * 快速创建命令奖励
     */
    public static Reward command(String command) {
        return new Builder()
            .type(RewardType.COMMAND)
            .value(command)
            .displayName("特殊奖励")
            .build();
    }
    
    /**
     * 快速创建MythicMobs物品奖励
     */
    public static Reward mythicItem(String itemId, int amount) {
        return new Builder()
            .type(RewardType.MYTHIC_ITEM)
            .value(itemId + ":" + amount)
            .displayName("神话物品 x" + amount)
            .build();
    }
}
