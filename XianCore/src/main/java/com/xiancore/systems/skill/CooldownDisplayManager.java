package com.xiancore.systems.skill;

import com.xiancore.XianCore;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

/**
 * 冷却时间显示管理器
 * 在ActionBar实时显示功法冷却状态
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class CooldownDisplayManager {

    private final XianCore plugin;
    private final SkillSystem skillSystem;
    private BukkitTask displayTask;
    private boolean enabled;

    public CooldownDisplayManager(XianCore plugin, SkillSystem skillSystem) {
        this.plugin = plugin;
        this.skillSystem = skillSystem;
        this.enabled = plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.cooldown-display.enabled", true);
    }

    /**
     * 启动冷却显示任务
     */
    public void start() {
        if (!enabled) {
            plugin.getLogger().info("  §7冷却显示功能已禁用");
            return;
        }

        // 每秒更新一次所有在线玩家的ActionBar
        displayTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateCooldownDisplay(player);
            }
        }, 0L, 20L); // 20 ticks = 1秒

        plugin.getLogger().info("  §a✓ 冷却显示管理器已启动");
    }

    /**
     * 停止冷却显示任务
     */
    public void stop() {
        if (displayTask != null) {
            displayTask.cancel();
            displayTask = null;
        }
    }

    /**
     * 更新玩家的冷却显示
     */
    private void updateCooldownDisplay(Player player) {
        Map<String, Integer> cooldowns = skillSystem.getCooldownManager().getAllCooldowns(player);

        // 没有冷却，清空ActionBar
        if (cooldowns.isEmpty()) {
            return;
        }

        // 构建ActionBar消息
        StringBuilder message = new StringBuilder("§e⏱ 冷却: ");
        int displayCount = 0;
        int maxDisplay = plugin.getConfigManager().getConfig("config")
                .getInt("skill.cooldown-display.max-skills", 3);

        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            if (displayCount >= maxDisplay) {
                // 超过最大显示数量，显示省略号
                int remaining = cooldowns.size() - displayCount;
                message.append("§7(+").append(remaining).append(")");
                break;
            }

            Skill skill = skillSystem.getSkill(entry.getKey());
            if (skill == null) continue;

            String skillName = skill.getName();
            String formattedTime = formatCooldown(entry.getValue());

            // 根据剩余时间显示不同颜色
            String color = getCooldownColor(entry.getValue());
            message.append(skillName).append(" ")
                   .append(color).append(formattedTime)
                   .append(" §7| ");

            displayCount++;
        }

        // 移除末尾的分隔符
        if (message.length() > 0 && message.toString().endsWith(" §7| ")) {
            message.setLength(message.length() - 5);
        }

        // 发送ActionBar（同步执行）
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(message.toString()));
        });
    }

    /**
     * 格式化冷却时间
     * 
     * @param seconds 秒数
     * @return 格式化后的字符串
     */
    public String formatCooldown(int seconds) {
        if (seconds >= 3600) {
            // 超过1小时：显示小时和分钟
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return String.format("%d时%d分", hours, minutes);
        } else if (seconds >= 60) {
            // 1分钟到1小时：显示分钟和秒
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return String.format("%d分%d秒", minutes, secs);
        } else {
            // 小于1分钟：只显示秒
            return seconds + "秒";
        }
    }

    /**
     * 根据剩余时间获取颜色
     */
    private String getCooldownColor(int seconds) {
        if (seconds <= 5) {
            return "§a"; // 绿色：即将就绪
        } else if (seconds <= 15) {
            return "§e"; // 黄色：较短冷却
        } else {
            return "§c"; // 红色：长时间冷却
        }
    }

    /**
     * 手动触发更新（用于即时反馈）
     */
    public void updatePlayer(Player player) {
        if (!enabled) return;
        updateCooldownDisplay(player);
    }

    /**
     * 设置是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            start();
        } else {
            stop();
        }
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
}












