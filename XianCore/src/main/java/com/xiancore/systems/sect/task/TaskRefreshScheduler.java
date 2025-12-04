package com.xiancore.systems.sect.task;

import com.xiancore.XianCore;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务刷新调度器
 * 负责自动定时刷新日常和周常任务
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class TaskRefreshScheduler {

    private final XianCore plugin;
    private final SectTaskManager taskManager;

    // 定时任务
    private BukkitTask dailyRefreshTask;
    private BukkitTask weeklyRefreshTask;
    private final List<BukkitTask> warningTasks = new ArrayList<>();

    // 刷新记录
    private RefreshRecord record;

    // 配置
    private boolean enabled = true;
    private LocalTime dailyRefreshTime;
    private DayOfWeek weeklyRefreshDay;
    private LocalTime weeklyRefreshTime;
    private ZoneId timezone;

    // 刷新策略配置
    private boolean protectHighProgress;
    private int progressThreshold;
    private boolean protectCompleted;
    private boolean enableCompensation;
    private double compensationRate;
    private int batchSize;
    private int batchDelayTicks;
    private boolean asyncRefresh;

    // 通知配置
    private boolean notificationsEnabled;
    private List<Integer> warnBeforeMinutes;
    private boolean broadcastRefresh;
    private boolean soundEffect;

    public TaskRefreshScheduler(XianCore plugin, SectTaskManager taskManager) {
        this.plugin = plugin;
        this.taskManager = taskManager;
        this.record = new RefreshRecord(); // 稍后从数据库加载
    }

    /**
     * 初始化定时器
     */
    public void initialize() {
        plugin.getLogger().info("§e正在初始化任务刷新定时器...");

        // 加载配置
        loadConfig();

        // 加载刷新记录
        loadRefreshRecord();

        if (!enabled) {
            plugin.getLogger().info("§7  任务自动刷新已禁用");
            return;
        }

        // 启动时检查是否需要补刷新
        checkAndRefreshOnStartup();

        // 调度定时刷新
        scheduleDailyRefresh();
        scheduleWeeklyRefresh();

        plugin.getLogger().info("§a✓ 任务自动刷新定时器已启动");
        plugin.getLogger().info("§7  日常刷新: 每天 " + TimeParser.formatTime(dailyRefreshTime) + " (" + timezone.getId() + ")");
        plugin.getLogger().info("§7  周常刷新: 每" + TimeParser.formatDayOfWeek(weeklyRefreshDay) + " " + TimeParser.formatTime(weeklyRefreshTime));
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getConfig("sect_task");

        // 基础配置
        enabled = config.getBoolean("settings.refresh-strategy.auto-refresh", true);

        // 解析时间
        String dailyTimeStr = config.getString("settings.refresh-times.daily", "06:00");
        dailyRefreshTime = TimeParser.parseTime(dailyTimeStr);

        String weeklyTimeStr = config.getString("settings.refresh-times.weekly", "MON 06:00");
        String[] parts = weeklyTimeStr.split("\\s+");
        weeklyRefreshDay = TimeParser.parseDayOfWeek(parts[0]);
        weeklyRefreshTime = TimeParser.parseTime(parts.length > 1 ? parts[1] : "06:00");

        // 时区
        String zoneStr = config.getString("settings.refresh-times.timezone", "Asia/Shanghai");
        try {
            timezone = ZoneId.of(zoneStr);
        } catch (Exception e) {
            plugin.getLogger().warning("§e无效的时区: " + zoneStr + ", 使用系统默认时区");
            timezone = ZoneId.systemDefault();
        }

        // 刷新策略
        protectHighProgress = config.getBoolean("settings.refresh-strategy.protect-high-progress", true);
        progressThreshold = config.getInt("settings.refresh-strategy.progress-threshold", 80);
        protectCompleted = config.getBoolean("settings.refresh-strategy.protect-completed", true);
        enableCompensation = config.getBoolean("settings.refresh-strategy.enable-compensation", true);
        compensationRate = config.getDouble("settings.refresh-strategy.compensation-rate", 0.5);
        batchSize = config.getInt("settings.refresh-strategy.batch-size", 50);
        batchDelayTicks = config.getInt("settings.refresh-strategy.batch-delay-ticks", 20);
        asyncRefresh = config.getBoolean("settings.refresh-strategy.async-refresh", true);

        // 通知配置
        notificationsEnabled = config.getBoolean("settings.refresh-notifications.enabled", true);
        warnBeforeMinutes = config.getIntegerList("settings.refresh-notifications.warn-before-minutes");
        if (warnBeforeMinutes.isEmpty()) {
            warnBeforeMinutes = List.of(10, 5, 1); // 默认值
        }
        broadcastRefresh = config.getBoolean("settings.refresh-notifications.broadcast-refresh", true);
        soundEffect = config.getBoolean("settings.refresh-notifications.sound-effect", true);
    }

    /**
     * 加载刷新记录
     */
    private void loadRefreshRecord() {
        // TODO: 从数据库加载刷新记录
        // 这里暂时使用新记录
        if (record == null) {
            record = new RefreshRecord();
        }
    }

    /**
     * 保存刷新记录
     */
    private void saveRefreshRecord() {
        // TODO: 保存到数据库
        record.cleanOldErrors();
    }

    /**
     * 启动时检查并补刷新
     */
    private void checkAndRefreshOnStartup() {
        if (record.needsDailyRefresh()) {
            plugin.getLogger().info("§e检测到需要刷新日常任务（启动补刷新）");
            // 延迟10秒执行，等待系统完全初始化
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeDailyRefresh();
                saveRefreshRecord();
            }, 200L);
        }

        if (record.needsWeeklyRefresh(weeklyRefreshDay)) {
            plugin.getLogger().info("§e检测到需要刷新周常任务（启动补刷新）");
            // 延迟20秒执行
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeWeeklyRefresh();
                saveRefreshRecord();
            }, 400L);
        }
    }

    /**
     * 调度日常刷新
     */
    private void scheduleDailyRefresh() {
        // 计算下次刷新时间
        ZonedDateTime nextRefresh = calculateNextDailyRefresh();
        long delayTicks = calculateDelayTicks(nextRefresh);

        plugin.getLogger().info("§7  下次日常刷新: " + nextRefresh.toLocalDateTime() +
                " (延迟 " + (delayTicks / 20) + " 秒)");

        // 调度刷新任务
        dailyRefreshTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            executeDailyRefresh();
            // 刷新完成后，重新调度下一次
            scheduleDailyRefresh();
        }, delayTicks);

        // 调度警告通知
        if (notificationsEnabled) {
            scheduleWarnings(nextRefresh, SectTaskType.DAILY);
        }
    }

    /**
     * 调度周常刷新
     */
    private void scheduleWeeklyRefresh() {
        // 计算下次刷新时间
        ZonedDateTime nextRefresh = calculateNextWeeklyRefresh();
        long delayTicks = calculateDelayTicks(nextRefresh);

        plugin.getLogger().info("§7  下次周常刷新: " + nextRefresh.toLocalDateTime() +
                " (延迟 " + (delayTicks / 20) + " 秒)");

        // 调度刷新任务
        weeklyRefreshTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            executeWeeklyRefresh();
            // 刷新完成后，重新调度下一次
            scheduleWeeklyRefresh();
        }, delayTicks);

        // 调度警告通知
        if (notificationsEnabled) {
            scheduleWarnings(nextRefresh, SectTaskType.WEEKLY);
        }
    }

    /**
     * 调度警告通知
     */
    private void scheduleWarnings(ZonedDateTime refreshTime, SectTaskType type) {
        for (int minutes : warnBeforeMinutes) {
            ZonedDateTime warnTime = refreshTime.minusMinutes(minutes);
            long delayTicks = calculateDelayTicks(warnTime);

            if (delayTicks > 0) {
                String message = formatWarningMessage(type, minutes);
                BukkitTask warningTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    notifyWarning(message);
                }, delayTicks);

                warningTasks.add(warningTask);
            }
        }
    }

    /**
     * 格式化警告消息
     */
    private String formatWarningMessage(SectTaskType type, int minutes) {
        if (minutes >= 10) {
            return "§e§l[任务提醒] §r" + minutes + "分钟后将刷新" + type.getDisplayName() + "，请尽快完成当前任务！";
        } else if (minutes >= 5) {
            return "§c§l[任务提醒] §r" + minutes + "分钟后将刷新" + type.getDisplayName() + "！";
        } else {
            return "§4§l[任务提醒] §r" + minutes + "分钟后将刷新" + type.getDisplayName() + "，未完成任务将获得补偿！";
        }
    }

    /**
     * 发送警告通知
     */
    private void notifyWarning(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getSectSystem().getPlayerSect(player.getUniqueId()) != null) {
                player.sendMessage(message);
                if (soundEffect) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                }
            }
        }
    }

    /**
     * 执行日常刷新
     */
    private void executeDailyRefresh() {
        plugin.getLogger().info("§e━━━━━━━━ 开始刷新日常任务 ━━━━━━━━");

        long startTime = System.currentTimeMillis();
        int playerCount = 0;

        try {
            // 刷新任务
            playerCount = taskManager.refreshTasksWithProtection(SectTaskType.DAILY,
                    protectHighProgress, progressThreshold, protectCompleted,
                    enableCompensation, compensationRate);

            // 记录刷新
            record.recordRefresh(SectTaskType.DAILY, playerCount);
            saveRefreshRecord();

            long elapsed = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("§a日常任务刷新完成！耗时: " + elapsed + "ms，共刷新 " + playerCount + " 个玩家");

            // 通知玩家
            if (notificationsEnabled) {
                notifyPlayersAfterRefresh(SectTaskType.DAILY, playerCount);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("§c日常任务刷新失败: " + e.getMessage());
            e.printStackTrace();

            // 记录错误
            record.recordError(SectTaskType.DAILY, e.getMessage());
            saveRefreshRecord();
        }
    }

    /**
     * 执行周常刷新
     */
    private void executeWeeklyRefresh() {
        plugin.getLogger().info("§e━━━━━━━━ 开始刷新周常任务 ━━━━━━━━");

        long startTime = System.currentTimeMillis();
        int playerCount = 0;

        try {
            // 刷新任务
            playerCount = taskManager.refreshTasksWithProtection(SectTaskType.WEEKLY,
                    protectHighProgress, progressThreshold, protectCompleted,
                    enableCompensation, compensationRate);

            // 记录刷新
            record.recordRefresh(SectTaskType.WEEKLY, playerCount);
            saveRefreshRecord();

            long elapsed = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("§a周常任务刷新完成！耗时: " + elapsed + "ms，共刷新 " + playerCount + " 个玩家");

            // 通知玩家
            if (notificationsEnabled) {
                notifyPlayersAfterRefresh(SectTaskType.WEEKLY, playerCount);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("§c周常任务刷新失败: " + e.getMessage());
            e.printStackTrace();

            // 记录错误
            record.recordError(SectTaskType.WEEKLY, e.getMessage());
            saveRefreshRecord();
        }
    }

    /**
     * 刷新后通知玩家
     */
    private void notifyPlayersAfterRefresh(SectTaskType type, int playerCount) {
        String message = "§a§l[任务刷新] §r" + type.getDisplayName() +
                " 已刷新！共为 " + playerCount + " 个玩家刷新了任务";

        // 全服广播（可选）
        if (broadcastRefresh) {
            Bukkit.broadcastMessage(message);
        }

        // 个人通知
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getSectSystem().getPlayerSect(player.getUniqueId()) != null) {
                player.sendMessage("§e使用 §f/sect task §e查看新任务");
                if (soundEffect) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                }
            }
        }
    }

    /**
     * 计算下次日常刷新时间
     */
    private ZonedDateTime calculateNextDailyRefresh() {
        ZonedDateTime now = ZonedDateTime.now(timezone);

        // 设置为今天的刷新时间
        ZonedDateTime next = now.toLocalDate()
                .atTime(dailyRefreshTime)
                .atZone(timezone);

        // 如果已经过了今天的刷新时间，推到明天
        if (next.isBefore(now) || next.isEqual(now)) {
            next = next.plusDays(1);
        }

        return next;
    }

    /**
     * 计算下次周常刷新时间
     */
    private ZonedDateTime calculateNextWeeklyRefresh() {
        ZonedDateTime now = ZonedDateTime.now(timezone);

        // 找到下一个目标星期几
        LocalDate nextDate = now.toLocalDate()
                .with(TemporalAdjusters.nextOrSame(weeklyRefreshDay));

        ZonedDateTime next = nextDate
                .atTime(weeklyRefreshTime)
                .atZone(timezone);

        // 如果是今天但已经过了刷新时间，推到下周
        if (next.toLocalDate().equals(now.toLocalDate()) && next.isBefore(now)) {
            next = next.plusWeeks(1);
        }

        return next;
    }

    /**
     * 计算延迟 ticks
     *
     * @param targetTime 目标时间
     * @return 延迟的 ticks 数
     */
    private long calculateDelayTicks(ZonedDateTime targetTime) {
        ZonedDateTime now = ZonedDateTime.now(timezone);
        long millis = Duration.between(now, targetTime).toMillis();

        // 确保不为负数
        if (millis < 0) {
            return 0;
        }

        // 转换为 ticks (1 tick = 50ms)
        return millis / 50;
    }

    /**
     * 重新调度（配置重载后调用）
     */
    public void reschedule() {
        plugin.getLogger().info("§e重新调度任务刷新定时器...");

        // 取消现有定时器
        stop();

        // 清空警告任务
        warningTasks.clear();

        // 重新加载配置
        loadConfig();

        if (!enabled) {
            plugin.getLogger().info("§7  任务自动刷新已禁用");
            return;
        }

        // 重新调度
        scheduleDailyRefresh();
        scheduleWeeklyRefresh();

        plugin.getLogger().info("§a任务刷新定时器重新调度完成");
    }

    /**
     * 停止所有定时器
     */
    public void stop() {
        if (dailyRefreshTask != null && !dailyRefreshTask.isCancelled()) {
            dailyRefreshTask.cancel();
            plugin.getLogger().info("§7  日常任务定时器已停止");
        }

        if (weeklyRefreshTask != null && !weeklyRefreshTask.isCancelled()) {
            weeklyRefreshTask.cancel();
            plugin.getLogger().info("§7  周常任务定时器已停止");
        }

        for (BukkitTask task : warningTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        warningTasks.clear();
    }

    /**
     * 停止调度器（插件卸载时调用）
     */
    public void shutdown() {
        plugin.getLogger().info("§e正在停止任务刷新定时器...");
        stop();
        plugin.getLogger().info("§a✓ 任务刷新定时器已停止");
    }

    /**
     * 获取下次日常刷新时间（格式化）
     */
    public String getNextDailyRefreshTime() {
        if (dailyRefreshTask == null || dailyRefreshTask.isCancelled()) {
            return "未调度";
        }
        ZonedDateTime next = calculateNextDailyRefresh();
        return next.toLocalDateTime().toString();
    }

    /**
     * 获取下次周常刷新时间（格式化）
     */
    public String getNextWeeklyRefreshTime() {
        if (weeklyRefreshTask == null || weeklyRefreshTask.isCancelled()) {
            return "未调度";
        }
        ZonedDateTime next = calculateNextWeeklyRefresh();
        return next.toLocalDateTime().toString();
    }

    /**
     * 获取最后日常刷新时间
     */
    public String getLastDailyRefresh() {
        if (record.getLastDailyRefresh() == null) {
            return "从未刷新";
        }
        return record.getLastDailyRefresh().toString();
    }

    /**
     * 获取最后周常刷新时间
     */
    public String getLastWeeklyRefresh() {
        if (record.getLastWeeklyRefresh() == null) {
            return "从未刷新";
        }
        return record.getLastWeeklyRefresh().toString();
    }

    /**
     * 手动触发刷新
     *
     * @param type 任务类型
     * @return 刷新的玩家数
     */
    public int manualRefresh(SectTaskType type) {
        plugin.getLogger().info("§e手动刷新 " + type.getDisplayName());

        int playerCount = taskManager.refreshTasksWithProtection(type,
                protectHighProgress, progressThreshold, protectCompleted,
                enableCompensation, compensationRate);

        // 记录刷新
        record.recordRefresh(type, playerCount);
        saveRefreshRecord();

        return playerCount;
    }
}







