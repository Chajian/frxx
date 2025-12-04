package com.xiancore.distributed;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 数据同步管理器 - 跨服务器数据同步与冲突解决
 * Data Sync Manager - Cross-server synchronization and conflict resolution
 *
 * @author XianCore
 * @version 1.0
 */
public class DataSyncManager {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, BossDataVersion> bossVersionMap = new ConcurrentHashMap<>();
    private final Map<String, SyncTransaction> transactionMap = new ConcurrentHashMap<>();
    private final List<SyncCallback> syncCallbacks = new CopyOnWriteArrayList<>();
    private final ConflictResolver conflictResolver = new ConflictResolver();

    private volatile long syncCounter = 0;
    private static final long SYNC_VERSION_INCREMENT = 1;
    private static final long TRANSACTION_TIMEOUT = 30000; // 30秒超时

    /**
     * Boss数据版本控制
     */
    public static class BossDataVersion {
        public String bossId;
        public long version;              // 版本号 (Lamport时钟)
        public String lastUpdatedServer;  // 最后更新的服务器
        public long lastUpdateTime;       // 最后更新时间
        public Map<String, Object> data;  // Boss数据快照
        public String dataHash;           // 数据哈希值 (冲突检测)
        public List<String> updateHistory; // 更新历史 (最近10条)

        public BossDataVersion(String bossId) {
            this.bossId = bossId;
            this.version = 0;
            this.lastUpdateTime = System.currentTimeMillis();
            this.data = new ConcurrentHashMap<>();
            this.updateHistory = new CopyOnWriteArrayList<>();
        }

        public void recordUpdate(String serverId, Map<String, Object> newData) {
            this.version++;
            this.lastUpdatedServer = serverId;
            this.lastUpdateTime = System.currentTimeMillis();
            this.data.putAll(newData);
            this.dataHash = calculateHash(newData);

            String updateRecord = String.format("%s@%s[v%d]", serverId,
                    new Date(lastUpdateTime), version);
            updateHistory.add(updateRecord);
            if (updateHistory.size() > 10) {
                updateHistory.remove(0);
            }
        }

        public String calculateHash(Map<String, Object> data) {
            return String.valueOf(data.toString().hashCode());
        }

        public boolean isConflict(Map<String, Object> remoteData) {
            String remoteHash = calculateHash(remoteData);
            return !this.dataHash.equals(remoteHash);
        }
    }

    /**
     * 同步事务
     */
    public static class SyncTransaction {
        public String transactionId;
        public String bossId;
        public String sourceServer;
        public String targetServer;
        public TransactionState state;   // PENDING/COMMITTED/ROLLED_BACK
        public long startTime;
        public long endTime;
        public Map<String, Object> originalData;
        public Map<String, Object> newData;
        public List<String> affectedServers;

        public enum TransactionState {
            PENDING, COMMITTED, ROLLED_BACK, CONFLICTED
        }

        public SyncTransaction(String transactionId, String bossId, String sourceServer) {
            this.transactionId = transactionId;
            this.bossId = bossId;
            this.sourceServer = sourceServer;
            this.state = TransactionState.PENDING;
            this.startTime = System.currentTimeMillis();
            this.originalData = new ConcurrentHashMap<>();
            this.newData = new ConcurrentHashMap<>();
            this.affectedServers = new CopyOnWriteArrayList<>();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > TRANSACTION_TIMEOUT;
        }
    }

    /**
     * 冲突解决策略
     */
    public static class ConflictResolver {
        public enum ConflictResolutionStrategy {
            LAST_WRITE_WINS,           // 最后写入者胜利
            FIRST_WRITE_WINS,          // 第一个写入者胜利
            VERSION_BASED,             // 基于版本号
            MERGE,                     // 合并
            CUSTOM                     // 自定义回调
        }

        private ConflictResolutionStrategy strategy = ConflictResolutionStrategy.LAST_WRITE_WINS;

        public void setStrategy(ConflictResolutionStrategy strategy) {
            this.strategy = strategy;
        }

