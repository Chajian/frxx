package com.xiancore.gui.boss;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Boss列表GUI - 显示所有Boss刷新点
 * 业务逻辑委托给 BossListDisplayService
 * 使用 InventoryFramework 统一 GUI 框架
 *
 * @author XianCore
 * @version 3.0.0 - 统一使用 IF 框架
 */
public class BossListGUI {

    private final Plugin plugin;
    private final Logger logger;
    private final BossListDisplayService displayService;

    /**
     * Boss信息数据类
     */
    public static class BossInfo {
        public String id;
        public String type;
        public String world;
        public int x, y, z;
        public int tier;
        public double health;
        public String status;

        public BossInfo(String id, String type, String world, int x, int y, int z,
                        int tier, double health, String status) {
            this.id = id;
            this.type = type;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.tier = tier;
            this.health = health;
            this.status = status;
        }
    }

    public BossListGUI(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.displayService = new BossListDisplayService();
    }

    /**
     * 打开Boss列表菜单 (分页)
     */
    public void openBossListGUI(Player player, int page) {
        try {
            List<BossInfo> bosses = getSampleBosses();
            BossListDisplayService.PageInfo pageInfo = displayService.calculatePageInfo(bosses.size(), page);

            String title = "§6§lBoss列表 (" + pageInfo.getCurrentPage() + "/" + pageInfo.getTotalPages() + ")";
            ChestGui gui = new ChestGui(4, title);
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            GUIUtils.addGrayBackground(gui, 4);

            StaticPane contentPane = new StaticPane(0, 0, 9, 4);

            // 添加Boss项
            int slot = 0;
            for (int i = pageInfo.getStartIndex(); i < pageInfo.getEndIndex() && slot < 21; i++) {
                BossInfo boss = bosses.get(i);
                int row = slot / 7;
                int col = 1 + (slot % 7);

                ItemStack bossItem = createBossItem(boss);
                final BossInfo finalBoss = boss;
                final int currentPage = pageInfo.getCurrentPage();
                contentPane.addItem(new GuiItem(bossItem, event -> {
                    if (event.isLeftClick()) {
                        showBossDetail(player, finalBoss);
                    } else if (event.isRightClick()) {
                        teleportToBoss(player, finalBoss);
                    }
                }), col, row);

                slot++;
            }

            // 分页按钮
            if (pageInfo.hasPrevious()) {
                ItemStack prevBtn = new ItemBuilder(Material.ARROW)
                        .name("§c上一页")
                        .lore("§7点击查看上一页")
                        .build();
                final int prevPage = pageInfo.getCurrentPage() - 1;
                contentPane.addItem(new GuiItem(prevBtn, event -> {
                    openBossListGUI(player, prevPage);
                }), 0, 3);
            }

            if (pageInfo.hasNext()) {
                ItemStack nextBtn = new ItemBuilder(Material.ARROW)
                        .name("§a下一页")
                        .lore("§7点击查看下一页")
                        .build();
                final int nextPage = pageInfo.getCurrentPage() + 1;
                contentPane.addItem(new GuiItem(nextBtn, event -> {
                    openBossListGUI(player, nextPage);
                }), 8, 3);
            }

            // 关闭按钮
            ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                    .name("§c§l关闭")
                    .build();
            contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 4, 3);

            gui.addPane(contentPane);
            gui.show(player);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了Boss列表 (第 " + pageInfo.getCurrentPage() + " 页)");

        } catch (Exception e) {
            logger.severe("§c✗ 打开Boss列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建Boss物品
     */
    private ItemStack createBossItem(BossInfo boss) {
        List<String> lore = new ArrayList<>();
        lore.add("§7世界: §a" + boss.world);
        lore.add("§7位置: §a" + boss.x + ", " + boss.y + ", " + boss.z);
        lore.add("§7等级: §b" + displayService.getTierName(boss.tier));
        lore.add("§7血量: §c" + String.format("%.1f", boss.health) + " / 100.0");
        lore.add("§7状态: " + displayService.getStatusColor(boss.status) + boss.status);
        lore.add("");
        lore.add("§a左键 §7查看详情");
        lore.add("§e右键 §7传送到此");

        return new ItemBuilder(displayService.getMaterialForTier(boss.tier))
                .name(displayService.getColorForTier(boss.tier) + boss.type)
                .lore(lore)
                .build();
    }

    /**
     * 显示Boss详情
     */
    private void showBossDetail(Player player, BossInfo boss) {
        List<String> lines = displayService.createBossDetailLines(boss);
        player.sendMessage("");
        for (String line : lines) {
            player.sendMessage(line);
        }
        player.sendMessage("");
        player.sendMessage("§7[§a编辑§7] [§b传送§7] [§c删除§7] [§4关闭§7]");
    }

    /**
     * 传送到Boss位置
     */
    private void teleportToBoss(Player player, BossInfo boss) {
        org.bukkit.World world = plugin.getServer().getWorld(boss.world);
        if (world != null) {
            player.teleport(new org.bukkit.Location(world, boss.x, boss.y, boss.z));
            player.sendMessage("§a已传送到 " + boss.type + " 的位置！");
            player.closeInventory();
        } else {
            player.sendMessage("§c无法找到世界: " + boss.world);
        }
    }

    /**
     * 处理Boss列表点击事件 (保留兼容性)
     */
    public void handleBossListClick(Player player, int slot, int currentPage) {
        // IF 框架自动处理点击，此方法保留兼容性
    }

    /**
     * 获取样本Boss数据
     */
    private List<BossInfo> getSampleBosses() {
        List<BossInfo> bosses = new ArrayList<>();

        bosses.add(new BossInfo("boss-1", "SkeletonKing", "world", 100, 64, 100,
                1, 100.0, "ACTIVE"));
        bosses.add(new BossInfo("boss-2", "GhoulBeast", "world", 150, 65, 150,
                1, 85.5, "ACTIVE"));
        bosses.add(new BossInfo("boss-3", "FrostGiant", "world", 500, 70, -300,
                2, 200.0, "ACTIVE"));
        bosses.add(new BossInfo("boss-4", "SandWraith", "world", 520, 68, -280,
                2, 175.3, "DEAD"));
        bosses.add(new BossInfo("boss-5", "SkywingDragon", "world", 1000, 200, 1000,
                3, 350.0, "ACTIVE"));
        bosses.add(new BossInfo("boss-6", "IcePhoenix", "world", 1050, 195, 1050,
                3, 280.5, "DESPAWNED"));
        bosses.add(new BossInfo("boss-7", "AbyssDemon", "world", -2000, 50, 2000,
                4, 500.0, "ACTIVE"));
        bosses.add(new BossInfo("boss-8", "ShadowLord", "world", -1950, 55, 2050,
                4, 450.2, "DEAD"));
        bosses.add(new BossInfo("boss-9", "WoodGolem", "world", -500, 65, 500,
                1, 120.0, "ACTIVE"));
        bosses.add(new BossInfo("boss-10", "FireTitan", "world", 1500, 100, -1500,
                2, 220.0, "ACTIVE"));

        return bosses;
    }

    /**
     * 搜索Boss
     */
    public List<BossInfo> searchBosses(String query) {
        return displayService.searchBosses(getSampleBosses(), query);
    }

    /**
     * 获取活跃Boss数量
     */
    public int getActiveBossCount() {
        return displayService.getActiveBossCount(getSampleBosses());
    }

    /**
     * 获取特定Tier的Boss列表
     */
    public List<BossInfo> getBossesByTier(int tier) {
        return displayService.getBossesByTier(getSampleBosses(), tier);
    }
}
