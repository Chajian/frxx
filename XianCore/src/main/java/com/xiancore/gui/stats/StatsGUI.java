package com.xiancore.gui.stats;

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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * 统计显示GUI - 显示系统和玩家统计数据
 * 业务逻辑委托给 StatsDisplayService
 * 使用 InventoryFramework 统一 GUI 框架
 *
 * @author XianCore
 * @version 3.0.0 - 统一使用 IF 框架
 */
public class StatsGUI {

    private final Plugin plugin;
    private final Logger logger;
    private final StatsDisplayService displayService;

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

    public StatsGUI(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.displayService = new StatsDisplayService();
    }

    /**
     * 显示系统统计菜单
     */
    public void showSystemStatsMenu(Player player) {
        try {
            SystemStats stats = getSampleSystemStats();

            ChestGui gui = new ChestGui(3, "§6§l系统统计");
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            GUIUtils.addGrayBackground(gui, 3);

            StaticPane contentPane = new StaticPane(0, 0, 9, 3);

            // 统计卡片
            contentPane.addItem(new GuiItem(createStatCard("§a总生成数", Material.ZOMBIE_HEAD,
                    stats.totalBossesSpawned + "")), 1, 1);
            contentPane.addItem(new GuiItem(createStatCard("§b总击杀数", Material.DIAMOND,
                    stats.totalBossesKilled + "")), 3, 1);
            contentPane.addItem(new GuiItem(createStatCard("§c当前活跃", Material.REDSTONE,
                    stats.currentActiveBosses + "")), 5, 1);
            contentPane.addItem(new GuiItem(createStatCard("§d参与玩家", Material.PLAYER_HEAD,
                    stats.totalPlayers + "")), 7, 1);

            // 关闭按钮
            ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                    .name("§c§l关闭")
                    .build();
            contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 2);

            gui.addPane(contentPane);
            gui.show(player);
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

            ChestGui gui = new ChestGui(3, title);
            gui.setOnGlobalClick(event -> event.setCancelled(true));

            GUIUtils.addGrayBackground(gui, 3);

            StaticPane contentPane = new StaticPane(0, 0, 9, 3);

            List<PlayerRank> rankings = getRankings(rankType);

            // 显示前6名
            int[] cols = {1, 2, 3, 5, 6, 7};
            for (int i = 0; i < Math.min(6, rankings.size()); i++) {
                contentPane.addItem(new GuiItem(createRankCard(rankings.get(i))), cols[i], 1);
            }

            // 类型切换按钮
            String otherType = "击杀数".equals(rankType) ? "伤害排名" : "击杀排名";
            final String switchTo = "击杀数".equals(rankType) ? "伤害" : "击杀数";
            ItemStack switchBtn = new ItemBuilder(Material.COMPARATOR)
                    .name("§e切换: " + otherType)
                    .lore("§7点击切换排名类型")
                    .build();
            contentPane.addItem(new GuiItem(switchBtn, event -> {
                showRankingsMenu(player, switchTo);
            }), 4, 2);

            // 关闭按钮
            ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                    .name("§c§l关闭")
                    .build();
            contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 2);

            gui.addPane(contentPane);
            gui.show(player);
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
            StatsDisplayService.PlayerStatsInfo info = displayService.createPlayerStatsInfo(
                    player.getName(), 42, 2100.5, 8, 12, "5分钟前");
            List<String> lines = displayService.createPlayerStatsDisplay(info);

            player.sendMessage("");
            for (String line : lines) {
                player.sendMessage(line);
            }
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
            List<StatsDisplayService.RecentBossInfo> recentBosses = Arrays.asList(
                    new StatsDisplayService.RecentBossInfo("SkeletonKing", "5分钟前", "50.5伤害"),
                    new StatsDisplayService.RecentBossInfo("FrostGiant", "15分钟前", "150.3伤害"),
                    new StatsDisplayService.RecentBossInfo("FireTitan", "1小时前", "200.8伤害")
            );

            List<String> lines = displayService.createRecentBossesDisplay(recentBosses);

            player.sendMessage("");
            for (String line : lines) {
                player.sendMessage(line);
            }
            player.sendMessage("");

        } catch (Exception e) {
            logger.severe("§c✗ 显示最近Boss信息失败: " + e.getMessage());
        }
    }

    /**
     * 创建统计卡片
     */
    private ItemStack createStatCard(String name, Material icon, String value) {
        return new ItemBuilder(icon)
                .name(name)
                .lore("§7数值: §a" + value)
                .build();
    }

    /**
     * 创建排名卡片
     */
    private ItemStack createRankCard(PlayerRank rank) {
        Material icon = displayService.getMaterialForRank(rank.rank);
        String color = displayService.getColorForRank(rank.rank);

        List<String> lore = new ArrayList<>();
        lore.add("§7击杀: §a" + rank.killCount);
        lore.add("§7总伤害: §a" + displayService.formatDamage(rank.totalDamage));
        lore.add("§7平均伤害: §a" + displayService.formatDamage(rank.averageDamage));

        return new ItemBuilder(icon)
                .name(color + "§l#" + rank.rank + " " + rank.playerName)
                .lore(lore)
                .build();
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
     * 获取今日统计
     */
    public void showTodayStats(Player player) {
        List<String> lines = displayService.createTodayStatsDisplay(
                28, 24, 16, 12500.5, "3分12秒", "Player1");

        player.sendMessage("");
        for (String line : lines) {
            player.sendMessage(line);
        }
        player.sendMessage("");
    }
}
