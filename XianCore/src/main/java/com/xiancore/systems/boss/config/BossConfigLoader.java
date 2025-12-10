package com.xiancore.systems.boss.config;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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

    // ==================== MySQL持久化支持 ====================

    /**
     * 从MySQL数据库加载配置
     *
     * @param connection 数据库连接
     * @return 加载的配置对象
     */
    public BossRefreshConfig loadConfigFromDatabase(Connection connection) {
        try {
            logger.info("✓ 从MySQL加载Boss配置...");

            BossRefreshConfig config = new BossRefreshConfig();

            // 1. 加载全局配置
            loadGlobalSettingsFromDatabase(connection, config);

            // 2. 加载所有刷新点
            List<BossSpawnPoint> spawnPoints = loadSpawnPointsFromDatabase(connection);
            config.setSpawnPoints(spawnPoints);

            logger.info("✓ MySQL Boss配置已加载: " + spawnPoints.size() + " 个刷新点");
            return config;

        } catch (Exception e) {
            logger.warning("✗ 从MySQL加载配置失败: " + e.getMessage());
            e.printStackTrace();
            return BossRefreshConfig.loadDefault();
        }
    }

    /**
     * 从MySQL加载全局配置
     */
    private void loadGlobalSettingsFromDatabase(Connection conn, BossRefreshConfig config) throws SQLException {
        String sql = "SELECT * FROM xian_boss_refresh_config WHERE id = 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                config.setCheckIntervalSeconds(rs.getInt("check_interval_seconds"));
                config.setMaxActiveBosses(rs.getInt("max_active_bosses"));
                config.setMinOnlinePlayers(rs.getInt("min_online_players"));
                config.setEnabled(rs.getBoolean("enabled"));

                logger.info("✓ 全局设置已从MySQL加载");
            } else {
                logger.warning("未找到全局配置，使用默认值");
            }
        }
    }

    /**
     * 从MySQL加载所有刷新点
     */
    private List<BossSpawnPoint> loadSpawnPointsFromDatabase(Connection conn) throws SQLException {
        List<BossSpawnPoint> points = new ArrayList<>();

        String sql = "SELECT * FROM xian_boss_spawn_points ORDER BY id";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                try {
                    BossSpawnPoint point = loadSpawnPointFromResultSet(rs);
                    if (point != null) {
                        points.add(point);
                        logger.info("✓ 加载刷新点: " + point.getId());
                    }
                } catch (Exception e) {
                    logger.warning("✗ 加载刷新点失败: " + rs.getString("id") + " - " + e.getMessage());
                }
            }
        }

        return points;
    }

    /**
     * 从ResultSet构建BossSpawnPoint对象
     */
    private BossSpawnPoint loadSpawnPointFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String worldName = rs.getString("world_name");
        double x = rs.getDouble("x");
        double y = rs.getDouble("y");
        double z = rs.getDouble("z");
        String mythicMobId = rs.getString("mythic_mob_id");

        BossSpawnPoint point = new BossSpawnPoint(id, worldName, (int)x, (int)y, (int)z, mythicMobId);

        // 加载基本属性
        point.setTier(rs.getInt("tier"));
        point.setCooldownSeconds(rs.getLong("cooldown_seconds"));
        point.setMaxCount(rs.getInt("max_count"));
        point.setCurrentCount(rs.getInt("current_count"));
        point.setLastSpawnTime(rs.getLong("last_spawn_time"));
        point.setEnabled(rs.getBoolean("enabled"));

        // 加载刷新模式相关
        point.setSpawnMode(rs.getString("spawn_mode"));
        point.setRandomRadius(rs.getInt("random_radius"));
        point.setAutoFindGround(rs.getBoolean("auto_find_ground"));
        point.setMinDistance(rs.getInt("min_distance"));
        point.setMaxDistance(rs.getInt("max_distance"));

        // 加载区域列表（JSON格式转List）
        String regionsJson = rs.getString("regions");
        if (regionsJson != null && !regionsJson.isEmpty()) {
            // 简单JSON解析: ["region1","region2"] -> List
            regionsJson = regionsJson.replace("[", "").replace("]", "").replace("\"", "");
            if (!regionsJson.isEmpty()) {
                point.setRegions(Arrays.asList(regionsJson.split(",")));
            }
        }

        // 加载智能评分配置
        point.setEnableSmartScoring(rs.getBoolean("enable_smart_scoring"));
        if (point.isEnableSmartScoring()) {
            // 加载偏好生物群系
            String biomesJson = rs.getString("preferred_biomes");
            if (biomesJson != null && !biomesJson.isEmpty()) {
                biomesJson = biomesJson.replace("[", "").replace("]", "").replace("\"", "");
                if (!biomesJson.isEmpty()) {
                    point.setPreferredBiomes(Arrays.asList(biomesJson.split(",")));
                }
            }

            // 加载权重
            point.setBiomeWeight(rs.getDouble("biome_weight"));
            point.setSpiritualEnergyWeight(rs.getDouble("spiritual_energy_weight"));
            point.setPlayerDensityWeight(rs.getDouble("player_density_weight"));
            point.setOpennessWeight(rs.getDouble("openness_weight"));
            point.setMinScore(rs.getDouble("min_score"));
        }

        return point;
    }

    /**
     * 保存配置到MySQL数据库
     *
     * @param config 要保存的配置
     * @param connection 数据库连接
     */
    public void saveConfigToDatabase(BossRefreshConfig config, Connection connection) {
        try {
            logger.info("✓ 保存Boss配置到MySQL...");

            // 1. 保存全局配置
            saveGlobalSettingsToDatabase(config, connection);

            // 2. 保存所有刷新点
            saveSpawnPointsToDatabase(config.getSpawnPoints(), connection);

            logger.info("✓ Boss配置已保存到MySQL");

        } catch (Exception e) {
            logger.warning("✗ 保存配置到MySQL失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存全局配置到MySQL
     */
    private void saveGlobalSettingsToDatabase(BossRefreshConfig config, Connection conn) throws SQLException {
        String sql = """
                INSERT INTO xian_boss_refresh_config (
                    id, check_interval_seconds, max_active_bosses,
                    min_online_players, enabled, updated_at
                ) VALUES (1, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    check_interval_seconds = VALUES(check_interval_seconds),
                    max_active_bosses = VALUES(max_active_bosses),
                    min_online_players = VALUES(min_online_players),
                    enabled = VALUES(enabled),
                    updated_at = VALUES(updated_at)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, config.getCheckIntervalSeconds());
            pstmt.setInt(2, config.getMaxActiveBosses());
            pstmt.setInt(3, config.getMinOnlinePlayers());
            pstmt.setBoolean(4, config.isEnabled());
            pstmt.setLong(5, System.currentTimeMillis());

            pstmt.executeUpdate();
            logger.info("✓ 全局配置已保存到MySQL");
        }
    }

    /**
     * 保存所有刷新点到MySQL
     */
    private void saveSpawnPointsToDatabase(List<BossSpawnPoint> points, Connection conn) throws SQLException {
        // 先删除所有旧数据
        String deleteSql = "DELETE FROM xian_boss_spawn_points";
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.executeUpdate();
        }

        // 插入新数据
        String insertSql = """
                INSERT INTO xian_boss_spawn_points (
                    id, world_name, x, y, z, mythic_mob_id, tier,
                    cooldown_seconds, max_count, current_count, last_spawn_time, enabled,
                    spawn_mode, random_radius, auto_find_ground,
                    min_distance, max_distance, regions,
                    enable_smart_scoring, preferred_biomes,
                    biome_weight, spiritual_energy_weight, player_density_weight,
                    openness_weight, min_score, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (BossSpawnPoint point : points) {
                pstmt.setString(1, point.getId());
                pstmt.setString(2, point.getWorld());
                pstmt.setDouble(3, point.getX());
                pstmt.setDouble(4, point.getY());
                pstmt.setDouble(5, point.getZ());
                pstmt.setString(6, point.getMythicMobId());
                pstmt.setInt(7, point.getTier());
                pstmt.setLong(8, point.getCooldownSeconds());
                pstmt.setInt(9, point.getMaxCount());
                pstmt.setInt(10, point.getCurrentCount());
                pstmt.setLong(11, point.getLastSpawnTime());
                pstmt.setBoolean(12, point.isEnabled());
                pstmt.setString(13, point.getSpawnMode());
                pstmt.setInt(14, point.getRandomRadius());
                pstmt.setBoolean(15, point.isAutoFindGround());
                pstmt.setInt(16, point.getMinDistance());
                pstmt.setInt(17, point.getMaxDistance());

                // 区域列表转JSON格式
                List<String> regions = point.getRegions();
                if (regions != null && !regions.isEmpty()) {
                    pstmt.setString(18, "[\"" + String.join("\",\"", regions) + "\"]");
                } else {
                    pstmt.setString(18, null);
                }

                pstmt.setBoolean(19, point.isEnableSmartScoring());

                // 偏好生物群系转JSON
                List<String> biomes = point.getPreferredBiomes();
                if (biomes != null && !biomes.isEmpty()) {
                    pstmt.setString(20, "[\"" + String.join("\",\"", biomes) + "\"]");
                } else {
                    pstmt.setString(20, null);
                }

                pstmt.setDouble(21, point.getBiomeWeight());
                pstmt.setDouble(22, point.getSpiritualEnergyWeight());
                pstmt.setDouble(23, point.getPlayerDensityWeight());
                pstmt.setDouble(24, point.getOpennessWeight());
                pstmt.setDouble(25, point.getMinScore());
                pstmt.setLong(26, System.currentTimeMillis());
                pstmt.setLong(27, System.currentTimeMillis());

                pstmt.addBatch();
            }

            pstmt.executeBatch();
            logger.info("✓ 已保存 " + points.size() + " 个刷新点到MySQL");
        }
    }
}
