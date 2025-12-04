package com.xiancore.monitor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 告警系统 - 实时监控和生成系统告警
 * Alert System - Real-time monitoring and alert generation
 *
 * @author XianCore
 * @version 1.0
 */
public class AlertSystem {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final List<Alert> alertHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();
    private volatile int maxHistorySize = 500;
    private volatile long alertCooldownMs = 60000; // 同一类告警60秒冷却期

    /**
     * 告警规则
     */
    public static class AlertRule {
        public String ruleId;
        public String ruleName;
        public String condition;           // 告警条件描述
        public String severity;            // CRITICAL, HIGH, MEDIUM, LOW
        public double threshold;           // 阈值
        public String metricType;          // 指标类型: CPU, MEMORY, THREAD, BOSS, etc.
        public boolean enabled;
        public long cooldownMs;            // 冷却期

        public AlertRule(String ruleId, String ruleName, String condition, String severity,
                        double threshold, String metricType) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.condition = condition;
            this.severity = severity;
            this.threshold = threshold;
            this.metricType = metricType;
            this.enabled = true;
            this.cooldownMs = 60000;
        }
    }

    /**
     * 告警消息
     */
    public static class Alert {
        public String alertId;
        public String ruleId;
        public String title;
        public String message;
        public String severity;            // CRITICAL, HIGH, MEDIUM, LOW
        public String source;              // 告警来源 (CPU, MEMORY, BOSS, etc.)
        public LocalDateTime timestamp;
        public boolean resolved;
        public LocalDateTime resolvedTime;
        public Map<String, String> metadata; // 附加信息

        public Alert(String alertId, String ruleId, String title, String message, String severity, String source) {
            this.alertId = alertId;
            this.ruleId = ruleId;
            this.title = title;
            this.message = message;
            this.severity = severity;
            this.source = source;
            this.timestamp = LocalDateTime.now();
            this.resolved = false;
            this.metadata = new LinkedHashMap<>();
        }
    }

    /**
     * 告警统计
     */
    public static class AlertStatistics {
        public int totalAlerts;             // 总告警数
        public int unresolvedAlerts;        // 未解决告警数
        public int criticalCount;           // 严重告警数
        public int highCount;               // 高级告警数
        public int mediumCount;             // 中级告警数
        public int lowCount;                // 低级告警数
        public double alertRate;            // 每分钟告警率
        public LocalDateTime lastAlertTime; // 最后告警时间
        public long timestamp;

        public AlertStatistics() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 构造函数
     */
    public AlertSystem() {
        initializeDefaultRules();
        logger.info("✓ AlertSystem已初始化");
    }

    /**
     * 初始化默认告警规则
     */
    private void initializeDefaultRules() {
        // CPU告警
        addRule(new AlertRule("cpu-high", "CPU使用率过高", "CPU > 80%", "HIGH", 80.0, "CPU"));
        addRule(new AlertRule("cpu-critical", "CPU使用率严重过高", "CPU > 90%", "CRITICAL", 90.0, "CPU"));

        // 内存告警
        addRule(new AlertRule("mem-high", "内存使用率过高", "Memory > 80%", "HIGH", 80.0, "MEMORY"));
        addRule(new AlertRule("mem-critical", "内存使用率严重过高", "Memory > 90%", "CRITICAL", 90.0, "MEMORY"));

        // 线程告警
        addRule(new AlertRule("thread-high", "线程数过多", "Threads > 200", "MEDIUM", 200.0, "THREAD"));
        addRule(new AlertRule("thread-critical", "线程数严重过多", "Threads > 300", "HIGH", 300.0, "THREAD"));

        // Boss告警
        addRule(new AlertRule("boss-low-health", "Boss血量低于20%", "BossHealth < 20%", "MEDIUM", 20.0, "BOSS"));
        addRule(new AlertRule("boss-too-many", "活跃Boss过多", "ActiveBosses > 10", "MEDIUM", 10.0, "BOSS"));

        logger.info("✓ 默认告警规则已加载 (8条)");
    }

    /**
     * 添加告警规则
     */
    public void addRule(AlertRule rule) {
        alertRules.put(rule.ruleId, rule);
        logger.info("✓ 告警规则已添加: " + rule.ruleName);
    }

    /**
     * 获取告警规则
     */
    public AlertRule getRule(String ruleId) {
        return alertRules.get(ruleId);
    }

    /**
     * 获取所有规则
     */
    public Collection<AlertRule> getAllRules() {
        return alertRules.values();
    }

    /**
     * 启用/禁用规则
     */
    public void setRuleEnabled(String ruleId, boolean enabled) {
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            rule.enabled = enabled;
            String status = enabled ? "启用" : "禁用";
            logger.info("✓ 告警规则已" + status + ": " + rule.ruleName);
        }
    }

    /**
     * 创建告警
     */
    public Alert createAlert(String ruleId, String title, String message, String severity, String source) {
        // 检查冷却期
        String cooldownKey = ruleId;
        Long lastTime = lastAlertTime.get(cooldownKey);
        if (lastTime != null && System.currentTimeMillis() - lastTime < alertCooldownMs) {
            logger.info("⚠ 告警在冷却期内，已忽略: " + title);
            return null;
        }

        // 创建告警
        String alertId = "alert-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        Alert alert = new Alert(alertId, ruleId, title, message, severity, source);

        alertHistory.add(alert);
        lastAlertTime.put(cooldownKey, System.currentTimeMillis());

        // 保持历史大小
        if (alertHistory.size() > maxHistorySize) {
            alertHistory.remove(0);
        }

        logger.warning("⚠ 告警已生成: [" + severity + "] " + title);
        return alert;
    }

    /**
     * CPU告警检查
     */
    public Alert checkCPUAlert(double cpuUsage) {
        if (cpuUsage > 90) {
            AlertRule rule = getRule("cpu-critical");
            if (rule != null && rule.enabled) {
                return createAlert("cpu-critical", "CPU严重过高",
                        String.format("CPU使用率: %.1f%% (阈值: 90%%)", cpuUsage),
                        "CRITICAL", "CPU");
            }
        } else if (cpuUsage > 80) {
            AlertRule rule = getRule("cpu-high");
            if (rule != null && rule.enabled) {
                return createAlert("cpu-high", "CPU使用率过高",
                        String.format("CPU使用率: %.1f%% (阈值: 80%%)", cpuUsage),
                        "HIGH", "CPU");
            }
        }
        return null;
    }

    /**
     * 内存告警检查
     */
    public Alert checkMemoryAlert(double memoryUsage) {
        if (memoryUsage > 90) {
            AlertRule rule = getRule("mem-critical");
            if (rule != null && rule.enabled) {
                return createAlert("mem-critical", "内存严重过高",
                        String.format("内存使用率: %.1f%% (阈值: 90%%)", memoryUsage),
                        "CRITICAL", "MEMORY");
            }
        } else if (memoryUsage > 80) {
            AlertRule rule = getRule("mem-high");
            if (rule != null && rule.enabled) {
                return createAlert("mem-high", "内存使用率过高",
                        String.format("内存使用率: %.1f%% (阈值: 80%%)", memoryUsage),
                        "HIGH", "MEMORY");
            }
        }
        return null;
    }

    /**
     * 线程告警检查
     */
    public Alert checkThreadAlert(int threadCount) {
        if (threadCount > 300) {
            AlertRule rule = getRule("thread-critical");
            if (rule != null && rule.enabled) {
                return createAlert("thread-critical", "线程数严重过多",
                        String.format("当前线程数: %d (阈值: 300)", threadCount),
                        "HIGH", "THREAD");
            }
        } else if (threadCount > 200) {
            AlertRule rule = getRule("thread-high");
            if (rule != null && rule.enabled) {
                return createAlert("thread-high", "线程数过多",
                        String.format("当前线程数: %d (阈值: 200)", threadCount),
                        "MEDIUM", "THREAD");
            }
        }
        return null;
    }

    /**
     * Boss告警检查
     */
    public Alert checkBossAlert(int activeBossCount, double lowestHealth) {
        if (lowestHealth < 20) {
            AlertRule rule = getRule("boss-low-health");
            if (rule != null && rule.enabled) {
                return createAlert("boss-low-health", "Boss血量低于20%",
                        String.format("最低血量: %.1f%% (阈值: 20%%)", lowestHealth),
                        "MEDIUM", "BOSS");
            }
        }

        if (activeBossCount > 10) {
            AlertRule rule = getRule("boss-too-many");
            if (rule != null && rule.enabled) {
                return createAlert("boss-too-many", "活跃Boss过多",
                        String.format("当前Boss数: %d (阈值: 10)", activeBossCount),
                        "MEDIUM", "BOSS");
            }
        }

        return null;
    }

    /**
     * 解决告警
     */
    public void resolveAlert(String alertId) {
        for (Alert alert : alertHistory) {
            if (alert.alertId.equals(alertId)) {
                alert.resolved = true;
                alert.resolvedTime = LocalDateTime.now();
                logger.info("✓ 告警已解决: " + alert.title);
                break;
            }
        }
    }

    /**
     * 自动解决告警
     */
    public void autoResolveAlerts(String source, double currentValue, double threshold) {
        for (Alert alert : alertHistory) {
            if (!alert.resolved && alert.source.equals(source) && currentValue < threshold) {
                resolveAlert(alert.alertId);
            }
        }
    }

    /**
     * 获取活跃告警
     */
    public List<Alert> getActiveAlerts() {
        return alertHistory.stream()
                .filter(a -> !a.resolved)
                .sorted((a, b) -> {
                    // 先按严重级别排序
                    Map<String, Integer> severity = Map.of(
                            "CRITICAL", 4,
                            "HIGH", 3,
                            "MEDIUM", 2,
                            "LOW", 1
                    );
                    return Integer.compare(
                            severity.getOrDefault(b.severity, 0),
                            severity.getOrDefault(a.severity, 0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取告警历史
     */
    public List<Alert> getAlertHistory(int limit) {
        int startIndex = Math.max(0, alertHistory.size() - limit);
        return new ArrayList<>(alertHistory.subList(startIndex, alertHistory.size()));
    }

    /**
     * 获取特定来源的告警
     */
    public List<Alert> getAlertsBySource(String source, int limit) {
        return alertHistory.stream()
                .filter(a -> a.source.equals(source))
                .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取告警统计
     */
    public AlertStatistics getAlertStatistics() {
        AlertStatistics stats = new AlertStatistics();

        stats.totalAlerts = alertHistory.size();
        stats.unresolvedAlerts = (int) alertHistory.stream()
                .filter(a -> !a.resolved)
                .count();

        stats.criticalCount = (int) alertHistory.stream()
                .filter(a -> "CRITICAL".equals(a.severity))
                .count();
        stats.highCount = (int) alertHistory.stream()
                .filter(a -> "HIGH".equals(a.severity))
                .count();
        stats.mediumCount = (int) alertHistory.stream()
                .filter(a -> "MEDIUM".equals(a.severity))
                .count();
        stats.lowCount = (int) alertHistory.stream()
                .filter(a -> "LOW".equals(a.severity))
                .count();

        // 计算每分钟告警率
        if (!alertHistory.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            long alertsLastMinute = alertHistory.stream()
                    .filter(a -> a.timestamp.isAfter(now.minusMinutes(1)))
                    .count();
            stats.alertRate = alertsLastMinute;

            // 最后告警时间
            stats.lastAlertTime = alertHistory.get(alertHistory.size() - 1).timestamp;
        }

        return stats;
    }

    /**
     * 清除已解决的告警
     */
    public int clearResolvedAlerts() {
        int beforeSize = alertHistory.size();
        alertHistory.removeIf(alert -> alert.resolved);
        int removed = beforeSize - alertHistory.size();
        if (removed > 0) {
            logger.info("✓ 已清除 " + removed + " 个已解决的告警");
        }
        return removed;
    }

    /**
     * 清除过期告警
     */
    public int clearOldAlerts(long ageMillis) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(ageMillis / 1000);
        int beforeSize = alertHistory.size();
        alertHistory.removeIf(a -> a.timestamp.isBefore(cutoffTime));
        int removed = beforeSize - alertHistory.size();
        if (removed > 0) {
            logger.info("✓ 已清除 " + removed + " 个过期告警");
        }
        return removed;
    }

    /**
     * 重置系统
     */
    public void reset() {
        alertHistory.clear();
        lastAlertTime.clear();
        logger.info("✓ 告警系统已重置");
    }

    /**
     * 获取系统概览
     */
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        AlertStatistics stats = getAlertStatistics();

        overview.put("totalAlerts", stats.totalAlerts);
        overview.put("activeAlerts", stats.unresolvedAlerts);
        overview.put("criticalCount", stats.criticalCount);
        overview.put("highCount", stats.highCount);
        overview.put("mediumCount", stats.mediumCount);
        overview.put("lowCount", stats.lowCount);
        overview.put("alertRate", String.format("%.1f/分钟", stats.alertRate));
        overview.put("lastAlert", stats.lastAlertTime != null ?
                stats.lastAlertTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "无");
        overview.put("enabledRules", alertRules.values().stream()
                .filter(r -> r.enabled)
                .count());

        return overview;
    }
}
