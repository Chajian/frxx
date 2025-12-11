package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.data.SpiritualRootType;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 玩家数据管理命令
 * /xiancore player <操作> [参数]
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class PlayerCommand extends AbstractSubCommand {

    public PlayerCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "player";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"玩家"};
    }

    @Override
    public String getPermission() {
        return "xiancore.admin.player";
    }

    @Override
    public String getUsage() {
        return "/xiancore player <info|set|reset|list> [参数]";
    }

    @Override
    public String getDescription() {
        return "管理玩家数据";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }

        if (args.length < 1) {
            showUsageHelp(sender);
            return;
        }

        String operation = args[0].toLowerCase();

        switch (operation) {
            case "info":
            case "信息":
                handleInfo(sender, args);
                break;
            case "set":
            case "设置":
                handleSet(sender, args);
                break;
            case "reset":
            case "重置":
                handleReset(sender, args);
                break;
            case "list":
            case "列表":
                handleList(sender);
                break;
            default:
                sendError(sender, "未知操作: " + operation);
                showUsageHelp(sender);
                break;
        }
    }

    private void showUsageHelp(CommandSender sender) {
        sendError(sender, "用法: " + getUsage());
        sendInfo(sender, "操作:");
        sendInfo(sender, "  §einfo <玩家> §7- 查看玩家数据");
        sendInfo(sender, "  §eset <玩家> <字段> <值> §7- 设置玩家数据");
        sendInfo(sender, "  §ereset <玩家> §7- 重置玩家数据");
        sendInfo(sender, "  §elist §7- 列出所有在线玩家");
    }

    /**
     * 查看玩家信息
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "用法: /xiancore player info <玩家>");
            return;
        }

        Player target = getOnlinePlayer(sender, args[1]);
        if (target == null) {
            return;
        }

        PlayerData data = loadPlayerData(sender, target);
        if (data == null) {
            return;
        }

        sendInfo(sender, "§b========== 玩家数据 ==========");
        sendInfo(sender, "§e玩家: §f" + target.getName());
        sendInfo(sender, "§eUUID: §7" + target.getUniqueId());
        sendInfo(sender, "§e境界: §f" + data.getFullRealmName());
        sendInfo(sender, "§e修为: §f" + data.getQi());
        sendInfo(sender, "§e灵根: " + data.getSpiritualRootDisplay());
        sendInfo(sender, "§e五行: " + data.getSpiritualRootElements());
        sendInfo(sender, "§e灵根值: §f" + String.format("%.3f", data.getSpiritualRoot()));
        sendInfo(sender, "§e灵石: §6" + data.getSpiritStones());
        sendInfo(sender, "§e等级: §f" + data.getLevel());
        sendInfo(sender, "§e宗门ID: §f" + (data.getSectId() != null ? data.getSectId() : "无"));
        sendInfo(sender, "§e职位: " + SectRank.getColoredDisplayName(data.getSectRank()));
        sendInfo(sender, "§e贡献: §f" + data.getContributionPoints());
        sendInfo(sender, "§e活跃灵气: §b" + data.getActiveQi());
        sendInfo(sender, "§b============================");
    }

    /**
     * 设置玩家数据
     */
    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendError(sender, "用法: /xiancore player set <玩家> <字段> <值>");
            sendInfo(sender, "可用字段: qi, realm, spiritStones, level, contribution, activeQi, spiritualRoot, rootType");
            return;
        }

        Player target = getOnlinePlayer(sender, args[1]);
        if (target == null) {
            return;
        }

        PlayerData data = loadPlayerData(sender, target);
        if (data == null) {
            return;
        }

        String field = args[2].toLowerCase();
        String value = args[3];

        try {
            switch (field) {
                case "qi":
                case "修为":
                    long qi = Long.parseLong(value);
                    data.setQi(qi);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的修为为: " + qi);
                    break;

                case "realm":
                case "境界":
                    data.setRealm(value);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的境界为: " + value);
                    break;

                case "spiritstones":
                case "spiritstone":
                case "灵石":
                    long stones = Long.parseLong(value);
                    data.setSpiritStones(stones);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的灵石为: " + stones);
                    break;

                case "level":
                case "等级":
                    int level = Integer.parseInt(value);
                    data.setPlayerLevel(level);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的等级为: " + level);
                    break;

                case "contribution":
                case "贡献":
                    int contribution = Integer.parseInt(value);
                    data.setContributionPoints(contribution);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的贡献为: " + contribution);
                    break;

                case "activeqi":
                case "活跃灵气":
                    long activeQi = Long.parseLong(value);
                    data.setActiveQi(activeQi);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的活跃灵气为: " + activeQi);
                    break;

                case "spiritualroot":
                case "灵根值":
                    double spiritualRoot = Double.parseDouble(value);
                    if (spiritualRoot < 0.0 || spiritualRoot > 1.0) {
                        sendError(sender, "灵根值必须在 0.0 到 1.0 之间");
                        return;
                    }
                    data.setSpiritualRoot(spiritualRoot);
                    data.setSpiritualRootType(SpiritualRootType.fromValue(spiritualRoot));
                    sendSuccess(sender, "已设置 " + target.getName() + " 的灵根值为: " + String.format("%.3f", spiritualRoot));
                    sendInfo(sender, "灵根类型: " + data.getSpiritualRootDisplay());
                    break;

                case "roottype":
                case "灵根类型":
                    SpiritualRootType rootType = SpiritualRootType.fromName(value);
                    if (rootType == null) {
                        try {
                            rootType = SpiritualRootType.valueOf(value.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            sendError(sender, "未知的灵根类型: " + value);
                            sendInfo(sender, "可用类型示例: HEAVENLY_FIRE, VARIANT_METAL_WOOD, TRUE_THREE_1, MIXED_FIVE");
                            sendInfo(sender, "或使用中文名称: 纯火灵根, 金木双灵根, 金木水三灵根, 杂灵根");
                            return;
                        }
                    }

                    data.setSpiritualRootType(rootType);
                    double newRootValue = (rootType.getMinValue() + rootType.getMaxValue()) / 2.0;
                    data.setSpiritualRoot(newRootValue);

                    sendSuccess(sender, "§a已设置 " + target.getName() + " 的灵根类型");
                    sendInfo(sender, "灵根: " + data.getSpiritualRootDisplay());
                    sendInfo(sender, "五行属性: " + data.getSpiritualRootElements());
                    sendInfo(sender, "灵根值: " + String.format("%.3f", newRootValue));

                    if (target != sender) {
                        target.sendMessage("§b========================================");
                        target.sendMessage("§6§l你的灵根已被管理员修改！");
                        target.sendMessage("");
                        target.sendMessage("§e新灵根: " + data.getSpiritualRootDisplay());
                        target.sendMessage("§e五行属性: " + data.getSpiritualRootElements());
                        target.sendMessage("§e灵根值: §f" + String.format("%.1f%%", newRootValue * 100));
                        target.sendMessage("");
                        target.sendMessage(data.getSpiritualRootDescription());
                        target.sendMessage("§b========================================");
                    }
                    break;

                default:
                    sendError(sender, "未知字段: " + field);
                    sendInfo(sender, "可用字段: qi, realm, spiritStones, level, contribution, activeQi, spiritualRoot, rootType");
                    return;
            }

            plugin.getDataManager().savePlayerData(data);
            plugin.getLogger().info(sender.getName() + " 设置了 " + target.getName() + " 的 " + field + " 为 " + value);

        } catch (NumberFormatException e) {
            sendError(sender, "无效的数值: " + value);
        }
    }

    /**
     * 重置玩家数据
     */
    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "用法: /xiancore player reset <玩家>");
            return;
        }

        Player target = getOnlinePlayer(sender, args[1]);
        if (target == null) {
            return;
        }

        sendWarning(sender, "§c警告：此操作将重置 " + target.getName() + " 的所有数据!");
        sendWarning(sender, "§c如需确认，请再次执行此命令（功能待完善）");
        sendInfo(sender, "§7此功能暂未完全实现");
    }

    /**
     * 列出所有在线玩家
     */
    private void handleList(CommandSender sender) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        sendInfo(sender, "§b========== 在线玩家列表 ==========");
        sendInfo(sender, "§e总计: §f" + players.size() + " 人");
        sendInfo(sender, "");

        int count = 0;
        for (Player player : players) {
            PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
            if (data != null) {
                count++;
                sendInfo(sender, String.format("§f%d. §e%s §7- §f%s §7灵石: §6%d",
                        count,
                        player.getName(),
                        data.getFullRealmName(),
                        data.getSpiritStones()
                ));
            }
        }

        sendInfo(sender, "§b================================");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(Arrays.asList("info", "set", "reset", "list"), args[0]);
        }

        if (args.length == 2) {
            String operation = args[0].toLowerCase();
            if (operation.equals("info") || operation.equals("set") || operation.equals("reset") ||
                    operation.equals("信息") || operation.equals("设置") || operation.equals("重置")) {
                return filterTabComplete(getOnlinePlayerNames(), args[1]);
            }
        }

        if (args.length == 3) {
            String operation = args[0].toLowerCase();
            if (operation.equals("set") || operation.equals("设置")) {
                return filterTabComplete(Arrays.asList(
                        "qi", "realm", "spiritStones", "level", "contribution", "activeQi", "spiritualRoot", "rootType"
                ), args[2]);
            }
        }

        return new ArrayList<>();
    }
}
