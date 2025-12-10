package com.xiancore.core.data.repository;

import com.xiancore.XianCore;
import com.xiancore.core.data.DatabaseManager;
import com.xiancore.systems.tribulation.Tribulation;
import com.xiancore.systems.tribulation.TribulationType;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 天劫数据仓储
 * 负责天劫数据的加载、保存（仅支持 MySQL）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TribulationRepository {

    private final XianCore plugin;
    private final DatabaseManager databaseManager;

    // SQL 常量
    private static final String SQL_UPSERT = """
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

    private static final String SQL_SELECT_ACTIVE =
            "SELECT * FROM xian_tribulations WHERE player_uuid = ? AND active = TRUE LIMIT 1";

    private static final String SQL_DELETE =
            "DELETE FROM xian_tribulations WHERE tribulation_uuid = ?";

    public TribulationRepository(XianCore plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    // ==================== 公开 API ====================

    /**
     * 保存天劫数据
     *
     * @param tribulation 天劫
     */
    public void save(Tribulation tribulation) {
        if (!databaseManager.isUseMySql()) {
            return;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPSERT)) {

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
     *
     * @param playerId 玩家 UUID
     * @return 活跃的天劫，如果不存在返回 null
     */
    public Tribulation loadActive(UUID playerId) {
        if (!databaseManager.isUseMySql()) {
            return null;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_ACTIVE)) {

            pstmt.setString(1, playerId.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs, playerId);
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
     *
     * @param tribulationId 天劫 UUID
     */
    public void delete(UUID tribulationId) {
        if (!databaseManager.isUseMySql()) {
            return;
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {

            pstmt.setString(1, tribulationId.toString());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("§e删除天劫数据失败: " + tribulationId);
            e.printStackTrace();
        }
    }

    // ==================== 私有方法 ====================

    private Tribulation mapFromResultSet(ResultSet rs, UUID playerId) throws SQLException {
        UUID tribulationId = UUID.fromString(rs.getString("tribulation_uuid"));
        TribulationType type = TribulationType.valueOf(rs.getString("type"));

        String worldName = rs.getString("world_name");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("§e世界不存在，跳过加载天劫: " + worldName);
            return null;
        }

        Location location = new Location(
                world,
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z")
        );

        Tribulation tribulation = new Tribulation(tribulationId, playerId, type, location);
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
