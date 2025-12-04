package com.xiancore.systems.boss.location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * 位置选择标准和权重配置
 * 定义位置选择的所有标准和权重参数
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
@Setter
public class SelectionCriteria {

    // ==================== 权重配置 ====================

    /** 安全性权重 (0.0-1.0) */
    private double safetyWeight;

    /** 玩家分布权重 */
    private double distributionWeight;

    /** 距离冲突权重 */
    private double distanceWeight;

    /** 随机性权重 */
    private double randomWeight;

    // ==================== 阈值配置 ====================

    /** 最小安全性分数 (0.0-1.0) */
    private double minSafetyScore;

    /** 与出生点的最小距离 (格) */
    private double minDistanceFromSpawn;

    /** 与其他Boss的最小距离 (格) */
    private double minDistanceFromOtherBosses;

    /** 最大玩家密度 (5格内的最大玩家数) */
    private int maxPlayerDensity;

    /** 最小玩家聚集距离 (避免太靠近玩家) */
    private double minDistanceFromPlayers;

    // ==================== 缓存配置 ====================

    /** 缓存是否启用 */
    private boolean cacheEnabled;

    /** 缓存过期时间 (毫秒) */
    private long cacheExpireMillis;

    /** 缓存最大大小 */
    private int maxCacheSize;

    // ==================== 创建时间 ====================

    private long createdTime;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数 - 使用推荐的默认值
     */
    public SelectionCriteria() {
        // 权重: 均匀分布
        this.safetyWeight = 0.30;
        this.distributionWeight = 0.30;
        this.distanceWeight = 0.20;
        this.randomWeight = 0.20;

        // 阈值
        this.minSafetyScore = 0.50;
        this.minDistanceFromSpawn = 50;
        this.minDistanceFromOtherBosses = 100;
        this.maxPlayerDensity = 5;
        this.minDistanceFromPlayers = 30;

        // 缓存
        this.cacheEnabled = true;
        this.cacheExpireMillis = 30000;  // 30秒
        this.maxCacheSize = 100;

        this.createdTime = System.currentTimeMillis();
    }

    /**
     * 自定义权重的构造函数
     */
    public SelectionCriteria(double safety, double distribution, double distance, double random) {
        this();
        this.safetyWeight = safety;
        this.distributionWeight = distribution;
        this.distanceWeight = distance;
        this.randomWeight = random;
    }

    // ==================== 业务方法 ====================

    /**
     * 验证权重配置
     * 权重总和应该为1.0
     *
     * @return 是否有效
     */
    public boolean isWeightValid() {
        double sum = safetyWeight + distributionWeight + distanceWeight + randomWeight;
        return Math.abs(sum - 1.0) < 0.01;  // 允许浮点误差
    }

    /**
     * 规范化权重 (确保总和为1.0)
     */
    public void normalizeWeights() {
        double sum = safetyWeight + distributionWeight + distanceWeight + randomWeight;
        if (sum <= 0) {
            return;
        }

        safetyWeight /= sum;
        distributionWeight /= sum;
        distanceWeight /= sum;
        randomWeight /= sum;
    }

    /**
     * 验证所有阈值是否有效
     */
    public boolean isThresholdValid() {
        return minSafetyScore >= 0.0 && minSafetyScore <= 1.0 &&
               minDistanceFromSpawn >= 0 &&
               minDistanceFromOtherBosses >= 0 &&
               maxPlayerDensity > 0 &&
               minDistanceFromPlayers >= 0;
    }

    /**
     * 获取权重配置字符串
     */
    public String getWeightInfo() {
        return String.format(
            "权重配置: 安全性%.2f%% + 分布%.2f%% + 距离%.2f%% + 随机%.2f%% = %.2f%%",
            safetyWeight * 100,
            distributionWeight * 100,
            distanceWeight * 100,
            randomWeight * 100,
            (safetyWeight + distributionWeight + distanceWeight + randomWeight) * 100
        );
    }

