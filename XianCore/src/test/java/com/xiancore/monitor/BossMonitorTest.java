package com.xiancore.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BossMonitor集成测试
 * Boss Monitor Integration Tests
 *
 * @author XianCore
 * @version 1.0
 */
@DisplayName("Boss监控系统测试")
public class BossMonitorTest {

    private BossMonitor monitor;

    @BeforeEach
    public void setUp() {
        monitor = new BossMonitor();
    }

    @Test
    @DisplayName("测试记录Boss生成")
    public void testRecordBossSpawn() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertNotNull(record, "Boss记录不应为null");
        assertEquals("boss-1", record.bossId, "Boss ID应匹配");
        assertEquals("SkeletonKing", record.bossName, "Boss名称应匹配");
        assertEquals("SPAWNED", record.status, "初始状态应为SPAWNED");
        assertEquals(100.0, record.maxHealth, "最大血量应匹配");
        assertEquals(100.0, record.currentHealth, "初始血量应等于最大血量");
    }

    @Test
    @DisplayName("测试记录Boss伤害")
    public void testRecordBossDamage() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);

        monitor.recordBossDamage("boss-1", "Player1", 25.0);
        monitor.recordBossDamage("boss-1", "Player2", 15.0);

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertEquals(60.0, record.currentHealth, "剩余血量应为60.0");
        assertEquals("ACTIVE", record.status, "状态应变为ACTIVE");
        assertEquals(2, record.damageCount, "伤害次数应为2");
        assertEquals(40.0, record.totalDamageReceived, "总伤害应为40.0");
        assertTrue(record.damageContributors.containsKey("Player1"), "应记录Player1");
        assertTrue(record.damageContributors.containsKey("Player2"), "应记录Player2");
    }

    @Test
    @DisplayName("测试伤害贡献者排行")
    public void testDamageContributors() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);

        monitor.recordBossDamage("boss-1", "Player1", 50.0);
        monitor.recordBossDamage("boss-1", "Player2", 30.0);
        monitor.recordBossDamage("boss-1", "Player3", 20.0);

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        Map<String, Double> topContributors = record.getTopContributors(2);

        assertEquals(2, topContributors.size(), "Top 2应返回2个贡献者");
        List<Map.Entry<String, Double>> ranking = monitor.getDamageRanking("boss-1", 3);
        assertEquals("Player1", ranking.get(0).getKey(), "第一名应是Player1");
        assertEquals("Player2", ranking.get(1).getKey(), "第二名应是Player2");
        assertEquals("Player3", ranking.get(2).getKey(), "第三名应是Player3");
    }

    @Test
    @DisplayName("测试Boss死亡")
    public void testRecordBossDeath() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossDamage("boss-1", "Player1", 100.0);
        monitor.recordBossDeath("boss-1", "Player1");

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertEquals("DEAD", record.status, "状态应为DEAD");
        assertEquals(0.0, record.currentHealth, "血量应为0");
    }

    @Test
    @DisplayName("测试Boss消失")
    public void testRecordBossDespawn() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossDespawn("boss-1");

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertEquals("DESPAWNED", record.status, "状态应为DESPAWNED");
    }

    @Test
    @DisplayName("测试血量百分比")
    public void testHealthPercent() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertEquals(100.0, record.getHealthPercent(), "满血时血量百分比应为100%");

        monitor.recordBossDamage("boss-1", "Player1", 50.0);
        assertEquals(50.0, record.getHealthPercent(), "受50点伤害后血量百分比应为50%");

        monitor.recordBossDamage("boss-1", "Player1", 50.0);
        assertEquals(0.0, record.getHealthPercent(), "完全失血时血量百分比应为0%");
    }

    @Test
    @DisplayName("测试获取活跃Boss")
    public void testGetActiveBosses() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "world", 200, 70, 200, 2, 200.0);
        monitor.recordBossDamage("boss-2", "Player1", 200.0); // 导致boss-2死亡
        monitor.recordBossDeath("boss-2", "Player1");

        List<BossMonitor.BossRecord> activeBosses = monitor.getActiveBosses();
        assertEquals(1, activeBosses.size(), "应有1个活跃Boss");
        assertEquals("boss-1", activeBosses.get(0).bossId, "活跃Boss应为boss-1");
    }

    @Test
    @DisplayName("测试按世界查询")
    public void testGetBossesByWorld() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "nether", 200, 70, 200, 2, 200.0);

        List<BossMonitor.BossRecord> worldBosses = monitor.getBossesByWorld("world");
        List<BossMonitor.BossRecord> netherBosses = monitor.getBossesByWorld("nether");

        assertEquals(1, worldBosses.size(), "world中应有1个Boss");
        assertEquals(1, netherBosses.size(), "nether中应有1个Boss");
        assertEquals("boss-1", worldBosses.get(0).bossId, "world Boss应为boss-1");
        assertEquals("boss-2", netherBosses.get(0).bossId, "nether Boss应为boss-2");
    }

    @Test
    @DisplayName("测试按Tier查询")
    public void testGetBossesByTier() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "world", 200, 70, 200, 2, 200.0);
        monitor.recordBossSpawn("boss-3", "Dragon", "Dragon",
                "world", 300, 100, 300, 1, 150.0);

        List<BossMonitor.BossRecord> tier1Bosses = monitor.getBossesByTier(1);
        List<BossMonitor.BossRecord> tier2Bosses = monitor.getBossesByTier(2);

        assertEquals(2, tier1Bosses.size(), "Tier 1应有2个Boss");
        assertEquals(1, tier2Bosses.size(), "Tier 2应有1个Boss");
    }

    @Test
    @DisplayName("测试低血量Boss检测")
    public void testLowHealthBosses() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "world", 200, 70, 200, 2, 200.0);

        monitor.recordBossDamage("boss-1", "Player1", 85.0); // 15%血量
        monitor.recordBossDamage("boss-2", "Player1", 150.0); // 25%血量

        List<BossMonitor.BossRecord> lowHealth = monitor.getLowHealthBosses(20.0);
        assertEquals(1, lowHealth.size(), "应检测到1个低血量Boss");
        assertEquals("boss-1", lowHealth.get(0).bossId, "低血量Boss应为boss-1");
    }

    @Test
    @DisplayName("测试事件历史")
    public void testEventHistory() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossDamage("boss-1", "Player1", 25.0);
        monitor.recordBossDamage("boss-1", "Player2", 25.0);
        monitor.recordBossDeath("boss-1", "Player1");

        List<BossMonitor.BossEvent> events = monitor.getEventHistory(10);
        assertEquals(4, events.size(), "应记录4个事件");
        assertEquals("SPAWNED", events.get(0).eventType, "第一个事件应为SPAWNED");
        assertEquals("DAMAGE", events.get(1).eventType, "第二个事件应为DAMAGE");
        assertEquals("DEAD", events.get(3).eventType, "最后一个事件应为DEAD");
    }

    @Test
    @DisplayName("测试Boss特定事件")
    public void testBossSpecificEvents() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "world", 200, 70, 200, 2, 200.0);
        monitor.recordBossDamage("boss-1", "Player1", 10.0);
        monitor.recordBossDamage("boss-2", "Player2", 20.0);

        List<BossMonitor.BossEvent> boss1Events = monitor.getBossEvents("boss-1", 10);
        List<BossMonitor.BossEvent> boss2Events = monitor.getBossEvents("boss-2", 10);

        assertEquals(2, boss1Events.size(), "boss-1应有2个事件");
        assertEquals(2, boss2Events.size(), "boss-2应有2个事件");
    }

    @Test
    @DisplayName("测试Boss统计")
    public void testBossStatistics() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "world", 200, 70, 200, 2, 200.0);
        monitor.recordBossDamage("boss-1", "Player1", 100.0);
        monitor.recordBossDeath("boss-1", "Player1");
        monitor.recordBossDespawn("boss-2");

        BossMonitor.BossStatistics stats = monitor.getBossStatistics();
        assertEquals(2, stats.totalBossesSpawned, "总刷新数应为2");
        assertEquals(0, stats.activeBossCount, "活跃Boss应为0");
        assertEquals(1, stats.deadBossCount, "已死Boss应为1");
        assertEquals(1, stats.despawnedBossCount, "消失Boss应为1");
        assertTrue(stats.averageAliveTime > 0, "平均存活时间应大于0");
    }

    @Test
    @DisplayName("测试死亡Boss查询")
    public void testGetDeadBosses() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "world", 200, 70, 200, 2, 200.0);

        monitor.recordBossDamage("boss-1", "Player1", 100.0);
        monitor.recordBossDeath("boss-1", "Player1");

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        monitor.recordBossDamage("boss-2", "Player2", 200.0);
        monitor.recordBossDeath("boss-2", "Player2");

        List<BossMonitor.BossRecord> deadBosses = monitor.getDeadBosses(1);
        assertEquals(1, deadBosses.size(), "应返回1个死亡Boss");
        assertEquals("boss-2", deadBosses.get(0).bossId, "最近死亡Boss应为boss-2");
    }

    @Test
    @DisplayName("测试存活时间计算")
    public void testAliveTimeCalculation() throws InterruptedException {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        long aliveTime1 = record.getAliveTime();

        Thread.sleep(100);

        long aliveTime2 = record.getAliveTime();
        assertTrue(aliveTime2 > aliveTime1, "存活时间应逐渐增加");
    }

    @Test
    @DisplayName("测试数据清理")
    public void testCleanupOldData() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossDamage("boss-1", "Player1", 100.0);
        monitor.recordBossDeath("boss-1", "Player1");

        // 清理所有过期数据（包括刚创建的）
        monitor.cleanupOldData(-1);

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertNull(record, "过期Boss记录应被清理");
    }

    @Test
    @DisplayName("测试重置功能")
    public void testReset() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.reset();

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertNull(record, "重置后Boss应被清除");

        List<BossMonitor.BossEvent> events = monitor.getEventHistory(10);
        assertEquals(0, events.size(), "重置后事件历史应为空");
    }

    @Test
    @DisplayName("测试监控概览")
    public void testGetMonitorOverview() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossSpawn("boss-2", "FrostGiant", "FrostGiant",
                "world", 200, 70, 200, 2, 200.0);

        Map<String, Object> overview = monitor.getMonitorOverview();
        assertTrue(overview.containsKey("totalBosses"), "应包含总Boss数");
        assertTrue(overview.containsKey("activeBosses"), "应包含活跃Boss数");
        assertTrue(overview.containsKey("deadBosses"), "应包含已死Boss数");
        assertTrue(overview.containsKey("averageAliveTime"), "应包含平均存活时间");
        assertTrue(overview.containsKey("totalDamage"), "应包含总伤害");
    }

    @Test
    @DisplayName("测试伤害不能负数")
    public void testDamageNeverNegative() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);
        monitor.recordBossDamage("boss-1", "Player1", 100.0);
        monitor.recordBossDamage("boss-1", "Player1", 50.0); // 超额伤害

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertEquals(0.0, record.currentHealth, "血量不应变为负数");
    }

    @Test
    @DisplayName("测试大量伤害事件")
    public void testManyDamageEvents() {
        monitor.recordBossSpawn("boss-1", "SkeletonKing", "SkeletonKing",
                "world", 100, 64, 100, 1, 100.0);

        // 模拟100次伤害事件
        for (int i = 0; i < 100; i++) {
            monitor.recordBossDamage("boss-1", "Player" + (i % 10), 1.0);
        }

        BossMonitor.BossRecord record = monitor.getBossRecord("boss-1");
        assertEquals(0.0, record.currentHealth, "经过100次1点伤害后血量应为0");
        assertEquals(100, record.damageCount, "伤害次数应为100");
    }
}
