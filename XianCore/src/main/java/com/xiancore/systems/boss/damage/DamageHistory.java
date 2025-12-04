package com.xiancore.systems.boss.damage;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 伤害历史记录
 * 存储已完成Boss的完整伤害统计历史
 *
 * 职责:
 * - 记录Boss的完整生命周期伤害数据
 * - 维护历史统计信息
 * - 支持历史查询和分析
 * - 用于数据持久化
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
@Setter
public class DamageHistory {

    // ==================== Boss信息 ====================
    /** Boss UUID */
    private UUID bossUUID;

    /** Boss类型 */
    private String bossType = "Unknown";

    /** Boss等级 */
    private int bossTier = 1;

    // ==================== 时间信息 ====================
    /** 伤害开始时间 (毫秒时间戳) */
    private long startTime;

    /** 伤害结束时间 (毫秒时间戳) */
    private long endTime;

    /** 持续时间 (秒) */
    private long durationSeconds;

    // ==================== 伤害统计 ====================
    /** 伤害统计信息 */
    private DamageStatistics statistics;

    /** 伤害排行信息 */
    private DamageRanking ranking;

    /** 完整伤害记录 */
    private DamageRecord damageRecord;

    // ==================== 元数据 ====================
    /** 是否已归档 */
    private boolean archived = false;

    /** 创建时间 */
    private long createdAt = System.currentTimeMillis();

    /** 元数据 (可扩展) */
    private Map<String, Object> metadata = new HashMap<>();

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public DamageHistory() {
    }

    /**
     * 从DamageRecord创建历史记录
     * TODO: 需要重构以适配新的DamageRecord API
     *
     * @param record 伤害记录
     */
    /* 暂时禁用，等待API重构
    public DamageHistory(DamageRecord record) {
        if (record == null) {
            return;
        }

        this.bossUUID = record.getBossUUID();
        this.bossType = record.getBossType();
        this.bossTier = record.getBossTier();

        this.startTime = record.getRecordTime();
        this.endTime = System.currentTimeMillis();
        this.durationSeconds = (this.endTime - this.startTime) / 1000;

        // 计算伤害统计
        this.statistics = new DamageStatistics(record);
        this.statistics.setDurationSeconds(this.durationSeconds);

        // 计算伤害排行
        this.ranking = new DamageRanking(bossUUID);
        this.ranking.updateRanking(record);

        // 保存完整记录
        this.damageRecord = record;
    }
    */

    /**
     * 从DamageRecord和时间范围创建
     * TODO: 需要重构以适配新的DamageRecord API
     *
     * @param record 伤害记录
     * @param endTime 结束时间
     */
    /* 暂时禁用，等待API重构
    public DamageHistory(DamageRecord record, long endTime) {
        this(record);
        this.endTime = endTime;
        this.durationSeconds = (endTime - this.startTime) / 1000;
        if (this.statistics != null) {
            this.statistics.setDurationSeconds(this.durationSeconds);
        }
    }
    */

    // ==================== 查询方法 ====================

    /**
     * 获取最高伤害玩家
     *
     * @return 最高伤害玩家UUID
     */
    public UUID getTopDamager() {
        if (ranking == null || ranking.getRankings().isEmpty()) {
            return null;
        }

        return ranking.getRankings().get(0).playerUUID;
    }

    /**
     * 获取参与玩家数
     *
     * @return 玩家数
     */
    public int getParticipantCount() {
        if (ranking == null) {
            return 0;
        }

        return ranking.getRankings().size();
    }

    /**
     * 获取玩家排名
     *
     * @param playerUUID 玩家UUID
     * @return 排名 (从1开始)，如果不在排行则返回-1
     */
    public int getPlayerRank(UUID playerUUID) {
        if (ranking == null) {
            return -1;
        }

        return ranking.getPlayerRank(playerUUID);
    }

    /**
     * 获取玩家伤害
     *
     * @param playerUUID 玩家UUID
     * @return 伤害值
     */
    public double getPlayerDamage(UUID playerUUID) {
        if (damageRecord == null) {
            return 0.0;
        }

        return damageRecord.getPlayerDamage(playerUUID);
    }

    /**
     * 获取玩家伤害百分比
     *
     * @param playerUUID 玩家UUID
     * @return 伤害百分比 (0.0-1.0)
     */
    public double getPlayerDamagePercentage(UUID playerUUID) {
        if (damageRecord == null || damageRecord.getTotalDamage() <= 0) {
            return 0.0;
        }

        return getPlayerDamage(playerUUID) / damageRecord.getTotalDamage();
    }

