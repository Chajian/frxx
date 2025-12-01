package com.xiancore.web.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiancore.common.dto.BossDTO;
import com.xiancore.common.dto.DamageRecordDTO;
import com.xiancore.web.XianCoreWebApplication;
import com.xiancore.web.entity.Boss;
import com.xiancore.web.entity.PlayerStats;
import com.xiancore.web.service.BossService;
import com.xiancore.web.service.DamageService;
import com.xiancore.web.service.PlayerStatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API 控制器集成测试
 * 测试所有REST API端点
 */
@SpringBootTest(classes = XianCoreWebApplication.class)
@AutoConfigureMockMvc
@Transactional
@DisplayName("REST API集成测试")
class RestApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        bossDTO.setDifficultyLevel("5");

        Boss created = bossService.createBoss(bossDTO);
        testBossId = created.getId();

        // 创建玩家统计
        playerStatsService.getOrCreatePlayerStats(testPlayerId, "TestPlayer");
    }

    // ==================== Boss API Tests ====================

    @Test
    @DisplayName("REST API - 创建Boss")
    void testCreateBossApi() throws Exception {
        BossDTO bossDTO = new BossDTO();
        bossDTO.setBossName("NewBoss");
        bossDTO.setBossType("SKELETON_KING");
        bossDTO.setMaxHealth(200.0);
        bossDTO.setDifficultyLevel("4");

        MvcResult result = mockMvc.perform(post("/api/v1/bosses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bossDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("NewBoss"))
                .andReturn();

        assertNotNull(result);
    }

    @Test
    @DisplayName("REST API - 获取所有活跃Boss")
    void testGetAllAliveBossesApi() throws Exception {
        mockMvc.perform(get("/api/v1/bosses/alive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("REST API - 获取Boss详细信息")
    void testGetBossByIdApi() throws Exception {
        mockMvc.perform(get("/api/v1/bosses/{id}", testBossId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(testBossId));
    }

    @Test
    @DisplayName("REST API - 记录Boss伤害")
    void testRecordDamageApi() throws Exception {
        mockMvc.perform(put("/api/v1/bosses/{id}/damage", testBossId)
                .param("damage", "50.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 标记Boss为击杀")
    void testMarkBossAsKilledApi() throws Exception {
        mockMvc.perform(put("/api/v1/bosses/{id}/kill", testBossId)
                .param("playerId", testPlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DEAD"));
    }

    @Test
    @DisplayName("REST API - 获取Boss血量百分比")
    void testGetBossHealthPercentageApi() throws Exception {
        // 先记录伤害
        bossService.recordDamage(testBossId, 50.0);

        mockMvc.perform(get("/api/v1/bosses/{id}/health-percentage", testBossId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(50.0));
    }

    // ==================== Damage API Tests ====================

    @Test
    @DisplayName("REST API - 记录伤害事件")
    void testRecordDamageEventApi() throws Exception {
        DamageRecordDTO damageDTO = new DamageRecordDTO();
        damageDTO.setBossId(testBossId);
        damageDTO.setPlayerId(testPlayerId);
        damageDTO.setPlayerName("TestPlayer");
        damageDTO.setDamage(100.0);
        damageDTO.setDamageType("PHYSICAL");

        mockMvc.perform(post("/api/v1/damage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(damageDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.damage").value(100.0));
    }

    @Test
    @DisplayName("REST API - 获取Boss伤害记录")
    void testGetDamageRecordsByBossApi() throws Exception {
        // 先记录伤害
        DamageRecordDTO damageDTO = new DamageRecordDTO();
        damageDTO.setBossId(testBossId);
        damageDTO.setPlayerId(testPlayerId);
        damageDTO.setPlayerName("TestPlayer");
        damageDTO.setDamage(50.0);
        damageService.recordDamage(damageDTO);

        mockMvc.perform(get("/api/v1/damage/boss/{bossId}", testBossId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("REST API - 获取Boss总伤害")
    void testGetBossTotalDamageApi() throws Exception {
        mockMvc.perform(get("/api/v1/damage/boss/{bossId}/total", testBossId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 获取玩家对Boss的伤害")
    void testGetPlayerDamageTowardsBossApi() throws Exception {
        mockMvc.perform(get("/api/v1/damage/boss/{bossId}/player/{playerId}", testBossId, testPlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== Stats API Tests ====================

    @Test
    @DisplayName("REST API - 获取玩家统计")
    void testGetPlayerStatsApi() throws Exception {
        mockMvc.perform(get("/api/v1/stats/{playerId}", testPlayerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.playerId").value(testPlayerId));
    }

    @Test
    @DisplayName("REST API - 增加Boss击杀")
    void testAddBossKillApi() throws Exception {
        mockMvc.perform(put("/api/v1/stats/{playerId}/kill", testPlayerId)
                .param("reward", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 增加伤害统计")
    void testAddPlayerDamageApi() throws Exception {
        mockMvc.perform(put("/api/v1/stats/{playerId}/damage", testPlayerId)
                .param("damage", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 增加收入")
    void testAddEarningsApi() throws Exception {
        mockMvc.perform(put("/api/v1/stats/{playerId}/earnings", testPlayerId)
                .param("amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 增加支出")
    void testAddSpendingApi() throws Exception {
        // 先添加余额
        playerStatsService.addBalance(testPlayerId, 1000.0);

        mockMvc.perform(put("/api/v1/stats/{playerId}/spending", testPlayerId)
                .param("amount", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 获取击杀排名")
    void testGetKillRankingApi() throws Exception {
        mockMvc.perform(get("/api/v1/stats/rankings/kills")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 获取财富排名")
    void testGetWealthRankingApi() throws Exception {
        mockMvc.perform(get("/api/v1/stats/rankings/wealth")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 获取伤害排名")
    void testGetDamageRankingApi() throws Exception {
        mockMvc.perform(get("/api/v1/stats/rankings/damage")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 按名称搜索玩家")
    void testSearchPlayerByNameApi() throws Exception {
        mockMvc.perform(get("/api/v1/stats/search")
                .param("keyword", "Test")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("REST API - 获取总玩家数")
    void testCountTotalPlayersApi() throws Exception {
        mockMvc.perform(get("/api/v1/stats/count/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("REST API - 获取最高余额")
    void testGetMaxBalanceApi() throws Exception {
        mockMvc.perform(get("/api/v1/stats/max-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
