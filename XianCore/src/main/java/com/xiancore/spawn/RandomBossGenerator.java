package com.xiancore.spawn;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 随机Boss生成器 - 高级Boss生成系统
 * Random Boss Generator - Advanced Boss Generation System
 *
 * @author XianCore
 * @version 1.0
 */
public class RandomBossGenerator {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, BossTemplate> bossTemplates = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Boss模板
     */
    public static class BossTemplate {
        public String templateId;
        public String bossName;
        public BossRarity rarity;        // 稀有度
        public int minTier;              // 最小等级
        public int maxTier;              // 最大等级
        public double baseHealth;
        public double baseDamage;
        public List<String> skills;      // 技能列表
        public Map<String, Object> loot; // 掉落物品
        public double spawnWeight;       // 生成权重

        public enum BossRarity {
            COMMON(0.5),       // 普通
            UNCOMMON(0.3),     // 不普通
            RARE(0.15),        // 稀有
            EPIC(0.04),        // 史诗
            LEGENDARY(0.01);   // 传说

            public final double probability;

            BossRarity(double probability) {
                this.probability = probability;
            }
        }

        public BossTemplate(String templateId, String bossName, BossRarity rarity) {
            this.templateId = templateId;
            this.bossName = bossName;
            this.rarity = rarity;
            this.minTier = 1;
            this.maxTier = 5;
            this.baseHealth = 100.0;
            this.baseDamage = 10.0;
            this.skills = new ArrayList<>();
            this.loot = new HashMap<>();
            this.spawnWeight = rarity.probability;
        }
    }

    /**
     * 生成的Boss信息
     */
    public static class GeneratedBoss {
        public String bossId;
        public BossTemplate template;
        public int tier;
        public double health;
        public double damage;
        public String abilities;         // Boss能力描述
        public String modifiers;         // Boss修饰符
        public Map<String, Object> loot;
        public long generatedTime;

        public GeneratedBoss(String bossId, BossTemplate template, int tier) {
            this.bossId = bossId;
            this.template = template;
            this.tier = tier;
            this.health = template.baseHealth * (1 + tier * 0.3);
            this.damage = template.baseDamage * (1 + tier * 0.2);
            this.loot = new HashMap<>(template.loot);
            this.generatedTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("[T%d] %s - HP: %.0f, DMG: %.0f", tier, template.bossName, health, damage);
        }
    }

    /**
     * Boss修饰符组合
     */
    public static class BossModifier {
        public String modifierId;
        public String modifierName;
        public ModifierType type;
        public double healthMultiplier;
        public double damageMultiplier;
        public String description;

        public enum ModifierType {
            STAT_BOOST,      // 属性加强
            SKILL_ENHANCE,   // 技能强化
            RESISTANCE,      // 抗性
            SPECIAL_ABILITY  // 特殊能力
        }

        public BossModifier(String modifierId, String modifierName, ModifierType type) {
            this.modifierId = modifierId;
            this.modifierName = modifierName;
            this.type = type;
            this.healthMultiplier = 1.0;
            this.damageMultiplier = 1.0;
        }
    }

    /**
     * 构造函数
     */
    public RandomBossGenerator() {
        initializeDefaultTemplates();
        logger.info("✓ RandomBossGenerator已初始化");
    }

    /**
     * 初始化默认Boss模板
     */
    private void initializeDefaultTemplates() {
        // 普通Boss
        BossTemplate skeletonKing = new BossTemplate("skeleton_king", "骷髅王", BossTemplate.BossRarity.COMMON);
        skeletonKing.baseHealth = 80.0;
        skeletonKing.baseDamage = 8.0;
        skeletonKing.skills.addAll(List.of("skill-basic-attack", "skill-bone-throw"));
        bossTemplates.put("skeleton_king", skeletonKing);

        // 不普通Boss
        BossTemplate zombieLord = new BossTemplate("zombie_lord", "僵尸领主", BossTemplate.BossRarity.UNCOMMON);
        zombieLord.baseHealth = 120.0;
        zombieLord.baseDamage = 12.0;
        zombieLord.skills.addAll(List.of("skill-grab", "skill-summon-undead"));
        bossTemplates.put("zombie_lord", zombieLord);

        // 稀有Boss
        BossTemplate vampirePrince = new BossTemplate("vampire_prince", "吸血鬼王子", BossTemplate.BossRarity.RARE);
        vampirePrince.baseHealth = 150.0;
        vampirePrince.baseDamage = 15.0;
        vampirePrince.skills.addAll(List.of("skill-life-drain", "skill-shadow-form", "skill-bat-swarm"));
        bossTemplates.put("vampire_prince", vampirePrince);

        // 史诗Boss
        BossTemplate demonLord = new BossTemplate("demon_lord", "恶魔领主", BossTemplate.BossRarity.EPIC);
        demonLord.baseHealth = 200.0;
        demonLord.baseDamage = 20.0;
        demonLord.skills.addAll(List.of("skill-inferno", "skill-hellfire", "skill-chaos-magic"));
        bossTemplates.put("demon_lord", demonLord);

        // 传说Boss
        BossTemplate ancientDragon = new BossTemplate("ancient_dragon", "远古龙", BossTemplate.BossRarity.LEGENDARY);
        ancientDragon.baseHealth = 300.0;
        ancientDragon.baseDamage = 30.0;
        ancientDragon.skills.addAll(List.of("skill-dragon-breath", "skill-meteor-storm", "skill-time-warp"));
        bossTemplates.put("ancient_dragon", ancientDragon);

        logger.info("✓ 5个默认Boss模板已加载");
    }

