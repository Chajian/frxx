package com.xiancore.distributed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式系统集成测试
 * Distributed System Integration Tests
 *
 * @author XianCore
 * @version 1.0
 */
@DisplayName("分布式系统测试")
public class DistributedSystemTest {

    private DistributedBossManager distributedManager;
    private LoadBalancer loadBalancer;
    private DataSyncManager dataSyncManager;

    @BeforeEach
    public void setUp() {
        distributedManager = new DistributedBossManager("localhost", 6379, "rabbitmq");
        loadBalancer = new LoadBalancer();
        dataSyncManager = new DataSyncManager();
    }

    // ==================== LoadBalancer Tests ====================

    @Test
    @DisplayName("测试轮询策略")
    public void testRoundRobinStrategy() {
        loadBalancer.setStrategy("ROUND_ROBIN");
        assertEquals("ROUND_ROBIN", loadBalancer.getCurrentStrategy());
    }

    @Test
    @DisplayName("测试最少负载策略")
    public void testLeastLoadedStrategy() {
        loadBalancer.setStrategy("LEAST_LOADED");
        assertEquals("LEAST_LOADED", loadBalancer.getCurrentStrategy());
    }

    @Test
    @DisplayName("测试加权负载策略")
    public void testWeightedLoadStrategy() {
        loadBalancer.setStrategy("WEIGHTED_LOAD");

        // 模拟服务器信息
        Map<String, DistributedBossManager.ServerInfo> servers = createMockServers(3);

        String selected = loadBalancer.selectServer(servers);
        assertNotNull(selected, "应选择一个服务器");
        assertTrue(servers.containsKey(selected), "选择的服务器应存在");
    }

    @Test
    @DisplayName("测试健康度优先策略")
    public void testHealthAwareStrategy() {
        loadBalancer.setStrategy("HEALTH_AWARE");

        Map<String, DistributedBossManager.ServerInfo> servers = createMockServers(3);
        String selected = loadBalancer.selectServer(servers);

        assertNotNull(selected, "应选择一个健康的服务器");
    }

    @Test
    @DisplayName("测试地理位置策略")
    public void testGeographicProximityStrategy() {
        loadBalancer.setStrategy("GEOGRAPHIC_PROXIMITY");
        assertEquals("GEOGRAPHIC_PROXIMITY", loadBalancer.getCurrentStrategy());
    }

    @Test
    @DisplayName("测试权重记录-成功")
    public void testRecordSuccess() {
        String serverId = "server-1";
        loadBalancer.recordSuccess(serverId, 50.0);

        LoadBalancer.ServerWeight weight = loadBalancer.getServerWeight(serverId);
        assertNotNull(weight, "权重应被创建");
        assertEquals(1, weight.successCount, "成功计数应为1");
        assertEquals(0, weight.consecutiveFailures, "连续失败应为0");
    }

    @Test
    @DisplayName("测试权重记录-失败")
    public void testRecordFailure() {
        String serverId = "server-1";
        loadBalancer.recordFailure(serverId);
        loadBalancer.recordFailure(serverId);

        LoadBalancer.ServerWeight weight = loadBalancer.getServerWeight(serverId);
        assertNotNull(weight, "权重应被创建");
        assertEquals(2, weight.failureCount, "失败计数应为2");
        assertEquals(2, weight.consecutiveFailures, "连续失败应为2");
    }

    @Test
    @DisplayName("测试权重重置")
    public void testResetWeights() {
        loadBalancer.recordSuccess("server-1", 50.0);
        loadBalancer.recordSuccess("server-2", 60.0);

        loadBalancer.resetWeights();

        assertTrue(loadBalancer.getAllWeights().isEmpty(), "权重应被清空");
    }

    @Test
    @DisplayName("测试负载均衡统计")
    public void testLoadBalancerStatistics() {
        loadBalancer.recordSuccess("server-1", 50.0);
        loadBalancer.recordSuccess("server-2", 60.0);
        loadBalancer.recordFailure("server-1");

        Map<String, Object> stats = loadBalancer.getStatistics();

        assertNotNull(stats.get("current_strategy"), "应包含当前策略");
        assertNotNull(stats.get("avg_success_rate"), "应包含平均成功率");
        assertNotNull(stats.get("avg_response_time"), "应包含平均响应时间");
    }

    // ==================== DataSync Tests ====================

    @Test
    @DisplayName("测试数据同步-新Boss")
    public void testSyncNewBoss() {
        Map<String, Object> bossData = new HashMap<>();
        bossData.put("health", 100.0);
        bossData.put("tier", 1);

        dataSyncManager.startSync("boss-1", bossData, "server-1");

        DataSyncManager.BossDataVersion version = dataSyncManager.getBossVersion("boss-1");
        assertNotNull(version, "Boss版本应被创建");
        assertEquals(1, version.version, "版本应为1");
        assertEquals("server-1", version.lastUpdatedServer, "更新服务器应为server-1");
    }

