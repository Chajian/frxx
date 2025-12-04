package com.xiancore.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.logging.Logger;

/**
 * Boss GUI菜单系统 - 游戏内菜单导航
 * Boss GUI Menu System - In-game menu navigation
 *
 * @author XianCore
 * @version 1.0
 */
public class BossGUIMenu {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, MenuState> playerMenuState; // 玩家当前菜单状态

    /**
     * 菜单状态枚举
     */
    public enum MenuState {
        MAIN,           // 主菜单
        BOSS_LIST,      // Boss列表
        BOSS_DETAIL,    // Boss详情
        BOSS_EDIT,      // Boss编辑
        LOCATION_SELECT,// 位置选择
        STATS,          // 统计显示
        SETTINGS        // 设置菜单
    }

    /**
     * 菜单项数据类
     */
    public static class MenuItem {
        public String name;
        public Material icon;
        public String description;
        public MenuState targetState;
        public int slot;
        public Runnable action;

        public MenuItem(String name, Material icon, String description, int slot) {
            this.name = name;
            this.icon = icon;
            this.description = description;
            this.slot = slot;
            this.targetState = null;
        }

        public MenuItem(String name, Material icon, String description, int slot, MenuState targetState) {
            this.name = name;
            this.icon = icon;
            this.description = description;
            this.slot = slot;
            this.targetState = targetState;
        }
    }

    /**
     * 构造函数
     */
    public BossGUIMenu(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.playerMenuState = new HashMap<>();
    }

