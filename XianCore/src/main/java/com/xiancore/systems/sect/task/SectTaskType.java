package com.xiancore.systems.sect.task;

/**
 * 宗门任务类型
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public enum SectTaskType {

    /**
     * 日常任务 - 每天刷新
     */
    DAILY("日常任务", "§a", 24 * 60 * 60 * 1000L),

    /**
     * 周常任务 - 每周刷新
     */
    WEEKLY("周常任务", "§b", 7 * 24 * 60 * 60 * 1000L),

    /**
     * 特殊任务 - 限时活动
     */
    SPECIAL("特殊任务", "§d", -1L);

    private final String displayName;
    private final String color;
    private final long refreshInterval;  // 刷新间隔（毫秒），-1表示不自动刷新

    SectTaskType(String displayName, String color, long refreshInterval) {
        this.displayName = displayName;
        this.color = color;
        this.refreshInterval = refreshInterval;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    public long getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * 从字符串解析任务类型
     */
    public static SectTaskType fromString(String str) {
        for (SectTaskType type : values()) {
            if (type.name().equalsIgnoreCase(str) || type.displayName.equals(str)) {
                return type;
            }
        }
        return DAILY;  // 默认返回日常任务
    }
}