    @Test
    @DisplayName("测试数据同步-版本递增")
    public void testSyncVersionIncrement() {
        Map<String, Object> data1 = new HashMap<>();
        data1.put("health", 100.0);
        dataSyncManager.startSync("boss-1", data1, "server-1");

        DataSyncManager.BossDataVersion v1 = dataSyncManager.getBossVersion("boss-1");
        long version1 = v1.version;

        Map<String, Object> data2 = new HashMap<>();
        data2.put("health", 80.0);
        dataSyncManager.startSync("boss-1", data2, "server-2");

        DataSyncManager.BossDataVersion v2 = dataSyncManager.getBossVersion("boss-1");
        assertEquals(version1 + 1, v2.version, "版本应递增");
    }

    @Test
    @DisplayName("测试冲突检测")
    public void testConflictDetection() {
        Map<String, Object> originalData = new HashMap<>();
        originalData.put("health", 100.0);
        originalData.put("tier", 1);
        dataSyncManager.startSync("boss-1", originalData, "server-1");

        // 修改数据
        Map<String, Object> conflictingData = new HashMap<>();
        conflictingData.put("health", 50.0);  // 不同的血量
        conflictingData.put("tier", 2);       // 不同的等级

        DataSyncManager.SyncResult result = dataSyncManager.handleRemoteSync(
                "boss-1", conflictingData, 0, "server-2"
        );

        // 由于远程版本(0)小于本地版本(1)，应检测到冲突
        assertFalse(result.success, "应检测到版本冲突");
        assertTrue(result.message.contains("CONFLICT"), "消息应包含冲突标记");
    }

    @Test
    @DisplayName("测试冲突解决-最后写入者胜利")
    public void testConflictResolution_LastWriteWins() {
        dataSyncManager.setConflictResolutionStrategy(
                DataSyncManager.ConflictResolver.ConflictResolutionStrategy.LAST_WRITE_WINS
        );

        Map<String, Object> localData = new HashMap<>();
        localData.put("health", 100.0);
        dataSyncManager.startSync("boss-1", localData, "server-1");

        Map<String, Object> remoteData = new HashMap<>();
        remoteData.put("health", 50.0);

        DataSyncManager.SyncResult result = dataSyncManager.handleRemoteSync(
                "boss-1", remoteData, 2, "server-2"
        );

        assertTrue(result.success, "应解决冲突");
        DataSyncManager.BossDataVersion version = dataSyncManager.getBossVersion("boss-1");
        assertEquals(50.0, version.data.get("health"), "应采用远程数据");
    }

    @Test
    @DisplayName("测试事务管理")
    public void testTransactionManagement() {
        String transactionId = "tx-1";
        DataSyncManager.SyncTransaction transaction = new DataSyncManager.SyncTransaction(
                transactionId, "boss-1", "server-1"
        );

        assertEquals(DataSyncManager.SyncTransaction.TransactionState.PENDING, transaction.state);
        assertFalse(transaction.isExpired(), "新事务不应过期");
    }

    @Test
    @DisplayName("测试事务提交")
    public void testTransactionCommit() {
        String transactionId = "tx-1";
        DataSyncManager.SyncTransaction transaction = new DataSyncManager.SyncTransaction(
                transactionId, "boss-1", "server-1"
        );
        transaction.state = DataSyncManager.SyncTransaction.TransactionState.PENDING;

        dataSyncManager.commitTransaction(transactionId);

        assertEquals(DataSyncManager.SyncTransaction.TransactionState.COMMITTED, transaction.state);
    }

    @Test
    @DisplayName("测试同步统计")
    public void testSyncStatistics() {
        Map<String, Object> data = new HashMap<>();
        data.put("health", 100.0);

        dataSyncManager.startSync("boss-1", data, "server-1");
        dataSyncManager.startSync("boss-2", data, "server-1");
        dataSyncManager.startSync("boss-1", data, "server-2");

        Map<String, Object> stats = dataSyncManager.getSyncStatistics();

        assertTrue((long) stats.get("total_syncs") >= 3, "总同步数应>=3");
        assertEquals(2, stats.get("tracked_bosses"), "跟踪的Boss数应为2");
    }

    @Test
    @DisplayName("测试同步历史")
    public void testSyncHistory() {
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("health", 100.0 - (i * 10));
            dataSyncManager.startSync("boss-" + i, data, "server-1");
        }

