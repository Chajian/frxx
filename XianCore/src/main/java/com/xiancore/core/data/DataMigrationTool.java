package com.xiancore.core.data;

import com.xiancore.XianCore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * YML到MySQL数据迁移工具
 * 独立工具类，用于将玩家数据从YML文件批量迁移到MySQL数据库
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class DataMigrationTool {
    
    private final XianCore plugin;
    private final DataManager dataManager;
    private final File playersFolder;
    
    public DataMigrationTool(XianCore plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.playersFolder = new File(plugin.getDataFolder(), "players");
    }
    
    /**
     * 执行完整迁移（同步）
     * 
     * @param dryRun 如果为true，仅预览不实际写入
     * @return 迁移报告
     */
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();
        
        plugin.getLogger().info("§e========================================");
        plugin.getLogger().info("§e§l开始数据迁移: YML → MySQL");
        if (dryRun) {
            plugin.getLogger().info("§7模式: 预览模式（不写入数据库）");
        }
        plugin.getLogger().info("§e========================================");
        
        // 检查MySQL是否启用
        if (!isMySqlAvailable()) {
            plugin.getLogger().severe("§c错误: MySQL未启用或无法连接！");
            plugin.getLogger().severe("§c请在config.yml中设置 database.use-mysql: true");
            plugin.getLogger().severe("§c并确保MySQL数据库连接配置正确");
            return report;
        }
        
        // 检查players文件夹
        if (!playersFolder.exists() || !playersFolder.isDirectory()) {
            plugin.getLogger().severe("§c错误: players文件夹不存在: " + playersFolder.getAbsolutePath());
            return report;
        }
        
        // 获取所有YML文件
        File[] ymlFiles = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles == null || ymlFiles.length == 0) {
            plugin.getLogger().warning("§e警告: 未找到任何YML文件");
            return report;
        }
        
        report.setTotalFiles(ymlFiles.length);
        plugin.getLogger().info("§a找到 " + ymlFiles.length + " 个YML文件");
        
        // 开始迁移
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger lastProgress = new AtomicInteger(0);
        
        for (File ymlFile : ymlFiles) {
            try {
                // 解析UUID
                String fileName = ymlFile.getName();
                String uuidStr = fileName.replace(".yml", "");
                UUID uuid = UUID.fromString(uuidStr);
                
                // 检查是否已存在
                if (!dryRun && existsInDatabase(uuid)) {
                    plugin.getLogger().info("§7跳过: " + fileName + " (已存在于数据库)");
                    report.recordSkipped();
                    continue;
                }
                
                // 加载YML数据
                PlayerData data = loadPlayerDataFromYml(ymlFile, uuid);
                if (data == null) {
                    report.recordFailure(fileName, uuidStr, "无法加载YML数据");
                    continue;
                }
                
                // 写入数据库（如果不是预览模式）
                if (!dryRun) {
                    saveToDatabase(data);
                }
                
                // 记录文件大小
                report.addDataSize(ymlFile.length());
                report.recordSuccess();
                
                // 显示进度
                int current = processed.incrementAndGet();
                int progress = (current * 100) / ymlFiles.length;
                if (progress >= lastProgress.get() + 10) {
                    lastProgress.set(progress);
                    plugin.getLogger().info(String.format("§a进度: %d%% (%d/%d)", 
                        progress, current, ymlFiles.length));
                }
                
            } catch (Exception e) {
                String fileName = ymlFile.getName();
                plugin.getLogger().warning("§c迁移失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }
        
        report.complete();
        
        // 输出报告
        plugin.getLogger().info(report.generateReport());
        
        return report;
    }
    
    /**
     * 异步执行迁移
     */
    public CompletableFuture<MigrationReport> migrateAsync(boolean dryRun) {
        return CompletableFuture.supplyAsync(() -> migrate(dryRun));
    }
    
    /**
     * 从YML文件加载玩家数据
     */
    private PlayerData loadPlayerDataFromYml(File file, UUID uuid) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            PlayerData data = new PlayerData(uuid);
            data.setName(config.getString("name", ""));
            data.setRealm(config.getString("realm", "炼气期"));
            data.setRealmStage(config.getInt("realm_stage", 1));
            data.setQi(config.getLong("qi", 0));
            data.setSpiritualRoot(config.getDouble("spiritual_root", 0.5));
            
            // 加载灵根类型
            String rootTypeStr = config.getString("spiritual_root_type");
            if (rootTypeStr != null && !rootTypeStr.isEmpty()) {
                try {
                    data.setSpiritualRootType(SpiritualRootType.valueOf(rootTypeStr));
                } catch (IllegalArgumentException e) {
                    data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
                }
            } else {
                data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
            }
            
            data.setComprehension(config.getDouble("comprehension", 0.5));
            data.setTechniqueAdaptation(config.getDouble("technique_adaptation", 0.6));
            data.setSpiritStones(config.getLong("spirit_stones", 0));
            data.setContributionPoints(config.getInt("contribution_points", 0));
            data.setSkillPoints(config.getInt("skill_points", 0));
            data.setPlayerLevel(config.getInt("player_level", 1));
            
            if (config.contains("sect_id")) {
                data.setSectId(config.getInt("sect_id"));
            }
            
            data.setSectRank(config.getString("sect_rank", "member"));
            data.setLastLogin(config.getLong("last_login", System.currentTimeMillis()));
            data.setCreatedAt(config.getLong("created_at", System.currentTimeMillis()));
            data.setUpdatedAt(config.getLong("updated_at", System.currentTimeMillis()));
            data.setBreakthroughAttempts(config.getInt("breakthrough_attempts", 0));
            data.setSuccessfulBreakthroughs(config.getInt("successful_breakthroughs", 0));
            data.setActiveQi(config.getLong("active_qi", 0));
            data.setLastFateTime(config.getLong("last_fate_time", 0));
            data.setFateCount(config.getInt("fate_count", 0));
            
            // 加载功法数据
            if (config.contains("learned_skills")) {
                for (String skillId : config.getConfigurationSection("learned_skills").getKeys(false)) {
                    int level = config.getInt("learned_skills." + skillId, 1);
                    data.getLearnedSkills().put(skillId, level);
                }
            }
            
            // 加载功法绑定数据
            if (config.contains("skill_bindings")) {
                for (String slotStr : config.getConfigurationSection("skill_bindings").getKeys(false)) {
                    int slot = Integer.parseInt(slotStr);
                    String skillId = config.getString("skill_bindings." + slotStr);
                    data.getSkillBindings().put(slot, skillId);
                }
            }
            
            // 加载装备数据
            if (config.contains("equipment")) {
                for (String slot : config.getConfigurationSection("equipment").getKeys(false)) {
                    String equipmentUuid = config.getString("equipment." + slot);
                    data.getEquipment().put(slot, equipmentUuid);
                }
            }
            
            return data;
        } catch (Exception e) {
            plugin.getLogger().severe("§c加载YML文件失败: " + file.getName());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 检查玩家是否已存在于数据库
     */
    private boolean existsInDatabase(UUID uuid) {
        String sql = "SELECT COUNT(*) FROM xian_players WHERE uuid = ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, uuid.toString());
            
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
     * 保存到数据库（使用DataManager的方法）
     */
    private void saveToDatabase(PlayerData data) {
        try {
            // 直接使用DataManager的保存方法
            dataManager.savePlayerData(data);
        } catch (Exception e) {
            throw new RuntimeException("保存到数据库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 估算迁移时间
     */
    public String estimateMigrationTime() {
        File[] ymlFiles = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles == null || ymlFiles.length == 0) {
            return "无文件需要迁移";
        }
        
        int fileCount = ymlFiles.length;
        // 假设每个文件处理需要10-50ms，取平均30ms
        long estimatedMs = fileCount * 30L;
        long estimatedSeconds = estimatedMs / 1000;
        
        if (estimatedSeconds < 60) {
            return "约 " + estimatedSeconds + " 秒";
        } else if (estimatedSeconds < 3600) {
            return "约 " + (estimatedSeconds / 60) + " 分钟";
        } else {
            return "约 " + (estimatedSeconds / 3600) + " 小时";
        }
    }
    
    /**
     * 获取迁移前的信息摘要
     */
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("§b========================================\n");
        sb.append("§e§l       数据迁移准备信息\n");
        sb.append("§b========================================\n");
        
        // YML文件信息
        File[] ymlFiles = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = (ymlFiles != null) ? ymlFiles.length : 0;
        long totalSize = 0;
        if (ymlFiles != null) {
            for (File file : ymlFiles) {
                totalSize += file.length();
            }
        }
        
        sb.append("§eYML文件位置: §f").append(playersFolder.getAbsolutePath()).append("\n");
        sb.append("§e找到文件数: §f").append(fileCount).append(" 个\n");
        sb.append("§e总数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime()).append("\n");
        
        // 数据库信息
        sb.append("\n§b======== 目标数据库 ========\n");
        sb.append("§e类型: §fMySQL\n");
        sb.append("§e状态: §f").append(isMySqlAvailable() ? "§a已连接" : "§c未连接").append("\n");
        
        sb.append("\n§e§l提示:\n");
        sb.append("§7- 迁移过程中会自动跳过已存在的数据\n");
        sb.append("§7- 建议先使用 --dry-run 参数预览\n");
        sb.append("§7- 迁移不会删除YML文件（作为备份保留）\n");
        sb.append("§b========================================\n");
        
        return sb.toString();
    }
    
    /**
     * 检查MySQL是否可用
     */
    private boolean isMySqlAvailable() {
        try (Connection conn = dataManager.getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
