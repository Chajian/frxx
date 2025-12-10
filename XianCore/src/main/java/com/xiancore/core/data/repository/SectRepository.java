package com.xiancore.core.data.repository;

import com.xiancore.XianCore;
import com.xiancore.core.data.DatabaseManager;
import com.xiancore.core.data.mapper.SectDataMapper;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 宗门数据仓储
 * 负责宗门数据的加载、保存
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectRepository {

    private final XianCore plugin;
    private final DatabaseManager databaseManager;
    private final SectDataMapper mapper;

    // SQL 常量
    private static final String SQL_SELECT_ALL = "SELECT * FROM xian_sects";

    private static final String SQL_UPSERT = """
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

    private static final String SQL_UPSERT_SIMPLE = """
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

    private static final String SQL_DELETE = "DELETE FROM xian_sects WHERE id = ?";

    private static final String SQL_SELECT_MEMBERS = "SELECT * FROM xian_sect_members WHERE sect_id = ?";

    private static final String SQL_DELETE_MEMBERS = "DELETE FROM xian_sect_members WHERE sect_id = ?";

    private static final String SQL_INSERT_MEMBER = """
            INSERT INTO xian_sect_members (
                sect_id, player_uuid, player_name, `rank`, contribution,
                weekly_contribution, joined_at, last_active_at,
                tasks_completed, donation_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    public SectRepository(XianCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.mapper = new SectDataMapper();
    }

    // ==================== 公开 API ====================

    /**
     * 加载所有宗门数据
     *
     * @return 宗门列表
     */
    public List<Sect> loadAll() {
        if (databaseManager.isUseMySql()) {
            return loadAllFromDatabase();
        } else {
            return loadAllFromFile();
        }
    }

    /**
     * 保存宗门数据
     *
     * @param sect 宗门
     */
    public void save(Sect sect) {
        if (databaseManager.isUseMySql()) {
            saveToDatabase(sect);
        } else {
            saveToFile(sect);
        }
    }

    /**
     * 事务化保存（供 TransactionManager 调用）
     *
     * @param conn 数据库连接
     * @param sect 宗门数据
     * @throws SQLException SQL 异常
     */
    public void saveWithConnection(Connection conn, Sect sect) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPSERT_SIMPLE)) {
            mapper.bindForSaveSimple(pstmt, sect);
            pstmt.executeUpdate();
        }

        // 保存成员数据
        saveMembersWithConnection(conn, sect);
    }

    /**
     * 删除宗门数据
     *
     * @param sectId 宗门 ID
     */
    public void delete(int sectId) {
        if (databaseManager.isUseMySql()) {
            deleteFromDatabase(sectId);
        } else {
            deleteFromFile(sectId);
        }
    }

    // ==================== 私有方法：MySQL ====================

    private List<Sect> loadAllFromDatabase() {
        List<Sect> sects = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_SELECT_ALL)) {

            while (rs.next()) {
                Sect sect = mapper.mapFromResultSet(rs);
                loadMembersFromDatabase(sect);
                sects.add(sect);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库加载宗门数据失败");
            e.printStackTrace();
        }

        return sects;
    }

    private void saveToDatabase(Sect sect) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPSERT)) {

            mapper.bindForSave(pstmt, sect);
            pstmt.executeUpdate();

            // 保存成员数据
            saveMembersToDatabase(sect);

        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存宗门数据到数据库失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    private void deleteFromDatabase(int sectId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setInt(1, sectId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库删除宗门数据失败: " + sectId);
            e.printStackTrace();
        }
    }

    private void loadMembersFromDatabase(Sect sect) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_MEMBERS)) {

            pstmt.setInt(1, sect.getId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SectMember member = mapper.mapMemberFromResultSet(rs);
                    sect.getMembers().put(member.getPlayerId(), member);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e加载宗门成员数据失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    private void saveMembersToDatabase(Sect sect) {
        try (Connection conn = databaseManager.getConnection()) {
            // 删除旧数据
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_MEMBERS)) {
                pstmt.setInt(1, sect.getId());
                pstmt.executeUpdate();
            }

            // 插入新数据
            if (!sect.getMemberList().isEmpty()) {
                try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_MEMBER)) {
                    for (SectMember member : sect.getMemberList()) {
                        bindMemberForSave(pstmt, sect.getId(), member);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存宗门成员数据失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    private void saveMembersWithConnection(Connection conn, Sect sect) throws SQLException {
        // 删除旧数据
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE_MEMBERS)) {
            pstmt.setInt(1, sect.getId());
            pstmt.executeUpdate();
        }

        // 插入新数据
        if (!sect.getMemberList().isEmpty()) {
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_MEMBER)) {
                for (SectMember member : sect.getMemberList()) {
                    bindMemberForSave(pstmt, sect.getId(), member);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }

    private void bindMemberForSave(PreparedStatement pstmt, int sectId, SectMember member) throws SQLException {
        pstmt.setInt(1, sectId);
        pstmt.setString(2, member.getPlayerId().toString());
        pstmt.setString(3, member.getPlayerName());
        pstmt.setString(4, member.getRank().name());
        pstmt.setInt(5, member.getContribution());
        pstmt.setInt(6, member.getWeeklyContribution());
        pstmt.setLong(7, member.getJoinedAt());
        pstmt.setLong(8, member.getLastActiveAt());
        pstmt.setInt(9, member.getTasksCompleted());
        pstmt.setInt(10, member.getDonationCount());
    }

    // ==================== 私有方法：File ====================

    private List<Sect> loadAllFromFile() {
        List<Sect> sects = new ArrayList<>();

        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            return sects;
        }

        File[] sectFiles = sectDir.listFiles((dir, name) ->
                name.endsWith(".yml") &&
                        !name.contains("_warehouse") &&
                        !name.contains("_facilities"));

        if (sectFiles == null) {
            return sects;
        }

        for (File file : sectFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Sect sect = mapper.mapFromYaml(config);
                sects.add(sect);
            } catch (Exception e) {
                plugin.getLogger().warning("§e从文件加载宗门数据失败: " + file.getName());
                e.printStackTrace();
            }
        }

        return sects;
    }

    private void saveToFile(Sect sect) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            sectDir.mkdirs();
        }

        File file = new File(sectDir, sect.getId() + ".yml");

        try {
            YamlConfiguration config = new YamlConfiguration();
            mapper.mapToYaml(config, sect);
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("§e保存宗门数据到文件失败: " + sect.getId());
            e.printStackTrace();
        }
    }

    private void deleteFromFile(int sectId) {
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
}
