package com.xiancore.systems.skill.effects;

import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * 减益效果
 * 为目标添加负面的药水效果（虚弱、缓慢等）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class DebuffEffect implements SkillEffect {

    @Override
    public boolean apply(Player caster, LivingEntity target, Skill skill, int level) {
        if (!canApply(caster, target)) {
            return false;
        }

        // 计算效果持续时间（转换为 tick，1 秒 = 20 tick）
        int duration = skill.calculateDuration(level) * 20;
        int amplifier = Math.min(level / 3, 3);  // 最多4级效果

        // 根据功法元素类型应用不同减益
        applyDebuffByElement(target, skill, duration, amplifier);

        if (target instanceof Player player) {
            player.sendMessage(String.format("§c你被施加了减益效果，持续 %d 秒", duration / 20));
        }

        return true;
    }

    @Override
    public String getEffectType() {
        return "DEBUFF";
    }

    @Override
    public String getDescription(Skill skill, int level) {
        int duration = skill.calculateDuration(level);
        return String.format("对目标施加减益效果，持续 §c%d §7秒", duration);
    }

    /**
     * 根据功法元素类型应用不同的减益
     */
    private void applyDebuffByElement(LivingEntity target, Skill skill, int duration, int amplifier) {
        if (skill.getElement() == null) {
            // 无元素：应用默认减益
            applyDefaultDebuff(target, duration, amplifier);
            return;
        }

        switch (skill.getElement()) {
            case FIRE -> {
                // 火系：凋零效果（持续伤害）
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, amplifier));
                // 额外添加燃烧效果
                target.setFireTicks(duration);
            }
            case WATER -> {
                // 水系：缓慢和挖掘疲劳
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, amplifier));
            }
            case WOOD -> {
                // 木系：中毒效果
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, duration, amplifier));
            }
            case METAL -> {
                // 金系：虚弱和缓慢（降低攻击和移动）
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amplifier - 1));
            }
            case EARTH -> {
                // 土系：挖掘疲劳和缓慢
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amplifier - 1));
            }
            case THUNDER -> {
                // 雷系：发光（暴露位置）和虚弱
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, amplifier));
            }
            case ICE -> {
                // 冰系：缓慢和挖掘疲劳（冰冻效果）
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, Math.min(amplifier + 1, 5)));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, amplifier));
            }
            case WIND -> {
                // 风系：漂浮和失明（视觉混乱）
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration / 4, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration / 2, 0));
            }
            case DARK -> {
                // 暗系：失明和虚弱
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, amplifier));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration / 2, amplifier - 1));
            }
            case LIGHT -> {
                // 光系：发光和缓慢（净化效果的反面）
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amplifier - 1));
            }
            default -> {
                // 默认减益
                applyDefaultDebuff(target, duration, amplifier);
            }
        }
    }

    /**
     * 应用默认减益效果
     */
    private void applyDefaultDebuff(LivingEntity target, int duration, int amplifier) {
        // 虚弱 + 缓慢
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, amplifier));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, amplifier - 1));
    }
}
