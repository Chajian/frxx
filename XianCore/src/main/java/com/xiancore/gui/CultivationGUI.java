package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 修炼界面
 * 显示玩家的修炼信息和突破选项
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class CultivationGUI {

    private final XianCore plugin;

    public CultivationGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开修炼界面
     */
    public static void open(Player player, XianCore plugin) {
        new CultivationGUI(plugin).show(player);
    }

    private void show(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        // 创建5行的GUI
        ChestGui gui = new ChestGui(5, "§6§l修炼系统");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 创建边框面板
        GUIUtils.addGrayBackground(gui, 5);

        // 创建内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 5);

        // 修炼信息展示区
        displayCultivationInfo(player, data, contentPane);

        // 突破按钮
        displayBreakthroughButton(player, data, contentPane);

        // 修炼进度
        displayProgress(player, data, contentPane);

        // 成功率详情与指南/刷新
        displaySuccessRateDetails(player, data, contentPane);
        displayGuide(contentPane);
        displayRefreshButton(player, contentPane);

        // 修炼控制与速率
        displayCultivationControls(player, data, contentPane);
        displayCultivationRate(player, data, contentPane);
        displayBossEntry(player, contentPane);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER).name("§c关闭").build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 4, 4);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示修炼信息
     */
    private void displayCultivationInfo(Player player, PlayerData data, StaticPane pane) {
        // 境界信息
        ItemStack realmItem = new ItemBuilder(Material.NETHER_STAR)
                .name("§6§l当前境界")
                .lore(
                        "§e境界: §f" + data.getFullRealmName(),
                        "§e修为: §f" + formatQi(data.getQi()),
                        "",
                        "§7境界决定你的实力上限")
                .glow()
                .build();
        pane.addItem(new GuiItem(realmItem), 2, 1);

        // 灵根信息
        ItemStack rootItem = new ItemBuilder(Material.DIAMOND)
                .name("§b§l灵根资质")
                .lore(
                        "§e灵根: " + data.getSpiritualRootDisplay(),
                        "§e五行属性: " + data.getSpiritualRootElements(),
                        "§e灵根值: §f" + formatPercentage(data.getSpiritualRoot()),
                        "",
                        "§e悟性: §f" + formatPercentage(data.getComprehension()),
                        "§e功法适配: §f" + formatPercentage(data.getTechniqueAdaptation()),
                        "",
                        "§7更高的资质提升突破成功率")
                .build();
        pane.addItem(new GuiItem(rootItem), 4, 1);

        // 统计信息
        ItemStack statsItem = new ItemBuilder(Material.BOOK)
                .name("§a§l修炼统计")
                .lore(
                        "§e突破尝试: §f" + data.getBreakthroughAttemptCount() + " 次",
                        "§e突破成功: §a" + data.getBreakthroughSuccessCount() + " 次",
                        data.getBreakthroughAttemptCount() > 0
                                ? "§e成功率: §6" + String.format("%.1f%%", data.getBreakthroughSuccessRate() * 100)
                                : "§7暂无突破记录",
                        "",
                        "§7记录你的修炼历程")
                .build();
        pane.addItem(new GuiItem(statsItem), 6, 1);

        // 活跃灵气信息
        String activeQiStatus = getActiveQiStatus(data.getActiveQi());
        ItemStack activeQiItem = new ItemBuilder(Material.GLOWSTONE_DUST)
                .name("§d§l活跃灵气")
                .lore(
                        "§e当前值: §f" + data.getActiveQi() + " §7/ 100",
                        "§e状态: " + activeQiStatus,
                        "",
                        "§7活跃灵气影响奇遇触发率",
                        "§7通过修炼、突破、完成任务获得",
                        "§7每日自动衰减 15%")
                .glow()
                .build();
        pane.addItem(new GuiItem(activeQiItem), 2, 0);
    }

    /**
     * 获取活跃灵气状态描述
     */
    private String getActiveQiStatus(long activeQi) {
        if (activeQi >= 80) {
            return "§a§l极其活跃";
        } else if (activeQi >= 60) {
            return "§2§l非常活跃";
        } else if (activeQi >= 40) {
            return "§e§l较为活跃";
        } else if (activeQi >= 20) {
            return "§6§l一般";
        } else {
            return "§7§l不活跃";
        }
    }

    /**
     * 显示突破按钮
     */
    private void displayBreakthroughButton(Player player, PlayerData data, StaticPane pane) {
        long currentQi = data.getQi();
        long requiredQi = getRequiredQi(data);
        boolean canBreakthrough = currentQi >= requiredQi;

        if (canBreakthrough) {
            // 可以突破
            double successRate = calculateBreakthroughChance(data);

            ItemStack breakthroughButton = new ItemBuilder(Material.EMERALD)
                    .name("§a§l尝试突破")
                    .lore(
                            "§e当前修为: §f" + formatQi(currentQi),
                            "§e需要修为: §f" + formatQi(requiredQi),
                            "",
                            "§e预估成功率: §6" + String.format("%.1f%%", successRate * 100),
                            "",
                            "§a✔ 修为充足，可以突破!",
                            "§7点击尝试突破境界")
                    .glow()
                    .build();

            pane.addItem(new GuiItem(breakthroughButton, event -> {
                // 执行突破
                plugin.getCultivationSystem().attemptBreakthrough(player);
                // 更新GUI而不是关闭（使用延迟任务防止UI闪烁）
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    show(player);
                }, 2L);
            }), 4, 3);

        } else {
            // 无法突破
            long deficit = requiredQi - currentQi;

            ItemStack breakthroughButton = new ItemBuilder(Material.REDSTONE)
                    .name("§c§l无法突破")
                    .lore(
                            "§e当前修为: §f" + formatQi(currentQi),
                            "§e需要修为: §f" + formatQi(requiredQi),
                            "§c还需: §f" + formatQi(deficit),
                            "",
                            "§c✘ 修为不足!",
                            "§7继续修炼积累修为")
                    .build();

            pane.addItem(new GuiItem(breakthroughButton), 4, 3);
        }
    }

    /**
     * 显示修炼进度
     */
    private void displayProgress(Player player, PlayerData data, StaticPane pane) {
        long currentQi = data.getQi();
        long requiredQi = getRequiredQi(data);
        double progress = Math.min(100.0, (double) currentQi / requiredQi * 100);

        // 进度条（使用不同颜色的玻璃板）
        int totalSlots = 7; // 进度条长度
        int filledSlots = (int) (totalSlots * progress / 100);

        for (int i = 0; i < totalSlots; i++) {
            Material material;
            String name;

            if (i < filledSlots) {
                material = Material.LIME_STAINED_GLASS_PANE;
                name = "§a▮";
            } else {
                material = Material.RED_STAINED_GLASS_PANE;
                name = "§7▯";
            }

            ItemStack progressItem = new ItemBuilder(material)
                    .name(name)
                    .lore("§e进度: §f" + String.format("%.1f%%", progress))
                    .build();

            pane.addItem(new GuiItem(progressItem), i + 1, 2);
        }
    }

    /**
     * 开始/暂停修炼按钮
     */
    private void displayCultivationControls(Player player, PlayerData data, StaticPane pane) {
        boolean cultivating = plugin.getCultivationSystem().isCultivating(player.getUniqueId());

        if (cultivating || data.isCultivating()) {
            ItemStack pauseItem = new ItemBuilder(Material.RED_DYE)
                    .name("§c§l暂停修炼")
                    .lore(
                            "§7当前状态: §a修炼中",
                            "§7点击暂停被动修炼")
                    .build();
            pane.addItem(new GuiItem(pauseItem, event -> {
                plugin.getCultivationSystem().pauseCultivation(player);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> show(player), 2L);
            }), 2, 3);
        } else {
            ItemStack startItem = new ItemBuilder(Material.LIME_DYE)
                    .name("§a§l开始修炼")
                    .lore(
                            "§7当前状态: §c未修炼",
                            "§7点击开始被动修炼（每分钟自动增长修为）")
                    .glow()
                    .build();
            pane.addItem(new GuiItem(startItem, event -> {
                plugin.getCultivationSystem().startCultivation(player);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> show(player), 2L);
            }), 2, 3);
        }
    }

    /**
     * 显示实时修炼速率
     */
    private void displayCultivationRate(Player player, PlayerData data, StaticPane pane) {
        long perMinute = plugin.getCultivationSystem().calculateQiGainPerMinute(player, data);
        ItemStack rateItem = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .name("§d§l修炼速率")
                .lore(
                        "§e实时速率: §f" + perMinute + " §7/ 分",
                        "§7速率受灵根、悟性、功法适配、环境与宗门加成影响",
                        "§7如需更新数值请点击刷新")
                .build();
        pane.addItem(new GuiItem(rateItem), 2, 4);
    }

    /**
     * 显示成功率详情卡片
     */
    private void displaySuccessRateDetails(Player player, PlayerData data, StaticPane pane) {
        double L = data.getSpiritualRoot();
        double P = data.getTechniqueAdaptation();
        double G = data.getComprehension();
        double D = getRealmDifficulty(data.getRealm());
        double successRate = calculateBreakthroughChance(data);

        ItemStack detailItem = new ItemBuilder(Material.PAPER)
                .name("§e§l成功率详情")
                .lore(
                        "§7基于当前资质的预估:",
                        "§e灵根: §f" + formatPercentage(L),
                        "§e悟性: §f" + formatPercentage(G),
                        "§e功法适配: §f" + formatPercentage(P),
                        "§e境界难度: §f" + String.format("%.1f", D),
                        "",
                        "§e综合成功率: §6" + String.format("%.1f%%", successRate * 100),
                        "§7提示: 资源与环境加成可进一步提高成功率")
                .build();
        pane.addItem(new GuiItem(detailItem), 6, 3);
    }

    /**
     * 显示修炼指南
     */
    private void displayGuide(StaticPane pane) {
        ItemStack guideItem = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§b§l修炼指南")
                .lore(
                        "§7获取修为: §f打坐/任务/丹药/奇遇",
                        "§7活跃灵气: §f提升奇遇率, 每日衰减",
                        "§7建议: §f修为接近阈值时再突破更稳",
                        "§7注意: §f高境界难度更大, 需更多资源")
                .build();
        pane.addItem(new GuiItem(guideItem), 6, 0);
    }

    /**
     * 刷新按钮
     */
    private void displayRefreshButton(Player player, StaticPane pane) {
        ItemStack refresh = new ItemBuilder(Material.CLOCK)
                .name("§f刷新")
                .lore("§7点击更新当前面板")
                .build();
        pane.addItem(new GuiItem(refresh, event -> show(player)), 8, 4);
    }

    /**
     * 获取突破所需修为
     */
    private long getRequiredQi(PlayerData data) {
        String realm = data.getRealm();
        int stage = data.getRealmStage();

        long baseQi = switch (realm) {
            case "炼气期" -> 1000L;
            case "筑基期" -> 5000L;
            case "结丹期" -> 50000L;
            case "元婴期" -> 500000L;
            case "化神期" -> 5000000L;
            case "炼虚期" -> 50000000L;
            case "合体期" -> 500000000L;
            case "大乘期" -> 5000000000L;
            default -> 1000L;
        };

        return (long) (baseQi * Math.pow(1.5, stage - 1));
    }

    /**
     * 计算突破成功率
     */
    private double calculateBreakthroughChance(PlayerData data) {
        double L = data.getSpiritualRoot();
        double P = data.getTechniqueAdaptation();
        double E = 0.5; // 环境灵气
        double S = 0.5; // 资源投入
        double G = data.getComprehension();
        double D = getRealmDifficulty(data.getRealm());

        double alpha = 1.5;
        return 1 - Math.exp(-alpha * L * P * E * S * G / D);
    }

    /**
     * 获取境界难度
     */
    private double getRealmDifficulty(String realm) {
        return switch (realm) {
            case "炼气期" -> 1.0;
            case "筑基期" -> 2.0;
            case "结丹期" -> 5.0;
            case "元婴期" -> 10.0;
            case "化神期" -> 20.0;
            case "炼虚期" -> 40.0;
            case "合体期" -> 80.0;
            case "大乘期" -> 160.0;
            default -> 1.0;
        };
    }

    /**
     * 格式化修为数值
     */
    private String formatQi(long qi) {
        if (qi >= 1_000_000_000) {
            return String.format("%.1f亿", qi / 1_000_000_000.0);
        } else if (qi >= 10_000) {
            return String.format("%.1f万", qi / 10_000.0);
        } else {
            return String.valueOf(qi);
        }
    }

    /**
     * 格式化百分比
     */
    private String formatPercentage(double value) {
        return String.format("%.1f%%", value * 100);
    }

    private void displayBossEntry(Player player, StaticPane pane) {
        ItemStack bossItem = new ItemBuilder(Material.WITHER_SKELETON_SKULL)
                .name("§c§l世界Boss")
                .lore(
                        "§7查看活跃Boss与刷新",
                        "§7查看伤害排行",
                        "§7传送到Boss"
                )
                .build();
        pane.addItem(new GuiItem(bossItem, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.performCommand("boss gui");
            }, 1L);
        }), 6, 4);
    }
}
