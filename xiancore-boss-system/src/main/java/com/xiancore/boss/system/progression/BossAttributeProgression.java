package com.xiancore.boss.system.progression;

import com.xiancore.boss.system.damage.DamageStatisticsManager;
import com.xiancore.boss.system.model.BossTier;
import lombok.Getter;

import java.util.*;

/**
 * Boss属性提升系统
 * 根据击杀历史和玩家战力动态调整Boss属性
 *
 * 职责:
 * - 计算Boss生命值倍数
 * - 计算Boss伤害倍数
 * - 计算Boss速度倍数
 * - 计算Boss防御倍数
 * - 应用玩家战力修饰
 * - 应用击杀历史修饰
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossAttributeProgression {

    // ==================== 配置常量 ====================

    /** 最大生命值倍数 */
    private static final double MAX_HEALTH_MULTIPLIER = 5.0;

    /** 最大伤害倍数 */
    private static final double MAX_DAMAGE_MULTIPLIER = 3.0;

    /** 最大速度倍数 */
    private static final double MAX_SPEED_MULTIPLIER = 2.0;

    /** 最大防御倍数 */
    private static final double MAX_ARMOR_MULTIPLIER = 1.5;

    /** 最大攻击范围倍数 */
    private static final double MAX_ATTACK_RANGE_MULTIPLIER = 1.5;

    /** 最大掉落倍数 */
    private static final double MAX_DROP_MULTIPLIER = 3.0;

    /** 每次击杀的倍数增加 */
    private static final double KILL_COUNT_MULTIPLIER_PER_KILL = 0.05;

    /** 玩家战力修饰上限 */
    private static final double PLAYER_POWER_MULTIPLIER_CAP = 1.5;

    // ==================== 属性倍数 ====================

    /** 生命值倍数 (1.0 - 5.0) */
    private volatile double healthMultiplier = 1.0;

    /** 伤害倍数 (1.0 - 3.0) */
    private volatile double damageMultiplier = 1.0;

    /** 移动速度倍数 (1.0 - 2.0) */
    private volatile double speedMultiplier = 1.0;

    /** 防御倍数 (1.0 - 1.5) */
    private volatile double armorMultiplier = 1.0;

    /** 攻击范围倍数 (1.0 - 1.5) */
    private volatile double attackRangeMultiplier = 1.0;

    /** 掉落倍数 (1.0 - 3.0) */
    private volatile double dropMultiplier = 1.0;

    // ==================== 内部状态 ====================

    /** Boss UUID */
    private final UUID bossUUID;

    /** Boss Tier等级 */
    private final BossTier bossTier;

    /** 伤害统计管理器 (用于查询历史) */
    private final DamageStatisticsManager damageManager;

    /** Boss类型 */
    private final String bossType;

    /** 计算时间戳 */
    private volatile long lastCalculationTime = 0;

    /** 计算结果缓存 */
    private volatile long cacheExpireTime = 1000; // 1秒缓存

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param bossUUID Boss UUID
     * @param bossType Boss类型
     * @param bossTier Boss等级
     * @param damageManager 伤害统计管理器
     */
    public BossAttributeProgression(UUID bossUUID, String bossType, BossTier bossTier,
                                   DamageStatisticsManager damageManager) {
        this.bossUUID = bossUUID;
        this.bossType = bossType;
        this.bossTier = bossTier;
        this.damageManager = damageManager;

        // 初始化为Tier的基础倍数
        initializeBaseTierMultipliers();
    }

    // ==================== 初始化方法 ====================

    /**
     * 初始化Tier等级对应的基础倍数
     */
    private void initializeBaseTierMultipliers() {
        double tierMultiplier = bossTier.getHealthMultiplier();

        this.healthMultiplier = tierMultiplier;
        this.damageMultiplier = tierMultiplier * 0.6;  // 伤害为血量的60%
        this.speedMultiplier = 1.0;
        this.armorMultiplier = tierMultiplier * 0.3;   // 防御为血量的30%
        this.dropMultiplier = tierMultiplier;
    }

    // ==================== 核心计算方法 ====================

    /**
     * 计算完整的属性提升
     *
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @param participantCount 参与人数
     * @return 是否计算成功
     */
    public synchronized boolean calculateProgression(double playerAveragePower,
                                                     double bossRecommendedPower,
                                                     int participantCount) {
        // 检查缓存是否有效
        if (isCacheValid()) {
            return true;
        }

        try {
            // 1. 获取击杀历史
            int killCount = getHistoricalKillCount();

            // 2. 计算击杀次数修饰
            double killCountModifier = calculateKillCountModifier(killCount);

            // 3. 计算玩家战力修饰
            double playerPowerModifier = calculatePlayerPowerModifier(playerAveragePower, bossRecommendedPower);

            // 4. 获取基础倍数 (Tier)
            double baseTierMultiplier = bossTier.getHealthMultiplier();

            // 5. 计算最终倍数
            calculateFinalMultipliers(baseTierMultiplier, killCountModifier, playerPowerModifier);

            // 6. 记录计算时间
            this.lastCalculationTime = System.currentTimeMillis();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 计算击杀次数修饰
     *
     * 公式: 1.0 + min(killCount × 0.05, 1.5)
     * 即: 0次=1.0x, 10次=1.5x, 30次=2.5x (上限)
     *
     * @param killCount 击杀次数
     * @return 修饰倍数
     */
    private double calculateKillCountModifier(int killCount) {
        double modifier = 1.0 + (killCount * KILL_COUNT_MULTIPLIER_PER_KILL);
        return Math.min(modifier, 2.5); // 最多2.5x
    }

    /**
     * 计算玩家战力修饰
     *
     * 公式:
     * 战力差 = (playerPower - recommendedPower) / recommendedPower
     * 如果战力差 < -50%: 2.0x (玩家太弱, Boss更强)
     * 如果战力差在 -50% 到 +50%: 1.0x (平衡)
     * 如果战力差 > +50%: 0.5x (玩家太强, Boss更弱)
     *
     * @param playerAveragePower 玩家平均战力
     * @param bossRecommendedPower Boss推荐战力
     * @return 修饰倍数
     */
    private double calculatePlayerPowerModifier(double playerAveragePower, double bossRecommendedPower) {
        if (bossRecommendedPower <= 0) {
            return 1.0;
        }

        double powerDifference = (playerAveragePower - bossRecommendedPower) / bossRecommendedPower;

        if (powerDifference < -0.5) {
            // 玩家战力低于推荐50% 以上
            return 2.0;
        } else if (powerDifference > 0.5) {
            // 玩家战力高于推荐50% 以上
            return 0.5;
        } else {
            // 线性插值: -0.5到0.5之间
            return 1.0 + (powerDifference);
        }
    }

    /**
     * 计算最终的所有倍数
     */
    private void calculateFinalMultipliers(double baseTierMultiplier,
                                          double killCountModifier,
                                          double playerPowerModifier) {
        // 生命值倍数
        this.healthMultiplier = Math.min(
            baseTierMultiplier * killCountModifier * playerPowerModifier,
            MAX_HEALTH_MULTIPLIER
        );

        // 伤害倍数 (生命值的0.6倍)
        this.damageMultiplier = Math.min(
            this.healthMultiplier * 0.6 / baseTierMultiplier * baseTierMultiplier,
            MAX_DAMAGE_MULTIPLIER
        );

        // 速度倍数 (基础1.0, 最多2.0)
        this.speedMultiplier = Math.min(
            1.0 + ((killCountModifier - 1.0) * 0.5),
            MAX_SPEED_MULTIPLIER
        );

        // 防御倍数 (生命值的0.3倍)
        this.armorMultiplier = Math.min(
            this.healthMultiplier * 0.3 / baseTierMultiplier * baseTierMultiplier,
            MAX_ARMOR_MULTIPLIER
        );

        // 攻击范围倍数 (基础1.0)
        this.attackRangeMultiplier = Math.min(
            1.0 + ((killCountModifier - 1.0) * 0.3),
            MAX_ATTACK_RANGE_MULTIPLIER
        );

        // 掉落倍数 (与生命值相同)
        this.dropMultiplier = Math.min(
            this.healthMultiplier,
            MAX_DROP_MULTIPLIER
        );
    }

    // ==================== 查询方法 ====================

    /**
     * 获取击杀历史
     *
     * @return 击杀次数
     */
    private int getHistoricalKillCount() {
        if (damageManager == null) {
            return 0;
        }

        try {
            // 对于现在，简单地返回0
            // 在实现DamageStatisticsManager后，此方法将查询实际历史
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 应用属性到Boss实体
     *
     * @param health 基础生命值
     * @return 调整后的生命值
     */
    public double applyHealthMultiplier(double health) {
        return health * this.healthMultiplier;
    }

    /**
     * 获取伤害倍数
     *
     * @param baseDamage 基础伤害
     * @return 调整后的伤害
     */
    public double applyDamageMultiplier(double baseDamage) {
        return baseDamage * this.damageMultiplier;
    }

    /**
     * 获取速度倍数
     *
     * @param baseSpeed 基础速度
     * @return 调整后的速度
     */
    public double applySpeedMultiplier(double baseSpeed) {
        return baseSpeed * this.speedMultiplier;
    }

    /**
     * 获取防御倍数
     *
     * @param baseArmor 基础防御
     * @return 调整后的防御
     */
    public double applyArmorMultiplier(double baseArmor) {
        return baseArmor * this.armorMultiplier;
    }

    /**
     * 获取掉落倍数
     *
     * @param baseDrops 基础掉落数量
     * @return 调整后的掉落数量
     */
    public int applyDropMultiplier(int baseDrops) {
        return Math.max(1, (int) (baseDrops * this.dropMultiplier));
    }

    /**
     * 获取属性信息
     *
     * @return 属性信息字符串
     */
    public String getAttributeInfo() {
        return String.format(
            "Health: %.2fx, Damage: %.2fx, Speed: %.2fx, Armor: %.2fx, Drops: %.2fx",
            healthMultiplier,
            damageMultiplier,
            speedMultiplier,
            armorMultiplier,
            dropMultiplier
        );
    }

    /**
     * 获取属性总概览
     *
     * @return 总体难度描述
     */
    public String getDifficultyDescription() {
        double avgMultiplier = (healthMultiplier + damageMultiplier) / 2.0;

        if (avgMultiplier < 1.2) {
            return "简单 (绿色)";
        } else if (avgMultiplier < 1.5) {
            return "普通 (黄色)";
        } else if (avgMultiplier < 2.0) {
            return "困难 (橙色)";
        } else if (avgMultiplier < 3.0) {
            return "地狱 (红色)";
        } else {
            return "绝望 (紫色)";
        }
    }

    // ==================== 缓存管理 ====================

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid() {
        return (System.currentTimeMillis() - lastCalculationTime) < cacheExpireTime;
    }

    /**
     * 设置缓存过期时间
     *
     * @param milliseconds 毫秒
     */
    public void setCacheExpireTime(long milliseconds) {
        this.cacheExpireTime = milliseconds;
    }

    /**
     * 强制失效缓存
     */
    public void invalidateCache() {
        this.lastCalculationTime = 0;
    }

    /**
     * 重置为基础倍数
     */
    public void reset() {
        initializeBaseTierMultipliers();
        invalidateCache();
    }
}
