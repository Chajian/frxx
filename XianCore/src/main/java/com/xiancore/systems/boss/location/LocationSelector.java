package com.xiancore.systems.boss.location;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss位置选择器 - 核心引擎
 * 根据多种策略和评分选择最优的Boss生成位置
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class LocationSelector {

    // ==================== 选择策略枚举 ====================

    public enum SelectionStrategy {
        RANDOM("完全随机"),
        BALANCED("平衡分布"),
        CLUSTERED("集群分布"),
        ADAPTIVE("自适应");

        public final String description;

        SelectionStrategy(String description) {
            this.description = description;
        }
    }

    // ==================== 内部状态 ====================

    private final SelectionCriteria criteria;
    private final SafetyAnalyzer safetyAnalyzer;
    private final PlayerDistribution playerDistribution;
    private final Map<String, LocationCache> caches;
    private final Random random;
    private volatile SelectionStrategy currentStrategy;

    // ==================== 统计 ====================

    private volatile long totalSelections;
    private volatile long totalCacheHits;

    // ==================== 构造函数 ====================

    public LocationSelector() {
        this(SelectionCriteria.createBalanced());
    }

    public LocationSelector(SelectionCriteria criteria) {
        this.criteria = criteria != null ? criteria : SelectionCriteria.createBalanced();
        this.safetyAnalyzer = new SafetyAnalyzer();
        this.playerDistribution = new PlayerDistribution();
        this.caches = new ConcurrentHashMap<>();
        this.random = new Random();
        this.currentStrategy = SelectionStrategy.BALANCED;
        this.totalSelections = 0;
        this.totalCacheHits = 0;

        // 初始化缓存
        if (criteria.isCacheEnabled()) {
            caches.put("primary", new LocationCache(criteria.getMaxCacheSize(), criteria.getCacheExpireMillis()));
        }
    }

    // ==================== 核心选择方法 ====================

    /**
     * 从候选位置中选择一个最优位置
     *
     * @param candidates 候选位置列表
     * @param players 在线玩家列表
     * @param strategy 选择策略
     * @return 选中的位置
     */
    public Location selectLocation(List<Location> candidates, List<Player> players, SelectionStrategy strategy) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        totalSelections++;

        // 检查缓存
        LocationCache cache = caches.get("primary");
        if (cache != null && criteria.isCacheEnabled()) {
            String cacheKey = strategy.name() + "_" + candidates.size();
            Location cached = cache.get(cacheKey);
            if (cached != null) {
                totalCacheHits++;
                return cached;
            }
        }

        // 过滤不安全的位置
        List<Location> safeLocations = safetyAnalyzer.filterUnsafeLocations(candidates);
        if (safeLocations.isEmpty()) {
            // 如果没有完全安全的位置，使用原始列表
            safeLocations = candidates;
        }

        // 根据策略选择
        Location selected = null;
        switch (strategy) {
            case RANDOM:
                selected = selectRandom(safeLocations);
                break;
            case BALANCED:
                selected = selectBalanced(safeLocations, players);
                break;
            case CLUSTERED:
                selected = selectClustered(safeLocations, players);
                break;
            case ADAPTIVE:
                selected = selectAdaptive(safeLocations, players);
                break;
            default:
                selected = selectBalanced(safeLocations, players);
        }

        // 缓存结果
        if (selected != null && cache != null && criteria.isCacheEnabled()) {
            String cacheKey = strategy.name() + "_" + candidates.size();
            cache.put(cacheKey, selected);
        }

        return selected;
    }

    /**
     * 简化版本 (使用当前策略和玩家列表)
     */
    public Location selectLocation(List<Location> candidates, List<Player> players) {
        return selectLocation(candidates, players, currentStrategy);
    }

    // ==================== 策略实现 ====================

    /**
     * RANDOM策略: 完全随机选择
     */
    private Location selectRandom(List<Location> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * BALANCED策略: 平衡分布 (安全+分散+随机)
     */
    private Location selectBalanced(List<Location> candidates, List<Player> players) {
        return selectByScore(candidates, players,
            new double[]{0.3, 0.3, 0.2, 0.2});
    }

    /**
     * CLUSTERED策略: 集群分布 (靠近玩家)
     */
    private Location selectClustered(List<Location> candidates, List<Player> players) {
        return selectByScore(candidates, players,
            new double[]{0.3, 0.1, 0.4, 0.2});  // 距离权重高
    }

    /**
     * ADAPTIVE策略: 自适应 (动态调整)
     */
    private Location selectAdaptive(List<Location> candidates, List<Player> players) {
        // 根据在线玩家数动态调整权重
        int playerCount = players != null ? players.size() : 0;
        double distributionWeight = Math.min(0.5, playerCount / 50.0);

        return selectByScore(candidates, players,
            new double[]{0.30, distributionWeight, 0.20, 0.50 - distributionWeight});
    }

    /**
     * 按综合分数选择
     */
    private Location selectByScore(List<Location> candidates, List<Player> players, double[] weights) {
        if (candidates.isEmpty()) {
            return null;
        }

        // 计算每个候选位置的分数
        Map<Location, Double> scores = new HashMap<>();
        double maxScore = 0;

        for (Location candidate : candidates) {
            // 安全性分数
            double safetyScore = safetyAnalyzer.calculateSafetyScore(candidate);

            // 玩家分布分数
            double distributionScore = players != null && !players.isEmpty() ?
                playerDistribution.calculateDistributionScore(candidate, players) : 0.5;

            // 距离分数 (远离其他Boss) - 这里简化处理
            double distanceScore = 0.5;

            // 随机性分数
            double randomScore = random.nextDouble();

            // 综合分数
            double finalScore = weights[0] * safetyScore +
                               weights[1] * distributionScore +
                               weights[2] * distanceScore +
                               weights[3] * randomScore;

            scores.put(candidate, finalScore);
            maxScore = Math.max(maxScore, finalScore);
        }

        // 找到最高分的位置
        for (Map.Entry<Location, Double> entry : scores.entrySet()) {
            if (entry.getValue() == maxScore) {
                return entry.getKey();
            }
        }

        return candidates.get(0);
    }

    // ==================== 查询方法 ====================

    /**
     * 获取当前选择策略
     */
    public SelectionStrategy getCurrentStrategy() {
        return currentStrategy;
    }

    /**
     * 设置选择策略
     */
    public void setCurrentStrategy(SelectionStrategy strategy) {
        this.currentStrategy = strategy != null ? strategy : SelectionStrategy.BALANCED;
    }

    /**
     * 获取选择标准
     */
    public SelectionCriteria getCriteria() {
        return criteria;
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        double hitRate = totalSelections > 0 ? (totalCacheHits * 100.0 / totalSelections) : 0;
        return String.format(
            "位置选择器: 总选择%d次, 缓存命中%d次 (%.1f%%), 策略:%s",
            totalSelections,
            totalCacheHits,
            hitRate,
            currentStrategy.name()
        );
    }

    /**
     * 获取分析报告
     */
    public String getAnalysisReport(Location location, List<Player> players) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 位置选择分析报告 ===\n");
        sb.append(String.format("策略: %s\n", currentStrategy.description));
        sb.append("\n").append(safetyAnalyzer.getSafetyReport(location));
        if (players != null && !players.isEmpty()) {
            sb.append("\n").append(playerDistribution.getAnalysisReport(location, players));
        }
        sb.append("\n").append(getStatistics());
        return sb.toString();
    }
}

/**
 * 简单的位置缓存实现
 */
class LocationCache {
    private final Map<String, CacheEntry> cache;
    private final int maxSize;
    private final long expireMillis;

    LocationCache(int maxSize, long expireMillis) {
        this.cache = new ConcurrentHashMap<>();
        this.maxSize = maxSize;
        this.expireMillis = expireMillis;
    }

    void put(String key, Location value) {
        if (cache.size() >= maxSize) {
            cache.clear();
        }
        cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
    }

    Location get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        // 检查是否过期
        if (System.currentTimeMillis() - entry.time > expireMillis) {
            cache.remove(key);
            return null;
        }

        return entry.location;
    }

    private static class CacheEntry {
        Location location;
        long time;

        CacheEntry(Location location, long time) {
            this.location = location;
            this.time = time;
        }
    }
}
