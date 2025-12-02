package com.xiancore.web.api.controller;

import com.xiancore.common.dto.ApiResponse;
import com.xiancore.web.service.AuditLogService;
import com.xiancore.web.service.CacheService;
import com.xiancore.web.service.PerformanceMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统监控和管理REST API端点
 * 提供缓存、性能监控和审计日志管理
 */
@RestController
@RequestMapping("/api/v1/management")
@RequiredArgsConstructor
@Slf4j
public class ManagementController {

    private final CacheService cacheService;
    private final PerformanceMonitorService performanceMonitorService;
    private final AuditLogService auditLogService;

    // ==================== 缓存管理 ====================

    /**
     * 获取缓存统计信息
     * GET /api/v1/management/cache/stats
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<ApiResponse<CacheService.CacheStatistics>> getCacheStatistics() {
        log.info("Fetching cache statistics");
        try {
            CacheService.CacheStatistics stats = cacheService.getCacheStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to fetch cache statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch cache statistics"));
        }
    }

    /**
     * 清空所有缓存
     * DELETE /api/v1/management/cache/clear
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<ApiResponse<Void>> clearAllCache() {
        log.info("Clearing all cache");
        try {
            cacheService.clearAllCache();
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to clear cache"));
        }
    }

    /**
     * 清空Boss缓存
     * DELETE /api/v1/management/cache/boss-cache
     */
    @DeleteMapping("/cache/boss-cache")
    public ResponseEntity<ApiResponse<Void>> clearBossCache() {
        log.info("Clearing boss cache");
        try {
            cacheService.evictAllBossCache();
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to clear boss cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to clear boss cache"));
        }
    }

    /**
     * 清空玩家统计缓存
     * DELETE /api/v1/management/cache/player-cache
     */
    @DeleteMapping("/cache/player-cache")
    public ResponseEntity<ApiResponse<Void>> clearPlayerCache() {
        log.info("Clearing player cache");
        try {
            cacheService.evictAllPlayerStatsCache();
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to clear player cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to clear player cache"));
        }
    }

    /**
     * 清空排行榜缓存
     * DELETE /api/v1/management/cache/ranking-cache
     */
    @DeleteMapping("/cache/ranking-cache")
    public ResponseEntity<ApiResponse<Void>> clearRankingCache() {
        log.info("Clearing ranking cache");
        try {
            cacheService.evictAllRankingCache();
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to clear ranking cache", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to clear ranking cache"));
        }
    }

    // ==================== 性能监控 ====================

    /**
     * 获取性能统计
     * GET /api/v1/management/performance/stats
     */
    @GetMapping("/performance/stats")
    public ResponseEntity<ApiResponse<PerformanceMonitorService.PerformanceStatistics>> getPerformanceStatistics() {
        log.info("Fetching performance statistics");
        try {
            PerformanceMonitorService.PerformanceStatistics stats = performanceMonitorService.getPerformanceStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to fetch performance statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch performance statistics"));
        }
    }

    /**
     * 获取特定端点的性能统计
     * GET /api/v1/management/performance/endpoint?endpoint=/api/v1/bosses
     */
    @GetMapping("/performance/endpoint")
    public ResponseEntity<ApiResponse<PerformanceMonitorService.EndpointStatistics>> getEndpointStatistics(
            @RequestParam String endpoint) {
        log.info("Fetching endpoint statistics for: {}", endpoint);
        try {
            PerformanceMonitorService.EndpointStatistics stats = performanceMonitorService.getEndpointStatistics(endpoint);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to fetch endpoint statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch endpoint statistics"));
        }
    }

    /**
     * 清空性能指标
     * DELETE /api/v1/management/performance/clear
     */
    @DeleteMapping("/performance/clear")
    public ResponseEntity<ApiResponse<Void>> clearPerformanceMetrics() {
        log.info("Clearing performance metrics");
        try {
            performanceMonitorService.clearMetrics();
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to clear performance metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to clear performance metrics"));
        }
    }

    // ==================== 审计日志 ====================

    /**
     * 获取所有审计日志
     * GET /api/v1/management/audit-logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<java.util.List<AuditLogService.AuditLog>>> getAllAuditLogs() {
        log.info("Fetching all audit logs");
        try {
            var logs = auditLogService.getAllAuditLogs();
            return ResponseEntity.ok(ApiResponse.success(logs));
        } catch (Exception e) {
            log.error("Failed to fetch audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch audit logs"));
        }
    }

    /**
     * 获取特定用户的审计日志
     * GET /api/v1/management/audit-logs/user/{userId}
     */
    @GetMapping("/audit-logs/user/{userId}")
    public ResponseEntity<ApiResponse<java.util.List<AuditLogService.AuditLog>>> getAuditLogsByUser(
            @PathVariable String userId) {
        log.info("Fetching audit logs for user: {}", userId);
        try {
            var logs = auditLogService.getAuditLogsByUser(userId);
            return ResponseEntity.ok(ApiResponse.success(logs));
        } catch (Exception e) {
            log.error("Failed to fetch audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch audit logs"));
        }
    }

    /**
     * 获取特定操作的审计日志
     * GET /api/v1/management/audit-logs/operation/{operation}
     */
    @GetMapping("/audit-logs/operation/{operation}")
    public ResponseEntity<ApiResponse<java.util.List<AuditLogService.AuditLog>>> getAuditLogsByOperation(
            @PathVariable String operation) {
        log.info("Fetching audit logs for operation: {}", operation);
        try {
            var logs = auditLogService.getAuditLogsByOperation(operation);
            return ResponseEntity.ok(ApiResponse.success(logs));
        } catch (Exception e) {
            log.error("Failed to fetch audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch audit logs"));
        }
    }

    /**
     * 清空审计日志（谨慎使用）
     * DELETE /api/v1/management/audit-logs/clear
     */
    @DeleteMapping("/audit-logs/clear")
    public ResponseEntity<ApiResponse<Void>> clearAuditLogs() {
        log.warn("Clearing audit logs - this operation should be audited");
        try {
            auditLogService.clearAuditLogs();
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to clear audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to clear audit logs"));
        }
    }

    // ==================== 系统健康检查 ====================

    /**
     * 获取系统健康信息
     * GET /api/v1/management/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        log.info("Fetching system health");
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());

            // 获取缓存统计
            CacheService.CacheStatistics cacheStats = cacheService.getCacheStatistics();
            health.put("cache", cacheStats);

            // 获取性能统计
            PerformanceMonitorService.PerformanceStatistics perfStats = performanceMonitorService.getPerformanceStatistics();
            health.put("performance", perfStats);

            // 获取审计日志统计
            int auditLogCount = auditLogService.getAllAuditLogs().size();
            health.put("auditLogCount", auditLogCount);

            // 获取系统资源信息
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memory = new HashMap<>();
            memory.put("totalMemory", runtime.totalMemory());
            memory.put("freeMemory", runtime.freeMemory());
            memory.put("maxMemory", runtime.maxMemory());
            memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            health.put("memory", memory);

            return ResponseEntity.ok(ApiResponse.success(health));
        } catch (Exception e) {
            log.error("Failed to fetch system health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch system health"));
        }
    }
}
