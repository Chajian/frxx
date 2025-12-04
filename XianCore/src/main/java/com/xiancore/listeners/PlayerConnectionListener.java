package com.xiancore.listeners;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 玩家连接监听器
 * 处理玩家登录、退出事件
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class PlayerConnectionListener implements Listener {

    private final XianCore plugin;

    public PlayerConnectionListener(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 玩家加入服务器
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        try {
            // 加载玩家数据
            PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

            // 检查是否是新玩家
            boolean isNewPlayer = (data == null);
            
            // 如果是新玩家，初始化数据
            if (isNewPlayer) {
                data = plugin.getDataManager().createPlayerData(player.getUniqueId());
                plugin.getLogger().info("为新玩家 " + player.getName() + " 创建数据");
            }

            // 更新最后登录时间
            data.setLastLogin(System.currentTimeMillis());

            // 计算离线修炼收益
            if (plugin.getConfigManager().getConfig("config").getBoolean("cultivation.offline-cultivation", true)) {
                calculateOfflineCultivation(player, data);
            }

            // 保存数据
            plugin.getDataManager().savePlayerData(data);

            // 发送欢迎消息
            sendWelcomeMessage(player, data, isNewPlayer);

            // 加载功法快捷键绑定
            plugin.getSkillSystem().getBindManager().onPlayerJoin(player);

            // 触发加入事件（供其他系统监听）
            publishPlayerJoinEvent(player, data);

        } catch (Exception e) {
            plugin.getLogger().severe("处理玩家 " + player.getName() + " 加入事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 玩家退出服务器
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        try {
            // 保存玩家数据
            PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
            if (data != null) {
                data.setLastLogin(System.currentTimeMillis());
                plugin.getDataManager().savePlayerData(data);
                plugin.getLogger().info("已保存玩家 " + player.getName() + " 的数据");
            }

            // 保存功法快捷键绑定
            plugin.getSkillSystem().getBindManager().onPlayerQuit(player);

            // 触发退出事件（供其他系统监听）
            publishPlayerQuitEvent(player);

        } catch (Exception e) {
            plugin.getLogger().severe("处理玩家 " + player.getName() + " 退出事件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 计算离线修炼收益
     */
    private void calculateOfflineCultivation(Player player, PlayerData data) {
        long lastLogin = data.getLastLogin();
        if (lastLogin == 0) {
            return; // 新玩家，跳过
        }

        long now = System.currentTimeMillis();
        long offlineTime = now - lastLogin; // 离线时长（毫秒）
        long offlineSeconds = offlineTime / 1000; // 转换为秒

        // 最少离线5分钟才计算
        if (offlineSeconds < 300) {
            return;
        }

        // 获取离线修炼效率
        double efficiency = plugin.getConfigManager().getConfig("config").getDouble("cultivation.offline-efficiency", 0.5);

        // 计算离线修炼获得的修为
        // 假设在线每小时获得 100 修为，离线按效率计算
        long baseQiPerHour = 100;
        long offlineHours = offlineSeconds / 3600;
        long gainedQi = (long) (baseQiPerHour * offlineHours * efficiency);

        if (gainedQi > 0) {
            data.addQi(gainedQi);

            // 格式化离线时长
            String timeStr = formatOfflineTime(offlineSeconds);

            // 通知玩家
            player.sendMessage("§b========== 离线修炼 ==========");
            player.sendMessage("§e离线时长: §f" + timeStr);
            player.sendMessage("§e修炼效率: §f" + String.format("%.0f%%", efficiency * 100));
            player.sendMessage("§e获得修为: §a+" + gainedQi);
            player.sendMessage("§e当前修为: §f" + data.getQi());
            player.sendMessage("§b===========================");

            plugin.getLogger().info(player.getName() + " 离线修炼获得 " + gainedQi + " 修为");
        }
    }

    /**
     * 格式化离线时长
     */
    private String formatOfflineTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        if (days > 0) {
            return String.format("%d天 %d小时", days, hours);
        } else if (hours > 0) {
            return String.format("%d小时 %d分钟", hours, minutes);
        } else {
            return String.format("%d分钟", minutes);
        }
    }

    /**
     * 发布玩家加入事件
     */
    private void publishPlayerJoinEvent(Player player, PlayerData data) {
        try {
            // 创建自定义事件数据
            java.util.Map<String, Object> eventData = new java.util.HashMap<>();
            eventData.put("player", player);
            eventData.put("uuid", player.getUniqueId());
            eventData.put("playerName", player.getName());
            eventData.put("realm", data.getRealm());
            eventData.put("level", data.getPlayerLevel());
            eventData.put("isNewPlayer", data.getQi() == 0);

            // 发送事件到事件总线（如果有实现的话）
            // 这里简化为只输出日志
            if (plugin.isDebugMode()) {
                plugin.getLogger().fine("[事件系统] 玩家加入事件: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("发布玩家加入事件时出错: " + e.getMessage());
        }
    }

    /**
     * 发布玩家退出事件
     */
    private void publishPlayerQuitEvent(Player player) {
        try {
            // 创建自定义事件数据
            java.util.Map<String, Object> eventData = new java.util.HashMap<>();
            eventData.put("player", player);
            eventData.put("uuid", player.getUniqueId());
            eventData.put("playerName", player.getName());

            // 发送事件到事件总线（如果有实现的话）
            // 这里简化为只输出日志
            if (plugin.isDebugMode()) {
                plugin.getLogger().fine("[事件系统] 玩家退出事件: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("发布玩家退出事件时出错: " + e.getMessage());
        }
    }

    /**
     * 发送欢迎消息
     */
    private void sendWelcomeMessage(Player player, PlayerData data, boolean isNewPlayer) {
        player.sendMessage("§b========================================");
        
        if (isNewPlayer) {
            player.sendMessage("§6§l欢迎来到修仙世界!");
            player.sendMessage("");
            player.sendMessage("§d✨ 测灵完成！你的资质为：");
            player.sendMessage("§f   " + data.getSpiritualRootDisplay() + " §f(" + String.format("%.1f%%", data.getSpiritualRoot() * 100) + ")");
            player.sendMessage("§f   五行属性: " + data.getSpiritualRootElements());
            player.sendMessage("");
            
            // 根据灵根品质给予不同的祝贺
            player.sendMessage(data.getSpiritualRootDescription());
            player.sendMessage("");
        } else {
            player.sendMessage("§6§l欢迎回到修仙世界!");
        }
        
        player.sendMessage("§e当前境界: §f" + data.getFullRealmName());
        player.sendMessage("§e修为: §f" + data.getQi());
        
        if (!isNewPlayer) {
            // 非新玩家也显示灵根信息，但更简洁
            player.sendMessage("§e灵根: " + data.getSpiritualRootDisplay() + " §7(" + data.getSpiritualRootElements() + "§7)");
        }

        if (data.getSectId() != null) {
            player.sendMessage("§e宗门: §f宗门#" + data.getSectId() + " (" + com.xiancore.systems.sect.SectRank.getDisplayName(data.getSectRank()) + ")");
        } else {
            player.sendMessage("§7你还未加入宗门，使用 §e/sect list §7查看所有宗门");
        }

        player.sendMessage("§7使用 §e/cultivation §7查看修炼信息");
        player.sendMessage("§b========================================");
    }
}
