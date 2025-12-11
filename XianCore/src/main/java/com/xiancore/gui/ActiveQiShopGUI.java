package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.activeqi.ActiveQiManager.ActiveQiBoostType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 活跃灵气商店 GUI
 * 玩家可以使用活跃灵气购买加成和领取每日礼包
 * 使用 IF (Inventory Framework) 重构
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class ActiveQiShopGUI {

    private final XianCore plugin;

    public ActiveQiShopGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开商店 GUI
     */
    public void open(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return;
        }

        // 创建 GUI
        ChestGui gui = new ChestGui(3, "§b§l活跃灵气商店");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addGrayBackground(gui, 3);

        // 内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 3);

        // 设置商品
        setBreakthroughBoostItem(contentPane, player);
        setForgeBoostItem(contentPane, player);
        setDailyGiftItem(contentPane, player);

        // 设置当前活跃灵气显示
        setActiveQiDisplay(contentPane, player, data);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 设置突破加成物品
     */
    private void setBreakthroughBoostItem(StaticPane pane, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7消耗: §e30 活跃灵气");
        lore.add("§7效果: §a+5% 突破成功率");
        lore.add("§7持续: §f本次突破");
        lore.add("");

        boolean hasBoost = plugin.getActiveQiManager().hasActiveBoost(
                player.getUniqueId(), ActiveQiBoostType.BREAKTHROUGH);

        if (hasBoost) {
            lore.add("§c§l✗ 已拥有此加成");
        } else {
            lore.add("§e点击购买");
        }

        ItemStack item = new ItemBuilder(Material.NETHER_STAR)
                .name("§e§l★ 突破加成")
                .lore(lore)
                .glow()
                .build();

        pane.addItem(new GuiItem(item, event -> {
            if (!hasBoost) {
                if (plugin.getActiveQiManager().purchaseBreakthroughBoost(player)) {
                    player.closeInventory();
                }
            }
        }), 2, 1);
    }

    /**
     * 设置炼制加成物品
     */
    private void setForgeBoostItem(StaticPane pane, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7消耗: §e25 活跃灵气");
        lore.add("§7效果: §a+3% 炼制成功率");
        lore.add("§7持续: §f本次炼制/强化/融合");
        lore.add("");

        boolean hasBoost = plugin.getActiveQiManager().hasActiveBoost(
                player.getUniqueId(), ActiveQiBoostType.FORGE);

        if (hasBoost) {
            lore.add("§c§l✗ 已拥有此加成");
        } else {
            lore.add("§e点击购买");
        }

        ItemStack item = new ItemBuilder(Material.ANVIL)
                .name("§6§l⚏ 炼制加成")
                .lore(lore)
                .build();

        pane.addItem(new GuiItem(item, event -> {
            if (!hasBoost) {
                if (plugin.getActiveQiManager().purchaseForgeBoost(player)) {
                    player.closeInventory();
                }
            }
        }), 4, 1);
    }

    /**
     * 设置每日礼包物品
     */
    private void setDailyGiftItem(StaticPane pane, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7消耗: §e100 活跃灵气");
        lore.add("§7冷却: §f24 小时");
        lore.add("");
        lore.add("§e奖励内容:");
        lore.add("§7  - 灵石: §6100-500");
        lore.add("§7  - 功法点: §d1-3");
        lore.add("§7  - 贡献点: §b50-200");
        lore.add("§7（根据境界调整）");
        lore.add("");

        boolean onCooldown = plugin.getActiveQiManager().isDailyGiftOnCooldown(player.getUniqueId());

        if (onCooldown) {
            long remainingHours = plugin.getActiveQiManager()
                    .getDailyGiftRemainingCooldown(player.getUniqueId());
            lore.add("§c§l✗ 冷却中 (" + remainingHours + " 小时)");
        } else {
            lore.add("§e点击领取");
        }

        ItemStack item = new ItemBuilder(Material.CHEST)
                .name("§d§l❤ 每日礼包")
                .lore(lore)
                .build();

        pane.addItem(new GuiItem(item, event -> {
            if (!onCooldown) {
                if (plugin.getActiveQiManager().claimDailyGift(player)) {
                    player.closeInventory();
                }
            }
        }), 6, 1);
    }

    /**
     * 显示当前活跃灵气
     */
    private void setActiveQiDisplay(StaticPane pane, Player player, PlayerData data) {
        ItemStack item = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("§a§l⚡ 活跃灵气")
                .lore(
                        "",
                        "§7当前: §e" + data.getActiveQi() + " §7/ §f100",
                        "",
                        "§7获取方式:",
                        "§7  • 学习功法: +10",
                        "§7  • 升级功法: +3-15",
                        "§7  • 施放功法: +2",
                        "§7  • 突破成功: +30",
                        "§7  • 突破失败: +5",
                        "§7  • 渡劫成功: +25-80",
                        "§7  • 炼制装备: +5-20",
                        "§7  • 强化装备: +3-10",
                        "§7  • 融合装备: +12",
                        "",
                        "§7每日衰减: §c-15%",
                        "",
                        "§e点击刷新"
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(item, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 2L);
        }), 4, 2);
    }
}
