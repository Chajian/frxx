package com.xiancore.ai;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 动态难度调整 - 实时游戏难度自适应
 * Dynamic Difficulty - Real-time Game Difficulty Adaptation
 *
 * @author XianCore
 * @version 1.0
 */
public class DynamicDifficulty {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, DifficultySession> sessions = new ConcurrentHashMap<>();

    /**
     * 难度级别
     */
    public enum DifficultyLevel {
        TRIVIAL(0.5),      // 极易
        EASY(0.7),         // 简单
        NORMAL(1.0),       // 普通
        HARD(1.3),         // 困难
        INSANE(1.8);       // 疯狂

        public final double multiplier;

        DifficultyLevel(double multiplier) {
            this.multiplier = multiplier;
        }
    }

    /**
     * 难度调整参数
     */
    public static class DifficultyModifier {
        public double bossHealthMultiplier;      // Boss血量倍数
        public double bossDamageMultiplier;      // Boss伤害倍数
        public double bossSpeedMultiplier;       // Boss速度倍数
        public double skillCooldownMultiplier;   // 技能冷却倍数
        public double playerDamageMultiplier;    // 玩家伤害倍数
        public double encounterRewardMultiplier; // 遭遇奖励倍数

        public DifficultyModifier(DifficultyLevel level) {
            this.bossHealthMultiplier = level.multiplier;
            this.bossDamageMultiplier = level.multiplier * 0.9;
            this.bossSpeedMultiplier = level.multiplier * 0.8;
            this.skillCooldownMultiplier = 2.0 - level.multiplier;
            this.playerDamageMultiplier = 1.0 / level.multiplier;
            this.encounterRewardMultiplier = level.multiplier;
        }
    }

    /**
     * 难度调整会话
     */
    public static class DifficultySession {
        public String sessionId;
        public String bossId;
        public DifficultyLevel currentLevel;
        public DifficultyModifier modifier;
        public List<String> players;
        public PerformanceMetrics metrics;
        public long sessionStartTime;
        public int adjustmentCount;
        public List<DifficultyAdjustment> adjustmentHistory;

        public DifficultySession(String sessionId, String bossId, List<String> players) {
            this.sessionId = sessionId;
            this.bossId = bossId;
            this.currentLevel = DifficultyLevel.NORMAL;
            this.modifier = new DifficultyModifier(DifficultyLevel.NORMAL);
            this.players = new ArrayList<>(players);
            this.metrics = new PerformanceMetrics();
            this.sessionStartTime = System.currentTimeMillis();
            this.adjustmentCount = 0;
            this.adjustmentHistory = new ArrayList<>();
        }

        public double getSessionDuration() {
            return (System.currentTimeMillis() - sessionStartTime) / 1000.0;  // 秒
        }
    }

    /**
     * 性能指标
     */
    public static class PerformanceMetrics {
        public double playerWinRate;            // 玩家胜率 (0-1)
        public double bossDamagePerSecond;      // Boss每秒伤害
        public double playerDamagePerSecond;    // 玩家每秒伤害
        public double averagePlayerHealth;      // 平均玩家血量%
        public int playerDeaths;                // 玩家死亡次数
        public double sessionDuration;          // 会话时长
        public double difficultyRating;         // 难度评分 (0-100)

        public PerformanceMetrics() {
            this.playerWinRate = 0.5;
            this.bossDamagePerSecond = 0;
            this.playerDamagePerSecond = 0;
            this.averagePlayerHealth = 100;
            this.playerDeaths = 0;
            this.sessionDuration = 0;
            this.difficultyRating = 50;
        }

        public void calculateDifficultyRating() {
            // 难度评分 = (玩家伤害/Boss伤害) * 50 + 玩家胜率 * 50
            double damageRatio = playerDamagePerSecond / Math.max(1, bossDamagePerSecond);
            difficultyRating = (damageRatio * 50) + (playerWinRate * 50);
            difficultyRating = Math.min(100, Math.max(0, difficultyRating));
        }
    }

