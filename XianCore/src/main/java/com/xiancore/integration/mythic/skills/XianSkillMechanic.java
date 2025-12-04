package com.xiancore.integration.mythic.skills;

import com.xiancore.XianCore;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 修仙技能机制
 * 用于 MythicMobs 调用修仙系统的技能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class XianSkillMechanic implements ITargetedEntitySkill {

    private final String skillId;
    private final double damage;
    private final String element;

    /**
     * 构造函数
     *
     * @param config MythicMobs 配置行
     */
    public XianSkillMechanic(MythicLineConfig config) {
        this.skillId = config.getString("skill", "default");
        this.damage = config.getDouble("damage", 100.0);
        this.element = config.getString("element", "none");
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata metadata, AbstractEntity target) {
        try {
            Entity caster = metadata.getCaster().getEntity().getBukkitEntity();
            Entity targetEntity = target.getBukkitEntity();

            // 检查目标是否是生物实体
            if (!(targetEntity instanceof LivingEntity livingTarget)) {
                return SkillResult.INVALID_TARGET;
            }

            // 应用技能效果
            applySkillEffect(caster, livingTarget);

            return SkillResult.SUCCESS;

        } catch (Exception e) {
            XianCore.getInstance().getLogger().severe("执行修仙技能时发生错误: " + e.getMessage());
            e.printStackTrace();
            return SkillResult.ERROR;
        }
    }

    /**
     * 应用技能效果
     *
     * @param caster 施法者
     * @param target 目标
     */
    private void applySkillEffect(Entity caster, LivingEntity target) {
        // 基础伤害
        double finalDamage = damage;

        // 如果施法者是玩家，考虑其修炼境界
        if (caster instanceof Player player) {
            // 从玩家数据中获取境界加成
            // PlayerData data = XianCore.getInstance().getDataManager().loadPlayerData(player.getUniqueId());
            // finalDamage *= getRealm DamageMultiplier(data.getRealm());
        }

        // 应用元素伤害
        finalDamage *= getElementMultiplier(target);

        // 造成伤害
        target.damage(finalDamage, caster);

        // 特效
        applyVisualEffects(target);
    }

    /**
     * 获取元素伤害倍率
     *
     * @param target 目标
     * @return 伤害倍率
     */
    private double getElementMultiplier(LivingEntity target) {
        // 这里可以实现五行相克逻辑
        return switch (element.toLowerCase()) {
            case "fire" -> 1.2;
            case "water" -> 1.1;
            case "wood" -> 1.0;
            case "metal" -> 1.15;
            case "earth" -> 1.05;
            default -> 1.0;
        };
    }

    /**
     * 应用视觉效果
     *
     * @param target 目标
     */
    private void applyVisualEffects(LivingEntity target) {
        // 可以在这里添加粒子效果、声音等
        // 例如：target.getWorld().spawnParticle(Particle.FLAME, target.getLocation(), 20);
    }
}
