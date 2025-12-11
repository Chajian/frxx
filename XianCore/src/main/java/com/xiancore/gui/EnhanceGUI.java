package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.core.utils.QualityUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.enhance.EnhanceService;
import com.xiancore.systems.forge.enhance.EnhanceService.EnhanceRateInfo;
import com.xiancore.systems.forge.enhance.EnhanceService.EnhanceResult;
import com.xiancore.systems.forge.items.Equipment;
import com.xiancore.systems.forge.items.EquipmentParser;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 装备强化GUI
 * 提供可视化的装备强化操作界面
 * 业务逻辑委托给 EnhanceService
 *
 * @author Olivia Diaz
 * @version 3.0.0 - 使用 Service 层分离业务逻辑
 */
public class EnhanceGUI {

    private final XianCore plugin;
    private final Player player;
    private final EnhanceService enhanceService;
    private final ItemStack originalItem;
    private final Equipment equipment;
    private final int currentLevel;
    private final int selectedSlot;

    public EnhanceGUI(XianCore plugin, Player player, ItemStack item, int selectedSlot) {
        this.plugin = plugin;
        this.player = player;
        this.enhanceService = new EnhanceService(plugin);
        this.originalItem = item.clone();
        this.equipment = EquipmentParser.parseFromItemStack(item);
        this.currentLevel = EquipmentParser.getEnhanceLevel(item);
        this.selectedSlot = selectedSlot;
    }

    /**
     * 打开强化GUI
     */
    public static void open(Player player, XianCore plugin, ItemStack item, int selectedSlot) {
        new EnhanceGUI(plugin, player, item, selectedSlot).show();
    }

    private void show() {
        ChestGui gui = new ChestGui(5, "§b§l装备强化");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 5);

        StaticPane contentPane = new StaticPane(0, 0, 9, 5);

        displayEquipmentInfo(contentPane);
        displayLevelPreview(contentPane);
        displayEnhanceInfo(contentPane);
        displaySuccessRateBar(contentPane);
        displayActionButtons(contentPane);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示装备信息（第0行）
     */
    private void displayEquipmentInfo(StaticPane pane) {
        if (equipment == null) return;

        String qualityColor = QualityUtils.getColor(equipment.getQuality());

        ItemStack equipInfo = new ItemBuilder(equipment.getType().getMaterial())
                .name(qualityColor + (equipment.getCustomName() != null ? equipment.getCustomName() : equipment.getType().getDisplayName()))
                .lore(
                        "§e品质: " + qualityColor + equipment.getQuality(),
                        "§e当前强化: §6+" + currentLevel,
                        "§e五行属性: §f" + equipment.getElement(),
                        "",
                        "§7当前属性:",
                        "§c  ⚔ 攻击力: +" + equipment.getBaseAttack(),
                        "§9  ◈ 防御力: +" + equipment.getBaseDefense(),
                        "§a  ❤ 生命值: +" + equipment.getBaseHp(),
                        "§b  ✦ 灵力值: +" + equipment.getBaseQi()
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(equipInfo), 4, 0);
    }

    /**
     * 显示等级预览（第1行）
     */
    private void displayLevelPreview(StaticPane pane) {
        ItemStack currentCard = new ItemBuilder(Material.IRON_BLOCK)
                .name("§e§l当前等级: §f+" + currentLevel)
                .lore(
                        "§7当前属性加成:",
                        "§c  攻击: +" + (currentLevel * 10),
                        "§9  防御: +" + (currentLevel * 5),
                        "",
                        "§7强化次数: §f" + currentLevel + " 次"
                )
                .build();
        pane.addItem(new GuiItem(currentCard), 2, 1);

        if (currentLevel < EnhanceService.MAX_ENHANCE_LEVEL) {
            int nextLevel = currentLevel + 1;
            ItemStack nextCard = new ItemBuilder(Material.DIAMOND_BLOCK)
                    .name("§a§l升级后: §f+" + nextLevel)
                    .lore(
                            "§7升级后属性加成:",
                            "§c  攻击: +" + (nextLevel * 10) + " §a(+" + 10 + ")",
                            "§9  防御: +" + (nextLevel * 5) + " §a(+" + 5 + ")",
                            "",
                            "§a✓ 属性提升明显!"
                    )
                    .glow()
                    .build();
            pane.addItem(new GuiItem(nextCard), 6, 1);
        } else {
            ItemStack maxCard = new ItemBuilder(Material.GOLD_BLOCK)
                    .name("§6§l已达最大等级!")
                    .lore(
                            "§7当前等级: §f+" + EnhanceService.MAX_ENHANCE_LEVEL,
                            "§7属性加成:",
                            "§c  攻击: +200",
                            "§9  防御: +100",
                            "",
                            "§6已是顶级强化!"
                    )
                    .glow()
                    .build();
            pane.addItem(new GuiItem(maxCard), 6, 1);
        }
    }

