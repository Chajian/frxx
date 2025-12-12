package com.xiancore.systems.boss.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.permission.BossPermission;
import com.xiancore.systems.boss.permission.BossPermissionManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * Boss系统管理员GUI
 * 提供游戏内可视化管理界面
 * 使用 InventoryFramework 统一 GUI 框架
 *
 * @author XianCore Team
 * @version 2.0.0 - 统一使用 IF 框架
 */
public class BossAdminGUI {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;
    private final BossPermissionManager permissionManager;

    public BossAdminGUI(XianCore plugin, BossRefreshManager bossManager, BossPermissionManager permissionManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.permissionManager = permissionManager;
    }

    /**
     * 打开主管理菜单
     */
    public void openMainMenu(Player player) {
        if (!player.isOp() && !permissionManager.hasPermission(player, BossPermission.ADMIN)) {
            player.sendMessage("§c你没有权限使用管理界面！");
            return;
        }

        ChestGui gui = new ChestGui(3, "§6§lBoss管理面板");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 3);

        StaticPane contentPane = new StaticPane(0, 0, 9, 3);

        // 刷新点管理
        ItemStack spawnPointsBtn = new ItemBuilder(Material.SPAWNER)
                .name("§e§l刷新点管理")
                .lore(
                        "§7管理Boss刷新点",
                        "",
                        "§a✓ §7添加/删除刷新点",
                        "§a✓ §7编辑刷新点配置",
                        "§a✓ §7查看刷新点状态",
                        "",
                        "§e点击进入"
                )
                .build();
        contentPane.addItem(new GuiItem(spawnPointsBtn, event -> openSpawnPointsMenu(player)), 1, 1);

        // Boss管理
        ItemStack bossManageBtn = new ItemBuilder(Material.DRAGON_HEAD)
                .name("§c§lBoss管理")
                .lore(
                        "§7管理活跃Boss",
                        "",
                        "§a✓ §7查看活跃Boss",
                        "§a✓ §7手动生成Boss",
                        "§a✓ §7清除Boss",
                        "",
                        "§e点击进入"
                )
                .build();
        contentPane.addItem(new GuiItem(bossManageBtn, event -> openBossManageMenu(player)), 3, 1);

        // 权限管理
        ItemStack permissionsBtn = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§b§l权限管理")
                .lore(
                        "§7管理玩家权限",
                        "",
                        "§a✓ §7查看玩家权限",
                        "§a✓ §7权限检查",
                        "",
                        "§e点击进入"
                )
                .build();
        contentPane.addItem(new GuiItem(permissionsBtn, event -> openPermissionsMenu(player)), 5, 1);

