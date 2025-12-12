package com.xiancore.core.data.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xiancore.XianCore;
import com.xiancore.core.data.DatabaseManager;
import com.xiancore.systems.sect.warehouse.SectWarehouse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 仓库数据仓储
 * 负责仓库数据的加载、保存（支持 YAML/MySQL 双模式）
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class WarehouseRepository {

    private final XianCore plugin;
    private final DatabaseManager databaseManager;
    private final Gson gson;

    // 重试配置
    private static final int RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 100;

    // SQL 常量
    private static final String SQL_SELECT =
            "SELECT capacity, items_json, last_modified FROM xian_sect_warehouses WHERE sect_id = ?";

    private static final String SQL_UPSERT =
            "INSERT INTO xian_sect_warehouses (sect_id, capacity, items_json, last_modified) " +
            "VALUES (?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE capacity = VALUES(capacity), items_json = VALUES(items_json), " +
            "last_modified = VALUES(last_modified)";

    public WarehouseRepository(XianCore plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDataManager().getDatabaseManager();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // ==================== 公开 API ====================

    /**
     * 保存仓库数据
     *
     * @param warehouse 仓库
     */
    public void save(SectWarehouse warehouse) {
        if (databaseManager.isUseMySql()) {
            saveToDatabase(warehouse);
        } else {
            saveToFile(warehouse);
        }
    }

    /**
     * 加载仓库数据
     *
     * @param sectId 宗门 ID
     * @return 仓库数据，如果不存在返回 null
     */
    public SectWarehouse load(int sectId) {
        return databaseManager.isUseMySql()
                ? loadFromDatabase(sectId)
                : loadFromFile(sectId);
    }

    // ==================== MySQL 实现 ====================

    private SectWarehouse loadFromDatabase(int sectId) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT)) {

            pstmt.setInt(1, sectId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int capacity = rs.getInt("capacity");
                    String itemsJson = rs.getString("items_json");

                    SectWarehouse warehouse = new SectWarehouse(sectId, capacity);

                    // 反序列化物品数据
                    if (itemsJson != null && !itemsJson.isEmpty()) {
                        try {
                            Map<Integer, Map<String, Object>> itemsData = deserializeItems(itemsJson);

                            for (Map.Entry<Integer, Map<String, Object>> entry : itemsData.entrySet()) {
                                try {
                                    ItemStack item = ItemStack.deserialize(entry.getValue());
                                    warehouse.setItem(entry.getKey(), item);
                                } catch (Exception e) {
                                    plugin.getLogger().warning("§e反序列化物品失败 (宗门=" + sectId +
                                            ", 槽位=" + entry.getKey() + "): " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("§e解析仓库物品JSON失败: 宗门ID=" + sectId);
                            e.printStackTrace();
                        }
                    }

                    return warehouse;
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("§e从数据库加载仓库数据失败: 宗门ID=" + sectId);
            e.printStackTrace();
        }

        return null;
    }

    private void saveToDatabase(SectWarehouse warehouse) {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL_UPSERT)) {

            // 序列化物品数据
            String itemsJson = serializeItems(warehouse.getAllItems());

            pstmt.setInt(1, warehouse.getSectId());
            pstmt.setInt(2, warehouse.getCapacity());
            pstmt.setString(3, itemsJson);
            pstmt.setLong(4, System.currentTimeMillis());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().warning("§e保存仓库数据到数据库失败: 宗门ID=" + warehouse.getSectId());
            e.printStackTrace();
        }
    }

    /**
     * 序列化物品数据为 JSON
     *
     * @param items 物品 Map
     * @return JSON 字符串
     */
    private String serializeItems(Map<Integer, ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return "{}";
        }

        // 转换为可序列化的格式
        Map<Integer, Map<String, Object>> serializableItems = new HashMap<>();

        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            if (entry.getValue() != null) {
                serializableItems.put(entry.getKey(), entry.getValue().serialize());
            }
        }

        return gson.toJson(serializableItems);
    }

    /**
     * 从 JSON 反序列化物品数据
     *
     * @param json JSON 字符串
     * @return 物品 Map
     */
    private Map<Integer, Map<String, Object>> deserializeItems(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            Type type = new TypeToken<Map<Integer, Map<String, Object>>>() {}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            plugin.getLogger().warning("§eJSON 反序列化失败: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // ==================== YAML 实现 ====================

    private SectWarehouse loadFromFile(int sectId) {
        File file = new File(plugin.getDataFolder(), "sects/" + sectId + "_warehouse.yml");

        if (!file.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            int capacity = config.getInt("capacity", 27);
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
            plugin.getLogger().warning("§e从文件加载仓库数据失败: 宗门ID=" + sectId);
            e.printStackTrace();
            return null;
        }
    }

    private void saveToFile(SectWarehouse warehouse) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            sectDir.mkdirs();
        }

        File file = new File(sectDir, warehouse.getSectId() + "_warehouse.yml");

        // 重试机制
        Exception lastException = null;

        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                YamlConfiguration config = new YamlConfiguration();

                config.set("sect_id", warehouse.getSectId());
                config.set("capacity", warehouse.getCapacity());

                // 保存物品数据
                for (Map.Entry<Integer, ItemStack> entry : warehouse.getAllItems().entrySet()) {
                    int slot = entry.getKey();
                    ItemStack item = entry.getValue();

                    String itemPath = "items." + slot;
                    config.set(itemPath, item.serialize());
                }

                config.save(file);
                return; // 成功，退出

            } catch (Exception e) {
                lastException = e;
                plugin.getLogger().warning("§e保存仓库数据失败，重试 " + (i + 1) + "/" + RETRY_COUNT +
                        ": 宗门ID=" + warehouse.getSectId());

                // 等待后重试
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // 所有重试都失败
        plugin.getLogger().severe("§c保存仓库数据彻底失败: 宗门ID=" + warehouse.getSectId());
        if (lastException != null) {
            lastException.printStackTrace();
        }
    }
}
