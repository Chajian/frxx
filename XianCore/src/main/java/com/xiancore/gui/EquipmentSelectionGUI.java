package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.items.Equipment;
import com.xiancore.systems.forge.items.EquipmentParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 装备选择GUI
 * 从玩家背包中选择要强化的装备
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class EquipmentSelectionGUI {

    private final XianCore plugin;
    private final Player player;

    public EquipmentSelectionGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * 打开装备选择界面
     */
    public static void open(Player player, XianCore plugin) {
        new EquipmentSelectionGUI(plugin, player).show();
    }

    private void show() {
        // 扫描背包中的装备
        List<EquipmentSlotInfo> equipments = scanPlayerInventory();

        // 如果没有装备，显示提示
        if (equipments.isEmpty()) {
            showNoEquipmentMessage();
            return;
        }

        // 计算需要的行数（每行9个，至少3行）
        int rows = Math.max(3, (equipments.size() + 8) / 9 + 1);
        ChestGui gui = new ChestGui(rows, "§b§l选择要强化的装备");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        OutlinePane background = new OutlinePane(0, 0, 9, rows);
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        StaticPane contentPane = new StaticPane(0, 0, 9, rows);

        // 显示装备列表
        int slot = 0;
        for (EquipmentSlotInfo info : equipments) {
            if (slot >= (rows - 1) * 9) break;  // 最后一行留给返回按钮

            int row = slot / 9;
            int col = slot % 9;

            // 创建装备显示物品
            ItemStack equipmentItem = createEquipmentDisplayItem(info);

            // 保存引用以便在事件中使用
            final EquipmentSlotInfo finalInfo = info;
            contentPane.addItem(new GuiItem(equipmentItem, event -> {
                // 打开强化界面
                EnhanceGUI.open(player, plugin, finalInfo.itemStack, finalInfo.slot);
            }), col, row);

            slot++;
        }

        // 返回按钮（放在最后一行中间）
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
     * 扫描玩家背包中的装备
     */
    private List<EquipmentSlotInfo> scanPlayerInventory() {
        List<EquipmentSlotInfo> equipments = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && EquipmentParser.isEquipment(item)) {
                Equipment equipment = EquipmentParser.parseFromItemStack(item);
                if (equipment != null) {
                    equipments.add(new EquipmentSlotInfo(equipment, i, item));
                }
            }
        }

        return equipments;
    }

    /**
     * 创建装备显示物品
     */
    private ItemStack createEquipmentDisplayItem(EquipmentSlotInfo info) {
        Equipment equipment = info.equipment;
        ItemStack originalItem = info.itemStack;

        // 使用原始物品作为基础
        ItemStack displayItem = originalItem.clone();

        // 添加点击提示
        var meta = displayItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§7槽位: §f" + (info.slot < 9 ? "快捷栏" : "背包") + " #" + (info.slot % 9 + 1));
            int enhanceLevel = EquipmentParser.getEnhanceLevel(originalItem);
            lore.add("§7当前强化: §6+" + enhanceLevel);
            lore.add("");
            
            // 显示是否可以强化
            if (enhanceLevel >= 20) {
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
        OutlinePane background = new OutlinePane(0, 0, 9, 3);
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

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

    /**
     * 装备槽位信息
     */
    private static class EquipmentSlotInfo {
        final Equipment equipment;
        final int slot;
        final ItemStack itemStack;

        EquipmentSlotInfo(Equipment equipment, int slot, ItemStack itemStack) {
            this.equipment = equipment;
            this.slot = slot;
            this.itemStack = itemStack;
        }
    }
}

