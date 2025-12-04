package com.xiancore.boss.reward;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.bukkit.plugin.Plugin;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RewardDistributor 集成测试
 * Integration Tests for RewardDistributor
 */
public class RewardDistributorTest {

    private RewardDistributor rewardDistributor;

    @Mock
    private Plugin mockPlugin;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rewardDistributor = new RewardDistributor(mockPlugin);
    }

    @Test
    public void testRewardConfigCreation() {
        RewardDistributor.RewardConfig config = new RewardDistributor.RewardConfig(
                "TestBoss", 100.0, 50.0, 3600
        );

        assertEquals("TestBoss", config.mobType);
        assertEquals(100.0, config.baseExp);
        assertEquals(50.0, config.baseGold);
        assertEquals(3600, config.cooldownSeconds);
        assertEquals(1.0, config.tierMultiplier);
        assertTrue(config.dropItems.isEmpty());
    }

    @Test
    public void testRegisterRewardConfig() {
        RewardDistributor.RewardConfig config = new RewardDistributor.RewardConfig(
                "SkeletonKing", 100.0, 50.0, 3600
        );
        rewardDistributor.registerRewardConfig(config);

        RewardDistributor.RewardConfig retrieved = rewardDistributor.getRewardConfig("SkeletonKing");
        assertNotNull(retrieved);
        assertEquals("SkeletonKing", retrieved.mobType);
    }

    @Test
    public void testContributionPercentageCalculation() {
        double playerDamage = 150.0;
        double totalDamage = 500.0;

        double contribution = rewardDistributor.calculateContributionPercentage(playerDamage, totalDamage);
        assertEquals(0.3, contribution, 0.01);
    }

    @Test
    public void testContributionPercentageZeroTotal() {
        double playerDamage = 150.0;
        double totalDamage = 0.0;

        double contribution = rewardDistributor.calculateContributionPercentage(playerDamage, totalDamage);
        assertEquals(0.0, contribution);
    }

    @Test
    public void testExpRewardCalculation() {
        double baseExp = 100.0;
        double contribution = 0.5;
        int tierLevel = 2;

        double expReward = rewardDistributor.calculateExpReward(baseExp, contribution, tierLevel);
        // 100 * 0.5 * (1 + 2 * 0.1) = 100 * 0.5 * 1.2 = 60
        assertEquals(60.0, expReward, 0.01);
    }

    @Test
    public void testGoldRewardCalculation() {
        double baseGold = 50.0;
        double contribution = 0.5;
        int tierLevel = 2;

        double goldReward = rewardDistributor.calculateGoldReward(baseGold, contribution, tierLevel);
        // 50 * 0.5 * (1 + 2 * 0.1) = 50 * 0.5 * 1.2 = 30
        assertEquals(30.0, goldReward, 0.01);
    }

    @Test
    public void testPlayerContributionCreation() {
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";
        double damageDealt = 200.0;

        RewardDistributor.PlayerContribution contribution = new RewardDistributor.PlayerContribution(
                playerId, playerName, damageDealt
        );

        assertEquals(playerId, contribution.playerUuid);
        assertEquals(playerName, contribution.playerName);
        assertEquals(200.0, contribution.damageDealt);
        assertEquals(0.0, contribution.contributionPercentage);
    }

    @Test
    public void testRewardResultCreation() {
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";

        RewardDistributor.RewardResult result = new RewardDistributor.RewardResult(playerId, playerName);

        assertEquals(playerId, result.playerId);
        assertEquals(playerName, result.playerName);
        assertEquals(0.0, result.expReward);
        assertEquals(0.0, result.goldReward);
        assertTrue(result.itemsReceived.isEmpty());
    }

    @Test
    public void testClearExpiredCooldowns() {
        UUID playerId = UUID.randomUUID();
        rewardDistributor.recordRewardTime(playerId);

        long lastTime = rewardDistributor.getLastRewardTime(playerId);
        assertNotEquals(0, lastTime);

        // 清除过期的冷却时间 (所有超过1秒的)
        rewardDistributor.clearExpiredCooldowns(1);
        // 注意: 这个测试可能需要等待足够的时间

        rewardDistributor.clear();
    }

    @Test
    public void testGetAllRegisteredMobs() {
        RewardDistributor.RewardConfig config1 = new RewardDistributor.RewardConfig(
                "Boss1", 100.0, 50.0, 3600
        );
        RewardDistributor.RewardConfig config2 = new RewardDistributor.RewardConfig(
                "Boss2", 200.0, 100.0, 3600
        );

        rewardDistributor.registerRewardConfig(config1);
        rewardDistributor.registerRewardConfig(config2);

        Set<String> registeredMobs = rewardDistributor.getAllRegisteredMobs();
        assertEquals(2, registeredMobs.size());
        assertTrue(registeredMobs.contains("Boss1"));
        assertTrue(registeredMobs.contains("Boss2"));
    }

    @Test
    public void testRewardConfigTierMultiplierBounds() {
        RewardDistributor.RewardConfig config = new RewardDistributor.RewardConfig(
                "TestBoss", 100.0, 50.0, 3600
        );

        // 测试下界
        config.setTierMultiplier(0.2);
        assertEquals(0.5, config.tierMultiplier); // 应该被限制到最小0.5

        // 测试上界
        config.setTierMultiplier(10.0);
        assertEquals(5.0, config.tierMultiplier); // 应该被限制到最大5.0

        // 测试正常值
        config.setTierMultiplier(2.5);
        assertEquals(2.5, config.tierMultiplier);
    }

    @Test
    public void testAddDropItem() {
        RewardDistributor.RewardConfig config = new RewardDistributor.RewardConfig(
                "TestBoss", 100.0, 50.0, 3600
        );

        config.addDropItem("item1");
        config.addDropItem("item2");

        assertEquals(2, config.dropItems.size());
        assertTrue(config.dropItems.contains("item1"));
        assertTrue(config.dropItems.contains("item2"));
    }

    @Test
    public void testAddNullDropItem() {
        RewardDistributor.RewardConfig config = new RewardDistributor.RewardConfig(
                "TestBoss", 100.0, 50.0, 3600
        );

        config.addDropItem(null);
        config.addDropItem("");

        assertEquals(0, config.dropItems.size());
    }
}
