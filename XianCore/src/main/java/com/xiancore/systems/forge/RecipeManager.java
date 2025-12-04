package com.xiancore.systems.forge;

import com.xiancore.XianCore;
import com.xiancore.systems.forge.items.EquipmentType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 配方管理器
 * 负责加载和管理所有炼制配方
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class RecipeManager {

    private final XianCore plugin;
    private final Map<String, ForgeRecipe> recipes;  // 配方ID -> 配方

    public RecipeManager(XianCore plugin) {
        this.plugin = plugin;
        this.recipes = new HashMap<>();
    }

    /**
     * 加载配方
     */
    public void loadRecipes() {
        recipes.clear();

        FileConfiguration config = plugin.getConfigManager().getConfig("forge");
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");

        if (recipesSection == null) {
            plugin.getLogger().warning("§e! 未找到炼制配方配置 (recipes)");
            createDefaultRecipes();
            return;
        }

        for (String recipeId : recipesSection.getKeys(false)) {
            try {
                ForgeRecipe recipe = loadRecipeFromConfig(recipeId, recipesSection.getConfigurationSection(recipeId));
                if (recipe != null) {
                    recipes.put(recipeId, recipe);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("§c! 加载配方失败: " + recipeId + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("  §a✓ 加载了 " + recipes.size() + " 个炼制配方");
    }

    /**
     * 从配置加载单个配方
     */
    private ForgeRecipe loadRecipeFromConfig(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        ForgeRecipe recipe = new ForgeRecipe();
        recipe.setId(id);
        recipe.setName(section.getString("name", id));
        recipe.setDescription(section.getString("description", ""));
        recipe.setQuality(section.getString("quality", "凡品"));
        recipe.setBaseSuccessRate(section.getInt("success-rate", 80));
        recipe.setSpiritStoneCost(section.getInt("spirit-stone-cost", 100));
        recipe.setElement(section.getString("element", "无"));

        // 加载装备类型
        String typeStr = section.getString("equipment-type", "SWORD");
        try {
            recipe.setEquipmentType(EquipmentType.valueOf(typeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("§c! 无效的装备类型: " + typeStr + " in recipe " + id);
            return null;
        }

        // 加载材料列表
        List<ForgeRecipe.RecipeIngredient> ingredients = new ArrayList<>();
        List<Map<?, ?>> materialsList = section.getMapList("ingredients");

        for (Map<?, ?> materialMap : materialsList) {
            String materialName = (String) materialMap.get("material");

            // 安全地获取数量，避免类型转换问题
            int amount = 1;
            Object amountObj = materialMap.get("amount");
            if (amountObj != null) {
                if (amountObj instanceof Integer) {
                    amount = (Integer) amountObj;
                } else if (amountObj instanceof Number) {
                    amount = ((Number) amountObj).intValue();
                }
            }

            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                ingredients.add(new ForgeRecipe.RecipeIngredient(material, amount));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("§c! 无效的材料类型: " + materialName + " in recipe " + id);
            }
        }

        if (ingredients.isEmpty()) {
            plugin.getLogger().warning("§c! 配方没有材料: " + id);
            return null;
        }

        if (ingredients.size() > 4) {
            plugin.getLogger().warning("§e! 配方材料超过4个，将只使用前4个: " + id);
            ingredients = ingredients.subList(0, 4);
        }

        recipe.setIngredients(ingredients);

        return recipe;
    }

    /**
     * 创建默认配方（如果配置文件中没有）
     */
    private void createDefaultRecipes() {
        plugin.getLogger().info("  §e创建默认炼制配方...");

        // 示例：铁剑配方
        ForgeRecipe ironSword = new ForgeRecipe();
        ironSword.setId("iron_sword");
        ironSword.setName("§7炼制铁剑");
        ironSword.setEquipmentType(EquipmentType.SWORD);
        ironSword.setQuality("凡品");
        ironSword.setBaseSuccessRate(90);
        ironSword.setSpiritStoneCost(50);
        ironSword.setElement("金");
        ironSword.setDescription("基础的铁剑，适合新手使用");
        ironSword.setIngredients(List.of(
                new ForgeRecipe.RecipeIngredient(Material.IRON_INGOT, 3),
                new ForgeRecipe.RecipeIngredient(Material.STICK, 1)
        ));
        recipes.put(ironSword.getId(), ironSword);

        plugin.getLogger().info("  §a✓ 创建了 " + recipes.size() + " 个默认配方");
    }

    /**
     * 根据材料匹配配方
     *
     * @param materials 玩家提供的材料 (Material -> 数量)
     * @return 匹配的配方列表（按品质降序）
     */
    public List<ForgeRecipe> matchRecipes(Map<Material, Integer> materials) {
        return recipes.values().stream()
                .filter(recipe -> recipe.checkMaterials(materials))
                .sorted(Comparator.comparing(ForgeRecipe::getQuality).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 通过ID获取配方
     */
    public ForgeRecipe getRecipe(String id) {
        return recipes.get(id);
    }

    /**
     * 获取所有配方
     */
    public Collection<ForgeRecipe> getAllRecipes() {
        return recipes.values();
    }

    /**
     * 根据装备类型获取配方
     */
    public List<ForgeRecipe> getRecipesByType(EquipmentType type) {
        return recipes.values().stream()
                .filter(recipe -> recipe.getEquipmentType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 根据品质获取配方
     */
    public List<ForgeRecipe> getRecipesByQuality(String quality) {
        return recipes.values().stream()
                .filter(recipe -> recipe.getQuality().equals(quality))
                .collect(Collectors.toList());
    }
}
