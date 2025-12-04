package com.yourserver.xiancore.command;

import com.yourserver.xiancore.XianCoreAddon;
import com.yourserver.xiancore.command.subcommand.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

/**
 * 主命令处理器
 */
public class XianCoreCommand implements CommandExecutor, TabCompleter {
    
    private final XianCoreAddon plugin;
    private final Map<String, SubCommand> subCommands;
    
    public XianCoreCommand(XianCoreAddon plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        
        // 注册子命令
        registerSubCommand(new AttributeCommand(plugin));
        registerSubCommand(new BuffCommand(plugin));
        registerSubCommand(new ItemsCommand(plugin));
        registerSubCommand(new ReloadCommand(plugin));
        registerSubCommand(new AdminCommand(plugin));
    }
    
    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        
        // 注册别名
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 无参数显示帮助
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        // 获取子命令
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.invalid-arguments")
                .replace("%usage%", "/xiancore <attribute|buff|items|reload|admin>"));
            return true;
        }
        
        // 检查权限
        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        
        // 执行子命令
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab补全子命令
            for (SubCommand subCommand : subCommands.values()) {
                if (sender.hasPermission(subCommand.getPermission())) {
                    String name = subCommand.getName();
                    if (name.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(name);
                    }
                }
            }
        } else if (args.length > 1) {
            // Tab补全子命令参数
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }
        
        Collections.sort(completions);
        return completions;
    }
    
    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== XianCore 帮助 =====");
        sender.sendMessage("§e/xiancore attribute §7- 属性管理");
        sender.sendMessage("§e/xiancore buff §7- Buff管理");
        sender.sendMessage("§e/xiancore items §7- 道具记录查询");
        sender.sendMessage("§e/xiancore reload §7- 重载配置");
        sender.sendMessage("§e/xiancore admin §7- 管理员命令");
        sender.sendMessage("§6§l====================");
    }
}