        public Map<String, Object> resolve(
                BossDataVersion local,
                Map<String, Object> remote,
                long remoteVersion) {

            switch (strategy) {
                case LAST_WRITE_WINS:
                    return remote;  // 远程数据（最新的）
                case FIRST_WRITE_WINS:
                    return local.data;  // 本地数据（先到的）
                case VERSION_BASED:
                    return remoteVersion > local.version ? remote : local.data;
                case MERGE:
                    return mergeData(local.data, remote);
                default:
                    return remote;
            }
        }

        private Map<String, Object> mergeData(Map<String, Object> local, Map<String, Object> remote) {
            Map<String, Object> merged = new HashMap<>(local);

            // 合并策略：保留数值字段的较大值，时间戳保留最新的
            for (Map.Entry<String, Object> entry : remote.entrySet()) {
                String key = entry.getKey();
                Object remoteValue = entry.getValue();
                Object localValue = merged.get(key);

                if (localValue == null) {
                    merged.put(key, remoteValue);
                } else if (isNumeric(localValue) && isNumeric(remoteValue)) {
                    double localNum = ((Number) localValue).doubleValue();
                    double remoteNum = ((Number) remoteValue).doubleValue();
                    merged.put(key, Math.max(localNum, remoteNum));
                }
            }

            return merged;
        }

        private boolean isNumeric(Object obj) {
            return obj instanceof Number;
        }
    }

    /**
     * 同步回调接口
     */
    public interface SyncCallback {
        void onSyncStart(String bossId);
        void onSyncSuccess(String bossId, long version);
        void onSyncConflict(String bossId, SyncTransaction transaction);
        void onSyncFailure(String bossId, String reason);
    }

    /**
     * 构造函数
     */
    public DataSyncManager() {
        logger.info("✓ DataSyncManager已初始化");
    }

    /**
     * 启动数据同步
     */
    public void startSync(String bossId, Map<String, Object> bossData, String serverId) {
        logger.info("↻ 开始数据同步: " + bossId + " from " + serverId);

        BossDataVersion version = bossVersionMap.computeIfAbsent(bossId, BossDataVersion::new);

        // 触发同步开始回调
        syncCallbacks.forEach(cb -> cb.onSyncStart(bossId));

        version.recordUpdate(serverId, bossData);
        syncCounter++;

        // 触发同步成功回调
        syncCallbacks.forEach(cb -> cb.onSyncSuccess(bossId, version.version));
    }

    /**
     * 处理远程数据同步
     */
    public SyncResult handleRemoteSync(String bossId, Map<String, Object> remoteData,
                                       long remoteVersion, String remoteServer) {
        BossDataVersion local = bossVersionMap.get(bossId);

        if (local == null) {
            // 新的Boss，直接创建
            local = new BossDataVersion(bossId);
            local.recordUpdate(remoteServer, remoteData);
            bossVersionMap.put(bossId, local);
            return new SyncResult(true, "NEW_DATA_ACCEPTED", remoteVersion);
        }

        // 检查冲突
        if (local.isConflict(remoteData) && remoteVersion <= local.version) {
            // 发现冲突
            SyncTransaction transaction = new SyncTransaction(
                    UUID.randomUUID().toString(), bossId, remoteServer
            );
            transaction.originalData.putAll(local.data);
            transaction.newData.putAll(remoteData);
            transaction.state = SyncTransaction.TransactionState.CONFLICTED;

            transactionMap.put(transaction.transactionId, transaction);
            syncCallbacks.forEach(cb -> cb.onSyncConflict(bossId, transaction));

            return new SyncResult(false, "CONFLICT_DETECTED", local.version);
        }

        // 解决冲突或直接更新
        Map<String, Object> resolvedData = conflictResolver.resolve(local, remoteData, remoteVersion);
        local.recordUpdate(remoteServer, resolvedData);

        return new SyncResult(true, "SYNCED", local.version);
    }

    /**
     * 同步结果
     */
    public static class SyncResult {
        public boolean success;
        public String message;
        public long currentVersion;

        public SyncResult(boolean success, String message, long currentVersion) {
            this.success = success;
            this.message = message;
            this.currentVersion = currentVersion;
        }
    }

