package com.xiancore.systems.skill;

import lombok.Getter;

/**
 * 功法五行属性枚举
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public enum SkillElement {

    FIRE("火", "§c", "火系功法，爆发力强"),
    WATER("水", "§9", "水系功法，连绵不绝"),
    WOOD("木", "§a", "木系功法，生生不息"),
    METAL("金", "§f", "金系功法，锋芒毕露"),
    EARTH("土", "§6", "土系功法，厚重坚实"),

    // 复合属性
    THUNDER("雷", "§e", "雷系功法，迅猛无比"),
    ICE("冰", "§b", "冰系功法，冻结一切"),
    WIND("风", "§7", "风系功法，飘忽不定"),

    // 特殊属性
    LIGHT("光", "§f", "光系功法，驱散黑暗"),
    DARK("暗", "§8", "暗系功法，吞噬万物"),
    NEUTRAL("无", "§7", "无属性功法，平衡发展");

    private final String displayName;
    private final String colorCode;
    private final String description;

    SkillElement(String displayName, String colorCode, String description) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.description = description;
    }

    /**
     * 根据名称获取属性
     */
    public static SkillElement fromString(String name) {
        for (SkillElement element : values()) {
            if (element.name().equalsIgnoreCase(name) || element.displayName.equals(name)) {
                return element;
            }
        }
        return NEUTRAL; // 默认
    }

    /**
     * 获取带颜色的显示名称
     */
    public String getColoredName() {
        return colorCode + displayName;
    }

    /**
     * 计算相克系数
     * 火克金，金克木，木克土，土克水，水克火
     */
    public double getDamageMultiplier(SkillElement target) {
        if (target == null) {
            return 1.0;
        }

        // 相克关系：伤害增加50%
        if (this == FIRE && target == METAL) return 1.5;
        if (this == METAL && target == WOOD) return 1.5;
        if (this == WOOD && target == EARTH) return 1.5;
        if (this == EARTH && target == WATER) return 1.5;
        if (this == WATER && target == FIRE) return 1.5;

        // 相生关系：伤害减少30%
        if (this == FIRE && target == WATER) return 0.7;
        if (this == WATER && target == EARTH) return 0.7;
        if (this == EARTH && target == WOOD) return 0.7;
        if (this == WOOD && target == METAL) return 0.7;
        if (this == METAL && target == FIRE) return 0.7;

        // 同属性：伤害减少20%
        if (this == target) return 0.8;

        // 其他情况：正常伤害
        return 1.0;
    }
}