        // 系统统计
        ItemStack statsBtn = new ItemBuilder(Material.BOOK)
                .name("§d§l系统统计")
                .lore(
                        "§7查看系统状态",
                        "",
                        "§a✓ §7刷新统计",
                        "§a✓ §7传送统计",
                        "§a✓ §7奖励统计",
                        "",
                        "§e点击进入"
                )
                .build();
        contentPane.addItem(new GuiItem(statsBtn, event -> openStatsMenu(player)), 7, 1);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 2);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 打开刷新点管理菜单
     */
    public void openSpawnPointsMenu(Player player) {
        ChestGui gui = new ChestGui(6, "§6§l刷新点管理");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 添加刷新点
        ItemStack addBtn = new ItemBuilder(Material.EMERALD)
                .name("§a§l添加刷新点")
                .lore(
                        "§7添加新的Boss刷新点",
                        "",
                        "§e点击后输入命令:",
                        "§7/boss add <id> <mob>",
                        "",
                        "§c需要管理员权限"
                )
                .build();
        contentPane.addItem(new GuiItem(addBtn, event -> {
            player.closeInventory();
            player.sendMessage("§e请使用命令: §7/boss add <id> <mob>");
        }), 1, 1);

        // 刷新点列表
        ItemStack listBtn = new ItemBuilder(Material.PAPER)
                .name("§e§l刷新点列表")
                .lore(
                        "§7查看所有刷新点",
                        "",
                        "§a当前刷新点: §f" + bossManager.getAllSpawnPoints().size() + " 个",
                        "",
                        "§e点击查看详情"
                )
                .build();
        contentPane.addItem(new GuiItem(listBtn, event -> {
            player.closeInventory();
            player.performCommand("boss list");
        }), 3, 1);

        // 删除刷新点
        ItemStack deleteBtn = new ItemBuilder(Material.REDSTONE)
                .name("§c§l删除刷新点")
                .lore(
                        "§7删除指定刷新点",
                        "",
                        "§e点击后输入命令:",
                        "§7/boss remove <id>",
                        "",
                        "§c需要管理员权限"
                )
                .build();
        contentPane.addItem(new GuiItem(deleteBtn, event -> {
            player.closeInventory();
            player.sendMessage("§e请使用命令: §7/boss remove <id>");
        }), 5, 1);

        // 编辑刷新点
        ItemStack editBtn = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§b§l编辑刷新点")
                .lore(
                        "§7编辑刷新点配置",
                        "",
                        "§a✓ §7修改刷新间隔",
                        "§a✓ §7修改Boss等级",
                        "§a✓ §7启用/禁用刷新点",
                        "",
                        "§e点击查看列表"
                )
                .build();
        contentPane.addItem(new GuiItem(editBtn, event -> {
            player.closeInventory();
            player.sendMessage("§e请使用命令: §7/boss edit <id> <key> <value>");
        }), 7, 1);

        // 返回按钮
        ItemStack backBtn = new ItemBuilder(Material.ARROW)
                .name("§e§l返回主菜单")
                .build();
        contentPane.addItem(new GuiItem(backBtn, event -> openMainMenu(player)), 0, 5);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 打开Boss管理菜单
     */
    public void openBossManageMenu(Player player) {
        int activeBosses = bossManager.getActiveBosses().size();

        ChestGui gui = new ChestGui(6, "§6§lBoss管理");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 查看活跃Boss
        ItemStack viewBtn = new ItemBuilder(Material.ENDER_EYE)
                .name("§a§l查看活跃Boss")
                .lore(
                        "§7查看当前活跃的Boss",
                        "",
                        "§a活跃Boss: §f" + activeBosses + " 个",
                        "",
                        "§e点击查看列表"
                )
                .build();
        contentPane.addItem(new GuiItem(viewBtn, event -> {
            player.closeInventory();
            player.performCommand("boss list");
        }), 1, 1);

        // 手动生成Boss
        ItemStack spawnBtn = new ItemBuilder(Material.NETHER_STAR)
                .name("§e§l手动生成Boss")
                .lore(
                        "§7手动在指定位置生成Boss",
                        "",
                        "§e使用命令:",
                        "§7/boss spawn <id>",
                        "",
                        "§c需要管理员权限"
                )
                .glow()
                .build();
        contentPane.addItem(new GuiItem(spawnBtn, event -> {
            player.closeInventory();
            player.sendMessage("§e请使用命令: §7/boss spawn <id>");
        }), 3, 1);

        // 清除全部Boss
        ItemStack clearBtn = new ItemBuilder(Material.TNT)
                .name("§c§l清除全部Boss")
                .lore(
                        "§7清除所有活跃的Boss",
                        "",
                        "§c警告: 此操作不可撤销!",
                        "",
                        "§e点击执行"
                )
                .build();
        contentPane.addItem(new GuiItem(clearBtn, event -> {
            player.closeInventory();
            player.sendMessage("§c此功能暂未实现");
        }), 5, 1);

        // 刷新统计
        ItemStack statsBtn = new ItemBuilder(Material.CLOCK)
                .name("§b§l刷新统计")
                .lore(
                        "§7查看Boss刷新统计",
                        "",
                        "§a总刷新点: §f" + bossManager.getSpawnPoints().size(),
                        "§a活跃Boss: §f" + activeBosses,
                        "",
                        "§e点击查看详情"
                )
                .build();
        contentPane.addItem(new GuiItem(statsBtn, event -> {
            player.closeInventory();
            player.performCommand("boss stats");
        }), 7, 1);

        // 返回按钮
        ItemStack backBtn = new ItemBuilder(Material.ARROW)
                .name("§e§l返回主菜单")
                .build();
        contentPane.addItem(new GuiItem(backBtn, event -> openMainMenu(player)), 0, 5);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 打开权限管理菜单
     */
    public void openPermissionsMenu(Player player) {
        ChestGui gui = new ChestGui(6, "§6§l权限管理");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 查看玩家权限
        ItemStack viewPermBtn = new ItemBuilder(Material.PLAYER_HEAD)
                .name("§a§l查看玩家权限")
                .lore(
                        "§7查看指定玩家的权限",
                        "",
                        "§e使用命令:",
                        "§7/boss perm list <player>",
                        "",
                        "§c需要管理员权限"
                )
                .build();
        contentPane.addItem(new GuiItem(viewPermBtn, event -> {
            player.closeInventory();
            player.sendMessage("§e请使用命令: §7/boss perm list <player>");
        }), 1, 1);

        // 权限检查
        ItemStack checkBtn = new ItemBuilder(Material.COMPASS)
                .name("§e§l权限检查")
                .lore(
                        "§7检查玩家特定权限",
                        "",
                        "§e使用命令:",
                        "§7/boss perm check <player> <perm>",
                        "",
                        "§c需要管理员权限"
                )
                .build();
        contentPane.addItem(new GuiItem(checkBtn, event -> {
            player.closeInventory();
            player.sendMessage("§e请使用命令: §7/boss perm check <player> <perm>");
        }), 3, 1);

        // 权限节点列表
        ItemStack nodeListBtn = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§b§l权限节点列表")
                .lore(
                        "§7查看所有Boss权限节点",
                        "",
                        "§a管理权限:",
                        "§7  - boss.admin",
                        "",
                        "§a基础权限:",
                        "§7  - boss.view",
                        "§7  - boss.announce",
                        "§7  - boss.teleport",
                        "",
                        "§aVIP权限:",
                        "§7  - boss.teleport.free",
                        "§7  - boss.teleport.nocooldown"
                )
                .build();
        contentPane.addItem(new GuiItem(nodeListBtn, event -> {
            player.sendMessage("§a=== Boss权限节点列表 ===");
            player.sendMessage("§e管理权限: §7boss.admin");
            player.sendMessage("§e基础权限: §7boss.view, boss.announce, boss.teleport");
            player.sendMessage("§eVIP权限: §7boss.teleport.free, boss.teleport.nocooldown");
        }), 5, 1);

        // 权限说明
        ItemStack helpBtn = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("§d§l权限说明")
                .lore(
                        "§7如何配置权限",
                        "",
                        "§e推荐使用LuckPerms:",
                        "§7/lp user <player> permission set <权限>",
                        "",
                        "§c权限由权限插件管理",
                        "§c此GUI仅用于查看"
                )
                .glow()
                .build();
        contentPane.addItem(new GuiItem(helpBtn, event -> {
            player.sendMessage("§a=== 权限配置说明 ===");
            player.sendMessage("§7推荐使用LuckPerms插件管理权限");
            player.sendMessage("§e示例: §7/lp user Steve permission set boss.teleport.free true");
        }), 7, 1);

        // 返回按钮
        ItemStack backBtn = new ItemBuilder(Material.ARROW)
                .name("§e§l返回主菜单")
                .build();
        contentPane.addItem(new GuiItem(backBtn, event -> openMainMenu(player)), 0, 5);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 打开系统统计菜单
     */
    public void openStatsMenu(Player player) {
        int spawnPoints = bossManager.getAllSpawnPoints().size();
        int activeBosses = bossManager.getActiveBosses().size();
        int totalSpawned = bossManager.getTotalBossesSpawned();
        int totalKilled = bossManager.getTotalBossesKilled();

        ChestGui gui = new ChestGui(6, "§6§l系统统计");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 刷新点统计
        ItemStack spawnStatsBtn = new ItemBuilder(Material.SPAWNER)
                .name("§e§l刷新点统计")
                .lore(
                        "§7刷新系统状态",
                        "",
                        "§a总刷新点: §f" + spawnPoints,
                        "§a活跃Boss: §f" + activeBosses,
                        "§a已生成: §f" + totalSpawned,
                        "§a已击杀: §f" + totalKilled
                )
                .build();
        contentPane.addItem(new GuiItem(spawnStatsBtn), 2, 1);

        // 传送统计
        ItemStack tpStatsBtn = new ItemBuilder(Material.ENDER_PEARL)
                .name("§b§l传送统计")
                .lore(
                        "§7传送系统状态",
                        "",
                        "§7功能待实现..."
                )
                .build();
        contentPane.addItem(new GuiItem(tpStatsBtn), 4, 1);

        // 奖励统计
        ItemStack rewardStatsBtn = new ItemBuilder(Material.DIAMOND)
                .name("§d§l奖励统计")
                .lore(
                        "§7奖励系统状态",
                        "",
                        "§7功能待实现..."
                )
                .build();
        contentPane.addItem(new GuiItem(rewardStatsBtn), 6, 1);

        // 系统信息
        ItemStack sysInfoBtn = new ItemBuilder(Material.REDSTONE)
                .name("§c§l系统信息")
                .lore(
                        "§7系统运行状态",
                        "",
                        "§a版本: §fv1.0.0",
                        "§a状态: §f正常运行",
                        "",
                        "§7内存使用情况...",
                        "§7线程池状态..."
                )
                .build();
        contentPane.addItem(new GuiItem(sysInfoBtn), 4, 2);

        // 返回按钮
        ItemStack backBtn = new ItemBuilder(Material.ARROW)
                .name("§e§l返回主菜单")
                .build();
        contentPane.addItem(new GuiItem(backBtn, event -> openMainMenu(player)), 0, 5);

        // 刷新按钮
        ItemStack refreshBtn = new ItemBuilder(Material.COMPASS)
                .name("§e§l刷新统计")
                .lore("§7点击刷新统计数据")
                .build();
        contentPane.addItem(new GuiItem(refreshBtn, event -> {
            openStatsMenu(player);
            player.sendMessage("§a已刷新统计数据！");
        }), 4, 5);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }
}
