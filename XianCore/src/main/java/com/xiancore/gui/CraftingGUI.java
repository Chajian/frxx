package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.core.utils.QualityUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.ForgeRecipe;
import com.xiancore.systems.forge.crafting.CraftingService;
import com.xiancore.systems.forge.crafting.CraftingService.CraftingResult;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 炼制GUI界面
 * 提供1-4个材料槽位的炼制系统
 * 业务逻辑委托给 CraftingService
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 使用 Service 层分离业务逻辑
 */
public class CraftingGUI {

    private final XianCore plugin;
    private final Player player;
    private final CraftingService craftingService;
    private final Map<Integer, ItemStack> materialSlots;
    private String customName;
    private ForgeRecipe matchedRecipe;

    private static final int[] SLOT_POSITIONS_X = {1, 3, 5, 7};
    private static final int SLOT_Y = 1;

    public CraftingGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.craftingService = new CraftingService(plugin);
        this.materialSlots = new HashMap<>();
        this.customName = plugin.getForgeSystem().getPendingEquipmentName(player.getUniqueId());
    }

    /**
     * 打开炼制界面
     */
    public static void open(Player player, XianCore plugin) {
        new CraftingGUI(plugin, player).show();
    }

    private void show() {
        ChestGui gui = new ChestGui(6, "§6§l装备炼制台");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        displayMaterialSlots(contentPane);
        displayMatchedRecipe(contentPane);
        displayFunctionButtons(contentPane);

        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示材料槽位
     */
    private void displayMaterialSlots(StaticPane pane) {
        for (int i = 0; i < 4; i++) {
            final int slotIndex = i;
            ItemStack slotItem;

            if (materialSlots.containsKey(slotIndex)) {
                ItemStack material = materialSlots.get(slotIndex);
                slotItem = new ItemBuilder(material.getType())
                        .amount(material.getAmount())
                        .name("§e材料槽 " + (slotIndex + 1))
                        .lore(
                                "§7材料: §f" + craftingService.getMaterialName(material.getType()),
                                "§7数量: §f" + material.getAmount(),
                                "",
                                "§c左键 - 取出材料",
                                "§e右键 - 减少1个"
                        )
                        .build();
            } else {
                slotItem = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("§7材料槽 " + (slotIndex + 1))
                        .lore(
                                "§7空槽位",
                                "",
                                "§a手持材料点击放入"
                        )
                        .build();
            }

            pane.addItem(new GuiItem(slotItem, event -> {
                if (event.isLeftClick()) {
                    removeMaterial(slotIndex);
                } else if (event.isRightClick()) {
                    decreaseMaterial(slotIndex);
                }
            }), SLOT_POSITIONS_X[slotIndex], SLOT_Y);
        }

        ItemStack addButton = new ItemBuilder(Material.CHEST)
                .name("§a§l添加材料")
                .lore(
                        "§7从背包选择材料放入槽位",
                        "",
                        "§e点击打开背包选择"
                )
                .build();

        pane.addItem(new GuiItem(addButton, event -> openMaterialSelection()), 4, 3);
    }

    /**
     * 显示匹配的配方
     */
    private void displayMatchedRecipe(StaticPane pane) {
        matchedRecipe = craftingService.matchRecipe(materialSlots);

        if (matchedRecipe != null) {
            ItemStack recipeDisplay = new ItemBuilder(matchedRecipe.getEquipmentType().getMaterial())
                    .name("§a§l匹配配方: " + matchedRecipe.getName())
                    .lore(
                            "§7配方: §f" + matchedRecipe.getName(),
                            "§7装备类型: §f" + matchedRecipe.getEquipmentType().getDisplayName(),
                            "§7品质: " + QualityUtils.getColor(matchedRecipe.getQuality()) + matchedRecipe.getQuality(),
                            "§7五行: §f" + matchedRecipe.getElement(),
                            "",
                            "§e成功率: §a" + matchedRecipe.calculateActualSuccessRate() + "%",
                            "§e消耗灵石: §6" + matchedRecipe.getSpiritStoneCost(),
                            "",
                            matchedRecipe.getDescription().isEmpty() ? "" : "§7" + matchedRecipe.getDescription()
                    )
                    .glow()
                    .build();

            pane.addItem(new GuiItem(recipeDisplay), 4, 4);
        } else {
            ItemStack noRecipe = new ItemBuilder(Material.BARRIER)
                    .name("§c未匹配配方")
                    .lore(
                            "§7当前材料组合未匹配任何配方",
                            "",
                            "§e请尝试添加或更换材料"
                    )
                    .build();

            pane.addItem(new GuiItem(noRecipe), 4, 4);
        }
    }

    /**
     * 显示功能按钮
     */
    private void displayFunctionButtons(StaticPane pane) {
        boolean canCraft = matchedRecipe != null && craftingService.checkMaterials(matchedRecipe, materialSlots);

        ItemStack craftButton;
        if (canCraft) {
            craftButton = new ItemBuilder(Material.ANVIL)
                    .name("§a§l开始炼制")
                    .lore(
                            "§7点击开始炼制装备",
                            "",
                            "§e成功率: §a" + matchedRecipe.calculateActualSuccessRate() + "%",
                            "§e消耗灵石: §6" + matchedRecipe.getSpiritStoneCost(),
                            "",
                            "§a✓ 材料充足",
                            "§a点击炼制"
                    )
                    .glow()
                    .build();
        } else {
            craftButton = new ItemBuilder(Material.BARRIER)
                    .name("§c无法炼制")
                    .lore(
                            "§7请先放入材料并匹配配方",
                            "",
                            "§c材料不足或未匹配配方"
                    )
                    .build();
        }

        pane.addItem(new GuiItem(craftButton, event -> {
            if (canCraft) {
                performCrafting();
            } else {
                player.sendMessage("§c无法炼制! 请检查材料和配方");
            }
        }), 2, 5);

        ItemStack nameButton = new ItemBuilder(Material.NAME_TAG)
                .name("§e§l装备命名")
                .lore(
                        "§7为炼制的装备设置自定义名称",
                        "",
                        customName == null ? "§7当前: §f默认名称" : "§7当前: §f" + customName,
                        "",
                        "§e使用命令设置名称:",
                        "§a/forge name <名称>"
                )
                .build();

        pane.addItem(new GuiItem(nameButton, event -> {
            player.closeInventory();
            player.sendMessage("§e请使用命令设置装备名称: §a/forge name <名称>");
            player.sendMessage("§7示例: /forge name §c火焰之剑");
        }), 4, 5);

        ItemStack clearButton = new ItemBuilder(Material.LAVA_BUCKET)
                .name("§c§l清空槽位")
                .lore(
                        "§7清空所有材料槽位",
                        "§7材料将返还到背包",
                        "",
                        "§c点击清空"
                )
                .build();

        pane.addItem(new GuiItem(clearButton, event -> {
            clearAllSlots();
            show();
        }), 6, 5);
    }

    /**
     * 打开材料选择界面
     */
    private void openMaterialSelection() {
        ChestGui selectionGui = new ChestGui(4, "§6选择材料");
        selectionGui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(selectionGui, 4);

        StaticPane contentPane = new StaticPane(0, 0, 9, 4);

        Map<Material, Integer> availableMaterials = craftingService.scanPlayerInventory(player);
        int slot = 0;

        for (Map.Entry<Material, Integer> entry : availableMaterials.entrySet()) {
            if (slot >= 27) break;

            Material material = entry.getKey();
            int amount = entry.getValue();

            ItemStack materialItem = new ItemBuilder(material)
                    .name("§e" + craftingService.getMaterialName(material))
                    .lore(
                            "§7数量: §f" + amount,
                            "",
                            "§a左键 - 放入1个",
                            "§e右键 - 放入全部"
                    )
                    .build();

            int row = slot / 9;
            int col = slot % 9;

            contentPane.addItem(new GuiItem(materialItem, event -> {
                if (event.isLeftClick()) {
                    addMaterial(material, 1);
                } else if (event.isRightClick()) {
                    addMaterial(material, amount);
                }
                player.closeInventory();
                show();
            }), col, row);

            slot++;
        }

        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> show()), 4, 3);

        selectionGui.addPane(contentPane);
        selectionGui.show(player);
    }

    /**
     * 添加材料到槽位
     */
    private void addMaterial(Material material, int amount) {
        for (int i = 0; i < 4; i++) {
            if (!materialSlots.containsKey(i)) {
                if (craftingService.removeMaterialFromInventory(player, material, amount)) {
                    materialSlots.put(i, new ItemStack(material, amount));
                    player.sendMessage("§a已添加 " + amount + " 个 " + craftingService.getMaterialName(material) + " 到槽位 " + (i + 1));
                } else {
                    player.sendMessage("§c材料不足!");
                }
                return;
            }
        }
        player.sendMessage("§c槽位已满! 请先清空一个槽位");
    }

    /**
     * 移除槽位中的材料
     */
    private void removeMaterial(int slotIndex) {
        if (materialSlots.containsKey(slotIndex)) {
            ItemStack material = materialSlots.remove(slotIndex);
            player.getInventory().addItem(material);
            player.sendMessage("§a已取出材料: " + craftingService.getMaterialName(material.getType()) + " x" + material.getAmount());
            show();
        }
    }

    /**
     * 减少槽位中的材料
     */
    private void decreaseMaterial(int slotIndex) {
        if (materialSlots.containsKey(slotIndex)) {
            ItemStack material = materialSlots.get(slotIndex);
            if (material.getAmount() > 1) {
                material.setAmount(material.getAmount() - 1);
                player.getInventory().addItem(new ItemStack(material.getType(), 1));
                show();
            } else {
                removeMaterial(slotIndex);
            }
        }
    }

    /**
     * 清空所有槽位
     */
    private void clearAllSlots() {
        for (ItemStack material : materialSlots.values()) {
            player.getInventory().addItem(material);
        }
        materialSlots.clear();
        player.sendMessage("§a已清空所有槽位");
    }

    /**
     * 执行炼制 - 委托给 CraftingService
     */
    private void performCrafting() {
        CraftingResult result = craftingService.performCrafting(player, matchedRecipe, materialSlots, customName);

        player.closeInventory();

        if (!result.isExecuted()) {
            player.sendMessage("§c" + result.getErrorMessage());
            return;
        }

        // 显示炼制信息
        player.sendMessage("§b========== 炼制装备 ==========");
        player.sendMessage("§e配方: §f" + result.getRecipe().getName());
        player.sendMessage("§e成功率: §a" + result.getRecipe().calculateActualSuccessRate() + "%");
        player.sendMessage("§e消耗灵石: §6" + (result.getCostPaid() + result.getCostRefunded()));
        player.sendMessage("§b===========================");

        if (result.isSuccess()) {
            // 清空槽位（材料已消耗）
            materialSlots.clear();

            player.sendMessage("§a✓ 炼制成功!");
            player.sendMessage("§e获得了 " + QualityUtils.getColor(result.getRecipe().getQuality()) +
                    result.getRecipe().getEquipmentType().getDisplayName() + " [" + result.getRecipe().getQuality() + "]");
            player.sendMessage("§7活跃灵气 +" + result.getActiveQiGain());

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            // 清空槽位（材料损坏）
            materialSlots.clear();

            player.sendMessage("§c✗ 炼制失败!");
            player.sendMessage("§7材料在炼制过程中损坏了...");
            player.sendMessage("§7返还了 " + result.getCostRefunded() + " 灵石");

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        }
    }

    /**
     * 设置自定义名称
     */
    public void setCustomName(String name) {
        this.customName = name;
    }
}
