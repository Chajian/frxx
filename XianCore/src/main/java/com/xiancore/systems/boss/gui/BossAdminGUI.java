package com.xiancore.systems.boss.gui;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.permission.BossPermission;
import com.xiancore.systems.boss.permission.BossPermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Boss系统管理员GUI
 * 提供游戏内可视化管理界面
 *
 * 功能:
 * - 刷新点管理
 * - Boss管理
 * - 权限管理
 * - 系统统计
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
public class BossAdminGUI implements Listener {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;
    private final BossPermissionManager permissionManager;
    
    private static final String TITLE_MAIN = "§6§lBoss管理面板";
    private static final String TITLE_SPAWN_POINTS = "§6§l刷新点管理";
    private static final String TITLE_BOSS_MANAGE = "§6§lBoss管理";
    private static final String TITLE_PERMISSIONS = "§6§l权限管理";
    private static final String TITLE_STATS = "§6§l系统统计";

    public BossAdminGUI(XianCore plugin, BossRefreshManager bossManager, BossPermissionManager permissionManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.permissionManager = permissionManager;
    }

    // ==================== 主菜单 ====================

    /**
     * 打开主管理菜单
     */
    public void openMainMenu(Player player) {
        // 权限检查
        if (!player.isOp() && !permissionManager.hasPermission(player, BossPermission.ADMIN)) {
            player.sendMessage("§c你没有权限使用管理界面！");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);

        // 背景装饰
        fillBorders(inv, 27);

        // 刷新点管理
        inv.setItem(10, createItem(
            Material.SPAWNER,
            "§e§l刷新点管理",
            Arrays.asList(
                "§7管理Boss刷新点",
                "",
                "§a✓ §7添加/删除刷新点",
                "§a✓ §7编辑刷新点配置",
                "§a✓ §7查看刷新点状态",
                "",
                "§e点击进入"
            )
        ));

        // Boss管理
        inv.setItem(12, createItem(
            Material.DRAGON_HEAD,
            "§c§lBoss管理",
            Arrays.asList(
                "§7管理活跃Boss",
                "",
                "§a✓ §7查看活跃Boss",
                "§a✓ §7手动生成Boss",
                "§a✓ §7清除Boss",
                "",
                "§e点击进入"
            )
        ));

        // 权限管理
        inv.setItem(14, createItem(
            Material.WRITABLE_BOOK,
            "§b§l权限管理",
            Arrays.asList(
                "§7管理玩家权限",
                "",
                "§a✓ §7查看玩家权限",
                "§a✓ §7权限检查",
                "",
                "§e点击进入"
            )
        ));

        // 系统统计
        inv.setItem(16, createItem(
            Material.BOOK,
            "§d§l系统统计",
            Arrays.asList(
                "§7查看系统状态",
                "",
                "§a✓ §7刷新统计",
                "§a✓ §7传送统计",
                "§a✓ §7奖励统计",
                "",
                "§e点击进入"
            )
        ));

        // 关闭按钮
        inv.setItem(26, createItem(Material.BARRIER, "§c§l关闭", null));

        player.openInventory(inv);
    }

    // ==================== 刷新点管理 ====================

