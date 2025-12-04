package com.xiancore.distributed;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 负载均衡器 - 分布式Boss分配策略
 * Load Balancer - Distributed Boss allocation strategies
 *
 * @author XianCore
 * @version 1.0
 */
public class LoadBalancer {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, ServerWeight> serverWeights = new ConcurrentHashMap<>();
    private final List<LoadBalancingStrategy> strategies = new ArrayList<>();
    private LoadBalancingStrategy currentStrategy;

    /**
     * 负载均衡策略接口
     */
    public interface LoadBalancingStrategy {
        String selectServer(Map<String, DistributedBossManager.ServerInfo> servers);
        String getName();
    }

    /**
     * 服务器权重信息
     */
    public static class ServerWeight {
        public String serverId;
        public double weight;            // 权重 (0-1)
        public int successCount;         // 成功分配数
        public int failureCount;         // 失败分配数
        public long lastSelected;        // 最后选择时间
        public double responseTime;      // 平均响应时间 (ms)
        public int consecutiveFailures;  // 连续失败次数

        public ServerWeight(String serverId) {
            this.serverId = serverId;
            this.weight = 1.0;
            this.successCount = 0;
            this.failureCount = 0;
            this.lastSelected = 0;
            this.responseTime = 0.0;
            this.consecutiveFailures = 0;
        }

        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total == 0 ? 1.0 : (double) successCount / total;
        }