    /**
     * 难度调整记录
     */
    public static class DifficultyAdjustment {
        public long timestamp;
        public DifficultyLevel previousLevel;
        public DifficultyLevel newLevel;
        public String reason;
        public PerformanceMetrics metricsSnapshot;

        public DifficultyAdjustment(DifficultyLevel prev, DifficultyLevel next, String reason, PerformanceMetrics metrics) {
            this.timestamp = System.currentTimeMillis();
            this.previousLevel = prev;
            this.newLevel = next;
            this.reason = reason;
            this.metricSnapshot = new PerformanceMetrics();
            // 复制指标
            this.metricSnapshot.playerWinRate = metrics.playerWinRate;
            this.metricSnapshot.difficultyRating = metrics.difficultyRating;
        }

        public PerformanceMetrics metricSnapshot;  // 调整时的性能快照
    }

    /**
     * 构造函数
     */
    public DynamicDifficulty() {
        logger.info("✓ DynamicDifficulty已初始化");
    }

    /**
     * 创建难度调整会话
     */
    public DifficultySession createSession(String sessionId, String bossId, List<String> players) {
        DifficultySession session = new DifficultySession(sessionId, bossId, players);
        sessions.put(sessionId, session);
        logger.info("✓ 难度调整会话已创建: " + sessionId);
        return session;
    }

    /**
     * 更新性能指标
     */
    public void updateMetrics(String sessionId, PerformanceMetrics newMetrics) {
        DifficultySession session = sessions.get(sessionId);
        if (session == null) return;

        session.metrics = newMetrics;
        newMetrics.calculateDifficultyRating();
    }

    /**
     * 评估并调整难度
     */
    public void evaluateAndAdjustDifficulty(String sessionId) {
        DifficultySession session = sessions.get(sessionId);
        if (session == null) return;

        PerformanceMetrics metrics = session.metrics;
        metrics.calculateDifficultyRating();

        DifficultyLevel newLevel = session.currentLevel;

        // 根据难度评分调整
        if (metrics.difficultyRating < 30) {
            // 太容易，提高难度
            newLevel = increaseDifficulty(session.currentLevel);
        } else if (metrics.difficultyRating > 70) {
            // 太难，降低难度
            newLevel = decreaseDifficulty(session.currentLevel);
        }

        if (newLevel != session.currentLevel) {
            applyDifficultyChange(session, newLevel);
        }
    }

    /**
     * 提高难度
     */
    private DifficultyLevel increaseDifficulty(DifficultyLevel current) {
        return switch (current) {
            case TRIVIAL -> DifficultyLevel.EASY;
            case EASY -> DifficultyLevel.NORMAL;
            case NORMAL -> DifficultyLevel.HARD;
            case HARD -> DifficultyLevel.INSANE;
            case INSANE -> DifficultyLevel.INSANE;
        };
    }

    /**
     * 降低难度
     */
    private DifficultyLevel decreaseDifficulty(DifficultyLevel current) {
        return switch (current) {
            case INSANE -> DifficultyLevel.HARD;
            case HARD -> DifficultyLevel.NORMAL;
            case NORMAL -> DifficultyLevel.EASY;
            case EASY -> DifficultyLevel.TRIVIAL;
            case TRIVIAL -> DifficultyLevel.TRIVIAL;
        };
    }

    /**
     * 应用难度变化
     */
    private void applyDifficultyChange(DifficultySession session, DifficultyLevel newLevel) {
        DifficultyLevel previousLevel = session.currentLevel;

        // 记录调整
        String reason = "自动调整: 难度评分 " + String.format("%.1f", session.metrics.difficultyRating);
        DifficultyAdjustment adjustment = new DifficultyAdjustment(previousLevel, newLevel, reason, session.metrics);
        session.adjustmentHistory.add(adjustment);
        session.adjustmentCount++;

        // 应用新的修饰符
        session.currentLevel = newLevel;
        session.modifier = new DifficultyModifier(newLevel);

        logger.info("↻ 难度已调整: " + previousLevel.name() + " -> " + newLevel.name() +
                " (评分: " + String.format("%.1f", session.metrics.difficultyRating) + ")");
    }

