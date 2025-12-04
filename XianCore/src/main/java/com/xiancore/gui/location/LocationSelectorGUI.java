package com.xiancore.gui.location;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.logging.Logger;

/**
 * 位置选择器GUI系统
 * Location Selector GUI System
 *
 * @author XianCore
 * @version 1.0
 */
public class LocationSelectorGUI {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, SelectionData> playerSelections; // 玩家位置选择数据

    /**
     * 选择数据类
     */
    public static class SelectionData {
        public UUID playerId;
        public String playerName;
        public Location primaryLocation;   // 第一个点
        public Location secondaryLocation; // 第二个点
        public boolean selecting;
        public long startTime;

        public SelectionData(UUID playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.primaryLocation = null;
            this.secondaryLocation = null;
            this.selecting = false;
            this.startTime = System.currentTimeMillis();
        }

        public boolean isComplete() {
            return primaryLocation != null && secondaryLocation != null;
        }

        public void reset() {
            this.primaryLocation = null;
            this.secondaryLocation = null;
        }
    }

    /**
     * 选择区域信息
     */
    public static class SelectionRegion {
        public Location point1;
        public Location point2;
        public int volumeBlocks;
        public double distance;

        public SelectionRegion(Location point1, Location point2) {
            this.point1 = point1;
            this.point2 = point2;
            this.distance = point1.distance(point2);

            // 计算体积
            int dx = Math.abs(point1.getBlockX() - point2.getBlockX()) + 1;
            int dy = Math.abs(point1.getBlockY() - point2.getBlockY()) + 1;
            int dz = Math.abs(point1.getBlockZ() - point2.getBlockZ()) + 1;
            this.volumeBlocks = dx * dy * dz;
        }

        public Location getCenter() {
            double x = (point1.getX() + point2.getX()) / 2;
            double y = (point1.getY() + point2.getY()) / 2;
            double z = (point1.getZ() + point2.getZ()) / 2;
            return new Location(point1.getWorld(), x, y, z);
        }
    }

    /**
     * 构造函数
     */
    public LocationSelectorGUI(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerSelections = new HashMap<>();
    }

    /**
     * 开始位置选择
     */
    public void startSelection(Player player) {
        try {
            SelectionData data = new SelectionData(player.getUniqueId(), player.getName());
            data.selecting = true;
            playerSelections.put(player.getUniqueId(), data);

            player.sendMessage("");
            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("§6§l  位置选择器");
            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("§e模式: §a位置选择");
            player.sendMessage("§e指令:");
            player.sendMessage("  §a左键 §7- 选择第一个点 (主点)");
            player.sendMessage("  §b右键 §7- 选择第二个点 (副点)");
            player.sendMessage("  §c/confirm §7- 确认选择");
            player.sendMessage("  §d/cancel §7- 取消选择");
            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("");

            logger.info("§a✓ 玩家 " + player.getName() + " 开始位置选择");

        } catch (Exception e) {
            logger.severe("§c✗ 开始位置选择失败: " + e.getMessage());
        }
    }

    /**
     * 处理玩家左键点击事件 (选择第一个点)
     */
    public void handlePrimaryClick(Player player, Location location) {
        try {
            SelectionData data = playerSelections.get(player.getUniqueId());
            if (data == null) {
                return;
            }

            data.primaryLocation = location;

            player.sendMessage("§a✓ 已选择第一个点: §7(" + location.getBlockX() + ", " +
                    location.getBlockY() + ", " + location.getBlockZ() + ")");
            player.sendMessage("§7请选择第二个点");

            logger.info("§a✓ 玩家 " + player.getName() + " 选择了第一个点");

        } catch (Exception e) {
            logger.severe("§c✗ 处理主点击失败: " + e.getMessage());
        }
    }

    /**
     * 处理玩家右键点击事件 (选择第二个点)
     */
    public void handleSecondaryClick(Player player, Location location) {
        try {
            SelectionData data = playerSelections.get(player.getUniqueId());
            if (data == null) {
                return;
            }

            if (data.primaryLocation == null) {
                player.sendMessage("§c✗ 请先选择第一个点");
                return;
            }

            data.secondaryLocation = location;

            player.sendMessage("§a✓ 已选择第二个点: §7(" + location.getBlockX() + ", " +
                    location.getBlockY() + ", " + location.getBlockZ() + ")");

            // 显示选择信息
            SelectionRegion region = new SelectionRegion(data.primaryLocation, data.secondaryLocation);
            showSelectionInfo(player, region);

            logger.info("§a✓ 玩家 " + player.getName() + " 选择了第二个点");

        } catch (Exception e) {
            logger.severe("§c✗ 处理副点击失败: " + e.getMessage());
        }
    }

