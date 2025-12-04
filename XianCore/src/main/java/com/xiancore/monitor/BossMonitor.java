package com.xiancore.monitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Boss监控系统 - 实时监控Boss状态和事件
 * Boss Monitor - Monitor Boss status and events in real-time
 *
 * @author XianCore
 * @version 1.0
 */
public class BossMonitor {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, BossRecord> bossRecords = new ConcurrentHashMap<>();
    private final List<BossEvent> eventHistory = Collections.synchronizedList(new ArrayList<>());
    private volatile int maxHistorySize = 1000;

    /**
     * Boss监控记录
     */
    public static class BossRecord {
        public String bossId;
        public String bossName;
        public String bossType;
        public String world;
        public double x, y, z;
        public int tier;
        public double maxHealth;
        public double currentHealth;
        public String status;              // SPAWNED, ACTIVE, DEAD, DESPAWNED
        public LocalDateTime spawnTime;
        public LocalDateTime lastDamageTime;
        public int damageCount;            // 受伤次数
        public Map<String, Double> damageContributors; // 伤害贡献者
        public long totalDamageReceived;    // 总伤害

        public BossRecord(String bossId, String bossName, String bossType, String world,
                         double x, double y, double z, int tier, double health) {
            this.bossId = bossId;
            this.bossName = bossName;
            this.bossType = bossType;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.tier = tier;
            this.maxHealth = health;
            this.currentHealth = health;
            this.status = "SPAWNED";
            this.spawnTime = LocalDateTime.now();
            this.lastDamageTime = null;
            this.damageCount = 0;
            this.damageContributors = new ConcurrentHashMap<>();
            this.totalDamageReceived = 0;
        }

        public double getHealthPercent() {
            return (currentHealth / maxHealth) * 100.0;
        }

        public long getAliveTime() {
            return java.time.temporal.ChronoUnit.SECONDS.between(spawnTime, LocalDateTime.now());
        }

        public Map<String, Double> getTopContributors(int limit) {
            return damageContributors.entrySet().stream()
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(limit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
        }
    }

    /**
     * Boss事件记录
     */
    public static class BossEvent {
        public String eventType;            // SPAWNED, DAMAGE, HEALED, DEAD, DESPAWNED
        public String bossId;
        public String bossName;
        public String details;
        public LocalDateTime timestamp;
        public String sourcePlayer;         // 事件来源玩家

        public BossEvent(String eventType, String bossId, String bossName, String details) {
            this.eventType = eventType;
            this.bossId = bossId;
            this.bossName = bossName;
            this.details = details;
            this.timestamp = LocalDateTime.now();
        }
    }

    /**
     * Boss统计信息
     */
    public static class BossStatistics {
        public int totalBossesSpawned;      // 总刷新数
        public int activeBossCount;         // 活跃Boss数
        public int deadBossCount;           // 已死亡Boss数
        public int despawnedBossCount;      // 已消失Boss数
        public double averageAliveTime;     // 平均存活时间 (秒)
        public long totalDamageDealt;       // 总伤害
        public int totalDamageEvents;       // 伤害事件数
        public List<String> activeBosses;   // 活跃Boss列表
        public long timestamp;

