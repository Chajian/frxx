package com.xiancore.systems.boss.teleport;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boss付费传送系统单元测试
 * 测试传送点、费用计算、冷却管理、动画系统
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@DisplayName("Boss付费传送系统单元测试")
public class BossTeleportTest {

    private BossTeleportManager manager;
    private PricingCalculator pricing;
    private TeleportScheduler scheduler;
    private TeleportAnimation animation;

    private BossTeleportPoint testPoint;
    private UUID testPlayerUUID;

    @BeforeEach
    public void setUp() {
        manager = new BossTeleportManager();
        manager.initialize();

        pricing = manager.getPricingCalculator();
        scheduler = manager.getScheduler();
        animation = new TeleportAnimation();

        testPlayerUUID = UUID.randomUUID();

        // 创建测试传送点
        testPoint = new BossTeleportPoint(
            "test-point",
            "测试传送点",
            new Location(null, 100, 64, 100),
            100.0
        );
    }

    // ==================== BossTeleportPoint 测试 ====================

    @Test
    @DisplayName("测试传送点创建")
    public void testTeleportPointCreation() {
        assertNotNull(testPoint, "传送点应该被创建");
        assertEquals("test-point", testPoint.getPointId());
        assertEquals("测试传送点", testPoint.getDisplayName());
        assertEquals(100.0, testPoint.getBaseCost());
        assertTrue(testPoint.isEnabled(), "新创建的传送点应该启用");
    }

    @Test
    @DisplayName("测试传送点距离计算")
    public void testDistanceCalculation() {
        Location from = new Location(null, 0, 64, 0);
        double distance = testPoint.calculateDistance(from);

        assertTrue(distance > 0, "距离应该大于0");
        assertTrue(distance < 200, "距离应该接近对角线距离141.4");
    }

    @Test
    @DisplayName("测试传送点访问控制")
    public void testAccessControl() {
        testPoint.setRequiredLevel(10);

        // 等级不足
        assertFalse(testPoint.canAccess(true, 5, false), "等级不足应该无法访问");

        // 等级足够
        assertTrue(testPoint.canAccess(true, 10, false), "等级足够应该可以访问");

        // VIP专用
        testPoint.setVipOnly(true);
        assertFalse(testPoint.canAccess(true, 10, false), "非VIP无法访问VIP专用");
        assertTrue(testPoint.canAccess(true, 10, true), "VIP可以访问");
    }

    @Test
    @DisplayName("测试使用统计")
    public void testUsageTracking() {
        assertEquals(0, testPoint.getUsageCount(), "初始使用次数为0");

        testPoint.recordUsage(100.0);
        assertEquals(1, testPoint.getUsageCount(), "应该记录一次使用");
        assertEquals(100.0, testPoint.getTotalRevenue(), "总收益应该累计");

        testPoint.recordUsage(50.0);
        assertEquals(2, testPoint.getUsageCount());
        assertEquals(150.0, testPoint.getTotalRevenue());
    }

    // ==================== PricingCalculator 测试 ====================

    @Test
    @DisplayName("测试费用计算")
    public void testPriceCalculation() {
        Location from = new Location(null, 0, 64, 0);
        double cost = pricing.calculateCostByDistance(from, testPoint.getLocation());

        assertTrue(cost > 50, "费用应该大于基础费用");
        assertFalse(Double.isInfinite(cost) || Double.isNaN(cost), "费用应该是有效数字");
    }

    @Test
    @DisplayName("测试VIP折扣")
    public void testVIPDiscount() {
        double originalCost = 100.0;
        double discountedCost = pricing.applyVIPDiscount(originalCost);

        assertEquals(70.0, discountedCost, "VIP折扣应该是原价的70%");
        double savings = pricing.calculateVIPSavings(originalCost);
        assertEquals(30.0, savings, "节省金额应该是原价的30%");
    }

    @Test
    @DisplayName("测试余额检查")
    public void testAffordabilityCheck() {
        // 余额足够
        assertTrue(pricing.canAfford(testPlayerUUID, 500.0), "默认余额足够");

        // 余额不足 (模拟经济系统)
        assertFalse(pricing.canAfford(testPlayerUUID, 10000.0), "余额不足");
    }

    @Test
    @DisplayName("测试费用扣除")
    public void testCostDeduction() {
        double initialBalance = pricing.getBalance(testPlayerUUID);
        boolean success = pricing.deductCost(testPlayerUUID, 100.0);

        assertTrue(success, "扣除费用应该成功");
        double newBalance = pricing.getBalance(testPlayerUUID);
        assertEquals(initialBalance - 100.0, newBalance, "余额应该减少100");
    }

    // ==================== TeleportScheduler 测试 ====================

