package com.xiancore.boss.damage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * DamageTracker 集成测试
 * Integration Tests for DamageTracker
 */
public class DamageTrackerTest {

    private DamageTracker damageTracker;

    @Mock
    private Plugin mockPlugin;

    @Mock
    private Player mockPlayer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        damageTracker = new DamageTracker(mockPlugin);

        // 配置mock Player
        when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(mockPlayer.getName()).thenReturn("TestPlayer");
    }

    @Test
    public void testCreateBossRecord() {
        UUID bossId = UUID.randomUUID();
        String bossType = "TestBoss";

        DamageTracker.BossDamageRecord record = damageTracker.createBossRecord(bossId, bossType);

        assertNotNull(record);
        assertEquals(bossId, record.bossId);
        assertEquals(bossType, record.bossType);
        assertEquals(0.0, record.totalDamage);
        assertTrue(record.playerDamages.isEmpty());
    }

    @Test
    public void testGetBossRecord() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        DamageTracker.BossDamageRecord record = damageTracker.getBossRecord(bossId);

        assertNotNull(record);
        assertEquals(bossId, record.bossId);
    }

    @Test
    public void testRecordDamage() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        damageTracker.recordDamage(bossId, mockPlayer, 100.0);

        DamageTracker.BossDamageRecord record = damageTracker.getBossRecord(bossId);
        assertEquals(100.0, record.totalDamage);
        assertEquals(1, record.playerDamages.size());
    }

    @Test
    public void testRecordMultipleDamage() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        damageTracker.recordDamage(bossId, mockPlayer, 50.0);
        damageTracker.recordDamage(bossId, mockPlayer, 50.0);

        DamageTracker.BossDamageRecord record = damageTracker.getBossRecord(bossId);
        assertEquals(100.0, record.totalDamage);

        DamageTracker.DamageRecord playerDamage = record.playerDamages.get(mockPlayer.getUniqueId());
        assertNotNull(playerDamage);
        assertEquals(100.0, playerDamage.damageDealt);
        assertEquals(2, playerDamage.hitCount);
    }

    @Test
    public void testGetPlayerDamage() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");
        damageTracker.recordDamage(bossId, mockPlayer, 100.0);

        DamageTracker.DamageRecord playerDamage = damageTracker.getPlayerDamage(bossId, mockPlayer.getUniqueId());

        assertNotNull(playerDamage);
        assertEquals(100.0, playerDamage.damageDealt);
        assertEquals(mockPlayer.getName(), playerDamage.playerName);
    }

    @Test
    public void testGetPlayerDamagePercentage() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        // 创建两个玩家，伤害分别为300和200
        damageTracker.recordDamage(bossId, mockPlayer, 300.0);

        Player mockPlayer2 = org.mockito.Mockito.mock(Player.class);
        when(mockPlayer2.getUniqueId()).thenReturn(UUID.randomUUID());
        when(mockPlayer2.getName()).thenReturn("TestPlayer2");
        damageTracker.recordDamage(bossId, mockPlayer2, 200.0);

        double percentage = damageTracker.getPlayerDamagePercentage(bossId, mockPlayer.getUniqueId());
        // 300 / (300 + 200) = 0.6 = 60%
        assertEquals(60.0, percentage, 0.01);
    }

    @Test
    public void testGetPlayerRank() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        damageTracker.recordDamage(bossId, mockPlayer, 300.0);

        Player mockPlayer2 = org.mockito.Mockito.mock(Player.class);
        UUID player2Id = UUID.randomUUID();
        when(mockPlayer2.getUniqueId()).thenReturn(player2Id);
        when(mockPlayer2.getName()).thenReturn("TestPlayer2");
        damageTracker.recordDamage(bossId, mockPlayer2, 200.0);

        int rank = damageTracker.getPlayerRank(bossId, mockPlayer.getUniqueId());
        assertEquals(1, rank); // 第一名

        int rank2 = damageTracker.getPlayerRank(bossId, player2Id);
        assertEquals(2, rank2); // 第二名
    }

    @Test
    public void testFinishTracking() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        DamageTracker.BossDamageRecord record = damageTracker.finishTracking(bossId);

        assertNotNull(record);
        assertNotEquals(0, record.recordEndTime);
    }

    @Test
    public void testDeleteBossRecord() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        boolean deleted = damageTracker.deleteBossRecord(bossId);
        assertTrue(deleted);

        DamageTracker.BossDamageRecord record = damageTracker.getBossRecord(bossId);
        assertNull(record);
    }

    @Test
    public void testGetActiveRecordCount() {
        UUID bossId1 = UUID.randomUUID();
        UUID bossId2 = UUID.randomUUID();

        damageTracker.createBossRecord(bossId1, "TestBoss1");
        damageTracker.createBossRecord(bossId2, "TestBoss2");

        assertEquals(2, damageTracker.getActiveRecordCount());
    }

    @Test
    public void testCalculateAverageDamage() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        Player player1 = org.mockito.Mockito.mock(Player.class);
        when(player1.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player1.getName()).thenReturn("Player1");

        Player player2 = org.mockito.Mockito.mock(Player.class);
        when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player2.getName()).thenReturn("Player2");

        damageTracker.recordDamage(bossId, player1, 100.0);
        damageTracker.recordDamage(bossId, player2, 200.0);

        double avgDamage = damageTracker.calculateAverageDamage(bossId);
        // (100 + 200) / 2 = 150
        assertEquals(150.0, avgDamage);
    }

    @Test
    public void testDamageRecordGetAverageDamagePerHit() {
        UUID playerId = UUID.randomUUID();
        DamageTracker.DamageRecord record = new DamageTracker.DamageRecord(
                playerId, "TestPlayer", 100.0
        );

        record.hitCount = 5;
        double avgDamage = record.getAverageDamagePerHit();
        assertEquals(20.0, avgDamage);
    }

    @Test
    public void testBossDamageRecordIsNotExpired() {
        DamageTracker.BossDamageRecord record = new DamageTracker.BossDamageRecord(
                UUID.randomUUID(), "TestBoss"
        );

        boolean expired = record.isExpired(3600000); // 1小时
        assertFalse(expired);
    }

    @Test
    public void testGetTopDamageDealer() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        for (int i = 1; i <= 5; i++) {
            Player player = org.mockito.Mockito.mock(Player.class);
            UUID playerId = UUID.randomUUID();
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getName()).thenReturn("Player" + i);
            damageTracker.recordDamage(bossId, player, i * 100.0);
        }

        List<DamageTracker.DamageRecord> topDamagers = damageTracker.getTopDamageDealer(bossId, 3);
        assertEquals(3, topDamagers.size());
        // 最高伤害应该在前面
        assertEquals(500.0, topDamagers.get(0).damageDealt);
        assertEquals(400.0, topDamagers.get(1).damageDealt);
        assertEquals(300.0, topDamagers.get(2).damageDealt);
    }

    @Test
    public void testGetDamageStatisticsSummary() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        damageTracker.recordDamage(bossId, mockPlayer, 100.0);

        Map<String, Object> summary = damageTracker.getDamageStatisticsSummary(bossId);

        assertNotNull(summary);
        assertEquals(bossId, summary.get("boss_id"));
        assertEquals("TestBoss", summary.get("boss_type"));
        assertEquals(100.0, summary.get("total_damage"));
        assertEquals(1, summary.get("player_count"));
    }

    @Test
    public void testRecordZeroDamage() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        damageTracker.recordDamage(bossId, mockPlayer, 0.0);

        DamageTracker.BossDamageRecord record = damageTracker.getBossRecord(bossId);
        assertEquals(0.0, record.totalDamage);
    }

    @Test
    public void testClearAllRecords() {
        UUID bossId = UUID.randomUUID();
        damageTracker.createBossRecord(bossId, "TestBoss");

        damageTracker.clear();

        assertEquals(0, damageTracker.getActiveRecordCount());
    }
}
