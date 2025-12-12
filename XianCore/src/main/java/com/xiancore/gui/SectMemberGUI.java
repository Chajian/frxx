package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectMemberDisplayService;
import com.xiancore.systems.sect.SectMemberDisplayService.*;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 宗门成员管理 GUI
 * 提供成员列表、权限查看、职位管理等功能
 * 业务逻辑委托给 SectMemberDisplayService
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 使用 Service 层分离业务逻辑
 */
public class SectMemberGUI {

    private final XianCore plugin;
    private final SectMemberDisplayService displayService;

    public SectMemberGUI(XianCore plugin) {
        this.plugin = plugin;
        this.displayService = new SectMemberDisplayService(plugin);
    }

    /**
     * 打开成员列表主界面
     */
    public void openMemberList(Player player, Sect sect) {
        ChestGui gui = new ChestGui(6, "§a§l宗门成员管理");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(gui, 6, Material.GREEN_STAINED_GLASS_PANE);

        PaginatedPane memberPane = new PaginatedPane(1, 1, 7, 4);
        memberPane.populateWithGuiItems(createMemberItems(player, sect));
        gui.addPane(memberPane);

        StaticPane controlPane = new StaticPane(0, 5, 9, 1);

        addStatsItem(controlPane, sect);
        addPermissionItem(controlPane, sect);
        addNavigationButtons(controlPane, memberPane, gui);
        addCloseButton(controlPane, player);

        gui.addPane(controlPane);
        gui.show(player);
    }

    /**
     * 添加统计信息物品
     */
    private void addStatsItem(StaticPane pane, Sect sect) {
        MemberStatsInfo stats = displayService.getMemberStats(sect);

        ItemStack statsItem = new ItemBuilder(Material.BOOK)
                .name("§e§l成员统计")
                .lore(
                        "§7总成员数: §f" + stats.getMemberCount() + "§7/§f" + stats.getMaxMembers(),
                        "§7宗主: §6" + stats.getLeaderCount(),
                        "§7长老: §b" + stats.getElderCount(),
                        "§7核心弟子: §a" + stats.getCoreCount(),
                        "§7内门弟子: §e" + stats.getInnerCount(),
                        "§7外门弟子: §7" + stats.getOuterCount()
                )
                .build();
        pane.addItem(new GuiItem(statsItem), 1, 0);
    }

    /**
     * 添加权限信息物品
     */
    private void addPermissionItem(StaticPane pane, Sect sect) {
        PermissionStatsInfo permStats = displayService.getPermissionStats(sect);
        if (permStats == null) return;

        List<String> loreList = new ArrayList<>();
        loreList.add("§7领地ID: §f" + permStats.getLandId());
        loreList.add("");
        loreList.addAll(permStats.getStatsLines());

        ItemStack permItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§d§l权限统计")
                .lore(loreList)
                .build();
        pane.addItem(new GuiItem(permItem), 3, 0);
    }

    /**
     * 添加导航按钮
     */
    private void addNavigationButtons(StaticPane pane, PaginatedPane memberPane, ChestGui gui) {
        if (memberPane.getPage() > 0) {
            ItemStack prevItem = new ItemBuilder(Material.ARROW)
                    .name("§7上一页")
                    .build();
            pane.addItem(new GuiItem(prevItem, event -> {
                memberPane.setPage(memberPane.getPage() - 1);
                gui.update();
            }), 5, 0);
        }

        if (memberPane.getPage() < memberPane.getPages() - 1) {
            ItemStack nextItem = new ItemBuilder(Material.ARROW)
                    .name("§7下一页")
                    .build();
            pane.addItem(new GuiItem(nextItem, event -> {
                memberPane.setPage(memberPane.getPage() + 1);
                gui.update();
            }), 6, 0);
        }
    }

    /**
     * 添加关闭按钮
     */
    private void addCloseButton(StaticPane pane, Player player) {
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
        pane.addItem(new GuiItem(closeItem, event -> player.closeInventory()), 7, 0);
    }

    /**
     * 创建成员项列表
     */
    private List<GuiItem> createMemberItems(Player viewer, Sect sect) {
        List<GuiItem> items = new ArrayList<>();
        List<SectMember> members = displayService.getSortedMembers(sect);

        for (SectMember member : members) {
            items.add(createMemberItem(viewer, sect, member));
        }

        return items;
    }

    /**
     * 创建单个成员项
     */
    private GuiItem createMemberItem(Player viewer, Sect sect, SectMember member) {
        MemberDisplayInfo info = displayService.getMemberDisplayInfo(viewer, sect, member);
        SectRank rank = info.getRank();
        Material material = displayService.getMaterialForRank(rank);

        List<String> lore = new ArrayList<>();
        lore.add("§7玩家: §f" + info.getPlayerName());
        lore.add("§7职位: " + rank.getColorCode() + rank.getDisplayName());
        lore.add("§7贡献: §6" + info.getContribution());
        lore.add("§7加入时间: §f" + info.getJoinDate());
        lore.add("");
        lore.add(info.getActivityStatus());

        if (info.hasPermissionInfo()) {
            lore.add("");
            lore.add("§d领地权限: " + info.getPermissionLevel());
            lore.add("§7" + info.getPermissionDesc());
        }

        if (info.canViewDetails()) {
            lore.add("");
            lore.add("§e左键: §7查看详情");
            if (info.canManage()) {
                lore.add("§eShift+左键: §7管理职位");
                lore.add("§eShift+右键: §c踢出宗门");
            }
        }

        ItemStack item = new ItemBuilder(material)
                .name(rank.getColorCode() + "§l" + info.getPlayerName())
                .lore(lore.toArray(new String[0]))
                .build();

        return new GuiItem(item, event -> {
            if (event.isShiftClick()) {
                handleMemberManagement(viewer, sect, member, event.isRightClick());
            } else {
                openMemberDetail(viewer, sect, member);
            }
        });
    }

