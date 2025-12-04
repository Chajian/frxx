package com.xiancore.systems.skill.effects;

import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 治疗效果
 * 对目标进行治疗
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class HealingEffect implements SkillEffect {

    @Override
    public boolean apply(Player caster, LivingEntity target, Skill skill, int level) {
        if (!canApply(caster, target)) {
            return false;
        }

        // 计算治疗量
        double healAmount = skill.calculateHealing(level);

        // 获取目标当前生命值和最大生命值
        double currentHealth = target.getHealth();
        double maxHealth = target.getMaxHealth();

        // 计算新的生命值（不超过最大值）
        double newHealth = Math.min(currentHealth + healAmount, maxHealth);

        // 应用治疗
        target.setHealth(newHealth);

        // 发送信息
        if (target instanceof Player player) {
            player.sendMessage(String.format("§a你被治疗了 §f%.0f §a生命值", healAmount));
        }

        return true;
    }

    @Override
    public String getEffectType() {
        return "HEALING";
    }

    @Override
    public String getDescription(Skill skill, int level) {
        double baseHealing = skill.calculateHealing(level);
        return String.format("恢复 §a%.0f §7点生命值", baseHealing);
    }
}
