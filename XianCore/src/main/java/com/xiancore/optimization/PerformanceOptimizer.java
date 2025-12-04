package com.xiancore.optimization;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 性能优化系统 - 缓存、内存、数据库优化
 * Performance Optimization System - Caching, Memory, Database Optimization
 *
 * @author XianCore
 * @version 1.0
 */
public class PerformanceOptimizer {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final CacheManager cacheManager;
    private final MemoryOptimizer memoryOptimizer;
    private final DatabaseOptimizer databaseOptimizer;

    /**
     * 缓存管理器
     */
    public static class CacheManager {
        private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
        private final long defaultTTL = 300000;  // 5分钟默认TTL
        private volatile long hitCount = 0;
        private volatile long missCount = 0;

        /**
         * 缓存条目
         */
        public static class CacheEntry<T> {
            public String key;
            public T value;
            public long createdTime;
            public long ttl;
            public long accessCount;

            public CacheEntry(String key, T value, long ttl) {
                this.key = key;
                this.value = value;
                this.createdTime = System.currentTimeMillis();
                this.ttl = ttl;
                this.accessCount = 0;
            }

            public boolean isExpired() {
                return System.currentTimeMillis() - createdTime > ttl;
            }
        }

        /**
         * 添加缓存
         */
        public <T> void put(String key, T value, long ttl) {
            cache.put(key, new CacheEntry<>(key, value, ttl));
        }

        /**
         * 添加缓存 (使用默认TTL)
         */
        public <T> void put(String key, T value) {
            put(key, value, defaultTTL);
        }

        /**
         * 获取缓存
         */
        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            CacheEntry<?> entry = cache.get(key);
            if (entry == null) {
                missCount++;
                return null;
            }

            if (entry.isExpired()) {
                cache.remove(key);
                missCount++;
                return null;
            }

            entry.accessCount++;
            hitCount++;
            return (T) entry.value;
        }

