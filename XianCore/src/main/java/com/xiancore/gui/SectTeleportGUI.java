package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.sect.Sect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 宗门领地传送GUI
 *
 * 功能：
 * - 显示宗门领地信息
 * - 提供快速传送到领地的功能
 * - 所有宗门成员都可以使用
 *
 * @author Claude
 * @version 1.0.0
 */
public class SectTeleportGUI {

    private final XianCore plugin;

    public SectTeleportGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开传送菜单
     */
    public void openTeleportMenu(Player player, Sect sect) {
        ChestGui gui = new ChestGui(3, "§b§l宗门领地传送");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        OutlinePane background = new OutlinePane(0, 0, 9, 3);
        ItemStack border = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        // 内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 3);

        // 标题
        ItemStack titleItem = new ItemBuilder(Material.ENDER_PEARL)
                .name("§b§l宗门领地传送")
                .lore("§7宗门: §f" + sect.getName())
                .lore("§7宗主: §f" + sect.getOwnerName())
                .build();
        contentPane.addItem(new GuiItem(titleItem), 1, 0);

        // 检查是否有绑定领地
        if (sect.getResidenceLandId() == null || sect.getResidenceLandId().isEmpty()) {
            // 未绑定领地
            ItemStack noLandItem = new ItemBuilder(Material.REDSTONE_BLOCK)
                    .name("§c§l未绑定领地")
                    .lore("§7宗门还没有绑定领地")
                    .lore("§7请联系宗主绑定领地: §a/sect land bind <ID>")
                    .build();
            contentPane.addItem(new GuiItem(noLandItem), 4, 1);
        } else {
            // 已绑定领地 - 显示信息和传送按钮
            ItemStack landInfoItem = new ItemBuilder(Material.GRASS_BLOCK)
                    .name("§a§l领地信息")
                    .lore("§7领地ID: §f" + sect.getResidenceLandId())
                    .lore("§7状态: §a✓ 已绑定")
                    .build();
            contentPane.addItem(new GuiItem(landInfoItem), 2, 0);

            // 传送按钮
            ItemStack teleportItem = new ItemBuilder(Material.ENDER_PEARL)
                    .name("§a§l传送到领地")
                    .lore("§7点击传送到宗门领地")
                    .glow()
                    .build();
            contentPane.addItem(new GuiItem(teleportItem, event -> {
                event.setCancelled(true);
                performTeleport(player, sect);
            }), 4, 1);

            // 领地中心位置信息
            Location landCenter = sect.getLandCenter();
            if (landCenter != null) {
                ItemStack locationItem = new ItemBuilder(Material.COMPASS)
                        .name("§6§l坐标信息")
                        .lore("§7X: §f" + landCenter.getBlockX())
                        .lore("§7Y: §f" + landCenter.getBlockY())
                        .lore("§7Z: §f" + landCenter.getBlockZ())
                        .lore("§7世界: §f" + landCenter.getWorld().getName())
                        .build();
                contentPane.addItem(new GuiItem(locationItem), 6, 0);
            }
        }

        // 成员数量信息
        ItemStack memberItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name("§d§l成员信息")
                .lore("§7当前成员: §f" + sect.getMemberCount() + "§7/§f" + sect.getMaxMembers())
                .build();
        contentPane.addItem(new GuiItem(memberItem), 2, 1);

        // 宗门等级和经验
        ItemStack levelItem = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("§9§l宗门等级")
                .lore("§7等级: §fLv " + sect.getLevel())
                .lore("§7经验: §f" + sect.getExperience())
                .build();
        contentPane.addItem(new GuiItem(levelItem), 6, 1);

        // 返回按钮
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("§c返回")
                .build();
        contentPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            player.closeInventory();
        }), 4, 2);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 执行传送
     */
    private void performTeleport(Player player, Sect sect) {
        Location landCenter = sect.getLandCenter();

        if (landCenter == null) {
            // 尝试从Residence获取领地中心
            try {
                com.bekvon.bukkit.residence.Residence residence = com.bekvon.bukkit.residence.Residence.getInstance();
                com.bekvon.bukkit.residence.protection.ClaimedResidence claimed =
                    residence.getResidenceManager().getByName(sect.getResidenceLandId());

                if (claimed != null) {
                    // 使用Residence API获取传送点
                    landCenter = claimed.getTeleportLocation(player);
                    if (landCenter != null) {
                        sect.setLandCenter(landCenter);
                    } else {
                        player.sendMessage("§c无法获取领地传送点");
                        return;
                    }
                } else {
                    player.sendMessage("§c领地信息获取失败，请联系管理员");
                    return;
                }
            } catch (Exception e) {
                player.sendMessage("§c传送失败: " + e.getMessage());
                plugin.getLogger().warning("传送失败: " + e.getMessage());
                return;
            }
        }

        // 执行传送
        try {
            player.teleport(landCenter);
            player.sendMessage("§a✓ 传送成功！");
            player.sendTitle(
                    "§a传送成功",
                    "§7欢迎来到 " + sect.getName() + " 的领地",
                    20, 40, 20
            );
            player.closeInventory();
        } catch (Exception e) {
            player.sendMessage("§c传送失败: " + e.getMessage());
            plugin.getLogger().warning("传送执行失败: " + e.getMessage());
        }
    }
}
