package com.xiancore.systems.boss.damage;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 伤害排行榜
 * 维护Boss的伤害排行信息
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class DamageRanking {

    // ==================== 排行数据 ====================
    /** Boss UUID */
    private UUID bossUUID;

    /** 排行列表 */
    private List<RankingEntry> rankings = new ArrayList<>();

    /** 更新时间 */
    private long updateTime = System.currentTimeMillis();

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param bossUUID Boss UUID
     */
    public DamageRanking(UUID bossUUID) {
        this.bossUUID = bossUUID;
    }

    // ==================== 更新方法 ====================

    /**
     * 从DamageRecord更新排行
     *
     * @param record 伤害记录
     */
    public void updateRanking(DamageRecord record) {
        rankings.clear();

        int rank = 1;
        double totalDamage = record.getTotalDamage();

        List<UUID> sorted = record.getSortedPlayers();

        for (UUID playerId : sorted) {
            double dmg = record.getPlayerDamage(playerId);
            double percentage = totalDamage > 0 ? dmg / totalDamage : 0.0;
            rankings.add(new RankingEntry(rank, playerId, dmg, percentage));
            rank++;
        }

        this.updateTime = System.currentTimeMillis();
    }

    // ==================== 查询方法 ====================

    /**
     * 获取前N名
     *
     * @param n 数量
     * @return 排行列表
     */
    public List<RankingEntry> getTopN(int n) {
        return rankings.stream().limit(n).collect(Collectors.toList());
    }

    /**
     * 获取玩家排名
     *
     * @param playerUUID 玩家UUID
     * @return 排名位置 (从1开始)，如果不在排行则返回-1
     */
    public int getPlayerRank(UUID playerUUID) {
        for (RankingEntry entry : rankings) {
            if (entry.playerUUID.equals(playerUUID)) {
                return entry.rank;
            }
        }

        return -1;
    }

    /**
     * 获取玩家的排行条目
     *
     * @param playerUUID 玩家UUID
     * @return 排行条目，如果不存在则返回null
     */
    public RankingEntry getPlayerRankingEntry(UUID playerUUID) {
        return rankings.stream()
            .filter(e -> e.playerUUID.equals(playerUUID))
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查玩家是否在排行中
     *
     * @param playerUUID 玩家UUID
     * @return 是否在排行中
     */
    public boolean isInRanking(UUID playerUUID) {
        return rankings.stream().anyMatch(e -> e.playerUUID.equals(playerUUID));
    }

    /**
     * 获取排行大小
     *
     * @return 排行人数
     */
    public int getRankingSize() {
        return rankings.size();
    }

    // ==================== 内部类 ====================

    /**
     * 排行条目
     */
    @Getter
    public static class RankingEntry {
        public int rank;
        public UUID playerUUID;
        public double damage;
        public double percentage;

        public RankingEntry(int rank, UUID playerUUID, double damage, double percentage) {
            this.rank = rank;
            this.playerUUID = playerUUID;
            this.damage = damage;
            this.percentage = percentage;
        }

        /**
         * 获取信息字符串
         *
         * @return 信息字符串
         */
        public String getInfo() {
            return String.format("#%d: %s (%.1f伤害, %.1f%%)",
                rank, playerUUID.toString().substring(0, 8), damage, percentage * 100);
        }

        @Override
        public String toString() {
            return getInfo();
        }
    }

    // ==================== 信息方法 ====================

    /**
     * 获取简要信息
     *
     * @return 信息字符串
     */
    public String getSimpleInfo() {
        return String.format("DamageRanking{boss=%s, players=%d}", bossUUID, rankings.size());
    }

    /**
     * 获取详细排行信息
     *
     * @param limit 显示数量
     * @return 排行信息字符串
     */
    public String getDetailedInfo(int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 伤害排行榜 ===\\n");

        for (RankingEntry entry : getTopN(limit)) {
            sb.append(entry.getInfo()).append("\\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }
}
