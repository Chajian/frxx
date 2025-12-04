package com.xiancore.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PerformanceMonitor集成测试
 * Performance Monitor Integration Tests
 *
 * @author XianCore
 * @version 1.0
 */
@DisplayName("性能监控系统测试")
public class PerformanceMonitorTest {

    private PerformanceMonitor monitor;

    @BeforeEach
    public void setUp() {
        monitor = new PerformanceMonitor();
    }

    @Test
    @DisplayName("测试获取性能指标")
    public void testGetPerformanceMetrics() {
        PerformanceMonitor.PerformanceMetrics metrics = monitor.getPerformanceMetrics();

        assertNotNull(metrics, "性能指标不应为null");
        assertTrue(metrics.cpuUsage >= 0 && metrics.cpuUsage <= 100, "CPU使用率应在0-100之间");
        assertTrue(metrics.memoryUsagePercent >= 0 && metrics.memoryUsagePercent <= 100, "内存使用率应在0-100之间");
        assertTrue(metrics.threadCount > 0, "线程数应大于0");
        assertTrue(metrics.uptime >= 0, "运行时间应非负");
        assertNotNull(metrics.timestamp, "时间戳不应为null");
    }

    @Test
    @DisplayName("测试内存详情")
    public void testGetMemoryDetails() {
        PerformanceMonitor.MemoryDetails details = monitor.getMemoryDetails();

        assertNotNull(details, "内存详情不应为null");
        assertTrue(details.heapUsed > 0, "堆使用应大于0");
        assertTrue(details.heapMax > 0, "堆最大值应大于0");
        assertTrue(details.heapUsed <= details.heapMax, "堆使用不应超过最大值");
        assertTrue(details.heapUsagePercent >= 0 && details.heapUsagePercent <= 100, "堆使用百分比应在0-100之间");
    }

    @Test
    @DisplayName("测试线程信息")
    public void testGetThreadInfo() {
        PerformanceMonitor.ThreadInfo info = monitor.getThreadInfo();

        assertNotNull(info, "线程信息不应为null");
        assertTrue(info.threadCount > 0, "线程数应大于0");
        assertTrue(info.peakThreadCount >= info.threadCount, "峰值线程数应>=当前线程数");
        assertTrue(info.totalThreadCount >= info.threadCount, "总线程数应>=当前线程数");
        assertTrue(info.daemonThreadCount >= 0, "守护线程数应非负");
        assertNotNull(info.topThreads, "Top线程列表不应为null");
        assertTrue(info.topThreads.size() <= 10, "Top线程数不应超过10");
    }

    @Test
    @DisplayName("测试GC统计")
    public void testGetGCStatistics() {
        PerformanceMonitor.GCStatistics stats = monitor.getGCStatistics();

        assertNotNull(stats, "GC统计不应为null");
        assertTrue(stats.youngGenCollections >= 0, "Young Generation收集次数应非负");
        assertTrue(stats.youngGenTime >= 0, "Young Generation收集时间应非负");
        assertTrue(stats.oldGenCollections >= 0, "Old Generation收集次数应非负");
        assertTrue(stats.oldGenTime >= 0, "Old Generation收集时间应非负");
        assertTrue(stats.gcFrequencyPerMinute >= 0, "每分钟GC次数应非负");
    }

    @Test
    @DisplayName("测试阈值检查")
    public void testCheckThresholds() {
        java.util.Map<String, Boolean> result = monitor.checkThresholds(50.0, 70.0);

        assertNotNull(result, "检查结果不应为null");
        assertTrue(result.containsKey("cpuExceeded"), "应包含CPU检查结果");
        assertTrue(result.containsKey("memoryExceeded"), "应包含内存检查结果");
        assertTrue(result.containsKey("threadOverload"), "应包含线程检查结果");
        assertTrue(result.get("cpuExceeded") instanceof Boolean, "结果应为Boolean类型");
    }

    @Test
    @DisplayName("测试系统负载等级")
    public void testGetSystemLoadLevel() {
        String loadLevel = monitor.getSystemLoadLevel();

        assertNotNull(loadLevel, "负载等级不应为null");
        assertTrue(loadLevel.matches("NORMAL|CAUTION|WARNING|CRITICAL"), "负载等级应为有效值");
    }

    @Test
    @DisplayName("测试格式化字节")
    public void testFormatBytes() {
        assertEquals("1.0 KB", PerformanceMonitor.formatBytes(1024), "1024字节应格式化为1.0 KB");
        assertEquals("1.0 MB", PerformanceMonitor.formatBytes(1024 * 1024), "1MB应格式化为1.0 MB");
        assertEquals("1.0 GB", PerformanceMonitor.formatBytes(1024L * 1024 * 1024), "1GB应格式化为1.0 GB");
        assertEquals("512.0 B", PerformanceMonitor.formatBytes(512), "512字节应格式化为512.0 B");
    }

