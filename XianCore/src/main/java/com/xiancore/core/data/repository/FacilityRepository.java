package com.xiancore.core.data.repository;

import com.xiancore.XianCore;
import com.xiancore.core.data.DatabaseManager;
import com.xiancore.systems.sect.facilities.SectFacilityData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 设施数据仓储
 * 负责设施数据的加载、保存（支持 YAML/MySQL 双模式）
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class FacilityRepository {

    private final XianCore plugin;
    private final DatabaseManager databaseManager;

    // 重试配置
    private static final int RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 100;

    // SQL 常量
    private static final String SQL_SELECT =
            "SELECT facility_type, level, upgraded_at FROM xian_sect_facilities WHERE sect_id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM xian_sect_facilities WHERE sect_id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO xian_sect_facilities (sect_id, facility_type, level, upgraded_at) VALUES (?, ?, ?, ?)";

    public FacilityRepository(XianCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDataManager().getDatabaseManager();
    }

    // ==================== 公开 API ====================

    /**
     * 保存设施数据
     *
     * @param data 设施数据
     */
    public void save(SectFacilityData data) {
        if (databaseManager.isUseMySql()) {
            saveToDatabase(data);
        } else {
            saveToFile(data);
        }
    }

    /**
     * 加载设施数据
     *
     * @param sectId 宗门 ID
     * @return 设施数据，如果不存在返回 null
     */
    public SectFacilityData load(int sectId) {
        return databaseManager.isUseMySql()
                ? loadFromDatabase(sectId)
                : loadFromFile(sectId);
    }

    // ==================== MySQL 实现 ====================

    private SectFacilityData loadFromDatabase(int sectId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT)) {

            pstmt.setInt(1, sectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                SectFacilityData data = new SectFacilityData(sectId);

                while (rs.next()) {
                    String facilityType = rs.getString("facility_type");
                    int level = rs.getInt("level");
                    long upgradedAt = rs.getLong("upgraded_at");

                    data.getFacilityLevels().put(facilityType, level);
                }

                // 如果没有数据，返回 null
                return data.getFacilityLevels().isEmpty() ? null : data;
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库加载设施数据失败: 宗门ID=" + sectId);
            e.printStackTrace();
            return null;
        }
    }

    private void saveToDatabase(SectFacilityData data) {
        try (Connection conn = databaseManager.getConnection()) {
            // 删除旧数据
            try (PreparedStatement pstmt = conn.prepareStatement(SQL_DELETE)) {
                pstmt.setInt(1, data.getSectId());
                pstmt.executeUpdate();
            }

            // 插入新数据
            if (!data.getFacilityLevels().isEmpty()) {
                try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT)) {
                    for (var entry : data.getFacilityLevels().entrySet()) {
                        pstmt.setInt(1, data.getSectId());
                        pstmt.setString(2, entry.getKey());
                        pstmt.setInt(3, entry.getValue());
                        pstmt.setLong(4, data.getLastUpdated());
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存设施数据到数据库失败: 宗门ID=" + data.getSectId());
            e.printStackTrace();
        }
    }

    // ==================== YAML 实现 ====================

    private SectFacilityData loadFromFile(int sectId) {
        File file = new File(plugin.getDataFolder(), "sects/" + sectId + "_facilities.yml");

        if (!file.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            SectFacilityData data = new SectFacilityData(sectId);
            data.setLastUpdated(config.getLong("last_updated", System.currentTimeMillis()));

            // 加载设施等级
            if (config.contains("facility_levels")) {
                ConfigurationSection levelsSection = config.getConfigurationSection("facility_levels");

                if (levelsSection != null) {
                    for (String facilityId : levelsSection.getKeys(false)) {
                        int level = config.getInt("facility_levels." + facilityId, 0);
                        data.getFacilityLevels().put(facilityId, level);
                    }
                }
            }

            return data;

        } catch (Exception e) {
            plugin.getLogger().warning("§e从文件加载设施数据失败: 宗门ID=" + sectId);
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFile(SectFacilityData data) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            sectDir.mkdirs();
        }

        File file = new File(sectDir, data.getSectId() + "_facilities.yml");

        // 重试机制
        Exception lastException = null;

        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                YamlConfiguration config = new YamlConfiguration();

                config.set("sect_id", data.getSectId());
                config.set("last_updated", data.getLastUpdated());

                // 保存设施等级
                for (var entry : data.getFacilityLevels().entrySet()) {
                    config.set("facility_levels." + entry.getKey(), entry.getValue());
                }

                config.save(file);
                return; // 成功，退出

            } catch (Exception e) {
                lastException = e;
                plugin.getLogger().warning("§e保存设施数据失败，重试 " + (i + 1) + "/" + RETRY_COUNT +
                        ": 宗门ID=" + data.getSectId());

                // 等待后重试
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // 所有重试都失败
        plugin.getLogger().severe("§c保存设施数据彻底失败: 宗门ID=" + data.getSectId());
        if (lastException != null) {
            lastException.printStackTrace();
        }
    }
}
