package com.xiancore.systems.boss.config;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Boss 系统配置加载器
 * 从 YAML 文件加载配置，支持热重载
 *
 * 功能:
 * - 从 YAML 文件加载配置
 * - 保存配置到 YAML 文件
 * - 生成默认配置文件
 * - 支持热重载
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class BossConfigLoader {

    private final XianCore plugin;
    private final Logger logger;
    private final ConfigValidator validator;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     */
    public BossConfigLoader(XianCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.validator = new ConfigValidator();
    }

    // ==================== 加载配置 ====================

    /**
     * 从文件加载完整配置
     *
     * @param configFile 配置文件
     * @return 加载的配置对象
     */
    public BossRefreshConfig loadConfig(File configFile) {
        try {
            // 确保文件存在
            if (!configFile.exists()) {
                generateDefaultConfig(configFile);
            }

            // 加载 YAML 文件
            FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(configFile);

            // 创建配置对象
            BossRefreshConfig config = new BossRefreshConfig();

            // 加载全局设置
            loadGlobalSettings(fileConfig, config);

            // 加载刷新点
            List<BossSpawnPoint> spawnPoints = loadSpawnPoints(fileConfig);
            config.setSpawnPoints(spawnPoints);

            logger.info("✓ Boss 配置已加载: " + spawnPoints.size() + " 个刷新点");

            return config;

        } catch (Exception e) {
            logger.warning("✗ 加载配置失败: " + e.getMessage());
            e.printStackTrace();
            return BossRefreshConfig.loadDefault();
        }
    }

    /**
     * 加载全局设置
     *
     * @param fileConfig YAML 配置
     * @param config 要填充的配置对象
     */
    private void loadGlobalSettings(FileConfiguration fileConfig, BossRefreshConfig config) {
        ConfigurationSection globalSection = fileConfig.getConfigurationSection(
            ConfigConstants.GLOBAL_CONFIG_PATH);

        if (globalSection == null) {
            logger.warning("未找到全局配置，使用默认值");
            return;
        }

        // 加载检查间隔
        int checkInterval = globalSection.getInt(
            "check-interval",
            ConfigConstants.DEFAULT_CHECK_INTERVAL);
        String checkError = validator.validateCheckInterval(checkInterval);
        if (checkError != null) {
            logger.warning(checkError + "，使用默认值");
            checkInterval = ConfigConstants.DEFAULT_CHECK_INTERVAL;
        }
        config.setCheckIntervalSeconds(checkInterval);

        // 加载最大 Boss 数
        int maxBosses = globalSection.getInt(
            "max-active-bosses",
            ConfigConstants.DEFAULT_MAX_ACTIVE_BOSSES);
        String maxError = validator.validateMaxActiveBosses(maxBosses);
        if (maxError != null) {
            logger.warning(maxError + "，使用默认值");
            maxBosses = ConfigConstants.DEFAULT_MAX_ACTIVE_BOSSES;
        }
        config.setMaxActiveBosses(maxBosses);

        // 加载最少玩家数
        int minPlayers = globalSection.getInt(
            "min-online-players",
            ConfigConstants.DEFAULT_MIN_ONLINE_PLAYERS);
        String minError = validator.validateMinOnlinePlayers(minPlayers);
        if (minError != null) {
            logger.warning(minError + "，使用默认值");
            minPlayers = ConfigConstants.DEFAULT_MIN_ONLINE_PLAYERS;
        }
        config.setMinOnlinePlayers(minPlayers);

        // 加载启用状态
        boolean enabled = globalSection.getBoolean(
            "enabled",
            ConfigConstants.DEFAULT_ENABLED);
        config.setEnabled(enabled);

        logger.info("✓ 全局设置已加载");
    }

    /**
     * 加载所有刷新点
     *
     * @param fileConfig YAML 配置
     * @return 刷新点列表
     */
    private List<BossSpawnPoint> loadSpawnPoints(FileConfiguration fileConfig) {
        List<BossSpawnPoint> points = new ArrayList<>();

        ConfigurationSection pointsSection = fileConfig.getConfigurationSection(
            ConfigConstants.SPAWN_POINTS_PATH);

        if (pointsSection == null) {
            logger.warning("未找到刷新点配置");
            return points;
        }

        for (String id : pointsSection.getKeys(false)) {
            try {
                ConfigurationSection pointSection = pointsSection.getConfigurationSection(id);
                if (pointSection == null) continue;

                BossSpawnPoint point = loadSpawnPoint(pointSection, id);
                if (point != null) {
                    points.add(point);
                    logger.info("✓ 加载刷新点: " + id);
                }

            } catch (Exception e) {
                logger.warning("✗ 加载刷新点 " + id + " 失败: " + e.getMessage());
            }
        }

        return points;
    }

    /**
     * 加载单个刷新点
     *
     * @param section 刷新点配置节点
     * @param id 刷新点 ID
     * @return 刷新点对象
     */
    private BossSpawnPoint loadSpawnPoint(ConfigurationSection section, String id) {
        // 验证 ID
        String idError = validator.validateSpawnPointId(id);
        if (idError != null) {
            throw new IllegalArgumentException(idError);
        }

        // 加载必需字段
        String mythicMobId = section.getString("mythic-mob");
        String mobError = validator.validateMythicMobId(mythicMobId);
        if (mobError != null) {
            throw new IllegalArgumentException("刷新点 " + id + ": " + mobError);
        }

        // 加载生成模式
        String spawnMode = section.getString("spawn-mode", "fixed");
        
        // 创建刷新点（坐标可选）
        BossSpawnPoint point;
        
        if ("player-nearby".equalsIgnoreCase(spawnMode)) {
            // 玩家附近模式：不需要固定坐标，只需要世界名
            String world = section.getString("world", "world"); // 默认主世界
            point = new BossSpawnPoint(id, world, 0, 0, 0, mythicMobId);
            point.setSpawnMode("player-nearby");
            
            // 加载距离配置
            int minDistance = section.getInt("min-distance", 50);
            int maxDistance = section.getInt("max-distance", 200);
            point.setMinDistance(minDistance);
            point.setMaxDistance(maxDistance);
            
            logger.info("  - 刷新点 " + id + ": 玩家附近模式 (距离: " + minDistance + "-" + maxDistance + "格)");
        } else {
            // 固定位置模式：需要location字段
            String locationString = section.getString("location");
            String locError = validator.validateLocationString(locationString);
            if (locError != null) {
                throw new IllegalArgumentException("刷新点 " + id + ": " + locError);
            }
            
            // 解析坐标
            String[] coords = locationString.split(",");
            String world = coords[0].trim();
            int x = Integer.parseInt(coords[1].trim());
            int y = Integer.parseInt(coords[2].trim());
            int z = Integer.parseInt(coords[3].trim());
            
            point = new BossSpawnPoint(id, world, x, y, z, mythicMobId);
            point.setSpawnMode("fixed");
        }

        // 加载可选字段
        int tier = section.getInt("tier", ConfigConstants.DEFAULT_TIER);
        String tierError = validator.validateTier(tier);
        if (tierError != null) {
            logger.warning("刷新点 " + id + ": " + tierError + "，使用默认值");
            tier = ConfigConstants.DEFAULT_TIER;
        }
        point.setTier(tier);

        long cooldown = section.getLong("cooldown", ConfigConstants.DEFAULT_COOLDOWN_SECONDS);
        String coolError = validator.validateCooldown(cooldown);
        if (coolError != null) {
            logger.warning("刷新点 " + id + ": " + coolError + "，使用默认值");
            cooldown = ConfigConstants.DEFAULT_COOLDOWN_SECONDS;
        }
        point.setCooldownSeconds(cooldown);

        int maxCount = section.getInt("max-count", ConfigConstants.DEFAULT_MAX_COUNT);
        String countError = validator.validateMaxCount(maxCount);
        if (countError != null) {
            logger.warning("刷新点 " + id + ": " + countError + "，使用默认值");
            maxCount = ConfigConstants.DEFAULT_MAX_COUNT;
        }
        point.setMaxCount(maxCount);
        
        // 加载随机偏移半径
        int randomRadius = section.getInt("random-radius", 0);
        if (randomRadius < 0) {
            logger.warning("刷新点 " + id + ": random-radius不能为负数，使用0");
            randomRadius = 0;
        }
        point.setRandomRadius(randomRadius);
        
        // 加载自动寻找地面选项
        boolean autoFindGround = section.getBoolean("auto-find-ground", false);
        point.setAutoFindGround(autoFindGround);
        
        // 加载区域列表（仅region模式）
        if ("region".equalsIgnoreCase(spawnMode)) {
            List<String> regions = section.getStringList("regions");
            if (regions != null && !regions.isEmpty()) {
                point.setRegions(regions);
                logger.info("  - 刷新点 " + id + ": 区域模式，共" + regions.size() + "个区域");
            } else {
                logger.warning("刷新点 " + id + ": 区域模式但未配置regions，将跳过");
            }
        }
        
        // 加载智能评分配置
        boolean enableSmartScoring = section.getBoolean("enable-smart-scoring", false);
        point.setEnableSmartScoring(enableSmartScoring);
        
        if (enableSmartScoring) {
            // 加载偏好生物群系
            List<String> preferredBiomes = section.getStringList("preferred-biomes");
            if (preferredBiomes != null && !preferredBiomes.isEmpty()) {
                point.setPreferredBiomes(preferredBiomes);
            }
            
            // 加载权重配置（使用默认值）
            double biomeWeight = section.getDouble("biome-weight", 0.2);
            double spiritualWeight = section.getDouble("spiritual-energy-weight", 0.3);
            double playerDensityWeight = section.getDouble("player-density-weight", 0.2);
            double opennessWeight = section.getDouble("openness-weight", 0.3);
            double minScore = section.getDouble("min-score", 0.4);
            
            point.setBiomeWeight(biomeWeight);
            point.setSpiritualEnergyWeight(spiritualWeight);
            point.setPlayerDensityWeight(playerDensityWeight);
            point.setOpennessWeight(opennessWeight);
            point.setMinScore(minScore);
            
            logger.info("  - 刷新点 " + id + ": 智能评分已启用 (生物群系:" + biomeWeight + 
                ", 灵气:" + spiritualWeight + ", 玩家密度:" + playerDensityWeight + 
                ", 开阔度:" + opennessWeight + ", 最小评分:" + minScore + ")");
        }

        return point;
    }

    // ==================== 保存配置 ====================

    /**
     * 保存配置到文件
     *
     * @param config 要保存的配置
     * @param configFile 配置文件
     */
    public void saveConfig(BossRefreshConfig config, File configFile) {
        try {
            FileConfiguration fileConfig = new YamlConfiguration();

            // 保存全局设置
            fileConfig.set(ConfigConstants.CHECK_INTERVAL_PATH, config.getCheckIntervalSeconds());
            fileConfig.set(ConfigConstants.MAX_ACTIVE_BOSSES_PATH, config.getMaxActiveBosses());
            fileConfig.set(ConfigConstants.MIN_ONLINE_PLAYERS_PATH, config.getMinOnlinePlayers());
            fileConfig.set(ConfigConstants.ENABLED_PATH, config.isEnabled());

            // 保存刷新点
            for (BossSpawnPoint point : config.getSpawnPoints()) {
                String path = ConfigConstants.SPAWN_POINTS_PATH + "." + point.getId();
                fileConfig.set(path + ".location", point.getLocationString());
                fileConfig.set(path + ".mythic-mob", point.getMythicMobId());
                fileConfig.set(path + ".tier", point.getTier());
                fileConfig.set(path + ".cooldown", point.getCooldownSeconds());
                fileConfig.set(path + ".max-count", point.getMaxCount());
            }

            // 写入文件
            fileConfig.save(configFile);
            logger.info("✓ 配置已保存到文件");

        } catch (IOException e) {
            logger.warning("✗ 保存配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 生成默认配置 ====================

    /**
     * 生成默认配置文件
     *
     * @param configFile 配置文件路径
     */
    public void generateDefaultConfig(File configFile) {
        try {
            // 创建父目录
            configFile.getParentFile().mkdirs();

            // 创建新文件
            if (!configFile.createNewFile()) {
                logger.warning("配置文件已存在");
                return;
            }

            FileConfiguration fileConfig = new YamlConfiguration();

            // ===== 全局设置 =====
            fileConfig.set("# Boss 刷新系统配置", null);
            fileConfig.set("# 生成时间: " + System.currentTimeMillis(), null);

            fileConfig.set(ConfigConstants.CHECK_INTERVAL_PATH, ConfigConstants.DEFAULT_CHECK_INTERVAL);
            fileConfig.set(ConfigConstants.MAX_ACTIVE_BOSSES_PATH, ConfigConstants.DEFAULT_MAX_ACTIVE_BOSSES);
            fileConfig.set(ConfigConstants.MIN_ONLINE_PLAYERS_PATH, ConfigConstants.DEFAULT_MIN_ONLINE_PLAYERS);
            fileConfig.set(ConfigConstants.ENABLED_PATH, ConfigConstants.DEFAULT_ENABLED);

            // ===== 刷新点示例 =====
            String examplePath = ConfigConstants.SPAWN_POINTS_PATH + ".dragon_lair";
            fileConfig.set(examplePath + ".location", "world,100,64,200");
            fileConfig.set(examplePath + ".mythic-mob", "EnderDragon");
            fileConfig.set(examplePath + ".tier", ConfigConstants.DEFAULT_TIER);
            fileConfig.set(examplePath + ".cooldown", ConfigConstants.DEFAULT_COOLDOWN_SECONDS);
            fileConfig.set(examplePath + ".max-count", ConfigConstants.DEFAULT_MAX_COUNT);

            // 写入文件
            fileConfig.save(configFile);
            logger.info("✓ 已生成默认配置文件: " + configFile.getPath());

        } catch (IOException e) {
            logger.warning("✗ 生成默认配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
