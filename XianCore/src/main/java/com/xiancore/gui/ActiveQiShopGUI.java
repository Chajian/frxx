package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.activeqi.ActiveQiShopDisplayService;
import com.xiancore.systems.activeqi.ActiveQiShopDisplayService.DailyGiftInfo;
import com.xiancore.systems.activeqi.ActiveQiShopDisplayService.ShopItemInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 活跃灵气商店 GUI
 * 玩家可以使用活跃灵气购买加成和领取每日礼包
 * 业务逻辑委托给 ActiveQiShopDisplayService
 *
 * @author Olivia Diaz
 * @version 3.0.0 - 使用 Service 层分离业务逻辑
 */
public class ActiveQiShopGUI {

    private final XianCore plugin;
    private final ActiveQiShopDisplayService displayService;

    public ActiveQiShopGUI(XianCore plugin) {
        this.plugin = plugin;
        this.displayService = new ActiveQiShopDisplayService(plugin);
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

        ChestGui gui = new ChestGui(3, "§b§l活跃灵气商店");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 3);

        StaticPane contentPane = new StaticPane(0, 0, 9, 3);

        setBreakthroughBoostItem(contentPane, player);
        setForgeBoostItem(contentPane, player);
        setDailyGiftItem(contentPane, player);
        setActiveQiDisplay(contentPane, player, data);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 设置突破加成物品
     */
    private void setBreakthroughBoostItem(StaticPane pane, Player player) {
        ShopItemInfo info = displayService.getBreakthroughBoostInfo(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7消耗: §e" + info.getCost() + " 活跃灵气");
        lore.add("§7效果: §a" + info.getEffect());
        lore.add("§7持续: §f" + info.getDuration());
        lore.add("");

        if (info.isOwned()) {
            lore.add("§c§l✗ " + info.getActionText());
        } else {
            lore.add("§e" + info.getActionText());
        }

        ItemStack item = new ItemBuilder(Material.NETHER_STAR)
                .name("§e§l" + info.getName())
                .lore(lore)
                .glow()
                .build();

        pane.addItem(new GuiItem(item, event -> {
            if (!info.isOwned()) {
                if (displayService.purchaseBreakthroughBoost(player)) {
                    player.closeInventory();
                }
            }
        }), 2, 1);
    }

    /**
     * 设置炼制加成物品
     */
    private void setForgeBoostItem(StaticPane pane, Player player) {
        ShopItemInfo info = displayService.getForgeBoostInfo(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7消耗: §e" + info.getCost() + " 活跃灵气");
        lore.add("§7效果: §a" + info.getEffect());
        lore.add("§7持续: §f" + info.getDuration());
        lore.add("");

        if (info.isOwned()) {
            lore.add("§c§l✗ " + info.getActionText());
        } else {
            lore.add("§e" + info.getActionText());
        }

        ItemStack item = new ItemBuilder(Material.ANVIL)
                .name("§6§l" + info.getName())
                .lore(lore)
                .build();

        pane.addItem(new GuiItem(item, event -> {
            if (!info.isOwned()) {
                if (displayService.purchaseForgeBoost(player)) {
                    player.closeInventory();
                }
            }
        }), 4, 1);
    }

    /**
     * 设置每日礼包物品
     */
    private void setDailyGiftItem(StaticPane pane, Player player) {
        DailyGiftInfo info = displayService.getDailyGiftInfo(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7消耗: §e" + info.getCost() + " 活跃灵气");
        lore.add("§7冷却: §f24 小时");
        lore.add("");
        lore.add("§e奖励内容:");
        lore.add("§7  - 灵石: §6100-500");
        lore.add("§7  - 功法点: §d1-3");
        lore.add("§7  - 贡献点: §b50-200");
        lore.add("§7（根据境界调整）");
        lore.add("");

        if (info.isOnCooldown()) {
            lore.add("§c§l✗ " + info.getActionText());
        } else {
            lore.add("§e" + info.getActionText());
        }

        ItemStack item = new ItemBuilder(Material.CHEST)
                .name("§d§l" + info.getName())
                .lore(lore)
                .build();

        pane.addItem(new GuiItem(item, event -> {
            if (!info.isOnCooldown()) {
                if (displayService.claimDailyGift(player)) {
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
