package com.xiancore.systems.forge.items;

import com.xiancore.core.utils.QualityUtils;
import lombok.Data;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 仙家装备类
 * 从胚胎炼制而成的装备
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class Equipment {

    private String uuid;
    private EquipmentType type;
    private String quality;
    private int baseAttack;
    private int baseDefense;
    private int baseHp;
    private int baseQi;
    private String element;
    private int enhanceLevel = 0;
    private int durability = 100;
    private String customName;  // 自定义装备名称

    /**
     * 转换为 ItemStack（带 PDC 数据）
     * 用于确保装备数据可以被强化系统正确识别
     *
     * @param plugin 插件实例
     * @return ItemStack 对象
     */
    public ItemStack toItemStack(Plugin plugin) {
        // 创建基础 ItemStack（包含 DisplayName 和 Lore）
        ItemStack item = createBaseItemStack();

        // 写入 PDC 数据
        if (plugin != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();

                // 写入装备类型（统一使用 xian_type）
                NamespacedKey xianTypeKey = new NamespacedKey(plugin, "xian_type");
                pdc.set(xianTypeKey, PersistentDataType.STRING, type.name());

                // 写入其他 PDC 数据
                if (uuid != null) {
                    NamespacedKey uuidKey = new NamespacedKey(plugin, "xian_uuid");
                    pdc.set(uuidKey, PersistentDataType.STRING, uuid);
                }

                if (quality != null) {
                    NamespacedKey qualityKey = new NamespacedKey(plugin, "xian_quality");
                    pdc.set(qualityKey, PersistentDataType.STRING, quality);
                }

                NamespacedKey enhanceKey = new NamespacedKey(plugin, "xian_enhance_level");
                pdc.set(enhanceKey, PersistentDataType.INTEGER, enhanceLevel);

                if (element != null) {
                    NamespacedKey elementKey = new NamespacedKey(plugin, "xian_element");
                    pdc.set(elementKey, PersistentDataType.STRING, element);
                }

                NamespacedKey durabilityKey = new NamespacedKey(plugin, "xian_durability");
                pdc.set(durabilityKey, PersistentDataType.INTEGER, durability);

                // 写入基础属性值
                NamespacedKey attackKey = new NamespacedKey(plugin, "xian_base_attack");
                NamespacedKey defenseKey = new NamespacedKey(plugin, "xian_base_defense");
                NamespacedKey hpKey = new NamespacedKey(plugin, "xian_base_hp");
                NamespacedKey qiKey = new NamespacedKey(plugin, "xian_base_qi");

                pdc.set(attackKey, PersistentDataType.INTEGER, baseAttack);
                pdc.set(defenseKey, PersistentDataType.INTEGER, baseDefense);
                pdc.set(hpKey, PersistentDataType.INTEGER, baseHp);
                pdc.set(qiKey, PersistentDataType.INTEGER, baseQi);

                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * 创建基础 ItemStack（内部方法）
     * 包含 DisplayName 和 Lore，但不包含 PDC 数据
     *
     * @return ItemStack 对象
     */
    private ItemStack createBaseItemStack() {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // 设置名称（优先使用自定义名称）
            String displayName;
            if (customName != null && !customName.isEmpty()) {
                displayName = QualityUtils.getColor(quality) + customName + " [" + quality + "]";
            } else {
                displayName = QualityUtils.getColor(quality) + type.getDisplayName() + " [" + quality + "]";
            }
            meta.setDisplayName(displayName);

            // 设置Lore
            List<String> lore = new ArrayList<>();
            lore.add("§7");
            lore.add("§e装备类型: §f" + getCategoryName());
            lore.add("§e五行属性: §f" + element);

            if (enhanceLevel > 0) {
                lore.add("§b强化等级: §f+" + enhanceLevel);
            }

            lore.add("§7");
            lore.add("§e━━━ §6修仙加成 §e━━━");

            if (type.isWeapon()) {
                lore.add("§c  ⚔ 攻击力: +" + getCurrentAttack());
                lore.add("§b  ✦ 灵力值: +" + getCurrentQi());
            } else if (type.isArmor()) {
                lore.add("§9  ◈ 防御力: +" + getCurrentDefense());
                lore.add("§a  ❤ 生命值: +" + getCurrentHp());
                lore.add("§b  ✦ 灵力值: +" + getCurrentQi());
            } else {
                // 饰品提供全属性
                lore.add("§c  ⚔ 攻击力: +" + getCurrentAttack());
                lore.add("§9  ◈ 防御力: +" + getCurrentDefense());
                lore.add("§a  ❤ 生命值: +" + getCurrentHp());
                lore.add("§b  ✦ 灵力值: +" + getCurrentQi());
            }

            lore.add("§e━━━━━━━━━━━━");
            lore.add("§7");
            lore.add("§7§o总属性还受技能等级影响");
            lore.add("§7");
            lore.add("§e耐久度: §f" + durability + "/100");
            lore.add("§7");
            lore.add("§7UUID: " + uuid);

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 获取当前攻击力（考虑强化）
     */
    public int getCurrentAttack() {
        return (int) (baseAttack * (1 + enhanceLevel * 0.05));
    }

    /**
     * 获取当前防御力（考虑强化）
     */
    public int getCurrentDefense() {
        return (int) (baseDefense * (1 + enhanceLevel * 0.05));
    }

    /**
     * 获取当前生命值（考虑强化）
     */
    public int getCurrentHp() {
        return (int) (baseHp * (1 + enhanceLevel * 0.05));
    }

    /**
     * 获取当前灵力值（考虑强化）
     */
    public int getCurrentQi() {
        return (int) (baseQi * (1 + enhanceLevel * 0.05));
    }

    /**
     * 获取类别名称
     */
    private String getCategoryName() {
        return switch (type.getCategory()) {
            case "weapon" -> "武器";
            case "armor" -> "护甲";
            case "accessory" -> "饰品";
            default -> "未知";
        };
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public String getUuid() {
        return uuid;
    }

    public EquipmentType getType() {
        return type;
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

    public int getEnhanceLevel() {
        return enhanceLevel;
    }

    public int getDurability() {
        return durability;
    }

    public String getCustomName() {
        return customName;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setType(EquipmentType type) {
        this.type = type;
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

    public void setEnhanceLevel(int enhanceLevel) {
        this.enhanceLevel = enhanceLevel;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }
}
