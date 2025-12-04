package com.yourserver.xiancore;

import com.yourserver.xiancore.command.XianCoreCommand;
import com.yourserver.xiancore.config.ConfigManager;
import com.yourserver.xiancore.listener.ItemUseListener;
import com.yourserver.xiancore.listener.PlayerDataListener;
import com.yourserver.xiancore.manager.AttributeManager;
import com.yourserver.xiancore.manager.BuffManager;
import com.yourserver.xiancore.manager.DatabaseManager;
import com.yourserver.xiancore.manager.ItemUsageManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * XianCore-Addon 主插件类
 * 
 * 属性提升道具系统 - 基于 MMOCore 扩展
 * 
 * @author YourServer Team
 * @version 1.0.0
 */
public class XianCoreAddon extends JavaPlugin {
    
    // 单例实例
    private static XianCoreAddon instance;
    
    // 管理器
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private AttributeManager attributeManager;
    private ItemUsageManager itemUsageManager;
    private BuffManager buffManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 打印启动信息
        getLogger().info("╔═══════════════════════════════════════╗");
        getLogger().info("║   XianCore-Addon 正在启动...         ║");
        getLogger().info("║   版本: 1.0.0                         ║");
        getLogger().info("║   作者: YourServer Team              ║");
        getLogger().info("╚═══════════════════════════════════════╝");
        
        // 检查依赖
        if (!checkDependencies()) {
            getLogger().severe("缺少必要依赖插件！插件已禁用。");
            getLogger().severe("请确保已安装: MMOCore, MMOItems, MythicLib");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化配置
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        getLogger().info("✓ 配置文件加载完成");
        
        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        getLogger().info("✓ 数据库初始化完成");
        
        // 初始化管理器
        attributeManager = new AttributeManager(this);
        itemUsageManager = new ItemUsageManager(this);
        buffManager = new BuffManager(this);
        getLogger().info("✓ 管理器初始化完成");
        
        // 注册监听器
        registerListeners();
        getLogger().info("✓ 事件监听器注册完成");
        
        // 注册命令
        registerCommands();
        getLogger().info("✓ 命令系统注册完成");
        
        // 启动定时任务
        startScheduledTasks();
        getLogger().info("✓ 定时任务启动完成");
        
        // 启动完成
        getLogger().info("╔═══════════════════════════════════════╗");
        getLogger().info("║   XianCore-Addon 启动成功！          ║");
        getLogger().info("║   使用 /xiancore help 查看帮助       ║");
        getLogger().info("╚═══════════════════════════════════════╝");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("XianCore-Addon 正在关闭...");
        
        // 保存所有Buff数据
        if (buffManager != null) {
            getLogger().info("正在保存Buff数据...");
            buffManager.saveAllBuffs();
        }
        
        // 最后一次数据库备份
        if (databaseManager != null) {
            getLogger().info("正在备份数据库...");
            databaseManager.backupDatabase();
            
            // 等待异步任务完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 关闭数据库连接
            databaseManager.close();
        }
        
        getLogger().info("XianCore-Addon 已安全关闭！");
    }
    
    /**
     * 检查依赖插件
     */
    private boolean checkDependencies() {
        // 检查 MMOCore
        if (getServer().getPluginManager().getPlugin("MMOCore") == null) {
            getLogger().severe("未找到 MMOCore 插件！");
            getLogger().severe("下载地址: https://www.spigotmc.org/resources/mmocore.70575/");
            return false;
        }
        
        // 检查 MMOItems
        if (getServer().getPluginManager().getPlugin("MMOItems") == null) {
            getLogger().severe("未找到 MMOItems 插件！");
            getLogger().severe("下载地址: https://www.spigotmc.org/resources/mmoitems.39267/");
            return false;
        }
        
        // 检查 MythicLib
        if (getServer().getPluginManager().getPlugin("MythicLib") == null) {
            getLogger().warning("未找到 MythicLib 插件！");
            getLogger().warning("MMOCore/MMOItems 需要 MythicLib 才能正常工作");
            return false;
        }
        
        // 检查 PlaceholderAPI（可选）
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("未找到 PlaceholderAPI，部分功能可能受限。");
            getLogger().warning("下载地址: https://www.spigotmc.org/resources/placeholderapi.6245/");
        }
        
        getLogger().info("✓ 依赖检查通过");
        return true;
    }
    
    /**
     * 注册事件监听器
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ItemUseListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);
    }
    
    /**
     * 注册命令
     */
    private void registerCommands() {
        XianCoreCommand commandHandler = new XianCoreCommand(this);
        getCommand("xiancore").setExecutor(commandHandler);
        getCommand("xiancore").setTabCompleter(commandHandler);
    }
    
    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        // Buff过期检查任务（每秒）
        if (getConfig().getBoolean("features.buff-system", true)) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                buffManager.checkExpiredBuffs();
            }, 20L, 20L);
        }
        
        // ActionBar显示任务（每秒）
        if (getConfig().getBoolean("buff-display.update-interval", 20) > 0) {
            int interval = getConfig().getInt("buff-display.update-interval", 20);
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                buffManager.updateBuffDisplay();
            }, interval, interval);
        }
        
        // 数据库自动备份（每小时）
        if (getConfig().getBoolean("backup.enabled", true)) {
            long backupInterval = getConfig().getLong("backup.interval", 3600) * 20L;
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                databaseManager.backupDatabase();
            }, backupInterval, backupInterval);
        }
        
        // 清理过期Buff（每10分钟）
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            databaseManager.cleanupExpiredBuffs();
        }, 12000L, 12000L);
    }
    
    // ==================== Getters ====================
    
    public static XianCoreAddon getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public AttributeManager getAttributeManager() {
        return attributeManager;
    }
    
    public ItemUsageManager getItemUsageManager() {
        return itemUsageManager;
    }
    
    public BuffManager getBuffManager() {
        return buffManager;
    }
}

