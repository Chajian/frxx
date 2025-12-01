package com.xiancore.boss.system.model;

import lombok.Getter;

/**
 * Boss等级枚举
 * 定义Boss的4个等级及其对应的属性
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public enum BossTier {
    /**
     * 等级1: 普通Boss
     * 难度: 单人/双人可应对
     * 修为倍数: 0.5倍 (相对于普通怪物)
     * 刷新频率: 每2-3小时
     */
    NORMAL(1, 0.5, "普通Boss", "§6普通", 5000, 50, 10),

    /**
     * 等级2: 精英Boss
     * 难度: 小队(3-5人)
     * 修为倍数: 2.0倍
     * 刷新频率: 每4-6小时
     */
    ELITE(2, 2.0, "精英Boss", "§e精英", 15000, 80, 30),

    /**
     * 等级3: 区域Boss
     * 难度: 团队(5-10人)
     * 修为倍数: 3.0倍
     * 刷新频率: 每日固定时间
     */
    BOSS(3, 3.0, "世界Boss", "§c世界", 50000, 120, 50),

    /**
     * 等级4: 传奇Boss
     * 难度: 多团队(10+人)
     * 修为倍数: 5.0倍
     * 刷新频率: 每周一次
     */
    LEGENDARY(4, 5.0, "传奇Boss", "§4传奇", 200000, 200, 80);

    // ==================== 字段 ====================

    /** Boss等级 (1-4) */
    private final int level;

    /** 修为倍数 (相对于普通怪物) */
    private final double cultivationMultiplier;

    /** 显示名称 (英文) */
    private final String displayName;

    /** 显示前缀 (包含颜色代码) */
    private final String coloredPrefix;

    /** 推荐血量 */
    private final int recommendedHealth;

    /** 推荐伤害 */
    private final int recommendedDamage;

    /** 推荐护甲 */
    private final int recommendedArmor;

    // ==================== 构造函数 ====================

    BossTier(int level, double cultivationMultiplier, String displayName,
             String coloredPrefix, int recommendedHealth, int recommendedDamage,
             int recommendedArmor) {
        this.level = level;
        this.cultivationMultiplier = cultivationMultiplier;
        this.displayName = displayName;
        this.coloredPrefix = coloredPrefix;
        this.recommendedHealth = recommendedHealth;
        this.recommendedDamage = recommendedDamage;
        this.recommendedArmor = recommendedArmor;
    }

    // ==================== 静态方法 ====================

    /**
     * 根据等级获取BossTier
     *
     * @param level 等级 (1-4)
     * @return 对应的BossTier，如果无效则返回null
     */
    public static BossTier fromLevel(int level) {
        for (BossTier tier : values()) {
            if (tier.level == level) {
                return tier;
            }
        }
        return null;
    }

    /**
     * 获取最高等级
     */
    public static BossTier getMaxTier() {
        return LEGENDARY;
    }

    /**
     * 获取最低等级
     */
    public static BossTier getMinTier() {
        return NORMAL;
    }

    /**
     * 获取下一个等级
     *
     * @return 下一个等级，如果已是最高级则返回自身
     */
    public BossTier getNextTier() {
        if (level >= 4) {
            return this;
        }
        return fromLevel(level + 1);
    }

    /**
     * 获取上一个等级
     *
     * @return 上一个等级，如果已是最低级则返回自身
     */
    public BossTier getPreviousTier() {
        if (level <= 1) {
            return this;
        }
        return fromLevel(level - 1);
    }

    // ==================== 比较方法 ====================

    /**
     * 是否比另一个等级高
     */
    public boolean isHigherThan(BossTier other) {
        return this.level > other.level;
    }

    /**
     * 是否比另一个等级低
     */
    public boolean isLowerThan(BossTier other) {
        return this.level < other.level;
    }

    /**
     * 是否与另一个等级相同
     */
    public boolean isSameAs(BossTier other) {
        return this.level == other.level;
    }

    // ==================== 信息方法 ====================

    /**
     * 获取等级信息字符串
     */
    @Override
    public String toString() {
        return String.format("%s (Lv.%d)", displayName, level);
    }

    /**
     * 获取详细信息
     */
    public String getDetailedInfo() {
        return String.format(
            "BossTier{\n" +
            "  级别: %d\n" +
            "  名称: %s\n" +
            "  修为倍数: %.1fx\n" +
            "  推荐血量: %d\n" +
            "  推荐伤害: %d\n" +
            "  推荐护甲: %d\n" +
            "}",
            level, displayName, cultivationMultiplier,
            recommendedHealth, recommendedDamage, recommendedArmor
        );
    }

    /**
     * 获取血量倍数
     * 根据等级计算血量倍数，等级越高血量倍数越大
     *
     * @return 血量倍数 (等级 * 10)
     */
    public int getHealthMultiplier() {
        return level * 10;
    }

    /**
     * 获取当前等级
     * 返回BossTier自身
     *
     * @return 当前BossTier实例
     */
    public BossTier getTier() {
        return this;
    }
}
