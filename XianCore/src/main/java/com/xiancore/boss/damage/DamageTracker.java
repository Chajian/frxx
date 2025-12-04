package com.xiancore.boss.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Boss 伤害追踪系统
 * 追踪和管理对Boss的伤害数据
 *
 * @author XianCore
 * @version 1.0
 */
public class DamageTracker {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, BossDamageRecord> damageRecords; // bossUuid -> BossDamageRecord
    private final long RECORD_EXPIRE_TIME = 3600000; // 1小时过期

    /**
     * 伤害记录类
     */
    public static class DamageRecord {
        public UUID playerId;
        public String playerName;
        public double damageDealt;
        public int hitCount;
        public long firstHitTime;
        public long lastHitTime;

        public DamageRecord(UUID playerId, String playerName, double damageDealt) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.damageDealt = damageDealt;
            this.hitCount = 1;
            this.firstHitTime = System.currentTimeMillis();
            this.lastHitTime = System.currentTimeMillis();
        }

        public double getAverageDamagePerHit() {
            return hitCount > 0 ? damageDealt / hitCount : 0;
        }

        public long getDurationMillis() {
            return lastHitTime - firstHitTime;
        }
    }

    /**
     * Boss伤害记录类
     */
    public static class BossDamageRecord {
        public UUID bossId;
        public String bossType;
        public double totalDamage;
        public Map<UUID, DamageRecord> playerDamages; // playerId -> DamageRecord
        public long recordStartTime;
        public long recordEndTime;

        public BossDamageRecord(UUID bossId, String bossType) {
            this.bossId = bossId;
            this.bossType = bossType;
            this.totalDamage = 0;
            this.playerDamages = new ConcurrentHashMap<>();
            this.recordStartTime = System.currentTimeMillis();
            this.recordEndTime = 0;
        }

        public boolean isExpired(long expireMillis) {
            return (System.currentTimeMillis() - recordStartTime) > expireMillis;
        }

        public double getPlayerDamagePercentage(UUID playerId) {
            if (totalDamage <= 0) {
                return 0;
            }
            DamageRecord record = playerDamages.get(playerId);
            return record != null ? (record.damageDealt / totalDamage) * 100 : 0;
        }

        public int getPlayerRank(UUID playerId) {
            List<DamageRecord> sortedByDamage = new ArrayList<>(playerDamages.values());
            sortedByDamage.sort(Comparator.comparingDouble((DamageRecord d) -> d.damageDealt).reversed());

            for (int i = 0; i < sortedByDamage.size(); i++) {
                if (sortedByDamage.get(i).playerId.equals(playerId)) {
                    return i + 1;
                }
            }
            return -1;
        }

        public List<DamageRecord> getTopDamageDealer(int limit) {
            List<DamageRecord> sortedByDamage = new ArrayList<>(playerDamages.values());
            sortedByDamage.sort(Comparator.comparingDouble((DamageRecord d) -> d.damageDealt).reversed());
            return sortedByDamage.size() > limit ? sortedByDamage.subList(0, limit) : sortedByDamage;
        }
    }

    /**
     * 构造函数
     */
    public DamageTracker(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.damageRecords = new ConcurrentHashMap<>();
    }

    /**
     * 创建Boss伤害记录
     */
    public BossDamageRecord createBossRecord(UUID bossId, String bossType) {
        BossDamageRecord record = new BossDamageRecord(bossId, bossType);
        damageRecords.put(bossId, record);
        logger.info("✓ 创建Boss伤害记录: " + bossType + " (UUID: " + bossId + ")");
        return record;
    }

    /**
     * 获取Boss伤害记录
     */
    public BossDamageRecord getBossRecord(UUID bossId) {
        return damageRecords.get(bossId);
    }

    /**
     * 记录伤害
     */
    public void recordDamage(UUID bossId, Player player, double damageDealt) {
        if (damageDealt <= 0) {
            return;
        }

        try {
            BossDamageRecord bossRecord = damageRecords.get(bossId);
            if (bossRecord == null) {
                logger.warning("✗ Boss记录不存在: " + bossId);
                return;
            }

            UUID playerId = player.getUniqueId();
            String playerName = player.getName();

            DamageRecord damageRecord = bossRecord.playerDamages.get(playerId);
            if (damageRecord == null) {
                // 首次伤害
                damageRecord = new DamageRecord(playerId, playerName, damageDealt);
                bossRecord.playerDamages.put(playerId, damageRecord);
                logger.info("✓ 玩家首次伤害记录: " + playerName + " -> " + damageDealt);
            } else {
                // 累计伤害
                damageRecord.damageDealt += damageDealt;
                damageRecord.hitCount++;
                damageRecord.lastHitTime = System.currentTimeMillis();
            }

            bossRecord.totalDamage += damageDealt;

        } catch (Exception e) {
            logger.severe("✗ 记录伤害异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取玩家对Boss的伤害
     */
    public DamageRecord getPlayerDamage(UUID bossId, UUID playerId) {
        BossDamageRecord bossRecord = damageRecords.get(bossId);
        if (bossRecord == null) {
            return null;
        }
        return bossRecord.playerDamages.get(playerId);
    }

    /**
     * 获取Boss的伤害排名（前N名）
     */
    public List<DamageRecord> getTopDamageDealer(UUID bossId, int limit) {
        BossDamageRecord bossRecord = damageRecords.get(bossId);
        if (bossRecord == null) {
            return new ArrayList<>();
        }
        return bossRecord.getTopDamageDealer(limit);
    }

    /**
     * 获取玩家对Boss的伤害百分比
     */
    public double getPlayerDamagePercentage(UUID bossId, UUID playerId) {
        BossDamageRecord bossRecord = damageRecords.get(bossId);
        if (bossRecord == null) {
            return 0;
        }
        return bossRecord.getPlayerDamagePercentage(playerId);
    }

    /**
     * 获取玩家的伤害排名
     */
    public int getPlayerRank(UUID bossId, UUID playerId) {
        BossDamageRecord bossRecord = damageRecords.get(bossId);
        if (bossRecord == null) {
            return -1;
        }
        return bossRecord.getPlayerRank(playerId);
    }

    /**
     * 结束Boss伤害追踪
     */
    public BossDamageRecord finishTracking(UUID bossId) {
        BossDamageRecord record = damageRecords.get(bossId);
        if (record != null) {
            record.recordEndTime = System.currentTimeMillis();
            logger.info("✓ 已结束Boss伤害追踪: " + record.bossType);
        }
        return record;
    }

    /**
     * 删除Boss伤害记录
     */
    public boolean deleteBossRecord(UUID bossId) {
        BossDamageRecord removed = damageRecords.remove(bossId);
        if (removed != null) {
            logger.info("✓ 已删除Boss伤害记录: " + removed.bossType);
            return true;
        }
        return false;
    }

    /**
     * 清除过期的伤害记录
     */
    public int clearExpiredRecords() {
        int removed = 0;
        Iterator<Map.Entry<UUID, BossDamageRecord>> iterator = damageRecords.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BossDamageRecord> entry = iterator.next();
            if (entry.getValue().isExpired(RECORD_EXPIRE_TIME)) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            logger.info("✓ 已清除 " + removed + " 个过期的伤害记录");
        }
        return removed;
    }

    /**
     * 获取所有活跃的Boss记录
     */
    public Collection<BossDamageRecord> getAllActiveRecords() {
        return damageRecords.values();
    }

    /**
     * 获取活跃Boss的数量
     */
    public int getActiveRecordCount() {
        return damageRecords.size();
    }

    /**
     * 计算对Boss的平均伤害
     */
    public double calculateAverageDamage(UUID bossId) {
        BossDamageRecord record = damageRecords.get(bossId);
        if (record == null || record.playerDamages.isEmpty()) {
            return 0;
        }

        double totalPlayers = record.playerDamages.size();
        return record.totalDamage / totalPlayers;
    }

    /**
     * 获取伤害统计摘要
     */
    public Map<String, Object> getDamageStatisticsSummary(UUID bossId) {
        Map<String, Object> summary = new HashMap<>();

        BossDamageRecord record = damageRecords.get(bossId);
        if (record == null) {
            return summary;
        }

        summary.put("boss_id", record.bossId);
        summary.put("boss_type", record.bossType);
        summary.put("total_damage", record.totalDamage);
        summary.put("player_count", record.playerDamages.size());
        summary.put("average_damage", calculateAverageDamage(bossId));
        summary.put("top_damagers", record.getTopDamageDealer(5));
        summary.put("record_duration", record.recordEndTime > 0 ? (record.recordEndTime - record.recordStartTime) : (System.currentTimeMillis() - record.recordStartTime));

        return summary;
    }

    /**
     * 打印伤害统计
     */
    public void printDamageStatistics(UUID bossId) {
        BossDamageRecord record = damageRecords.get(bossId);
        if (record == null) {
            logger.info("✗ Boss记录不存在: " + bossId);
            return;
        }

        logger.info("=== " + record.bossType + " 伤害统计 ===");
        logger.info("总伤害: " + String.format("%.1f", record.totalDamage));
        logger.info("参与玩家数: " + record.playerDamages.size());
        logger.info("平均伤害: " + String.format("%.1f", calculateAverageDamage(bossId)));

        List<DamageRecord> topDamagers = record.getTopDamageDealer(5);
        logger.info("--- 伤害排名 ---");
        for (int i = 0; i < topDamagers.size(); i++) {
            DamageRecord dmg = topDamagers.get(i);
            double percentage = (dmg.damageDealt / record.totalDamage) * 100;
            logger.info((i + 1) + ". " + dmg.playerName + ": " + String.format("%.1f", dmg.damageDealt) +
                       " (" + String.format("%.1f%%", percentage) + ") [" + dmg.hitCount + " 次]");
        }
    }

    /**
     * 清除所有记录
     */
    public void clear() {
        damageRecords.clear();
        logger.info("✓ 已清除所有伤害追踪数据");
    }
}
