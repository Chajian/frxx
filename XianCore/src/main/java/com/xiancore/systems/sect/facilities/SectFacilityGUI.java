package com.xiancore.systems.sect.facilities;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 宗门设施管理 GUI
 * 显示所有设施的状态，允许升级
 * 使用 InventoryFramework 统一 GUI 框架
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 统一使用 IF 框架
 */
public class SectFacilityGUI {

    private final XianCore plugin;

    public SectFacilityGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开设施管理界面
     */
    public void open(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return;
        }

        Integer sectId = data.getSectId();
        if (sectId == null) {
            player.sendMessage("§c你还没有加入宗门!");
            return;
        }

        Sect sect = plugin.getSectSystem().getSect(sectId);
        if (sect == null) {
            player.sendMessage("§c宗门不存在!");
            return;
        }

        SectRank sectRank = SectRank.fromRankString(data.getSectRank());
        boolean canUpgrade = sectRank != null && sectRank.hasManagePermission();

        ChestGui gui = new ChestGui(6, "§b§l宗门设施管理");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        SectFacilityData facilityData = plugin.getSectSystem().getFacilityManager().getFacilityData(sectId);

        // 设施物品
        addFacilityItem(contentPane, player, 2, 1, SectFacility.SPIRITUAL_VEIN, facilityData, canUpgrade, sectId);
        addFacilityItem(contentPane, player, 4, 1, SectFacility.FORGE_ALTAR, facilityData, canUpgrade, sectId);
        addFacilityItem(contentPane, player, 6, 1, SectFacility.SCRIPTURE_PAVILION, facilityData, canUpgrade, sectId);
        addFacilityItem(contentPane, player, 2, 3, SectFacility.SECT_WAREHOUSE, facilityData, canUpgrade, sectId);
        addFacilityItem(contentPane, player, 6, 3, SectFacility.SECT_SHOP, facilityData, canUpgrade, sectId);

        // 宗门信息
        contentPane.addItem(new GuiItem(createSectInfoItem(sect)), 4, 5);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 添加设施物品
     */
    private void addFacilityItem(StaticPane pane, Player player, int x, int y,
                                  SectFacility facility, SectFacilityData data,
                                  boolean canUpgrade, int sectId) {
        ItemStack item = createFacilityItem(facility, data, canUpgrade);
        pane.addItem(new GuiItem(item, event -> {
            if (!canUpgrade) {
                player.sendMessage("§c你没有权限升级设施!");
                return;
            }

            if (data.isMaxLevel(facility)) {
                player.sendMessage("§6该设施已达最高等级!");
                return;
            }

            if (plugin.getSectSystem().getFacilityManager().upgradeFacility(sectId, facility, player)) {
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 2L);
            }
        }), x, y);
    }

    /**
     * 创建设施物品
     */
    private ItemStack createFacilityItem(SectFacility facility, SectFacilityData data, boolean canUpgrade) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7功能: " + facility.getDescription());
        lore.add("");

        int currentLevel = data.getLevel(facility);
        lore.add("§7当前等级: §e" + currentLevel + " §7/ §f" + facility.getMaxLevel());

        if (currentLevel > 0) {
            lore.add("§7当前效果: " + facility.getFormattedBonus(currentLevel));
        } else {
            lore.add("§c未建造");
        }

        lore.add("");

        if (!data.isMaxLevel(facility)) {
            int nextLevel = currentLevel + 1;
            long upgradeCost = facility.getUpgradeCost(nextLevel);

            lore.add("§7下一级效果: " + facility.getFormattedBonus(nextLevel));
            lore.add("§7升级消耗: §6" + upgradeCost + " 灵石");
            lore.add("");

            if (canUpgrade) {
                lore.add("§e点击升级");
            } else {
                lore.add("§c需要宗主/长老权限");
            }
        } else {
            lore.add("§a§l✔ 已达最高等级");
        }

        return new ItemBuilder(facility.getIcon())
                .name(facility.getDisplayName())
                .lore(lore)
                .build();
    }

    /**
     * 创建宗门信息物品
     */
    private ItemStack createSectInfoItem(Sect sect) {
        return new ItemBuilder(Material.NETHER_STAR)
                .name("§e§l" + sect.getName())
                .lore(
                        "",
                        "§7宗主: §f" + sect.getOwnerName(),
                        "§7成员数: §f" + sect.getMembers().size() + " §7/ §f" + sect.getMaxMembers(),
                        "§7宗门灵石: §6" + sect.getSectFunds(),
                        "",
                        "§7宗门等级: §e" + sect.getLevel(),
                        "§7宗门经验: §b" + sect.getExperience()
                )
                .glow()
                .build();
    }
}
