package com.xiancore.systems.fate;

import com.xiancore.systems.fate.rewards.FateReward;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 奇遇奖励池
 * 存储一个奇遇类型的所有奖励
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class FateRewardPool {

    /**
     * 权重（用于奇遇类型概率计算）
     */
    private double weight;

    /**
     * 是否全服广播
     */
    private boolean broadcast;

    /**
     * 奖励列表
     */
    private List<FateReward> rewards;

    public FateRewardPool() {
        this.rewards = new ArrayList<>();
        this.weight = 1.0;
        this.broadcast = false;
    }

    /**
     * 添加奖励
     */
    public void addReward(FateReward reward) {
        if (reward != null) {
            rewards.add(reward);
        }
    }

    /**
     * 获取奖励数量
     */
    public int getRewardCount() {
        return rewards.size();
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public double getWeight() {
        return weight;
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public List<FateReward> getRewards() {
        return rewards;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public void setRewards(List<FateReward> rewards) {
        this.rewards = rewards;
    }
}



