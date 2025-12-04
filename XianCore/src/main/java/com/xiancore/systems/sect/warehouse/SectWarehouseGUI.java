package com.xiancore.systems.sect.warehouse;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 宗门仓库 GUI
 * 提供共享物品存储界面
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectWarehouseGUI implements Listener {

    private final XianCore plugin;
    private static final String TITLE_PREFIX = "\u00a76\u00a7l\u5b97\u95e8\u4ed3\u5e93";

    // 追踪打开仓库的玩家 (玩家UUID -> 宗门ID)
    private final Map<UUID, Integer> openWarehouses = new HashMap<>();

    public SectWarehouseGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开仓库界面
     */
    public void open(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("\u00a7c\u6570\u636e\u52a0\u8f7d\u5931\u8d25!");
            return;
        }

        // 检查玩家是否在宗门
        Integer sectId = data.getSectId();
        if (sectId == null) {
            player.sendMessage("\u00a7c\u4f60\u8fd8\u6ca1\u6709\u52a0\u5165\u5b97\u95e8!");
            return;
        }

        Sect sect = plugin.getSectSystem().getSect(sectId);
        if (sect == null) {
            player.sendMessage("\u00a7c\u5b97\u95e8\u4e0d\u5b58\u5728!");
            return;
        }

        // 检查仓库是否已建造
        int warehouseLevel = plugin.getSectSystem().getFacilityManager()
                .getFacilityData(sectId)
                .getLevel(com.xiancore.systems.sect.facilities.SectFacility.SECT_WAREHOUSE);

        if (warehouseLevel == 0) {
            player.sendMessage("\u00a7c\u5b97\u95e8\u8fd8\u6ca1\u6709\u5efa\u9020\u4ed3\u5e93!");
            player.sendMessage("\u00a77\u8bf7\u8054\u7cfb\u5b97\u4e3b\u6216\u957f\u8001\u5347\u7ea7\u8bbe\u65bd");
            return;
        }

        // 获取仓库容量
        int capacity = plugin.getSectSystem().getFacilityManager().getWarehouseCapacity(sectId);

        // 获取仓库数据
        SectWarehouse warehouse = plugin.getSectSystem().getWarehouseManager().getWarehouse(sectId);

        // 创建界面
        String title = TITLE_PREFIX + " \u00a7f- \u00a7e" + sect.getName();
        Inventory inventory = Bukkit.createInventory(null, capacity, title);

        // 填充物品
        for (Map.Entry<Integer, ItemStack> entry : warehouse.getAllItems().entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < capacity) {
                inventory.setItem(slot, entry.getValue());
            }
        }

        // 记录打开的仓库
        openWarehouses.put(player.getUniqueId(), sectId);

        player.openInventory(inventory);
        player.sendMessage("\u00a7a\u5df2\u6253\u5f00\u5b97\u95e8\u4ed3\u5e93 \u00a77(\u5bb9\u91cf: " + capacity + "\u683c)");
    }

    /**
     * 处理点击事件
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // 检查是否是仓库界面
        if (!title.startsWith(TITLE_PREFIX)) {
            return;
        }

        // 检查是否记录了打开的仓库
        Integer sectId = openWarehouses.get(player.getUniqueId());
        if (sectId == null) {
            return;
        }

        // 允许所有宗门成员存取物品
        // 可以根据需要添加权限检查
    }

    /**
     * 处理关闭事件
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        // 检查是否是仓库界面
        if (!title.startsWith(TITLE_PREFIX)) {
            return;
        }

        // 获取宗门ID
        Integer sectId = openWarehouses.remove(player.getUniqueId());
        if (sectId == null) {
            return;
        }

        // 保存仓库内容
        SectWarehouse warehouse = plugin.getSectSystem().getWarehouseManager().getWarehouse(sectId);
        int capacity = plugin.getSectSystem().getFacilityManager().getWarehouseCapacity(sectId);

        Inventory inventory = event.getInventory();

        // 清空仓库数据
        warehouse.clear();

        // 保存新内容
        for (int i = 0; i < capacity && i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                warehouse.setItem(i, item);
            }
        }

        // 保存到数据库
        plugin.getSectSystem().getWarehouseManager().saveWarehouse(warehouse);

        player.sendMessage("\u00a7a\u4ed3\u5e93\u5df2\u4fdd\u5b58!");
    }

    /**
     * 清理玩家追踪
     */
    public void cleanup(UUID playerId) {
        openWarehouses.remove(playerId);
    }
}
