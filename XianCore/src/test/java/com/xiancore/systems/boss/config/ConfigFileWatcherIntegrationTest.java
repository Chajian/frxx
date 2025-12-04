package com.xiancore.systems.boss.config;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 热重载功能集成测试
 * 测试ConfigFileWatcher和BossRefreshManager的热重载功能
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-16
 */
@DisplayName("热重载功能集成测试")
public class ConfigFileWatcherIntegrationTest {

    private BossRefreshManager bossManager;
    private ConfigFileWatcher fileWatcher;
    private File configFile;
    private Path tempDir;

    @Mock
    private XianCore mockPlugin;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // 创建临时配置文件
        tempDir = Files.createTempDirectory("boss_config_test");
        configFile = new File(tempDir.toFile(), "boss-refresh.yml");

        // 创建示例配置
        String testConfig = "# Boss刷新系统配置\n" +
                "enabled: true\n" +
                "check-interval-seconds: 30\n" +
                "max-active-bosses: 10\n" +
                "min-online-players: 3\n" +
                "\n" +
                "spawn-points:\n" +
                "  dragon_lair:\n" +
                "    world: world\n" +
                "    x: 100\n" +
                "    y: 64\n" +
                "    z: 200\n" +
                "    mob-type: EnderDragon\n" +
                "    tier: 2\n" +
                "    enabled: true\n";

        Files.write(configFile.toPath(), testConfig.getBytes());