    /**
     * 打开主菜单
     */
    public void openMainMenu(Player player) {
        try {
            Inventory menu = Bukkit.createInventory(null, 27, "§6§lBoss管理系统");

            // 菜单项定义
            MenuItem[] items = {
                    new MenuItem("§aBoss列表", Material.ZOMBIE_HEAD, "查看所有Boss", 10, MenuState.BOSS_LIST),
                    new MenuItem("§b创建Boss", Material.EMERALD, "创建新的Boss", 12, MenuState.BOSS_EDIT),
                    new MenuItem("§c统计数据", Material.EXPERIENCE_BOTTLE, "查看系统统计", 14, MenuState.STATS),
                    new MenuItem("§d配置设置", Material.REDSTONE, "管理系统配置", 16, MenuState.SETTINGS),
                    new MenuItem("§4关闭菜单", Material.BARRIER, "关闭当前菜单", 26)
            };

            // 添加菜单项
            for (MenuItem item : items) {
                addMenuItem(menu, item);
            }

            // 记录玩家菜单状态
            playerMenuState.put(player.getUniqueId(), MenuState.MAIN);

            // 打开菜单
            player.openInventory(menu);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了Boss管理菜单");

        } catch (Exception e) {
            logger.severe("§c✗ 打开主菜单失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加菜单项到菜单
     */
    private void addMenuItem(Inventory inventory, MenuItem item) {
        ItemStack menuItem = new ItemStack(item.icon);
        ItemMeta meta = menuItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(item.name);
            if (item.description != null) {
                meta.setLore(Arrays.asList(
                        "§7" + item.description,
                        "§8点击选择"
                ));
            }
            menuItem.setItemMeta(meta);
        }

        inventory.setItem(item.slot, menuItem);
    }

    /**
     * 处理菜单点击事件
     */
    public void handleMenuClick(InventoryClickEvent event) {
        try {
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot < 0 || slot >= 27) {
                return;
            }

            MenuState currentState = playerMenuState.getOrDefault(player.getUniqueId(), MenuState.MAIN);

            switch (currentState) {
                case MAIN:
                    handleMainMenuClick(player, slot);
                    break;
                case BOSS_LIST:
                    handleBossListClick(player, slot);
                    break;
                case STATS:
                    handleStatsClick(player, slot);
                    break;
                case SETTINGS:
                    handleSettingsClick(player, slot);
                    break;
            }

            event.setCancelled(true);

        } catch (Exception e) {
            logger.severe("§c✗ 处理菜单点击失败: " + e.getMessage());
        }
    }

    /**
     * 处理主菜单点击
     */
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 10:
                // Boss列表
                openBossListMenu(player);
                break;
            case 12:
                // 创建Boss
                startBossCreation(player);
                break;
            case 14:
                // 统计数据
                openStatsMenu(player);
                break;
            case 16:
                // 配置设置
                openSettingsMenu(player);
                break;
            case 26:
                // 关闭菜单
                player.closeInventory();
                playerMenuState.remove(player.getUniqueId());
                break;
        }
    }

    /**
     * 打开Boss列表菜单
     */
    public void openBossListMenu(Player player) {
        try {
            Inventory listMenu = Bukkit.createInventory(null, 27, "§6§lBoss列表");

            // 模拟Boss数据
            addBossItem(listMenu, 10, "§aSkeletonKing", Material.SKELETON_SKULL, "Tier 1");
            addBossItem(listMenu, 12, "§bFrostGiant", Material.ICE, "Tier 2");
            addBossItem(listMenu, 14, "§cSkywingDragon", Material.DRAGON_EGG, "Tier 3");
            addBossItem(listMenu, 16, "§dAbyssDemon", Material.NETHER_STAR, "Tier 4");

            // 返回按钮
            ItemStack backItem = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backItem.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName("§c返回上级");
                backItem.setItemMeta(backMeta);
            }
            listMenu.setItem(26, backItem);

            playerMenuState.put(player.getUniqueId(), MenuState.BOSS_LIST);
            player.openInventory(listMenu);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了Boss列表");

        } catch (Exception e) {
            logger.severe("§c✗ 打开Boss列表失败: " + e.getMessage());
        }
    }

    /**
     * 添加Boss项到菜单
     */
    private void addBossItem(Inventory inventory, int slot, String name, Material icon, String tier) {
        ItemStack bossItem = new ItemStack(icon);
        ItemMeta meta = bossItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                    "§7" + tier,
                    "§8左键查看详情",
                    "§8右键编辑"
            ));
            bossItem.setItemMeta(meta);
        }

        inventory.setItem(slot, bossItem);
    }

    /**
     * 处理Boss列表点击
     */
    private void handleBossListClick(Player player, int slot) {
        switch (slot) {
            case 10:
                showBossDetail(player, "SkeletonKing");
                break;
            case 12:
                showBossDetail(player, "FrostGiant");
                break;
            case 14:
                showBossDetail(player, "SkywingDragon");
                break;
            case 16:
                showBossDetail(player, "AbyssDemon");
                break;
            case 26:
                openMainMenu(player);
                break;
        }
    }

    /**
     * 显示Boss详情
     */
    private void showBossDetail(Player player, String bossType) {
        player.sendMessage("§a═══════════════════════════════");
        player.sendMessage("§eBoss详情: §6" + bossType);
        player.sendMessage("§7类型: §a" + bossType);
        player.sendMessage("§7状态: §c活跃");
        player.sendMessage("§7血量: §a100.0 / 100.0");
        player.sendMessage("§7位置: 世界 (100, 64, 100)");
        player.sendMessage("§a═══════════════════════════════");

        // 返回菜单
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            openBossListMenu(player);
        }, 40L); // 2秒后自动返回
    }

    /**
     * 开始创建Boss
     */
    private void startBossCreation(Player player) {
        player.sendMessage("§a═══════════════════════════════");
        player.sendMessage("§e创建Boss向导");
        player.sendMessage("§71. 请选择Boss类型");
        player.sendMessage("§7可用类型: SkeletonKing, FrostGiant, SkywingDragon, AbyssDemon");
        player.sendMessage("§72. 在聊天框中输入类型名称");
        player.sendMessage("§a═══════════════════════════════");

        playerMenuState.put(player.getUniqueId(), MenuState.BOSS_EDIT);
    }

    /**
     * 打开统计菜单
     */
    public void openStatsMenu(Player player) {
        try {
            Inventory statsMenu = Bukkit.createInventory(null, 27, "§6§l系统统计");

            // 添加统计卡片
            addStatCard(statsMenu, 10, "§a总生成数", Material.EMERALD, "1,234");
            addStatCard(statsMenu, 12, "§b总击杀数", Material.DIAMOND, "987");
            addStatCard(statsMenu, 14, "§c当前活跃", Material.REDSTONE, "5");
            addStatCard(statsMenu, 16, "§d参与玩家", Material.PLAYER_HEAD, "48");

            // 返回按钮
            ItemStack backItem = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backItem.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName("§c返回上级");
                backItem.setItemMeta(backMeta);
            }
            statsMenu.setItem(26, backItem);

            playerMenuState.put(player.getUniqueId(), MenuState.STATS);
            player.openInventory(statsMenu);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了统计菜单");

        } catch (Exception e) {
            logger.severe("§c✗ 打开统计菜单失败: " + e.getMessage());
        }
    }

    /**
     * 添加统计卡片
     */
    private void addStatCard(Inventory inventory, int slot, String name, Material icon, String value) {
        ItemStack card = new ItemStack(icon);
        ItemMeta meta = card.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                    "§7数值: §a" + value
            ));
            card.setItemMeta(meta);
        }

        inventory.setItem(slot, card);
    }

    /**
     * 处理统计菜单点击
     */
    private void handleStatsClick(Player player, int slot) {
        if (slot == 26) {
            openMainMenu(player);
        }
    }

    /**
     * 打开设置菜单
     */
    public void openSettingsMenu(Player player) {
        try {
            Inventory settingsMenu = Bukkit.createInventory(null, 27, "§6§l系统设置");

            // 添加设置项
            addSettingItem(settingsMenu, 10, "§a启用奖励", Material.GOLD_INGOT, true);
            addSettingItem(settingsMenu, 12, "§b启用伤害追踪", Material.REDSTONE, true);
            addSettingItem(settingsMenu, 14, "§c启用公告", Material.BELL, true);
            addSettingItem(settingsMenu, 16, "§d系统信息", Material.BOOK, true);

            // 返回按钮
            ItemStack backItem = new ItemStack(Material.ARROW);
            ItemMeta backMeta = backItem.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName("§c返回上级");
                backItem.setItemMeta(backMeta);
            }
            settingsMenu.setItem(26, backItem);

            playerMenuState.put(player.getUniqueId(), MenuState.SETTINGS);
            player.openInventory(settingsMenu);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了设置菜单");

        } catch (Exception e) {
            logger.severe("§c✗ 打开设置菜单失败: " + e.getMessage());
        }
    }

    /**
     * 添加设置项
     */
    private void addSettingItem(Inventory inventory, int slot, String name, Material icon, boolean enabled) {
        ItemStack setting = new ItemStack(icon);
        ItemMeta meta = setting.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            String status = enabled ? "§a已启用" : "§c已禁用";
            meta.setLore(Arrays.asList(
                    "§7状态: " + status,
                    "§8左键切换"
            ));
            setting.setItemMeta(meta);
        }

        inventory.setItem(slot, setting);
    }

    /**
     * 处理设置菜单点击
     */
    private void handleSettingsClick(Player player, int slot) {
        switch (slot) {
            case 10:
            case 12:
            case 14:
            case 16:
                player.sendMessage("§a设置已切换");
                openSettingsMenu(player);
                break;
            case 26:
                openMainMenu(player);
                break;
        }
    }

    /**
     * 获取玩家当前菜单状态
     */
    public MenuState getPlayerMenuState(UUID playerId) {
        return playerMenuState.getOrDefault(playerId, MenuState.MAIN);
    }

    /**
     * 清除玩家菜单状态
     */
    public void clearPlayerMenuState(UUID playerId) {
        playerMenuState.remove(playerId);
        logger.info("§a✓ 已清除玩家菜单状态");
    }

    /**
     * 清除所有菜单状态
     */
    public void clearAllMenuStates() {
        playerMenuState.clear();
        logger.info("§a✓ 已清除所有菜单状态");
    }
}