        public double getEffectiveWeight() {
            return weight * getSuccessRate() * (1.0 / (1.0 + responseTime / 100.0));
        }
    }

    /**
     * 轮询策略 (Round Robin)
     */
    public class RoundRobinStrategy implements LoadBalancingStrategy {
        private int currentIndex = 0;

        @Override
        public String selectServer(Map<String, DistributedBossManager.ServerInfo> servers) {
            if (servers.isEmpty()) return null;

            List<String> activeServers = servers.values().stream()
                    .filter(s -> s.isActive)
                    .map(s -> s.serverId)
                    .toList();

            if (activeServers.isEmpty()) return null;

            currentIndex = (currentIndex + 1) % activeServers.size();
            return activeServers.get(currentIndex);
        }

        @Override
        public String getName() {
            return "ROUND_ROBIN";
        }
    }

    /**
     * 最少负载策略 (Least Loaded)
     */
    public class LeastLoadedStrategy implements LoadBalancingStrategy {
        @Override
        public String selectServer(Map<String, DistributedBossManager.ServerInfo> servers) {
            if (servers.isEmpty()) return null;

            return servers.values().stream()
                    .filter(s -> s.isActive)
                    .min(Comparator.comparingDouble(s -> s.serverLoad))
                    .map(s -> s.serverId)
                    .orElse(null);
        }

        @Override
        public String getName() {
            return "LEAST_LOADED";
        }
    }

    /**
     * 加权负载策略 (Weighted Load)
     */
    public class WeightedLoadStrategy implements LoadBalancingStrategy {
        @Override
        public String selectServer(Map<String, DistributedBossManager.ServerInfo> servers) {
            if (servers.isEmpty()) return null;

            List<DistributedBossManager.ServerInfo> activeServers = servers.values().stream()
                    .filter(s -> s.isActive)
                    .toList();

            if (activeServers.isEmpty()) return null;

            // 计算加权分数
            double totalScore = 0.0;
            Map<String, Double> scores = new HashMap<>();

            for (DistributedBossManager.ServerInfo server : activeServers) {
                ServerWeight weight = serverWeights.get(server.serverId);
                if (weight == null) {
                    weight = new ServerWeight(server.serverId);
                    serverWeights.put(server.serverId, weight);
                }

                // 分数 = 权重 * 成功率 * (1 - 正规化负载) * 响应时间系数
                double loadFactor = Math.max(0.1, 1.0 - server.serverLoad);
                double responseTimeFactor = 1.0 / (1.0 + weight.responseTime / 100.0);
                double score = weight.getEffectiveWeight() * loadFactor * responseTimeFactor;

                scores.put(server.serverId, score);
                totalScore += score;
            }

            // 随机选择 (根据分数加权)
            double random = Math.random() * totalScore;
            double current = 0.0;

            for (String serverId : scores.keySet()) {
                current += scores.get(serverId);
                if (random <= current) {
                    return serverId;
                }
            }

            return activeServers.get(0).serverId;
        }

        @Override
        public String getName() {
            return "WEIGHTED_LOAD";
        }
    }

    /**
     * 健康度优先策略 (Health-aware)
     */
    public class HealthAwareStrategy implements LoadBalancingStrategy {
        @Override
        public String selectServer(Map<String, DistributedBossManager.ServerInfo> servers) {
            if (servers.isEmpty()) return null;

            return servers.values().stream()
                    .filter(s -> s.isActive && serverWeights.getOrDefault(s.serverId, new ServerWeight(s.serverId)).consecutiveFailures < 3)
                    .min(Comparator
                            .comparingDouble((DistributedBossManager.ServerInfo s) ->
                                    serverWeights.getOrDefault(s.serverId, new ServerWeight(s.serverId))
                                            .consecutiveFailures)
                            .thenComparingDouble(s -> s.serverLoad))
                    .map(s -> s.serverId)
                    .orElse(null);
        }

        @Override
        public String getName() {
            return "HEALTH_AWARE";
        }
    }

    /**
     * 地理位置策略 (Geographic Proximity)
     */
    public class GeographicProximityStrategy implements LoadBalancingStrategy {
        private String preferredRegion = "DEFAULT";

        public void setPreferredRegion(String region) {
            this.preferredRegion = region;
        }

        @Override
        public String selectServer(Map<String, DistributedBossManager.ServerInfo> servers) {
            if (servers.isEmpty()) return null;

            // 优先选择同区域的服务器
            var preferredServers = servers.values().stream()
                    .filter(s -> s.isActive)
                    .filter(s -> s.metadata.getOrDefault("region", "DEFAULT").equals(preferredRegion))
                    .toList();

            if (!preferredServers.isEmpty()) {
                return preferredServers.stream()
                        .min(Comparator.comparingDouble(s -> s.serverLoad))
                        .map(s -> s.serverId)
                        .orElse(null);
            }

            // 备选：选择任何可用的服务器
            return servers.values().stream()
                    .filter(s -> s.isActive)
                    .min(Comparator.comparingDouble(s -> s.serverLoad))
                    .map(s -> s.serverId)
                    .orElse(null);
        }

        @Override
        public String getName() {
            return "GEOGRAPHIC_PROXIMITY";
        }
    }

    /**
     * 会话粘性策略 (Session Sticky)
     */
    public class SessionStickyStrategy implements LoadBalancingStrategy {
        private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

        public void bindSession(String sessionId, String serverId) {
            sessionMap.put(sessionId, serverId);
        }

        @Override
        public String selectServer(Map<String, DistributedBossManager.ServerInfo> servers) {
            if (servers.isEmpty()) return null;

            // 这个策略需要从上下文获取sessionId
            // 这里只是演示结构
            return servers.values().stream()
                    .filter(s -> s.isActive)
                    .min(Comparator.comparingDouble(s -> s.serverLoad))
                    .map(s -> s.serverId)
                    .orElse(null);
        }

        @Override
        public String getName() {
            return "SESSION_STICKY";
        }
    }

    /**
     * 构造函数
     */
    public LoadBalancer() {
        initializeStrategies();
        this.currentStrategy = strategies.get(0);
        logger.info("✓ LoadBalancer已初始化");
    }

    /**
     * 初始化所有策略
     */
    private void initializeStrategies() {
        strategies.add(new RoundRobinStrategy());
        strategies.add(new LeastLoadedStrategy());
        strategies.add(new WeightedLoadStrategy());
        strategies.add(new HealthAwareStrategy());
        strategies.add(new GeographicProximityStrategy());
        strategies.add(new SessionStickyStrategy());
    }

    /**
     * 选择服务器
     */
    public String selectServer(Map<String, DistributedBossManager.ServerInfo> servers) {
        String serverId = currentStrategy.selectServer(servers);

        if (serverId != null && servers.containsKey(serverId)) {
            ServerWeight weight = serverWeights.computeIfAbsent(serverId, ServerWeight::new);
            weight.lastSelected = System.currentTimeMillis();
        }

        return serverId;
    }

    /**
     * 设置负载均衡策略
     */
    public void setStrategy(String strategyName) {
        for (LoadBalancingStrategy strategy : strategies) {
            if (strategy.getName().equals(strategyName)) {
                this.currentStrategy = strategy;
                logger.info("✓ 负载均衡策略已切换: " + strategyName);
                return;
            }
        }
        logger.warning("⚠ 策略不存在: " + strategyName);
    }

    /**
     * 获取当前策略
     */
    public String getCurrentStrategy() {
        return currentStrategy.getName();
    }

    /**
     * 记录成功
     */
    public void recordSuccess(String serverId, double responseTime) {
        ServerWeight weight = serverWeights.computeIfAbsent(serverId, ServerWeight::new);
        weight.successCount++;
        weight.responseTime = (weight.responseTime + responseTime) / 2.0;  // 平均响应时间
        weight.consecutiveFailures = 0;
        weight.weight = Math.min(1.0, weight.weight + 0.01);  // 逐步增加权重
    }

    /**
     * 记录失败
     */
    public void recordFailure(String serverId) {
        ServerWeight weight = serverWeights.computeIfAbsent(serverId, ServerWeight::new);
        weight.failureCount++;
        weight.consecutiveFailures++;
        weight.weight = Math.max(0.1, weight.weight - 0.1);  // 降低权重

        logger.warning("⚠ 服务器分配失败: " + serverId + " (连续失败: " + weight.consecutiveFailures + ")");
    }

    /**
     * 获取所有服务器权重信息
     */
    public Collection<ServerWeight> getAllWeights() {
        return serverWeights.values();
    }

    /**
     * 获取特定服务器权重
     */
    public ServerWeight getServerWeight(String serverId) {
        return serverWeights.get(serverId);
    }

    /**
     * 重置权重
     */
    public void resetWeights() {
        serverWeights.clear();
        logger.info("✓ 所有权重已重置");
    }

    /**
     * 重置特定服务器权重
     */
    public void resetServerWeight(String serverId) {
        serverWeights.remove(serverId);
        logger.info("✓ 服务器权重已重置: " + serverId);
    }

    /**
     * 获取负载均衡统计
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("current_strategy", currentStrategy.getName());
        stats.put("total_servers", serverWeights.size());

        double avgSuccessRate = serverWeights.values().stream()
                .mapToDouble(ServerWeight::getSuccessRate)
                .average()
                .orElse(0.0);

        double avgResponseTime = serverWeights.values().stream()
                .mapToDouble(w -> w.responseTime)
                .average()
                .orElse(0.0);

        stats.put("avg_success_rate", String.format("%.2f%%", avgSuccessRate * 100));
        stats.put("avg_response_time", String.format("%.2fms", avgResponseTime));
        stats.put("server_count", serverWeights.size());

        return stats;
    }
}
