package com.xiancore.web.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * 处理所有缓存操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 缓存键定义 ====================
    private static final String CACHE_PREFIX = "xiancore:";
    private static final String BOSS_PREFIX = CACHE_PREFIX + "boss:";
    private static final String PLAYER_PREFIX = CACHE_PREFIX + "player:";
    private static final String RANKING_PREFIX = CACHE_PREFIX + "ranking:";
    private static final String STATS_PREFIX = CACHE_PREFIX + "stats:";

    // ==================== TTL定义（秒） ====================
    private static final long BOSS_CACHE_TTL = 5 * 60; // 5分钟
    private static final long PLAYER_CACHE_TTL = 10 * 60; // 10分钟
    private static final long RANKING_CACHE_TTL = 30 * 60; // 30分钟
    private static final long STATS_CACHE_TTL = 10 * 60; // 10分钟

    // ==================== Boss缓存 ====================

    /**
     * 获取Boss缓存
     */
    public Object getBossCache(String bossId) {
        String key = BOSS_PREFIX + bossId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置Boss缓存
     */
    public void setBossCache(String bossId, Object boss) {
        String key = BOSS_PREFIX + bossId;
        redisTemplate.opsForValue().set(key, boss, BOSS_CACHE_TTL, TimeUnit.SECONDS);
        log.debug("Cached boss: {}", bossId);
    }

    /**
     * 删除Boss缓存
     */
    public void evictBossCache(String bossId) {
        String key = BOSS_PREFIX + bossId;
        redisTemplate.delete(key);
        log.debug("Evicted boss cache: {}", bossId);
    }

    /**
     * 删除所有Boss缓存
     */
    public void evictAllBossCache() {
        redisTemplate.delete(redisTemplate.keys(BOSS_PREFIX + "*"));
        log.debug("Evicted all boss cache");
    }

    // ==================== 玩家统计缓存 ====================

    /**
     * 获取玩家统计缓存
     */
    public Object getPlayerStatsCache(String playerId) {
        String key = PLAYER_PREFIX + playerId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置玩家统计缓存
     */
    public void setPlayerStatsCache(String playerId, Object stats) {
        String key = PLAYER_PREFIX + playerId;
        redisTemplate.opsForValue().set(key, stats, PLAYER_CACHE_TTL, TimeUnit.SECONDS);
        log.debug("Cached player stats: {}", playerId);
    }

    /**
     * 删除玩家统计缓存
     */
    public void evictPlayerStatsCache(String playerId) {
        String key = PLAYER_PREFIX + playerId;
        redisTemplate.delete(key);
        log.debug("Evicted player stats cache: {}", playerId);
    }

    /**
     * 删除所有玩家统计缓存
     */
    public void evictAllPlayerStatsCache() {
        redisTemplate.delete(redisTemplate.keys(PLAYER_PREFIX + "*"));
        log.debug("Evicted all player stats cache");
    }

    // ==================== 排行榜缓存 ====================

    /**
     * 获取排行榜缓存
     */
    public Object getRankingCache(String rankingType) {
        String key = RANKING_PREFIX + rankingType;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置排行榜缓存
     */
    public void setRankingCache(String rankingType, Object ranking) {
        String key = RANKING_PREFIX + rankingType;
        redisTemplate.opsForValue().set(key, ranking, RANKING_CACHE_TTL, TimeUnit.SECONDS);
        log.debug("Cached ranking: {}", rankingType);
    }

    /**
     * 删除排行榜缓存
     */
    public void evictRankingCache(String rankingType) {
        String key = RANKING_PREFIX + rankingType;
        redisTemplate.delete(key);
        log.debug("Evicted ranking cache: {}", rankingType);
    }

    /**
     * 删除所有排行榜缓存
     */
    public void evictAllRankingCache() {
        redisTemplate.delete(redisTemplate.keys(RANKING_PREFIX + "*"));
        log.debug("Evicted all ranking cache");
    }

    // ==================== 统计缓存 ====================

    /**
     * 获取统计缓存
     */
    public Object getStatsCache(String statsKey) {
        String key = STATS_PREFIX + statsKey;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 设置统计缓存
     */
    public void setStatsCache(String statsKey, Object stats) {
        String key = STATS_PREFIX + statsKey;
        redisTemplate.opsForValue().set(key, stats, STATS_CACHE_TTL, TimeUnit.SECONDS);
        log.debug("Cached stats: {}", statsKey);
    }

    /**
     * 删除统计缓存
     */
    public void evictStatsCache(String statsKey) {
        String key = STATS_PREFIX + statsKey;
        redisTemplate.delete(key);
        log.debug("Evicted stats cache: {}", statsKey);
    }

    /**
     * 删除所有统计缓存
     */
    public void evictAllStatsCache() {
        redisTemplate.delete(redisTemplate.keys(STATS_PREFIX + "*"));
        log.debug("Evicted all stats cache");
    }

    // ==================== 通用缓存操作 ====================

    /**
     * 清空所有缓存
     */
    public void clearAllCache() {
        evictAllBossCache();
        evictAllPlayerStatsCache();
        evictAllRankingCache();
        evictAllStatsCache();
        log.info("Cleared all cache");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getCacheStatistics() {
        long bossCount = countKeysByPattern(BOSS_PREFIX + "*");
        long playerCount = countKeysByPattern(PLAYER_PREFIX + "*");
        long rankingCount = countKeysByPattern(RANKING_PREFIX + "*");
        long statsCount = countKeysByPattern(STATS_PREFIX + "*");

        return CacheStatistics.builder()
                .bossCacheCount(bossCount)
                .playerStatsCacheCount(playerCount)
                .rankingCacheCount(rankingCount)
                .statsCacheCount(statsCount)
                .totalCacheCount(bossCount + playerCount + rankingCount + statsCount)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 统计匹配模式的键数量
     */
    private long countKeysByPattern(String pattern) {
        long count = 0;
        var keys = redisTemplate.keys(pattern);
        if (keys != null) {
            count = keys.size();
        }
        return count;
    }

    /**
     * 缓存统计信息DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    public static class CacheStatistics {
        private long bossCacheCount;
        private long playerStatsCacheCount;
        private long rankingCacheCount;
        private long statsCacheCount;
        private long totalCacheCount;
        private long timestamp;
    }
}
