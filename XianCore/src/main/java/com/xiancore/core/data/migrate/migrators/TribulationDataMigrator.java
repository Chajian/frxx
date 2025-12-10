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
 * 渡劫数据迁移器
 * 从 YML 文件迁移玩家渡劫记录到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class TribulationDataMigrator extends AbstractMigrator {
    
    private final File tribulationFolder;
    
    public TribulationDataMigrator(XianCore plugin) {
        super(plugin);
        this.tribulationFolder = new File(plugin.getDataFolder(), "tribulation");
    }
    
    @Override
    public String getName() {
        return "渡劫数据迁移器";
    }
    
    @Override
    public String getDescription() {
        return "迁移玩家渡劫记录、突破尝试、天劫威力等";
    }
    
    @Override
    public boolean hasDataToMigrate() {
        if (!tribulationFolder.exists() || !tribulationFolder.isDirectory()) {
            return false;
        }
        File[] files = tribulationFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files != null && files.length > 0;
    }
    
    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();
        
        File[] ymlFiles = tribulationFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = ymlFiles != null ? ymlFiles.length : 0;
        long totalSize = calculateTotalSize(tribulationFolder, ".yml");
        
        sb.append("§e渡劫记录数: §f").append(fileCount).append(" 个\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());
        
        return sb.toString();
    }
    
    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();
        
        plugin.getLogger().info("§e开始迁移渡劫数据...");
        plugin.getLogger().info("§e模式: " + (dryRun ? "§6预览模式" : "§c真实迁移"));
        
        // 检查MySQL
        if (!isMySqlAvailable()) {
            plugin.getLogger().severe("§cMySQL未连接！");
            return report;
        }
        
        // 检查文件夹
        if (!tribulationFolder.exists()) {
            plugin.getLogger().warning("§c未找到 tribulation 目录");
            return report;
        }
        
        // 获取所有YML文件
        File[] tribulationFiles = tribulationFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (tribulationFiles == null || tribulationFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何渡劫YML文件");
            return report;
        }
        
        report.setTotalFiles(tribulationFiles.length);
        plugin.getLogger().info("§a找到 " + tribulationFiles.length + " 个渡劫记录");
        
        // 开始迁移
        for (File tribFile : tribulationFiles) {
            try {
                String fileName = tribFile.getName();
                String uuidStr = fileName.replace(".yml", "");
                UUID playerUuid = UUID.fromString(uuidStr);
                
                // 加载渡劫数据
                YamlConfiguration config = YamlConfiguration.loadConfiguration(tribFile);
                
                // 检查是否已存在
                if (!dryRun && tribulationExistsInDatabase(playerUuid)) {
                    plugin.getLogger().info("§7跳过: " + fileName + " (已存在)");
                    report.recordSkipped();
                    continue;
                }
                
                // 写入数据库
                if (!dryRun) {
                    saveTribulationToDatabase(playerUuid, config);
                }
                
                plugin.getLogger().info("§a迁移渡劫记录: " + playerUuid);
                report.addDataSize(tribFile.length());
                report.recordSuccess();
                
            } catch (Exception e) {
                String fileName = tribFile.getName();
                plugin.getLogger().warning("§c失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }
        
        report.complete();
        plugin.getLogger().info("§a渡劫数据迁移完成！");
        
        return report;
    }
    
    @Override
    protected long estimateTimeInMillis() {
        File[] files = tribulationFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = files != null ? files.length : 0;
        return fileCount * 25L; // 每个记录约25ms
    }
    
    /**
     * 保存渡劫数据到数据库
     */
    private void saveTribulationToDatabase(UUID playerUuid, YamlConfiguration config) throws SQLException {
        String sql = "INSERT INTO xian_tribulation_records " +
                    "(player_uuid, total_attempts, successful_attempts, failed_attempts, " +
                    "last_tribulation_time, next_tribulation_realm, tribulation_power, " +
                    "heavenly_punishment, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "total_attempts = VALUES(total_attempts), " +
                    "successful_attempts = VALUES(successful_attempts), " +
                    "failed_attempts = VALUES(failed_attempts), " +
                    "last_tribulation_time = VALUES(last_tribulation_time), " +
                    "next_tribulation_realm = VALUES(next_tribulation_realm), " +
                    "tribulation_power = VALUES(tribulation_power), " +
                    "heavenly_punishment = VALUES(heavenly_punishment), " +
                    "updated_at = VALUES(updated_at)";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, playerUuid.toString());
            pstmt.setInt(2, config.getInt("total_attempts", 0));
            pstmt.setInt(3, config.getInt("successful_attempts", 0));
            pstmt.setInt(4, config.getInt("failed_attempts", 0));
            pstmt.setLong(5, config.getLong("last_tribulation_time", 0));
            pstmt.setString(6, config.getString("next_tribulation_realm", ""));
            pstmt.setDouble(7, config.getDouble("tribulation_power", 1.0));
            pstmt.setInt(8, config.getInt("heavenly_punishment", 0));
            pstmt.setLong(9, config.getLong("created_at", System.currentTimeMillis()));
            pstmt.setLong(10, System.currentTimeMillis());
            
            pstmt.executeUpdate();
        }
    }
    
    /**
     * 检查渡劫记录是否已存在
     */
    private boolean tribulationExistsInDatabase(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM xian_tribulation_records WHERE player_uuid = ?";
        
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
