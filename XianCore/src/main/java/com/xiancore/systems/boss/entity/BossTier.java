package com.xiancore.systems.boss.entity;

import lombok.Getter;

/**
 * Boss 等级枚举
 * 定义 Boss 的不同难度等级及其属性
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public enum BossTier {

    /**
     * 普通 Boss (Tier 1)
     */
    NORMAL(
        1,
        "§f普通",
        "§f【普通】",
        1.0,
        1000.0,
        50.0,
        10.0
    ),

    /**
     * 精英 Boss (Tier 2)
     */
    ELITE(
        2,
        "§a精英",
        "§a【精英】",
        1.5,
        5000.0,
        150.0,
        30.0
    ),

    /**
     * Boss (Tier 3)
     */
    BOSS(
        3,
        "§c首领",
        "§c【首领】",
        2.5,
        20000.0,
        500.0,
        100.0
    ),

    /**
     * 传说 Boss (Tier 4)
     */
    LEGENDARY(
        4,
        "§6§l传说",
        "§6§l【传说】",
        5.0,
        100000.0,
        2000.0,
        500.0
    );

    // ==================== 属性字段 ====================

    /** 等级数值 (1-4) */
    private final int level;

    /** 显示名称 */
    private final String displayName;

    /** 带颜色的前缀 */
    private final String coloredPrefix;

    /** 修为倍数 */
    private final double cultivationMultiplier;

    /** 推荐生命值 */
    private final double recommendedHealth;

    /** 推荐伤害 */
    private final double recommendedDamage;

    /** 推荐护甲 */
    private final double recommendedArmor;

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param level 等级
     * @param displayName 显示名称
     * @param coloredPrefix 颜色前缀
     * @param cultivationMultiplier 修为倍数
     * @param recommendedHealth 推荐生命
     * @param recommendedDamage 推荐伤害
     * @param recommendedArmor 推荐护甲
     */
    BossTier(int level, String displayName, String coloredPrefix,
             double cultivationMultiplier, double recommendedHealth,
             double recommendedDamage, double recommendedArmor) {
        this.level = level;
        this.displayName = displayName;
        this.coloredPrefix = coloredPrefix;
        this.cultivationMultiplier = cultivationMultiplier;
        this.recommendedHealth = recommendedHealth;
        this.recommendedDamage = recommendedDamage;
        this.recommendedArmor = recommendedArmor;
    }

    // ==================== 工具方法 ====================

    /**
     * 根据等级数值获取对应的 BossTier
     *
     * @param level 等级 (1-4)
     * @return BossTier，如果无效则返回 NORMAL
     */
    public static BossTier fromLevel(int level) {
        for (BossTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return NORMAL;
    }

    /**
     * 根据名称获取对应的 BossTier
     *
     * @param name 名称
     * @return BossTier，如果无效则返回 NORMAL
     */
    public static BossTier fromName(String name) {
        if (name == null) return NORMAL;
        
        for (BossTier tier : values()) {
            if (tier.name().equalsIgnoreCase(name) || 
                tier.displayName.equals(name)) {
                return tier;
            }
        }
        return NORMAL;
    }

    /**
     * 获取下一个等级
     *
     * @return 下一个等级，如果已是最高则返回自身
     */
    public BossTier next() {
        if (this == LEGENDARY) return LEGENDARY;
        return fromLevel(this.level + 1);
    }

    /**
     * 获取上一个等级
     *
     * @return 上一个等级，如果已是最低则返回自身
     */
    public BossTier previous() {
        if (this == NORMAL) return NORMAL;
        return fromLevel(this.level - 1);
    }

    /**
     * 是否为最高等级
     *
     * @return 是否为传说等级
     */
    public boolean isMaxTier() {
        return this == LEGENDARY;
    }

    /**
     * 是否为最低等级
     *
     * @return 是否为普通等级
     */
    public boolean isMinTier() {
        return this == NORMAL;
    }

    /**
     * 获取信息字符串
     *
     * @return 信息字符串
     */
    public String getInfo() {
        return String.format("%s (等级%d) - 修为x%.1f",
            displayName, level, cultivationMultiplier);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
