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
import com.xiancore.systems.boss.location.SelectionCriteria;
import com.xiancore.systems.boss.teleport.BossTeleportManager;
import com.xiancore.systems.boss.reward.BossRewardManager;
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
    private final BossEventBus eventBus;
    private final BossDataManager dataManager;
    private final BossConfigLoader configLoader;
    private final BossAttributeManager attributeManager;
    private final BossAnnouncementManager announcementManager;
    private final LocationSelector locationSelector;
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
        this.eventBus = new BossEventBus();
        this.dataManager = new BossDataManager(plugin);
        this.configLoader = new BossConfigLoader(plugin);
        this.attributeManager = new BossAttributeManager(plugin, mythicIntegration);
        this.announcementManager = new BossAnnouncementManager();
        this.locationSelector = new LocationSelector(SelectionCriteria.createBalanced());
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
            // 1. 调用MythicMobs API生成实体
            LivingEntity entity = mythicIntegration.spawnMythicMob(mythicMobId, location);
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
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            // 获取配置文件路径
            File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");

            // 使用BossConfigLoader加载配置
            refreshConfig = configLoader.loadConfig(configFile);

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
     * 保存当前配置到文件
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

            // 获取配置文件路径
            File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");

            // 使用BossConfigLoader保存配置
            configLoader.saveConfig(refreshConfig, configFile);

            plugin.getLogger().info("✓ Boss配置已保存到文件");
            return true;

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
     * 支持固定位置、随机偏移和玩家附近随机
     */
    private Location determineSpawnLocation(BossSpawnPoint point) {
        String spawnMode = point.getSpawnMode();
        
        // 玩家附近随机模式
        if ("player-nearby".equalsIgnoreCase(spawnMode)) {
            return determinePlayerNearbyLocation(point);
        }
        
        // 区域随机模式
        if ("region".equalsIgnoreCase(spawnMode)) {
            return determineRegionLocation(point);
        }
        
        // 固定位置模式（包含随机偏移）
        Location baseLocation = point.getLocation();
        if (baseLocation == null) {
            plugin.getLogger().warning("无法获取刷新点位置: " + point.getId() + " (世界不存在？)");
            return null;
        }
        
        // 1. 强制加载区块
        baseLocation.getChunk().load();
        baseLocation.getChunk().setForceLoaded(true);
        
        // 2. 应用随机偏移（如果配置了）
        Location finalLocation = baseLocation.clone();
        int randomRadius = point.getRandomRadius();
        if (randomRadius > 0) {
            int offsetX = (int) (Math.random() * randomRadius * 2) - randomRadius;
            int offsetZ = (int) (Math.random() * randomRadius * 2) - randomRadius;
            finalLocation.add(offsetX, 0, offsetZ);
            
            plugin.getLogger().info("随机偏移: (" + offsetX + ", 0, " + offsetZ + ")");
        }
        
        // 3. 自动寻找安全Y坐标（如果配置了）
        if (point.isAutoFindGround()) {
            Location safeLocation = findSafeGroundLocation(finalLocation);
            if (safeLocation != null) {
                plugin.getLogger().info("找到安全位置: Y=" + safeLocation.getBlockY());
                return safeLocation;
            } else {
                plugin.getLogger().warning("未找到安全位置，使用原始坐标");
            }
        }
        
        return finalLocation;
    }
    
    /**
     * 确定玩家附近随机位置
     * 使用多候选点策略，通过 LocationSelector 选择最佳位置
     * 
     * @param point 刷新点配置
     * @return 随机位置，如果未找到合适位置则返回null
     */
    private Location determinePlayerNearbyLocation(BossSpawnPoint point) {
        // 1. 获取所有在线玩家
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) {
            plugin.getLogger().warning("没有在线玩家，无法在玩家附近生成Boss");
            return null;
        }
        
        // 2. 随机选择一个玩家
        Player randomPlayer = onlinePlayers.get(ThreadLocalRandom.current().nextInt(onlinePlayers.size()));
        Location playerLoc = randomPlayer.getLocation();
        
        plugin.getLogger().info("基于玩家 " + randomPlayer.getName() + " 寻找生成位置...");
        
        // 3. 生成多个候选位置
        List<Location> candidates = new ArrayList<>();
        int minDist = point.getMinDistance();
        int maxDist = point.getMaxDistance();
        int maxAttempts = 10; // 生成10个候选位置
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 3.1 随机距离和角度
            double distance = minDist + (Math.random() * (maxDist - minDist));
            double angle = Math.random() * 2 * Math.PI;
            
            int offsetX = (int) (distance * Math.cos(angle));
            int offsetZ = (int) (distance * Math.sin(angle));
            
            Location candidateLoc = playerLoc.clone().add(offsetX, 0, offsetZ);
            
            // 3.2 强制加载区块
            candidateLoc.getChunk().load();
            
            // 3.3 寻找安全地面
            Location safeLocation = null;
            if (point.isAutoFindGround()) {
                safeLocation = findSafeGroundLocation(candidateLoc);
            } else {
                safeLocation = candidateLoc;
            }
            
            if (safeLocation != null) {
                candidates.add(safeLocation);
            }
        }
        
        if (candidates.isEmpty()) {
            plugin.getLogger().warning("✗ 未找到任何候选位置");
            return null;
        }
        
        plugin.getLogger().info("生成了 " + candidates.size() + " 个候选位置，使用智能选择器...");
        
        // 4. 使用 LocationSelector 选择最佳位置
        Location bestLocation = locationSelector.selectLocation(candidates, onlinePlayers);
        
        // 5. 返回最佳位置
        if (bestLocation != null) {
            plugin.getLogger().info("✓ LocationSelector 选定位置: Y=" + bestLocation.getBlockY());
            
            // 强制加载并锁定区块
            bestLocation.getChunk().load();
            bestLocation.getChunk().setForceLoaded(true);
            
            return bestLocation;
        } else {
            plugin.getLogger().warning("✗ LocationSelector 未能选择有效位置");
            return null;
        }
    }
    
    /**
     * 区域随机模式位置选择
     * 从配置的多个区域中随机选择一个，然后在区域内随机位置生成
     * 使用多候选点策略和开阔度评分
     * 
     * @param point Boss刷新点
     * @return 最佳生成位置，如果未找到则返回null
     */
    private Location determineRegionLocation(BossSpawnPoint point) {
    List<String> regions = point.getRegions();
    
    // 检查区域列表是否有效
    if (regions == null || regions.isEmpty()) {
        plugin.getLogger().warning("✗ [区域模式] 刷新点 " + point.getId() + " 未配置regions");
        return null;
    }
    
    plugin.getLogger().info("========================================");
    plugin.getLogger().info("▶ [区域模式] 开始位置选择");
    plugin.getLogger().info("  刷新点: " + point.getId());
    plugin.getLogger().info("  可选区域: " + regions.size() + " 个");
    
    // 随机选择一个区域
    String selectedRegion = regions.get(new Random().nextInt(regions.size()));
    plugin.getLogger().info("  选中区域: " + selectedRegion);
    
    // 解析区域定义：格式 "world,x1,z1,x2,z2"
    String[] parts = selectedRegion.split(",");
    if (parts.length != 5) {
        plugin.getLogger().warning("✗ 区域格式错误: " + selectedRegion + "，应为 'world,x1,z1,x2,z2'");
        return null;
    }
    
    String worldName = parts[0].trim();
    World world = Bukkit.getWorld(worldName);
    if (world == null) {
        plugin.getLogger().warning("✗ 世界不存在: " + worldName);
        return null;
    }
    
    try {
        int x1 = Integer.parseInt(parts[1].trim());
        int z1 = Integer.parseInt(parts[2].trim());
        int x2 = Integer.parseInt(parts[3].trim());
        int z2 = Integer.parseInt(parts[4].trim());
        
        // 确保坐标是正确顺序（min < max）
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);
        
        plugin.getLogger().info("  区域范围: X(" + minX + " - " + maxX + "), Z(" + minZ + " - " + maxZ + ")");
        
        // 多候选点策略
        final int MAX_ATTEMPTS = 5;
        double bestScore = 0.0;
        Location bestLocation = null;
        
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // 在区域内随机选择一个XZ坐标
            int randomX = minX + new Random().nextInt(maxX - minX + 1);
            int randomZ = minZ + new Random().nextInt(maxZ - minZ + 1);
            
            // 从较高处开始搜索（避免从地下开始）
            Location candidateLocation = new Location(world, randomX, 128, randomZ);
            
            // 如果启用了自动寻找地面
            if (point.isAutoFindGround()) {
                candidateLocation = findSafeGroundLocation(candidateLocation);
                if (candidateLocation == null) {
                    plugin.getLogger().info("  候选点 " + attempt + "/5: (" + randomX + ", ?, " + randomZ + ") - 未找到安全地面");
                    continue;
                }
            }
            
            // 计算位置评分（智能评分或简单开阔度）
            double score = calculateSmartScore(candidateLocation, point);
            
            plugin.getLogger().info("  候选点 " + attempt + "/5: (" 
                + candidateLocation.getBlockX() + ", " 
                + candidateLocation.getBlockY() + ", " 
                + candidateLocation.getBlockZ() + ") - 评分: " 
                + String.format("%.2f", score));
            
            // 更新最佳位置
            if (score > bestScore) {
                bestScore = score;
                bestLocation = candidateLocation.clone();
            }
            
            // 如果找到足够好的位置，提前结束
            double minThreshold = point.isEnableSmartScoring() ? point.getMinScore() : 0.7;
            if (score >= minThreshold && score >= 0.7) {
                plugin.getLogger().info("  ✓ 找到优质位置，提前结束搜索");
                break;
            }
        }
        
        // 评估最佳候选点
        double minAcceptableScore = point.isEnableSmartScoring() ? point.getMinScore() : 0.3;
        if (bestLocation != null && bestScore >= minAcceptableScore) {
            plugin.getLogger().info("✓ [区域模式] 选择最佳位置: (" 
                + bestLocation.getBlockX() + ", " 
                + bestLocation.getBlockY() + ", " 
                + bestLocation.getBlockZ() + ")"
                + ", 评分=" + String.format("%.2f", bestScore));
            
            // 强制加载并锁定区块
            bestLocation.getChunk().load();
            bestLocation.getChunk().setForceLoaded(true);
            
            return bestLocation;
        } else {
            plugin.getLogger().warning("✗ 区域内所有候选点均不适合生成，生成失败");
            return null;
        }
        
    } catch (NumberFormatException e) {
        plugin.getLogger().warning("✗ 区域坐标解析失败: " + selectedRegion);
        return null;
    }
}
    
    /**
     * 寻找安全的地面位置
     * 从给定位置向下搜索固体方块，并确保上方有足够空间
     * 
     * @param center 中心位置
     * @return 安全位置，如果未找到则返回null
     */
    private Location findSafeGroundLocation(Location center) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int startY = Math.min(center.getBlockY(), world.getMaxHeight() - 10);
        
        // 从起始Y坐标向下搜索
        for (int y = startY; y > world.getMinHeight(); y--) {
            org.bukkit.block.Block block = world.getBlockAt(centerX, y, centerZ);
            
            // 检查当前方块是否为固体（地面）
            if (block.getType().isSolid()) {
                // 检查上方是否有足够空间（至少3格空气）
                org.bukkit.block.Block above1 = world.getBlockAt(centerX, y + 1, centerZ);
                org.bukkit.block.Block above2 = world.getBlockAt(centerX, y + 2, centerZ);
                org.bukkit.block.Block above3 = world.getBlockAt(centerX, y + 3, centerZ);
                
                if (above1.getType().isAir() && above2.getType().isAir() && above3.getType().isAir()) {
                    // 找到安全位置：固体地面+上方3格空气
                    return new Location(world, centerX + 0.5, y + 1, centerZ + 0.5);
                }
            }
        }
        
        // 未找到安全位置
        return null;
    }
    
    /**
     * 计算位置的开阔度评分（0.0-1.0）
     * 检查周围是否有足够的空间，避免在洞穴或建筑内生成
     * 
     * @param location 要评分的位置
     * @return 开阔度分数，1.0=完全开阔，0.0=完全封闭
     */
    private double calculateOpennessScore(Location location) {
        if (location == null) return 0.0;
        
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        int totalChecks = 0;
        int openChecks = 0;
        
        // 1. 检查头顶是否露天（能看到天空）
        boolean canSeeSky = world.getHighestBlockYAt(x, z) <= y;
        if (canSeeSky) {
            openChecks += 5; // 露天加5分
        }
        totalChecks += 5;
        
        // 2. 检查水平8个方向，半径5格内是否开阔
        int[][] directions = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},  // 东西南北
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}  // 四个对角
        };
        
        for (int[] dir : directions) {
            boolean isOpen = true;
            for (int dist = 1; dist <= 5; dist++) {
                int checkX = x + dir[0] * dist;
                int checkZ = z + dir[1] * dist;
                
                // 检查该位置及上方2格是否为空气
                for (int dy = 0; dy <= 2; dy++) {
                    org.bukkit.block.Block block = world.getBlockAt(checkX, y + dy, checkZ);
                    if (block.getType().isSolid()) {
                        isOpen = false;
                        break;
                    }
                }
                if (!isOpen) break;
            }
            
            if (isOpen) {
                openChecks++;
            }
            totalChecks++;
        }
        
        // 3. 计算评分
        double score = totalChecks > 0 ? (double) openChecks / totalChecks : 0.0;
        
        plugin.getLogger().fine("位置开阔度评分: " + String.format("%.2f", score) + 
            " (露天: " + canSeeSky + ", 开阔方向: " + (openChecks - (canSeeSky ? 5 : 0)) + "/8)");
        
        return score;
    }
    
    /**
     * 计算综合位置评分（智能评分系统）
     * 综合考虑：开阔度、生物群系匹配、灵气浓度、玩家密集度
     * 
     * @param location 待评分的位置
     * @param point 刷新点配置
     * @return 综合评分 (0.0-1.0)
     */
    private double calculateSmartScore(Location location, BossSpawnPoint point) {
        // 如果未启用智能评分，直接使用简单开阔度评分
        if (!point.isEnableSmartScoring()) {
            return calculateOpennessScore(location);
        }
        
        // 各维度评分
        double opennessScore = calculateOpennessScore(location);
        double biomeScore = calculateBiomeMatchScore(location, point);
        double spiritualScore = calculateSpiritualEnergyScore(location);
        double playerDensityScore = calculatePlayerDensityScore(location);
        
        // 获取权重
        double w1 = point.getOpennessWeight();
        double w2 = point.getBiomeWeight();
        double w3 = point.getSpiritualEnergyWeight();
        double w4 = point.getPlayerDensityWeight();
        
        // 权重归一化（确保总和为1.0）
        double totalWeight = w1 + w2 + w3 + w4;
        if (totalWeight > 0) {
            w1 /= totalWeight;
            w2 /= totalWeight;
            w3 /= totalWeight;
            w4 /= totalWeight;
        }
        
        // 计算加权平均分
        double finalScore = opennessScore * w1 + 
                           biomeScore * w2 + 
                           spiritualScore * w3 + 
                           playerDensityScore * w4;
        
        plugin.getLogger().fine(String.format(
            "智能评分详情: 总分=%.2f (开阔度=%.2f×%.2f, 生物群系=%.2f×%.2f, 灵气=%.2f×%.2f, 玩家密度=%.2f×%.2f)",
            finalScore, opennessScore, w1, biomeScore, w2, spiritualScore, w3, playerDensityScore, w4
        ));
        
        return finalScore;
    }
    
    /**
     * 计算生物群系匹配评分
     * 
     * @param location 位置
     * @param point 刷新点配置
     * @return 匹配评分 (0.0-1.0)
     */
    private double calculateBiomeMatchScore(Location location, BossSpawnPoint point) {
        List<String> preferredBiomes = point.getPreferredBiomes();
        
        // 如果没有配置偏好生物群系，返回中性分数
        if (preferredBiomes == null || preferredBiomes.isEmpty()) {
            return 0.5;
        }
        
        // 获取当前位置的生物群系
        org.bukkit.block.Biome currentBiome = location.getBlock().getBiome();
        String biomeName = currentBiome.name();
        
        // 完全匹配：1.0分
        for (String preferred : preferredBiomes) {
            if (biomeName.equalsIgnoreCase(preferred)) {
                return 1.0;
            }
        }
        
        // 部分匹配：检查生物群系类别（如FOREST, DESERT等）
        for (String preferred : preferredBiomes) {
            if (biomeName.contains(preferred.toUpperCase()) || 
                preferred.toUpperCase().contains(biomeName)) {
                return 0.7; // 类别匹配给0.7分
            }
        }
        
        // 不匹配：0.3分（不完全拒绝）
        return 0.3;
    }
    
    /**
     * 计算灵气浓度评分
     * 基于生物群系类型和环境特征计算灵气浓度
     * 
     * @param location 位置
     * @return 灵气评分 (0.0-1.0)
     */
    private double calculateSpiritualEnergyScore(Location location) {
        org.bukkit.block.Biome biome = location.getBlock().getBiome();
        String biomeName = biome.name();
        double baseScore = 0.5; // 默认基础分
        
        // 高灵气生物群系（神秘、稀有）
        if (biomeName.contains("MUSHROOM")) {
            baseScore = 0.95; // 蘑菇岛 - 极高灵气
        } else if (biomeName.contains("JUNGLE")) {
            baseScore = 0.85; // 丛林 - 很高灵气
        } else if (biomeName.contains("BAMBOO")) {
            baseScore = 0.80; // 竹林 - 高灵气
        } else if (biomeName.contains("DARK_FOREST") || biomeName.contains("DARK_OAK")) {
            baseScore = 0.75; // 黑森林 - 高灵气
        } else if (biomeName.contains("FOREST")) {
            baseScore = 0.70; // 森林 - 较高灵气
        } else if (biomeName.contains("MOUNTAIN") || biomeName.contains("PEAKS")) {
            baseScore = 0.75; // 山脉 - 高灵气
        } else if (biomeName.contains("TAIGA")) {
            baseScore = 0.65; // 针叶林 - 中等偏高
        } else if (biomeName.contains("SWAMP")) {
            baseScore = 0.60; // 沼泽 - 中等
        } else if (biomeName.contains("RIVER") || biomeName.contains("OCEAN")) {
            baseScore = 0.55; // 水域 - 中等
        } else if (biomeName.contains("PLAINS")) {
            baseScore = 0.50; // 平原 - 中等
        } else if (biomeName.contains("DESERT")) {
            baseScore = 0.45; // 沙漠 - 较低
        } else if (biomeName.contains("SAVANNA")) {
            baseScore = 0.40; // 热带草原 - 较低
        } else if (biomeName.contains("BADLANDS") || biomeName.contains("MESA")) {
            baseScore = 0.35; // 恶地 - 低
        } else if (biomeName.contains("NETHER")) {
            baseScore = 0.80; // 下界 - 特殊高灵气（魔法能量）
        } else if (biomeName.contains("END")) {
            baseScore = 0.90; // 末地 - 极高灵气
        }
        
        // 环境加成：高度修正（高山获得加成）
        int y = location.getBlockY();
        if (y > 120) {
            baseScore += 0.05; // 高处+5%
        } else if (y < 40) {
            baseScore -= 0.05; // 低处-5%
        }
        
        // 确保评分在0.0-1.0范围内
        return Math.max(0.0, Math.min(1.0, baseScore));
    }
    
    /**
     * 计算玩家密集度评分
     * 评分逻辑：适度玩家密集度最佳（太多或太少都不好）
     * 
     * @param location 位置
     * @return 密集度评分 (0.0-1.0)
     */
    private double calculatePlayerDensityScore(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return 0.5;
        }
        
        // 统计附近玩家数量
        int nearbyPlayers = 0;
        int checkRadius = 100; // 检查半径100格
        
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(location) <= checkRadius) {
                nearbyPlayers++;
            }
        }
        
        // 评分逻辑：
        // 0人：0.3分（太冷清）
        // 1-2人：0.8分（适中偏少）
        // 3-5人：1.0分（最佳）
        // 6-10人：0.7分（适中偏多）
        // 10+人：0.4分（太拥挤）
        
        if (nearbyPlayers == 0) {
            return 0.3;
        } else if (nearbyPlayers <= 2) {
            return 0.8;
        } else if (nearbyPlayers <= 5) {
            return 1.0;
        } else if (nearbyPlayers <= 10) {
            return 0.7;
        } else {
            return 0.4;
        }
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
