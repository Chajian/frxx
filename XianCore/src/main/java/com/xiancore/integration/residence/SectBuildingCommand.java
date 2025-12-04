package com.xiancore.integration.residence;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectRank;
import com.xiancore.systems.sect.SectSystem;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 宗门建筑位命令处理器
 * 处理所有与建筑位相关的命令（/sect building ...）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectBuildingCommand {

    private final XianCore plugin;
    private final SectSystem sectSystem;
    private final BuildingSlotManager buildingSlotManager;

    /**
     * 构造函数
     */
    public SectBuildingCommand(XianCore plugin, SectSystem sectSystem, BuildingSlotManager buildingSlotManager) {
        this.plugin = plugin;
        this.sectSystem = sectSystem;
        this.buildingSlotManager = buildingSlotManager;
    }

    /**
     * 处理建筑位命令
     */
    public void handle(Player player, String[] args) {
        if (args.length < 2) {
            showBuildingHelp(player);
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "place":
            case "放置":
                handlePlace(player, args);
                break;

            case "remove":
            case "移除":
                handleRemove(player, args);
                break;

            case "list":
            case "列表":
                handleList(player, args);
                break;

            case "info":
            case "信息":
                handleInfo(player, args);
                break;

            case "upgrade":
            case "升级":
                handleUpgrade(player, args);
                break;

            default:
                showBuildingHelp(player);
                break;
        }
    }

    /**
     * 处理建筑位放置命令
     * /sect building place <类型> [等级]
     */
    private void handlePlace(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.building.place")) {
            sendError(player, "§c你没有权限放置建筑位!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        // 检查宗门是否有领地
        if (!sect.hasLand()) {
            sendError(player, "§c宗门还没有领地，无法放置建筑位!");
            return;
        }

        // 检查权限：只有宗主和长老可以放置建筑位
        SectRank rank = sect.getMember(player.getUniqueId()).getRank();
        if (rank != SectRank.LEADER && rank != SectRank.ELDER) {
            sendError(player, "§c只有宗主和长老才能放置建筑位!");
            return;
        }

        // 解析参数
        if (args.length < 3) {
            sendError(player, "§c请指定建筑位类型!");
            sendInfo(player, "§7用法: /sect building place <类型> [等级]");
            sendInfo(player, "§7类型: tower, statue, fountain, altar");
            return;
        }

        String slotType = args[2].toLowerCase();
        int level = 1;

        if (args.length > 3) {
            try {
                level = Integer.parseInt(args[3]);
                if (level <= 0 || level > 10) {
                    sendError(player, "§c等级必须在 1 到 10 之间!");
                    return;
                }
            } catch (NumberFormatException e) {
                sendError(player, "§c等级必须是整数!");
                return;
            }
        }

        // 验证建筑位类型
        if (!isValidBuildingType(slotType)) {
            sendError(player, "§c未知的建筑位类型: " + slotType);
            sendInfo(player, "§7有效类型: tower, statue, fountain, altar");
            return;
        }

        // 获取玩家当前位置
        Location location = player.getLocation();

        // 添加建筑位
        if (buildingSlotManager.addBuildingSlot(sect.getId(), slotType, location, level)) {
            sendSuccess(player, "§a§l========== 建筑位放置成功 ==========\"");
            sendSuccess(player, "§a类型: §f" + slotType);
            sendSuccess(player, "§a等级: §f" + level);
            sendSuccess(player, "§a位置: §f" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
            sendSuccess(player, "§a§l============================\"");

            // 广播消息
            sect.broadcastMessage("§a§e【宗门建筑】");
            sect.broadcastMessage("§a宗门放置了新的建筑位!");
            sect.broadcastMessage("§a类型: §f" + slotType + " §a等级: §f" + level);
        } else {
            sendError(player, "§c放置建筑位失败!");
        }
    }

    /**
     * 处理建筑位移除命令
     * /sect building remove <索引号>
     */
    private void handleRemove(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.building.remove")) {
            sendError(player, "§c你没有权限移除建筑位!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        // 检查宗门是否有领地
        if (!sect.hasLand()) {
            sendError(player, "§c宗门还没有领地!");
            return;
        }

        // 检查权限
        SectRank rank = sect.getMember(player.getUniqueId()).getRank();
        if (rank != SectRank.LEADER && rank != SectRank.ELDER) {
            sendError(player, "§c只有宗主和长老才能移除建筑位!");
            return;
        }

        // 解析索引号
        if (args.length < 3) {
            sendError(player, "§c请指定建筑位索引号!");
            sendInfo(player, "§7用法: /sect building remove <索引号>");
            sendInfo(player, "§7使用 /sect building list 查看索引号");
            return;
        }

        try {
            int index = Integer.parseInt(args[2]);
            if (buildingSlotManager.removeBuildingSlot(sect.getId(), index)) {
                sendSuccess(player, "§a建筑位已移除!");

                // 广播消息
                sect.broadcastMessage("§c§e【宗门建筑】");
                sect.broadcastMessage("§c宗门的一个建筑位已被移除!");
            } else {
                sendError(player, "§c移除建筑位失败，索引号无效!");
            }
        } catch (NumberFormatException e) {
            sendError(player, "§c索引号必须是整数!");
        }
    }

    /**
     * 列表查看建筑位
     */
    private void handleList(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.use")) {
            sendError(player, "§c你没有权限查看建筑位列表!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        // 检查宗门是否有领地
        if (!sect.hasLand()) {
            sendError(player, "§c宗门还没有领地!");
            return;
        }

        // 获取建筑位列表
        var slots = buildingSlotManager.getBuildingSlots(sect.getId());

        if (slots.isEmpty()) {
            sendInfo(player, "§7宗门还没有任何建筑位");
            return;
        }

        sendSuccess(player, "§a§l========== 宗门建筑位列表 ==========\"");
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            sendSuccess(player, "§f[" + i + "] " + slot.getDescription());
        }
        sendSuccess(player, "§a§l============================\"");
    }

    /**
     * 查看建筑位信息
     */
    private void handleInfo(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.use")) {
            sendError(player, "§c你没有权限查看建筑位信息!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        sendSuccess(player, "§a§l========== 宗门建筑位统计 ==========\"");
        sendSuccess(player, buildingSlotManager.getStatistics(sect.getId()));
        sendSuccess(player, "§a§l============================\"");
    }

    /**
     * 升级建筑位
     */
    private void handleUpgrade(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.building.upgrade")) {
            sendError(player, "§c你没有权限升级建筑位!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        sendInfo(player, "§7建筑位升级功能即将实现...");
    }

    /**
     * 验证建筑位类型
     */
    private boolean isValidBuildingType(String type) {
        return type.equals("tower") || type.equals("statue") ||
               type.equals("fountain") || type.equals("altar");
    }

    /**
     * 显示建筑位帮助
     */
    private void showBuildingHelp(Player player) {
        player.sendMessage("§a§l========== 宗门建筑位帮助 ==========\"");
        player.sendMessage("§e/sect building place <类型> [等级] §7- 放置建筑位");
        player.sendMessage("§e/sect building remove <索引号> §7- 移除建筑位");
        player.sendMessage("§e/sect building list §7- 查看建筑位列表");
        player.sendMessage("§e/sect building info §7- 查看建筑位统计");
        player.sendMessage("§e/sect building upgrade <索引号> §7- 升级建筑位");
        player.sendMessage("§a§l=============================\"");
    }

    /**
     * 权限检查
     */
    private boolean hasPermission(Player player, String permission) {
        if (!player.hasPermission(permission)) {
            sendError(player, "§c你没有权限执行此命令!");
            return false;
        }
        return true;
    }

    /**
     * 发送成功消息
     */
    private void sendSuccess(Player player, String message) {
        player.sendMessage(message);
    }

    /**
     * 发送错误消息
     */
    private void sendError(Player player, String message) {
        player.sendMessage(message);
    }

    /**
     * 发送信息消息
     */
    private void sendInfo(Player player, String message) {
        player.sendMessage(message);
    }
}
