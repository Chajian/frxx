package com.xiancore.systems.skill.shop;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.items.SkillBookFactory;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 功法商店 GUI
 * 玩家可以使用灵石购买功法秘籍
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillShopGUI {

    private final XianCore plugin;
    private String currentCategory = "all"; // 当前分类

    public SkillShopGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开商店主界面
     */
    public void open(Player player) {
        // 检查商店是否启用
        if (!SkillShopConfig.isEnabled()) {
            player.sendMessage("§c功法商店暂时关闭！");
            return;
        }

        // 加载玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return;
        }

        // 创建GUI
        int rows = SkillShopConfig.getShopSize() / 9;
        ChestGui gui = new ChestGui(rows, SkillShopConfig.getShopTitle());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 添加背景
        addBackground(gui);

        // 添加分类按钮
        addCategoryButtons(gui, player, data);

        // 添加商品展示
        addShopItems(gui, player, data, currentCategory);

        // 添加玩家信息显示
        addPlayerInfo(gui, player, data);

        gui.show(player);
    }

    /**
     * 打开指定分类的商店
     */
    public void openCategory(Player player, String category) {
        this.currentCategory = category;
        open(player);
    }

    /**
     * 添加背景
     */
    private void addBackground(ChestGui gui) {
        GUIUtils.addGrayBackground(gui, gui.getRows());
    }

    /**
     * 添加分类按钮
     */
    private void addCategoryButtons(ChestGui gui, Player player, PlayerData data) {
        StaticPane pane = new StaticPane(0, 0, 9, 1, Pane.Priority.HIGH);

        // "全部"分类按钮
        ItemStack allButton = new ItemStack(Material.CHEST);
        ItemMeta allMeta = allButton.getItemMeta();
        if (allMeta != null) {
            allMeta.setDisplayName("§e§l全部功法");
            List<String> allLore = new ArrayList<>();
            allLore.add("§7显示所有可购买功法");
            allLore.add("");
            allLore.add("§e点击查看");
            allMeta.setLore(allLore);
            allButton.setItemMeta(allMeta);
        }
        
        pane.addItem(new GuiItem(allButton, event -> {
            openCategory(player, "all");
        }), 1, 0);

        // 从配置加载的分类按钮
        Map<String, SkillShopConfig.CategoryInfo> categories = SkillShopConfig.getCategories();
        int slot = 3;
        for (SkillShopConfig.CategoryInfo category : categories.values()) {
            ItemStack categoryItem = new ItemStack(category.getIcon());
            ItemMeta categoryMeta = categoryItem.getItemMeta();
            
            if (categoryMeta != null) {
                categoryMeta.setDisplayName(category.getDisplayName());
                
                List<String> categoryLore = new ArrayList<>(category.getDescription());
                categoryLore.add("");
                
                // 显示该分类的商品数量
                int itemCount = SkillShopConfig.getItemsByCategory(category.getId()).size();
                categoryLore.add("§7商品数量: §e" + itemCount);
                categoryLore.add("");
                categoryLore.add("§e点击查看");
                
                categoryMeta.setLore(categoryLore);
                categoryItem.setItemMeta(categoryMeta);
            }
            
            pane.addItem(new GuiItem(categoryItem, event -> {
                openCategory(player, category.getId());
            }), slot, 0);
            
            slot += 2;
        }

        gui.addPane(pane);
    }

    /**
     * 添加商品展示
     */
    private void addShopItems(ChestGui gui, Player player, PlayerData data, String category) {
        StaticPane itemPane = new StaticPane(0, 2, 9, gui.getRows() - 3, Pane.Priority.NORMAL);

        // 获取要显示的商品列表
        List<SkillShopItem> items;
        if ("all".equals(category)) {
            items = SkillShopConfig.getAllItems();
        } else {
            items = SkillShopConfig.getItemsByCategory(category);
        }

        // 玩家灵石数量
        int playerSpiritStones = (int) data.getSpiritStones();

        // 显示商品
        int x = 1;
        int y = 0;
        for (SkillShopItem item : items) {
            // 检查购买条件
            boolean meetsRequirements = checkRequirements(player, data, item);
            boolean canAfford = playerSpiritStones >= item.getActualPrice();

            // 创建显示物品
            ItemStack displayItem = item.createDisplayItem(playerSpiritStones, canAfford, meetsRequirements);

            // 添加点击事件
            itemPane.addItem(new GuiItem(displayItem, event -> {
                handlePurchase(player, data, item);
            }), x, y);

            // 更新位置
            x++;
            if (x >= 8) {
                x = 1;
                y++;
                if (y >= gui.getRows() - 3) {
                    break; // 超出显示范围
                }
            }
        }

        gui.addPane(itemPane);
    }

    /**
     * 添加玩家信息显示
     */
    private void addPlayerInfo(ChestGui gui, Player player, PlayerData data) {
        StaticPane infoPane = new StaticPane(0, gui.getRows() - 1, 9, 1, Pane.Priority.HIGH);

        // 灵石显示
        ItemStack infoItem = new ItemStack(Material.EMERALD);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§a§l你的灵石");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("");
            infoLore.add("§7当前灵石: §e" + data.getSpiritStones());
            infoLore.add("");
            infoLore.add("§7通过修炼和完成任务获得灵石");
            infoLore.add("§7使用灵石可以购买功法秘籍");
            infoMeta.setLore(infoLore);
            infoItem.setItemMeta(infoMeta);
        }
        
        infoPane.addItem(new GuiItem(infoItem), 4, 0);

        // 刷新按钮
        ItemStack refreshItem = new ItemStack(Material.COMPASS);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        if (refreshMeta != null) {
            refreshMeta.setDisplayName("§b§l刷新商店");
            List<String> refreshLore = new ArrayList<>();
            refreshLore.add("§7点击刷新商店界面");
            refreshMeta.setLore(refreshLore);
            refreshItem.setItemMeta(refreshMeta);
        }
        
        infoPane.addItem(new GuiItem(refreshItem, event -> {
            open(player);
        }), 8, 0);

        gui.addPane(infoPane);
    }

    /**
     * 检查购买条件
     */
    private boolean checkRequirements(Player player, PlayerData data, SkillShopItem item) {
        // 检查境界要求
        if (item.getRequiredRealm() != null && !item.getRequiredRealm().isEmpty()) {
            String currentRealm = data.getRealm();
            // 简单的境界检查，实际应该使用境界等级比较
            // TODO: 使用 RealmParser 进行更准确的境界等级比较
        }

        // 检查等级要求
        if (item.getRequiredLevel() > 0) {
            if (data.getLevel() < item.getRequiredLevel()) {
                return false;
            }
        }

        // 检查是否已学习（如果配置不允许重复购买）
        if (!SkillShopConfig.isAllowDuplicatePurchase()) {
            if (data.getSkills().containsKey(item.getSkillId())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 处理购买
     */
    private void handlePurchase(Player player, PlayerData data, SkillShopItem item) {
        // 检查库存
        if (!item.hasStock()) {
            player.sendMessage("§c该商品已售罄！");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // 检查购买条件
        if (!checkRequirements(player, data, item)) {
            player.sendMessage("§c你不满足购买条件！");
            if (item.getRequiredRealm() != null) {
                player.sendMessage("§7需要境界: §e" + item.getRequiredRealm());
            }
            if (item.getRequiredLevel() > 0) {
                player.sendMessage("§7需要等级: §e" + item.getRequiredLevel());
            }
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // 检查灵石是否足够
        int actualPrice = item.getActualPrice();
        if (data.getSpiritStones() < actualPrice) {
            player.sendMessage("§c灵石不足！");
            player.sendMessage("§7需要: §e" + actualPrice + " §7当前: §f" + data.getSpiritStones());
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // 检查背包空间
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§c背包已满！请清理背包后再试");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        // 扣除灵石
        data.removeSpiritStones((long) actualPrice);

        // 减少库存
        item.decreaseStock();

        // 给予功法秘籍
        // 先从SkillSystem获取Skill对象
        Skill skill = plugin.getSkillSystem().getSkill(item.getSkillId());
        if (skill == null) {
            player.sendMessage("§c功法不存在！请联系管理员");
            player.sendMessage("§7功法ID: " + item.getSkillId());
            // 退还灵石
            data.addSpiritStones((long) actualPrice);
            return;
        }
        
        ItemStack skillBook = SkillBookFactory.createSkillBook(skill);
        if (skillBook != null) {
            player.getInventory().addItem(skillBook);
        } else {
            player.sendMessage("§c功法秘籍生成失败！请联系管理员");
            // 退还灵石
            data.addSpiritStones((long) actualPrice);
            return;
        }

        // 保存数据
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        player.sendMessage("§a§l========== 购买成功 ==========");
        player.sendMessage("§e物品: " + item.getDisplayName());
        player.sendMessage("§e功法: §f" + item.getSkillId());
        player.sendMessage("§e消耗: §6" + actualPrice + " §7灵石");
        player.sendMessage("§e剩余: §f" + data.getSpiritStones() + " §7灵石");
        player.sendMessage("§a§l===========================");
        player.sendMessage("§7§o右键使用秘籍学习功法");

        playSound(player, Sound.ENTITY_PLAYER_LEVELUP);

        // 刷新界面
        open(player);
    }

    /**
     * 播放音效
     */
    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
}

