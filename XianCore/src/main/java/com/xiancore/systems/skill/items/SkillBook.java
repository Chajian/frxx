package com.xiancore.systems.skill.items;

import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillElement;
import com.xiancore.systems.skill.SkillType;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * 功法秘籍物品类
 * 用于创建和管理功法秘籍物品，玩家右键使用后可学习功法
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SkillBook {

    // PDC Keys
    private static final String KEY_ITEM_TYPE = "xian_item_type";
    private static final String KEY_SKILL_ID = "xian_skill_id";
    private static final String KEY_SKILL_LEVEL = "xian_skill_level";

    private final String skillId;
    private final int skillLevel;

    public SkillBook(String skillId, int skillLevel) {
        this.skillId = skillId;
        this.skillLevel = skillLevel;
    }

    /**
     * 创建功法秘籍物品
     *
     * @param plugin 插件实例
     * @param skill  功法对象
     * @param level  功法等级
     * @return 功法秘籍 ItemStack
     */
    public static ItemStack createSkillBook(Plugin plugin, Skill skill, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        if (meta == null) {
            return book;
        }

        // 设置显示名称
        String displayName = formatSkillBookName(skill, level);
        meta.setDisplayName(displayName);

        // 设置 Lore（描述）
        List<String> lore = createSkillBookLore(skill, level);
        meta.setLore(lore);

        // 添加发光效果（隐藏附魔标签）
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // 存储 NBT 数据
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey itemTypeKey = new NamespacedKey(plugin, KEY_ITEM_TYPE);
        NamespacedKey skillIdKey = new NamespacedKey(plugin, KEY_SKILL_ID);
        NamespacedKey skillLevelKey = new NamespacedKey(plugin, KEY_SKILL_LEVEL);

        pdc.set(itemTypeKey, PersistentDataType.STRING, "SKILL_BOOK");
        pdc.set(skillIdKey, PersistentDataType.STRING, skill.getId());
        pdc.set(skillLevelKey, PersistentDataType.INTEGER, level);

        book.setItemMeta(meta);
        return book;
    }

    /**
     * 检查物品是否为功法秘籍
     *
     * @param plugin 插件实例
     * @param item   物品
     * @return 是否为功法秘籍
     */
    public static boolean isSkillBook(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey itemTypeKey = new NamespacedKey(plugin, KEY_ITEM_TYPE);

        String itemType = pdc.get(itemTypeKey, PersistentDataType.STRING);
        return "SKILL_BOOK".equals(itemType);
    }

    /**
     * 从物品中解析功法秘籍信息
     *
     * @param plugin 插件实例
     * @param item   物品
     * @return 功法秘籍对象，如果不是功法秘籍则返回 null
     */
    public static SkillBook fromItemStack(Plugin plugin, ItemStack item) {
        if (!isSkillBook(plugin, item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey skillIdKey = new NamespacedKey(plugin, KEY_SKILL_ID);
        NamespacedKey skillLevelKey = new NamespacedKey(plugin, KEY_SKILL_LEVEL);

        String skillId = pdc.get(skillIdKey, PersistentDataType.STRING);
        Integer skillLevel = pdc.get(skillLevelKey, PersistentDataType.INTEGER);

        if (skillId == null || skillLevel == null) {
            return null;
        }

        return new SkillBook(skillId, skillLevel);
    }

    /**
     * 格式化功法秘籍名称
     */
    private static String formatSkillBookName(Skill skill, int level) {
        // 根据功法等级确定品质颜色
        String qualityColor = getQualityColor(level);
        String elementPrefix = getElementPrefix(skill.getElement());

        return String.format("%s%s《%s》%s",
                qualityColor,
                elementPrefix,
                skill.getName(),
                ChatColor.GRAY + " [Lv." + level + "]");
    }

    /**
     * 创建功法秘籍的 Lore
     */
    private static List<String> createSkillBookLore(Skill skill, int level) {
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━");

        // 功法类型
        lore.add(ChatColor.GOLD + "类型：" + ChatColor.WHITE + skill.getType().getDisplayName());

        // 功法属性
        if (skill.getElement() != null && skill.getElement() != SkillElement.NEUTRAL) {
            lore.add(ChatColor.GOLD + "属性：" + getElementColor(skill.getElement()) + skill.getElement().getDisplayName());
        }

        // 功法等级
        lore.add(ChatColor.GOLD + "等级：" + ChatColor.WHITE + level + "/" + skill.getMaxLevel());

        // 需求境界
        if (skill.getRequiredRealm() != null && !skill.getRequiredRealm().isEmpty()) {
            lore.add(ChatColor.GOLD + "需求境界：" + ChatColor.YELLOW + skill.getRequiredRealm());
        }

        // 需求等级
        if (skill.getRequiredLevel() > 0) {
            lore.add(ChatColor.GOLD + "需求等级：" + ChatColor.YELLOW + skill.getRequiredLevel());
        }

        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━");

        // 功法描述
        if (skill.getDescription() != null && !skill.getDescription().isEmpty()) {
            // 分割长描述
            String[] descLines = wrapText(skill.getDescription(), 28);
            for (String line : descLines) {
                lore.add(ChatColor.GRAY + line);
            }
            lore.add("");
        }

        // 功法效果
        addEffectLore(lore, skill, level);

        lore.add(ChatColor.GRAY + "━━━━━━━━━━━━━━━");
        lore.add(ChatColor.YELLOW + "✦ 右键使用学习此功法");

        return lore;
    }

    /**
     * 添加功法效果到 Lore
     */
    private static void addEffectLore(List<String> lore, Skill skill, int level) {
        // 伤害
        if (skill.getBaseDamage() > 0) {
            double damage = skill.calculateDamage(level);
            lore.add(ChatColor.RED + "⚔ 伤害：" + ChatColor.WHITE + String.format("%.0f", damage));
        }

        // 治疗
        if (skill.getBaseHealing() > 0) {
            double healing = skill.calculateHealing(level);
            lore.add(ChatColor.GREEN + "❤ 治疗：" + ChatColor.WHITE + String.format("%.0f", healing));
        }

        // 灵气消耗
        int qiCost = skill.calculateQiCost(level);
        lore.add(ChatColor.AQUA + "◆ 灵气消耗：" + ChatColor.WHITE + qiCost);

        // 冷却时间
        int cooldown = skill.calculateCooldown(level);
        lore.add(ChatColor.LIGHT_PURPLE + "⏱ 冷却：" + ChatColor.WHITE + cooldown + "秒");

        // 作用范围
        if (skill.getBaseRange() > 0) {
            double range = skill.calculateRange(level);
            lore.add(ChatColor.YELLOW + "◎ 范围：" + ChatColor.WHITE + String.format("%.0f", range) + "格");
        }

        // 持续时间
        if (skill.getBaseDuration() > 0) {
            int duration = skill.calculateDuration(level);
            lore.add(ChatColor.DARK_AQUA + "⌛ 持续：" + ChatColor.WHITE + duration + "秒");
        }
    }

    /**
     * 根据等级获取品质颜色
     */
    private static String getQualityColor(int level) {
        if (level >= 9) return ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD; // 神品
        if (level >= 7) return ChatColor.GOLD + "" + ChatColor.BOLD;         // 仙品
        if (level >= 5) return ChatColor.BLUE + "";                          // 宝品
        if (level >= 3) return ChatColor.GREEN + "";                         // 灵品
        return ChatColor.WHITE + "";                                         // 凡品
    }

    /**
     * 获取元素前缀
     */
    private static String getElementPrefix(SkillElement element) {
        if (element == null || element == SkillElement.NEUTRAL) {
            return "";
        }

        return switch (element) {
            case FIRE -> ChatColor.RED + "[火] " + ChatColor.RESET;
            case WATER -> ChatColor.BLUE + "[水] " + ChatColor.RESET;
            case WOOD -> ChatColor.GREEN + "[木] " + ChatColor.RESET;
            case METAL -> ChatColor.GRAY + "[金] " + ChatColor.RESET;
            case EARTH -> ChatColor.YELLOW + "[土] " + ChatColor.RESET;
            case THUNDER -> ChatColor.LIGHT_PURPLE + "[雷] " + ChatColor.RESET;
            case ICE -> ChatColor.AQUA + "[冰] " + ChatColor.RESET;
            case WIND -> ChatColor.WHITE + "[风] " + ChatColor.RESET;
            default -> "";
        };
    }

    /**
     * 获取元素颜色
     */
    private static ChatColor getElementColor(SkillElement element) {
        return switch (element) {
            case FIRE -> ChatColor.RED;
            case WATER -> ChatColor.BLUE;
            case WOOD -> ChatColor.GREEN;
            case METAL -> ChatColor.GRAY;
            case EARTH -> ChatColor.YELLOW;
            case THUNDER -> ChatColor.LIGHT_PURPLE;
            case ICE -> ChatColor.AQUA;
            case WIND -> ChatColor.WHITE;
            default -> ChatColor.WHITE;
        };
    }

    /**
     * 文本换行工具
     *
     * @param text      文本
     * @param maxLength 每行最大长度
     * @return 分割后的文本行
     */
    private static String[] wrapText(String text, int maxLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxLength) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }
}
