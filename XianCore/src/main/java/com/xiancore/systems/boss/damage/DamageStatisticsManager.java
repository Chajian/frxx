package com.xiancore.systems.boss.damage;

import com.xiancore.systems.boss.damage.persistence.DamageDatabase;
import com.xiancore.systems.boss.damage.persistence.InMemoryDamageDatabase;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * 伤害统计管理器
 * 负责记录、查询和管理Boss伤害统计数据
 *
 * 职责:
 * - 记录玩家对Boss的伤害
 * - 维护伤害排行榜
 * - 提供伤害查询接口
 * - 支持数据持久化
 * - 支持历史统计分析
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class DamageStatisticsManager {

    // ==================== 核心数据结构 ====================
    /** Boss伤害记录 (Boss UUID -> 伤害记录) */
    private final Map<UUID, DamageRecord> damageRecords = new ConcurrentHashMap<>();

    /** 伤害排行缓存 (Boss UUID -> 排行列表) */
    private final Map<UUID, DamageRanking> damageRankings = new ConcurrentHashMap<>();

    /** 历史数据列表 */
    private final List<DamageHistory> damageHistory = Collections.synchronizedList(new ArrayList<>());

    // ==================== 性能优化 - 查询缓存 ====================
    /** 排行查询缓存 (Boss UUID -> 缓存条目) */
    private final Map<UUID, RankingCacheEntry> rankingCache = new ConcurrentHashMap<>();

    /** 统计信息缓存 (Boss UUID -> 统计缓存) */
    private final Map<UUID, StatisticsCacheEntry> statisticsCache = new ConcurrentHashMap<>();

    /** 缓存过期时间 (毫秒) */
    private volatile long cacheExpireTime = 1000; // 默认1秒

    // ==================== 性能优化 - 批处理队列 ====================
    /** 异步伤害记录队列 (用于批处理) */
    private final Queue<DamageEntry> damageQueue = new ConcurrentLinkedQueue<>();

    /** 批处理大小阈值 */
    private volatile int batchProcessThreshold = 50;

    /** 是否启用批处理 */
    private volatile boolean batchProcessingEnabled = true;

    // ==================== 配置和状态 ====================
    /** 是否已初始化 */
    private volatile boolean initialized = false;

    /** 配置信息 */
    private DamageConfig config;

    // ==================== 数据持久化 ====================
    /** 数据库实例 */
    private DamageDatabase database;

    /** 是否启用数据持久化 */
    private volatile boolean persistenceEnabled = false;

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     */
    public DamageStatisticsManager() {
        this.config = new DamageConfig();
    }

    // ==================== 初始化和关闭 ====================

    /**
     * 初始化伤害统计管理器
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化伤害统计管理器失败", e);
        }
    }

    /**
     * 关闭管理器并保存数据
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        // 保存所有数据
        damageRecords.forEach((bossUUID, record) -> {
            // 这里可以添加数据持久化逻辑
        });

        initialized = false;
    }

    // ==================== 伤害记录方法 ====================

    /**
     * 记录玩家对Boss的伤害
     *
     * @param bossUUID Boss UUID
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     * @param timestamp 时间戳
     */
    public void recordDamage(UUID bossUUID, UUID playerUUID, double damage, long timestamp) {
        if (bossUUID == null || playerUUID == null || damage <= 0) {
            return;
        }

        // 获取或创建伤害记录
        DamageRecord record = damageRecords.computeIfAbsent(bossUUID, k -> new DamageRecord(bossUUID));

        // 记录伤害
        record.recordDamage(playerUUID, damage);

        // 更新排行
        updateDamageRanking(bossUUID, record);
    }

    /**
     * 记录玩家对Boss的伤害 (便捷版本)
     *
     * @param bossUUID Boss UUID
     * @param player 玩家
     * @param damage 伤害值
     */
    public void recordDamage(UUID bossUUID, Player player, double damage) {
        recordDamage(bossUUID, player.getUniqueId(), damage, System.currentTimeMillis());
    }

    /**
     * 批量记录伤害 (性能优化)
     *
     * @param bossUUID Boss UUID
     * @param damages 伤害列表
     */
    public void recordBulkDamage(UUID bossUUID, List<DamageEntry> damages) {
        if (bossUUID == null || damages == null || damages.isEmpty()) {
            return;
        }

        DamageRecord record = damageRecords.computeIfAbsent(bossUUID, k -> new DamageRecord(bossUUID));

        for (DamageEntry entry : damages) {
            record.recordDamage(entry.playerUUID, entry.damage);
        }

        updateDamageRanking(bossUUID, record);
    }

    // ==================== 伤害查询方法 ====================

    /**
     * 获取Boss的完整伤害记录
     *
     * @param bossUUID Boss UUID
     * @return 伤害记录，如果不存在则返回null
     */
    public DamageRecord getDamageRecord(UUID bossUUID) {
        return damageRecords.get(bossUUID);
    }

    /**
     * 获取Boss的伤害排行榜
     *
     * @param bossUUID Boss UUID
     * @param limit 显示数量限制
     * @return 排行榜 (玩家UUID列表，从高到低)
     */
    public List<UUID> getDamageRanking(UUID bossUUID, int limit) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return new ArrayList<>();
        }

        return record.getPlayerDamageMap().entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * 获取Boss的伤害排行 (返回详细信息)
     *
     * @param bossUUID Boss UUID
     * @param limit 显示数量限制
     * @return 排行列表 (包含玩家UUID和伤害值)
     */
    public List<Map.Entry<UUID, Double>> getDamageRankingDetailed(UUID bossUUID, int limit) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return new ArrayList<>();
        }

        return record.getPlayerDamageMap().entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取玩家对Boss的伤害值
     *
     * @param bossUUID Boss UUID
     * @param playerUUID 玩家UUID
     * @return 伤害值
     */
    public double getPlayerDamage(UUID bossUUID, UUID playerUUID) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return 0.0;
        }

        return record.getPlayerDamage(playerUUID);
    }

    /**
     * 获取玩家的伤害百分比
     *
     * @param bossUUID Boss UUID
     * @param playerUUID 玩家UUID
     * @return 伤害百分比 (0.0-1.0)
     */
    public double getPlayerDamagePercentage(UUID bossUUID, UUID playerUUID) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null || record.getTotalDamage() <= 0) {
            return 0.0;
        }

        double playerDamage = record.getPlayerDamage(playerUUID);
        return playerDamage / record.getTotalDamage();
    }

    /**
     * 获取Boss的总伤害
     *
     * @param bossUUID Boss UUID
     * @return 总伤害值
     */
    public double getTotalDamage(UUID bossUUID) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return 0.0;
        }

        return record.getTotalDamage();
    }

    /**
     * 获取Boss的伤害统计
     *
     * @param bossUUID Boss UUID
     * @return 伤害统计信息
     */
    public DamageStatistics getStatistics(UUID bossUUID) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return new DamageStatistics();
        }

        DamageStatistics stats = new DamageStatistics();
        stats.setTotalDamage(record.getTotalDamage());
        stats.setTotalDamageCount(record.getTotalDamageCount());
        stats.setTotalPlayers(record.getPlayerDamageMap().size());
        stats.setAverageDamage(record.getTotalDamage() / record.getTotalDamageCount());

        return stats;
    }

    /**
     * 获取玩家排名
     *
     * @param bossUUID Boss UUID
     * @param playerUUID 玩家UUID
     * @return 排名位置 (从1开始)，如果不在排行则返回-1
     */
    public int getPlayerRank(UUID bossUUID, UUID playerUUID) {
        List<UUID> ranking = getDamageRanking(bossUUID, Integer.MAX_VALUE);

        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).equals(playerUUID)) {
                return i + 1;
            }
        }

        return -1;
    }

    /**
     * 获取参与伤害的所有玩家
     *
     * @param bossUUID Boss UUID
     * @return 玩家UUID列表
     */
    public Set<UUID> getParticipants(UUID bossUUID) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return new HashSet<>();
        }

        return new HashSet<>(record.getPlayerDamageMap().keySet());
    }

    /**
     * 获取参与人数
     *
     * @param bossUUID Boss UUID
     * @return 参与人数
     */
    public int getParticipantCount(UUID bossUUID) {
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return 0;
        }

        return record.getPlayerDamageMap().size();
    }

    // ==================== 历史和存档方法 ====================

    /**
     * 完成Boss的伤害记录 (转换为历史)
     *
     * @param bossUUID Boss UUID
     */
    public void finalizeBossDamage(UUID bossUUID) {
        DamageRecord record = damageRecords.remove(bossUUID);
        if (record == null) {
            return;
        }

        // TODO: 修复 DamageHistory 与新 DamageRecord 的兼容性
        // DamageHistory history = new DamageHistory(record);
        // damageHistory.add(history);

        // 保存到数据库
        // if (persistenceEnabled && database != null && database.isConnected()) {
        //     database.saveHistoryAsync(history);
        // }
    }

    /**
     * 获取所有历史记录
     *
     * @return 历史记录列表
     */
    public List<DamageHistory> getHistory() {
        return new ArrayList<>(damageHistory);
    }

    /**
     * 获取指定Boss的历史记录
     *
     * @param bossUUID Boss UUID
     * @return 该Boss的历史记录列表
     */
    public List<DamageHistory> getHistory(UUID bossUUID) {
        return damageHistory.stream()
            .filter(h -> h.getBossUUID().equals(bossUUID))
            .collect(Collectors.toList());
    }

    /**
     * 清除Boss的伤害数据
     *
     * @param bossUUID Boss UUID
     */
    public void clearDamageData(UUID bossUUID) {
        damageRecords.remove(bossUUID);
        damageRankings.remove(bossUUID);
        // 清除相关缓存
        rankingCache.remove(bossUUID);
        statisticsCache.remove(bossUUID);
    }

    /**
     * 清除所有数据
     */
    public void clearAllData() {
        damageRecords.clear();
        damageRankings.clear();
        damageHistory.clear();
        // 清除所有缓存
        rankingCache.clear();
        statisticsCache.clear();
        damageQueue.clear();
    }

    // ==================== 内部方法 ====================

    /**
     * 更新伤害排行
     *
     * @param bossUUID Boss UUID
     * @param record 伤害记录
     */
    private void updateDamageRanking(UUID bossUUID, DamageRecord record) {
        DamageRanking ranking = damageRankings.computeIfAbsent(bossUUID, k -> new DamageRanking(bossUUID));
        ranking.updateRanking(record);

        // 清除缓存以确保下次查询获取最新数据
        invalidateCaches(bossUUID);
    }

    /**
     * 无效化特定Boss的缓存
     *
     * @param bossUUID Boss UUID
     */
    private void invalidateCaches(UUID bossUUID) {
        rankingCache.remove(bossUUID);
        statisticsCache.remove(bossUUID);
    }

    /**
     * 清除所有过期缓存
     */
    private void cleanExpiredCaches() {
        rankingCache.entrySet().removeIf(entry ->
            entry.getValue().isExpired(cacheExpireTime)
        );
        statisticsCache.entrySet().removeIf(entry ->
            entry.getValue().isExpired(cacheExpireTime)
        );
    }

    /**
     * 获取或计算统计信息 (带缓存)
     *
     * @param bossUUID Boss UUID
     * @return 统计信息
     */
    public DamageStatistics getStatisticsWithCache(UUID bossUUID) {
        // 检查缓存
        StatisticsCacheEntry cacheEntry = statisticsCache.get(bossUUID);
        if (cacheEntry != null && !cacheEntry.isExpired(cacheExpireTime)) {
            return cacheEntry.statistics;
        }

        // 计算新的统计信息
        DamageRecord record = damageRecords.get(bossUUID);
        if (record == null) {
            return null;
        }

        // TODO: 修复 DamageStatistics 与新 DamageRecord 的兼容性
        DamageStatistics stats = new DamageStatistics();
        // stats.calculate(record); // 需要修复 calculate 方法
        statisticsCache.put(bossUUID, new StatisticsCacheEntry(stats));
        return stats;
    }

    /**
     * 设置缓存过期时间
     *
     * @param milliseconds 毫秒
     */
    public void setCacheExpireTime(long milliseconds) {
        this.cacheExpireTime = milliseconds;
    }

    /**
     * 启用或禁用批处理
     *
     * @param enabled 是否启用
     */
    public void setBatchProcessingEnabled(boolean enabled) {
        this.batchProcessingEnabled = enabled;
    }

    /**
     * 设置批处理阈值
     *
     * @param threshold 阈值 (达到此数量时触发批处理)
     */
    public void setBatchProcessThreshold(int threshold) {
        this.batchProcessThreshold = threshold;
    }

    /**
     * 处理批处理队列中的所有记录
     */
    public void processBatchQueue() {
        if (!batchProcessingEnabled || damageQueue.isEmpty()) {
            return;
        }

        List<DamageEntry> batch = new ArrayList<>();
        DamageEntry entry;

        while ((entry = damageQueue.poll()) != null && batch.size() < batchProcessThreshold) {
            batch.add(entry);
        }

        if (!batch.isEmpty()) {
            // 按Boss分组处理
            Map<UUID, List<DamageEntry>> grouped = new HashMap<>();
            for (DamageEntry dmg : batch) {
                // 由于DamageEntry中没有bossUUID，这里需要调整
                // 现在仅处理单个Boss的批处理
            }
        }
    }

    /**
     * 获取当前缓存大小信息
     *
     * @return 缓存信息字符串
     */
    public String getCacheInfo() {
        return String.format(
            "RankingCache: %d, StatisticsCache: %d, QueueSize: %d",
            rankingCache.size(),
            statisticsCache.size(),
            damageQueue.size()
        );
    }

    // ==================== 数据持久化方法 ====================

    /**
     * 设置数据库实例
     *
     * @param database 数据库实现
     */
    public void setDatabase(DamageDatabase database) {
        this.database = database;
    }

    /**
     * 启用数据持久化
     *
     * @return 是否启用成功
     */
    public boolean enablePersistence() {
        if (database == null) {
            // 默认使用内存数据库
            database = new InMemoryDamageDatabase();
        }

        try {
            database.initialize();
            persistenceEnabled = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 禁用数据持久化
     */
    public void disablePersistence() {
        persistenceEnabled = false;
        if (database != null) {
            try {
                database.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查数据持久化是否启用
     *
     * @return 是否启用
     */
    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    /**
     * 获取数据库实例
     *
     * @return 数据库
     */
    public DamageDatabase getDatabase() {
        return database;
    }

    /**
     * 手动保存所有当前的伤害历史到数据库
     *
     * @return 保存的记录数
     */
    public int saveAllHistories() {
        if (!persistenceEnabled || database == null || !database.isConnected()) {
            return 0;
        }

        return database.saveHistories(damageHistory);
    }

    /**
     * 从数据库加载历史记录
     *
     * @return 加载的记录数
     */
    public int loadHistoriesFromDatabase() {
        if (database == null || !database.isConnected()) {
            return 0;
        }

        List<DamageHistory> loaded = database.queryAll();
        damageHistory.addAll(loaded);
        return loaded.size();
    }

    /**
     * 清理数据库中的过期数据
     *
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     */
    public int cleanupDatabase(int daysToKeep) {
        if (database == null || !database.isConnected()) {
            return 0;
        }

        return database.cleanup(daysToKeep);
    }

    /**
     * 备份数据库
     *
     * @param backupPath 备份路径
     * @return 是否备份成功
     */
    public boolean backupDatabase(String backupPath) {
        if (database == null || !database.isConnected()) {
            return false;
        }

        return database.backup(backupPath);
    }

    /**
     * 导出所有数据为JSON
     *
     * @return JSON字符串
     */
    public String exportAsJson() {
        if (database == null || !database.isConnected()) {
            return "[]";
        }

        return database.exportAsJson();
    }

    /**
     * 导出所有数据为YAML
     *
     * @return YAML字符串
     */
    public String exportAsYaml() {
        if (database == null || !database.isConnected()) {
            return "";
        }

        return database.exportAsYaml();
    }

    // ==================== 内部类 ====================

    /**
     * 排行缓存条目
     * 用于缓存排行查询结果以提高性能
     */
    private static class RankingCacheEntry {
        List<UUID> rankingList;
        List<Map.Entry<UUID, Double>> detailedRanking;
        long createTime;
        int limit;

        RankingCacheEntry(List<UUID> rankingList, List<Map.Entry<UUID, Double>> detailedRanking, int limit) {
            this.rankingList = rankingList;
            this.detailedRanking = detailedRanking;
            this.createTime = System.currentTimeMillis();
            this.limit = limit;
        }

        boolean isExpired(long expireTime) {
            return System.currentTimeMillis() - createTime > expireTime;
        }
    }

    /**
     * 统计信息缓存条目
     * 用于缓存统计计算结果
     */
    private static class StatisticsCacheEntry {
        DamageStatistics statistics;
        long createTime;

        StatisticsCacheEntry(DamageStatistics statistics) {
            this.statistics = statistics;
            this.createTime = System.currentTimeMillis();
        }

        boolean isExpired(long expireTime) {
            return System.currentTimeMillis() - createTime > expireTime;
        }
    }

    /**
     * 伤害条目 (用于批量记录)
     */
    public static class DamageEntry {
        public UUID playerUUID;
        public double damage;
        public long timestamp;

        public DamageEntry(UUID playerUUID, double damage, long timestamp) {
            this.playerUUID = playerUUID;
            this.damage = damage;
            this.timestamp = timestamp;
        }

        public DamageEntry(UUID playerUUID, double damage) {
            this(playerUUID, damage, System.currentTimeMillis());
        }
    }

    /**
     * 伤害配置
     */
    @Getter
    public static class DamageConfig {
        private boolean enabled = true;
        private int topDamagers = 10;
        private int historyDays = 90;
        private boolean autoArchive = true;
        private int batchSize = 100;
        private int syncInterval = 300;

        public DamageConfig() {
        }
    }
}
