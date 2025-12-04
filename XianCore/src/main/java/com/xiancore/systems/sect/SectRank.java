package com.xiancore.systems.sect;

import lombok.Getter;

/**
 * 宗门职位枚举
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public enum SectRank {

    LEADER("宗主", 5, "§c", "宗门最高领导者"),
    ELDER("长老", 4, "§6", "宗门管理层"),
    CORE_DISCIPLE("核心弟子", 3, "§d", "宗门核心成员"),
    INNER_DISCIPLE("内门弟子", 2, "§b", "宗门正式成员"),
    OUTER_DISCIPLE("外门弟子", 1, "§a", "宗门普通成员");

    private final String displayName;     // 显示名称
    private final int level;              // 职位等级
    private final String colorCode;       // 颜色代码
    private final String description;     // 描述

    SectRank(String displayName, int level, String colorCode, String description) {
        this.displayName = displayName;
        this.level = level;
        this.colorCode = colorCode;
        this.description = description;
    }

    /**
     * 获取职位显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取职位等级
     */
    public int getLevel() {
        return level;
    }

    /**
     * 获取颜色代码
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * 获取职位描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据名称获取职位
     */
    public static SectRank fromString(String name) {
        for (SectRank rank : values()) {
            if (rank.name().equalsIgnoreCase(name) || rank.displayName.equals(name)) {
                return rank;
            }
        }
        return OUTER_DISCIPLE; // 默认
    }

    /**
     * 获取带颜色的显示名称
     */
    public String getColoredName() {
        return colorCode + displayName;
    }

    /**
     * 检查是否有管理权限
     */
    public boolean hasManagePermission() {
        return level >= ELDER.level;
    }

    /**
     * 检查是否可以邀请成员
     */
    public boolean canInvite() {
        return level >= CORE_DISCIPLE.level;
    }

    /**
     * 检查是否可以踢出成员
     */
    public boolean canKick() {
        return level >= ELDER.level;
    }

    /**
     * 检查是否高于指定职位
     */
    public boolean isHigherThan(SectRank other) {
        return this.level > other.level;
    }

    /**
     * 从字符串安全地获取职位显示名称
     * 如果无效则返回原字符串
     */
    public static String getDisplayName(String rankName) {
        if (rankName == null) {
            return "未知";
        }
        try {
            SectRank rank = valueOf(rankName);
            return rank.getDisplayName();
        } catch (IllegalArgumentException e) {
            return rankName;
        }
    }

    /**
     * 从字符串安全地获取带颜色的职位名称
     * 如果无效则返回原字符串
     */
    public static String getColoredDisplayName(String rankName) {
        if (rankName == null) {
            return "§7未知";
        }
        try {
            SectRank rank = valueOf(rankName);
            return rank.getColoredName();
        } catch (IllegalArgumentException e) {
            return "§7" + rankName;
        }
    }

    /**
     * 从字符串安全地获取职位对象
     * 如果无效则返回 null
     */
    public static SectRank fromRankString(String rankName) {
        if (rankName == null) {
            return null;
        }
        try {
            return valueOf(rankName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
