package com.xiancore.boss.drop;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Boss 掉落表管理器
 * 管理 Boss 击杀后的物品掉落
 *
 * @author XianCore
 * @version 1.0
 */
public class DropTableManager {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, DropTable> dropTables;
    private final Random random;

    /**
     * 掉落物品配置
     */
    public static class DropItem {
        public Material material;
        public int chance; // 0-100
        public int minQuantity;
        public int maxQuantity;
        public Map<String, Object> metadata; // 额外信息

        public DropItem(Material material, int chance, int minQuantity, int maxQuantity) {
            this.material = material;
            this.chance = Math.max(0, Math.min(100, chance));
            this.minQuantity = Math.max(1, minQuantity);
            this.maxQuantity = Math.max(minQuantity, maxQuantity);
            this.metadata = new HashMap<>();
        }

        /**
         * 检查是否应该掉落
         */
        public boolean shouldDrop() {
            return new Random().nextInt(100) < chance;
        }

        /**
         * 获取随机数量
         */
        public int getRandomQuantity() {
            if (minQuantity == maxQuantity) {
                return minQuantity;
            }
            return minQuantity + new Random().nextInt(maxQuantity - minQuantity + 1);
        }
    }

    /**
     * 掉落表
     */
    public static class DropTable {
        public String tableId;
        public List<DropItem> items;
        public double totalChance;

        public DropTable(String tableId) {
            this.tableId = tableId;
            this.items = new ArrayList<>();
            this.totalChance = 0;
        }

        /**
         * 添加掉落物品
         */
        public void addItem(DropItem item) {
            items.add(item);
            totalChance += item.chance;
        }

        /**
         * 获取掉落物品列表
         */
        public List<ItemStack> generateDrops() {
            List<ItemStack> drops = new ArrayList<>();
            for (DropItem item : items) {
                if (item.shouldDrop()) {
                    int quantity = item.getRandomQuantity();
                    drops.add(new ItemStack(item.material, quantity));
                }
            }
            return drops;
        }
    }

    /**
     * 构造函数
     */
    public DropTableManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dropTables = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    /**
     * 注册掉落表
     */
    public void registerDropTable(DropTable table) {
        if (table == null || table.tableId == null) {
            logger.warning("✗ 无法注册掉落表: 参数为 null");
            return;
        }

        dropTables.put(table.tableId, table);
        logger.info("✓ 已注册掉落表: " + table.tableId + " (物品数: " + table.items.size() + ")");
    }

    /**
     * 获取掉落表
     */
    public DropTable getDropTable(String tableId) {
        DropTable table = dropTables.get(tableId);
        if (table == null) {
            logger.warning("✗ 掉落表不存在: " + tableId);
            return null;
        }
        return table;
    }

    /**
     * 生成 Boss 掉落物品
     */
    public List<ItemStack> generateDrops(String tableId, LivingEntity boss, Player killer) {
        DropTable table = getDropTable(tableId);
        if (table == null) {
            return new ArrayList<>();
        }

        List<ItemStack> drops = table.generateDrops();

        logger.info("✓ 生成掉落物品: " + tableId);
        logger.info("  Boss: " + boss.getName() + " | 击杀者: " + (killer != null ? killer.getName() : "未知"));
        logger.info("  掉落物品数: " + drops.size());

        return drops;
    }

    /**
     * 创建标准掉落表（示例）
     */
    public static DropTable createStandardDropTable(String tableId) {
        DropTable table = new DropTable(tableId);

        // 钻石 - 50% 概率，2-4个
        DropItem diamond = new DropItem(Material.DIAMOND, 50, 2, 4);
        table.addItem(diamond);

        // 翡翠 - 60% 概率，1-3个
        DropItem emerald = new DropItem(Material.EMERALD, 60, 1, 3);
        table.addItem(emerald);

        // 金锭 - 70% 概率，3-6个
        DropItem goldIngot = new DropItem(Material.GOLD_INGOT, 70, 3, 6);
        table.addItem(goldIngot);

        // 稀有掉落 - 龙蛋 - 5% 概率，1个
        DropItem dragonEgg = new DropItem(Material.DRAGON_EGG, 5, 1, 1);
        table.addItem(dragonEgg);

        // 经验瓶 - 100% 概率，5-10个
        DropItem expBottle = new DropItem(Material.EXPERIENCE_BOTTLE, 100, 5, 10);
        table.addItem(expBottle);

        return table;
    }

    /**
     * 验证掉落表中的所有物品
     */
    public boolean validateDropTable(DropTable table) {
        if (table == null || table.items.isEmpty()) {
            logger.warning("✗ 掉落表为空或 null: " + (table != null ? table.tableId : "null"));
            return false;
        }

        boolean valid = true;

        // 检查物品是否存在
        for (DropItem item : table.items) {
            if (item.material == Material.AIR) {
                logger.warning("⚠ 掉落表包含 AIR 物品: " + table.tableId);
                valid = false;
            }
            if (item.chance < 0 || item.chance > 100) {
                logger.warning("⚠ 概率无效 (" + item.chance + "%): " + item.material.name());
                valid = false;
            }
        }

        // 检查概率总和（可选）
        double totalChance = table.items.stream().mapToDouble(i -> i.chance).sum();
        if (totalChance > 500) {
            logger.warning("⚠ 掉落表概率总和过高: " + totalChance + "%");
        }

        if (valid) {
            logger.info("✓ 掉落表验证通过: " + table.tableId);
        } else {
            logger.warning("⚠ 掉落表验证有警告但可用: " + table.tableId);
        }

        return true;
    }

    /**
     * 添加掉落物品到表
     */
    public void addDropItem(String tableId, Material material, int chance, int minQty, int maxQty) {
        DropTable table = getDropTable(tableId);
        if (table == null) {
            logger.warning("✗ 掉落表不存在: " + tableId);
            return;
        }

        DropItem item = new DropItem(material, chance, minQty, maxQty);
        table.addItem(item);
        logger.info("✓ 已添加掉落物品: " + tableId + " -> " + material.name());
    }

    /**
     * 删除掉落表
     */
    public void removeDropTable(String tableId) {
        dropTables.remove(tableId);
        logger.info("✓ 已删除掉落表: " + tableId);
    }

    /**
     * 获取所有掉落表
     */
    public Collection<DropTable> getAllDropTables() {
        return new HashSet<>(dropTables.values());
    }

    /**
     * 获取掉落表数量
     */
    public int getTableCount() {
        return dropTables.size();
    }

    /**
     * 打印掉落表信息
     */
    public void printDropTableInfo(String tableId) {
        DropTable table = getDropTable(tableId);
        if (table == null) {
            return;
        }

        logger.info("=== 掉落表: " + tableId + " ===");
        logger.info("物品数: " + table.items.size());
        for (DropItem item : table.items) {
            logger.info("  [" + item.material.name() + "] " +
                       "概率: " + item.chance + "% " +
                       "数量: " + item.minQuantity + "-" + item.maxQuantity);
        }
        logger.info("概率总和: " + table.totalChance + "%");
    }

    /**
     * 清除所有掉落表
     */
    public void clearAll() {
        dropTables.clear();
        logger.info("✓ 已清除所有掉落表");
    }
}
