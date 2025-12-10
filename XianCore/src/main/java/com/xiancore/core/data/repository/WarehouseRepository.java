package com.xiancore.core.data.repository;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.warehouse.SectWarehouse;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Map;

/**
 * 仓库数据仓储
 * 负责仓库数据的加载、保存（仅支持文件存储）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class WarehouseRepository {

    private final XianCore plugin;

    // 重试配置
    private static final int RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 100;

    public WarehouseRepository(XianCore plugin) {
        this.plugin = plugin;
    }

    // ==================== 公开 API ====================

    /**
     * 保存仓库数据
     *
     * @param warehouse 仓库
     */
    public void save(SectWarehouse warehouse) {
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
                        ": " + warehouse.getSectId());

                // 等待后重试
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // 所有重试都失败
        plugin.getLogger().severe("§c保存仓库数据彻底失败: " + warehouse.getSectId());
        if (lastException != null) {
            lastException.printStackTrace();
        }
    }

    /**
     * 加载仓库数据
     *
     * @param sectId 宗门 ID
     * @return 仓库数据，如果不存在返回 null
     */
    public SectWarehouse load(int sectId) {
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
            plugin.getLogger().warning("§e加载仓库数据失败: " + sectId);
            e.printStackTrace();
            return null;
        }
    }
}
