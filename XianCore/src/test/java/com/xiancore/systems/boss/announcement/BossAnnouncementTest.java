package com.xiancore.systems.boss.announcement;

import com.xiancore.boss.system.model.BossTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boss全服公告系统单元测试
 * 测试公告创建、格式化、调度、管理功能
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@DisplayName("Boss全服公告系统单元测试")
public class BossAnnouncementTest {

    private BossAnnouncement announcement;
    private AnnouncementFormatter formatter;
    private AnnouncementScheduler scheduler;
    private BossAnnouncementManager manager;

    private UUID testBossUUID;
    private UUID testPlayerUUID;

    @BeforeEach
    public void setUp() {
        formatter = new AnnouncementFormatter();
        scheduler = new AnnouncementScheduler();
        manager = new BossAnnouncementManager();
        manager.initialize();

        testBossUUID = UUID.randomUUID();
        testPlayerUUID = UUID.randomUUID();
    }

    // ==================== BossAnnouncement 测试 ====================

    @Test
    @DisplayName("测试公告创建")
    public void testAnnouncementCreation() {
        announcement = new BossAnnouncement(
            testBossUUID, "SkeletonKing", "SkeletonKing",
            BossTier.ELITE, null, 3
        );

        assertNotNull(announcement, "公告应该被成功创建");
        assertEquals(BossAnnouncement.AnnouncementType.SPAWN, announcement.getType());
        assertEquals(testBossUUID, announcement.getBossUUID());
        assertFalse(announcement.isSent(), "新创建的公告应该未发送");
    }

    @Test
    @DisplayName("测试公告优先级")
    public void testAnnouncementPriority() {
        // 传奇Boss优先级最高
        announcement = new BossAnnouncement(
            testBossUUID, "FrostDragon", "FrostDragon",
            BossTier.LEGENDARY, null, 5
        );
        assertEquals(BossAnnouncement.AnnouncementPriority.CRITICAL, announcement.getPriority());

        // 普通Boss优先级低
        announcement = new BossAnnouncement(
            testBossUUID, "Zombie", "Zombie",
            BossTier.NORMAL, null, 1
        );
        assertEquals(BossAnnouncement.AnnouncementPriority.LOW, announcement.getPriority());
    }

    @Test
    @DisplayName("测试公告冷却")
    public void testAnnouncementCooldown() {
        announcement = new BossAnnouncement();
        announcement.setCooldownSeconds(5);
        announcement.markAsSent();

        assertTrue(announcement.isInCooldown(), "新发送的公告应该在冷却中");
        long remaining = announcement.getRemainingCooldown();
        assertTrue(remaining > 0 && remaining <= 5, "剩余冷却时间应该在0-5秒之间");
    }

    @Test
    @DisplayName("测试公告聚合")
    public void testAnnouncementAggregation() {
        BossAnnouncement ann1 = new BossAnnouncement();
        ann1.setBossUUID(testBossUUID);
        ann1.setType(BossAnnouncement.AnnouncementType.SPAWN);
        ann1.setCreatedTime(System.currentTimeMillis());

        BossAnnouncement ann2 = new BossAnnouncement();
        ann2.setBossUUID(testBossUUID);
        ann2.setType(BossAnnouncement.AnnouncementType.SPAWN);
        ann2.setCreatedTime(System.currentTimeMillis() + 500);

        assertTrue(ann1.shouldAggregatWith(ann2), "同Boss的相同类型公告应该可以聚合");
    }

    // ==================== AnnouncementFormatter 测试 ====================

    @Test
    @DisplayName("测试占位符替换")
    public void testPlaceholderReplacement() {
        String template = "Boss名称: {boss_name}, 位置: {x},{y},{z}";
        Map<String, String> params = new HashMap<>();
        params.put("boss_name", "SkeletonKing");
        params.put("x", "100");
        params.put("y", "64");
        params.put("z", "200");

        String result = formatter.replacePlaceholders(template, params);
        assertTrue(result.contains("SkeletonKing"), "应该包含Boss名称");
        assertTrue(result.contains("100"), "应该包含X坐标");
        assertFalse(result.contains("{"), "应该没有未替换的占位符");
    }

    @Test
    @DisplayName("测试颜色格式化")
    public void testColorFormatting() {
        String message = "&c红色 &a绿色 &e黄色";
        String result = formatter.applyColors(message);

        assertTrue(result.contains("§c"), "应该转换红色代码");
        assertTrue(result.contains("§a"), "应该转换绿色代码");
        assertTrue(result.contains("§e"), "应该转换黄色代码");
    }

    @Test
    @DisplayName("测试去色")
    public void testStripColors() {
        String message = formatter.applyColors("&c红色&a绿色");
        String stripped = formatter.stripColors(message);

        assertFalse(stripped.contains("§"), "应该移除所有颜色代码");
        assertTrue(stripped.contains("红色"), "应该保留文本内容");
    }