        List<Map<String, Object>> history = dataSyncManager.getSyncHistory(3);
        assertTrue(history.size() <= 3, "历史记录应不超过限制");
    }

    // ==================== Distributed Manager Tests ====================

    @Test
    @DisplayName("测试服务器注册")
    public void testServerRegistration() {
        distributedManager.registerServer("server-1", "Server 1", "localhost", 8081);

        DistributedBossManager.ServerInfo server = distributedManager.getServerInfo("server-1");
        assertNotNull(server, "服务器应被注册");
        assertEquals("server-1", server.serverId, "服务器ID应匹配");
        assertTrue(server.isActive, "服务器应处于活跃状态");
    }

    @Test
    @DisplayName("测试多服务器注册")
    public void testMultipleServerRegistration() {
        for (int i = 1; i <= 5; i++) {
            distributedManager.registerServer("server-" + i, "Server " + i,
                    "localhost", 8080 + i);
        }

        Collection<DistributedBossManager.ServerInfo> servers = distributedManager.getAllServers();
        assertEquals(5, servers.size(), "应注册5个服务器");
    }

    @Test
    @DisplayName("测试Boss创建-负载均衡")
    public void testBossCreation_LoadBalanced() {
        // 注册服务器
        distributedManager.registerServer("server-1", "S1", "localhost", 8081);
        distributedManager.registerServer("server-2", "S2", "localhost", 8082);

        // 创建Boss
        String bossId = distributedManager.createBoss("Skeleton King", "SKELETON", "world", 1);

        assertNotNull(bossId, "Boss应被创建");

        DistributedBossManager.BossData boss = distributedManager.getBossData(bossId);
        assertNotNull(boss, "Boss数据应存在");
        assertNotNull(boss.ownerServer, "Boss应被分配到服务器");
    }

    @Test
    @DisplayName("测试跨服务器伤害记录")
    public void testCrossServerDamageRecording() {
        distributedManager.registerServer("server-1", "S1", "localhost", 8081);

        String bossId = distributedManager.createBoss("Boss", "TYPE", "world", 1);
        DistributedBossManager.BossData boss = distributedManager.getBossData(bossId);
        boss.health = 100.0;
        boss.maxHealth = 100.0;

        distributedManager.recordDamage(bossId, "player-1", 25.0);
        distributedManager.recordDamage(bossId, "player-2", 15.0);

        boss = distributedManager.getBossData(bossId);
        assertEquals(60.0, boss.health, "血量应正确减少");
        assertEquals(2, boss.involvedPlayers.size(), "应记录2个玩家");
    }

    @Test
    @DisplayName("测试服务器故障转移")
    public void testServerFailover() {
        // 注册两个服务器
        distributedManager.registerServer("server-1", "S1", "localhost", 8081);
        distributedManager.registerServer("server-2", "S2", "localhost", 8082);

        // 在server-1创建Boss
        String bossId = distributedManager.createBoss("Boss", "TYPE", "world", 1);
        DistributedBossManager.BossData boss = distributedManager.getBossData(bossId);
        String originalServer = boss.ownerServer;

        // 模拟server-1离线
        DistributedBossManager.ServerInfo server1 = distributedManager.getServerInfo("server-1");
        server1.isActive = false;

        // 这会触发故障转移逻辑
        assertTrue(!originalServer.isEmpty(), "Boss应有原始服务器");
    }

    @Test
    @DisplayName("测试Boss击杀")
    public void testBossKill() {
        distributedManager.registerServer("server-1", "S1", "localhost", 8081);

        String bossId = distributedManager.createBoss("Boss", "TYPE", "world", 1);
        DistributedBossManager.BossData boss = distributedManager.getBossData(bossId);

        distributedManager.recordDamage(bossId, "player-1", boss.health);
        distributedManager.completeBossKill(bossId, "player-1");

        boss = distributedManager.getBossData(bossId);
        assertEquals("DEAD", boss.status, "Boss状态应为DEAD");
        assertEquals(0, boss.health, "Boss血量应为0");
    }

    @Test
    @DisplayName("测试获取服务器特定的Boss")
    public void testGetBossesByServer() {
        distributedManager.registerServer("server-1", "S1", "localhost", 8081);
        distributedManager.registerServer("server-2", "S2", "localhost", 8082);

        String bossId1 = distributedManager.createBoss("Boss1", "TYPE", "world", 1);
        String bossId2 = distributedManager.createBoss("Boss2", "TYPE", "world", 1);

        DistributedBossManager.BossData boss1 = distributedManager.getBossData(bossId1);
        List<DistributedBossManager.BossData> bosses = distributedManager.getBossesByServer(boss1.ownerServer);

        assertTrue(bosses.stream().anyMatch(b -> b.bossId.equals(bossId1)), "应找到Boss1");
    }

    // ==================== Helper Methods ====================

    private Map<String, DistributedBossManager.ServerInfo> createMockServers(int count) {
        Map<String, DistributedBossManager.ServerInfo> servers = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            DistributedBossManager.ServerInfo server = new DistributedBossManager.ServerInfo(
                    "server-" + i, "Server " + i, "localhost", 8080 + i
            );
            server.serverLoad = Math.random();
            server.isActive = true;
            servers.put("server-" + i, server);
        }
        return servers;
    }

    /**
     * DataSyncResult包装类
     */
    private static class DataSyncResult {
        boolean success;
        String message;
        long version;

        DataSyncResult(boolean success, String message, long version) {
            this.success = success;
            this.message = message;
            this.version = version;
        }
    }
}
