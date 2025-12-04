package com.xiancore.systems.forge.items;

import com.xiancore.XianCore;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * MythicMobs 物品适配器
 * 负责生成基于 MythicMobs 模板的装备物品
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class MythicItemAdapter {

    private final XianCore plugin;
    private final MythicBukkit mythicMobs;

    // PDC Keys
    private static final String KEY_XIAN_UUID = "xian_uuid";
    private static final String KEY_XIAN_QUALITY = "xian_quality";
    private static final String KEY_XIAN_ENHANCE_LEVEL = "xian_enhance_level";
    private static final String KEY_XIAN_ELEMENT = "xian_element";
    private static final String KEY_XIAN_TYPE = "xian_type";
    private static final String KEY_XIAN_BASE_ATTACK = "xian_base_attack";
    private static final String KEY_XIAN_BASE_DEFENSE = "xian_base_defense";
    private static final String KEY_XIAN_BASE_HP = "xian_base_hp";
    private static final String KEY_XIAN_BASE_QI = "xian_base_qi";

    public MythicItemAdapter(XianCore plugin) {
        this.plugin = plugin;
        this.mythicMobs = plugin.getMythicIntegration() != null ? 
                plugin.getMythicIntegration().getMythicMobs() : null;
    }

    /**
     * 检查 MythicMobs 是否可用
     */
    public boolean isAvailable() {
        return mythicMobs != null && plugin.getMythicIntegration().isInitialized();
    }

    /**
     * 从 Equipment 对象生成 MythicMobs 物品
     * 
     * @param equipment 装备对象
     * @return 生成的 ItemStack，如果失败返回 null
     */
    public ItemStack createMythicItem(Equipment equipment) {
        if (!isAvailable()) {
            plugin.getLogger().warning("MythicMobs 未初始化，无法生成物品");
            return null;
        }

        // 构建 MythicMobs 物品ID
        String mmItemId = buildMythicItemId(equipment);
        
        plugin.getLogger().fine("尝试生成 MythicMobs 物品: " + mmItemId);

        // 从 MythicMobs 获取物品
        ItemStack mmItem = getMythicItemStack(mmItemId);
        
        if (mmItem == null) {
            plugin.getLogger().fine("MythicMobs 物品不存在: " + mmItemId + "，尝试基础模板");
            
            // 回退：尝试无强化等级的基础模板
            String baseItemId = buildMythicItemId(
                equipment.getType(), 
                equipment.getQuality(), 
                equipment.getElement(), 
                0
            );
            mmItem = getMythicItemStack(baseItemId);
            
            if (mmItem == null) {
                plugin.getLogger().fine("MythicMobs 基础模板也不存在: " + baseItemId);
                return null;
            }
        }

        // 注入自定义数据到 PDC
        injectCustomData(mmItem, equipment);

        return mmItem;
    }

    /**
     * 从 MythicMobs 获取物品实例
     */
    private ItemStack getMythicItemStack(String itemId) {
        try {
            var itemOptional = mythicMobs.getItemManager().getItem(itemId);
            if (itemOptional.isEmpty()) {
                return null;
            }

            // 生成物品实例
            var abstractItem = itemOptional.get();
            var abstractItemStack = abstractItem.generateItemStack(1);
            
            // 使用 BukkitAdapter 转换为 Bukkit ItemStack
            return BukkitAdapter.adapt(abstractItemStack);

        } catch (Exception e) {
            plugin.getLogger().warning("生成 MythicMobs 物品失败: " + itemId + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 构建 MythicMobs 物品ID
     * 
     * 命名规则: xian_{type}_{quality}_{element}_lv{level}
     * 示例: xian_sword_spiritual_lv5
     */
    private String buildMythicItemId(Equipment equipment) {
        return buildMythicItemId(
            equipment.getType(),
            equipment.getQuality(),
            equipment.getElement(),
            equipment.getEnhanceLevel()
        );
    }

    /**
     * 构建 MythicMobs 物品ID（重载）
     */
    private String buildMythicItemId(EquipmentType type, String quality, String element, int enhanceLevel) {
        StringBuilder sb = new StringBuilder("xian_");

        // 装备类型
        sb.append(getTypeCode(type)).append("_");

        // 品质
        sb.append(getQualityCode(quality));

        // 五行属性（可选，如果有特殊五行变体）
        if (hasElementVariant(element)) {
            sb.append("_").append(getElementCode(element));
        }

        // 强化等级（按档位：0, 5, 10, 15, 20）
        int levelTier = (enhanceLevel / 5) * 5;  // 向下取整到5的倍数
        sb.append("_lv").append(levelTier);

        return sb.toString();
    }

    /**
     * 获取装备类型代码
     */
    private String getTypeCode(EquipmentType type) {
        return switch (type) {
            case SWORD -> "sword";
            case AXE -> "axe";
            case BOW -> "bow";
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case RING -> "ring";
            case NECKLACE -> "necklace";
            case TALISMAN -> "talisman";
        };
    }

    /**
     * 获取品质代码
     */
    private String getQualityCode(String quality) {
        return switch (quality) {
            case "凡品" -> "common";
            case "灵品" -> "spiritual";
            case "宝品" -> "treasure";
            case "仙品" -> "celestial";
            case "神品" -> "divine";
            default -> "common";
        };
    }

    /**
     * 获取五行代码
     */
    private String getElementCode(String element) {
        return switch (element) {
            case "火" -> "fire";
            case "水" -> "water";
            case "木" -> "wood";
            case "金" -> "metal";
            case "土" -> "earth";
            case "雷" -> "thunder";
            case "冰" -> "ice";
            case "风" -> "wind";
            default -> "";
        };
    }

    /**
     * 检查是否有五行变体
     * （目前只有火、水、雷、冰有特殊变体）
     */
    private boolean hasElementVariant(String element) {
        return element != null && (
            element.equals("火") || 
            element.equals("水") || 
            element.equals("雷") || 
            element.equals("冰")
        );
    }

    /**
     * 注入自定义数据到物品的 PDC
     */
    private void injectCustomData(ItemStack item, Equipment equipment) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        
        // 写入自定义标识
        NamespacedKey uuidKey = new NamespacedKey(plugin, KEY_XIAN_UUID);
        NamespacedKey qualityKey = new NamespacedKey(plugin, KEY_XIAN_QUALITY);
        NamespacedKey enhanceKey = new NamespacedKey(plugin, KEY_XIAN_ENHANCE_LEVEL);
        NamespacedKey elementKey = new NamespacedKey(plugin, KEY_XIAN_ELEMENT);
        NamespacedKey typeKey = new NamespacedKey(plugin, KEY_XIAN_TYPE);
        NamespacedKey attackKey = new NamespacedKey(plugin, KEY_XIAN_BASE_ATTACK);
        NamespacedKey defenseKey = new NamespacedKey(plugin, KEY_XIAN_BASE_DEFENSE);
        NamespacedKey hpKey = new NamespacedKey(plugin, KEY_XIAN_BASE_HP);
        NamespacedKey qiKey = new NamespacedKey(plugin, KEY_XIAN_BASE_QI);

        meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, equipment.getUuid());
        meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.STRING, equipment.getQuality());
        meta.getPersistentDataContainer().set(enhanceKey, PersistentDataType.INTEGER, equipment.getEnhanceLevel());
        meta.getPersistentDataContainer().set(elementKey, PersistentDataType.STRING, equipment.getElement());
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, equipment.getType().name());
        
        // 存储基础属性值（新装备）
        meta.getPersistentDataContainer().set(attackKey, PersistentDataType.INTEGER, equipment.getBaseAttack());
        meta.getPersistentDataContainer().set(defenseKey, PersistentDataType.INTEGER, equipment.getBaseDefense());
        meta.getPersistentDataContainer().set(hpKey, PersistentDataType.INTEGER, equipment.getBaseHp());
        meta.getPersistentDataContainer().set(qiKey, PersistentDataType.INTEGER, equipment.getBaseQi());

        item.setItemMeta(meta);

        plugin.getLogger().fine("已注入自定义数据到物品 PDC: UUID=" + equipment.getUuid() + 
                ", 攻击=" + equipment.getBaseAttack() + ", 防御=" + equipment.getBaseDefense() + 
                ", 生命=" + equipment.getBaseHp() + ", 灵力=" + equipment.getBaseQi());
    }

    /**
     * 从物品中读取自定义数据
     * 
     * @param item 物品
     * @return 装备 UUID，如果不是自定义装备返回 null
     */
    public static String getEquipmentUuid(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey uuidKey = new NamespacedKey(plugin, KEY_XIAN_UUID);
        return item.getItemMeta().getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
    }

    /**
     * 检查物品是否为仙家装备
     */
    public static boolean isXianEquipment(XianCore plugin, ItemStack item) {
        return getEquipmentUuid(plugin, item) != null;
    }

    /**
     * 从物品读取品质
     */
    public static String getQuality(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey qualityKey = new NamespacedKey(plugin, KEY_XIAN_QUALITY);
        return item.getItemMeta().getPersistentDataContainer().get(qualityKey, PersistentDataType.STRING);
    }

    /**
     * 从物品读取强化等级
     */
    public static int getEnhanceLevel(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }

        NamespacedKey enhanceKey = new NamespacedKey(plugin, KEY_XIAN_ENHANCE_LEVEL);
        Integer level = item.getItemMeta().getPersistentDataContainer().get(enhanceKey, PersistentDataType.INTEGER);
        return level != null ? level : 0;
    }

    /**
     * 从物品读取五行
     */
    public static String getElement(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey elementKey = new NamespacedKey(plugin, KEY_XIAN_ELEMENT);
        return item.getItemMeta().getPersistentDataContainer().get(elementKey, PersistentDataType.STRING);
    }

    /**
     * 从物品读取类型
     */
    public static EquipmentType getEquipmentType(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey typeKey = new NamespacedKey(plugin, KEY_XIAN_TYPE);
        String typeName = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        
        if (typeName == null) {
            return null;
        }

        try {
            return EquipmentType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 从物品读取基础攻击力
     * 
     * @param plugin 插件实例
     * @param item 物品
     * @return 基础攻击力，如果不是新装备（没有PDC属性值）返回 null
     */
    public static Integer getBaseAttack(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey attackKey = new NamespacedKey(plugin, KEY_XIAN_BASE_ATTACK);
        return item.getItemMeta().getPersistentDataContainer().get(attackKey, PersistentDataType.INTEGER);
    }

    /**
     * 从物品读取基础防御力
     * 
     * @param plugin 插件实例
     * @param item 物品
     * @return 基础防御力，如果不是新装备（没有PDC属性值）返回 null
     */
    public static Integer getBaseDefense(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey defenseKey = new NamespacedKey(plugin, KEY_XIAN_BASE_DEFENSE);
        return item.getItemMeta().getPersistentDataContainer().get(defenseKey, PersistentDataType.INTEGER);
    }

    /**
     * 从物品读取基础生命值
     * 
     * @param plugin 插件实例
     * @param item 物品
     * @return 基础生命值，如果不是新装备（没有PDC属性值）返回 null
     */
    public static Integer getBaseHp(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey hpKey = new NamespacedKey(plugin, KEY_XIAN_BASE_HP);
        return item.getItemMeta().getPersistentDataContainer().get(hpKey, PersistentDataType.INTEGER);
    }

    /**
     * 从物品读取基础灵力值
     * 
     * @param plugin 插件实例
     * @param item 物品
     * @return 基础灵力值，如果不是新装备（没有PDC属性值）返回 null
     */
    public static Integer getBaseQi(XianCore plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        NamespacedKey qiKey = new NamespacedKey(plugin, KEY_XIAN_BASE_QI);
        return item.getItemMeta().getPersistentDataContainer().get(qiKey, PersistentDataType.INTEGER);
    }
}

