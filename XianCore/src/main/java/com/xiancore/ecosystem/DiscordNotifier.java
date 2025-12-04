package com.xiancore.ecosystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Discord通知系统 - 游戏事件推送到Discord
 * Discord Notification System - Push Game Events to Discord
 *
 * @author XianCore
 * @version 1.0
 */
public class DiscordNotifier {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, DiscordChannel> channels = new ConcurrentHashMap<>();
    private final List<DiscordNotification> notificationHistory = new CopyOnWriteArrayList<>();
    private boolean discordEnabled = false;
    private String botToken = "";
    private String webhookUrl = "";

    /**
     * Discord频道配置
     */
    public static class DiscordChannel {
        public String channelId;
        public String channelName;
        public ChannelType type;       // 事件类型
        public String webhookUrl;
        public boolean enabled;
        public int notificationCount;  // 发送通知数

        public enum ChannelType {
            BOSS_EVENTS,       // Boss事件
            PLAYER_KILLS,      // 玩家击杀
            ECONOMY,          // 经济事件
            ALERTS,           // 系统警报
            ADMIN_LOG         // 管理日志
        }

        public DiscordChannel(String channelId, String channelName, ChannelType type) {
            this.channelId = channelId;
            this.channelName = channelName;
            this.type = type;
            this.enabled = true;
            this.notificationCount = 0;
        }
    }

    /**
     * Discord通知消息
     */
    public static class DiscordNotification {
        public String notificationId;
        public String title;
        public String description;
        public NotificationType type;
        public String color;           // 十六进制颜色 (无#)
        public long timestamp;
        public Map<String, String> fields;
        public boolean sent;
        public String sendError;       // 发送错误信息

        public enum NotificationType {
            BOSS_SPAWN,        // Boss生成
            BOSS_KILL,         // Boss击杀
            MILESTONE,         // 里程碑
            WARNING,           // 警告
            INFO               // 信息
        }

        public DiscordNotification(String title, String description, NotificationType type) {
            this.notificationId = UUID.randomUUID().toString();
            this.title = title;
            this.description = description;
            this.type = type;
            this.color = getColorForType(type);
            this.timestamp = System.currentTimeMillis();
            this.fields = new LinkedHashMap<>();
            this.sent = false;
        }

        private static String getColorForType(NotificationType type) {
            return switch (type) {
                case BOSS_SPAWN -> "FF6B00";    // 橙色
                case BOSS_KILL -> "00AA00";    // 绿色
                case MILESTONE -> "FFAA00";    // 黄色
                case WARNING -> "FF0000";      // 红色
                case INFO -> "0099FF";         // 蓝色
            };
        }

        public String toEmbedJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"title\":\"").append(title).append("\",");
            sb.append("\"description\":\"").append(description).append("\",");
            sb.append("\"color\":").append(Integer.parseInt(color, 16)).append(",");
            sb.append("\"timestamp\":\"").append(new Date(timestamp)).append("\"");

            if (!fields.isEmpty()) {
                sb.append(",\"fields\":[");
                boolean first = true;
                for (Map.Entry<String, String> entry : fields.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append("{\"name\":\"").append(entry.getKey()).append("\",");
                    sb.append("\"value\":\"").append(entry.getValue()).append("\"}");
                    first = false;
                }
                sb.append("]");
            }

            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * 构造函数
     */
    public DiscordNotifier() {
        logger.info("✓ DiscordNotifier已初始化");
    }

    /**
     * 初始化Discord连接
     */
    public void initializeDiscord(String botToken, String webhookUrl, boolean enabled) {
        this.botToken = botToken;
        this.webhookUrl = webhookUrl;
        this.discordEnabled = enabled;

        if (enabled && !webhookUrl.isEmpty()) {
            logger.info("✓ Discord通知系统已启用");
            initializeDefaultChannels();
        } else {
            logger.info("⚠ Discord通知系统已禁用");
        }
    }

    /**
     * 初始化默认频道
     */
    private void initializeDefaultChannels() {
        createChannel("boss-events", "Boss事件", DiscordChannel.ChannelType.BOSS_EVENTS);
        createChannel("kills", "击杀记录", DiscordChannel.ChannelType.PLAYER_KILLS);
        createChannel("economy", "经济系统", DiscordChannel.ChannelType.ECONOMY);
        createChannel("alerts", "系统警报", DiscordChannel.ChannelType.ALERTS);
        createChannel("logs", "管理日志", DiscordChannel.ChannelType.ADMIN_LOG);
    }