    /**
     * 获取前N名伤害者
     *
     * @param limit 数量限制
     * @return 排行列表
     */
    public List<DamageRanking.RankingEntry> getTopDamagers(int limit) {
        if (ranking == null) {
            return new ArrayList<>();
        }

        return ranking.getTopN(limit);
    }

    /**
     * 获取总伤害
     *
     * @return 总伤害值
     */
    public double getTotalDamage() {
        if (statistics == null) {
            return 0.0;
        }

        return statistics.getTotalDamage();
    }

    /**
     * 获取所有参与玩家的UUID集合
     *
     * @return 参与玩家UUID集合
     */
    public Set<UUID> getParticipants() {
        if (damageRecord == null || damageRecord.getPlayerDamage() == null) {
            return new HashSet<>();
        }

        return new HashSet<>(damageRecord.getPlayerDamage().keySet());
    }

    /**
     * 获取记录创建时间
     *
     * @return 记录时间戳 (毫秒)
     */
    public long getRecordTime() {
        return this.startTime;
    }

    /**
     * 获取DPS (每秒伤害)
     *
     * @return DPS值
     */
    public double getDPS() {
        if (statistics == null) {
            return 0.0;
        }

        return statistics.getDps();
    }

    /**
     * 获取平均伤害
     *
     * @return 平均伤害值
     */
    public double getAverageDamage() {
        if (statistics == null) {
            return 0.0;
        }

        return statistics.getAverageSingleDamage();
    }

    // ==================== 数据操作方法 ====================

    /**
     * 标记为已归档
     */
    public void archive() {
        this.archived = true;
    }

    /**
     * 设置元数据
     *
     * @param key 键
     * @param value 值
     */
    public void setMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }

        this.metadata.put(key, value);
    }

    /**
     * 获取元数据
     *
     * @param key 键
     * @return 值
     */
    public Object getMetadata(String key) {
        if (this.metadata == null) {
            return null;
        }

        return this.metadata.get(key);
    }

    // ==================== 转换方法 ====================

    /**
     * 转换为Map (用于序列化)
     *
     * @return 数据映射
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("bossUUID", bossUUID);
        map.put("bossType", bossType);
        map.put("bossTier", bossTier);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("durationSeconds", durationSeconds);

        if (statistics != null) {
            map.put("statistics", statistics.toMap());
        }

        map.put("participantCount", getParticipantCount());
        map.put("totalDamage", getTotalDamage());
        map.put("dps", getDPS());
        map.put("archived", archived);
        map.put("createdAt", createdAt);

        return map;
    }

    // ==================== 信息方法 ====================

    /**
     * 获取简要信息
     *
     * @return 信息字符串
     */
    public String getSimpleInfo() {
        return String.format(
            "DamageHistory{type=%s, tier=%d, damage=%.1f, dps=%.1f, players=%d, duration=%ds}",
            bossType, bossTier, getTotalDamage(), getDPS(), getParticipantCount(), durationSeconds
        );
    }

    /**
     * 获取详细信息
     *
     * @return 详细信息字符串
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("DamageHistory{\\n");
        sb.append("  Boss UUID: ").append(bossUUID).append("\\n");
        sb.append("  类型: ").append(bossType).append("\\n");
        sb.append("  等级: ").append(bossTier).append("\\n");
        sb.append("  开始时间: ").append(new Date(startTime)).append("\\n");
        sb.append("  结束时间: ").append(new Date(endTime)).append("\\n");
        sb.append("  持续时间: ").append(durationSeconds).append("秒\\n");
        sb.append("  总伤害: ").append(String.format("%.1f", getTotalDamage())).append("\\n");
        sb.append("  DPS: ").append(String.format("%.1f", getDPS())).append("\\n");
        sb.append("  参与玩家: ").append(getParticipantCount()).append("\\n");
        sb.append("  已归档: ").append(archived).append("\\n");

        if (!getRankings().isEmpty()) {
            sb.append("  前3名伤害者:\\n");
            for (DamageRanking.RankingEntry entry : getTopDamagers(3)) {
                sb.append("    ").append(entry.getInfo()).append("\\n");
            }
        }

        sb.append("}\\n");
        return sb.toString();
    }

    /**
     * 获取排行列表
     *
     * @return 排行列表
     */
    public List<DamageRanking.RankingEntry> getRankings() {
        if (ranking == null) {
            return new ArrayList<>();
        }

        return ranking.getRankings();
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DamageHistory that = (DamageHistory) o;
        return Objects.equals(bossUUID, that.bossUUID) &&
               startTime == that.startTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bossUUID, startTime);
    }
}
