package com.xiancore.systems.skill.items;

import com.xiancore.systems.skill.Skill;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * 功法秘籍工厂类
 * 负责创建和解析功法秘籍
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillBookFactory {

    private static Plugin plugin;

    /**
     * 初始化工厂
     *
     * @param pluginInstance 插件实例
     */
    public static void initialize(Plugin pluginInstance) {
        plugin = pluginInstance;
    }

    /**
     * 从功法创建秘籍（默认1级）
     *
     * @param skill 功法对象
     * @return 秘籍物品
     */
    public static ItemStack createSkillBook(Skill skill) {
        return createSkillBook(skill, 1);
    }

    /**
     * 从功法创建秘籍（指定等级）
     *
     * @param skill 功法对象
     * @param level 功法等级
     * @return 秘籍物品
     */
    public static ItemStack createSkillBook(Skill skill, int level) {
        if (plugin == null) {
            throw new IllegalStateException("SkillBookFactory not initialized!");
        }
        return SkillBook.createSkillBook(plugin, skill, level);
    }

    /**
     * 判断物品是否为功法秘籍
     *
     * @param item 物品
     * @return 是否为秘籍
     */
    public static boolean isSkillBook(ItemStack item) {
        if (plugin == null) {
            throw new IllegalStateException("SkillBookFactory not initialized!");
        }
        return SkillBook.isSkillBook(plugin, item);
    }

    /**
     * 从物品解析功法秘籍
     *
     * @param item 物品
     * @return 功法秘籍对象，如果不是秘籍返回null
     */
    public static SkillBook fromItemStack(ItemStack item) {
        if (plugin == null) {
            throw new IllegalStateException("SkillBookFactory not initialized!");
        }
        return SkillBook.fromItemStack(plugin, item);
    }
}
