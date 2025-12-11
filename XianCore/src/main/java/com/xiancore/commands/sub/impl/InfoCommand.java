package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * 信息命令
 * /xiancore info
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class InfoCommand extends AbstractSubCommand {

    public InfoCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"信息", "about"};
    }

    @Override
    public String getUsage() {
        return "/xiancore info";
    }

    @Override
    public String getDescription() {
        return "显示插件信息";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sendInfo(sender, "§b========== XianCore 插件信息 ==========");
        sendInfo(sender, "§e版本: §f" + plugin.getDescription().getVersion());
        sendInfo(sender, "§e作者: §f" + plugin.getDescription().getAuthors());
        sendInfo(sender, "§e描述: §f" + plugin.getDescription().getDescription());

        // 显示各系统状态
        sendInfo(sender, "§b========== 系统状态 ==========");
        sendInfo(sender, "§e修炼系统: §a已加载");
        sendInfo(sender, "§e炼器系统: §a已加载");
        sendInfo(sender, "§e奇遇系统: §a已加载");
        sendInfo(sender, "§e宗门系统: §a已加载");
        sendInfo(sender, "§e功法系统: §a已加载");
        sendInfo(sender, "§e天劫系统: §a已加载");

        // 显示集成状态
        sendInfo(sender, "§b========== 集成状态 ==========");
        sendInfo(sender, "§eMythicMobs: " + (Bukkit.getPluginManager().getPlugin("MythicMobs") != null ? "§a已连接" : "§c未安装"));
        sendInfo(sender, "§eVault: " + (Bukkit.getPluginManager().getPlugin("Vault") != null ? "§a已连接" : "§c未安装"));
        sendInfo(sender, "§b=====================================");
    }
}
