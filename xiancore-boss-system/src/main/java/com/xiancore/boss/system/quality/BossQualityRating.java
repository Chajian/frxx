package com.xiancore.boss.system.quality;

import lombok.Getter;

import java.util.*;

/**
 * Boss品质评级系统
 * 根据多个因素计算Boss战斗的品质评级 (S/A/B/C/D)
 *
 * 职责:
 * - 计算基础品质分数
 * - 应用加分条件 (无人死亡等)
 * - 应用减分条件 (战斗超时等)
 * - 确定最终品质等级
 * - 提供奖励倍数
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossQualityRating {

    // ==================== 品质等级定义 ====================

    public enum QualityLevel {
        S(90, 100, "S", "金色", 2.0, 3.0),
        A(75, 89, "A", "紫色", 1.5, 2.0),
        B(50, 74, "B", "蓝色", 1.0, 1.0),
        C(25, 49, "C", "绿色", 0.8, 0.8),
        D(0, 24, "D", "灰色", 0.5, 0.5);

        private final int minScore;
        private final int maxScore;
        private final String level;
        private final String color;
        private final double experienceMultiplier;
        private final double dropMultiplier;

        QualityLevel(int minScore, int maxScore, String level, String color,
                    double experienceMultiplier, double dropMultiplier) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.level = level;
            this.color = color;
            this.experienceMultiplier = experienceMultiplier;
            this.dropMultiplier = dropMultiplier;
        }

        public static QualityLevel fromScore(int score) {
            for (QualityLevel quality : values()) {
                if (score >= quality.minScore && score <= quality.maxScore) {
                    return quality;
                }
            }
            return D;
        }
    }

    // ==================== 配置常量 ====================

    // 加分条件
    private static final int BONUS_NO_DEATH = 10;           // 无人死亡
    private static final int BONUS_UNDER_TIME_LIMIT = 5;    // 10分钟内完成
    private static final int BONUS_ALL_SURVIVE = 15;        // 所有玩家生存
    private static final int BONUS_NO_DAMAGE = 20;          // 零受伤完成
    private static final int BONUS_FIRST_KILL = 15;         // 新手高挑战

    // 减分条件
    private static final int PENALTY_OVERTIME = -10;        // 超过40分钟
    private static final int PENALTY_PER_DEATH = -5;        // 每个死亡
    private static final int PENALTY_TIMEOUT_WEAK = -20;    // Boss超时变弱
    private static final int PENALTY_SPAM_KILL = -15;       // 刷屏击杀

    // 时间限制 (毫秒)
    private static final long TIME_LIMIT_BONUS = 10 * 60 * 1000;  // 10分钟
    private static final long TIME_LIMIT_PENALTY = 40 * 60 * 1000; // 40分钟

    // ==================== 内部状态 ====================

    /** Boss UUID */
    private final UUID bossUUID;

    /** 当前品质分数 */
    private volatile int qualityScore = 50;

    /** 当前品质等级 */
    private volatile QualityLevel currentQuality = QualityLevel.B;

    // ==================== 构造函数 ====================

    public BossQualityRating(UUID bossUUID) {
        this.bossUUID = bossUUID;
    }

    // ==================== 核心计算方法 ====================

    /**
     * 计算战斗品质
     *
     * @param participantCount 参与人数
     * @param bossLevel Boss等级
     * @param difficultyScore 难度分数 (0-100)
     * @param duration 战斗时长 (毫秒)
     * @param deathCount 死亡人数
     * @param isBossWeakened Boss是否被削弱
     * @param isRecentKill 是否是最近刷屏击杀
     * @return 品质分数 (0-100)
     */
    public int calculateQuality(int participantCount,
                               int bossLevel,
                               int difficultyScore,
                               long duration,
                               int deathCount,
                               boolean isBossWeakened,
                               boolean isRecentKill) {
        try {
            // 基础分数 = (参与人数 + Boss等级×20 + 难度系数×30) / 3
            int baseScore = (participantCount + (bossLevel * 20) + (difficultyScore * 30)) / 3;
            baseScore = Math.max(0, Math.min(100, baseScore));

            // 应用加分条件
            int score = baseScore;

            // 无人死亡加分
            if (deathCount == 0) {
                score += BONUS_NO_DEATH;
            }

            // 时间限制加分
            if (duration < TIME_LIMIT_BONUS) {
                score += BONUS_UNDER_TIME_LIMIT;
            }

            // 所有玩家生存 (极难条件)
            if (deathCount == 0 && participantCount >= 3) {
                score += BONUS_ALL_SURVIVE;
            }

            // 零受伤完成 (几乎不可能)
            // 需要单独的健康跟踪, 这里作为特殊奖励

            // 新手高挑战 (难度高但成功)
            if (difficultyScore > 70 && deathCount == 0) {
                score += BONUS_FIRST_KILL;
            }

            // 应用减分条件

            // 超时减分
            if (duration > TIME_LIMIT_PENALTY) {
                score += PENALTY_OVERTIME;
            }

            // 死亡减分
            score += (deathCount * PENALTY_PER_DEATH);

            // Boss被削弱减分
            if (isBossWeakened) {
                score += PENALTY_TIMEOUT_WEAK;
            }

            // 刷屏击杀减分
            if (isRecentKill) {
                score += PENALTY_SPAM_KILL;
            }

            // 限制在0-100范围内
            score = Math.max(0, Math.min(100, score));

            // 保存结果
            this.qualityScore = score;
            this.currentQuality = QualityLevel.fromScore(score);

            return score;
        } catch (Exception e) {
            e.printStackTrace();
            return 50; // 默认B级
        }
    }

    /**
     * 获取品质等级
     */
    public QualityLevel getQualityLevel() {
        return currentQuality;
    }

    /**
     * 获取经验倍数
     */
    public double getExperienceMultiplier() {
        return currentQuality.experienceMultiplier;
    }

    /**
     * 获取掉落倍数
     */
    public double getDropMultiplier() {
        return currentQuality.dropMultiplier;
    }

    /**
     * 获取品质信息字符串
     */
    public String getQualityInfo() {
        return String.format(
            "品质: %s, 分数: %d/100, 经验: %.1fx, 掉落: %.1fx",
            currentQuality.level,
            qualityScore,
            currentQuality.experienceMultiplier,
            currentQuality.dropMultiplier
        );
    }

    /**
     * 获取品质描述
     */
    public String getQualityDescription() {
        return String.format(
            "品质%s (%s) - 分数%d/100",
            currentQuality.level,
            currentQuality.color,
            qualityScore
        );
    }

    /**
     * 重置品质评级
     */
    public void reset() {
        this.qualityScore = 50;
        this.currentQuality = QualityLevel.B;
    }
}
