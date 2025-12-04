package com.xiancore.systems.sect.task;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 宗门任务生成器
 * 根据玩家境界和任务类型生成任务
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectTaskGenerator {

    private final XianCore plugin;

    // 任务模板配置
    private final Map<SectTaskType, List<TaskTemplate>> templates;
    
    // 配置验证器
    private final TaskConfigValidator validator;
    
    // 配置文件名
    private static final String CONFIG_FILE = "sect_task.yml";
    
    // 难度系数（从配置加载）
    private double targetMultiplier = 0.1;
    private double rewardMultiplier = 0.2;
    
    // 境界映射（从配置加载）
    private Map<String, Integer> realmLevels = new HashMap<>();

    public SectTaskGenerator(XianCore plugin) {
        this.plugin = plugin;
        this.templates = new EnumMap<>(SectTaskType.class);
        this.validator = new TaskConfigValidator(plugin);
        initializeDefaultRealms();
    }

    /**
     * 初始化默认境界（防止配置文件缺失）
     */
    private void initializeDefaultRealms() {
        realmLevels.put("炼气期", 1);
        realmLevels.put("筑基期", 2);
        realmLevels.put("结丹期", 3);
        realmLevels.put("元婴期", 4);
        realmLevels.put("化神期", 5);
        realmLevels.put("炼虚期", 6);
        realmLevels.put("合体期", 7);
        realmLevels.put("大乘期", 8);
    }

    /**
     * 加载任务模板
     */
    public void loadTaskTemplates() {
        // 清空现有模板
        templates.clear();

        FileConfiguration config = plugin.getConfigManager().getConfig(CONFIG_FILE);
        
        // 验证配置文件
        TaskConfigValidator.ValidationResult validationResult = validator.validate(config);
        validationResult.printResults(plugin);
        
        if (!validationResult.isValid()) {
            plugin.getLogger().severe("§c任务配置验证失败，使用默认任务模板");
            loadDefaultTemplates();
            return;
        }
        
        // 加载全局设置
        loadSettings(config);
        
        // 加载境界配置
        loadRealms(config);
        
        // 加载任务模板
        loadTemplatesFromConfig(config);

        plugin.getLogger().info("  §a✓ 已加载 " + getTotalTemplateCount() + " 个任务模板");
    }
    
    /**
     * 加载全局设置
     */
    private void loadSettings(FileConfiguration config) {
        ConfigurationSection settings = config.getConfigurationSection("settings.difficulty");
        if (settings != null) {
            targetMultiplier = settings.getDouble("target-multiplier", 0.1);
            rewardMultiplier = settings.getDouble("reward-multiplier", 0.2);
            plugin.getLogger().info("  §7  难度系数: 目标×" + targetMultiplier + ", 奖励×" + rewardMultiplier);
        }
    }
    
    /**
     * 加载境界配置
     */
    private void loadRealms(FileConfiguration config) {
        List<?> realmsList = config.getList("realms");
        if (realmsList != null && !realmsList.isEmpty()) {
            realmLevels.clear();
            for (Object obj : realmsList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> realmMap = (Map<String, Object>) obj;
                    String name = (String) realmMap.get("name");
                    Object levelObj = realmMap.get("level");
                    if (name != null && levelObj != null) {
                        int level = levelObj instanceof Integer ? (Integer) levelObj : 
                                    Integer.parseInt(levelObj.toString());
                        realmLevels.put(name, level);
                    }
                }
            }
            plugin.getLogger().info("  §7  已加载 " + realmLevels.size() + " 个境界配置");
        }
    }
    
    /**
     * 从配置加载任务模板
     */
    private void loadTemplatesFromConfig(FileConfiguration config) {
        ConfigurationSection templatesSection = config.getConfigurationSection("templates");
        if (templatesSection == null) {
            plugin.getLogger().warning("§c未找到任务模板配置!");
            loadDefaultTemplates();
            return;
        }
        
        // 加载每种类型的任务
        for (SectTaskType type : SectTaskType.values()) {
            String typeName = type.name().toLowerCase();
            ConfigurationSection typeSection = templatesSection.getConfigurationSection(typeName);
            
            if (typeSection == null) {
                plugin.getLogger().warning("§e未找到 " + typeName + " 类型的任务模板");
                continue;
            }
            
            List<TaskTemplate> typeTemplates = new ArrayList<>();
            int loadedCount = 0;
            int skippedCount = 0;
            
            for (String taskId : typeSection.getKeys(false)) {
                ConfigurationSection taskSection = typeSection.getConfigurationSection(taskId);
                if (taskSection == null) {
                    continue;
                }
                
                // 检查是否启用
                if (!taskSection.getBoolean("enabled", true)) {
                    skippedCount++;
                    continue;
                }
                
                try {
                    TaskTemplate template = loadTemplateFromConfig(taskId, taskSection);
                    if (template != null) {
                        typeTemplates.add(template);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("§c加载任务模板失败: " + taskId + " - " + e.getMessage());
                }
            }
            
            templates.put(type, typeTemplates);
            plugin.getLogger().info("  §7  " + type.getDisplayName() + ": " + loadedCount + " 个模板" +
                                   (skippedCount > 0 ? " (跳过 " + skippedCount + " 个)" : ""));
        }
    }
    
    /**
     * 从配置加载单个任务模板
     */
    private TaskTemplate loadTemplateFromConfig(String id, ConfigurationSection section) {
        try {
            String name = section.getString("name", id);
            String objectiveStr = section.getString("objective", "KILL_MOB");
            TaskObjective objective = TaskObjective.valueOf(objectiveStr);
            String target = section.getString("target", "");
            int baseAmount = section.getInt("base-amount", 20);
            int minRealmLevel = section.getInt("min-realm", 1);
            int weight = section.getInt("weight", 10);
            
            // 加载奖励
            ConfigurationSection rewards = section.getConfigurationSection("rewards");
            int baseContribution = 0;
            int baseSpiritStone = 0;
            int baseActivity = 0;
            int baseSectExp = 0;
            
            if (rewards != null) {
                baseContribution = rewards.getInt("contribution", 0);
                baseSpiritStone = rewards.getInt("spirit-stone", 0);
                baseActivity = rewards.getInt("activity", 0);
                baseSectExp = rewards.getInt("sect-exp", 0);
            }
            
            TaskTemplate template = new TaskTemplate(
                name, objective, target, baseAmount, minRealmLevel,
                baseContribution, baseSpiritStone, baseActivity, baseSectExp
            );
            template.weight = weight;
            
            return template;
            
        } catch (Exception e) {
            plugin.getLogger().warning("§c解析任务模板失败 (" + id + "): " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 加载默认任务模板（当配置文件加载失败时使用）
     */
    @Deprecated
    private void loadDefaultTemplates() {
        plugin.getLogger().warning("§e使用默认任务模板（硬编码）");
        initializeDailyTemplates();
        initializeWeeklyTemplates();
        initializeSpecialTemplates();
    }

    /**
     * 生成任务
     *
     * @param player 玩家
     * @param type   任务类型
     * @param count  生成数量
     * @return 生成的任务列表
     */
    public List<SectTask> generateTasks(Player player, SectTaskType type, int count) {
        List<SectTask> tasks = new ArrayList<>();

        // 获取玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return tasks;
        }

        // 获取玩家境界等级（1-8）
        int realmLevel = getRealmLevel(data.getRealm());

        // 获取对应类型的模板
        List<TaskTemplate> availableTemplates = templates.getOrDefault(type, new ArrayList<>());
        if (availableTemplates.isEmpty()) {
            plugin.getLogger().warning("§c没有可用的 " + type.getDisplayName() + " 模板!");
            return tasks;
        }

        // 筛选适合玩家境界的模板
        List<TaskTemplate> suitableTemplates = availableTemplates.stream()
                .filter(template -> template.minRealmLevel <= realmLevel)
                .toList();

        if (suitableTemplates.isEmpty()) {
            suitableTemplates = availableTemplates; // 如果没有合适的，使用全部
        }

        // 随机选择模板并生成任务
        Random random = ThreadLocalRandom.current();
        Set<TaskTemplate> usedTemplates = new HashSet<>();

        for (int i = 0; i < count && !suitableTemplates.isEmpty(); i++) {
            // 随机选择一个未使用的模板
            TaskTemplate template = null;
            int attempts = 0;
            while (attempts < 10) {
                template = suitableTemplates.get(random.nextInt(suitableTemplates.size()));
                if (!usedTemplates.contains(template)) {
                    break;
                }
                attempts++;
            }

            if (template == null) {
                continue;
            }

            usedTemplates.add(template);

            // 根据模板生成任务
            SectTask task = generateFromTemplate(template, realmLevel, type);
            tasks.add(task);
        }

        return tasks;
    }

    /**
     * 根据模板生成任务
     */
    private SectTask generateFromTemplate(TaskTemplate template, int realmLevel, SectTaskType type) {
        // 计算目标数量（根据境界调整）
        int targetAmount = calculateTargetAmount(template, realmLevel);

        // 创建任务
        SectTask task = new SectTask(template.name, type, template.objective, template.target, targetAmount);

        // 设置奖励（根据境界调整）
        task.setContributionReward(calculateReward(template.baseContribution, realmLevel));
        task.setSpiritStoneReward(calculateReward(template.baseSpiritStone, realmLevel));
        task.setActivityReward(calculateReward(template.baseActivity, realmLevel));
        task.setSectExpReward(calculateReward(template.baseSectExp, realmLevel));

        // 设置时间
        long currentTime = System.currentTimeMillis();
        task.setCreatedTime(currentTime);
        task.setExpireTime(currentTime + type.getRefreshInterval());

        return task;
    }

    /**
     * 计算目标数量
     */
    private int calculateTargetAmount(TaskTemplate template, int realmLevel) {
        // 基础数量 * (1 + 境界等级 * 系数)
        double multiplier = 1.0 + (realmLevel - 1) * targetMultiplier;
        return (int) (template.baseTargetAmount * multiplier);
    }

    /**
     * 计算奖励数值
     */
    private int calculateReward(int baseReward, int realmLevel) {
        if (baseReward == 0) {
            return 0;
        }
        // 基础奖励 * (1 + 境界等级 * 系数)
        double multiplier = 1.0 + (realmLevel - 1) * rewardMultiplier;
        return (int) (baseReward * multiplier);
    }

    /**
     * 初始化日常任务模板
     */
    private void initializeDailyTemplates() {
        List<TaskTemplate> dailyTemplates = new ArrayList<>();

        // 击杀怪物任务
        dailyTemplates.add(new TaskTemplate(
                "除魔卫道",
                TaskObjective.KILL_MOB_TYPE,
                "僵尸",
                20,
                1,
                10, 50, 5, 10
        ));

        dailyTemplates.add(new TaskTemplate(
                "斩妖除怪",
                TaskObjective.KILL_MOB_TYPE,
                "骷髅",
                20,
                1,
                10, 50, 5, 10
        ));

        dailyTemplates.add(new TaskTemplate(
                "清理蜘蛛巢穴",
                TaskObjective.KILL_MOB_TYPE,
                "蜘蛛",
                15,
                1,
                10, 50, 5, 10
        ));

        // 收集物品任务
        dailyTemplates.add(new TaskTemplate(
                "采集灵草",
                TaskObjective.COLLECT_ITEM,
                Material.WHEAT.name(),
                30,
                1,
                15, 30, 10, 5
        ));

        dailyTemplates.add(new TaskTemplate(
                "收集矿石",
                TaskObjective.COLLECT_ITEM,
                Material.IRON_ORE.name(),
                20,
                2,
                15, 40, 10, 8
        ));

        dailyTemplates.add(new TaskTemplate(
                "采集木材",
                TaskObjective.COLLECT_ITEM,
                Material.OAK_LOG.name(),
                40,
                1,
                10, 20, 8, 5
        ));

        // 修炼任务
        dailyTemplates.add(new TaskTemplate(
                "日常修炼",
                TaskObjective.CULTIVATE,
                "",
                30,
                1,
                20, 0, 15, 15
        ));

        // 在线时长
        dailyTemplates.add(new TaskTemplate(
                "勤修苦练",
                TaskObjective.ONLINE_TIME,
                "",
                60,
                1,
                15, 0, 20, 10
        ));

        // 捐献灵石
        dailyTemplates.add(new TaskTemplate(
                "为宗门添砖加瓦",
                TaskObjective.DONATE_SPIRIT_STONE,
                "",
                100,
                2,
                30, 0, 10, 20
        ));

        // 使用功法
        dailyTemplates.add(new TaskTemplate(
                "磨砺功法",
                TaskObjective.USE_SKILL,
                "",
                10,
                2,
                15, 0, 10, 10
        ));

        templates.put(SectTaskType.DAILY, dailyTemplates);
    }

    /**
     * 初始化周常任务模板
     */
    private void initializeWeeklyTemplates() {
        List<TaskTemplate> weeklyTemplates = new ArrayList<>();

        // 击杀怪物任务（更高数量）
        weeklyTemplates.add(new TaskTemplate(
                "妖魔猎手",
                TaskObjective.KILL_MOB_TYPE,
                "僵尸",
                100,
                1,
                50, 200, 30, 50
        ));

        weeklyTemplates.add(new TaskTemplate(
                "骷髅克星",
                TaskObjective.KILL_MOB_TYPE,
                "骷髅",
                100,
                1,
                50, 200, 30, 50
        ));

        // 收集物品任务（更高数量）
        weeklyTemplates.add(new TaskTemplate(
                "灵材大师",
                TaskObjective.COLLECT_ITEM,
                Material.DIAMOND.name(),
                20,
                3,
                60, 300, 40, 60
        ));

        weeklyTemplates.add(new TaskTemplate(
                "矿石富翁",
                TaskObjective.COLLECT_ITEM,
                Material.GOLD_INGOT.name(),
                50,
                2,
                50, 250, 35, 55
        ));

        // 修炼任务
        weeklyTemplates.add(new TaskTemplate(
                "周修炼",
                TaskObjective.CULTIVATE,
                "",
                180,
                1,
                80, 0, 60, 70
        ));

        // 炼制装备
        weeklyTemplates.add(new TaskTemplate(
                "炼器大师",
                TaskObjective.FORGE_EQUIPMENT,
                "",
                10,
                3,
                70, 200, 50, 60
        ));

        // 完成任务
        weeklyTemplates.add(new TaskTemplate(
                "任务达人",
                TaskObjective.COMPLETE_QUEST,
                "",
                5,
                2,
                60, 100, 40, 50
        ));

        // 参与宗门活动
        weeklyTemplates.add(new TaskTemplate(
                "积极分子",
                TaskObjective.ATTEND_EVENT,
                "",
                3,
                1,
                50, 0, 50, 40
        ));

        templates.put(SectTaskType.WEEKLY, weeklyTemplates);
    }

    /**
     * 初始化特殊任务模板
     */
    private void initializeSpecialTemplates() {
        List<TaskTemplate> specialTemplates = new ArrayList<>();

        // 境界突破
        specialTemplates.add(new TaskTemplate(
                "突破瓶颈",
                TaskObjective.BREAKTHROUGH,
                "",
                1,
                1,
                100, 500, 50, 100
        ));

        // 组队副本
        specialTemplates.add(new TaskTemplate(
                "协同作战",
                TaskObjective.TEAM_DUNGEON,
                "",
                3,
                4,
                80, 300, 60, 80
        ));

        // 击杀BOSS（MythicMobs）
        specialTemplates.add(new TaskTemplate(
                "挑战BOSS",
                TaskObjective.KILL_MOB,
                "xian_boss_qi_rare_zombie_king",
                1,
                3,
                150, 1000, 100, 150
        ));

        // 高级炼制
        specialTemplates.add(new TaskTemplate(
                "神兵出世",
                TaskObjective.FORGE_EQUIPMENT,
                "",
                5,
                5,
                120, 500, 80, 120
        ));

        // 高级捐献
        specialTemplates.add(new TaskTemplate(
                "慷慨解囊",
                TaskObjective.DONATE_SPIRIT_STONE,
                "",
                1000,
                4,
                200, 0, 100, 200
        ));

        templates.put(SectTaskType.SPECIAL, specialTemplates);
    }

    /**
     * 获取总模板数量
     */
    private int getTotalTemplateCount() {
        return templates.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 将境界名称转换为等级（1-8）
     */
    private int getRealmLevel(String realmName) {
        return realmLevels.getOrDefault(realmName, 1); // 默认返回1（炼气期）
    }

    /**
     * 任务模板内部类
     */
    private static class TaskTemplate {
        String name;                // 任务名称
        TaskObjective objective;    // 目标类型
        String target;              // 目标对象
        int baseTargetAmount;       // 基础目标数量
        int minRealmLevel;          // 最低境界等级
        int weight;                 // 权重（生成概率）

        // 基础奖励
        int baseContribution;       // 贡献值
        int baseSpiritStone;        // 灵石
        int baseActivity;           // 活跃度
        int baseSectExp;            // 宗门经验

        TaskTemplate(String name, TaskObjective objective, String target,
                     int baseTargetAmount, int minRealmLevel,
                     int baseContribution, int baseSpiritStone,
                     int baseActivity, int baseSectExp) {
            this.name = name;
            this.objective = objective;
            this.target = target;
            this.baseTargetAmount = baseTargetAmount;
            this.minRealmLevel = minRealmLevel;
            this.baseContribution = baseContribution;
            this.baseSpiritStone = baseSpiritStone;
            this.baseActivity = baseActivity;
            this.baseSectExp = baseSectExp;
            this.weight = 10;  // 默认权重
        }
    }
}
