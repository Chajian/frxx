package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 修复宗门数据命令
 * /xiancore fixsect [玩家|--all|--check]
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class FixSectCommand extends AbstractSubCommand {

    public FixSectCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "fixsect";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"修复宗门"};
    }

    @Override
    public String getPermission() {
        return "xiancore.admin.fixsect";
    }

    @Override
    public String getUsage() {
        return "/xiancore fixsect [玩家|--all|--check]";
    }

    @Override
    public String getDescription() {
        return "修复宗门数据不一致";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }

        // 检查是否为批量操作
        if (args.length >= 1 && args[0].equalsIgnoreCase("--all")) {
            handleFixSectAll(sender);
            return;
        }

        // 检查是否为检查模式（不修复，只检查）
        if (args.length >= 1 && args[0].equalsIgnoreCase("--check")) {
            handleCheckSectConsistency(sender);
            return;
        }

        Player target;

        if (args.length < 1) {
            // 修复自己
            if (!(sender instanceof Player)) {
                sendError(sender, "控制台必须指定玩家名称!");
                sendInfo(sender, "用法: /xiancore fixsect <玩家名>");
                sendInfo(sender, "      /xiancore fixsect --all   (批量修复所有玩家)");
                sendInfo(sender, "      /xiancore fixsect --check (仅检查不修复)");
                return;
            }
            target = (Player) sender;
        } else {
            // 修复指定玩家
            target = getOnlinePlayer(sender, args[0]);
            if (target == null) {
                return;
            }
        }

        // 加载玩家数据
        PlayerData data = loadPlayerData(sender, target);
        if (data == null) {
            return;
        }

        // 从内存中获取玩家的宗门
        Sect memSect = plugin.getSectSystem().getPlayerSect(target.getUniqueId());
        Integer dbSectId = data.getSectId();

        sendInfo(sender, "§b========== 宗门数据检测 ==========");
        sendInfo(sender, "§e玩家: §f" + target.getName());
        sendInfo(sender, "§e数据库中的宗门ID: §f" + (dbSectId != null ? dbSectId : "无"));
        sendInfo(sender, "§e内存中的宗门: §f" + (memSect != null ? memSect.getName() + " (ID: " + memSect.getId() + ")" : "无"));

        // 检查是否不一致
        if (memSect == null && dbSectId == null) {
            sendInfo(sender, "§a数据一致：玩家未加入任何宗门");
            sendInfo(sender, "§b================================");
            return;
        }

        if (memSect != null && dbSectId != null && memSect.getId() == dbSectId) {
            sendInfo(sender, "§a数据一致：玩家已加入宗门 " + memSect.getName());
            sendInfo(sender, "§b================================");
            return;
        }

        // 数据不一致，需要修复
        sendWarning(sender, "§c检测到数据不一致!");

        if (memSect == null && dbSectId != null) {
            // 内存中没有宗门，但数据库有 sectId
            sendInfo(sender, "§7问题：数据库中有宗门记录，但内存中找不到");
            sendInfo(sender, "§7正在清理数据库中的宗门数据...");

            data.setSectId(null);
            data.setSectRank("member");
            data.setContributionPoints(0);
            plugin.getDataManager().savePlayerData(data);

            sendSuccess(sender, "§a已清除玩家的宗门数据");
        } else if (memSect != null && dbSectId == null) {
            // 内存中有宗门，但数据库没有 sectId
            sendInfo(sender, "§7问题：内存中有宗门记录，但数据库没有");
            sendInfo(sender, "§7正在同步数据到数据库...");

            data.setSectId(memSect.getId());
            SectMember member = memSect.getMember(target.getUniqueId());
            if (member != null) {
                data.setSectRank(member.getRank().name());
            }
            plugin.getDataManager().savePlayerData(data);

            sendSuccess(sender, "§a已同步宗门数据到数据库");
        } else {
            // 宗门ID不匹配
            sendInfo(sender, "§7问题：宗门ID不匹配");
            sendInfo(sender, "§7正在同步内存数据到数据库...");

            data.setSectId(memSect.getId());
            SectMember member = memSect.getMember(target.getUniqueId());
            if (member != null) {
                data.setSectRank(member.getRank().name());
            }
            plugin.getDataManager().savePlayerData(data);

            sendSuccess(sender, "§a已同步宗门数据");
        }

        sendSuccess(sender, "§a§l========== 修复完成 ==========");
        sendSuccess(sender, "§e最终状态:");
        sendSuccess(sender, "§e  宗门ID: §f" + data.getSectId());
        sendSuccess(sender, "§e  职位: " + SectRank.getColoredDisplayName(data.getSectRank()));
        sendSuccess(sender, "§a§l============================");

        if (target != sender) {
            sendSuccess(target, "§a你的宗门数据已被管理员修复");
        }

        plugin.getLogger().info(sender.getName() + " 修复了 " + target.getName() + " 的宗门数据不一致问题");
    }

    /**
     * 批量修复所有玩家的宗门数据
     */
    private void handleFixSectAll(CommandSender sender) {
        sendInfo(sender, "§e§l========== 批量修复宗门数据 ==========");
        sendInfo(sender, "§7正在扫描所有宗门成员数据...");

        int totalChecked = 0;
        int fixedCount = 0;
        int errorCount = 0;

        // 遍历所有宗门的所有成员
        for (Sect sect : plugin.getSectSystem().getAllSects()) {
            for (SectMember member : sect.getMemberList()) {
                totalChecked++;

                try {
                    // 加载玩家数据
                    PlayerData data = plugin.getDataManager().loadPlayerData(member.getPlayerId());
                    if (data == null) {
                        sendWarning(sender, "§c玩家数据不存在: " + member.getPlayerName() + " (UUID: " + member.getPlayerId() + ")");
                        errorCount++;
                        continue;
                    }

                    // 检查是否不一致
                    Integer dbSectId = data.getSectId();
                    if (dbSectId == null || !dbSectId.equals(sect.getId())) {
                        sendInfo(sender, "§e修复: " + member.getPlayerName() + " (宗门: " + sect.getName() + ")");

                        // 修复数据
                        data.setSectId(sect.getId());
                        data.setSectRank(member.getRank().name());
                        plugin.getDataManager().savePlayerData(data);

                        fixedCount++;
                    }
                } catch (Exception e) {
                    sendError(sender, "§c处理失败: " + member.getPlayerName() + " - " + e.getMessage());
                    errorCount++;
                }
            }
        }

        sendInfo(sender, "§a§l========== 批量修复完成 ==========");
        sendInfo(sender, "§e总计检查: §f" + totalChecked + " §e个成员");
        sendInfo(sender, "§a成功修复: §f" + fixedCount + " §a条记录");
        if (errorCount > 0) {
            sendWarning(sender, "§c错误数量: §f" + errorCount);
        }
        sendInfo(sender, "§a§l=================================");

        plugin.getLogger().info(sender.getName() + " 执行了批量宗门数据修复，修复了 " + fixedCount + " 条记录");
    }

    /**
     * 检查宗门数据一致性（不修复）
     */
    private void handleCheckSectConsistency(CommandSender sender) {
        sendInfo(sender, "§e§l========== 宗门数据一致性检查 ==========");
        sendInfo(sender, "§7正在检查所有宗门成员数据...");

        int totalChecked = 0;
        int inconsistentCount = 0;
        List<String> inconsistentPlayers = new ArrayList<>();

        // 遍历所有宗门的所有成员
        for (Sect sect : plugin.getSectSystem().getAllSects()) {
            for (SectMember member : sect.getMemberList()) {
                totalChecked++;

                try {
                    // 加载玩家数据
                    PlayerData data = plugin.getDataManager().loadPlayerData(member.getPlayerId());
                    if (data == null) {
                        inconsistentPlayers.add("§c" + member.getPlayerName() + " §7(数据不存在)");
                        inconsistentCount++;
                        continue;
                    }

                    // 检查是否不一致
                    Integer dbSectId = data.getSectId();
                    if (dbSectId == null || !dbSectId.equals(sect.getId())) {
                        String issue = "§e" + member.getPlayerName() + " §7(宗门: " + sect.getName() + ", DB宗门ID: " + dbSectId + ")";
                        inconsistentPlayers.add(issue);
                        inconsistentCount++;
                    }
                } catch (Exception e) {
                    inconsistentPlayers.add("§c" + member.getPlayerName() + " §7(检查失败: " + e.getMessage() + ")");
                    inconsistentCount++;
                }
            }
        }

        sendInfo(sender, "§a§l========== 检查完成 ==========");
        sendInfo(sender, "§e总计检查: §f" + totalChecked + " §e个成员");

        if (inconsistentCount == 0) {
            sendSuccess(sender, "§a✓ 所有数据一致，未发现问题");
        } else {
            sendWarning(sender, "§c发现 " + inconsistentCount + " 个数据不一致:");
            for (String player : inconsistentPlayers) {
                sendInfo(sender, "  " + player);
            }
            sendInfo(sender, "");
            sendInfo(sender, "§e使用 §f/xiancore fixsect --all §e来批量修复");
        }
        sendInfo(sender, "§a§l==============================");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(getOnlinePlayerNames());
            options.add("--all");
            options.add("--check");
            return filterTabComplete(options, args[0]);
        }
        return new ArrayList<>();
    }
}
