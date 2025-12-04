package com.xiancore.systems.skill;

import lombok.Getter;

/**
 * 功法类型枚举
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public enum SkillType {

    // 攻击类
    ATTACK("攻击", "造成伤害的功法"),
    RANGED_ATTACK("远程攻击", "远距离攻击功法"),
    AOE_ATTACK("群体攻击", "范围伤害功法"),

    // 防御类
    DEFENSE("防御", "提升防御力的功法"),
    SHIELD("护盾", "创建护盾的功法"),
    DODGE("闪避", "提升闪避率的功法"),

    // 治疗类
    HEAL("治疗", "恢复生命值的功法"),
    REGENERATION("再生", "持续恢复的功法"),

    // 辅助类
    BUFF("增益", "提升属性的功法"),
    DEBUFF("减益", "削弱敌人的功法"),
    CONTROL("控制", "控制敌人的功法"),

    // 移动类
    MOVEMENT("移动", "位移类功法"),
    TELEPORT("传送", "瞬间移动功法"),

    // 特殊类
    SUMMON("召唤", "召唤生物的功法"),
    TRANSFORM("变身", "改变形态的功法"),
    PASSIVE("被动", "被动效果功法");

    private final String displayName;
    private final String description;

    SkillType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 根据名称获取类型
     */
    public static SkillType fromString(String name) {
        for (SkillType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equals(name)) {
                return type;
            }
        }
        return ATTACK; // 默认
    }
}
