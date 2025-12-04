package com.xiancore.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlertSystem集成测试
 * Alert System Integration Tests
 *
 * @author XianCore
 * @version 1.0
 */
@DisplayName("告警系统测试")
public class AlertSystemTest {

    private AlertSystem alertSystem;

    @BeforeEach
    public void setUp() {
        alertSystem = new AlertSystem();
    }

    @Test
    @DisplayName("测试CPU高告警")
    public void testCPUHighAlert() {
        AlertSystem.Alert alert = alertSystem.checkCPUAlert(85.0);
        assertNotNull(alert, "CPU 85%应触发告警");
        assertEquals("HIGH", alert.severity, "严重级别应为HIGH");
        assertEquals("CPU", alert.source, "来源应为CPU");
    }

    @Test
    @DisplayName("测试CPU严重告警")
    public void testCPUCriticalAlert() {
        AlertSystem.Alert alert = alertSystem.checkCPUAlert(95.0);
        assertNotNull(alert, "CPU 95%应触发告警");
        assertEquals("CRITICAL", alert.severity, "严重级别应为CRITICAL");
    }

    @Test
    @DisplayName("测试CPU正常范围")
    public void testCPUNormal() {
        AlertSystem.Alert alert1 = alertSystem.checkCPUAlert(50.0);
        AlertSystem.Alert alert2 = alertSystem.checkCPUAlert(70.0);
        assertNull(alert1, "CPU 50%不应触发告警");
        assertNull(alert2, "CPU 70%不应触发告警");
    }

    @Test
    @DisplayName("测试内存高告警")
    public void testMemoryHighAlert() {
        AlertSystem.Alert alert = alertSystem.checkMemoryAlert(85.0);
        assertNotNull(alert, "内存 85%应触发告警");
        assertEquals("HIGH", alert.severity, "严重级别应为HIGH");
        assertEquals("MEMORY", alert.source, "来源应为MEMORY");
    }

    @Test
    @DisplayName("测试内存严重告警")
    public void testMemoryCriticalAlert() {
        AlertSystem.Alert alert = alertSystem.checkMemoryAlert(95.0);
        assertNotNull(alert, "内存 95%应触发告警");
        assertEquals("CRITICAL", alert.severity, "严重级别应为CRITICAL");
    }

    @Test
    @DisplayName("测试线程高告警")
    public void testThreadHighAlert() {
        AlertSystem.Alert alert = alertSystem.checkThreadAlert(250);
        assertNotNull(alert, "线程数250应触发告警");
        assertEquals("MEDIUM", alert.severity, "严重级别应为MEDIUM");
        assertEquals("THREAD", alert.source, "来源应为THREAD");
    }

    @Test
    @DisplayName("测试线程严重告警")
    public void testThreadCriticalAlert() {
        AlertSystem.Alert alert = alertSystem.checkThreadAlert(350);
        assertNotNull(alert, "线程数350应触发告警");
        assertEquals("HIGH", alert.severity, "严重级别应为HIGH");
    }

    @Test
    @DisplayName("测试Boss低血量告警")
    public void testBossLowHealthAlert() {
        AlertSystem.Alert alert = alertSystem.checkBossAlert(5, 15.0);
        assertNotNull(alert, "Boss血量15%应触发告警");
        assertEquals("MEDIUM", alert.severity, "严重级别应为MEDIUM");
        assertEquals("BOSS", alert.source, "来源应为BOSS");
    }

    @Test
    @DisplayName("测试Boss过多告警")
    public void testBossTooManyAlert() {
        AlertSystem.Alert alert = alertSystem.checkBossAlert(12, 80.0);
        assertNotNull(alert, "活跃Boss 12应触发告警");
        assertEquals("MEDIUM", alert.severity, "严重级别应为MEDIUM");
    }

    @Test
    @DisplayName("测试默认规则已加载")
    public void testDefaultRulesLoaded() {
        Collection<AlertSystem.AlertRule> rules = alertSystem.getAllRules();
        assertEquals(8, rules.size(), "应有8个默认规则");
    }

    @Test
    @DisplayName("测试获取特定规则")
    public void testGetRule() {
        AlertSystem.AlertRule rule = alertSystem.getRule("cpu-high");
        assertNotNull(rule, "cpu-high规则应存在");
        assertEquals("cpu-high", rule.ruleId, "规则ID应匹配");
        assertTrue(rule.enabled, "默认规则应启用");
    }

    @Test
    @DisplayName("测试启用/禁用规则")
    public void testEnableDisableRule() {
        alertSystem.setRuleEnabled("cpu-high", false);
        AlertSystem.AlertRule rule = alertSystem.getRule("cpu-high");
        assertFalse(rule.enabled, "规则应被禁用");

        // 禁用后不应触发告警
        AlertSystem.Alert alert = alertSystem.checkCPUAlert(85.0);
        assertNull(alert, "禁用规则后不应生成告警");

        // 重新启用
        alertSystem.setRuleEnabled("cpu-high", true);
        alert = alertSystem.checkCPUAlert(85.0);
        assertNotNull(alert, "重新启用规则后应生成告警");
    }

