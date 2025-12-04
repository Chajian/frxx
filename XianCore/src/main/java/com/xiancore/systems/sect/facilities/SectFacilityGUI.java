package com.xiancore.systems.sect.facilities;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 宗门设施管理 GUI
 * 显示所有设施的状态，允许升级
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectFacilityGUI implements Listener {

    private final XianCore plugin;
    private static final String TITLE = "\u00a7b\u00a7l\u5b97\u95e8\u8bbe\u65bd\u7ba1\u7406";

    public SectFacilityGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开设施管理界面
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

        // 检查权限（只有宗主和长老可以升级）
        SectRank sectRank = SectRank.fromRankString(data.getSectRank());
        boolean canUpgrade = sectRank != null && sectRank.hasManagePermission();

        // 创建 54 格 GUI
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);

        // 填充背景
        fillBackground(inventory);

        // 设置设施物品
        setFacilityItems(inventory, sectId, canUpgrade);

        // 设置宗门信息显示
        setSectInfo(inventory, sect);

        player.openInventory(inventory);
    }

    /**
     * 填充背景
     */
    private void fillBackground(Inventory inventory) {
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = bg.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            bg.setItemMeta(meta);
        }

        // 填充边框
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, bg);
            }
        }
    }

    /**
     * 设置设施物品
     */
    private void setFacilityItems(Inventory inventory, int sectId, boolean canUpgrade) {
        SectFacilityData data = plugin.getSectSystem().getFacilityManager().getFacilityData(sectId);

        // 灵脉 (槽位 11)
        setFacilityItem(inventory, 11, SectFacility.SPIRITUAL_VEIN, data, canUpgrade);

        // 炼器台 (槽位 13)
        setFacilityItem(inventory, 13, SectFacility.FORGE_ALTAR, data, canUpgrade);

        // 藏经阁 (槽位 15)
        setFacilityItem(inventory, 15, SectFacility.SCRIPTURE_PAVILION, data, canUpgrade);

        // 宗门仓库 (槽位 29)
        setFacilityItem(inventory, 29, SectFacility.SECT_WAREHOUSE, data, canUpgrade);

        // 宗门商店 (槽位 33)
        setFacilityItem(inventory, 33, SectFacility.SECT_SHOP, data, canUpgrade);
    }

    /**
     * 设置单个设施物品
     */
    private void setFacilityItem(Inventory inventory, int slot, SectFacility facility,
                                   SectFacilityData data, boolean canUpgrade) {
        ItemStack item = new ItemStack(facility.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(facility.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u529f\u80fd: " + facility.getDescription());
            lore.add("");

            int currentLevel = data.getLevel(facility);
            lore.add("\u00a77\u5f53\u524d\u7b49\u7ea7: \u00a7e" + currentLevel + " \u00a77/ \u00a7f" + facility.getMaxLevel());

            if (currentLevel > 0) {
                lore.add("\u00a77\u5f53\u524d\u6548\u679c: " + facility.getFormattedBonus(currentLevel));
            } else {
                lore.add("\u00a7c\u672a\u5efa\u9020");
            }

            lore.add("");

            if (!data.isMaxLevel(facility)) {
                int nextLevel = currentLevel + 1;
                long upgradeCost = facility.getUpgradeCost(nextLevel);

                lore.add("\u00a77\u4e0b\u4e00\u7ea7\u6548\u679c: " + facility.getFormattedBonus(nextLevel));
                lore.add("\u00a77\u5347\u7ea7\u6d88\u8017: \u00a76" + upgradeCost + " \u7075\u77f3");
                lore.add("");

                if (canUpgrade) {
                    lore.add("\u00a7e\u70b9\u51fb\u5347\u7ea7");
                } else {
                    lore.add("\u00a7c\u9700\u8981\u5b97\u4e3b/\u957f\u8001\u6743\u9650");
                }
            } else {
                lore.add("\u00a7a\u00a7l\u2714 \u5df2\u8fbe\u6700\u9ad8\u7b49\u7ea7");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    /**
     * 设置宗门信息显示
     */
    private void setSectInfo(Inventory inventory, Sect sect) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7e\u00a7l" + sect.getName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u5b97\u4e3b: \u00a7f" + sect.getOwnerName());
            lore.add("\u00a77\u6210\u5458\u6570: \u00a7f" + sect.getMembers().size() + " \u00a77/ \u00a7f" + sect.getMaxMembers());
            lore.add("\u00a77\u5b97\u95e8\u7075\u77f3: \u00a76" + sect.getSectFunds());
            lore.add("");
            lore.add("\u00a77\u5b97\u95e8\u7b49\u7ea7: \u00a7e" + sect.getLevel());
            lore.add("\u00a77\u5b97\u95e8\u7ecf\u9a8c: \u00a7b" + sect.getExperience());

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(49, item);
    }

    /**
     * 处理点击事件
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!event.getView().getTitle().equals(TITLE)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || data.getSectId() == null) {
            return;
        }

        // 检查权限
        SectRank sectRank = SectRank.fromRankString(data.getSectRank());
        if (sectRank == null || !sectRank.hasManagePermission()) {
            player.sendMessage("§c你没有权限升级设施!");
            return;
        }

        // 根据点击的槽位确定设施类型
        SectFacility facility = null;
        int slot = event.getSlot();

        if (slot == 11) {
            facility = SectFacility.SPIRITUAL_VEIN;
        } else if (slot == 13) {
            facility = SectFacility.FORGE_ALTAR;
        } else if (slot == 15) {
            facility = SectFacility.SCRIPTURE_PAVILION;
        } else if (slot == 29) {
            facility = SectFacility.SECT_WAREHOUSE;
        } else if (slot == 33) {
            facility = SectFacility.SECT_SHOP;
        }

        if (facility != null) {
            // 尝试升级
            if (plugin.getSectSystem().getFacilityManager().upgradeFacility(data.getSectId(), facility, player)) {
                // 升级成功，刷新界面
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 2L);
            }
        }
    }
}
