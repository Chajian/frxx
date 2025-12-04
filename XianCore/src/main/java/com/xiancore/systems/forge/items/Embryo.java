package com.xiancore.systems.forge.items;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 仙家胚胎类
 * 代表可炼制成装备的胚胎物品
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class Embryo {

    private String uuid;
    private String quality;
    private int baseAttack;
    private int baseDefense;
    private int baseHp;
    private int baseQi;
    private String element;  // 五行属性

    /**
     * 转换为 ItemStack
     *
     * @return ItemStack 对象
     */
    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(getQualityColor() + "仙家胚胎 [" + quality + "]");

            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§7基础属性:");
            lore.add("§c  攻击力: " + baseAttack);
            lore.add("§9  防御力: " + baseDefense);
            lore.add("§a  生命值: " + baseHp);
            lore.add("§b  灵力值: " + baseQi);
            lore.add("§7");
            lore.add("§e五行属性: " + element);
            lore.add("§7");
            lore.add("§7UUID: " + uuid);

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 获取品质颜色
     */
    private String getQualityColor() {
        return switch (quality) {
            case "神品" -> "§d§l";
            case "仙品" -> "§6§l";
            case "宝品" -> "§5";
            case "灵品" -> "§b";
            default -> "§f";
        };
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public String getUuid() {
        return uuid;
    }

    public String getQuality() {
        return quality;
    }

    public int getBaseAttack() {
        return baseAttack;
    }

    public int getBaseDefense() {
        return baseDefense;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public int getBaseQi() {
        return baseQi;
    }

    public String getElement() {
        return element;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setBaseAttack(int baseAttack) {
        this.baseAttack = baseAttack;
    }

    public void setBaseDefense(int baseDefense) {
        this.baseDefense = baseDefense;
    }

    public void setBaseHp(int baseHp) {
        this.baseHp = baseHp;
    }

    public void setBaseQi(int baseQi) {
        this.baseQi = baseQi;
    }

    public void setElement(String element) {
        this.element = element;
    }
}
