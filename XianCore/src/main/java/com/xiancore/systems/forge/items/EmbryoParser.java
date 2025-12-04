package com.xiancore.systems.forge.items;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 胚胎解析工具类
 * 从ItemStack中解析胚胎数据
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class EmbryoParser {

    /**
     * 判断ItemStack是否为胚胎
     *
     * @param item 物品
     * @return 是否为胚胎
     */
    public static boolean isEmbryo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        String displayName = meta.getDisplayName();
        return displayName.contains("仙家胚胎");
    }

    /**
     * 从ItemStack解析胚胎数据
     *
     * @param item 物品
     * @return 胚胎对象，如果解析失败返回null
     */
    public static Embryo parseFromItemStack(ItemStack item) {
        if (!isEmbryo(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }

        Embryo embryo = new Embryo();

        // 解析品质
        String displayName = meta.getDisplayName();
        String quality = extractQuality(displayName);
        if (quality == null) {
            return null;
        }
        embryo.setQuality(quality);

        // 解析Lore中的属性
        List<String> lore = meta.getLore();
        for (String line : lore) {
            String cleanLine = stripColor(line);

            if (cleanLine.contains("攻击力:")) {
                embryo.setBaseAttack(extractNumber(cleanLine));
            } else if (cleanLine.contains("防御力:")) {
                embryo.setBaseDefense(extractNumber(cleanLine));
            } else if (cleanLine.contains("生命值:")) {
                embryo.setBaseHp(extractNumber(cleanLine));
            } else if (cleanLine.contains("灵力值:")) {
                embryo.setBaseQi(extractNumber(cleanLine));
            } else if (cleanLine.contains("五行属性:")) {
                embryo.setElement(extractElement(cleanLine));
            } else if (cleanLine.contains("UUID:")) {
                embryo.setUuid(extractUUID(cleanLine));
            }
        }

        return embryo;
    }

    /**
     * 提取品质
     */
    private static String extractQuality(String displayName) {
        if (displayName.contains("神品")) return "神品";
        if (displayName.contains("仙品")) return "仙品";
        if (displayName.contains("宝品")) return "宝品";
        if (displayName.contains("灵品")) return "灵品";
        if (displayName.contains("凡品")) return "凡品";
        return null;
    }

    /**
     * 提取数字
     */
    private static int extractNumber(String text) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        return 0;
    }

    /**
     * 提取五行属性
     */
    private static String extractElement(String text) {
        int colonIndex = text.indexOf(":");
        if (colonIndex != -1 && colonIndex + 1 < text.length()) {
            return text.substring(colonIndex + 1).trim();
        }
        return "无";
    }

    /**
     * 提取UUID
     */
    private static String extractUUID(String text) {
        int colonIndex = text.indexOf(":");
        if (colonIndex != -1 && colonIndex + 1 < text.length()) {
            return text.substring(colonIndex + 1).trim();
        }
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 移除颜色代码
     */
    private static String stripColor(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}
