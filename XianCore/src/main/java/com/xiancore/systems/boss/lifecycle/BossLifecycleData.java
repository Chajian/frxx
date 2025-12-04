package com.xiancore.systems.boss.lifecycle;

import com.xiancore.boss.system.model.BossTier;
import com.xiancore.boss.system.difficulty.BossDifficultyCalculator;
import com.xiancore.boss.system.reward.BossRewardCalculator;
import com.xiancore.boss.system.quality.BossQualityRating;
import com.xiancore.boss.system.progression.BossAttributeProgression;
import com.xiancore.boss.system.damage.DamageRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Boss生命周期数据
 * 存储Boss从生成到击杀的完整生命周期信息
 *
 * 包含:
 * - Boss基本信息
 * - 属性提升数据
 * - 难度计算数据
 * - 品质评级数据
 * - 奖励计算数据
 * - 战斗统计数据
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
@Setter
public class BossLifecycleData {

    // ==================== Boss基本信息 ====================

    /** Boss UUID */
    private UUID bossUUID;

    /** Boss类型 (如 "SkeletonKing") */
    private String bossType;

    /** Boss等级 */
    private BossTier bossTier;

    // ==================== 参与者信息 ====================

    /** 参与人数 */
    private int participantCount = 0;

    /** 玩家平均战力 */
    private double averagePlayerPower = 0;

    /** 参与玩家UUID列表 */
    private Set<UUID> participants = new HashSet<>();

    // ==================== 属性提升 ====================

    /** 属性提升计算器 */
    private BossAttributeProgression attributeProgression;

    /** 生命值倍数 */
    private volatile double healthMultiplier = 1.0;

    /** 伤害倍数 */
    private volatile double damageMultiplier = 1.0;

    /** 速度倍数 */
    private volatile double speedMultiplier = 1.0;

    /** 防御倍数 */
    private volatile double armorMultiplier = 1.0;

    // ==================== 难度信息 ====================

    /** 难度计算器 */
    private BossDifficultyCalculator difficultyCalculator;

    /** 难度分数 (0-100) */
    private volatile int difficultyScore = 50;

    /** 难度等级 */
    private volatile BossDifficultyCalculator.DifficultyLevel difficultyLevel
        = BossDifficultyCalculator.DifficultyLevel.NORMAL;

    // ==================== 品质评级 ====================

    /** 品质评级器 */
    private BossQualityRating qualityRating;

    /** 最终品质等级 */
    private volatile BossQualityRating.QualityLevel quality
        = BossQualityRating.QualityLevel.B;

    // ==================== 奖励计算 ====================

    /** 奖励计算器 */
    private BossRewardCalculator rewardCalculator;

    /** 基础经验值 */
    private volatile double baseExperience = 0;

    /** 基础精魄值 */
    private volatile double baseSpirits = 0;

    // ==================== 时间信息 ====================

    /** 生成时间戳 */
    private long spawnTime = 0;

    /** 死亡时间戳 */
    private long deathTime = 0;

    /** 战斗时长 (毫秒) */
    private long duration = 0;

    /** 记录创建时间 */
    private long recordTime = System.currentTimeMillis();

    // ==================== 击杀历史 ====================

    /** 总击杀次数 */
    private int totalKillCount = 0;

    /** 最近1小时击杀次数 */
    private int recentKillCount = 0;

    /** 上次击杀时间 */
    private long lastKillTime = 0;

    // ==================== 战斗统计 ====================

    /** 死亡人数 */
    private int deathCount = 0;

    /** 是否有玩家受伤 */
    private boolean hasPlayerDamaged = false;

    /** Boss是否被削弱 */
    private boolean bossWeakened = false;

    // ==================== 元数据 ====================

    /** 自定义元数据 */
    private Map<String, Object> metadata = new HashMap<>();

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public BossLifecycleData() {
    }

    /**
     * 带参数的构造函数
     */
    public BossLifecycleData(UUID bossUUID, String bossType, BossTier bossTier) {
        this.bossUUID = bossUUID;
        this.bossType = bossType;
        this.bossTier = bossTier;
    }

    // ==================== 业务方法 ====================

    /**
     * 获取战斗时长(秒)
     */
    public long getDurationSeconds() {
        return duration / 1000;
    }

    /**
     * 获取难度描述
     */
    public String getDifficultyDescription() {
        if (attributeProgression != null) {
            return attributeProgression.getDifficultyDescription();
        }
        return "未知";
    }

    /**
     * 获取品质描述
     */
    public String getQualityDescription() {
        if (qualityRating != null) {
            return qualityRating.getQualityDescription();
        }
        return "未评级";
    }

    /**
     * 获取完整的生命周期信息
     */
    public String getCompleteInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Boss生命周期信息 ===\n");
        sb.append(String.format("Boss: %s (等级%d)\n", bossType, bossTier.getTier()));
        sb.append(String.format("参与人数: %d, 平均战力: %.0f\n", participantCount, averagePlayerPower));
        sb.append(String.format("属性倍数: 血%.1fx 伤%.1fx 防%.1fx\n",
            healthMultiplier, damageMultiplier, armorMultiplier));
        sb.append(String.format("难度分数: %d/100 (%s)\n", difficultyScore,
            difficultyLevel != null ? difficultyLevel.name : "未知"));
        sb.append(String.format("品质等级: %s\n", getQualityDescription()));
        if (deathTime > 0) {
            sb.append(String.format("战斗时长: %d秒, 死亡: %d人\n", getDurationSeconds(), deathCount));
        }
        return sb.toString();
    }

    /**
     * 添加元数据
     */
    public void putMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }

    /**
     * 清除所有数据
     */
    public void clear() {
        bossUUID = null;
        bossType = null;
        bossTier = null;
        participants.clear();
        metadata.clear();

        attributeProgression = null;
        difficultyCalculator = null;
        qualityRating = null;
        rewardCalculator = null;
    }

    /**
     * 转换为Map格式 (用于序列化)
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        // 基本信息
        map.put("bossUUID", bossUUID != null ? bossUUID.toString() : null);
        map.put("bossType", bossType);
        map.put("bossTier", bossTier != null ? bossTier.name() : null);

        // 参与者信息
        map.put("participantCount", participantCount);
        map.put("averagePlayerPower", averagePlayerPower);

        // 属性倍数
        map.put("healthMultiplier", healthMultiplier);
        map.put("damageMultiplier", damageMultiplier);
        map.put("speedMultiplier", speedMultiplier);
        map.put("armorMultiplier", armorMultiplier);

        // 难度和品质
        map.put("difficultyScore", difficultyScore);
        map.put("quality", quality != null ? quality.name() : null);

        // 时间信息
        map.put("spawnTime", spawnTime);
        map.put("deathTime", deathTime);
        map.put("duration", duration);

        // 统计信息
        map.put("deathCount", deathCount);
        map.put("bossWeakened", bossWeakened);

        return map;
    }
}
