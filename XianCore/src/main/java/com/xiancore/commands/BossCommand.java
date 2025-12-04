package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.command.BossCommandImpl;
import com.xiancore.systems.boss.config.BossConfigLoader;
import com.xiancore.systems.boss.damage.DamageStatisticsManager;
import com.xiancore.systems.boss.gui.BossGUI;
import com.xiancore.systems.boss.gui.BossAdminGUI;
import com.xiancore.systems.boss.permission.BossPermissions;
import com.xiancore.systems.boss.permission.PermissionChecker;
import com.xiancore.systems.boss.permission.BossPermissionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Boss 命令处理器
 * 处理 /boss 命令
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class BossCommand implements CommandExecutor, TabCompleter {

    private final XianCore plugin;
    private final BossGUI bossGUI;
    private final BossAdminGUI adminGUI;
    private final BossCommandImpl adminCommand;
    private final PermissionChecker permissionChecker;

    public BossCommand(XianCore plugin, BossRefreshManager bossManager, DamageStatisticsManager damageManager) {
        this.plugin = plugin;
        this.bossGUI = new BossGUI(plugin, bossManager, damageManager);
        this.permissionChecker = new PermissionChecker(plugin.getLogger());

        BossPermissionManager permissionManager = new BossPermissionManager(plugin.getLogger());
        this.adminGUI = new BossAdminGUI(plugin, bossManager, permissionManager);

        BossConfigLoader configLoader = new BossConfigLoader(plugin);
        this.adminCommand = new BossCommandImpl(plugin, permissionChecker, bossManager, configLoader, this.adminGUI);

        plugin.getServer().getPluginManager().registerEvents(bossGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(adminGUI, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                bossGUI.openBossListGUI((Player) sender);
            } else {
                sender.sendMessage("§c控制台请使用: /boss help");
            }
            return true;
        }

        String subCmd = args[0].toLowerCase();
        
        switch (subCmd) {
            case "list", "gui" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c此命令只能由玩家执行！");
                    return true;
                }
                bossGUI.openBossListGUI((Player) sender);
                return true;
            }
            case "help" -> {
                return adminCommand.onCommand(sender, command, label, args);
            }
            case "info", "stats" -> {
                return adminCommand.onCommand(sender, command, label, args);
            }
            case "add", "remove", "edit", "enable", "disable", "spawn", "reload" -> {
                if (!permissionChecker.hasPermission(sender, BossPermissions.ADMIN)) {
                    sender.sendMessage("§c你没有权限使用管理命令！");
                    return true;
                }
                return adminCommand.onCommand(sender, command, label, args);
            }
            default -> {
                sender.sendMessage("§c未知的子命令！使用 /boss help 查看帮助");
                return true;
            }
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> allSubs = new ArrayList<>();
            allSubs.add("list");
            allSubs.add("gui");
            allSubs.add("help");
            allSubs.add("info");
            allSubs.add("stats");
            
            if (permissionChecker.hasPermission(sender, BossPermissions.ADMIN)) {
                allSubs.add("add");
                allSubs.add("remove");
                allSubs.add("edit");
                allSubs.add("enable");
                allSubs.add("disable");
                allSubs.add("spawn");
                allSubs.add("reload");
            }
            
            String input = args[0].toLowerCase();
            return allSubs.stream()
                .filter(s -> s.startsWith(input))
                .collect(Collectors.toList());
        }
        
        return adminCommand.onTabComplete(sender, command, alias, args);
    }
}