    /**
     * 提交事务
     */
    public void commitTransaction(String transactionId) {
        SyncTransaction transaction = transactionMap.get(transactionId);
        if (transaction != null) {
            transaction.state = SyncTransaction.TransactionState.COMMITTED;
            transaction.endTime = System.currentTimeMillis();
            logger.info("✓ 事务已提交: " + transactionId);
        }
    }

    /**
     * 回滚事务
     */
    public void rollbackTransaction(String transactionId) {
        SyncTransaction transaction = transactionMap.get(transactionId);
        if (transaction != null) {
            transaction.state = SyncTransaction.TransactionState.ROLLED_BACK;
            transaction.endTime = System.currentTimeMillis();
            logger.info("↶ 事务已回滚: " + transactionId);
        }
    }

    /**
     * 获取Boss数据版本
     */
    public BossDataVersion getBossVersion(String bossId) {
        return bossVersionMap.get(bossId);
    }

    /**
     * 获取所有Boss版本
     */
    public Collection<BossDataVersion> getAllBossVersions() {
        return bossVersionMap.values();
    }

    /**
     * 清理过期事务
     */
    public int cleanupExpiredTransactions() {
        int count = 0;
        Iterator<SyncTransaction> iterator = transactionMap.values().iterator();

        while (iterator.hasNext()) {
            SyncTransaction transaction = iterator.next();
            if (transaction.isExpired()) {
                if (transaction.state == SyncTransaction.TransactionState.PENDING) {
                    transaction.state = SyncTransaction.TransactionState.ROLLED_BACK;
                }
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            logger.info("✓ 清理过期事务: " + count + "个");
        }
        return count;
    }

    /**
     * 获取同步统计
     */
    public Map<String, Object> getSyncStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_syncs", syncCounter);
        stats.put("tracked_bosses", bossVersionMap.size());
        stats.put("pending_transactions", transactionMap.values().stream()
                .filter(t -> t.state == SyncTransaction.TransactionState.PENDING)
                .count());
        stats.put("conflicted_transactions", transactionMap.values().stream()
                .filter(t -> t.state == SyncTransaction.TransactionState.CONFLICTED)
                .count());

        // 最高版本
        long maxVersion = bossVersionMap.values().stream()
                .mapToLong(v -> v.version)
                .max()
                .orElse(0);
        stats.put("max_version", maxVersion);

        // 平均版本
        double avgVersion = bossVersionMap.values().stream()
                .mapToLong(v -> v.version)
                .average()
                .orElse(0.0);
        stats.put("avg_version", String.format("%.2f", avgVersion));

        return stats;
    }

    /**
     * 获取同步历史
     */
    public List<Map<String, Object>> getSyncHistory(int limit) {
        return bossVersionMap.values().stream()
                .limit(limit)
                .map(v -> {
                    Map<String, Object> history = new LinkedHashMap<>();
                    history.put("boss_id", v.bossId);
                    history.put("version", v.version);
                    history.put("last_updated_server", v.lastUpdatedServer);
                    history.put("last_update_time", v.lastUpdateTime);
                    history.put("update_history", v.updateHistory);
                    return history;
                })
                .toList();
    }

    /**
     * 设置冲突解决策略
     */
    public void setConflictResolutionStrategy(ConflictResolver.ConflictResolutionStrategy strategy) {
        conflictResolver.setStrategy(strategy);
        logger.info("✓ 冲突解决策略已设置: " + strategy.name());
    }

    /**
     * 添加同步回调
     */
    public void addSyncCallback(SyncCallback callback) {
        syncCallbacks.add(callback);
    }

    /**
     * 移除同步回调
     */
    public void removeSyncCallback(SyncCallback callback) {
        syncCallbacks.remove(callback);
    }

    /**
     * 重置所有数据
     */
    public void reset() {
        bossVersionMap.clear();
        transactionMap.clear();
        syncCounter = 0;
        logger.info("✓ 所有同步数据已重置");
    }

    /**
     * 获取系统概览
     */
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("sync_counter", syncCounter);
        overview.put("tracked_bosses", bossVersionMap.size());
        overview.put("active_transactions", transactionMap.size());
        overview.put("statistics", getSyncStatistics());
        overview.put("conflict_resolution_strategy", conflictResolver.strategy.name());
        overview.put("recent_syncs", getSyncHistory(5));
        return overview;
    }
}
