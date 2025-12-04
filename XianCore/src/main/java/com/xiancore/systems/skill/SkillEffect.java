package com.xiancore.systems.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 功法效果接口
 * 定义功法施放时的效果行为
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public interface SkillEffect {

    /**
     * 应用效果到目标
     *
     * @param caster 施法者
     * @param target 目标实体
     * @param skill  功法数据
     * @param level  功法等级
     * @return 是否成功应用效果
     */
    boolean apply(Player caster, LivingEntity target, Skill skill, int level);

    /**
     * 获取效果类型
     *
     * @return 效果类型名称
     */
    String getEffectType();

    /**
     * 获取效果描述
     *
     * @param skill 功法数据
     * @param level 功法等级
     * @return 效果描述
     */
    String getDescription(Skill skill, int level);

    /**
     * 检查效果是否可以应用
     *
     * @param caster 施法者
     * @param target 目标实体
     * @return 是否可以应用
     */
    default boolean canApply(Player caster, LivingEntity target) {
        return target != null && !target.isDead();
    }
}
