package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.items.Embryo;
import com.xiancore.systems.forge.items.EmbryoParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 炼器界面
 * 提供炼制、强化、融合功能
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 优化版
 */
public class ForgeGUI {

    private final XianCore plugin;

    public ForgeGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开炼器主界面
     */
    public static void open(Player player, XianCore plugin) {
        new ForgeGUI(plugin).show(player);
    }

    private void show(Player player) {
        // 创建5行的GUI
        ChestGui gui = new ChestGui(5, "§c§l炼器系统");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 创建边框面板
        OutlinePane background = new OutlinePane(0, 0, 9, 5);
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        // 创建内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 5);

        // ========== 第0行：玩家状态信息 ==========
        displayPlayerStatus(player, contentPane);

        // ========== 第1行：配方炼制（主要功能，居中突出）==========
        displayMakeButton(player, contentPane);

        // ========== 第2行：其他功能按钮 ==========
        displayCraftButton(player, contentPane);   // 炼制胚胎
        displayRefineButton(player, contentPane);  // 精炼装备
        displayEnhanceButton(player, contentPane); // 强化装备
        displayFuseButton(player, contentPane);    // 融合装备

        // ========== 第3行：快捷入口 ==========
        displayQuickActions(player, contentPane);

        // ========== 第4行：控制按钮 ==========
        displayHelpButton(player, contentPane);
        displayRefreshButton(player, contentPane);
        
        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 4, 4);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示玩家状态（第0行）
     */
    private void displayPlayerStatus(Player player, StaticPane pane) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) return;

        // 灵石信息
        ItemStack spiritStoneItem = new ItemBuilder(Material.EMERALD)
                .name("§a§l灵石")
                .lore(
                        "§e当前灵石: §f" + data.getSpiritStones(),
                        "",
                        "§7炼制装备消耗灵石",
                        "§7品质越高消耗越多"
                )
                .glow()
                .build();
        pane.addItem(new GuiItem(spiritStoneItem), 2, 0);

        // 胚胎统计
        int embryoCount = countEmbryos(player);
        ItemStack embryoItem = new ItemBuilder(Material.ENDER_PEARL)
                .name("§d§l我的胚胎")
                .lore(
                        "§e背包中胚胎: §f" + embryoCount + " 个",
                        "",
                        "§7胚胎可以精炼成装备",
                        "§7使用 §e精炼装备 §7功能"
                )
                .build();
        pane.addItem(new GuiItem(embryoItem), 4, 0);

        // 装备统计
        int equipmentCount = countEquipments(player);
        ItemStack equipmentItem = new ItemBuilder(Material.DIAMOND_SWORD)
                .name("§b§l我的装备")
                .lore(
                        "§e背包中装备: §f" + equipmentCount + " 件",
                        "",
                        "§7可以强化或融合装备",
                        "§7提升装备属性"
                )
                .build();
        pane.addItem(new GuiItem(equipmentItem), 6, 0);
    }

    /**
     * 配方炼制按钮（第1行，主要功能）
     */
    private void displayMakeButton(Player player, StaticPane pane) {
        ItemStack makeButton = new ItemBuilder(Material.CRAFTING_TABLE)
                .name("§a§l§n配方炼制")
                .lore(
                        "§7使用1-4种材料按配方炼制装备",
                        "",
                        "§e核心功能:",
                        "§7✦ 4个材料槽位系统",
                        "§7✦ 自动配方匹配",
                        "§7✦ 48个预设配方",
                        "§7✦ 自定义装备名称",
                        "",
                        "§6推荐新手使用！",
                        "",
                        "§a§l点击打开炼制界面"
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(makeButton, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                CraftingGUI.open(player, plugin);
            });
        }), 4, 1);
    }

    /**
     * 炼制胚胎按钮（第2行）
     */
    private void displayCraftButton(Player player, StaticPane pane) {
        ItemStack craftButton = new ItemBuilder(Material.ANVIL)
                .name("§6§l炼制胚胎")
                .lore(
                        "§7使用矿石材料炼制仙家胚胎",
                        "",
                        "§e材料价值:",
                        "§c下界合金 §7→ §f500",
                        "§b钻石 §7→ §f100",
                        "§a绿宝石 §7→ §f80",
                        "§6金矿石 §7→ §f20",
                        "§7铁矿石 §7→ §f5",
                        "",
                        "§a点击执行炼制"
                )
                .build();

        pane.addItem(new GuiItem(craftButton, event -> {
            player.closeInventory();
            player.performCommand("forge craft");
        }), 1, 2);
    }

    /**
     * 精炼装备按钮（第2行，优化版）
     */
    private void displayRefineButton(Player player, StaticPane pane) {
        // 检查背包中是否有胚胎
        int embryoCount = countEmbryos(player);
        boolean hasEmbryo = embryoCount > 0;

        Material material = hasEmbryo ? Material.SMITHING_TABLE : Material.BARRIER;
        String nameColor = hasEmbryo ? "§a§l" : "§7";

        ItemStack refineButton = new ItemBuilder(material)
                .name(nameColor + "精炼装备")
                .lore(
                        "§7将胚胎精炼成可用装备",
                        "",
                        "§e背包中胚胎: §f" + embryoCount + " 个",
                        "",
                        "§e装备类型:",
                        "§c武器 §7- 剑/斧/弓/法杖/匕首",
                        "§9护甲 §7- 头盔/胸甲/护腿/靴子",
                        "§e饰品 §7- 戒指/项链/法宝",
                        "",
                        hasEmbryo ? "§a§l点击打开精炼界面" : "§7背包中没有胚胎"
                )
                .build();

        pane.addItem(new GuiItem(refineButton, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                EmbryoSelectionGUI.open(player, plugin);
            });
        }), 3, 2);
    }

    /**
     * 强化装备按钮（第2行）
     */
    private void displayEnhanceButton(Player player, StaticPane pane) {
        // 检查背包中是否有装备
        int equipmentCount = countEquipments(player);
        boolean hasEquipment = equipmentCount > 0;

        Material material = hasEquipment ? Material.ENCHANTING_TABLE : Material.BARRIER;
        String nameColor = hasEquipment ? "§b§l" : "§7";
        
        ItemStack enhanceButton = new ItemBuilder(material)
                .name(nameColor + "强化装备")
                .lore(
                        "§7提升装备等级和属性",
                        "",
                        "§e背包中装备: §f" + equipmentCount + " 件",
                        "",
                        "§e强化机制:",
                        "§7- 消耗: §f100 + 等级×50 §7灵石",
                        "§7- 成功率: §f90% - 等级×3%",
                        "§7- 最高等级: §f+20",
                        "§7- 失败惩罚: §c10%概率降级",
                        "",
                        hasEquipment ? "§a§l点击打开强化界面" : "§7背包中没有装备"
                )
                .build();

        pane.addItem(new GuiItem(enhanceButton, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                EquipmentSelectionGUI.open(player, plugin);
            });
        }), 5, 2);
    }

    /**
     * 融合装备按钮（第2行）
     */
    private void displayFuseButton(Player player, StaticPane pane) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean canFuse = EmbryoParser.isEmbryo(mainHand) && EmbryoParser.isEmbryo(offHand);

        String nameColor = canFuse ? "§d§l" : "§7";

        ItemStack fuseButton = new ItemBuilder(Material.BREWING_STAND)
                .name(nameColor + "融合装备")
                .lore(
                        "§7合并两件胚胎提升品质",
                        "",
                        "§e前置条件:",
                        EmbryoParser.isEmbryo(mainHand) ? "§a✓ 主手持胚胎" : "§c✗ 主手需要胚胎",
                        EmbryoParser.isEmbryo(offHand) ? "§a✓ 副手持胚胎" : "§c✗ 副手需要胚胎",
                        "",
                        "§e融合机制:",
                        "§7- 品质必须相同",
                        "§7- 20%概率提升品质",
                        "§7- 活跃灵气可提升概率",
                        "",
                        canFuse ? "§a点击执行融合" : "§7请先准备好胚胎"
                )
                .build();

        pane.addItem(new GuiItem(fuseButton, event -> {
            player.closeInventory();
            player.performCommand("forge fuse");
        }), 7, 2);
    }

    /**
     * 显示快捷入口（第3行）
     */
    private void displayQuickActions(Player player, StaticPane pane) {
        // 配方图鉴按钮
        ItemStack recipeButton = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name("§e§l配方图鉴")
                .lore(
                        "§7查看所有可用配方",
                        "",
                        "§e已有配方: §f48个",
                        "",
                        "§8功能开发中..."
                )
                .build();
        pane.addItem(new GuiItem(recipeButton, event -> {
            player.sendMessage("§e配方图鉴功能正在开发中，敬请期待!");
            player.sendMessage("§7当前可使用 §a/forge make §7打开配方炼制界面");
        }), 2, 3);

        // 查看信息按钮
        ItemStack infoButton = new ItemBuilder(Material.SPYGLASS)
                .name("§b§l查看物品信息")
                .lore(
                        "§7查看手中物品的详细信息",
                        "",
                        "§e功能:",
                        "§7- 胚胎属性查看",
                        "§7- 装备属性查看",
                        "§7- 强化等级查看",
                        "",
                        "§a点击执行"
                )
                .build();
        pane.addItem(new GuiItem(infoButton, event -> {
            player.closeInventory();
            player.performCommand("forge info");
        }), 4, 3);

        // 装备命名按钮
        ItemStack nameButton = new ItemBuilder(Material.NAME_TAG)
                .name("§e§l装备命名")
                .lore(
                        "§7为下次炼制的装备设置名称",
                        "",
                        "§e命令: §f/forge name <名称>",
                        "§7示例: §f/forge name §c火焰之剑",
                        "",
                        "§a点击查看说明"
                )
                .build();
        pane.addItem(new GuiItem(nameButton, event -> {
            player.closeInventory();
            player.sendMessage("§e§l========== 装备命名 ==========");
            player.sendMessage("§7使用命令设置装备名称:");
            player.sendMessage("§a/forge name <名称>");
            player.sendMessage("");
            player.sendMessage("§7示例:");
            player.sendMessage("§f/forge name §c火焰之剑");
            player.sendMessage("§f/forge name §b冰霜护甲");
            player.sendMessage("");
            player.sendMessage("§7设置后，下次炼制将使用此名称");
            player.sendMessage("§e§l============================");
        }), 6, 3);
    }

    /**
     * 帮助按钮（第4行）
     */
    private void displayHelpButton(Player player, StaticPane pane) {
        ItemStack helpButton = new ItemBuilder(Material.BOOK)
                .name("§e§l炼器指南")
                .lore(
                        "§7查看炼器系统使用说明",
                        "",
                        "§e包含:",
                        "§7- 炼制流程说明",
                        "§7- 品质提升技巧",
                        "§7- 材料获取方式",
                        "",
                        "§a点击查看"
                )
                .build();

        pane.addItem(new GuiItem(helpButton, event -> {
            player.closeInventory();
            player.performCommand("forge help");
        }), 0, 4);
    }

    /**
     * 刷新按钮（第4行）
     */
    private void displayRefreshButton(Player player, StaticPane pane) {
        ItemStack refreshButton = new ItemBuilder(Material.CLOCK)
                .name("§f刷新")
                .lore("§7更新界面数据")
                .build();

        pane.addItem(new GuiItem(refreshButton, event -> {
            show(player);
            player.sendMessage("§a界面已刷新");
        }), 8, 4);
    }

    /**
     * 统计背包中的胚胎数量
     */
    private int countEmbryos(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && EmbryoParser.isEmbryo(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 统计背包中的装备数量
     */
    private int countEquipments(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isEquipment(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 检查是否是装备
     */
    private boolean isEquipment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        // 检查是否是胚胎（胚胎不算装备）
        if (EmbryoParser.isEmbryo(item)) {
            return false;
        }
        
        // 获取ItemMeta并检查Lore
        var meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            var lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("装备类型:") || line.contains("强化等级:") || line.contains("五行属性:")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
