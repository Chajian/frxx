package com.xiancore.systems.skill;

import lombok.Data;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * 功法数据类
 * 存储功法的基本信息和属性
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class Skill {

    // 基本信息
    private String id;              // 功法ID (唯一标识)
    private String name;            // 功法名称
    private String description;     // 功法描述
    private SkillType type;         // 功法类型
    private SkillElement element;   // 五行属性

    // 等级相关
    private int maxLevel;           // 最大等级
    private int baseLevel;          // 基础等级 (学习时的等级)

    // 需求相关
    private String requiredRealm;   // 需求境界
    private int requiredLevel;      // 需求玩家等级
    private List<String> requiredSkills; // 前置功法

    // 消耗相关
    private int baseQiCost;         // 基础灵气消耗
    private int baseCooldown;       // 基础冷却时间(秒)

    // 效果相关
    private double baseDamage;      // 基础伤害
    private double baseHealing;     // 基础治疗
    private int baseDuration;       // 基础持续时间(秒)
    private double baseRange;       // 基础范围

    // 升级相关
    private int baseUpgradeCost;    // 基础升级消耗(灵石)
    private int upgradeSkillPoints; // 升级所需功法点

    // 特殊效果
    private List<String> specialEffects; // 特殊效果列表

    // 显示相关
    private Material displayMaterial; // 显示材质
    private boolean glowing;          // 是否发光

    // MythicMobs 集成
    private String mythicSkillId;     // MythicMobs 技能 ID
    private boolean useMythic;        // 是否使用 MythicMobs 技能

    /**
     * 构造函数
     */
    public Skill() {
        this.maxLevel = 10;
        this.baseLevel = 1;
        this.requiredSkills = new ArrayList<>();
        this.specialEffects = new ArrayList<>();
        this.displayMaterial = Material.BOOK;
        this.glowing = false;
        this.useMythic = false;
    }

    /**
     * 构造函数
     *
     * @param id   功法ID
     * @param name 功法名称
     * @param type 功法类型
     */
    public Skill(String id, String name, SkillType type) {
        this();
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /**
     * 根据等级计算灵气消耗
     *
     * @param level 功法等级
     * @return 灵气消耗
     */
    public int calculateQiCost(int level) {
        return baseQiCost + (level - 1) * 10;
    }

    /**
     * 根据等级计算冷却时间
     *
     * @param level 功法等级
     * @return 冷却时间(秒)
     */
    public int calculateCooldown(int level) {
        // 等级越高,冷却时间越短(最多减少30%)
        double reduction = Math.min(0.3, (level - 1) * 0.03);
        return (int) (baseCooldown * (1 - reduction));
    }

    /**
     * 根据等级计算伤害
     *
     * @param level 功法等级
     * @return 伤害值
     */
    public double calculateDamage(int level) {
        return baseDamage * (1 + (level - 1) * 0.5);
    }

    /**
     * 根据等级计算治疗量
     *
     * @param level 功法等级
     * @return 治疗量
     */
    public double calculateHealing(int level) {
        return baseHealing * (1 + (level - 1) * 0.5);
    }

    /**
     * 根据等级计算范围
     *
     * @param level 功法等级
     * @return 范围
     */
    public double calculateRange(int level) {
        return baseRange + (level - 1) * 0.5;
    }

    /**
     * 根据等级计算持续时间
     *
     * @param level 功法等级
     * @return 持续时间（秒）
     */
    public int calculateDuration(int level) {
        return baseDuration + (level - 1) * 2;
    }

    /**
     * 根据等级计算升级消耗
     *
     * @param currentLevel 当前等级
     * @return 升级消耗(灵石)
     */
    public int calculateUpgradeCost(int currentLevel) {
        return (int) (baseUpgradeCost * Math.pow(1.5, currentLevel - 1));
    }

    /**
     * 根据等级计算升级所需功法点
     *
     * @param currentLevel 当前等级
     * @return 功法点
     */
    public int calculateUpgradeSkillPoints(int currentLevel) {
        return upgradeSkillPoints * currentLevel;
    }

    /**
     * 检查是否满足学习条件
     *
     * @param playerRealm  玩家境界
     * @param playerLevel  玩家等级
     * @param learnedSkills 已学功法列表
     * @return 是否满足条件
     */
    public boolean canLearn(String playerRealm, int playerLevel, List<String> learnedSkills) {
        // 检查境界要求
        if (requiredRealm != null && !checkRealmRequirement(playerRealm)) {
            return false;
        }

        // 检查等级要求
        if (playerLevel < requiredLevel) {
            return false;
        }

        // 检查前置功法
        if (requiredSkills != null && !requiredSkills.isEmpty()) {
            for (String reqSkill : requiredSkills) {
                if (!learnedSkills.contains(reqSkill)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 检查境界是否满足要求
     */
    private boolean checkRealmRequirement(String playerRealm) {
        // 实现境界比较逻辑：玩家的境界必须 >= 需求的境界
        String[] realms = {"炼气期", "筑基期", "结丹期", "元婴期", "化神期", "炼虚期", "合体期", "大乘期"};

        // 如果没有设置需求，则默认允许
        if (requiredRealm == null || requiredRealm.isEmpty()) {
            return true;
        }

        int playerRealmIndex = -1;
        int requiredRealmIndex = -1;

        for (int i = 0; i < realms.length; i++) {
            if (realms[i].equals(playerRealm)) {
                playerRealmIndex = i;
            }
            if (realms[i].equals(requiredRealm)) {
                requiredRealmIndex = i;
            }
        }

        // 如果找不到对应的境界，默认检查失败
        if (playerRealmIndex == -1 || requiredRealmIndex == -1) {
            return false;
        }

        return playerRealmIndex >= requiredRealmIndex;
    }

    /**
     * 获取功法的详细描述
     *
     * @param level 功法等级
     * @return 描述列表
     */
    public List<String> getDetailedLore(int level) {
        List<String> lore = new ArrayList<>();

        lore.add("§e等级: §f" + level + "/" + maxLevel);
        lore.add("§e类型: §f" + type.getDisplayName());

        if (element != null) {
            lore.add("§e属性: §f" + element.getDisplayName());
        }

        lore.add("");
        lore.add("§7" + description);
        lore.add("");

        if (baseDamage > 0) {
            lore.add("§c伤害: §f" + String.format("%.1f", calculateDamage(level)));
        }

        if (baseHealing > 0) {
            lore.add("§a治疗: §f" + String.format("%.1f", calculateHealing(level)));
        }

        if (baseRange > 0) {
            lore.add("§e范围: §f" + String.format("%.1f", calculateRange(level)) + "格");
        }

        lore.add("§b消耗: §f" + calculateQiCost(level) + " 灵气");
        lore.add("§e冷却: §f" + calculateCooldown(level) + "秒");

        if (!specialEffects.isEmpty()) {
            lore.add("");
            lore.add("§6特殊效果:");
            for (String effect : specialEffects) {
                lore.add("§7- " + effect);
            }
        }

        return lore;
    }

    // ==================== 显式 Getter 方法 ====================

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SkillType getType() {
        return type;
    }

    public SkillElement getElement() {
        return element;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getBaseLevel() {
        return baseLevel;
    }

    public String getRequiredRealm() {
        return requiredRealm;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public int getBaseQiCost() {
        return baseQiCost;
    }

    public int getBaseCooldown() {
        return baseCooldown;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public double getBaseHealing() {
        return baseHealing;
    }

    public int getBaseDuration() {
        return baseDuration;
    }

    public double getBaseRange() {
        return baseRange;
    }

    public int getBaseUpgradeCost() {
        return baseUpgradeCost;
    }

    public int getUpgradeSkillPoints() {
        return upgradeSkillPoints;
    }

    public List<String> getSpecialEffects() {
        return specialEffects;
    }

    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public String getMythicSkillId() {
        return mythicSkillId;
    }

    public boolean isUseMythic() {
        return useMythic;
    }

    // ==================== 显式 Setter 方法 ====================

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(SkillType type) {
        this.type = type;
    }

    public void setElement(SkillElement element) {
        this.element = element;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public void setBaseLevel(int baseLevel) {
        this.baseLevel = baseLevel;
    }

    public void setRequiredRealm(String requiredRealm) {
        this.requiredRealm = requiredRealm;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public void setBaseQiCost(int baseQiCost) {
        this.baseQiCost = baseQiCost;
    }

    public void setBaseCooldown(int baseCooldown) {
        this.baseCooldown = baseCooldown;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    public void setBaseHealing(double baseHealing) {
        this.baseHealing = baseHealing;
    }

    public void setBaseDuration(int baseDuration) {
        this.baseDuration = baseDuration;
    }

    public void setBaseRange(double baseRange) {
        this.baseRange = baseRange;
    }

    public void setBaseUpgradeCost(int baseUpgradeCost) {
        this.baseUpgradeCost = baseUpgradeCost;
    }

    public void setUpgradeSkillPoints(int upgradeSkillPoints) {
        this.upgradeSkillPoints = upgradeSkillPoints;
    }

    public void setSpecialEffects(List<String> specialEffects) {
        this.specialEffects = specialEffects;
    }

    public void setDisplayMaterial(Material displayMaterial) {
        this.displayMaterial = displayMaterial;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public void setMythicSkillId(String mythicSkillId) {
        this.mythicSkillId = mythicSkillId;
    }

    public void setUseMythic(boolean useMythic) {
        this.useMythic = useMythic;
    }
}