    /**
     * 显示强化信息（第2行）
     */
    private void displayEnhanceInfo(StaticPane pane) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) return;

        int cost = enhanceService.calculateCost(currentLevel);
        EnhanceRateInfo rateInfo = enhanceService.calculateFinalSuccessRate(player, currentLevel);

        String rateColor = getSuccessRateColor(rateInfo.getFinalRate());

        ItemStack infoCard = new ItemBuilder(Material.PAPER)
                .name("§e§l强化信息")
                .lore(
                        "§e消耗灵石: §6" + cost,
                        "§7当前灵石: §f" + data.getSpiritStones(),
                        data.getSpiritStones() >= cost ? "§a✓ 灵石充足" : "§c✗ 灵石不足",
                        "",
                        "§e基础成功率: §f" + rateInfo.getBaseRate() + "%",
                        rateInfo.getActiveQiBonus() > 0 ? "§a  ⚡ 活跃灵气: +" + rateInfo.getActiveQiBonus() + "%" : "",
                        rateInfo.getSectBonus() > 0 ? "§a  ⚒ 宗门设施: +" + rateInfo.getSectBonus() + "%" : "",
                        "§e最终成功率: " + rateColor + "§l" + rateInfo.getFinalRate() + "%",
                        "",
                        "§c失败惩罚: §7" + EnhanceService.DOWNGRADE_CHANCE + "%概率降级",
                        "",
                        currentLevel < EnhanceService.MAX_ENHANCE_LEVEL ? "§a准备就绪!" : "§6已达最大等级"
                )
                .build();

        pane.addItem(new GuiItem(infoCard), 4, 2);
    }

    /**
     * 显示成功率进度条（第3行）
     */
    private void displaySuccessRateBar(StaticPane pane) {
        EnhanceRateInfo rateInfo = enhanceService.calculateFinalSuccessRate(player, currentLevel);
        int finalSuccessRate = rateInfo.getFinalRate();

        int totalSlots = 7;
        int filledSlots = (int) (totalSlots * finalSuccessRate / 100.0);

        for (int i = 0; i < totalSlots; i++) {
            Material material;
            String name;
            String loreText;

            if (i < filledSlots) {
                material = Material.LIME_STAINED_GLASS_PANE;
                name = "§a▮";
                loreText = "§a成功概率";
            } else {
                material = Material.RED_STAINED_GLASS_PANE;
                name = "§c▯";
                loreText = "§c失败概率";
            }

            ItemStack progressItem = new ItemBuilder(material)
                    .name(name)
                    .lore(
                            "§e成功率: §f" + finalSuccessRate + "%",
                            loreText
                    )
                    .build();

            pane.addItem(new GuiItem(progressItem), i + 1, 3);
        }
    }

    /**
     * 显示操作按钮（第4行）
     */
    private void displayActionButtons(StaticPane pane) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) return;

        int cost = enhanceService.calculateCost(currentLevel);
        boolean canEnhance = currentLevel < EnhanceService.MAX_ENHANCE_LEVEL && data.getSpiritStones() >= cost;

        if (canEnhance) {
            ItemStack enhanceButton = new ItemBuilder(Material.ANVIL)
                    .name("§a§l开始强化")
                    .lore(
                            "§7点击确认强化装备",
                            "",
                            "§e将消耗 §6" + cost + " §e灵石",
                            "§e强化等级: §f+" + currentLevel + " §a→ §f+" + (currentLevel + 1),
                            "",
                            "§a点击确认强化"
                    )
                    .glow()
                    .build();

            pane.addItem(new GuiItem(enhanceButton, event -> performEnhance()), 3, 4);
        } else {
            String reason = currentLevel >= EnhanceService.MAX_ENHANCE_LEVEL ? "§6已达最大等级" : "§c灵石不足";

            ItemStack disabledButton = new ItemBuilder(Material.BARRIER)
                    .name("§c§l无法强化")
                    .lore(
                            reason,
                            "",
                            currentLevel >= EnhanceService.MAX_ENHANCE_LEVEL ? "§7装备已达+" + EnhanceService.MAX_ENHANCE_LEVEL + "满强化" : "§7需要 §6" + cost + " §7灵石",
                            currentLevel >= EnhanceService.MAX_ENHANCE_LEVEL ? "" : "§7当前只有 §6" + data.getSpiritStones() + " §7灵石"
                    )
                    .build();

            pane.addItem(new GuiItem(disabledButton), 3, 4);
        }

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .lore("§7返回炼器主界面")
                .build();
        pane.addItem(new GuiItem(backButton, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> ForgeGUI.open(player, plugin));
        }), 5, 4);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
        pane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 7, 4);

        // 刷新按钮
        ItemStack refreshButton = new ItemBuilder(Material.CLOCK)
                .name("§f刷新")
                .lore("§7更新界面数据")
                .build();
        pane.addItem(new GuiItem(refreshButton, event -> {
            ItemStack currentItem = player.getInventory().getItem(selectedSlot);
            if (currentItem == null || currentItem.getType().isAir() || !EquipmentParser.isEquipment(currentItem)) {
                player.sendMessage("§c装备已移除或改变，请重新选择!");
                player.closeInventory();
                EquipmentSelectionGUI.open(player, plugin);
                return;
            }

            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> open(player, plugin, currentItem, selectedSlot));
        }), 1, 4);
    }

    /**
     * 执行强化 - 委托给 EnhanceService
     */
    private void performEnhance() {
        // 获取当前物品
        ItemStack currentItem = player.getInventory().getItem(selectedSlot);

        // 调用服务执行强化
        EnhanceResult result = enhanceService.performEnhance(player, currentItem, selectedSlot);

        // 关闭GUI
        player.closeInventory();

        // 处理结果
        if (!result.isExecuted()) {
            // 前置检查失败
            player.sendMessage("§c" + result.getErrorMessage());
            return;
        }

        // 显示强化过程信息
        EnhanceRateInfo rateInfo = enhanceService.calculateFinalSuccessRate(player, result.getPreviousLevel());

        player.sendMessage("§b========== 装备强化 ==========");
        player.sendMessage("§e当前等级: §f+" + result.getPreviousLevel());
        player.sendMessage("§e目标等级: §a+" + (result.getPreviousLevel() + 1));
        player.sendMessage("§e消耗灵石: §6" + result.getCostPaid());
        player.sendMessage("§e成功率: " + getSuccessRateColor(rateInfo.getFinalRate()) + rateInfo.getFinalRate() + "%");
        player.sendMessage("§b===========================");

        if (result.isSuccess()) {
            // 强化成功
            player.sendMessage("§a✓ 强化成功! 装备等级: +" + result.getNewLevel());
            player.sendMessage("§7装备属性已提升");
            if (result.getActiveQiGain() > 0) {
                player.sendMessage("§7活跃灵气 +" + result.getActiveQiGain());
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            // 强化失败
            player.sendMessage("§c✗ 强化失败! 消耗了 " + result.getCostPaid() + " 灵石");

            if (result.isDowngraded()) {
                player.sendMessage("§c§l✗✗ 装备降级了! 当前等级: +" + result.getNewLevel());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
            }
        }
    }

    /**
     * 获取成功率颜色
     */
    private String getSuccessRateColor(int rate) {
        if (rate >= 80) return "§a";
        if (rate >= 60) return "§e";
        if (rate >= 40) return "§6";
        return "§c";
    }
}
