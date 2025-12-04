package com.xiancore.systems.boss.gui;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.damage.DamageRanking;
import com.xiancore.systems.boss.damage.DamageRecord;
import com.xiancore.systems.boss.damage.DamageStatistics;
import com.xiancore.systems.boss.damage.DamageStatisticsManager;
import com.xiancore.systems.boss.entity.BossEntity;
import com.xiancore.systems.boss.entity.BossTier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Boss 系统 GUI
 * 显示活跃 Boss 列表和伤害排行
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class BossGUI implements Listener {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;
    private final DamageStatisticsManager damageManager;

    private static final String TITLE_LIST = "§c§lBoss 列表";
    private static final String TITLE_DETAIL = "§c§lBoss 详情 - ";

    private static final DecimalFormat DF = new DecimalFormat("#,###");
    private static final DecimalFormat DF_PERCENT = new DecimalFormat("0.0");

    public BossGUI(XianCore plugin, BossRefreshManager bossManager, DamageStatisticsManager damageManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
        this.damageManager = damageManager;
    }

    /**
     * 打开 Boss 列表页
     */
    public void openBossListGUI(Player player) {
        List<BossEntity> activeBosses = new ArrayList<>(bossManager.getActiveBosses());

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_LIST);

        // 背景装饰
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
            inv.setItem(45 + i, filler);
        }

        // 刷新按钮
        inv.setItem(49, createItem(Material.COMPASS, "§e§l刷新列表", 
            Arrays.asList("§7点击刷新 Boss 列表")));

        // 关闭按钮
        inv.setItem(53, createItem(Material.BARRIER, "§c§l关闭", null));

        // 显示 Boss 列表
        if (activeBosses.isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c当前无活跃 Boss", 
                Arrays.asList("§7等待 Boss 刷新...")));
        } else {
            int slot = 10;
            for (BossEntity boss : activeBosses) {
                if (slot >= 44) break; // 防止越界

                ItemStack bossItem = createBossItem(boss);
                inv.setItem(slot, bossItem);

                slot++;
                if ((slot + 1) % 9 == 0) slot += 2; // 跳过边框
            }
        }

        player.openInventory(inv);
    }

    /**
     * 打开 Boss 详情页（伤害排行）
     */
    public void openBossDetailGUI(Player player, UUID bossUUID) {
        BossEntity boss = bossManager.getBossEntity(bossUUID);
        if (boss == null) {
            player.sendMessage("§c该 Boss 已不存在！");
            return;
        }

        DamageRanking ranking = damageManager.getDamageRankings().get(bossUUID);
        
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_DETAIL + boss.getMythicMobType());

        // 背景装饰
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
            inv.setItem(45 + i, filler);
        }

        // Boss 信息
        inv.setItem(4, createBossInfoItem(boss));

        // 返回按钮
        inv.setItem(45, createItem(Material.ARROW, "§e§l返回列表", null));

        // 刷新按钮
        inv.setItem(49, createItem(Material.COMPASS, "§e§l刷新排行", null));

        // 关闭按钮
        inv.setItem(53, createItem(Material.BARRIER, "§c§l关闭", null));

        // 显示伤害排行
        if (ranking == null || ranking.getTopN(50).isEmpty()) {
            inv.setItem(22, createItem(Material.BARRIER, "§c暂无伤害记录", 
                Arrays.asList("§7还没有玩家对此 Boss 造成伤害")));
        } else {
            List<DamageRanking.RankingEntry> topDamagers = ranking.getTopN(28);
            int slot = 10;
            int rank = 1;
            
            for (DamageRanking.RankingEntry entry : topDamagers) {
                if (slot >= 44) break;

                ItemStack rankItem = createRankItem(rank, entry, boss);
                inv.setItem(slot, rankItem);

                rank++;
                slot++;
                if ((slot + 1) % 9 == 0) slot += 2; // 跳过边框
            }
        }

        player.openInventory(inv);
    }

    /**
     * 创建 Boss 物品
     */
    private ItemStack createBossItem(BossEntity boss) {
        LivingEntity entity = boss.getBukkitEntity();
        BossTier tier = BossTier.fromLevel(boss.getTier());
        
        String name = tier.getColoredPrefix() + boss.getMythicMobType();
        List<String> lore = new ArrayList<>();

        lore.add("§7═══════════════════");
        lore.add("§e等级: " + tier.getDisplayName());
        
        if (entity != null && !entity.isDead()) {
            double health = entity.getHealth();
            double maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            double healthPercent = (health / maxHealth) * 100;
            
            lore.add(String.format("§c生命: §f%s§7/§f%s §7(%.1f%%)", 
                DF.format(health), DF.format(maxHealth), healthPercent));
            
            Location loc = entity.getLocation();
            lore.add(String.format("§b位置: §f%s §7(%d, %d, %d)", 
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        } else {
            lore.add("§c生命: §7已死亡");
        }

        // 伤害统计
        DamageRecord damageRecord = damageManager.getDamageRecords().get(boss.getBossUUID());
        if (damageRecord != null) {
            lore.add(String.format("§6参与者: §f%d 人", damageRecord.getParticipantCount()));
            lore.add(String.format("§6总伤害: §f%s", DF.format(damageRecord.getTotalDamage())));
        }

        lore.add("§7═══════════════════");
        lore.add("§a左键 §7查看伤害排行");
        lore.add("§e右键 §7传送到 Boss");

        Material material = tier == BossTier.LEGENDARY ? Material.DRAGON_HEAD :
                          tier == BossTier.BOSS ? Material.WITHER_SKELETON_SKULL :
                          tier == BossTier.ELITE ? Material.ZOMBIE_HEAD :
                          Material.SKELETON_SKULL;

        return createItem(material, name, lore);
    }

    /**
     * 创建 Boss 信息物品
     */
    private ItemStack createBossInfoItem(BossEntity boss) {
        LivingEntity entity = boss.getBukkitEntity();
        BossTier tier = BossTier.fromLevel(boss.getTier());
        
        String name = tier.getColoredPrefix() + boss.getMythicMobType();
        List<String> lore = new ArrayList<>();

        lore.add("§7═══════════════════");
        lore.add("§eUUID: §7" + boss.getBossUUID().toString().substring(0, 8) + "...");
        lore.add("§e等级: " + tier.getDisplayName());
        
        if (entity != null && !entity.isDead()) {
            double health = entity.getHealth();
            double maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
            double healthPercent = (health / maxHealth) * 100;
            
            lore.add(String.format("§c当前生命: §f%s", DF.format(health)));
            lore.add(String.format("§c最大生命: §f%s", DF.format(maxHealth)));
            lore.add(String.format("§c生命百分比: §f%.1f%%", healthPercent));
        }

        DamageStatistics stats = damageManager.getStatistics(boss.getBossUUID());
        if (stats != null) {
            lore.add("§7═══════════════════");
            lore.add(String.format("§6总伤害: §f%s", DF.format(stats.getTotalDamage())));
            lore.add(String.format("§6平均伤害: §f%s", DF.format(stats.getAverageDamage())));
            lore.add(String.format("§6最大单次: §f%s", DF.format(stats.getMaxSingleDamage())));
            lore.add(String.format("§6参与人数: §f%d", stats.getTotalPlayers()));
        }
        
        lore.add("§7═══════════════════");

        return createItem(Material.NETHER_STAR, name, lore);
    }

    /**
     * 创建排行物品
     */
    private ItemStack createRankItem(int rank, DamageRanking.RankingEntry entry, BossEntity boss) {
        String playerName = Bukkit.getOfflinePlayer(entry.getPlayerUUID()).getName();
        if (playerName == null) playerName = "未知玩家";

        Material material;
        String prefix;
        if (rank == 1) {
            material = Material.GOLD_INGOT;
            prefix = "§6§l";
        } else if (rank == 2) {
            material = Material.IRON_INGOT;
            prefix = "§7§l";
        } else if (rank == 3) {
            material = Material.COPPER_INGOT;
            prefix = "§c§l";
        } else {
            material = Material.PAPER;
            prefix = "§f";
        }

        String name = prefix + "#" + rank + " " + playerName;
        List<String> lore = new ArrayList<>();

        lore.add("§7═══════════════════");
        lore.add(String.format("§6造成伤害: §f%s", DF.format(entry.getDamage())));
        lore.add(String.format("§6伤害占比: §f%s%%", DF_PERCENT.format(entry.getPercentage() * 100)));
        
        DamageRecord record = damageManager.getDamageRecords().get(boss.getBossUUID());
        if (record != null) {
            int hitCount = record.getHitCount(entry.getPlayerUUID());
            double avgDamage = entry.getDamage() / Math.max(1, hitCount);
            lore.add(String.format("§6攻击次数: §f%d", hitCount));
            lore.add(String.format("§6平均伤害: §f%s", DF.format(avgDamage)));
        }
        
        lore.add("§7═══════════════════");

        return createItem(material, name, lore);
    }

    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * GUI 点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith("§c§lBoss")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // Boss 列表页
        if (title.equals(TITLE_LIST)) {
            handleListClick(player, event.getSlot(), clickedItem, event.isLeftClick());
        }
        // Boss 详情页
        else if (title.startsWith(TITLE_DETAIL)) {
            handleDetailClick(player, event.getSlot());
        }
    }

    /**
     * 处理列表页点击
     */
    private void handleListClick(Player player, int slot, ItemStack item, boolean isLeftClick) {
        // 刷新按钮
        if (slot == 49) {
            openBossListGUI(player);
            player.sendMessage("§a已刷新 Boss 列表！");
            return;
        }

        // 关闭按钮
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        // Boss 物品点击
        if (item.getType() == Material.SKELETON_SKULL || 
            item.getType() == Material.ZOMBIE_HEAD || 
            item.getType() == Material.WITHER_SKELETON_SKULL || 
            item.getType() == Material.DRAGON_HEAD) {
            
            // 从显示名称解析 Boss 类型
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            String displayName = meta.getDisplayName();
            BossEntity boss = findBossByDisplayName(displayName);
            
            if (boss != null) {
                if (isLeftClick) {
                    // 左键 - 查看详情
                    openBossDetailGUI(player, boss.getBossUUID());
                } else {
                    // 右键 - 传送
                    LivingEntity entity = boss.getBukkitEntity();
                    if (entity != null && !entity.isDead()) {
                        player.teleport(entity.getLocation());
                        player.sendMessage("§a已传送到 Boss！");
                        player.closeInventory();
                    } else {
                        player.sendMessage("§c该 Boss 已死亡或不存在！");
                    }
                }
            }
        }
    }

    /**
     * 处理详情页点击
     */
    private void handleDetailClick(Player player, int slot) {
        // 返回按钮
        if (slot == 45) {
            openBossListGUI(player);
            return;
        }

        // 刷新按钮
        if (slot == 49) {
            player.sendMessage("§a请返回列表重新查看");
            return;
        }

        // 关闭按钮
        if (slot == 53) {
            player.closeInventory();
        }
    }

    /**
     * 根据显示名称查找 Boss
     */
    private BossEntity findBossByDisplayName(String displayName) {
        for (BossEntity boss : bossManager.getActiveBosses()) {
            BossTier tier = BossTier.fromLevel(boss.getTier());
            String bossName = tier.getColoredPrefix() + boss.getMythicMobType();
            if (displayName.contains(boss.getMythicMobType()) || bossName.equals(displayName)) {
                return boss;
            }
        }
        return null;
    }
}
