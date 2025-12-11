package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import com.xiancore.commands.sub.SubCommand;
import com.xiancore.commands.sub.SubCommandRegistry;
import org.bukkit.command.CommandSender;

/**
 * 帮助命令
 * /xiancore help
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class HelpCommand extends AbstractSubCommand {

    private final SubCommandRegistry registry;

    public HelpCommand(XianCore plugin, SubCommandRegistry registry) {
        super(plugin);
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"帮助", "?"};
    }

    @Override
    public String getUsage() {
        return "/xiancore help";
    }

    @Override
    public String getDescription() {
        return "显示帮助信息";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sendInfo(sender, "§b========== XianCore 命令帮助 ==========");

        // 动态显示所有已注册的命令
        for (SubCommand cmd : registry.getAllCommands()) {
            sendInfo(sender, "§e" + cmd.getUsage() + " §7- " + cmd.getDescription());
        }

        sendInfo(sender, "§b=====================================");
    }
}
