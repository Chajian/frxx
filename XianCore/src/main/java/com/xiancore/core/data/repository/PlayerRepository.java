package com.xiancore.core.data.repository;

import com.xiancore.XianCore;
import com.xiancore.core.data.DatabaseManager;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.data.SpiritualRootType;
import com.xiancore.core.data.mapper.PlayerDataMapper;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家数据仓储
 * 负责玩家数据的加载、保存、缓存管理
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class PlayerRepository {

    private final XianCore plugin;
    private final DatabaseManager databaseManager;
    private final PlayerDataMapper mapper;

    // 缓存
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    // SQL 常量
    private static final String SQL_SELECT_BY_UUID =
            "SELECT * FROM xian_players WHERE uuid = ?";

    private static final String SQL_UPSERT = """
            INSERT INTO xian_players (
                uuid, name, realm, realm_stage, qi, spiritual_root, spiritual_root_type,
                comprehension, technique_adaptation, spirit_stones, contribution_points,
                skill_points, player_level, sect_id, sect_rank, last_login, created_at,
                updated_at, breakthrough_attempts, successful_breakthroughs, active_qi,
                last_fate_time, fate_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name), realm = VALUES(realm), realm_stage = VALUES(realm_stage),
                qi = VALUES(qi), spiritual_root = VALUES(spiritual_root),
                spiritual_root_type = VALUES(spiritual_root_type),
                comprehension = VALUES(comprehension),
                technique_adaptation = VALUES(technique_adaptation),
                spirit_stones = VALUES(spirit_stones),
                contribution_points = VALUES(contribution_points),
                skill_points = VALUES(skill_points), player_level = VALUES(player_level),
                sect_id = VALUES(sect_id), sect_rank = VALUES(sect_rank),
                last_login = VALUES(last_login), updated_at = VALUES(updated_at),
                breakthrough_attempts = VALUES(breakthrough_attempts),
                successful_breakthroughs = VALUES(successful_breakthroughs),
                active_qi = VALUES(active_qi), last_fate_time = VALUES(last_fate_time),
                fate_count = VALUES(fate_count)
            """;

    private static final String SQL_SELECT_SKILLS =
            "SELECT skill_id, skill_level FROM xian_player_skills WHERE player_uuid = ?";

    private static final String SQL_DELETE_SKILLS =
            "DELETE FROM xian_player_skills WHERE player_uuid = ?";

    private static final String SQL_INSERT_SKILL =
            "INSERT INTO xian_player_skills (player_uuid, skill_id, skill_level) VALUES (?, ?, ?)";

    private static final String SQL_SELECT_EQUIPMENT =
            "SELECT slot, equipment_uuid FROM xian_player_equipment WHERE player_uuid = ?";

    private static final String SQL_DELETE_EQUIPMENT =
            "DELETE FROM xian_player_equipment WHERE player_uuid = ?";

    private static final String SQL_INSERT_EQUIPMENT =
            "INSERT INTO xian_player_equipment (player_uuid, slot, equipment_uuid) VALUES (?, ?, ?)";

    public PlayerRepository(XianCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.mapper = new PlayerDataMapper();
    }

    // ==================== 公开 API ====================

    /**
     * 加载玩家数据（带缓存）
     *
     * @param uuid 玩家 UUID
     * @return 玩家数据，如果不存在返回 null
     */
    public PlayerData load(UUID uuid) {
        // 1. 先查缓存
        PlayerData cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        // 2. 从存储加载
        PlayerData data = databaseManager.isUseMySql()
                ? loadFromDatabase(uuid)
                : loadFromFile(uuid);

        // 3. 放入缓存
        if (data != null) {
            cache.put(uuid, data);
        }

        return data;
    }

    /**
     * 保存玩家数据
     *
     * @param data 玩家数据
     */
    public void save(PlayerData data) {
        // 更新缓存
        cache.put(data.getUuid(), data);

        // 持久化
        if (databaseManager.isUseMySql()) {
            saveToDatabase(data);
        } else {
            saveToFile(data);
        }
    }

    /**
     * 事务化保存（供 TransactionManager 调用）
     *
     * @param conn 数据库连接
     * @param data 玩家数据
     * @throws SQLException SQL 异常
     */
    public void saveWithConnection(Connection conn, PlayerData data) throws SQLException {
        // 更新缓存
        cache.put(data.getUuid(), data);

        // 保存主数据
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPSERT)) {
            mapper.bindForSave(pstmt, data);
            pstmt.executeUpdate();
        }

        // 保存关联数据
        saveSkillsWithConnection(conn, data);
        saveEquipmentWithConnection(conn, data);
    }

    /**
     * 创建新玩家数据
     *
     * @param uuid 玩家 UUID
     * @return 新创建的玩家数据
     */
    public PlayerData create(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        data.setCreatedAt(System.currentTimeMillis());
        data.setLastLogin(System.currentTimeMillis());

        // 随机生成灵根
        SpiritualRootType rootType = SpiritualRootType.randomGenerate();
        data.setSpiritualRootType(rootType);
        data.setSpiritualRoot(rootType.generateValue());

        plugin.getLogger().info("新玩家 " + uuid + " 获得灵根: " +
                rootType.getFullName() + " (" + String.format("%.3f", data.getSpiritualRoot()) + ")");

        // 缓存并保存
        cache.put(uuid, data);
        save(data);

        return data;
    }

    /**
     * 移除缓存
     *
     * @param uuid 玩家 UUID
     */
    public void evict(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * 保存所有缓存数据
     */
    public void saveAll() {
        plugin.getLogger().info(String.format("正在保存 %d 个玩家的数据...", cache.size()));
        for (PlayerData data : cache.values()) {
            save(data);
        }
        plugin.getLogger().info("§a所有玩家数据已保存!");
    }

    /**
     * 获取缓存的玩家数据（不触发加载）
     *
     * @param uuid 玩家 UUID
     * @return 缓存的玩家数据
     */
    public PlayerData getCached(UUID uuid) {
        return cache.get(uuid);
    }

    /**
     * 获取所有缓存的玩家数据
     *
     * @return 缓存的玩家数据集合
     */
    public Collection<PlayerData> getAllCached() {
        return cache.values();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * 获取缓存 Map（用于兼容）
     *
     * @return 缓存 Map
     */
    public Map<UUID, PlayerData> getCache() {
        return cache;
    }

    // ==================== 私有方法：MySQL ====================

    private PlayerData loadFromDatabase(UUID uuid) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_BY_UUID)) {

            pstmt.setString(1, uuid.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    PlayerData data = mapper.mapFromResultSet(rs, uuid);
                    loadSkillsFromDatabase(uuid, data);
                    loadEquipmentFromDatabase(uuid, data);
                    return data;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库加载玩家数据失败: " + uuid);
            e.printStackTrace();
        }
        return null;
    }

    private void saveToDatabase(PlayerData data) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPSERT)) {

            mapper.bindForSave(pstmt, data);
            pstmt.executeUpdate();

            saveSkillsToDatabase(data);
            saveEquipmentToDatabase(data);

        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存玩家数据到数据库失败: " + data.getUuid());
            e.printStackTrace();
        }
    }

    private void loadSkillsFromDatabase(UUID uuid, PlayerData data) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_SKILLS)) {

            pstmt.setString(1, uuid.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
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

    private void loadEquipmentFromDatabase(UUID uuid, PlayerData data) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_EQUIPMENT)) {

            pstmt.setString(1, uuid.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
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

    private void saveSkillsToDatabase(PlayerData data) {
        try (Connection conn = databaseManager.getConnection()) {
            // 删除旧数据
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_SKILLS)) {
                pstmt.setString(1, data.getUuid().toString());
                pstmt.executeUpdate();
            }

            // 插入新数据
            if (!data.getLearnedSkills().isEmpty()) {
                try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_SKILL)) {
                    for (var entry : data.getLearnedSkills().entrySet()) {
                        pstmt.setString(1, data.getUuid().toString());
                        pstmt.setString(2, entry.getKey());
                        pstmt.setInt(3, entry.getValue());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存玩家功法数据失败: " + data.getUuid());
            e.printStackTrace();
        }
    }

    private void saveEquipmentToDatabase(PlayerData data) {
        try (Connection conn = databaseManager.getConnection()) {
            // 删除旧数据
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_EQUIPMENT)) {
                pstmt.setString(1, data.getUuid().toString());
                pstmt.executeUpdate();
            }

            // 插入新数据
            if (!data.getEquipment().isEmpty()) {
                try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_EQUIPMENT)) {
                    for (var entry : data.getEquipment().entrySet()) {
                        pstmt.setString(1, data.getUuid().toString());
                        pstmt.setString(2, entry.getKey());
                        pstmt.setString(3, entry.getValue());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存玩家装备数据失败: " + data.getUuid());
            e.printStackTrace();
        }
    }

    private void saveSkillsWithConnection(Connection conn, PlayerData data) throws SQLException {
        // 删除旧数据
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_SKILLS)) {
            pstmt.setString(1, data.getUuid().toString());
            pstmt.executeUpdate();
        }

        // 插入新数据
        if (!data.getLearnedSkills().isEmpty()) {
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_SKILL)) {
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

    private void saveEquipmentWithConnection(Connection conn, PlayerData data) throws SQLException {
        // 删除旧数据
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_EQUIPMENT)) {
            pstmt.setString(1, data.getUuid().toString());
            pstmt.executeUpdate();
        }

        // 插入新数据
        if (!data.getEquipment().isEmpty()) {
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_EQUIPMENT)) {
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

    // ==================== 私有方法：File ====================

    private PlayerData loadFromFile(UUID uuid) {
        File file = new File(plugin.getDataFolder(), "players/" + uuid + ".yml");
        if (!file.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            return mapper.mapFromYaml(config, uuid);
        } catch (Exception e) {
            plugin.getLogger().warning("§e从文件加载玩家数据失败: " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFile(PlayerData data) {
        File playerDir = new File(plugin.getDataFolder(), "players");
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }

        File file = new File(playerDir, data.getUuid() + ".yml");

        try {
            YamlConfiguration config = new YamlConfiguration();
            mapper.mapToYaml(config, data);
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("§e保存玩家数据到文件失败: " + data.getUuid());
            e.printStackTrace();
        }
    }
}
