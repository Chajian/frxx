package com.xiancore.gui.stats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.logging.Logger;

/**
 * 统计显示GUI - 显示系统和玩家统计数据
 * Stats Display GUI - Show system and player statistics
 *
 * @author XianCore
 * @version 1.0
 */
public class StatsGUI {

    private final Plugin plugin;
    private final Logger logger;

    /**
     * 系统统计数据类
     */
    public static class SystemStats {
        public int totalBossesSpawned;
        public int totalBossesKilled;
        public int currentActiveBosses;
        public int totalPlayers;
        public long totalGameTime;
        public double averageKillTime;

        public SystemStats(int spawned, int killed, int active, int players,
                          long gameTime, double avgKillTime) {
            this.totalBossesSpawned = spawned;
            this.totalBossesKilled = killed;
            this.currentActiveBosses = active;
            this.totalPlayers = players;
            this.totalGameTime = gameTime;
            this.averageKillTime = avgKillTime;
        }
    }

    /**
     * 玩家排名数据类
     */
    public static class PlayerRank {
        public int rank;
        public String playerName;
        public int killCount;
        public double totalDamage;
        public double averageDamage;

        public PlayerRank(int rank, String playerName, int kills, double totalDmg, double avgDmg) {
            this.rank = rank;
            this.playerName = playerName;
            this.killCount = kills;
            this.totalDamage = totalDmg;
            this.averageDamage = avgDmg;
        }
    }

