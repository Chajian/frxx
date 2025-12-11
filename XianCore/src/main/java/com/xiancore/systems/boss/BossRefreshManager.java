package com.xiancore.systems.boss;

import com.xiancore.XianCore;
import com.xiancore.integration.mythic.MythicIntegration;
import com.xiancore.systems.boss.config.BossConfigLoader;
import com.xiancore.systems.boss.config.BossRefreshConfig;
import com.xiancore.systems.boss.config.ConfigFileWatcher;
import com.xiancore.systems.boss.entity.BossEntity;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import com.xiancore.systems.boss.event.BossDespawnedEvent;
import com.xiancore.systems.boss.event.BossKilledEvent;
import com.xiancore.systems.boss.event.BossSpawnedEvent;
import com.xiancore.systems.boss.handler.BossAttributeManager;
import com.xiancore.systems.boss.announcement.BossAnnouncementManager;
import com.xiancore.systems.boss.location.LocationSelector;
import com.xiancore.systems.boss.location.LocationStrategyManager;
import com.xiancore.systems.boss.location.SelectionCriteria;
import com.xiancore.systems.boss.teleport.BossTeleportManager;
import com.xiancore.systems.boss.reward.BossRewardManager;
import com.xiancore.systems.boss.spawner.MobSpawner;
import com.xiancore.systems.boss.spawner.MythicMobsSpawner;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Boss刷新管理器 - Boss系统的核心管理器
 *
 * 职责:
 * - 定时检查和执行Boss刷新
 * - 管理所有刷新点 (SpawnPoint)
 * - 管理活跃的Boss实体
 * - 协调与其他系统的集成
 * - 发送系统事件
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossRefreshManager {

    // ==================== 核心依赖 ====================
    private final XianCore plugin;
    private final MythicIntegration mythicIntegration;
    private final MobSpawner mobSpawner;
    private final BossEventBus eventBus;
    private final BossDataManager dataManager;
    private final BossConfigLoader configLoader;
    private final BossAttributeManager attributeManager;
    private final BossAnnouncementManager announcementManager;
    private final LocationSelector locationSelector;
    private final LocationStrategyManager locationStrategyManager;
    private final BossTeleportManager teleportManager;
    private final BossRewardManager rewardManager;
    private BossRefreshConfig refreshConfig;

    // 配置文件监听器 (热重载)
    private ConfigFileWatcher configFileWatcher;

    // 可选依赖 (后续)
    // LocationSelectionManager 和 AnnouncementManager 将在 Week 5+ 集成
    // private LocationSelectionManager locationSelectionManager;
    // private AnnouncementManager announcementManager;

    // ==================== 配置 ====================
    private FileConfiguration config;
    private int checkIntervalTicks = 600;  // 30秒
    private int maxActiveBosses = 10;
    private int minOnlinePlayers = 3;

    // ==================== 刷新点管理 ====================
    /** 所有刷新点 */
    private final Map<String, BossSpawnPoint> spawnPoints = new ConcurrentHashMap<>();

    /** 启用的刷新点ID列表 */
    private final List<String> enabledPoints = Collections.synchronizedList(new ArrayList<>());

    // ==================== 活跃Boss管理 ====================
    /** 活跃的Boss实体 (bossUUID -> BossEntity) */
    private final Map<UUID, BossEntity> activeBosses = new ConcurrentHashMap<>();
    private final Map<UUID, String> bossUUIDToSpawnPoint = new ConcurrentHashMap<>();
    private final Map<LivingEntity, UUID> entityToBossUUID = new ConcurrentHashMap<>();

    /** Boss ID -> UUID 映射 (用于快速查询) */
    private final Map<String, UUID> bossIdToUUID = new ConcurrentHashMap<>();

    // ==================== 状态管理 ====================
    private volatile boolean initialized = false;
    private volatile boolean enabled = false;

    /** 刷新检查任务 */
    private BukkitTask refreshTask;

    /** 健康检查任务 */
    private BukkitTask healthMonitorTask;

    // ==================== 统计信息 ====================
    private volatile int totalBossesSpawned = 0;
    private volatile int totalBossesKilled = 0;
    private volatile long lastRefreshTime = 0;

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     */
    public BossRefreshManager(XianCore plugin, MythicIntegration mythicIntegration) {
        this.plugin = plugin;
        this.mythicIntegration = mythicIntegration;
        this.mobSpawner = new MythicMobsSpawner(mythicIntegration);
        this.eventBus = new BossEventBus();
        this.dataManager = new BossDataManager(plugin);
        this.configLoader = new BossConfigLoader(plugin);
        this.attributeManager = new BossAttributeManager(plugin, mythicIntegration);
        this.announcementManager = new BossAnnouncementManager();
        this.locationSelector = new LocationSelector(SelectionCriteria.createBalanced());
        this.locationStrategyManager = new LocationStrategyManager(plugin);
        this.teleportManager = new BossTeleportManager();
        this.rewardManager = new BossRewardManager(plugin, mythicIntegration);
    }

    // ==================== 初始化和启用/禁用 ====================

    /**
     * 初始化Boss刷新管理器
     * 加载配置、注册刷新点、启动定时任务
     *
     * @throws RuntimeException 如果初始化失败
     */
    public void initialize() {
        if (initialized) {
            plugin.getLogger().warning("BossRefreshManager already initialized!");
            return;
        }

        try {
            plugin.getLogger().info("Initializing BossRefreshManager...");

            // 1. 加载配置文件
            loadConfig();

            // 2. 解析刷新点配置
            parseSpawnPoints();

            // 3. 初始化依赖项
            dataManager.initialize();
            announcementManager.initialize();
            teleportManager.initialize();
            rewardManager.initialize();

            plugin.getLogger().info("LocationSelector initialized with BALANCED strategy");
            plugin.getLogger().info("LocationStrategyManager initialized with strategies: " +
                    String.join(", ", locationStrategyManager.getRegisteredStrategyNames()));
            plugin.getLogger().info("BossTeleportManager initialized");
            plugin.getLogger().info("BossRewardManager initialized");

            // 4. 标记为已初始化
            initialized = true;

            plugin.getLogger().info("BossRefreshManager initialized successfully!");
            plugin.getLogger().info("Registered " + spawnPoints.size() + " spawn points");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize BossRefreshManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 启用Boss刷新系统
     * 启动定时检查任务
     */
    public void enable() {
        if (!initialized) {
            plugin.getLogger().severe("BossRefreshManager not initialized! Call initialize() first!");
            return;
        }

        if (enabled) {
            plugin.getLogger().warning("BossRefreshManager already enabled!");
            return;
        }

        try {
            plugin.getLogger().info("Enabling BossRefreshManager...");

            // 1. 启动刷新检查任务 (每30秒)
            startRefreshTask();

            // 2. 启动健康检查任务 (每秒)
            startHealthMonitorTask();

            // 3. 启动配置文件监听器 (热重载)
            startConfigFileWatcher();

            // 4. 标记为启用
            enabled = true;

            plugin.getLogger().info("BossRefreshManager enabled successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable BossRefreshManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 禁用Boss刷新系统
     * 停止定时任务、清理Boss、保存数据
     */
    public void disable() {
        if (!enabled) {
            return;
        }

        try {
            plugin.getLogger().info("Disabling BossRefreshManager...");

            // 1. 停止所有定时任务
            if (refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
            if (healthMonitorTask != null) {
                healthMonitorTask.cancel();
                healthMonitorTask = null;
            }

            // 2. 停止配置文件监听器
            if (configFileWatcher != null) {
                configFileWatcher.stop();
                configFileWatcher = null;
            }

            // 3. 保存所有活跃Boss的历史数据
            for (BossEntity boss : activeBosses.values()) {
                try {
                    dataManager.saveBossDespawned(boss);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save boss data: " + boss.getBossUUID());
                }
            }

            // 4. 清空缓存
            activeBosses.clear();
            entityToBossUUID.clear();
            bossIdToUUID.clear();

            // 5. 关闭公告系统
            announcementManager.shutdown();

            // 6. 标记为禁用
            enabled = false;

            plugin.getLogger().info("BossRefreshManager disabled successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Error disabling BossRefreshManager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 刷新点管理 ====================

    /**
     * 注册刷新点
     *
     * @param point 刷新点对象
     * @throws IllegalArgumentException 如果已存在相同ID的刷新点
     */
    public void registerSpawnPoint(BossSpawnPoint point) {
        if (spawnPoints.containsKey(point.getId())) {
            throw new IllegalArgumentException("Spawn point with ID '" + point.getId() + "' already exists!");
        }

        spawnPoints.put(point.getId(), point);
        enabledPoints.add(point.getId());
        plugin.getLogger().info("Registered spawn point: " + point.getId());
    }

    /**
     * 注销刷新点
     * 不会影响已生成的Boss
     *
     * @param pointId 刷新点ID
     */
    public void unregisterSpawnPoint(String pointId) {
        if (spawnPoints.containsKey(pointId)) {
            spawnPoints.remove(pointId);
            enabledPoints.remove(pointId);
            plugin.getLogger().info("Unregistered spawn point: " + pointId);
        }
    }

    /**
     * 获取所有刷新点
     */
    public List<BossSpawnPoint> getAllSpawnPoints() {
        return new ArrayList<>(spawnPoints.values());
    }

    /**
     * 获取启用的刷新点
     */
    public List<BossSpawnPoint> getEnabledSpawnPoints() {
        return enabledPoints.stream()
            .map(spawnPoints::get)
            .filter(p -> p != null)
            .collect(Collectors.toList());
    }

    // ==================== 主要刷新逻辑 ====================

    /**
     * 启动刷新检查任务
     * 每30秒检查一次是否需要刷新Boss
     */
    private void startRefreshTask() {
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndRefresh, checkIntervalTicks, checkIntervalTicks);
    }

    /**
     * 启动健康检查任务
     * 每秒检查一次活跃Boss的状态
     */
    private void startHealthMonitorTask() {
        healthMonitorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkBossHealth, 20L, 20L);
    }

    /**
     * 启动配置文件监听器 (热重载)
     * 自动监听boss-refresh.yml文件变更
     */
    private void startConfigFileWatcher() {
        try {
            File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
            configFileWatcher = new ConfigFileWatcher(
                plugin,
                configFile,
                this::reloadConfigFileWithErrorHandling
            );
            configFileWatcher.start();
        } catch (Exception e) {
            plugin.getLogger().warning("✗ 启动配置文件监听器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 定时检查是否需要刷新
     * 由BukkitScheduler每30秒调用一次
     */
    public void checkAndRefresh() {
        if (!enabled) {
            return;
        }

        try {
            // 1. 检查在线玩家数
            if (Bukkit.getOnlinePlayers().size() < minOnlinePlayers) {
                return;
            }

            // 2. 检查活跃Boss总数
            if (activeBosses.size() >= maxActiveBosses) {
                return;
            }

            // 3. 遍历所有启用的刷新点
            for (BossSpawnPoint point : getEnabledSpawnPoints()) {
                if (activeBosses.size() >= maxActiveBosses) {
                    break;
                }
                attemptSpawn(point);
            }

            // 4. 更新最后刷新时间
            lastRefreshTime = System.currentTimeMillis();
        } catch (Exception e) {
            plugin.getLogger().severe("Error in checkAndRefresh: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查Boss的健康状态
     * 移除死亡的Boss实体
     */
    private void checkBossHealth() {
        try {
            List<UUID> deadBosses = new ArrayList<>();
            long currentTime = System.currentTimeMillis();
            int healthCheckDelay = 5000; // 5秒延迟

            for (Map.Entry<UUID, BossEntity> entry : activeBosses.entrySet()) {
                BossEntity boss = entry.getValue();
                
                // 延迟健康检查：Boss生成后5秒内不检查
                long aliveTime = currentTime - boss.getSpawnTime();
                if (aliveTime < healthCheckDelay) {
                    continue; // 跳过新生成的Boss
                }
                
                // 检查Boss是否有效
                if (!boss.isValid()) {
                    plugin.getLogger().warning("Boss无效: " + boss.getMythicMobType() + 
                        " (UUID: " + boss.getBossUUID() + ", 存活: " + (aliveTime / 1000) + "秒)");
                    deadBosses.add(entry.getKey());
                }
            }

            // 移除死亡的Boss
            for (UUID bossUUID : deadBosses) {
                BossEntity boss = activeBosses.get(bossUUID);
                if (boss != null) {
                    onBossDespawned(boss);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking boss health: " + e.getMessage());
        }
    }

    /**
     * 尝试在指定刷新点生成Boss
     *
     * @param point 刷新点
     */
    private void attemptSpawn(BossSpawnPoint point) {
        try {
            // 检查是否准备好刷新
            if (!point.isReadyToSpawn()) {
                return;
            }

            // 检查点的当前Boss数
            if (point.getCurrentCount() >= point.getMaxCount()) {
                return;
            }

            // 执行生成
            spawnBossAtPoint(point);
        } catch (Exception e) {
            plugin.getLogger().warning("Error attempting spawn at point " + point.getId() + ": " + e.getMessage());
        }
    }

    /**
     * 在指定刷新点生成Boss
     *
     * @param point 刷新点
     */
    private void spawnBossAtPoint(BossSpawnPoint point) {
        try {
            // 1. 确定生成位置
            Location spawnLoc = determineSpawnLocation(point);
            if (spawnLoc == null) {
                plugin.getLogger().warning("Cannot find valid spawn location for point: " + point.getId());
                return;
            }

            // 2. 选择MobType
            String mobType = selectMobType(point);

            // 3. 生成Boss
            UUID bossUUID = spawnBossAtLocation(spawnLoc, mobType, point.getTier());
            if (bossUUID == null) {
                plugin.getLogger().warning("Failed to spawn boss at point: " + point.getId());
                return;
            }

            // 4. 记录生成与关联
            point.recordSpawn(bossUUID);
            bossUUIDToSpawnPoint.put(bossUUID, point.getId());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn boss at point: " + point.getId() + " - " + e.getMessage());
        }
    }

    /**
     * 在指定位置生成Boss
     *
     * @param location 生成位置
     * @param mythicMobId MythicMobs ID
     * @param tier Boss等级 (1-4)
     * @return 生成的Boss UUID，失败返回null
     */
    public UUID spawnBossAtLocation(Location location, String mythicMobId, int tier) {
        try {
            // 1. 调用 MobSpawner 生成实体
            LivingEntity entity = mobSpawner.spawn(mythicMobId, location);
            if (entity == null) {
                plugin.getLogger().warning("Failed to spawn MythicMob: " + mythicMobId);
                return null;
            }

            // 2. 创建BossEntity
            UUID bossUUID = UUID.randomUUID();
            BossEntity boss = new BossEntity(
                bossUUID,
                entity,
                mythicMobId,
                tier,
                location,
                System.currentTimeMillis()
            );

            // 3. 标记为Boss
            markAsBoss(entity, bossUUID, tier);

            // 4. 应用 Tier 属性倍数
            attributeManager.applyBossAttributesByTier(entity, tier);

            // 5. 添加到activeBosses
            activeBosses.put(bossUUID, boss);
            entityToBossUUID.put(entity, bossUUID);

            // 6. 发送事件
            eventBus.publishEvent(new BossSpawnedEvent(boss));

            // 7. 保存到数据库
            // 6. 保存到数据库
            try {
                dataManager.saveBossSpawned(boss);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save boss data: " + e.getMessage());
            }

            totalBossesSpawned++;

            plugin.getLogger().info("Boss spawned: " + mythicMobId + " at " + location + " (Tier: " + tier + ")");
            return bossUUID;
        } catch (Exception e) {
            plugin.getLogger().severe("Error spawning boss at location: " + location + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取所有活跃的Boss
     */
    public List<BossEntity> getActiveBosses() {
        return new ArrayList<>(activeBosses.values());
    }

    /**
     * 按UUID获取Boss实体
     */
    public BossEntity getBossEntity(UUID bossUUID) {
        return activeBosses.get(bossUUID);
    }

    /**
     * 按Bukkit实体获取Boss实体
     */
    public BossEntity getBossEntityByMythicMob(LivingEntity entity) {
        UUID bossUUID = entityToBossUUID.get(entity);
        return bossUUID != null ? activeBosses.get(bossUUID) : null;
    }

    /**
     * 获取Boss总数 (已生成)
     */
    public int getTotalBossesSpawned() {
        return totalBossesSpawned;
    }

    /**
     * 获取Boss总数 (已击杀)
     */
    public int getTotalBossesKilled() {
        return totalBossesKilled;
    }

    /**
     * 获取活跃Boss数量
     */
    public int getActiveBossCount() {
        return activeBosses.size();
    }

    /**
     * 事件总线访问器（避免在某些环境下 Lombok 失效导致无法生成 getter）
     */
    public BossEventBus getEventBus() {
        return eventBus;
    }

    /**
     * 公告管理器访问器
     */
    public com.xiancore.systems.boss.announcement.BossAnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    /**
     * 传送管理器访问器
     */
    public BossTeleportManager getTeleportManager() {
        return teleportManager;
    }

    /**
     * 奖励管理器访问器
     */
    public BossRewardManager getRewardManager() {
        return rewardManager;
    }

    // ==================== 事件回调 ====================

    /**
     * 当Boss被击杀时调用
     */
    public void onBossKilled(BossEntity boss, Player killer) {
        try {
            if (!activeBosses.containsValue(boss)) {
                return;
            }

            UUID bossUUID = boss.getBossUUID();

            // 标记为已死亡
            boss.markAsKilled(killer);

            // 触发事件
            eventBus.publishEvent(new BossKilledEvent(boss, killer));

            // 保存数据
            try {
                dataManager.saveBossKilled(boss);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save killed boss data: " + e.getMessage());
            }

            // 回退刷新点计数
            String spawnPointId = bossUUIDToSpawnPoint.remove(bossUUID);
            if (spawnPointId != null) {
                BossSpawnPoint point = spawnPoints.get(spawnPointId);
                if (point != null) {
                    point.decrementCount();
                    plugin.getLogger().fine("Decremented count for spawn point: " + spawnPointId + " (now: " + point.getCurrentCount() + ")");
                }
            }

            // 清理
            removeBoss(bossUUID);
            totalBossesKilled++;

            plugin.getLogger().info("Boss killed: " + boss.getMythicMobType() + " by " + killer.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling boss kill: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 当Boss自然消失时调用
     */
    public void onBossDespawned(BossEntity boss) {
        try {
            UUID bossUUID = boss.getBossUUID();

            // 标记为已消失
            boss.markAsDespawned();

            // 触发事件
            eventBus.publishEvent(new BossDespawnedEvent(boss, "despawned"));

            // 保存数据
            try {
                dataManager.saveBossDespawned(boss);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save despawned boss data: " + e.getMessage());
            }

            // 回退刷新点计数
            String spawnPointId = bossUUIDToSpawnPoint.remove(bossUUID);
            if (spawnPointId != null) {
                BossSpawnPoint point = spawnPoints.get(spawnPointId);
                if (point != null) {
                    point.decrementCount();
                    plugin.getLogger().fine("Decremented count for spawn point: " + spawnPointId + " (now: " + point.getCurrentCount() + ")");
                }
            }

            // 清理
            removeBoss(bossUUID);

            plugin.getLogger().info("Boss despawned: " + boss.getMythicMobType());
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling boss despawn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 加载配置文件（支持双模式：YAML或MySQL）
     */
    private void loadConfig() {
        try {
            // 读取存储类型配置
            String storageType = plugin.getConfig().getString("boss-refresh.storage-type", "yaml");
            plugin.getLogger().info("✓ Boss配置存储模式: " + storageType.toUpperCase());

            // 根据配置选择加载方式
            if ("mysql".equalsIgnoreCase(storageType)) {
                // MySQL模式
                if (plugin.getDataManager() != null && plugin.getDataManager().isUsingMySql()) {
                    try (java.sql.Connection conn = plugin.getDataManager().getConnection()) {
                        refreshConfig = configLoader.loadConfigFromDatabase(conn);
                        plugin.getLogger().info("✓ 已从MySQL加载Boss配置");
                    } catch (Exception e) {
                        plugin.getLogger().warning("✗ MySQL加载失败，降级到YAML模式");
                        e.printStackTrace();
                        // 降级到YAML
                        File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
                        refreshConfig = configLoader.loadConfig(configFile);
                    }
                } else {
                    plugin.getLogger().warning("✗ MySQL未启用，降级到YAML模式");
                    File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
                    refreshConfig = configLoader.loadConfig(configFile);
                }
            } else {
                // YAML模式（默认）
                File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
                refreshConfig = configLoader.loadConfig(configFile);
            }

            // 应用全局配置
            checkIntervalTicks = refreshConfig.getCheckIntervalSeconds() * 20;  // 转换为tick (秒 * 20)
            maxActiveBosses = refreshConfig.getMaxActiveBosses();
            minOnlinePlayers = refreshConfig.getMinOnlinePlayers();

            plugin.getLogger().info("✓ Boss系统配置已加载");
            plugin.getLogger().info("  - 检查间隔: " + refreshConfig.getCheckIntervalSeconds() + "秒");
            plugin.getLogger().info("  - 最大Boss数: " + maxActiveBosses);
            plugin.getLogger().info("  - 最少玩家数: " + minOnlinePlayers);

        } catch (Exception e) {
            plugin.getLogger().severe("✗ 加载Boss配置失败: " + e.getMessage());
            e.printStackTrace();

            // 使用默认配置
            refreshConfig = BossRefreshConfig.loadDefault();
            checkIntervalTicks = 600;
            maxActiveBosses = 10;
            minOnlinePlayers = 3;
        }
    }

    /**
     * 解析刷新点配置
     */
    private void parseSpawnPoints() {
        try {
            if (refreshConfig == null) {
                plugin.getLogger().warning("refreshConfig is null, skipping spawn point parsing");
                return;
            }

            spawnPoints.clear();
            enabledPoints.clear();

            // 从配置中获取所有刷新点
            for (BossSpawnPoint point : refreshConfig.getSpawnPoints()) {
                // 验证刷新点
                List<String> errors = point.getValidationErrors();
                if (!errors.isEmpty()) {
                    plugin.getLogger().warning("✗ 刷新点 " + point.getId() + " 验证失败:");
                    for (String error : errors) {
                        plugin.getLogger().warning("  - " + error);
                    }
                    continue;
                }

                // 注册刷新点
                spawnPoints.put(point.getId(), point);

                // 如果启用，添加到启用列表
                if (point.isEnabled()) {
                    enabledPoints.add(point.getId());
                }

                plugin.getLogger().info("✓ 已注册刷新点: " + point.getId() +
                    " (位置: " + point.getLocationString() +
                    ", Boss: " + point.getMythicMobId() +
                    ", 等级: " + point.getTier() + ")");
            }

            plugin.getLogger().info("✓ 刷新点解析完成: " + spawnPoints.size() + " 个点位");

        } catch (Exception e) {
            plugin.getLogger().severe("✗ 解析刷新点配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 配置保存 ====================

    /**
     * 保存当前配置到文件或数据库（支持双模式）
     * 在命令修改后调用以持久化更改
     *
     * @return 是否保存成功
     */
    public boolean saveCurrentConfig() {
        try {
            if (refreshConfig == null) {
                plugin.getLogger().warning("refreshConfig is null, cannot save");
                return false;
            }

            // 更新refreshConfig中的刷新点列表（确保与内存中的一致）
            refreshConfig.setSpawnPoints(new ArrayList<>(spawnPoints.values()));

            // 读取存储类型配置
            String storageType = plugin.getConfig().getString("boss-refresh.storage-type", "yaml");

            // 根据配置选择保存方式
            if ("mysql".equalsIgnoreCase(storageType)) {
                // MySQL模式
                if (plugin.getDataManager() != null && plugin.getDataManager().isUsingMySql()) {
                    try (java.sql.Connection conn = plugin.getDataManager().getConnection()) {
                        configLoader.saveConfigToDatabase(refreshConfig, conn);
                        plugin.getLogger().info("✓ Boss配置已保存到MySQL");
                        return true;
                    } catch (Exception e) {
                        plugin.getLogger().severe("✗ 保存到MySQL失败: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                } else {
                    plugin.getLogger().warning("✗ MySQL未启用，无法保存到数据库");
                    return false;
                }
            } else {
                // YAML模式（默认）
                File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
                configLoader.saveConfig(refreshConfig, configFile);
                plugin.getLogger().info("✓ Boss配置已保存到YAML文件");
                return true;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("✗ 保存Boss配置失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 带错误处理的配置文件重载 (热重载)
     * 在配置文件监听器中使用
     */
    private void reloadConfigFileWithErrorHandling() {
        try {
            plugin.getLogger().info("✓ 开始热重载配置...");

            // 保存当前配置作为备份
            BossRefreshConfig backupConfig = refreshConfig;

            // 尝试加载新配置
            File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
            BossRefreshConfig newConfig = configLoader.loadConfig(configFile);

            if (newConfig == null) {
                throw new IllegalStateException("配置加载返回null");
            }

            // 应用新配置
            refreshConfig = newConfig;
            checkIntervalTicks = refreshConfig.getCheckIntervalSeconds() * 20;
            maxActiveBosses = refreshConfig.getMaxActiveBosses();
            minOnlinePlayers = refreshConfig.getMinOnlinePlayers();

            // 更新刷新点
            updateSpawnPointsFromConfig();

            plugin.getLogger().info("✓ 热重载配置成功");
            plugin.getLogger().info("  - 刷新点数: " + spawnPoints.size());
            plugin.getLogger().info("  - 启用点数: " + enabledPoints.size());

        } catch (Exception e) {
            plugin.getLogger().severe("✗ 热重载配置失败: " + e.getMessage());
            plugin.getLogger().warning("  - 配置已回滚到上一个稳定版本");
            e.printStackTrace();
        }
    }

    /**
     * 从配置更新刷新点 (热重载使用)
     * 保留运行时状态（活跃Boss等）
     */
    private void updateSpawnPointsFromConfig() {
        try {
            // 获取新配置中的刷新点
            Map<String, BossSpawnPoint> newPoints = new HashMap<>();
            for (BossSpawnPoint point : refreshConfig.getSpawnPoints()) {
                newPoints.put(point.getId(), point);
            }

            // 处理已删除的刷新点
            spawnPoints.keySet().removeIf(id -> !newPoints.containsKey(id));

            // 处理新增和更新的刷新点
            for (Map.Entry<String, BossSpawnPoint> entry : newPoints.entrySet()) {
                String id = entry.getKey();
                BossSpawnPoint newPoint = entry.getValue();

                if (spawnPoints.containsKey(id)) {
                    // 更新现有刷新点（保留运行时状态）
                    BossSpawnPoint oldPoint = spawnPoints.get(id);
                    // 只更新配置项，保留当前计数
                    newPoint.setCurrentCount(oldPoint.getCurrentCount());
                    spawnPoints.put(id, newPoint);
                } else {
                    // 添加新刷新点
                    spawnPoints.put(id, newPoint);
                }
            }

            // 更新启用列表
            enabledPoints.clear();
            for (BossSpawnPoint point : spawnPoints.values()) {
                if (point.isEnabled()) {
                    enabledPoints.add(point.getId());
                }
            }

            plugin.getLogger().info("✓ 刷新点已更新: " + spawnPoints.size() + " 个");

        } catch (Exception e) {
            plugin.getLogger().severe("✗ 更新刷新点失败: " + e.getMessage());
            throw new RuntimeException("Failed to update spawn points", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 确定生成位置
     * 委托给 LocationStrategyManager 处理
     * 支持固定位置、随机偏移和玩家附近随机等多种策略
     */
    private Location determineSpawnLocation(BossSpawnPoint point) {
        return locationStrategyManager.determineSpawnLocation(point);
    }

    /**
     * 选择要生成的MobType
     * 简化版: 直接返回该刷新点配置的mythicMobId
     */
    private String selectMobType(BossSpawnPoint point) {
        String mobType = point.getMythicMobId();
        if (mobType == null || mobType.isEmpty()) {
            throw new IllegalStateException("No mob type configured for point: " + point.getId());
        }
        return mobType;
    }

    /**
     * 标记实体为Boss
     */
    private void markAsBoss(LivingEntity entity, UUID bossUUID, int tier) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(
            new NamespacedKey(plugin, "boss_uuid"),
            PersistentDataType.STRING,
            bossUUID.toString()
        );
        pdc.set(
            new NamespacedKey(plugin, "boss_tier"),
            PersistentDataType.INTEGER,
            tier
        );
    }

    /**
     * 移除Boss
     */
    private void removeBoss(UUID bossUUID) {
        BossEntity boss = activeBosses.remove(bossUUID);
        if (boss != null && boss.getBukkitEntity() != null) {
            entityToBossUUID.remove(boss.getBukkitEntity());
        }
    }

    // ==================== 状态查询 ====================

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 是否已启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取指定ID的刷新点
     *
     * @param id 刷新点ID
     * @return 刷新点，如果不存在则返回null
     */
    public BossSpawnPoint getSpawnPoint(String id) {
        return spawnPoints.get(id);
    }

    /**
     * 获取启用的刷新点列表
     *
     * @return 启用的刷新点ID列表
     */
    public List<String> getEnabledPoints() {
        return enabledPoints;
    }
}
