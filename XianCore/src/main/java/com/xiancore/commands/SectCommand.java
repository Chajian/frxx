package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.SectGUI;
import com.xiancore.integration.residence.SectLandCommand;
import com.xiancore.integration.residence.SectResidenceManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 宗门命令处理器
 * 处理 /sect 命令
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectCommand extends BaseCommand {

    public SectCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // 如果没有参数，打开宗门GUI
        if (args.length == 0) {
            SectGUI.open(player, plugin);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
                SectGUI.open(player, plugin);
                break;

            case "create":
            case "创建":
                handleCreate(player, args);
                break;

            case "join":
            case "加入":
                handleJoin(player, args);
                break;

            case "leave":
            case "离开":
            case "退出":
                handleLeave(player);
                break;

            case "disband":
            case "解散":
                handleDisband(player);
                break;

            case "info":
            case "信息":
                showSectInfo(player);
                break;

            case "list":
            case "列表":
                handleList(player);
                break;

            case "invite":
            case "邀请":
                handleInvite(player, args);
                break;

            case "kick":
            case "踢出":
                handleKick(player, args);
                break;

            case "promote":
            case "晋升":
                handlePromote(player, args);
                break;

            case "demote":
            case "降职":
                handleDemote(player, args);
                break;

            case "checkin":
            case "签到":
                handleCheckIn(player);
                break;

            case "task":
            case "任务":
                handleTask(player);
                break;

            case "facility":
            case "facilities":
            case "设施":
                handleFacility(player);
                break;

            case "warehouse":
            case "仓库":
            case "storage":
                handleWarehouse(player);
                break;

            case "shop":
            case "商店":
            case "store":
                handleShop(player);
                break;

            case "donate":
            case "捐赠":
                handleDonate(player, args);
                break;

            case "land":
            case "领地":
                handleLand(player, args);
                break;

            case "permission":
            case "权限":
                handlePermission(player, args);
                break;

            case "teleport":
            case "传送":
            case "tp":
                handleTeleport(player);
                break;

            case "help":
            case "帮助":
                showHelp(sender);
                break;
            
            // ========== 管理员命令 ==========
            case "reload":
            case "重载":
                handleReload(player);
                break;
            
            case "validate":
            case "验证":
                handleValidate(player);
                break;
            
            case "refresh":
            case "刷新":
                handleRefresh(player, args);
                break;

            default:
                sendError(sender, "未知的子命令: " + subCommand);
                sendInfo(sender, "使用 /sect help 查看帮助");
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(Arrays.asList("create", "join", "leave", "disband", "info", "list", "invite", "kick", "promote", "demote", "checkin", "task", "facility", "warehouse", "shop", "donate", "permission", "help"));

            // 添加管理员命令（需要权限）
            if (sender.hasPermission("xiancore.sect.admin")) {
                commands.add("reload");
                commands.add("validate");
                commands.add("refresh");
            }

            return filterTabComplete(commands, args[0]);
        }
        return List.of();
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== 宗门系统命令帮助 ==========");
        sendInfo(sender, "§e/sect §7- 查看宗门信息");
        sendInfo(sender, "§e/sect create <名称> §7- 创建宗门");
        sendInfo(sender, "§e/sect join <名称> §7- 加入宗门");
        sendInfo(sender, "§e/sect leave §7- 离开当前宗门");
        sendInfo(sender, "§e/sect disband §7- 解散当前宗门（宗主专用）");
        sendInfo(sender, "§e/sect info §7- 查看宗门信息");
        sendInfo(sender, "§e/sect list §7- 查看所有宗门");
        sendInfo(sender, "§e/sect invite <玩家> §7- 邀请玩家加入");
        sendInfo(sender, "§e/sect kick <玩家> §7- 踢出宗门成员");
        sendInfo(sender, "§e/sect promote <玩家> §7- 晋升宗门成员");
        sendInfo(sender, "§e/sect demote <玩家> §7- 降职宗门成员");
        sendInfo(sender, "§e/sect checkin §7- 每日签到");
        sendInfo(sender, "§e/sect task §7- 查看宗门任务");
        sendInfo(sender, "§e/sect facility §7- 管理宗门设施");
        sendInfo(sender, "§e/sect warehouse §7- 打开宗门仓库");
        sendInfo(sender, "§e/sect shop §7- 打开宗门商店");
        sendInfo(sender, "§e/sect donate <数量> §7- 向宗门捐赠灵石");
        sendInfo(sender, "§e/sect permission §7- 管理宗门角色权限（宗主专用）");
        sendInfo(sender, "§e/sect help §7- 显示此帮助");

        // 管理员命令
        if (sender.hasPermission("xiancore.sect.admin")) {
            sendInfo(sender, "");
            sendInfo(sender, "§c========== 管理员命令 ==========");
            sendInfo(sender, "§c/sect reload §7- 重载任务配置");
            sendInfo(sender, "§c/sect validate §7- 验证任务配置");
            sendInfo(sender, "§c/sect refresh <type> §7- 手动刷新任务");
        }

        sendInfo(sender, "§b=====================================");
    }

    /**
     * 显示宗门信息
     */
    private void showSectInfo(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        if (data.getSectId() == null) {
            sendWarning(player, "你还没有加���任何宗门!");
            sendInfo(player, "使用 /sect list 查看所有宗门");
            sendInfo(player, "使用 /sect create <名称> 创建宗门");
            return;
        }

        // 从宗门系统获取宗门详细信息
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "宗门数据获取失败!");
            return;
        }

        // 显示详细宗门信息
        sendInfo(player, "§b========== 宗门信息 ==========");
        sendInfo(player, "§e宗门名称: §f" + sect.getName());
        sendInfo(player, "§e宗门等级: §f" + sect.getLevel());
        sendInfo(player, "§e宗主: §b" + sect.getOwnerName());
        sendInfo(player, "§e成员数量: §f" + sect.getMemberList().size() + "§7/§f" + sect.getMaxMembers());
        sendInfo(player, "§e创建时间: §f" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(sect.getCreatedAt())));
        sendInfo(player, "§e招募状态: §f" + (sect.isRecruiting() ? "§a开放" : "§c关闭"));
        sendInfo(player, "");
        sendInfo(player, "§e你的信息:");
        sendInfo(player, "§e  职位: " + com.xiancore.systems.sect.SectRank.getColoredDisplayName(data.getSectRank()));
        sendInfo(player, "§e  贡献值: §f" + data.getContributionPoints());
        sendInfo(player, "§e  创建时间: §f" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(data.getCreatedAt())));
        sendInfo(player, "§b=============================");
    }

    /**
     * 处理创建宗门
     */
    private void handleCreate(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.sect.create")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定宗门名称!");
            sendInfo(player, "用法: /sect create <名称>");
            return;
        }

        String sectName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // 检查玩家是否已在宗门
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data.getSectId() != null) {
            sendError(player, "你已经加入了宗门! 请先离开当前宗门。");
            return;
        }

        // 检查宗门名称是否已存在
        if (plugin.getSectSystem().getSectByName(sectName) != null) {
            sendError(player, "宗门 '" + sectName + "' 已存在，请使用其他名称!");
            return;
        }

        // 创建宗门（所有检查都在 SectSystem 中进行）
        if (plugin.getSectSystem().createSect(player, sectName)) {
            sendSuccess(player, "§a宗门创建成功！");
            sendInfo(player, "§e宗门名称: §f" + sectName);
        } else {
            sendError(player, "§c宗门创建失败，请稍后重试!");
        }
    }

    /**
     * 处理加入宗门
     */
    private void handleJoin(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要加入的宗门名称!");
            sendInfo(player, "用法: /sect join <名称>");
            return;
        }

        String sectName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // 检查玩家是否已在宗门
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data.getSectId() != null) {
            sendError(player, "你已经加入了宗门! 请先离开当前宗门。");
            return;
        }

        // 检查宗门是否存在
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getSectByName(sectName);
        if (sect == null) {
            sendError(player, "宗门 '" + sectName + "' 不存在!");
            sendInfo(player, "使用 /sect list 查看所有宗门");
            return;
        }

        // 检查宗门是否开放招募
        if (!sect.isRecruiting()) {
            sendError(player, "宗门 '" + sectName + "' 暂不招募成员!");
            return;
        }

        // 检查宗门是否有空位
        if (sect.getMemberList().size() >= sect.getMaxMembers()) {
            sendError(player, "宗门 '" + sectName + "' 人数已满!");
            return;
        }

        // 加入宗门
        if (plugin.getSectSystem().joinSect(player, sectName)) {
            sendSuccess(player, "§a成功加入宗门: §f" + sectName);
            sendInfo(player, "§e现在你是宗门的一员了！");

            // 通知宗门成员
            for (com.xiancore.systems.sect.SectMember member : sect.getMemberList()) {
                org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(member.getPlayerId());
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage("§e" + player.getName() + " 加入了宗门!");
                }
            }
        } else {
            sendError(player, "§c加入宗门失败，请稍后重试!");
        }
    }

    /**
     * 处理离开宗门
     */
    private void handleLeave(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        if (data.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            return;
        }

        // 获取宗门对象
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "宗门数据获取失败!");
            return;
        }

        // 检查是否是宗主（宗主不能直接离开，需要解散宗门或转移宗主权）
        com.xiancore.systems.sect.SectRank sectRank = com.xiancore.systems.sect.SectRank.fromRankString(data.getSectRank());
        if (sectRank == com.xiancore.systems.sect.SectRank.LEADER) {
            sendError(player, "§c宗主不能离开宗门!");
            sendInfo(player, "§e你可以选择:");
            sendInfo(player, "§e1. 使用 /sect disband 解散宗门");
            sendInfo(player, "§e2. 将宗主权转让给其他人（功能待实现）");
            return;
        }

        // 离开宗门
        if (plugin.getSectSystem().leaveSect(player)) {
            String sectName = sect.getName();
            sendSuccess(player, "§a你已离开宗门: §f" + sectName);
            sendInfo(player, "§e失去了所有宗门贡献和职位!");

            // 通知宗门成员
            for (com.xiancore.systems.sect.SectMember member : sect.getMemberList()) {
                org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(member.getPlayerId());
                if (onlinePlayer != null && !onlinePlayer.getUniqueId().equals(player.getUniqueId())) {
                    onlinePlayer.sendMessage("§e" + player.getName() + " 离开了宗门!");
                }
            }
        } else {
            sendError(player, "§c离开宗门失败，请稍后重试!");
        }
    }

    /**
     * 处理宗门解散
     */
    private void handleDisband(Player player) {
        if (!hasPermission(player, "xiancore.sect.admin")) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        if (data.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            return;
        }

        // 获取宗门对象
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "宗门数据获取失败!");
            return;
        }

        // 检查是否是宗主
        com.xiancore.systems.sect.SectRank sectRank = com.xiancore.systems.sect.SectRank.fromRankString(data.getSectRank());
        if (sectRank != com.xiancore.systems.sect.SectRank.LEADER) {
            sendError(player, "§c只有宗主才能解散宗门!");
            sendInfo(player, "§e当前宗主: §f" + sect.getOwnerName());
            return;
        }

        // 获取宗门所有成员
        List<com.xiancore.systems.sect.SectMember> members = new ArrayList<>(sect.getMemberList());

        // 解散宗门
        if (plugin.getSectSystem().disbandSect(player)) {
            String sectName = sect.getName();
            sendSuccess(player, "§a宗门已解散!");

            // 通知所有在线成员
            for (com.xiancore.systems.sect.SectMember member : members) {
                org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(member.getPlayerId());
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage("§c§l宗门已解散!");
                    onlinePlayer.sendMessage("§e宗门 §f" + sectName + " §e已被宗主解散");
                }
            }

            // 全服广播
            plugin.getServer().broadcastMessage("§c宗门 §f" + sectName + " §c已被解散!");
        } else {
            sendError(player, "§c解散宗门失败，请稍后重试!");
        }
    }

    /**
     * 处理宗门列表
     */
    private void handleList(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 获取所有宗门
        List<com.xiancore.systems.sect.Sect> sects = plugin.getSectSystem().getAllSects();

        if (sects.isEmpty()) {
            sendWarning(player, "当前服务器还没有宗门!");
            sendInfo(player, "使用 /sect create <名称> 创建一个新宗门");
            return;
        }

        sendInfo(player, "§b========== 宗门列表 (共 " + sects.size() + " 个) ==========");

        for (com.xiancore.systems.sect.Sect sect : sects) {
            String recruitStatus = sect.isRecruiting() ? "§a[开放招募]" : "§c[不招募]";
            sendInfo(player, String.format("§e%-15s %s §7等级: §f%d §7成员: §f%d/%d §7宗主: §b%s",
                    sect.getName(),
                    recruitStatus,
                    sect.getLevel(),
                    sect.getMemberList().size(),
                    sect.getMaxMembers(),
                    sect.getOwnerName()
            ));
        }

        sendInfo(player, "§b使用 /sect join <宗门名> 加入宗门");
        sendInfo(player, "§b========================================");
    }

    /**
     * 处理邀请玩家
     */
    private void handleInvite(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.sect.manage")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要邀请的玩家!");
            sendInfo(player, "用法: /sect invite <玩家>");
            return;
        }

        String targetName = args[1];
        Player targetPlayer = plugin.getServer().getPlayer(targetName);

        // 检查目标玩家是否在线
        if (targetPlayer == null) {
            sendError(player, "玩家 " + targetName + " 不在线!");
            return;
        }

        // 检查邀请者是否在宗门
        PlayerData inviterData = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (inviterData.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            return;
        }

        // 检查邀请者的权限
        com.xiancore.systems.sect.SectRank sectRank = com.xiancore.systems.sect.SectRank.fromRankString(inviterData.getSectRank());
        if (sectRank == null || !sectRank.canInvite()) {
            sendError(player, "你没有权限邀请玩家加入宗门!");
            sendInfo(player, "§7需要核心弟子及以上职位");
            return;
        }

        // 检查目标是否已在宗门
        PlayerData targetData = plugin.getDataManager().loadPlayerData(targetPlayer.getUniqueId());
        if (targetData.getSectId() != null) {
            sendError(player, "玩家 " + targetName + " 已经加入了宗门!");
            return;
        }

        // 获取宗门对象
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "宗门数据获取失败!");
            return;
        }

        // 检查宗门是否有空位
        if (sect.getMemberList().size() >= sect.getMaxMembers()) {
            sendError(player, "宗门人数已满!");
            return;
        }

        // 发送邀请
        if (plugin.getSectSystem().invitePlayer(player, targetPlayer)) {
            sendSuccess(player, "§a已向 " + targetName + " 发送邀请！");
            targetPlayer.sendMessage("§e" + player.getName() + " 邀请你加入宗门: " + sect.getName());
            targetPlayer.sendMessage("§7使用 /sect join " + sect.getName() + " 加入");
        } else {
            sendError(player, "邀请失败，请稍后重试！");
        }
    }

    /**
     * 处理踢出成员
     */
    private void handleKick(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.sect.manage")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要踢出的玩家!");
            sendInfo(player, "用法: /sect kick <玩家>");
            return;
        }

        String targetName = args[1];

        // 检查执行者是否在宗门
        PlayerData executorData = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (executorData.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            return;
        }

        // 检查执行者的权限
        com.xiancore.systems.sect.SectRank sectRank = com.xiancore.systems.sect.SectRank.fromRankString(executorData.getSectRank());
        if (sectRank == null || !sectRank.canKick()) {
            sendError(player, "你没有权限踢出宗门成员!");
            sendInfo(player, "§7需要长老及以上职位");
            return;
        }

        // 尝试找到目标玩家的数据
        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        PlayerData targetData = null;

        if (targetPlayer != null) {
            targetData = plugin.getDataManager().loadPlayerData(targetPlayer.getUniqueId());
        } else {
            // 玩家不在线，但仍然可以踢出
            sendWarning(player, "§e目标玩家不在线，将尝试踢出...");
        }

        // 检查目标是否在宗门
        if (targetData != null && targetData.getSectId() == null) {
            sendError(player, "玩家 " + targetName + " 不在宗门中!");
            return;
        }

        // 获取宗门对象
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "宗门数据获取失败!");
            return;
        }

        // 执行踢出操作
        if (plugin.getSectSystem().kickMember(player, targetName)) {
            sendSuccess(player, "§a已将 " + targetName + " 踢出宗门！");

            // 如果目标玩家在线，发送消息
            if (targetPlayer != null) {
                targetPlayer.sendMessage("§c你已被宗门踢出: " + sect.getName());
            }
        } else {
            sendError(player, "踢出失败，请检查玩家是否在宗门中！");
        }
    }

    /**
     * 处理晋升命令 - 优化版本（包含详细错误诊断）
     * 命令: /sect promote <玩家名>
     */
    private void handlePromote(Player player, String[] args) {
        // ==================== 权限检查 ====================
        if (!hasPermission(player, "xiancore.sect.manage")) {
            sendError(player, "§c你没有管理宗门的权限！");
            return;
        }

        // ==================== 参数检查 ====================
        if (args.length < 2) {
            sendError(player, "§c请指定要晋升的玩家！");
            sendInfo(player, "§7用法: §e/sect promote <玩家>");
            return;
        }

        String targetName = args[1];

        // ==================== 执行者数据检查 ====================
        PlayerData executorData = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (executorData == null || executorData.getSectId() == null) {
            sendError(player, "§c你还没有加入任何宗门！");
            return;
        }

        // ==================== 宗门对象检查 ====================
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem()
                .getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c宗门数据获取失败，请稍后重试！");
            plugin.getLogger().warning("获取宗门失败: " + executorData.getSectId());
            return;
        }

        // ==================== 执行者职位检查 ====================
        com.xiancore.systems.sect.SectRank executorRank =
                com.xiancore.systems.sect.SectRank.fromRankString(executorData.getSectRank());
        if (executorRank == null) {
            sendError(player, "§c无法获取你的职位信息！");
            plugin.getLogger().warning("无效的职位: " + executorData.getSectRank());
            return;
        }

        // ==================== 执行者权限检查 ====================
        // 只检查职位权限，Plugin权限已在第一步检查
        if (!executorRank.hasManagePermission()) {
            sendError(player, "§c你没有权限晋升成员！");
            sendInfo(player, "§7需要 §e长老§7及以上职位，你当前的职位是: "
                    + executorRank.getColoredName());
            return;
        }

        // ==================== 目标成员查找 ====================
        com.xiancore.systems.sect.SectMember targetMember = null;
        for (com.xiancore.systems.sect.SectMember m : sect.getMemberList()) {
            if (m.getPlayerName().equalsIgnoreCase(targetName)) {
                targetMember = m;
                break;
            }
        }

        if (targetMember == null) {
            sendError(player, "§c在宗门中找不到玩家 §e" + targetName + "§c！");
            sendInfo(player, "§7请确认玩家名称是否正确且已加入你的宗门。");
            return;
        }

        java.util.UUID targetId = targetMember.getPlayerId();
        // 保存晋升前的职位，避免使用晋升后的职位
        com.xiancore.systems.sect.SectRank beforeRank = targetMember.getRank();

        // ==================== 目标资格检查 ====================

        // 检查1: 不能晋升宗主
        if (sect.isOwner(targetId)) {
            sendError(player, "§c不能晋升宗主！");
            sendInfo(player, "§7宗主已经是最高职位，无法再晋升");
            return;
        }

        // 检查2: 执行者职位必须高于目标（防止越权晋升）
        if (executorRank.getLevel() <= beforeRank.getLevel()) {
            sendError(player, "§c你的职位不足以晋升 §e" + targetName + "§c！");
            sendInfo(player, "§7你的职位: " + executorRank.getColoredName()
                    + " §7≤ 目标职位: " + beforeRank.getColoredName());
            sendInfo(player, "§7你必须拥有高于目标职位的权限才能晋升");
            return;
        }

        // ==================== 执行晋升操作 ====================
        com.xiancore.systems.sect.Sect.PromotionResult result =
                plugin.getSectSystem().getSyncManager().promoteAndSync(sect, targetId);

        if (!result.isSuccess()) {
            // 晋升失败，返回详细的诊断信息
            sendError(player, "§c晋升失败: " + result.getShortMessage());
            sendInfo(player, "§7原因: " + result.getDetailedMessage());

            String suggestion = getPromotionFailureSuggestion(result.getShortMessage());
            if (suggestion != null) {
                sendInfo(player, "§7建议: " + suggestion);
            }

            // 详细日志记录
            plugin.getLogger().warning("晋升失败 - " + result.getShortMessage() +
                    ": " + result.getDetailedMessage());
            return;
        }

        // ==================== 晋升成功处理 ====================
        // 获取晋升后的职位
        com.xiancore.systems.sect.SectRank afterRank = targetMember.getRank();

        sendSuccess(player, "§a晋升成功: " + targetName);
        sendInfo(player, "§7职位变更: " + beforeRank.getDisplayName() + " → " + afterRank.getDisplayName());

        // 通知被晋升的玩家（如果在线）
        Player targetPlayer = plugin.getServer().getPlayer(targetId);
        if (targetPlayer != null) {
            targetPlayer.sendMessage("§a恭喜！你已被晋升为 " + afterRank.getColoredName());
        } else {
            // 即使玩家不在线，也要确保数据被保存
            // 这里可能需要触发数据保存逻辑（取决于框架实现）
            plugin.getLogger().info("目标玩家不在线，确保职位变更已保存: " + targetName);
        }

        // 通知整个宗门（排除执行者和目标玩家本人）
        for (com.xiancore.systems.sect.SectMember member : sect.getMemberList()) {
            Player memberPlayer = plugin.getServer().getPlayer(member.getPlayerId());
            if (memberPlayer != null && !memberPlayer.getUniqueId().equals(player.getUniqueId())
                    && !memberPlayer.getUniqueId().equals(targetId)) {
                memberPlayer.sendMessage("§b[宗门公告] " + targetName + " 已被晋升为 " + afterRank.getColoredName());
            }
        }

        // 记录详细日志
        plugin.getLogger().info("晋升成功 - 执行者: " + player.getName() +
                ", 目标: " + targetName + ", 职位变更: " + beforeRank.name() + " → " + afterRank.name());
    }

    /**
     * 处理降职命令
     * 命令: /sect demote <玩家名>
     */
    private void handleDemote(Player player, String[] args) {
        // ==================== 权限检查 ====================
        if (!hasPermission(player, "xiancore.sect.manage")) {
            sendError(player, "§c你没有管理宗门的权限！");
            return;
        }

        // ==================== 参数检查 ====================
        if (args.length < 2) {
            sendError(player, "§c请指定要降职的玩家！");
            sendInfo(player, "§7用法: §e/sect demote <玩家>");
            return;
        }

        String targetName = args[1];

        // ==================== 执行者数据检查 ====================
        PlayerData executorData = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (executorData == null || executorData.getSectId() == null) {
            sendError(player, "§c你还没有加入任何宗门！");
            return;
        }

        // ==================== 宗门对象检查 ====================
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem()
                .getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "§c宗门数据获取失败，请稍后重试！");
            return;
        }

        // ==================== 执行者职位检查 ====================
        com.xiancore.systems.sect.SectRank executorRank =
                com.xiancore.systems.sect.SectRank.fromRankString(executorData.getSectRank());
        if (executorRank == null || !executorRank.hasManagePermission()) {
            sendError(player, "§c你没有权限降职成员！");
            sendInfo(player, "§7需要 §e长老§7及以上职位");
            return;
        }

        // ==================== 目标成员查找 ====================
        com.xiancore.systems.sect.SectMember targetMember = null;
        for (com.xiancore.systems.sect.SectMember m : sect.getMemberList()) {
            if (m.getPlayerName().equalsIgnoreCase(targetName)) {
                targetMember = m;
                break;
            }
        }

        if (targetMember == null) {
            sendError(player, "§c在宗门中找不到玩家 §e" + targetName + "§c！");
            return;
        }

        java.util.UUID targetId = targetMember.getPlayerId();
        com.xiancore.systems.sect.SectRank targetRank = targetMember.getRank();

        // ==================== 目标资格检查 ====================
        // 不能降职宗主
        if (sect.isOwner(targetId)) {
            sendError(player, "§c不能降职宗主！");
            sendInfo(player, "§7宗主是宗门的最高职位");
            return;
        }

        // 不能降职比你职位更高或相同的成员
        if (executorRank.getLevel() <= targetRank.getLevel()) {
            sendError(player, "§c你的职位不足以降职 §e" + targetName + "§c！");
            sendInfo(player, "§7你必须拥有高于目标职位的权限才能降职");
            return;
        }

        // 检查是否已是最低职位
        if (targetRank == com.xiancore.systems.sect.SectRank.OUTER_DISCIPLE) {
            sendError(player, "§c" + targetName + " 已是最低职位，无法继续降职！");
            return;
        }

        // ==================== 执行降职操作 ====================
        if (plugin.getSectSystem().getSyncManager().demoteAndSync(sect, targetId)) {
            sendSuccess(player, "§a降职成功: " + targetName);
            sendInfo(player, "§7新职位: " + targetRank.getDisplayName() + " → " +
                    targetMember.getRank().getDisplayName());

            // 通知被降职的玩家（如果在线）
            Player targetPlayer = plugin.getServer().getPlayer(targetId);
            if (targetPlayer != null) {
                targetPlayer.sendMessage("§c你已被降职为 " + targetMember.getRank().getColoredName());
            }

            plugin.getLogger().info("降职成功 - 执行者: " + player.getName() +
                    ", 目标: " + targetName + ", 新职位: " + targetMember.getRank().name());
        } else {
            sendError(player, "§c降职失败，请稍后重试！");
            plugin.getLogger().warning("降职失败 - 目标: " + targetName +
                    ", 宗门: " + sect.getName());
        }
    }

    /**
     * 根据晋升失败原因提供建议
     */
    private String getPromotionFailureSuggestion(String failureReason) {
        switch (failureReason) {
            case "成员不存在":
                return "检查玩家是否还在宗门中，或尝试 /sect info 查看成员列表";
            case "已是最高职位":
                return "该成员已无法再晋升，若要提升其他权限，请考虑其他方式";
            case "职位无效":
                return "系统数据异常，请联系管理员进行检查";
            default:
                return null;
        }
    }

    /**
     * 处理每日签到
     */
    private void handleCheckIn(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 调用宗门活跃度管理器的签到方法
        if (plugin.getSectSystem().getActivityManager() != null) {
            plugin.getSectSystem().getActivityManager().checkIn(player);
        } else {
            sendError(player, "§c签到系统暂未初始化!");
        }
    }

    /**
     * 处理宗门任务
     */
    private void handleTask(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 检查玩家是否在宗门
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            sendInfo(player, "使用 /sect join <名称> 加入宗门");
            return;
        }

        // 打开任务GUI
        if (plugin.getSectSystem().getTaskGUI() != null) {
            plugin.getSectSystem().getTaskGUI().openGUI(player);
        } else {
            sendError(player, "§c任务系统暂未初始化!");
        }
    }

    /**
     * 处理设施管理
     */
    private void handleFacility(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 检查玩家是否在宗门
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            sendInfo(player, "使用 /sect join <名称> 加入宗门");
            return;
        }

        // 打开设施GUI
        if (plugin.getSectSystem().getFacilityGUI() != null) {
            plugin.getSectSystem().getFacilityGUI().open(player);
        } else {
            sendError(player, "§c设施系统暂未初始化!");
        }
    }

    /**
     * 处理仓库
     */
    private void handleWarehouse(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 检查玩家是否在宗门
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            sendInfo(player, "使用 /sect join <名称> 加入宗门");
            return;
        }

        // 打开仓库GUI
        if (plugin.getSectSystem().getWarehouseGUI() != null) {
            plugin.getSectSystem().getWarehouseGUI().open(player);
        } else {
            sendError(player, "§c仓库系统暂未初始化!");
        }
    }

    /**
     * 处理商店
     */
    private void handleShop(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 检查玩家是否在宗门
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            sendInfo(player, "使用 /sect join <名称> 加入宗门");
            return;
        }

        // 打开商店GUI
        if (plugin.getSectSystem().getShopGUI() != null) {
            plugin.getSectSystem().getShopGUI().open(player);
        } else {
            sendError(player, "§c商店系统暂未初始化!");
        }
    }

    /**
     * 处理捐赠灵石
     */
    private void handleDonate(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 检查玩家是否在宗门
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data.getSectId() == null) {
            sendError(player, "你还没有加入任何宗门!");
            sendInfo(player, "使用 /sect join <名称> 加入宗门");
            return;
        }

        // 检查参数
        if (args.length < 2) {
            sendError(player, "请指定捐赠数量!");
            sendInfo(player, "用法: /sect donate <数量>");
            sendInfo(player, "§7当前灵石: §f" + data.getSpiritStones());
            return;
        }

        // 解析数量
        long amount;
        try {
            amount = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            sendError(player, "无效的数量!");
            return;
        }

        // 检查数量有效性
        if (amount <= 0) {
            sendError(player, "捐赠数量必须大于 0!");
            return;
        }

        // 检查玩家是否有足够的灵石
        if (data.getSpiritStones() < amount) {
            sendError(player, "灵石不足!");
            sendInfo(player, "§7需要: §6" + amount + " §7当前: §f" + data.getSpiritStones());
            return;
        }

        // 获取宗门对象
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "宗门数据获取失败!");
            return;
        }

        // 扣除玩家灵石
        data.setSpiritStones(data.getSpiritStones() - amount);

        // 增加宗门灵石
        sect.addFunds(amount);

        // 增加贡献值（按 1:1 比例）
        data.setContributionPoints(data.getContributionPoints() + (int) amount);

        // 保存数据
        plugin.getDataManager().savePlayerData(data);
        plugin.getSectSystem().saveSect(sect);

        // 成功消息
        sendSuccess(player, "§a§l========== 捐赠成功 ==========");
        sendSuccess(player, "§e捐赠数量: §6" + amount + " 灵石");
        sendSuccess(player, "§e获得贡献: §b+" + amount);
        sendSuccess(player, "§e当前贡献: §f" + data.getContributionPoints());
        sendSuccess(player, "§e剩余灵石: §f" + data.getSpiritStones());
        sendSuccess(player, "§e宗门资金: §6" + sect.getSectFunds() + " 灵石");
        sendSuccess(player, "§a§l===========================");

        // 全宗门公告
        String announcement = "§e§l[宗门公告] §r" + player.getName() +
                              " §e向宗门捐赠了 §6" + amount + " §e灵石!";
        sect.broadcastMessage(announcement);

        plugin.getLogger().info(player.getName() + " 向宗门 " + sect.getName() + " 捐赠了 " + amount + " 灵石");
    }
    
    /**
     * 处理重载任务配置
     */
    private void handleReload(Player player) {
        if (!hasPermission(player, "xiancore.sect.admin")) {
            return;
        }
        
        player.sendMessage("§e正在重载宗门任务配置...");
        
        try {
            // 重载任务配置
            plugin.getSectSystem().getTaskManager().reloadTaskConfig();
            
            player.sendMessage("§a§l========== 配置重载成功 ==========");
            player.sendMessage("§a任务配置已重载！");
            player.sendMessage("§7- 新生成的任务将使用新配置");
            player.sendMessage("§7- 玩家已接取的任务不受影响");
            player.sendMessage("§a================================");
            
            plugin.getLogger().info(player.getName() + " 重载了宗门任务配置");
            
        } catch (Exception e) {
            player.sendMessage("§c重载失败: " + e.getMessage());
            plugin.getLogger().warning("任务配置重载失败:");
            e.printStackTrace();
        }
    }
    
    /**
     * 处理验证任务配置
     */
    private void handleValidate(Player player) {
        if (!hasPermission(player, "xiancore.sect.admin")) {
            return;
        }
        
        try {
            plugin.getSectSystem().getTaskManager().validateTaskConfig(player);
        } catch (Exception e) {
            player.sendMessage("§c验证失败: " + e.getMessage());
            plugin.getLogger().warning("任务配置验证失败:");
            e.printStackTrace();
        }
    }
    
    /**
     * 处理手动刷新任务
     */
    private void handleRefresh(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.sect.admin")) {
            return;
        }
        
        if (args.length < 2) {
            showRefreshHelp(player);
            return;
        }
        
        String subCmd = args[1].toLowerCase();
        
        try {
            switch (subCmd) {
                case "daily", "日常" -> {
                    player.sendMessage("§e正在刷新日常任务...");
                    int count = plugin.getSectSystem().getTaskScheduler().manualRefresh(com.xiancore.systems.sect.task.SectTaskType.DAILY);
                    player.sendMessage("§a日常任务刷新完成! 共刷新 " + count + " 个玩家");
                }
                case "weekly", "周常" -> {
                    player.sendMessage("§e正在刷新周常任务...");
                    int count = plugin.getSectSystem().getTaskScheduler().manualRefresh(com.xiancore.systems.sect.task.SectTaskType.WEEKLY);
                    player.sendMessage("§a周常任务刷新完成! 共刷新 " + count + " 个玩家");
                }
                case "all", "全部" -> {
                    player.sendMessage("§e正在刷新所有任务...");
                    int count1 = plugin.getSectSystem().getTaskScheduler().manualRefresh(com.xiancore.systems.sect.task.SectTaskType.DAILY);
                    int count2 = plugin.getSectSystem().getTaskScheduler().manualRefresh(com.xiancore.systems.sect.task.SectTaskType.WEEKLY);
                    player.sendMessage("§a所有任务刷新完成! 共刷新 " + (count1 + count2) + " 个玩家");
                }
                case "status", "状态" -> showRefreshStatus(player);
                case "history", "历史" -> showRefreshHistory(player);
                case "schedule", "计划" -> showRefreshSchedule(player);
                default -> showRefreshHelp(player);
            }
        } catch (Exception e) {
            player.sendMessage("§c刷新失败: " + e.getMessage());
            plugin.getLogger().warning("手动刷新任务失败:");
            e.printStackTrace();
        }
    }
    
    /**
     * 显示刷新帮助
     */
    private void showRefreshHelp(Player player) {
        player.sendMessage("§b========== 任务刷新命令 ==========");
        player.sendMessage("§e/sect refresh daily §7- 刷新日常任务");
        player.sendMessage("§e/sect refresh weekly §7- 刷新周常任务");
        player.sendMessage("§e/sect refresh all §7- 刷新所有任务");
        player.sendMessage("§e/sect refresh status §7- 查看刷新状态");
        player.sendMessage("§e/sect refresh history §7- 查看刷新历史");
        player.sendMessage("§e/sect refresh schedule §7- 查看刷新计划");
        player.sendMessage("§b================================");
    }
    
    /**
     * 显示刷新状态
     */
    private void showRefreshStatus(Player player) {
        com.xiancore.systems.sect.task.TaskRefreshScheduler scheduler = plugin.getSectSystem().getTaskScheduler();
        
        player.sendMessage("§b========== 任务刷新状态 ==========");
        player.sendMessage("§e自动刷新: " + (scheduler.isEnabled() ? "§a启用" : "§c禁用"));
        player.sendMessage("§e下次日常刷新: §f" + scheduler.getNextDailyRefreshTime());
        player.sendMessage("§e下次周常刷新: §f" + scheduler.getNextWeeklyRefreshTime());
        player.sendMessage("§e最后刷新:");
        player.sendMessage("§7  日常: §f" + scheduler.getLastDailyRefresh());
        player.sendMessage("§7  周常: §f" + scheduler.getLastWeeklyRefresh());
        player.sendMessage("§b================================");
    }
    
    /**
     * 显示刷新历史
     */
    private void showRefreshHistory(Player player) {
        com.xiancore.systems.sect.task.RefreshRecord record = plugin.getSectSystem().getTaskScheduler().getRecord();
        
        player.sendMessage("§b========== 刷新历史 ==========");
        player.sendMessage("§e日常刷新次数: §f" + record.getDailyRefreshCount());
        player.sendMessage("§e周常刷新次数: §f" + record.getWeeklyRefreshCount());
        player.sendMessage("§e总刷新玩家: §f" + record.getTotalPlayersRefreshed());
        
        if (!record.getErrors().isEmpty()) {
            player.sendMessage("§c最近错误 (" + record.getErrors().size() + " 个):");
            record.getRecentErrors().forEach(err -> 
                player.sendMessage("§7  - " + err.getErrorMessage())
            );
        } else {
            player.sendMessage("§a无刷新错误记录");
        }
        
        player.sendMessage("§b===========================");
    }
    
    /**
     * 显示刷新计划
     */
    private void showRefreshSchedule(Player player) {
        com.xiancore.systems.sect.task.TaskRefreshScheduler scheduler = plugin.getSectSystem().getTaskScheduler();
        
        player.sendMessage("§b========== 刷新计划 ==========");
        player.sendMessage("§e日常任务:");
        player.sendMessage("§7  刷新时间: §f每天 " + scheduler.getDailyRefreshTime());
        player.sendMessage("§7  时区: §f" + scheduler.getTimezone().getId());
        player.sendMessage("§7  下次刷新: §f" + scheduler.getNextDailyRefreshTime());
        
        player.sendMessage("");
        player.sendMessage("§e周常任务:");
        player.sendMessage("§7  刷新时间: §f每" + com.xiancore.systems.sect.task.TimeParser.formatDayOfWeek(scheduler.getWeeklyRefreshDay()) + 
                " " + scheduler.getWeeklyRefreshTime());
        player.sendMessage("§7  时区: §f" + scheduler.getTimezone().getId());
        player.sendMessage("§7  下次刷新: §f" + scheduler.getNextWeeklyRefreshTime());
        
        player.sendMessage("§b===========================");
    }

    /**
     * 处理领地命令
     */
    private void handleLand(Player player, String[] args) {
        // 创建领地命令处理器
        Map<String, Object> residenceConfig = new java.util.HashMap<>();
        Map<String, Object> messagesConfig = new java.util.HashMap<>();

        // 从配置文件加载（如果存在）
        try {
            if (plugin.getConfig().getConfigurationSection("residence") != null) {
                residenceConfig.putAll(plugin.getConfig().getConfigurationSection("residence").getValues(true));
            }
            if (plugin.getConfig().getConfigurationSection("messages") != null) {
                messagesConfig.putAll(plugin.getConfig().getConfigurationSection("messages").getValues(true));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load land configuration: " + e.getMessage());
        }

        SectResidenceManager residenceManager = new SectResidenceManager(residenceConfig, messagesConfig);

        SectLandCommand landCommand = new SectLandCommand(
            plugin,
            plugin.getSectSystem(),
            residenceManager
        );

        // 委托给领地命令处理器
        landCommand.handle(player, args);
    }

    /**
     * 处理权限管理命令
     */
    private void handlePermission(Player player, String[] args) {
        // 检查玩家是否有宗门
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "你还没有加入任何宗门!");
            return;
        }

        // 检查玩家是否是宗主
        if (!sect.getOwnerId().equals(player.getUniqueId())) {
            sendError(player, "只有宗主才能管理权限!");
            return;
        }

        // 检查宗门是否有绑定的领地
        if (!sect.hasLand()) {
            sendError(player, "宗门还没有绑定领地，无法管理权限!");
            sendInfo(player, "请先使用 /sect land bind 绑定领地");
            return;
        }

        // 解析子命令：gui(简化版) 或 complete(分页完整版，默认)
        String subCmd = args.length > 1 ? args[1].toLowerCase() : "complete";

        // 打开权限管理GUI
        try {
            com.xiancore.integration.residence.ResidencePermissionManager permissionManager =
                new com.xiancore.integration.residence.ResidencePermissionManager(plugin);

            if ("gui".equals(subCmd) || "simple".equals(subCmd)) {
                // 简化版：6个核心权限
                com.xiancore.gui.SectRolePermissionGUI permissionGUI =
                    new com.xiancore.gui.SectRolePermissionGUI(plugin, permissionManager);
                permissionGUI.openRolePermissionMain(player, sect);
                sendSuccess(player, "§a已打开权限管理界面（简化版）");
            } else {
                // 完整分页版：100+权限（默认）
                com.bekvon.bukkit.residence.Residence residence = 
                    com.bekvon.bukkit.residence.Residence.getInstance();
                com.bekvon.bukkit.residence.protection.ClaimedResidence claimed = 
                    residence.getResidenceManager().getByName(sect.getResidenceLandId());

                if (claimed == null) {
                    sendError(player, "领地信息获取失败!");
                    return;
                }

                com.xiancore.gui.SectRoleCompletePermissionGUIWithPagination completeGUI =
                    new com.xiancore.gui.SectRoleCompletePermissionGUIWithPagination(
                        plugin, sect, claimed, permissionManager);
                completeGUI.openMainMenu(player);
                sendSuccess(player, "§a已打开完整权限管理界面（分页版）");
            }
        } catch (Exception e) {
            sendError(player, "权限管理界面打开失败: " + e.getMessage());
            plugin.getLogger().warning("权限管理GUI打开失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理领地传送命令
     */
    private void handleTeleport(Player player) {
        if (!hasPermission(player, "xiancore.sect.use")) {
            return;
        }

        // 检查玩家是否有宗门
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());
        if (sect == null) {
            sendError(player, "你还没有加入任何宗门!");
            return;
        }

        // 检查宗门是否有绑定的领地
        if (!sect.hasLand()) {
            sendError(player, "宗门还没有绑定领地，无法传送!");
            sendInfo(player, "请联系宗主绑定领地: /sect land bind <ID>");
            return;
        }

        // 打开传送GUI
        try {
            com.xiancore.gui.SectTeleportGUI teleportGUI = new com.xiancore.gui.SectTeleportGUI(plugin);
            teleportGUI.openTeleportMenu(player, sect);
            sendSuccess(player, "§a已打开传送菜单");
        } catch (Exception e) {
            sendError(player, "传送菜单打开失败: " + e.getMessage());
            plugin.getLogger().warning("传送GUI打开失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
