package com.xiancore.systems.skill.shop;

import com.xiancore.XianCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * 功法商店配置管理器
 * 从 skill_shop.yml 加载所有商店配置
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillShopConfig {

    private static XianCore plugin;
    private static Map<String, SkillShopItem> items = new LinkedHashMap<>();
    private static Map<String, CategoryInfo> categories = new LinkedHashMap<>();
    
    // 商店基础配置
    private static String shopTitle = "§2§l功法商店";
    private static int shopSize = 54;
    private static boolean enabled = true;
    private static String currency = "spirit_stone";
    private static boolean allowDuplicatePurchase = true;
    
    // VIP折扣
    private static Map<String, Double> vipDiscounts = new HashMap<>();
    
    // 购买限制
    private static boolean dailyLimitEnabled = true;
    private static Map<String, Integer> dailyLimits = new HashMap<>();
    
    /**
     * 初始化配置
     */
    public static void initialize(XianCore pluginInstance) {
        plugin = pluginInstance;
        loadConfig();
    }

    /**
     * 从配置文件加载
     */
    public static void loadConfig() {
        items.clear();
        categories.clear();
        
        FileConfiguration config = plugin.getConfigManager().getConfig("skill_shop.yml");
        if (config == null) {
            plugin.getLogger().warning("无法加载 skill_shop.yml 配置文件！");
            return;
        }

        // 加载基础配置
        loadShopSettings(config);
        
        // 加载分类配置
        loadCategories(config);
        
        // 加载商品配置
        loadItems(config);
        
        // 加载VIP折扣
        loadVipDiscounts(config);
        
        // 加载购买限制
        loadPurchaseLimits(config);

        plugin.getLogger().info("§a✓ 功法商店配置加载完成 (共 " + items.size() + " 个商品, " + categories.size() + " 个分类)");
    }

    /**
     * 加载商店基础设置
     */
    private static void loadShopSettings(FileConfiguration config) {
        ConfigurationSection shopSection = config.getConfigurationSection("shop");
        if (shopSection != null) {
            shopTitle = shopSection.getString("title", "§2§l功法商店");
            shopSize = shopSection.getInt("size", 54);
            enabled = shopSection.getBoolean("enabled", true);
            currency = shopSection.getString("currency", "spirit_stone");
            allowDuplicatePurchase = shopSection.getBoolean("allow-duplicate-purchase", true);
        }
    }

    /**
     * 加载分类配置
     */
    private static void loadCategories(FileConfiguration config) {
        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) {
            return;
        }

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
            if (categorySection == null) continue;

            String displayName = categorySection.getString("display-name", categoryId);
            String iconStr = categorySection.getString("icon", "BOOK");
            int slot = categorySection.getInt("slot", 0);
            List<String> description = categorySection.getStringList("description");

            Material icon;
            try {
                icon = Material.valueOf(iconStr);
            } catch (IllegalArgumentException e) {
                icon = Material.BOOK;
                plugin.getLogger().warning("无效的图标材质: " + iconStr + " (分类: " + categoryId + ")");
            }

            categories.put(categoryId, new CategoryInfo(categoryId, displayName, icon, slot, description));
        }
    }

    /**
     * 加载商品配置
     */
    private static void loadItems(FileConfiguration config) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("skill_shop.yml 中没有找到 items 配置节！");
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) continue;

            try {
                // 读取基础信息
                String displayName = itemSection.getString("display-name", itemId);
                String skillId = itemSection.getString("skill-id", "");
                String category = itemSection.getString("category", "basic");
                int price = itemSection.getInt("price", 100);
                int stock = itemSection.getInt("stock", -1);
                int refreshTime = itemSection.getInt("refresh-time", -1);
                
                // 创建商品对象
                SkillShopItem item = new SkillShopItem(itemId, displayName, skillId, category, price, stock, refreshTime);
                
                // 读取可选信息
                if (itemSection.contains("required-realm")) {
                    item.setRequiredRealm(itemSection.getString("required-realm"));
                }
                
                if (itemSection.contains("required-level")) {
                    item.setRequiredLevel(itemSection.getInt("required-level"));
                }
                
                if (itemSection.contains("discount")) {
                    item.setDiscount(itemSection.getDouble("discount", 1.0));
                }
                
                // 读取描述
                if (itemSection.contains("lore")) {
                    List<String> lore = itemSection.getStringList("lore");
                    for (String line : lore) {
                        item.addLore(line);
                    }
                }
                
                // 读取图标
                if (itemSection.contains("icon")) {
                    String iconStr = itemSection.getString("icon");
                    try {
                        Material icon = Material.valueOf(iconStr);
                        item.setIcon(icon);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("无效的图标材质: " + iconStr + " (商品: " + itemId + ")");
                    }
                }
                
                items.put(itemId, item);
                
            } catch (Exception e) {
                plugin.getLogger().warning("加载商品配置失败: " + itemId);
                e.printStackTrace();
            }
        }
    }

    /**
     * 加载VIP折扣
     */
    private static void loadVipDiscounts(FileConfiguration config) {
        vipDiscounts.clear();
        ConfigurationSection vipSection = config.getConfigurationSection("discounts.vip-discount");
        if (vipSection != null && vipSection.getBoolean("enabled", true)) {
            vipDiscounts.put("vip1", vipSection.getDouble("vip1", 0.95));
            vipDiscounts.put("vip2", vipSection.getDouble("vip2", 0.90));
            vipDiscounts.put("vip3", vipSection.getDouble("vip3", 0.85));
        }
    }

    /**
     * 加载购买限制
     */
    private static void loadPurchaseLimits(FileConfiguration config) {
        dailyLimits.clear();
        ConfigurationSection limitsSection = config.getConfigurationSection("purchase-limits.daily-limit");
        if (limitsSection != null) {
            dailyLimitEnabled = limitsSection.getBoolean("enabled", true);
            dailyLimits.put("default", limitsSection.getInt("default", 10));
            dailyLimits.put("vip1", limitsSection.getInt("vip1", 15));
            dailyLimits.put("vip2", limitsSection.getInt("vip2", 20));
            dailyLimits.put("vip3", limitsSection.getInt("vip3", -1));
        }
    }

    /**
     * 重载配置
     */
    public static void reload() {
        plugin.getConfigManager().reloadConfig("skill_shop.yml");
        loadConfig();
    }

    // ========== Getter 方法 ==========

    /**
     * 获取所有商品
     */
    public static List<SkillShopItem> getAllItems() {
        return new ArrayList<>(items.values());
    }

    /**
     * 根据ID获取商品
     */
    public static SkillShopItem getItemById(String id) {
        return items.get(id);
    }

    /**
     * 根据分类获取商品
     */
    public static List<SkillShopItem> getItemsByCategory(String category) {
        List<SkillShopItem> result = new ArrayList<>();
        for (SkillShopItem item : items.values()) {
            if (item.getCategory().equals(category)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 获取所有分类
     */
    public static Map<String, CategoryInfo> getCategories() {
        return new HashMap<>(categories);
    }

    /**
     * 获取分类信息
     */
    public static CategoryInfo getCategory(String id) {
        return categories.get(id);
    }

    /**
     * 获取商店标题
     */
    public static String getShopTitle() {
        return shopTitle;
    }

    /**
     * 获取商店大小
     */
    public static int getShopSize() {
        return shopSize;
    }

    /**
     * 商店是否启用
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取货币类型
     */
    public static String getCurrency() {
        return currency;
    }

    /**
     * 是否允许重复购买
     */
    public static boolean isAllowDuplicatePurchase() {
        return allowDuplicatePurchase;
    }

    /**
     * 获取VIP折扣
     */
    public static double getVipDiscount(String vipLevel) {
        return vipDiscounts.getOrDefault(vipLevel, 1.0);
    }

    /**
     * 是否启用每日限制
     */
    public static boolean isDailyLimitEnabled() {
        return dailyLimitEnabled;
    }

    /**
     * 获取每日购买限制
     */
    public static int getDailyLimit(String playerGroup) {
        return dailyLimits.getOrDefault(playerGroup, dailyLimits.getOrDefault("default", 10));
    }

    /**
     * 分类信息类
     */
    public static class CategoryInfo {
        private final String id;
        private final String displayName;
        private final Material icon;
        private final int slot;
        private final List<String> description;

        public CategoryInfo(String id, String displayName, Material icon, int slot, List<String> description) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.slot = slot;
            this.description = description;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public int getSlot() { return slot; }
        public List<String> getDescription() { return description; }
    }
}

