package com.xiancore.spawn;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 概率生成器 - 基于概率的Boss生成系统
 * Probabilistic Spawner - Probability-based Boss Spawning System
 *
 * @author XianCore
 * @version 1.0
 */
public class ProbabilisticSpawner {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final RandomBossGenerator bossGenerator;
    private final LocationGenerator locationGenerator;
    private final Map<String, SpawnZone> spawnZones = new ConcurrentHashMap<>();
    private final Map<String, SpawnEvent> spawnHistory = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * 生成区域
     */
    public static class SpawnZone {
        public String zoneId;
        public String world;
        public double centerX, centerY, centerZ;
        public double radius;
        public int minPlayers;          // 最少玩家数
        public int maxPlayers;          // 最多玩家数
        public int minLevel;            // 最小等级
        public int maxLevel;            // 最大等级
        public double spawnRate;        // 每分钟生成概率
        public int maxConcurrentBosses; // 最多并发Boss数
        public int currentBossCount;
        public long lastSpawnTime;
        public Map<String, Object> properties;

        public SpawnZone(String zoneId, String world, double x, double y, double z, double radius) {
            this.zoneId = zoneId;
            this.world = world;
            this.centerX = x;
            this.centerY = y;
            this.centerZ = z;
            this.radius = radius;
            this.minPlayers = 1;
            this.maxPlayers = 10;
            this.minLevel = 1;
            this.maxLevel = 5;
            this.spawnRate = 0.5;       // 每分钟50%概率
            this.maxConcurrentBosses = 3;
            this.currentBossCount = 0;
            this.lastSpawnTime = System.currentTimeMillis();
            this.properties = new ConcurrentHashMap<>();
        }

        public boolean canSpawn() {
            return currentBossCount < maxConcurrentBosses;
        }

        public void recordSpawn() {
            currentBossCount++;
            lastSpawnTime = System.currentTimeMillis();
        }

        public void recordDeath() {
            currentBossCount = Math.max(0, currentBossCount - 1);
        }
    }

    /**
     * 生成条件
     */
    public static class SpawnCondition {
        public int playerCount;
        public int averagePlayerLevel;
        public double timeSinceLastSpawn;  // 秒
        public boolean isNight;
        public boolean hasSpecialEvent;
        public String world;
        public Map<String, Object> customConditions;

        public SpawnCondition(String world) {
            this.world = world;
            this.playerCount = 1;
            this.averagePlayerLevel = 1;
            this.timeSinceLastSpawn = 0;
            this.isNight = false;
            this.hasSpecialEvent = false;
            this.customConditions = new HashMap<>();
        }
    }

    /**
     * 生成事件
     */
    public static class SpawnEvent {
        public String eventId;
        public String zoneId;
        public String bossId;
        public RandomBossGenerator.GeneratedBoss boss;
        public LocationGenerator.Location location;
        public long spawnTime;
        public long deathTime;          // 死亡时间（如果已死）
        public boolean isActive;
        public Map<String, Object> metadata;

        public SpawnEvent(String eventId, String zoneId, String bossId,
                         RandomBossGenerator.GeneratedBoss boss,
                         LocationGenerator.Location location) {
            this.eventId = eventId;
            this.zoneId = zoneId;
            this.bossId = bossId;
            this.boss = boss;
            this.location = location;
            this.spawnTime = System.currentTimeMillis();
            this.isActive = true;
            this.metadata = new HashMap<>();
        }

        public double getAliveTime() {
            if (!isActive) {
                return (deathTime - spawnTime) / 1000.0;  // 秒
            }
            return (System.currentTimeMillis() - spawnTime) / 1000.0;
        }

        @Override
        public String toString() {
            return String.format("SpawnEvent[%s] %s at %s (alive: %.1fs)",
                    eventId, boss.template.bossName, location, getAliveTime());
        }
    }

    /**
     * 生成概率计算器
     */
    public static class SpawnProbabilityCalculator {
        public double baseRate;         // 基础概率

        public SpawnProbabilityCalculator(double baseRate) {
            this.baseRate = baseRate;
        }

        public double calculateProbability(SpawnZone zone, SpawnCondition condition) {
            double probability = baseRate * zone.spawnRate;

            // 根据玩家数量调整
            if (condition.playerCount > zone.maxPlayers) {
                probability *= 1.2;  // 玩家过多，提高生成概率
            } else if (condition.playerCount < zone.minPlayers) {
                probability = 0;     // 玩家过少，不生成
            }

            // 根据玩家等级调整
            double levelDiff = condition.averagePlayerLevel - (zone.minLevel + zone.maxLevel) / 2.0;
            probability *= Math.exp(-Math.abs(levelDiff) / 5.0);  // 高斯分布

            // 时间衰减 (越长不生成，概率越高)
            long timeSinceLastSpawn = System.currentTimeMillis() - zone.lastSpawnTime;
            probability *= Math.min(1.0, timeSinceLastSpawn / 60000.0);  // 最长1分钟

            // 夜间加成
            if (condition.isNight) {
                probability *= 1.5;
            }

            // 特殊事件加成
            if (condition.hasSpecialEvent) {
                probability *= 2.0;
            }

            // 并发Boss限制
            if (!zone.canSpawn()) {
                probability = 0;
            }

            return Math.min(1.0, probability);
        }
    }

