package com.xiancore.core.data.migrate.migrators;

import com.xiancore.XianCore;
import com.xiancore.core.data.migrate.MigrationReport;
import com.xiancore.core.data.migrate.base.AbstractMigrator;
import com.xiancore.core.data.repository.WarehouseRepository;
import com.xiancore.systems.sect.warehouse.SectWarehouse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * 宗门仓库数据迁移器
 * 从 YML 文件迁移宗门仓库数据到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class WarehouseDataMigrator extends AbstractMigrator {

    private final File sectsFolder;
    private final WarehouseRepository warehouseRepository;

    public WarehouseDataMigrator(XianCore plugin) {
        super(plugin);
        this.sectsFolder = new File(plugin.getDataFolder(), "sects");
        this.warehouseRepository = new WarehouseRepository(plugin);
    }

    @Override
    public String getName() {
        return "宗门仓库迁移器";
    }

    @Override
    public String getDescription() {
        return "迁移宗门仓库物品数据";
    }

    @Override
    public boolean hasDataToMigrate() {
        if (!sectsFolder.exists() || !sectsFolder.isDirectory()) {
            return false;
        }
        // 匹配 *_warehouse.yml 文件
        File[] files = sectsFolder.listFiles((dir, name) -> name.endsWith("_warehouse.yml"));
        return files != null && files.length > 0;
    }

    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();

        // 匹配 *_warehouse.yml 文件
        File[] ymlFiles = sectsFolder.listFiles((dir, name) -> name.endsWith("_warehouse.yml"));
        int fileCount = ymlFiles != null ? ymlFiles.length : 0;
        long totalSize = calculateWarehouseFilesSize();

        sb.append("§e仓库文件数量: §f").append(fileCount).append(" 个\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalSize)).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());

        return sb.toString();
    }

    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();

        plugin.getLogger().info("§e开始迁移宗门仓库数据...");
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

        // 获取所有仓库文件
        File[] warehouseFiles = sectsFolder.listFiles((dir, name) -> name.endsWith("_warehouse.yml"));
        if (warehouseFiles == null || warehouseFiles.length == 0) {
            plugin.getLogger().warning("§c未找到任何仓库YML文件");
            return report;
        }

        report.setTotalFiles(warehouseFiles.length);
        plugin.getLogger().info("§a找到 " + warehouseFiles.length + " 个仓库文件");

        // 开始迁移
        for (File warehouseFile : warehouseFiles) {
            try {
                String fileName = warehouseFile.getName();
                // 从文件名提取宗门ID (如: 1_warehouse.yml -> 1)
                String sectIdStr = fileName.replace("_warehouse.yml", "");
                int sectId = Integer.parseInt(sectIdStr);

                // 检查是否已存在
                if (!dryRun && existsInDatabase(sectId)) {
                    plugin.getLogger().info("§7跳过: " + fileName + " (已存在)");
                    report.recordSkipped();
                    continue;
                }

                // 加载仓库数据
                SectWarehouse warehouse = loadWarehouseDataFromYml(warehouseFile, sectId);
                if (warehouse == null) {
                    plugin.getLogger().warning("§c无法加载: " + fileName);
                    report.recordFailure(fileName, fileName, "无法加载YML数据");
                    continue;
                }

                // 写入数据库
                if (!dryRun) {
                    warehouseRepository.save(warehouse);
                }

                plugin.getLogger().info("§a迁移仓库数据: 宗门ID=" + sectId +
                    " (物品数: " + warehouse.getUsedSlots() + "/" + warehouse.getCapacity() + ")");
                report.addDataSize(warehouseFile.length());
                report.recordSuccess();

            } catch (Exception e) {
                String fileName = warehouseFile.getName();
                plugin.getLogger().warning("§c失败: " + fileName + " - " + e.getMessage());
                report.recordFailure(fileName, fileName, e.getMessage());
            }
        }

        report.complete();
        plugin.getLogger().info("§a宗门仓库数据迁移完成！");

        return report;
    }

    @Override
    protected long estimateTimeInMillis() {
        File[] files = sectsFolder.listFiles((dir, name) -> name.endsWith("_warehouse.yml"));
        int fileCount = files != null ? files.length : 0;
        return fileCount * 40L; // 假设每个文件40ms（包含物品反序列化）
    }

    /**
     * 从YML加载仓库数据
     */
    private SectWarehouse loadWarehouseDataFromYml(File file, int sectId) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            int capacity = config.getInt("capacity", 54);
            SectWarehouse warehouse = new SectWarehouse(sectId, capacity);

            // 加载物品数据
            if (config.contains("items")) {
                ConfigurationSection itemsSection = config.getConfigurationSection("items");

                if (itemsSection != null) {
                    for (String slotStr : itemsSection.getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);

                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemData =
                                    (Map<String, Object>) config.get("items." + slotStr);

                            if (itemData != null) {
                                ItemStack item = ItemStack.deserialize(itemData);
                                warehouse.setItem(slot, item);
                            }

                        } catch (Exception e) {
                            plugin.getLogger().warning("§e加载仓库物品失败 (宗门=" + sectId +
                                    ", 槽位=" + slotStr + "): " + e.getMessage());
                        }
                    }
                }
            }

            return warehouse;

        } catch (Exception e) {
            plugin.getLogger().severe("§c加载失败: " + file.getName() + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查仓库数据是否已存在
     */
    private boolean existsInDatabase(int sectId) {
        String sql = "SELECT COUNT(*) FROM xian_sect_warehouses WHERE sect_id = ?";

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
     * 计算仓库文件总大小
     */
    private long calculateWarehouseFilesSize() {
        if (!sectsFolder.exists() || !sectsFolder.isDirectory()) {
            return 0;
        }

        File[] files = sectsFolder.listFiles((dir, name) -> name.endsWith("_warehouse.yml"));
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
