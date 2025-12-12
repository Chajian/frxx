package com.xiancore.core.data.migrate.migrators;

import com.xiancore.XianCore;
import com.xiancore.core.data.migrate.MigrationReport;
import com.xiancore.core.data.migrate.base.AbstractMigrator;
import com.xiancore.core.data.repository.FacilityRepository;
import com.xiancore.systems.sect.facilities.SectFacilityData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 宗门设施数据迁移器
 * 从 YML 文件迁移宗门设施数据到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class FacilityDataMigrator extends AbstractMigrator {

    private final File sectsFolder;
    private final FacilityRepository facilityRepository;

    public FacilityDataMigrator(XianCore plugin) {
        super(plugin);
        this.sectsFolder = new File(plugin.getDataFolder(), "sects");
        this.facilityRepository = new FacilityRepository(plugin);
    }

    @Override
    public String getName() {
        return "宗门设施迁移器";
    }

    @Override
    public String getDescription() {
        return "迁移宗门设施建筑等级数据";
    }

    @Override
    public boolean hasDataToMigrate() {
        if (!sectsFolder.exists() || !sectsFolder.isDirectory()) {
            return false;
        }
        // 匹配 *_facilities.yml 文件
        File[] files = sectsFolder.listFiles((dir, name) -> name.endsWith("_facilities.yml"));
        return files != null && files.length > 0;
    }

    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();

        // 匹配 *_facilities.yml 文件
        File[] ymlFiles = sectsFolder.listFiles((dir, name) -> name.endsWith("_facilities.yml"));
        int fileCount = ymlFiles != null ? ymlFiles.length : 0;
        long totalSize = calculateFacilityFilesSize();

        sb.append("§e设施文件数量: §f").append(fileCount).append(" 个\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());

        return sb.toString();
    }

    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();

        plugin.getLogger().info("§e开始迁移宗门设施数据...");
        plugin.getLogger().info("§e模式: " + (dryRun ? "§6预览模式" : "§c真实迁移"));

        // 检查MySQL
        if (!isMySqlAvailable()) {
            plugin.getLogger().severe("§cMySQL未连接！");
            return report;
        }

        // 检查文件夹
        if (!sectsFolder.exists()) {
            plugin.getLogger().warning("§c未找到 sects 目录");
            return report;
        }

        // 获取所有设施文件
        File[] facilityFiles = sectsFolder.listFiles((dir, name) -> name.endsWith("_facilities.yml"));
        if (facilityFiles == null || facilityFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何设施YML文件");
            return report;
        }

        report.setTotalFiles(facilityFiles.length);
        plugin.getLogger().info("§a找到 " + facilityFiles.length + " 个设施文件");

        // 开始迁移
        for (File facilityFile : facilityFiles) {
            try {
                String fileName = facilityFile.getName();
                // 从文件名提取宗门ID (如: 1_facilities.yml -> 1)
                String sectIdStr = fileName.replace("_facilities.yml", "");
                int sectId = Integer.parseInt(sectIdStr);

                // 检查是否已存在
                if (!dryRun && existsInDatabase(sectId)) {
                    plugin.getLogger().info("§7跳过: " + fileName + " (已存在)");
                    report.recordSkipped();
                    continue;
                }

                // 加载设施数据
                SectFacilityData data = loadFacilityDataFromYml(facilityFile, sectId);
                if (data == null) {
                    plugin.getLogger().warning("§c无法加载: " + fileName);
                    report.recordFailure(fileName, fileName, "无法加载YML数据");
                    continue;
                }

                // 写入数据库
                if (!dryRun) {
                    facilityRepository.save(data);
                }

                plugin.getLogger().info("§a迁移设施数据: 宗门ID=" + sectId);
                report.addDataSize(facilityFile.length());
                report.recordSuccess();

            } catch (Exception e) {
                String fileName = facilityFile.getName();
                plugin.getLogger().warning("§c失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }

        report.complete();
        plugin.getLogger().info("§a宗门设施数据迁移完成！");

        return report;
    }

    @Override
    protected long estimateTimeInMillis() {
        File[] files = sectsFolder.listFiles((dir, name) -> name.endsWith("_facilities.yml"));
        int fileCount = files != null ? files.length : 0;
        return fileCount * 20L; // 假设每个文件20ms
    }

    /**
     * 从YML加载设施数据
     */
    private SectFacilityData loadFacilityDataFromYml(File file, int sectId) {
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
            plugin.getLogger().severe("§c加载失败: " + file.getName() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查设施数据是否已存在
     */
    private boolean existsInDatabase(int sectId) {
        String sql = "SELECT COUNT(*) FROM xian_sect_facilities WHERE sect_id = ?";

        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e检查数据库失败: " + e.getMessage());
        }

        return false;
    }

    /**
     * 计算设施文件总大小
     */
    private long calculateFacilityFilesSize() {
        if (!sectsFolder.exists() || !sectsFolder.isDirectory()) {
            return 0;
        }

        File[] files = sectsFolder.listFiles((dir, name) -> name.endsWith("_facilities.yml"));
        if (files == null) {
            return 0;
        }

        long total = 0;
        for (File file : files) {
            total += file.length();
        }
        return total;
    }
}
