package com.xiancore.systems.boss.command;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.permission.PermissionChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Boss命令基类
 * 所有Boss相关命令的基础实现
 *
 * 支持:
 * - 权限检查
 * - 参数验证
 * - 自动补全
 * - 帮助信息
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-15
 */
public abstract class BossCommand implements CommandExecutor, TabCompleter {

    protected final XianCore plugin;
    protected final Logger logger;
    protected final PermissionChecker permissionChecker;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param permissionChecker 权限检查器
     */
    public BossCommand(XianCore plugin, PermissionChecker permissionChecker) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.permissionChecker = permissionChecker;
    }

    // ==================== 命令执行 ====================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0) {
                return handleHelp(sender);
            }

            String subcommand = args[0].toLowerCase();

            // 分发子命令
            return switch (subcommand) {
                case "help" -> handleHelp(sender);
                case "list" -> handleList(sender, args);
                case "info" -> handleInfo(sender, args);
                case "stats" -> handleStats(sender, args);
                case "tp", "teleport" -> handleTeleport(sender, args);
                case "gui", "menu" -> handleGUI(sender, args);
                case "perm", "permission" -> handlePermission(sender, args);
                case "add" -> handleAdd(sender, args);
                case "remove" -> handleRemove(sender, args);
                case "edit" -> handleEdit(sender, args);
                case "enable" -> handleEnable(sender, args);
                case "disable" -> handleDisable(sender, args);
                case "spawn" -> handleSpawn(sender, args);
                case "reload" -> handleReload(sender, args);
                default -> {
                    sender.sendMessage("§c未知命令: §e" + subcommand);
                    handleHelp(sender);
                    yield false;
                }
            };
        } catch (Exception e) {
            sender.sendMessage("§c执行命令时出错: " + e.getMessage());
            logger.severe("Boss命令执行异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return getMainCommandSuggestions(sender);
        }

        String subcommand = args[0].toLowerCase();
        return switch (subcommand) {
            case "info", "remove", "enable", "disable", "spawn", "tp", "teleport" -> getSpawnPointNames();
            case "edit" -> args.length == 2 ? getSpawnPointNames() : getEditParameterSuggestions(args);
            case "perm", "permission" -> getPermissionCommandSuggestions(args);
            default -> new ArrayList<>();
        };
    }

    // ==================== 子命令处理 ====================

    /**
     * 处理帮助命令
     */
    protected abstract boolean handleHelp(CommandSender sender);

    /**
     * 处理列表命令 - /boss list
     */
    protected abstract boolean handleList(CommandSender sender, String[] args);

    /**
     * 处理信息命令 - /boss info <id>
     */
    protected abstract boolean handleInfo(CommandSender sender, String[] args);

    /**
     * 处理统计命令 - /boss stats
     */
    protected abstract boolean handleStats(CommandSender sender, String[] args);

    /**
     * 处理添加命令 - /boss add <id> <location> <mob-type>
     */
    protected abstract boolean handleAdd(CommandSender sender, String[] args);

    /**
     * 处理删除命令 - /boss remove <id>
     */
    protected abstract boolean handleRemove(CommandSender sender, String[] args);

    /**
     * 处理编辑命令 - /boss edit <id> <参数> <值>
     */
    protected abstract boolean handleEdit(CommandSender sender, String[] args);

    /**
     * 处理启用命令 - /boss enable <id>
     */
    protected abstract boolean handleEnable(CommandSender sender, String[] args);

    /**
     * 处理禁用命令 - /boss disable <id>
     */
    protected abstract boolean handleDisable(CommandSender sender, String[] args);

    /**
     * 处理生成命令 - /boss spawn <id>
     */
    protected abstract boolean handleSpawn(CommandSender sender, String[] args);

    /**
     * 处理传送命令 - /boss tp <id>
     */
    protected abstract boolean handleTeleport(CommandSender sender, String[] args);

    /**
     * 处理重载命令 - /boss reload
     */
    protected abstract boolean handleReload(CommandSender sender, String[] args);

    /**
     * 处理权限命令 - /boss perm <subcommand>
     */
    protected abstract boolean handlePermission(CommandSender sender, String[] args);

    /**
     * 处理GUI命令 - /boss gui
     */
    protected abstract boolean handleGUI(CommandSender sender, String[] args);

    // ==================== 自动补全 ====================

    /**
     * 获取主命令建议
     */
    protected List<String> getMainCommandSuggestions(CommandSender sender) {
        List<String> suggestions = new ArrayList<>();

        suggestions.add("help");
        suggestions.add("list");
        suggestions.add("info");
        suggestions.add("stats");

        if (permissionChecker.hasPermission(sender, "boss.command.add")) {
            suggestions.add("add");
        }
        if (permissionChecker.hasPermission(sender, "boss.command.remove")) {
            suggestions.add("remove");
        }
        if (permissionChecker.hasPermission(sender, "boss.command.edit")) {
            suggestions.add("edit");
        }
        if (permissionChecker.hasPermission(sender, "boss.command.enable")) {
            suggestions.add("enable");
            suggestions.add("disable");
        }
        if (permissionChecker.hasPermission(sender, "boss.command.spawn")) {
            suggestions.add("spawn");
        }
        if (permissionChecker.hasPermission(sender, "boss.reload")) {
            suggestions.add("reload");
        }
        if (permissionChecker.hasPermission(sender, "boss.admin")) {
            suggestions.add("perm");
            suggestions.add("permission");
            suggestions.add("gui");
            suggestions.add("menu");
        }

        return suggestions;
    }

    /**
     * 获取权限命令建议
     */
    protected List<String> getPermissionCommandSuggestions(String[] args) {
        if (args.length == 2) {
            return List.of("list", "check", "grant", "revoke", "reload");
        }
        return new ArrayList<>();
    }

    /**
     * 获取刷新点名称列表
     */
    protected abstract List<String> getSpawnPointNames();

    /**
     * 获取编辑参数建议
     */
    protected List<String> getEditParameterSuggestions(String[] args) {
        if (args.length == 2) {
            return List.of("tier", "cooldown", "max-count", "location", "enabled");
        }
        return new ArrayList<>();
    }

    // ==================== 辅助方法 ====================

    /**
     * 发送命令使用提示
     */
    protected void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage("§c用法: §e" + usage);
    }

    /**
     * 发送成功消息
     */
    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a✓ " + message);
    }

    /**
     * 发送错误消息
     */
    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c✗ " + message);
    }

    /**
     * 发送信息消息
     */
    protected void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§b➤ " + message);
    }

    /**
     * 发送标题
     */
    protected void sendTitle(CommandSender sender, String title) {
        sender.sendMessage("§e========== " + title + " ==========");
    }

    /**
     * 发送分隔线
     */
    protected void sendSeparator(CommandSender sender) {
        sender.sendMessage("§7" + "=".repeat(50));
    }
}
