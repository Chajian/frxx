package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.items.Embryo;
import com.xiancore.systems.forge.items.EmbryoParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 胚胎选择GUI
 * 从玩家背包中选择要精炼的胚胎
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class EmbryoSelectionGUI {

    private final XianCore plugin;
    private final Player player;

    public EmbryoSelectionGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * 打开胚胎选择界面
     */
    public static void open(Player player, XianCore plugin) {
        new EmbryoSelectionGUI(plugin, player).show();
    }

    private void show() {
        // 扫描背包中的胚胎
        List<EmbryoSlotInfo> embryos = scanPlayerInventory();

        // 如果没有胚胎，显示提示
        if (embryos.isEmpty()) {
            showNoEmbryoMessage();
            return;
        }

        // 计算需要的行数（每行9个，至少3行）
        int rows = Math.max(3, (embryos.size() + 8) / 9 + 1);
        ChestGui gui = new ChestGui(rows, "§6§l选择要精炼的胚胎");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        OutlinePane background = new OutlinePane(0, 0, 9, rows);
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        StaticPane contentPane = new StaticPane(0, 0, 9, rows);

        // 显示胚胎列表
        int slot = 0;
        for (EmbryoSlotInfo info : embryos) {
            if (slot >= (rows - 1) * 9) break;  // 最后一行留给返回按钮

            int row = slot / 9;
            int col = slot % 9;

            // 创建胚胎显示物品
            ItemStack embryoItem = createEmbryoDisplayItem(info);
            
            // 保存引用以便在事件中使用
            final EmbryoSlotInfo finalInfo = info;
            contentPane.addItem(new GuiItem(embryoItem, event -> {
                // 打开精炼界面
                EquipmentCraftGUI.open(player, plugin, finalInfo.embryo, finalInfo.slot);
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
     * 扫描玩家背包中的胚胎
     */
    private List<EmbryoSlotInfo> scanPlayerInventory() {
        List<EmbryoSlotInfo> embryos = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && EmbryoParser.isEmbryo(item)) {
                Embryo embryo = EmbryoParser.parseFromItemStack(item);
                if (embryo != null) {
                    embryos.add(new EmbryoSlotInfo(embryo, i, item));
                }
            }
        }

        return embryos;
    }

    /**
     * 创建胚胎显示物品
     */
    private ItemStack createEmbryoDisplayItem(EmbryoSlotInfo info) {
        Embryo embryo = info.embryo;
        ItemStack originalItem = info.itemStack;

        // 使用原始物品作为基础
        ItemStack displayItem = originalItem.clone();

        // 添加点击提示
        var meta = displayItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§7槽位: §f" + (info.slot < 9 ? "快捷栏" : "背包") + " #" + (info.slot % 9 + 1));
            lore.add("");
            lore.add("§a§l点击选择此胚胎进行精炼");
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    /**
     * 显示无胚胎提示
     */
    private void showNoEmbryoMessage() {
        ChestGui gui = new ChestGui(3, "§6§l选择胚胎");
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
                .name("§c§l背包中没有胚胎")
                .lore(
                        "§7您需要先炼制胚胎才能精炼装备",
                        "",
                        "§e如何获得胚胎:",
                        "§7使用 §a/forge craft §7命令炼制胚胎",
                        "§7或使用 §a配方炼制 §7功能",
                        "",
                        "§7需要材料:",
                        "§c下界合金 §7→ §f500",
                        "§b钻石 §7→ §f100",
                        "§a绿宝石 §7→ §f80",
                        "§6金矿石 §7→ §f20",
                        "§7铁矿石 §7→ §f5"
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
     * 胚胎槽位信息
     */
    private static class EmbryoSlotInfo {
        final Embryo embryo;
        final int slot;
        final ItemStack itemStack;

        EmbryoSlotInfo(Embryo embryo, int slot, ItemStack itemStack) {
            this.embryo = embryo;
            this.slot = slot;
            this.itemStack = itemStack;
        }
    }
}
