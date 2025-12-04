package com.xiancore.systems.sect;

import lombok.Data;

import java.util.UUID;

/**
 * 宗门成员数据类
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class SectMember {

    // 基本信息
    private UUID playerId;           // 玩家UUID
    private String playerName;       // 玩家名称
    private SectRank rank;           // 职位

    // 贡献相关
    private int contribution = 0;    // 宗门贡献值
    private int weeklyContribution = 0;  // 本周贡献值

    // 时间相关
    private long joinedAt;           // 加入时间
    private long lastActiveAt;       // 最后活跃时间

    // 统计相关
    private int tasksCompleted = 0;  // 完成任务数
    private int donationCount = 0;   // 捐献次数

    /**
     * 构造函数
     */
    public SectMember(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.rank = SectRank.OUTER_DISCIPLE;  // 默认外门弟子
        this.joinedAt = System.currentTimeMillis();
        this.lastActiveAt = joinedAt;
    }

    /**
     * 增加贡献值
     */
    public void addContribution(int amount) {
        this.contribution += amount;
        this.weeklyContribution += amount;
        this.lastActiveAt = System.currentTimeMillis();
    }

    /**
     * 重置周贡献
     */
    public void resetWeeklyContribution() {
        this.weeklyContribution = 0;
    }

    /**
     * 完成任务
     */
    public void completeTask() {
        this.tasksCompleted++;
        this.lastActiveAt = System.currentTimeMillis();
    }

    /**
     * 记录捐献
     */
    public void recordDonation() {
        this.donationCount++;
        this.lastActiveAt = System.currentTimeMillis();
    }

    /**
     * 更新活跃时间
     */
    public void updateActivity() {
        this.lastActiveAt = System.currentTimeMillis();
    }

    /**
     * 获取在宗门天数
     */
    public long getDaysInSect() {
        long diff = System.currentTimeMillis() - joinedAt;
        return diff / (1000 * 60 * 60 * 24);
    }

    /**
     * 获取离线天数
     */
    public long getDaysOffline() {
        long diff = System.currentTimeMillis() - lastActiveAt;
        return diff / (1000 * 60 * 60 * 24);
    }

    /**
     * 检查是否长期离线
     */
    public boolean isLongTimeOffline() {
        return getDaysOffline() > 30; // 超过30天
    }

    // ==================== 显式 Getter 方法 ====================

    public SectRank getRank() {
        return rank;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getContribution() {
        return contribution;
    }

    public int getWeeklyContribution() {
        return weeklyContribution;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public int getTasksCompleted() {
        return tasksCompleted;
    }

    public int getDonationCount() {
        return donationCount;
    }

    // ==================== 显式 Setter 方法 ====================

    public void setRank(SectRank rank) {
        this.rank = rank;
    }

    public void setContribution(int contribution) {
        this.contribution = contribution;
    }

    public void setWeeklyContribution(int weeklyContribution) {
        this.weeklyContribution = weeklyContribution;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setTasksCompleted(int tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }

    public void setDonationCount(int donationCount) {
        this.donationCount = donationCount;
    }
}
