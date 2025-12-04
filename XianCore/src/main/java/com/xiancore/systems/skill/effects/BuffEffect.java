package com.xiancore.systems.skill.effects;

import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 增益效果
 * 为目标添加正面的药水效果（力量、速度等）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class BuffEffect implements SkillEffect {

    @Override
    public boolean apply(Player caster, LivingEntity target, Skill skill, int level) {
        if (!canApply(caster, target)) {
            return false;
        }

        // 计算效果持续时间（转换为 tick，1 秒 = 20 tick）
        int duration = (int) (skill.calculateDuration(level) * 20);
        int amplifier = Math.min(level / 3, 3);  // 最多4级

        // 根据功法类型应用不同增益
        applyBuffBySkillType(target, skill, duration, amplifier);

        if (target instanceof Player player) {
            player.sendMessage(String.format("§a你获得了增益效果，持续 %d 秒", duration / 20));
        }

        return true;
    }

    @Override
    public String getEffectType() {
        return "BUFF";
    }

    @Override
    public String getDescription(Skill skill, int level) {
        int duration = (int) skill.calculateDuration(level);
        return String.format("获得增益效果，持续 §a%d §7秒", duration);
    }

    /**
     * 根据功法类型应用不同的增益
     */
    private void applyBuffBySkillType(LivingEntity target, Skill skill, int duration, int amplifier) {
        switch (skill.getElement().toString().toUpperCase()) {
            case "FIRE" -> {
                // 火系：增加攻击力和速度
                target.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier - 1));
            }
            case "WATER" -> {
                // 水系：增加生命恢复和水下呼吸
                target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 0));
            }
            case "WOOD" -> {
                // 木系：增加防御和耐久
                target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, duration, amplifier));
            }
            case "METAL" -> {
                // 金系：增加采掘速度和防御
                target.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, amplifier - 1));
            }
            case "EARTH" -> {
                // 土系：增加缓慢抗性和防御
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, amplifier));
            }
            default -> {
                // 默认增益：力量和防御
                target.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 0));
            }
        }
    }
}