    /**
     * 构造函数
     */
    public ProbabilisticSpawner(int worldWidth, int worldHeight) {
        this.bossGenerator = new RandomBossGenerator();
        this.locationGenerator = new LocationGenerator(worldWidth, worldHeight);
        logger.info("✓ ProbabilisticSpawner已初始化");
    }

    /**
     * 创建生成区域
     */
    public void createSpawnZone(String zoneId, String world, double x, double y, double z, double radius) {
        SpawnZone zone = new SpawnZone(zoneId, world, x, y, z, radius);
        spawnZones.put(zoneId, zone);
        logger.info("✓ 生成区域已创建: " + zoneId);
    }

    /**
     * 尝试在区域生成Boss
     */
    public SpawnEvent trySpawnBoss(String zoneId, SpawnCondition condition) {
        SpawnZone zone = spawnZones.get(zoneId);
        if (zone == null) return null;

        SpawnProbabilityCalculator calculator = new SpawnProbabilityCalculator(1.0);
        double probability = calculator.calculateProbability(zone, condition);

        if (random.nextDouble() > probability) {
            return null;  // 没有生成
        }

        // 生成Boss
        int tier = calculateTier(zone, condition);
        RandomBossGenerator.GeneratedBoss boss = bossGenerator.generateRandomBoss(tier);

        // 生成位置
        LocationGenerator.Location location = locationGenerator.generateSafeLocation(
                zone.world, 5
        );

        // 创建生成事件
        String eventId = "event-" + System.currentTimeMillis() + "-" + random.nextInt(10000);
        SpawnEvent event = new SpawnEvent(eventId, zoneId, boss.bossId, boss, location);

        spawnHistory.put(eventId, event);
        zone.recordSpawn();

        logger.info("✓ Boss已生成: " + event.toString());

        return event;
    }

    /**
     * 计算Boss等级
     */
    private int calculateTier(SpawnZone zone, SpawnCondition condition) {
        // 基础等级为玩家平均等级
        int tier = condition.averagePlayerLevel;

        // 根据玩家数量调整
        if (condition.playerCount >= 5) {
            tier += 1;
        } else if (condition.playerCount == 1) {
            tier = Math.max(1, tier - 1);
        }

        // 夹在区域限制内
        return Math.max(zone.minLevel, Math.min(zone.maxLevel, tier));
    }

    /**
     * 记录Boss死亡
     */
    public void recordBossDeath(String bossId) {
        // 查找对应的生成事件
        for (SpawnEvent event : spawnHistory.values()) {
            if (event.bossId.equals(bossId)) {
                event.isActive = false;
                event.deathTime = System.currentTimeMillis();

                // 更新区域Boss计数
                SpawnZone zone = spawnZones.get(event.zoneId);
                if (zone != null) {
                    zone.recordDeath();
                }

                logger.info("↻ Boss已记录为死亡: " + bossId + " (存活时间: " +
                        String.format("%.1f", event.getAliveTime()) + "s)");
                break;
            }
        }
    }

    /**
     * 获取区域活跃Boss
     */
    public List<SpawnEvent> getActiveBoSSesInZone(String zoneId) {
        return spawnHistory.values().stream()
                .filter(e -> e.zoneId.equals(zoneId) && e.isActive)
                .toList();
    }

    /**
     * 获取所有活跃Boss
     */
    public List<SpawnEvent> getAllActiveBosses() {
        return spawnHistory.values().stream()
                .filter(e -> e.isActive)
                .toList();
    }

    /**
     * 获取生成区域
     */
    public SpawnZone getSpawnZone(String zoneId) {
        return spawnZones.get(zoneId);
    }

    /**
     * 获取所有生成区域
     */
    public Collection<SpawnZone> getAllSpawnZones() {
        return spawnZones.values();
    }

    /**
     * 获取生成事件
     */
    public SpawnEvent getSpawnEvent(String eventId) {
        return spawnHistory.get(eventId);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_zones", spawnZones.size());
        stats.put("active_bosses", getAllActiveBosses().size());
        stats.put("total_spawn_events", spawnHistory.size());

        // 按区域统计
        Map<String, Integer> zoneStats = new HashMap<>();
        for (SpawnZone zone : spawnZones.values()) {
            zoneStats.put(zone.zoneId, zone.currentBossCount);
        }
        stats.put("bosses_per_zone", zoneStats);

        // 平均生存时间
        double avgAliveTime = spawnHistory.values().stream()
                .filter(e -> !e.isActive)
                .mapToDouble(SpawnEvent::getAliveTime)
                .average()
                .orElse(0);
        stats.put("avg_boss_lifetime", String.format("%.1f秒", avgAliveTime));

        return stats;
    }

    /**
     * 清空历史
     */
    public void clearHistory() {
        spawnHistory.clear();
        logger.info("✓ 生成历史已清空");
    }

    /**
     * 重置系统
     */
    public void reset() {
        spawnZones.values().forEach(z -> z.currentBossCount = 0);
        spawnHistory.clear();
        locationGenerator.reset();
        logger.info("✓ 概率生成器已重置");
    }
}
