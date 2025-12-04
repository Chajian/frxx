package com.xiancore.systems.forge.items;

import org.bukkit.Material;

/**
 * 装备类型枚举
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public enum EquipmentType {

    SWORD("剑", Material.DIAMOND_SWORD, "weapon"),
    AXE("斧", Material.DIAMOND_AXE, "weapon"),
    BOW("弓", Material.BOW, "weapon"),

    HELMET("头盔", Material.DIAMOND_HELMET, "armor"),
    CHESTPLATE("胸甲", Material.DIAMOND_CHESTPLATE, "armor"),
    LEGGINGS("护腿", Material.DIAMOND_LEGGINGS, "armor"),
    BOOTS("靴子", Material.DIAMOND_BOOTS, "armor"),

    RING("戒指", Material.GOLD_INGOT, "accessory"),
    NECKLACE("项链", Material.CHAIN, "accessory"),
    TALISMAN("法宝", Material.TOTEM_OF_UNDYING, "accessory");

    private final String displayName;
    private final Material material;
    private final String category;

    EquipmentType(String displayName, Material material, String category) {
        this.displayName = displayName;
        this.material = material;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public String getCategory() {
        return category;
    }

    public boolean isWeapon() {
        return "weapon".equals(category);
    }

    public boolean isArmor() {
        return "armor".equals(category);
    }

    public boolean isAccessory() {
        return "accessory".equals(category);
    }
}
