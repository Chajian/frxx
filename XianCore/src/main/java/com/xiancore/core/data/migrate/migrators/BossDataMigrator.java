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

/**
 * Boss数据迁移器
 * 从 YML 文件迁移Boss相关数据到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class BossDataMigrator extends AbstractMigrator {
    
    private final File bossFolder;
    
    public BossDataMigrator(XianCore plugin) {
        super(plugin);
        this.bossFolder = new File(plugin.getDataFolder(), "boss");
    }
    
    @Override
    public String getName() {
        return "Boss数据迁移器";
    }
    
    @Override
    public String getDescription() {
        return "迁移Boss击杀记录、伤害统计、奖励发放等";
    }
    
    @Override
    public boolean hasDataToMigrate() {
        if (!bossFolder.exists() || !bossFolder.isDirectory()) {
            return false;
        }
        File[] files = bossFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files != null && files.length > 0;
    }
    
    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();
        
        File[] ymlFiles = bossFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = ymlFiles != null ? ymlFiles.length : 0;
        long totalSize = calculateTotalSize(bossFolder, ".yml");
        
        sb.append("§eBoss记录数: §f").append(fileCount).append(" 个\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());
        
        return sb.toString();
    }
    
    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();
        
        plugin.getLogger().info("§e开始迁移Boss数据...");
        plugin.getLogger().info("§e模式: " + (dryRun ? "§6预览模式" : "§c真实迁移"));
        
        // 检查MySQL
        if (!isMySqlAvailable()) {
            plugin.getLogger().severe("§cMySQL未连接！");
            return report;
        }
        
        // 检查文件夹
        if (!bossFolder.exists()) {
            plugin.getLogger().warning("§c未找到 boss 目录");
            return report;
        }
        
        // 获取所有YML文件
        File[] bossFiles = bossFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (bossFiles == null || bossFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何BossYML文件");
            return report;
        }
        
        report.setTotalFiles(bossFiles.length);
        plugin.getLogger().info("§a找到 " + bossFiles.length + " 个Boss记录");
        
        // 开始迁移
        for (File bossFile : bossFiles) {
            try {
                String fileName = bossFile.getName();
                String bossId = fileName.replace(".yml", "");
                
                // 加载Boss数据
                YamlConfiguration config = YamlConfiguration.loadConfiguration(bossFile);
                
                // 检查是否已存在
                if (!dryRun && bossExistsInDatabase(bossId)) {
                    plugin.getLogger().info("§7跳过: " + fileName + " (已存在)");
                    report.recordSkipped();
                    continue;
                }
                
                // 写入数据库
                if (!dryRun) {
                    saveBossToDatabase(bossId, config);
                }
                
                plugin.getLogger().info("§a迁移Boss记录: " + bossId);
                report.addDataSize(bossFile.length());
                report.recordSuccess();
                
            } catch (Exception e) {
                String fileName = bossFile.getName();
                plugin.getLogger().warning("§c失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }
        
        report.complete();
        plugin.getLogger().info("§aBoss数据迁移完成！");
        
        return report;
    }
    
    @Override
    protected long estimateTimeInMillis() {
        File[] files = bossFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = files != null ? files.length : 0;
        return fileCount * 40L; // 每个Boss记录约40ms
    }
    
    /**
     * 保存Boss数据到数据库
     */
    private void saveBossToDatabase(String bossId, YamlConfiguration config) throws SQLException {
        String sql = "INSERT INTO xian_boss_records " +
                    "(boss_id, boss_name, total_kills, last_spawn_time, " +
                    "last_kill_time, total_damage_dealt, killer_uuid, " +
                    "reward_distributed, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "boss_name = VALUES(boss_name), " +
                    "total_kills = VALUES(total_kills), " +
                    "last_spawn_time = VALUES(last_spawn_time), " +
                    "last_kill_time = VALUES(last_kill_time), " +
                    "total_damage_dealt = VALUES(total_damage_dealt), " +
                    "killer_uuid = VALUES(killer_uuid), " +
                    "reward_distributed = VALUES(reward_distributed), " +
                    "updated_at = VALUES(updated_at)";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, bossId);
            pstmt.setString(2, config.getString("boss_name", ""));
            pstmt.setInt(3, config.getInt("total_kills", 0));
            pstmt.setLong(4, config.getLong("last_spawn_time", 0));
            pstmt.setLong(5, config.getLong("last_kill_time", 0));
            pstmt.setLong(6, config.getLong("total_damage_dealt", 0));
            pstmt.setString(7, config.getString("killer_uuid", ""));
            pstmt.setBoolean(8, config.getBoolean("reward_distributed", false));
            pstmt.setLong(9, config.getLong("created_at", System.currentTimeMillis()));
            pstmt.setLong(10, System.currentTimeMillis());
            
            pstmt.executeUpdate();
        }
    }
    
    /**
     * 检查Boss记录是否已存在
     */
    private boolean bossExistsInDatabase(String bossId) {
        String sql = "SELECT COUNT(*) FROM xian_boss_records WHERE boss_id = ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, bossId);
            
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
