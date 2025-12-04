package com.xiancore.data.dao;

import com.xiancore.data.database.IDatabaseAdapter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.logging.Logger;

/**
 * 玩家击杀统计数据访问对象 (DAO)
 * 管理玩家的 Boss 击杀记录和排名
 *
 * @author XianCore
 * @version 1.0
 */
public class KillStatisticsDAO {

    private final Plugin plugin;
    private final Logger logger;
    private final IDatabaseAdapter adapter;

    /**
     * 玩家统计数据类
     */
    public static class PlayerStatistics {
        public UUID playerUuid;
        public String playerName;
        public int totalKills;
        public long lastKillTime;
        public double totalDamage;
        public double averageDamage;
        public long firstKillTime;
        public int longestKillStreak;

        public PlayerStatistics(UUID playerUuid, String playerName) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.totalKills = 0;
            this.lastKillTime = 0;
            this.totalDamage = 0;
            this.averageDamage = 0;
            this.firstKillTime = System.currentTimeMillis();
            this.longestKillStreak = 0;
        }
    }

    /**
     * 构造函数
     */
    public KillStatisticsDAO(Plugin plugin, IDatabaseAdapter adapter) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.adapter = adapter;

        // 初始化表
        initializeTable();
    }

    /**
     * 初始化数据表
     */
    private void initializeTable() {
        String sql = "CREATE TABLE IF NOT EXISTS kill_statistics (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "player_name TEXT NOT NULL, " +
                "total_kills INTEGER DEFAULT 0, " +
                "last_kill_time LONG, " +
                "total_damage REAL DEFAULT 0, " +
                "average_damage REAL DEFAULT 0, " +
                "first_kill_time LONG, " +
                "longest_kill_streak INTEGER DEFAULT 0, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        if (adapter.createTable(sql)) {
            logger.info("✓ 玩家统计表初始化成功");
        } else {
            logger.warning("✗ 玩家统计表初始化失败");
        }
    }

    /**
     * 更新玩家击杀统计
     */
    public boolean updateKillStats(Player player, double damageDealt) {
        if (player == null) {
            logger.warning("✗ 玩家对象为 null");
            return false;
        }

        UUID playerUuid = player.getUniqueId();
        String playerName = player.getName();
        long currentTime = System.currentTimeMillis();

        // 获取或创建玩家记录
        PlayerStatistics stats = getPlayerStats(playerUuid);

        if (stats == null) {
            // 创建新记录
            String insertSql = "INSERT INTO kill_statistics " +
                    "(player_uuid, player_name, total_kills, last_kill_time, total_damage, " +
                    "average_damage, first_kill_time) " +
                    "VALUES (?, ?, 1, ?, ?, ?, ?)";

            return adapter.preparedExecute(insertSql,
                    playerUuid.toString(),
                    playerName,
                    currentTime,
                    damageDealt,
                    damageDealt,
                    currentTime);

        } else {
            // 更新现有记录
            int newTotalKills = stats.totalKills + 1;
            double newTotalDamage = stats.totalDamage + damageDealt;
            double newAverageDamage = newTotalDamage / newTotalKills;

            String updateSql = "UPDATE kill_statistics " +
                    "SET total_kills = ?, last_kill_time = ?, total_damage = ?, " +
                    "average_damage = ?, player_name = ? " +
                    "WHERE player_uuid = ?";

            return adapter.preparedExecute(updateSql,
                    newTotalKills,
                    currentTime,
                    newTotalDamage,
                    newAverageDamage,
                    playerName,
                    playerUuid.toString());
        }
    }

    /**
     * 获取玩家统计
     */
    public PlayerStatistics getPlayerStats(UUID playerUuid) {
        String sql = "SELECT * FROM kill_statistics WHERE player_uuid = ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, playerUuid.toString());

        if (results.isEmpty()) {
            return null;
        }

        return mapRowToStats(results.get(0));
    }

    /**
     * 按玩家名称获取统计
     */
    public PlayerStatistics getPlayerStatsByName(String playerName) {
        String sql = "SELECT * FROM kill_statistics WHERE player_name = ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, playerName);

        if (results.isEmpty()) {
            return null;
        }

        return mapRowToStats(results.get(0));
    }

    /**
     * 获取击杀排名
     */
    public List<PlayerStatistics> getRankings(int limit) {
        String sql = "SELECT * FROM kill_statistics ORDER BY total_kills DESC LIMIT ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, limit);
        List<PlayerStatistics> rankings = new ArrayList<>();

        for (Map<String, Object> row : results) {
            rankings.add(mapRowToStats(row));
        }

        return rankings;
    }

    /**
     * 获取伤害排名
     */
    public List<PlayerStatistics> getDamageRankings(int limit) {
        String sql = "SELECT * FROM kill_statistics ORDER BY total_damage DESC LIMIT ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, limit);
        List<PlayerStatistics> rankings = new ArrayList<>();

        for (Map<String, Object> row : results) {
            rankings.add(mapRowToStats(row));
        }

        return rankings;
    }

    /**
     * 获取平均伤害排名
     */
    public List<PlayerStatistics> getAverageDamageRankings(int limit) {
        String sql = "SELECT * FROM kill_statistics WHERE total_kills >= 5 " +
                "ORDER BY average_damage DESC LIMIT ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, limit);
        List<PlayerStatistics> rankings = new ArrayList<>();

        for (Map<String, Object> row : results) {
            rankings.add(mapRowToStats(row));
        }

        return rankings;
    }

    /**
     * 获取玩家排名位置
     */
    public int getPlayerRank(UUID playerUuid) {
        String sql = "SELECT COUNT(*) as rank FROM kill_statistics " +
                "WHERE total_kills > (SELECT total_kills FROM kill_statistics " +
                "WHERE player_uuid = ?)";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, playerUuid.toString());

        if (!results.isEmpty()) {
            Number rank = (Number) results.get(0).get("rank");
            return rank.intValue() + 1; // 排名从 1 开始
        }

        return -1;
    }

    /**
     * 获取总注册玩家数
     */
    public int getTotalPlayers() {
        String sql = "SELECT COUNT(*) as count FROM kill_statistics";
        List<Map<String, Object>> results = adapter.query(sql);

        if (!results.isEmpty()) {
            Number count = (Number) results.get(0).get("count");
            return count.intValue();
        }

        return 0;
    }

    /**
     * 删除玩家记录
     */
    public boolean deletePlayerStats(UUID playerUuid) {
        String sql = "DELETE FROM kill_statistics WHERE player_uuid = ?";
        return adapter.preparedExecute(sql, playerUuid.toString());
    }

    /**
     * 重置玩家统计
     */
    public boolean resetPlayerStats(UUID playerUuid) {
        String sql = "UPDATE kill_statistics " +
                "SET total_kills = 0, last_kill_time = 0, total_damage = 0, " +
                "average_damage = 0, longest_kill_streak = 0 " +
                "WHERE player_uuid = ?";
        return adapter.preparedExecute(sql, playerUuid.toString());
    }

    /**
     * 获取活跃玩家（最近30天有击杀）
     */
    public List<PlayerStatistics> getActivePlayers(int days) {
        String sql = "SELECT * FROM kill_statistics " +
                "WHERE last_kill_time >= ? " +
                "ORDER BY last_kill_time DESC";

        long timeThreshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        List<Map<String, Object>> results = adapter.preparedQuery(sql, timeThreshold);
        List<PlayerStatistics> activePlayers = new ArrayList<>();

        for (Map<String, Object> row : results) {
            activePlayers.add(mapRowToStats(row));
        }

        return activePlayers;
    }

    /**
     * 获取统计摘要
     */
    public Map<String, Object> getStatisticsSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            // 总玩家数
            int totalPlayers = getTotalPlayers();
            summary.put("total_players", totalPlayers);

            // 总击杀数
            String totalKillsSql = "SELECT SUM(total_kills) as sum FROM kill_statistics";
            List<Map<String, Object>> totalKillsResult = adapter.query(totalKillsSql);
            if (!totalKillsResult.isEmpty()) {
                Object sum = totalKillsResult.get(0).get("sum");
                summary.put("total_kills", sum != null ? sum : 0);
            }

            // 总伤害
            String totalDamageSql = "SELECT SUM(total_damage) as sum FROM kill_statistics";
            List<Map<String, Object>> totalDamageResult = adapter.query(totalDamageSql);
            if (!totalDamageResult.isEmpty()) {
                Object sum = totalDamageResult.get(0).get("sum");
                summary.put("total_damage", sum != null ? sum : 0.0);
            }

            // 平均击杀数
            if (totalPlayers > 0) {
                String avgKillsSql = "SELECT AVG(total_kills) as avg FROM kill_statistics";
                List<Map<String, Object>> avgKillsResult = adapter.query(avgKillsSql);
                if (!avgKillsResult.isEmpty()) {
                    Object avg = avgKillsResult.get(0).get("avg");
                    summary.put("average_kills_per_player", avg != null ? avg : 0.0);
                }
            }

            logger.info("✓ 获取统计摘要成功");

        } catch (Exception e) {
            logger.warning("获取统计摘要失败: " + e.getMessage());
        }

        return summary;
    }

    /**
     * 将数据行映射为对象
     */
    private PlayerStatistics mapRowToStats(Map<String, Object> row) {
        String playerUuid = (String) row.get("player_uuid");
        String playerName = (String) row.get("player_name");

        PlayerStatistics stats = new PlayerStatistics(UUID.fromString(playerUuid), playerName);
        stats.totalKills = ((Number) row.get("total_kills")).intValue();
        stats.totalDamage = ((Number) row.get("total_damage")).doubleValue();
        stats.averageDamage = ((Number) row.get("average_damage")).doubleValue();
        stats.longestKillStreak = ((Number) row.get("longest_kill_streak")).intValue();

        Object lastKillTimeObj = row.get("last_kill_time");
        if (lastKillTimeObj != null) {
            stats.lastKillTime = ((Number) lastKillTimeObj).longValue();
        }

        Object firstKillTimeObj = row.get("first_kill_time");
        if (firstKillTimeObj != null) {
            stats.firstKillTime = ((Number) firstKillTimeObj).longValue();
        }

        return stats;
    }

    /**
     * 清除所有记录
     */
    public boolean clearAllRecords() {
        return adapter.execute("DELETE FROM kill_statistics");
    }
}
