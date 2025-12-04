package com.xiancore.integration.residence;

import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 权限审计日志系统
 * 记录所有权限变更事件，包括权限设置、移除、更新等
 * 用于权限问题的追踪和审查
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class PermissionAuditLog {

    // 权限日志事件列表（宗门ID -> 该宗门的日志列表）
    private final Map<Integer, List<PermissionAuditEvent>> auditLogs = new ConcurrentHashMap<>();

    // 最大日志保留数量
    private static final int MAX_LOG_SIZE = 1000;

    /**
     * 记录权限设置事件
     */
    public void logPermissionGranted(int sectId, String playerUuid, String playerName, String permissionLevel) {
        addLog(sectId, new PermissionAuditEvent(
            sectId,
            playerUuid,
            playerName,
            PermissionAuditEvent.EventType.PERMISSION_GRANTED,
            permissionLevel,
            "权限被设置为: " + permissionLevel,
            System.currentTimeMillis()
        ));
    }

    /**
     * 记录权限移除事件
     */
    public void logPermissionRevoked(int sectId, String playerUuid, String playerName, String previousLevel) {
        addLog(sectId, new PermissionAuditEvent(
            sectId,
            playerUuid,
            playerName,
            PermissionAuditEvent.EventType.PERMISSION_REVOKED,
            previousLevel,
            "权限已移除 (之前: " + previousLevel + ")",
            System.currentTimeMillis()
        ));
    }

    /**
     * 记录权限更新事件
     */
    public void logPermissionUpdated(int sectId, String playerUuid, String playerName, String oldLevel, String newLevel) {
        addLog(sectId, new PermissionAuditEvent(
            sectId,
            playerUuid,
            playerName,
            PermissionAuditEvent.EventType.PERMISSION_UPDATED,
            newLevel,
            "权限已更新: " + oldLevel + " -> " + newLevel,
            System.currentTimeMillis()
        ));
    }

    /**
     * 记录权限清除事件（删除领地时）
     */
    public void logPermissionsCleared(int sectId, String reason) {
        addLog(sectId, new PermissionAuditEvent(
            sectId,
            "SYSTEM",
            "系统",
            PermissionAuditEvent.EventType.PERMISSIONS_CLEARED,
            "N/A",
            "所有权限已清除: " + reason,
            System.currentTimeMillis()
        ));
    }

    /**
     * 记录权限批量设置事件（圈地时）
     */
    public void logPermissionsBatchSet(int sectId, int memberCount) {
        addLog(sectId, new PermissionAuditEvent(
            sectId,
            "SYSTEM",
            "系统",
            PermissionAuditEvent.EventType.BATCH_PERMISSION_SET,
            "N/A",
            "为 " + memberCount + " 个成员批量设置权限",
            System.currentTimeMillis()
        ));
    }

    /**
     * 添加日志条目
     */
    private void addLog(int sectId, PermissionAuditEvent event) {
        auditLogs.putIfAbsent(sectId, Collections.synchronizedList(new ArrayList<>()));
        List<PermissionAuditEvent> logs = auditLogs.get(sectId);
        logs.add(event);

        // 仅保留最近的MAX_LOG_SIZE条日志
        if (logs.size() > MAX_LOG_SIZE) {
            logs.remove(0);
        }
    }

    /**
     * 获取宗门的所有权限日志
     */
    public List<PermissionAuditEvent> getSectLogs(int sectId) {
        return new ArrayList<>(auditLogs.getOrDefault(sectId, new ArrayList<>()));
    }

    /**
     * 获取宗门特定玩家的权限日志
     */
    public List<PermissionAuditEvent> getPlayerLogs(int sectId, String playerUuid) {
        List<PermissionAuditEvent> logs = auditLogs.getOrDefault(sectId, new ArrayList<>());
        return logs.stream()
            .filter(log -> log.getPlayerUuid().equals(playerUuid))
            .collect(Collectors.toList());
    }

    /**
     * 获取特定事件类型的日志
     */
    public List<PermissionAuditEvent> getLogsByType(int sectId, PermissionAuditEvent.EventType type) {
        List<PermissionAuditEvent> logs = auditLogs.getOrDefault(sectId, new ArrayList<>());
        return logs.stream()
            .filter(log -> log.getEventType() == type)
            .collect(Collectors.toList());
    }

    /**
     * 获取最近的N条日志
     */
    public List<PermissionAuditEvent> getRecentLogs(int sectId, int count) {
        List<PermissionAuditEvent> logs = auditLogs.getOrDefault(sectId, new ArrayList<>());
        int startIndex = Math.max(0, logs.size() - count);
        return new ArrayList<>(logs.subList(startIndex, logs.size()));
    }

    /**
     * 清空宗门的日志（仅在宗门删除时）
     */
    public void clearSectLogs(int sectId) {
        auditLogs.remove(sectId);
    }

    /**
     * 获取日志统计信息
     */
    public String getLogStatistics(int sectId) {
        List<PermissionAuditEvent> logs = auditLogs.getOrDefault(sectId, new ArrayList<>());

        long grantedCount = logs.stream()
            .filter(log -> log.getEventType() == PermissionAuditEvent.EventType.PERMISSION_GRANTED)
            .count();
        long revokedCount = logs.stream()
            .filter(log -> log.getEventType() == PermissionAuditEvent.EventType.PERMISSION_REVOKED)
            .count();
        long updatedCount = logs.stream()
            .filter(log -> log.getEventType() == PermissionAuditEvent.EventType.PERMISSION_UPDATED)
            .count();

        return String.format(
            "权限审计统计 (宗门ID: %d):\n" +
            "  总日志条数: %d\n" +
            "  权限设置: %d 次\n" +
            "  权限移除: %d 次\n" +
            "  权限更新: %d 次",
            sectId, logs.size(), grantedCount, revokedCount, updatedCount
        );
    }

    /**
     * 权限审计事件类
     */
    public static class PermissionAuditEvent {

        @Getter
        public enum EventType {
            PERMISSION_GRANTED("权限设置"),
            PERMISSION_REVOKED("权限移除"),
            PERMISSION_UPDATED("权限更新"),
            PERMISSIONS_CLEARED("权限清除"),
            BATCH_PERMISSION_SET("批量设置权限");

            private final String description;

            EventType(String description) {
                this.description = description;
            }

            /**
             * 获取事件类型的描述
             */
            public String getDescription() {
                return description;
            }
        }

        private final int sectId;
        private final String playerUuid;
        private final String playerName;
        private final EventType eventType;
        private final String permissionLevel;
        private final String description;
        private final long timestamp;

        public PermissionAuditEvent(int sectId, String playerUuid, String playerName, EventType eventType,
                                    String permissionLevel, String description, long timestamp) {
            this.sectId = sectId;
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.eventType = eventType;
            this.permissionLevel = permissionLevel;
            this.description = description;
            this.timestamp = timestamp;
        }

        public int getSectId() {
            return sectId;
        }

        public String getPlayerUuid() {
            return playerUuid;
        }

        public String getPlayerName() {
            return playerName;
        }

        public EventType getEventType() {
            return eventType;
        }

        public String getPermissionLevel() {
            return permissionLevel;
        }

        public String getDescription() {
            return description;
        }

        public long getTimestamp() {
            return timestamp;
        }

        /**
         * 获取格式化的时间戳
         */
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date(timestamp));
        }

        /**
         * 获取完整的日志信息
         */
        public String getFullInfo() {
            return String.format(
                "[%s] %s - 玩家: %s (%s) - 权限: %s - %s",
                getFormattedTime(),
                eventType.getDescription(),
                playerName,
                playerUuid,
                permissionLevel,
                description
            );
        }
    }
}
