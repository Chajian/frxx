package com.xiancore.core.data;

import lombok.Getter;
import java.util.*;

/**
 * 灵根类型枚举
 * 基于五行属性的灵根系统，符合《凡人修仙传》设定
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public enum SpiritualRootType {
    
    // ========== 单灵根（天灵根） 0.5% ==========
    HEAVENLY_METAL("天灵根", "纯金灵根", Arrays.asList("金"), 0.95, 1.00, "§d§l", 0.005),
    HEAVENLY_WOOD("天灵根", "纯木灵根", Arrays.asList("木"), 0.95, 1.00, "§d§l", 0.005),
    HEAVENLY_WATER("天灵根", "纯水灵根", Arrays.asList("水"), 0.95, 1.00, "§d§l", 0.005),
    HEAVENLY_FIRE("天灵根", "纯火灵根", Arrays.asList("火"), 0.95, 1.00, "§d§l", 0.005),
    HEAVENLY_EARTH("天灵根", "纯土灵根", Arrays.asList("土"), 0.95, 1.00, "§d§l", 0.005),
    
    // ========== 双灵根（异灵根） 2.5% ==========
    VARIANT_METAL_WOOD("异灵根", "金木双灵根", Arrays.asList("金", "木"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_METAL_WATER("异灵根", "金水双灵根", Arrays.asList("金", "水"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_METAL_FIRE("异灵根", "金火双灵根", Arrays.asList("金", "火"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_METAL_EARTH("异灵根", "金土双灵根", Arrays.asList("金", "土"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_WOOD_WATER("异灵根", "木水双灵根", Arrays.asList("木", "水"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_WOOD_FIRE("异灵根", "木火双灵根", Arrays.asList("木", "火"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_WOOD_EARTH("异灵根", "木土双灵根", Arrays.asList("木", "土"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_WATER_FIRE("异灵根", "水火双灵根", Arrays.asList("水", "火"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_WATER_EARTH("异灵根", "水土双灵根", Arrays.asList("水", "土"), 0.80, 0.90, "§5§l", 0.025),
    VARIANT_FIRE_EARTH("异灵根", "火土双灵根", Arrays.asList("火", "土"), 0.80, 0.90, "§5§l", 0.025),
    
    // ========== 三灵根（真灵根） 7% ==========
    TRUE_THREE_1("真灵根", "金木水三灵根", Arrays.asList("金", "木", "水"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_2("真灵根", "金木火三灵根", Arrays.asList("金", "木", "火"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_3("真灵根", "金木土三灵根", Arrays.asList("金", "木", "土"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_4("真灵根", "金水火三灵根", Arrays.asList("金", "水", "火"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_5("真灵根", "金水土三灵根", Arrays.asList("金", "水", "土"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_6("真灵根", "金火土三灵根", Arrays.asList("金", "火", "土"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_7("真灵根", "木水火三灵根", Arrays.asList("木", "水", "火"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_8("真灵根", "木水土三灵根", Arrays.asList("木", "水", "土"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_9("真灵根", "木火土三灵根", Arrays.asList("木", "火", "土"), 0.70, 0.80, "§b§l", 0.07),
    TRUE_THREE_10("真灵根", "水火土三灵根", Arrays.asList("水", "火", "土"), 0.70, 0.80, "§b§l", 0.07),
    
    // ========== 四灵根（上品灵根） 15% ==========
    SUPERIOR_FOUR_1("上品灵根", "金木水火四灵根", Arrays.asList("金", "木", "水", "火"), 0.60, 0.70, "§e§l", 0.15),
    SUPERIOR_FOUR_2("上品灵根", "金木水土四灵根", Arrays.asList("金", "木", "水", "土"), 0.60, 0.70, "§e§l", 0.15),
    SUPERIOR_FOUR_3("上品灵根", "金木火土四灵根", Arrays.asList("金", "木", "火", "土"), 0.60, 0.70, "§e§l", 0.15),
    SUPERIOR_FOUR_4("上品灵根", "金水火土四灵根", Arrays.asList("金", "水", "火", "土"), 0.60, 0.70, "§e§l", 0.15),
    SUPERIOR_FOUR_5("上品灵根", "木水火土四灵根", Arrays.asList("木", "水", "火", "土"), 0.60, 0.70, "§e§l", 0.15),
    
    // ========== 五灵根（杂灵根） 75% ==========
    MIXED_FIVE("杂灵根", "金木水火土五灵根", Arrays.asList("金", "木", "水", "火", "土"), 0.20, 0.60, "§7", 0.75);
    
    /**
     * 灵根等级名称（如：天灵根、异灵根等）
     */
    private final String gradeName;
    
    /**
     * 灵根完整名称（如：纯金灵根、金木双灵根等）
     */
    private final String fullName;
    
    /**
     * 五行属性列表
     */
    private final List<String> elements;
    
    /**
     * 灵根值范围-最小值
     */
    private final double minValue;
    
    /**
     * 灵根值范围-最大值
     */
    private final double maxValue;
    
    /**
     * 颜色代码
     */
    private final String colorCode;
    
    /**
     * 出现概率（用于随机生成）
     */
    private final double probability;
    
    SpiritualRootType(String gradeName, String fullName, List<String> elements,
                     double minValue, double maxValue, String colorCode, double probability) {
        this.gradeName = gradeName;
        this.fullName = fullName;
        this.elements = elements;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.colorCode = colorCode;
        this.probability = probability;
    }
    
    /**
     * 获取灵根完整名称
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * 获取灵根等级名称
     */
    public String getGradeName() {
        return gradeName;
    }

    /**
     * 获取五行属性列表
     */
    public List<String> getElements() {
        return elements;
    }

    /**
     * 获取灵根值范围-最小值
     */
    public double getMinValue() {
        return minValue;
    }

    /**
     * 获取灵根值范围-最大值
     */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * 获取颜色代码
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * 获取出现概率
     */
    public double getProbability() {
        return probability;
    }

    /**
     * 获取带颜色的等级名称
     */
    public String getColoredGradeName() {
        return colorCode + gradeName;
    }
    
    /**
     * 获取带颜色的完整名称
     */
    public String getColoredFullName() {
        return colorCode + fullName;
    }
    
    /**
     * 获取五行属性字符串（带颜色）
     */
    public String getElementsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append("§f、");
            }
            String element = elements.get(i);
            sb.append(getElementColor(element)).append(element);
        }
        return sb.toString();
    }
    
    /**
     * 获取元素颜色
     */
    private String getElementColor(String element) {
        return switch (element) {
            case "金" -> "§e";  // 黄色
            case "木" -> "§a";  // 绿色
            case "水" -> "§b";  // 青色
            case "火" -> "§c";  // 红色
            case "土" -> "§6";  // 金色
            default -> "§f";    // 白色
        };
    }
    
    /**
     * 生成该类型的随机灵根值
     */
    public double generateValue() {
        return minValue + Math.random() * (maxValue - minValue);
    }
    
    /**
     * 获取灵根品质描述
     */
    public String getQualityDescription() {
        return switch (elements.size()) {
            case 1 -> "§d§l传说中的天灵根！修炼之路将一帆风顺！";
            case 2 -> "§5§l罕见的异灵根！注定成为一方强者！";
            case 3 -> "§b§l不错的真灵根！勤加修炼，前途无量！";
            case 4 -> "§e§l上品灵根！努力修炼，终将有所成就！";
            default -> "§7虽然是杂灵根，但凡人亦可逆天改命！";
        };
    }
    
    /**
     * 根据灵根值获取对应类型（用于兼容旧数据）
     */
    public static SpiritualRootType fromValue(double value) {
        if (value >= 0.90) {
            // 随机返回一个天灵根
            SpiritualRootType[] heavenly = {
                HEAVENLY_METAL, HEAVENLY_WOOD, HEAVENLY_WATER, HEAVENLY_FIRE, HEAVENLY_EARTH
            };
            return heavenly[new Random().nextInt(heavenly.length)];
        } else if (value >= 0.80) {
            // 随机返回一个异灵根
            SpiritualRootType[] variant = {
                VARIANT_METAL_WOOD, VARIANT_METAL_WATER, VARIANT_METAL_FIRE, VARIANT_METAL_EARTH,
                VARIANT_WOOD_WATER, VARIANT_WOOD_FIRE, VARIANT_WOOD_EARTH,
                VARIANT_WATER_FIRE, VARIANT_WATER_EARTH, VARIANT_FIRE_EARTH
            };
            return variant[new Random().nextInt(variant.length)];
        } else if (value >= 0.70) {
            // 随机返回一个真灵根
            SpiritualRootType[] trueSR = {
                TRUE_THREE_1, TRUE_THREE_2, TRUE_THREE_3, TRUE_THREE_4, TRUE_THREE_5,
                TRUE_THREE_6, TRUE_THREE_7, TRUE_THREE_8, TRUE_THREE_9, TRUE_THREE_10
            };
            return trueSR[new Random().nextInt(trueSR.length)];
        } else if (value >= 0.60) {
            // 随机返回一个上品灵根
            SpiritualRootType[] superior = {
                SUPERIOR_FOUR_1, SUPERIOR_FOUR_2, SUPERIOR_FOUR_3, SUPERIOR_FOUR_4, SUPERIOR_FOUR_5
            };
            return superior[new Random().nextInt(superior.length)];
        } else {
            return MIXED_FIVE;
        }
    }
    
    /**
     * 随机生成一个灵根类型（基于概率权重）
     */
    public static SpiritualRootType randomGenerate() {
        double rand = Math.random();
        double cumulative = 0.0;
        
        // 按概率区间生成
        if (rand < 0.005) {
            // 0.5% - 天灵根（5种随机1种）
            SpiritualRootType[] heavenly = {
                HEAVENLY_METAL, HEAVENLY_WOOD, HEAVENLY_WATER, HEAVENLY_FIRE, HEAVENLY_EARTH
            };
            return heavenly[new Random().nextInt(heavenly.length)];
            
        } else if (rand < 0.03) {
            // 2.5% - 异灵根（10种随机1种）
            SpiritualRootType[] variant = {
                VARIANT_METAL_WOOD, VARIANT_METAL_WATER, VARIANT_METAL_FIRE, VARIANT_METAL_EARTH,
                VARIANT_WOOD_WATER, VARIANT_WOOD_FIRE, VARIANT_WOOD_EARTH,
                VARIANT_WATER_FIRE, VARIANT_WATER_EARTH, VARIANT_FIRE_EARTH
            };
            return variant[new Random().nextInt(variant.length)];
            
        } else if (rand < 0.10) {
            // 7% - 真灵根（10种随机1种）
            SpiritualRootType[] trueSR = {
                TRUE_THREE_1, TRUE_THREE_2, TRUE_THREE_3, TRUE_THREE_4, TRUE_THREE_5,
                TRUE_THREE_6, TRUE_THREE_7, TRUE_THREE_8, TRUE_THREE_9, TRUE_THREE_10
            };
            return trueSR[new Random().nextInt(trueSR.length)];
            
        } else if (rand < 0.25) {
            // 15% - 上品灵根（5种随机1种）
            SpiritualRootType[] superior = {
                SUPERIOR_FOUR_1, SUPERIOR_FOUR_2, SUPERIOR_FOUR_3, SUPERIOR_FOUR_4, SUPERIOR_FOUR_5
            };
            return superior[new Random().nextInt(superior.length)];
            
        } else {
            // 75% - 杂灵根
            return MIXED_FIVE;
        }
    }
    
    /**
     * 通过名称查找灵根类型
     */
    public static SpiritualRootType fromName(String name) {
        for (SpiritualRootType type : values()) {
            if (type.name().equalsIgnoreCase(name) || 
                type.fullName.equals(name) || 
                type.gradeName.equals(name)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 获取所有天灵根类型
     */
    public static List<SpiritualRootType> getHeavenlyRoots() {
        return Arrays.asList(
            HEAVENLY_METAL, HEAVENLY_WOOD, HEAVENLY_WATER, HEAVENLY_FIRE, HEAVENLY_EARTH
        );
    }
    
    /**
     * 获取所有异灵根类型
     */
    public static List<SpiritualRootType> getVariantRoots() {
        return Arrays.asList(
            VARIANT_METAL_WOOD, VARIANT_METAL_WATER, VARIANT_METAL_FIRE, VARIANT_METAL_EARTH,
            VARIANT_WOOD_WATER, VARIANT_WOOD_FIRE, VARIANT_WOOD_EARTH,
            VARIANT_WATER_FIRE, VARIANT_WATER_EARTH, VARIANT_FIRE_EARTH
        );
    }
}











