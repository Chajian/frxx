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
import com.xiancore.systems.forge.ForgeRecipe;
import com.xiancore.systems.forge.items.Equipment;
import com.xiancore.systems.forge.items.EquipmentFactory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 炼制GUI界面
 * 提供1-4个材料槽位的炼制系统
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class CraftingGUI {

    private final XianCore plugin;
    private final Player player;
    private final Map<Integer, ItemStack> materialSlots;  // 槽位索引 -> 材料
    private String customName;  // 自定义装备名称
    private ForgeRecipe matchedRecipe;  // 匹配的配方

    private static final int[] SLOT_POSITIONS_X = {1, 3, 5, 7};  // 4个槽位的X坐标
    private static final int SLOT_Y = 1;  // 槽位Y坐标

    public CraftingGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.materialSlots = new HashMap<>();
        // 从ForgeSystem获取待设置的装备名称
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

        // 背景
        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 显示材料槽位
        displayMaterialSlots(contentPane);

        // 显示匹配的配方
        displayMatchedRecipe(contentPane);

        // 功能按钮
        displayFunctionButtons(contentPane);

        // 关闭按钮
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
                // 槽位有材料
                ItemStack material = materialSlots.get(slotIndex);
                slotItem = new ItemBuilder(material.getType())
                        .amount(material.getAmount())
                        .name("§e材料槽 " + (slotIndex + 1))
                        .lore(
                                "§7材料: §f" + getMaterialName(material.getType()),
                                "§7数量: §f" + material.getAmount(),
                                "",
                                "§c左键 - 取出材料",
                                "§e右键 - 减少1个"
                        )
                        .build();
            } else {
                // 空槽位
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
                    // 取出材料
                    removeMaterial(slotIndex);
                } else if (event.isRightClick()) {
                    // 减少1个
                    decreaseMaterial(slotIndex);
                }
            }), SLOT_POSITIONS_X[slotIndex], SLOT_Y);
        }

        // 添加材料按钮
        ItemStack addButton = new ItemBuilder(Material.CHEST)
                .name("§a§l添加材料")
                .lore(
                        "§7从背包选择材料放入槽位",
                        "",
                        "§e点击打开背包选择"
                )
                .build();

        pane.addItem(new GuiItem(addButton, event -> {
            openMaterialSelection();
        }), 4, 3);
    }

    /**
     * 显示匹配的配方
     */
    private void displayMatchedRecipe(StaticPane pane) {
        // 检查当前材料是否匹配配方
        matchRecipe();

        if (matchedRecipe != null) {
            // 显示匹配的配方信息
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
            // 无匹配配方
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
        // 炼制按钮
        boolean canCraft = matchedRecipe != null && checkMaterials();

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

        // 命名按钮
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

        // 清空槽位按钮
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
            show();  // 刷新界面
        }), 6, 5);
    }

    /**
     * 打开材料选择界面
     */
    private void openMaterialSelection() {
        ChestGui selectionGui = new ChestGui(4, "§6选择材料");
        selectionGui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addBackground(selectionGui, 4);

        StaticPane contentPane = new StaticPane(0, 0, 9, 4);

        // 显示背包中的可用材料
        Map<Material, Integer> availableMaterials = scanPlayerInventory();
        int slot = 0;

        for (Map.Entry<Material, Integer> entry : availableMaterials.entrySet()) {
            if (slot >= 27) break;  // 最多显示3行

            Material material = entry.getKey();
            int amount = entry.getValue();

            ItemStack materialItem = new ItemBuilder(material)
                    .name("§e" + getMaterialName(material))
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
                show();  // 返回主界面
            }), col, row);

            slot++;
        }

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> {
            show();
        }), 4, 3);

        selectionGui.addPane(contentPane);
        selectionGui.show(player);
    }

    /**
     * 扫描玩家背包中的材料
     */
    private Map<Material, Integer> scanPlayerInventory() {
        Map<Material, Integer> materials = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && isCraftingMaterial(item.getType())) {
                materials.put(item.getType(), materials.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        return materials;
    }

    /**
     * 判断是否是炼制材料
     */
    private boolean isCraftingMaterial(Material material) {
        // 可以根据配置或预定义列表判断
        return material == Material.IRON_INGOT ||
               material == Material.GOLD_INGOT ||
               material == Material.DIAMOND ||
               material == Material.EMERALD ||
               material == Material.NETHERITE_INGOT ||
               material == Material.STICK ||
               material == Material.STRING ||
               material == Material.LEATHER ||
               material == Material.IRON_ORE ||
               material == Material.GOLD_ORE ||
               material == Material.DEEPSLATE_IRON_ORE ||
               material == Material.DEEPSLATE_GOLD_ORE;
    }

    /**
     * 添加材料到槽位
     */
    private void addMaterial(Material material, int amount) {
        // 找到第一个空槽位
        for (int i = 0; i < 4; i++) {
            if (!materialSlots.containsKey(i)) {
                // 从玩家背包扣除材料
                if (removeMaterialFromInventory(material, amount)) {
                    materialSlots.put(i, new ItemStack(material, amount));
                    player.sendMessage("§a已添加 " + amount + " 个 " + getMaterialName(material) + " 到槽位 " + (i + 1));
                } else {
                    player.sendMessage("§c材料不足!");
                }
                return;
            }
        }

        player.sendMessage("§c槽位已满! 请先清空一个槽位");
    }

    /**
     * 从玩家背包移除材料
     */
    private boolean removeMaterialFromInventory(Material material, int amount) {
        int remaining = amount;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                if (item.getAmount() >= remaining) {
                    item.setAmount(item.getAmount() - remaining);
                    return true;
                } else {
                    remaining -= item.getAmount();
                    item.setAmount(0);
                }
            }
        }

        return remaining == 0;
    }

    /**
     * 移除槽位中的材料
     */
    private void removeMaterial(int slotIndex) {
        if (materialSlots.containsKey(slotIndex)) {
            ItemStack material = materialSlots.remove(slotIndex);
            player.getInventory().addItem(material);
            player.sendMessage("§a已取出材料: " + getMaterialName(material.getType()) + " x" + material.getAmount());
            show();  // 刷新界面
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
                show();  // 刷新界面
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
     * 匹配配方
     */
    private void matchRecipe() {
        if (materialSlots.isEmpty()) {
            matchedRecipe = null;
            return;
        }

        // 将槽位材料转换为Map
        Map<Material, Integer> materials = new HashMap<>();
        for (ItemStack item : materialSlots.values()) {
            materials.put(item.getType(), materials.getOrDefault(item.getType(), 0) + item.getAmount());
        }

        // 匹配配方
        List<ForgeRecipe> matched = plugin.getForgeSystem().getRecipeManager().matchRecipes(materials);
        matchedRecipe = matched.isEmpty() ? null : matched.get(0);  // 取第一个匹配的配方
    }

    /**
     * 检查材料是否足够
     */
    private boolean checkMaterials() {
        if (matchedRecipe == null) {
            return false;
        }

        Map<Material, Integer> materials = new HashMap<>();
        for (ItemStack item : materialSlots.values()) {
            materials.put(item.getType(), materials.getOrDefault(item.getType(), 0) + item.getAmount());
        }

        return matchedRecipe.checkMaterials(materials);
    }

    /**
     * 执行炼制
     */
    private void performCrafting() {
        if (matchedRecipe == null) {
            player.sendMessage("§c未匹配配方!");
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return;
        }

        // 检查灵石
        int cost = matchedRecipe.getSpiritStoneCost();
        if (data.getSpiritStones() < cost) {
            player.sendMessage("§c灵石不足! 需要: " + cost + " 当前: " + data.getSpiritStones());
            return;
        }

        // 计算成功率
        int successRate = matchedRecipe.calculateActualSuccessRate();
        Random random = new Random();

        player.closeInventory();
        player.sendMessage("§b========== 炼制装备 ==========");
        player.sendMessage("§e配方: §f" + matchedRecipe.getName());
        player.sendMessage("§e成功率: §a" + successRate + "%");
        player.sendMessage("§e消耗灵石: §6" + cost);
        player.sendMessage("§b===========================");

        if (random.nextInt(100) < successRate) {
            // 炼制成功
            data.removeSpiritStones(cost);

            // 创建装备
            Equipment equipment = EquipmentFactory.createFromRecipe(matchedRecipe);

            // 应用自定义名称
            if (customName != null && !customName.isEmpty()) {
                equipment.setCustomName(customName);
            }

            ItemStack equipmentItem = equipment.toItemStack(plugin);
            player.getInventory().addItem(equipmentItem);

            // 增加活跃灵气（根据品质给予不同奖励）
            int activeQiGain = switch (matchedRecipe.getQuality()) {
                case "神品" -> 20;
                case "仙品" -> 15;
                case "宝品" -> 10;
                case "灵品" -> 8;
                default -> 5;
            };
            data.addActiveQi(activeQiGain);

            // 清空槽位（材料已消耗）
            materialSlots.clear();

            player.sendMessage("§a✓ 炼制成功!");
            player.sendMessage("§e获得了 " + QualityUtils.getColor(matchedRecipe.getQuality()) +
                    matchedRecipe.getEquipmentType().getDisplayName() + " [" + matchedRecipe.getQuality() + "]");
            player.sendMessage("§7活跃灵气 +" + activeQiGain);
        } else {
            // 炼制失败
            data.removeSpiritStones(cost / 2);  // 返还一半灵石

            // 材料损坏（清空槽位）
            materialSlots.clear();

            player.sendMessage("§c✗ 炼制失败!");
            player.sendMessage("§7材料在炼制过程中损坏了...");
            player.sendMessage("§7返还了 " + (cost / 2) + " 灵石");
        }

        plugin.getDataManager().savePlayerData(data);
    }

    /**
     * 获取材料名称
     */
    private String getMaterialName(Material material) {
        return switch (material) {
            case NETHERITE_INGOT -> "下界合金锭";
            case DIAMOND -> "钻石";
            case EMERALD -> "绿宝石";
            case GOLD_INGOT -> "金锭";
            case IRON_INGOT -> "铁锭";
            case STICK -> "木棍";
            case STRING -> "线";
            case LEATHER -> "皮革";
            default -> material.name();
        };
    }

    /**
     * 设置自定义名称
     */
    public void setCustomName(String name) {
        this.customName = name;
    }
}
