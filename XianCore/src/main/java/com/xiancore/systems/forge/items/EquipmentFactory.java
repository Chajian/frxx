package com.xiancore.systems.forge.items;

import com.xiancore.XianCore;
import com.xiancore.systems.forge.ForgeRecipe;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 装备工厂类
 * 负责从胚胎生成装备
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class EquipmentFactory {

    /**
     * 从胚胎创建装备并生成 ItemStack
     *
     * @param plugin 插件实例
     * @param embryo 胚胎对象
     * @param type   装备类型
     * @return 生成的 ItemStack，如果失败则回退到原生物品
     */
    public static ItemStack createItemFromEmbryo(XianCore plugin, Embryo embryo, EquipmentType type) {
        // 先创建 Equipment 对象
        Equipment equipment = createFromEmbryo(embryo, type);

        // 尝试使用 MythicMobs 生成物品
        MythicItemAdapter adapter = new MythicItemAdapter(plugin);
        if (adapter.isAvailable()) {
            ItemStack mmItem = adapter.createMythicItem(equipment);
            if (mmItem != null) {
                plugin.getLogger().info("已生成 MythicMobs 装备: " + equipment.getQuality() + " " + type.getDisplayName());
                return mmItem;
            } else {
                plugin.getLogger().warning("MythicMobs 模板缺失，回退到原生物品: " + equipment.getQuality() + " " + type.getDisplayName());
            }
        }

        // 回退：使用原生方法生成物品（带 PDC 数据）
        return equipment.toItemStack(plugin);
    }

    /**
     * 从胚胎创建装备
     *
     * @param embryo 胚胎对象
     * @param type   装备类型
     * @return 装备对象
     */
    public static Equipment createFromEmbryo(Embryo embryo, EquipmentType type) {
        Equipment equipment = new Equipment();

        // 生成新的UUID
        equipment.setUuid(UUID.randomUUID().toString());

        // 设置装备类型
        equipment.setType(type);

        // 继承胚胎的品质
        equipment.setQuality(embryo.getQuality());

        // 继承胚胎的五行属性
        equipment.setElement(embryo.getElement());

        // 根据装备类型调整属性
        int attackBonus = type.isWeapon() ? 2 : 1;
        int defenseBonus = type.isArmor() ? 2 : 1;
        int hpBonus = type.isArmor() ? 2 : 1;
        int qiBonus = type.isAccessory() ? 2 : 1;

        // 🆕 添加随机波动（±10%），让每件装备都略有不同
        equipment.setBaseAttack(applyRandomVariance(embryo.getBaseAttack() * attackBonus));
        equipment.setBaseDefense(applyRandomVariance(embryo.getBaseDefense() * defenseBonus));
        equipment.setBaseHp(applyRandomVariance(embryo.getBaseHp() * hpBonus));
        equipment.setBaseQi(applyRandomVariance(embryo.getBaseQi() * qiBonus));

        // 初始化强化等级和耐久
        equipment.setEnhanceLevel(0);
        equipment.setDurability(100);

        return equipment;
    }

    /**
     * 从配方创建装备
     *
     * @param recipe 配方对象
     * @return 装备对象
     */
    public static Equipment createFromRecipe(ForgeRecipe recipe) {
        Equipment equipment = new Equipment();

        // 生成新的UUID
        equipment.setUuid(UUID.randomUUID().toString());

        // 设置装备类型
        equipment.setType(recipe.getEquipmentType());

        // 设置品质
        equipment.setQuality(recipe.getQuality());

        // 设置五行属性
        equipment.setElement(recipe.getElement());

        // 根据品质和装备类型计算基础属性
        int baseValue = getQualityBaseValue(recipe.getQuality());

        int attackBonus = recipe.getEquipmentType().isWeapon() ? 2 : 1;
        int defenseBonus = recipe.getEquipmentType().isArmor() ? 2 : 1;
        int hpBonus = recipe.getEquipmentType().isArmor() ? 2 : 1;
        int qiBonus = recipe.getEquipmentType().isAccessory() ? 2 : 1;

        equipment.setBaseAttack(baseValue * attackBonus);
        equipment.setBaseDefense(baseValue * defenseBonus);
        equipment.setBaseHp(baseValue * 10 * hpBonus);
        equipment.setBaseQi(baseValue * 5 * qiBonus);

        // 初始化强化等级和耐久
        equipment.setEnhanceLevel(0);
        equipment.setDurability(100);

        return equipment;
    }

    /**
     * 根据品质获取基础属性值
     */
    private static int getQualityBaseValue(String quality) {
        return switch (quality) {
            case "神品" -> 100;
            case "仙品" -> 80;
            case "宝品" -> 60;
            case "灵品" -> 40;
            default -> 20;  // 凡品
        };
    }

    /**
     * 根据装备类别获取可选类型
     *
     * @param category 类别（weapon, armor, accessory）
     * @return 可选装备类型数组
     */
    public static EquipmentType[] getTypesByCategory(String category) {
        return switch (category) {
            case "weapon" -> new EquipmentType[]{
                    EquipmentType.SWORD,
                    EquipmentType.AXE,
                    EquipmentType.BOW
            };
            case "armor" -> new EquipmentType[]{
                    EquipmentType.HELMET,
                    EquipmentType.CHESTPLATE,
                    EquipmentType.LEGGINGS,
                    EquipmentType.BOOTS
            };
            case "accessory" -> new EquipmentType[]{
                    EquipmentType.RING,
                    EquipmentType.NECKLACE,
                    EquipmentType.TALISMAN
            };
            default -> new EquipmentType[0];
        };
    }

    /**
     * 应用随机波动（±10%）
     * 让每件装备的属性都略有不同
     *
     * @param baseValue 基础数值
     * @return 应用随机波动后的数值
     */
    private static int applyRandomVariance(int baseValue) {
        if (baseValue == 0) {
            return 0;
        }
        
        // 随机波动范围：0.9 - 1.1（±10%）
        double variance = 0.9 + (Math.random() * 0.2);
        return (int)(baseValue * variance);
    }
}
