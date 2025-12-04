package com.xiancore.systems.boss.permission;

import lombok.Getter;

/**
 * Boss系统权限枚举
 * 定义所有Boss系统相关的权限节点
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
@Getter
public enum BossPermission {

    // ==================== 管理权限 ====================
    
    /**
     * 管理员权限 - 所有权限
     */
    ADMIN("boss.admin", "管理员权限", "拥有所有Boss系统权限"),
    
    // ==================== 基础权限 ====================
    
    /**
     * 查看Boss信息
     */
    VIEW("boss.view", "查看Boss信息", "查看活跃Boss列表和详细信息"),
    
    /**
     * 接收Boss公告
     */
    ANNOUNCE("boss.announce", "接收Boss公告", "接收Boss生成、击杀等公告消息"),
    
    /**
     * 挑战Boss
     */
    CHALLENGE("boss.challenge", "挑战Boss", "允许攻击和挑战Boss"),
    
    // ==================== 传送权限 ====================
    
    /**
     * 使用传送功能
     */
    TELEPORT("boss.teleport", "使用传送", "使用付费传送到Boss位置"),
    
    /**
     * 免费传送
     */
    TELEPORT_FREE("boss.teleport.free", "免费传送", "传送到Boss位置无需支付费用"),
    
    /**
     * 无冷却传送
     */
    TELEPORT_NO_COOLDOWN("boss.teleport.nocooldown", "无冷却传送", "传送无需等待冷却时间"),
    
    // ==================== 命令权限 ====================
    
    /**
     * 使用/boss命令
     */
    COMMAND_BASE("boss.command", "基础命令", "使用基础boss命令"),
    
    /**
     * 查看Boss列表
     */
    COMMAND_LIST("boss.command.list", "列表命令", "使用/boss list命令"),
    
    /**
     * 查看Boss信息
     */
    COMMAND_INFO("boss.command.info", "信息命令", "使用/boss info命令"),
    
    /**
     * 查看Boss统计
     */
    COMMAND_STATS("boss.command.stats", "统计命令", "使用/boss stats命令"),
    
    /**
     * 传送命令
     */
    COMMAND_TELEPORT("boss.command.teleport", "传送命令", "使用/boss tp命令"),
    
    // ==================== 管理命令权限 ====================
    
    /**
     * 重载配置
     */
    COMMAND_RELOAD("boss.command.reload", "重载命令", "使用/boss reload重载配置"),
    
    /**
     * 强制生成Boss
     */
    COMMAND_SPAWN("boss.command.spawn", "生成命令", "使用/boss spawn强制生成Boss"),
    
    /**
     * 添加刷新点
     */
    COMMAND_ADD("boss.command.add", "添加命令", "使用/boss add添加刷新点"),
    
    /**
     * 删除刷新点
     */
    COMMAND_REMOVE("boss.command.remove", "删除命令", "使用/boss remove删除刷新点"),
    
    /**
     * 编辑刷新点
     */
    COMMAND_EDIT("boss.command.edit", "编辑命令", "使用/boss edit编辑刷新点"),
    
    /**
     * 启用/禁用刷新点
     */
    COMMAND_TOGGLE("boss.command.toggle", "开关命令", "使用/boss enable/disable切换刷新点状态"),
    
    // ==================== 奖励权限 ====================
    
    /**
     * 接收Boss击杀奖励
     */
    REWARD_RECEIVE("boss.reward.receive", "接收奖励", "击杀Boss后可获得奖励"),
    
    /**
     * 额外奖励倍率
     */
    REWARD_BONUS("boss.reward.bonus", "奖励加成", "获得额外的奖励倍率");

    // ==================== 字段 ====================
    
    /**
     * 权限节点
     */
    private final String node;
    
    /**
     * 权限显示名称
     */
    private final String displayName;
    
    /**
     * 权限描述
     */
    private final String description;

    // ==================== 构造函数 ====================
    
    /**
     * 构造函数
     *
     * @param node 权限节点
     * @param displayName 显示名称
     * @param description 描述
     */
    BossPermission(String node, String displayName, String description) {
        this.node = node;
        this.displayName = displayName;
        this.description = description;
    }

    // ==================== Getter方法 ====================

    /**
     * 获取权限节点
     *
     * @return 权限节点
     */
    public String getNode() {
        return node;
    }

    /**
     * 获取显示名称
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取描述
     *
     * @return 描述
     */
    public String getDescription() {
        return description;
    }

    // ==================== 工具方法 ====================
    
    /**
     * 根据权限节点获取权限枚举
     *
     * @param node 权限节点
     * @return 权限枚举，如果不存在则返回null
     */
    public static BossPermission fromNode(String node) {
        if (node == null || node.isEmpty()) {
            return null;
        }
        
        for (BossPermission permission : values()) {
            if (permission.node.equalsIgnoreCase(node)) {
                return permission;
            }
        }
        
        return null;
    }
    
    /**
     * 检查权限节点是否有效
     *
     * @param node 权限节点
     * @return 是否有效
     */
    public static boolean isValidNode(String node) {
        return fromNode(node) != null;
    }
    
    /**
     * 获取所有权限节点
     *
     * @return 权限节点数组
     */
    public static String[] getAllNodes() {
        BossPermission[] permissions = values();
        String[] nodes = new String[permissions.length];
        
        for (int i = 0; i < permissions.length; i++) {
            nodes[i] = permissions[i].node;
        }
        
        return nodes;
    }
    
    @Override
    public String toString() {
        return node;
    }
}
