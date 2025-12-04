package com.xiancore.systems.skill;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 功法系统
 * 负责管理功法的学习、升级、施放等功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SkillSystem {

    private final XianCore plugin;
    private final SkillCooldownManager cooldownManager;
    private final TargetSelector targetSelector;
    private final SkillBindManager bindManager;
    private CooldownDisplayManager cooldownDisplayManager;
    private final Map<String, Skill> skills; // 所有可用功法
    private final Map<String, SkillEffect> skillEffects; // 功法效果
    private boolean initialized = false;

    public SkillSystem(XianCore plugin) {
        this.plugin = plugin;
        this.cooldownManager = new SkillCooldownManager();
        this.targetSelector = new TargetSelector();
        this.bindManager = new SkillBindManager(plugin);
        this.skills = new HashMap<>();
        this.skillEffects = new HashMap<>();
    }

    /**
     * 初始化功法系统
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 初始化功法秘籍工厂
        com.xiancore.systems.skill.items.SkillBookFactory.initialize(plugin);

        // 初始化元素属性系统
        ElementalAttribute.initialize(plugin);

        // 初始化功法商店配置
        com.xiancore.systems.skill.shop.SkillShopConfig.initialize(plugin);

        // 加载功法数据
        loadSkills();

        // 注册默认效果
        registerDefaultEffects();

        // 启动冷却显示管理器
        cooldownDisplayManager = new CooldownDisplayManager(plugin, this);
        cooldownDisplayManager.start();

        initialized = true;
        plugin.getLogger().info("  §a✓ 功法系统初始化完成 (已加载 " + skills.size() + " 个功法)");
    }

    /**
     * 关闭功法系统
     */
    public void shutdown() {
        if (cooldownDisplayManager != null) {
            cooldownDisplayManager.stop();
        }
        if (bindManager != null) {
            bindManager.shutdown();
        }
    }

    /**
     * 从配置文件加载功法
     */
    private void loadSkills() {
        FileConfiguration config = plugin.getConfigManager().getConfig("skill");
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");

        if (skillsSection == null) {
            plugin.getLogger().warning("§c! 未找到功法配置 (skills)");
            plugin.getLogger().warning("§c! 配置文件内容：" + config.getKeys(false));
            return;
        }

        int loadedCount = 0;
        for (String skillId : skillsSection.getKeys(false)) {
            try {
                Skill skill = loadSkillFromConfig(skillId, skillsSection.getConfigurationSection(skillId));
                if (skill != null) {
                    skills.put(skillId, skill);
                    loadedCount++;
                    plugin.getLogger().info("  §a✓ 加载功法: " + skillId + " (" + skill.getName() + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("§c! 加载功法失败: " + skillId + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("§a成功加载 " + loadedCount + " 个功法");
    }

    /**
     * 从配置加载单个功法
     */
    private Skill loadSkillFromConfig(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        Skill skill = new Skill();
        skill.setId(id);
        skill.setName(section.getString("name", id));
        skill.setDescription(section.getString("description", ""));

        // 类型和属性
        skill.setType(SkillType.fromString(section.getString("type", "ATTACK")));
        skill.setElement(SkillElement.fromString(section.getString("element", "NEUTRAL")));

        // 等级相关
        skill.setMaxLevel(section.getInt("max-level", 10));
        skill.setBaseLevel(section.getInt("base-level", 1));

        // 需求
        skill.setRequiredRealm(section.getString("required.realm"));
        skill.setRequiredLevel(section.getInt("required.level", 0));
        skill.setRequiredSkills(section.getStringList("required.skills"));

        // 消耗
        skill.setBaseQiCost(section.getInt("cost.qi", 100));
        skill.setBaseCooldown(section.getInt("cost.cooldown", 10));

        // 效果
        skill.setBaseDamage(section.getDouble("effect.damage", 0));
        skill.setBaseHealing(section.getDouble("effect.healing", 0));
        skill.setBaseDuration(section.getInt("effect.duration", 0));
        skill.setBaseRange(section.getDouble("effect.range", 0));

        // 升级
        skill.setBaseUpgradeCost(section.getInt("upgrade.cost", 1000));
        skill.setUpgradeSkillPoints(section.getInt("upgrade.skill-points", 1));

        // 特殊效果
        skill.setSpecialEffects(section.getStringList("special-effects"));

        // MythicMobs 集成
        skill.setMythicSkillId(section.getString("mythic-skill-id"));
        skill.setUseMythic(section.getBoolean("use-mythic", skill.getMythicSkillId() != null));

        return skill;
    }

    /**
     * 注册默认功法效果
     */
    private void registerDefaultEffects() {
        // 注册伤害效果
        skillEffects.put("DAMAGE", new com.xiancore.systems.skill.effects.DamageEffect());

        // 注册治疗效果
        skillEffects.put("HEALING", new com.xiancore.systems.skill.effects.HealingEffect());

        // 注册增益效果
        skillEffects.put("BUFF", new com.xiancore.systems.skill.effects.BuffEffect());

        // 注册减益效果
        skillEffects.put("DEBUFF", new com.xiancore.systems.skill.effects.DebuffEffect());

        // 注册控制效果
        skillEffects.put("CONTROL", new com.xiancore.systems.skill.effects.ControlEffect());

        plugin.getLogger().info("  §a✓ 功法效果系统已初始化 (" + skillEffects.size() + " 种效果)");
    }

    /**
     * 学习功法
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 是否成功学习
     */
    public boolean learnSkill(Player player, String skillId) {
        // 检查功法是否存在
        Skill skill = skills.get(skillId);
        if (skill == null) {
            player.sendMessage("§c功法不存在!");
            return false;
        }

        // 获取玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查是否已学习
        if (data.getSkills().containsKey(skillId)) {
            player.sendMessage("§c你已经学习了这个功法!");
            return false;
        }

        // 检查遗忘锁
        if (cooldownManager.isForgetLocked(player, skillId)) {
            int remaining = cooldownManager.getRemainingForgetLock(player, skillId);
            int minutes = remaining / 60;
            int seconds = remaining % 60;
            String lockTime = minutes > 0 ? 
                (minutes + "分" + (seconds > 0 ? seconds + "秒" : "")) : 
                (seconds + "秒");
            
            player.sendMessage("§c该功法刚被遗忘，暂时无法重新学习!");
            player.sendMessage("§7剩余冷却时间: §e" + lockTime);
            return false;
        }

        // 检查学习条件（详细错误提示）
        List<String> learnedSkills = new ArrayList<>(data.getSkills().keySet());

        // 检查境界要求
        if (skill.getRequiredRealm() != null && !skill.getRequiredRealm().isEmpty()) {
            if (!checkRealmRequirement(data.getRealm(), skill.getRequiredRealm())) {
                player.sendMessage("§c境界不足!");
                player.sendMessage("§7需要: §e" + skill.getRequiredRealm());
                player.sendMessage("§7当前: §f" + data.getRealm());
                return false;
            }
        }

        // 检查等级要求
        if (data.getLevel() < skill.getRequiredLevel()) {
            player.sendMessage("§c等级不足!");
            player.sendMessage("§7需要: §e" + skill.getRequiredLevel());
            player.sendMessage("§7当前: §f" + data.getLevel());
            return false;
        }

        // 检查前置功法
        if (skill.getRequiredSkills() != null && !skill.getRequiredSkills().isEmpty()) {
            for (String reqSkillId : skill.getRequiredSkills()) {
                if (!learnedSkills.contains(reqSkillId)) {
                    Skill reqSkill = skills.get(reqSkillId);
                    String reqSkillName = reqSkill != null ? reqSkill.getName() : reqSkillId;
                    player.sendMessage("§c需要先学习前置功法: §e" + reqSkillName);
                    return false;
                }
            }
        }

        // 检查功法点
        int requiredPoints = skill.getUpgradeSkillPoints();
        if (data.getSkillPoints() < requiredPoints) {
            player.sendMessage("§c功法点不足!");
            player.sendMessage("§7需要: §e" + requiredPoints);
            player.sendMessage("§7当前: §f" + data.getSkillPoints());
            return false;
        }

        // 检查功法槽位限制
        if (!checkSkillSlots(player, data)) {
            return false;
        }

        // 扣除功法点
        data.removeSkillPoints(requiredPoints);

        // 学习功法
        data.learnSkill(skillId);

        // 增加活跃灵气
        data.addActiveQi(10);

        // 保存数据
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("§a§l========== 功法学习成功 ==========");
        player.sendMessage("§e功法: §f" + skill.getName());
        player.sendMessage("§e类型: §f" + skill.getType().getDisplayName());
        if (skill.getElement() != null) {
            player.sendMessage("§e属性: §f" + skill.getElement().getDisplayName());
        }
        player.sendMessage("§e初始等级: §f" + skill.getBaseLevel());
        player.sendMessage("§7消耗功法点: §f" + requiredPoints);
        player.sendMessage("§7活跃灵气 +10");
        player.sendMessage("§a§l================================");

        return true;
    }

    /**
     * 遗忘功法
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 是否成功遗忘
     */
    public boolean forgetSkill(Player player, String skillId) {
        // 检查功法是否存在
        Skill skill = skills.get(skillId);
        if (skill == null) {
            player.sendMessage("§c功法不存在!");
            return false;
        }

        // 获取玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查是否已学习
        if (!data.getSkills().containsKey(skillId)) {
            player.sendMessage("§c你还没有学习这个功法!");
            return false;
        }

        // 检查是否启用遗忘功能
        boolean forgetEnabled = plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.forget.enabled", true);
        
        if (!forgetEnabled) {
            player.sendMessage("§c功法遗忘功能已被禁用!");
            return false;
        }

        // 检查黑名单
        if (isSkillForgetBlacklisted(skillId)) {
            player.sendMessage("§c该功法不可遗忘!");
            player.sendMessage("§7功法: §f" + skill.getName());
            return false;
        }

        // 检查是否有其他功法依赖此功法
        List<String> dependentSkills = findDependentSkills(player, skillId);
        if (!dependentSkills.isEmpty()) {
            player.sendMessage("§c§l无法遗忘该功法!");
            player.sendMessage("§7以下功法依赖此功法:");
            for (String depSkillId : dependentSkills) {
                Skill depSkill = skills.get(depSkillId);
                String depSkillName = depSkill != null ? depSkill.getName() : depSkillId;
                player.sendMessage("§7  • §e" + depSkillName);
            }
            player.sendMessage("");
            player.sendMessage("§7请先遗忘依赖功法，或保留此功法");
            return false;
        }

        // 检查战斗状态（如果配置启用）
        boolean blockInCombat = plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.forget.block-when-combat", true);
        
        if (blockInCombat && isInCombat(player)) {
            player.sendMessage("§c战斗状态下无法遗忘功法!");
            return false;
        }

        // 获取当前等级
        int currentLevel = data.getSkills().get(skillId);

        // 计算返还
        int refundedSkillPoints = calculateSkillPointsRefund(skill, currentLevel);
        long refundedSpiritStones = calculateSpiritStonesRefund(skill, currentLevel);

        // 触发事件
        com.xiancore.systems.skill.events.SkillForgetEvent event = 
            new com.xiancore.systems.skill.events.SkillForgetEvent(
                player, skill, currentLevel, refundedSkillPoints, refundedSpiritStones
            );
        plugin.getServer().getPluginManager().callEvent(event);

        // 检查事件是否被取消
        if (event.isCancelled()) {
            player.sendMessage("§c无法遗忘功法: " + 
                (event.getCancelReason() != null ? event.getCancelReason() : "未知原因"));
            return false;
        }

        // 使用事件中可能被修改的返还值
        refundedSkillPoints = event.getRefundedSkillPoints();
        refundedSpiritStones = event.getRefundedSpiritStones();

        // 从玩家数据中移除功法（直接操作底层Map，而非副本）
        data.getLearnedSkills().remove(skillId);

        // 返还资源
        if (refundedSkillPoints > 0) {
            data.addSkillPoints(refundedSkillPoints);
        }
        if (refundedSpiritStones > 0) {
            data.addSpiritStones(refundedSpiritStones);
        }

        // 清理冷却
        cooldownManager.clearCooldown(player, skillId);

        // 设置遗忘锁（防止立即重学绕过冷却）
        int forgetLockSeconds = plugin.getConfigManager().getConfig("config")
                .getInt("skill.forget.relearn-lock-seconds", 300);
        
        if (forgetLockSeconds > 0) {
            cooldownManager.setForgetLock(player, skillId, forgetLockSeconds);
        }

        // 清理所有槽位绑定
        int unboundSlots = bindManager.unbindAllSlotsForSkill(player, skillId);

        // 保存数据
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("§c§l========== 功法遗忘成功 ==========");
        player.sendMessage("§e功法: §f" + skill.getName());
        player.sendMessage("§e等级: §f" + currentLevel);
        player.sendMessage("");
        
        if (refundedSkillPoints > 0 || refundedSpiritStones > 0) {
            player.sendMessage("§a返还资源:");
            if (refundedSkillPoints > 0) {
                player.sendMessage("§7  • 功法点: §f+" + refundedSkillPoints);
            }
            if (refundedSpiritStones > 0) {
                player.sendMessage("§7  • 灵石: §f+" + refundedSpiritStones);
            }
        } else {
            player.sendMessage("§7未返还任何资源");
        }
        
        player.sendMessage("");
        
        if (unboundSlots > 0) {
            player.sendMessage("§7已自动解绑 §f" + unboundSlots + " §7个快捷键槽位");
        }
        
        if (forgetLockSeconds > 0) {
            int minutes = forgetLockSeconds / 60;
            int seconds = forgetLockSeconds % 60;
            String lockTime = minutes > 0 ? 
                (minutes + "分" + (seconds > 0 ? seconds + "秒" : "")) : 
                (seconds + "秒");
            player.sendMessage("§7重新学习冷却: §e" + lockTime);
        }
        
        player.sendMessage("§c§l================================");

        // 日志记录
        plugin.getLogger().info(String.format(
            "玩家 %s 遗忘了功法 %s (等级 %d), 返还: %d功法点, %d灵石",
            player.getName(), skillId, currentLevel, refundedSkillPoints, refundedSpiritStones
        ));

        return true;
    }

    /**
     * 计算功法点返还
     */
    private int calculateSkillPointsRefund(Skill skill, int currentLevel) {
        // 获取返还比例（默认50%）
        double refundRate = plugin.getConfigManager().getConfig("config")
                .getDouble("skill.forget.refund.skill-points-rate", 0.5);

        if (refundRate <= 0) {
            return 0;
        }

        // 计算总消耗的功法点（学习 + 所有升级）
        int totalPoints = skill.getUpgradeSkillPoints(); // 学习时的基础消耗
        
        // 累加所有升级消耗
        for (int level = 1; level < currentLevel; level++) {
            totalPoints += skill.calculateUpgradeSkillPoints(level);
        }

        // 应用返还比例并向下取整
        return (int) (totalPoints * refundRate);
    }

    /**
     * 计算灵石返还
     */
    private long calculateSpiritStonesRefund(Skill skill, int currentLevel) {
        // 获取返还比例（默认0%，不返还灵石）
        double refundRate = plugin.getConfigManager().getConfig("config")
                .getDouble("skill.forget.refund.spirit-stones-rate", 0.0);

        if (refundRate <= 0) {
            return 0;
        }

        // 计算总消耗的灵石（所有升级）
        long totalCost = 0;
        for (int level = 1; level < currentLevel; level++) {
            totalCost += skill.calculateUpgradeCost(level);
        }

        // 应用返还比例并向下取整
        return (long) (totalCost * refundRate);
    }

    /**
     * 查找依赖指定功法的其他已学功法
     */
    private List<String> findDependentSkills(Player player, String skillId) {
        List<String> dependents = new ArrayList<>();
        
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return dependents;
        }

        // 遍历玩家已学的所有功法
        for (String learnedSkillId : data.getSkills().keySet()) {
            Skill learnedSkill = skills.get(learnedSkillId);
            if (learnedSkill == null) continue;

            // 检查该功法的前置需求中是否包含要遗忘的功法
            List<String> requiredSkills = learnedSkill.getRequiredSkills();
            if (requiredSkills != null && requiredSkills.contains(skillId)) {
                dependents.add(learnedSkillId);
            }
        }

        return dependents;
    }

    /**
     * 检查功法是否在遗忘黑名单中
     */
    private boolean isSkillForgetBlacklisted(String skillId) {
        List<String> blacklist = plugin.getConfigManager().getConfig("config")
                .getStringList("skill.forget.blacklist");
        
        return blacklist != null && blacklist.contains(skillId);
    }

    /**
     * 检查玩家是否在战斗状态
     * TODO: 集成战斗标记系统
     */
    private boolean isInCombat(Player player) {
        // 简单实现：检查玩家最近是否受到伤害
        // 更完善的实现应该集成专门的战斗标记系统
        
        // 暂时返回 false，允许遗忘
        // 实际项目中应该检查玩家的战斗标记
        return false;
    }

    /**
     * 升级功法
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 是否成功升级
     */
    public boolean upgradeSkill(Player player, String skillId) {
        // 检查功法是否存在
        Skill skill = skills.get(skillId);
        if (skill == null) {
            player.sendMessage("§c功法不存在!");
            return false;
        }

        // 获取玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查是否已学习
        if (!data.getSkills().containsKey(skillId)) {
            player.sendMessage("§c你还没有学习这个功法!");
            return false;
        }

        // 获取当前等级
        int currentLevel = data.getSkills().get(skillId);
        if (currentLevel >= skill.getMaxLevel()) {
            player.sendMessage("§c功法已达到最大等级!");
            return false;
        }

        // 检查功法点
        int requiredPoints = skill.calculateUpgradeSkillPoints(currentLevel);
        if (data.getSkillPoints() < requiredPoints) {
            player.sendMessage("§c功法点不足!");
            player.sendMessage("§7需要: §e" + requiredPoints);
            player.sendMessage("§7当前: §f" + data.getSkillPoints());
            return false;
        }

        // 检查灵石
        int requiredCost = skill.calculateUpgradeCost(currentLevel);
        if (data.getSpiritStones() < requiredCost) {
            player.sendMessage("§c灵石不足!");
            player.sendMessage("§7需要: §e" + requiredCost);
            player.sendMessage("§7当前: §f" + data.getSpiritStones());
            return false;
        }

        // 扣除消耗
        data.removeSkillPoints(requiredPoints);
        data.removeSpiritStones(requiredCost);

        // 升级功法（直接操作底层Map，而非副本）
        int newLevel = currentLevel + 1;
        data.getLearnedSkills().put(skillId, newLevel);

        // 增加活跃灵气（升级奖励随等级增加）
        int activeQiGain = Math.min(15, 3 + newLevel);
        data.addActiveQi(activeQiGain);

        // 保存数据
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("§a§l========== 功法升级成功 ==========");
        player.sendMessage("§e功法: §f" + skill.getName());
        player.sendMessage("§e等级: §f" + currentLevel + " §a→ §f" + newLevel);
        player.sendMessage("§7消耗: §f" + requiredPoints + " 功法点, " + requiredCost + " 灵石");
        player.sendMessage("§7活跃灵气 +" + activeQiGain);
        player.sendMessage("§a§l================================");

        return true;
    }

    /**
     * 施放功法（自动选择目标）
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 是否成功施放
     */
    public boolean castSkill(Player player, String skillId) {
        // 检查功法是否存在
        Skill skill = skills.get(skillId);
        if (skill == null) {
            player.sendMessage("§c功法不存在!");
            return false;
        }

        // 获取玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查是否已学习
        if (!data.getSkills().containsKey(skillId)) {
            player.sendMessage("§c你还没有学习这个功法!");
            return false;
        }

        // 获取功法等级
        int level = data.getSkills().get(skillId);

        // 检查冷却
        if (cooldownManager.isOnCooldown(player, skillId)) {
            int remaining = cooldownManager.getRemainingCooldown(player, skillId);
            player.sendMessage("§c功法冷却中! 剩余: §f" + remaining + " §c秒");
            return false;
        }

        // 检查灵气
        int qiCost = skill.calculateQiCost(level);
        if (data.getQi() < qiCost) {
            player.sendMessage("§c灵气不足!");
            player.sendMessage("§7需要: §e" + qiCost);
            player.sendMessage("§7当前: §f" + data.getQi());
            return false;
        }

        // 使用目标选择器自动选择目标
        List<LivingEntity> targets = targetSelector.autoSelectTargets(player, skill);
        
        if (targets.isEmpty()) {
            player.sendMessage("§c未找到有效目标!");
            return false;
        }

        // 扣除灵气
        data.setQi(data.getQi() - qiCost);

        // 对所有目标应用效果
        int successCount = 0;
        for (LivingEntity target : targets) {
            if (applySkillEffect(player, target, skill, level)) {
                successCount++;
            }
        }

        boolean success = successCount > 0;

        if (success) {
            // 设置冷却
            int cooldown = skill.calculateCooldown(level);
            cooldownManager.setCooldown(player, skillId, cooldown);

            // 增加活跃灵气（施放功法获得少量）
            data.addActiveQi(2);

            // 保存数据
            plugin.getDataManager().savePlayerData(data);

            player.sendMessage("§a施放功法: §e" + skill.getName());
            
            // 显示目标数量
            if (targets.size() > 1) {
                player.sendMessage("§7命中目标: §f" + successCount + "/" + targets.size());
            } else if (targets.get(0) != player) {
                player.sendMessage("§7目标: §f" + targets.get(0).getName());
            }
            
            player.sendMessage("§7消耗灵气: §f" + qiCost + " §7| 活跃灵气 +2");
        } else {
            // 施放失败,返还一半灵气
            data.setQi(data.getQi() + qiCost / 2);
            plugin.getDataManager().savePlayerData(data);
            player.sendMessage("§c功法施放失败!");
        }

        return success;
    }

    /**
     * 应用功法效果
     */
    private boolean applySkillEffect(Player caster, LivingEntity target, Skill skill, int level) {
        // 优先使用 MythicMobs 技能
        if (skill.isUseMythic() && skill.getMythicSkillId() != null) {
            return executeMythicSkill(caster, skill, level, target);
        }

        // 降级：使用 XianCore 内置效果
        return executeInternalSkill(caster, target, skill, level);
    }

    /**
     * 执行 MythicMobs 技能
     */
    private boolean executeMythicSkill(Player caster, Skill skill, int level, LivingEntity target) {
        try {
            // 检查 MythicMobs 是否可用
            if (plugin.getMythicIntegration() == null || !plugin.getMythicIntegration().isEnabled()) {
                plugin.getLogger().warning("MythicMobs 未启用，降级使用内置效果");
                return executeInternalSkill(caster, target, skill, level);
            }

            // 执行 MythicMobs 技能
            boolean success = plugin.getMythicIntegration().executeMythicSkill(
                    caster,
                    skill.getMythicSkillId(),
                    level,
                    target
            );

            if (!success) {
                plugin.getLogger().warning("MythicMobs 技能执行失败，降级使用内置效果");
                return executeInternalSkill(caster, target, skill, level);
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("执行 MythicMobs 技能时出错: " + e.getMessage());
            e.printStackTrace();
            // 降级使用内置效果
            return executeInternalSkill(caster, target, skill, level);
        }
    }

    /**
     * 执行 XianCore 内置效果
     */
    private boolean executeInternalSkill(Player caster, LivingEntity target, Skill skill, int level) {
        // 根据功法类型应用不同效果
        String effectType = skill.getType().toString();

        // 获取对应的效果处理器
        SkillEffect effect = null;

        switch (skill.getType()) {
            case ATTACK, RANGED_ATTACK, AOE_ATTACK -> effect = skillEffects.get("DAMAGE");
            case HEAL, REGENERATION -> effect = skillEffects.get("HEALING");
            case BUFF -> effect = skillEffects.get("BUFF");
            case DEBUFF -> effect = skillEffects.get("DEBUFF");
            case CONTROL -> effect = skillEffects.get("CONTROL");
        }

        // 如果找不到效果处理器，使用默认伤害效果
        if (effect == null) {
            effect = skillEffects.get("DAMAGE");
        }

        // 应用效果
        if (effect != null) {
            try {
                return effect.apply(caster, target, skill, level);
            } catch (Exception e) {
                plugin.getLogger().warning("应用功法效果失败: " + skill.getName() + " - " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        caster.sendMessage("§c功法效果未定义");
        return false;
    }

    /**
     * 获取功法
     *
     * @param skillId 功法ID
     * @return 功法对象
     */
    public Skill getSkill(String skillId) {
        return skills.get(skillId);
    }

    /**
     * 获取所有功法
     *
     * @return 功法映射
     */
    public Map<String, Skill> getAllSkills() {
        return new HashMap<>(skills);
    }

    /**
     * 获取玩家可学习的功法列表
     *
     * @param player 玩家
     * @return 可学习的功法列表
     */
    public List<Skill> getAvailableSkills(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return Collections.emptyList();
        }

        List<String> learnedSkills = new ArrayList<>(data.getSkills().keySet());
        String realm = data.getRealm();
        int level = data.getLevel();

        return skills.values().stream()
                .filter(skill -> !learnedSkills.contains(skill.getId()))
                .filter(skill -> skill.canLearn(realm, level, learnedSkills))
                .collect(Collectors.toList());
    }

    /**
     * 获取玩家已学习的功法
     *
     * @param player 玩家
     * @return 功法ID -> 等级
     */
    public Map<String, Integer> getPlayerSkills(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(data.getSkills());
    }

    /**
     * 获取玩家功法等级
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 功法等级,未学习返回0
     */
    public int getPlayerSkillLevel(Player player, String skillId) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return 0;
        }
        return data.getSkills().getOrDefault(skillId, 0);
    }

    /**
     * 清理所有过期的冷却
     * 应该定期调用
     */
    public void cleanupCooldowns() {
        cooldownManager.cleanupExpiredCooldowns();
    }

    /**
     * 检查境界是否满足要求
     *
     * @param playerRealm   玩家境界
     * @param requiredRealm 需求境界
     * @return 是否满足
     */
    private boolean checkRealmRequirement(String playerRealm, String requiredRealm) {
        String[] realms = {"炼气期", "筑基期", "结丹期", "元婴期", "化神期", "炼虚期", "合体期", "大乘期"};

        int playerIndex = -1;
        int requiredIndex = -1;

        for (int i = 0; i < realms.length; i++) {
            if (realms[i].equals(playerRealm)) {
                playerIndex = i;
            }
            if (realms[i].equals(requiredRealm)) {
                requiredIndex = i;
            }
        }

        if (playerIndex == -1 || requiredIndex == -1) {
            return false;
        }

        return playerIndex >= requiredIndex;
    }

    /**
     * 检查功法槽位是否已满
     *
     * @param player 玩家
     * @param data   玩家数据
     * @return 是否可以学习（true=可以学习）
     */
    private boolean checkSkillSlots(Player player, PlayerData data) {
        // 检查是否启用槽位限制
        boolean slotLimitEnabled = plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.slot-limit.enabled", false);

        if (!slotLimitEnabled) {
            return true; // 未启用限制，直接通过
        }

        int currentSlots = data.getSkills().size();
        int maxSlots = getMaxSkillSlots(player, data);

        if (currentSlots >= maxSlots) {
            player.sendMessage("§c§l功法槽位已满！");
            player.sendMessage("§7当前槽位: §f" + currentSlots + "/" + maxSlots);
            player.sendMessage("");
            player.sendMessage("§e解锁更多槽位的方式:");
            player.sendMessage("§7• §e提升境界 §7(当前: §f" + data.getRealm() + "§7)");

            // 显示下一境界的槽位数
            String nextRealm = getNextRealm(data.getRealm());
            if (nextRealm != null) {
                int nextSlots = plugin.getConfigManager().getConfig("config")
                        .getInt("skill.slot-limit.base-slots." + nextRealm, maxSlots);
                player.sendMessage("§7  → §a" + nextRealm + " §7可解锁 §a" + nextSlots + " §7个槽位");
            }

            // 如果有VIP系统
            if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
                player.sendMessage("§7• §e升级VIP §7获得额外槽位");
            }

            return false;
        }

        return true;
    }

    /**
     * 获取玩家最大功法槽位数
     *
     * @param player 玩家
     * @param data   玩家数据
     * @return 最大槽位数
     */
    public int getMaxSkillSlots(Player player, PlayerData data) {
        // 基础槽位（从配置读取）
        int baseSlots = plugin.getConfigManager().getConfig("config")
                .getInt("skill.slot-limit.base-slots." + data.getRealm(), 5);

        // VIP加成（可选）
        int vipBonus = getVIPSlotBonus(player);

        return baseSlots + vipBonus;
    }

    /**
     * 获取VIP槽位加成
     *
     * @param player 玩家
     * @return VIP加成槽位数
     */
    private int getVIPSlotBonus(Player player) {
        // TODO: 集成VIP系统
        // 这里是示例代码，需要根据实际VIP插件调整
        
        // 示例：如果有VIP插件
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            // 检查玩家权限组判断VIP等级
            if (player.hasPermission("xiancore.vip3")) {
                return plugin.getConfigManager().getConfig("config")
                        .getInt("skill.slot-limit.vip-bonus.vip3", 6);
            } else if (player.hasPermission("xiancore.vip2")) {
                return plugin.getConfigManager().getConfig("config")
                        .getInt("skill.slot-limit.vip-bonus.vip2", 4);
            } else if (player.hasPermission("xiancore.vip1")) {
                return plugin.getConfigManager().getConfig("config")
                        .getInt("skill.slot-limit.vip-bonus.vip1", 2);
            }
        }

        return 0; // 无VIP加成
    }

    /**
     * 获取下一个境界
     *
     * @param currentRealm 当前境界
     * @return 下一个境界，如果已是最高境界则返回null
     */
    private String getNextRealm(String currentRealm) {
        String[] realms = {"炼气期", "筑基期", "结丹期", "元婴期", "化神期", "炼虚期", "合体期", "大乘期"};

        for (int i = 0; i < realms.length - 1; i++) {
            if (realms[i].equals(currentRealm)) {
                return realms[i + 1];
            }
        }

        return null; // 已是最高境界
    }
}
