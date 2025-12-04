package com.xiancore.data.dao;

import com.xiancore.data.database.IDatabaseAdapter;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.logging.Logger;

/**
 * 掉落物品记录数据访问对象 (DAO)
 * 管理 Boss 掉落的物品记录
 *
 * @author XianCore
 * @version 1.0
 */
public class DropRecordDAO {

    private final Plugin plugin;
    private final Logger logger;
    private final IDatabaseAdapter adapter;

    /**
     * 掉落物品记录数据类
     */
    public static class DropRecord {
        public UUID recordId;
        public UUID bossId;
        public UUID playerUuid;
        public String playerName;
        public String itemType;
        public int quantity;
        public long timestamp;

        public DropRecord(UUID bossId, UUID playerUuid, String playerName, String itemType, int quantity) {
            this.recordId = UUID.randomUUID();
            this.bossId = bossId;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.itemType = itemType;
            this.quantity = quantity;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 构造函数
     */
    public DropRecordDAO(Plugin plugin, IDatabaseAdapter adapter) {
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
        String sql = "CREATE TABLE IF NOT EXISTS drop_records (" +
                "record_id TEXT PRIMARY KEY, " +
                "boss_id TEXT NOT NULL, " +
                "player_uuid TEXT NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "item_type TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "timestamp LONG NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        if (adapter.createTable(sql)) {
            logger.info("✓ 掉落物品表初始化成功");
        } else {
            logger.warning("✗ 掉落物品表初始化失败");
        }
    }

    /**
     * 保存掉落物品记录
     */
    public boolean saveDropRecord(DropRecord record) {
        if (record == null) {
            logger.warning("✗ 无法保存 null 记录");
            return false;
        }

        String sql = "INSERT INTO drop_records " +
                "(record_id, boss_id, player_uuid, player_name, item_type, quantity, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        return adapter.preparedExecute(sql,
                record.recordId.toString(),
                record.bossId.toString(),
                record.playerUuid.toString(),
                record.playerName,
                record.itemType,
                record.quantity,
                record.timestamp);
    }

    /**
     * 保存多个掉落物品记录（批量）
     */
    public boolean saveDropRecords(List<DropRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }

        List<String> sqlList = new ArrayList<>();

        for (DropRecord record : records) {
            String sql = String.format(
                    "INSERT INTO drop_records (record_id, boss_id, player_uuid, player_name, item_type, quantity, timestamp) " +
                    "VALUES ('%s', '%s', '%s', '%s', '%s', %d, %d)",
                    record.recordId.toString(),
                    record.bossId.toString(),
                    record.playerUuid.toString(),
                    record.playerName,
                    record.itemType,
                    record.quantity,
                    record.timestamp);
            sqlList.add(sql);
        }

