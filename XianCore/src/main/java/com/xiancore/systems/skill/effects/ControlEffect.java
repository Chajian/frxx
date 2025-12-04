package com.xiancore.systems.skill.effects;

import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillEffect;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * 控制效果
 * 限制目标移动或行动（冰冻、定身、击退等）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ControlEffect implements SkillEffect {

    @Override
    public boolean apply(Player caster, LivingEntity target, Skill skill, int level) {
        if (!canApply(caster, target)) {
            return false;
        }

        // 计算效果持续时间（转换为 tick，1 秒 = 20 tick）
        int duration = skill.calculateDuration(level) * 20;

        // 根据功法元素类型应用不同控制效果
        applyControlByElement(caster, target, skill, duration, level);

        if (target instanceof Player player) {
            player.sendMessage(String.format("§c你被控制了，持续 %d 秒", duration / 20));
        }

        return true;
    }

    @Override
    public String getEffectType() {
        return "CONTROL";
    }

    @Override
    public String getDescription(Skill skill, int level) {
        int duration = skill.calculateDuration(level);
        return String.format("控制目标，限制行动，持续 §c%d §7秒", duration);
    }

    /**
     * 根据功法元素类型应用不同的控制效果
     */
    private void applyControlByElement(Player caster, LivingEntity target, Skill skill, int duration, int level) {
        if (skill.getElement() == null) {
            // 无元素：应用默认定身效果
            applyRootEffect(target, duration);
            return;
        }

        switch (skill.getElement()) {
            case FIRE -> {
                // 火系：击退效果（爆炸冲击）
                applyKnockbackEffect(caster, target, level);
                // 短暂眩晕（缓慢）
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration / 2, 3));
            }
            case WATER -> {
                // 水系：束缚效果（缓慢移动）
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 4));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, 2));
            }
            case WOOD -> {
                // 木系：缠绕效果（禁锢+跳跃限制）
                applyRootEffect(target, duration);
                // 负跳跃效果（无法跳跃）
                target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 128));
            }
            case METAL -> {
                // 金系：击飞效果（向上击飞）
                applyLaunchEffect(target, level);
                // 空中缓慢下降
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, duration, 0));
            }
            case EARTH -> {
                // 土系：沉重效果（无法移动和跳跃）
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10));
                target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 250));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, 2));
            }
            case ICE -> {
                // 冰系：冰冻效果（完全禁锢）
                applyFreezeEffect(target, duration);
            }
            case THUNDER -> {
                // 雷系：眩晕效果（失明+缓慢）
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 3));
                // NAUSEA在某些版本中叫CONFUSION
                try {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration, 0));
                } catch (Exception e) {
                    // 如果不支持该效果，跳过
                }
            }
            case WIND -> {
                // 风系：吹飞效果（水平击退）
                applyWindBlowEffect(caster, target, level);
                // 漂浮效果
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration / 4, 1));
            }
            case DARK -> {
                // 暗系：恐惧效果（失明+缓慢+虚弱）
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 1));
            }
            case LIGHT -> {
                // 光系：致盲效果（强光致盲+缓慢）
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0));
            }
            default -> {
                // 默认定身效果
                applyRootEffect(target, duration);
            }
        }
    }

    /**
     * 应用定身效果（禁止移动）
     */
    private void applyRootEffect(LivingEntity target, int duration) {
        // 极慢速度 + 无法跳跃 + 挖掘疲劳
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 6));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 200));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, 3));
    }

    /**
     * 应用冰冻效果（完全禁锢）
     */
    private void applyFreezeEffect(LivingEntity target, int duration) {
        // 完全无法移动：最高级缓慢 + 无法跳跃 + 挖掘疲劳
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 10));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, 250));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, 5));
        
        // 发光效果表示被冰冻
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0));
    }

    /**
     * 应用击退效果
     */
    private void applyKnockbackEffect(Player caster, LivingEntity target, int level) {
        // 计算击退方向（从施法者指向目标）
        Location casterLoc = caster.getLocation();
        Location targetLoc = target.getLocation();
        
        Vector direction = targetLoc.toVector().subtract(casterLoc.toVector()).normalize();
        
        // 击退强度随等级增加
        double knockbackStrength = 0.5 + (level * 0.1);
        direction.multiply(knockbackStrength);
        
        // 添加向上的分量
        direction.setY(0.3 + (level * 0.05));
        
        // 应用速度
        target.setVelocity(direction);
    }

    /**
     * 应用击飞效果（向上）
     */
    private void applyLaunchEffect(LivingEntity target, int level) {
        // 向上击飞
        double launchStrength = 0.8 + (level * 0.1);
        Vector velocity = target.getVelocity();
        velocity.setY(launchStrength);
        target.setVelocity(velocity);
    }

    /**
     * 应用风吹效果（水平击退）
     */
    private void applyWindBlowEffect(Player caster, LivingEntity target, int level) {
        // 计算风吹方向
        Location casterLoc = caster.getLocation();
        Location targetLoc = target.getLocation();
        
        Vector direction = targetLoc.toVector().subtract(casterLoc.toVector()).normalize();
        
        // 风吹强度（水平方向更强）
        double windStrength = 1.0 + (level * 0.15);
        direction.multiply(windStrength);
        
        // 保持一定的向上分量，避免卡地面
        direction.setY(0.2);
        
        // 应用速度
        target.setVelocity(direction);
    }
}
