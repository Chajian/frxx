package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.tribulation.Tribulation;
import com.xiancore.systems.tribulation.TribulationType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 天劫选择GUI界面
 * 允许玩家选择并触发对应的天劫
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TribulationSelectionGUI {

    private final XianCore plugin;
    private final Player player;
    private final PlayerData data;

    public TribulationSelectionGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
    }

    /**
     * 打开天劫选择界面
     */
    public static void open(Player player, XianCore plugin) {
        new TribulationSelectionGUI(plugin, player).show();
    }

    private void show() {
        ChestGui gui = new ChestGui(6, "§c§l天劫系统 - 选择天劫");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        OutlinePane background = new OutlinePane(0, 0, 9, 6);
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 显示玩家当前状态
        displayPlayerStatus(contentPane);

        // 显示所有天劫类型
        displayTribulationTypes(contentPane);

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .lore("§7返回修炼界面")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> {
            CultivationGUI.open(player, plugin);
        }), 0, 5);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .lore("§7关闭界面")
                .build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示玩家当前状态
     */
    private void displayPlayerStatus(StaticPane pane) {
        // 当前境界
        TribulationType currentRealmTribulation = TribulationType.fromRealm(data.getRealm());
        String recommendedTrib = currentRealmTribulation != null ? currentRealmTribulation.getDisplayName() : "无";

        ItemStack statusItem = new ItemBuilder(Material.PLAYER_HEAD)
                .name("§6§l当前状态")
                .lore(
                        "§e玩家: §f" + player.getName(),
                        "§e境界: §f" + data.getFullRealmName(),
                        "§e推荐天劫: §c" + recommendedTrib,
                        "",
                        "§7你可以渡当前境界对应的天劫",
                        "§7或高一个等级的天劫"
                )
                .build();
        pane.addItem(new GuiItem(statusItem), 4, 0);

        // 检查是否有进行中的天劫
        Tribulation activeTribulation = plugin.getTribulationSystem().getTribulation(player.getUniqueId());
        if (activeTribulation != null) {
            ItemStack activeItem = new ItemBuilder(Material.LIGHTNING_ROD)
                    .name("§c§l进行中的天劫")
                    .lore(
                            "§e类型: §f" + activeTribulation.getType().getDisplayName(),
                            "§e波数: §f" + activeTribulation.getCurrentWave() + "/" + activeTribulation.getTotalWaves(),
                            "§e进度: §f" + String.format("%.1f%%", activeTribulation.getProgress()),
                            "",
                            "§c你已经在渡劫中!",
                            "§7点击查看天劫进度"
                    )
                    .glow()
                    .build();
            pane.addItem(new GuiItem(activeItem, event -> {
                TribulationGUI.open(player, plugin, activeTribulation);
            }), 2, 0);
        }

        // 冷却状态
        boolean onCooldown = plugin.getTribulationSystem().getTribulation(player.getUniqueId()) != null;
        if (onCooldown) {
            ItemStack cooldownItem = new ItemBuilder(Material.CLOCK)
                    .name("§e§l天劫状态")
                    .lore(
                            "§a无冷却",
                            "§7可以开始新的天劫"
                    )
                    .build();
            pane.addItem(new GuiItem(cooldownItem), 6, 0);
        } else {
            ItemStack readyItem = new ItemBuilder(Material.LIME_DYE)
                    .name("§a§l准备就绪")
                    .lore(
                            "§a可以开始渡劫!",
                            "§7选择下方的天劫类型开始"
                    )
                    .glow()
                    .build();
            pane.addItem(new GuiItem(readyItem), 6, 0);
        }
    }

    /**
     * 显示所有天劫类型
     */
    private void displayTribulationTypes(StaticPane pane) {
        TribulationType[] types = TribulationType.values();
        TribulationType currentRealmTribulation = TribulationType.fromRealm(data.getRealm());
        int currentTier = currentRealmTribulation != null ? currentRealmTribulation.getTier() : 0;

        int row = 2;
        int col = 1;

        for (TribulationType type : types) {
            boolean canAttempt = type.getTier() <= currentTier + 1;
            boolean isRecommended = type.getTier() == currentTier || type.getTier() == currentTier + 1;

            ItemStack tribItem = createTribulationItem(type, canAttempt, isRecommended);

            pane.addItem(new GuiItem(tribItem, event -> {
                if (canAttempt) {
                    handleTribulationStart(type);
                } else {
                    player.sendMessage("§c你的境界不足以渡此天劫!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }), col, row);

            col++;
            if (col >= 8) {
                col = 1;
                row++;
            }
        }
    }

    /**
     * 创建天劫物品
     */
    private ItemStack createTribulationItem(TribulationType type, boolean canAttempt, boolean isRecommended) {
        List<String> lore = new ArrayList<>();

        // 基础信息
        lore.add("§7" + type.getDescription());
        lore.add("");
        lore.add("§e劫数等阶: §c" + type.getTier());
        lore.add("§e劫雷波数: §c" + type.getWaves() + " 波");
        lore.add("§e难度倍率: §c" + String.format("%.1fx", type.getDifficultyMultiplier()));
        lore.add("§e波次间隔: §f3秒");
        lore.add("");

        // 奖励预览
        long baseExp = (long) (10000 * type.getDifficultyMultiplier());
        int baseStones = 100 * type.getTier();
        int baseSkillPoints = type.getTier();

        lore.add("§6基础奖励:");
        lore.add("§7- 修为: §b+" + formatReward(baseExp));
        lore.add("§7- 灵石: §6+" + baseStones + "+");
        lore.add("§7- 功法点: §d+" + baseSkillPoints + "+");
        lore.add("§7§o(实际奖励基于评级)");
        lore.add("");

        // 状态提示
        if (!canAttempt) {
            lore.add("§c✘ 境界不足，无法尝试");
            lore.add("§7需要达到更高境界");
        } else if (isRecommended) {
            lore.add("§a✔ 推荐尝试");
            lore.add("§7适合你当前的境界");
            lore.add("");
            lore.add("§e§l点击开始渡劫!");
        } else {
            lore.add("§a✔ 可以尝试");
            lore.add("§7你的境界足够渡此劫");
            lore.add("");
            lore.add("§e§l点击开始渡劫!");
        }

        // 选择材质和名称颜色
        Material material;
        String nameColor;

        if (!canAttempt) {
            material = Material.GRAY_DYE;
            nameColor = "§7§m";
        } else if (isRecommended) {
            material = getMaterialForTier(type.getTier());
            nameColor = "§6§l";
        } else {
            material = getMaterialForTier(type.getTier());
            nameColor = "§e";
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name(nameColor + type.getDisplayName())
                .lore(lore.toArray(new String[0]));

        if (isRecommended && canAttempt) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * 根据天劫等级获取对应材质
     */
    private Material getMaterialForTier(int tier) {
        return switch (tier) {
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.GOLD_INGOT;
            case 3 -> Material.EMERALD;
            case 4 -> Material.DIAMOND;
            case 5 -> Material.NETHERITE_INGOT;
            case 6 -> Material.NETHER_STAR;
            case 7 -> Material.DRAGON_BREATH;
            case 8 -> Material.ELYTRA;
            case 9 -> Material.BEACON;
            default -> Material.STONE;
        };
    }

    /**
     * 处理天劫开始
     */
    private void handleTribulationStart(TribulationType type) {
        player.closeInventory();

        // 检查境界
        TribulationType realmTribulation = TribulationType.fromRealm(data.getRealm());
        int currentTier = realmTribulation != null ? realmTribulation.getTier() : 0;

        if (type.getTier() > currentTier + 1) {
            player.sendMessage("§c你的境界不足以渡此天劫!");
            player.sendMessage("§7当前境界: §f" + data.getFullRealmName());
            if (realmTribulation != null) {
                player.sendMessage("§7推荐天劫: §f" + realmTribulation.getDisplayName());
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 尝试开始天劫
        boolean success = plugin.getTribulationSystem().startTribulation(player, type);

        if (success) {
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§c§l   天劫已开始!");
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");
            player.sendMessage("§e准备须知:");
            player.sendMessage("§7• 请保持在天劫范围内 (50格)");
            player.sendMessage("§7• 离开范围将导致渡劫失败");
            player.sendMessage("§7• 每 " + getWaveInterval(type) + " 秒一波劫雷");
            player.sendMessage("§7• 评级影响最终奖励倍率");
            player.sendMessage("");
            player.sendMessage("§6§l祝你好运!");

            // 播放成功音效
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);

            // 1秒后自动打开天劫进度GUI
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Tribulation tribulation = plugin.getTribulationSystem().getTribulation(player.getUniqueId());
                if (tribulation != null && player.isOnline()) {
                    TribulationGUI.open(player, plugin, tribulation);
                }
            }, 20L);
        } else {
            // 失败音效已在TribulationSystem中播放
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    /**
     * 获取波次间隔描述
     */
    private String getWaveInterval(TribulationType type) {
        if (type.getTier() >= 8) {
            return "1";
        } else if (type.getTier() >= 5) {
            return "2";
        } else {
            return "3";
        }
    }

    /**
     * 格式化奖励数值
     */
    private String formatReward(long value) {
        if (value >= 1_000_000_000) {
            return String.format("%.1f亿", value / 1_000_000_000.0);
        } else if (value >= 10_000) {
            return String.format("%.1f万", value / 10_000.0);
        } else {
            return String.valueOf(value);
        }
    }
}







