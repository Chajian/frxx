package com.xiancore.systems.skill.effects;

import com.xiancore.XianCore;
import com.xiancore.systems.skill.ElementalAttribute;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillEffect;
import com.xiancore.systems.skill.SkillElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 伤害效果
 * 对目标造成伤害，支持五行相克系统
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class DamageEffect implements SkillEffect {

    @Override
    public boolean apply(Player caster, LivingEntity target, Skill skill, int level) {
        if (!canApply(caster, target)) {
            return false;
        }

        // 计算基础伤害
        double damage = skill.calculateDamage(level);

        // 应用五行相克修正
        damage = applyElementalModifier(damage, skill.getElement(), target, caster);

        // 造成伤害
        target.damage(damage, caster);

        return true;
    }

    @Override
    public String getEffectType() {
        return "DAMAGE";
    }

    @Override
    public String getDescription(Skill skill, int level) {
        double baseDamage = skill.calculateDamage(level);
        return String.format("对目标造成 %.0f 点伤害", baseDamage);
    }

    /**
     * 应用五行相克修正
     */
    private double applyElementalModifier(double baseDamage, SkillElement skillElement, LivingEntity target, Player caster) {
        XianCore plugin = XianCore.getInstance();
        
        // 检查是否启用五行相克系统
        boolean elementalEnabled = plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.elemental.enabled", false);

        if (!elementalEnabled) {
            // 未启用五行相克，使用简化的元素加成
            return applySimpleElementalBonus(baseDamage, skillElement);
        }

        // 启用五行相克系统
        SkillElement targetElement = ElementalAttribute.getEntityElement(target);

        if (targetElement == null) {
            // 目标无元素，不应用相克
            return baseDamage;
        }

        // 检查是否为PVP
        boolean isPVP = target instanceof Player;

        // 获取相克系数
        double multiplier = ElementalAttribute.getDamageMultiplier(
            skillElement, 
            targetElement, 
            isPVP
        );

        // 应用相克系数
        double finalDamage = baseDamage * multiplier;

        // 显示相克提示
        if (multiplier > 1.0) {
            caster.sendMessage("§a§l✦ 属性相克！§f伤害 ×" + String.format("%.1f", multiplier));
        } else if (multiplier < 1.0) {
            caster.sendMessage("§c§l✦ 属性相生！§f伤害 ×" + String.format("%.1f", multiplier));
        }

        return finalDamage;
    }

    /**
     * 简化的元素加成（未启用五行相克时使用）
     */
    private double applySimpleElementalBonus(double baseDamage, SkillElement element) {
        if (element == null) {
            return baseDamage;
        }

        return switch (element) {
            case FIRE -> baseDamage * 1.2;        // 火系伤害 +20%
            case WATER -> baseDamage * 1.1;       // 水系伤害 +10%
            case WOOD -> baseDamage * 1.0;        // 木系伤害 基础
            case METAL -> baseDamage * 1.15;      // 金系伤害 +15%
            case EARTH -> baseDamage * 1.05;      // 土系伤害 +5%
            case THUNDER -> baseDamage * 1.25;    // 雷系伤害 +25%
            case ICE -> baseDamage * 1.15;        // 冰系伤害 +15%
            case WIND -> baseDamage * 1.1;        // 风系伤害 +10%
            case DARK -> baseDamage * 1.3;        // 暗系伤害 +30%
            case LIGHT -> baseDamage * 1.2;       // 光系伤害 +20%
            default -> baseDamage;
        };
    }
}

