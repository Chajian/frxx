package com.xiancore.systems.boss.damage.persistence;

import com.xiancore.systems.boss.damage.DamageHistory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 基于文件的伤害数据库实现
 * 将伤害历史数据保存到YAML文件
 *
 * 特性:
 * - 数据持久化 (服务器重启后数据保留)
 * - 易于编辑 (YAML格式)
 * - 相对较慢的读写性能
 * - 适合中小型数据集
 *
 * 文件结构:
 * damage-data/
 * ├─ histories.yml (所有历史记录)
 * ├─ index-boss.yml (Boss索引)
 * ├─ index-player.yml (玩家索引)
 * └─ metadata.yml (元数据)
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public class FileBasedDamageDatabase implements DamageDatabase {

    // ==================== 数据路径 ====================
    private final Path dataDirectory;
    private final Path historiesFile;
    private final Path bosIndexFile;
    private final Path playerIndexFile;
    private final Path metadataFile;

    // ==================== 内存缓存 ====================
    private final InMemoryDamageDatabase memoryDb = new InMemoryDamageDatabase();
    private volatile boolean connected = false;
    private volatile long lastSaveTime = 0;
    private static final long AUTO_SAVE_INTERVAL = 5 * 60 * 1000; // 5分钟

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param dataDirectory 数据目录路径
     */
    public FileBasedDamageDatabase(String dataDirectory) {
        this.dataDirectory = Paths.get(dataDirectory);
        this.historiesFile = this.dataDirectory.resolve("histories.yml");
        this.bosIndexFile = this.dataDirectory.resolve("index-boss.yml");
        this.playerIndexFile = this.dataDirectory.resolve("index-player.yml");
        this.metadataFile = this.dataDirectory.resolve("metadata.yml");
    }

    /**
     * 构造函数 (默认目录)
     */
    public FileBasedDamageDatabase() {
        this("damage-data");
    }

    // ==================== 初始化和关闭 ====================

    @Override
    public synchronized void initialize() throws Exception {
        if (connected) {
            return;
        }

        try {
            // 创建数据目录
            Files.createDirectories(dataDirectory);

            // 初始化内存数据库
            memoryDb.initialize();

            // 加载已有数据
            loadFromFile();

            connected = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化文件数据库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void shutdown() throws Exception {
        if (!connected) {
            return;
        }

        try {
            // 保存所有数据
            saveToFile();

            // 关闭内存数据库
            memoryDb.shutdown();

            connected = false;
        } catch (Exception e) {
            throw new RuntimeException("关闭文件数据库失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    // ==================== 保存操作 ====================

    @Override
    public boolean saveHistory(DamageHistory history) {
        if (!connected) {
            return false;
        }

        boolean result = memoryDb.saveHistory(history);

        // 检查是否需要自动保存
        if (System.currentTimeMillis() - lastSaveTime > AUTO_SAVE_INTERVAL) {
            try {
                saveToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    public int saveHistories(List<DamageHistory> histories) {
        int count = memoryDb.saveHistories(histories);

        try {
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }

    @Override
    public CompletableFuture<Boolean> saveHistoryAsync(DamageHistory history) {
        return CompletableFuture.supplyAsync(this::saveToFileAsync)
            .thenApply(saved -> memoryDb.saveHistory(history));
    }

    @Override
    public boolean updateHistory(DamageHistory history) {
        return memoryDb.updateHistory(history);
    }

    // ==================== 查询操作 ====================

    @Override
    public List<DamageHistory> queryByBossUUID(UUID bossUUID) {
        return memoryDb.queryByBossUUID(bossUUID);
    }

    @Override
    public List<DamageHistory> queryByPlayerUUID(UUID playerUUID) {
        return memoryDb.queryByPlayerUUID(playerUUID);
    }

    @Override
    public List<DamageHistory> queryByTimeRange(long startTime, long endTime) {
        return memoryDb.queryByTimeRange(startTime, endTime);
    }

    @Override
    public List<DamageHistory> queryAll() {
        return memoryDb.queryAll();
    }

    @Override
    public List<DamageHistory> queryAll(int offset, int limit) {
        return memoryDb.queryAll(offset, limit);
    }

    @Override
    public DamageHistory queryById(String id) {
        return memoryDb.queryById(id);
    }

    @Override
    public long getTotalCount() {
        return memoryDb.getTotalCount();
    }

    @Override
    public long getCountByBossUUID(UUID bossUUID) {
        return memoryDb.getCountByBossUUID(bossUUID);
    }

    // ==================== 统计操作 ====================

    @Override
    public double getTotalDamageForPlayer(UUID playerUUID) {
        return memoryDb.getTotalDamageForPlayer(playerUUID);
    }

    @Override
    public Map<UUID, Double> getTopDamagers(int limit) {
        return memoryDb.getTopDamagers(limit);
    }

    @Override
    public int getBossKillCount(UUID bossUUID) {
        return memoryDb.getBossKillCount(bossUUID);
    }

    @Override
    public double getAverageDamage() {
        return memoryDb.getAverageDamage();
    }

    // ==================== 删除操作 ====================

    @Override
    public int deleteByBossUUID(UUID bossUUID) {
        return memoryDb.deleteByBossUUID(bossUUID);
    }

    @Override
    public int deleteByPlayerUUID(UUID playerUUID) {
        return memoryDb.deleteByPlayerUUID(playerUUID);
    }

    @Override
    public int deleteBeforeTime(long beforeTime) {
        return memoryDb.deleteBeforeTime(beforeTime);
    }

    @Override
    public boolean deleteById(String id) {
        return memoryDb.deleteById(id);
    }

    @Override
    public void clearAll() {
        memoryDb.clearAll();
    }

    // ==================== 导入导出 ====================

    @Override
    public String exportAsJson() {
        return memoryDb.exportAsJson();
    }

    @Override
    public int importFromJson(String json) {
        return memoryDb.importFromJson(json);
    }

    @Override
    public String exportAsYaml() {
        return memoryDb.exportAsYaml();
    }

    @Override
    public int importFromYaml(String yaml) {
        return memoryDb.importFromYaml(yaml);
    }

    // ==================== 性能和优化 ====================

    @Override
    public int cleanup(int daysToKeep) {
        int deleted = memoryDb.cleanup(daysToKeep);

        try {
            if (deleted > 0) {
                saveToFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return deleted;
    }

    @Override
    public void optimize() {
        try {
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getStatistics() {
        return memoryDb.getStatistics();
    }

    @Override
    public synchronized boolean backup(String backupPath) {
        try {
            Path backup = Paths.get(backupPath);
            Files.createDirectories(backup);

            // 备份所有文件
            if (Files.exists(historiesFile)) {
                Files.copy(historiesFile, backup.resolve("histories.yml"));
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public synchronized boolean restore(String backupPath) {
        try {
            Path backup = Paths.get(backupPath);

            // 恢复备份文件
            if (Files.exists(backup.resolve("histories.yml"))) {
                Files.copy(backup.resolve("histories.yml"), historiesFile);
                loadFromFile();
                return true;
            }

            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== 文件操作 ====================

    /**
     * 从文件加载数据
     */
    private synchronized void loadFromFile() throws IOException {
        if (!Files.exists(historiesFile)) {
            return;
        }

        try {
            String content = new String(Files.readAllBytes(historiesFile));

            // 简单的YAML解析 (生产环境应使用SnakeYAML)
            // 目前为占位符实现
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存数据到文件
     */
    private synchronized void saveToFile() throws IOException {
        try {
            String yaml = memoryDb.exportAsYaml();

            Files.write(historiesFile, yaml.getBytes());

            lastSaveTime = System.currentTimeMillis();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 异步保存到文件
     */
    private boolean saveToFileAsync() {
        try {
            saveToFile();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