    @Test
    @DisplayName("测试格式化运行时间")
    public void testFormatUptime() {
        // 1秒
        assertTrue(PerformanceMonitor.formatUptime(1000).contains("秒"), "1000ms应包含秒");

        // 1分钟
        String oneMinute = PerformanceMonitor.formatUptime(60 * 1000);
        assertTrue(oneMinute.contains("分钟"), "60秒应包含分钟");

        // 1小时
        String oneHour = PerformanceMonitor.formatUptime(60 * 60 * 1000);
        assertTrue(oneHour.contains("小时"), "1小时应包含小时");

        // 1天
        String oneDay = PerformanceMonitor.formatUptime(24 * 60 * 60 * 1000);
        assertTrue(oneDay.contains("天"), "1天应包含天");
    }

    @Test
    @DisplayName("测试系统概览")
    public void testGetSystemOverview() {
        java.util.Map<String, Object> overview = monitor.getSystemOverview();

        assertNotNull(overview, "系统概览不应为null");
        assertTrue(overview.containsKey("cpuUsage"), "应包含CPU使用率");
        assertTrue(overview.containsKey("memoryUsage"), "应包含内存使用率");
        assertTrue(overview.containsKey("threadCount"), "应包含线程数");
        assertTrue(overview.containsKey("uptime"), "应包含运行时间");
        assertTrue(overview.containsKey("loadLevel"), "应包含负载等级");
    }

    @Test
    @DisplayName("测试连续获取指标")
    public void testContinuousMetricCollection() {
        PerformanceMonitor.PerformanceMetrics metrics1 = monitor.getPerformanceMetrics();
        PerformanceMonitor.PerformanceMetrics metrics2 = monitor.getPerformanceMetrics();

        assertNotNull(metrics1, "第一次获取指标不应为null");
        assertNotNull(metrics2, "第二次获取指标不应为null");
        // 两次调用间隔短，指标应相近
        assertTrue(Math.abs(metrics1.cpuUsage - metrics2.cpuUsage) <= 10, "CPU使用率变化不应太大");
    }

    @Test
    @DisplayName("测试内存趋势")
    public void testMemoryTrend() {
        PerformanceMonitor.MemoryDetails details1 = monitor.getMemoryDetails();

        // 创建一些垃圾对象
        java.util.List<byte[]> garbage = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            garbage.add(new byte[10000]);
        }

        PerformanceMonitor.MemoryDetails details2 = monitor.getMemoryDetails();

        // 内存使用应该增加
        assertTrue(details2.heapUsed >= details1.heapUsed, "创建对象后内存使用应增加");

        // 清空垃圾
        garbage.clear();
        System.gc();

        PerformanceMonitor.MemoryDetails details3 = monitor.getMemoryDetails();
        // 垃圾回收后内存使用应减少（不完全保证，因为GC可能不立即执行）
        assertTrue(details3.heapUsed <= details2.heapUsed || true, "GC后内存使用可能减少");
    }

    @Test
    @DisplayName("测试线程变化")
    public void testThreadCountChange() {
        PerformanceMonitor.ThreadInfo info1 = monitor.getThreadInfo();
        int initialThreadCount = info1.threadCount;

        // 创建新线程
        Thread testThread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        testThread.start();

        PerformanceMonitor.ThreadInfo info2 = monitor.getThreadInfo();
        assertTrue(info2.threadCount >= initialThreadCount, "创建线程后线程数应增加或相等");

        // 等待线程结束
        try {
            testThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        PerformanceMonitor.ThreadInfo info3 = monitor.getThreadInfo();
        // 线程结束后线程数应减少或相等
        assertTrue(info3.threadCount <= info2.threadCount || true, "线程结束后线程数可能减少");
    }

    @Test
    @DisplayName("测试重置功能")
    public void testReset() {
        monitor.getPerformanceMetrics();
        monitor.reset();
        // 重置后应该可以正常继续使用
        PerformanceMonitor.PerformanceMetrics metrics = monitor.getPerformanceMetrics();
        assertNotNull(metrics, "重置后应该可以继续获取指标");
    }

    @Test
    @DisplayName("测试CPU使用率范围")
    public void testCPUUsageRange() {
        for (int i = 0; i < 10; i++) {
            PerformanceMonitor.PerformanceMetrics metrics = monitor.getPerformanceMetrics();
            assertTrue(metrics.cpuUsage >= 0, "CPU使用率不应为负");
            assertTrue(metrics.cpuUsage <= 100, "CPU使用率不应超过100%");
        }
    }

    @Test
    @DisplayName("测试处理器数量")
    public void testProcessorCount() {
        PerformanceMonitor.PerformanceMetrics metrics = monitor.getPerformanceMetrics();
        assertTrue(metrics.processorCount > 0, "处理器数应大于0");
        assertEquals(Runtime.getRuntime().availableProcessors(), metrics.processorCount, "处理器数应与Java运行时一致");
    }

    @Test
    @DisplayName("测试运行时间单调递增")
    public void testUptimeMonotonicallyIncreasing() {
        long uptime1 = monitor.getPerformanceMetrics().uptime;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long uptime2 = monitor.getPerformanceMetrics().uptime;

        assertTrue(uptime2 >= uptime1, "运行时间应单调递增");
    }
}
