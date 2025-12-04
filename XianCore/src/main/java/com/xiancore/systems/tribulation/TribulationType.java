package com.xiancore.systems.tribulation;

import lombok.Getter;

/**
 * 天劫类型枚举
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public enum TribulationType {

    // 根据境界划分的天劫
    QI_CONDENSATION("凝气劫", 1, "炼气期突破到筑基期的天劫", 1),
    FOUNDATION("筑基劫", 2, "筑基期突破到金丹期的天劫", 3),
    GOLDEN_CORE("金丹劫", 3, "金丹期突破到元婴期的天劫", 6),
    NASCENT_SOUL("元婴劫", 4, "元婴期突破到化神期的天劫", 9),
    SOUL_FORMATION("化神劫", 5, "化神期突破到炼虚期的天劫", 18),
    VOID_REFINEMENT("炼虚劫", 6, "炼虚期突破到合体期的天劫", 27),
    INTEGRATION("合体劫", 7, "合体期突破到大乘期的天劫", 36),
    MAHAYANA("大乘劫", 8, "大乘期突破到渡劫期的天劫", 49),
    TRANSCENDENCE("飞升劫", 9, "渡劫期飞升的终极天劫", 81);

    private final String displayName;      // 显示名称
    private final int tier;                // 劫数等阶
    private final String description;      // 描述
    private final int waves;               // 劫雷波数

    TribulationType(String displayName, int tier, String description, int waves) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
        this.waves = waves;
    }

    /**
     * 根据境界获取对应的天劫类型
     */
    public static TribulationType fromRealm(String realm) {
        return switch (realm) {
            case "炼气期" -> QI_CONDENSATION;
            case "筑基期" -> FOUNDATION;
            case "结丹期" -> GOLDEN_CORE;
            case "元婴期" -> NASCENT_SOUL;
            case "化神期" -> SOUL_FORMATION;
            case "炼虚期" -> VOID_REFINEMENT;
            case "合体期" -> INTEGRATION;
            case "大乘期" -> MAHAYANA;
            default -> null;
        };
    }

    /**
     * 计算天劫难度系数
     */
    public double getDifficultyMultiplier() {
        return 1.0 + (tier - 1) * 0.5;
    }

    /**
     * 获取劫雷伤害
     */
    public double getLightningDamage(int wave) {
        double baseDamage = 10.0 * tier;
        double waveMultiplier = 1.0 + (wave - 1) * 0.2;
        return baseDamage * waveMultiplier * getDifficultyMultiplier();
    }

    // ==================== 显式 Getter 方法 ====================

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }

    public String getDescription() {
        return description;
    }

    public int getWaves() {
        return waves;
    }
}
