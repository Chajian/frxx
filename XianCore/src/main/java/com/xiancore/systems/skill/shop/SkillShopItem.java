package com.xiancore.systems.skill.shop;

import lombok.Data;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 功法商店商品
 * 从配置文件加载的功法秘籍商品
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class SkillShopItem {

    private String id;                   // 商品ID
    private String displayName;          // 显示名称
    private String skillId;              // 对应的功法ID
    private String category;             // 分类
    private int price;                   // 价格（灵石）
    private int stock;                   // 库存（-1为无限）
    private int refreshTime;             // 刷新时间（秒，-1为不刷新）
    private String requiredRealm;        // 需求境界
    private int requiredLevel;           // 需求等级
    private double discount;             // 折扣（1.0为原价，0.8为8折）
    private List<String> lore;           // 描述
    private Material icon;               // 显示图标
    private int currentStock;            // 当前库存（运行时数据）

    /**
     * 构造函数
     */
    public SkillShopItem(String id, String displayName, String skillId, String category,
                         int price, int stock, int refreshTime) {
        this.id = id;
        this.displayName = displayName;
        this.skillId = skillId;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.refreshTime = refreshTime;
        this.discount = 1.0;
        this.lore = new ArrayList<>();
        this.icon = Material.BOOK;
        this.currentStock = stock;
        this.requiredLevel = 0;
    }

    /**
     * 添加描述行
     */
    public SkillShopItem addLore(String line) {
        this.lore.add(line);
        return this;
    }

    /**
     * 设置图标
     */
    public SkillShopItem setIcon(Material icon) {
        this.icon = icon;
        return this;
    }

    /**
     * 设置折扣
     */
    public SkillShopItem setDiscount(double discount) {
        this.discount = discount;
        return this;
    }

    /**
     * 设置境界要求
     */
    public SkillShopItem setRequiredRealm(String realm) {
        this.requiredRealm = realm;
        return this;
    }

    /**
     * 设置等级要求
     */
    public SkillShopItem setRequiredLevel(int level) {
        this.requiredLevel = level;
        return this;
    }

    /**
     * 获取实际价格（应用折扣后）
     */
    public int getActualPrice() {
        return (int) (price * discount);
    }

    /**
     * 应用VIP折扣
     */
    public int getActualPrice(double vipDiscount) {
        return (int) (price * discount * vipDiscount);
    }

    /**
     * 检查是否有库存
     */
    public boolean hasStock() {
        return stock == -1 || currentStock > 0;
    }

    /**
     * 减少库存
     */
    public void decreaseStock() {
        if (stock != -1 && currentStock > 0) {
            currentStock--;
        }
    }

    /**
     * 刷新库存
     */
    public void refreshStock() {
        if (stock != -1) {
            currentStock = stock;
        }
    }

    /**
     * 创建显示用的物品堆
     */
    public ItemStack createDisplayItem(int playerSpiritStones, boolean canAfford, boolean meetsRequirements) {
        ItemStack item = new ItemStack(icon, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);

            List<String> fullLore = new ArrayList<>();
            
            // 添加原始描述
            if (lore != null && !lore.isEmpty()) {
                fullLore.addAll(lore);
                fullLore.add("");
            }

            // 添加价格信息
            int actualPrice = getActualPrice();
            if (discount < 1.0) {
                fullLore.add("§7原价: §f" + price + " §7灵石");
                fullLore.add("§e折扣价: §6" + actualPrice + " §7灵石 §a(" + (int)(discount * 100) + "折)");
            } else {
                fullLore.add("§7价格: §e" + actualPrice + " §7灵石");
            }

            // 添加库存信息
            if (stock != -1) {
                if (currentStock > 0) {
                    fullLore.add("§7库存: §a" + currentStock + "§7/§f" + stock);
                } else {
                    fullLore.add("§7库存: §c已售罄");
                }
            }

            fullLore.add("");

            // 添加购买条件
            if (requiredRealm != null && !requiredRealm.isEmpty()) {
                fullLore.add("§7需求境界: §e" + requiredRealm);
            }
            if (requiredLevel > 0) {
                fullLore.add("§7需求等级: §e" + requiredLevel);
            }

            fullLore.add("");

            // 添加购买提示
            if (!hasStock()) {
                fullLore.add("§c§l已售罄");
                if (refreshTime > 0) {
                    fullLore.add("§7等待商店刷新");
                }
            } else if (!meetsRequirements) {
                fullLore.add("§c§l条件不足");
            } else if (!canAfford) {
                fullLore.add("§c§l灵石不足");
                fullLore.add("§7当前: §f" + playerSpiritStones + " §7需要: §e" + actualPrice);
            } else {
                fullLore.add("§a§l点击购买");
            }

            meta.setLore(fullLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * 创建简化版显示物品（用于分类预览）
     */
    public ItemStack createSimpleDisplay() {
        ItemStack item = new ItemStack(icon, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);
            List<String> simpleLore = new ArrayList<>();
            simpleLore.add("§7功法: §f" + skillId);
            simpleLore.add("§7价格: §e" + getActualPrice() + " §7灵石");
            meta.setLore(simpleLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    // ========== Getter方法（如果Lombok @Data没有生效，手动添加） ==========
    
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getCategory() {
        return category;
    }

    public int getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public int getRefreshTime() {
        return refreshTime;
    }

    public String getRequiredRealm() {
        return requiredRealm;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public double getDiscount() {
        return discount;
    }

    public List<String> getLore() {
        return lore;
    }

    public Material getIcon() {
        return icon;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }
}

