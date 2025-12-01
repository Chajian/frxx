package com.xiancore.web.service;

import com.xiancore.common.dto.BossDTO;
import com.xiancore.common.dto.DamageRecordDTO;
import com.xiancore.web.entity.Boss;
import com.xiancore.web.entity.DamageRecord;
import com.xiancore.web.entity.PlayerStats;
import com.xiancore.web.repository.BossRepository;
import com.xiancore.web.repository.DamageRecordRepository;
import com.xiancore.web.repository.PlayerStatsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Service 单元测试
 * 测试所有Service的业务逻辑
 */
@SpringBootTest
@DisplayName("Service业务逻辑测试")
class ServiceTests {

    @Autowired
    private BossService bossService;

    @Autowired
    private DamageService damageService;

    @Autowired
    private PlayerStatsService playerStatsService;

    private String testBossId;
    private String testPlayerId;

    @BeforeEach
    void setUp() {
        testPlayerId = UUID.randomUUID().toString();

        // 创建测试Boss
        BossDTO bossDTO = new BossDTO();
        bossDTO.setBossName("TestBoss");
        bossDTO.setBossType("DRAGON");
        bossDTO.setWorld("world");
        bossDTO.setX(0.0);
        bossDTO.setY(64.0);
        bossDTO.setZ(0.0);
        bossDTO.setMaxHealth(100.0);
        bossDTO.setDifficultyLevel("3");

        Boss created = bossService.createBoss(bossDTO);
        testBossId = created.getId();

        // 创建玩家统计
        playerStatsService.getOrCreatePlayerStats(testPlayerId, "TestPlayer");
    }

    // ==================== BossService Tests ====================

    @Test
    @DisplayName("BossService - 创建Boss")
    void testCreateBoss() {
        BossDTO dto = new BossDTO();
        dto.setBossName("NewBoss");
        dto.setBossType("SKELETON_KING");
        dto.setMaxHealth(200.0);
        dto.setDifficultyLevel("4");

        Boss created = bossService.createBoss(dto);

        assertNotNull(created.getId());
        assertEquals("NewBoss", created.getName());
        assertEquals("SKELETON_KING", created.getType());
        assertEquals("SPAWNED", created.getStatus());
    }

    @Test
    @DisplayName("BossService - 获取Boss信息")
    void testGetBossById() {
        Optional<Boss> boss = bossService.getBossById(testBossId);

        assertTrue(boss.isPresent());
        assertEquals("TestBoss", boss.get().getName());
    }

    @Test
    @DisplayName("BossService - 记录伤害")
    void testBossRecordDamage() {
        Boss before = bossService.getBossById(testBossId).get();
        double beforeDamage = before.getTotalDamage();
        double beforeHealth = before.getCurrentHealth();

        bossService.recordDamage(testBossId, 25.0);

        Boss after = bossService.getBossById(testBossId).get();
        assertEquals(beforeDamage + 25.0, after.getTotalDamage());
        assertEquals(beforeHealth - 25.0, after.getCurrentHealth());
    }

    @Test
    @DisplayName("BossService - 标记Boss为击杀")
    void testMarkBossAsKilled() {
        bossService.markBossAsKilled(testBossId, testPlayerId);

        Optional<Boss> boss = bossService.getBossById(testBossId);
        assertTrue(boss.isPresent());
        assertEquals("DEAD", boss.get().getStatus());
        assertEquals(testPlayerId, boss.get().getKillerPlayerId());
        assertNotNull(boss.get().getKilledTime());
    }

    @Test
    @DisplayName("BossService - 获取所有活跃Boss")
    void testGetAllAliveBosses() {
        List<Boss> bosses = bossService.getAllAliveBosses();

        assertFalse(bosses.isEmpty());
        assertTrue(bosses.stream().allMatch(Boss::isAlive));
    }

    @Test
    @DisplayName("BossService - 统计玩家击杀的Boss数量")
    void testCountBossesKilledByPlayer() {
        bossService.markBossAsKilled(testBossId, testPlayerId);

        Long count = bossService.countBossesKilledByPlayer(testPlayerId);
        assertTrue(count > 0);
    }

    @Test
    @DisplayName("BossService - 获取Boss血量百分比")
    void testGetBossHealthPercentage() {
        bossService.recordDamage(testBossId, 50.0);

        Double percentage = bossService.getBossHealthPercentage(testBossId);

        assertEquals(50.0, percentage);
    }

    @Test
    @DisplayName("BossService - 检查Boss是否活跃")
    void testIsBossAlive() {
        Boolean alive = bossService.isBossAlive(testBossId);

        assertTrue(alive);

        bossService.markBossAsKilled(testBossId, testPlayerId);
        alive = bossService.isBossAlive(testBossId);

        assertFalse(alive);
    }

    // ==================== DamageService Tests ====================

