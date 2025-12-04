package com.xiancore.core;

import com.xiancore.XianCore;
import lombok.Getter;
import org.bukkit.Bukkit;

/**
 * XianCore 核心引擎
 * 提供核心 API、事件总线和数据管理功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class XianCoreEngine {

    private final XianCore plugin;
    private boolean initialized = false;

    public XianCoreEngine(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化核心引擎
     */
    public void initialize() {
        if (initialized) {
            plugin.getLogger().warning("核心引擎已经初始化过了!");
            return;
        }

        plugin.getLogger().info("核心引擎正在初始化...");

        // 注册 API
        registerAPI();

        // 启动性能监控
        startPerformanceMonitor();

        initialized = true;
        plugin.getLogger().info("核心引擎初始化完成!");
    }

    /**
     * 注册 API
     */
    private void registerAPI() {
        // 注册到 Bukkit ServicesManager
        Bukkit.getServicesManager().register(
                XianCoreEngine.class,
                this,
                plugin,
                org.bukkit.plugin.ServicePriority.Normal
        );
    }

    /**
     * 启动性能监控
     */
    private void startPerformanceMonitor() {
        // 每5分钟检查一次服务器性能
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            double tps = getTPS();
            if (tps < 18.0) {
                plugin.getLogger().warning(String.format(
                        "§e服务器 TPS 较低: %.2f - 建议检查性能!",
                        tps
                ));
            }
        }, 20L * 60 * 5, 20L * 60 * 5);
    }

    /**
     * 获取服务器 TPS
     */
    private double getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Object tpsArray = server.getClass().getField("recentTps").get(server);
            return ((double[]) tpsArray)[0];
        } catch (Exception e) {
            return 20.0;
        }
    }

    /**
     * 检查引擎是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}
