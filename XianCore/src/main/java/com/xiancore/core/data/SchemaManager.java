package com.xiancore.core.data;

import com.xiancore.XianCore;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 数据库 Schema 管理器
 * 负责表结构创建和数据迁移
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SchemaManager {

    private final XianCore plugin;
    private final DatabaseManager databaseManager;

    public SchemaManager(XianCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * 初始化 Schema（创建表和执行迁移）
     */
    public void initialize() {
        if (!databaseManager.isUseMySql()) {
            return;
        }

        createTables();
        runMigrations();
    }

    /**
     * 创建数据表
     */
    private void createTables() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // 玩家数据表
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                    """);

            // 玩家功法表
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_player_skills (
                        player_uuid VARCHAR(36) NOT NULL,
                        skill_id VARCHAR(64) NOT NULL,
                        skill_level INT DEFAULT 1,
                        PRIMARY KEY (player_uuid, skill_id),
                        FOREIGN KEY (player_uuid) REFERENCES xian_players(uuid) ON DELETE CASCADE,
                        INDEX idx_player (player_uuid)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                    """);

            // 宗门数据表
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                    """);

            // 宗门成员表
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_sect_members (
                        sect_id INT NOT NULL,
                        player_uuid VARCHAR(36) NOT NULL,
                        player_name VARCHAR(16) NOT NULL,
                        `rank` VARCHAR(32) DEFAULT 'OUTER_DISCIPLE',
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                    """);

            // 玩家技能绑定表（新增）
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_player_skill_binds (
                        player_uuid VARCHAR(36) NOT NULL,
                        slot INT NOT NULL,
                        skill_id VARCHAR(64) NOT NULL,
                        PRIMARY KEY (player_uuid, slot),
                        FOREIGN KEY (player_uuid) REFERENCES xian_players(uuid) ON DELETE CASCADE,
                        INDEX idx_player (player_uuid)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                    """);

            // 宗门设施表（新增）
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_sect_facilities (
                        sect_id INT NOT NULL,
                        facility_type VARCHAR(32) NOT NULL,
                        level INT DEFAULT 1,
                        upgraded_at BIGINT,
                        PRIMARY KEY (sect_id, facility_type),
                        FOREIGN KEY (sect_id) REFERENCES xian_sects(id) ON DELETE CASCADE,
                        INDEX idx_sect (sect_id),
                        INDEX idx_type_level (facility_type, level)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                    """);

            // 宗门仓库表（新增）
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS xian_sect_warehouses (
                        sect_id INT PRIMARY KEY,
                        capacity INT DEFAULT 54,
                        items_json LONGTEXT,
                        last_modified BIGINT,
                        FOREIGN KEY (sect_id) REFERENCES xian_sects(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8
                    """);

            plugin.getLogger().info("§a✓ 数据表创建/检查完成（包含3张新表）");

        } catch (SQLException e) {
            plugin.getLogger().severe("§c✗ 创建数据表失败!");
            e.printStackTrace();
        }
    }

    /**
     * 执行数据库迁移
     */
    private void runMigrations() {
        migrateAddSpiritualRootType();
        migrateAddResidenceLandFields();
    }

    /**
     * 迁移：添加 spiritual_root_type 字段
     */
    private void migrateAddSpiritualRootType() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查字段是否已存在
            boolean fieldExists = false;
            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM xian_players LIKE 'spiritual_root_type'")) {
                fieldExists = rs.next();
            }

            if (!fieldExists) {
                stmt.execute("ALTER TABLE xian_players ADD COLUMN spiritual_root_type VARCHAR(64) DEFAULT NULL AFTER spiritual_root");
                plugin.getLogger().info("§a✓ 数据库迁移：已添加 spiritual_root_type 字段");

                // 为现有玩家生成灵根类型
                try (var updateStmt = conn.createStatement();
                     var rs = stmt.executeQuery("SELECT uuid, spiritual_root FROM xian_players WHERE spiritual_root_type IS NULL")) {

                    int updatedCount = 0;
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");
                        double spiritualRoot = rs.getDouble("spiritual_root");

                        SpiritualRootType rootType = SpiritualRootType.fromValue(spiritualRoot);

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
     * 迁移：添加宗门领地相关字段
     */
    private void migrateAddResidenceLandFields() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查 residence_land_id 字段是否已存在
            boolean fieldExists = false;
            try (var rs = stmt.executeQuery("SHOW COLUMNS FROM xian_sects LIKE 'residence_land_id'")) {
                fieldExists = rs.next();
            }

            if (!fieldExists) {
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN residence_land_id VARCHAR(128) DEFAULT NULL");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_world VARCHAR(64) DEFAULT NULL");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_x DOUBLE DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_y DOUBLE DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN land_center_z DOUBLE DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN last_maintenance_time BIGINT DEFAULT 0");
                stmt.execute("ALTER TABLE xian_sects ADD COLUMN building_slots_data LONGTEXT");

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
}
