package com.xiancore.ai;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 战斗AI - 威胁评估和技能选择
 * Combat AI - Threat Assessment and Skill Selection
 *
 * @author XianCore
 * @version 1.0
 */
public class CombatAI {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, BossAI> bossAIs = new ConcurrentHashMap<>();
    private final List<Skill> skillLibrary = new ArrayList<>();

    /**
     * 技能定义
     */
    public static class Skill {
        public String skillId;
        public String skillName;
        public SkillType type;           // ATTACK/DEFENSE/UTILITY
        public double damage;
        public double cooldown;          // 冷却时间 (秒)
        public double range;             // 范围
        public double manaCost;
        public String description;

        public enum SkillType {
            ATTACK, DEFENSE, UTILITY, HEAL
        }

        public Skill(String skillId, String skillName, SkillType type, double damage, double cooldown) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.type = type;
            this.damage = damage;
            this.cooldown = cooldown;
            this.range = 20.0;
            this.manaCost = 10.0;
        }
    }

    /**
     * 单个Boss的AI状态
     */
    public static class BossAI {
        public String bossId;
        public double health;
        public double maxHealth;
        public double mana;
        public double maxMana;
        public AIState state;
        public List<String> threatList;  // 威胁排行
        public Map<String, Double> skillCooldowns;
        public StrategyMode strategyMode; // 战斗策略

        public enum AIState {
            IDLE, PATROLLING, COMBAT, WEAK, DESPERATE
        }

        public enum StrategyMode {
            AGGRESSIVE,      // 激进：优先高伤害技能
            DEFENSIVE,       // 防御：优先保护自己
            BALANCED,        // 均衡：混合策略
            ADAPTIVE         // 适应：根据条件改变
        }

        public BossAI(String bossId, double maxHealth) {
            this.bossId = bossId;
            this.health = maxHealth;
            this.maxHealth = maxHealth;
            this.mana = 100;
            this.maxMana = 100;
            this.state = AIState.IDLE;
            this.threatList = new ArrayList<>();
            this.skillCooldowns = new ConcurrentHashMap<>();
            this.strategyMode = StrategyMode.BALANCED;
        }

        public double getHealthPercent() {
            return (health / maxHealth) * 100;
        }

        public double getManaPercent() {
            return (mana / maxMana) * 100;
        }
    }

    /**
     * 威胁评估结果
     */
    public static class ThreatAssessment {
        public Map<String, Double> playerThreats;  // 玩家威胁值
        public String primaryTarget;               // 主要目标
        public double totalThreat;
        public String recommendation;              // 推荐动作
        public double dangerLevel;                 // 0-100危险度

        public ThreatAssessment() {
            this.playerThreats = new HashMap<>();
            this.totalThreat = 0;
            this.dangerLevel = 0;
        }
    }

    /**
     * 技能选择结果
     */
    public static class SkillSelection {
        public Skill selectedSkill;
        public String targetPlayer;
        public double confidence;       // 0-1置信度
        public String reasoning;

        public SkillSelection(Skill skill, String target, double confidence, String reasoning) {
            this.selectedSkill = skill;
            this.targetPlayer = target;
            this.confidence = confidence;
            this.reasoning = reasoning;
        }
    }

    /**
     * 构造函数
     */
    public CombatAI() {
        initializeSkillLibrary();
        logger.info("✓ CombatAI已初始化");
    }

    /**
     * 初始化技能库
     */
    private void initializeSkillLibrary() {
        // 攻击技能
        skillLibrary.add(new Skill("skill-basic-attack", "基础攻击", Skill.SkillType.ATTACK, 25.0, 1.0));
        skillLibrary.add(new Skill("skill-power-strike", "强力一击", Skill.SkillType.ATTACK, 50.0, 5.0));
        skillLibrary.add(new Skill("skill-area-attack", "范围攻击", Skill.SkillType.ATTACK, 35.0, 8.0));

        // 防御技能
        skillLibrary.add(new Skill("skill-shield", "盾牌防御", Skill.SkillType.DEFENSE, 0, 6.0));
        skillLibrary.add(new Skill("skill-dodge", "闪避", Skill.SkillType.DEFENSE, 0, 4.0));

        // 恢复技能
        skillLibrary.add(new Skill("skill-heal", "自我治疗", Skill.SkillType.HEAL, 0, 10.0));
        skillLibrary.add(new Skill("skill-regenerate", "再生", Skill.SkillType.HEAL, 0, 15.0));

        // 工具技能
        skillLibrary.add(new Skill("skill-stun", "眩晕", Skill.SkillType.UTILITY, 0, 12.0));
        skillLibrary.add(new Skill("skill-slow", "减速", Skill.SkillType.UTILITY, 0, 8.0));

        logger.info("✓ 技能库已初始化: " + skillLibrary.size() + "个技能");
    }

    /**
     * 创建Boss AI
     */
    public BossAI createBossAI(String bossId, double maxHealth) {
        BossAI bossAI = new BossAI(bossId, maxHealth);
        bossAIs.put(bossId, bossAI);
        return bossAI;
    }

    /**
     * 威胁评估
     */
    public ThreatAssessment assessThreat(String bossId, Map<String, Double> playerDamage,
                                        Map<String, Double> playerDistance) {
        BossAI bossAI = bossAIs.get(bossId);
        if (bossAI == null) return new ThreatAssessment();

        ThreatAssessment assessment = new ThreatAssessment();

        // 计算每个玩家的威胁值
        for (Map.Entry<String, Double> entry : playerDamage.entrySet()) {
            String player = entry.getKey();
            double damage = entry.getValue();
            double distance = playerDistance.getOrDefault(player, 100.0);

            // 威胁值 = 伤害 * (1 / 距离)
            double threat = damage * (1.0 / Math.max(1.0, distance));
            assessment.playerThreats.put(player, threat);
            assessment.totalThreat += threat;
        }

        // 找出主要目标
        String primaryTarget = assessment.playerThreats.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        assessment.primaryTarget = primaryTarget;

        // 更新Boss的威胁列表
        bossAI.threatList = assessment.playerThreats.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        // 计算危险度
        assessment.dangerLevel = Math.min(100.0, assessment.totalThreat);

        // 生成推荐动作
        if (assessment.dangerLevel > 80) {
            assessment.recommendation = "DESPERATE";
        } else if (assessment.dangerLevel > 50) {
            assessment.recommendation = "AGGRESSIVE";
        } else {
            assessment.recommendation = "NORMAL";
        }

        return assessment;
    }

    /**
     * 选择要使用的技能
     */
    public SkillSelection selectSkill(String bossId, ThreatAssessment threat) {
        BossAI bossAI = bossAIs.get(bossId);
        if (bossAI == null) return null;

        // 更新AI状态
        updateBossState(bossAI, threat);

        // 根据策略和状态选择技能
        List<Skill> availableSkills = getAvailableSkills(bossAI);

        if (availableSkills.isEmpty()) {
            return new SkillSelection(skillLibrary.get(0), threat.primaryTarget, 0.5, "无可用技能");
        }

        SkillSelection selection = null;

        switch (bossAI.strategyMode) {
            case AGGRESSIVE:
                selection = selectAggressiveSkill(availableSkills, threat, bossAI);
                break;
            case DEFENSIVE:
                selection = selectDefensiveSkill(availableSkills, threat, bossAI);
                break;
            case BALANCED:
                selection = selectBalancedSkill(availableSkills, threat, bossAI);
                break;
            case ADAPTIVE:
                selection = selectAdaptiveSkill(availableSkills, threat, bossAI);
                break;
        }

        return selection != null ? selection :
                new SkillSelection(availableSkills.get(0), threat.primaryTarget, 0.5, "默认技能");
    }

    /**
     * 更新Boss状态
     */
    private void updateBossState(BossAI bossAI, ThreatAssessment threat) {
        double healthPercent = bossAI.getHealthPercent();

        if (healthPercent > 75) {
            bossAI.state = BossAI.AIState.COMBAT;
        } else if (healthPercent > 30) {
            bossAI.state = BossAI.AIState.COMBAT;
            bossAI.strategyMode = BossAI.StrategyMode.ADAPTIVE;
        } else if (healthPercent > 10) {
            bossAI.state = BossAI.AIState.WEAK;
            bossAI.strategyMode = BossAI.StrategyMode.DEFENSIVE;
        } else {
            bossAI.state = BossAI.AIState.DESPERATE;
            bossAI.strategyMode = BossAI.StrategyMode.ADAPTIVE;
        }
    }

    /**
     * 获取可用的技能
     */
    private List<Skill> getAvailableSkills(BossAI bossAI) {
        return skillLibrary.stream()
                .filter(skill -> {
                    // 检查冷却时间
                    Double cooldownRemaining = bossAI.skillCooldowns.get(skill.skillId);
                    if (cooldownRemaining != null && cooldownRemaining > 0) {
                        return false;
                    }

                    // 检查魔法值
                    return bossAI.mana >= skill.manaCost;
                })
                .toList();
    }

    /**
     * 激进策略 - 优先高伤害技能
     */
    private SkillSelection selectAggressiveSkill(List<Skill> available, ThreatAssessment threat, BossAI bossAI) {
        Skill selected = available.stream()
                .filter(s -> s.type == Skill.SkillType.ATTACK)
                .max(Comparator.comparingDouble(s -> s.damage))
                .orElse(available.get(0));

        return new SkillSelection(selected, threat.primaryTarget, 0.8,
                "激进策略：选择最高伤害技能");
    }

    /**
     * 防御策略 - 优先保护自己
     */
    private SkillSelection selectDefensiveSkill(List<Skill> available, ThreatAssessment threat, BossAI bossAI) {
        // 优先防御技能
        Skill selected = available.stream()
                .filter(s -> s.type == Skill.SkillType.DEFENSE)
                .findFirst()
                .or(() -> available.stream()
                        .filter(s -> s.type == Skill.SkillType.HEAL)
                        .findFirst())
                .orElse(available.get(0));

        return new SkillSelection(selected, threat.primaryTarget, 0.7,
                "防御策略：优先自我保护");
    }

    /**
     * 均衡策略 - 混合攻防
     */
    private SkillSelection selectBalancedSkill(List<Skill> available, ThreatAssessment threat, BossAI bossAI) {
        // 根据血量选择
        double healthPercent = bossAI.getHealthPercent();

        if (healthPercent < 50) {
            // 血量低于50%，优先防御/治疗
            return selectDefensiveSkill(available, threat, bossAI);
        } else {
            // 血量充足，优先进攻
            return selectAggressiveSkill(available, threat, bossAI);
        }
    }

    /**
     * 适应策略 - 根据条件改变
     */
    private SkillSelection selectAdaptiveSkill(List<Skill> available, ThreatAssessment threat, BossAI bossAI) {
        // 根据危险度调整策略
        if (threat.dangerLevel > 70) {
            return selectDefensiveSkill(available, threat, bossAI);
        } else if (threat.dangerLevel > 40) {
            return selectBalancedSkill(available, threat, bossAI);
        } else {
            return selectAggressiveSkill(available, threat, bossAI);
        }
    }

    /**
     * 记录技能使用 (设置冷却时间)
     */
    public void recordSkillUsage(String bossId, Skill skill) {
        BossAI bossAI = bossAIs.get(bossId);
        if (bossAI != null) {
            bossAI.skillCooldowns.put(skill.skillId, skill.cooldown);
            bossAI.mana -= skill.manaCost;
            logger.info("↻ 技能已使用: " + skill.skillName + " (" + bossId + ")");
        }
    }

    /**
     * 更新冷却时间 (每秒调用)
     */
    public void updateCooldowns(String bossId, double deltaTime) {
        BossAI bossAI = bossAIs.get(bossId);
        if (bossAI == null) return;

        bossAI.skillCooldowns.replaceAll((skill, remaining) -> Math.max(0, remaining - deltaTime));

        // 恢复魔法值
        bossAI.mana = Math.min(bossAI.maxMana, bossAI.mana + 5.0 * deltaTime);
    }

    /**
     * 获取Boss AI信息
     */
    public BossAI getBossAI(String bossId) {
        return bossAIs.get(bossId);
    }

    /**
     * 获取所有Boss AI
     */
    public Collection<BossAI> getAllBossAIs() {
        return bossAIs.values();
    }

    /**
     * 删除Boss AI
     */
    public void removeBossAI(String bossId) {
        bossAIs.remove(bossId);
    }

    /**
     * 获取技能库
     */
    public List<Skill> getSkillLibrary() {
        return new ArrayList<>(skillLibrary);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_bosses", bossAIs.size());
        stats.put("total_skills", skillLibrary.size());

        // 按策略分类统计
        Map<String, Integer> strategyCount = new HashMap<>();
        for (BossAI bossAI : bossAIs.values()) {
            strategyCount.merge(bossAI.strategyMode.name(), 1, Integer::sum);
        }
        stats.put("strategy_distribution", strategyCount);

        return stats;
    }
}