    /**
     * 创建频道
     */
    public void createChannel(String channelId, String channelName, DiscordChannel.ChannelType type) {
        DiscordChannel channel = new DiscordChannel(channelId, channelName, type);
        channels.put(channelId, channel);
    }

    /**
     * 发送Boss生成通知
     */
    public void notifyBossSpawn(String bossName, String bossType, int tier, String world, double x, double y, double z) {
        DiscordNotification notification = new DiscordNotification(
                "🔴 Boss已生成",
                bossName + " (" + bossType + ")",
                DiscordNotification.NotificationType.BOSS_SPAWN
        );

        notification.fields.put("等级", "T" + tier);
        notification.fields.put("世界", world);
        notification.fields.put("坐标", String.format("%.0f, %.0f, %.0f", x, y, z));

        sendNotification(notification, "boss-events");
    }

    /**
     * 发送Boss击杀通知
     */
    public void notifyBossKill(String playerName, String bossName, int tier, long aliveTime, String reward) {
        DiscordNotification notification = new DiscordNotification(
                "✅ Boss已击杀",
                playerName + " 击杀了 " + bossName,
                DiscordNotification.NotificationType.BOSS_KILL
        );

        notification.fields.put("等级", "T" + tier);
        notification.fields.put("存活时间", formatTime(aliveTime));
        notification.fields.put("奖励", reward);
        notification.color = "00AA00";  // 绿色

        sendNotification(notification, "kills");
    }

    /**
     * 发送里程碑通知
     */
    public void notifyMilestone(String playerName, String achievement, int count) {
        DiscordNotification notification = new DiscordNotification(
                "🏆 里程碑成就",
                playerName + " 达成了 " + achievement,
                DiscordNotification.NotificationType.MILESTONE
        );

        notification.fields.put("数量", String.valueOf(count));
        notification.color = "FFAA00";  // 黄色

        sendNotification(notification, "kills");
    }

    /**
     * 发送警告通知
     */
    public void notifyWarning(String title, String description) {
        DiscordNotification notification = new DiscordNotification(
                title,
                description,
                DiscordNotification.NotificationType.WARNING
        );

        notification.fields.put("时间", new Date().toString());

        sendNotification(notification, "alerts");
    }

    /**
     * 发送通知
     */
    private void sendNotification(DiscordNotification notification, String channelId) {
        if (!discordEnabled) {
            logger.warning("⚠ Discord通知系统未启用");
            return;
        }

        DiscordChannel channel = channels.get(channelId);
        if (channel == null || !channel.enabled) {
            logger.warning("⚠ 频道不存在或已禁用: " + channelId);
            return;
        }

        // 模拟发送 (实际应用中会使用HTTP POST请求)
        try {
            notification.sent = true;
            channel.notificationCount++;
            notificationHistory.add(notification);

            logger.info("✓ Discord通知已发送: [" + channelId + "] " + notification.title);
        } catch (Exception e) {
            notification.sent = false;
            notification.sendError = e.getMessage();
            logger.warning("⚠ Discord通知发送失败: " + e.getMessage());
        }
    }

    /**
     * 时间格式化
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "小时 " + (minutes % 60) + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟 " + (seconds % 60) + "秒";
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 获取频道
     */
    public DiscordChannel getChannel(String channelId) {
        return channels.get(channelId);
    }

    /**
     * 获取所有频道
     */
    public Collection<DiscordChannel> getAllChannels() {
        return channels.values();
    }

    /**
     * 获取通知历史
     */
    public List<DiscordNotification> getNotificationHistory(int limit) {
        return notificationHistory.stream()
                .skip(Math.max(0, notificationHistory.size() - limit))
                .toList();
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("discord_enabled", discordEnabled);
        stats.put("total_channels", channels.size());
        stats.put("total_notifications", notificationHistory.size());

        // 按频道统计
        Map<String, Integer> channelStats = new HashMap<>();
        for (DiscordChannel channel : channels.values()) {
            channelStats.put(channel.channelName, channel.notificationCount);
        }
        stats.put("notifications_by_channel", channelStats);

        return stats;
    }

    /**
     * 重置系统
     */
    public void reset() {
        notificationHistory.clear();
        for (DiscordChannel channel : channels.values()) {
            channel.notificationCount = 0;
        }
        logger.info("✓ Discord通知系统已重置");
    }
}
