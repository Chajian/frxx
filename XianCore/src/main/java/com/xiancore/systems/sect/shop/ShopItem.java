package com.xiancore.systems.sect.shop;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 商店物品
 * 定义可兑换的物品及其价格
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class ShopItem {

    private String id;                   // 物品ID
    private String displayName;          // 显示名称
    private Material material;           // 物品材质
    private int amount;                  // 数量
    private int contributionCost;        // 贡献点消耗
    private List<String> lore;           // 描述
    private String category;             // 分类 (资源/装备/材料/特殊)

    /**
     * 构造函数
     */
    public ShopItem(String id, String displayName, Material material, int amount,
                    int contributionCost, String category) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.amount = amount;
        this.contributionCost = contributionCost;
        this.category = category;
        this.lore = new ArrayList<>();
    }

    /**
     * 添加描述行
     */
    public ShopItem addLore(String line) {
        this.lore.add(line);
        return this;
    }

    /**
     * 创建物品堆
     */
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);

            List<String> fullLore = new ArrayList<>();
            fullLore.addAll(lore);
            fullLore.add("");
            fullLore.add("\u00a77\u4ef7\u683c: \u00a7e" + contributionCost + " \u00a76\u8d21\u732e\u70b9");
            fullLore.add("");
            fullLore.add("\u00a7e\u70b9\u51fb\u8d2d\u4e70");

            meta.setLore(fullLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 创建显示用的物品堆（带价格信息）
     */
    public ItemStack createDisplayItem(int playerContribution) {
        ItemStack item = createItemStack();
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            List<String> displayLore = new ArrayList<>(meta.getLore());

            // 检查是否买得起
            if (playerContribution >= contributionCost) {
                displayLore.add("\u00a7a\u53ef\u4ee5\u8d2d\u4e70");
            } else {
                displayLore.add("\u00a7c\u8d21\u732e\u70b9\u4e0d\u8db3");
            }

            meta.setLore(displayLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public int getContributionCost() {
        return contributionCost;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getCategory() {
        return category;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setContributionCost(int contributionCost) {
        this.contributionCost = contributionCost;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
