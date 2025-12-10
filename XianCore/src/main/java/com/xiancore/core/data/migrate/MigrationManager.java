package com.xiancore.core.data.migrate;

import com.xiancore.XianCore;
import com.xiancore.core.data.migrate.base.IMigrator;
import com.xiancore.core.data.migrate.migrators.PlayerDataMigrator;
import com.xiancore.core.data.migrate.migrators.SectDataMigrator;
import com.xiancore.core.data.migrate.migrators.BossDataMigrator;
import com.xiancore.core.data.migrate.migrators.BossConfigMigrator;
import com.xiancore.core.data.migrate.migrators.TribulationDataMigrator;
import com.xiancore.core.data.migrate.migrators.FateDataMigrator;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 迁移管理器
 * 统一管理所有类型的数据迁移
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class MigrationManager {
    
    private final XianCore plugin;
    private final Map<String, IMigrator> migrators = new LinkedHashMap<>();
    
    public MigrationManager(XianCore plugin) {
        this.plugin = plugin;
        registerMigrators();
    }
    
    /**
     * 注册所有迁移器
     */
    private void registerMigrators() {
        // 注册玩家数据迁移器
        registerMigrator("player", new PlayerDataMigrator(plugin));

        // 注册宗门数据迁移器
        registerMigrator("sect", new SectDataMigrator(plugin));

        // 注册Boss记录迁移器
        registerMigrator("boss", new BossDataMigrator(plugin));

        // 注册Boss配置迁移器（新增）
        registerMigrator("boss-config", new BossConfigMigrator(plugin));

        // 注册渡劫数据迁移器
        registerMigrator("tribulation", new TribulationDataMigrator(plugin));

        // 注册奇遇数据迁移器
        registerMigrator("fate", new FateDataMigrator(plugin));
    }
    
    /**
     * 注册迁移器
     */
    public void registerMigrator(String type, IMigrator migrator) {
        migrators.put(type, migrator);
        plugin.getLogger().info("§a注册迁移器: §f" + type + " - " + migrator.getName());
    }
    
    /**
     * 获取指定类型的迁移器
     */
    public IMigrator getMigrator(String type) {
        return migrators.get(type);
    }
    
    /**
     * 获取所有迁移器
     */
    public Collection<IMigrator> getAllMigrators() {
        return migrators.values();
    }
    
    /**
     * 执行完整迁移（所有类型）
     */
    public CompletableFuture<Map<String, MigrationReport>> migrateAll(boolean dryRun) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, MigrationReport> reports = new LinkedHashMap<>();
            
            plugin.getLogger().info("§e========================================");
            plugin.getLogger().info("§e§l    开始完整数据迁移");
            plugin.getLogger().info("§e========================================");
            plugin.getLogger().info("§e模式: " + (dryRun ? "§6预览模式（不写入数据库）" : "§c真实迁移"));
            plugin.getLogger().info("§e迁移器数量: §f" + migrators.size());
            plugin.getLogger().info("");
            
            int current = 0;
            for (Map.Entry<String, IMigrator> entry : migrators.entrySet()) {
                current++;
                String type = entry.getKey();
                IMigrator migrator = entry.getValue();
                
                plugin.getLogger().info(String.format("§e[%d/%d] 开始迁移: §f%s", 
                    current, migrators.size(), migrator.getName()));
                
                try {
                    MigrationReport report = migrator.migrate(dryRun);
                    reports.put(type, report);
                    
                    plugin.getLogger().info(String.format("§a[%d/%d] §f%s §a迁移完成！", 
                        current, migrators.size(), migrator.getName()));
                } catch (Exception e) {
                    plugin.getLogger().severe(String.format("§c[%d/%d] §f%s §c迁移失败: %s", 
                        current, migrators.size(), migrator.getName(), e.getMessage()));
                    e.printStackTrace();
                }
                
                plugin.getLogger().info("");
            }
            
            // 输出总结报告
            printSummaryReport(reports);
            
            return reports;
        });
    }
    
    /**
     * 执行指定类型的迁移
     */
    public CompletableFuture<MigrationReport> migrate(String type, boolean dryRun) {
        return CompletableFuture.supplyAsync(() -> {
            IMigrator migrator = getMigrator(type);
            if (migrator == null) {
                throw new IllegalArgumentException("未知的迁移类型: " + type);
            }
            
            plugin.getLogger().info("§e开始迁移: §f" + migrator.getName());
            return migrator.migrate(dryRun);
        });
    }
    
    /**
     * 获取迁移前摘要
     */
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("§b========================================\n");
        sb.append("§e§l       数据迁移准备信息\n");
        sb.append("§b========================================\n");
        sb.append("\n");
        
        // 遍历所有迁移器
        for (Map.Entry<String, IMigrator> entry : migrators.entrySet()) {
            IMigrator migrator = entry.getValue();
            
            if (migrator.hasDataToMigrate()) {
                sb.append("§e▶ §f").append(migrator.getName()).append("\n");
                sb.append("§7  ").append(migrator.getDescription()).append("\n");
                String summary = migrator.getPreMigrationSummary();
                for (String line : summary.split("\n")) {
                    if (!line.trim().isEmpty()) {
                        sb.append("  ").append(line).append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        
        // 提示信息
        sb.append("§e提示:\n");
        sb.append("§7- 迁移过程中会自动跳过已存在的数据\n");
        sb.append("§7- 建议先使用 --dry-run 参数预览\n");
        sb.append("§7- 迁移不会删除YML文件（作为备份保留）\n");
        sb.append("§b========================================\n");
        
        return sb.toString();
    }
    
    /**
     * 打印总结报告
     */
    private void printSummaryReport(Map<String, MigrationReport> reports) {
        plugin.getLogger().info("§b========================================");
        plugin.getLogger().info("§e§l         迁移总结报告");
        plugin.getLogger().info("§b========================================");
        plugin.getLogger().info("");
        
        int totalSuccess = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        
        for (Map.Entry<String, MigrationReport> entry : reports.entrySet()) {
            String type = entry.getKey();
            MigrationReport report = entry.getValue();
            
            plugin.getLogger().info(String.format("§e%s:", type));
            plugin.getLogger().info(String.format("  §a成功: %d  §c失败: %d  §7跳过: %d",
                report.getSuccessCount(), report.getFailedCount(), report.getSkippedCount()));
            
            totalSuccess += report.getSuccessCount();
            totalFailed += report.getFailedCount();
            totalSkipped += report.getSkippedCount();
        }
        
        plugin.getLogger().info("");
        plugin.getLogger().info("§e总计:");
        plugin.getLogger().info(String.format("  §a成功: %d  §c失败: %d  §7跳过: %d",
            totalSuccess, totalFailed, totalSkipped));
        plugin.getLogger().info("§b========================================");
    }
}