        /**
         * 缓存统计
         */
        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0 : (double) hitCount / total * 100;
        }

        /**
         * 清理过期缓存
         */
        public int cleanup() {
            int removed = 0;
            Iterator<Map.Entry<String, CacheEntry<?>>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getValue().isExpired()) {
                    iterator.remove();
                    removed++;
                }
            }
            return removed;
        }

        /**
         * 获取缓存大小
         */
        public int size() {
            return cache.size();
        }

        /**
         * 清空缓存
         */
        public void clear() {
            cache.clear();
            hitCount = 0;
            missCount = 0;
        }

        /**
         * 获取统计信息
         */
        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("cache_size", size());
            stats.put("hit_count", hitCount);
            stats.put("miss_count", missCount);
            stats.put("hit_rate", String.format("%.2f%%", getHitRate()));
            stats.put("total_access", hitCount + missCount);
            return stats;
        }
    }

    /**
     * 内存优化器
     */
    public static class MemoryOptimizer {
        private final Runtime runtime = Runtime.getRuntime();
        private final List<Long> memoryHistory = new CopyOnWriteArrayList<>();
        private volatile long lastGC = System.currentTimeMillis();

        /**
         * 获取内存使用信息
         */
        public MemoryInfo getMemoryInfo() {
            MemoryInfo info = new MemoryInfo();
            info.totalMemory = runtime.totalMemory();
            info.freeMemory = runtime.freeMemory();
            info.maxMemory = runtime.maxMemory();
            info.usedMemory = info.totalMemory - info.freeMemory;
            info.usagePercent = (info.usedMemory / (double) info.maxMemory) * 100;
            return info;
        }

        public static class MemoryInfo {
            public long totalMemory;
            public long freeMemory;
            public long maxMemory;
            public long usedMemory;
            public double usagePercent;
        }

        /**
         * 强制垃圾回收
         */
        public void forceGC() {
            System.gc();
            lastGC = System.currentTimeMillis();
        }

        /**
         * 条件垃圾回收 (内存超过80%时)
         */
        public boolean conditionalGC() {
            MemoryInfo info = getMemoryInfo();
            if (info.usagePercent > 80) {
                forceGC();
                return true;
            }
            return false;
        }

        /**
         * 记录内存历史
         */
        public void recordMemoryUsage() {
            memoryHistory.add(getMemoryInfo().usedMemory);
            if (memoryHistory.size() > 1000) {
                memoryHistory.remove(0);
            }
        }

        /**
         * 获取平均内存使用
         */
        public long getAverageMemoryUsage() {
            if (memoryHistory.isEmpty()) return 0;
            return (long) memoryHistory.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);
        }

        /**
         * 获取统计信息
         */
        public Map<String, Object> getStatistics() {
            MemoryInfo info = getMemoryInfo();
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total_memory", formatBytes(info.totalMemory));
            stats.put("used_memory", formatBytes(info.usedMemory));
            stats.put("free_memory", formatBytes(info.freeMemory));
            stats.put("max_memory", formatBytes(info.maxMemory));
            stats.put("usage_percent", String.format("%.2f%%", info.usagePercent));
            stats.put("avg_memory", formatBytes(getAverageMemoryUsage()));
            return stats;
        }

        private String formatBytes(long bytes) {
            if (bytes <= 0) return "0B";
            final String[] units = new String[]{"B", "KB", "MB", "GB"};
            int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
            return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
        }
    }

    /**
     * 数据库优化器
     */
    public static class DatabaseOptimizer {
        private final Map<String, QueryCache> queryCache = new ConcurrentHashMap<>();
        private volatile long queryCount = 0;
        private volatile long cacheHits = 0;

        public static class QueryCache {
            public String query;
            public List<Map<String, Object>> results;
            public long cachedTime;
            public long ttl;

            public QueryCache(String query, List<Map<String, Object>> results, long ttl) {
                this.query = query;
                this.results = new ArrayList<>(results);
                this.cachedTime = System.currentTimeMillis();
                this.ttl = ttl;
            }

            public boolean isExpired() {
                return System.currentTimeMillis() - cachedTime > ttl;
            }
        }

        /**
         * 执行查询 (带缓存)
         */
        public List<Map<String, Object>> executeQuery(String query) {
            queryCount++;

            // 检查缓存
            QueryCache cached = queryCache.get(query);
            if (cached != null && !cached.isExpired()) {
                cacheHits++;
                return new ArrayList<>(cached.results);
            }

            // 执行查询并缓存结果
            List<Map<String, Object>> results = new ArrayList<>();  // 实际应执行数据库查询
            queryCache.put(query, new QueryCache(query, results, 60000));  // 缓存60秒

            return results;
        }

        /**
         * 清理过期查询缓存
         */
        public int cleanupQueryCache() {
            int removed = 0;
            Iterator<QueryCache> iterator = queryCache.values().iterator();
            while (iterator.hasNext()) {
                if (iterator.next().isExpired()) {
                    iterator.remove();
                    removed++;
                }
            }
            return removed;
        }

        /**
         * 获取统计信息
         */
        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("total_queries", queryCount);
            stats.put("cache_hits", cacheHits);
            stats.put("query_cache_size", queryCache.size());
            double hitRate = queryCount == 0 ? 0 : (double) cacheHits / queryCount * 100;
            stats.put("cache_hit_rate", String.format("%.2f%%", hitRate));
            return stats;
        }
    }

    /**
     * 构造函数
     */
    public PerformanceOptimizer() {
        this.cacheManager = new CacheManager();
        this.memoryOptimizer = new MemoryOptimizer();
        this.databaseOptimizer = new DatabaseOptimizer();
        logger.info("✓ PerformanceOptimizer已初始化");
    }

    /**
     * 获取缓存管理器
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * 获取内存优化器
     */
    public MemoryOptimizer getMemoryOptimizer() {
        return memoryOptimizer;
    }

    /**
     * 获取数据库优化器
     */
    public DatabaseOptimizer getDatabaseOptimizer() {
        return databaseOptimizer;
    }

    /**
     * 执行全面优化
     */
    public void performFullOptimization() {
        // 清理过期缓存
        int cacheRemoved = cacheManager.cleanup();

        // 记录内存使用
        memoryOptimizer.recordMemoryUsage();

        // 条件垃圾回收
        boolean gcExecuted = memoryOptimizer.conditionalGC();

        // 清理过期查询缓存
        int queryCacheRemoved = databaseOptimizer.cleanupQueryCache();

        logger.info("✓ 性能优化完成: 清理缓存" + cacheRemoved + "条, 清理查询缓存" +
                queryCacheRemoved + "条" + (gcExecuted ? ", 执行GC" : ""));
    }

    /**
     * 获取综合统计信息
     */
    public Map<String, Object> getComprehensiveStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("cache_stats", cacheManager.getStatistics());
        stats.put("memory_stats", memoryOptimizer.getStatistics());
        stats.put("database_stats", databaseOptimizer.getStatistics());
        return stats;
    }

    /**
     * 重置系统
     */
    public void reset() {
        cacheManager.clear();
        logger.info("✓ 性能优化系统已重置");
    }
}
