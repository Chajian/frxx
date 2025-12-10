package com.xiancore.core.data.migrate.migrators;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.data.SpiritualRootType;
import com.xiancore.core.data.migrate.MigrationReport;
import com.xiancore.core.data.migrate.base.AbstractMigrator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家数据迁移器
 * 从 YML 文件迁移玩家数据到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class PlayerDataMigrator extends AbstractMigrator {
    
    private final File playersFolder;
    
    public PlayerDataMigrator(XianCore plugin) {
        super(plugin);
        this.playersFolder = new File(plugin.getDataFolder(), "players");
    }
    
    @Override
    public String getName() {
        return "玩家数据迁移器";
    }
    
    @Override
    public String getDescription() {
        return "迁移玩家基本信息、修炼数据、功法、装备等";
    }
    
    @Override
    public boolean hasDataToMigrate() {
        if (!playersFolder.exists() || !playersFolder.isDirectory()) {
            return false;
        }
        File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        return files != null && files.length > 0;
    }
    
    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();
        
        File[] ymlFiles = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = ymlFiles != null ? ymlFiles.length : 0;
        long totalSize = calculateTotalSize(playersFolder, ".yml");
        
        sb.append("§e文件数量: §f").append(fileCount).append(" 个\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());
        
        return sb.toString();
    }
    
    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();
        
        plugin.getLogger().info("§e开始迁移玩家数据...");
        plugin.getLogger().info("§e模式: " + (dryRun ? "§6预览模式" : "§c真实迁移"));
        
        // 检查MySQL
        if (!isMySqlAvailable()) {
            plugin.getLogger().severe("§cMySQL未连接！");
            return report;
        }
        
        // 检查文件夹
        if (!playersFolder.exists()) {
            plugin.getLogger().warning("§c未找到 players 目录");
            return report;
        }
        
        // 获取所有YML文件
        File[] ymlFiles = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (ymlFiles == null || ymlFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何玩家YML文件");
            return report;
        }
        
        report.setTotalFiles(ymlFiles.length);
        plugin.getLogger().info("§a找到 " + ymlFiles.length + " 个玩家文件");
        
        // 开始迁移
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger lastProgress = new AtomicInteger(0);
        
        for (File ymlFile : ymlFiles) {
            try {
                String fileName = ymlFile.getName();
                String uuidStr = fileName.replace(".yml", "");
                UUID uuid = UUID.fromString(uuidStr);
                
                // 检查是否已存在
                if (!dryRun && existsInDatabase(uuid)) {
                    plugin.getLogger().info("§7跳过: " + fileName + " (已存在)");
                    report.recordSkipped();
                    continue;
                }
                
                // 加载YML数据
                PlayerData data = loadPlayerDataFromYml(ymlFile, uuid);
                if (data == null) {
                    report.recordFailure(fileName, uuidStr, "无法加载YML数据");
                    continue;
                }
                
                // 写入数据库
                if (!dryRun) {
                    dataManager.savePlayerData(data);
                }
                
                report.addDataSize(ymlFile.length());
                report.recordSuccess();
                
                // 显示进度
                int current = processed.incrementAndGet();
                int progress = (current * 100) / ymlFiles.length;
                if (progress >= lastProgress.get() + 20) {
                    lastProgress.set(progress);
                    plugin.getLogger().info(String.format("§a进度: %d%% (%d/%d)", 
                        progress, current, ymlFiles.length));
                }
                
            } catch (Exception e) {
                String fileName = ymlFile.getName();
                plugin.getLogger().warning("§c失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }
        
        report.complete();
        plugin.getLogger().info("§a玩家数据迁移完成！");
        
        return report;
    }
    
    @Override
    protected long estimateTimeInMillis() {
        File[] files = playersFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        int fileCount = files != null ? files.length : 0;
        return fileCount * 30L; // 假设每个文件30ms
    }
    
    /**
     * 从YML加载玩家数据
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
            
            // 灵根类型
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
            
            // 功法数据
            if (config.contains("learned_skills")) {
                for (String skillId : config.getConfigurationSection("learned_skills").getKeys(false)) {
                    int level = config.getInt("learned_skills." + skillId, 1);
                    data.getLearnedSkills().put(skillId, level);
                }
            }
            
            // 功法绑定
            if (config.contains("skill_bindings")) {
                for (String slotStr : config.getConfigurationSection("skill_bindings").getKeys(false)) {
                    int slot = Integer.parseInt(slotStr);
                    String skillId = config.getString("skill_bindings." + slotStr);
                    data.getSkillBindings().put(slot, skillId);
                }
            }
            
            // 装备数据
            if (config.contains("equipment")) {
                for (String slot : config.getConfigurationSection("equipment").getKeys(false)) {
                    String equipmentUuid = config.getString("equipment." + slot);
                    data.getEquipment().put(slot, equipmentUuid);
                }
            }
            
            return data;
        } catch (Exception e) {
            plugin.getLogger().severe("§c加载失败: " + file.getName() + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查玩家是否已存在
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
}
