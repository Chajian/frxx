package com.xiancore.systems.sect.warehouse;

import com.xiancore.XianCore;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 宗门仓库管理器
 * 负责管理所有宗门的仓库数据
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SectWarehouseManager {

    private final XianCore plugin;

    // 仓库数据缓存 (宗门ID -> 仓库数据)
    private final Map<Integer, SectWarehouse> warehouseCache;

    public SectWarehouseManager(XianCore plugin) {
        this.plugin = plugin;
        this.warehouseCache = new ConcurrentHashMap<>();
    }

    /**
     * 初始化管理器
     */
    public void initialize() {
        plugin.getLogger().info("  \u00a7a\u2713 \u5b97\u95e8\u4ed3\u5e93\u7ba1\u7406\u5668\u521d\u59cb\u5316\u5b8c\u6210");
    }

    /**
     * 获取宗门仓库
     *
     * @param sectId 宗门ID
     * @return 仓库数据
     */
    public SectWarehouse getWarehouse(int sectId) {
        return warehouseCache.computeIfAbsent(sectId, id -> {
            // 尝试从文件加载
            SectWarehouse warehouse = plugin.getDataManager().loadWarehouseFromFile(id);
            
            if (warehouse != null) {
                plugin.getLogger().fine("§7从文件加载仓库数据: " + id);
                return warehouse;
            }
            
            // 文件不存在，创建新仓库
            int capacity = plugin.getSectSystem().getFacilityManager().getWarehouseCapacity(id);
            plugin.getLogger().fine("§7创建新仓库: " + id + " (容量=" + capacity + ")");
            return new SectWarehouse(id, capacity);
        });
    }

    /**
     * 保存仓库数据
     *
     * @param warehouse 仓库数据
     */
    public void saveWarehouse(SectWarehouse warehouse) {
        // 更新缓存
        warehouseCache.put(warehouse.getSectId(), warehouse);
        
        // 保存到文件
        plugin.getDataManager().saveWarehouseToFile(warehouse);
    }

    /**
     * 移除仓库（宗门解散时调用）
     *
     * @param sectId 宗门ID
     */
    public void removeWarehouse(int sectId) {
        warehouseCache.remove(sectId);
        
        // 删除文件
        java.io.File file = new java.io.File(plugin.getDataFolder(), "sects/" + sectId + "_warehouse.yml");
        if (file.exists()) {
            if (file.delete()) {
                plugin.getLogger().info("§7删除仓库数据文件: " + sectId);
            } else {
                plugin.getLogger().warning("§e删除仓库数据文件失败: " + sectId);
            }
        }
    }
    
    /**
     * 保存所有仓库数据
     */
    public void saveAll() {
        int count = 0;
        for (SectWarehouse warehouse : warehouseCache.values()) {
            plugin.getDataManager().saveWarehouseToFile(warehouse);
            count++;
        }
        
        if (count > 0) {
            plugin.getLogger().info("§7保存了 " + count + " 个宗门仓库数据");
        }
    }

    /**
     * 升级仓库容量
     * 当仓库设施升级时调用
     *
     * @param sectId 宗门ID
     * @param newCapacity 新容量
     */
    public void upgradeCapacity(int sectId, int newCapacity) {
        SectWarehouse warehouse = getWarehouse(sectId);
        warehouse.setCapacity(newCapacity);
        saveWarehouse(warehouse);
    }
}
