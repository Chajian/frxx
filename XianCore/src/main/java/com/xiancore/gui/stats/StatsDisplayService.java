package com.xiancore.gui.stats;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计显示服务
 * 负责统计GUI的数据格式化和显示逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class StatsDisplayService {

    // 排名材料映射
    private static final Material[] RANK_MATERIALS = {
            Material.STONE,        // 默认
            Material.GOLD_BLOCK,   // 第1名
            Material.IRON_BLOCK,   // 第2名
            Material.COPPER_BLOCK  // 第3名
    };

    // 排名颜色映射
    private static final String[] RANK_COLORS = {
            "§8", // 默认
            "§6", // 第1名 黄金
            "§7", // 第2名 白银
            "§c"  // 第3名 铜
    };

    /**
     * 根据排名获取材料
     */
    public Material getMaterialForRank(int rank) {
        if (rank >= 1 && rank < RANK_MATERIALS.length) {
            return RANK_MATERIALS[rank];
        }
        return RANK_MATERIALS[0];
    }

    /**
     * 根据排名获取颜色
     */
    public String getColorForRank(int rank) {
        if (rank >= 1 && rank < RANK_COLORS.length) {
            return RANK_COLORS[rank];
        }
        return RANK_COLORS[0];
    }

    /**
     * 格式化时间显示
     */
    public String formatTime(long millis) {
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
     * 格式化伤害数值
     */
    public String formatDamage(double damage) {
        return String.format("%.1f", damage);
    }

    /**
     * 创建个人统计信息
     */
    public PlayerStatsInfo createPlayerStatsInfo(String playerName, int kills, double totalDamage,
                                                  int killRank, int damageRank, String lastKillTime) {
        double avgDamage = kills > 0 ? totalDamage / kills : 0;
        return new PlayerStatsInfo(playerName, kills, totalDamage, avgDamage, killRank, damageRank, lastKillTime);
    }

    /**
     * 创建系统统计信息的显示文本
     */
    public List<String> createSystemStatsDisplay(StatsGUI.SystemStats stats) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l═══════════════════════════════");
        lines.add("§6§l  系统统计");
        lines.add("§6§l═══════════════════════════════");
        lines.add("§e总生成数: §a" + stats.totalBossesSpawned);
        lines.add("§e总击杀数: §a" + stats.totalBossesKilled);
        lines.add("§e当前活跃: §a" + stats.currentActiveBosses);
        lines.add("§e参与玩家: §a" + stats.totalPlayers);
        lines.add("§e总游戏时间: §a" + formatTime(stats.totalGameTime));
        lines.add("§e平均击杀时间: §a" + formatDamage(stats.averageKillTime) + "秒");
        lines.add("§6§l═══════════════════════════════");
        return lines;
    }

    /**
     * 创建玩家统计显示文本
     */
    public List<String> createPlayerStatsDisplay(PlayerStatsInfo info) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l═══════════════════════════════");
        lines.add("§6§l  个人统计");
        lines.add("§6§l═══════════════════════════════");
        lines.add("§e玩家名: §a" + info.getPlayerName());
        lines.add("§e击杀数: §a" + info.getKillCount());
        lines.add("§e总伤害: §a" + formatDamage(info.getTotalDamage()));
        lines.add("§e平均伤害: §a" + formatDamage(info.getAverageDamage()));
        lines.add("§e击杀排名: §a#" + info.getKillRank());
        lines.add("§e伤害排名: §a#" + info.getDamageRank());
        lines.add("§e最后击杀: §a" + info.getLastKillTime());
        lines.add("§6§l═══════════════════════════════");
        return lines;
    }

    /**
     * 创建今日统计显示文本
     */
    public List<String> createTodayStatsDisplay(int spawned, int killed, int players,
                                                 double totalDamage, String avgKillTime, String topPlayer) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l═══════════════════════════════");
        lines.add("§6§l  今日统计");
        lines.add("§6§l═══════════════════════════════");
        lines.add("§e生成Boss数: §a" + spawned);
        lines.add("§e击杀Boss数: §a" + killed);
        lines.add("§e参与玩家数: §a" + players);
        lines.add("§e总伤害数: §a" + formatDamage(totalDamage));
        lines.add("§e平均击杀时间: §a" + avgKillTime);
        lines.add("§e最活跃玩家: §a" + topPlayer);
        lines.add("§6§l═══════════════════════════════");
        return lines;
    }

    /**
     * 创建最近Boss显示文本
     */
    public List<String> createRecentBossesDisplay(List<RecentBossInfo> bosses) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l═══════════════════════════════");
        lines.add("§6§l  最近击杀的Boss");
        lines.add("§6§l═══════════════════════════════");

        for (RecentBossInfo boss : bosses) {
            lines.add("§e" + boss.name() + "§7 - " + boss.time() + " (伤害: " + boss.damage() + ")");
        }

        lines.add("§6§l═══════════════════════════════");
        return lines;
    }

    /**
     * 玩家统计信息
     */
    public static class PlayerStatsInfo {
        private final String playerName;
        private final int killCount;
        private final double totalDamage;
        private final double averageDamage;
        private final int killRank;
        private final int damageRank;
        private final String lastKillTime;

        public PlayerStatsInfo(String playerName, int killCount, double totalDamage,
                               double averageDamage, int killRank, int damageRank, String lastKillTime) {
            this.playerName = playerName;
            this.killCount = killCount;
            this.totalDamage = totalDamage;
            this.averageDamage = averageDamage;
            this.killRank = killRank;
            this.damageRank = damageRank;
            this.lastKillTime = lastKillTime;
        }

        public String getPlayerName() { return playerName; }
        public int getKillCount() { return killCount; }
        public double getTotalDamage() { return totalDamage; }
        public double getAverageDamage() { return averageDamage; }
        public int getKillRank() { return killRank; }
        public int getDamageRank() { return damageRank; }
        public String getLastKillTime() { return lastKillTime; }
    }

    /**
     * 最近Boss信息
     */
    public record RecentBossInfo(String name, String time, String damage) {}
}
