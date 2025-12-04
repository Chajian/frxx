package com.xiancore.commands;

import com.xiancore.XianCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令基类
 * 提供统一的命令处理框架和工具方法
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final XianCore plugin;

    public BaseCommand(XianCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // 如果需要玩家执行
        if (requiresPlayer() && !(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行!");
            return true;
        }

        // 执行具体命令
        try {
            execute(sender, args);
        } catch (Exception e) {
            sender.sendMessage("§c命令执行出错: " + e.getMessage());
            plugin.getLogger().severe("命令执行异常: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        try {
            return tabComplete(sender, args);
        } catch (Exception e) {
            plugin.getLogger().warning("Tab补全异常: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 执行命令的具体逻辑
     *
     * @param sender 命令发送者
     * @param args   命令参数
     */
    protected abstract void execute(CommandSender sender, String[] args);

    /**
     * Tab 补全逻辑
     *
     * @param sender 命令发送者
     * @param args   当前输入的参数
     * @return 补全建议列表
     */
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    /**
     * 是否需要玩家执行（默认为 true）
     *
     * @return true 需要玩家，false 控制台也可以
     */
    protected boolean requiresPlayer() {
        return true;
    }

    /**
     * 发送成功消息
     */
    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }

    /**
     * 发送错误消息
     */
    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }

    /**
     * 发送警告消息
     */
    protected void sendWarning(CommandSender sender, String message) {
        sender.sendMessage("§e" + message);
    }

    /**
     * 发送信息消息
     */
    protected void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§b" + message);
    }

    /**
     * 检查权限
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sendError(sender, "你没有权限使用此命令!");
            return false;
        }
        return true;
    }

    /**
     * 显示帮助信息
     */
    protected abstract void showHelp(CommandSender sender);

    /**
     * 过滤 Tab 补全结果
     */
    protected List<String> filterTabComplete(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return options;
        }

        List<String> filtered = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        for (String option : options) {
            if (option.toLowerCase().startsWith(lowerInput)) {
                filtered.add(option);
            }
        }

        return filtered;
    }
}
