package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 调试命令
 * /xiancore debug
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class DebugCommand extends AbstractSubCommand {

    public DebugCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "debug";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"调试"};
    }

    @Override
    public String getPermission() {
        return "xiancore.debug";
    }

    @Override
    public String getUsage() {
        return "/xiancore debug";
    }

    @Override
    public String getDescription() {
        return "切换调试模式";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }

        // 切换调试模式
        boolean currentDebugMode = plugin.isDebugMode();
        boolean newDebugMode = !currentDebugMode;

        plugin.setDebugMode(newDebugMode);

        if (newDebugMode) {
            sendSuccess(sender, "§a调试模式已§l启用§r");
            sendInfo(sender, "§7日志级别已提升至 DEBUG");
            sendInfo(sender, "§7将显示详细的系统日志和性能信息");
        } else {
            sendSuccess(sender, "§c调试模式已§l禁用§r");
            sendInfo(sender, "§7日志级别已恢复至 INFO");
            sendInfo(sender, "§7将隐藏详细日志");
        }

        // 全管理员广播
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("xiancore.debug")) {
                player.sendMessage("§6[调试模式]§r " + sender.getName() + " 已" + (newDebugMode ? "启用" : "禁用") + "调试模式");
            }
        }
    }
}
