package com.xiancore.systems.sect.task;

import lombok.Getter;

/**
 * 宗门任务目标类型
 * 定义玩家可完成的各种任务目标
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public enum TaskObjective {

    /**
     * 击杀怪物
     * 参数: 怪物类型（MythicMobs ID），数量
     */
    KILL_MOB("击杀怪物", "§c⚔", "击杀 {amount} 个 {target}"),

    /**
     * 击杀特定类型的怪物
     * 参数: 怪物类型（ZOMBIE/SKELETON等），数量
     */
    KILL_MOB_TYPE("击杀特定怪物", "§c⚔", "击杀 {amount} 个 {target}"),

    /**
     * 收集物品
     * 参数: 物品类型，数量
     */
    COLLECT_ITEM("收集物品", "§e✦", "收集 {amount} 个 {target}"),

    /**
     * 捐献灵石
     * 参数: 灵石数量
     */
    DONATE_SPIRIT_STONE("捐献灵石", "§6◆", "向宗门捐献 {amount} 灵石"),

    /**
     * 境界突破
     * 参数: 突破次数
     */
    BREAKTHROUGH("境界突破", "§d⚡", "完成 {amount} 次境界突破"),

    /**
     * 参与宗门活动
     * 参数: 活动次数
     */
    ATTEND_EVENT("参与宗门活动", "§a✪", "参与 {amount} 次宗门活动"),

    /**
     * 完成 Quests 任务
     * 参数: Quests 任务名称，数量
     */
    COMPLETE_QUEST("完成任务", "§b★", "完成 {amount} 个宗门任务"),

    /**
     * 修炼
     * 参数: 修炼时长（分钟）
     */
    CULTIVATE("修炼", "§3◉", "修炼 {amount} 分钟"),

    /**
     * 炼制装备
     * 参数: 装备数量
     */
    FORGE_EQUIPMENT("炼制装备", "§7⚒", "炼制 {amount} 件装备"),

    /**
     * 使用功法
     * 参数: 使用次数
     */
    USE_SKILL("使用功法", "§5✧", "使用功法 {amount} 次"),

    /**
     * 组队副本
     * 参数: 副本次数
     */
    TEAM_DUNGEON("组队副本", "§9◈", "完成 {amount} 次组队副本"),

    /**
     * 在线时长
     * 参数: 在线时长（分钟）
     */
    ONLINE_TIME("在线时长", "§2⌚", "在线 {amount} 分钟");

    private final String displayName;      // 显示名称
    private final String icon;             // 图标
    private final String descriptionTemplate; // 描述模板

    TaskObjective(String displayName, String icon, String descriptionTemplate) {
        this.displayName = displayName;
        this.icon = icon;
        this.descriptionTemplate = descriptionTemplate;
    }

    /**
     * 格式化任务描述
     *
     * @param target 目标对象（如怪物名称、物品名称）
     * @param amount 目标数量
     * @return 格式化后的描述
     */
    public String formatDescription(String target, int amount) {
        return descriptionTemplate
                .replace("{target}", target)
                .replace("{amount}", String.valueOf(amount));
    }

    /**
     * 格式化任务描述（仅数量）
     *
     * @param amount 目标数量
     * @return 格式化后的描述
     */
    public String formatDescription(int amount) {
        return descriptionTemplate
                .replace("{amount}", String.valueOf(amount))
                .replace(" {target}", ""); // 移除未替换的 target
    }

    /**
     * 从字符串解析任务目标（增强版，支持更多格式）
     */
    public static TaskObjective fromString(String str) {
        if (str == null || str.isEmpty()) {
            return KILL_MOB; // 默认值
        }
        
        // 1. 尝试精确匹配枚举名称
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // 继续尝试其他方式
        }
        
        // 2. 尝试匹配显示名称
        for (TaskObjective objective : values()) {
            if (objective.displayName.equals(str)) {
                return objective;
            }
        }
        
        // 3. 尝试不区分大小写的枚举名称匹配
        for (TaskObjective objective : values()) {
            if (objective.name().equalsIgnoreCase(str)) {
                return objective;
            }
        }
        
        // 4. 尝试部分匹配（处理空格、下划线等）
        String normalized = str.toUpperCase().replace(" ", "_").replace("-", "_");
        for (TaskObjective objective : values()) {
            if (objective.name().equals(normalized)) {
                return objective;
            }
        }
        
        // 记录警告
        System.err.println("[TaskObjective] 未知的任务目标类型: " + str + ", 使用默认值 KILL_MOB");
        return KILL_MOB; // 默认返回击杀怪物
    }
    
    /**
     * 验证字符串是否为有效的任务目标
     */
    public static boolean isValid(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            fromString(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取所有可用的任务目标名称（用于配置提示）
     */
    public static String[] getAllNames() {
        TaskObjective[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }

    /**
     * 获取带图标的显示名称
     */
    public String getDisplayNameWithIcon() {
        return icon + " " + displayName;
    }
}
