package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import org.bukkit.command.CommandSender;

/**
 * 重载命令
 * /xiancore reload
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class ReloadCommand extends AbstractSubCommand {

    public ReloadCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"重载", "rl"};
    }

    @Override
    public String getPermission() {
        return "xiancore.reload";
    }

    @Override
    public String getUsage() {
        return "/xiancore reload";
    }

    @Override
    public String getDescription() {
        return "重载配置文件";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }

        try {
            long start = System.currentTimeMillis();
            plugin.reloadConfigs();
            long elapsed = System.currentTimeMillis() - start;

            sendSuccess(sender, "配置已重载! 耗时: " + elapsed + "ms");
            plugin.getLogger().info(sender.getName() + " 重载了配置文件");
        } catch (Exception e) {
            sendError(sender, "配置重载失败: " + e.getMessage());
            plugin.getLogger().severe("配置重载失败: " + e.getMessage());
        }
    }
}