    @Test
    @DisplayName("测试自定义规则")
    public void testAddCustomRule() {
        AlertSystem.AlertRule customRule = new AlertSystem.AlertRule(
                "custom-rule", "自定义规则", "Test > 100", "HIGH", 100.0, "TEST"
        );
        alertSystem.addRule(customRule);

        AlertSystem.AlertRule retrieved = alertSystem.getRule("custom-rule");
        assertNotNull(retrieved, "自定义规则应存在");
        assertEquals("自定义规则", retrieved.ruleName, "规则名称应匹配");
    }

    @Test
    @DisplayName("测试冷却期机制")
    public void testCooldownMechanism() throws InterruptedException {
        // 第一次触发告警
        AlertSystem.Alert alert1 = alertSystem.checkCPUAlert(85.0);
        assertNotNull(alert1, "第一次应触发告警");

        // 立即第二次触发
        AlertSystem.Alert alert2 = alertSystem.checkCPUAlert(85.0);
        assertNull(alert2, "冷却期内不应触发重复告警");

        // 可以通过修改冷却期来测试
        AlertSystem.AlertRule rule = alertSystem.getRule("cpu-high");
        rule.cooldownMs = 0;

        AlertSystem.Alert alert3 = alertSystem.checkCPUAlert(85.0);
        assertNotNull(alert3, "冷却期为0时应触发告警");
    }

    @Test
    @DisplayName("测试告警解决")
    public void testResolveAlert() {
        AlertSystem.Alert alert = alertSystem.checkCPUAlert(95.0);
        assertNotNull(alert, "应生成告警");
        assertFalse(alert.resolved, "告警初始状态应未解决");

        alertSystem.resolveAlert(alert.alertId);
        assertEquals(alert, alertSystem.getActiveAlerts().stream()
                .filter(a -> a.alertId.equals(alert.alertId))
                .findFirst()
                .orElse(null), "告警应在活跃告警中");
    }

    @Test
    @DisplayName("测试获取活跃告警")
    public void testGetActiveAlerts() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(95.0);
        alertSystem.checkMemoryAlert(95.0);

