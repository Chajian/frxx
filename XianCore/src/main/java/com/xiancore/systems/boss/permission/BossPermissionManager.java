package com.xiancore.systems.boss.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Boss系统权限管理器
 * 负责权限检查、缓存和管理
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
public class BossPermissionManager {

    private final Logger logger;
    
    // 权限缓存 (玩家UUID -> 权限节点 -> 是否拥有)
    private final Map<UUID, Map<String, Boolean>> permissionCache = new ConcurrentHashMap<>();
    
    // 缓存过期时间 (毫秒)
    private final long cacheExpireTime = 60000; // 1分钟
    
    // 缓存时间戳 (玩家UUID -> 最后更新时间)
    private final Map<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();

    /**
     * 构造函数
     *
     * @param logger 日志记录器
     */
    public BossPermissionManager(Logger logger) {
        this.logger = logger;
    }

    // ==================== 权限检查 ====================

    /**
     * 检查玩家是否拥有指定权限
     *
     * @param player 玩家
     * @param permission 权限
     * @return 是否拥有权限
     */
    public boolean hasPermission(Player player, BossPermission permission) {
        if (player == null || permission == null) {
            return false;
        }
        
        return hasPermission(player, permission.getNode());
    }

    /**
     * 检查玩家是否拥有指定权限节点
     *
     * @param player 玩家
     * @param node 权限节点
     * @return 是否拥有权限
     */
    public boolean hasPermission(Player player, String node) {
        if (player == null || node == null || node.isEmpty()) {
            return false;
        }

        // 1. 检查管理员权限
        if (player.hasPermission(BossPermission.ADMIN.getNode())) {
            return true;
        }

        // 2. 检查OP权限
        if (player.isOp()) {
            return true;
        }

        // 3. 从缓存中获取
        Boolean cachedResult = getCachedPermission(player.getUniqueId(), node);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 4. 检查实际权限
        boolean hasPermission = player.hasPermission(node);
        
        // 5. 缓存结果
        cachePermission(player.getUniqueId(), node, hasPermission);
        
        return hasPermission;
    }

    /**
     * 检查命令发送者是否拥有权限
     *
     * @param sender 命令发送者
     * @param permission 权限
     * @return 是否拥有权限
     */
    public boolean hasPermission(CommandSender sender, BossPermission permission) {
        if (sender == null || permission == null) {
            return false;
        }

        // 控制台拥有所有权限
        if (!(sender instanceof Player)) {
            return true;
        }

        return hasPermission((Player) sender, permission);
    }

    /**
     * 检查命令发送者是否拥有权限节点
     *
     * @param sender 命令发送者
     * @param node 权限节点
     * @return 是否拥有权限
     */
    public boolean hasPermission(CommandSender sender, String node) {
        if (sender == null || node == null || node.isEmpty()) {
            return false;
        }

        // 控制台拥有所有权限
        if (!(sender instanceof Player)) {
            return true;
        }

        return hasPermission((Player) sender, node);
    }

