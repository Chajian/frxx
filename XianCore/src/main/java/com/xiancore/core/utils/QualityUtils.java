package com.xiancore.core.utils;

/**
 * 品质工具类
 * 统一管理装备/胚胎的品质颜色映射
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public final class QualityUtils {

    private QualityUtils() {
        // 工具类禁止实例化
    }

    /**
     * 获取品质对应的颜色代码
     *
     * @param quality 品质名称（凡品、灵品、宝品、仙品、神品）
     * @return Minecraft 颜色代码
     */
    public static String getColor(String quality) {
        if (quality == null) {
            return "§f";
        }
        return switch (quality) {
            case "神品" -> "§d§l";  // 粉色加粗
            case "仙品" -> "§6§l";  // 金色加粗
            case "宝品" -> "§5";    // 紫色
            case "灵品" -> "§b";    // 青色
            case "凡品" -> "§7";    // 灰色
            default -> "§f";        // 白色
        };
    }

    /**
     * 获取品质对应的数值权重（用于计算属性加成等）
     *
     * @param quality 品质名称
     * @return 权重值 (1-5)
     */
    public static int getWeight(String quality) {
        if (quality == null) {
            return 1;
        }
        return switch (quality) {
            case "神品" -> 5;
            case "仙品" -> 4;
            case "宝品" -> 3;
            case "灵品" -> 2;
            case "凡品" -> 1;
            default -> 1;
        };
    }

    /**
     * 根据数值获取对应的品质名称
     *
     * @param value 品质数值 (1-100)
     * @return 品质名称
     */
    public static String fromValue(int value) {
        if (value >= 95) return "神品";
        if (value >= 80) return "仙品";
        if (value >= 60) return "宝品";
        if (value >= 30) return "灵品";
        return "凡品";
    }

    /**
     * 格式化品质显示（带颜色）
     *
     * @param quality 品质名称
     * @return 带颜色的品质文本
     */
    public static String format(String quality) {
        return getColor(quality) + quality;
    }

    /**
     * 格式化品质显示（带颜色和方括号）
     *
     * @param quality 品质名称
     * @return 带颜色的品质文本，如 [§d§l神品§r]
     */
    public static String formatBracketed(String quality) {
        return "§7[" + getColor(quality) + quality + "§7]";
    }
}
