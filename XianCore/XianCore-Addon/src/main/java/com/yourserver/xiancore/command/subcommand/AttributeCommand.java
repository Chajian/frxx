package com.yourserver.xiancore.command.subcommand;

import com.yourserver.xiancore.XianCoreAddon;
import com.yourserver.xiancore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 属性命令
 */
public class AttributeCommand implements SubCommand {
    
    private final XianCoreAddon plugin;
    
    public AttributeCommand(XianCoreAddon plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "attribute";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("attr", "a");
    }
    
    @Override
    public String getPermission() {
        return "xiancore.admin";
    }
    
    @Override
    public String getDescription() {
        return "属性管理（管理员）";
    }
    
    @Override
    public String getUsage() {
        return "/xiancore attribute <get|set|add> <玩家名> <属性名> [数值]";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: " + getUsage());
            return;
        }
        
        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        
        if (target == null) {
            sender.sendMessage("§c玩家不存在或不在线！");
            return;
        }
        
        String attribute = args[2];
        
        switch (action) {
            case "get":
            case "g":
                handleGet(sender, target, attribute);
                break;
            case "set":
            case "s":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /xiancore attribute set <玩家名> <属性名> <数值>");
                    return;
                }
                handleSet(sender, target, attribute, args[3]);
                break;
            case "add":
            case "a":
                if (args.length < 4) {
                    sender.sendMessage("§c用法: /xiancore attribute add <玩家名> <属性名> <数值>");
                    return;
                }
                handleAdd(sender, target, attribute, args[3]);
                break;
            default:
                sender.sendMessage("§c未知操作！用法: " + getUsage());
        }
    }
    
    /**
     * 获取属性值
     */
    private void handleGet(CommandSender sender, Player target, String attribute) {
        double value = plugin.getAttributeManager().getAttribute(target, attribute);
        String attrName = plugin.getConfigManager().getAttributeDisplayName(attribute);
        
        sender.sendMessage(String.format(
            "§a玩家 %s 的 %s: §e%.3f",
            target.getName(), attrName, value
        ));
    }
    
    /**
     * 设置属性值
     */
    private void handleSet(CommandSender sender, Player target, String attribute, String valueStr) {
        try {
            double value = Double.parseDouble(valueStr);
            boolean success = plugin.getAttributeManager().setAttribute(target, attribute, value);
            
            if (success) {
                String attrName = plugin.getConfigManager().getAttributeDisplayName(attribute);
                sender.sendMessage(String.format(
                    "§a成功设置玩家 %s 的 %s 为 §e%.3f",
                    target.getName(), attrName, value
                ));
                target.sendMessage(String.format(
                    "§a管理员将你的 %s 设置为 §e%.3f",
                    attrName, value
                ));
            } else {
                sender.sendMessage("§c设置属性失败！");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值格式错误！");
        }
    }
    
    /**
     * 增加属性值
     */
    private void handleAdd(CommandSender sender, Player target, String attribute, String valueStr) {
        try {
            double value = Double.parseDouble(valueStr);
            boolean success = plugin.getAttributeManager().addAttribute(target, attribute, value);
            
            if (success) {
                double newValue = plugin.getAttributeManager().getAttribute(target, attribute);
                String attrName = plugin.getConfigManager().getAttributeDisplayName(attribute);
                sender.sendMessage(String.format(
                    "§a成功为玩家 %s 的 %s 增加 +%.3f (当前: §e%.3f§a)",
                    target.getName(), attrName, value, newValue
                ));
                target.sendMessage(String.format(
                    "§a管理员为你的 %s 增加了 +%.3f (当前: §e%.3f§a)",
                    attrName, value, newValue
                ));
            } else {
                sender.sendMessage("§c增加属性失败！");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数值格式错误！");
        }
    }
    
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("get");
            completions.add("set");
            completions.add("add");
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3) {
            completions.add("spiritual_root");
            completions.add("comprehension");
            completions.add("technique_adaptation");
            completions.add("active_qi");
            completions.add("skill_points");
        }
        
        return completions;
    }
}

