package com.xiancore.core.data;

import com.xiancore.XianCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据管理器
 * 负责管理玩家数据、数据库连接等
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class DataManager {

    private final XianCore plugin;
    private HikariDataSource dataSource;
    private boolean useMySql = false;

    // 玩家数据缓存
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    public DataManager(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化数据管理器
     */
    public void initialize() {
        plugin.getLogger().info("正在初始化数据管理器...");

        // 加载数据库配置
        FileConfiguration config = plugin.getConfig();
        useMySql = config.getBoolean("database.use-mysql", false);

        if (useMySql) {
            setupMySql();
        } else {
            plugin.getLogger().info("使用本地文件存储（YAML）");
        }

        // 创建数据表
        createTables();

        plugin.getLogger().info("§a数据管理器初始化完成!");
    }

    /**
     * 设置 MySQL 连接池
     */
    private void setupMySql() {
        plugin.getLogger().info("正在连接到 MySQL 数据库...");

        FileConfiguration config = plugin.getConfig();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf8",
                config.getString("database.host", "localhost"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "xiancore")
        ));
        hikariConfig.setUsername(config.getString("database.username", "root"));
        hikariConfig.setPassword(config.getString("database.password", "password"));

        // 连接池配置
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool-size", 10));
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        // 性能优化
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("§a✓ MySQL 数据库连接成功!");
        } catch (Exception e) {
            plugin.getLogger().severe("§c✗ MySQL 数据库连接失败!");
            e.printStackTrace();
            plugin.getLogger().warning("§e切换到本地文件存储（YAML）");
            useMySql = false;
        }
    }

    /**
     * 创建数据表
     */
    private void createTables() {
        if (!useMySql) {
            return;
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 玩家数据表 - 扩展字段
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        name VARCHAR(16) NOT NULL,
                        realm VARCHAR(32) DEFAULT '炼气期',
                        realm_stage INT DEFAULT 1,
                        qi BIGINT DEFAULT 0,
                        spiritual_root DOUBLE DEFAULT 0.5,
                        spiritual_root_type VARCHAR(64) DEFAULT NULL,
                        comprehension DOUBLE DEFAULT 0.5,
                        technique_adaptation DOUBLE DEFAULT 0.6,
                        spirit_stones BIGINT DEFAULT 0,
                        contribution_points INT DEFAULT 0,
                        skill_points INT DEFAULT 0,
                        player_level INT DEFAULT 1,
                        sect_id INT DEFAULT NULL,
                        sect_rank VARCHAR(32) DEFAULT 'member',
                        last_login BIGINT,
                        created_at BIGINT,
                        updated_at BIGINT,
                        breakthrough_attempts INT DEFAULT 0,
                        successful_breakthroughs INT DEFAULT 0,
                        active_qi BIGINT DEFAULT 0,
                        last_fate_time BIGINT DEFAULT 0,
                        fate_count INT DEFAULT 0,
                        INDEX idx_sect (sect_id),
                        INDEX idx_realm (realm),
                        INDEX idx_last_login (last_login)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
            
            // 数据库迁移：添加 spiritual_root_type 字段（如果不存在）
            migrateAddSpiritualRootType();

            // 玩家功法表
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_player_skills (
                        player_uuid VARCHAR(36) NOT NULL,
                        skill_id VARCHAR(64) NOT NULL,
                        skill_level INT DEFAULT 1,
                        PRIMARY KEY (player_uuid, skill_id),
                        FOREIGN KEY (player_uuid) REFERENCES xian_players(uuid) ON DELETE CASCADE,
                        INDEX idx_player (player_uuid)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            // 玩家装备表
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_player_equipment (
                        player_uuid VARCHAR(36) NOT NULL,
                        slot VARCHAR(32) NOT NULL,
                        equipment_uuid VARCHAR(36) NOT NULL,
                        PRIMARY KEY (player_uuid, slot),
                        FOREIGN KEY (player_uuid) REFERENCES xian_players(uuid) ON DELETE CASCADE,
                        INDEX idx_player (player_uuid)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            // 宗门数据表 - 扩展字段
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_sects (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(32) UNIQUE NOT NULL,
                        description TEXT,
                        owner_uuid VARCHAR(36) NOT NULL,
                        owner_name VARCHAR(16) NOT NULL,
                        level INT DEFAULT 1,
                        experience BIGINT DEFAULT 0,
                        sect_funds BIGINT DEFAULT 0,
                        sect_contribution INT DEFAULT 0,
                        max_members INT DEFAULT 10,
                        recruiting BOOLEAN DEFAULT TRUE,
                        pvp_enabled BOOLEAN DEFAULT FALSE,
                        announcement TEXT,
                        residence_land_id VARCHAR(128),
                        land_center_world VARCHAR(64),
                        land_center_x DOUBLE DEFAULT 0,
                        land_center_y DOUBLE DEFAULT 0,
                        land_center_z DOUBLE DEFAULT 0,
                        last_maintenance_time BIGINT DEFAULT 0,
                        building_slots_data LONGTEXT,
                        created_at BIGINT,
                        updated_at BIGINT,
                        INDEX idx_name (name),
                        INDEX idx_owner (owner_uuid),
                        INDEX idx_residence (residence_land_id)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            // 数据库迁移：添加宗门领地相关字段（如果不存在）
            migrateAddResidenceLandFields();

            // 宗门成员表 - 扩展字段
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_sect_members (
                        sect_id INT NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        rank VARCHAR(32) DEFAULT 'OUTER_DISCIPLE',
                        contribution INT DEFAULT 0,
                        weekly_contribution INT DEFAULT 0,
                        joined_at BIGINT,
                        last_active_at BIGINT,
                        tasks_completed INT DEFAULT 0,
                        donation_count INT DEFAULT 0,
                        PRIMARY KEY (sect_id, player_uuid),
                        FOREIGN KEY (sect_id) REFERENCES xian_sects(id) ON DELETE CASCADE,
                        INDEX idx_sect (sect_id),
                        INDEX idx_player (player_uuid)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            // 天劫数据表
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_tribulations (
                        tribulation_uuid VARCHAR(36) PRIMARY KEY,
                        player_uuid VARCHAR(36) NOT NULL,
                        type VARCHAR(32) NOT NULL,
                        world_name VARCHAR(64) NOT NULL,
                        x DOUBLE NOT NULL,
                        y DOUBLE NOT NULL,
                        z DOUBLE NOT NULL,
                        current_wave INT DEFAULT 0,
                        total_waves INT NOT NULL,
                        active BOOLEAN DEFAULT FALSE,
                        completed BOOLEAN DEFAULT FALSE,
                        failed BOOLEAN DEFAULT FALSE,
                        start_time BIGINT,
                        end_time BIGINT,
                        last_wave_time BIGINT,
                        total_damage_dealt DOUBLE DEFAULT 0,
                        total_damage_taken DOUBLE DEFAULT 0,
                        FOREIGN KEY (player_uuid) REFERENCES xian_players(uuid) ON DELETE CASCADE,
                        INDEX idx_player (player_uuid),
                        INDEX idx_active (active)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            plugin.getLogger().info("§a✓ 数据表创建/检查完成");

        } catch (SQLException e) {
            plugin.getLogger().severe("§c✗ 创建数据表失败!");
            e.printStackTrace();
        }
    }
    
    /**
     * 数据库迁移：添加 spiritual_root_type 字段
     * 兼容旧版本数据库，如果字段不存在则添加
     */
    private void migrateAddSpiritualRootType() {
        if (!useMySql) {
            return; // 文件存储不需要迁移
        }
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 检查字段是否已存在
            boolean fieldExists = false;
            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM xian_players LIKE 'spiritual_root_type'")) {
                fieldExists = rs.next();
            }
            
            if (!fieldExists) {
                // 添加字段
                stmt.execute("ALTER TABLE xian_players ADD COLUMN spiritual_root_type VARCHAR(64) DEFAULT NULL AFTER spiritual_root");
                plugin.getLogger().info("§a✓ 数据库迁移：已添加 spiritual_root_type 字段");
                
                // 为现有玩家生成灵根类型（基于现有的灵根值）
                try (var updateStmt = conn.createStatement();
                     var rs = stmt.executeQuery("SELECT uuid, spiritual_root FROM xian_players WHERE spiritual_root_type IS NULL")) {
                    
                    int updatedCount = 0;
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");
                        double spiritualRoot = rs.getDouble("spiritual_root");
                        
                        // 根据灵根值生成类型
                        SpiritualRootType rootType = SpiritualRootType.fromValue(spiritualRoot);
                        
                        // 更新数据库
                        updateStmt.execute(String.format(
                            "UPDATE xian_players SET spiritual_root_type = '%s' WHERE uuid = '%s'",
                            rootType.name(), uuid
                        ));
                        updatedCount++;
                    }
                    
                    if (updatedCount > 0) {
                        plugin.getLogger().info("§a✓ 已为 " + updatedCount + " 个玩家生成灵根类型");
                    }
                }
            } else {
                plugin.getLogger().info("§7✓ spiritual_root_type 字段已存在，跳过迁移");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("§e数据库迁移失败（spiritual_root_type）: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 数据库迁移：添加宗门领地相关字段
     * 兼容旧版本数据库，如果字段不存在则添加
     */
    private void migrateAddResidenceLandFields() {
        if (!useMySql) {
            return; // 文件存储不需要迁移
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查 residence_land_id 字段是否已存在
            boolean fieldExists = false;
            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM xian_sects LIKE 'residence_land_id'")) {
                fieldExists = rs.next();
            }

            if (!fieldExists) {
                // 添加领地相关字段
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN residence_land_id VARCHAR(128) DEFAULT NULL");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_world VARCHAR(64) DEFAULT NULL");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_x DOUBLE DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_y DOUBLE DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_z DOUBLE DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN last_maintenance_time BIGINT DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN building_slots_data LONGTEXT");

                // 添加索引
                try {
                    stmt.execute("CREATE INDEX idx_residence ON xian_sects(residence_land_id)");
                } catch (SQLException ignored) {
                    // 索引可能已存在
                }

                plugin.getLogger().info("§a✓ 数据库迁移：已添加宗门领地相关字段");
            } else {
                plugin.getLogger().info("§7✓ 宗门领地字段已存在，跳过迁移");
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e数据库迁移失败（residence_land）: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (!useMySql || dataSource == null) {
            throw new SQLException("MySQL 未启用或连接池未初始化");
        }
        return dataSource.getConnection();
    }

    /**
     * 加载玩家数据
     *
     * @param uuid 玩家 UUID
     * @return 玩家数据
     */
    public PlayerData loadPlayerData(UUID uuid) {
        // 先从缓存获取
        PlayerData cached = playerDataCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        // 从数据库或文件加载
        PlayerData data = useMySql ? loadFromDatabase(uuid) : loadFromFile(uuid);

        // 缓存数据
        if (data != null) {
            playerDataCache.put(uuid, data);
        }

        return data;
    }

    /**
     * 创建新玩家数据
     *
     * @param uuid 玩家 UUID
     * @return 新创建的玩家数据
     */
    public PlayerData createPlayerData(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        data.setCreatedAt(System.currentTimeMillis());
        data.setLastLogin(System.currentTimeMillis());

        // 随机生成灵根类型（新系统）
        SpiritualRootType rootType = SpiritualRootType.randomGenerate();
        data.setSpiritualRootType(rootType);
        
        // 生成该类型范围内的随机值
        double spiritualRoot = rootType.generateValue();
        data.setSpiritualRoot(spiritualRoot);

        // 记录日志
        plugin.getLogger().info("新玩家 " + uuid + " 获得灵根: " + 
            rootType.getFullName() + " (" + String.format("%.3f", spiritualRoot) + ")");

        // 缓存并保存
        playerDataCache.put(uuid, data);
        savePlayerData(data);

        return data;
    }

    /**
     * 从数据库加载玩家数据
     */
    private PlayerData loadFromDatabase(UUID uuid) {
        String sql = """
                SELECT * FROM xian_players WHERE uuid = ?
                """;

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());

            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    PlayerData data = new PlayerData(uuid);
                    data.setName(rs.getString("name"));
                    data.setRealm(rs.getString("realm"));
                    data.setRealmStage(rs.getInt("realm_stage"));
                    data.setQi(rs.getLong("qi"));
                    data.setSpiritualRoot(rs.getDouble("spiritual_root"));
                    
                    // 加载灵根类型（新系统）
                    String rootTypeStr = rs.getString("spiritual_root_type");
                    if (rootTypeStr != null && !rootTypeStr.isEmpty()) {
                        try {
                            data.setSpiritualRootType(SpiritualRootType.valueOf(rootTypeStr));
                        } catch (IllegalArgumentException e) {
                            // 如果类型不存在，根据数值生成一个
                            data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
                        }
                    } else {
                        // 兼容旧数据：根据灵根数值生成类型
                        data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
                    }
                    
                    data.setComprehension(rs.getDouble("comprehension"));
                    data.setTechniqueAdaptation(rs.getDouble("technique_adaptation"));
                    data.setSpiritStones(rs.getLong("spirit_stones"));
                    data.setContributionPoints(rs.getInt("contribution_points"));
                    data.setSkillPoints(rs.getInt("skill_points"));
                    data.setPlayerLevel(rs.getInt("player_level"));
                    data.setSectId(rs.getObject("sect_id", Integer.class));
                    data.setSectRank(rs.getString("sect_rank"));
                    data.setLastLogin(rs.getLong("last_login"));
                    data.setCreatedAt(rs.getLong("created_at"));
                    data.setUpdatedAt(rs.getLong("updated_at"));
                    data.setBreakthroughAttempts(rs.getInt("breakthrough_attempts"));
                    data.setSuccessfulBreakthroughs(rs.getInt("successful_breakthroughs"));
                    data.setActiveQi(rs.getLong("active_qi"));
                    data.setLastFateTime(rs.getLong("last_fate_time"));
                    data.setFateCount(rs.getInt("fate_count"));

                    // 加载功法数据
                    loadPlayerSkills(uuid, data);

                    // 加载装备数据
                    loadPlayerEquipment(uuid, data);

                    return data;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库加载玩家数据失败: " + uuid);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 加载玩家功法数据
     */
    private void loadPlayerSkills(UUID uuid, PlayerData data) {
        String sql = "SELECT skill_id, skill_level FROM xian_player_skills WHERE player_uuid = ?";

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());

            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String skillId = rs.getString("skill_id");
                    int level = rs.getInt("skill_level");
                    data.getLearnedSkills().put(skillId, level);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e加载玩家功法数据失败: " + uuid);
            e.printStackTrace();
        }
    }

    /**
     * 加载玩家装备数据
     */
    private void loadPlayerEquipment(UUID uuid, PlayerData data) {
        String sql = "SELECT slot, equipment_uuid FROM xian_player_equipment WHERE player_uuid = ?";

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid.toString());

            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String slot = rs.getString("slot");
                    String equipmentUuid = rs.getString("equipment_uuid");
                    data.getEquipment().put(slot, equipmentUuid);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e加载玩家装备数据失败: " + uuid);
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载玩家数据
     */
    private PlayerData loadFromFile(UUID uuid) {
        File file = new File(plugin.getDataFolder(), "players/" + uuid + ".yml");
        if (!file.exists()) {
            return null;
        }

        try {
            org.bukkit.configuration.file.YamlConfiguration config =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

            PlayerData data = new PlayerData(uuid);
            data.setName(config.getString("name", ""));
            data.setRealm(config.getString("realm", "炼气期"));
            data.setRealmStage(config.getInt("realm_stage", 1));
            data.setQi(config.getLong("qi", 0));
            data.setSpiritualRoot(config.getDouble("spiritual_root", 0.5));
            
            // 加载灵根类型（新系统）
            String rootTypeStr = config.getString("spiritual_root_type");
            if (rootTypeStr != null && !rootTypeStr.isEmpty()) {
                try {
                    data.setSpiritualRootType(SpiritualRootType.valueOf(rootTypeStr));
                } catch (IllegalArgumentException e) {
                    // 如果类型不存在，根据数值生成一个
                    data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
                }
            } else {
                // 兼容旧数据：根据灵根数值生成类型
                data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
            }
            
            data.setComprehension(config.getDouble("comprehension", 0.5));
            data.setTechniqueAdaptation(config.getDouble("technique_adaptation", 0.6));
            data.setSpiritStones(config.getLong("spirit_stones", 0));
            data.setContributionPoints(config.getInt("contribution_points", 0));
            data.setSkillPoints(config.getInt("skill_points", 0));
            data.setPlayerLevel(config.getInt("player_level", 1));

            if (config.contains("sect_id")) {
                data.setSectId(config.getInt("sect_id"));
            }

            data.setSectRank(config.getString("sect_rank", "member"));
            data.setLastLogin(config.getLong("last_login", System.currentTimeMillis()));
            data.setCreatedAt(config.getLong("created_at", System.currentTimeMillis()));
            data.setUpdatedAt(config.getLong("updated_at", System.currentTimeMillis()));
            data.setBreakthroughAttempts(config.getInt("breakthrough_attempts", 0));
            data.setSuccessfulBreakthroughs(config.getInt("successful_breakthroughs", 0));
            data.setActiveQi(config.getLong("active_qi", 0));
            data.setLastFateTime(config.getLong("last_fate_time", 0));
            data.setFateCount(config.getInt("fate_count", 0));

            // 加载功法数据
            if (config.contains("learned_skills")) {
                for (String skillId : config.getConfigurationSection("learned_skills").getKeys(false)) {
                    int level = config.getInt("learned_skills." + skillId, 1);
                    data.getLearnedSkills().put(skillId, level);
                }
            }

            // 加载功法绑定数据
            if (config.contains("skill_bindings")) {
                for (String slotStr : config.getConfigurationSection("skill_bindings").getKeys(false)) {
                    int slot = Integer.parseInt(slotStr);
                    String skillId = config.getString("skill_bindings." + slotStr);
                    data.getSkillBindings().put(slot, skillId);
                }
            }

            // 加载装备数据
            if (config.contains("equipment")) {
                for (String slot : config.getConfigurationSection("equipment").getKeys(false)) {
                    String equipmentUuid = config.getString("equipment." + slot);
                    data.getEquipment().put(slot, equipmentUuid);
                }
            }

            return data;
        } catch (Exception e) {
            plugin.getLogger().warning("§e从文件加载玩家数据失败: " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存玩家数据
     *
     * @param data 玩家数据
     */
    public void savePlayerData(PlayerData data) {
        if (useMySql) {
            saveToDatabase(data);
        } else {
            saveToFile(data);
        }
    }

    /**
     * 保存到数据库
     */
    private void saveToDatabase(PlayerData data) {
        String sql = """
                INSERT INTO xian_players (
                    uuid, name, realm, realm_stage, qi, spiritual_root, spiritual_root_type, comprehension,
                    technique_adaptation, spirit_stones, contribution_points, skill_points,
                    player_level, sect_id, sect_rank, last_login, created_at, updated_at,
                    breakthrough_attempts, successful_breakthroughs, active_qi, last_fate_time, fate_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    realm = VALUES(realm),
                    realm_stage = VALUES(realm_stage),
                    qi = VALUES(qi),
                    spiritual_root = VALUES(spiritual_root),
                    spiritual_root_type = VALUES(spiritual_root_type),
                    comprehension = VALUES(comprehension),
                    technique_adaptation = VALUES(technique_adaptation),
                    spirit_stones = VALUES(spirit_stones),
                    contribution_points = VALUES(contribution_points),
                    skill_points = VALUES(skill_points),
                    player_level = VALUES(player_level),
                    sect_id = VALUES(sect_id),
                    sect_rank = VALUES(sect_rank),
                    last_login = VALUES(last_login),
                    updated_at = VALUES(updated_at),
                    breakthrough_attempts = VALUES(breakthrough_attempts),
                    successful_breakthroughs = VALUES(successful_breakthroughs),
                    active_qi = VALUES(active_qi),
                    last_fate_time = VALUES(last_fate_time),
                    fate_count = VALUES(fate_count)
                """;

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, data.getUuid().toString());
            pstmt.setString(2, data.getName());
            pstmt.setString(3, data.getRealm());
            pstmt.setInt(4, data.getRealmStage());
            pstmt.setLong(5, data.getQi());
            pstmt.setDouble(6, data.getSpiritualRoot());
            
            // 新增：灵根类型
            if (data.getSpiritualRootType() != null) {
                pstmt.setString(7, data.getSpiritualRootType().name());
            } else {
                pstmt.setNull(7, java.sql.Types.VARCHAR);
            }
            
            pstmt.setDouble(8, data.getComprehension());
            pstmt.setDouble(9, data.getTechniqueAdaptation());
            pstmt.setLong(10, data.getSpiritStones());
            pstmt.setInt(11, data.getContributionPoints());
            pstmt.setInt(12, data.getSkillPoints());
            pstmt.setInt(13, data.getPlayerLevel());

            if (data.getSectId() != null) {
                pstmt.setInt(14, data.getSectId());
            } else {
                pstmt.setNull(14, java.sql.Types.INTEGER);
            }

            pstmt.setString(15, data.getSectRank());
            pstmt.setLong(16, data.getLastLogin());
            pstmt.setLong(17, data.getCreatedAt());
            pstmt.setLong(18, System.currentTimeMillis());
            pstmt.setInt(19, data.getBreakthroughAttempts());
            pstmt.setInt(20, data.getSuccessfulBreakthroughs());
            pstmt.setLong(21, data.getActiveQi());
            pstmt.setLong(22, data.getLastFateTime());
            pstmt.setInt(23, data.getFateCount());

            pstmt.executeUpdate();

            // 保存功法数据
            savePlayerSkills(data);

            // 保存装备数据
            savePlayerEquipment(data);

        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存玩家数据到数据库失败: " + data.getUuid());
            e.printStackTrace();
        }
    }

    /**
     * 保存玩家功法数据
     */
    private void savePlayerSkills(PlayerData data) {
        // 先删除旧数据
        String deleteSql = "DELETE FROM xian_player_skills WHERE player_uuid = ?";
        String insertSql = "INSERT INTO xian_player_skills (player_uuid, skill_id, skill_level) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            // 删除旧数据
            try (var pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, data.getUuid().toString());
                pstmt.executeUpdate();
            }

            // 插入新数据
            try (var pstmt = conn.prepareStatement(insertSql)) {
                for (var entry : data.getLearnedSkills().entrySet()) {
                    pstmt.setString(1, data.getUuid().toString());
                    pstmt.setString(2, entry.getKey());
                    pstmt.setInt(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存玩家功法数据失败: " + data.getUuid());
            e.printStackTrace();
        }
    }

    /**
     * 保存玩家装备数据
     */
    private void savePlayerEquipment(PlayerData data) {
        // 先删除旧数据
        String deleteSql = "DELETE FROM xian_player_equipment WHERE player_uuid = ?";
        String insertSql = "INSERT INTO xian_player_equipment (player_uuid, slot, equipment_uuid) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            // 删除旧数据
            try (var pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, data.getUuid().toString());
                pstmt.executeUpdate();
            }

            // 插入新数据
            try (var pstmt = conn.prepareStatement(insertSql)) {
                for (var entry : data.getEquipment().entrySet()) {
                    pstmt.setString(1, data.getUuid().toString());
                    pstmt.setString(2, entry.getKey());
                    pstmt.setString(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存玩家装备数据失败: " + data.getUuid());
            e.printStackTrace();
        }
    }

    /**
     * 保存到文件
     */
    private void saveToFile(PlayerData data) {
        File playerDir = new File(plugin.getDataFolder(), "players");
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }

        File file = new File(playerDir, data.getUuid() + ".yml");

        try {
            org.bukkit.configuration.file.YamlConfiguration config =
                new org.bukkit.configuration.file.YamlConfiguration();

            // 保存基础数据
            config.set("name", data.getName());
            config.set("realm", data.getRealm());
            config.set("realm_stage", data.getRealmStage());
            config.set("qi", data.getQi());
            config.set("spiritual_root", data.getSpiritualRoot());
            
            // 保存灵根类型（新系统）
            if (data.getSpiritualRootType() != null) {
                config.set("spiritual_root_type", data.getSpiritualRootType().name());
            }
            
            config.set("comprehension", data.getComprehension());
            config.set("technique_adaptation", data.getTechniqueAdaptation());
            config.set("spirit_stones", data.getSpiritStones());
            config.set("contribution_points", data.getContributionPoints());
            config.set("skill_points", data.getSkillPoints());
            config.set("player_level", data.getPlayerLevel());

            if (data.getSectId() != null) {
                config.set("sect_id", data.getSectId());
            }

            config.set("sect_rank", data.getSectRank());
            config.set("last_login", data.getLastLogin());
            config.set("created_at", data.getCreatedAt());
            config.set("updated_at", data.getUpdatedAt());
            config.set("breakthrough_attempts", data.getBreakthroughAttempts());
            config.set("successful_breakthroughs", data.getSuccessfulBreakthroughs());
            config.set("active_qi", data.getActiveQi());
            config.set("last_fate_time", data.getLastFateTime());
            config.set("fate_count", data.getFateCount());

            // 保存功法数据
            for (var entry : data.getLearnedSkills().entrySet()) {
                config.set("learned_skills." + entry.getKey(), entry.getValue());
            }

            // 保存功法绑定数据
            for (var entry : data.getSkillBindings().entrySet()) {
                config.set("skill_bindings." + entry.getKey(), entry.getValue());
            }

            // 保存装备数据
            for (var entry : data.getEquipment().entrySet()) {
                config.set("equipment." + entry.getKey(), entry.getValue());
            }

            // 保存到文件
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("§e保存玩家数据到文件失败: " + data.getUuid());
            e.printStackTrace();
        }
    }

    /**
     * 保存所有玩家数据
     */
    public void saveAll() {
        plugin.getLogger().info(String.format("正在保存 %d 个玩家的数据...", playerDataCache.size()));

        for (PlayerData data : playerDataCache.values()) {
            savePlayerData(data);
        }

        plugin.getLogger().info("§a所有玩家数据已保存!");

        // 保存所有宗门数据
        if (plugin.getSectSystem() != null) {
            plugin.getSectSystem().saveAll();
        }
    }

    /**
     * 移除玩家数据缓存
     *
     * @param uuid 玩家 UUID
     */
    public void removePlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
    }

    // ==================== 宗门数据持久化 ====================

    /**
     * 保存宗门数据
     */
    public void saveSect(com.xiancore.systems.sect.Sect sect) {
        if (useMySql) {
            saveSectToDatabase(sect);
        } else {
            saveSectToFile(sect);
        }
    }

    /**
     * 保存宗门数据到数据库
     */
    private void saveSectToDatabase(com.xiancore.systems.sect.Sect sect) {
        String sql = """
                INSERT INTO xian_sects (
                    id, name, description, owner_uuid, owner_name, level, experience,
                    sect_funds, sect_contribution, max_members, recruiting, pvp_enabled,
                    announcement, residence_land_id, land_center_world, land_center_x,
                    land_center_y, land_center_z, last_maintenance_time, building_slots_data,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    description = VALUES(description),
                    owner_uuid = VALUES(owner_uuid),
                    owner_name = VALUES(owner_name),
                    level = VALUES(level),
                    experience = VALUES(experience),
                    sect_funds = VALUES(sect_funds),
                    sect_contribution = VALUES(sect_contribution),
                    max_members = VALUES(max_members),
                    recruiting = VALUES(recruiting),
                    pvp_enabled = VALUES(pvp_enabled),
                    announcement = VALUES(announcement),
                    residence_land_id = VALUES(residence_land_id),
                    land_center_world = VALUES(land_center_world),
                    land_center_x = VALUES(land_center_x),
                    land_center_y = VALUES(land_center_y),
                    land_center_z = VALUES(land_center_z),
                    last_maintenance_time = VALUES(last_maintenance_time),
                    building_slots_data = VALUES(building_slots_data),
                    updated_at = VALUES(updated_at)
                """;

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sect.getId());
            pstmt.setString(2, sect.getName());
            pstmt.setString(3, sect.getDescription());
            pstmt.setString(4, sect.getOwnerId().toString());
            pstmt.setString(5, sect.getOwnerName());
            pstmt.setInt(6, sect.getLevel());
            pstmt.setLong(7, sect.getExperience());
            pstmt.setLong(8, sect.getSectFunds());
            pstmt.setInt(9, sect.getSectContribution());
            pstmt.setInt(10, sect.getMaxMembers());
            pstmt.setBoolean(11, sect.isRecruiting());
            pstmt.setBoolean(12, sect.isPvpEnabled());
            pstmt.setString(13, sect.getAnnouncement());

            // 保存领地相关数据
            pstmt.setString(14, sect.getResidenceLandId());
            if (sect.getLandCenter() != null) {
                pstmt.setString(15, sect.getLandCenter().getWorld().getName());
                pstmt.setDouble(16, sect.getLandCenter().getX());
                pstmt.setDouble(17, sect.getLandCenter().getY());
                pstmt.setDouble(18, sect.getLandCenter().getZ());
            } else {
                pstmt.setString(15, null);
                pstmt.setDouble(16, 0);
                pstmt.setDouble(17, 0);
                pstmt.setDouble(18, 0);
            }

            // 维护费相关
            pstmt.setLong(19, sect.getLastMaintenanceTime());

            // 建筑位数据 - 使用简单的序列化格式 (key1:value1;key2:value2)
            StringBuilder buildingSlotsStr = new StringBuilder();
            for (java.util.Map.Entry<String, Integer> entry : sect.getBuildingSlots().entrySet()) {
                if (buildingSlotsStr.length() > 0) {
                    buildingSlotsStr.append(";");
                }
                buildingSlotsStr.append(entry.getKey()).append(":").append(entry.getValue());
            }
            pstmt.setString(20, buildingSlotsStr.toString());

            pstmt.setLong(21, sect.getCreatedAt());
            pstmt.setLong(22, sect.getUpdatedAt());

            pstmt.executeUpdate();

            // 保存成员数据
            saveSectMembers(sect);

        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存宗门数据到数据库失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    /**
     * 保存宗门数据到文件
     */
    private void saveSectToFile(com.xiancore.systems.sect.Sect sect) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            sectDir.mkdirs();
        }

        File file = new File(sectDir, sect.getId() + ".yml");

        try {
            org.bukkit.configuration.file.YamlConfiguration config =
                new org.bukkit.configuration.file.YamlConfiguration();

            // 保存基础数据
            config.set("id", sect.getId());
            config.set("name", sect.getName());
            config.set("description", sect.getDescription());
            config.set("owner_uuid", sect.getOwnerId().toString());
            config.set("owner_name", sect.getOwnerName());
            config.set("level", sect.getLevel());
            config.set("experience", sect.getExperience());
            config.set("sect_funds", sect.getSectFunds());
            config.set("sect_contribution", sect.getSectContribution());
            config.set("max_members", sect.getMaxMembers());
            config.set("recruiting", sect.isRecruiting());
            config.set("pvp_enabled", sect.isPvpEnabled());
            config.set("announcement", sect.getAnnouncement());
            config.set("created_at", sect.getCreatedAt());
            config.set("updated_at", sect.getUpdatedAt());

            // 保存领地相关数据
            config.set("residence_land_id", sect.getResidenceLandId());
            if (sect.getLandCenter() != null) {
                config.set("land_center.world", sect.getLandCenter().getWorld().getName());
                config.set("land_center.x", sect.getLandCenter().getX());
                config.set("land_center.y", sect.getLandCenter().getY());
                config.set("land_center.z", sect.getLandCenter().getZ());
            }

            // 保存维护费相关
            config.set("last_maintenance_time", sect.getLastMaintenanceTime());
            config.set("building_slots", sect.getBuildingSlots());

            // 保存成员数据
            for (var member : sect.getMemberList()) {
                String memberPath = "members." + member.getPlayerId().toString();
                config.set(memberPath + ".name", member.getPlayerName());
                config.set(memberPath + ".rank", member.getRank().name());
                config.set(memberPath + ".contribution", member.getContribution());
                config.set(memberPath + ".weekly_contribution", member.getWeeklyContribution());
                config.set(memberPath + ".joined_at", member.getJoinedAt());
                config.set(memberPath + ".last_active_at", member.getLastActiveAt());
                config.set(memberPath + ".tasks_completed", member.getTasksCompleted());
                config.set(memberPath + ".donation_count", member.getDonationCount());
            }

            // 保存到文件
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("§e保存宗门数据到文件失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    // ==================== 仓库数据持久化 ====================

    /**
     * 保存仓库数据到文件
     */
    public void saveWarehouseToFile(com.xiancore.systems.sect.warehouse.SectWarehouse warehouse) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            sectDir.mkdirs();
        }

        File file = new File(sectDir, warehouse.getSectId() + "_warehouse.yml");

        // 重试机制
        int retryCount = 3;
        Exception lastException = null;

        for (int i = 0; i < retryCount; i++) {
            try {
                org.bukkit.configuration.file.YamlConfiguration config =
                    new org.bukkit.configuration.file.YamlConfiguration();

                config.set("sect_id", warehouse.getSectId());
                config.set("capacity", warehouse.getCapacity());

                // 保存物品数据
                for (java.util.Map.Entry<Integer, org.bukkit.inventory.ItemStack> entry : 
                     warehouse.getAllItems().entrySet()) {
                    int slot = entry.getKey();
                    org.bukkit.inventory.ItemStack item = entry.getValue();

                    String itemPath = "items." + slot;

                    // 使用 Bukkit 的序列化 API
                    config.set(itemPath, item.serialize());
                }

                // 保存到文件
                config.save(file);
                return; // 成功，退出

            } catch (Exception e) {
                lastException = e;
                plugin.getLogger().warning("§e保存仓库数据失败，重试 " + (i + 1) + "/" + retryCount + 
                                          ": " + warehouse.getSectId());

                // 等待后重试
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // 所有重试都失败
        plugin.getLogger().severe("§c保存仓库数据彻底失败: " + warehouse.getSectId());
        if (lastException != null) {
            lastException.printStackTrace();
        }
    }

    /**
     * 从文件加载仓库数据
     */
    public com.xiancore.systems.sect.warehouse.SectWarehouse loadWarehouseFromFile(int sectId) {
        File file = new File(plugin.getDataFolder(), "sects/" + sectId + "_warehouse.yml");

        if (!file.exists()) {
            return null; // 文件不存在，返回 null
        }

        try {
            org.bukkit.configuration.file.YamlConfiguration config =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

            int capacity = config.getInt("capacity", 27);
            com.xiancore.systems.sect.warehouse.SectWarehouse warehouse =
                new com.xiancore.systems.sect.warehouse.SectWarehouse(sectId, capacity);

            // 加载物品数据
            if (config.contains("items")) {
                org.bukkit.configuration.ConfigurationSection itemsSection = 
                    config.getConfigurationSection("items");
                
                if (itemsSection != null) {
                    for (String slotStr : itemsSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            
                            // 使用 Bukkit 的反序列化 API
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> itemData = 
                                (java.util.Map<String, Object>) config.get("items." + slotStr);
                            
                            if (itemData != null) {
                                org.bukkit.inventory.ItemStack item = 
                                    org.bukkit.inventory.ItemStack.deserialize(itemData);
                                warehouse.setItem(slot, item);
                            }
                            
                        } catch (Exception e) {
                            plugin.getLogger().warning("§e加载仓库物品失败 (宗门=" + sectId + 
                                                      ", 槽位=" + slotStr + "): " + e.getMessage());
                        }
                    }
                }
            }

            return warehouse;

        } catch (Exception e) {
            plugin.getLogger().warning("§e加载仓库数据失败: " + sectId);
            e.printStackTrace();
            return null;
        }
    }

    // ==================== 设施数据持久化 ====================

    /**
     * 保存设施数据到文件
     */
    public void saveFacilityDataToFile(com.xiancore.systems.sect.facilities.SectFacilityData data) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            sectDir.mkdirs();
        }

        File file = new File(sectDir, data.getSectId() + "_facilities.yml");

        // 重试机制
        int retryCount = 3;
        Exception lastException = null;

        for (int i = 0; i < retryCount; i++) {
            try {
                org.bukkit.configuration.file.YamlConfiguration config =
                    new org.bukkit.configuration.file.YamlConfiguration();

                config.set("sect_id", data.getSectId());
                config.set("last_updated", data.getLastUpdated());

                // 保存设施等级
                for (java.util.Map.Entry<String, Integer> entry : data.getFacilityLevels().entrySet()) {
                    config.set("facility_levels." + entry.getKey(), entry.getValue());
                }

                // 保存到文件
                config.save(file);
                return; // 成功，退出

            } catch (Exception e) {
                lastException = e;
                plugin.getLogger().warning("§e保存设施数据失败，重试 " + (i + 1) + "/" + retryCount + 
                                          ": " + data.getSectId());

                // 等待后重试
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // 所有重试都失败
        plugin.getLogger().severe("§c保存设施数据彻底失败: " + data.getSectId());
        if (lastException != null) {
            lastException.printStackTrace();
        }
    }

    /**
     * 从文件加载设施数据
     */
    public com.xiancore.systems.sect.facilities.SectFacilityData loadFacilityDataFromFile(int sectId) {
        File file = new File(plugin.getDataFolder(), "sects/" + sectId + "_facilities.yml");

        if (!file.exists()) {
            return null; // 文件不存在，返回 null
        }

        try {
            org.bukkit.configuration.file.YamlConfiguration config =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

            com.xiancore.systems.sect.facilities.SectFacilityData data =
                new com.xiancore.systems.sect.facilities.SectFacilityData(sectId);

            data.setLastUpdated(config.getLong("last_updated", System.currentTimeMillis()));

            // 加载设施等级
            if (config.contains("facility_levels")) {
                org.bukkit.configuration.ConfigurationSection levelsSection = 
                    config.getConfigurationSection("facility_levels");
                
                if (levelsSection != null) {
                    for (String facilityId : levelsSection.getKeys(false)) {
                        int level = config.getInt("facility_levels." + facilityId, 0);
                        data.getFacilityLevels().put(facilityId, level);
                    }
                }
            }

            return data;

        } catch (Exception e) {
            plugin.getLogger().warning("§e加载设施数据失败: " + sectId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存宗门成员数据
     */
    private void saveSectMembers(com.xiancore.systems.sect.Sect sect) {
        // 先删除旧数据
        String deleteSql = "DELETE FROM xian_sect_members WHERE sect_id = ?";
        String insertSql = """
                INSERT INTO xian_sect_members (
                    sect_id, player_uuid, player_name, rank, contribution,
                    weekly_contribution, joined_at, last_active_at,
                    tasks_completed, donation_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = getConnection()) {
            // 删除旧数据
            try (var pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, sect.getId());
                pstmt.executeUpdate();
            }

            // 插入新数据
            try (var pstmt = conn.prepareStatement(insertSql)) {
                for (var member : sect.getMemberList()) {
                    pstmt.setInt(1, sect.getId());
                    pstmt.setString(2, member.getPlayerId().toString());
                    pstmt.setString(3, member.getPlayerName());
                    pstmt.setString(4, member.getRank().name());
                    pstmt.setInt(5, member.getContribution());
                    pstmt.setInt(6, member.getWeeklyContribution());
                    pstmt.setLong(7, member.getJoinedAt());
                    pstmt.setLong(8, member.getLastActiveAt());
                    pstmt.setInt(9, member.getTasksCompleted());
                    pstmt.setInt(10, member.getDonationCount());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存宗门成员数据失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    /**
     * 加载所有宗门数据
     */
    public java.util.List<com.xiancore.systems.sect.Sect> loadAllSects() {
        if (useMySql) {
            return loadSectsFromDatabase();
        } else {
            return loadSectsFromFile();
        }
    }

    /**
     * 从数据库加载所有宗门
     */
    private java.util.List<com.xiancore.systems.sect.Sect> loadSectsFromDatabase() {
        java.util.List<com.xiancore.systems.sect.Sect> sects = new java.util.ArrayList<>();

        String sql = "SELECT * FROM xian_sects";

        try (Connection conn = getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                java.util.UUID ownerUuid = java.util.UUID.fromString(rs.getString("owner_uuid"));
                String ownerName = rs.getString("owner_name");

                // 从数据库加载时不自动添加宗主（成员会从 sect_members 表加载）
                com.xiancore.systems.sect.Sect sect = new com.xiancore.systems.sect.Sect(id, name, ownerUuid, ownerName, false);
                sect.setDescription(rs.getString("description"));
                sect.setLevel(rs.getInt("level"));
                sect.setExperience(rs.getLong("experience"));
                sect.setSectFunds(rs.getLong("sect_funds"));
                sect.setSectContribution(rs.getInt("sect_contribution"));
                sect.setMaxMembers(rs.getInt("max_members"));
                sect.setRecruiting(rs.getBoolean("recruiting"));
                sect.setPvpEnabled(rs.getBoolean("pvp_enabled"));
                sect.setAnnouncement(rs.getString("announcement"));
                sect.setCreatedAt(rs.getLong("created_at"));
                sect.setUpdatedAt(rs.getLong("updated_at"));

                // 加载领地相关数据
                sect.setResidenceLandId(rs.getString("residence_land_id"));
                String worldName = rs.getString("land_center_world");
                if (worldName != null && !worldName.isEmpty()) {
                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = rs.getDouble("land_center_x");
                        double y = rs.getDouble("land_center_y");
                        double z = rs.getDouble("land_center_z");
                        sect.setLandCenter(new org.bukkit.Location(world, x, y, z));
                    }
                }

                // 加载维护费相关
                sect.setLastMaintenanceTime(rs.getLong("last_maintenance_time"));

                // 加载建筑位数据
                String buildingSlotsJson = rs.getString("building_slots_data");
                if (buildingSlotsJson != null && !buildingSlotsJson.isEmpty()) {
                    try {
                        java.util.Map<String, Integer> buildingSlots = new java.util.HashMap<>();
                        String[] pairs = buildingSlotsJson.split(";");
                        for (String pair : pairs) {
                            if (pair.contains(":")) {
                                String[] kv = pair.split(":");
                                buildingSlots.put(kv[0], Integer.parseInt(kv[1]));
                            }
                        }
                        sect.setBuildingSlots(buildingSlots);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to deserialize building slots for sect " + id);
                    }
                }

                // 加载成员数据
                loadSectMembers(sect);

                sects.add(sect);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库加载宗门数据失败");
            e.printStackTrace();
        }

        return sects;
    }

    /**
     * 从文件加载所有宗门
     */
    private java.util.List<com.xiancore.systems.sect.Sect> loadSectsFromFile() {
        java.util.List<com.xiancore.systems.sect.Sect> sects = new java.util.ArrayList<>();

        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            return sects;
        }

        File[] sectFiles = sectDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (sectFiles == null) {
            return sects;
        }

        for (File file : sectFiles) {
            try {
                org.bukkit.configuration.file.YamlConfiguration config =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

                int id = config.getInt("id");
                String name = config.getString("name");
                java.util.UUID ownerUuid = java.util.UUID.fromString(config.getString("owner_uuid"));
                String ownerName = config.getString("owner_name");

                // 从文件加载时不自动添加宗主（成员会从文件中加载）
                com.xiancore.systems.sect.Sect sect = new com.xiancore.systems.sect.Sect(id, name, ownerUuid, ownerName, false);
                sect.setDescription(config.getString("description", ""));
                sect.setLevel(config.getInt("level", 1));
                sect.setExperience(config.getLong("experience", 0));
                sect.setSectFunds(config.getLong("sect_funds", 0));
                sect.setSectContribution(config.getInt("sect_contribution", 0));
                sect.setMaxMembers(config.getInt("max_members", 10));
                sect.setRecruiting(config.getBoolean("recruiting", true));
                sect.setPvpEnabled(config.getBoolean("pvp_enabled", false));
                sect.setAnnouncement(config.getString("announcement", ""));
                sect.setCreatedAt(config.getLong("created_at", System.currentTimeMillis()));
                sect.setUpdatedAt(config.getLong("updated_at", System.currentTimeMillis()));

                // 加载领地相关数据
                sect.setResidenceLandId(config.getString("residence_land_id", null));
                if (config.contains("land_center.world")) {
                    String worldName = config.getString("land_center.world");
                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
                    if (world != null) {
                        double x = config.getDouble("land_center.x", 0);
                        double y = config.getDouble("land_center.y", 0);
                        double z = config.getDouble("land_center.z", 0);
                        sect.setLandCenter(new org.bukkit.Location(world, x, y, z));
                    }
                }

                // 加载维护费相关
                sect.setLastMaintenanceTime(config.getLong("last_maintenance_time", 0));

                // 加载建筑位数据
                if (config.contains("building_slots")) {
                    java.util.Map<String, Integer> buildingSlots = new java.util.HashMap<>();
                    for (String key : config.getConfigurationSection("building_slots").getKeys(false)) {
                        buildingSlots.put(key, config.getInt("building_slots." + key, 0));
                    }
                    sect.setBuildingSlots(buildingSlots);
                }

                // 加载成员数据
                if (config.contains("members")) {
                    for (String uuidStr : config.getConfigurationSection("members").getKeys(false)) {
                        String memberPath = "members." + uuidStr;
                        java.util.UUID playerUuid = java.util.UUID.fromString(uuidStr);
                        String playerName = config.getString(memberPath + ".name");

                        com.xiancore.systems.sect.SectMember member = new com.xiancore.systems.sect.SectMember(playerUuid, playerName);
                        member.setRank(com.xiancore.systems.sect.SectRank.valueOf(config.getString(memberPath + ".rank", "OUTER_DISCIPLE")));
                        member.setContribution(config.getInt(memberPath + ".contribution", 0));
                        member.setWeeklyContribution(config.getInt(memberPath + ".weekly_contribution", 0));
                        member.setJoinedAt(config.getLong(memberPath + ".joined_at", System.currentTimeMillis()));
                        member.setLastActiveAt(config.getLong(memberPath + ".last_active_at", System.currentTimeMillis()));
                        member.setTasksCompleted(config.getInt(memberPath + ".tasks_completed", 0));
                        member.setDonationCount(config.getInt(memberPath + ".donation_count", 0));

                        sect.getMembers().put(playerUuid, member);
                    }
                }

                sects.add(sect);
            } catch (Exception e) {
                plugin.getLogger().warning("§e从文件加载宗门数据失败: " + file.getName());
                e.printStackTrace();
            }
        }

        return sects;
    }

    /**
     * 加载宗门成员数据
     */
    private void loadSectMembers(com.xiancore.systems.sect.Sect sect) {
        String sql = "SELECT * FROM xian_sect_members WHERE sect_id = ?";

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sect.getId());

            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    java.util.UUID playerUuid = java.util.UUID.fromString(rs.getString("player_uuid"));
                    String playerName = rs.getString("player_name");

                    com.xiancore.systems.sect.SectMember member = new com.xiancore.systems.sect.SectMember(playerUuid, playerName);
                    member.setRank(com.xiancore.systems.sect.SectRank.valueOf(rs.getString("rank")));
                    member.setContribution(rs.getInt("contribution"));
                    member.setWeeklyContribution(rs.getInt("weekly_contribution"));
                    member.setJoinedAt(rs.getLong("joined_at"));
                    member.setLastActiveAt(rs.getLong("last_active_at"));
                    member.setTasksCompleted(rs.getInt("tasks_completed"));
                    member.setDonationCount(rs.getInt("donation_count"));

                    sect.getMembers().put(playerUuid, member);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e加载宗门成员数据失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    /**
     * 删除宗门数据
     */
    public void deleteSect(int sectId) {
        if (useMySql) {
            deleteSectFromDatabase(sectId);
        } else {
            deleteSectFromFile(sectId);
        }
    }

    /**
     * 从数据库删除宗门
     */
    private void deleteSectFromDatabase(int sectId) {
        String sql = "DELETE FROM xian_sects WHERE id = ?";

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sectId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库删除宗门数据失败: " + sectId);
            e.printStackTrace();
        }
    }

    /**
     * 从文件删除宗门
     */
    private void deleteSectFromFile(int sectId) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        File file = new File(sectDir, sectId + ".yml");

        if (file.exists()) {
            if (file.delete()) {
                plugin.getLogger().info("§a已删除宗门文件: " + sectId);
            } else {
                plugin.getLogger().warning("§e删除宗门文件失败: " + sectId);
            }
        }
    }

    // ==================== 天劫数据持久化 ====================

    /**
     * 保存天劫数据
     */
    public void saveTribulation(com.xiancore.systems.tribulation.Tribulation tribulation) {
        if (!useMySql) {
            return;
        }

        String sql = """
                INSERT INTO xian_tribulations (
                    tribulation_uuid, player_uuid, type, world_name, x, y, z,
                    current_wave, total_waves, active, completed, failed,
                    start_time, end_time, last_wave_time,
                    total_damage_dealt, total_damage_taken
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    current_wave = VALUES(current_wave),
                    active = VALUES(active),
                    completed = VALUES(completed),
                    failed = VALUES(failed),
                    end_time = VALUES(end_time),
                    last_wave_time = VALUES(last_wave_time),
                    total_damage_dealt = VALUES(total_damage_dealt),
                    total_damage_taken = VALUES(total_damage_taken)
                """;

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tribulation.getTribulationId().toString());
            pstmt.setString(2, tribulation.getPlayerId().toString());
            pstmt.setString(3, tribulation.getType().name());
            pstmt.setString(4, tribulation.getLocation().getWorld().getName());
            pstmt.setDouble(5, tribulation.getLocation().getX());
            pstmt.setDouble(6, tribulation.getLocation().getY());
            pstmt.setDouble(7, tribulation.getLocation().getZ());
            pstmt.setInt(8, tribulation.getCurrentWave());
            pstmt.setInt(9, tribulation.getTotalWaves());
            pstmt.setBoolean(10, tribulation.isActive());
            pstmt.setBoolean(11, tribulation.isCompleted());
            pstmt.setBoolean(12, tribulation.isFailed());
            pstmt.setLong(13, tribulation.getStartTime());
            pstmt.setLong(14, tribulation.getEndTime());
            pstmt.setLong(15, tribulation.getLastWaveTime());
            pstmt.setDouble(16, tribulation.getTotalDamage());
            pstmt.setDouble(17, 0.0); // total_damage_taken 暂时设为0

            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存天劫数据失败: " + tribulation.getTribulationId());
            e.printStackTrace();
        }
    }

    /**
     * 加载玩家的活跃天劫
     */
    public com.xiancore.systems.tribulation.Tribulation loadActiveTribulation(UUID playerId) {
        if (!useMySql) {
            return null;
        }

        String sql = "SELECT * FROM xian_tribulations WHERE player_uuid = ? AND active = TRUE LIMIT 1";

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerId.toString());

            try (var rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UUID tribulationId = UUID.fromString(rs.getString("tribulation_uuid"));
                    com.xiancore.systems.tribulation.TribulationType type =
                            com.xiancore.systems.tribulation.TribulationType.valueOf(rs.getString("type"));

                    String worldName = rs.getString("world_name");
                    org.bukkit.World world = plugin.getServer().getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("§e世界不存在，跳过加载天劫: " + worldName);
                        return null;
                    }

                    org.bukkit.Location location = new org.bukkit.Location(
                            world,
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z")
                    );

                    com.xiancore.systems.tribulation.Tribulation tribulation =
                            new com.xiancore.systems.tribulation.Tribulation(tribulationId, playerId, type, location);
                    tribulation.setCurrentWave(rs.getInt("current_wave"));
                    tribulation.setTotalWaves(rs.getInt("total_waves"));
                    tribulation.setActive(rs.getBoolean("active"));
                    tribulation.setCompleted(rs.getBoolean("completed"));
                    tribulation.setFailed(rs.getBoolean("failed"));
                    tribulation.setStartTime(rs.getLong("start_time"));
                    tribulation.setEndTime(rs.getLong("end_time"));
                    tribulation.setLastWaveTime(rs.getLong("last_wave_time"));
                    tribulation.setTotalDamage(rs.getDouble("total_damage_dealt"));

                    return tribulation;
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e加载天劫数据失败: " + playerId);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 删除天劫数据
     */
    public void deleteTribulation(UUID tribulationId) {
        if (!useMySql) {
            return;
        }

        String sql = "DELETE FROM xian_tribulations WHERE tribulation_uuid = ?";

        try (Connection conn = getConnection();
             var pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tribulationId.toString());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("§e删除天劫数据失败: " + tribulationId);
            e.printStackTrace();
        }
    }

    /**
     * 关闭数据管理器
     */
    public void shutdown() {
        // 保存所有数据
        saveAll();

        // 关闭数据库连接池
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("§a数据库连接池已关闭");
        }

        // 清空缓存
        playerDataCache.clear();
    }

    // ==================== 事务化保存方法（原子性保证） ====================

    /**
     * 原子性保存玩家数据和宗门数据
     * 使用数据库事务确保两个操作要么都成功，要么都失败
     * 这样可以避免数据不一致问题
     *
     * @param playerData 玩家数据
     * @param sect 宗门数据
     * @throws RuntimeException 如果保存失败，事务会回滚
     */
    public void savePlayerAndSectAtomic(PlayerData playerData, com.xiancore.systems.sect.Sect sect) {
        if (!useMySql) {
            // 如果不使用 MySQL，降级为普通保存
            savePlayerData(playerData);
            saveSect(sect);
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);  // 开启事务

            // 保存玩家数据（使用同一个连接）
            savePlayerDataWithConnection(conn, playerData);

            // 保存宗门数据（使用同一个连接）
            saveSectWithConnection(conn, sect);

            conn.commit();  // 提交事务
            plugin.getLogger().fine(String.format("§a事务提交成功: 玩家=%s, 宗门=%s",
                    playerData.getUuid(), sect.getName()));

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();  // 回滚事务
                    plugin.getLogger().warning(String.format(
                            "§c事务回滚: 玩家=%s, 宗门=%s, 原因=%s",
                            playerData.getUuid(), sect.getName(), e.getMessage()));
                } catch (SQLException ex) {
                    plugin.getLogger().severe("§c事务回滚失败!");
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("保存玩家和宗门数据失败", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);  // 恢复自动提交
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 使用指定的数据库连接保存玩家数据
     * 此方法用于事务化操作
     */
    private void savePlayerDataWithConnection(Connection conn, PlayerData data) throws SQLException {
        String sql = """
                INSERT INTO xian_players (
                    uuid, name, realm, realm_stage, qi, spiritual_root, spiritual_root_type, comprehension,
                    technique_adaptation, spirit_stones, contribution_points, skill_points,
                    player_level, sect_id, sect_rank, last_login, created_at, updated_at,
                    breakthrough_attempts, successful_breakthroughs, active_qi, last_fate_time, fate_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    realm = VALUES(realm),
                    realm_stage = VALUES(realm_stage),
                    qi = VALUES(qi),
                    spiritual_root = VALUES(spiritual_root),
                    spiritual_root_type = VALUES(spiritual_root_type),
                    comprehension = VALUES(comprehension),
                    technique_adaptation = VALUES(technique_adaptation),
                    spirit_stones = VALUES(spirit_stones),
                    contribution_points = VALUES(contribution_points),
                    skill_points = VALUES(skill_points),
                    player_level = VALUES(player_level),
                    sect_id = VALUES(sect_id),
                    sect_rank = VALUES(sect_rank),
                    last_login = VALUES(last_login),
                    updated_at = VALUES(updated_at),
                    breakthrough_attempts = VALUES(breakthrough_attempts),
                    successful_breakthroughs = VALUES(successful_breakthroughs),
                    active_qi = VALUES(active_qi),
                    last_fate_time = VALUES(last_fate_time),
                    fate_count = VALUES(fate_count)
                """;

        try (var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.getUuid().toString());
            pstmt.setString(2, data.getName());
            pstmt.setString(3, data.getRealm());
            pstmt.setInt(4, data.getRealmStage());
            pstmt.setLong(5, data.getQi());
            pstmt.setDouble(6, data.getSpiritualRoot());
            
            // 新增：灵根类型
            if (data.getSpiritualRootType() != null) {
                pstmt.setString(7, data.getSpiritualRootType().name());
            } else {
                pstmt.setNull(7, java.sql.Types.VARCHAR);
            }
            
            pstmt.setDouble(8, data.getComprehension());
            pstmt.setDouble(9, data.getTechniqueAdaptation());
            pstmt.setLong(10, data.getSpiritStones());
            pstmt.setInt(11, data.getContributionPoints());
            pstmt.setInt(12, data.getSkillPoints());
            pstmt.setInt(13, data.getPlayerLevel());

            if (data.getSectId() != null) {
                pstmt.setInt(14, data.getSectId());
            } else {
                pstmt.setNull(14, java.sql.Types.INTEGER);
            }

            pstmt.setString(15, data.getSectRank());
            pstmt.setLong(16, data.getLastLogin());
            pstmt.setLong(17, data.getCreatedAt());
            pstmt.setLong(18, System.currentTimeMillis());
            pstmt.setInt(19, data.getBreakthroughAttempts());
            pstmt.setInt(20, data.getSuccessfulBreakthroughs());
            pstmt.setLong(21, data.getActiveQi());
            pstmt.setLong(22, data.getLastFateTime());
            pstmt.setInt(23, data.getFateCount());

            pstmt.executeUpdate();
        }

        // 保存功法和装备数据（使用同一个连接）
        savePlayerSkillsWithConnection(conn, data);
        savePlayerEquipmentWithConnection(conn, data);
    }

    /**
     * 使用指定的数据库连接保存玩家功法数据
     */
    private void savePlayerSkillsWithConnection(Connection conn, PlayerData data) throws SQLException {
        String deleteSql = "DELETE FROM xian_player_skills WHERE player_uuid = ?";
        String insertSql = "INSERT INTO xian_player_skills (player_uuid, skill_id, skill_level) VALUES (?, ?, ?)";

        // 删除旧数据
        try (var pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, data.getUuid().toString());
            pstmt.executeUpdate();
        }

        // 插入新数据
        if (!data.getLearnedSkills().isEmpty()) {
            try (var pstmt = conn.prepareStatement(insertSql)) {
                for (var entry : data.getLearnedSkills().entrySet()) {
                    pstmt.setString(1, data.getUuid().toString());
                    pstmt.setString(2, entry.getKey());
                    pstmt.setInt(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }

    /**
     * 使用指定的数据库连接保存玩家装备数据
     */
    private void savePlayerEquipmentWithConnection(Connection conn, PlayerData data) throws SQLException {
        String deleteSql = "DELETE FROM xian_player_equipment WHERE player_uuid = ?";
        String insertSql = "INSERT INTO xian_player_equipment (player_uuid, slot, equipment_uuid) VALUES (?, ?, ?)";

        // 删除旧数据
        try (var pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setString(1, data.getUuid().toString());
            pstmt.executeUpdate();
        }

        // 插入新数据
        if (!data.getEquipment().isEmpty()) {
            try (var pstmt = conn.prepareStatement(insertSql)) {
                for (var entry : data.getEquipment().entrySet()) {
                    pstmt.setString(1, data.getUuid().toString());
                    pstmt.setString(2, entry.getKey());
                    pstmt.setString(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }

    /**
     * 使用指定的数据库连接保存宗门数据
     * 此方法用于事务化操作
     */
    private void saveSectWithConnection(Connection conn, com.xiancore.systems.sect.Sect sect) throws SQLException {
        String sql = """
                INSERT INTO xian_sects (
                    id, name, description, owner_uuid, owner_name, level, experience,
                    sect_funds, sect_contribution, max_members, recruiting, pvp_enabled,
                    announcement, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    description = VALUES(description),
                    owner_uuid = VALUES(owner_uuid),
                    owner_name = VALUES(owner_name),
                    level = VALUES(level),
                    experience = VALUES(experience),
                    sect_funds = VALUES(sect_funds),
                    sect_contribution = VALUES(sect_contribution),
                    max_members = VALUES(max_members),
                    recruiting = VALUES(recruiting),
                    pvp_enabled = VALUES(pvp_enabled),
                    announcement = VALUES(announcement),
                    updated_at = VALUES(updated_at)
                """;

        try (var pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sect.getId());
            pstmt.setString(2, sect.getName());
            pstmt.setString(3, sect.getDescription());
            pstmt.setString(4, sect.getOwnerId().toString());
            pstmt.setString(5, sect.getOwnerName());
            pstmt.setInt(6, sect.getLevel());
            pstmt.setLong(7, sect.getExperience());
            pstmt.setLong(8, sect.getSectFunds());
            pstmt.setInt(9, sect.getSectContribution());
            pstmt.setInt(10, sect.getMaxMembers());
            pstmt.setBoolean(11, sect.isRecruiting());
            pstmt.setBoolean(12, sect.isPvpEnabled());
            pstmt.setString(13, sect.getAnnouncement());
            pstmt.setLong(14, sect.getCreatedAt());
            pstmt.setLong(15, sect.getUpdatedAt());

            pstmt.executeUpdate();
        }

        // 保存成员数据（使用同一个连接）
        saveSectMembersWithConnection(conn, sect);
    }

    /**
     * 使用指定的数据库连接保存宗门成员数据
     */
    private void saveSectMembersWithConnection(Connection conn, com.xiancore.systems.sect.Sect sect) throws SQLException {
        String deleteSql = "DELETE FROM xian_sect_members WHERE sect_id = ?";
        String insertSql = """
                INSERT INTO xian_sect_members (
                    sect_id, player_uuid, player_name, rank, contribution,
                    weekly_contribution, joined_at, last_active_at,
                    tasks_completed, donation_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        // 删除旧数据
        try (var pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setInt(1, sect.getId());
            pstmt.executeUpdate();
        }

        // 插入新数据
        if (!sect.getMemberList().isEmpty()) {
            try (var pstmt = conn.prepareStatement(insertSql)) {
                for (var member : sect.getMemberList()) {
                    pstmt.setInt(1, sect.getId());
                    pstmt.setString(2, member.getPlayerId().toString());
                    pstmt.setString(3, member.getPlayerName());
                    pstmt.setString(4, member.getRank().name());
                    pstmt.setInt(5, member.getContribution());
                    pstmt.setInt(6, member.getWeeklyContribution());
                    pstmt.setLong(7, member.getJoinedAt());
                    pstmt.setLong(8, member.getLastActiveAt());
                    pstmt.setInt(9, member.getTasksCompleted());
                    pstmt.setInt(10, member.getDonationCount());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }
}
