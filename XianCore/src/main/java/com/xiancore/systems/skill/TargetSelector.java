package com.xiancore.systems.skill;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 目标选择器
 * 根据不同的技能类型自动选择合适的目标
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TargetSelector {

    /**
     * 根据技能类型自动选择目标
     *
     * @param caster 施法者
     * @param skill  功法
     * @return 目标列表
     */
    public List<LivingEntity> autoSelectTargets(Player caster, Skill skill) {
        return switch (skill.getType()) {
            // AOE攻击：范围内的敌对实体
            case AOE_ATTACK -> selectAOETargets(caster, skill.calculateRange(1), 10);
            
            // 治疗和增益：施法者自身
            case HEAL, REGENERATION, BUFF, DEFENSE, SHIELD, DODGE -> List.of(selectSelf(caster));
            
            // 单体攻击：视线内的单个目标
            case ATTACK, RANGED_ATTACK, DEBUFF, CONTROL -> {
                LivingEntity target = selectSingleTarget(caster, skill.calculateRange(1));
                yield target != null ? List.of(target) : List.of();
            }
            
            // 召唤、变身等：施法者自身
            case SUMMON, TRANSFORM, PASSIVE -> List.of(selectSelf(caster));
            
            // 移动、传送：施法者自身
            case MOVEMENT, TELEPORT -> List.of(selectSelf(caster));
            
            // 默认：施法者自身
            default -> List.of(selectSelf(caster));
        };
    }

    /**
     * 选择单体目标（视线检测）
     *
     * @param caster 施法者
     * @param range  范围
     * @return 目标实体，如果没有则返回null
     */
    public LivingEntity selectSingleTarget(Player caster, double range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                range,
                entity -> entity instanceof LivingEntity && entity != caster
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }

        return null;
    }

    /**
     * 选择AOE范围内的所有目标
     *
     * @param caster     施法者
     * @param range      范围
     * @param maxTargets 最大目标数量
     * @return 目标列表
     */
    public List<LivingEntity> selectAOETargets(Player caster, double range, int maxTargets) {
        Location center = caster.getLocation();
        
        return caster.getWorld().getNearbyEntities(center, range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != caster)
                .filter(entity -> !isFriendly(caster, entity))
                .map(entity -> (LivingEntity) entity)
                .limit(maxTargets)
                .collect(Collectors.toList());
    }

    /**
     * 选择AOE范围内的所有目标（基于指定位置）
     *
     * @param world      世界
     * @param center     中心点
     * @param range      范围
     * @param maxTargets 最大目标数量
     * @param caster     施法者（用于排除）
     * @return 目标列表
     */
    public List<LivingEntity> selectAOETargetsAtLocation(
            org.bukkit.World world,
            Location center,
            double range,
            int maxTargets,
            Player caster) {
        
        return world.getNearbyEntities(center, range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != caster)
                .filter(entity -> !isFriendly(caster, entity))
                .map(entity -> (LivingEntity) entity)
                .limit(maxTargets)
                .collect(Collectors.toList());
    }

    /**
     * 选择自身
     *
     * @param caster 施法者
     * @return 施法者自己
     */
    public LivingEntity selectSelf(Player caster) {
        return caster;
    }

    /**
     * 选择最近的敌对目标
     *
     * @param caster 施法者
     * @param range  搜索范围
     * @return 最近的敌对目标，如果没有则返回null
     */
    public LivingEntity selectNearestEnemy(Player caster, double range) {
        Location casterLoc = caster.getLocation();
        
        return caster.getWorld().getNearbyEntities(casterLoc, range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != caster)
                .filter(entity -> !isFriendly(caster, entity))
                .map(entity -> (LivingEntity) entity)
                .min((e1, e2) -> {
                    double dist1 = e1.getLocation().distanceSquared(casterLoc);
                    double dist2 = e2.getLocation().distanceSquared(casterLoc);
                    return Double.compare(dist1, dist2);
                })
                .orElse(null);
    }

    /**
     * 选择扇形范围内的目标
     *
     * @param caster     施法者
     * @param range      范围
     * @param angle      扇形角度（度）
     * @param maxTargets 最大目标数量
     * @return 目标列表
     */
    public List<LivingEntity> selectConeTargets(Player caster, double range, double angle, int maxTargets) {
        Location casterLoc = caster.getLocation();
        org.bukkit.util.Vector direction = casterLoc.getDirection();
        
        return caster.getWorld().getNearbyEntities(casterLoc, range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != caster)
                .filter(entity -> !isFriendly(caster, entity))
                .filter(entity -> {
                    // 计算目标是否在扇形范围内
                    org.bukkit.util.Vector toTarget = entity.getLocation().toVector()
                            .subtract(casterLoc.toVector())
                            .normalize();
                    
                    double dotProduct = direction.dot(toTarget);
                    double angleRad = Math.acos(dotProduct);
                    double angleDeg = Math.toDegrees(angleRad);
                    
                    return angleDeg <= angle / 2;
                })
                .map(entity -> (LivingEntity) entity)
                .limit(maxTargets)
                .collect(Collectors.toList());
    }

    /**
     * 选择视线内的所有目标
     *
     * @param caster     施法者
     * @param range      范围
     * @param maxTargets 最大目标数量
     * @return 目标列表
     */
    public List<LivingEntity> selectLineTargets(Player caster, double range, int maxTargets) {
        List<LivingEntity> targets = new ArrayList<>();
        Location eyeLoc = caster.getEyeLocation();
        org.bukkit.util.Vector direction = eyeLoc.getDirection();
        
        // 每0.5格检测一次
        double step = 0.5;
        for (double d = 0; d < range; d += step) {
            Location checkLoc = eyeLoc.clone().add(direction.clone().multiply(d));
            
            // 检查该位置附近的实体
            for (Entity entity : checkLoc.getWorld().getNearbyEntities(checkLoc, 1, 1, 1)) {
                if (entity instanceof LivingEntity livingEntity) {
                    if (entity != caster && !targets.contains(livingEntity) && !isFriendly(caster, entity)) {
                        targets.add(livingEntity);
                        
                        if (targets.size() >= maxTargets) {
                            return targets;
                        }
                    }
                }
            }
        }
        
        return targets;
    }

    /**
     * 检查目标是否为友方
     * 
     * @param caster 施法者
     * @param target 目标
     * @return 是否为友方
     */
    private boolean isFriendly(Player caster, Entity target) {
        // 玩家之间：根据队伍判断（暂时简化为所有玩家都是友方）
        if (target instanceof Player) {
            // TODO: 集成组队系统或PvP系统
            // 暂时返回false，允许攻击其他玩家
            return false;
        }
        
        // 对于非玩家实体：村民、宠物等视为友方
        if (target instanceof org.bukkit.entity.Villager ||
            target instanceof org.bukkit.entity.IronGolem) {
            return true;
        }
        
        // 检查是否为玩家的宠物
        if (target instanceof org.bukkit.entity.Tameable tameable) {
            return tameable.isTamed() && tameable.getOwner() instanceof Player;
        }
        
        return false;
    }

    /**
     * 选择半径范围内的友方目标（用于治疗、增益）
     *
     * @param caster     施法者
     * @param range      范围
     * @param maxTargets 最大目标数量
     * @return 目标列表
     */
    public List<LivingEntity> selectFriendlyTargets(Player caster, double range, int maxTargets) {
        Location center = caster.getLocation();
        
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(caster); // 始终包含施法者自己
        
        targets.addAll(
            caster.getWorld().getNearbyEntities(center, range, range, range).stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != caster)
                .filter(entity -> isFriendly(caster, entity))
                .map(entity -> (LivingEntity) entity)
                .limit(maxTargets - 1) // 减1是因为已经包含了施法者
                .collect(Collectors.toList())
        );
        
        return targets;
    }
}












