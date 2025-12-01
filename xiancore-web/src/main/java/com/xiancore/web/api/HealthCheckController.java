package com.xiancore.web.api;

import com.xiancore.common.dto.ApiResponse;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查API - 用于插件和监控系统检查服务状态
 */
@Log
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    /**
     * 健康检查端点
     * GET /api/health
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "XianCore Web Service");
        data.put("version", "1.0.0");
        data.put("timestamp", System.currentTimeMillis());
        data.put("uptime", Runtime.getRuntime().totalMemory());

        return ApiResponse.success(data);
    }

    /**
     * 版本信息端点
     * GET /api/version
     */
    @GetMapping("/version")
    public ApiResponse<Map<String, String>> version() {
        Map<String, String> data = new HashMap<>();
        data.put("version", "1.0.0");
        data.put("buildTime", "2025-11-16");
        data.put("javaVersion", System.getProperty("java.version"));

        return ApiResponse.success(data);
    }
}
