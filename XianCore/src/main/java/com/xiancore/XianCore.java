package com.xiancore;

import com.xiancore.commands.*;
import com.xiancore.gui.GUIManager;
import com.xiancore.listeners.*;
import com.xiancore.core.XianCoreEngine;
import com.xiancore.core.config.ConfigManager;
import com.xiancore.core.data.DataManager;
import com.xiancore.core.event.XianEventBus;
import com.xiancore.integration.mythic.MythicIntegration;
import com.xiancore.integration.placeholder.XianCorePlaceholderExpansion;
import com.xiancore.systems.cultivation.CultivationSystem;
import com.xiancore.systems.fate.FateSystem;
import com.xiancore.systems.forge.ForgeSystem;
import com.xiancore.systems.sect.SectSystem;
import com.xiancore.systems.skill.SkillSystem;
import com.xiancore.systems.tribulation.TribulationSystem;
import com.xiancore.bridge.world.WorldEventBridge;
import com.xiancore.bridge.economy.EconomyBridge;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * XianCore 主类
 * 修仙体系插件 - 基于凡人修仙传世界观
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class XianCore extends JavaPlugin {

    @Getter
    private static XianCore instance;

    // 核心引擎
    private XianCoreEngine coreEngine;
    private ConfigManager configManager;
    private DataManager dataManager;
    private XianEventBus eventBus;
    private com.xiancore.core.data.migrate.MigrationManager migrationManager;

    // MythicMobs 集成
    private MythicIntegration mythicIntegration;

    // PlaceholderAPI 扩展
    private XianCorePlaceholderExpansion placeholderExpansion;

    // 游戏系统
    private FateSystem fateSystem;
    private CultivationSystem cultivationSystem;
    private ForgeSystem forgeSystem;
    private SectSystem sectSystem;
    private SkillSystem skillSystem;
    private TribulationSystem tribulationSystem;
    private com.xiancore.systems.activeqi.ActiveQiManager activeQiManager;

    // Boss 系统
    private com.xiancore.systems.boss.BossRefreshManager bossRefreshManager;
    private com.xiancore.systems.boss.damage.DamageStatisticsManager damageStatisticsManager;

    // 桥接系统
    private WorldEventBridge worldEventBridge;
    private EconomyBridge economyBridge;

    // GUI 管理器
    private GUIManager guiManager;

    // 定时任务
    private DataSaveTask dataSaveTask;

    // 调试模式
    private boolean debugMode = false;

    @Override
    public void onLoad() {
        instance = this;
        getLogger().info("XianCore 正在加载...");
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        // 打印启动横幅
        printBanner();

        try {
            // 1. 初始化核心引擎
            getLogger().info("§a[阶段 1/5] 初始化核心引擎...");
            initializeCoreEngine();

            // 2. 初始化 MythicMobs 集成
            getLogger().info("§a[阶段 2/5] 初始化 MythicMobs 集成...");
            initializeMythicIntegration();

            // 3. 初始化游戏系统
            getLogger().info("§a[阶段 3/6] 初始化游戏系统...");
            initializeGameSystems();

            // 4. 初始化 GUI 管理器
            getLogger().info("§a[阶段 4/6] 初始化 GUI 管理器...");
            initializeGUI();

            // 5. 初始化桥接系统
            getLogger().info("§a[阶段 5/6] 初始化桥接系统...");
            initializeBridges();

            // 6. 注册监听器和命令
            getLogger().info("§a[阶段 6/6] 注册监听器和命令...");
            registerListenersAndCommands();

            // 7. 启动定时任务
            startScheduledTasks();

            long elapsed = System.currentTimeMillis() - startTime;
            getLogger().info("§a========================================");
            getLogger().info("§aXianCore 启动成功! 耗时: " + elapsed + "ms");
            getLogger().info("§a版本: " + getDescription().getVersion());
            getLogger().info("§a作者: Olivia Diaz");
            getLogger().info("§a========================================");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "§c插件启动失败!", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("XianCore 正在卸载...");

        try {
            // 注销 PlaceholderAPI 扩展
            if (placeholderExpansion != null) {
                placeholderExpansion.unregister();
                getLogger().info("PlaceholderAPI 扩展已注销");
            }

            // 停止定时任务
            if (dataSaveTask != null) {
                dataSaveTask.cancel();
            }
            
            // 停止宗门系统（包括任务刷新调度器）
            if (sectSystem != null) {
                getLogger().info("停止宗门系统...");
                sectSystem.shutdown();
            }

            // 停止功法系统
            if (skillSystem != null) {
                getLogger().info("停止功法系统...");
                skillSystem.shutdown();
            }

            // 保存所有数据
            if (dataManager != null) {
                getLogger().info("保存玩家数据...");
                dataManager.saveAll();
            }

            // 关闭数据库连接
            if (dataManager != null) {
                getLogger().info("关闭数据库连接...");
                dataManager.shutdown();
            }

            // 关闭 Redis 连接
            if (worldEventBridge != null) {
                getLogger().info("关闭 Redis 连接...");
                worldEventBridge.shutdown();
            }

            getLogger().info("§aXianCore 已安全卸载!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "§c插件卸载过程中发生错误!", e);
        }
    }

    /**
     * 初始化核心引擎
     */
    private void initializeCoreEngine() {
        // 配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // 事件总线
        eventBus = new XianEventBus();

        // 数据管理器
        dataManager = new DataManager(this);
        dataManager.initialize();

        // 迁移管理器
        migrationManager = new com.xiancore.core.data.migrate.MigrationManager(this);

        // 核心引擎
        coreEngine = new XianCoreEngine(this);
        coreEngine.initialize();

        getLogger().info("§a✓ 核心引擎初始化完成");
    }

    /**
     * 初始化 MythicMobs 集成
     */
    private void initializeMythicIntegration() {
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") == null) {
            getLogger().warning("§e未检测到 MythicMobs 插件，部分功能将无法使用!");
            return;
        }

        mythicIntegration = new MythicIntegration(this);
        mythicIntegration.initialize();

        getLogger().info("§a✓ MythicMobs 集成初始化完成");
    }

    /**
     * 初始化游戏系统
     */
    private void initializeGameSystems() {
        // 活跃灵气管理器
        activeQiManager = new com.xiancore.systems.activeqi.ActiveQiManager(this);

        // 奇遇系统
        fateSystem = new FateSystem(this);
        fateSystem.initialize();

        // 修炼系统
        cultivationSystem = new CultivationSystem(this);
        cultivationSystem.initialize();

        // 炼器系统
        forgeSystem = new ForgeSystem(this);
        forgeSystem.initialize();

        // 宗门系统
        sectSystem = new SectSystem(this);
        sectSystem.initialize();

        // 功法系统
        skillSystem = new SkillSystem(this);
        skillSystem.initialize();

        // 天劫系统
        tribulationSystem = new TribulationSystem(this);
        tribulationSystem.initialize();

        // Boss 系统
        if (mythicIntegration != null) {
            try {
                bossRefreshManager = new com.xiancore.systems.boss.BossRefreshManager(this, mythicIntegration);
                bossRefreshManager.initialize();
                bossRefreshManager.enable();
                
                damageStatisticsManager = new com.xiancore.systems.boss.damage.DamageStatisticsManager();
                damageStatisticsManager.initialize();
                
                getLogger().info("  §a✓ Boss 系统初始化完成");
            } catch (Exception e) {
                getLogger().warning("§eBoss 系统初始化失败: " + e.getMessage());
            }
        } else {
            getLogger().warning("§eBoss 系统需要 MythicMobs，已跳过初始化");
        }

        getLogger().info("§a✓ 游戏系统初始化完成");
    }

    /**
     * 初始化 GUI 管理器
     */
    private void initializeGUI() {
        guiManager = new GUIManager(this);
        guiManager.initialize();

        getLogger().info("§a✓ GUI 管理器初始化完成");
    }

    /**
     * 初始化桥接系统
     */
    private void initializeBridges() {
        // 世界事件桥接
        worldEventBridge = new WorldEventBridge(this);
        worldEventBridge.initialize();

        // 经济桥接 (Vault)
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            economyBridge = new EconomyBridge(this);
            economyBridge.initialize();
            getLogger().info("  §a✓ Vault 集成初始化完成");
        } else {
            getLogger().warning("  §e未检测到 Vault 插件，经济系统将无法使用!");
        }

        // PlaceholderAPI 集成
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new XianCorePlaceholderExpansion(this);
            if (placeholderExpansion.register()) {
                getLogger().info("  §a✓ PlaceholderAPI 集成初始化完成");
            } else {
                getLogger().warning("  §e无法注册 PlaceholderAPI 扩展!");
            }
        } else {
            getLogger().warning("  §e未检测到 PlaceholderAPI 插件，占位符功能将无法使用!");
        }

        getLogger().info("§a✓ 桥接系统初始化完成");
    }

    /**
     * 注册监听器和命令
     */
    private void registerListenersAndCommands() {
        // 注册命令
        registerCommands();

        // 注册监听器
        registerListeners();

        getLogger().info("§a✓ 监听器和命令注册完成");
    }

    /**
     * 注册所有命令
     */
    private void registerCommands() {
        // 主命令
        getCommand("xiancore").setExecutor(new XianCoreCommand(this));
        getCommand("xiancore").setTabCompleter(new XianCoreCommand(this));

        // 修炼系统命令
        getCommand("cultivation").setExecutor(new CultivationCommand(this));
        getCommand("cultivation").setTabCompleter(new CultivationCommand(this));

        // 炼器系统命令
        getCommand("forge").setExecutor(new ForgeCommand(this));
        getCommand("forge").setTabCompleter(new ForgeCommand(this));

        // 宗门系统命令
        getCommand("sect").setExecutor(new SectCommand(this));
        getCommand("sect").setTabCompleter(new SectCommand(this));

        // 功法系统命令
        getCommand("skill").setExecutor(new SkillCommand(this));
        getCommand("skill").setTabCompleter(new SkillCommand(this));

        // 功法调试命令
        getCommand("skilldebug").setExecutor(new SkillDebugCommand(this));
        getCommand("skilldebug").setTabCompleter(new SkillDebugCommand(this));

        // 修为奖励调试命令
        getCommand("qireward").setExecutor(new QiRewardDebugCommand(this));
        getCommand("qireward").setTabCompleter(new QiRewardDebugCommand(this));

        // 天劫系统命令
        getCommand("tribulation").setExecutor(new TribulationCommand(this));
        getCommand("tribulation").setTabCompleter(new TribulationCommand(this));

        // Boss 系统命令
        if (bossRefreshManager != null && damageStatisticsManager != null) {
            BossCommand bossCmd = new BossCommand(this, bossRefreshManager, damageStatisticsManager);
            getCommand("boss").setExecutor(bossCmd);
            getCommand("boss").setTabCompleter(bossCmd);
        }

        getLogger().info("  §a✓ 命令系统注册完成");
    }

    /**
     * 注册所有监听器
     */
    private void registerListeners() {
        // 玩家连接监听器
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        // 奇遇触发监听器
        getServer().getPluginManager().registerEvents(new FateTriggerListener(this), this);

        // 功法秘籍监听器
        getServer().getPluginManager().registerEvents(new com.xiancore.systems.skill.listeners.SkillBookListener(this), this);

        // 功法快捷键监听器
        getServer().getPluginManager().registerEvents(new SkillKeybindListener(this), this);

        // 元素属性监听器
        getServer().getPluginManager().registerEvents(new com.xiancore.systems.skill.listeners.ElementalAttributeListener(this), this);

        // 实体击杀监听器（修为奖励）
        getServer().getPluginManager().registerEvents(new EntityKillListener(this), this);

        // 装备属性应用监听器（饰品/自定义属性）
        getServer().getPluginManager().registerEvents(new com.xiancore.systems.forge.listeners.EquipmentAttributeListener(this), this);

        // Quests 插件集成监听器
        // 使用动态事件注册，避免编译时依赖
        if (Bukkit.getPluginManager().getPlugin("Quests") != null) {
            new QuestsIntegrationListener(this).register();
        }

        // Boss 系统监听器
        if (bossRefreshManager != null && damageStatisticsManager != null) {
            // 基础 Boss 事件（生成/击杀/消失 + 原生死亡转发）
            com.xiancore.systems.boss.listener.BossEventListener.register(this, bossRefreshManager);

            // 战斗伤害监听（玩家对 Boss 的伤害记录）
            getServer().getPluginManager().registerEvents(
                new com.xiancore.systems.boss.listener.BossCombatListener(this, bossRefreshManager, damageStatisticsManager),
                this
            );

            // Boss 结束时收尾（归档/清理伤害记录等）
            getServer().getPluginManager().registerEvents(
                new com.xiancore.systems.boss.listener.BossDamageFinalizeListener(this, bossRefreshManager, damageStatisticsManager),
                this
            );
        }

        getLogger().info("  §a✓ 监听器系统注册完成");
    }

    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        // 数据自动保存任务
        dataSaveTask = new DataSaveTask(this);
        dataSaveTask.start();

        // 每日活跃灵气衰减任务（每天凌晨4点执行，约24小时 = 1728000 ticks）
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            getLogger().info("§7执行每日活跃灵气衰减任务...");

            int decayedPlayers = 0;
            // 遍历所有在线玩家
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                com.xiancore.core.data.PlayerData data = dataManager.loadPlayerData(player.getUniqueId());
                if (data != null) {
                    long oldActiveQi = data.getActiveQi();
                    data.decayActiveQi();  // 衰减15%
                    dataManager.savePlayerData(data);
                    decayedPlayers++;

                    if (isDebugMode()) {
                        getLogger().fine("§7玩家 " + data.getName() + " 活跃灵气: " + oldActiveQi + " → " + data.getActiveQi());
                    }
                }
            }

            getLogger().info("§a每日活跃灵气衰减完成！共处理 " + decayedPlayers + " 个在线玩家");
        }, 20L * 60 * 60 * 24, 20L * 60 * 60 * 24); // 24小时执行一次

        // 定时宗门数据一致性检查任务（每小时执行一次）
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (sectSystem != null && sectSystem.getAllSects() != null) {
                getLogger().info("§7执行定时宗门数据一致性检查...");
                try {
                    sectSystem.validateAndFixDataConsistency();
                } catch (Exception e) {
                    getLogger().warning("§c定时一致性检查失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 20L * 60 * 60, 20L * 60 * 60); // 1小时后开始，每1小时执行一次

        getLogger().info("§a✓ 定时任务启动完成");
    }

    /**
     * 打印启动横幅
     */
    private void printBanner() {
        getLogger().info("§b========================================");
        getLogger().info("§b  __  ___            ____              ");
        getLogger().info("§b  \\ \\/ (_)__ _ _ __ / ___|___  _ __ ___ ");
        getLogger().info("§b   \\  /| / _` | '_ | |   / _ \\| '__/ _ \\");
        getLogger().info("§b   /  \\| | (_| | | || |__| (_) | | |  __/");
        getLogger().info("§b  /_/\\_\\_|\\__,_|_| |_\\____\\___/|_|  \\___|");
        getLogger().info("§b                                        ");
        getLogger().info("§b  修仙体系插件 - 凡人修仙传");
        getLogger().info("§b  Version: " + getDescription().getVersion());
        getLogger().info("§b========================================");
    }

    /**
     * 重载配置
     */
    public void reloadConfigs() {
        getLogger().info("正在重载配置...");
        configManager.reloadConfigs();
        
        // 重载功法商店配置
        if (skillSystem != null) {
            com.xiancore.systems.skill.shop.SkillShopConfig.reload();
        }
        
        getLogger().info("§a配置重载完成!");
    }

    // ==================== Getter 方法 ====================

    public XianCoreEngine getEngine() {
        return coreEngine;
    }

    public SectSystem getSectSystem() {
        return sectSystem;
    }

    public SkillSystem getSkillSystem() {
        return skillSystem;
    }

    public CultivationSystem getCultivationSystem() {
        return cultivationSystem;
    }

    public ForgeSystem getForgeSystem() {
        return forgeSystem;
    }

    public FateSystem getFateSystem() {
        return fateSystem;
    }

    public TribulationSystem getTribulationSystem() {
        return tribulationSystem;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public XianEventBus getEventBus() {
        return eventBus;
    }

    public GUIManager getGuiManager() {
        return guiManager;
    }

    public MythicIntegration getMythicIntegration() {
        return mythicIntegration;
    }

    /**
     * 获取调试模式状态
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * 设置调试模式
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        if (enabled) {
            getLogger().setLevel(Level.FINE);
            getLogger().info("§6[调试模式] 已启用调试日志输出");
        } else {
            getLogger().setLevel(Level.INFO);
            getLogger().info("§6[调试模式] 已禁用调试日志输出");
        }
    }
    
    /**
     * 获取迁移管理器
     */
    public com.xiancore.core.data.migrate.MigrationManager getMigrationManager() {
        return migrationManager;
    }
}
