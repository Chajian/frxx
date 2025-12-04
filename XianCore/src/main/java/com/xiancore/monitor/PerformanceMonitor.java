package com.xiancore.monitor;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 性能监控系统 - 实时收集系统性能指标
 * Performance Monitor - Collect system performance metrics in real-time
 *
 * @author XianCore
 * @version 1.0
 */
public class PerformanceMonitor {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    private final Map<String, Long> lastMemoryCheck = new ConcurrentHashMap<>();
    private final Map<String, Double> metricCache = new ConcurrentHashMap<>();
    private volatile long startTime = System.currentTimeMillis();
    private volatile int peakThreadCount = 0;

    /**
     * 性能指标数据类
     */
    public static class PerformanceMetrics {
        public double cpuUsage;              // CPU使用率 (0-100)
        public double memoryUsagePercent;    // 内存使用百分比 (0-100)
        public long memoryUsedMB;            // 使用的内存 (MB)
        public long memoryMaxMB;             // 最大内存 (MB)
        public int threadCount;              // 当前线程数
        public int peakThreadCount;          // 峰值线程数
        public long processorCount;          // 处理器数
        public double systemLoadAverage;     // 系统平均负载
        public long uptime;                  // 运行时间 (毫秒)
        public long timestamp;               // 时间戳

        public PerformanceMetrics() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 内存详细信息
     */
    public static class MemoryDetails {
        public long heapUsed;                // Heap已使用 (字节)
        public long heapMax;                 // Heap最大值 (字节)
        public long nonHeapUsed;             // Non-Heap已使用 (字节)
        public long nonHeapMax;              // Non-Heap最大值 (字节)
        public double heapUsagePercent;      // Heap使用百分比
        public long timestamp;

        public MemoryDetails() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 线程信息
     */
    public static class ThreadInfo {
        public int threadCount;              // 当前线程数
        public int peakThreadCount;          // 峰值线程数
        public long totalThreadCount;        // 总线程数
        public int daemonThreadCount;        // 守护线程数
        public List<String> topThreads;      // 前10个最耗时的线程
        public long timestamp;

