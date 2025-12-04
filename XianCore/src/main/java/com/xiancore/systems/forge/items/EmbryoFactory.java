package com.xiancore.systems.forge.items;

import java.util.Random;
import java.util.UUID;

/**
 * 仙家胚胎工厂类
 * 负责生成随机仙家胚胎
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class EmbryoFactory {

    private static final Random random = new Random();

    /**
     * 生成随机品质的胚胎
     *
     * @return 仙家胚胎
     */
    public static Embryo randomGenerate() {
        return randomGenerate("random");
    }

    /**
     * 生成指定品质的胚胎
     *
     * @param quality 品质（random为随机）
     * @return 仙家胚胎
     */
    public static Embryo randomGenerate(String quality) {
        Embryo embryo = new Embryo();
        embryo.setUuid(UUID.randomUUID().toString());

        // 确定品质
        if ("random".equals(quality)) {
            quality = determineRandomQuality();
        }
        embryo.setQuality(quality);

        // 根据品质生成属性
        generateAttributes(embryo, quality);

        // 生成五行属性
        embryo.setElement(generateElement());

        return embryo;
    }

    /**
     * 根据品质生成胚胎（别名方法）
     *
     * @param quality 品质
     * @return 仙家胚胎
     */
    public static Embryo generateByQuality(String quality) {
        return randomGenerate(quality);
    }

    /**
     * 随机确定品质
     *
     * @return 品质
     */
    private static String determineRandomQuality() {
        double roll = random.nextDouble();

        if (roll < 0.01) return "神品";      // 1%
        if (roll < 0.05) return "仙品";      // 4%
        if (roll < 0.20) return "宝品";      // 15%
        if (roll < 0.50) return "灵品";      // 30%
        return "凡品";                        // 50%
    }

    /**
     * 根据品质生成属性
     *
     * @param embryo  胚胎对象
     * @param quality 品质
     */
    private static void generateAttributes(Embryo embryo, String quality) {
        int min, max;

        switch (quality) {
            case "神品":
                min = 95;
                max = 100;
                break;
            case "仙品":
                min = 85;
                max = 95;
                break;
            case "宝品":
                min = 60;
                max = 85;
                break;
            case "灵品":
                min = 30;
                max = 60;
                break;
            default: // 凡品
                min = 10;
                max = 30;
                break;
        }

        embryo.setBaseAttack(randomInRange(min, max));
        embryo.setBaseDefense(randomInRange(min / 2, max / 2));
        embryo.setBaseHp(randomInRange(min * 5, max * 5));
        embryo.setBaseQi(randomInRange(min * 2, max * 2));
    }

    /**
     * 生成五行属性
     *
     * @return 五行属性
     */
    private static String generateElement() {
        double roll = random.nextDouble();

        if (roll < 0.10) {
            // 复合属性 10%
            String[] elements = {"火", "水", "木", "金", "土"};
            String element1 = elements[random.nextInt(elements.length)];
            String element2 = elements[random.nextInt(elements.length)];
            while (element2.equals(element1)) {
                element2 = elements[random.nextInt(elements.length)];
            }
            return element1 + "+" + element2;
        } else {
            // 单属性 90%
            String[] elements = {"火", "水", "木", "金", "土"};
            return elements[random.nextInt(elements.length)];
        }
    }

    /**
     * 在范围内生成随机数
     *
     * @param min 最小值
     * @param max 最大值
     * @return 随机数
     */
    private static int randomInRange(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}