    /**
     * 打开刷新点管理菜单
     */
    public void openSpawnPointsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SPAWN_POINTS);

        // 背景装饰
        fillBorders(inv, 54);

        // 功能按钮
        inv.setItem(10, createItem(
            Material.EMERALD,
            "§a§l添加刷新点",
            Arrays.asList(
                "§7添加新的Boss刷新点",
                "",
                "§e点击后输入命令:",
                "§7/boss add <id> <mob>",
                "",
                "§c需要管理员权限"
            )
        ));

        inv.setItem(12, createItem(
            Material.PAPER,
            "§e§l刷新点列表",
            Arrays.asList(
                "§7查看所有刷新点",
                "",
                "§a当前刷新点: §f" + bossManager.getAllSpawnPoints().size() + " 个",
                "",
                "§e点击查看详情"
            )
        ));

        inv.setItem(14, createItem(
            Material.REDSTONE,
            "§c§l删除刷新点",
            Arrays.asList(
                "§7删除指定刷新点",
                "",
                "§e点击后输入命令:",
                "§7/boss remove <id>",
                "",
                "§c需要管理员权限"
            )
        ));

        inv.setItem(16, createItem(
            Material.WRITABLE_BOOK,
            "§b§l编辑刷新点",
            Arrays.asList(
                "§7编辑刷新点配置",
                "",
                "§a✓ §7修改刷新间隔",
                "§a✓ §7修改Boss等级",
                "§a✓ §7启用/禁用刷新点",
                "",
                "§e点击查看列表"
            )
        ));

        // 返回按钮
        inv.setItem(45, createItem(Material.ARROW, "§e§l返回主菜单", null));

        // 关闭按钮
        inv.setItem(53, createItem(Material.BARRIER, "§c§l关闭", null));

        player.openInventory(inv);
    }

    // ==================== Boss管理 ====================

    /**
     * 打开Boss管理菜单
     */
    public void openBossManageMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_BOSS_MANAGE);

        // 背景装饰
        fillBorders(inv, 54);

        // 活跃Boss数量
        int activeBosses = bossManager.getActiveBosses().size();

        // 功能按钮
        inv.setItem(10, createItem(
            Material.ENDER_EYE,
            "§a§l查看活跃Boss",
            Arrays.asList(
                "§7查看当前活跃的Boss",
                "",
                "§a活跃Boss: §f" + activeBosses + " 个",
                "",
                "§e点击查看列表"
            )
        ));

        inv.setItem(12, createItem(
            Material.NETHER_STAR,
            "§e§l手动生成Boss",
            Arrays.asList(
                "§7手动在指定位置生成Boss",
                "",
                "§e使用命令:",
                "§7/boss spawn <id>",
                "",
                "§c需要管理员权限"
            )
        ));

        inv.setItem(14, createItem(
            Material.TNT,
            "§c§l清除全部Boss",
            Arrays.asList(
                "§7清除所有活跃的Boss",
                "",
                "§c警告: 此操作不可撤销!",
                "",
                "§e点击执行"
            )
        ));

        inv.setItem(16, createItem(
            Material.CLOCK,
            "§b§l刷新统计",
            Arrays.asList(
                "§7查看Boss刷新统计",
                "",
                "§a总刷新点: §f" + bossManager.getSpawnPoints().size(),
                "§a活跃Boss: §f" + activeBosses,
                "",
                "§e点击查看详情"
            )
        ));

        // 返回按钮
        inv.setItem(45, createItem(Material.ARROW, "§e§l返回主菜单", null));

        // 关闭按钮
        inv.setItem(53, createItem(Material.BARRIER, "§c§l关闭", null));

        player.openInventory(inv);
    }

    // ==================== 权限管理 ====================

    /**
     * 打开权限管理菜单
     */
    public void openPermissionsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PERMISSIONS);

        // 背景装饰
        fillBorders(inv, 54);

        // 功能按钮
        inv.setItem(10, createItem(
            Material.PLAYER_HEAD,
            "§a§l查看玩家权限",
            Arrays.asList(
                "§7查看指定玩家的权限",
                "",
                "§e使用命令:",
                "§7/boss perm list <player>",
                "",
                "§c需要管理员权限"
            )
        ));

        inv.setItem(12, createItem(
            Material.COMPASS,
            "§e§l权限检查",
            Arrays.asList(
                "§7检查玩家特定权限",
                "",
                "§e使用命令:",
                "§7/boss perm check <player> <perm>",
                "",
                "§c需要管理员权限"
            )
        ));

        inv.setItem(14, createItem(
            Material.WRITABLE_BOOK,
            "§b§l权限节点列表",
            Arrays.asList(
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
        ));

        inv.setItem(16, createItem(
            Material.ENCHANTED_BOOK,
            "§d§l权限说明",
            Arrays.asList(
                "§7如何配置权限",
                "",
                "§e推荐使用LuckPerms:",
                "§7/lp user <player> permission set <权限>",
                "",
                "§c权限由权限插件管理",
                "§c此GUI仅用于查看"
            )
        ));

        // 返回按钮
        inv.setItem(45, createItem(Material.ARROW, "§e§l返回主菜单", null));

        // 关闭按钮
        inv.setItem(53, createItem(Material.BARRIER, "§c§l关闭", null));

        player.openInventory(inv);
    }

    // ==================== 系统统计 ====================

    /**
     * 打开系统统计菜单
     */
    public void openStatsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_STATS);

        // 背景装饰
        fillBorders(inv, 54);

        // 统计信息
        int spawnPoints = bossManager.getAllSpawnPoints().size();
        int activeBosses = bossManager.getActiveBosses().size();
        int totalSpawned = bossManager.getTotalBossesSpawned();
        int totalKilled = bossManager.getTotalBossesKilled();

        // 统计卡片
        inv.setItem(11, createItem(
            Material.SPAWNER,
            "§e§l刷新点统计",
            Arrays.asList(
                "§7刷新系统状态",
                "",
                "§a总刷新点: §f" + spawnPoints,
                "§a活跃Boss: §f" + activeBosses,
                "§a已生成: §f" + totalSpawned,
                "§a已击杀: §f" + totalKilled
            )
        ));

        inv.setItem(13, createItem(
            Material.ENDER_PEARL,
            "§b§l传送统计",
            Arrays.asList(
                "§7传送系统状态",
                "",
                "§7功能待实现..."
            )
        ));

        inv.setItem(15, createItem(
            Material.DIAMOND,
            "§d§l奖励统计",
            Arrays.asList(
                "§7奖励系统状态",
                "",
                "§7功能待实现..."
            )
        ));

        inv.setItem(22, createItem(
            Material.REDSTONE,
            "§c§l系统信息",
            Arrays.asList(
                "§7系统运行状态",
                "",
                "§a版本: §fv1.0.0",
                "§a状态: §f正常运行",
                "",
                "§7内存使用情况...",
                "§7线程池状态..."
            )
        ));

        // 返回按钮
        inv.setItem(45, createItem(Material.ARROW, "§e§l返回主菜单", null));

        // 刷新按钮
        inv.setItem(49, createItem(Material.COMPASS, "§e§l刷新统计", 
            Arrays.asList("§7点击刷新统计数据")));

        // 关闭按钮
        inv.setItem(53, createItem(Material.BARRIER, "§c§l关闭", null));

        player.openInventory(inv);
    }

    // ==================== 事件处理 ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith("§6§lBoss")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();

        // 主菜单
        if (title.equals(TITLE_MAIN)) {
            handleMainMenuClick(player, slot);
        }
        // 刷新点管理
        else if (title.equals(TITLE_SPAWN_POINTS)) {
            handleSpawnPointsMenuClick(player, slot);
        }
        // Boss管理
        else if (title.equals(TITLE_BOSS_MANAGE)) {
            handleBossManageMenuClick(player, slot);
        }
        // 权限管理
        else if (title.equals(TITLE_PERMISSIONS)) {
            handlePermissionsMenuClick(player, slot);
        }
        // 系统统计
        else if (title.equals(TITLE_STATS)) {
            handleStatsMenuClick(player, slot);
        }
    }

    /**
     * 处理主菜单点击
     */
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 10: // 刷新点管理
                openSpawnPointsMenu(player);
                break;
            case 12: // Boss管理
                openBossManageMenu(player);
                break;
            case 14: // 权限管理
                openPermissionsMenu(player);
                break;
            case 16: // 系统统计
                openStatsMenu(player);
                break;
            case 26: // 关闭
                player.closeInventory();
                break;
        }
    }

    /**
     * 处理刷新点菜单点击
     */
    private void handleSpawnPointsMenuClick(Player player, int slot) {
        switch (slot) {
            case 10: // 添加刷新点
                player.closeInventory();
                player.sendMessage("§e请使用命令: §7/boss add <id> <mob>");
                break;
            case 12: // 查看列表
                player.closeInventory();
                player.performCommand("boss list");
                break;
            case 14: // 删除刷新点
                player.closeInventory();
                player.sendMessage("§e请使用命令: §7/boss remove <id>");
                break;
            case 16: // 编辑刷新点
                player.closeInventory();
                player.sendMessage("§e请使用命令: §7/boss edit <id> <key> <value>");
                break;
            case 45: // 返回
                openMainMenu(player);
                break;
            case 53: // 关闭
                player.closeInventory();
                break;
        }
    }

    /**
     * 处理Boss管理菜单点击
     */
    private void handleBossManageMenuClick(Player player, int slot) {
        switch (slot) {
            case 10: // 查看活跃Boss
                player.closeInventory();
                player.performCommand("boss list");
                break;
            case 12: // 手动生成
                player.closeInventory();
                player.sendMessage("§e请使用命令: §7/boss spawn <id>");
                break;
            case 14: // 清除全部
                player.closeInventory();
                player.sendMessage("§c此功能暂未实现");
                break;
            case 16: // 统计
                player.closeInventory();
                player.performCommand("boss stats");
                break;
            case 45: // 返回
                openMainMenu(player);
                break;
            case 53: // 关闭
                player.closeInventory();
                break;
        }
    }

    /**
     * 处理权限菜单点击
     */
    private void handlePermissionsMenuClick(Player player, int slot) {
        switch (slot) {
            case 10: // 查看玩家权限
                player.closeInventory();
                player.sendMessage("§e请使用命令: §7/boss perm list <player>");
                break;
            case 12: // 权限检查
                player.closeInventory();
                player.sendMessage("§e请使用命令: §7/boss perm check <player> <perm>");
                break;
            case 14: // 权限节点列表
                player.sendMessage("§a=== Boss权限节点列表 ===");
                player.sendMessage("§e管理权限: §7boss.admin");
                player.sendMessage("§e基础权限: §7boss.view, boss.announce, boss.teleport");
                player.sendMessage("§eVIP权限: §7boss.teleport.free, boss.teleport.nocooldown");
                break;
            case 16: // 权限说明
                player.sendMessage("§a=== 权限配置说明 ===");
                player.sendMessage("§7推荐使用LuckPerms插件管理权限");
                player.sendMessage("§e示例: §7/lp user Steve permission set boss.teleport.free true");
                break;
            case 45: // 返回
                openMainMenu(player);
                break;
            case 53: // 关闭
                player.closeInventory();
                break;
        }
    }

    /**
     * 处理统计菜单点击
     */
    private void handleStatsMenuClick(Player player, int slot) {
        switch (slot) {
            case 49: // 刷新
                openStatsMenu(player);
                player.sendMessage("§a已刷新统计数据！");
                break;
            case 45: // 返回
                openMainMenu(player);
                break;
            case 53: // 关闭
                player.closeInventory();
                break;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 填充边框
     */
    private void fillBorders(Inventory inv, int size) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        
        // 填充第一行
        for (int i = 0; i < 9 && i < size; i++) {
            inv.setItem(i, filler);
        }
        
        // 填充最后一行
        for (int i = size - 9; i < size; i++) {
            if (i >= 0) {
                inv.setItem(i, filler);
            }
        }
        
        // 填充左右边框
        for (int i = 9; i < size - 9; i += 9) {
            if (i < size) inv.setItem(i, filler);
            if (i + 8 < size) inv.setItem(i + 8, filler);
        }
    }
}
