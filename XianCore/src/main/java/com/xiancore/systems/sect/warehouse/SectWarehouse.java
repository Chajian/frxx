package com.xiancore.systems.sect.warehouse;

import lombok.Data;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 宗门仓库数据类
 * 存储宗门的共享物品
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class SectWarehouse {

    private int sectId;                              // 宗门ID
    private Map<Integer, ItemStack> items;           // 物品存储 (槽位 -> 物品)
    private int capacity;                            // 仓库容量（格数）

    /**
     * 构造函数
     */
    public SectWarehouse(int sectId, int capacity) {
        this.sectId = sectId;
        this.capacity = capacity;
        this.items = new HashMap<>();
    }

    /**
     * 获取物品
     */
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    /**
     * 设置物品
     */
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < capacity) {
            if (item == null || item.getType().isAir()) {
                items.remove(slot);
            } else {
                items.put(slot, item.clone());
            }
        }
    }

    /**
     * 移除物品
     */
    public void removeItem(int slot) {
        items.remove(slot);
    }

    /**
     * 清空仓库
     */
    public void clear() {
        items.clear();
    }

    /**
     * 获取已使用的槽位数量
     */
    public int getUsedSlots() {
        return items.size();
    }

    /**
     * 检查槽位是否为空
     */
    public boolean isSlotEmpty(int slot) {
        return !items.containsKey(slot);
    }

    /**
     * 获取所有物品的副本
     */
    public Map<Integer, ItemStack> getAllItems() {
        Map<Integer, ItemStack> copy = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public int getSectId() {
        return sectId;
    }

    public Map<Integer, ItemStack> getItems() {
        return items;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setSectId(int sectId) {
        this.sectId = sectId;
    }

    public void setItems(Map<Integer, ItemStack> items) {
        this.items = items;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