    /**
     * 处理成员管理操作
     */
    private void handleMemberManagement(Player viewer, Sect sect, SectMember target, boolean isKick) {
        ManagePermissionResult result = displayService.checkManagePermission(viewer, sect, target);

        if (!result.isAllowed()) {
            viewer.sendMessage(result.getErrorMessage());
            return;
        }

        viewer.closeInventory();

        if (isKick) {
            viewer.sendMessage("§e正在踢出成员 " + target.getPlayerName() + "...");
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                viewer.performCommand("sect kick " + target.getPlayerName());
            }, 1L);
        } else {
            openRankManagement(viewer, sect, target);
        }
    }

    /**
     * 打开职位管理界面
     */
    private void openRankManagement(Player viewer, Sect sect, SectMember target) {
        ChestGui gui = new ChestGui(3, "§e§l职位管理 - " + target.getPlayerName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 3);

        ItemStack currentItem = new ItemBuilder(Material.NAME_TAG)
                .name("§e当前职位")
                .lore(
                        "§7" + target.getRank().getDisplayName(),
                        "",
                        "§7等级: §f" + target.getRank().getLevel()
                )
                .build();
        pane.addItem(new GuiItem(currentItem), 4, 0);

        if (displayService.canPromote(target.getRank())) {
            ItemStack promoteItem = new ItemBuilder(Material.LIME_DYE)
                    .name("§a晋升")
                    .lore("§7提升成员职位")
                    .build();
            pane.addItem(new GuiItem(promoteItem, event -> {
                viewer.closeInventory();
                viewer.performCommand("sect promote " + target.getPlayerName());
            }), 2, 1);
        }

        if (displayService.canDemote(target.getRank())) {
            ItemStack demoteItem = new ItemBuilder(Material.ORANGE_DYE)
                    .name("§6降职")
                    .lore("§7降低成员职位")
                    .build();
            pane.addItem(new GuiItem(demoteItem, event -> {
                viewer.closeInventory();
                viewer.performCommand("sect demote " + target.getPlayerName());
            }), 6, 1);
        }

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("§7返回")
                .build();
        pane.addItem(new GuiItem(backItem, event -> {
            openMemberList(viewer, sect);
        }), 4, 2);

        gui.addPane(pane);
        gui.show(viewer);
    }

    /**
     * 打开成员详情界面
     */
    private void openMemberDetail(Player viewer, Sect sect, SectMember member) {
        MemberDetailInfo info = displayService.getMemberDetailInfo(sect, member);

        ChestGui gui = new ChestGui(4, "§b§l成员详情 - " + info.getPlayerName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 4);

        ItemStack infoItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name("§b" + info.getPlayerName())
                .lore(
                        "§7UUID: §f" + info.getShortUuid(),
                        "§7职位: " + info.getRank().getColorCode() + info.getRank().getDisplayName(),
                        "§7贡献: §6" + info.getContribution(),
                        "",
                        "§7加入时间: §f" + info.getJoinDate(),
                        "§7最后活跃: §f" + info.getLastActiveDate()
                )
                .build();
        pane.addItem(new GuiItem(infoItem), 4, 1);

        if (info.hasLand()) {
            List<String> permLore = new ArrayList<>();
            permLore.add("§7等级: " + info.getPermLevelName());
            permLore.add("§7" + info.getPermLevelDesc());
            permLore.add("");
            permLore.add("§e权限详情:");
            for (String flag : info.getPermissionFlags()) {
                permLore.add("§7- §a" + flag);
            }

            ItemStack permItem = new ItemBuilder(Material.WRITABLE_BOOK)
                    .name("§d领地权限")
                    .lore(permLore.toArray(new String[0]))
                    .build();
            pane.addItem(new GuiItem(permItem), 2, 1);
        }

        ItemStack contribItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("§6贡献记录")
                .lore(
                        "§7总贡献: §6" + info.getContribution(),
                        "",
                        "§7贡献可用于:",
                        "§7- 宗门商店兑换",
                        "§7- 提升职位参考",
                        "§7- 领地权限依据"
                )
                .build();
        pane.addItem(new GuiItem(contribItem), 6, 1);

        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("§7返回成员列表")
                .build();
        pane.addItem(new GuiItem(backItem, event -> {
            openMemberList(viewer, sect);
        }), 4, 3);

        gui.addPane(pane);
        gui.show(viewer);
    }
}
