package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.integration.residence.ResidencePermissionManager;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 宗门成员管理 GUI
 * 提供成员列表、权限查看、职位管理等功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectMemberGUI {

    private final XianCore plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public SectMemberGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开成员列表主界面
     */
    public void openMemberList(Player player, Sect sect) {
        ChestGui gui = new ChestGui(6, "§a§l宗门成员管理");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景边框
        GUIUtils.addBackground(gui, 6, Material.GREEN_STAINED_GLASS_PANE);

        // 分页面板 - 显示成员列表
        PaginatedPane memberPane = new PaginatedPane(1, 1, 7, 4);
        memberPane.populateWithGuiItems(createMemberItems(player, sect));
        gui.addPane(memberPane);

        // 底部控制面板
        StaticPane controlPane = new StaticPane(0, 5, 9, 1);

        // 统计信息
        ItemStack statsItem = new ItemBuilder(Material.BOOK)
            .name("§e§l成员统计")
            .lore(
                "§7总成员数: §f" + sect.getMemberCount() + "§7/§f" + sect.getMaxMembers(),
                "§7宗主: §6" + sect.getMembersByRank(SectRank.LEADER).size(),
                "§7长老: §b" + sect.getMembersByRank(SectRank.ELDER).size(),
                "§7核心弟子: §a" + sect.getMembersByRank(SectRank.CORE_DISCIPLE).size(),
                "§7内门弟子: §e" + sect.getMembersByRank(SectRank.INNER_DISCIPLE).size(),
                "§7外门弟子: §7" + sect.getMembersByRank(SectRank.OUTER_DISCIPLE).size()
            )
            .build();
        controlPane.addItem(new GuiItem(statsItem), 1, 0);

        // 权限信息
        if (sect.hasLand()) {
            String[] statsLines = plugin.getSectSystem().getPermissionManager()
                .getPermissionStatistics(sect.getId()).split("\n");
            
            List<String> loreList = new ArrayList<>();
            loreList.add("§7领地ID: §f" + sect.getResidenceLandId());
            loreList.add("");
            loreList.addAll(Arrays.asList(statsLines));
            
            ItemStack permItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§d§l权限统计")
                .lore(loreList)
                .build();
            controlPane.addItem(new GuiItem(permItem), 3, 0);
        }

        // 上一页
        if (memberPane.getPage() > 0) {
            ItemStack prevItem = new ItemBuilder(Material.ARROW)
                .name("§7上一页")
                .build();
            controlPane.addItem(new GuiItem(prevItem, event -> {
                memberPane.setPage(memberPane.getPage() - 1);
                gui.update();
            }), 5, 0);
        }

        // 下一页
        if (memberPane.getPage() < memberPane.getPages() - 1) {
            ItemStack nextItem = new ItemBuilder(Material.ARROW)
                .name("§7下一页")
                .build();
            controlPane.addItem(new GuiItem(nextItem, event -> {
                memberPane.setPage(memberPane.getPage() + 1);
                gui.update();
            }), 6, 0);
        }

        // 关闭按钮
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
            .name("§c关闭")
            .build();
        controlPane.addItem(new GuiItem(closeItem, event -> player.closeInventory()), 7, 0);

        gui.addPane(controlPane);
        gui.show(player);
    }

    /**
     * 创建成员项列表
     */
    private List<GuiItem> createMemberItems(Player viewer, Sect sect) {
        List<GuiItem> items = new ArrayList<>();
        List<SectMember> members = new ArrayList<>(sect.getMemberList());

        // 按职位排序（高到低）
        members.sort(Comparator.comparing((SectMember m) -> m.getRank().getLevel()).reversed());

        for (SectMember member : members) {
            items.add(createMemberItem(viewer, sect, member));
        }

        return items;
    }

    /**
     * 创建单个成员项
     */
    private GuiItem createMemberItem(Player viewer, Sect sect, SectMember member) {
        SectRank rank = member.getRank();
        Material material = getMaterialForRank(rank);

        List<String> lore = new ArrayList<>();
        lore.add("§7玩家: §f" + member.getPlayerName());
        lore.add("§7职位: " + rank.getColorCode() + rank.getDisplayName());
        lore.add("§7贡献: §6" + member.getContribution());
        lore.add("§7加入时间: §f" + dateFormat.format(new Date(member.getJoinedAt())));
        lore.add("");

        // 活跃度信息
        long daysSinceActive = (System.currentTimeMillis() - member.getLastActiveAt()) / (1000 * 60 * 60 * 24);
        if (daysSinceActive > 30) {
            lore.add("§c长期未活跃 (" + daysSinceActive + " 天)");
        } else if (daysSinceActive > 7) {
            lore.add("§e最近活跃: " + daysSinceActive + " 天前");
        } else {
            lore.add("§a活跃成员");
        }

        // 权限信息
        if (sect.hasLand()) {
            ResidencePermissionManager.PermissionLevel permLevel = 
                plugin.getSectSystem().getPermissionManager().getPermissionLevel(rank);
            lore.add("");
            lore.add("§d领地权限: " + permLevel.getDisplayName());
            lore.add("§7" + permLevel.getDescription());
        }

        // 管理操作提示
        SectMember viewerMember = sect.getMember(viewer.getUniqueId());
        if (viewerMember != null && viewerMember.getRank().hasManagePermission()) {
            lore.add("");
            lore.add("§e左键: §7查看详情");
            if (!member.getPlayerId().equals(viewer.getUniqueId())) {
                if (viewerMember.getRank().isHigherThan(rank)) {
                    lore.add("§eShift+左键: §7管理职位");
                    lore.add("§eShift+右键: §c踢出宗门");
                }
            }
        }

        ItemStack item = new ItemBuilder(material)
            .name(rank.getColorCode() + "§l" + member.getPlayerName())
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
     * 获取职位对应的材质
     */
    private Material getMaterialForRank(SectRank rank) {
        return switch (rank) {
            case LEADER -> Material.GOLDEN_HELMET;
            case ELDER -> Material.DIAMOND_HELMET;
            case CORE_DISCIPLE -> Material.IRON_HELMET;
            case INNER_DISCIPLE -> Material.CHAINMAIL_HELMET;
            case OUTER_DISCIPLE -> Material.LEATHER_HELMET;
        };
    }

    /**
     * 处理成员管理操作
     */
    private void handleMemberManagement(Player viewer, Sect sect, SectMember target, boolean isKick) {
        SectMember viewerMember = sect.getMember(viewer.getUniqueId());
        
        if (viewerMember == null || !viewerMember.getRank().hasManagePermission()) {
            viewer.sendMessage("§c你没有权限管理成员!");
            return;
        }

        if (target.getPlayerId().equals(viewer.getUniqueId())) {
            viewer.sendMessage("§c你不能管理自己!");
            return;
        }

        if (!viewerMember.getRank().isHigherThan(target.getRank())) {
            viewer.sendMessage("§c你不能管理职位不低于你的成员!");
            return;
        }

        viewer.closeInventory();

        if (isKick) {
            // 踢出成员
            viewer.sendMessage("§e正在踢出成员 " + target.getPlayerName() + "...");
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                viewer.performCommand("sect kick " + target.getPlayerName());
            }, 1L);
        } else {
            // 打开职位管理界面
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

        // 当前职位
        ItemStack currentItem = new ItemBuilder(Material.NAME_TAG)
            .name("§e当前职位")
            .lore(
                "§7" + target.getRank().getDisplayName(),
                "",
                "§7等级: §f" + target.getRank().getLevel()
            )
            .build();
        pane.addItem(new GuiItem(currentItem), 4, 0);

        // 晋升按钮
        if (target.getRank() != SectRank.LEADER) {
            ItemStack promoteItem = new ItemBuilder(Material.LIME_DYE)
                .name("§a晋升")
                .lore("§7提升成员职位")
                .build();
            pane.addItem(new GuiItem(promoteItem, event -> {
                viewer.closeInventory();
                viewer.performCommand("sect promote " + target.getPlayerName());
            }), 2, 1);
        }

        // 降职按钮
        if (target.getRank() != SectRank.OUTER_DISCIPLE) {
            ItemStack demoteItem = new ItemBuilder(Material.ORANGE_DYE)
                .name("§6降职")
                .lore("§7降低成员职位")
                .build();
            pane.addItem(new GuiItem(demoteItem, event -> {
                viewer.closeInventory();
                viewer.performCommand("sect demote " + target.getPlayerName());
            }), 6, 1);
        }

        // 返回按钮
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
        ChestGui gui = new ChestGui(4, "§b§l成员详情 - " + member.getPlayerName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(0, 0, 9, 4);

        // 基本信息
        ItemStack infoItem = new ItemBuilder(Material.PLAYER_HEAD)
            .name("§b" + member.getPlayerName())
            .lore(
                "§7UUID: §f" + member.getPlayerId().toString().substring(0, 8) + "...",
                "§7职位: " + member.getRank().getColorCode() + member.getRank().getDisplayName(),
                "§7贡献: §6" + member.getContribution(),
                "",
                "§7加入时间: §f" + dateFormat.format(new Date(member.getJoinedAt())),
                "§7最后活跃: §f" + dateFormat.format(new Date(member.getLastActiveAt()))
            )
            .build();
        pane.addItem(new GuiItem(infoItem), 4, 1);

        // 权限详情
        if (sect.hasLand()) {
            ResidencePermissionManager permManager = plugin.getSectSystem().getPermissionManager();
            ResidencePermissionManager.PermissionLevel permLevel = permManager.getPermissionLevel(member.getRank());
            
            List<String> permLore = new ArrayList<>();
            permLore.add("§7等级: " + permLevel.getDisplayName());
            permLore.add("§7" + permLevel.getDescription());
            permLore.add("");
            permLore.add("§e权限详情:");
            
            // 从配置读取该等级的 flags
            List<String> flags = permManager.getFlagsForLevel(permLevel);
            for (String flag : flags) {
                permLore.add("§7- §a" + flag);
            }

            ItemStack permItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§d领地权限")
                .lore(permLore.toArray(new String[0]))
                .build();
            pane.addItem(new GuiItem(permItem), 2, 1);
        }

        // 贡献记录
        ItemStack contribItem = new ItemBuilder(Material.GOLD_INGOT)
            .name("§6贡献记录")
            .lore(
                "§7总贡献: §6" + member.getContribution(),
                "",
                "§7贡献可用于:",
                "§7- 宗门商店兑换",
                "§7- 提升职位参考",
                "§7- 领地权限依据"
            )
            .build();
        pane.addItem(new GuiItem(contribItem), 6, 1);

        // 返回按钮
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
