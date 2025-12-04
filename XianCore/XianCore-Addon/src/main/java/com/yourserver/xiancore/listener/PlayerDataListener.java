package com.yourserver.xiancore.listener;

import com.yourserver.xiancore.XianCoreAddon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家数据监听器
 */
public class PlayerDataListener implements Listener {
    
    private final XianCoreAddon plugin;
    
    public PlayerDataListener(XianCoreAddon plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 加载玩家Buff
        plugin.getBuffManager().loadPlayerBuffs(player.getUniqueId());
        
        if (plugin.getConfig().getBoolean("debug.verbose-logging", false)) {
            plugin.getLogger().info("[玩家登录] " + player.getName() + " - 数据加载完成");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 清除缓存
        plugin.getItemUsageManager().clearCache(player.getUniqueId());
        
        if (plugin.getConfig().getBoolean("debug.verbose-logging", false)) {
            plugin.getLogger().info("[玩家退出] " + player.getName() + " - 缓存已清理");
        }
    }
}