        public BossStatistics() {
            this.activeBosses = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 构造函数
     */
    public BossMonitor() {
        logger.info("✓ BossMonitor已初始化");
    }

    /**
     * 记录新的Boss生成
     */
    public void recordBossSpawn(String bossId, String bossName, String bossType, String world,
                               double x, double y, double z, int tier, double health) {
        BossRecord record = new BossRecord(bossId, bossName, bossType, world, x, y, z, tier, health);
        bossRecords.put(bossId, record);

        BossEvent event = new BossEvent("SPAWNED", bossId, bossName,
                String.format("在 %s (%d, %d, %d) 刷新 [Tier %d]", world, (int)x, (int)y, (int)z, tier));
        addEvent(event);

        logger.info("✓ Boss已记录: " + bossName + " (ID: " + bossId + ")");
    }

    /**
     * 记录Boss受伤
     */
    public void recordBossDamage(String bossId, String playerName, double damage) {
        BossRecord record = bossRecords.get(bossId);
        if (record == null) {
            logger.warning("✗ 未找到Boss: " + bossId);
            return;
        }

        record.currentHealth = Math.max(0, record.currentHealth - damage);
        record.lastDamageTime = LocalDateTime.now();
        record.damageCount++;
        record.totalDamageReceived += (long) damage;

        // 记录伤害贡献者
        record.damageContributors.put(playerName,
                record.damageContributors.getOrDefault(playerName, 0.0) + damage);

        // 更新Boss状态
        if (record.currentHealth == 0) {
            record.status = "DEAD";
        } else if (record.status.equals("SPAWNED")) {
            record.status = "ACTIVE";
        }

        BossEvent event = new BossEvent("DAMAGE", bossId, record.bossName,
                playerName + " 造成了 " + String.format("%.1f", damage) + " 伤害 (当前血量: " +
                String.format("%.1f%%", record.getHealthPercent()) + ")");
        event.sourcePlayer = playerName;
        addEvent(event);
    }

    /**
     * 记录Boss死亡
     */
    public void recordBossDeath(String bossId, String killerName) {
        BossRecord record = bossRecords.get(bossId);
        if (record == null) {
            logger.warning("✗ 未找到Boss: " + bossId);
            return;
        }

        record.status = "DEAD";
        record.currentHealth = 0;

        BossEvent event = new BossEvent("DEAD", bossId, record.bossName,
                killerName + " 击杀了 " + record.bossName + " (存活时间: " +
                PerformanceMonitor.formatUptime(record.getAliveTime() * 1000) + ")");
        event.sourcePlayer = killerName;
        addEvent(event);

        logger.info("✓ Boss已死亡: " + record.bossName + " (击杀者: " + killerName + ")");
    }

    /**
     * 记录Boss消失
     */
    public void recordBossDespawn(String bossId) {
        BossRecord record = bossRecords.get(bossId);
        if (record == null) {
            return;
        }

        record.status = "DESPAWNED";

        BossEvent event = new BossEvent("DESPAWNED", bossId, record.bossName,
                record.bossName + " 已消失 (存活时间: " +
                PerformanceMonitor.formatUptime(record.getAliveTime() * 1000) + ")");
        addEvent(event);

        logger.info("✓ Boss已消失: " + record.bossName);
    }

    /**
     * 添加事件到历史记录
     */
    private void addEvent(BossEvent event) {
        eventHistory.add(event);

        // 保持历史大小不超过限制
        if (eventHistory.size() > maxHistorySize) {
            eventHistory.remove(0);
        }
    }

    /**
     * 获取Boss记录
     */
    public BossRecord getBossRecord(String bossId) {
        return bossRecords.get(bossId);
    }

    /**
     * 获取所有活跃的Boss
     */
    public List<BossRecord> getActiveBosses() {
        return bossRecords.values().stream()
                .filter(b -> b.status.equals("ACTIVE") || b.status.equals("SPAWNED"))
                .collect(Collectors.toList());
    }

    /**
     * 获取特定世界的Boss
     */
    public List<BossRecord> getBossesByWorld(String world) {
        return bossRecords.values().stream()
                .filter(b -> b.world.equals(world))
                .collect(Collectors.toList());
    }

    /**
     * 获取特定Tier的Boss
     */
    public List<BossRecord> getBossesByTier(int tier) {
        return bossRecords.values().stream()
                .filter(b -> b.tier == tier)
                .collect(Collectors.toList());
    }

    /**
     * 获取已死亡的Boss
     */
    public List<BossRecord> getDeadBosses(int limit) {
        return bossRecords.values().stream()
                .filter(b -> b.status.equals("DEAD"))
                .sorted((a, b) -> b.lastDamageTime.compareTo(a.lastDamageTime))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取事件历史
     */
    public List<BossEvent> getEventHistory(int limit) {
        int startIndex = Math.max(0, eventHistory.size() - limit);
        return new ArrayList<>(eventHistory.subList(startIndex, eventHistory.size()));
    }

    /**
     * 获取特定Boss的事件
     */
    public List<BossEvent> getBossEvents(String bossId, int limit) {
        return eventHistory.stream()
                .filter(e -> e.bossId.equals(bossId))
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取Boss统计信息
     */
    public BossStatistics getBossStatistics() {
        BossStatistics stats = new BossStatistics();

        stats.totalBossesSpawned = bossRecords.size();
        stats.activeBossCount = (int) bossRecords.values().stream()
                .filter(b -> b.status.equals("ACTIVE") || b.status.equals("SPAWNED"))
                .count();
        stats.deadBossCount = (int) bossRecords.values().stream()
                .filter(b -> b.status.equals("DEAD"))
                .count();
        stats.despawnedBossCount = (int) bossRecords.values().stream()
                .filter(b -> b.status.equals("DESPAWNED"))
                .count();

        // 计算平均存活时间
        if (stats.totalBossesSpawned > 0) {
            long totalAliveTime = bossRecords.values().stream()
                    .mapToLong(BossRecord::getAliveTime)
                    .sum();
            stats.averageAliveTime = totalAliveTime / (double) stats.totalBossesSpawned;
        }

        // 计算总伤害
        stats.totalDamageDealt = bossRecords.values().stream()
                .mapToLong(b -> b.totalDamageReceived)
                .sum();

        stats.totalDamageEvents = (int) eventHistory.stream()
                .filter(e -> e.eventType.equals("DAMAGE"))
                .count();

        // 活跃Boss列表
        stats.activeBosses = getActiveBosses().stream()
                .map(b -> b.bossName + " (" + String.format("%.1f%%", b.getHealthPercent()) + ")")
                .collect(Collectors.toList());

        return stats;
    }

    /**
     * 获取伤害排行
     */
    public List<Map.Entry<String, Double>> getDamageRanking(String bossId, int limit) {
        BossRecord record = bossRecords.get(bossId);
        if (record == null) {
            return new ArrayList<>();
        }

        return record.damageContributors.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否有Boss濒临死亡
     */
    public List<BossRecord> getLowHealthBosses(double healthThreshold) {
        return bossRecords.values().stream()
                .filter(b -> (b.status.equals("ACTIVE") || b.status.equals("SPAWNED"))
                        && b.getHealthPercent() < healthThreshold)
                .sorted((a, b) -> Double.compare(a.getHealthPercent(), b.getHealthPercent()))
                .collect(Collectors.toList());
    }

    /**
     * 获取最近的事件
     */
    public List<BossEvent> getRecentEvents(int limit) {
        return getEventHistory(limit);
    }

    /**
     * 清除旧数据
     */
    public void cleanupOldData(long ageMillis) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(ageMillis / 1000);

        // 移除已过期的Boss记录
        int removedCount = 0;
        for (String bossId : new HashSet<>(bossRecords.keySet())) {
            BossRecord record = bossRecords.get(bossId);
            if (record.spawnTime.isBefore(cutoffTime) &&
                    (record.status.equals("DEAD") || record.status.equals("DESPAWNED"))) {
                bossRecords.remove(bossId);
                removedCount++;
            }
        }

        // 移除过期的事件记录
        int removedEvents = 0;
        for (BossEvent event : new ArrayList<>(eventHistory)) {
            if (event.timestamp.isBefore(cutoffTime)) {
                eventHistory.remove(event);
                removedEvents++;
            }
        }

        if (removedCount > 0 || removedEvents > 0) {
            logger.info("✓ 清理过期数据: " + removedCount + " Boss记录, " + removedEvents + " 事件");
        }
    }

    /**
     * 重置监控数据
     */
    public void reset() {
        bossRecords.clear();
        eventHistory.clear();
        logger.info("✓ Boss监控数据已重置");
    }

    /**
     * 获取监控概览
     */
    public Map<String, Object> getMonitorOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        BossStatistics stats = getBossStatistics();

        overview.put("totalBosses", stats.totalBossesSpawned);
        overview.put("activeBosses", stats.activeBossCount);
        overview.put("deadBosses", stats.deadBossCount);
        overview.put("despawnedBosses", stats.despawnedBossCount);
        overview.put("averageAliveTime", String.format("%.1f秒", stats.averageAliveTime));
        overview.put("totalDamage", stats.totalDamageDealt);
        overview.put("damageEvents", stats.totalDamageEvents);
        overview.put("activeBossList", stats.activeBosses);

        return overview;
    }
}
