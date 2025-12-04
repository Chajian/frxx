package com.xiancore.boss.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Boss 技能管理器
 * 管理 Boss 的技能系统，包括技能触发、冷却等
 *
 * @author XianCore
 * @version 1.0
 */
public class BossSkillManager {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, List<BossSkill>> mobSkillMap; // mobType -> skills
    private final Map<UUID, Map<String, Long>> skillCooldowns; // entityUuid -> (skillId -> lastUseTime)

    /**
     * Boss 技能数据类
     */
    public static class BossSkill {
        public String skillId;
        public String triggerType; // distance, hp_percent, time
        public double triggerValue; // 距离、血量百分比或时间
        public int cooldownSeconds;
        public int priority; // 优先级，越高越优先执行

        public BossSkill(String skillId, String triggerType, double triggerValue,
                        int cooldownSeconds, int priority) {
            this.skillId = skillId;
            this.triggerType = triggerType;
            this.triggerValue = triggerValue;
            this.cooldownSeconds = cooldownSeconds;
            this.priority = priority;
        }
    }

    /**
     * 构造函数
     */
    public BossSkillManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.mobSkillMap = new ConcurrentHashMap<>();
        this.skillCooldowns = new ConcurrentHashMap<>();
    }

    /**
     * 为某个 Boss 类型注册技能
     */
    public void registerSkill(String mobType, BossSkill skill) {
        if (skill == null || mobType == null) {
            logger.warning("✗ 无法注册技能: 参数为 null");
            return;
        }

        mobSkillMap.computeIfAbsent(mobType, k -> new ArrayList<>()).add(skill);
        logger.info("✓ 已注册技能: " + mobType + " -> " + skill.skillId);
    }

    /**
     * 获取 Boss 的所有技能
     */
    public List<BossSkill> getBossSkills(String mobType) {
        return mobSkillMap.getOrDefault(mobType, new ArrayList<>());
    }

    /**
     * 应用技能到 Boss 实体
     */
    public void applySkills(LivingEntity boss, List<BossSkill> skills) {
        if (boss == null || skills == null || skills.isEmpty()) {
            return;
        }

        // 按优先级排序
        List<BossSkill> sortedSkills = new ArrayList<>(skills);
        sortedSkills.sort(Comparator.comparingInt(s -> s.priority));

        for (BossSkill skill : sortedSkills) {
            if (canUseSkill(boss, skill)) {
                triggerSkill(boss, skill);
                recordSkillUsage(boss, skill);
            }
        }
    }

    /**
     * 检查技能是否可以使用（检查冷却）
     */
    private boolean canUseSkill(LivingEntity boss, BossSkill skill) {
        UUID bossUuid = boss.getUniqueId();
        Map<String, Long> cooldowns = skillCooldowns.computeIfAbsent(bossUuid, k -> new ConcurrentHashMap<>());

        Long lastUseTime = cooldowns.get(skill.skillId);
        if (lastUseTime == null) {
            return true; // 从未使用过
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMillis = skill.cooldownSeconds * 1000L;
        return (currentTime - lastUseTime) >= cooldownMillis;
    }

    /**
     * 触发技能
     */
    private void triggerSkill(LivingEntity boss, BossSkill skill) {
        try {
            logger.info("→ 触发技能: " + skill.skillId + " (Boss: " + boss.getName() + ")");

            // 这里可以集成 MythicMobs 的技能触发系统
            // 或自定义技能效果
            // 例如: 发射火球、治疗、AOE 伤害等

            switch (skill.triggerType) {
                case "distance":
                    logger.info("  [距离触发] 范围: " + skill.triggerValue);
                    break;
                case "hp_percent":
                    logger.info("  [血量触发] 阈值: " + (skill.triggerValue * 100) + "%");
                    break;
                case "time":
                    logger.info("  [时间触发] 延迟: " + skill.triggerValue + "秒");
                    break;
            }
        } catch (Exception e) {
            logger.severe("✗ 触发技能异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 记录技能使用时间
     */
    private void recordSkillUsage(LivingEntity boss, BossSkill skill) {
        UUID bossUuid = boss.getUniqueId();
        Map<String, Long> cooldowns = skillCooldowns.computeIfAbsent(bossUuid, k -> new ConcurrentHashMap<>());
        cooldowns.put(skill.skillId, System.currentTimeMillis());
    }

    /**
     * 检查技能是否应该被触发（根据条件）
     */
    public boolean shouldTrigger(LivingEntity boss, BossSkill skill,
                                 List<LivingEntity> nearbyPlayers) {
        if (nearbyPlayers == null || nearbyPlayers.isEmpty()) {
            return false;
        }

        switch (skill.triggerType) {
            case "distance":
                // 检查是否有玩家在指定距离内
                for (LivingEntity player : nearbyPlayers) {
                    double distance = boss.getLocation().distance(player.getLocation());
                    if (distance <= skill.triggerValue) {
                        return true;
                    }
                }
                return false;

            case "hp_percent":
                // 检查血量是否低于指定百分比
                double healthPercent = boss.getHealth() / boss.getMaxHealth();
                return healthPercent <= skill.triggerValue;

            case "time":
                // 时间触发由外部计时系统处理
                return true;

            default:
                logger.warning("⚠ 未知的触发类型: " + skill.triggerType);
                return false;
        }
    }

    /**
     * 清除 Boss 的技能冷却
     */
    public void clearCooldown(LivingEntity boss, String skillId) {
        UUID bossUuid = boss.getUniqueId();
        Map<String, Long> cooldowns = skillCooldowns.get(bossUuid);
        if (cooldowns != null) {
            cooldowns.remove(skillId);
            logger.info("✓ 已清除冷却: " + boss.getName() + " -> " + skillId);
        }
    }

    /**
     * 清除 Boss 的所有技能冷却
     */
    public void clearAllCooldowns(LivingEntity boss) {
        UUID bossUuid = boss.getUniqueId();
        skillCooldowns.remove(bossUuid);
        logger.info("✓ 已清除所有冷却: " + boss.getName());
    }

    /**
     * 获取技能剩余冷却时间（秒）
     */
    public int getRemainingCooldown(LivingEntity boss, BossSkill skill) {
        UUID bossUuid = boss.getUniqueId();
        Map<String, Long> cooldowns = skillCooldowns.get(bossUuid);
        if (cooldowns == null) {
            return 0;
        }

        Long lastUseTime = cooldowns.get(skill.skillId);
        if (lastUseTime == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long cooldownMillis = skill.cooldownSeconds * 1000L;
        long elapsedMillis = currentTime - lastUseTime;
        long remainingMillis = cooldownMillis - elapsedMillis;

        return Math.max(0, (int) (remainingMillis / 1000));
    }

    /**
     * 获取所有已注册的 Boss 类型
     */
    public Set<String> getAllRegisteredMobs() {
        return mobSkillMap.keySet();
    }

    /**
     * 获取某个 Boss 的技能数量
     */
    public int getSkillCount(String mobType) {
        return mobSkillMap.getOrDefault(mobType, new ArrayList<>()).size();
    }

    /**
     * 打印技能信息
     */
    public void printSkillInfo(String mobType) {
        List<BossSkill> skills = getBossSkills(mobType);
        if (skills.isEmpty()) {
            logger.info("✗ 该 Boss 类型没有注册技能: " + mobType);
            return;
        }

        logger.info("=== " + mobType + " 的技能列表 ===");
        for (BossSkill skill : skills) {
            logger.info("  [" + skill.skillId + "] 触发方式: " + skill.triggerType +
                       " | 值: " + skill.triggerValue +
                       " | 冷却: " + skill.cooldownSeconds + "s" +
                       " | 优先级: " + skill.priority);
        }
    }

    /**
     * 清除缓存
     */
    public void clear() {
        mobSkillMap.clear();
        skillCooldowns.clear();
        logger.info("✓ 已清除所有技能数据");
    }
}