        return adapter.executeTransaction(sqlList);
    }

    /**
     * 获取掉落记录
     */
    public DropRecord getDropRecord(UUID recordId) {
        String sql = "SELECT * FROM drop_records WHERE record_id = ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, recordId.toString());

        if (results.isEmpty()) {
            return null;
        }

        return mapRowToRecord(results.get(0));
    }

    /**
     * 查询某个 Boss 的所有掉落
     */
    public List<DropRecord> queryDropsByBoss(UUID bossId) {
        String sql = "SELECT * FROM drop_records WHERE boss_id = ? ORDER BY timestamp DESC";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, bossId.toString());
        List<DropRecord> records = new ArrayList<>();

        for (Map<String, Object> row : results) {
            records.add(mapRowToRecord(row));
        }

        return records;
    }

    /**
     * 查询玩家获得的所有掉落
     */
    public List<DropRecord> queryDropsByPlayer(UUID playerUuid, int limit) {
        String sql = "SELECT * FROM drop_records WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, playerUuid.toString(), limit);
        List<DropRecord> records = new ArrayList<>();

        for (Map<String, Object> row : results) {
            records.add(mapRowToRecord(row));
        }

        return records;
    }

    /**
     * 查询特定物品的掉落记录
     */
    public List<DropRecord> queryDropsByItem(String itemType, int limit) {
        String sql = "SELECT * FROM drop_records WHERE item_type = ? ORDER BY timestamp DESC LIMIT ?";
        List<Map<String, Object>> results = adapter.preparedQuery(sql, itemType, limit);
        List<DropRecord> records = new ArrayList<>();

        for (Map<String, Object> row : results) {
            records.add(mapRowToRecord(row));
        }

        return records;
    }

    /**
     * 获取物品掉落统计
     */
    public Map<String, Integer> getItemDropStatistics(int days) {
        Map<String, Integer> stats = new HashMap<>();

        String sql = "SELECT item_type, SUM(quantity) as total_qty FROM drop_records " +
                "WHERE timestamp >= ? GROUP BY item_type ORDER BY total_qty DESC";

        long timeThreshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        List<Map<String, Object>> results = adapter.preparedQuery(sql, timeThreshold);

        for (Map<String, Object> row : results) {
            String itemType = (String) row.get("item_type");
            Number totalQty = (Number) row.get("total_qty");
            stats.put(itemType, totalQty.intValue());
        }

        return stats;
    }

    /**
     * 获取玩家的物品掉落统计
     */
    public Map<String, Integer> getPlayerItemStatistics(UUID playerUuid) {
        Map<String, Integer> stats = new HashMap<>();

        String sql = "SELECT item_type, SUM(quantity) as total_qty FROM drop_records " +
                "WHERE player_uuid = ? GROUP BY item_type ORDER BY total_qty DESC";

        List<Map<String, Object>> results = adapter.preparedQuery(sql, playerUuid.toString());

        for (Map<String, Object> row : results) {
            String itemType = (String) row.get("item_type");
            Number totalQty = (Number) row.get("total_qty");
            stats.put(itemType, totalQty.intValue());
        }

        return stats;
    }

    /**
     * 获取稀有物品掉落列表（特定物品的掉落者）
     */
    public List<Map<String, Object>> getRareItemDrops(String itemType, int days) {
        String sql = "SELECT player_name, player_uuid, timestamp FROM drop_records " +
                "WHERE item_type = ? AND timestamp >= ? " +
                "ORDER BY timestamp DESC";

        long timeThreshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        List<Map<String, Object>> results = adapter.preparedQuery(sql, itemType, timeThreshold);

        return results;
    }

    /**
     * 删除掉落记录
     */
    public boolean deleteDropRecord(UUID recordId) {
        String sql = "DELETE FROM drop_records WHERE record_id = ?";
        return adapter.preparedExecute(sql, recordId.toString());
    }

    /**
     * 删除过期记录
     */
    public int deleteOldRecords(int daysOld) {
        String sql = "DELETE FROM drop_records WHERE timestamp < ?";
        long timeThreshold = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L);

        return adapter.preparedExecute(sql, timeThreshold) ? 1 : 0;
    }

    /**
     * 获取总掉落数
     */
    public int getTotalDrops() {
        String sql = "SELECT COUNT(*) as count FROM drop_records";
        List<Map<String, Object>> results = adapter.query(sql);

        if (!results.isEmpty()) {
            Number count = (Number) results.get(0).get("count");
            return count.intValue();
        }

        return 0;
    }

    /**
     * 获取不同物品类型的数量
     */
    public int getUniqueItemTypes() {
        String sql = "SELECT COUNT(DISTINCT item_type) as count FROM drop_records";
        List<Map<String, Object>> results = adapter.query(sql);

        if (!results.isEmpty()) {
            Number count = (Number) results.get(0).get("count");
            return count.intValue();
        }

        return 0;
    }

    /**
     * 获取掉落统计摘要
     */
    public Map<String, Object> getDropStatisticsSummary() {
        Map<String, Object> summary = new HashMap<>();

        try {
            summary.put("total_drops", getTotalDrops());
            summary.put("unique_item_types", getUniqueItemTypes());

            // 获取最受欢迎的物品
            String topItemSql = "SELECT item_type, SUM(quantity) as total FROM drop_records " +
                    "GROUP BY item_type ORDER BY total DESC LIMIT 5";
            List<Map<String, Object>> topItems = adapter.query(topItemSql);
            summary.put("top_items", topItems);

            // 获取最活跃的玩家
            String topPlayersSql = "SELECT player_name, COUNT(*) as drops FROM drop_records " +
                    "GROUP BY player_uuid ORDER BY drops DESC LIMIT 5";
            List<Map<String, Object>> topPlayers = adapter.query(topPlayersSql);
            summary.put("top_players", topPlayers);

            logger.info("✓ 获取掉落统计摘要成功");

        } catch (Exception e) {
            logger.warning("获取掉落统计摘要失败: " + e.getMessage());
        }

        return summary;
    }

    /**
     * 将数据行映射为对象
     */
    private DropRecord mapRowToRecord(Map<String, Object> row) {
        UUID bossId = UUID.fromString((String) row.get("boss_id"));
        UUID playerUuid = UUID.fromString((String) row.get("player_uuid"));
        String playerName = (String) row.get("player_name");
        String itemType = (String) row.get("item_type");
        int quantity = ((Number) row.get("quantity")).intValue();

        DropRecord record = new DropRecord(bossId, playerUuid, playerName, itemType, quantity);
        record.recordId = UUID.fromString((String) row.get("record_id"));
        record.timestamp = ((Number) row.get("timestamp")).longValue();

        return record;
    }

    /**
     * 清除所有记录
     */
    public boolean clearAllRecords() {
        return adapter.execute("DELETE FROM drop_records");
    }
}