    @Test
    @DisplayName("测试文本截断")
    public void testTextTruncation() {
        String longText = "这是一个很长的文本用来测试文本截断功能";
        String truncated = formatter.truncateText(longText, 5);

        assertTrue(formatter.stripColors(truncated).length() <= 8, "截断后的文本不应超过限制");
    }

    // ==================== AnnouncementScheduler 测试 ====================

    @Test
    @DisplayName("测试公告队列")
    public void testAnnouncementQueue() {
        scheduler.start();

        BossAnnouncement ann1 = new BossAnnouncement();
        ann1.setType(BossAnnouncement.AnnouncementType.SPAWN);
        ann1.setPriority(BossAnnouncement.AnnouncementPriority.LOW);

        BossAnnouncement ann2 = new BossAnnouncement();
        ann2.setType(BossAnnouncement.AnnouncementType.KILLED);
        ann2.setPriority(BossAnnouncement.AnnouncementPriority.CRITICAL);

        scheduler.enqueue(ann1);
        scheduler.enqueue(ann2);

        assertEquals(2, scheduler.getQueueSize(), "队列应该有2个公告");

        // 高优先级应该先出列
        BossAnnouncement next = scheduler.nextAnnouncement();
        assertNotNull(next, "应该能获取公告");
    }

    @Test
    @DisplayName("测试冷却管理")
    public void testCooldownManagement() {
        scheduler.addCooldown(testBossUUID, 5);

        assertTrue(scheduler.isInCooldown(testBossUUID), "应该在冷却中");

        long remaining = scheduler.getRemainingCooldown(testBossUUID);
        assertTrue(remaining > 0 && remaining <= 5, "剩余时间应该在0-5秒");

        scheduler.removeCooldown(testBossUUID);
        assertFalse(scheduler.isInCooldown(testBossUUID), "移除后应该不在冷却中");
    }

    @Test
    @DisplayName("测试定时任务")
    public void testScheduledTasks() {
        scheduler.start();

        BossAnnouncement announcement = new BossAnnouncement();
        announcement.setType(BossAnnouncement.AnnouncementType.SPAWN);

        scheduler.scheduleAnnouncement("test-task", announcement, 100);

        assertTrue(scheduler.getAllTaskIds().contains("test-task"), "应该包含该任务");

        scheduler.cancelTask("test-task");
        assertFalse(scheduler.getAllTaskIds().contains("test-task"), "应该已取消任务");

        scheduler.shutdown();
    }

    // ==================== BossAnnouncementManager 测试 ====================

    @Test
    @DisplayName("测试公告发送")
    public void testAnnounceBossSpawn() {
        assertTrue(manager.isEnabled(), "管理器应该已启用");
        assertTrue(manager.isInitialized(), "管理器应该已初始化");

        List<BossAnnouncement> recent = manager.getRecentAnnouncements(10);
        assertNotNull(recent, "应该能获取最近公告");
    }

    @Test
    @DisplayName("测试模板管理")
    public void testTemplateManagement() {
        String customTemplate = "自定义模板: {boss_name}";
        manager.setTemplate(BossAnnouncement.AnnouncementType.SPAWN, customTemplate);

        String retrieved = manager.getTemplate(BossAnnouncement.AnnouncementType.SPAWN);
        assertEquals(customTemplate, retrieved, "应该能检索到自定义模板");
    }

    @Test
    @DisplayName("测试冷却配置")
    public void testCooldownConfiguration() {
        manager.setCooldown(BossAnnouncement.AnnouncementType.SPAWN, 180);

        long cooldown = manager.getCooldown(BossAnnouncement.AnnouncementType.SPAWN);
        assertEquals(180, cooldown, "冷却时间应该是180秒");
    }

    @Test
    @DisplayName("测试公告统计")
    public void testAnnouncementStatistics() {
        String stats = manager.getStatistics();
        assertNotNull(stats, "应该能获取统计信息");
        assertTrue(stats.contains("公告系统"), "统计信息应该包含'公告系统'");
    }

    // ==================== 集成测试 ====================

    @Test
    @DisplayName("测试完整公告流程")
    public void testCompleteAnnouncementFlow() {
        manager.initialize();

        // 创建并设置模板
        String template = "&c[Boss] &6{boss_name}&a出现在&b{world}";
        manager.setTemplate(BossAnnouncement.AnnouncementType.SPAWN, template);

        // 验证模板
        assertEquals(template, manager.getTemplate(BossAnnouncement.AnnouncementType.SPAWN));

        manager.shutdown();
        assertFalse(manager.isEnabled(), "关闭后应该禁用");
    }
}
