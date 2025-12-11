package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 商店命令
 * /xiancore shop
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class ShopCommand extends AbstractSubCommand {

    public ShopCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "shop";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"商店", "activeqi", "活跃灵气"};
    }

    @Override
    public String getPermission() {
        return "xiancore.shop";
    }

    @Override
    public String getUsage() {
        return "/xiancore shop";
    }

    @Override
    public String getDescription() {
        return "打开活跃灵气商店";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendError(sender, "此命令只能由玩家执行!");
            return;
        }

        if (!checkPermission(sender)) {
            return;
        }

        Player player = (Player) sender;
        plugin.getGuiManager().getActiveQiShopGUI().open(player);
    }
}
