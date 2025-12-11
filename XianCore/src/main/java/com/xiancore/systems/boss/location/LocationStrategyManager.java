package com.xiancore.systems.boss.location;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 位置策略管理器
 * 负责注册、选择和执行位置选择策略
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class LocationStrategyManager {

    private final XianCore plugin;
    private final Logger logger;
    private final Map<String, LocationSelectionStrategy> strategies;
    private final LocationScorer scorer;

    /**
     * 默认候选位置数量
     */
    private static final int DEFAULT_MAX_CANDIDATES = 10;

    public LocationStrategyManager(XianCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.strategies = new HashMap<>();
        this.scorer = new LocationScorer(plugin);

        // 注册默认策略
        registerDefaultStrategies();
    }

    /**
     * 注册默认策略
     */
    private void registerDefaultStrategies() {
        registerStrategy(new FixedLocationStrategy(plugin));
        registerStrategy(new PlayerNearbyLocationStrategy(plugin));
        registerStrategy(new RegionLocationStrategy(plugin));
    }

    /**
     * 注册策略
     *
     * @param strategy 策略实例
     */
    public void registerStrategy(LocationSelectionStrategy strategy) {
        strategies.put(strategy.getName().toLowerCase(), strategy);
        logger.info("✓ 已注册位置选择策略: " + strategy.getName());
    }

    /**
     * 获取策略
     *
     * @param name 策略名称
     * @return 策略实例，未找到返回null
     */
    public LocationSelectionStrategy getStrategy(String name) {
        if (name == null || name.isEmpty()) {
            return strategies.get(FixedLocationStrategy.NAME);
        }
        return strategies.get(name.toLowerCase());
    }

    /**
     * 为刷新点选择最佳生成位置
     * 自动根据刷新点配置选择合适的策略
     *
     * @param point 刷新点配置
     * @return 最佳位置，如果未找到合适位置则返回null
     */
    public Location determineSpawnLocation(BossSpawnPoint point) {
        // 1. 获取适用的策略
        LocationSelectionStrategy strategy = findApplicableStrategy(point);
        if (strategy == null) {
            logger.warning("✗ 未找到适用的位置选择策略: " + point.getSpawnMode());
            return null;
        }

        logger.info("[位置选择] 使用策略: " + strategy.getName() + ", 刷新点: " + point.getId());

        // 2. 生成候选位置
        List<Location> candidates = strategy.generateCandidates(point, DEFAULT_MAX_CANDIDATES);
        if (candidates.isEmpty()) {
            logger.warning("✗ 策略 " + strategy.getName() + " 未能生成任何候选位置");
            return null;
        }

        // 3. 使用评分器选择最佳位置
        Location bestLocation = scorer.selectBestLocation(candidates, point);
        if (bestLocation == null) {
            logger.warning("✗ 评分器未能选择有效位置");
            return null;
        }

        // 4. 强制加载并锁定区块
        bestLocation.getChunk().load();
        bestLocation.getChunk().setForceLoaded(true);

        logger.info("✓ 选定最佳位置: (" +
                bestLocation.getBlockX() + ", " +
                bestLocation.getBlockY() + ", " +
                bestLocation.getBlockZ() + ")");

        return bestLocation;
    }

    /**
     * 查找适用于刷新点的策略
     *
     * @param point 刷新点配置
     * @return 适用的策略，未找到返回默认策略
     */
    private LocationSelectionStrategy findApplicableStrategy(BossSpawnPoint point) {
        String spawnMode = point.getSpawnMode();

        // 1. 尝试直接匹配策略名称
        if (spawnMode != null && !spawnMode.isEmpty()) {
            LocationSelectionStrategy strategy = strategies.get(spawnMode.toLowerCase());
            if (strategy != null) {
                return strategy;
            }
        }

        // 2. 遍历所有策略，检查是否适用
        for (LocationSelectionStrategy strategy : strategies.values()) {
            if (strategy.isApplicable(point)) {
                return strategy;
            }
        }

        // 3. 返回默认策略（固定位置）
        return strategies.get(FixedLocationStrategy.NAME);
    }

    /**
     * 获取评分器
     *
     * @return 位置评分器
     */
    public LocationScorer getScorer() {
        return scorer;
    }

    /**
     * 获取所有已注册的策略名称
     *
     * @return 策略名称列表
     */
    public String[] getRegisteredStrategyNames() {
        return strategies.keySet().toArray(new String[0]);
    }
}
