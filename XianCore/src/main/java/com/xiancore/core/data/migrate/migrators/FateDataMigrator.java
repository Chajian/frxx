package com.xiancore.core.data.migrate.migrators;

import com.xiancore.XianCore;
import com.xiancore.core.data.migrate.MigrationReport;
import com.xiancore.core.data.migrate.base.AbstractMigrator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 奇遇数据迁移器
 * 从 YML 文件迁移玩家奇遇记录到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class FateDataMigrator extends AbstractMigrator {
    
    private final File fateFolder;
    
    public FateDataMigrator(XianCore plugin) {
        super(plugin);
        this.fateFolder = new File(plugin.getDataFolder(), "fate");
    }
    
    @Override
    public String getName() {
        return "奇遇数据迁移器";
    }
    
    @Override
    public String getDescription() {
        return "迁移玩家奇遇记录、触发次数、奖励历史等";
    }
    
    @Override
    public boolean hasDataToMigrate() {
        if (!fateFolder.exists() || !fateFolder.isDirectory()) {
            return false;
        }
        File[] files = fateFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files != null && files.length > 0;
    }
    
    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();
        
        File[] ymlFiles = fateFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = ymlFiles != null ? ymlFiles.length : 0;
        long totalSize = calculateTotalSize(fateFolder, ".yml");
        
        sb.append("§e奇遇记录数: §f").append(fileCount).append(" 个\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());
        
        return sb.toString();
    }
    
    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();
        
        plugin.getLogger().info("§e开始迁移奇遇数据...");
        plugin.getLogger().info("§e模式: " + (dryRun ? "§6预览模式" : "§c真实迁移"));
        
        // 检查MySQL
        if (!isMySqlAvailable()) {
            plugin.getLogger().severe("§cMySQL未连接！");
            return report;
        }
        
        // 检查文件夹
        if (!fateFolder.exists()) {
            plugin.getLogger().warning("§c未找到 fate 目录");
            return report;
        }
        
        // 获取所有YML文件
        File[] fateFiles = fateFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (fateFiles == null || fateFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何奇遇YML文件");
            return report;
        }
        
        report.setTotalFiles(fateFiles.length);
        plugin.getLogger().info("§a找到 " + fateFiles.length + " 个奇遇记录");
        
        // 开始迁移
        for (File fateFile : fateFiles) {
            try {
                String fileName = fateFile.getName();
                String uuidStr = fileName.replace(".yml", "");
                UUID playerUuid = UUID.fromString(uuidStr);
                
                // 加载奇遇数据
                YamlConfiguration config = YamlConfiguration.loadConfiguration(fateFile);
                
                // 检查是否已存在
                if (!dryRun && fateExistsInDatabase(playerUuid)) {
                    plugin.getLogger().info("§7跳过: " + fileName + " (已存在)");
                    report.recordSkipped();
                    continue;
                }
                
                // 写入数据库
                if (!dryRun) {
                    saveFateToDatabase(playerUuid, config);
                }
                
                plugin.getLogger().info("§a迁移奇遇记录: " + playerUuid);
                report.addDataSize(fateFile.length());
                report.recordSuccess();
                
            } catch (Exception e) {
                String fileName = fateFile.getName();
                plugin.getLogger().warning("§c失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }
        
        report.complete();
        plugin.getLogger().info("§a奇遇数据迁移完成！");
        
        return report;
    }
    
    @Override
    protected long estimateTimeInMillis() {
        File[] files = fateFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = files != null ? files.length : 0;
        return fileCount * 20L; // 每个记录约20ms
    }
    
    /**
     * 保存奇遇数据到数据库
     */
    private void saveFateToDatabase(UUID playerUuid, YamlConfiguration config) throws SQLException {
        String sql = "INSERT INTO xian_fate_records " +
                    "(player_uuid, total_encounters, last_encounter_time, " +
                    "fate_type, fate_reward, luck_value, " +
                    "completed_fates, rare_encounter_count, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "total_encounters = VALUES(total_encounters), " +
                    "last_encounter_time = VALUES(last_encounter_time), " +
                    "fate_type = VALUES(fate_type), " +
                    "fate_reward = VALUES(fate_reward), " +
                    "luck_value = VALUES(luck_value), " +
                    "completed_fates = VALUES(completed_fates), " +
                    "rare_encounter_count = VALUES(rare_encounter_count), " +
                    "updated_at = VALUES(updated_at)";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, playerUuid.toString());
            pstmt.setInt(2, config.getInt("total_encounters", 0));
            pstmt.setLong(3, config.getLong("last_encounter_time", 0));
            pstmt.setString(4, config.getString("fate_type", "NORMAL"));
            pstmt.setString(5, config.getString("fate_reward", ""));
            pstmt.setDouble(6, config.getDouble("luck_value", 1.0));
            pstmt.setInt(7, config.getInt("completed_fates", 0));
            pstmt.setInt(8, config.getInt("rare_encounter_count", 0));
            pstmt.setLong(9, config.getLong("created_at", System.currentTimeMillis()));
            pstmt.setLong(10, System.currentTimeMillis());
            
            pstmt.executeUpdate();
        }
    }
    
    /**
     * 检查奇遇记录是否已存在
     */
    private boolean fateExistsInDatabase(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM xian_fate_records WHERE player_uuid = ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, playerUuid.toString());
            
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
}
