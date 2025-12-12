package com.xiancore.systems.boss.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
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
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Boss 系统 GUI
 * 显示活跃 Boss 列表和伤害排行
 * 使用 InventoryFramework 统一 GUI 框架
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 统一使用 IF 框架
 */
public class BossGUI {

    private final XianCore plugin;
    private final BossRefreshManager bossManager;
    private final DamageStatisticsManager damageManager;

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

        ChestGui gui = new ChestGui(6, "§c§lBoss 列表");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        if (activeBosses.isEmpty()) {
            ItemStack noData = new ItemBuilder(Material.BARRIER)
                    .name("§c当前无活跃 Boss")
                    .lore("§7等待 Boss 刷新...")
                    .build();
            contentPane.addItem(new GuiItem(noData), 4, 2);
        } else {
            int slot = 0;
            for (BossEntity boss : activeBosses) {
                if (slot >= 36) break;

                int row = 1 + (slot / 7);
                int col = 1 + (slot % 7);

                ItemStack bossItem = createBossItem(boss);
                final BossEntity finalBoss = boss;
                contentPane.addItem(new GuiItem(bossItem, event -> {
                    if (event.isLeftClick()) {
                        openBossDetailGUI(player, finalBoss.getBossUUID());
                    } else if (event.isRightClick()) {
                        teleportToBoss(player, finalBoss);
                    }
                }), col, row);

                slot++;
            }
        }

        // 刷新按钮
        ItemStack refreshBtn = new ItemBuilder(Material.COMPASS)
                .name("§e§l刷新列表")
                .lore("§7点击刷新 Boss 列表")
                .build();
        contentPane.addItem(new GuiItem(refreshBtn, event -> {
            openBossListGUI(player);
            player.sendMessage("§a已刷新 Boss 列表！");
        }), 4, 5);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
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

        ChestGui gui = new ChestGui(6, "§c§lBoss 详情 - " + boss.getMythicMobType());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // Boss 信息
        contentPane.addItem(new GuiItem(createBossInfoItem(boss)), 4, 0);

        // 显示伤害排行
        if (ranking == null || ranking.getTopN(50).isEmpty()) {
            ItemStack noData = new ItemBuilder(Material.BARRIER)
                    .name("§c暂无伤害记录")
                    .lore("§7还没有玩家对此 Boss 造成伤害")
                    .build();
            contentPane.addItem(new GuiItem(noData), 4, 2);
        } else {
            List<DamageRanking.RankingEntry> topDamagers = ranking.getTopN(28);
            int slot = 0;
            int rank = 1;

            for (DamageRanking.RankingEntry entry : topDamagers) {
                if (slot >= 28) break;

                int row = 1 + (slot / 7);
                int col = 1 + (slot % 7);

                ItemStack rankItem = createRankItem(rank, entry, boss);
                contentPane.addItem(new GuiItem(rankItem), col, row);

                rank++;
                slot++;
            }
        }

        // 返回按钮
        ItemStack backBtn = new ItemBuilder(Material.ARROW)
                .name("§e§l返回列表")
                .build();
        contentPane.addItem(new GuiItem(backBtn, event -> openBossListGUI(player)), 0, 5);

        // 刷新按钮
        ItemStack refreshBtn = new ItemBuilder(Material.COMPASS)
                .name("§e§l刷新排行")
                .build();
        contentPane.addItem(new GuiItem(refreshBtn, event -> {
            openBossDetailGUI(player, bossUUID);
            player.sendMessage("§a已刷新排行数据！");
        }), 4, 5);

        // 关闭按钮
        ItemStack closeBtn = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeBtn, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 传送到 Boss
     */
    private void teleportToBoss(Player player, BossEntity boss) {
        LivingEntity entity = boss.getBukkitEntity();
        if (entity != null && !entity.isDead()) {
            player.teleport(entity.getLocation());
            player.sendMessage("§a已传送到 Boss！");
            player.closeInventory();
        } else {
            player.sendMessage("§c该 Boss 已死亡或不存在！");
        }
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

        DamageRecord damageRecord = damageManager.getDamageRecords().get(boss.getBossUUID());
        if (damageRecord != null) {
            lore.add(String.format("§6参与者: §f%d 人", damageRecord.getParticipantCount()));
            lore.add(String.format("§6总伤害: §f%s", DF.format(damageRecord.getTotalDamage())));
        }

        lore.add("§7═══════════════════");
        lore.add("§a左键 §7查看伤害排行");
        lore.add("§e右键 §7传送到 Boss");

        Material material = getMaterialForTier(tier);

        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build();
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

        return new ItemBuilder(Material.NETHER_STAR)
                .name(name)
                .lore(lore)
                .glow()
                .build();
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

        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build();
    }

    /**
     * 获取 Boss 等级对应材质
     */
    private Material getMaterialForTier(BossTier tier) {
        return switch (tier) {
            case LEGENDARY -> Material.DRAGON_HEAD;
            case BOSS -> Material.WITHER_SKELETON_SKULL;
            case ELITE -> Material.ZOMBIE_HEAD;
            default -> Material.SKELETON_SKULL;
        };
    }
}
