package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.CultivationGUI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 修炼命令处理器
 * 处理 /cultivation 命令
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class CultivationCommand extends BaseCommand {

    public CultivationCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // 如果没有参数，打开修炼GUI
        if (args.length == 0) {
            CultivationGUI.open(player, plugin);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
                CultivationGUI.open(player, plugin);
                break;

            case "info":
            case "信息":
                showCultivationInfo(player);
                break;

            case "breakthrough":
            case "突破":
                handleBreakthrough(player);
                break;

            case "progress":
            case "进度":
                showProgress(player);
                break;

            case "help":
            case "帮助":
                showHelp(sender);
                break;

            default:
                sendError(sender, "未知的子命令: " + subCommand);
                sendInfo(sender, "使用 /cultivation help 查看帮助");
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("info", "breakthrough", "progress", "help");
            return filterTabComplete(completions, args[0]);
        }
        return new ArrayList<>();
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== 修炼系统命令帮助 ==========");
        sendInfo(sender, "§e/cultivation §7- 查看修炼信息");
        sendInfo(sender, "§e/cultivation info §7- 查看修炼信息");
        sendInfo(sender, "§e/cultivation breakthrough §7- 尝试突破境界");
        sendInfo(sender, "§e/cultivation progress §7- 查看修炼进度");
        sendInfo(sender, "§e/cultivation help §7- 显示此帮助");
        sendInfo(sender, "§b====================================");
    }

    /**
     * 显示修炼信息
     */
    private void showCultivationInfo(Player player) {
        if (!hasPermission(player, "xiancore.cultivation.use")) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        sendInfo(player, "§b========== 修炼信息 ==========");
        sendInfo(player, "§e境界: §f" + data.getFullRealmName());
        sendInfo(player, "§e修为: §f" + formatQi(data.getQi()));
        sendInfo(player, "§e灵根: §f" + formatPercentage(data.getSpiritualRoot()));
        sendInfo(player, "§e悟性: §f" + formatPercentage(data.getComprehension()));
        sendInfo(player, "§e功法适配: §f" + formatPercentage(data.getTechniqueAdaptation()));
        sendInfo(player, "§b=============================");
        sendInfo(player, "§e突破成功次数: §a" + data.getBreakthroughSuccessCount());
        sendInfo(player, "§e突破尝试次数: §7" + data.getBreakthroughAttemptCount());

        if (data.getBreakthroughAttemptCount() > 0) {
            double successRate = (double) data.getBreakthroughSuccessCount() / data.getBreakthroughAttemptCount() * 100;
            sendInfo(player, "§e突破成功率: §6" + String.format("%.1f%%", successRate));
        }

        sendInfo(player, "§b=============================");
    }

    /**
     * 处理突破命令
     */
    private void handleBreakthrough(Player player) {
        if (!hasPermission(player, "xiancore.cultivation.breakthrough")) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        // 检查是否可以突破
        if (!canBreakthrough(data)) {
            sendError(player, "修为不足，无法突破!");
            sendInfo(player, "当前修为: " + formatQi(data.getQi()));
            sendInfo(player, "需要修为: " + formatQi(getRequiredQi(data)));
            return;
        }

        // 检查突破冷却
        if (plugin.getCultivationSystem().isOnBreakthroughCooldown(player.getUniqueId())) {
            long remaining = plugin.getCultivationSystem().getRemainingBreakthroughCooldown(player.getUniqueId());
            sendError(player, "突破冷却中，还需 " + remaining + " 秒!");
            return;
        }

        // 尝试突破
        sendInfo(player, "§e正在尝试突破...");
        boolean success = plugin.getCultivationSystem().attemptBreakthrough(player);

        if (success) {
            // 广播突破成功
            plugin.getServer().broadcastMessage("§6§l⚡ " + player.getName() + " 成功突破至" + data.getFullRealmName() + "!");
        }
    }

    /**
     * 显示修炼进度
     */
    private void showProgress(Player player) {
        if (!hasPermission(player, "xiancore.cultivation.use")) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        long currentQi = data.getQi();
        long requiredQi = getRequiredQi(data);
        double progress = Math.min(100.0, (double) currentQi / requiredQi * 100);

        sendInfo(player, "§b========== 修炼进度 ==========");
        sendInfo(player, "§e当前境界: §f" + data.getFullRealmName());
        sendInfo(player, "§e修为进度: " + getProgressBar(progress) + " §f" + String.format("%.1f%%", progress));
        sendInfo(player, "§e当前修为: §f" + formatQi(currentQi));
        sendInfo(player, "§e需要修为: §f" + formatQi(requiredQi));
        sendInfo(player, "§e距离突破: §f" + formatQi(Math.max(0, requiredQi - currentQi)));
        sendInfo(player, "§b=============================");

        // 显示预估成功率
        if (currentQi >= requiredQi) {
            double successRate = calculateBreakthroughChance(data);
            sendInfo(player, "§e预估突破成功率: §6" + String.format("%.1f%%", successRate * 100));
            sendInfo(player, "§a可以尝试突破了! 使用 /cultivation breakthrough");
        } else {
            sendWarning(player, "修为不足，继续修炼吧!");
        }
    }

    /**
     * 检查是否可以突破
     */
    private boolean canBreakthrough(PlayerData data) {
        return data.getQi() >= getRequiredQi(data);
    }

    /**
     * 获取突破所需修为
     */
    private long getRequiredQi(PlayerData data) {
        String realm = data.getRealm();
        int stage = data.getRealmStage();

        // 基础修为需求
        long baseQi = switch (realm) {
            case "炼气期" -> 1000L;
            case "筑基期" -> 5000L;
            case "结丹期" -> 50000L;
            case "元婴期" -> 500000L;
            case "化神期" -> 5000000L;
            case "炼虚期" -> 50000000L;
            case "合体期" -> 500000000L;
            case "大乘期" -> 5000000000L;
            default -> 1000L;
        };

        // 每个小境界增加50%
        return (long) (baseQi * Math.pow(1.5, stage - 1));
    }

    /**
     * 计算突破成功率（简化版）
     */
    private double calculateBreakthroughChance(PlayerData data) {
        double L = data.getSpiritualRoot();
        double P = data.getTechniqueAdaptation();
        double E = 0.5; // 环境灵气
        double S = 0.5; // 资源投入
        double G = data.getComprehension();
        double D = getRealmDifficulty(data.getRealm());

        double alpha = 1.5;
        return 1 - Math.exp(-alpha * L * P * E * S * G / D);
    }

    /**
     * 获取境界难度
     */
    private double getRealmDifficulty(String realm) {
        return switch (realm) {
            case "炼气期" -> 1.0;
            case "筑基期" -> 2.0;
            case "结丹期" -> 5.0;
            case "元婴期" -> 10.0;
            case "化神期" -> 20.0;
            case "炼虚期" -> 40.0;
            case "合体期" -> 80.0;
            case "大乘期" -> 160.0;
            default -> 1.0;
        };
    }

    /**
     * 格式化修为数值
     */
    private String formatQi(long qi) {
        if (qi >= 1_000_000_000) {
            return String.format("%.1f亿", qi / 1_000_000_000.0);
        } else if (qi >= 10_000) {
            return String.format("%.1f万", qi / 10_000.0);
        } else {
            return String.valueOf(qi);
        }
    }

    /**
     * 格式化百分比
     */
    private String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    /**
     * 获取进度条
     */
    private String getProgressBar(double progress) {
        int total = 20;
        int filled = (int) (progress / 100 * total);

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("§7▒");
            }
        }

        return bar.toString();
    }
}
