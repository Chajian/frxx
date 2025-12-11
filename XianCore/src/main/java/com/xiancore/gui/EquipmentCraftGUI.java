package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.core.utils.QualityUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.items.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 装备炼制GUI
 * 选择装备类型并炼制
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 支持从背包选择胚胎
 */
public class EquipmentCraftGUI {

    private final XianCore plugin;
    private final Player player;
    private final Embryo embryo;
    private final int selectedSlot;  // 选中的胚胎槽位

    public EquipmentCraftGUI(XianCore plugin, Player player, Embryo embryo, int selectedSlot) {
        this.plugin = plugin;
        this.player = player;
        this.embryo = embryo;
        this.selectedSlot = selectedSlot;
    }

    /**
     * 打开炼制装备选择界面
     */
    public static void open(Player player, XianCore plugin, Embryo embryo, int selectedSlot) {
        new EquipmentCraftGUI(plugin, player, embryo, selectedSlot).show();
    }

    private void show() {
        ChestGui gui = new ChestGui(5, "§6§l炼制装备 - 选择类型");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addBackground(gui, 5);

        StaticPane contentPane = new StaticPane(0, 0, 9, 5);

        // 显示胚胎信息
        ItemStack embryoDisplay = embryo.toItemStack();
        contentPane.addItem(new GuiItem(embryoDisplay), 4, 0);

        // 武器类别按钮
        ItemStack weaponButton = new ItemBuilder(Material.DIAMOND_SWORD)
                .name("§c§l武器")
                .lore(
                        "§7炼制为武器装备",
                        "§e可选类型:",
                        "§7- 剑",
                        "§7- 斧",
                        "§7- 弓",
                        "",
                        "§a点击选择武器类型"
                )
                .build();

        contentPane.addItem(new GuiItem(weaponButton, event -> {
            showTypeSelection("weapon");
        }), 1, 2);

        // 护甲类别按钮
        ItemStack armorButton = new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .name("§9§l护甲")
                .lore(
                        "§7炼制为护甲装备",
                        "§e可选类型:",
                        "§7- 头盔",
                        "§7- 胸甲",
                        "§7- 护腿",
                        "§7- 靴子",
                        "",
                        "§a点击选择护甲类型"
                )
                .build();

        contentPane.addItem(new GuiItem(armorButton, event -> {
            showTypeSelection("armor");
        }), 4, 2);

        // 饰品类别按钮
        ItemStack accessoryButton = new ItemBuilder(Material.TOTEM_OF_UNDYING)
                .name("§e§l饰品")
                .lore(
                        "§7炼制为饰品",
                        "§e可选类型:",
                        "§7- 戒指",
                        "§7- 项链",
                        "§7- 法宝",
                        "",
                        "§a点击选择饰品类型"
                )
                .build();

        contentPane.addItem(new GuiItem(accessoryButton, event -> {
            showTypeSelection("accessory");
        }), 7, 2);

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> {
            player.closeInventory();
        }), 0, 4);

        // 炼制消耗说明
        ItemStack infoButton = new ItemBuilder(Material.BOOK)
                .name("§e§l炼制说明")
                .lore(
                        "§7选择装备类型后消耗胚胎",
                        "§7根据品质消耗灵石:",
                        "§d  神品: 500灵石",
                        "§6  仙品: 300灵石",
                        "§5  宝品: 200灵石",
                        "§b  灵品: 100灵石",
                        "§f  凡品: 50灵石",
                        "",
                        "§a成功率: 100%"
                )
                .build();
        contentPane.addItem(new GuiItem(infoButton), 8, 4);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示具体装备类型选择
     */
    private void showTypeSelection(String category) {
        ChestGui gui = new ChestGui(3, "§6§l选择装备类型");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addGrayBackground(gui, 3);

        StaticPane contentPane = new StaticPane(0, 0, 9, 3);

        // 获取该类别的所有装备类型
        EquipmentType[] types = EquipmentFactory.getTypesByCategory(category);

        int startX = 9 / 2 - types.length / 2;
        for (int i = 0; i < types.length; i++) {
            EquipmentType type = types[i];

            ItemStack typeButton = new ItemBuilder(type.getMaterial())
                    .name("§e" + type.getDisplayName())
                    .lore(
                            "§7品质: " + QualityUtils.getColor(embryo.getQuality()) + embryo.getQuality(),
                            "§7五行: §f" + embryo.getElement(),
                            "",
                            "§a点击炼制"
                    )
                    .build();

            contentPane.addItem(new GuiItem(typeButton, event -> {
                craftEquipment(type);
            }), startX + i, 1);
        }

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> {
            show();
        }), 0, 2);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 炼制装备
     */
    private void craftEquipment(EquipmentType type) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            player.closeInventory();
            return;
        }

        // 计算消耗
        int cost = switch (embryo.getQuality()) {
            case "神品" -> 500;
            case "仙品" -> 300;
            case "宝品" -> 200;
            case "灵品" -> 100;
            default -> 50;
        };

        // 检查灵石
        if (data.getSpiritStones() < cost) {
            player.sendMessage("§c灵石不足! 需要: " + cost + " 当前: " + data.getSpiritStones());
            return;
        }

        // 消耗灵石
        data.removeSpiritStones(cost);
        plugin.getDataManager().savePlayerData(data);

        // 创建装备（使用 MythicMobs 模板）
        ItemStack equipmentItem = EquipmentFactory.createItemFromEmbryo(plugin, embryo, type);

        // 给予玩家
        player.getInventory().addItem(equipmentItem);

        // 消耗选中的胚胎
        if (!consumeSelectedEmbryo()) {
            // 消耗失败，回滚灵石
            data.addSpiritStones(cost);
            plugin.getDataManager().savePlayerData(data);
            player.sendMessage("§c精炼失败! 胚胎已被移动或替换，已返还灵石");
            return;
        }

        player.closeInventory();
        player.sendMessage("§a✓ 炼制成功!");
        player.sendMessage("§e获得了 " + QualityUtils.getColor(embryo.getQuality()) + type.getDisplayName() + " [" + embryo.getQuality() + "]");
        player.sendMessage("§7消耗了 " + cost + " 灵石");
    }

    /**
     * 消耗选中的胚胎
     * @return 是否成功消耗
     */
    private boolean consumeSelectedEmbryo() {
        // 验证槽位有效性
        if (selectedSlot < 0 || selectedSlot >= 36) {
            player.sendMessage("§c错误: 无法找到选中的胚胎!");
            return false;
        }

        // 获取槽位中的物品
        ItemStack item = player.getInventory().getItem(selectedSlot);
        if (item == null || item.getType().isAir()) {
            player.sendMessage("§c错误: 选中的槽位为空!");
            return false;
        }

        // 验证是否是胚胎
        if (!EmbryoParser.isEmbryo(item)) {
            player.sendMessage("§c错误: 选中的物品不是胚胎!");
            return false;
        }

        // 验证UUID是否匹配（防止物品被移动或替换）
        Embryo currentEmbryo = EmbryoParser.parseFromItemStack(item);
        if (currentEmbryo == null) {
            player.sendMessage("§c错误: 无法解析胚胎数据!");
            return false;
        }

        if (!currentEmbryo.getUuid().equals(embryo.getUuid())) {
            player.sendMessage("§c错误: 胚胎已被移动或替换!");
            return false;
        }

        // 消耗胚胎
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItem(selectedSlot, null);
        }

        return true;
    }
}