    /**
     * 显示选择信息
     */
    private void showSelectionInfo(Player player, SelectionRegion region) {
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§6§l  选择信息");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§e第一个点: §7(" + region.point1.getBlockX() + ", " +
                region.point1.getBlockY() + ", " + region.point1.getBlockZ() + ")");
        player.sendMessage("§e第二个点: §7(" + region.point2.getBlockX() + ", " +
                region.point2.getBlockY() + ", " + region.point2.getBlockZ() + ")");
        player.sendMessage("§e中心点: §7(" + String.format("%.1f", region.getCenter().getX()) + ", " +
                String.format("%.1f", region.getCenter().getY()) + ", " +
                String.format("%.1f", region.getCenter().getZ()) + ")");
        player.sendMessage("§e距离: §a" + String.format("%.2f", region.distance) + " 格");
        player.sendMessage("§e体积: §a" + region.volumeBlocks + " 方块");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("");
        player.sendMessage("§7输入 §a/confirm §7确认选择，§c/cancel §7取消选择");
    }

    /**
     * 确认选择
     */
    public SelectionRegion confirmSelection(Player player) {
        try {
            SelectionData data = playerSelections.get(player.getUniqueId());
            if (data == null) {
                player.sendMessage("§c✗ 你没有进行任何位置选择");
                return null;
            }

            if (!data.isComplete()) {
                player.sendMessage("§c✗ 请先选择两个点");
                return null;
            }

            SelectionRegion region = new SelectionRegion(data.primaryLocation, data.secondaryLocation);
            playerSelections.remove(player.getUniqueId());

            player.sendMessage("§a✓ 位置选择已确认");
            player.sendMessage("§7中心点: §a(" + String.format("%.1f", region.getCenter().getX()) + ", " +
                    String.format("%.1f", region.getCenter().getY()) + ", " +
                    String.format("%.1f", region.getCenter().getZ()) + ")");

            logger.info("§a✓ 玩家 " + player.getName() + " 确认了位置选择");

            return region;

        } catch (Exception e) {
            logger.severe("§c✗ 确认选择失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 取消选择
     */
    public void cancelSelection(Player player) {
        try {
            SelectionData data = playerSelections.get(player.getUniqueId());
            if (data == null) {
                player.sendMessage("§c✗ 你没有进行任何位置选择");
                return;
            }

            playerSelections.remove(player.getUniqueId());
            player.sendMessage("§c✗ 位置选择已取消");
            logger.info("§a✓ 玩家 " + player.getName() + " 取消了位置选择");

        } catch (Exception e) {
            logger.severe("§c✗ 取消选择失败: " + e.getMessage());
        }
    }

    /**
     * 获取玩家的选择数据
     */
    public SelectionData getSelectionData(UUID playerId) {
        return playerSelections.get(playerId);
    }

    /**
     * 检查玩家是否在选择中
     */
    public boolean isSelecting(UUID playerId) {
        SelectionData data = playerSelections.get(playerId);
        return data != null && data.selecting;
    }

    /**
     * 清除玩家的选择
     */
    public void clearSelection(UUID playerId) {
        playerSelections.remove(playerId);
    }

    /**
     * 清除所有选择
     */
    public void clearAllSelections() {
        playerSelections.clear();
        logger.info("§a✓ 已清除所有位置选择");
    }

    /**
     * 获取活跃选择数量
     */
    public int getActiveSelectionCount() {
        return playerSelections.size();
    }

    /**
     * 传送玩家到位置
     */
    public void teleportPlayer(Player player, Location location) {
        try {
            player.teleport(location);
            player.sendMessage("§a✓ 已传送到位置: §7(" + location.getBlockX() + ", " +
                    location.getBlockY() + ", " + location.getBlockZ() + ")");
            logger.info("§a✓ 玩家 " + player.getName() + " 被传送到了指定位置");

        } catch (Exception e) {
            logger.severe("§c✗ 传送失败: " + e.getMessage());
            player.sendMessage("§c✗ 传送失败: " + e.getMessage());
        }
    }
}
