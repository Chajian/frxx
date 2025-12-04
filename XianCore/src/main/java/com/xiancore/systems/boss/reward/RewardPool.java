package com.xiancore.systems.boss.reward;

import java.util.ArrayList;
import java.util.List;

/**
 * 奖励池 - 存储某个排名/等级的所有奖励
 * 
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
public class RewardPool {
    
    private final String poolName;
    private final List<Reward> rewards;
    private final double rankMultiplier; // 排名倍率
    
    public RewardPool(String poolName, double rankMultiplier) {
        this.poolName = poolName;
        this.rankMultiplier = rankMultiplier;
        this.rewards = new ArrayList<>();
    }
    
    public String getPoolName() {
        return poolName;
    }
    
    public double getRankMultiplier() {
        return rankMultiplier;
    }
    
    public List<Reward> getRewards() {
        return new ArrayList<>(rewards);
    }
    
    /**
     * 添加奖励到池中
     */
    public void addReward(Reward reward) {
        rewards.add(reward);
    }
    
    /**
     * 批量添加奖励
     */
    public void addRewards(List<Reward> rewards) {
        this.rewards.addAll(rewards);
    }
    
    /**
     * 获取应该给予的奖励（基于概率筛选）
     */
    public List<Reward> getRolledRewards() {
        List<Reward> result = new ArrayList<>();
        for (Reward reward : rewards) {
            if (reward.shouldGive()) {
                result.add(reward);
            }
        }
        return result;
    }
    
    /**
     * 获取缩放后的奖励（应用排名倍率）
     */
    public List<Reward> getScaledRewards(double damagePercent) {
        List<Reward> result = new ArrayList<>();
        double finalMultiplier = rankMultiplier * (0.5 + damagePercent);
        
        for (Reward reward : getRolledRewards()) {
            Reward scaledReward = scaleReward(reward, finalMultiplier);
            result.add(scaledReward);
        }
        
        return result;
    }
    
    /**
     * 缩放单个奖励
     */
    private Reward scaleReward(Reward reward, double multiplier) {
        Object scaledValue;
        
        switch (reward.getType()) {
            case EXPERIENCE:
                scaledValue = (int) (reward.getIntValue() * multiplier);
                break;
            case MONEY:
                scaledValue = reward.getDoubleValue() * multiplier;
                break;
            default:
                // 物品、命令等不需要缩放
                scaledValue = reward.getValue();
        }
        
        return new Reward(
            reward.getType(),
            scaledValue,
            reward.getChance(),
            reward.getDisplayName()
        );
    }
    
    /**
     * 奖励池是否为空
     */
    public boolean isEmpty() {
        return rewards.isEmpty();
    }
    
    /**
     * 奖励数量
     */
    public int size() {
        return rewards.size();
    }
}
