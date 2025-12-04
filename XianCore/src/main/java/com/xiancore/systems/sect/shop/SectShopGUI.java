package com.xiancore.systems.sect.shop;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 宗门商店 GUI
 * 提供贡献点兑换物品的界面
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectShopGUI implements Listener {

    private final XianCore plugin;
    private static final String TITLE = "\u00a76\u00a7l\u5b97\u95e8\u5546\u5e97";

    // 追踪打开商店的玩家
    private final Map<UUID, String> openShops = new HashMap<>();

    public SectShopGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开商店界面
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

        // 检查商店是否已开启
        if (!plugin.getSectSystem().getFacilityManager().isShopEnabled(sectId)) {
            player.sendMessage("\u00a7c\u5b97\u95e8\u8fd8\u6ca1\u6709\u5f00\u542f\u5546\u5e97!");
            player.sendMessage("\u00a77\u8bf7\u8054\u7cfb\u5b97\u4e3b\u6216\u957f\u8001\u5347\u7ea7\u8bbe\u65bd");
            return;
        }

        // 创建界面
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);

        // 填充背景
        fillBackground(inventory);

        // 获取玩家贡献点 - 修复：从 PlayerData 读取而不是 SectMember
        int contribution = data.getContributionPoints();

        // 设置物品
        setShopItems(inventory, contribution);

        // 设置信息栏
        setInfoItem(inventory, contribution);

        // 记录打开的商店
        openShops.put(player.getUniqueId(), "main");

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
     * 设置商店物品
     */
    private void setShopItems(Inventory inventory, int playerContribution) {
        List<ShopItem> items = ShopConfig.getAllItems();

        int[] slots = {10, 11, 12, 13, 14, 15, 16,
                       19, 20, 21, 22, 23, 24, 25,
                       28, 29, 30, 31, 32, 33, 34,
                       37, 38, 39, 40, 41, 42, 43};

        for (int i = 0; i < items.size() && i < slots.length; i++) {
            ShopItem shopItem = items.get(i);
            ItemStack displayItem = shopItem.createDisplayItem(playerContribution);
            inventory.setItem(slots[i], displayItem);
        }
    }

    /**
     * 设置信息栏
     */
    private void setInfoItem(Inventory inventory, int contribution) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7e\u00a7l\u4f60\u7684\u8d21\u732e\u70b9");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u5f53\u524d\u8d21\u732e\u70b9: \u00a7e" + contribution);
            lore.add("");
            lore.add("\u00a77\u901a\u8fc7\u5b8c\u6210\u5b97\u95e8\u4efb\u52a1\u83b7\u5f97\u8d21\u732e\u70b9");
            lore.add("\u00a77\u4f7f\u7528\u8d21\u732e\u70b9\u53ef\u4ee5\u5151\u6362\u5404\u79cd\u73cd\u8d35\u7269\u54c1");

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

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // 检查是否是商店界面
        if (!title.equals(TITLE)) {
            return;
        }

        event.setCancelled(true);

        // 检查是否记录了打开的商店
        if (!openShops.containsKey(player.getUniqueId())) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        // 获取玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || data.getSectId() == null) {
            return;
        }

        Sect sect = plugin.getSectSystem().getSect(data.getSectId());
        if (sect == null) {
            return;
        }

        // 查找对应的商店物品
        ShopItem shopItem = findShopItem(clickedItem);
        if (shopItem == null) {
            return;
        }

        // 尝试购买 - 修复：传入 PlayerData 而不是 SectMember
        attemptPurchase(player, data, sect, shopItem);
    }

    /**
     * 根据显示物品查找商店物品
     */
    private ShopItem findShopItem(ItemStack displayItem) {
        for (ShopItem item : ShopConfig.getAllItems()) {
            if (item.getMaterial() == displayItem.getType()) {
                return item;
            }
        }
        return null;
    }

    /**
     * 尝试购买物品
     * 修复：使用 PlayerData.contributionPoints 而不是 SectMember.contribution
     */
    private void attemptPurchase(Player player, PlayerData data, Sect sect, ShopItem shopItem) {
        // 检查贡献点是否足够 - 修复：从 PlayerData 读取
        int playerContribution = data.getContributionPoints();
        
        if (playerContribution < shopItem.getContributionCost()) {
            player.sendMessage("\u00a7c\u8d21\u732e\u70b9\u4e0d\u8db3!");
            player.sendMessage("\u00a77\u9700\u8981: \u00a7e" + shopItem.getContributionCost() +
                             " \u00a77\u5f53\u524d: \u00a7f" + playerContribution);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 检查背包空间
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("\u00a7c\u80cc\u5305\u5df2\u6ee1! \u8bf7\u6e05\u7406\u80cc\u5305\u540e\u518d\u8bd5");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 扣除贡献点 - 修复：从 PlayerData 扣除
        data.setContributionPoints(playerContribution - shopItem.getContributionCost());

        // 给予物品
        ItemStack item = new ItemStack(shopItem.getMaterial(), shopItem.getAmount());
        player.getInventory().addItem(item);

        // 保存数据 - 修复：保存 PlayerData 而不是 Sect
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("\u00a7a\u00a7l========== \u8d2d\u4e70\u6210\u529f ==========");
        player.sendMessage("\u00a7e\u7269\u54c1: " + shopItem.getDisplayName());
        player.sendMessage("\u00a7e\u6570\u91cf: \u00a7fx" + shopItem.getAmount());
        player.sendMessage("\u00a7e\u6d88\u8017: \u00a76" + shopItem.getContributionCost() + " \u8d21\u732e\u70b9");
        player.sendMessage("\u00a7e\u5269\u4f59: \u00a7f" + data.getContributionPoints() + " \u8d21\u732e\u70b9");
        player.sendMessage("\u00a7a\u00a7l===========================");

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // 刷新界面
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 2L);
    }

    /**
     * 清理玩家追踪
     */
    public void cleanup(UUID playerId) {
        openShops.remove(playerId);
    }
}