    /**
     * 手动设置难度
     */
    public void setDifficulty(String sessionId, DifficultyLevel level) {
        DifficultySession session = sessions.get(sessionId);
        if (session == null) return;

        if (session.currentLevel != level) {
            DifficultyLevel previousLevel = session.currentLevel;
            DifficultyAdjustment adjustment = new DifficultyAdjustment(previousLevel, level, "手动设置", session.metrics);
            session.adjustmentHistory.add(adjustment);

            session.currentLevel = level;
            session.modifier = new DifficultyModifier(level);

            logger.info("✓ 难度已手动设置: " + level.name());
        }
    }

    /**
     * 获取修饰后的Boss属性
     */
    public Map<String, Double> getModifiedBossStats(String sessionId, Map<String, Double> baseStats) {
        DifficultySession session = sessions.get(sessionId);
        if (session == null) return baseStats;

        Map<String, Double> modified = new HashMap<>();
        for (Map.Entry<String, Double> entry : baseStats.entrySet()) {
            String key = entry.getKey().toLowerCase();
            double value = entry.getValue();

            if (key.contains("health")) {
                value *= session.modifier.bossHealthMultiplier;
            } else if (key.contains("damage")) {
                value *= session.modifier.bossDamageMultiplier;
            } else if (key.contains("speed")) {
                value *= session.modifier.bossSpeedMultiplier;
            }

            modified.put(entry.getKey(), value);
        }

        return modified;
    }

    /**
     * 获取修饰后的玩家伤害
     */
    public double getModifiedPlayerDamage(String sessionId, double baseDamage) {
        DifficultySession session = sessions.get(sessionId);
        if (session == null) return baseDamage;
        return baseDamage * session.modifier.playerDamageMultiplier;
    }

    /**
     * 获取修饰后的奖励
     */
    public double getModifiedReward(String sessionId, double baseReward) {
        DifficultySession session = sessions.get(sessionId);
        if (session == null) return baseReward;
        return baseReward * session.modifier.encounterRewardMultiplier;
    }

    /**
     * 获取会话
     */
    public DifficultySession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 获取所有会话
     */
    public Collection<DifficultySession> getAllSessions() {
        return sessions.values();
    }

    /**
     * 结束会话
     */
    public void endSession(String sessionId) {
        sessions.remove(sessionId);
        logger.info("✓ 难度调整会话已结束: " + sessionId);
    }

    /**
     * 获取调整历史
     */
    public List<DifficultyAdjustment> getAdjustmentHistory(String sessionId) {
        DifficultySession session = sessions.get(sessionId);
        return session != null ? new ArrayList<>(session.adjustmentHistory) : new ArrayList<>();
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("active_sessions", sessions.size());

        // 难度级别分布
        Map<String, Integer> difficultyDistribution = new HashMap<>();
        for (DifficultySession session : sessions.values()) {
            difficultyDistribution.merge(session.currentLevel.name(), 1, Integer::sum);
        }
        stats.put("difficulty_distribution", difficultyDistribution);

        // 平均难度评分
        double avgRating = sessions.values().stream()
                .mapToDouble(s -> s.metrics.difficultyRating)
                .average()
                .orElse(50);
        stats.put("avg_difficulty_rating", String.format("%.1f", avgRating));

        // 总调整次数
        int totalAdjustments = (int) sessions.values().stream()
                .mapToInt(s -> s.adjustmentCount)
                .sum();
        stats.put("total_adjustments", totalAdjustments);

        return stats;
    }

    /**
     * 重置所有会话
     */
    public void reset() {
        sessions.clear();
        logger.info("✓ 所有难度调整会话已重置");
    }
}
