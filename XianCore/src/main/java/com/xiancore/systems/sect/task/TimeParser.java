package com.xiancore.systems.sect.task;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 时间解析工具类
 * 用于解析配置文件中的时间字符串
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TimeParser {

    /**
     * 解析时间字符串
     * 支持多种格式：
     * - "06:00"
     * - "6:00"
     * - "06:00:00"
     *
     * @param timeStr 时间字符串
     * @return LocalTime 对象，解析失败返回默认值 06:00
     */
    public static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return LocalTime.of(6, 0); // 默认早上6点
        }

        try {
            // 尝试标准格式解析
            return LocalTime.parse(timeStr.trim());
        } catch (DateTimeParseException e1) {
            // 尝试手动解析 "HH:MM" 或 "H:MM"
            try {
                String[] parts = timeStr.trim().split(":");
                if (parts.length >= 2) {
                    int hour = Integer.parseInt(parts[0].trim());
                    int minute = Integer.parseInt(parts[1].trim());
                    
                    // 验证范围
                    if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) {
                        return LocalTime.of(hour, minute);
                    }
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e2) {
                // 继续到默认值
            }
        }

        // 解析失败，使用默认值
        System.err.println("[TimeParser] 时间解析失败: " + timeStr + ", 使用默认值 06:00");
        return LocalTime.of(6, 0);
    }

    /**
     * 解析星期字符串
     * 支持格式：
     * - "MON", "MONDAY"
     * - "TUE", "TUESDAY"
     * - 等等...
     *
     * @param dayStr 星期字符串
     * @return DayOfWeek 对象，解析失败返回 MONDAY
     */
    public static DayOfWeek parseDayOfWeek(String dayStr) {
        if (dayStr == null || dayStr.trim().isEmpty()) {
            return DayOfWeek.MONDAY; // 默认周一
        }

        String normalized = dayStr.trim().toUpperCase();

        // 尝试匹配
        return switch (normalized) {
            case "MON", "MONDAY", "星期一", "周一" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY", "星期二", "周二" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY", "星期三", "周三" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY", "星期四", "周四" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY", "星期五", "周五" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY", "星期六", "周六" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY", "星期日", "周日" -> DayOfWeek.SUNDAY;
            default -> {
                System.err.println("[TimeParser] 未知的星期: " + dayStr + ", 使用默认值 MONDAY");
                yield DayOfWeek.MONDAY;
            }
        };
    }

    /**
     * 验证时间格式是否有效
     *
     * @param timeStr 时间字符串
     * @return 是否有效
     */
    public static boolean isValidTimeFormat(String timeStr) {
        try {
            LocalTime time = parseTime(timeStr);
            return time != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证星期格式是否有效
     *
     * @param dayStr 星期字符串
     * @return 是否有效
     */
    public static boolean isValidDayFormat(String dayStr) {
        try {
            DayOfWeek day = parseDayOfWeek(dayStr);
            return day != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 格式化时间为显示字符串
     *
     * @param time 时间
     * @return 格式化字符串 "HH:MM"
     */
    public static String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 格式化星期为中文显示
     *
     * @param day 星期
     * @return 中文星期
     */
    public static String formatDayOfWeek(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }
}