        public ThreadInfo() {
            this.topThreads = new ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 垃圾回收统计
     */
    public static class GCStatistics {
        public long youngGenCollections;     // Young Generation收集次数
        public long youngGenTime;            // Young Generation收集时间 (ms)
        public long oldGenCollections;       // Old Generation收集次数
        public long oldGenTime;              // Old Generation收集时间 (ms)
        public double gcFrequencyPerMinute;  // 每分钟GC次数
        public long timestamp;

        public GCStatistics() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * 构造函数
     */
    public PerformanceMonitor() {
        this.startTime = System.currentTimeMillis();
        logger.info("✓ PerformanceMonitor已初始化");
    }

    /**
     * 获取完整的性能指标
     */
    public PerformanceMetrics getPerformanceMetrics() {
        PerformanceMetrics metrics = new PerformanceMetrics();

        try {
            // CPU使用率 (系统平均负载)
            double loadAverage = osBean.getSystemLoadAverage();
            metrics.cpuUsage = Math.max(0, loadAverage * 100 / osBean.getAvailableProcessors());
            if (metrics.cpuUsage > 0) {
                metricCache.put("cpu", metrics.cpuUsage);
            }

            // 内存信息
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            metrics.memoryUsedMB = heapUsage.getUsed() / (1024 * 1024);
            metrics.memoryMaxMB = heapUsage.getMax() / (1024 * 1024);
            metrics.memoryUsagePercent = (heapUsage.getUsed() * 100.0) / heapUsage.getMax();

            // 线程信息
            metrics.threadCount = threadBean.getThreadCount();
            metrics.peakThreadCount = threadBean.getPeakThreadCount();
            if (metrics.threadCount > peakThreadCount) {
                peakThreadCount = metrics.threadCount;
            }

            // 处理器和负载信息
            metrics.processorCount = osBean.getAvailableProcessors();
            metrics.systemLoadAverage = osBean.getSystemLoadAverage();

            // 运行时间
            metrics.uptime = runtimeBean.getUptime();

            logger.info("✓ 性能指标已更新: CPU=" + String.format("%.1f", metrics.cpuUsage) +
                       "% Memory=" + metrics.memoryUsedMB + "MB/" + metrics.memoryMaxMB + "MB");

        } catch (Exception e) {
            logger.severe("✗ 获取性能指标失败: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * 获取详细的内存信息
     */
    public MemoryDetails getMemoryDetails() {
        MemoryDetails details = new MemoryDetails();

        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

            details.heapUsed = heapUsage.getUsed();
            details.heapMax = heapUsage.getMax();
            details.nonHeapUsed = nonHeapUsage.getUsed();
            details.nonHeapMax = nonHeapUsage.getMax();
            details.heapUsagePercent = (heapUsage.getUsed() * 100.0) / heapUsage.getMax();

        } catch (Exception e) {
            logger.severe("✗ 获取内存详情失败: " + e.getMessage());
        }

        return details;
    }

    /**
     * 获取线程信息
     */
    public ThreadInfo getThreadInfo() {
        ThreadInfo info = new ThreadInfo();

        try {
            info.threadCount = threadBean.getThreadCount();
            info.peakThreadCount = threadBean.getPeakThreadCount();
            info.totalThreadCount = threadBean.getTotalStartedThreadCount();
            info.daemonThreadCount = threadBean.getDaemonThreadCount();

            // 获取前10个最耗时的线程
            long[] threadIds = threadBean.getAllThreadIds();
            List<java.lang.management.ThreadInfo> threadInfoList = new ArrayList<>();

            for (long threadId : threadIds) {
                java.lang.management.ThreadInfo ti = threadBean.getThreadInfo(threadId);
                if (ti != null) {
                    threadInfoList.add(ti);
                }
            }

            // 按CPU时间排序 (需要使用 com.sun.management.ThreadMXBean 来获取CPU时间)
            // 暂时使用线程名称排序，实际应使用CPU时间排序
            threadInfoList.sort((a, b) -> a.getThreadName().compareTo(b.getThreadName()));

            // 取前10个
            int limit = Math.min(10, threadInfoList.size());
            for (int i = 0; i < limit; i++) {
                java.lang.management.ThreadInfo ti = threadInfoList.get(i);
                info.topThreads.add(ti.getThreadName() + " (" + ti.getThreadState() + ")");
            }

        } catch (Exception e) {
            logger.severe("✗ 获取线程信息失败: " + e.getMessage());
        }

        return info;
    }

    /**
     * 获取垃圾回收统计
     */
    public GCStatistics getGCStatistics() {
        GCStatistics stats = new GCStatistics();

        try {
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

            for (GarbageCollectorMXBean gcBean : gcBeans) {
                String gcName = gcBean.getName().toLowerCase();

                if (gcName.contains("young") || gcName.contains("copy") || gcName.contains("scavenge")) {
                    stats.youngGenCollections = gcBean.getCollectionCount();
                    stats.youngGenTime = gcBean.getCollectionTime();
                } else if (gcName.contains("old") || gcName.contains("mark") || gcName.contains("concurrent")) {
                    stats.oldGenCollections = gcBean.getCollectionCount();
                    stats.oldGenTime = gcBean.getCollectionTime();
                }
            }

            // 计算每分钟GC次数
            long totalGCCount = stats.youngGenCollections + stats.oldGenCollections;
            long uptimeMinutes = Math.max(1, runtimeBean.getUptime() / 60000);
            stats.gcFrequencyPerMinute = totalGCCount / (double) uptimeMinutes;

        } catch (Exception e) {
            logger.severe("✗ 获取GC统计失败: " + e.getMessage());
        }

        return stats;
    }

    /**
     * 检查是否超过阈值
     */
    public Map<String, Boolean> checkThresholds(double cpuThreshold, double memoryThreshold) {
        Map<String, Boolean> result = new HashMap<>();

        PerformanceMetrics metrics = getPerformanceMetrics();

        result.put("cpuExceeded", metrics.cpuUsage > cpuThreshold);
        result.put("memoryExceeded", metrics.memoryUsagePercent > memoryThreshold);
        result.put("threadOverload", metrics.threadCount > 200);  // 固定阈值200

        return result;
    }

    /**
     * 获取系统负载等级
     */
    public String getSystemLoadLevel() {
        PerformanceMetrics metrics = getPerformanceMetrics();

        if (metrics.cpuUsage > 80 || metrics.memoryUsagePercent > 85) {
            return "CRITICAL";
        } else if (metrics.cpuUsage > 60 || metrics.memoryUsagePercent > 70) {
            return "WARNING";
        } else if (metrics.cpuUsage > 40 || metrics.memoryUsagePercent > 50) {
            return "CAUTION";
        } else {
            return "NORMAL";
        }
    }

    /**
     * 格式化字节为可读格式
     */
    public static String formatBytes(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * 格式化时间
     */
    public static String formatUptime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "天" + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时" + (minutes % 60) + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟" + (seconds % 60) + "秒";
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 获取系统概览
     */
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        PerformanceMetrics metrics = getPerformanceMetrics();

        overview.put("cpuUsage", String.format("%.1f%%", metrics.cpuUsage));
        overview.put("memoryUsage", String.format("%.1f%%", metrics.memoryUsagePercent));
        overview.put("memoryInfo", metrics.memoryUsedMB + "MB / " + metrics.memoryMaxMB + "MB");
        overview.put("threadCount", metrics.threadCount);
        overview.put("peakThreadCount", metrics.peakThreadCount);
        overview.put("systemLoad", String.format("%.2f", metrics.systemLoadAverage));
        overview.put("uptime", formatUptime(metrics.uptime));
        overview.put("loadLevel", getSystemLoadLevel());

        return overview;
    }

    /**
     * 重置监控数据
     */
    public void reset() {
        lastMemoryCheck.clear();
        metricCache.clear();
        startTime = System.currentTimeMillis();
        peakThreadCount = 0;
        logger.info("✓ 监控数据已重置");
    }
}
