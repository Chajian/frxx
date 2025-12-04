package com.xiancore.systems.forge;

import com.xiancore.systems.forge.items.EquipmentType;
import lombok.Data;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * 炼制配方类
 * 定义从材料炼制装备的配方
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class ForgeRecipe {

    private String id;                      // 配方ID
    private String name;                    // 配方名称
    private List<RecipeIngredient> ingredients;  // 材料列表（1-4个）
    private EquipmentType equipmentType;    // 输出装备类型
    private String quality;                 // 输出品质
    private int baseSuccessRate;            // 基础成功率（0-100）
    private int spiritStoneCost;            // 灵石消耗
    private String element;                 // 五行属性（可选）
    private String description;             // 配方描述

    /**
     * 配方材料类
     */
    @Data
    public static class RecipeIngredient {
        private Material material;          // 材料类型
        private int amount;                 // 需要数量

        public RecipeIngredient(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public void setMaterial(Material material) {
            this.material = material;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }
    }

    /**
     * 检查玩家背包是否有足够的材料
     *
     * @param playerMaterials 玩家的材料 (Material -> 数量)
     * @return 是否满足配方要求
     */
    public boolean checkMaterials(Map<Material, Integer> playerMaterials) {
        for (RecipeIngredient ingredient : ingredients) {
            int playerAmount = playerMaterials.getOrDefault(ingredient.getMaterial(), 0);
            if (playerAmount < ingredient.getAmount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取配方所需材料的总数
     */
    public int getTotalIngredientCount() {
        return ingredients.stream().mapToInt(RecipeIngredient::getAmount).sum();
    }

    /**
     * 获取配方复杂度（材料种类数）
     */
    public int getComplexity() {
        return ingredients.size();
    }

    /**
     * 计算实际成功率
     * 基于基础成功率和材料复杂度
     *
     * @return 实际成功率（0-100）
     */
    public int calculateActualSuccessRate() {
        // 基础成功率
        int rate = baseSuccessRate;

        // 材料越多，成功率略微降低
        int penalty = (ingredients.size() - 1) * 5;
        rate = Math.max(10, rate - penalty);  // 最低10%

        return Math.min(100, rate);  // 最高100%
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public String getQuality() {
        return quality;
    }

    public int getBaseSuccessRate() {
        return baseSuccessRate;
    }

    public int getSpiritStoneCost() {
        return spiritStoneCost;
    }

    public String getElement() {
        return element;
    }

    public String getDescription() {
        return description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public void setEquipmentType(EquipmentType equipmentType) {
        this.equipmentType = equipmentType;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public void setBaseSuccessRate(int baseSuccessRate) {
        this.baseSuccessRate = baseSuccessRate;
    }

    public void setSpiritStoneCost(int spiritStoneCost) {
        this.spiritStoneCost = spiritStoneCost;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
