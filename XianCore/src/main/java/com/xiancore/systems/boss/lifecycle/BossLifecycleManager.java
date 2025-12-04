package com.xiancore.systems.boss.lifecycle;

import com.xiancore.systems.boss.damage.DamageStatisticsManager;
import com.xiancore.boss.system.model.BossTier;
import com.xiancore.boss.system.difficulty.BossDifficultyCalculator;
import com.xiancore.boss.system.reward.BossRewardCalculator;
import com.xiancore.boss.system.quality.BossQualityRating;
import com.xiancore.boss.system.progression.BossAttributeProgression;
import com.xiancore.boss.system.damage.DamageRecord;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss生命周期管理器
 * 统一管理Boss的属性提升、难度调整、品质评级、奖励计算
 *
 * 职责:
 * - 初始化Boss生命周期数据
 * - 管理属性提升
 * - 管理难度计算
 * - 管理品质评级
 * - 管理奖励计算
 * - 协调与伤害统计系统的集成
 * - 记录和查询Boss历史
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossLifecycleManager {

    // ==================== 核心数据结构 ====================

    /** Boss生命周期数据存储 */
    private final Map<UUID, BossLifecycleData> bossLifecycleData = new ConcurrentHashMap<>();

    /** 伤害统计管理器引用 */
    private final DamageStatisticsManager damageManager;

    /** 是否已初始化 */
    private volatile boolean initialized = false;

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param damageManager 伤害统计管理器
     */
    public BossLifecycleManager(DamageStatisticsManager damageManager) {
        this.damageManager = damageManager;
    }

    // ==================== 初始化和关闭 ====================

    /**
     * 初始化管理器
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化Boss生命周期管理器失败", e);
        }
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        bossLifecycleData.clear();
        initialized = false;
    }

    // ==================== Boss初始化方法 ====================

    /**
     * 初始化Boss生命周期
     *
     * @param bossUUID Boss UUID
     * @param bossType Boss类型
     * @param bossTier Boss等级
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @param participantCount 参与人数
     * @return Boss生命周期数据
     */
    public synchronized BossLifecycleData initializeBoss(UUID bossUUID,
                                                         String bossType,
                                                         BossTier bossTier,
                                                         double playerAveragePower,
                                                         double bossRecommendedPower,
                                                         int participantCount) {
        if (!initialized) {
            return null;
        }

        try {
            // 1. 创建生命周期数据对象
            BossLifecycleData lifecycleData = new BossLifecycleData();
            lifecycleData.setBossUUID(bossUUID);
            lifecycleData.setBossType(bossType);
            lifecycleData.setBossTier(bossTier);
            lifecycleData.setSpawnTime(System.currentTimeMillis());

            // 2. 初始化属性提升
            BossAttributeProgression progression = new BossAttributeProgression(
                bossUUID, bossType, bossTier, null  // damageManager will be integrated later
            );
            progression.calculateProgression(playerAveragePower, bossRecommendedPower, participantCount);
            lifecycleData.setAttributeProgression(progression);

            // 3. 初始化难度计算
            BossDifficultyCalculator difficultyCalc = new BossDifficultyCalculator(bossUUID);
            int killCount = getHistoricalKillCount(bossType);
            int difficultyScore = difficultyCalc.calculateDifficultyScore(
                participantCount, playerAveragePower, bossRecommendedPower,
                killCount, getRecentKillCount(bossType)
            );
            lifecycleData.setDifficultyCalculator(difficultyCalc);
            lifecycleData.setDifficultyScore(difficultyScore);

            // 4. 初始化品质评级
            BossQualityRating qualityRating = new BossQualityRating(bossUUID);
            lifecycleData.setQualityRating(qualityRating);

            // 5. 初始化奖励计算
            BossRewardCalculator rewardCalc = new BossRewardCalculator(bossUUID, bossTier.getLevel());
            lifecycleData.setRewardCalculator(rewardCalc);

            // 6. 记录参与信息
            lifecycleData.setParticipantCount(participantCount);
            lifecycleData.setAveragePlayerPower(playerAveragePower);

            // 7. 存储数据
            bossLifecycleData.put(bossUUID, lifecycleData);

            return lifecycleData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==================== 动态难度调整方法 ====================

    /**
     * 更新Boss难度 (战斗过程中实时调用)
     *
     * @param bossUUID Boss UUID
     * @param participantCount 当前参与人数
     * @param playerAveragePower 当前玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @return 是否更新成功
     */
    public boolean updateBossDifficulty(UUID bossUUID,
                                       int participantCount,
                                       double playerAveragePower,
                                       double bossRecommendedPower) {
        BossLifecycleData lifecycleData = bossLifecycleData.get(bossUUID);
        if (lifecycleData == null) {
            return false;
        }

        try {
            BossDifficultyCalculator calculator = lifecycleData.getDifficultyCalculator();
            if (calculator == null) {
                return false;
            }

            // 计算新的难度分数
            int killCount = getHistoricalKillCount(lifecycleData.getBossType());
            int recentKillCount = getRecentKillCount(lifecycleData.getBossType());

            int newScore = calculator.calculateDifficultyScore(
                participantCount, playerAveragePower, bossRecommendedPower,
                killCount, recentKillCount
            );

            lifecycleData.setDifficultyScore(newScore);
            lifecycleData.setParticipantCount(participantCount);
            lifecycleData.setAveragePlayerPower(playerAveragePower);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== Boss死亡处理方法 ====================

    /**
     * Boss被击杀时的处理
     *
     * @param bossUUID Boss UUID
     * @param killerUUID 击杀者UUID
     * @param duration 战斗时长 (毫秒)
     * @param deathCount 死亡人数
     * @param isBossWeakened Boss是否被削弱
     * @param isRecentKill 是否是最近刷屏击杀
     * @return 奖励信息
     */
    public synchronized Map<String, Object> onBossKilled(UUID bossUUID,
                                                        UUID killerUUID,
                                                        long duration,
                                                        int deathCount,
                                                        boolean isBossWeakened,
                                                        boolean isRecentKill) {
        BossLifecycleData lifecycleData = bossLifecycleData.remove(bossUUID);
        if (lifecycleData == null) {
            return new HashMap<>();
        }

        try {
            // 记录死亡时间
            lifecycleData.setDeathTime(System.currentTimeMillis());
            lifecycleData.setDuration(duration);

            // 1. 计算品质
            BossQualityRating qualityRating = lifecycleData.getQualityRating();
            if (qualityRating != null) {
                qualityRating.calculateQuality(
                    lifecycleData.getParticipantCount(),
                    lifecycleData.getBossTier().getLevel(),
                    lifecycleData.getDifficultyScore(),
                    duration,
                    deathCount,
                    isBossWeakened,
                    isRecentKill
                );
                lifecycleData.setQuality(qualityRating.getQualityLevel());
            }

            // 2. 获取伤害数据
            Map<UUID, Double> playerDamages = new HashMap<>();
            if (damageManager != null) {
                Set<UUID> participants = damageManager.getParticipants(bossUUID);
                if (participants != null) {
                    for (UUID player : participants) {
                        double damage = damageManager.getPlayerDamage(bossUUID, player);
                        if (damage > 0) {
                            playerDamages.put(player, damage);
                        }
                    }
                }
            }

            // 3. 计算奖励
            BossRewardCalculator rewardCalc = lifecycleData.getRewardCalculator();
            Map<String, Object> rewards = new HashMap<>();

            if (rewardCalc != null && !playerDamages.isEmpty() && qualityRating != null) {
                double qualityMult = qualityRating.getExperienceMultiplier();
                double difficultyMult = lifecycleData.getDifficultyCalculator()
                    .calculateDifficultyMultiplier(
                        lifecycleData.getParticipantCount(),
                        lifecycleData.getAveragePlayerPower(),
                        100.0, // Boss推荐战力(固定值)
                        getHistoricalKillCount(lifecycleData.getBossType()),
                        getRecentKillCount(lifecycleData.getBossType())
                    );

                // 计算总奖励
                double totalExperience = rewardCalc.calculateExperience(qualityMult, difficultyMult);
                double totalSpirits = rewardCalc.calculateSpirits(qualityMult, difficultyMult);

                // 分配奖励
                Map<UUID, Double> experienceRewards = rewardCalc.distributeExperience(
                    totalExperience, playerDamages, killerUUID
                );

                // 保存奖励信息
                rewards.put("quality", qualityRating.getQualityDescription());
                rewards.put("totalExperience", totalExperience);
                rewards.put("totalSpirits", totalSpirits);
                rewards.put("playerRewards", experienceRewards);
                rewards.put("dropMultiplier", qualityRating.getDropMultiplier());
            }

            // 4. 保存历史数据 (用于后续查询)
            rewards.put("lifecycleData", lifecycleData);

            return rewards;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取Boss的生命周期数据
     *
     * @param bossUUID Boss UUID
     * @return 生命周期数据
     */
    public BossLifecycleData getLifecycleData(UUID bossUUID) {
        return bossLifecycleData.get(bossUUID);
    }

    /**
     * 获取Boss的属性倍数信息
     *
     * @param bossUUID Boss UUID
     * @return 属性信息字符串
     */
    public String getAttributeInfo(UUID bossUUID) {
        BossLifecycleData data = bossLifecycleData.get(bossUUID);
        if (data == null || data.getAttributeProgression() == null) {
            return "不存在";
        }

        return data.getAttributeProgression().getAttributeInfo();
    }

    /**
     * 获取Boss的难度信息
     *
     * @param bossUUID Boss UUID
     * @return 难度信息字符串
     */
    public String getDifficultyInfo(UUID bossUUID) {
        BossLifecycleData data = bossLifecycleData.get(bossUUID);
        if (data == null || data.getDifficultyCalculator() == null) {
            return "不存在";
        }

        return data.getDifficultyCalculator().getDifficultyInfo();
    }

    /**
     * 获取历史击杀次数
     *
     * @param bossType Boss类型
     * @return 击杀次数
     */
    private int getHistoricalKillCount(String bossType) {
        if (damageManager == null) {
            return 0;
        }

        try {
            // 查询该Boss类型的所有历史
            return (int) damageManager.getHistory()
                .stream()
                .filter(h -> h.getBossType() != null && h.getBossType().equals(bossType))
                .count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取最近1小时的击杀次数
     *
     * @param bossType Boss类型
     * @return 击杀次数
     */
    private int getRecentKillCount(String bossType) {
        if (damageManager == null) {
            return 0;
        }

        try {
            long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);

            return (int) damageManager.getHistory()
                .stream()
                .filter(h -> h.getBossType() != null && h.getBossType().equals(bossType))
                .filter(h -> h.getRecordTime() > oneHourAgo)
                .count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取活跃的Boss数量
     *
     * @return 数量
     */
    public int getActiveBossCount() {
        return bossLifecycleData.size();
    }

    /**
     * 获取所有活跃的Boss UUID
     *
     * @return UUID列表
     */
    public Set<UUID> getActiveBosses() {
        return new HashSet<>(bossLifecycleData.keySet());
    }

    /**
     * 清除指定Boss的生命周期数据
     *
     * @param bossUUID Boss UUID
     */
    public void removeBoss(UUID bossUUID) {
        bossLifecycleData.remove(bossUUID);
    }

    /**
     * 清除所有Boss的生命周期数据
     */
    public void clearAllBosses() {
        bossLifecycleData.clear();
    }
}
