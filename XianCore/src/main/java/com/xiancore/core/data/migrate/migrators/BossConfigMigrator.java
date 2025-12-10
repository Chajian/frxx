package com.xiancore.core.data.migrate.migrators;

import com.xiancore.XianCore;
import com.xiancore.core.data.migrate.MigrationReport;
import com.xiancore.core.data.migrate.base.AbstractMigrator;
import com.xiancore.systems.boss.config.BossConfigLoader;
import com.xiancore.systems.boss.config.BossRefreshConfig;

import java.io.File;
import java.sql.Connection;

/**
 * Boss刷新配置迁移器
 * 将 boss-refresh.yml 配置文件迁移到MySQL数据库
 *
 * 迁移内容：
 * - 全局刷新配置（检查间隔、最大Boss数等）
 * - 所有Boss刷新点配置（位置、冷却时间、刷新模式等）
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class BossConfigMigrator extends AbstractMigrator {

    private final BossConfigLoader configLoader;
    private File configFile;
    private BossRefreshConfig config;

    public BossConfigMigrator(XianCore plugin) {
        super(plugin);
        this.configLoader = new BossConfigLoader(plugin);
        this.configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
    }

    @Override
    public String getName() {
        return "Boss刷新配置迁移器";
    }

    @Override
    public String getDescription() {
        return "将 boss-refresh.yml 配置文件迁移到MySQL数据库";
    }

    @Override
    public boolean hasDataToMigrate() {
        // 检查YAML配置文件是否存在
        return configFile.exists();
    }

    @Override
    public String getPreMigrationSummary() {
        if (!hasDataToMigrate()) {
            return "§7未找到 boss-refresh.yml 配置文件，跳过迁移";
        }

        try {
            // 尝试加载配置以获取摘要信息
            config = configLoader.loadConfig(configFile);

            StringBuilder summary = new StringBuilder();
            summary.append("§e配置文件: §fboss-refresh.yml\n");
            summary.append("§e刷新点数量: §f").append(config.getSpawnPoints().size()).append(" 个\n");
            summary.append("§e检查间隔: §f").append(config.getCheckIntervalSeconds()).append(" 秒\n");
            summary.append("§e最大Boss数: §f").append(config.getMaxActiveBosses()).append(" 个\n");
            summary.append("§e最少玩家数: §f").append(config.getMinOnlinePlayers()).append(" 人");

            return summary.toString();

        } catch (Exception e) {
            return "§c无法读取配置文件: " + e.getMessage();
        }
    }

    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();

        try {
            // 1. 检查配置文件是否存在
            if (!hasDataToMigrate()) {
                plugin.getLogger().info("  §7未找到 boss-refresh.yml 配置文件，跳过迁移");
                report.setTotalFiles(1);
                report.recordSkipped();
                report.complete();
                return report;
            }

            plugin.getLogger().info("  §e开始迁移Boss配置...");

            // 2. 加载YAML配置
            plugin.getLogger().info("  §7→ 读取 boss-refresh.yml...");
            config = configLoader.loadConfig(configFile);

            if (config == null || config.getSpawnPoints().isEmpty()) {
                plugin.getLogger().info("  §7配置为空，跳过迁移");
                report.setTotalFiles(1);
                report.recordSkipped();
                report.complete();
                return report;
            }

            int totalSpawnPoints = config.getSpawnPoints().size();
            plugin.getLogger().info("  §7→ 找到 " + totalSpawnPoints + " 个刷新点配置");

            report.setTotalFiles(1);

            // 3. 预览模式：只显示信息，不写入数据库
            if (dryRun) {
                plugin.getLogger().info("  §6[预览] 将迁移以下配置:");
                plugin.getLogger().info("  §6  - 全局配置: 检查间隔=" + config.getCheckIntervalSeconds() + "秒");
                plugin.getLogger().info("  §6  - 刷新点数: " + totalSpawnPoints + " 个");

                for (var point : config.getSpawnPoints()) {
                    plugin.getLogger().info("  §6    * " + point.getId() +
                            " (" + point.getMythicMobId() + ", Tier " + point.getTier() + ")");
                }

                report.recordSkipped();
                report.complete();
                return report;
            }

            // 4. 真实迁移：写入MySQL数据库
            plugin.getLogger().info("  §7→ 写入MySQL数据库...");

            try (Connection conn = plugin.getDataManager().getConnection()) {
                // 保存配置到数据库
                configLoader.saveConfigToDatabase(config, conn);

                // 记录成功
                report.recordSuccess();

                plugin.getLogger().info("  §a✓ Boss配置迁移完成！");
                plugin.getLogger().info("  §a  - 全局配置: 检查间隔=" + config.getCheckIntervalSeconds() + "秒");
                plugin.getLogger().info("  §a  - 刷新点数量: " + totalSpawnPoints + " 个");

            } catch (Exception e) {
                report.recordFailure("boss-refresh.yml", "", "数据库写入失败: " + e.getMessage());
                plugin.getLogger().warning("  §c✗ 迁移失败: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            report.recordFailure("boss-refresh.yml", "", "迁移异常: " + e.getMessage());
            plugin.getLogger().warning("  §c✗ 迁移异常: " + e.getMessage());
            e.printStackTrace();
        }

        report.complete();
        return report;
    }

    @Override
    protected long estimateTimeInMillis() {
        if (!hasDataToMigrate()) {
            return 0;
        }

        try {
            config = configLoader.loadConfig(configFile);
            int pointCount = config.getSpawnPoints().size();

            if (pointCount == 0) {
                return 500; // 0.5 seconds
            } else if (pointCount < 10) {
                return 1500; // 1.5 seconds
            } else if (pointCount < 50) {
                return 4000; // 4 seconds
            } else {
                return 7500; // 7.5 seconds
            }

        } catch (Exception e) {
            return 1000; // Default 1 second
        }
    }
}
