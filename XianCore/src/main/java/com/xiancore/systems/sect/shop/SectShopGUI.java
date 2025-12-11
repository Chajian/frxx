package com.xiancore.systems.sect.shop;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.sect.Sect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 宗门商店 GUI
 * 提供贡献点兑换物品的界面
 * 使用 IF (Inventory Framework) 重构
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class SectShopGUI {

    private final XianCore plugin;

    public SectShopGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开商店界面
     */
    public void open(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return;
        }

        // 检查玩家是否在宗门
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

        // 检查商店是否已开启
        if (!plugin.getSectSystem().getFacilityManager().isShopEnabled(sectId)) {
            player.sendMessage("§c宗门还没有开启商店!");
            player.sendMessage("§7请联系宗主或长老升级设施");
            return;
        }

        // 获取玩家贡献点
        int contribution = data.getContributionPoints();

        // 创建 GUI
        ChestGui gui = new ChestGui(6, "§6§l宗门商店");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        OutlinePane background = new OutlinePane(0, 0, 9, 6);
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        // 内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 设置商店物品
        setShopItems(contentPane, player, data, contribution);

        // 设置信息栏
        setInfoItem(contentPane, contribution);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 设置商店物品
     */
    private void setShopItems(StaticPane pane, Player player, PlayerData data, int playerContribution) {
        List<ShopItem> items = ShopConfig.getAllItems();

        // 物品槽位布局 (中间区域 7x4)
        int[][] slots = {
                {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1}, {7, 1},
                {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2}, {7, 2},
                {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3},
                {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4}, {7, 4}
        };

        for (int i = 0; i < items.size() && i < slots.length; i++) {
            ShopItem shopItem = items.get(i);
            ItemStack displayItem = shopItem.createDisplayItem(playerContribution);

            final ShopItem finalShopItem = shopItem;
            pane.addItem(new GuiItem(displayItem, event -> {
                attemptPurchase(player, data, finalShopItem);
            }), slots[i][0], slots[i][1]);
        }
    }

    /**
     * 设置信息栏
     */
    private void setInfoItem(StaticPane pane, int contribution) {
        ItemStack infoItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("§e§l你的贡献点")
                .lore(
                        "",
                        "§7当前贡献点: §e" + contribution,
                        "",
                        "§7通过完成宗门任务获得贡献点",
                        "§7使用贡献点可以兑换各种珍贵物品"
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(infoItem), 4, 5);
    }

    /**
     * 尝试购买物品
     */
    private void attemptPurchase(Player player, PlayerData data, ShopItem shopItem) {
        // 重新获取最新数据（防止数据不同步）
        PlayerData freshData = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (freshData == null) {
            player.sendMessage("§c数据加载失败!");
            return;
        }

        int playerContribution = freshData.getContributionPoints();

        // 检查贡献点是否足够
        if (playerContribution < shopItem.getContributionCost()) {
            player.sendMessage("§c贡献点不足!");
            player.sendMessage("§7需要: §e" + shopItem.getContributionCost() +
                    " §7当前: §f" + playerContribution);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 检查背包空间
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§c背包已满! 请清理背包后再试");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 扣除贡献点
        freshData.setContributionPoints(playerContribution - shopItem.getContributionCost());

        // 给予物品
        ItemStack item = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        player.getInventory().addItem(item);

        // 保存数据
        plugin.getDataManager().savePlayerData(freshData);

        // 成功消息
        player.sendMessage("§a§l========== 购买成功 ==========");
        player.sendMessage("§e物品: " + shopItem.getDisplayName());
        player.sendMessage("§e数量: §fx" + shopItem.getAmount());
        player.sendMessage("§e消耗: §6" + shopItem.getContributionCost() + " 贡献点");
        player.sendMessage("§e剩余: §f" + freshData.getContributionPoints() + " 贡献点");
        player.sendMessage("§a§l===========================");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // 刷新界面
        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> open(player), 2L);
    }
}
