package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.TribulationGUI;
import com.xiancore.systems.tribulation.Tribulation;
import com.xiancore.systems.tribulation.TribulationType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 天劫命令处理器
 * 处理 /tribulation 命令
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TribulationCommand extends BaseCommand {

    public TribulationCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // 如果没有参数，显示帮助
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
            case "开始":
                handleStart(player, args);
                break;

            case "progress":
            case "进度":
                handleProgress(player);
                break;

            case "cancel":
            case "取消":
                handleCancel(player);
                break;

            case "info":
            case "信息":
                handleInfo(player);
                break;

            case "help":
            case "帮助":
                showHelp(sender);
                break;

            default:
                sendError(sender, "未知的子命令: " + subCommand);
                sendInfo(sender, "使用 /tribulation help 查看帮助");
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(Arrays.asList("start", "progress", "cancel", "info", "help"), args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.stream(TribulationType.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== 天劫系统命令帮助 ==========");
        sendInfo(sender, "§e/tribulation start <类型> §7- 开始渡劫");
        sendInfo(sender, "§7  类型: qi_condensation (凝气劫)");
        sendInfo(sender, "§7        foundation (筑基劫)");
        sendInfo(sender, "§7        golden_core (金丹劫)");
        sendInfo(sender, "§7        nascent_soul (元婴劫)");
        sendInfo(sender, "§7        soul_formation (化神劫)");
        sendInfo(sender, "§7        void_refinement (炼虚劫)");
        sendInfo(sender, "§7        integration (合体劫)");
        sendInfo(sender, "§7        mahayana (大乘劫)");
        sendInfo(sender, "§7        transcendence (飞升劫)");
        sendInfo(sender, "§e/tribulation progress §7- 查看渡劫进度");
        sendInfo(sender, "§e/tribulation cancel §7- 取消当前天劫");
        sendInfo(sender, "§e/tribulation info §7- 查看天劫信息");
        sendInfo(sender, "§e/tribulation help §7- 显示此帮助");
        sendInfo(sender, "§b====================================");
    }

    /**
     * 处理开始天劫
     */
    private void handleStart(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.tribulation.start")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定天劫类型!");
            sendInfo(player, "§e用法: /tribulation start <类型>");
            sendInfo(player, "§7示例: /tribulation start qi_condensation");
            return;
        }

        String typeName = args[1].toUpperCase();
        TribulationType type;

        try {
            type = TribulationType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            sendError(player, "未知的天劫类型: " + args[1]);
            sendInfo(player, "§7使用 /tribulation help 查看可用类型");
            return;
        }

        // 检查玩家境界是否符合
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            sendError(player, "数据加载失败!");
            return;
        }

        // 获取对应境界的天劫类型
        TribulationType realmTribulation = TribulationType.fromRealm(data.getRealm());
        if (type.getTier() > (realmTribulation != null ? realmTribulation.getTier() : 0) + 1) {
            sendError(player, "§c你的境界不足以渡此天劫!");
            sendInfo(player, "§7当前境界: §f" + data.getFullRealmName());
            if (realmTribulation != null) {
                sendInfo(player, "§7推荐天劫: §f" + realmTribulation.getDisplayName());
            }
            return;
        }

        // 尝试开始天劫
        boolean success = plugin.getTribulationSystem().startTribulation(player, type);

        if (success) {
            sendSuccess(player, "天劫已开始! 请做好准备!");
            // 自动打开进度GUI
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Tribulation tribulation = plugin.getTribulationSystem().getTribulation(player.getUniqueId());
                if (tribulation != null) {
                    TribulationGUI.open(player, plugin, tribulation);
                }
            }, 20L); // 1秒后打开
        }
    }

    /**
     * 处理查看进度
     */
    private void handleProgress(Player player) {
        if (!hasPermission(player, "xiancore.tribulation.use")) {
            return;
        }

        Tribulation tribulation = plugin.getTribulationSystem().getTribulation(player.getUniqueId());

        if (tribulation == null) {
            sendError(player, "§c你当前没有进行中的天劫!");
            sendInfo(player, "§7使用 /tribulation start <类型> 开始渡劫");
            return;
        }

        // 打开进度GUI
        TribulationGUI.open(player, plugin, tribulation);
    }

    /**
     * 处理取消天劫
     */
    private void handleCancel(Player player) {
        if (!hasPermission(player, "xiancore.tribulation.admin")) {
            return;
        }

        boolean success = plugin.getTribulationSystem().cancelTribulation(player.getUniqueId());

        if (success) {
            sendSuccess(player, "已取消天劫");
        } else {
            sendError(player, "你当前没有进行中的天劫!");
        }
    }

    /**
     * 处理查看信息
     */
    private void handleInfo(Player player) {
        if (!hasPermission(player, "xiancore.tribulation.use")) {
            return;
        }

        Tribulation tribulation = plugin.getTribulationSystem().getTribulation(player.getUniqueId());

        if (tribulation == null) {
            sendError(player, "§c你当前没有进行中的天劫!");
            sendInfo(player, "§7使用 /tribulation start <类型> 开始渡劫");
            return;
        }

        player.sendMessage("§b========== 天劫信息 ==========");
        player.sendMessage("§e类型: §f" + tribulation.getType().getDisplayName());
        player.sendMessage("§e劫数: §f" + tribulation.getType().getTier());
        player.sendMessage("§e当前波: §f" + tribulation.getCurrentWave() + "/" + tribulation.getTotalWaves());
        player.sendMessage("§e进度: §f" + String.format("%.1f%%", tribulation.getProgress()));
        player.sendMessage("§e持续时间: §f" + tribulation.getDuration() + " 秒");
        player.sendMessage("§e死亡次数: §f" + tribulation.getDeaths());
        player.sendMessage("§e总伤害: §f" + String.format("%.1f", tribulation.getTotalDamage()));

        if (tribulation.isCompleted()) {
            String rating = tribulation.calculateRating();
            String ratingColor = tribulation.getRatingColor();
            player.sendMessage("§e评级: " + ratingColor + rating + " §7(" + tribulation.getRatingDescription() + ")");
        }

        player.sendMessage("§b============================");
    }
}
