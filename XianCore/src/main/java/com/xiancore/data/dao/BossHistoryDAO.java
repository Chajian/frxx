package com.xiancore.data.dao;

import com.xiancore.data.database.IDatabaseAdapter;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.logging.Logger;

/**
 * Boss 历史数据访问对象 (DAO)
 * 管理 Boss 生成和击杀记录
 *
 * @author XianCore
 * @version 1.0
 */
public class BossHistoryDAO {

    private final Plugin plugin;
    private final Logger logger;
    private final IDatabaseAdapter adapter;

    /**
     * Boss 历史记录数据类
     */
    public static class BossHistory {
        public UUID id;
        public String bossType;
        public String world;
        public double x, y, z;
        public long spawnTime;
        public long killTime;
        public UUID killerUuid;
        public String killerName;
        public String loot; // JSON 格式的掉落物品
        public int tier;
        public String status; // SPAWNED, KILLED, DESPAWNED

        public BossHistory(String bossType, String world, double x, double y, double z, int tier) {
            this.id = UUID.randomUUID();
            this.bossType = bossType;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnTime = System.currentTimeMillis();
            this.tier = tier;
            this.status = "SPAWNED";
        }
    }

    /**
     * 构造函数
     */
    public BossHistoryDAO(Plugin plugin, IDatabaseAdapter adapter) {
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
        String sql = "CREATE TABLE IF NOT EXISTS boss_history (" +
                "id TEXT PRIMARY KEY, " +
                "boss_type TEXT NOT NULL, " +
                "world TEXT NOT NULL, " +
                "x REAL, y REAL, z REAL, " +
                "spawn_time LONG NOT NULL, " +
                "kill_time LONG, " +
                "killer_uuid TEXT, " +
                "killer_name TEXT, " +
                "loot TEXT, " +
                "tier INTEGER, " +
                "status TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        if (adapter.createTable(sql)) {
            logger.info("✓ Boss 历史表初始化成功");
        } else {
            logger.warning("✗ Boss 历史表初始化失败");
        }
    }

    /**
     * 保存 Boss 记录
     */
    public boolean saveBossRecord(BossHistory history) {
        if (history == null) {
            logger.warning("✗ 无法保存 null 记录");
            return false;
        }

        String sql = "INSERT INTO boss_history " +
                "(id, boss_type, world, x, y, z, spawn_time, kill_time, killer_uuid, " +
                "killer_name, loot, tier, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        return adapter.preparedExecute(sql,
                history.id.toString(),
                history.bossType,
                history.world,
                history.x, history.y, history.z,
                history.spawnTime,
                history.killTime > 0 ? history.killTime : null,
                history.killerUuid != null ? history.killerUuid.toString() : null,
                history.killerName,
                history.loot,
                history.tier,
                history.status);
    }

    /**
     * 获取 Boss 历史记录
     */
    public BossHistory getBossRecord(UUID id) {
        String sql = "SELECT * FROM boss_history WHERE id = ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, id.toString());

        if (results.isEmpty()) {
            return null;
        }

        return mapRowToHistory(results.get(0));
    }

    /**
     * 查询 Boss 历史
     */
    public List<BossHistory> queryBossHistory(String mobType, String world, int limit) {
        String sql = "SELECT * FROM boss_history WHERE 1=1";
        List<Object> params = new ArrayList<>();

        if (mobType != null && !mobType.isEmpty()) {
            sql += " AND boss_type = ?";
            params.add(mobType);
        }

        if (world != null && !world.isEmpty()) {
            sql += " AND world = ?";
            params.add(world);
        }

        sql += " ORDER BY spawn_time DESC LIMIT ?";
        params.add(limit);

        List<Map<String, Object>> results = adapter.preparedQuery(sql, params.toArray());
        List<BossHistory> histories = new ArrayList<>();

        for (Map<String, Object> row : results) {
            histories.add(mapRowToHistory(row));
        }

        return histories;
    }

    /**
     * 删除 Boss 记录
     */
    public boolean deleteBossRecord(UUID id) {
        String sql = "DELETE FROM boss_history WHERE id = ?";
        return adapter.preparedExecute(sql, id.toString());
    }

    /**
     * 更新 Boss 记录为击杀状态
     */
    public boolean updateBossKilled(UUID id, UUID killerUuid, String killerName, String loot) {
        String sql = "UPDATE boss_history SET kill_time = ?, killer_uuid = ?, " +
                "killer_name = ?, loot = ?, status = 'KILLED' WHERE id = ?";

        return adapter.preparedExecute(sql,
                System.currentTimeMillis(),
                killerUuid != null ? killerUuid.toString() : null,
                killerName,
                loot,
                id.toString());
    }

    /**
     * 获取击杀统计
     */
    public Map<String, Integer> getKillStatistics(String world, int days) {
        Map<String, Integer> stats = new HashMap<>();

        String sql = "SELECT boss_type, COUNT(*) as count FROM boss_history " +
                "WHERE status = 'KILLED' AND kill_time >= ? " +
                (world != null ? "AND world = ? " : "") +
                "GROUP BY boss_type";

        List<Object> params = new ArrayList<>();
        long timeThreshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        params.add(timeThreshold);

        if (world != null) {
            params.add(world);
        }

        List<Map<String, Object>> results = adapter.preparedQuery(sql, params.toArray());

        for (Map<String, Object> row : results) {
            String bossType = (String) row.get("boss_type");
            Number count = (Number) row.get("count");
            stats.put(bossType, count.intValue());
        }

        return stats;
    }

    /**
     * 获取总击杀数
     */
    public int getTotalKills(String world) {
        String sql = "SELECT COUNT(*) as count FROM boss_history WHERE status = 'KILLED'" +
                (world != null ? " AND world = ?" : "");

        List<Object> params = new ArrayList<>();
        if (world != null) {
            params.add(world);
        }

        List<Map<String, Object>> results = world != null ?
                adapter.preparedQuery(sql, params.toArray()) :
                adapter.query(sql);

        if (!results.isEmpty()) {
            Number count = (Number) results.get(0).get("count");
            return count.intValue();
        }

        return 0;
    }

    /**
     * 清理过期记录
     */
    public int deleteOldRecords(int daysOld) {
        String sql = "DELETE FROM boss_history WHERE spawn_time < ?";
        long timeThreshold = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L);

        return adapter.preparedExecute(sql, timeThreshold) ? 1 : 0;
    }

    /**
     * 将数据行映射为对象
     */
    private BossHistory mapRowToHistory(Map<String, Object> row) {
        BossHistory history = new BossHistory(
                (String) row.get("boss_type"),
                (String) row.get("world"),
                ((Number) row.get("x")).doubleValue(),
                ((Number) row.get("y")).doubleValue(),
                ((Number) row.get("z")).doubleValue(),
                ((Number) row.get("tier")).intValue()
        );

        history.id = UUID.fromString((String) row.get("id"));
        history.spawnTime = ((Number) row.get("spawn_time")).longValue();

        Object killTimeObj = row.get("kill_time");
        if (killTimeObj != null) {
            history.killTime = ((Number) killTimeObj).longValue();
        }

        Object killerUuidObj = row.get("killer_uuid");
        if (killerUuidObj != null) {
            history.killerUuid = UUID.fromString((String) killerUuidObj);
        }

        history.killerName = (String) row.get("killer_name");
        history.loot = (String) row.get("loot");
        history.status = (String) row.get("status");

        return history;
    }

    /**
     * 获取表统计信息
     */
    public Map<String, Integer> getTableStatistics() {
        Map<String, Integer> stats = new HashMap<>();

        String totalSql = "SELECT COUNT(*) as count FROM boss_history";
        List<Map<String, Object>> totalResults = adapter.query(totalSql);
        if (!totalResults.isEmpty()) {
            stats.put("total_records", ((Number) totalResults.get(0).get("count")).intValue());
        }

        String killedSql = "SELECT COUNT(*) as count FROM boss_history WHERE status = 'KILLED'";
        List<Map<String, Object>> killedResults = adapter.query(killedSql);
        if (!killedResults.isEmpty()) {
            stats.put("killed_records", ((Number) killedResults.get(0).get("count")).intValue());
        }

        return stats;
    }

    /**
     * 清除所有记录
     */
    public boolean clearAllRecords() {
        return adapter.execute("DELETE FROM boss_history");
    }
}