    /**
     * 获取阈值配置字符串
     */
    public String getThresholdInfo() {
        return String.format(
            "阈值配置: 最小安全性%.2f, 与出生点最小距离%.0f, " +
            "与其他Boss最小距离%.0f, 最大玩家密度%d, 与玩家最小距离%.0f",
            minSafetyScore,
            minDistanceFromSpawn,
            minDistanceFromOtherBosses,
            maxPlayerDensity,
            minDistanceFromPlayers
        );
    }

    /**
     * 获取缓存配置字符串
     */
    public String getCacheInfo() {
        return String.format(
            "缓存配置: %s, 过期时间%dms, 最大大小%d",
            cacheEnabled ? "启用" : "禁用",
            cacheExpireMillis,
            maxCacheSize
        );
    }

    /**
     * 克隆标准对象
     */
    @Override
    public SelectionCriteria clone() {
        SelectionCriteria criteria = new SelectionCriteria();
        criteria.safetyWeight = this.safetyWeight;
        criteria.distributionWeight = this.distributionWeight;
        criteria.distanceWeight = this.distanceWeight;
        criteria.randomWeight = this.randomWeight;
        criteria.minSafetyScore = this.minSafetyScore;
        criteria.minDistanceFromSpawn = this.minDistanceFromSpawn;
        criteria.minDistanceFromOtherBosses = this.minDistanceFromOtherBosses;
        criteria.maxPlayerDensity = this.maxPlayerDensity;
        criteria.minDistanceFromPlayers = this.minDistanceFromPlayers;
        criteria.cacheEnabled = this.cacheEnabled;
        criteria.cacheExpireMillis = this.cacheExpireMillis;
        criteria.maxCacheSize = this.maxCacheSize;
        return criteria;
    }

    /**
     * 转换为Map格式
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("safetyWeight", safetyWeight);
        map.put("distributionWeight", distributionWeight);
        map.put("distanceWeight", distanceWeight);
        map.put("randomWeight", randomWeight);
        map.put("minSafetyScore", minSafetyScore);
        map.put("minDistanceFromSpawn", minDistanceFromSpawn);
        map.put("minDistanceFromOtherBosses", minDistanceFromOtherBosses);
        map.put("maxPlayerDensity", maxPlayerDensity);
        map.put("minDistanceFromPlayers", minDistanceFromPlayers);
        map.put("cacheEnabled", cacheEnabled);
        map.put("cacheExpireMillis", cacheExpireMillis);
        return map;
    }

    /**
     * 获取完整的信息
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 位置选择标准 ===\n");
        sb.append(getWeightInfo()).append("\n");
        sb.append(getThresholdInfo()).append("\n");
        sb.append(getCacheInfo()).append("\n");
        return sb.toString();
    }

    /**
     * 预设: BALANCED (平衡模式)
     */
    public static SelectionCriteria createBalanced() {
        return new SelectionCriteria(0.30, 0.30, 0.20, 0.20);
    }

    /**
     * 预设: SAFETY_FIRST (安全优先)
     */
    public static SelectionCriteria createSafetyFirst() {
        SelectionCriteria criteria = new SelectionCriteria(0.50, 0.20, 0.20, 0.10);
        criteria.setMinSafetyScore(0.70);
        return criteria;
    }

    /**
     * 预设: DISTRIBUTED (分布优先)
     */
    public static SelectionCriteria createDistributed() {
        SelectionCriteria criteria = new SelectionCriteria(0.20, 0.50, 0.20, 0.10);
        criteria.setMaxPlayerDensity(3);
        return criteria;
    }

    /**
     * 预设: RANDOM (随机优先)
     */
    public static SelectionCriteria createRandom() {
        SelectionCriteria criteria = new SelectionCriteria(0.10, 0.10, 0.20, 0.60);
        criteria.setMinSafetyScore(0.30);
        return criteria;
    }
}