    /**
     * 构造函数
     */
    public StatsGUI(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 显示系统统计菜单
     */
    public void showSystemStatsMenu(Player player) {
        try {
            Inventory statsMenu = Bukkit.createInventory(null, 27, "§6§l系统统计");

            // 获取统计数据
            SystemStats stats = getSampleSystemStats();

            // 添加统计卡片
            addStatCard(statsMenu, 10, "§a总生成数", Material.ZOMBIE_HEAD, stats.totalBossesSpawned + "");
            addStatCard(statsMenu, 12, "§b总击杀数", Material.DIAMOND, stats.totalBossesKilled + "");
            addStatCard(statsMenu, 14, "§c当前活跃", Material.REDSTONE, stats.currentActiveBosses + "");
            addStatCard(statsMenu, 16, "§d参与玩家", Material.PLAYER_HEAD, stats.totalPlayers + "");

            // 第二排统计
            addStatCard(statsMenu, 19, "§e总游戏时间", Material.CLOCK, formatTime(stats.totalGameTime));
            addStatCard(statsMenu, 21, "§f平均击杀时间", Material.CLOCK, String.format("%.1f秒", stats.averageKillTime));

            // 返回按钮
            ItemStack backButton = createButton("§4返回", Material.BARRIER);
            statsMenu.setItem(26, backButton);

            player.openInventory(statsMenu);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了系统统计菜单");

        } catch (Exception e) {
            logger.severe("§c✗ 显示系统统计失败: " + e.getMessage());
        }
    }

    /**
     * 显示排名菜单
     */
    public void showRankingsMenu(Player player, String rankType) {
        try {
            String title = "击杀数".equals(rankType) ? "§6§l击杀排名" : "§6§l伤害排名";
            Inventory rankMenu = Bukkit.createInventory(null, 27, title);

            // 获取排名数据
            List<PlayerRank> rankings = getRankings(rankType);

            // 显示前6名
            int[] slots = {10, 12, 14, 16, 19, 21};
            for (int i = 0; i < Math.min(6, rankings.size()); i++) {
                addRankCard(rankMenu, slots[i], rankings.get(i));
            }

            // 类型切换按钮
            String otherType = "击杀数".equals(rankType) ? "伤害排名" : "击杀排名";
            ItemStack switchButton = createButton("§e切换: " + otherType, Material.COMPARATOR);
            rankMenu.setItem(23, switchButton);

            // 返回按钮
            ItemStack backButton = createButton("§4返回", Material.BARRIER);
            rankMenu.setItem(26, backButton);

            player.openInventory(rankMenu);
            logger.info("§a✓ 玩家 " + player.getName() + " 打开了" + title + "菜单");

        } catch (Exception e) {
            logger.severe("§c✗ 显示排名菜单失败: " + e.getMessage());
        }
    }

    /**
     * 显示玩家个人统计
     */
    public void showPlayerStats(Player player) {
        try {
            player.sendMessage("");
            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("§6§l  个人统计");
            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("§e玩家名: §a" + player.getName());
            player.sendMessage("§e击杀数: §a42");
            player.sendMessage("§e总伤害: §a2100.5");
            player.sendMessage("§e平均伤害: §a50.01");
            player.sendMessage("§e击杀排名: §a#8");
            player.sendMessage("§e伤害排名: §a#12");
            player.sendMessage("§e最后击杀: §a5分钟前");
            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("");

        } catch (Exception e) {
            logger.severe("§c✗ 显示玩家统计失败: " + e.getMessage());
        }
    }

    /**
     * 显示最近的Boss信息
     */
    public void showRecentBosses(Player player) {
        try {
            player.sendMessage("");
            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("§6§l  最近击杀的Boss");
            player.sendMessage("§6§l═══════════════════════════════");

            // 模拟最近Boss数据
            String[][] recentBosses = {
                    {"SkeletonKing", "5分钟前", "50.5伤害"},
                    {"FrostGiant", "15分钟前", "150.3伤害"},
                    {"FireTitan", "1小时前", "200.8伤害"}
            };

            for (String[] boss : recentBosses) {
                player.sendMessage("§e" + boss[0] + "§7 - " + boss[1] + " (伤害: " + boss[2] + ")");
            }

            player.sendMessage("§6§l═══════════════════════════════");
            player.sendMessage("");

        } catch (Exception e) {
            logger.severe("§c✗ 显示最近Boss信息失败: " + e.getMessage());
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
     * 添加排名卡片
     */
    private void addRankCard(Inventory inventory, int slot, PlayerRank rank) {
        Material icon = getMaterialForRank(rank.rank);
        ItemStack card = new ItemStack(icon);
        ItemMeta meta = card.getItemMeta();

        if (meta != null) {
            String color = getColorForRank(rank.rank);
            meta.setDisplayName(color + "§l#" + rank.rank + " " + rank.playerName);
            List<String> lore = new ArrayList<>();
            lore.add("§7击杀: §a" + rank.killCount);
            lore.add("§7总伤害: §a" + String.format("%.1f", rank.totalDamage));
            lore.add("§7平均伤害: §a" + String.format("%.1f", rank.averageDamage));
            meta.setLore(lore);
            card.setItemMeta(meta);
        }

        inventory.setItem(slot, card);
    }

    /**
     * 根据排名获取材料
     */
    private Material getMaterialForRank(int rank) {
        switch (rank) {
            case 1: return Material.GOLD_BLOCK;
            case 2: return Material.IRON_BLOCK;
            case 3: return Material.COPPER_BLOCK;
            default: return Material.STONE;
        }
    }

    /**
     * 根据排名获取颜色
     */
    private String getColorForRank(int rank) {
        switch (rank) {
            case 1: return "§6";  // 黄金
            case 2: return "§7";  // 白银
            case 3: return "§c";  // 铜
            default: return "§8"; // 默认
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
     * 获取系统统计数据
     */
    private SystemStats getSampleSystemStats() {
        return new SystemStats(1234, 987, 5, 48, 86400000, 352.5);
    }

    /**
     * 获取排名数据
     */
    private List<PlayerRank> getRankings(String rankType) {
        List<PlayerRank> rankings = new ArrayList<>();

        if ("击杀数".equals(rankType)) {
            rankings.add(new PlayerRank(1, "TopKiller", 100, 5000, 50));
            rankings.add(new PlayerRank(2, "SecondPlace", 85, 4250, 50));
            rankings.add(new PlayerRank(3, "ThirdPlace", 72, 3600, 50));
            rankings.add(new PlayerRank(4, "FourthPlace", 65, 3250, 50));
            rankings.add(new PlayerRank(5, "FifthPlace", 58, 2900, 50));
            rankings.add(new PlayerRank(6, "SixthPlace", 52, 2600, 50));
        } else {
            rankings.add(new PlayerRank(1, "DamageDealer", 45, 10000, 222.2));
            rankings.add(new PlayerRank(2, "StrongAttack", 40, 9500, 237.5));
            rankings.add(new PlayerRank(3, "PowerfulHit", 38, 9000, 236.8));
            rankings.add(new PlayerRank(4, "MightyForce", 36, 8500, 236.1));
            rankings.add(new PlayerRank(5, "CrusherForce", 34, 8000, 235.3));
            rankings.add(new PlayerRank(6, "DeadlyStrike", 32, 7500, 234.4));
        }

        return rankings;
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "天" + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时" + (minutes % 60) + "分";
        } else if (minutes > 0) {
            return minutes + "分" + (seconds % 60) + "秒";
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 获取今日统计
     */
    public void showTodayStats(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§6§l  今日统计");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("§e生成Boss数: §a28");
        player.sendMessage("§e击杀Boss数: §a24");
        player.sendMessage("§e参与玩家数: §a16");
        player.sendMessage("§e总伤害数: §a12500.5");
        player.sendMessage("§e平均击杀时间: §a3分12秒");
        player.sendMessage("§e最活跃玩家: §aPlayer1");
        player.sendMessage("§6§l═══════════════════════════════");
        player.sendMessage("");
    }
}
