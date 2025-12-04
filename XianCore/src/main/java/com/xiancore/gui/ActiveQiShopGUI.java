package com.xiancore.gui;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
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
 * 活跃灵气商店 GUI
 * 玩家可以使用活跃灵气购买加成和领取每日礼包
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ActiveQiShopGUI implements Listener {

    private final XianCore plugin;
    private static final String TITLE = "\u00a7b\u00a7l\u6d3b\u8dc3\u7075\u6c14\u5546\u5e97";

    public ActiveQiShopGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开商店 GUI
     */
    public void open(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("\u00a7c\u6570\u636e\u52a0\u8f7d\u5931\u8d25!");
            return;
        }

        // 创建 27 格 GUI
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        // 填充背景
        fillBackground(inventory);

        // 设置商品
        setBreakthroughBoostItem(inventory, player);
        setForgeBoostItem(inventory, player);
        setDailyGiftItem(inventory, player);

        // 设置当前活跃灵气显示
        setActiveQiDisplay(inventory, data);

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

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }
    }

    /**
     * 设置突破加成物品
     */
    private void setBreakthroughBoostItem(Inventory inventory, Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7e\u00a7l\u2605 \u7a81\u7834\u52a0\u6210");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u6d88\u8017: \u00a7e30 \u6d3b\u8dc3\u7075\u6c14");
            lore.add("\u00a77\u6548\u679c: \u00a7a+5% \u7a81\u7834\u6210\u529f\u7387");
            lore.add("\u00a77\u6301\u7eed: \u00a7f\u672c\u6b21\u7a81\u7834");
            lore.add("");

            // 检查是否已有加成
            if (plugin.getActiveQiManager().hasActiveBoost(player.getUniqueId(),
                    com.xiancore.systems.activeqi.ActiveQiManager.ActiveQiBoostType.BREAKTHROUGH)) {
                lore.add("\u00a7c\u00a7l\u2717 \u5df2\u62e5\u6709\u6b64\u52a0\u6210");
            } else {
                lore.add("\u00a7e\u70b9\u51fb\u8d2d\u4e70");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(11, item);
    }

    /**
     * 设置炼制加成物品
     */
    private void setForgeBoostItem(Inventory inventory, Player player) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a76\u00a7l\u26cf \u70bc\u5236\u52a0\u6210");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u6d88\u8017: \u00a7e25 \u6d3b\u8dc3\u7075\u6c14");
            lore.add("\u00a77\u6548\u679c: \u00a7a+3% \u70bc\u5236\u6210\u529f\u7387");
            lore.add("\u00a77\u6301\u7eed: \u00a7f\u672c\u6b21\u70bc\u5236/\u5f3a\u5316/\u878d\u5408");
            lore.add("");

            // 检查是否已有加成
            if (plugin.getActiveQiManager().hasActiveBoost(player.getUniqueId(),
                    com.xiancore.systems.activeqi.ActiveQiManager.ActiveQiBoostType.FORGE)) {
                lore.add("\u00a7c\u00a7l\u2717 \u5df2\u62e5\u6709\u6b64\u52a0\u6210");
            } else {
                lore.add("\u00a7e\u70b9\u51fb\u8d2d\u4e70");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(13, item);
    }

    /**
     * 设置每日礼包物品
     */
    private void setDailyGiftItem(Inventory inventory, Player player) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7d\u00a7l\u2764 \u6bcf\u65e5\u793c\u5305");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u6d88\u8017: \u00a7e100 \u6d3b\u8dc3\u7075\u6c14");
            lore.add("\u00a77\u51b7\u5374: \u00a7f24 \u5c0f\u65f6");
            lore.add("");
            lore.add("\u00a7e\u5956\u52b1\u5185\u5bb9:");
            lore.add("\u00a77  - \u7075\u77f3: \u00a76100-500");
            lore.add("\u00a77  - \u529f\u6cd5\u70b9: \u00a7d1-3");
            lore.add("\u00a77  - \u8d21\u732e\u70b9: \u00a7b50-200");
            lore.add("\u00a77\uff08\u6839\u636e\u5883\u754c\u8c03\u6574\uff09");
            lore.add("");

            // 检查冷却时间
            if (plugin.getActiveQiManager().isDailyGiftOnCooldown(player.getUniqueId())) {
                long remainingHours = plugin.getActiveQiManager()
                        .getDailyGiftRemainingCooldown(player.getUniqueId());
                lore.add("\u00a7c\u00a7l\u2717 \u51b7\u5374\u4e2d (" + remainingHours + " \u5c0f\u65f6)");
            } else {
                lore.add("\u00a7e\u70b9\u51fb\u9886\u53d6");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(15, item);
    }

    /**
     * 显示当前活跃灵气
     */
    private void setActiveQiDisplay(Inventory inventory, PlayerData data) {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7a\u00a7l\u26a1 \u6d3b\u8dc3\u7075\u6c14");

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("\u00a77\u5f53\u524d: \u00a7e" + data.getActiveQi() + " \u00a77/ \u00a7f100");
            lore.add("");
            lore.add("\u00a77\u83b7\u53d6\u65b9\u5f0f:");
            lore.add("\u00a77  \u2022 \u5b66\u4e60\u529f\u6cd5: +10");
            lore.add("\u00a77  \u2022 \u5347\u7ea7\u529f\u6cd5: +3-15");
            lore.add("\u00a77  \u2022 \u65bd\u653e\u529f\u6cd5: +2");
            lore.add("\u00a77  \u2022 \u7a81\u7834\u6210\u529f: +30");
            lore.add("\u00a77  \u2022 \u7a81\u7834\u5931\u8d25: +5");
            lore.add("\u00a77  \u2022 \u6e21\u52ab\u6210\u529f: +25-80");
            lore.add("\u00a77  \u2022 \u70bc\u5236\u88c5\u5907: +5-20");
            lore.add("\u00a77  \u2022 \u5f3a\u5316\u88c5\u5907: +3-10");
            lore.add("\u00a77  \u2022 \u878d\u5408\u88c5\u5907: +12");
            lore.add("");
            lore.add("\u00a77\u6bcf\u65e5\u8870\u51cf: \u00a7c-15%");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        inventory.setItem(22, item);
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

        String displayName = clickedItem.getItemMeta().getDisplayName();

        // 突破加成
        if (displayName.contains("\u7a81\u7834\u52a0\u6210")) {
            if (plugin.getActiveQiManager().purchaseBreakthroughBoost(player)) {
                player.closeInventory();
            }
        }
        // 炼制加成
        else if (displayName.contains("\u70bc\u5236\u52a0\u6210")) {
            if (plugin.getActiveQiManager().purchaseForgeBoost(player)) {
                player.closeInventory();
            }
        }
        // 每日礼包
        else if (displayName.contains("\u6bcf\u65e5\u793c\u5305")) {
            if (plugin.getActiveQiManager().claimDailyGift(player)) {
                player.closeInventory();
            }
        }
        // 活跃灵气显示 - 刷新界面
        else if (displayName.contains("\u6d3b\u8dc3\u7075\u6c14")) {
            player.closeInventory();
            // 延迟重新打开，显示更新后的数据
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 2L);
        }
    }
}
