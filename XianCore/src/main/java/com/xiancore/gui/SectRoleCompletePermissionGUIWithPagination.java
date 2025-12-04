package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.integration.residence.SectRoleResidencePermissionWrapper;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectRank;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 完整权限编辑GUI - 支持分页显示所有权限标志
 *
 * 功能：
 * - 支持分页显示所有100+权限标志
 * - 每页28个权限 (7列 × 4行)
 * - 支持上一页、下一页导航
 * - 显示当前页数和总页数
 * - 权限状态可视化 (绿/红/灰)
 *
 * @author Claude
 * @version 2.0.0 (分页版本)
 */
public class SectRoleCompletePermissionGUIWithPagination {

    private final XianCore plugin;
    private final Sect sect;
    private final com.bekvon.bukkit.residence.protection.ClaimedResidence residence;
    private final SectRoleResidencePermissionWrapper wrapper;

    // 权限显示相关常量
    private static final int PERMISSIONS_PER_PAGE = 28;  // 7列 × 4行
    private static final int PERMISSIONS_PER_ROW = 7;
    private static final int ROWS_PER_PAGE = 4;

    public SectRoleCompletePermissionGUIWithPagination(
            XianCore plugin,
            Sect sect,
            com.bekvon.bukkit.residence.protection.ClaimedResidence residence,
            com.xiancore.integration.residence.ResidencePermissionManager permissionManager) {
        this.plugin = plugin;
        this.sect = sect;
        this.residence = residence;
        this.wrapper = new SectRoleResidencePermissionWrapper(plugin, sect, residence, permissionManager);
    }

    /**
     * 打开主菜单 - 显示所有角色
     */
    public void openMainMenu(Player player) {
        ChestGui gui = new ChestGui(6, "§b完整权限配置 - " + sect.getName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        OutlinePane background = new OutlinePane(0, 0, 9, 6);
        ItemStack border = new ItemBuilder(Material.BLUE_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        // 标题
        StaticPane titlePane = new StaticPane(1, 0, 7, 1);
        ItemStack titleItem = new ItemBuilder(Material.BOOK)
                .name("§b§l完整权限配置")
                .lore("§7支持所有 " + wrapper.getAllAvailableFlags().size() + " 个权限标志")
                .lore("§7点击角色编辑权限")
                .build();
        titlePane.addItem(new GuiItem(titleItem), 3, 0);
        gui.addPane(titlePane);

        // 角色列表
        StaticPane rolePane = new StaticPane(1, 1, 7, 4);

        SectRank[] ranks = SectRank.values();
        for (int i = 0; i < ranks.length; i++) {
            SectRank rank = ranks[i];

            ItemStack roleCard = createRoleCard(rank);
            int col = i % 7;
            int row = i / 7;

            if (row < 4) {
                GuiItem guiItem = new GuiItem(roleCard, event -> {
                    event.setCancelled(true);
                    openRoleEditorWithPagination(player, rank, 0);  // 从第0页开始
                });
                rolePane.addItem(guiItem, col, row);
            }
        }
        gui.addPane(rolePane);

        // 底部按钮
        StaticPane bottomPane = new StaticPane(1, 5, 7, 1);

        // 统计报告
        ItemStack reportItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§d权限统计")
                .lore("§7查看权限配置统计")
                .build();
        bottomPane.addItem(new GuiItem(reportItem, event -> {
            event.setCancelled(true);
            openStatistics(player);
        }), 0, 0);

        // 返回
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("§c返回")
                .build();
        bottomPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            player.closeInventory();
        }), 6, 0);

