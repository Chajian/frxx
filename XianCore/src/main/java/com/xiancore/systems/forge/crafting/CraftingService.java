package com.xiancore.systems.forge.crafting;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.forge.ForgeRecipe;
import com.xiancore.systems.forge.items.Equipment;
import com.xiancore.systems.forge.items.EquipmentFactory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 装备炼制服务
 * 负责炼制相关的业务逻辑，与 GUI 分离
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class CraftingService {

    private final XianCore plugin;
    private final Random random = new Random();

    // 支持的炼制材料列表
    private static final Set<Material> CRAFTING_MATERIALS = Set.of(
            Material.IRON_INGOT,
            Material.GOLD_INGOT,
            Material.DIAMOND,
            Material.EMERALD,
            Material.NETHERITE_INGOT,
            Material.STICK,
            Material.STRING,
            Material.LEATHER,
            Material.IRON_ORE,
            Material.GOLD_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.DEEPSLATE_GOLD_ORE
    );

    // 材料中文名称映射
    private static final Map<Material, String> MATERIAL_NAMES = Map.ofEntries(
            Map.entry(Material.NETHERITE_INGOT, "下界合金锭"),
            Map.entry(Material.DIAMOND, "钻石"),
            Map.entry(Material.EMERALD, "绿宝石"),
            Map.entry(Material.GOLD_INGOT, "金锭"),
            Map.entry(Material.IRON_INGOT, "铁锭"),
            Map.entry(Material.STICK, "木棍"),
            Map.entry(Material.STRING, "线"),
            Map.entry(Material.LEATHER, "皮革"),
            Map.entry(Material.IRON_ORE, "铁矿石"),
            Map.entry(Material.GOLD_ORE, "金矿石"),
            Map.entry(Material.DEEPSLATE_IRON_ORE, "深层铁矿石"),
            Map.entry(Material.DEEPSLATE_GOLD_ORE, "深层金矿石")
    );

    // 品质对应的活跃灵气奖励
    private static final Map<String, Integer> QUALITY_ACTIVE_QI_REWARDS = Map.of(
            "神品", 20,
            "仙品", 15,
            "宝品", 10,
            "灵品", 8,
            "凡品", 5
    );

    public CraftingService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 判断是否是炼制材料
     */
    public boolean isCraftingMaterial(Material material) {
        return CRAFTING_MATERIALS.contains(material);
    }

    /**
     * 获取材料中文名称
     */
    public String getMaterialName(Material material) {
        return MATERIAL_NAMES.getOrDefault(material, material.name());
    }

    /**
     * 扫描玩家背包中的可用材料
     */
    public Map<Material, Integer> scanPlayerInventory(Player player) {
        Map<Material, Integer> materials = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && isCraftingMaterial(item.getType())) {
                materials.put(item.getType(), materials.getOrDefault(item.getType(), 0) + item.getAmount());
            }
        }

        return materials;
    }

    /**
     * 从玩家背包移除材料
     */
    public boolean removeMaterialFromInventory(Player player, Material material, int amount) {
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
     * 匹配配方
     */
    public ForgeRecipe matchRecipe(Map<Integer, ItemStack> materialSlots) {
        if (materialSlots.isEmpty()) {
            return null;
        }

        Map<Material, Integer> materials = convertSlotsToMaterialMap(materialSlots);
        List<ForgeRecipe> matched = plugin.getForgeSystem().getRecipeManager().matchRecipes(materials);

        return matched.isEmpty() ? null : matched.get(0);
    }

    /**
     * 将槽位材料转换为材料Map
     */
    public Map<Material, Integer> convertSlotsToMaterialMap(Map<Integer, ItemStack> materialSlots) {
        Map<Material, Integer> materials = new HashMap<>();
        for (ItemStack item : materialSlots.values()) {
            materials.put(item.getType(), materials.getOrDefault(item.getType(), 0) + item.getAmount());
        }
        return materials;
    }

    /**
     * 检查材料是否足够
     */
    public boolean checkMaterials(ForgeRecipe recipe, Map<Integer, ItemStack> materialSlots) {
        if (recipe == null) {
            return false;
        }

        Map<Material, Integer> materials = convertSlotsToMaterialMap(materialSlots);
        return recipe.checkMaterials(materials);
    }

    /**
     * 检查是否可以炼制
     */
    public CraftingCheckResult checkCanCraft(Player player, ForgeRecipe recipe, Map<Integer, ItemStack> materialSlots) {
        if (recipe == null) {
            return CraftingCheckResult.failure("未匹配配方");
        }

        if (!checkMaterials(recipe, materialSlots)) {
            return CraftingCheckResult.failure("材料不足");
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return CraftingCheckResult.failure("数据加载失败");
        }

        int cost = recipe.getSpiritStoneCost();
        if (data.getSpiritStones() < cost) {
            return CraftingCheckResult.failure("灵石不足，需要 " + cost + " 灵石");
        }

        return CraftingCheckResult.success(recipe, cost, recipe.calculateActualSuccessRate());
    }

    /**
     * 执行炼制操作
     */
    public CraftingResult performCrafting(Player player, ForgeRecipe recipe, Map<Integer, ItemStack> materialSlots, String customName) {
        // 前置检查
        CraftingCheckResult checkResult = checkCanCraft(player, recipe, materialSlots);
        if (!checkResult.canCraft()) {
            return CraftingResult.failure(checkResult.getFailReason());
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return CraftingResult.failure("数据加载失败");
        }

        int cost = recipe.getSpiritStoneCost();
        int successRate = recipe.calculateActualSuccessRate();

        // 执行随机判定
        boolean success = random.nextInt(100) < successRate;

        if (success) {
            // 炼制成功
            data.removeSpiritStones(cost);

            // 创建装备
            Equipment equipment = EquipmentFactory.createFromRecipe(recipe);

            // 应用自定义名称
            if (customName != null && !customName.isEmpty()) {
                equipment.setCustomName(customName);
            }

            ItemStack equipmentItem = equipment.toItemStack(plugin);
            player.getInventory().addItem(equipmentItem);

            // 增加活跃灵气
            int activeQiGain = QUALITY_ACTIVE_QI_REWARDS.getOrDefault(recipe.getQuality(), 5);
            data.addActiveQi(activeQiGain);

            plugin.getDataManager().savePlayerData(data);

            return CraftingResult.success(recipe, equipment, cost, activeQiGain);
        } else {
            // 炼制失败
            int refund = cost / 2;
            data.removeSpiritStones(cost - refund);

            plugin.getDataManager().savePlayerData(data);

            return CraftingResult.fail(recipe, cost - refund, refund);
        }
    }

    /**
     * 获取品质对应的活跃灵气奖励
     */
    public int getActiveQiReward(String quality) {
        return QUALITY_ACTIVE_QI_REWARDS.getOrDefault(quality, 5);
    }

    /**
     * 炼制前检查结果
     */
    public static class CraftingCheckResult {
        private final boolean canCraft;
        private final String failReason;
        private final ForgeRecipe recipe;
        private final int cost;
        private final int successRate;

        private CraftingCheckResult(boolean canCraft, String failReason, ForgeRecipe recipe, int cost, int successRate) {
            this.canCraft = canCraft;
            this.failReason = failReason;
            this.recipe = recipe;
            this.cost = cost;
            this.successRate = successRate;
        }

        public static CraftingCheckResult success(ForgeRecipe recipe, int cost, int successRate) {
            return new CraftingCheckResult(true, null, recipe, cost, successRate);
        }

        public static CraftingCheckResult failure(String reason) {
            return new CraftingCheckResult(false, reason, null, 0, 0);
        }

        public boolean canCraft() { return canCraft; }
        public String getFailReason() { return failReason; }
        public ForgeRecipe getRecipe() { return recipe; }
        public int getCost() { return cost; }
        public int getSuccessRate() { return successRate; }
    }

    /**
     * 炼制结果
     */
    public static class CraftingResult {
        private final boolean success;
        private final boolean executed;
        private final String errorMessage;
        private final ForgeRecipe recipe;
        private final Equipment createdEquipment;
        private final int costPaid;
        private final int costRefunded;
        private final int activeQiGain;

        private CraftingResult(boolean success, boolean executed, String errorMessage,
                               ForgeRecipe recipe, Equipment createdEquipment,
                               int costPaid, int costRefunded, int activeQiGain) {
            this.success = success;
            this.executed = executed;
            this.errorMessage = errorMessage;
            this.recipe = recipe;
            this.createdEquipment = createdEquipment;
            this.costPaid = costPaid;
            this.costRefunded = costRefunded;
            this.activeQiGain = activeQiGain;
        }

        public static CraftingResult success(ForgeRecipe recipe, Equipment equipment, int costPaid, int activeQiGain) {
            return new CraftingResult(true, true, null, recipe, equipment, costPaid, 0, activeQiGain);
        }

        public static CraftingResult fail(ForgeRecipe recipe, int costPaid, int costRefunded) {
            return new CraftingResult(false, true, null, recipe, null, costPaid, costRefunded, 0);
        }

        public static CraftingResult failure(String errorMessage) {
            return new CraftingResult(false, false, errorMessage, null, null, 0, 0, 0);
        }

        public boolean isSuccess() { return success; }
        public boolean isExecuted() { return executed; }
        public String getErrorMessage() { return errorMessage; }
        public ForgeRecipe getRecipe() { return recipe; }
        public Equipment getCreatedEquipment() { return createdEquipment; }
        public int getCostPaid() { return costPaid; }
        public int getCostRefunded() { return costRefunded; }
        public int getActiveQiGain() { return activeQiGain; }
    }
}
