package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.core.utils.QualityUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.forge.items.Equipment;
import com.xiancore.systems.forge.items.EquipmentParser;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 装备强化GUI
 * 提供可视化的装备强化操作界面
 *
 * @author AI Assistant
 * @version 2.0.0 - 支持从背包选择装备
 */
public class EnhanceGUI {

    private final XianCore plugin;
    private final Player player;
    private final ItemStack originalItem;  // 界面打开时的物品（用于验证）
    private final Equipment equipment;
    private final int currentLevel;
    private final int selectedSlot;  // 选中的装备槽位
    
    // 并发控制锁
    private static final ConcurrentHashMap<java.util.UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private boolean isProcessing = false;

    public EnhanceGUI(XianCore plugin, Player player, ItemStack item, int selectedSlot) {
        this.plugin = plugin;
        this.player = player;
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

        // 创建边框
        GUIUtils.addGrayBackground(gui, 5);

        StaticPane contentPane = new StaticPane(0, 0, 9, 5);

        // ========== 第0行：当前装备信息 ==========
        displayEquipmentInfo(contentPane);

        // ========== 第1行：等级预览（当前 vs 升级后）==========
        displayLevelPreview(contentPane);

        // ========== 第2行：强化信息（消耗、成功率）==========
        displayEnhanceInfo(contentPane);

        // ========== 第3行：进度条（可选）==========
        displaySuccessRateBar(contentPane);

        // ========== 第4行：操作按钮 ==========
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
        // 当前等级卡片（左）
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

        // 升级后等级卡片（右）
        if (currentLevel < 20) {
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
            // 已满级
            ItemStack maxCard = new ItemBuilder(Material.GOLD_BLOCK)
                    .name("§6§l已达最大等级!")
                    .lore(
                            "§7当前等级: §f+20",
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

        // 计算消耗和成功率
        int cost = calculateCost(currentLevel);
        int baseSuccessRate = calculateBaseSuccessRate(currentLevel);
        
        // 获取加成
        double forgeBoost = plugin.getActiveQiManager().getForgeBoost(player.getUniqueId());
        int activeQiBonus = forgeBoost > 0 ? (int)(forgeBoost * 100) : 0;
        
        int sectBonus = 0;
        if (data.getSectId() != null) {
            sectBonus = (int) plugin.getSectSystem().getFacilityManager()
                    .getForgeSuccessBonus(data.getSectId());
        }
        
        int finalSuccessRate = Math.min(100, baseSuccessRate + activeQiBonus + sectBonus);
        
        // 成功率颜色
        String rateColor = getSuccessRateColor(finalSuccessRate);
        
        ItemStack infoCard = new ItemBuilder(Material.PAPER)
                .name("§e§l强化信息")
                .lore(
                        "§e消耗灵石: §6" + cost,
                        "§7当前灵石: §f" + data.getSpiritStones(),
                        data.getSpiritStones() >= cost ? "§a✓ 灵石充足" : "§c✗ 灵石不足",
                        "",
                        "§e基础成功率: §f" + baseSuccessRate + "%",
                        activeQiBonus > 0 ? "§a  ⚡ 活跃灵气: +" + activeQiBonus + "%" : "",
                        sectBonus > 0 ? "§a  ⚒ 宗门设施: +" + sectBonus + "%" : "",
                        "§e最终成功率: " + rateColor + "§l" + finalSuccessRate + "%",
                        "",
                        "§c失败惩罚: §710%概率降级",
                        "",
                        currentLevel < 20 ? "§a准备就绪!" : "§6已达最大等级"
                )
                .build();

        pane.addItem(new GuiItem(infoCard), 4, 2);
    }

    /**
     * 显示成功率进度条（第3行）
     */
    private void displaySuccessRateBar(StaticPane pane) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) return;

        int baseSuccessRate = calculateBaseSuccessRate(currentLevel);
        double forgeBoost = plugin.getActiveQiManager().getForgeBoost(player.getUniqueId());
        int activeQiBonus = forgeBoost > 0 ? (int)(forgeBoost * 100) : 0;
        
        int sectBonus = 0;
        if (data.getSectId() != null) {
            sectBonus = (int) plugin.getSectSystem().getFacilityManager()
                    .getForgeSuccessBonus(data.getSectId());
        }
        
        int finalSuccessRate = Math.min(100, baseSuccessRate + activeQiBonus + sectBonus);

        // 进度条（7格）
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

        int cost = calculateCost(currentLevel);
        boolean canEnhance = currentLevel < 20 && data.getSpiritStones() >= cost;

        // 强化按钮
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

            pane.addItem(new GuiItem(enhanceButton, event -> {
                performEnhance();
            }), 3, 4);
        } else {
            // 无法强化
            String reason = currentLevel >= 20 ? "§6已达最大等级" : "§c灵石不足";
            
            ItemStack disabledButton = new ItemBuilder(Material.BARRIER)
                    .name("§c§l无法强化")
                    .lore(
                            reason,
                            "",
                            currentLevel >= 20 ? "§7装备已达+20满强化" : "§7需要 §6" + cost + " §7灵石",
                            currentLevel >= 20 ? "" : "§7当前只有 §6" + data.getSpiritStones() + " §7灵石"
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
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ForgeGUI.open(player, plugin);
            });
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
            // 重新获取选中槽位的物品
            ItemStack currentItem = player.getInventory().getItem(selectedSlot);
            if (currentItem == null || currentItem.getType().isAir() || !EquipmentParser.isEquipment(currentItem)) {
                player.sendMessage("§c装备已移除或改变，请重新选择!");
                player.closeInventory();
                EquipmentSelectionGUI.open(player, plugin);
                return;
            }
            
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                open(player, plugin, currentItem, selectedSlot);
            });
        }), 1, 4);
    }

    /**
     * 执行强化
     */
    private void performEnhance() {
        // 并发控制
        ReentrantLock lock = playerLocks.computeIfAbsent(player.getUniqueId(), k -> new ReentrantLock());
        
        if (!lock.tryLock()) {
            player.sendMessage("§c正在处理中，请稍候...");
            return;
        }

        if (isProcessing) {
            player.sendMessage("§c正在强化中，请勿重复点击!");
            lock.unlock();
            return;
        }

        isProcessing = true;

        try {
            // 重新验证物品（从选中槽位）
            ItemStack currentItem = player.getInventory().getItem(selectedSlot);
            if (currentItem == null || currentItem.getType().isAir()) {
                player.sendMessage("§c物品已移除，强化取消!");
                player.closeInventory();
                return;
            }
            
            // 验证是否是装备
            if (!EquipmentParser.isEquipment(currentItem)) {
                player.sendMessage("§c选中的物品不是装备，强化取消!");
                player.closeInventory();
                return;
            }
            
            // 验证物品是否相同（通过UUID或DisplayName）
            if (!isSameItem(originalItem, currentItem)) {
                player.sendMessage("§c物品已改变，强化取消!");
                player.closeInventory();
                return;
            }

            // 获取玩家数据
            PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
            if (data == null) {
                player.sendMessage("§c数据加载失败!");
                player.closeInventory();
                return;
            }

            // 重新验证等级
            int currentItemLevel = EquipmentParser.getEnhanceLevel(currentItem);
            if (currentItemLevel >= 20) {
                player.sendMessage("§c装备强化已达最大等级!");
                player.closeInventory();
                return;
            }

            // 计算消耗
            int cost = calculateCost(currentItemLevel);

            // 检查灵石
            if (data.getSpiritStones() < cost) {
                player.sendMessage("§c灵石不足! 需要: " + cost + " 当前: " + data.getSpiritStones());
                player.closeInventory();
                return;
            }

            // 计算成功率
            int baseRate = calculateBaseSuccessRate(currentItemLevel);
            double forgeBoost = plugin.getActiveQiManager().getForgeBoost(player.getUniqueId());
            int finalRate = baseRate;
            
            if (forgeBoost > 0) {
                finalRate += (int)(forgeBoost * 100);
                finalRate = Math.min(100, finalRate);
            }
            
            if (data.getSectId() != null) {
                int sectBonus = (int) plugin.getSectSystem().getFacilityManager()
                        .getForgeSuccessBonus(data.getSectId());
                finalRate += sectBonus;
                finalRate = Math.min(100, finalRate);
            }

            // 关闭GUI
            player.closeInventory();

            // 显示强化信息
            player.sendMessage("§b========== 装备强化 ==========");
            player.sendMessage("§e当前等级: §f+" + currentItemLevel);
            player.sendMessage("§e目标等级: §a+" + (currentItemLevel + 1));
            player.sendMessage("§e消耗灵石: §6" + cost);
            player.sendMessage("§e成功率: " + getSuccessRateColor(finalRate) + finalRate + "%");
            player.sendMessage("§b===========================");

            // 播放音效
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);

            // 执行强化
            Random random = new Random();
            if (random.nextInt(100) < finalRate) {
                // 强化成功
                data.removeSpiritStones(cost);
                EquipmentParser.setEnhanceLevel(currentItem, currentItemLevel + 1);
                EquipmentParser.updateEnhanceStats(currentItem, currentItemLevel + 1);
                
                // 更新槽位中的物品
                player.getInventory().setItem(selectedSlot, currentItem);

                // 增加活跃灵气
                int activeQiGain = Math.min(10, 3 + (currentItemLevel + 1) / 5);
                data.addActiveQi(activeQiGain);

                player.sendMessage("§a✓ 强化成功! 装备等级: +" + (currentItemLevel + 1));
                player.sendMessage("§7装备属性已提升");
                player.sendMessage("§7活跃灵气 +" + activeQiGain);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            } else {
                // 强化失败
                data.removeSpiritStones(cost / 2);  // 返还一半灵石
                player.sendMessage("§c✗ 强化失败! 消耗了 " + (cost / 2) + " 灵石");
                
                // 10%概率降级
                if (random.nextInt(100) < 10 && currentItemLevel > 0) {
                    EquipmentParser.setEnhanceLevel(currentItem, currentItemLevel - 1);
                    EquipmentParser.updateEnhanceStats(currentItem, currentItemLevel - 1);
                    
                    // 更新槽位中的物品
                    player.getInventory().setItem(selectedSlot, currentItem);
                    
                    player.sendMessage("§c§l✗✗ 装备降级了!");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                } else {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
                }
            }

            // 消耗活跃灵气加成
            if (forgeBoost > 0) {
                plugin.getActiveQiManager().consumeBoost(player.getUniqueId(),
                        com.xiancore.systems.activeqi.ActiveQiManager.ActiveQiBoostType.FORGE);
            }

            plugin.getDataManager().savePlayerData(data);

        } finally {
            isProcessing = false;
            lock.unlock();
        }
    }

    /**
     * 计算强化消耗
     */
    private int calculateCost(int level) {
        return 100 + level * 50;
    }

    /**
     * 计算基础成功率
     */
    private int calculateBaseSuccessRate(int level) {
        return Math.max(20, 90 - level * 3);
    }

    /**
     * 比对物品是否相同
     */
    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        if (!item1.hasItemMeta() || !item2.hasItemMeta()) {
            return item1.hasItemMeta() == item2.hasItemMeta();
        }
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        if (meta1 == null || meta2 == null) {
            return false;
        }
        
        // 比对DisplayName
        String name1 = meta1.hasDisplayName() ? meta1.getDisplayName() : "";
        String name2 = meta2.hasDisplayName() ? meta2.getDisplayName() : "";
        
        return name1.equals(name2);
    }

    /**
     * 获取成功率颜色
     */
    private String getSuccessRateColor(int rate) {
        if (rate >= 80) return "§a";      // 绿色 - 高成功率
        if (rate >= 60) return "§e";      // 黄色 - 中等
        if (rate >= 40) return "§6";      // 金色 - 较低
        return "§c";                       // 红色 - 低
    }
}