        List<AlertSystem.Alert> activeAlerts = alertSystem.getActiveAlerts();
        assertTrue(activeAlerts.size() >= 2, "应有至少2个活跃告警");
    }

    @Test
    @DisplayName("测试告警历史")
    public void testAlertHistory() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(95.0);
        alertSystem.checkMemoryAlert(95.0);
        alertSystem.checkThreadAlert(350);

        List<AlertSystem.Alert> history = alertSystem.getAlertHistory(10);
        assertTrue(history.size() >= 3, "历史应包含至少3个告警");
    }

    @Test
    @DisplayName("测试按来源查询告警")
    public void testGetAlertsBySource() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(95.0);
        alertSystem.checkMemoryAlert(95.0);
        alertSystem.checkThreadAlert(350);

        List<AlertSystem.Alert> cpuAlerts = alertSystem.getAlertsBySource("CPU", 10);
        List<AlertSystem.Alert> memoryAlerts = alertSystem.getAlertsBySource("MEMORY", 10);

        assertTrue(cpuAlerts.stream().allMatch(a -> "CPU".equals(a.source)), "所有CPU告警来源应为CPU");
        assertTrue(memoryAlerts.stream().allMatch(a -> "MEMORY".equals(a.source)), "所有内存告警来源应为MEMORY");
    }

    @Test
    @DisplayName("测试告警统计")
    public void testAlertStatistics() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(95.0);  // CRITICAL
        alertSystem.checkMemoryAlert(85.0); // HIGH
        alertSystem.checkThreadAlert(250);  // MEDIUM

        AlertSystem.AlertStatistics stats = alertSystem.getAlertStatistics();
        assertEquals(3, stats.totalAlerts, "总告警应为3");
        assertEquals(3, stats.unresolvedAlerts, "未解决告警应为3");
        assertTrue(stats.criticalCount >= 1, "严重告警应>=1");
        assertTrue(stats.highCount >= 1, "高级告警应>=1");
        assertTrue(stats.mediumCount >= 1, "中级告警应>=1");
    }

    @Test
    @DisplayName("测试自动告警解决")
    public void testAutoResolveAlerts() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(95.0); // 触发CRITICAL告警

        List<AlertSystem.Alert> before = alertSystem.getActiveAlerts();
        assertTrue(before.stream().anyMatch(a -> a.source.equals("CPU")), "应有CPU告警");

        // 自动解决告警（CPU降至60%，在80%阈值以下）
        alertSystem.autoResolveAlerts("CPU", 60.0, 80.0);

        List<AlertSystem.Alert> after = alertSystem.getActiveAlerts();
        assertFalse(after.stream().anyMatch(a -> a.source.equals("CPU")), "CPU告警应被解决");
    }

    @Test
    @DisplayName("测试清除已解决告警")
    public void testClearResolvedAlerts() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        AlertSystem.Alert alert = alertSystem.checkCPUAlert(95.0);
        alertSystem.resolveAlert(alert.alertId);

        int removed = alertSystem.clearResolvedAlerts();
        assertEquals(1, removed, "应清除1个已解决告警");

        List<AlertSystem.Alert> history = alertSystem.getAlertHistory(10);
        assertFalse(history.stream().anyMatch(a -> a.alertId.equals(alert.alertId)), "已解决告警应被清除");
    }

    @Test
    @DisplayName("测试清除过期告警")
    public void testClearOldAlerts() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(95.0);
        int removed = alertSystem.clearOldAlerts(-1000); // 清除所有
        assertTrue(removed > 0, "应清除过期告警");
    }

    @Test
    @DisplayName("测试告警排序")
    public void testAlertSorting() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(85.0);     // HIGH
        alertSystem.checkCPUAlert(95.0);     // CRITICAL
        alertSystem.checkThreadAlert(250);   // MEDIUM

        List<AlertSystem.Alert> activeAlerts = alertSystem.getActiveAlerts();
        // 应按严重级别排序（CRITICAL > HIGH > MEDIUM）
        if (activeAlerts.size() >= 2) {
            int criticalIndex = -1, highIndex = -1;
            for (int i = 0; i < activeAlerts.size(); i++) {
                if ("CRITICAL".equals(activeAlerts.get(i).severity)) {
                    criticalIndex = i;
                }
                if ("HIGH".equals(activeAlerts.get(i).severity)) {
                    highIndex = i;
                }
            }
            if (criticalIndex >= 0 && highIndex >= 0) {
                assertTrue(criticalIndex <= highIndex, "CRITICAL告警应排在HIGH之前");
            }
        }
    }

    @Test
    @DisplayName("测试重置系统")
    public void testReset() {
        alertSystem.checkCPUAlert(95.0);
        alertSystem.reset();

        List<AlertSystem.Alert> history = alertSystem.getAlertHistory(10);
        assertEquals(0, history.size(), "重置后告警历史应为空");
    }

    @Test
    @DisplayName("测试系统概览")
    public void testGetSystemOverview() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        alertSystem.checkCPUAlert(95.0);
        alertSystem.checkMemoryAlert(85.0);

        Map<String, Object> overview = alertSystem.getSystemOverview();
        assertTrue(overview.containsKey("totalAlerts"), "应包含总告警数");
        assertTrue(overview.containsKey("activeAlerts"), "应包含活跃告警数");
        assertTrue(overview.containsKey("criticalCount"), "应包含严重告警数");
        assertTrue(overview.containsKey("enabledRules"), "应包含启用规则数");
    }

    @Test
    @DisplayName("测试告警元数据")
    public void testAlertMetadata() {
        AlertSystem.Alert alert = alertSystem.checkCPUAlert(95.0);
        assertNotNull(alert, "应生成告警");
        assertNotNull(alert.metadata, "元数据不应为null");
        alert.metadata.put("additional_info", "test");
        assertEquals("test", alert.metadata.get("additional_info"), "元数据应可编辑");
    }

    @Test
    @DisplayName("测试大量告警生成")
    public void testManyAlertsGeneration() {
        // 清除冷却期
        for (AlertSystem.AlertRule rule : alertSystem.getAllRules()) {
            rule.cooldownMs = 0;
        }

        // 生成多个不同类型的告警
        for (int i = 0; i < 5; i++) {
            alertSystem.checkCPUAlert(95.0 - i);
        }

        List<AlertSystem.Alert> history = alertSystem.getAlertHistory(100);
        assertTrue(history.size() > 0, "应有多个告警");
    }

    @Test
    @DisplayName("测试告警冷却期配置")
    public void testCooldownConfiguration() {
        AlertSystem.AlertRule rule = alertSystem.getRule("cpu-high");
        assertEquals(60000, rule.cooldownMs, "默认冷却期应为60秒");

        rule.cooldownMs = 1000; // 改为1秒
        AlertSystem.Alert alert1 = alertSystem.checkCPUAlert(85.0);
        assertNotNull(alert1, "第一次应触发告警");

        AlertSystem.Alert alert2 = alertSystem.checkCPUAlert(85.0);
        assertNull(alert2, "1秒内第二次不应触发");

        // 重置冷却期
        rule.cooldownMs = 0;
        AlertSystem.Alert alert3 = alertSystem.checkCPUAlert(85.0);
        assertNotNull(alert3, "冷却期为0时应触发");
    }
}