    @Test
    @DisplayName("测试冷却管理")
    public void testCooldownManagement() {
        scheduler.addCooldown(testPlayerUUID, 5);

        assertTrue(scheduler.isInCooldown(testPlayerUUID), "应该在冷却中");

        long remaining = scheduler.getRemainingCooldown(testPlayerUUID);
        assertTrue(remaining > 0 && remaining <= 5, "剩余时间应该在0-5秒");

        scheduler.removeCooldown(testPlayerUUID);
        assertFalse(scheduler.isInCooldown(testPlayerUUID), "移除后应该不在冷却中");
    }

    @Test
    @DisplayName("测试VIP冷却")
    public void testVIPCooldown() {
        scheduler.addCooldown(testPlayerUUID, false);  // 普通玩家
        long normalCooldown = scheduler.getRemainingCooldown(testPlayerUUID);

        scheduler.removeCooldown(testPlayerUUID);

        scheduler.addCooldown(testPlayerUUID, true);   // VIP玩家
        long vipCooldown = scheduler.getRemainingCooldown(testPlayerUUID);

        assertTrue(vipCooldown < normalCooldown, "VIP冷却应该更短");
    }

    @Test
    @DisplayName("测试倒计时")
    public void testCountdownScheduling() {
        assertFalse(scheduler.hasActiveCountdown(testPlayerUUID), "初始没有倒计时");

        // 在实际应用中会有真实的倒计时
        scheduler.scheduleSimpleCountdown(null, 3);  // 使用null避免NPE

        // 倒计时无法测试 (需要真实的Player对象)
    }

    // ==================== BossTeleportManager 测试 ====================

    @Test
    @DisplayName("测试传送点注册")
    public void testTeleportPointRegistration() {
        boolean registered = manager.registerTeleportPoint(testPoint);

        assertTrue(registered, "传送点应该注册成功");
        assertEquals(1, manager.getTeleportPointCount(), "应该有1个传送点");

        BossTeleportPoint retrieved = manager.getTeleportPoint("test-point");
        assertNotNull(retrieved, "应该能检索到传送点");
        assertEquals(testPoint.getPointId(), retrieved.getPointId());
    }

    @Test
    @DisplayName("测试传送点注销")
    public void testTeleportPointUnregistration() {
        manager.registerTeleportPoint(testPoint);
        assertEquals(1, manager.getTeleportPointCount());

        boolean unregistered = manager.unregisterTeleportPoint("test-point");
        assertTrue(unregistered, "传送点应该注销成功");
        assertEquals(0, manager.getTeleportPointCount(), "应该没有传送点");
    }

    @Test
    @DisplayName("测试获取所有传送点")
    public void testGetAllTeleportPoints() {
        BossTeleportPoint point1 = new BossTeleportPoint("p1", "Point1", null, 100);
        BossTeleportPoint point2 = new BossTeleportPoint("p2", "Point2", null, 200);

        manager.registerTeleportPoint(point1);
        manager.registerTeleportPoint(point2);

        List<BossTeleportPoint> all = manager.getAllTeleportPoints();
        assertEquals(2, all.size(), "应该有2个传送点");
    }

    @Test
    @DisplayName("测试传送结果枚举")
    public void testTeleportResultEnum() {
        assertTrue(BossTeleportManager.TeleportResult.SUCCESS.isSuccess());
        assertFalse(BossTeleportManager.TeleportResult.INSUFFICIENT_FUNDS.isSuccess());

        assertNotNull(BossTeleportManager.TeleportResult.SUCCESS.description);
    }

    // ==================== TeleportAnimation 测试 ====================

    @Test
    @DisplayName("测试动画系统")
    public void testAnimationSystem() {
        // Animation只能在玩家在线时执行，这里仅测试不报错
        // 实际上需要真实的Player对象

        try {
            animation.playPreTeleportAnimation(null);     // 使用null不应报错
            animation.playCountdownAnimation(null, 3);
            animation.playTeleportSuccessAnimation(null);
            animation.playTeleportFailureAnimation(null, "测试");
        } catch (Exception e) {
            fail("动画系统不应抛出异常");
        }
    }

    // ==================== 集成测试 ====================

    @Test
    @DisplayName("测试完整传送流程")
    public void testCompleteTeleportFlow() {
        // 注册传送点
        manager.registerTeleportPoint(testPoint);
        assertEquals(1, manager.getTeleportPointCount());

        // 验证费用计算
        Location from = new Location(null, 0, 64, 0);
        double cost = pricing.calculateCostByDistance(from, testPoint.getLocation());
        assertTrue(cost > 0);

        // 验证是否能接受
        assertTrue(pricing.canAfford(testPlayerUUID, cost));

        // 验证冷却
        scheduler.addCooldown(testPlayerUUID, false);
        assertTrue(scheduler.isInCooldown(testPlayerUUID));
    }

    @Test
    @DisplayName("测试传送系统统计")
    public void testStatistics() {
        String stats = manager.getStatistics();
        assertNotNull(stats, "应该能获取统计信息");
        assertTrue(stats.contains("传送系统"), "统计信息应该包含关键字");
    }
}
