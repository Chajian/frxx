package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.realm.Realm;
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
     * @param realmName  境界名称
     * @param realmStage 境界阶段 (1=初期, 2=中期, 3=后期)
     * @return 应有的等级
     */
    private int calculateLevelByRealm(String realmName, int realmStage) {
        Realm realm = plugin.getRealmRegistry().getByName(realmName);
        if (realm == null) {
            return 1;
        }
        return realm.calculateLevel(realmStage);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(getOnlinePlayerNames(), args[0]);
        }
        return new ArrayList<>();
    }
}
