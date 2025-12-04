package com.xiancore.systems.boss.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * Boss系统权限检查器
 * 负责检查玩家和命令发送者是否拥有特定权限
 *
 * 支持:
 * - Bukkit原生权限检查
 * - 权限继承链验证
 * - 通配符权限支持
 * - 权限拒绝原因详细记录
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-15
 */
public class PermissionChecker {

    private final Logger logger;

    /**
     * 构造函数
     *
     * @param logger 日志记录器
     */
    public PermissionChecker(Logger logger) {
        this.logger = logger;
    }

    // ==================== 权限检查方法 ====================

    /**
     * 检查命令发送者是否拥有权限
     *
     * @param sender 命令发送者
     * @param permission 权限节点
     * @return true 如果拥有权限，false 否则
     */
    public boolean hasPermission(CommandSender sender, String permission) {
        if (sender == null) {
            return false;
        }

        // 控制台总是拥有所有权限
        if (isConsole(sender)) {
            return true;
        }

        // 检查玩家权限
        if (sender instanceof Player player) {
            return checkPlayerPermission(player, permission);
        }

        return false;
    }

    /**
     * 检查玩家权限
     * 支持权限继承和通配符
     *
     * @param player 玩家
     * @param permission 权限节点
     * @return true 如果拥有权限，false 否则
     */
    private boolean checkPlayerPermission(Player player, String permission) {
        // 检查精确权限
        if (player.hasPermission(permission)) {
            return true;
        }

        // 检查通配符权限
        // 例如: boss.* 可以匹配 boss.command.list
        String[] parts = permission.split("\\.");
        for (int i = parts.length; i > 0; i--) {
            String wildcard = String.join(".", Arrays.copyOf(parts, i)) + ".*";
            if (player.hasPermission(wildcard)) {
                return true;
            }
        }

        // 检查管理员权限
        if (player.hasPermission(BossPermissions.ADMIN)) {
            return true;
        }

        return false;
    }

    /**
     * 检查命令发送者是否拥有权限，如果没有则发送消息
     *
     * @param sender 命令发送者
     * @param permission 权限节点
     * @return true 如果拥有权限，false 否则（并发送权限不足消息）
     */
    public boolean checkPermissionOrSendMessage(CommandSender sender, String permission) {
        if (hasPermission(sender, permission)) {
            return true;
        }

        // 发送权限不足消息
        String message = String.format(
            "§c权限不足! 需要权限: §e%s §c(%s)",
            permission,
            BossPermissions.getDescription(permission)
        );
        sender.sendMessage(message);

        // 记录权限拒绝日志
        logPermissionDenied(sender, permission);

        return false;
    }

    /**
     * 获取发送者的权限列表
     * （仅用于Debug或权限管理）
     *
     * @param sender 命令发送者
     * @return 权限列表，如果是控制台返回所有权限
     */
    public List<String> getPermissions(CommandSender sender) {
        List<String> permissions = new ArrayList<>();

        if (isConsole(sender)) {
            // 控制台拥有所有权限
            permissions.addAll(getAllPermissions());
            return permissions;
        }

        if (sender instanceof Player player) {
            // 获取玩家权限
            permissions.add(BossPermissions.ADMIN);
            permissions.add(BossPermissions.USER);
            permissions.add(BossPermissions.RELOAD);
            permissions.add(BossPermissions.COMMAND);
            permissions.add(BossPermissions.NOTIFY);
            // TODO: 从权限插件获取实际权限列表
        }

        return permissions;
    }

    /**
     * 获取所有权限节点
     *
     * @return 所有权限节点列表
     */
    public static List<String> getAllPermissions() {
        return Arrays.asList(
            BossPermissions.ADMIN,
            BossPermissions.USER,
            BossPermissions.RELOAD,
            BossPermissions.CONFIG_EDIT,
            BossPermissions.CONFIG_EDIT_GLOBAL,
            BossPermissions.CONFIG_EDIT_SPAWN_POINTS,
            BossPermissions.COMMAND,
            BossPermissions.COMMAND_LIST,
            BossPermissions.COMMAND_INFO,
            BossPermissions.COMMAND_STATS,
            BossPermissions.COMMAND_ADD,
            BossPermissions.COMMAND_REMOVE,
            BossPermissions.COMMAND_EDIT,
            BossPermissions.COMMAND_ENABLE,
            BossPermissions.COMMAND_SPAWN,
            BossPermissions.NOTIFY,
            BossPermissions.NOTIFY_SPAWN,
            BossPermissions.NOTIFY_KILL,
            BossPermissions.NOTIFY_DESPAWN,
            BossPermissions.NOTIFY_ERROR
        );
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查发送者是否是控制台
     *
     * @param sender 命令发送者
     * @return true 如果是控制台，false 否则
     */
    private boolean isConsole(CommandSender sender) {
        return !(sender instanceof Player);
    }

    /**
     * 记录权限拒绝事件
     *
     * @param sender 命令发送者
     * @param permission 被拒绝的权限
     */
    private void logPermissionDenied(CommandSender sender, String permission) {
        if (sender instanceof Player player) {
            logger.warning(String.format(
                "玩家 %s 尝试执行需要权限 '%s' 的操作",
                player.getName(),
                permission
            ));
        }
    }

    /**
     * 发送权限信息消息
     * 用于调试和管理员查看
     *
     * @param sender 命令发送者
     * @param permission 权限节点
     */
    public void sendPermissionInfo(CommandSender sender, String permission) {
        boolean hasPermission = hasPermission(sender, permission);
        String status = hasPermission ? "§a✓" : "§c✗";
        String description = BossPermissions.getDescription(permission);

        sender.sendMessage("§e==== 权限信息 ====");
        sender.sendMessage(String.format("权限: §b%s", permission));
        sender.sendMessage(String.format("状态: %s", status));
        sender.sendMessage(String.format("描述: §d%s", description));
        sender.sendMessage("§e================");
    }

    /**
     * 发送所有权限列表
     *
     * @param sender 命令发送者
     */
    public void sendPermissionsList(CommandSender sender) {
        sender.sendMessage("§e==== Boss权限列表 ====");

        // 主权限
        sender.sendMessage("§b>>> 主权限");
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.ADMIN, BossPermissions.getDescription(BossPermissions.ADMIN)));
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.USER, BossPermissions.getDescription(BossPermissions.USER)));

        // 配置权限
        sender.sendMessage("§b>>> 配置权限");
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.RELOAD, BossPermissions.getDescription(BossPermissions.RELOAD)));
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.CONFIG_EDIT, BossPermissions.getDescription(BossPermissions.CONFIG_EDIT)));

        // 命令权限
        sender.sendMessage("§b>>> 命令权限");
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.COMMAND_LIST, BossPermissions.getDescription(BossPermissions.COMMAND_LIST)));
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.COMMAND_INFO, BossPermissions.getDescription(BossPermissions.COMMAND_INFO)));
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.COMMAND_ADD, BossPermissions.getDescription(BossPermissions.COMMAND_ADD)));
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.COMMAND_REMOVE, BossPermissions.getDescription(BossPermissions.COMMAND_REMOVE)));

        // 通知权限
        sender.sendMessage("§b>>> 通知权限");
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.NOTIFY, BossPermissions.getDescription(BossPermissions.NOTIFY)));
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.NOTIFY_SPAWN, BossPermissions.getDescription(BossPermissions.NOTIFY_SPAWN)));
        sender.sendMessage(String.format("  §7%s - %s", BossPermissions.NOTIFY_KILL, BossPermissions.getDescription(BossPermissions.NOTIFY_KILL)));

        sender.sendMessage("§e=====================");
    }
}
