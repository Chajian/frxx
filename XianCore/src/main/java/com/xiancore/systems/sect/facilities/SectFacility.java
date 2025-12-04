package com.xiancore.systems.sect.facilities;

import lombok.Getter;
import org.bukkit.Material;

/**
 * 宗门设施枚举
 * 定义所有可用的宗门设施类型
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public enum SectFacility {

    /**
     * 灵脉系统
     * 提升修炼速度
     */
    SPIRITUAL_VEIN(
            "spiritual_vein",
            "\u00a7b\u00a7l\u7075\u8109",
            Material.BEACON,
            10,  // 最大等级
            1000,  // 基础升级消耗
            5.0,   // 每级加成百分比
            "\u00a77\u63d0\u5347\u4fee\u70bc\u901f\u5ea6",
            "\u00a7a+%d%% \u4fee\u70bc\u901f\u5ea6"
    ),

    /**
     * 炼器台
     * 提升炼制成功率
     */
    FORGE_ALTAR(
            "forge_altar",
            "\u00a76\u00a7l\u70bc\u5668\u53f0",
            Material.ANVIL,
            10,
            1500,
            3.0,
            "\u00a77\u63d0\u5347\u70bc\u5236\u6210\u529f\u7387",
            "\u00a7a+%d%% \u70bc\u5236\u6210\u529f\u7387"
    ),

    /**
     * 藏经阁
     * 学习宗门功法
     */
    SCRIPTURE_PAVILION(
            "scripture_pavilion",
            "\u00a7d\u00a7l\u85cf\u7ecf\u9601",
            Material.BOOKSHELF,
            10,
            2000,
            0.0,  // 藏经阁不是百分比加成，而是解锁功法
            "\u00a77\u5b66\u4e60\u5b97\u95e8\u529f\u6cd5",
            "\u00a7e\u89e3\u9501 %d \u4e2a\u529f\u6cd5"
    ),

    /**
     * 宗门仓库
     * 共享物品存储
     */
    SECT_WAREHOUSE(
            "sect_warehouse",
            "\u00a7e\u00a7l\u5b97\u95e8\u4ed3\u5e93",
            Material.CHEST,
            6,  // 最大6级（最多54格）
            3000,
            9.0,  // 每级增加9格（一行）
            "\u00a77\u5171\u4eab\u7269\u54c1\u5b58\u50a8",
            "\u00a7f%d \u683c\u5b58\u50a8\u7a7a\u95f4"
    ),

    /**
     * 宗门商店
     * 贡献点兑换物品
     */
    SECT_SHOP(
            "sect_shop",
            "\u00a7a\u00a7l\u5b97\u95e8\u5546\u5e97",
            Material.EMERALD,
            1,  // 商店不分等级，只有开启/未开启
            5000,
            0.0,
            "\u00a77\u8d21\u732e\u70b9\u5151\u6362\u7269\u54c1",
            "\u00a7a\u5df2\u5f00\u542f"
    );

    private final String id;
    private final String displayName;
    private final Material icon;
    private final int maxLevel;
    private final int baseUpgradeCost;  // 基础升级消耗（灵石）
    private final double bonusPerLevel;  // 每级加成百分比
    private final String description;
    private final String bonusFormat;  // 加成显示格式

    SectFacility(String id, String displayName, Material icon, int maxLevel,
                 int baseUpgradeCost, double bonusPerLevel, String description, String bonusFormat) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.maxLevel = maxLevel;
        this.baseUpgradeCost = baseUpgradeCost;
        this.bonusPerLevel = bonusPerLevel;
        this.description = description;
        this.bonusFormat = bonusFormat;
    }

    /**
     * 计算升级消耗
     *
     * @param currentLevel 当前等级
     * @return 升级所需灵石
     */
    public long getUpgradeCost(int currentLevel) {
        return (long) baseUpgradeCost * currentLevel;
    }

    /**
     * 计算加成效果
     *
     * @param level 设施等级
     * @return 加成百分比（例如：5.0 表示 5%）
     */
    public double getBonus(int level) {
        if (this == SCRIPTURE_PAVILION) {
            // 藏经阁返回解锁的功法数量
            return level * 2;  // 每级解锁2个功法
        } else if (this == SECT_WAREHOUSE) {
            // 仓库返回总格数（每级9格，最大54格）
            int capacity = (int) (level * bonusPerLevel);
            return Math.min(capacity, 54);  // 最大54格
        } else if (this == SECT_SHOP) {
            // 商店只有开启/未开启
            return level > 0 ? 1 : 0;
        } else {
            // 其他设施返回百分比加成
            return level * bonusPerLevel;
        }
    }

    /**
     * 获取格式化的加成显示
     *
     * @param level 设施等级
     * @return 格式化字符串
     */
    public String getFormattedBonus(int level) {
        if (this == SECT_WAREHOUSE) {
            return String.format(bonusFormat, (int) getBonus(level));
        } else if (this == SCRIPTURE_PAVILION) {
            return String.format(bonusFormat, (int) getBonus(level));
        } else if (this == SECT_SHOP) {
            return level > 0 ? bonusFormat : "\u00a7c\u672a\u5f00\u542f";
        } else {
            return String.format(bonusFormat, (int) getBonus(level));
        }
    }

    /**
     * 根据 ID 获取设施
     *
     * @param id 设施ID
     * @return 设施枚举，如果不存在返回 null
     */
    public static SectFacility fromId(String id) {
        for (SectFacility facility : values()) {
            if (facility.getId().equals(id)) {
                return facility;
            }
        }
        return null;
    }

    // ==================== 显式 Getter 方法 ====================

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getBaseUpgradeCost() {
        return baseUpgradeCost;
    }

    public double getBonusPerLevel() {
        return bonusPerLevel;
    }

    public String getDescription() {
        return description;
    }

    public String getBonusFormat() {
        return bonusFormat;
    }
}
