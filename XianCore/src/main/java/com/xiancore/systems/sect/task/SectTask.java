package com.xiancore.systems.sect.task;

import lombok.Data;

import java.util.UUID;

/**
 * 宗门任务数据类
 * 存储单个任务的完整信息
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class SectTask {

    // ==================== 基础信息 ====================

    /**
     * 任务ID（唯一标识）
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务类型（日常/周常/特殊）
     */
    private SectTaskType type;

    // ==================== 任务目标 ====================

    /**
     * 任务目标类型
     */
    private TaskObjective objective;

    /**
     * 目标对象（如怪物名称、物品ID）
     */
    private String target;

    /**
     * 目标数量
     */
    private int targetAmount;

    /**
     * 当前进度
     */
    private int currentProgress;

    // ==================== 奖励配置 ====================

    /**
     * 贡献点奖励
     */
    private int contributionReward;

    /**
     * 活跃度奖励
     */
    private int activityReward;

    /**
     * 灵石奖励
     */
    private int spiritStoneReward;

    /**
     * 宗门经验奖励
     */
    private int sectExpReward;

    // ==================== 时间管理 ====================

    /**
     * 任务创建时间
     */
    private long createdTime;

    /**
     * 任务刷新时间（下次刷新的时间戳）
     */
    private long refreshTime;

    /**
     * 任务过期时间
     */
    private long expireTime;

    // ==================== 状态管理 ====================

    /**
     * 任务所属玩家（null表示公共任务）
     */
    private UUID ownerId;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 任务是否已领取奖励
     */
    private boolean rewardClaimed;

    /**
     * 构造函数
     */
    public SectTask() {
        this.taskId = UUID.randomUUID().toString();
        this.createdTime = System.currentTimeMillis();
        this.currentProgress = 0;
        this.status = TaskStatus.AVAILABLE;
        this.rewardClaimed = false;
    }

    /**
     * 构造函数（带参数）
     */
    public SectTask(String name, SectTaskType type, TaskObjective objective, String target, int targetAmount) {
        this();
        this.name = name;
        this.type = type;
        this.objective = objective;
        this.target = target;
        this.targetAmount = targetAmount;
        this.description = objective.formatDescription(target, targetAmount);
    }

    // ==================== 业务方法 ====================

    /**
     * 增加任务进度
     *
     * @param amount 增加的数量
     * @return 是否完成任务
     */
    public boolean addProgress(int amount) {
        if (status == TaskStatus.COMPLETED) {
            return false;
        }

        currentProgress += amount;

        if (currentProgress >= targetAmount) {
            currentProgress = targetAmount;
            status = TaskStatus.COMPLETED;
            return true;
        }

        return false;
    }

    /**
     * 设置任务进度
     *
     * @param progress 新的进度值
     */
    public void setProgress(int progress) {
        this.currentProgress = Math.min(progress, targetAmount);

        if (currentProgress >= targetAmount) {
            status = TaskStatus.COMPLETED;
        }
    }

    /**
     * 检查任务是否完成
     */
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED || currentProgress >= targetAmount;
    }

    /**
     * 检查任务是否过期
     */
    public boolean isExpired() {
        return expireTime > 0 && System.currentTimeMillis() > expireTime;
    }

    /**
     * 获取进度百分比
     */
    public double getProgressPercentage() {
        if (targetAmount == 0) {
            return 0;
        }
        return (double) currentProgress / targetAmount * 100;
    }

    /**
     * 获取进度文本（如 "12/20"）
     */
    public String getProgressText() {
        return currentProgress + "/" + targetAmount;
    }

    /**
     * 重置任务
     */
    public void reset() {
        this.currentProgress = 0;
        this.status = TaskStatus.AVAILABLE;
        this.rewardClaimed = false;
        this.createdTime = System.currentTimeMillis();
    }

    /**
     * 领取奖励
     */
    public void claimReward() {
        if (isCompleted() && !rewardClaimed) {
            rewardClaimed = true;
        }
    }

    /**
     * 克隆任务（用于分配给多个玩家）
     */
    public SectTask clone(UUID ownerId) {
        SectTask cloned = new SectTask();
        cloned.setTaskId(UUID.randomUUID().toString());
        cloned.setName(this.name);
        cloned.setDescription(this.description);
        cloned.setType(this.type);
        cloned.setObjective(this.objective);
        cloned.setTarget(this.target);
        cloned.setTargetAmount(this.targetAmount);
        cloned.setContributionReward(this.contributionReward);
        cloned.setActivityReward(this.activityReward);
        cloned.setSpiritStoneReward(this.spiritStoneReward);
        cloned.setSectExpReward(this.sectExpReward);
        cloned.setRefreshTime(this.refreshTime);
        cloned.setExpireTime(this.expireTime);
        cloned.setOwnerId(ownerId);
        return cloned;
    }

    // ==================== 内部枚举 ====================

    /**
     * 任务状态
     */
    public enum TaskStatus {
        /**
         * 可接取
         */
        AVAILABLE("§a可接取"),

        /**
         * 进行中
         */
        IN_PROGRESS("§e进行中"),

        /**
         * 已完成
         */
        COMPLETED("§2已完成"),

        /**
         * 已过期
         */
        EXPIRED("§7已过期"),

        /**
         * 已放弃
         */
        ABANDONED("§c已放弃");

        private final String displayName;

        TaskStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public String getTaskId() {
        return taskId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SectTaskType getType() {
        return type;
    }

    public TaskObjective getObjective() {
        return objective;
    }

    public String getTarget() {
        return target;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public int getContributionReward() {
        return contributionReward;
    }

    public int getActivityReward() {
        return activityReward;
    }

    public int getSpiritStoneReward() {
        return spiritStoneReward;
    }

    public int getSectExpReward() {
        return sectExpReward;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getRefreshTime() {
        return refreshTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(SectTaskType type) {
        this.type = type;
    }

    public void setObjective(TaskObjective objective) {
        this.objective = objective;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public void setContributionReward(int contributionReward) {
        this.contributionReward = contributionReward;
    }

    public void setActivityReward(int activityReward) {
        this.activityReward = activityReward;
    }

    public void setSpiritStoneReward(int spiritStoneReward) {
        this.spiritStoneReward = spiritStoneReward;
    }

    public void setSectExpReward(int sectExpReward) {
        this.sectExpReward = sectExpReward;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public void setRefreshTime(long refreshTime) {
        this.refreshTime = refreshTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setRewardClaimed(boolean rewardClaimed) {
        this.rewardClaimed = rewardClaimed;
    }
}
