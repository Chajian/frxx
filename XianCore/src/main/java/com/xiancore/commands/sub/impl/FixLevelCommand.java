package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import com.xiancore.core.data.PlayerData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 修复等级命令
 * /xiancore fixlevel [玩家]
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class FixLevelCommand extends AbstractSubCommand {

    public FixLevelCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "fixlevel";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"修复等级"};
    }

    @Override
    public String getPermission() {
        return "xiancore.admin.fixlevel";
    }

    @Override
    public String getUsage() {
        return "/xiancore fixlevel [玩家]";
    }

    @Override
    public String getDescription() {
        return "根据境界修复玩家等级";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }

        Player target;

        if (args.length < 1) {
            // 修复自己
            if (!(sender instanceof Player)) {
                sendError(sender, "控制台必须指定玩家名称!");
                sendInfo(sender, "用法: " + getUsage());
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

        // 计算正确的等级
        int correctLevel = calculateLevelByRealm(data.getRealm(), data.getRealmStage());
        int oldLevel = data.getLevel();

        // 更新等级
        data.setPlayerLevel(correctLevel);
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        sendSuccess(sender, "§a§l========== 等级修复成功 ==========");
        sendSuccess(sender, "§e玩家: §f" + target.getName());
        sendSuccess(sender, "§e境界: §f" + data.getFullRealmName());
        sendSuccess(sender, "§e等级: §f" + oldLevel + " §a→ §f" + correctLevel);
        sendSuccess(sender, "§a§l================================");

        if (target != sender) {
            sendSuccess(target, "§a你的等级已被修复为 §e" + correctLevel + " §a级 (境界: " + data.getFullRealmName() + ")");
        }

        plugin.getLogger().info(sender.getName() + " 修复了 " + target.getName() + " 的等级: " + oldLevel + " → " + correctLevel);
    }

    /**
     * 根据境界和境界阶段计算应有的玩家等级
     *
     * 等级计算规则：
     * - 起始等级：1
     * - 小境界突破（初→中，中→后）：+5级
     * - 大境界突破（后→下一境界初）：+15级
     *
     * @param realm      境界
     * @param realmStage 境界阶段 (1=初期, 2=中期, 3=后期)
     * @return 应有的等级
     */
    private int calculateLevelByRealm(String realm, int realmStage) {
        // 基础等级 = 1
        int level = 1;

        // 计算大境界的等级
        switch (realm) {
            case "炼气期" -> level = 1;
            case "筑基期" -> level = 1 + 5 + 5 + 15;      // 炼气初→中(+5)→后(+5)→筑基初(+15) = 26
            case "结丹期" -> level = 26 + 5 + 5 + 15;     // +筑基中(+5)→后(+5)→结丹初(+15) = 51
            case "元婴期" -> level = 51 + 5 + 5 + 15;     // +结丹中(+5)→后(+5)→元婴初(+15) = 76
            case "化神期" -> level = 76 + 5 + 5 + 15;     // +元婴中(+5)→后(+5)→化神初(+15) = 101
            case "炼虚期" -> level = 101 + 5 + 5 + 15;    // +化神中(+5)→后(+5)→炼虚初(+15) = 126
            case "合体期" -> level = 126 + 5 + 5 + 15;    // +炼虚中(+5)→后(+5)→合体初(+15) = 151
            case "大乘期" -> level = 151 + 5 + 5 + 15;    // +合体中(+5)→后(+5)→大乘初(+15) = 176
            default -> level = 1;
        }

        // 加上小境界阶段的等级
        // 初期 = 0, 中期 = +5, 后期 = +10
        level += (realmStage - 1) * 5;

        return level;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(getOnlinePlayerNames(), args[0]);
        }
        return new ArrayList<>();
    }
}
