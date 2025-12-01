package com.xiancore.web.repository;

import com.xiancore.web.entity.Boss;
import com.xiancore.web.entity.DamageRecord;
import com.xiancore.web.entity.PlayerStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository 集成测试
 * 测试所有Repository的CRUD和查询功能
 */
@DataJpaTest
@DisplayName("Repository集成测试")
class RepositoryIntegrationTests {

    @Autowired
    private BossRepository bossRepository;

    @Autowired
    private DamageRecordRepository damageRepository;

    @Autowired
    private PlayerStatsRepository statsRepository;

    private Boss testBoss;
    private DamageRecord testDamage;
    private PlayerStats testStats;

    @BeforeEach
    void setUp() {
        // 创建测试Boss
        testBoss = Boss.builder()
                .id(UUID.randomUUID().toString())
                .name("TestBoss")
                .type("DRAGON")
                .status("ALIVE")
                .world("world")
                .coordX(0.0)
                .coordY(64.0)
                .coordZ(0.0)
                .currentHealth(100.0)
                .maxHealth(100.0)
                .totalDamage(0.0)
                .difficultyLevel(3)
                .spawnedTime(System.currentTimeMillis())
                .build();
        testBoss = bossRepository.save(testBoss);

        // 创建测试伤害记录
        testDamage = DamageRecord.builder()
                .id(UUID.randomUUID().toString())
                .boss(testBoss)
                .playerId(UUID.randomUUID().toString())
                .playerName("TestPlayer")
                .damage(25.0)
                .damageTime(System.currentTimeMillis())
                .damageType("PHYSICAL")
                .build();
        testDamage = damageRepository.save(testDamage);

        // 创建测试玩家统计
        testStats = PlayerStats.builder()
                .id(UUID.randomUUID().toString())
                .playerId(UUID.randomUUID().toString())
                .playerName("TestPlayer")
                .bossKills(5)
                .totalDamage(500.0)
                .totalBattles(10)
                .balance(1000.0)
                .totalEarned(1500.0)
                .totalSpent(500.0)
                .killRanking(1)
                .wealthRanking(1)
                .build();
        testStats = statsRepository.save(testStats);
    }

    // ==================== Boss Repository Tests ====================

    @Test
    @DisplayName("Boss Repository - 创建和查询")
    void testBossCrudOperations() {
        // 查询
        Optional<Boss> found = bossRepository.findById(testBoss.getId());
        assertTrue(found.isPresent());
        assertEquals("TestBoss", found.get().getName());

        // 更新
        found.get().setName("UpdatedBoss");
        bossRepository.save(found.get());

        // 验证更新
        Optional<Boss> updated = bossRepository.findById(testBoss.getId());
        assertTrue(updated.isPresent());
        assertEquals("UpdatedBoss", updated.get().getName());

        // 删除
        bossRepository.delete(updated.get());
        assertFalse(bossRepository.findById(testBoss.getId()).isPresent());
    }

    @Test
    @DisplayName("Boss Repository - 按状态查询")
    void testFindByStatus() {
        List<Boss> aliveBosses = bossRepository.findByStatus("ALIVE");
        assertFalse(aliveBosses.isEmpty());
        assertTrue(aliveBosses.stream().allMatch(b -> "ALIVE".equals(b.getStatus())));
    }

    @Test
    @DisplayName("Boss Repository - 获取所有活跃Boss")
    void testFindAllAliveBosses() {
        List<Boss> aliveBosses = bossRepository.findAllAliveBosses();
        assertFalse(aliveBosses.isEmpty());
        assertTrue(aliveBosses.stream().allMatch(Boss::isAlive));
    }

    @Test
    @DisplayName("Boss Repository - 分页查询")
    void testFindActiveByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Boss> page = bossRepository.findActiveByStatus("ALIVE", pageable);
        assertTrue(page.getTotalElements() > 0);
    }

    // ==================== DamageRecord Repository Tests ====================

    @Test
    @DisplayName("DamageRecord Repository - 按Boss查询伤害")
    void testFindByBossId() {
        List<DamageRecord> records = damageRepository.findByBossId(testBoss.getId());
        assertFalse(records.isEmpty());
        assertTrue(records.stream().allMatch(r -> testBoss.getId().equals(r.getBoss().getId())));
    }

    @Test
    @DisplayName("DamageRecord Repository - 按玩家查询伤害")
    void testFindByPlayerId_Damage() {
        List<DamageRecord> records = damageRepository.findByPlayerId(testDamage.getPlayerId());
        assertFalse(records.isEmpty());
        assertTrue(records.stream().allMatch(r -> testDamage.getPlayerId().equals(r.getPlayerId())));
    }

    @Test
    @DisplayName("DamageRecord Repository - 获取Boss总伤害")
    void testGetTotalDamageForBoss() {
        Double totalDamage = damageRepository.getTotalDamageForBoss(testBoss.getId());
        assertNotNull(totalDamage);
        assertTrue(totalDamage >= testDamage.getDamage());
    }

    @Test
    @DisplayName("DamageRecord Repository - 统计Boss伤害次数")
    void testCountDamageRecordsByBoss() {
        Long count = damageRepository.countDamageRecordsByBoss(testBoss.getId());
        assertTrue(count > 0);
    }

    // ==================== PlayerStats Repository Tests ====================

    @Test
    @DisplayName("PlayerStats Repository - 按玩家ID查询")
    void testFindByPlayerId_Stats() {
        Optional<PlayerStats> found = statsRepository.findByPlayerId(testStats.getPlayerId());
        assertTrue(found.isPresent());
        assertEquals(testStats.getPlayerName(), found.get().getPlayerName());
    }

    @Test
    @DisplayName("PlayerStats Repository - 获取击杀排名")
    void testGetKillRanking() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PlayerStats> ranking = statsRepository.getKillRanking(pageable);
        assertTrue(ranking.getTotalElements() > 0);
    }

    @Test
    @DisplayName("PlayerStats Repository - 获取财富排名")
    void testGetWealthRanking() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PlayerStats> ranking = statsRepository.getWealthRanking(pageable);
        assertTrue(ranking.getTotalElements() > 0);
    }

    @Test
    @DisplayName("PlayerStats Repository - 统计总玩家数")
    void testCountTotalPlayers() {
        Long total = statsRepository.countTotalPlayers();
        assertTrue(total > 0);
    }

    @Test
    @DisplayName("PlayerStats Repository - 按名称搜索")
    void testSearchPlayerByName() {
        List<PlayerStats> results = statsRepository.findByPlayerNameContaining("TestPlayer");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("PlayerStats Repository - 获取平均Boss击杀数")
    void testGetAverageBossKills() {
        Double average = statsRepository.getAverageBossKills();
        assertNotNull(average);
        assertTrue(average >= 0);
    }

    @Test
    @DisplayName("PlayerStats Repository - 获取最高余额")
    void testGetMaxBalance() {
        Double max = statsRepository.getMaxBalance();
        assertNotNull(max);
        assertTrue(max > 0);
    }
}