    /**
     * 根据稀有度生成Boss
     */
    public GeneratedBoss generateBossByRarity(BossTemplate.BossRarity rarity, int tier) {
        // 从指定稀有度的模板中随机选择
        List<BossTemplate> compatibleTemplates = bossTemplates.values().stream()
                .filter(t -> t.rarity == rarity)
                .toList();

        if (compatibleTemplates.isEmpty()) {
            return generateRandomBoss(tier);
        }

        BossTemplate template = compatibleTemplates.get(random.nextInt(compatibleTemplates.size()));
        return createBossInstance(template, tier);
    }

    /**
     * 生成随机Boss (权重选择)
     */
    public GeneratedBoss generateRandomBoss(int tier) {
        // 根据稀有度权重随机选择
        double roll = random.nextDouble();
        double cumulative = 0.0;

        for (BossTemplate template : bossTemplates.values()) {
            cumulative += template.spawnWeight;
            if (roll <= cumulative) {
                return createBossInstance(template, tier);
            }
        }

        // 备选：返回第一个模板
        return createBossInstance(bossTemplates.values().iterator().next(), tier);
    }

    /**
     * 创建Boss实例
     */
    private GeneratedBoss createBossInstance(BossTemplate template, int tier) {
        int clampedTier = Math.max(template.minTier, Math.min(template.maxTier, tier));
        String bossId = "boss-" + System.currentTimeMillis() + "-" + random.nextInt(10000);

        GeneratedBoss boss = new GeneratedBoss(bossId, template, clampedTier);

        logger.info("✓ Boss已生成: " + boss.toString());

        return boss;
    }

    /**
     * 添加Boss模板
     */
    public void addTemplate(BossTemplate template) {
        bossTemplates.put(template.templateId, template);
        logger.info("✓ Boss模板已添加: " + template.bossName);
    }

    /**
     * 获取Boss模板
     */
    public BossTemplate getTemplate(String templateId) {
        return bossTemplates.get(templateId);
    }

    /**
     * 获取所有Boss模板
     */
    public Collection<BossTemplate> getAllTemplates() {
        return bossTemplates.values();
    }

    /**
     * 按稀有度获取模板
     */
    public List<BossTemplate> getTemplatesByRarity(BossTemplate.BossRarity rarity) {
        return bossTemplates.values().stream()
                .filter(t -> t.rarity == rarity)
                .toList();
    }

    /**
     * 生成带修饰符的Boss
     */
    public GeneratedBoss generateBossWithModifiers(int tier, List<BossModifier> modifiers) {
        GeneratedBoss boss = generateRandomBoss(tier);

        // 应用修饰符
        for (BossModifier modifier : modifiers) {
            boss.health *= modifier.healthMultiplier;
            boss.damage *= modifier.damageMultiplier;
        }

        // 记录修饰符
        boss.modifiers = modifiers.stream()
                .map(m -> m.modifierName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("无");

        logger.info("↻ Boss已应用修饰符: " + boss.bossId);

        return boss;
    }

    /**
     * 根据玩家数量生成Boss
     */
    public GeneratedBoss generateBossForPlayerCount(int playerCount, int baseAverageLevel) {
        // 根据玩家数量调整难度
        int tier = baseAverageLevel;

        if (playerCount == 1) {
            tier = Math.max(1, baseAverageLevel - 1);
        } else if (playerCount >= 5) {
            tier = baseAverageLevel + 1;
        }

        GeneratedBoss boss = generateRandomBoss(tier);

        // 根据玩家数量提高Boss属性
        double healthScaling = 1.0 + (playerCount - 1) * 0.2;
        boss.health *= healthScaling;

        return boss;
    }

    /**
     * 批量生成Boss
     */
    public List<GeneratedBoss> generateBosses(int count, int tier) {
        List<GeneratedBoss> bosses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            bosses.add(generateRandomBoss(tier));
        }
        return bosses;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_templates", bossTemplates.size());

        // 按稀有度统计
        Map<String, Integer> rarityCount = new HashMap<>();
        for (BossTemplate template : bossTemplates.values()) {
            rarityCount.merge(template.rarity.name(), 1, Integer::sum);
        }
        stats.put("templates_by_rarity", rarityCount);

        return stats;
    }

    /**
     * 重置模板库
     */
    public void reset() {
        bossTemplates.clear();
        initializeDefaultTemplates();
    }
}
