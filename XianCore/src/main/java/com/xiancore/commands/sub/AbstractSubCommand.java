package com.xiancore.commands.sub;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 子命令抽象基类
 * 提供通用的工具方法，减少重复代码
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public abstract class AbstractSubCommand implements SubCommand {

    protected final XianCore plugin;

    public AbstractSubCommand(XianCore plugin) {
        this.plugin = plugin;
    }

    // ==================== 默认实现 ====================

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String getPermission() {
        return null; // 默认无需权限
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    // ==================== 权限检查 ====================

    /**
     * 检查权限（带错误提示）
     *
     * @param sender 发送者
     * @return true 有权限，false 无权限
     */
    protected boolean checkPermission(CommandSender sender) {
        String permission = getPermission();
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        if (!sender.hasPermission(permission)) {
            sendError(sender, "你没有权限使用此命令!");
            return false;
        }
        return true;
    }

    /**
     * 检查指定权限
     *
     * @param sender     发送者
     * @param permission 权限节点
     * @return true 有权限
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sendError(sender, "你没有权限执行此操作!");
            return false;
        }
        return true;
    }

    // ==================== 参数验证 ====================

    /**
     * 验证参数数量
     *
     * @param sender   发送者
     * @param args     参数
     * @param minCount 最小数量
     * @return true 参数足够
     */
    protected boolean validateArgs(CommandSender sender, String[] args, int minCount) {
        if (args.length < minCount) {
            sendError(sender, "参数不足!");
            sendInfo(sender, "用法: " + getUsage());
            return false;
        }
        return true;
    }

    /**
     * 验证参数数量（带自定义消息）
     *
     * @param sender   发送者
     * @param args     参数
     * @param minCount 最小数量
     * @param usage    用法提示
     * @return true 参数足够
     */
    protected boolean validateArgs(CommandSender sender, String[] args, int minCount, String usage) {
        if (args.length < minCount) {
            sendError(sender, "参数不足!");
            sendInfo(sender, "用法: " + usage);
            return false;
        }
        return true;
    }

    // ==================== 玩家获取 ====================

    /**
     * 获取在线玩家（带错误提示）
     *
     * @param sender     发送者
     * @param playerName 玩家名
     * @return 玩家对象，不存在返回 null
     */
    protected Player getOnlinePlayer(CommandSender sender, String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sendError(sender, "玩家 " + playerName + " 不在线!");
            return null;
        }
        return player;
    }

    /**
     * 加载玩家数据（带错误提示）
     *
     * @param sender 发送者
     * @param player 玩家
     * @return 玩家数据，失败返回 null
     */
    protected PlayerData loadPlayerData(CommandSender sender, Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            sendError(sender, "加载玩家数据失败!");
            return null;
        }
        return data;
    }

    // ==================== 消息发送 ====================

    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }

    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }

    protected void sendWarning(CommandSender sender, String message) {
        sender.sendMessage("§e" + message);
    }

    protected void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§b" + message);
    }

    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    // ==================== Tab 补全工具 ====================

    /**
     * 过滤 Tab 补全结果
     *
     * @param options 可选项
     * @param input   当前输入
     * @return 过滤后的列表
     */
    protected List<String> filterTabComplete(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>(options);
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

    /**
     * 获取所有在线玩家名（用于 Tab 补全）
     *
     * @return 玩家名列表
     */
    protected List<String> getOnlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    // ==================== 数字解析 ====================

    /**
     * 安全解析整数
     *
     * @param sender       发送者
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 解析结果
     */
    protected int parseInt(CommandSender sender, String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            sendError(sender, "'" + str + "' 不是有效的整数!");
            return defaultValue;
        }
    }

    /**
     * 安全解析长整数
     *
     * @param sender       发送者
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 解析结果
     */
    protected long parseLong(CommandSender sender, String str, long defaultValue) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            sendError(sender, "'" + str + "' 不是有效的数字!");
            return defaultValue;
        }
    }

    /**
     * 安全解析双精度浮点数
     *
     * @param sender       发送者
     * @param str          字符串
     * @param defaultValue 默认值
     * @return 解析结果
     */
    protected double parseDouble(CommandSender sender, String str, double defaultValue) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            sendError(sender, "'" + str + "' 不是有效的数字!");
            return defaultValue;
        }
    }
}