    /**
     * 检查玩家是否拥有任意一个权限
     *
     * @param player 玩家
     * @param permissions 权限列表
     * @return 是否拥有任意权限
     */
    public boolean hasAnyPermission(Player player, BossPermission... permissions) {
        if (player == null || permissions == null || permissions.length == 0) {
            return false;
        }

        for (BossPermission permission : permissions) {
            if (hasPermission(player, permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查玩家是否拥有所有权限
     *
     * @param player 玩家
     * @param permissions 权限列表
     * @return 是否拥有所有权限
     */
    public boolean hasAllPermissions(Player player, BossPermission... permissions) {
        if (player == null || permissions == null || permissions.length == 0) {
            return false;
        }

        for (BossPermission permission : permissions) {
            if (!hasPermission(player, permission)) {
                return false;
            }
        }

        return true;
    }

    // ==================== 权限检查（带消息） ====================

    /**
     * 检查权限，如果没有权限则发送消息
     *
     * @param player 玩家
     * @param permission 权限
     * @return 是否拥有权限
     */
    public boolean checkPermission(Player player, BossPermission permission) {
        if (hasPermission(player, permission)) {
            return true;
        }

        sendNoPermissionMessage(player, permission);
        return false;
    }

    /**
     * 检查命令发送者权限，如果没有权限则发送消息
     *
     * @param sender 命令发送者
     * @param permission 权限
     * @return 是否拥有权限
     */
    public boolean checkPermission(CommandSender sender, BossPermission permission) {
        if (hasPermission(sender, permission)) {
            return true;
        }

        sendNoPermissionMessage(sender, permission);
        return false;
    }

    // ==================== 缓存管理 ====================

    /**
     * 从缓存中获取权限
     *
     * @param playerUUID 玩家UUID
     * @param node 权限节点
     * @return 缓存的权限结果，如果缓存过期或不存在则返回null
     */
    private Boolean getCachedPermission(UUID playerUUID, String node) {
        // 检查缓存是否过期
        Long lastUpdate = cacheTimestamps.get(playerUUID);
        if (lastUpdate == null || System.currentTimeMillis() - lastUpdate > cacheExpireTime) {
            clearPlayerCache(playerUUID);
            return null;
        }

        Map<String, Boolean> playerPermissions = permissionCache.get(playerUUID);
        if (playerPermissions == null) {
            return null;
        }

        return playerPermissions.get(node);
    }

    /**
     * 缓存权限结果
     *
     * @param playerUUID 玩家UUID
     * @param node 权限节点
     * @param hasPermission 是否拥有权限
     */
    private void cachePermission(UUID playerUUID, String node, boolean hasPermission) {
        Map<String, Boolean> playerPermissions = permissionCache.computeIfAbsent(
            playerUUID, k -> new ConcurrentHashMap<>()
        );
        
        playerPermissions.put(node, hasPermission);
        cacheTimestamps.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * 清除玩家的权限缓存
     *
     * @param playerUUID 玩家UUID
     */
    public void clearPlayerCache(UUID playerUUID) {
        permissionCache.remove(playerUUID);
        cacheTimestamps.remove(playerUUID);
    }

    /**
     * 清除所有权限缓存
     */
    public void clearAllCache() {
        permissionCache.clear();
        cacheTimestamps.clear();
        logger.info("✓ 已清除所有权限缓存");
    }

    // ==================== 权限信息 ====================

    /**
     * 获取玩家拥有的所有Boss权限
     *
     * @param player 玩家
     * @return 权限列表
     */
    public List<BossPermission> getPlayerPermissions(Player player) {
        List<BossPermission> permissions = new ArrayList<>();
        
        for (BossPermission permission : BossPermission.values()) {
            if (hasPermission(player, permission)) {
                permissions.add(permission);
            }
        }
        
        return permissions;
    }

    /**
     * 获取玩家缺少的权限
     *
     * @param player 玩家
     * @return 缺少的权限列表
     */
    public List<BossPermission> getMissingPermissions(Player player) {
        List<BossPermission> missing = new ArrayList<>();
        
        for (BossPermission permission : BossPermission.values()) {
            if (!hasPermission(player, permission)) {
                missing.add(permission);
            }
        }
        
        return missing;
    }

    // ==================== 消息发送 ====================

    /**
     * 发送无权限消息
     *
     * @param sender 命令发送者
     * @param permission 权限
     */
    private void sendNoPermissionMessage(CommandSender sender, BossPermission permission) {
        sender.sendMessage("§c§l[权限不足]");
        sender.sendMessage("§c你没有权限执行此操作");
        sender.sendMessage("§7需要权限: §e" + permission.getNode());
        sender.sendMessage("§7权限说明: §f" + permission.getDescription());
    }

    /**
     * 发送自定义无权限消息
     *
     * @param sender 命令发送者
     * @param message 消息
     */
    public void sendNoPermissionMessage(CommandSender sender, String message) {
        sender.sendMessage("§c§l[权限不足] §c" + message);
    }

    // ==================== 工具方法 ====================

    /**
     * 是否为管理员
     *
     * @param player 玩家
     * @return 是否为管理员
     */
    public boolean isAdmin(Player player) {
        return hasPermission(player, BossPermission.ADMIN);
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public String getCacheStats() {
        return String.format(
            "权限缓存: %d个玩家, %d个权限记录",
            permissionCache.size(),
            permissionCache.values().stream().mapToInt(Map::size).sum()
        );
    }
}
