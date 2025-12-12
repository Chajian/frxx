package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.ItemSelectionService;
import com.xiancore.systems.forge.ItemSelectionService.EquipmentSlotInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备选择GUI
 * 从玩家背包中选择要强化的装备
 * 业务逻辑委托给 ItemSelectionService
 *
 * @author Olivia Diaz
 * @version 3.0.0 - 使用 Service 层分离业务逻辑
 */
public class EquipmentSelectionGUI {

    private final XianCore plugin;
    private final Player player;
    private final ItemSelectionService selectionService;

    public EquipmentSelectionGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.selectionService = new ItemSelectionService();
    }

    /**
     * 打开装备选择界面
     */
    public static void open(Player player, XianCore plugin) {
        new EquipmentSelectionGUI(plugin, player).show();
    }

    private void show() {
        List<EquipmentSlotInfo> equipments = selectionService.scanEquipments(player);

        if (equipments.isEmpty()) {
            showNoEquipmentMessage();
            return;
        }

        int rows = Math.max(3, (equipments.size() + 8) / 9 + 1);
        ChestGui gui = new ChestGui(rows, "§b§l选择要强化的装备");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(gui, rows);

        StaticPane contentPane = new StaticPane(0, 0, 9, rows);

        int slot = 0;
        for (EquipmentSlotInfo info : equipments) {
            if (slot >= (rows - 1) * 9) break;

            int row = slot / 9;
            int col = slot % 9;

            ItemStack equipmentItem = createEquipmentDisplayItem(info);

            final EquipmentSlotInfo finalInfo = info;
            contentPane.addItem(new GuiItem(equipmentItem, event -> {
                EnhanceGUI.open(player, plugin, finalInfo.getItemStack(), finalInfo.getSlot());
            }), col, row);

            slot++;
        }

        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .lore("§7返回炼器主界面")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> {
            player.closeInventory();
            ForgeGUI.open(player, plugin);
        }), 4, rows - 1);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 创建装备显示物品
     */
    private ItemStack createEquipmentDisplayItem(EquipmentSlotInfo info) {
        ItemStack displayItem = info.getItemStack().clone();

        var meta = displayItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§7槽位: §f" + selectionService.getSlotDescription(info.getSlot()));
            lore.add("§7当前强化: §6+" + info.getEnhanceLevel());
            lore.add("");

            if (info.isMaxEnhanced()) {
                lore.add("§6已达最大强化等级");
            } else {
                lore.add("§a§l点击选择此装备进行强化");
            }

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    /**
     * 显示无装备提示
     */
    private void showNoEquipmentMessage() {
        ChestGui gui = new ChestGui(3, "§b§l选择装备");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addBackground(gui, 3);

        StaticPane contentPane = new StaticPane(0, 0, 9, 3);

        // 提示信息
        ItemStack infoItem = new ItemBuilder(Material.BARRIER)
                .name("§c§l背包中没有可强化的装备")
                .lore(
                        "§7您需要先炼制装备才能强化",
                        "",
                        "§e如何获得装备:",
                        "§7使用 §a/forge refine §7精炼胚胎",
                        "§7或使用 §a/forge make §7配方炼制",
                        "",
                        "§7只有仙家装备可以强化",
                        "§7强化可以提升装备属性"
                )
                .build();
        contentPane.addItem(new GuiItem(infoItem), 4, 1);

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> {
            player.closeInventory();
            ForgeGUI.open(player, plugin);
        }), 4, 2);

        gui.addPane(contentPane);
        gui.show(player);
    }
}