    @Test
    @DisplayName("DamageService - 记录伤害")
    void testDamageServiceRecordDamage() {
        DamageRecordDTO dto = new DamageRecordDTO();
        dto.setBossId(testBossId);
        dto.setPlayerId(testPlayerId);
        dto.setPlayerName("TestPlayer");
        dto.setDamage(50.0);
        dto.setDamageType("PHYSICAL");

        DamageRecord record = damageService.recordDamage(dto);

        assertNotNull(record.getId());
        assertEquals(50.0, record.getDamage());
        assertEquals("PHYSICAL", record.getDamageType());
    }

    @Test
    @DisplayName("DamageService - 获取Boss伤害记录")
    void testGetDamageRecordsByBoss() {
        DamageRecordDTO dto = new DamageRecordDTO();
        dto.setBossId(testBossId);
        dto.setPlayerId(testPlayerId);
        dto.setPlayerName("TestPlayer");
        dto.setDamage(25.0);

        damageService.recordDamage(dto);

        List<DamageRecord> records = damageService.getDamageRecordsByBoss(testBossId);

        assertFalse(records.isEmpty());
    }

    @Test
    @DisplayName("DamageService - 获取Boss总伤害")
    void testGetBossTotalDamage() {
        DamageRecordDTO dto = new DamageRecordDTO();
        dto.setBossId(testBossId);
        dto.setPlayerId(testPlayerId);
        dto.setPlayerName("TestPlayer");
        dto.setDamage(75.0);

        damageService.recordDamage(dto);

        Double totalDamage = damageService.getBossTotalDamage(testBossId);

        assertTrue(totalDamage >= 75.0);
    }

    @Test
    @DisplayName("DamageService - 获取玩家对Boss的总伤害")
    void testGetPlayerDamageTowardsBoss() {
        DamageRecordDTO dto = new DamageRecordDTO();
        dto.setBossId(testBossId);
        dto.setPlayerId(testPlayerId);
        dto.setPlayerName("TestPlayer");
        dto.setDamage(60.0);

        damageService.recordDamage(dto);

        Double damage = damageService.getPlayerDamageTowardsBoss(testBossId, testPlayerId);

        assertTrue(damage >= 60.0);
    }

    // ==================== PlayerStatsService Tests ====================

    @Test
    @DisplayName("PlayerStatsService - 增加Boss击杀")
    void testAddBossKill() {
        PlayerStats before = playerStatsService.getPlayerStats(testPlayerId).get();
        int beforeKills = before.getBossKills();

        playerStatsService.addBossKill(testPlayerId, 100.0);

        PlayerStats after = playerStatsService.getPlayerStats(testPlayerId).get();
        assertEquals(beforeKills + 1, after.getBossKills());
        assertTrue(after.getTotalEarned() >= 100.0);
    }

    @Test
    @DisplayName("PlayerStatsService - 增加伤害统计")
    void testAddPlayerDamage() {
        PlayerStats before = playerStatsService.getPlayerStats(testPlayerId).get();
        double beforeDamage = before.getTotalDamage();

        playerStatsService.addPlayerDamage(testPlayerId, 150.0);

        PlayerStats after = playerStatsService.getPlayerStats(testPlayerId).get();
        assertEquals(beforeDamage + 150.0, after.getTotalDamage());
    }

    @Test
    @DisplayName("PlayerStatsService - 增加收入")
    void testAddEarnings() {
        PlayerStats before = playerStatsService.getPlayerStats(testPlayerId).get();
        double beforeEarned = before.getTotalEarned();
        double beforeBalance = before.getBalance();

        playerStatsService.addEarnings(testPlayerId, 500.0);

        PlayerStats after = playerStatsService.getPlayerStats(testPlayerId).get();
        assertEquals(beforeEarned + 500.0, after.getTotalEarned());
        assertEquals(beforeBalance + 500.0, after.getBalance());
    }

    @Test
    @DisplayName("PlayerStatsService - 增加支出")
    void testAddSpending() {
        playerStatsService.addBalance(testPlayerId, 1000.0);

        PlayerStats before = playerStatsService.getPlayerStats(testPlayerId).get();
        double beforeSpent = before.getTotalSpent();
        double beforeBalance = before.getBalance();

        playerStatsService.addSpending(testPlayerId, 200.0);

        PlayerStats after = playerStatsService.getPlayerStats(testPlayerId).get();
        assertEquals(beforeSpent + 200.0, after.getTotalSpent());
        assertEquals(beforeBalance - 200.0, after.getBalance());
    }

    @Test
    @DisplayName("PlayerStatsService - 获取财富等级")
    void testGetWealthLevel() {
        playerStatsService.addBalance(testPlayerId, 50000.0);

        String level = playerStatsService.getPlayerWealthLevel(testPlayerId);

        assertNotNull(level);
        assertFalse(level.isEmpty());
    }

    @Test
    @DisplayName("PlayerStatsService - 统计总玩家数")
    void testCountTotalPlayers() {
        Long total = playerStatsService.countTotalPlayers();

        assertTrue(total > 0);
    }

    @Test
    @DisplayName("PlayerStatsService - 获取最高余额")
    void testGetMaxBalance() {
        Double max = playerStatsService.getMaxBalance();

        assertNotNull(max);
        assertTrue(max > 0);
    }
}
