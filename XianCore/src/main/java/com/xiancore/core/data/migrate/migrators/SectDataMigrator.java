package com.xiancore.core.data.migrate.migrators;

import com.xiancore.XianCore;
import com.xiancore.core.data.migrate.MigrationReport;
import com.xiancore.core.data.migrate.base.AbstractMigrator;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 宗门数据迁移器
 * 从 YML 文件迁移宗门数据到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class SectDataMigrator extends AbstractMigrator {
    
    private final File sectsFolder;
    
    public SectDataMigrator(XianCore plugin) {
        super(plugin);
        this.sectsFolder = new File(plugin.getDataFolder(), "sects");
    }
    
    @Override
    public String getName() {
        return "宗门数据迁移器";
    }
    
    @Override
    public String getDescription() {
        return "迁移宗门基本信息、成员数据、资金贡献等";
    }
    
    @Override
    public boolean hasDataToMigrate() {
        if (!sectsFolder.exists() || !sectsFolder.isDirectory()) {
            return false;
        }
        // 只匹配宗门配置文件（数字.yml），排除 _facilities.yml 和 _warehouse.yml
        File[] files = sectsFolder.listFiles((dir, name) -> name.matches("\\d+\\.yml"));
        return files != null && files.length > 0;
    }
    
    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();

        // 只匹配宗门配置文件（数字.yml），排除 _facilities.yml 和 _warehouse.yml
        File[] ymlFiles = sectsFolder.listFiles((dir, name) -> name.matches("\\d+\\.yml"));
        int fileCount = ymlFiles != null ? ymlFiles.length : 0;
        long totalSize = calculateTotalSize(sectsFolder, ".yml");
        
        sb.append("§e宗门数量: §f").append(fileCount).append(" 个\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());
        
        return sb.toString();
    }
    
    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();
        
        plugin.getLogger().info("§e开始迁移宗门数据...");
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
        
        // 获取所有宗门YML文件（只匹配数字.yml，排除 _facilities.yml 和 _warehouse.yml）
        File[] sectFiles = sectsFolder.listFiles((dir, name) -> name.matches("\\d+\\.yml"));
        if (sectFiles == null || sectFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何宗门YML文件");
            return report;
        }
        
        report.setTotalFiles(sectFiles.length);
        plugin.getLogger().info("§a找到 " + sectFiles.length + " 个宗门文件");
        
        // 开始迁移
        for (File sectFile : sectFiles) {
            try {
                String fileName = sectFile.getName();
                
                // 加载宗门数据
                Sect sect = loadSectFromYml(sectFile);
                if (sect == null) {
                    plugin.getLogger().warning("§c无法加载: " + fileName);
                    report.recordFailure(fileName, fileName, "无法加载YML数据");
                    continue;
                }
                
                // 检查是否已存在
                if (!dryRun && sectExistsInDatabase(sect.getId())) {
                    plugin.getLogger().info("§7跳过: " + sect.getName() + " (已存在)");
                    report.recordSkipped();
                    continue;
                }
                
                // 写入数据库
                if (!dryRun) {
                    dataManager.saveSect(sect);
                }
                
                plugin.getLogger().info("§a迁移宗门: " + sect.getName() + " (ID: " + sect.getId() + ")");
                report.addDataSize(sectFile.length());
                report.recordSuccess();
                
            } catch (Exception e) {
                String fileName = sectFile.getName();
                plugin.getLogger().warning("§c失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }
        
        report.complete();
        plugin.getLogger().info("§a宗门数据迁移完成！");
        
        return report;
    }
    
    @Override
    protected long estimateTimeInMillis() {
        // 只匹配宗门配置文件（数字.yml），排除 _facilities.yml 和 _warehouse.yml
        File[] files = sectsFolder.listFiles((dir, name) -> name.matches("\\d+\\.yml"));
        int fileCount = files != null ? files.length : 0;
        return fileCount * 50L; // 假设每个宗门50ms（含成员数据）
    }
    
    /**
     * 从YML加载宗门数据
     */
    private Sect loadSectFromYml(File file) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            Integer id = config.getInt("id");
            String name = config.getString("name", "");
            UUID ownerUuid = UUID.fromString(config.getString("owner_uuid"));
            String ownerName = config.getString("owner_name", "");
            
            // 创建宗门对象（不自动添加成员）
            Sect sect = new Sect(id, name, ownerUuid, ownerName, false);
            
            // 加载基本信息
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
            sect.setLastMaintenanceTime(config.getLong("last_maintenance_time", 0));
            
            // 加载成员数据
            if (config.contains("members")) {
                for (String uuidStr : config.getConfigurationSection("members").getKeys(false)) {
                    try {
                        UUID memberUuid = UUID.fromString(uuidStr);
                        String memberPath = "members." + uuidStr;
                        
                        SectMember member = new SectMember(
                            memberUuid,
                            config.getString(memberPath + ".name", "")
                        );
                        
                        String rankStr = config.getString(memberPath + ".rank", "MEMBER");
                        member.setRank(SectRank.valueOf(rankStr));
                        member.setContribution(config.getInt(memberPath + ".contribution", 0));
                        member.setWeeklyContribution(config.getInt(memberPath + ".weekly_contribution", 0));
                        member.setJoinedAt(config.getLong(memberPath + ".joined_at", System.currentTimeMillis()));
                        member.setLastActiveAt(config.getLong(memberPath + ".last_active_at", System.currentTimeMillis()));
                        member.setTasksCompleted(config.getInt(memberPath + ".tasks_completed", 0));
                        member.setDonationCount(config.getInt(memberPath + ".donation_count", 0));
                        
                        sect.getMembers().put(memberUuid, member);
                    } catch (Exception e) {
                        plugin.getLogger().warning("§c加载成员失败: " + uuidStr + " - " + e.getMessage());
                    }
                }
            }
            
            return sect;
        } catch (Exception e) {
            plugin.getLogger().severe("§c加载失败: " + file.getName() + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查宗门是否已存在
     */
    private boolean sectExistsInDatabase(Integer sectId) {
        String sql = "SELECT COUNT(*) FROM xian_sects WHERE id = ?";
        
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
}
