package com.xiancore.listeners;

import com.xiancore.XianCore;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 数据自动保存任务
 * 定期保存玩家数据以防止数据丢失
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class DataSaveTask extends BukkitRunnable {

    private final XianCore plugin;

    public DataSaveTask(XianCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            // 保存所有在线玩家的数据
            plugin.getDataManager().saveAll();

            // 如果启用调试模式，输出日志
            if (plugin.getConfigManager().getConfig("config").getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("定期保存玩家数据完成");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("自动保存数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 启动定时保存任务
     */
    public void start() {
        // 从配置读取保存间隔（分钟）
        int interval = plugin.getConfigManager().getConfig("config").getInt("performance.auto-save-interval", 10);

        // 转换为 tick (1 分钟 = 1200 tick)
        long intervalTicks = interval * 1200L;

        // 延迟 1 分钟后开始，然后每隔指定间隔执行一次
        this.runTaskTimerAsynchronously(plugin, 1200L, intervalTicks);

        plugin.getLogger().info("数据自动保存任务已启动 (间隔: " + interval + " 分钟)");
    }
}
