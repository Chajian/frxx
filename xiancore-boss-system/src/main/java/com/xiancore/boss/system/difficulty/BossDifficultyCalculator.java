package com.xiancore.boss.system.difficulty;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss难度计算器
 * 根据多个因素动态计算Boss的难度系数
 *
 * 职责:
 * - 计算参与人数修饰
 * - 计算玩家战力修饰
 * - 计算击杀历史修饰
 * - 计算时间因素修饰
 * - 综合计算最终难度分数
 *
 * 难度分数: 0-100 (0=简单, 100=绝望)
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossDifficultyCalculator {

    // ==================== 难度等级定义 ====================

    public enum DifficultyLevel {
        EASY(0, 20, "简单", "绿色", 1.0, 1.2),
        NORMAL(21, 40, "普通", "黄色", 1.2, 1.5),
        HARD(41, 60, "困难", "橙色", 1.5, 2.0),
        HELL(61, 80, "地狱", "红色", 2.0, 3.0),
        DESPERATE(81, 100, "绝望", "紫色", 3.0, 5.0);

        public final int minScore;
        public final int maxScore;
        public final String name;
        public final String color;
        public final double minMultiplier;
        public final double maxMultiplier;

        DifficultyLevel(int minScore, int maxScore, String name, String color,
                       double minMultiplier, double maxMultiplier) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.name = name;
            this.color = color;
            this.minMultiplier = minMultiplier;
            this.maxMultiplier = maxMultiplier;
        }

        public static DifficultyLevel fromScore(int score) {
            for (DifficultyLevel level : values()) {
                if (score >= level.minScore && score <= level.maxScore) {
                    return level;
                }
            }
            return DESPERATE;
        }
    }

    // ==================== 配置常量 ====================

    /** 参与人数基础值 (标准人数) */
    private static final int PARTICIPANT_BASE = 3;

    /** 每个人数变化的修饰值 */
    private static final double PARTICIPANT_MULTIPLIER_PER_PERSON = 0.1;

    /** 参与人数修饰下限 */
    private static final double PARTICIPANT_MIN = 0.3;

    /** 参与人数修饰上限 */
    private static final double PARTICIPANT_MAX = 1.5;

    /** 战力差 - 玩家太弱阈值 (-50%) */
    private static final double POWER_WEAK_THRESHOLD = -0.5;

    /** 战力差 - 玩家太强阈值 (50%) */
    private static final double POWER_STRONG_THRESHOLD = 0.5;

    /** 玩家太弱时的修饰 */
    private static final double POWER_WEAK_MULTIPLIER = 2.0;

    /** 玩家平衡时的修饰 */
    private static final double POWER_BALANCED_MULTIPLIER = 1.0;

    /** 玩家太强时的修饰 */
    private static final double POWER_STRONG_MULTIPLIER = 0.5;

    /** 击杀历史修饰 - 每次击杀增加 */
    private static final double HISTORY_MULTIPLIER_PER_KILL = 0.02;

    /** 击杀历史修饰上限 */
    private static final double HISTORY_MAX_MULTIPLIER = 2.0;

    /** 时间窗口 (小时) */
    private static final int TIME_WINDOW_HOURS = 1;

    /** 每次最近击杀的修饰 */
    private static final double RECENT_KILL_MULTIPLIER_PER_COUNT = 0.05;

    /** 最近击杀修饰上限 */
    private static final double RECENT_KILL_MAX_MULTIPLIER = 1.2;

    // ==================== 内部状态 ====================

    /** Boss UUID */
    private final java.util.UUID bossUUID;

    /** 基础难度分数 (初始化为50) */
    private volatile int baseDifficultyScore = 50;

    /** 当前难度分数 */
    private volatile int currentDifficultyScore = 50;

    /** 当前难度等级 */
    private volatile DifficultyLevel currentDifficultyLevel = DifficultyLevel.NORMAL;

    /** 最后更新时间 */
    private volatile long lastUpdateTime = 0;

    /** 更新间隔 (毫秒) */
    private volatile long updateInterval = 1000; // 1秒更新一次

    /** 击杀历史时间戳 (用于时间窗口计算) */
    private final Map<Long, Integer> killTimestamps = new ConcurrentHashMap<>();

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param bossUUID Boss UUID
     */
    public BossDifficultyCalculator(java.util.UUID bossUUID) {
        this.bossUUID = bossUUID;
    }

    // ==================== 核心计算方法 ====================

    /**
     * 计算难度分数
     *
     * @param participantCount 参与人数
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @param totalKillCount 总击杀次数
     * @param recentKillCount 最近1小时击杀次数
     * @return 难度分数 (0-100)
     */
    public int calculateDifficultyScore(int participantCount,
                                       double playerAveragePower,
                                       double bossRecommendedPower,
                                       int totalKillCount,
                                       int recentKillCount) {
        // 检查更新间隔
        if (!shouldUpdate()) {
            return currentDifficultyScore;
        }

        try {
            // 基础分数
            int score = 50;

            // 1. 参与人数调整 (-30 到 +30分)
            int participantAdjustment = calculateParticipantAdjustment(participantCount);
            score += participantAdjustment;

            // 2. 战力调整 (-40 到 +40分)
            int powerAdjustment = calculatePowerAdjustment(playerAveragePower, bossRecommendedPower);
            score += powerAdjustment;

            // 3. 历史调整 (0 到 +30分)
            int historyAdjustment = calculateHistoryAdjustment(totalKillCount);
            score += historyAdjustment;

            // 4. 时间调整 (0 到 +10分)
            int timeAdjustment = calculateTimeAdjustment(recentKillCount);
            score += timeAdjustment;

            // 限制在0-100范围内
            score = Math.max(0, Math.min(100, score));

            // 保存结果
            this.currentDifficultyScore = score;
            this.currentDifficultyLevel = DifficultyLevel.fromScore(score);
            this.lastUpdateTime = System.currentTimeMillis();

            return score;
        } catch (Exception e) {
            e.printStackTrace();
            return currentDifficultyScore;
        }
    }

    /**
     * 计算参与人数调整
     * 3人为标准 (0分), 少于3人增加难度, 多于3人降低难度
     *
     * @param participantCount 参与人数
     * @return 调整分数 (-30 到 +30)
     */
    private int calculateParticipantAdjustment(int participantCount) {
        int adjustment = (PARTICIPANT_BASE - participantCount) * 10;
        return Math.max(-30, Math.min(30, adjustment));
    }

    /**
     * 计算玩家战力调整
     *
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @return 调整分数 (-40 到 +40)
     */
    private int calculatePowerAdjustment(double playerAveragePower, double bossRecommendedPower) {
        if (bossRecommendedPower <= 0) {
            return 0;
        }

        double powerDifference = (playerAveragePower - bossRecommendedPower) / bossRecommendedPower;

        // 转换为分数: 战力每差10%, 调整10分
        int adjustment = (int) (powerDifference * 40);

        return Math.max(-40, Math.min(40, adjustment));
    }

    /**
     * 计算击杀历史调整
     * 击杀次数越多, 难度越高
     *
     * @param totalKillCount 总击杀次数
     * @return 调整分数 (0 到 +30)
     */
    private int calculateHistoryAdjustment(int totalKillCount) {
        // 每10次击杀增加6分
        int adjustment = (int) Math.min(30, totalKillCount * 0.6);
        return Math.max(0, adjustment);
    }

    /**
     * 计算时间因素调整
     * 同一个Boss在短时间内被重复击杀, 难度上升
     *
     * @param recentKillCount 最近1小时击杀次数
     * @return 调整分数 (0 到 +10)
     */
    private int calculateTimeAdjustment(int recentKillCount) {
        // 每次最近击杀增加2分
        int adjustment = recentKillCount * 2;
        return Math.max(0, Math.min(10, adjustment));
    }

    /**
     * 计算参与人数修饰倍数
     *
     * @param participantCount 参与人数
     * @return 修饰倍数
     */
    public double getParticipantModifier(int participantCount) {
        double multiplier = 1.0 + (PARTICIPANT_BASE - participantCount) * PARTICIPANT_MULTIPLIER_PER_PERSON;
        return Math.max(PARTICIPANT_MIN, Math.min(PARTICIPANT_MAX, multiplier));
    }

    /**
     * 计算玩家战力修饰倍数
     *
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @return 修饰倍数
     */
    public double getPlayerPowerModifier(double playerAveragePower, double bossRecommendedPower) {
        if (bossRecommendedPower <= 0) {
            return POWER_BALANCED_MULTIPLIER;
        }

        double powerDifference = (playerAveragePower - bossRecommendedPower) / bossRecommendedPower;

        if (powerDifference < POWER_WEAK_THRESHOLD) {
            return POWER_WEAK_MULTIPLIER;
        } else if (powerDifference > POWER_STRONG_THRESHOLD) {
            return POWER_STRONG_MULTIPLIER;
        } else {
            return POWER_BALANCED_MULTIPLIER + (powerDifference);
        }
    }

    /**
     * 计算击杀历史修饰倍数
     *
     * @param totalKillCount 总击杀次数
     * @return 修饰倍数
     */
    public double getHistoryModifier(int totalKillCount) {
        double multiplier = 1.0 + (totalKillCount * HISTORY_MULTIPLIER_PER_KILL);
        return Math.min(HISTORY_MAX_MULTIPLIER, multiplier);
    }

    /**
     * 计算时间因素修饰倍数
     *
     * @param recentKillCount 最近1小时击杀次数
     * @return 修饰倍数
     */
    public double getTimeModifier(int recentKillCount) {
        double multiplier = 1.0 + (recentKillCount * RECENT_KILL_MULTIPLIER_PER_COUNT);
        return Math.min(RECENT_KILL_MAX_MULTIPLIER, multiplier);
    }

    /**
     * 综合计算难度倍数
     *
     * @param participantCount 参与人数
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @param totalKillCount 总击杀次数
     * @param recentKillCount 最近击杀次数
     * @return 综合难度倍数
     */
    public double calculateDifficultyMultiplier(int participantCount,
                                               double playerAveragePower,
                                               double bossRecommendedPower,
                                               int totalKillCount,
                                               int recentKillCount) {
        double participantMult = getParticipantModifier(participantCount);
        double powerMult = getPlayerPowerModifier(playerAveragePower, bossRecommendedPower);
        double historyMult = getHistoryModifier(totalKillCount);
        double timeMult = getTimeModifier(recentKillCount);

        // 综合倍数 = 参与人数 × 战力 × 历史 × 时间
        // 但参与人数和时间因素权重较低
        return participantMult * powerMult * (1.0 + (historyMult - 1.0) * 0.5) * (1.0 + (timeMult - 1.0) * 0.3);
    }

    /**
     * 获取难度分数对应的等级
     *
     * @return 难度等级
     */
    public DifficultyLevel getDifficultyLevel() {
        return currentDifficultyLevel;
    }

    /**
     * 获取难度信息字符串
     *
     * @return 难度信息
     */
    public String getDifficultyInfo() {
        return String.format(
            "等级: %s, 分数: %d/100, 颜色: %s",
            currentDifficultyLevel.name,
            currentDifficultyScore,
            currentDifficultyLevel.color
        );
    }

    /**
     * 获取详细的难度报告
     *
     * @param participantCount 参与人数
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @param totalKillCount 总击杀次数
     * @param recentKillCount 最近击杀次数
     * @return 详细报告
     */
    public String getDifficultyReport(int participantCount,
                                     double playerAveragePower,
                                     double bossRecommendedPower,
                                     int totalKillCount,
                                     int recentKillCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Boss难度报告 ===\n");
        sb.append(String.format("难度等级: %s (%d/100)\n", currentDifficultyLevel.name, currentDifficultyScore));
        sb.append(String.format("参与人数: %d人 (修饰: %.2fx)\n", participantCount,
            getParticipantModifier(participantCount)));
        sb.append(String.format("战力对比: 玩家%.0f vs Boss%d (修饰: %.2fx)\n", playerAveragePower,
            (int)bossRecommendedPower, getPlayerPowerModifier(playerAveragePower, bossRecommendedPower)));
        sb.append(String.format("击杀历史: %d次 (修饰: %.2fx)\n", totalKillCount,
            getHistoryModifier(totalKillCount)));
        sb.append(String.format("最近击杀: %d次 (修饰: %.2fx)\n", recentKillCount,
            getTimeModifier(recentKillCount)));

        return sb.toString();
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查是否应该更新
     */
    private boolean shouldUpdate() {
        return (System.currentTimeMillis() - lastUpdateTime) >= updateInterval;
    }

    /**
     * 设置更新间隔
     *
     * @param milliseconds 毫秒
     */
    public void setUpdateInterval(long milliseconds) {
        this.updateInterval = milliseconds;
    }

    /**
     * 强制更新
     */
    public void forceUpdate() {
        this.lastUpdateTime = 0;
    }

    /**
     * 记录击杀时间
     */
    public void recordKillTime(long timestamp) {
        killTimestamps.put(timestamp, 1);

        // 清理超过时间窗口的记录
        long windowStart = System.currentTimeMillis() - (TIME_WINDOW_HOURS * 60 * 60 * 1000);
        killTimestamps.entrySet().removeIf(entry -> entry.getKey() < windowStart);
    }

    /**
     * 获取最近击杀数
     *
     * @return 最近1小时击杀次数
     */
    public int getRecentKillCount() {
        long windowStart = System.currentTimeMillis() - (TIME_WINDOW_HOURS * 60 * 60 * 1000);
        return (int) killTimestamps.entrySet()
            .stream()
            .filter(entry -> entry.getKey() >= windowStart)
            .count();
    }

    /**
     * 重置难度计算
     */
    public void reset() {
        this.currentDifficultyScore = 50;
        this.currentDifficultyLevel = DifficultyLevel.NORMAL;
        this.lastUpdateTime = 0;
        this.killTimestamps.clear();
    }
}
