package com.xiancore.web.service;

import com.xiancore.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket事件广播服务
 * 处理实时事件通知
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 广播Boss事件
     */
    public void broadcastBossEvent(String eventType, Object data) {
        log.debug("Broadcasting boss event: {} - {}", eventType, data);
        messagingTemplate.convertAndSend("/topic/boss-events",
                ApiResponse.success(data));
    }

    /**
     * 广播伤害事件
     */
    public void broadcastDamageEvent(String playerId, Double damage, String bossId) {
        log.debug("Broadcasting damage event: player={}, damage={}, boss={}", playerId, damage, bossId);
        DamageEventDTO event = DamageEventDTO.builder()
                .playerId(playerId)
                .bossId(bossId)
                .damage(damage)
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSend("/topic/damage-events",
                ApiResponse.success(event));
    }

    /**
     * 广播击杀事件
     */
    public void broadcastKillEvent(String bossId, String playerId, String playerName) {
        log.debug("Broadcasting kill event: boss={}, player={}", bossId, playerId);
        KillEventDTO event = KillEventDTO.builder()
                .bossId(bossId)
                .playerId(playerId)
                .playerName(playerName)
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSend("/topic/kill-events",
                ApiResponse.success(event));
    }

    /**
     * 广播统计更新事件
     */
    public void broadcastStatsUpdate(String playerId, Object stats) {
        log.debug("Broadcasting stats update: player={}", playerId);
        messagingTemplate.convertAndSend("/topic/stats-update",
                ApiResponse.success(stats));
    }

    /**
     * 广播排行榜更新
     */
    public void broadcastLeaderboardUpdate(String type, Object leaderboard) {
        log.debug("Broadcasting leaderboard update: type={}", type);
        messagingTemplate.convertAndSend("/topic/leaderboard-update",
                ApiResponse.success(leaderboard));
    }

    /**
     * 广播系统告警
     */
    public void broadcastAlert(String title, String message, String severity) {
        log.info("Broadcasting alert: {} - {}", title, message);
        AlertDTO alert = AlertDTO.builder()
                .title(title)
                .message(message)
                .severity(severity)
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSend("/topic/alerts",
                ApiResponse.success(alert));
    }

    /**
     * 广播服务器状态
     */
    public void broadcastServerStatus(Object status) {
        log.debug("Broadcasting server status");
        messagingTemplate.convertAndSend("/topic/server-status",
                ApiResponse.success(status));
    }

    /**
     * 伤害事件DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    public static class DamageEventDTO {
        private String playerId;
        private String bossId;
        private Double damage;
        private long timestamp;
    }

    /**
     * 击杀事件DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    public static class KillEventDTO {
        private String bossId;
        private String playerId;
        private String playerName;
        private long timestamp;
    }

    /**
     * 告警DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    public static class AlertDTO {
        private String title;
        private String message;
        private String severity;
        private long timestamp;
    }
}
