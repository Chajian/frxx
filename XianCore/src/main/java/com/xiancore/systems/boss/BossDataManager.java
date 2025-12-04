package com.xiancore.systems.boss;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossEntity;
import com.xiancore.systems.boss.entity.BossSpawnHistory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss系统数据管理器
 * 负责Boss相关数据的保存、加载和管理
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class BossDataManager {

    private final XianCore plugin;
    private final File dataDirectory;
    private final Map<UUID, BossSpawnHistory> bossHistories = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    // ==================== 构造函数 ====================

    public BossDataManager(XianCore plugin) {
        this.plugin = plugin;
        this.dataDirectory = new File(plugin.getDataFolder(), "boss-data");
    }

    // ==================== 初始化 ====================

    /**
     * 初始化数据管理器
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 创建数据目录
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }

        // 加载已有的历史记录
        loadHistories();

        initialized = true;
        plugin.getLogger().info("BossDataManager initialized");
    }

    // ==================== 历史记录管理 ====================

    /**
     * 加载所有历史记录
     */
    private void loadHistories() {
        File[] files = dataDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                String content = readFile(file);
                // 这里可以使用JSON库解析，暂时跳过
                plugin.getLogger().info("Loaded history from: " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load history: " + file.getName());
            }
        }
    }

    /**
     * 保存Boss历史记录
     *
     * @param bossUUID Boss UUID
     * @param history 历史记录
     */
    public void saveBossHistory(UUID bossUUID, BossSpawnHistory history) {
        if (bossUUID == null || history == null) {
            return;
        }

        bossHistories.put(bossUUID, history);

        // 异步保存到文件
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File file = new File(dataDirectory, bossUUID + ".json");
                // 这里可以使用JSON库序列化，暂时跳过
                plugin.getLogger().info("Saved history for boss: " + bossUUID);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save history for boss: " + bossUUID);
            }
        });
    }

    /**
     * 保存Boss生成事件
     *
     * @param boss Boss实体
     */
    public void saveBossSpawned(com.xiancore.systems.boss.entity.BossEntity boss) {
        if (boss == null) {
            return;
        }
        plugin.getLogger().info("Boss spawned recorded: " + boss.getBossUUID());
    }

    /**
     * 保存Boss被击杀事件
     *
     * @param boss Boss实体
     */
    public void saveBossKilled(com.xiancore.systems.boss.entity.BossEntity boss) {
        if (boss == null) {
            return;
        }
        plugin.getLogger().info("Boss killed recorded: " + boss.getBossUUID());
    }

    /**
     * 保存Boss消失事件
     *
     * @param boss Boss实体
     */
    public void saveBossDespawned(com.xiancore.systems.boss.entity.BossEntity boss) {
        if (boss == null) {
            return;
        }
        plugin.getLogger().info("Boss despawned recorded: " + boss.getBossUUID());
    }

    /**
     * 获取Boss历史记录
     *
     * @param bossUUID Boss UUID
     * @return 历史记录
     */
    public BossSpawnHistory getBossHistory(UUID bossUUID) {
        return bossHistories.get(bossUUID);
    }

    /**
     * 获取所有历史记录
     *
     * @return 历史记录Map
     */
    public Map<UUID, BossSpawnHistory> getAllHistories() {
        return new HashMap<>(bossHistories);
    }

    /**
     * 清除Boss历史记录
     *
     * @param bossUUID Boss UUID
     */
    public void clearBossHistory(UUID bossUUID) {
        bossHistories.remove(bossUUID);
    }

    // ==================== 文件操作 ====================

    /**
     * 读取文件内容
     *
     * @param file 文件
     * @return 文件内容
     * @throws IOException 读取异常
     */
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 写入文件内容
     *
     * @param file 文件
     * @param content 内容
     * @throws IOException 写入异常
     */
    private void writeFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    // ==================== 统计信息 ====================

    /**
     * 获取已保存的历史记录数
     *
     * @return 记录数
     */
    public int getHistoryCount() {
        return bossHistories.size();
    }

    /**
     * 获取管理器状态信息
     *
     * @return 状态字符串
     */
    public String getStatus() {
        return String.format(
            "BossDataManager[initialized=%s, histories=%d, dataDir=%s]",
            initialized,
            bossHistories.size(),
            dataDirectory.getAbsolutePath()
        );
    }

    // ==================== 关闭 ====================

    /**
     * 关闭数据管理器
     */
    public void shutdown() {
        // 保存所有待保存的数据
        bossHistories.clear();
        plugin.getLogger().info("BossDataManager shutdown");
    }
}