        gui.addPane(bottomPane);
        gui.show(player);
    }

    /**
     * 打开带分页的角色编辑器
     * @param page 页数，从0开始
     */
    public void openRoleEditorWithPagination(Player player, SectRank rank, int page) {
        List<SectRoleResidencePermissionWrapper.PermissionItem> allItems = wrapper.getPermissionItemsForGui(rank);
        int totalPages = (int) Math.ceil((double) allItems.size() / PERMISSIONS_PER_PAGE);

        // 页数范围检查
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        // 为lambda表达式保存final变量
        final SectRank finalRank = rank;
        final int finalPage = page;

        // 获取当前页的权限列表
        int startIndex = page * PERMISSIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + PERMISSIONS_PER_PAGE, allItems.size());
        List<SectRoleResidencePermissionWrapper.PermissionItem> pageItems =
                allItems.subList(startIndex, endIndex);

        // 创建GUI
        ChestGui gui = new ChestGui(6, "§e编辑权限 - " + rank.getDisplayName() +
                " §7[" + (page + 1) + "/" + totalPages + "]");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        OutlinePane background = new OutlinePane(0, 0, 9, 6);
        ItemStack border = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        // 标题
        StaticPane titlePane = new StaticPane(1, 0, 7, 1);
        ItemStack titleItem = new ItemBuilder(Material.REDSTONE)
                .name("§6编辑 " + rank.getDisplayName() + " 的权限")
                .lore("§7左键切换权限状态")
                .lore("§7绿色=启用, 红色=禁用, 灰色=未设置")
                .lore("§7页数: " + (page + 1) + "/" + totalPages)
                .build();
        titlePane.addItem(new GuiItem(titleItem), 2, 0);
        gui.addPane(titlePane);

        // 权限列表
        OutlinePane permPane = new OutlinePane(1, 1, 7, 4);

        for (SectRoleResidencePermissionWrapper.PermissionItem item : pageItems) {
            final SectRoleResidencePermissionWrapper.PermissionItem finalItem = item;
            ItemStack permItem = createPermissionItem(rank, finalItem);

            permPane.addItem(new GuiItem(permItem, event -> {
                event.setCancelled(true);
                // 切换权限状态
                FlagState newState = wrapper.toggleRolePermission(finalRank, finalItem.flagName);
                player.sendMessage("§7[权限] " + finalItem.displayName + " → " + getStateDisplay(newState));

                // 刷新编辑器(保持在同一页)
                openRoleEditorWithPagination(player, finalRank, finalPage);
            }));
        }

        gui.addPane(permPane);

        // 底部导航和按钮
        StaticPane bottomPane = new StaticPane(0, 5, 9, 1);
        final int finalTotalPages = totalPages;

        // 上一页按钮
        if (page > 0) {
            ItemStack prevItem = new ItemBuilder(Material.ARROW)
                    .name("§a上一页")
                    .lore("§7点击查看前一页权限")
                    .build();
            bottomPane.addItem(new GuiItem(prevItem, event -> {
                event.setCancelled(true);
                openRoleEditorWithPagination(player, finalRank, finalPage - 1);
            }), 0, 0);
        }

        // 页数显示
        ItemStack pageItem = new ItemBuilder(Material.PAPER)
                .name("§7页数: §f" + (page + 1) + "§7/§f" + finalTotalPages)
                .lore("§7总权限数: " + allItems.size())
                .lore("§7当前页: " + (startIndex + 1) + "-" + endIndex)
                .build();
        bottomPane.addItem(new GuiItem(pageItem), 4, 0);

        // 下一页按钮
        if (page < totalPages - 1) {
            ItemStack nextItem = new ItemBuilder(Material.ARROW)
                    .name("§a下一页")
                    .lore("§7点击查看下一页权限")
                    .build();
            bottomPane.addItem(new GuiItem(nextItem, event -> {
                event.setCancelled(true);
                openRoleEditorWithPagination(player, finalRank, finalPage + 1);
            }), 8, 0);
        }

        // 应用权限按钮
        ItemStack applyItem = new ItemBuilder(Material.EMERALD_BLOCK)
                .name("§a应用权限")
                .lore("§7将权限应用到所有 " + rank.getDisplayName() + " 成员")
                .glow()
                .build();
        bottomPane.addItem(new GuiItem(applyItem, event -> {
            event.setCancelled(true);
            wrapper.applyRolePermissionsToAllMembers(finalRank);
            player.sendMessage("§a✓ 权限已应用！");
            openMainMenu(player);
        }), 2, 0);

        // 返回主菜单
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .name("§c返回主菜单")
                .build();
        bottomPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            openMainMenu(player);
        }), 6, 0);

        gui.addPane(bottomPane);
        gui.show(player);
    }

    /**
     * 创建角色卡片
     */
    private ItemStack createRoleCard(SectRank rank) {
        int memberCount = sect.getMembersByRank(rank).size();

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§7成员数: §f" + memberCount);
        lore.add("");

        // 权限启用统计
        java.util.Map<String, Object> stats = wrapper.getRolePermissionStatistics(rank);
        int enabled = (int) stats.get("enabled");
        int total = (int) stats.get("total");

        lore.add("§7权限启用: §f" + enabled + "§7/§f" + total);
        lore.add("§7启用率: §f" + (total > 0 ? (enabled * 100 / total) : 0) + "%");
        lore.add("");
        lore.add("§e点击编辑权限");

        return new ItemBuilder(getRankMaterial(rank))
                .name(rank.getDisplayName())
                .lore(lore.toArray(new String[0]))
                .build();
    }

    /**
     * 创建权限项
     */
    private ItemStack createPermissionItem(SectRank rank, SectRoleResidencePermissionWrapper.PermissionItem item) {
        Material material;
        String stateStr;

        switch (item.state) {
            case TRUE:
                material = Material.LIME_CONCRETE;
                stateStr = "§a启用";
                break;
            case FALSE:
                material = Material.RED_CONCRETE;
                stateStr = "§c禁用";
                break;
            case NEITHER:
                material = Material.GRAY_CONCRETE;
                stateStr = "§7未设置";
                break;
            default:
                material = Material.BARRIER;
                stateStr = "§c错误";
        }

        ItemStack itemStack = new ItemBuilder(material)
                .name(item.displayName + " " + stateStr)
                .lore("§7" + item.description)
                .lore("")
                .lore("§e点击切换状态")
                .build();

        return itemStack;
    }

    /**
     * 打开统计页面
     */
    private void openStatistics(Player player) {
        ChestGui gui = new ChestGui(4, "§d权限配置统计");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        OutlinePane background = new OutlinePane(0, 0, 9, 4);
        ItemStack border = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        StaticPane statsPane = new StaticPane(1, 0, 7, 3);

        int index = 0;
        for (SectRank rank : SectRank.values()) {
            java.util.Map<String, Object> stats = wrapper.getRolePermissionStatistics(rank);
            int enabled = (int) stats.get("enabled");
            int total = (int) stats.get("total");

            ItemStack statItem = new ItemBuilder(Material.PAPER)
                    .name(rank.getDisplayName())
                    .lore("§7启用权限: §f" + enabled + "§7/§f" + total)
                    .lore("§7启用率: §f" + (total > 0 ? (enabled * 100 / total) : 0) + "%")
                    .build();

            int col = index % 7;
            int row = index / 7;
            statsPane.addItem(new GuiItem(statItem), col, row);
            index++;
        }
        gui.addPane(statsPane);

        // 返回按钮
        StaticPane bottomPane = new StaticPane(3, 3, 3, 1);
        ItemStack backItem = new ItemBuilder(Material.ARROW).name("§c返回").build();
        bottomPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            openMainMenu(player);
        }), 1, 0);
        gui.addPane(bottomPane);

        gui.show(player);
    }

    /**
     * 获取权限状态的显示文本
     */
    private String getStateDisplay(FlagState state) {
        switch (state) {
            case TRUE:
                return "§a启用";
            case FALSE:
                return "§c禁用";
            case NEITHER:
                return "§7未设置";
            default:
                return "§c错误";
        }
    }

    /**
     * 获取角色的材料
     */
    private Material getRankMaterial(SectRank rank) {
        return switch (rank) {
            case LEADER -> Material.DIAMOND_BLOCK;
            case ELDER -> Material.GOLD_BLOCK;
            case CORE_DISCIPLE -> Material.IRON_BLOCK;
            case INNER_DISCIPLE -> Material.STONE;
            case OUTER_DISCIPLE -> Material.COBBLESTONE;
        };
    }
}
