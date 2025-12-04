package com.xiancore.systems.sect.task;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务刷新记录
 * 记录任务刷新历史，防止重复刷新和遗漏刷新
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class RefreshRecord {

    // ==================== 刷新历史 ====================

    /**
     * 最后日常刷新日期
     */
    private LocalDate lastDailyRefresh;

    /**
     * 最后周常刷新日期
     */
    private LocalDate lastWeeklyRefresh;

    /**
     * 最后日常刷新精确时间
     */
    private LocalDateTime lastDailyRefreshTime;

    /**
     * 最后周常刷新精确时间
     */
    private LocalDateTime lastWeeklyRefreshTime;

    // ==================== 统计信息 ====================

    /**
     * 日常刷新总次数
     */
    private int dailyRefreshCount = 0;

    /**
     * 周常刷新总次数
     */
    private int weeklyRefreshCount = 0;

    /**
     * 总刷新玩家数
     */
    private int totalPlayersRefreshed = 0;

    // ==================== 错误记录 ====================

    /**
     * 错误记录列表（最多保留20条）
     */
    private List<RefreshError> errors = new ArrayList<>();

    /**
     * 获取最后日常刷新日期
     */
    public LocalDate getLastDailyRefresh() {
        return lastDailyRefresh;
    }

    /**
     * 获取最后周常刷新日期
     */
    public LocalDate getLastWeeklyRefresh() {
        return lastWeeklyRefresh;
    }

    /**
     * 记录刷新
     *
     * @param type        任务类型
     * @param playerCount 刷新的玩家数量
     */
    public void recordRefresh(SectTaskType type, int playerCount) {
        LocalDateTime now = LocalDateTime.now();

        switch (type) {
            case DAILY -> {
                lastDailyRefresh = now.toLocalDate();
                lastDailyRefreshTime = now;
                dailyRefreshCount++;
            }
            case WEEKLY -> {
                lastWeeklyRefresh = now.toLocalDate();
                lastWeeklyRefreshTime = now;
                weeklyRefreshCount++;
            }
        }

        totalPlayersRefreshed += playerCount;
    }

    /**
     * 记录错误
     *
     * @param type    任务类型
     * @param message 错误消息
     */
    public void recordError(SectTaskType type, String message) {
        RefreshError error = new RefreshError();
        error.setTimestamp(LocalDateTime.now());
        error.setType(type);
        error.setErrorMessage(message);
        error.setAffectedPlayers(0);

        errors.add(error);

        // 只保留最近20条错误
        if (errors.size() > 20) {
            errors.remove(0);
        }
    }

    /**
     * 检查是否需要刷新日常任务
     *
     * @return 是否需要刷新
     */
    public boolean needsDailyRefresh() {
        LocalDate today = LocalDate.now();
        return lastDailyRefresh == null || lastDailyRefresh.isBefore(today);
    }

    /**
     * 检查是否需要刷新周常任务
     *
     * @param weeklyRefreshDay 配置的周常刷新星期
     * @return 是否需要刷新
     */
    public boolean needsWeeklyRefresh(DayOfWeek weeklyRefreshDay) {
        if (lastWeeklyRefresh == null) {
            return true;
        }

        LocalDate today = LocalDate.now();

        // 计算上次应该刷新的日期（上个刷新日）
        LocalDate lastExpectedRefresh = today
                .with(TemporalAdjusters.previousOrSame(weeklyRefreshDay));

        // 如果上次实际刷新日期早于上次应该刷新的日期，说明需要刷新
        return lastWeeklyRefresh.isBefore(lastExpectedRefresh);
    }

    /**
     * 获取最近的错误（最多5条）
     *
     * @return 最近的错误列表
     */
    public List<RefreshError> getRecentErrors() {
        int start = Math.max(0, errors.size() - 5);
        return new ArrayList<>(errors.subList(start, errors.size()));
    }

    /**
     * 清理旧错误记录
     */
    public void cleanOldErrors() {
        if (errors.size() > 20) {
            errors = new ArrayList<>(errors.subList(errors.size() - 20, errors.size()));
        }
    }

    // ==================== 内部类 ====================

    /**
     * 刷新错误记录
     */
    @Data
    public static class RefreshError {
        private LocalDateTime timestamp;
        private SectTaskType type;
        private String errorMessage;
        private int affectedPlayers;

        /**
         * 设置时间戳
         */
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        /**
         * 设置任务类型
         */
        public void setType(SectTaskType type) {
            this.type = type;
        }

        /**
         * 设置错误消息
         */
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        /**
         * 设置受影响的玩家数
         */
        public void setAffectedPlayers(int affectedPlayers) {
            this.affectedPlayers = affectedPlayers;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s (影响 %d 玩家)",
                    timestamp, type.getDisplayName(), errorMessage, affectedPlayers);
        }
    }
}







