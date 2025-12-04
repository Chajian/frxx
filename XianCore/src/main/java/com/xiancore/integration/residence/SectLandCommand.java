package com.xiancore.integration.residence;

import com.xiancore.XianCore;
import com.xiancore.commands.BaseCommand;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectRank;
import com.xiancore.systems.sect.SectSystem;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 宗门领地命令处理器
 * 处理所有与领地相关的命令（/sect land ...）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectLandCommand {

    private final XianCore plugin;
    private final SectSystem sectSystem;
    private final SectResidenceManager residenceManager;

    /**
     * 构造函数
     */
    public SectLandCommand(XianCore plugin, SectSystem sectSystem, SectResidenceManager residenceManager) {
        this.plugin = plugin;
        this.sectSystem = sectSystem;
        this.residenceManager = residenceManager;
    }

    /**
     * 处理领地命令
     */
    public void handle(Player player, String[] args) {
        if (args.length < 2) {
            showLandHelp(player);
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "claim":
            case "圈地":
                handleClaim(player, args);
                break;

            case "expand":
            case "扩展":
                handleExpand(player, args);
                break;

            case "shrink":
            case "缩小":
                handleShrink(player, args);
                break;

            case "delete":
            case "删除":
                handleDelete(player, args);
                break;

            case "info":
            case "信息":
                handleInfo(player, args);
                break;

            case "status":
            case "状态":
                handleStatus(player, args);
                break;

            case "pay":
            case "缴费":
                handlePay(player, args);
                break;

            case "members":
            case "成员":
                handleMembers(player, args);
                break;

            case "transfer":
            case "转让":
                handleTransfer(player, args);
                break;

            case "gui":
            case "界面":
                handleGUI(player, args);
                break;

            default:
                showLandHelp(player);
                break;
        }
    }

    /**
     * 处理圈地命令
     * /sect land claim
     * 改为：绑定玩家当前位置所在的已有 Residence 领地到宗门（固定绑定费用），并转移所有者为宗主
     */
    private void handleClaim(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.land.claim")) {
            sendError(player, "§c你没有权限圈地!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        // 检查权限：只有宗主和长老可以圈地
        SectRank rank = sect.getMember(player.getUniqueId()).getRank();
        if (rank != SectRank.LEADER && rank != SectRank.ELDER) {
            sendError(player, "§c只有宗主和长老才能圈地!");
            sendInfo(player, "§7当前职位: " + rank.getDisplayName());
            return;
        }

        // 检查宗门是否已有领地
        if (sect.hasLand()) {
            sendError(player, "§c宗门已经拥有领地，暂不支持多领地!");
            sendInfo(player, "§7当前领地: " + sect.getResidenceLandId());
            return;
        }

        // 绑定玩家当前位置所在的已有 Residence
        SectResidenceManager.ClaimResult claimResult = residenceManager.bindExistingResidenceAtPlayer(sect, player);

        if (claimResult.isSuccess()) {
            // 保存宗门数据
            sectSystem.saveSect(sect);

            // 为所有宗门成员设置权限
            try {
                com.bekvon.bukkit.residence.protection.ClaimedResidence residence =
                    com.bekvon.bukkit.residence.api.ResidenceApi.getResidenceManager()
                        .getByName(claimResult.getResidenceId());

                if (residence != null) {
                    sectSystem.getPermissionManager().setupSectPermissions(sect, residence);
                    sendSuccess(player, "§a权限已设置");
                } else {
                    sendError(player, "§c警告: 无法获取领地对象来设置权限");
                }
            } catch (Exception e) {
                sendError(player, "§c警告: 设置权限时出错");
                plugin.getLogger().warning("设置圈地权限失败: " + e.getMessage());
            }

            // 成功消息
            sendSuccess(player, "§a§l========== 绑定成功 ==========");
            sendSuccess(player, "§a领地ID: §f" + claimResult.getResidenceId());
            sendSuccess(player, "§a绑定费用: §6" + residenceManager.getBindingFlatFee());
            Location centerLoc = sect.getLandCenter();
            if (centerLoc != null) {
                sendSuccess(player, "§a领地中心: §f" + centerLoc.getBlockX() + ", " + centerLoc.getBlockY() + ", " + centerLoc.getBlockZ());
            }
            sendSuccess(player, "§a§l========================");

            // 广播消息
            sect.broadcastMessage("§a§e【宗门领地】");
            sect.broadcastMessage("§a宗门已成功绑定领地！");
        } else {
            sendError(player, "§c圈地失败!");
            sendError(player, "§c原因: " + claimResult.getErrorMessage());
        }
    }

    /**
     * 处理扩展领地命令
     * /sect land expand [块数]
     */
    private void handleExpand(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.land.expand")) {
            sendError(player, "§c你没有权限扩展领地!");
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
            sendError(player, "§c宗门还没有领地，请先圈地!");
            return;
        }

        // 检查权限：只有宗主和长老可以扩展
        SectRank rank = sect.getMember(player.getUniqueId()).getRank();
        if (rank != SectRank.LEADER && rank != SectRank.ELDER) {
            sendError(player, "§c只有宗主和长老才能扩展领地!");
            return;
        }

        // 解析参数
        int expandSize = 1;
        if (args.length > 2) {
            try {
                expandSize = Integer.parseInt(args[2]);
                if (expandSize <= 0 || expandSize > 50) {
                    sendError(player, "§c扩展大小必须在 1 到 50 之间!");
                    return;
                }
            } catch (NumberFormatException e) {
                sendError(player, "§c扩展大小必须是整数!");
                return;
            }
        }

        // 计算扩展成本
        long cost = residenceManager.calculateClaimCost(sect, expandSize);

        // 检查资金
        if (sect.getSectFunds() < cost) {
            long shortage = cost - sect.getSectFunds();
            sendError(player, "§c宗门资金不足！需要: §6" + cost + " §c当前: §6" + sect.getSectFunds());
            sendInfo(player, "§e缺少: §6" + shortage + " §e灵石");
            return;
        }

        // 执行扩展
        SectResidenceManager.ExpandResult result = residenceManager.expandLand(sect, expandSize, cost);

        if (result.isSuccess()) {
            // 保存宗门数据
            sectSystem.saveSect(sect);

            // 成功消息
            sendSuccess(player, "§a§l========== 领地扩展成功 ==========");
            sendSuccess(player, "§a扩展大小: §f" + expandSize + " 块");
            sendSuccess(player, "§a消耗灵石: §6" + cost);
            sendSuccess(player, "§a宗门剩余资金: §6" + sect.getSectFunds());
            sendSuccess(player, "§a§l============================");

            // 广播消息
            sect.broadcastMessage("§a§e【宗门领地】");
            sect.broadcastMessage("§a宗门领地已成功扩展！");
            sect.broadcastMessage("§a扩展大小: §f" + expandSize + " 块");
        } else {
            sendError(player, "§c领地扩展失败!");
            sendError(player, "§c原因: " + result.getErrorMessage());
        }
    }

    /**
     * 处理缩小领地命令
     * /sect land shrink [块数]
     */
    private void handleShrink(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.land.expand")) {
            sendError(player, "§c你没有权限缩小领地!");
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

        // 检查权限：只有宗主和长老可以缩小
        SectRank rank = sect.getMember(player.getUniqueId()).getRank();
        if (rank != SectRank.LEADER && rank != SectRank.ELDER) {
            sendError(player, "§c只有宗主和长老才能缩小领地!");
            return;
        }

        // 解析参数
        int shrinkSize = 1;
        if (args.length > 2) {
            try {
                shrinkSize = Integer.parseInt(args[2]);
                if (shrinkSize <= 0 || shrinkSize > 50) {
                    sendError(player, "§c缩小大小必须在 1 到 50 之间!");
                    return;
                }
            } catch (NumberFormatException e) {
                sendError(player, "§c缩小大小必须是整数!");
                return;
            }
        }

        // 执行缩小
        SectResidenceManager.ShrinkResult result = residenceManager.shrinkLand(sect, shrinkSize);

        if (result.isSuccess()) {
            // 保存宗门数据
            sectSystem.saveSect(sect);

            // 成功消息
            sendSuccess(player, "§a§l========== 领地缩小成功 ==========");
            sendSuccess(player, "§a缩小大小: §f" + shrinkSize + " 块");
            sendSuccess(player, "§a返还灵石: §6" + result.getRefund());
            sendSuccess(player, "§a宗门当前资金: §6" + sect.getSectFunds());
            sendSuccess(player, "§a§l============================");

            // 广播消息
            sect.broadcastMessage("§a§e【宗门领地】");
            sect.broadcastMessage("§a宗门领地已成功缩小！");
            sect.broadcastMessage("§a缩小大小: §f" + shrinkSize + " 块");
            sect.broadcastMessage("§a返还灵石: §6" + result.getRefund());
        } else {
            sendError(player, "§c领地缩小失败!");
            sendError(player, "§c原因: " + result.getErrorMessage());
        }
    }

    /**
     * 处理删除领地命令
     * /sect land delete [confirm]
     */
    private void handleDelete(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.land.delete")) {
            sendError(player, "§c你没有权限删除领地!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        // 检查权限：只有宗主可以删除
        if (!sect.isOwner(player.getUniqueId())) {
            sendError(player, "§c只有宗主才能删除领地!");
            return;
        }

        // 检查宗门是否有领地
        if (!sect.hasLand()) {
            sendError(player, "§c宗门还没有领地!");
            return;
        }

        // 检查确认参数
        boolean confirmed = args.length > 2 && (args[2].equalsIgnoreCase("confirm") || args[2].equalsIgnoreCase("确认"));

        if (!confirmed) {
            sendError(player, "§c§l警告: 删除领地是不可逆的操作!");
            sendInfo(player, "§e这将永久删除宗门的领地，包括：");
            sendInfo(player, "§e  • 领地范围内的所有权限设置");
            sendInfo(player, "§e  • 所有建筑位数据");
            sendInfo(player, "§e  • 维护费记录");
            sendInfo(player, "");
            sendInfo(player, "§c确认删除请输入: §6/sect land delete confirm");
            return;
        }

        // 执行删除
        SectResidenceManager.DeleteResult result = residenceManager.deleteLand(sect);

        if (result.isSuccess()) {
            // 清除所有成员的权限
            sectSystem.getPermissionManager().clearSectPermissions(sect);

            // 保存宗门数据
            sectSystem.saveSect(sect);

            // 成功消息
            sendSuccess(player, "§a§l========== 领地删除成功 ==========");
            sendSuccess(player, "§a领地ID: §f" + result.getResidenceId());
            if (result.getRefund() > 0) {
                sendSuccess(player, "§a返还灵石: §6" + result.getRefund());
            }
            sendSuccess(player, "§a宗门当前资金: §6" + sect.getSectFunds());
            sendSuccess(player, "§a§l============================");

            // 广播消息
            sect.broadcastMessage("§c§e【宗门领地】");
            sect.broadcastMessage("§c§l宗门领地已被删除！");
            sect.broadcastMessage("§c所有领地权限和建筑位已清除");
        } else {
            sendError(player, "§c领地删除失败!");
            sendError(player, "§c原因: " + result.getErrorMessage());
        }
    }

    /**
     * 查看领地信息
     */
    private void handleInfo(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.use")) {
            sendError(player, "§c你没有权限查看领地信息!");
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

        sendSuccess(player, "§a§l========== 宗门领地信息 ==========");
        sendSuccess(player, "§a领地ID: §f" + sect.getResidenceLandId());

        Location center = sect.getLandCenter();
        if (center != null) {
            sendSuccess(player, "§a领地中心: §f" + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ());
            sendSuccess(player, "§a所在世界: §f" + center.getWorld().getName());
        }

        sendSuccess(player, "§a最后缴费: §f" + formatTime(sect.getLastMaintenanceTime()));
        sendSuccess(player, "§a建筑位: §f" + sect.getBuildingSlots().size() + " 种类型已使用");
        sendSuccess(player, "§a§l===========================");
    }

    /**
     * 查看维护费状态
     */
    private void handleStatus(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.use")) {
            sendError(player, "§c你没有权限查看领地状态!");
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
            sendInfo(player, "§7宗门还没有领地");
            return;
        }

        // 获取维护费状态
        SectResidenceManager.MaintenanceStatus status = residenceManager.getMaintenanceStatus(sect);

        sendSuccess(player, "§a§l========== 维护费状态 ==========");
        sendSuccess(player, "§a状态: §f" + status.getDescription());

        long daysSinceMaintenance = (System.currentTimeMillis() - sect.getLastMaintenanceTime()) / (1000 * 60 * 60 * 24);
        sendSuccess(player, "§a距上次缴费: §f" + daysSinceMaintenance + " 天");

        if (sect.hasLand()) {
            // TODO: 获取领地大小计算维护费
            // int landSize = residenceManager.getResidenceLandSize(sect.getResidenceLandId());
            // long maintenanceCost = residenceManager.calculateMaintenanceCost(landSize);
            // sendSuccess(player, "§a下次维护费: §6" + maintenanceCost + " 灵石");
        }

        sendSuccess(player, "§a§l==========================");
    }

    /**
     * 缴纳维护费
     */
    private void handlePay(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.use")) {
            sendError(player, "§c你没有权限缴纳维护费!");
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

        // 获取领地大小（暂时用1作为占位符）
        int landSize = 1; // TODO: 从 SectResidenceManager 获取实际领地大小

        // 计算维护费
        long maintenanceCost = residenceManager.calculateMaintenanceCost(landSize);

        // 检查宗门资金
        if (sect.getSectFunds() < maintenanceCost) {
            long shortage = maintenanceCost - sect.getSectFunds();
            sendError(player, "§c宗门资金不足！");
            sendError(player, "§c需要: §6" + maintenanceCost + " §c当前: §6" + sect.getSectFunds());
            sendError(player, "§c缺少: §6" + shortage + " §c灵石");
            return;
        }

        // 从SectSystem获取维护费调度器进行支付处理
        if (sectSystem.getMaintenanceFeeScheduler().processManualPayment(sect, landSize)) {
            sendSuccess(player, "§a§l========== 维护费缴纳成功 ==========");
            sendSuccess(player, "§a缴纳费用: §6" + maintenanceCost + " 灵石");
            sendSuccess(player, "§a宗门剩余资金: §6" + sect.getSectFunds());
            sendSuccess(player, "§a下次缴费时间: §f" + sectSystem.getMaintenanceFeeScheduler().getNextMaintenanceTime());
            sendSuccess(player, "§a§l============================");

            // 广播消息
            sect.broadcastMessage("§a§e【宗门领地】");
            sect.broadcastMessage("§a宗门维护费已成功缴纳!");
            sect.broadcastMessage("§a缴纳金额: §6" + maintenanceCost + " 灵石");
        } else {
            sendError(player, "§c维护费缴纳失败!");
        }
    }

    /**
     * 查看成员权限
     */
    private void handleMembers(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.use")) {
            sendError(player, "§c你没有权限查看成员!");
            return;
        }

        sendInfo(player, "§7成员权限查看功能即将实现...");
    }

    /**
     * 转让领地
     */
    private void handleTransfer(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.land.delete")) {
            sendError(player, "§c你没有权限转让领地!");
            return;
        }

        // 获取玩家所在宗门
        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c你还没有加入任何宗门!");
            return;
        }

        // 检查权限：只有宗主可以转让
        if (!sect.isOwner(player.getUniqueId())) {
            sendError(player, "§c只有宗主才能转让领地!");
            return;
        }

        // 检查宗门是否有领地
        if (!sect.hasLand()) {
            sendError(player, "§c宗门还没有领地!");
            return;
        }

        // 检查参数
        if (args.length < 3) {
            sendError(player, "§c请指定要转让给的玩家!");
            sendInfo(player, "§7用法: /sect land transfer <玩家名>");
            return;
        }

        String targetName = args[2];
        org.bukkit.entity.Player targetPlayer = plugin.getServer().getPlayer(targetName);

        if (targetPlayer == null) {
            sendError(player, "§c玩家 " + targetName + " 不在线!");
            return;
        }

        // 检查目标是否是宗门成员
        if (!sect.isMember(targetPlayer.getUniqueId())) {
            sendError(player, "§c玩家 " + targetName + " 不是宗门成员!");
            return;
        }

        // 不能转让给自己
        if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
            sendInfo(player, "§e不能转让给自己!");
            return;
        }

        // 执行转让
        SectResidenceManager.TransferResult result = residenceManager.transferLand(sect, targetPlayer.getUniqueId());

        if (result.isSuccess()) {
            // 保存宗门数据
            sectSystem.saveSect(sect);

            // 成功消息
            sendSuccess(player, "§a§l========== 领地转让成功 ==========");
            sendSuccess(player, "§a新所有者: §f" + targetName);
            sendSuccess(player, "§a领地ID: §f" + sect.getResidenceLandId());
            sendSuccess(player, "§a§l============================");

            // 通知新所有者
            targetPlayer.sendMessage("§a§l========== 领地权限更新 ==========");
            targetPlayer.sendMessage("§a你已获得宗门领地的所有权!");
            targetPlayer.sendMessage("§a领地ID: §f" + sect.getResidenceLandId());
            targetPlayer.sendMessage("§a§l============================");

            // 广播消息
            sect.broadcastMessage("§a§e【宗门领地】");
            sect.broadcastMessage("§a领地所有权已转让给: §f" + targetName);
        } else {
            sendError(player, "§c领地转让失败!");
            sendError(player, "§c原因: " + result.getErrorMessage());
        }
    }

    /**
     * 打开领地管理 GUI（整合版）
     * 包含：领地信息、传送、维护费、成员管理、建筑位、权限配置等
     */
    private void handleGUI(Player player, String[] args) {
        // 权限检查
        if (!hasPermission(player, "xiancore.sect.use")) {
            sendError(player, "§c你没有权限打开领地GUI!");
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
            sendInfo(player, "§7请先使用 §e/sect land claim §7圈地");
            return;
        }

        // 打开整合版领地管理GUI
        try {
            sectSystem.getLandGUI().openMainGUI(player, sect);
            sendInfo(player, "§a已打开领地管理界面");
        } catch (Exception e) {
            sendError(player, "§c打开GUI失败: " + e.getMessage());
            plugin.getLogger().warning("领地GUI打开失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示领地帮助
     */
    private void showLandHelp(Player player) {
        player.sendMessage("§a§l========== 宗门领地帮助 ==========");
        player.sendMessage("§e/sect land claim [块数] §7- 圈地");
        player.sendMessage("§e/sect land expand [块数] §7- 扩展领地");
        player.sendMessage("§e/sect land shrink [块数] §7- 缩小领地");
        player.sendMessage("§e/sect land delete [confirm] §7- 删除领地");
        player.sendMessage("§e/sect land info §7- 查看领地信息");
        player.sendMessage("§e/sect land status §7- 查看维护费状态");
        player.sendMessage("§e/sect land pay §7- 缴纳维护费");
        player.sendMessage("§e/sect land members §7- 查看成员权限");
        player.sendMessage("§e/sect land gui §7- 打开领地管理界面");
        player.sendMessage("§a§l=============================");
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

    /**
     * 格式化时间戳
     */
    private String formatTime(long timestamp) {
        if (timestamp == 0) {
            return "未设置";
        }
        // TODO: 实现更好的时间格式化
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(timestamp));
    }
}
