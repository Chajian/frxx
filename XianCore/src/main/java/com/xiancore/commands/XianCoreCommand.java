package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.SubCommand;
import com.xiancore.commands.sub.SubCommandRegistry;
import com.xiancore.commands.sub.impl.*;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * XianCore 主命令处理器
 * 处理 /xiancore 命令，采用命令模式路由到各子命令
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class XianCoreCommand extends BaseCommand {

    private final SubCommandRegistry registry;

    public XianCoreCommand(XianCore plugin) {
        super(plugin);
        this.registry = new SubCommandRegistry();
        registerSubCommands();
    }

    /**
     * 注册所有子命令
     */
    private void registerSubCommands() {
        // 基础命令
        registry.register(new HelpCommand(plugin, registry));
        registry.register(new ReloadCommand(plugin));
        registry.register(new InfoCommand(plugin));
        registry.register(new DebugCommand(plugin));
        registry.register(new ShopCommand(plugin));

        // 功能命令
        registry.register(new GiveCommand(plugin));
        registry.register(new FixLevelCommand(plugin));
        registry.register(new FixSectCommand(plugin));
        registry.register(new PlayerCommand(plugin));
        registry.register(new MigrateCommand(plugin));
    }

    /**
     * 获取子命令注册器
     */
    public SubCommandRegistry getRegistry() {
        return registry;
    }

    @Override
    protected void showHelp(CommandSender sender) {
        SubCommand helpCmd = registry.get("help");
        if (helpCmd != null) {
            helpCmd.execute(sender, new String[0]);
        }
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        // 如果没有参数，显示帮助
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = registry.get(subCommandName);

        if (subCommand == null) {
            sendError(sender, "未知的子命令: " + subCommandName);
            sendInfo(sender, "使用 /xiancore help 查看帮助");
            return;
        }

        // 去掉第一个参数（子命令名），传递剩余参数
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 第一层：子命令名称
            return filterTabComplete(registry.getCommandNames(), args[0]);
        }

        if (args.length >= 2) {
            // 委托给子命令处理
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = registry.get(subCommandName);
            if (subCommand != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }

        return new ArrayList<>();
    }
}
