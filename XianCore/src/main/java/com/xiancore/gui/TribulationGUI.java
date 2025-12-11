package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.tribulation.Tribulation;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 天劫进度GUI界面
 * 显示渡劫实时进度和统计
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TribulationGUI {

    private final XianCore plugin;
    private final Player player;
    private final Tribulation tribulation;

    public TribulationGUI(XianCore plugin, Player player, Tribulation tribulation) {
        this.plugin = plugin;
        this.player = player;
        this.tribulation = tribulation;
    }

    /**
     * 打开天劫进度界面
     */
    public static void open(Player player, XianCore plugin, Tribulation tribulation) {
        new TribulationGUI(plugin, player, tribulation).show();
    }

    private void show() {
        ChestGui gui = new ChestGui(5, "§c§l天劫进度");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addBackground(gui, 5, Material.RED_STAINED_GLASS_PANE);

        StaticPane contentPane = new StaticPane(0, 0, 9, 5);

        // 天劫信息
        displayTribulationInfo(contentPane);

        // 进度条
        displayProgressBar(contentPane);

        // 统计信息
        displayStatistics(contentPane);

        // 当前状态
        displayCurrentStatus(contentPane);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .lore("§7点击关闭界面")
                .build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 8, 4);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示天劫信息
     */
    private void displayTribulationInfo(StaticPane pane) {
        ItemStack tribulationItem = new ItemBuilder(Material.NETHER_STAR)
                .name("§c§l" + tribulation.getType().getDisplayName())
                .lore(
                        "§7" + tribulation.getType().getDescription(),
                        "",
                        "§e劫数等阶: §c" + tribulation.getType().getTier(),
                        "§e总波数: §c" + tribulation.getTotalWaves(),
                        "§e难度倍率: §c" + String.format("%.1fx", tribulation.getType().getDifficultyMultiplier()),
                        "",
                        "§e持续时间: §f" + tribulation.getDuration() + " 秒",
                        "§e渡劫范围: §f" + tribulation.getRange() + " 格",
                        "",
                        tribulation.isActive() ? "§a§l● 进行中" : "§c§l● 已结束"
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(tribulationItem), 4, 0);
    }

    /**
     * 显示进度条
     */
    private void displayProgressBar(StaticPane pane) {
        int current = tribulation.getCurrentWave();
        int total = tribulation.getTotalWaves();
        double progress = tribulation.getProgress();

        // 进度条长度为7格
        int barLength = 7;
        int filled = (int) (barLength * (progress / 100.0));

        for (int i = 0; i < barLength; i++) {
            Material material;
            String name;
            String lore;

            if (i < filled) {
                material = Material.LIME_STAINED_GLASS_PANE;
                name = "§a▮ 已完成";
                lore = "§e进度: §a" + String.format("%.1f%%", progress);
            } else {
                material = Material.RED_STAINED_GLASS_PANE;
                name = "§7▯ 未完成";
                lore = "§7等待劫雷...";
            }

            ItemStack progressItem = new ItemBuilder(material)
                    .name(name)
                    .lore(lore, "§7当前: §f" + current + "§7/§f" + total)
                    .build();

            pane.addItem(new GuiItem(progressItem), i + 1, 2);
        }
    }

    /**
     * 显示统计信息
     */
    private void displayStatistics(StaticPane pane) {
        // 劫雷次数
        ItemStack strikeItem = new ItemBuilder(Material.LIGHTNING_ROD)
                .name("§e§l劫雷统计")
                .lore(
                        "§7劫雷总数: §c" + tribulation.getLightningStrikes(),
                        "§7当前波数: §f" + tribulation.getCurrentWave(),
                        "§7剩余波数: §6" + tribulation.getRemainingWaves(),
                        "",
                        "§7下一波间隔: §f" + (tribulation.getWaveInterval() / 1000) + " 秒"
                )
                .build();
        pane.addItem(new GuiItem(strikeItem), 1, 3);

        // 伤害统计
        ItemStack damageItem = new ItemBuilder(Material.TOTEM_OF_UNDYING)
                .name("§c§l伤害统计")
                .lore(
                        "§7总伤害: §c" + String.format("%.1f", tribulation.getTotalDamage()),
                        "§7死亡次数: §c" + tribulation.getDeaths(),
                        "§7最低血量: §c" + String.format("%.1f", tribulation.getMinHealth()) + " §7/ 20.0",
                        "",
                        tribulation.isPerfect() ? "§a§l✓ 无伤记录!" : "§7已受伤"
                )
                .build();
        pane.addItem(new GuiItem(damageItem), 3, 3);

        // 预计评级
        String rating = tribulation.isCompleted() ?
                tribulation.calculateRating() : getEstimatedRating();
        String ratingColor = tribulation.getRatingColor();

        ItemBuilder ratingItemBuilder = new ItemBuilder(Material.DIAMOND)
                .name(ratingColor + "评级: " + rating)
                .lore(
                        tribulation.isCompleted() ?
                                "§7最终评级: " + ratingColor + rating :
                                "§7预估评级: " + ratingColor + rating,
                        "",
                        "§e评级说明:",
                        "§d§lS §7- 无死亡，血量80%以上",
                        "§6§lA §7- 无死亡，血量50%以上",
                        "§e§lB §7- 死亡1次或血量低于50%",
                        "§7§lC §7- 死亡2次以上",
                        "",
                        "§7评级影响奖励倍率"
                );

        // 条件添加发光效果
        if (rating.equals("S") || rating.equals("A")) {
            ratingItemBuilder.glow();
        }

        ItemStack ratingItem = ratingItemBuilder.build();
        pane.addItem(new GuiItem(ratingItem), 5, 3);

        // 奖励预览
        displayRewardPreview(pane);
    }

    /**
     * 显示当前状态
     */
    private void displayCurrentStatus(StaticPane pane) {
        double health = player.getHealth();
        double maxHealth = 20.0;

        ItemStack statusItem = new ItemBuilder(Material.RED_DYE)
                .name("§c§l当前状态")
                .lore(
                        "§e血量: " + getHealthBar() + " §c" + String.format("%.1f", health) + "§7/20.0",
                        "§e位置: §f" + (int) player.getLocation().getX() + ", " +
                                (int) player.getLocation().getY() + ", " +
                                (int) player.getLocation().getZ(),
                        "§e距中心: §f" + String.format("%.1f",
                            tribulation.getLocation().distance(player.getLocation())) + " 格",
                        "",
                        tribulation.isPlayerInRange(player.getLocation()) ?
                                "§a§l✓ 在范围内" : "§c§l✗ 超出范围!"
                )
                .build();
        pane.addItem(new GuiItem(statusItem), 7, 3);
    }

    /**
     * 显示奖励预览
     */
    private void displayRewardPreview(StaticPane pane) {
        long baseExp = (long) (10000 * tribulation.getType().getDifficultyMultiplier());
        int baseStones = (int) (100 * tribulation.getType().getTier());
        int baseSkillPoints = tribulation.getType().getTier();
        int baseActiveQi = 25 + (tribulation.getType().getTier() * 5);

        double multiplier = tribulation.isCompleted() ?
                tribulation.getRewardMultiplier() :
                getEstimatedMultiplier();

        long exp = (long) (baseExp * multiplier);

        ItemStack rewardItem = new ItemBuilder(Material.GOLD_INGOT)
                .name("§6§l奖励预览")
                .lore(
                        tribulation.isCompleted() ? "§a最终奖励:" : "§7预估奖励:",
                        "",
                        "§7- 修为: §b+" + exp + " §7(×" + String.format("%.1f", multiplier) + ")",
                        "§7- 灵石: §6+" + baseStones + "§7+",
                        "§7- 功法点: §d+" + baseSkillPoints + "§7+",
                        "§7- 活跃灵气: §a+" + baseActiveQi + "§7+",
                        "",
                        "§7评级越高，奖励越多!"
                )
                .glow()
                .build();
        pane.addItem(new GuiItem(rewardItem), 4, 4);
    }

    /**
     * 获取预估评级
     */
    private String getEstimatedRating() {
        if (tribulation.getDeaths() >= 2) {
            return "C";
        }
        if (tribulation.getDeaths() == 1) {
            return "B";
        }

        double healthPercent = tribulation.getMinHealth() / 20.0;
        if (healthPercent >= 0.8 && tribulation.isPerfect()) {
            return "S";
        } else if (healthPercent >= 0.5) {
            return "A";
        } else {
            return "B";
        }
    }

    /**
     * 获取预估倍率
     */
    private double getEstimatedMultiplier() {
        String rating = getEstimatedRating();
        return switch (rating) {
            case "S" -> 2.0;
            case "A" -> 1.5;
            case "B" -> 1.2;
            case "C" -> 1.0;
            default -> 1.0;
        };
    }

    /**
     * 获取血量条
     */
    private String getHealthBar() {
        double health = player.getHealth();
        double maxHealth = 20.0;
        double percent = health / maxHealth;

        int barLength = 10;
        int filled = (int) (barLength * percent);

        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                if (percent > 0.6) {
                    bar.append("§a█");
                } else if (percent > 0.3) {
                    bar.append("§e█");
                } else {
                    bar.append("§c█");
                }
            } else {
                bar.append("§8█");
            }
        }
        bar.append("§7]");

        return bar.toString();
    }
}
