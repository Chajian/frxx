package com.xiancore.systems.cultivation;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 修炼服务
 * 负责修炼相关的计算逻辑，与 GUI 分离
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class CultivationService {

    private final XianCore plugin;

    // 境界基础修为需求
    private static final Map<String, Long> REALM_BASE_QI = Map.of(
            "炼气期", 1000L,
            "筑基期", 5000L,
            "结丹期", 50000L,
            "元婴期", 500000L,
            "化神期", 5000000L,
            "炼虚期", 50000000L,
            "合体期", 500000000L,
            "大乘期", 5000000000L
    );

    // 境界难度系数
    private static final Map<String, Double> REALM_DIFFICULTY = Map.of(
            "炼气期", 1.0,
            "筑基期", 2.0,
            "结丹期", 5.0,
            "元婴期", 10.0,
            "化神期", 20.0,
            "炼虚期", 40.0,
            "合体期", 80.0,
            "大乘期", 160.0
    );

    // 活跃灵气等级阈值
    private static final int ACTIVE_QI_VERY_HIGH = 80;
    private static final int ACTIVE_QI_HIGH = 60;
    private static final int ACTIVE_QI_MEDIUM = 40;
    private static final int ACTIVE_QI_LOW = 20;

    // 突破公式参数
    private static final double BREAKTHROUGH_ALPHA = 1.5;
    private static final double DEFAULT_ENVIRONMENT_QI = 0.5;
    private static final double DEFAULT_RESOURCE_INVESTMENT = 0.5;

    // 小境界倍率系数
    private static final double STAGE_MULTIPLIER = 1.5;

    public CultivationService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取突破所需修为
     */
    public long getRequiredQi(PlayerData data) {
        String realm = data.getRealm();
        int stage = data.getRealmStage();

        long baseQi = REALM_BASE_QI.getOrDefault(realm, 1000L);
        return (long) (baseQi * Math.pow(STAGE_MULTIPLIER, stage - 1));
    }

    /**
     * 获取境界难度系数
     */
    public double getRealmDifficulty(String realm) {
        return REALM_DIFFICULTY.getOrDefault(realm, 1.0);
    }

    /**
     * 计算突破成功率
     */
    public double calculateBreakthroughChance(PlayerData data) {
        double L = data.getSpiritualRoot();         // 灵根值
        double P = data.getTechniqueAdaptation();   // 功法适配
        double E = DEFAULT_ENVIRONMENT_QI;          // 环境灵气
        double S = DEFAULT_RESOURCE_INVESTMENT;     // 资源投入
        double G = data.getComprehension();         // 悟性
        double D = getRealmDifficulty(data.getRealm());

        return 1 - Math.exp(-BREAKTHROUGH_ALPHA * L * P * E * S * G / D);
    }

    /**
     * 计算突破成功率（带自定义环境和资源参数）
     */
    public double calculateBreakthroughChance(PlayerData data, double environmentQi, double resourceInvestment) {
        double L = data.getSpiritualRoot();
        double P = data.getTechniqueAdaptation();
        double G = data.getComprehension();
        double D = getRealmDifficulty(data.getRealm());

        return 1 - Math.exp(-BREAKTHROUGH_ALPHA * L * P * environmentQi * resourceInvestment * G / D);
    }

    /**
     * 检查是否可以突破
     */
    public boolean canBreakthrough(PlayerData data) {
        return data.getQi() >= getRequiredQi(data);
    }

    /**
     * 获取修为差额（还需要多少修为才能突破）
     */
    public long getQiDeficit(PlayerData data) {
        long required = getRequiredQi(data);
        long current = data.getQi();
        return Math.max(0, required - current);
    }

    /**
     * 计算修炼进度（百分比）
     */
    public double calculateProgress(PlayerData data) {
        long currentQi = data.getQi();
        long requiredQi = getRequiredQi(data);
        return Math.min(100.0, (double) currentQi / requiredQi * 100);
    }

    /**
     * 获取活跃灵气状态描述
     */
    public String getActiveQiStatus(long activeQi) {
        if (activeQi >= ACTIVE_QI_VERY_HIGH) {
            return "§a§l极其活跃";
        } else if (activeQi >= ACTIVE_QI_HIGH) {
            return "§2§l非常活跃";
        } else if (activeQi >= ACTIVE_QI_MEDIUM) {
            return "§e§l较为活跃";
        } else if (activeQi >= ACTIVE_QI_LOW) {
            return "§6§l一般";
        } else {
            return "§7§l不活跃";
        }
    }

    /**
     * 获取活跃灵气等级（0-4）
     */
    public int getActiveQiLevel(long activeQi) {
        if (activeQi >= ACTIVE_QI_VERY_HIGH) return 4;
        if (activeQi >= ACTIVE_QI_HIGH) return 3;
        if (activeQi >= ACTIVE_QI_MEDIUM) return 2;
        if (activeQi >= ACTIVE_QI_LOW) return 1;
        return 0;
    }

    /**
     * 格式化修为数值
     */
    public String formatQi(long qi) {
        if (qi >= 1_000_000_000) {
            return String.format("%.1f亿", qi / 1_000_000_000.0);
        } else if (qi >= 10_000) {
            return String.format("%.1f万", qi / 10_000.0);
        } else {
            return String.valueOf(qi);
        }
    }

    /**
     * 格式化百分比
     */
    public String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    /**
     * 获取修炼速率（每分钟修为增长）
     */
    public long getQiGainPerMinute(Player player, PlayerData data) {
        return plugin.getCultivationSystem().calculateQiGainPerMinute(player, data);
    }

    /**
     * 检查玩家是否在修炼中
     */
    public boolean isCultivating(Player player, PlayerData data) {
        return plugin.getCultivationSystem().isCultivating(player.getUniqueId()) || data.isCultivating();
    }

    /**
     * 获取突破信息
     */
    public BreakthroughInfo getBreakthroughInfo(PlayerData data) {
        long currentQi = data.getQi();
        long requiredQi = getRequiredQi(data);
        boolean canBreakthrough = currentQi >= requiredQi;
        double successRate = calculateBreakthroughChance(data);
        long deficit = Math.max(0, requiredQi - currentQi);

        return new BreakthroughInfo(
                canBreakthrough,
                currentQi,
                requiredQi,
                deficit,
                successRate,
                data.getRealm(),
                data.getRealmStage()
        );
    }

    /**
     * 获取成功率详情
     */
    public SuccessRateDetails getSuccessRateDetails(PlayerData data) {
        double spiritualRoot = data.getSpiritualRoot();
        double comprehension = data.getComprehension();
        double techniqueAdaptation = data.getTechniqueAdaptation();
        double realmDifficulty = getRealmDifficulty(data.getRealm());
        double successRate = calculateBreakthroughChance(data);

        return new SuccessRateDetails(
                spiritualRoot,
                comprehension,
                techniqueAdaptation,
                realmDifficulty,
                DEFAULT_ENVIRONMENT_QI,
                DEFAULT_RESOURCE_INVESTMENT,
                successRate
        );
    }

    /**
     * 突破信息
     */
    public static class BreakthroughInfo {
        private final boolean canBreakthrough;
        private final long currentQi;
        private final long requiredQi;
        private final long deficit;
        private final double successRate;
        private final String realm;
        private final int stage;

        public BreakthroughInfo(boolean canBreakthrough, long currentQi, long requiredQi,
                                long deficit, double successRate, String realm, int stage) {
            this.canBreakthrough = canBreakthrough;
            this.currentQi = currentQi;
            this.requiredQi = requiredQi;
            this.deficit = deficit;
            this.successRate = successRate;
            this.realm = realm;
            this.stage = stage;
        }

        public boolean canBreakthrough() { return canBreakthrough; }
        public long getCurrentQi() { return currentQi; }
        public long getRequiredQi() { return requiredQi; }
        public long getDeficit() { return deficit; }
        public double getSuccessRate() { return successRate; }
        public String getRealm() { return realm; }
        public int getStage() { return stage; }
    }

    /**
     * 成功率详情
     */
    public static class SuccessRateDetails {
        private final double spiritualRoot;
        private final double comprehension;
        private final double techniqueAdaptation;
        private final double realmDifficulty;
        private final double environmentQi;
        private final double resourceInvestment;
        private final double finalRate;

        public SuccessRateDetails(double spiritualRoot, double comprehension, double techniqueAdaptation,
                                  double realmDifficulty, double environmentQi, double resourceInvestment,
                                  double finalRate) {
            this.spiritualRoot = spiritualRoot;
            this.comprehension = comprehension;
            this.techniqueAdaptation = techniqueAdaptation;
            this.realmDifficulty = realmDifficulty;
            this.environmentQi = environmentQi;
            this.resourceInvestment = resourceInvestment;
            this.finalRate = finalRate;
        }

        public double getSpiritualRoot() { return spiritualRoot; }
        public double getComprehension() { return comprehension; }
        public double getTechniqueAdaptation() { return techniqueAdaptation; }
        public double getRealmDifficulty() { return realmDifficulty; }
        public double getEnvironmentQi() { return environmentQi; }
        public double getResourceInvestment() { return resourceInvestment; }
        public double getFinalRate() { return finalRate; }
    }
}
