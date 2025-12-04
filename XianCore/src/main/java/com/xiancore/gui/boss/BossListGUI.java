package com.xiancore.gui.boss;

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
 * Boss列表GUI - 显示所有Boss刷新点
 * Boss List GUI - Display all boss spawn points
 *
 * @author XianCore
 * @version 1.0
 */
public class BossListGUI {

    private final Plugin plugin;
    private final Logger logger;
    private final int ITEMS_PER_PAGE = 21; // 每页显示的Boss数量

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
        public String status; // ACTIVE, DEAD, DESPAWNED

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

    /**
     * 构造函数
     */
    public BossListGUI(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 打开Boss列表菜单 (分页)
     */
    public void openBossListGUI(Player player, int page) {
        try {
            // 获取Boss列表
            List<BossInfo> bosses = getSampleBosses();

            // 计算分页
            int totalPages = (int) Math.ceil((double) bosses.size() / ITEMS_PER_PAGE);
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;

            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, bosses.size());

            // 创建菜单
            String title = "§6§lBoss列表 (" + page + "/" + totalPages + ")";
            Inventory listGUI = Bukkit.createInventory(null, 27, title);

            // 添加Boss项
            int slot = 0;
            for (int i = startIndex; i < endIndex && slot < 21; i++) {
                BossInfo boss = bosses.get(i);
                addBossItem(listGUI, slot, boss);
                slot++;
            }

            // 分页按钮
            if (page > 1) {
                ItemStack prevButton = createButton("§c上一页", Material.ARROW);
                listGUI.setItem(21, prevButton);
            }

            if (page < totalPages) {
                ItemStack nextButton = createButton("§a下一页", Material.ARROW);
                listGUI.setItem(23, nextButton);
            }

            // 返回按钮
            ItemStack backButton = createButton("§4返回", Material.BARRIER);
            listGUI.setItem(25, backButton);

            player.openInventory(listGUI);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了Boss列表 (第 " + page + " 页)");

        } catch (Exception e) {
            logger.severe("§c✗ 打开Boss列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加Boss项
     */
    private void addBossItem(Inventory inventory, int slot, BossInfo boss) {
        ItemStack item = new ItemStack(getMaterialForTier(boss.tier));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(getColorForTier(boss.tier) + boss.type);
            List<String> lore = new ArrayList<>();
            lore.add("§7世界: §a" + boss.world);
            lore.add("§7位置: §a" + boss.x + ", " + boss.y + ", " + boss.z);
            lore.add("§7等级: §b" + getTierName(boss.tier));
            lore.add("§7血量: §c" + String.format("%.1f", boss.health) + " / 100.0");
            lore.add("§7状态: " + getStatusColor(boss.status) + boss.status);
            lore.add("");
            lore.add("§8左键查看详情 | 右键传送到此");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    /**
     * 根据Tier获取材料
     */
    private Material getMaterialForTier(int tier) {
        switch (tier) {
            case 1: return Material.ZOMBIE_HEAD;
            case 2: return Material.WITHER_SKELETON_SKULL;
            case 3: return Material.DRAGON_HEAD;
            case 4: return Material.NETHER_STAR;
            default: return Material.SKULL_BANNER_PATTERN;
        }
    }

    /**
     * 根据Tier获取颜色
     */
    private String getColorForTier(int tier) {
        switch (tier) {
            case 1: return "§a";  // 绿色
            case 2: return "§b";  // 青色
            case 3: return "§e";  // 黄色
            case 4: return "§c";  // 红色
            default: return "§7"; // 灰色
        }
    }

    /**
     * 获取Tier名称
     */
    private String getTierName(int tier) {
        switch (tier) {
            case 1: return "普通";
            case 2: return "精英";
            case 3: return "世界Boss";
            case 4: return "传奇";
            default: return "未知";
        }
    }

    /**
     * 获取状态颜色
     */
    private String getStatusColor(String status) {
        switch (status) {
            case "ACTIVE": return "§a";
            case "DEAD": return "§c";
            case "DESPAWNED": return "§7";
            default: return "§8";
        }
    }

    /**
     * 创建按钮
     */
    private ItemStack createButton(String name, Material material) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            button.setItemMeta(meta);
        }

        return button;
    }

    /**
     * 处理Boss列表点击事件
     */
    public void handleBossListClick(Player player, int slot, int currentPage) {
        try {
            switch (slot) {
                case 21: // 上一页
                    openBossListGUI(player, currentPage - 1);
                    break;
                case 23: // 下一页
                    openBossListGUI(player, currentPage + 1);
                    break;
                case 25: // 返回
                    player.closeInventory();
                    break;
                default:
                    if (slot < 21) {
                        // 点击了Boss项
                        List<BossInfo> bosses = getSampleBosses();
                        int index = (currentPage - 1) * ITEMS_PER_PAGE + slot;
                        if (index < bosses.size()) {
                            showBossDetail(player, bosses.get(index));
                        }
                    }
            }
        } catch (Exception e) {
            logger.severe("§c✗ 处理Boss列表点击失败: " + e.getMessage());
        }
    }

    /**
     * 显示Boss详情
     */
    private void showBossDetail(Player player, BossInfo boss) {
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§6§l  Boss详情");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§e名称: §a" + boss.type);
        player.sendMessage("§e世界: §a" + boss.world);
        player.sendMessage("§e位置: §a" + boss.x + ", " + boss.y + ", " + boss.z);
        player.sendMessage("§e等级: " + getColorForTier(boss.tier) + getTierName(boss.tier));
        player.sendMessage("§e血量: §c" + String.format("%.1f", boss.health) + " / 100.0");
        player.sendMessage("§e状态: " + getStatusColor(boss.status) + boss.status);
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("");

        player.sendMessage("§7[§a编辑§7] [§b传送§7] [§c删除§7] [§4关闭§7]");
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
        List<BossInfo> allBosses = getSampleBosses();
        List<BossInfo> results = new ArrayList<>();

        String lowerQuery = query.toLowerCase();

        for (BossInfo boss : allBosses) {
            if (boss.type.toLowerCase().contains(lowerQuery) ||
                boss.world.toLowerCase().contains(lowerQuery) ||
                boss.status.toLowerCase().contains(lowerQuery)) {
                results.add(boss);
            }
        }

        return results;
    }

    /**
     * 获取活跃Boss数量
     */
    public int getActiveBossCount() {
        return (int) getSampleBosses().stream()
                .filter(boss -> "ACTIVE".equals(boss.status))
                .count();
    }

    /**
     * 获取特定Tier的Boss列表
     */
    public List<BossInfo> getBossesByTier(int tier) {
        List<BossInfo> results = new ArrayList<>();
        for (BossInfo boss : getSampleBosses()) {
            if (boss.tier == tier) {
                results.add(boss);
            }
        }
        return results;
    }
}
