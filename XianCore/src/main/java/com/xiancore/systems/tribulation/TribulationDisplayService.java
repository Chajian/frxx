package com.xiancore.systems.tribulation;

import org.bukkit.entity.Player;

/**
 * 天劫显示服务
 * 负责天劫GUI相关的计算和显示逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TribulationDisplayService {

    // 评级奖励倍率
    private static final double RATING_S_MULTIPLIER = 2.0;
    private static final double RATING_A_MULTIPLIER = 1.5;
    private static final double RATING_B_MULTIPLIER = 1.2;
    private static final double RATING_C_MULTIPLIER = 1.0;

    // 评级阈值
    private static final double RATING_S_HEALTH_THRESHOLD = 0.8;
    private static final double RATING_A_HEALTH_THRESHOLD = 0.5;
    private static final int RATING_B_DEATH_THRESHOLD = 1;
    private static final int RATING_C_DEATH_THRESHOLD = 2;

    /**
     * 获取预估评级
     */
    public String getEstimatedRating(Tribulation tribulation) {
        if (tribulation.getDeaths() >= RATING_C_DEATH_THRESHOLD) {
            return "C";
        }
        if (tribulation.getDeaths() == RATING_B_DEATH_THRESHOLD) {
            return "B";
        }

        double healthPercent = tribulation.getMinHealth() / 20.0;
        if (healthPercent >= RATING_S_HEALTH_THRESHOLD && tribulation.isPerfect()) {
            return "S";
        } else if (healthPercent >= RATING_A_HEALTH_THRESHOLD) {
            return "A";
        } else {
            return "B";
        }
    }

    /**
     * 获取预估奖励倍率
     */
    public double getEstimatedMultiplier(Tribulation tribulation) {
        String rating = getEstimatedRating(tribulation);
        return switch (rating) {
            case "S" -> RATING_S_MULTIPLIER;
            case "A" -> RATING_A_MULTIPLIER;
            case "B" -> RATING_B_MULTIPLIER;
            case "C" -> RATING_C_MULTIPLIER;
            default -> RATING_C_MULTIPLIER;
        };
    }

    /**
     * 获取血量条显示
     */
    public String getHealthBar(Player player) {
        double health = player.getHealth();
        double maxHealth = 20.0;
        double percent = health / maxHealth;

        int barLength = 10;
        int filled = (int) (barLength * percent);

        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                if (percent > 0.6) {
                    bar.append("§a█");
                } else if (percent > 0.3) {
                    bar.append("§e█");
                } else {
                    bar.append("§c█");
                }
            } else {
                bar.append("§8█");
            }
        }
        bar.append("§7]");

        return bar.toString();
    }

    /**
     * 获取天劫显示信息
     */
    public TribulationDisplayInfo getDisplayInfo(Player player, Tribulation tribulation) {
        String estimatedRating = tribulation.isCompleted() ?
                tribulation.calculateRating() : getEstimatedRating(tribulation);
        double multiplier = tribulation.isCompleted() ?
                tribulation.getRewardMultiplier() : getEstimatedMultiplier(tribulation);

        return new TribulationDisplayInfo(
                tribulation,
                player,
                estimatedRating,
                multiplier,
                getHealthBar(player)
        );
    }

    /**
     * 获取奖励预览信息
     */
    public RewardPreview getRewardPreview(Tribulation tribulation) {
        double diffMultiplier = tribulation.getType().getDifficultyMultiplier();
        int tier = tribulation.getType().getTier();

        long baseExp = (long) (10000 * diffMultiplier);
        int baseStones = 100 * tier;
        int baseSkillPoints = tier;
        int baseActiveQi = 25 + (tier * 5);

        double ratingMultiplier = tribulation.isCompleted() ?
                tribulation.getRewardMultiplier() : getEstimatedMultiplier(tribulation);

        return new RewardPreview(
                baseExp,
                baseStones,
                baseSkillPoints,
                baseActiveQi,
                ratingMultiplier,
                tribulation.isCompleted()
        );
    }

    /**
     * 天劫显示信息
     */
    public static class TribulationDisplayInfo {
        private final Tribulation tribulation;
        private final Player player;
        private final String rating;
        private final double multiplier;
        private final String healthBar;

        public TribulationDisplayInfo(Tribulation tribulation, Player player,
                                      String rating, double multiplier, String healthBar) {
            this.tribulation = tribulation;
            this.player = player;
            this.rating = rating;
            this.multiplier = multiplier;
            this.healthBar = healthBar;
        }

        public Tribulation getTribulation() { return tribulation; }
        public Player getPlayer() { return player; }
        public String getRating() { return rating; }
        public double getMultiplier() { return multiplier; }
        public String getHealthBar() { return healthBar; }

        public boolean isHighRating() {
            return "S".equals(rating) || "A".equals(rating);
        }

        public double getDistanceFromCenter() {
            return tribulation.getLocation().distance(player.getLocation());
        }

        public boolean isInRange() {
            return tribulation.isPlayerInRange(player.getLocation());
        }
    }

    /**
     * 奖励预览信息
     */
    public static class RewardPreview {
        private final long baseExp;
        private final int baseStones;
        private final int baseSkillPoints;
        private final int baseActiveQi;
        private final double multiplier;
        private final boolean isFinal;

        public RewardPreview(long baseExp, int baseStones, int baseSkillPoints,
                             int baseActiveQi, double multiplier, boolean isFinal) {
            this.baseExp = baseExp;
            this.baseStones = baseStones;
            this.baseSkillPoints = baseSkillPoints;
            this.baseActiveQi = baseActiveQi;
            this.multiplier = multiplier;
            this.isFinal = isFinal;
        }

        public long getBaseExp() { return baseExp; }
        public long getFinalExp() { return (long) (baseExp * multiplier); }
        public int getBaseStones() { return baseStones; }
        public int getBaseSkillPoints() { return baseSkillPoints; }
        public int getBaseActiveQi() { return baseActiveQi; }
        public double getMultiplier() { return multiplier; }
        public boolean isFinal() { return isFinal; }
    }
}
