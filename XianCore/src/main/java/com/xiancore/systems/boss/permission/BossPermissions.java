package com.xiancore.systems.boss.permission;

/**
 * Boss系统权限定义
 * 定义所有Boss系统相关的权限节点
 *
 * 权限继承关系:
 * boss.admin
 *   ├─ boss.reload (热重载配置)
 *   ├─ boss.command (执行命令)
 *   │  ├─ boss.command.list (查看列表)
 *   │  ├─ boss.command.info (查看详情)
 *   │  ├─ boss.command.stats (查看统计)
 *   │  ├─ boss.command.add (添加刷新点)
 *   │  ├─ boss.command.remove (移除刷新点)
 *   │  ├─ boss.command.edit (编辑刷新点)
 *   │  └─ boss.command.enable (启用/禁用)
 *   └─ boss.notify (接收通知)
 *       ├─ boss.notify.spawn (生成通知)
 *       ├─ boss.notify.kill (击杀通知)
 *       └─ boss.notify.error (错误通知)
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-15
 */
public class BossPermissions {

    // ==================== 主权限 ====================

    /**
     * Boss系统管理员权限 - 拥有所有权限
     * 包括: 配置、命令执行、通知接收
     */
    public static final String ADMIN = "boss.admin";

    /**
     * Boss系统使用者权限 - 基础权限
     * 包括: 查看信息、接收通知
     */
    public static final String USER = "boss.user";

    // ==================== 配置管理权限 ====================

    /**
     * 配置重载权限
     * 允许使用 /boss reload 命令重载配置文件
     */
    public static final String RELOAD = "boss.reload";

    /**
     * 配置编辑权限 (通用)
     * 包括所有配置修改权限
     */
    public static final String CONFIG_EDIT = "boss.config.edit";

    /**
     * 全局设置编辑权限
     * 修改 check-interval, max-active-bosses, min-online-players
     */
    public static final String CONFIG_EDIT_GLOBAL = "boss.config.edit.global";

    /**
     * 刷新点编辑权限 (通用)
     * 包括添加、删除、修改刷新点
     */
    public static final String CONFIG_EDIT_SPAWN_POINTS = "boss.config.edit.spawn-points";

    // ==================== 命令权限 ====================

    /**
     * Boss命令权限 (通用)
     * 包括所有Boss命令
     */
    public static final String COMMAND = "boss.command";

    /**
     * 查看刷新点列表
     * 命令: /boss list
     */
    public static final String COMMAND_LIST = "boss.command.list";

    /**
     * 查看刷新点详细信息
     * 命令: /boss info <id>
     */
    public static final String COMMAND_INFO = "boss.command.info";

    /**
     * 查看系统统计信息
     * 命令: /boss stats
     */
    public static final String COMMAND_STATS = "boss.command.stats";

    /**
     * 添加新的刷新点
     * 命令: /boss add <id> <location> <mob-type>
     */
    public static final String COMMAND_ADD = "boss.command.add";

    /**
     * 移除刷新点
     * 命令: /boss remove <id>
     */
    public static final String COMMAND_REMOVE = "boss.command.remove";

    /**
     * 编辑刷新点参数
     * 命令: /boss edit <id> <参数> <值>
     */
    public static final String COMMAND_EDIT = "boss.command.edit";

    /**
     * 启用或禁用刷新点
     * 命令: /boss enable <id> 或 /boss disable <id>
     */
    public static final String COMMAND_ENABLE = "boss.command.enable";

    /**
     * 立即手动生成Boss
     * 命令: /boss spawn <id>
     */
    public static final String COMMAND_SPAWN = "boss.command.spawn";

    // ==================== 通知权限 ====================

    /**
     * 接收通知权限 (通用)
     * 包括所有Boss系统通知
     */
    public static final String NOTIFY = "boss.notify";

    /**
     * Boss生成通知
     * 当Boss被生成时，拥有此权限的玩家会收到通知
     */
    public static final String NOTIFY_SPAWN = "boss.notify.spawn";

    /**
     * Boss击杀通知
     * 当Boss被击杀时，拥有此权限的玩家会收到通知
     */
    public static final String NOTIFY_KILL = "boss.notify.kill";

    /**
     * Boss消失通知
     * 当Boss消失/逃脱时，拥有此权限的玩家会收到通知
     */
    public static final String NOTIFY_DESPAWN = "boss.notify.despawn";

    /**
     * 错误通知
     * 当Boss系统出错时，拥有此权限的玩家会收到通知
     */
    public static final String NOTIFY_ERROR = "boss.notify.error";

    // ==================== 权限判断辅助方法 ====================

    /**
     * 检查玩家是否拥有权限
     *
     * @param permissionNode 权限节点
     * @param operatorPermissions 操作者权限集合
     * @return true 如果拥有权限，false 否则
     */
    public static boolean hasPermission(String permissionNode, java.util.Set<String> operatorPermissions) {
        if (operatorPermissions == null) {
            return false;
        }

        // 检查精确权限
        if (operatorPermissions.contains(permissionNode)) {
            return true;
        }

        // 检查通配符权限
        // 例如: boss.* 可以匹配 boss.command.list
        String[] parts = permissionNode.split("\\.");
        for (int i = parts.length; i > 0; i--) {
            String parent = String.join(".", java.util.Arrays.copyOf(parts, i)) + ".*";
            if (operatorPermissions.contains(parent)) {
                return true;
            }
        }

        // 检查父权限
        // 例如: boss.admin 拥有所有 boss.* 权限
        if (operatorPermissions.contains(ADMIN)) {
            return permissionNode.startsWith("boss.");
        }

        return false;
    }

    /**
     * 获取权限的中文描述
     *
     * @param permission 权限节点
     * @return 权限描述
     */
    public static String getDescription(String permission) {
        return switch (permission) {
            case ADMIN -> "Boss系统管理员 - 拥有所有权限";
            case USER -> "Boss系统使用者 - 基础权限";
            case RELOAD -> "重载配置 - 允许使用 /boss reload";
            case CONFIG_EDIT -> "编辑配置 - 允许修改所有配置";
            case CONFIG_EDIT_GLOBAL -> "编辑全局设置";
            case CONFIG_EDIT_SPAWN_POINTS -> "编辑刷新点";
            case COMMAND -> "执行命令 - 允许使用所有Boss命令";
            case COMMAND_LIST -> "查看列表 - /boss list";
            case COMMAND_INFO -> "查看详情 - /boss info";
            case COMMAND_STATS -> "查看统计 - /boss stats";
            case COMMAND_ADD -> "添加刷新点 - /boss add";
            case COMMAND_REMOVE -> "移除刷新点 - /boss remove";
            case COMMAND_EDIT -> "编辑刷新点 - /boss edit";
            case COMMAND_ENABLE -> "启用/禁用 - /boss enable/disable";
            case COMMAND_SPAWN -> "手动生成 - /boss spawn";
            case NOTIFY -> "接收通知 - 所有Boss通知";
            case NOTIFY_SPAWN -> "生成通知 - Boss生成时通知";
            case NOTIFY_KILL -> "击杀通知 - Boss击杀时通知";
            case NOTIFY_DESPAWN -> "消失通知 - Boss消失时通知";
            case NOTIFY_ERROR -> "错误通知 - 系统错误时通知";
            default -> "未知权限: " + permission;
        };
    }

    /**
     * 私有构造函数，防止实例化
     */
    private BossPermissions() {
        throw new UnsupportedOperationException("Cannot instantiate BossPermissions");
    }
}
