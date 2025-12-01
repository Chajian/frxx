package com.xiancore.web.integration;

import com.xiancore.common.dto.BossDTO;
import com.xiancore.common.dto.DamageRecordDTO;
import com.xiancore.web.entity.Boss;
import com.xiancore.web.entity.DamageRecord;
import com.xiancore.web.entity.PlayerStats;
import com.xiancore.web.repository.BossRepository;
import com.xiancore.web.repository.DamageRecordRepository;
import com.xiancore.web.repository.PlayerStatsRepository;
import com.xiancore.web.service.BossService;
import com.xiancore.web.service.DamageService;
import com.xiancore.web.service.PlayerStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试
 * 测试完整的业务工作流和数据库连接
 */
@SpringBootTest
@Transactional
@AutoConfigureTestEntityManager
@DisplayName("集成测试")
class IntegrationTests {

    @Autowired
    private BossService bossService;

    @Autowired
    private DamageService damageService;

    @Autowired
    private PlayerStatsService playerStatsService;

    @Autowired
    private BossRepository bossRepository;

    @Autowired
    private DamageRecordRepository damageRepository;

    @Autowired
    private PlayerStatsRepository statsRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("数据库连接验证")
    void testDatabaseConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        assertNotNull(connection);
        assertTrue(connection.isValid(2));
        connection.close();
    }

    @Test
    @DisplayName("完整的Boss击杀工作流")
    void testBossKillWorkflow() {
        // 1. 创建Boss
        BossDTO bossDTO = new BossDTO();
        bossDTO.setBossName("IntegrationTestBoss");
        bossDTO.setBossType("DRAGON");
        bossDTO.setWorld("world");
        bossDTO.setX(0.0);
        bossDTO.setY(64.0);
        bossDTO.setZ(0.0);
        bossDTO.setMaxHealth(500.0);
        bossDTO.setDifficultyLevel("5");

        Boss boss = bossService.createBoss(bossDTO);
        assertNotNull(boss.getId());
        assertEquals("SPAWNED", boss.getStatus());

        // 2. 创建玩家
        String playerId = UUID.randomUUID().toString();
        PlayerStats playerStats = playerStatsService.getOrCreatePlayerStats(playerId, "IntegrationTestPlayer");
        assertNotNull(playerStats.getId());

        // 3. 记录伤害事件
        DamageRecordDTO damageDTO = new DamageRecordDTO();
        damageDTO.setBossId(boss.getId());
        damageDTO.setPlayerId(playerId);
        damageDTO.setPlayerName("IntegrationTestPlayer");
        damageDTO.setDamage(100.0);
        damageDTO.setDamageType("PHYSICAL");

        DamageRecord damageRecord = damageService.recordDamage(damageDTO);
        assertNotNull(damageRecord.getId());

        // 4. 标记Boss被击杀
        Boss killedBoss = bossService.markBossAsKilled(boss.getId(), playerId);
        assertEquals("DEAD", killedBoss.getStatus());
        assertEquals(playerId, killedBoss.getKillerPlayerId());

        // 5. 更新玩家统计
        PlayerStats updatedStats = playerStatsService.addBossKill(playerId, 500.0);
        assertEquals(1, updatedStats.getBossKills());
        assertEquals(500.0, updatedStats.getTotalEarned());

        // 6. 验证数据一致性
        Boss savedBoss = bossRepository.findById(boss.getId()).get();
        assertEquals("DEAD", savedBoss.getStatus());

        List<DamageRecord> damageRecords = damageRepository.findByBossId(boss.getId());
        assertFalse(damageRecords.isEmpty());

        PlayerStats savedStats = statsRepository.findByPlayerId(playerId).get();
        assertEquals(1, savedStats.getBossKills());
    }

    @Test
    @DisplayName("多玩家Boss击杀场景")
    void testMultiplayerBossKillScenario() {
        // 1. 创建Boss
        BossDTO bossDTO = new BossDTO();
        bossDTO.setBossName("MultiplayerBoss");
        bossDTO.setBossType("WITHER");
        bossDTO.setMaxHealth(300.0);
        bossDTO.setDifficultyLevel("4");

        Boss boss = bossService.createBoss(bossDTO);

        // 2. 创建多个玩家并记录伤害
        String[] playerIds = new String[3];
        for (int i = 0; i < 3; i++) {
            playerIds[i] = UUID.randomUUID().toString();
            playerStatsService.getOrCreatePlayerStats(playerIds[i], "Player" + i);

            // 记录伤害
            DamageRecordDTO damageDTO = new DamageRecordDTO();
            damageDTO.setBossId(boss.getId());
            damageDTO.setPlayerId(playerIds[i]);
            damageDTO.setPlayerName("Player" + i);
            damageDTO.setDamage(100.0);
            damageDTO.setDamageType("PHYSICAL");

            damageService.recordDamage(damageDTO);
        }

        // 3. 标记Boss被击杀（第一个玩家获得击杀权）
        bossService.markBossAsKilled(boss.getId(), playerIds[0]);

        // 4. 为所有玩家更新统计
        for (int i = 0; i < 3; i++) {
            if (i == 0) {
                playerStatsService.addBossKill(playerIds[i], 1000.0);
            }
            playerStatsService.addPlayerDamage(playerIds[i], 100.0);
            playerStatsService.addBattle(playerIds[i]);
        }

        // 5. 验证
        Boss killedBoss = bossRepository.findById(boss.getId()).get();
        assertEquals("DEAD", killedBoss.getStatus());

        List<DamageRecord> allDamage = damageRepository.findByBossId(boss.getId());
        assertEquals(3, allDamage.size());

        PlayerStats killer = statsRepository.findByPlayerId(playerIds[0]).get();
        assertEquals(1, killer.getBossKills());
        assertEquals(1000.0, killer.getTotalEarned());

        PlayerStats participant = statsRepository.findByPlayerId(playerIds[1]).get();
        assertEquals(0, participant.getBossKills());
        assertEquals(100.0, participant.getTotalDamage());
        assertEquals(1, participant.getTotalBattles());
    }

    @Test
    @DisplayName("玩家经济系统集成")
    void testPlayerEconomyIntegration() {
        String playerId = UUID.randomUUID().toString();
        playerStatsService.getOrCreatePlayerStats(playerId, "EconomyTestPlayer");

        // 1. 增加收入
        playerStatsService.addEarnings(playerId, 1000.0);
        PlayerStats stats = statsRepository.findByPlayerId(playerId).get();
        assertEquals(1000.0, stats.getBalance());

        // 2. 增加支出
        playerStatsService.addSpending(playerId, 300.0);
        stats = statsRepository.findByPlayerId(playerId).get();
        assertEquals(700.0, stats.getBalance());
        assertEquals(1000.0, stats.getTotalEarned());
        assertEquals(300.0, stats.getTotalSpent());

        // 3. 获取财富等级
        String wealthLevel = playerStatsService.getPlayerWealthLevel(playerId);
        assertNotNull(wealthLevel);
    }

    @Test
    @DisplayName("排行榜数据生成")
    void testLeaderboardDataGeneration() {
        // 创建多个玩家和Boss
        for (int i = 0; i < 5; i++) {
            // 创建Boss
            BossDTO bossDTO = new BossDTO();
            bossDTO.setBossName("Boss" + i);
            bossDTO.setMaxHealth(100.0);
            bossDTO.setDifficultyLevel(String.valueOf(i + 1));
            Boss boss = bossService.createBoss(bossDTO);

            // 创建玩家和伤害记录
            String playerId = UUID.randomUUID().toString();
            playerStatsService.getOrCreatePlayerStats(playerId, "Player" + i);

            for (int j = 0; j < 3; j++) {
                DamageRecordDTO damageDTO = new DamageRecordDTO();
                damageDTO.setBossId(boss.getId());
                damageDTO.setPlayerId(playerId);
                damageDTO.setPlayerName("Player" + i);
                damageDTO.setDamage(50.0 * (j + 1));
                damageService.recordDamage(damageDTO);
            }

            // 标记Boss击杀
            bossService.markBossAsKilled(boss.getId(), playerId);
            playerStatsService.addBossKill(playerId, 500.0);
        }

        // 验证排行榜数据
        Long totalPlayers = playerStatsService.countTotalPlayers();
        assertTrue(totalPlayers >= 5);

        Long totalKilledBosses = bossService.countKilledBosses();
        assertTrue(totalKilledBosses >= 5);

        Double totalDamage = playerStatsService.getTotalDamageAcrossAllPlayers();
        assertTrue(totalDamage > 0);
    }

    @Test
    @DisplayName("数据库事务处理")
    void testTransactionHandling() {
        // 创建Boss
        BossDTO bossDTO = new BossDTO();
        bossDTO.setBossName("TransactionTestBoss");
        bossDTO.setMaxHealth(100.0);
        Boss boss = bossService.createBoss(bossDTO);

        // 创建玩家
        String playerId = UUID.randomUUID().toString();
        playerStatsService.getOrCreatePlayerStats(playerId, "TransactionTestPlayer");

        // 在事务中执行多个操作
        DamageRecordDTO damageDTO = new DamageRecordDTO();
        damageDTO.setBossId(boss.getId());
        damageDTO.setPlayerId(playerId);
        damageDTO.setPlayerName("TransactionTestPlayer");
        damageDTO.setDamage(100.0);

        damageService.recordDamage(damageDTO);
        bossService.markBossAsKilled(boss.getId(), playerId);
        playerStatsService.addBossKill(playerId, 1000.0);

        // 验证所有操作成功提交
        Boss savedBoss = bossRepository.findById(boss.getId()).get();
        assertEquals("DEAD", savedBoss.getStatus());

        List<DamageRecord> damageRecords = damageRepository.findByBossId(boss.getId());
        assertFalse(damageRecords.isEmpty());

        PlayerStats playerStats = statsRepository.findByPlayerId(playerId).get();
        assertEquals(1, playerStats.getBossKills());
    }
}
