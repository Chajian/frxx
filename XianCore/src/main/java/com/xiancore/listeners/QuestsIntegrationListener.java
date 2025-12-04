package com.xiancore.listeners;

import com.xiancore.XianCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

/**
 * Quests 插件集成监听器
 * 监听 Quests 任务完成事件，发放宗门特定奖励
 *
 * 需要安装 Quests 插件: https://www.spigotmc.org/resources/quests.3711/
 *
 * 使用反射和动态事件注册，避免编译时依赖
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class QuestsIntegrationListener implements Listener {

    private final XianCore plugin;
    private static final String QUEST_COMPLETE_EVENT_CLASS = "me.pikamug.quests.events.quester.BukkitQuesterPostCompleteQuestEvent";

    public QuestsIntegrationListener(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 注册 Quests 事件监听器
     * 使用动态事件注册，避免编译时依赖
     */
    public void register() {
        try {
            // 尝试加载 Quests 完成事件类
            Class<?> eventClass = Class.forName(QUEST_COMPLETE_EVENT_CLASS);

            // 创建事件执行器
            EventExecutor executor = (listener, event) -> {
                if (!event.getClass().getName().equals(QUEST_COMPLETE_EVENT_CLASS)) {
                    return;
                }

                try {
                    // 通过反射获取玩家和任务名称
                    Object quester = event.getClass().getMethod("getQuester").invoke(event);
                    Player player = (Player) quester.getClass().getMethod("getPlayer").invoke(quester);

                    if (player == null) {
                        return;
                    }

                    Object quest = event.getClass().getMethod("getQuest").invoke(event);
                    String questName = (String) quest.getClass().getMethod("getName").invoke(quest);

                    // 交给活跃度管理器处理
                    if (plugin.getSectSystem() != null && plugin.getSectSystem().getActivityManager() != null) {
                        plugin.getSectSystem().getActivityManager().handleQuestComplete(player, questName);
                    }

                } catch (Exception e) {
                    // 处理异常，避免影响其他插件
                    if (plugin.isDebugMode()) {
                        plugin.getLogger().warning("§c处理 Quests 任务完成事件时出错: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };

            // 动态注册事件监听器
            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends Event>) eventClass,
                    this,
                    EventPriority.NORMAL,
                    executor,
                    plugin
            );

            plugin.getLogger().info("  §a✓ Quests 集成监听器已注册（使用反射模式）");

        } catch (ClassNotFoundException e) {
            // Quests 插件未安装或版本不兼容
            plugin.getLogger().warning("§e! Quests 插件未安装，宗门任务集成功能将无法使用");
            if (plugin.isDebugMode()) {
                plugin.getLogger().warning("§e提示: 请安装 Quests 5.x 版本以启用宗门任务功能");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("§c! 注册 Quests 集成监听器时出错: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
        }
    }
}