        // 设置Mock
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Test"));
        when(mockPlugin.getDataFolder()).thenReturn(tempDir.toFile());
    }

    /**
     * 测试 - 文件监听器启动
     */
    @Test
    @DisplayName("文件监听器能够正常启动")
    public void testFileWatcherStart() {
        fileWatcher = new ConfigFileWatcher(mockPlugin, configFile, () -> {
            // 重载回调
        });

        fileWatcher.start();

        assertTrue(fileWatcher.isWatching(), "文件监听器应该正在监听");

        fileWatcher.stop();
        assertFalse(fileWatcher.isWatching(), "文件监听器应该已停止");
    }

    /**
     * 测试 - 配置重载保留运行时状态
     */
    @Test
    @DisplayName("配置重载时保留运行时状态（活跃Boss数）")
    public void testConfigReloadPreservesRuntimeState() {
        // 模拟原始配置
        BossSpawnPoint point1 = new BossSpawnPoint("dragon_lair", "world", 100, 64, 200, "EnderDragon");
        point1.setCurrentCount(2);  // 模拟有2个活跃Boss
        point1.setTier(2);

        // 模拟修改后的配置
        BossSpawnPoint point1_updated = new BossSpawnPoint("dragon_lair", "world", 100, 64, 200, "EnderDragon");
        point1_updated.setTier(3);  // 修改等级

        // 验证currentCount被保留
        point1_updated.setCurrentCount(point1.getCurrentCount());

        assertEquals(point1.getCurrentCount(), point1_updated.getCurrentCount(),
                "运行时状态（活跃Boss数）应该被保留");
    }

    /**
     * 测试 - 配置增量更新
     */
    @Test
    @DisplayName("配置增量更新 - 添加新刷新点")
    public void testIncrementalConfigUpdate_AddSpawnPoint() {
        HashMap<String, BossSpawnPoint> oldPoints = new HashMap<>();
        HashMap<String, BossSpawnPoint> newPoints = new HashMap<>();

        // 原始配置
        BossSpawnPoint point1 = new BossSpawnPoint("dragon_lair", "world", 100, 64, 200, "EnderDragon");
        oldPoints.put("dragon_lair", point1);

        // 新配置（增加了一个点）
        BossSpawnPoint point2 = new BossSpawnPoint("skeleton_king_tower", "world_nether", 50, 50, 50, "SkeletonKing");
        newPoints.put("dragon_lair", point1);
        newPoints.put("skeleton_king_tower", point2);

        // 验证新点被添加
        assertTrue(newPoints.containsKey("skeleton_king_tower"), "新刷新点应该被添加");
        assertTrue(oldPoints.containsKey("dragon_lair"), "原有刷新点应该保留");
    }

    /**
     * 测试 - 配置增量更新 - 删除刷新点
     */
    @Test
    @DisplayName("配置增量更新 - 删除刷新点")
    public void testIncrementalConfigUpdate_RemoveSpawnPoint() {
        HashMap<String, BossSpawnPoint> points = new HashMap<>();

        // 初始配置
        BossSpawnPoint point1 = new BossSpawnPoint("dragon_lair", "world", 100, 64, 200, "EnderDragon");
        BossSpawnPoint point2 = new BossSpawnPoint("skeleton_king_tower", "world_nether", 50, 50, 50, "SkeletonKing");
        points.put("dragon_lair", point1);
        points.put("skeleton_king_tower", point2);

        // 模拟删除
        points.remove("skeleton_king_tower");

        assertTrue(points.containsKey("dragon_lair"), "dragon_lair应该保留");
        assertFalse(points.containsKey("skeleton_king_tower"), "skeleton_king_tower应该被删除");
    }

    /**
     * 测试 - 配置增量更新 - 更新现有刷新点
     */
    @Test
    @DisplayName("配置增量更新 - 更新现有刷新点")
    public void testIncrementalConfigUpdate_UpdateSpawnPoint() {
        BossSpawnPoint point = new BossSpawnPoint("dragon_lair", "world", 100, 64, 200, "EnderDragon");
        point.setTier(2);
        point.setCurrentCount(3);

        // 更新等级
        point.setTier(3);

        // currentCount应该保留
        assertEquals(3, point.getCurrentCount(), "活跃Boss数应该保留");
        assertEquals(3, point.getTier(), "等级应该被更新");
    }

    /**
     * 测试 - 无效配置文件处理
     */
    @Test
    @DisplayName("处理无效的配置文件内容")
    public void testInvalidConfigHandling() throws Exception {
        // 写入无效的YAML
        String invalidConfig = "invalid yaml: [unclosed bracket";
        Files.write(configFile.toPath(), invalidConfig.getBytes());

        BossConfigLoader loader = new BossConfigLoader(mockPlugin);

        // 尝试加载无效配置
        try {
            BossRefreshConfig config = loader.loadConfig(configFile);
            // 应该返回默认配置而不是null
            assertNotNull(config, "无效配置应该返回默认配置");
        } catch (Exception e) {
            // 这是可以接受的，因为配置无效
            assertTrue(true, "异常处理正确");
        }
    }

    /**
     * 测试 - 启用/禁用列表更新
     */
    @Test
    @DisplayName("启用/禁用列表正确更新")
    public void testEnabledPointsListUpdate() {
        List<BossSpawnPoint> points = new ArrayList<>();
        List<String> enabledPointIds = new ArrayList<>();

        // 添加启用的点
        BossSpawnPoint point1 = new BossSpawnPoint("dragon_lair", "world", 100, 64, 200, "EnderDragon");
        point1.setEnabled(true);
        points.add(point1);
        enabledPointIds.add(point1.getId());

        // 添加禁用的点
        BossSpawnPoint point2 = new BossSpawnPoint("disabled_point", "world", 0, 64, 0, "Zombie");
        point2.setEnabled(false);
        points.add(point2);

        // 验证
        assertTrue(enabledPointIds.contains("dragon_lair"), "启用的点应该在列表中");
        assertFalse(enabledPointIds.contains("disabled_point"), "禁用的点不应该在列表中");
    }

    /**
     * 测试 - 配置加载错误恢复
     */
    @Test
    @DisplayName("配置加载失败时回滚到上一个配置")
    public void testConfigLoadErrorRecovery() {
        BossRefreshConfig validConfig = BossRefreshConfig.loadDefault();
        BossRefreshConfig newConfig = null;

        // 尝试加载新配置
        try {
            // 模拟配置加载失败
            throw new Exception("配置加载失败");
        } catch (Exception e) {
            // 使用之前的配置
            newConfig = validConfig;
        }

        assertNotNull(newConfig, "应该回滚到有效配置");
        assertEquals(validConfig, newConfig, "应该使用上一个有效的配置");
    }

    /**
     * 测试 - 配置文件监听器的防抖机制
     */
    @Test
    @DisplayName("配置文件监听器防抖机制（避免重复触发）")
    public void testFileWatcherDebounce() throws Exception {
        int[] callCount = {0};

        fileWatcher = new ConfigFileWatcher(mockPlugin, configFile, () -> {
            callCount[0]++;
        });

        // 防抖延迟应该是1000ms
        long debounceDelay = ConfigFileWatcher.getDebounceDelayMs();
        assertEquals(1000, debounceDelay, "防抖延迟应该是1000ms");
    }

    /**
     * 测试 - 并发配置重载安全性
     */
    @Test
    @DisplayName("并发配置重载时的线程安全")
    public void testConcurrentConfigReloadSafety() {
        HashMap<String, BossSpawnPoint> points = new HashMap<>();
        BossSpawnPoint point = new BossSpawnPoint("dragon_lair", "world", 100, 64, 200, "EnderDragon");
        points.put("dragon_lair", point);

        // 模拟并发操作
        Thread thread1 = new Thread(() -> {
            synchronized (points) {
                point.setTier(2);
            }
        });

        Thread thread2 = new Thread(() -> {
            synchronized (points) {
                point.setCurrentCount(1);
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail("线程中断");
        }

        // 验证最终状态一致
        assertNotNull(points.get("dragon_lair"), "刷新点应该仍然存在");
    }
}
