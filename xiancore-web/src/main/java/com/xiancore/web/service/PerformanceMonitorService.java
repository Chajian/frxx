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
 * 性能监控服务
 * 监控API响应时间和数据库查询性能
 */
@Service
@Slf4j
public class PerformanceMonitorService {

    private final List<PerformanceMetric> metrics = new CopyOnWriteArrayList<>();
    private static final int MAX_METRICS = 10000; // 最多保存10000条记录

    /**
     * 记录性能指标
     */
    public void recordMetric(String endpoint, long responseTimeMs, boolean success) {
        PerformanceMetric metric = PerformanceMetric.builder()
                .endpoint(endpoint)
                .responseTimeMs(responseTimeMs)
                .success(success)
                .timestamp(LocalDateTime.now())
                .build();

        metrics.add(metric);

        // 防止内存溢出
        if (metrics.size() > MAX_METRICS) {
            metrics.remove(0);
        }

        if (responseTimeMs > 1000) {
            log.warn("Slow API: {} took {}ms", endpoint, responseTimeMs);
        }
    }

    /**
     * 获取性能统计
     */
    public PerformanceStatistics getPerformanceStatistics() {
        if (metrics.isEmpty()) {
            return PerformanceStatistics.builder()
                    .totalRequests(0)
                    .successfulRequests(0)
                    .failedRequests(0)
                    .averageResponseTimeMs(0)
                    .maxResponseTimeMs(0)
                    .minResponseTimeMs(0)
                    .successRate(0)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        long totalRequests = metrics.size();
        long successfulRequests = metrics.stream().filter(m -> m.success).count();
        long failedRequests = totalRequests - successfulRequests;
        double averageResponseTime = metrics.stream()
                .mapToLong(m -> m.responseTimeMs)
                .average()
                .orElse(0);
        long maxResponseTime = metrics.stream()
                .mapToLong(m -> m.responseTimeMs)
                .max()
                .orElse(0);
        long minResponseTime = metrics.stream()
                .mapToLong(m -> m.responseTimeMs)
                .min()
                .orElse(0);
        double successRate = (double) successfulRequests / totalRequests * 100;

        return PerformanceStatistics.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .averageResponseTimeMs((long) averageResponseTime)
                .maxResponseTimeMs(maxResponseTime)
                .minResponseTimeMs(minResponseTime)
                .successRate(successRate)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 获取特定端点的统计
     */
    public EndpointStatistics getEndpointStatistics(String endpoint) {
        List<PerformanceMetric> endpointMetrics = metrics.stream()
                .filter(m -> m.endpoint.equals(endpoint))
                .toList();

        if (endpointMetrics.isEmpty()) {
            return EndpointStatistics.builder()
                    .endpoint(endpoint)
                    .requestCount(0)
                    .successCount(0)
                    .failureCount(0)
                    .averageResponseTimeMs(0)
                    .build();
        }

        long requestCount = endpointMetrics.size();
        long successCount = endpointMetrics.stream().filter(m -> m.success).count();
        long failureCount = requestCount - successCount;
        double averageResponseTime = endpointMetrics.stream()
                .mapToLong(m -> m.responseTimeMs)
                .average()
                .orElse(0);

        return EndpointStatistics.builder()
                .endpoint(endpoint)
                .requestCount(requestCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .averageResponseTimeMs((long) averageResponseTime)
                .build();
    }

    /**
     * 清空性能指标
     */
    public void clearMetrics() {
        metrics.clear();
        log.info("Performance metrics cleared");
    }

    /**
     * 性能指标DTO
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PerformanceMetric {
        private String endpoint;
        private long responseTimeMs;
        private boolean success;
        private LocalDateTime timestamp;
    }

    /**
     * 性能统计DTO
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class PerformanceStatistics {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private long averageResponseTimeMs;
        private long maxResponseTimeMs;
        private long minResponseTimeMs;
        private double successRate;
        private LocalDateTime timestamp;
    }

    /**
     * 端点统计DTO
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class EndpointStatistics {
        private String endpoint;
        private long requestCount;
        private long successCount;
        private long failureCount;
        private long averageResponseTimeMs;
    }
}
