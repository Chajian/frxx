package com.xiancore.web.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 审计日志服务
 * 记录敏感操作的审计日志
 */
@Service
@Slf4j
public class AuditLogService {

    private final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();

    /**
     * 记录审计日志
     */
    public void log(String operation, String userId, String resourceId, String result, String details) {
        AuditLog auditLog = AuditLog.builder()
                .operation(operation)
                .userId(userId)
                .resourceId(resourceId)
                .result(result)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogs.add(auditLog);
        log.info("Audit: {} - User: {} - Resource: {} - Result: {}",
                operation, userId, resourceId, result);
    }

    /**
     * 获取所有审计日志
     */
    public List<AuditLog> getAllAuditLogs() {
        return new ArrayList<>(auditLogs);
    }

    /**
     * 获取特定用户的审计日志
     */
    public List<AuditLog> getAuditLogsByUser(String userId) {
        return auditLogs.stream()
                .filter(log -> log.getUserId().equals(userId))
                .toList();
    }

    /**
     * 获取特定操作的审计日志
     */
    public List<AuditLog> getAuditLogsByOperation(String operation) {
        return auditLogs.stream()
                .filter(log -> log.getOperation().equals(operation))
                .toList();
    }

    /**
     * 清空审计日志（谨慎使用）
     */
    public void clearAuditLogs() {
        auditLogs.clear();
        log.warn("Audit logs cleared");
    }

    /**
     * 审计日志DTO
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AuditLog {
        /** 操作名称 */
        private String operation;
        /** 用户ID */
        private String userId;
        /** 资源ID */
        private String resourceId;
        /** 操作结果 (SUCCESS/FAILURE) */
        private String result;
        /** 详细信息 */
        private String details;
        /** 时间戳 */
        private LocalDateTime timestamp;
    }
}
