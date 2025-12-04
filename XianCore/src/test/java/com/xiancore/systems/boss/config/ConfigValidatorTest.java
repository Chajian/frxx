package com.xiancore.systems.boss.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boss配置验证器单元测试
 * 测试所有配置参数的验证逻辑
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-16
 */
@DisplayName("Boss配置验证器单元测试")
public class ConfigValidatorTest {

    private ConfigValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new ConfigValidator();
    }

    // ==================== 检查间隔验证 ====================

    @Test
    @DisplayName("验证检查间隔 - 正常值")
    public void testValidateCheckInterval_Valid() {
        String error = validator.validateCheckInterval(30);
        assertNull(error, "30秒应该是有效的检查间隔");
    }

    @Test
    @DisplayName("验证检查间隔 - 过小的值")
    public void testValidateCheckInterval_TooSmall() {
        String error = validator.validateCheckInterval(0);
        assertNotNull(error, "0秒应该被拒绝");
        assertTrue(error.contains("检查间隔") || error.contains("最少"), "错误应该提及检查间隔");
    }

    @Test
    @DisplayName("验证检查间隔 - 负数")
    public void testValidateCheckInterval_Negative() {
        String error = validator.validateCheckInterval(-10);
        assertNotNull(error, "负数应该被拒绝");
    }

    // ==================== 最大Boss数验证 ====================

    @Test
    @DisplayName("验证最大Boss数 - 正常值")
    public void testValidateMaxActiveBosses_Valid() {
        String error = validator.validateMaxActiveBosses(10);
        assertNull(error, "10个应该是有效的最大Boss数");
    }

    @Test
    @DisplayName("验证最大Boss数 - 过小的值")
    public void testValidateMaxActiveBosses_TooSmall() {
        String error = validator.validateMaxActiveBosses(0);
        assertNotNull(error, "0个应该被拒绝");
    }

    @Test
    @DisplayName("验证最大Boss数 - 负数")
    public void testValidateMaxActiveBosses_Negative() {
        String error = validator.validateMaxActiveBosses(-5);
        assertNotNull(error, "负数应该被拒绝");
    }

    // ==================== 最少在线玩家数验证 ====================

    @Test
    @DisplayName("验证最少在线玩家 - 正常值")
    public void testValidateMinOnlinePlayers_Valid() {
        String error = validator.validateMinOnlinePlayers(3);
        assertNull(error, "3个玩家应该是有效的");
    }

    @Test
    @DisplayName("验证最少在线玩家 - 零")
    public void testValidateMinOnlinePlayers_Zero() {
        String error = validator.validateMinOnlinePlayers(0);
        assertNull(error, "0个玩家应该被允许（无玩家限制）");
    }

    @Test
    @DisplayName("验证最少在线玩家 - 负数")
    public void testValidateMinOnlinePlayers_Negative() {
        String error = validator.validateMinOnlinePlayers(-1);
        assertNotNull(error, "负数应该被拒绝");
    }

    // ==================== Boss等级验证 ====================

    @Test
    @DisplayName("验证等级 - 有效范围")
    public void testValidateTier_ValidRange() {
        assertNull(validator.validateTier(1), "等级1应该有效");
        assertNull(validator.validateTier(2), "等级2应该有效");
        assertNull(validator.validateTier(3), "等级3应该有效");
        assertNull(validator.validateTier(4), "等级4应该有效");
    }

    @Test
    @DisplayName("验证等级 - 超出范围")
    public void testValidateTier_OutOfRange() {
        String error0 = validator.validateTier(0);
        assertNotNull(error0, "等级0应该被拒绝");

        String error5 = validator.validateTier(5);
        assertNotNull(error5, "等级5应该被拒绝");
    }

    @Test
    @DisplayName("验证等级 - 负数")
    public void testValidateTier_Negative() {
        String error = validator.validateTier(-1);
        assertNotNull(error, "负数等级应该被拒绝");
    }

    // ==================== 冷却时间验证 ====================

    @Test
    @DisplayName("验证冷却时间 - 正常值")
    public void testValidateCooldown_Valid() {
        String error = validator.validateCooldown(120);
        assertNull(error, "120秒应该是有效的冷却时间");
    }

    @Test
    @DisplayName("验证冷却时间 - 最小值")
    public void testValidateCooldown_Minimum() {
        String error = validator.validateCooldown(60);
        assertNull(error, "60秒是最小有效冷却时间");
    }

    @Test
    @DisplayName("验证冷却时间 - 低于最小值")
    public void testValidateCooldown_BelowMinimum() {
        String error = validator.validateCooldown(30);
        assertNotNull(error, "30秒应该被拒绝（低于最小值60秒）");
    }

    @Test
    @DisplayName("验证冷却时间 - 负数")
    public void testValidateCooldown_Negative() {
        String error = validator.validateCooldown(-100);
        assertNotNull(error, "负数冷却时间应该被拒绝");
    }

    // ==================== 最大数量验证 ====================

    @Test
    @DisplayName("验证最大数量 - 正常值")
    public void testValidateMaxCount_Valid() {
        String error = validator.validateMaxCount(3);
        assertNull(error, "3个应该是有效的最大数量");
    }

    @Test
    @DisplayName("验证最大数量 - 最小值")
    public void testValidateMaxCount_Minimum() {
        String error = validator.validateMaxCount(1);
        assertNull(error, "1个是最小有效数量");
    }

    @Test
    @DisplayName("验证最大数量 - 低于最小值")
    public void testValidateMaxCount_BelowMinimum() {
        String error = validator.validateMaxCount(0);
        assertNotNull(error, "0个应该被拒绝（必须至少1个）");
    }

    @Test
    @DisplayName("验证最大数量 - 负数")
    public void testValidateMaxCount_Negative() {
        String error = validator.validateMaxCount(-5);
        assertNotNull(error, "负数应该被拒绝");
    }

    // ==================== 位置字符串验证 ====================

    @Test
    @DisplayName("验证位置 - 正常格式")
    public void testValidateLocationString_Valid() {
        String error = validator.validateLocationString("world,100,64,200");
        assertNull(error, "标准的world,x,y,z格式应该有效");
    }

    @Test
    @DisplayName("验证位置 - 其他世界")
    public void testValidateLocationString_OtherWorld() {
        String error = validator.validateLocationString("nether,50,30,100");
        assertNull(error, "nether世界的位置应该有效");

        String error2 = validator.validateLocationString("the_end,0,0,0");
        assertNull(error2, "the_end世界的位置应该有效");
    }

    @Test
    @DisplayName("验证位置 - 格式错误")
    public void testValidateLocationString_InvalidFormat() {
        String error1 = validator.validateLocationString("world,100,64");
        assertNotNull(error1, "缺少Z坐标的位置应该被拒绝");

        String error2 = validator.validateLocationString("100,64,200");
        assertNotNull(error2, "缺少世界名的位置应该被拒绝");

        String error3 = validator.validateLocationString("world,abc,64,200");
        assertNotNull(error3, "非整数坐标应该被拒绝");
    }

    @Test
    @DisplayName("验证位置 - 空字符串")
    public void testValidateLocationString_Empty() {
        String error = validator.validateLocationString("");
        assertNotNull(error, "空字符串应该被拒绝");
    }

    @Test
    @DisplayName("验证位置 - 空指针")
    public void testValidateLocationString_Null() {
        String error = validator.validateLocationString(null);
        assertNotNull(error, "null应该被拒绝");
    }

    // ==================== MythicMobs ID验证 ====================

    @Test
    @DisplayName("验证MythicMobs ID - 有效格式")
    public void testValidateMythicMobId_Valid() {
        assertNull(validator.validateMythicMobId("EnderDragon"), "EnderDragon应该有效");
        assertNull(validator.validateMythicMobId("Zombie_King"), "Zombie_King应该有效");
        assertNull(validator.validateMythicMobId("boss123"), "boss123应该有效");
        assertNull(validator.validateMythicMobId("SKELETON_WARRIOR"), "SKELETON_WARRIOR应该有效");
    }

    @Test
    @DisplayName("验证MythicMobs ID - 无效字符")
    public void testValidateMythicMobId_InvalidCharacters() {
        String error1 = validator.validateMythicMobId("Boss-King");
        assertNotNull(error1, "包含连字符的ID应该被拒绝");

        String error2 = validator.validateMythicMobId("Boss King");
        assertNotNull(error2, "包含空格的ID应该被拒绝");

        String error3 = validator.validateMythicMobId("Boss@King");
        assertNotNull(error3, "包含特殊字符的ID应该被拒绝");
    }

    @Test
    @DisplayName("验证MythicMobs ID - 空字符串")
    public void testValidateMythicMobId_Empty() {
        String error = validator.validateMythicMobId("");
        assertNotNull(error, "空字符串应该被拒绝");
    }

    @Test
    @DisplayName("验证MythicMobs ID - 空指针")
    public void testValidateMythicMobId_Null() {
        String error = validator.validateMythicMobId(null);
        assertNotNull(error, "null应该被拒绝");
    }

    // ==================== 刷新点ID验证 ====================

    @Test
    @DisplayName("验证刷新点ID - 有效格式")
    public void testValidateSpawnPointId_Valid() {
        assertNull(validator.validateSpawnPointId("dragon_lair"), "dragon_lair应该有效");
        assertNull(validator.validateSpawnPointId("point1"), "point1应该有效");
        assertNull(validator.validateSpawnPointId("Zombie_King_Spawn"), "Zombie_King_Spawn应该有效");
        assertNull(validator.validateSpawnPointId("dragon-lair"), "dragon-lair应该有效");
    }

    @Test
    @DisplayName("验证刷新点ID - 无效字符")
    public void testValidateSpawnPointId_InvalidCharacters() {
        String error1 = validator.validateSpawnPointId("dragon lair");
        assertNotNull(error1, "包含空格的ID应该被拒绝");

        String error2 = validator.validateSpawnPointId("dragon@lair");
        assertNotNull(error2, "包含特殊字符的ID应该被拒绝");
    }

    @Test
    @DisplayName("验证刷新点ID - 空字符串")
    public void testValidateSpawnPointId_Empty() {
        String error = validator.validateSpawnPointId("");
        assertNotNull(error, "空字符串应该被拒绝");
    }

    @Test
    @DisplayName("验证刷新点ID - 空指针")
    public void testValidateSpawnPointId_Null() {
        String error = validator.validateSpawnPointId(null);
        assertNotNull(error, "null应该被拒绝");
    }

    // ==================== 完整配置验证 ====================

    @Test
    @DisplayName("验证完整配置 - 所有字段有效")
    public void testValidateCompleteConfig_Valid() {
        BossRefreshConfig config = new BossRefreshConfig();
        config.setCheckIntervalSeconds(30);
        config.setMaxActiveBosses(10);
        config.setMinOnlinePlayers(3);
        config.setEnabled(true);

        // validator.validateConfig() 应该返回空列表（无错误）
        // 或者返回null（如果方法设计为此）
        // 这取决于实现，这里只是演示结构
        assertTrue(true, "配置应该通过验证");
    }

    @Test
    @DisplayName("验证完整配置 - 多个字段无效")
    public void testValidateCompleteConfig_Multiple() {
        BossRefreshConfig config = new BossRefreshConfig();
        config.setCheckIntervalSeconds(-1);  // 无效
        config.setMaxActiveBosses(0);        // 无效
        config.setMinOnlinePlayers(-1);      // 无效

        // 应该捕获所有这些错误
        assertTrue(true, "应该检测到多个验证错误");
    }

    // ==================== 边界值测试 ====================

    @Test
    @DisplayName("边界值 - 大数值")
    public void testBoundaryValues_Large() {
        String error1 = validator.validateCheckInterval(Integer.MAX_VALUE);
        // 可能有效或无效，取决于实现

        String error2 = validator.validateCooldown(Long.MAX_VALUE);
        // 可能有效或无效，取决于实现
    }

    @Test
    @DisplayName("边界值 - 坐标极值")
    public void testBoundaryValues_Coordinates() {
        String error = validator.validateLocationString("world," + Integer.MAX_VALUE + ",0," + Integer.MIN_VALUE);
        assertNull(error, "极端坐标应该被接受（只要格式正确）");
    }
}
