package com.xiancore.systems.forge.items;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备解析工具类
 * 使用NBT存储装备数据，确保数据可靠性
 *
 * @author AI Assistant
 * @version 1.0.0
 */
public class EquipmentParser {

    private static Plugin plugin;

    /**
     * 初始化（需要在插件启动时调用）
     */
    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * 判断ItemStack是否为装备
     */
    public static boolean isEquipment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        // 先排除胚胎
        if (EmbryoParser.isEmbryo(item)) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // 方法1：检查NBT标签（统一使用 xian_type）
        if (plugin != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey xianTypeKey = new NamespacedKey(plugin, "xian_type");
            if (pdc.has(xianTypeKey, PersistentDataType.STRING)) {
                return true;
            }
        }

        // 方法2：检查Lore标识（向后兼容）
        if (meta.hasLore()) {
            var lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("装备类型:") || 
                        line.contains("强化等级:") || 
                        line.contains("修仙加成")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取装备强化等级
     * 优先从NBT读取，回退到Lore解析
     */
    public static int getEnhanceLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        // 方法1：从NBT读取（推荐，统一使用 xian_enhance_level）
        if (plugin != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey xianEnhanceKey = new NamespacedKey(plugin, "xian_enhance_level");
            
            if (pdc.has(xianEnhanceKey, PersistentDataType.INTEGER)) {
                return pdc.get(xianEnhanceKey, PersistentDataType.INTEGER);
            }
        }

        // 方法2：从Lore解析（向后兼容）
        if (meta.hasLore()) {
            var lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("强化等级:") || line.contains("强化:")) {
                        try {
                            String[] parts = line.split("[^0-9]");
                            for (String part : parts) {
                                if (!part.isEmpty()) {
                                    int level = Integer.parseInt(part);
                                    // 迁移到NBT
                                    if (plugin != null) {
                                        setEnhanceLevel(item, level);
                                    }
                                    return level;
                                }
                            }
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    }
                }
            }
        }

        return 0;
    }

    /**
     * 设置装备强化等级
     * 同时更新NBT和Lore
     */
    public static void setEnhanceLevel(ItemStack item, int level) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        // 写入NBT（主要存储，统一使用 xian_enhance_level）
        if (plugin != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey xianEnhanceKey = new NamespacedKey(plugin, "xian_enhance_level");
            pdc.set(xianEnhanceKey, PersistentDataType.INTEGER, level);
        }

        // 更新Lore（显示用）
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // 移除旧的强化等级行
        lore.removeIf(line -> line.contains("强化等级:") || line.contains("强化:"));

        // 添加新的强化等级（如果level > 0）
        if (level > 0) {
            // 找到合适的位置插入（在属性加成之前）
            int insertIndex = lore.size();
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("攻击加成:") || lore.get(i).contains("防御加成:")) {
                    insertIndex = i;
                    break;
                }
            }
            lore.add(insertIndex, "§7强化等级: §6+" + level);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * 更新装备属性（基于强化等级）
     */
    public static void updateEnhanceStats(ItemStack item, int level) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // 移除旧的属性加成行
        lore.removeIf(line -> line.contains("攻击加成:") || line.contains("防御加成:"));

        // 添加新的属性加成
        if (level > 0) {
            int atkBonus = level * 10;
            int defBonus = level * 5;

            // 找到合适的位置插入（在强化等级之后）
            int insertIndex = lore.size();
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).contains("强化等级:")) {
                    insertIndex = i + 1;
                    break;
                }
            }

            lore.add(insertIndex, "§c攻击加成: +" + atkBonus);
            lore.add(insertIndex + 1, "§9防御加成: +" + defBonus);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * 从ItemStack解析装备对象
     * 用于GUI显示
     */
    public static Equipment parseFromItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        Equipment equipment = new Equipment();

        // 解析装备类型（优先从 PDC 读取，回退到材质推测）
        EquipmentType type = null;
        
        // 方法1：从 PDC 读取装备类型（统一使用 xian_type）
        if (plugin != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey xianTypeKey = new NamespacedKey(plugin, "xian_type");
            if (pdc.has(xianTypeKey, PersistentDataType.STRING)) {
                String typeName = pdc.get(xianTypeKey, PersistentDataType.STRING);
                try {
                    type = EquipmentType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    // 忽略无效的枚举值
                }
            }
        }
        
        // 方法2：回退到材质推测
        if (type == null) {
            type = guessEquipmentType(item.getType());
        }
        
        if (type == null) {
            return null;  // 无法确定装备类型
        }
        
        equipment.setType(type);

        // 解析DisplayName
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            
            // 提取品质
            String quality = extractQuality(displayName);
            equipment.setQuality(quality);
            
            // 提取自定义名称（移除颜色代码和品质标记）
            String cleanName = displayName.replaceAll("§[0-9a-fklmnor]", "");
            if (cleanName.contains("[")) {
                cleanName = cleanName.substring(0, cleanName.indexOf("[")).trim();
            }
            // 如果不是默认名称，设置为自定义名称
            if (!cleanName.equals(type.getDisplayName())) {
                equipment.setCustomName(cleanName);
            }
        }

        // 解析Lore
        if (meta.hasLore()) {
            var lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    String clean = line.replaceAll("§[0-9a-fklmnor]", "");
                    
                    // 解析五行属性
                    if (clean.contains("五行属性:")) {
                        equipment.setElement(clean.replace("五行属性:", "").trim());
                    }
                    
                    // 解析基础属性
                    if (clean.contains("攻击力:")) {
                        equipment.setBaseAttack(extractNumber(clean));
                    } else if (clean.contains("防御力:")) {
                        equipment.setBaseDefense(extractNumber(clean));
                    } else if (clean.contains("生命值:")) {
                        equipment.setBaseHp(extractNumber(clean));
                    } else if (clean.contains("灵力值:")) {
                        equipment.setBaseQi(extractNumber(clean));
                    }
                }
            }
        }

        // 读取强化等级
        equipment.setEnhanceLevel(getEnhanceLevel(item));
        equipment.setDurability(100);

        return equipment;
    }

    /**
     * 从材质推测装备类型
     */
    private static EquipmentType guessEquipmentType(Material material) {
        return switch (material) {
            case DIAMOND_SWORD, IRON_SWORD, GOLDEN_SWORD, NETHERITE_SWORD -> EquipmentType.SWORD;
            case DIAMOND_AXE, IRON_AXE, GOLDEN_AXE, NETHERITE_AXE -> EquipmentType.AXE;
            case BOW -> EquipmentType.BOW;
            case DIAMOND_HELMET, IRON_HELMET, GOLDEN_HELMET, NETHERITE_HELMET -> EquipmentType.HELMET;
            case DIAMOND_CHESTPLATE, IRON_CHESTPLATE, GOLDEN_CHESTPLATE, NETHERITE_CHESTPLATE -> EquipmentType.CHESTPLATE;
            case DIAMOND_LEGGINGS, IRON_LEGGINGS, GOLDEN_LEGGINGS, NETHERITE_LEGGINGS -> EquipmentType.LEGGINGS;
            case DIAMOND_BOOTS, IRON_BOOTS, GOLDEN_BOOTS, NETHERITE_BOOTS -> EquipmentType.BOOTS;
            case GOLD_INGOT -> EquipmentType.RING;
            case CHAIN -> EquipmentType.NECKLACE;
            case TOTEM_OF_UNDYING -> EquipmentType.TALISMAN;
            default -> null;
        };
    }

    /**
     * 从DisplayName提取品质
     */
    private static String extractQuality(String displayName) {
        if (displayName.contains("神品")) return "神品";
        if (displayName.contains("仙品")) return "仙品";
        if (displayName.contains("宝品")) return "宝品";
        if (displayName.contains("灵品")) return "灵品";
        if (displayName.contains("凡品")) return "凡品";
        return "凡品";
    }

    /**
     * 从文本中提取数字
     */
    private static int extractNumber(String text) {
        try {
            String[] parts = text.split("[^0-9]");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    return Integer.parseInt(part);
                }
            }
        } catch (NumberFormatException e) {
            return 0;
        }
        return 0;
    }
}

