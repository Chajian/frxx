package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.skill.SkillDisplayService;
import com.xiancore.systems.skill.SkillDisplayService.SkillDetailInfo;
import com.xiancore.systems.skill.SkillDisplayService.SlotInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 功法界面
 * 提供功法学习、升级、施放等功能
 * 业务逻辑委托给 SkillDisplayService
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 使用 Service 层分离业务逻辑
 */
public class SkillGUI {

    private final XianCore plugin;
    private final SkillDisplayService displayService;

    public SkillGUI(XianCore plugin) {
        this.plugin = plugin;
        this.displayService = new SkillDisplayService(plugin);
    }

    /**
     * 打开功法主界面
     */
    public static void open(Player player, XianCore plugin) {
        new SkillGUI(plugin).show(player);
    }

    private void show(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        ChestGui gui = new ChestGui(6, "§9§l功法系统");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(gui, 6, Material.CYAN_STAINED_GLASS_PANE);

        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        displayPlayerInfo(player, data, contentPane);
        displayLearnedSkills(player, data, contentPane);
        displayFunctionButtons(player, contentPane);

        ItemStack closeButton = new ItemBuilder(Material.BARRIER).name("§c关闭").build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 4, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示玩家信息
     */
    private void displayPlayerInfo(Player player, PlayerData data, StaticPane pane) {
        SlotInfo slotInfo = displayService.getSlotInfo(player, data);

        ItemStack infoItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("§9§l我的功法")
                .lore(
                        "§e境界: §f" + data.getFullRealmName(),
                        "§e已学功法: §f" + slotInfo.getCurrentCount() + " 本",
                        slotInfo.getDisplayText(),
                        "",
                        "§7功法等级越高，威力越强",
                        "§7突破境界可解锁更多功法"
                )
                .glow()
                .build();
        pane.addItem(new GuiItem(infoItem), 4, 1);
    }

    /**
     * 显示已学习的功法
     */
    private void displayLearnedSkills(Player player, PlayerData data, StaticPane pane) {
        Map<String, Integer> skills = data.getSkills();

        if (skills == null || skills.isEmpty()) {
            ItemStack noSkillItem = new ItemBuilder(Material.PAPER)
                    .name("§7未学习功法")
                    .lore(
                            "§7你还没有学习任何功法",
                            "",
                            "§e通过以下方式获取功法:",
                            "§7- 完成奇遇任务",
                            "§7- 宗门商店兑换",
                            "§7- 击败Boss掉落",
                            "",
                            "§a点击查看可学功法"
                    )
                    .build();
            pane.addItem(new GuiItem(noSkillItem, event -> {
                player.sendMessage("§e你还没有学习任何功法，通过奇遇、宗门或Boss掉落获取!");
            }), 4, 3);
        } else {
            int slot = 0;
            int row = 2;
            int col = 1;

            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                if (slot >= 7) break;

                String skillId = entry.getKey();
                int level = entry.getValue();

                ItemStack skillItem = createSkillItem(player, skillId, level);
                pane.addItem(new GuiItem(skillItem, event -> {
                    if (event.isLeftClick()) {
                        openSkillDetail(player, skillId, level);
                    } else if (event.isRightClick()) {
                        player.sendMessage("§e尝试升级功法 " + skillId + "...");
                        plugin.getSkillSystem().upgradeSkill(player, skillId);
                    }
                }), col, row);

                col++;
                if (col > 7) {
                    col = 1;
                    row++;
                }
                slot++;
            }
        }
    }

    /**
     * 创建功法物品
     */
    private ItemStack createSkillItem(Player player, String skillId, int level) {
        boolean isOnCooldown = displayService.isOnCooldown(player, skillId);
        int remainingCooldown = displayService.getRemainingCooldown(player, skillId);

        Material material = displayService.getSkillMaterial(level, isOnCooldown);
        String color = displayService.getLevelColor(level);

        List<String> lore = new ArrayList<>();
        lore.add("§e等级: " + color + level + "/10");
        lore.add("§e类型: §f" + displayService.getSkillType(skillId));
        lore.add("");
        lore.add("§7" + displayService.getSkillDescription(skillId));
        lore.add("");

        if (isOnCooldown) {
            lore.add("§c§l冷却中: §f" + displayService.formatCooldownTime(remainingCooldown));
        } else {
            lore.add("§e冷却时间: §f" + displayService.getSkillCooldown(skillId) + "秒");
        }

        lore.add("§e消耗灵气: §f" + displayService.getSkillQiCost(skillId, level));
        lore.add("");

        if (level < 10) {
            lore.add("§a左键 §7- 查看详情");
            lore.add("§a右键 §7- 升级功法");
        } else {
            lore.add("§6已满级!");
            lore.add("§a左键 §7- 查看详情");
        }

        lore.add("");
        lore.add(isOnCooldown ? "§c§l✗ 冷却中，无法使用" : "§a§l✓ 准备就绪");

        ItemBuilder builder = new ItemBuilder(material)
                .name(color + "§l" + skillId)
                .lore(lore);

        if (level >= 5 && !isOnCooldown) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * 显示功能按钮
     */
    private void displayFunctionButtons(Player player, StaticPane pane) {
        // 学习功法按钮
        ItemStack learnButton = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§a§l学习功法")
                .lore(
                        "§7查看可学习的功法",
                        "",
                        "§e功法来源:",
                        "§7- 功法秘籍 (物品)",
                        "§7- 宗门传授",
                        "§7- 奇遇获得",
                        "",
                        "§a点击查看可学功法"
                )
                .build();

        pane.addItem(new GuiItem(learnButton, event -> {
            player.sendMessage("§e你可以通过以下方式学习新功法:");
            player.sendMessage("§7- 完成奇遇任务获得");
            player.sendMessage("§7- 宗门成员传授");
            player.sendMessage("§7- 击败Boss掉落");
        }), 1, 4);

        // 快捷键绑定按钮
        ItemStack bindButton = new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name("§e§l快捷键绑定")
                .lore(
                        "§7将功法绑定到快捷栏",
                        "",
                        "§e功能:",
                        "§7- 绑定功法到槽位 §f1-9",
                        "§7- 切换槽位 + 按 §fF键 §7施放",
                        "§7- 快速解绑和调整",
                        "",
                        "§6战斗流畅度提升 300%!",
                        "",
                        "§e点击打开绑定界面"
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(bindButton, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                SkillBindGUI.open(player, plugin);
            }, 1L);
        }), 2, 4);

        // 功法商店按钮
        ItemStack shopButton = new ItemBuilder(Material.EMERALD)
                .name("§2§l功法商店")
                .lore(
                        "§7使用灵石购买功法秘籍",
                        "",
                        "§e可购买:",
                        "§7- 基础功法",
                        "§7- 进阶功法",
                        "§7- 特殊功法",
                        "",
                        "§2点击打开商店"
                )
                .build();

        pane.addItem(new GuiItem(shopButton, event -> {
            com.xiancore.systems.skill.shop.SkillShopGUI shopGUI =
                    new com.xiancore.systems.skill.shop.SkillShopGUI(plugin);
            shopGUI.open(player);
        }), 3, 4);

        // 功法传承按钮
        ItemStack inheritButton = new ItemBuilder(Material.DRAGON_EGG)
                .name("§d§l功法传承")
                .lore(
                        "§7将功法传授给其他玩家",
                        "",
                        "§e传承条件:",
                        "§7- 功法等级 §f5级 §7以上",
                        "§7- 目标玩家境界符合",
                        "§7- 消耗大量灵气",
                        "",
                        "§d点击传承功法",
                        "§8(功能开发中)"
                )
                .build();

        pane.addItem(new GuiItem(inheritButton, event -> {
            player.sendMessage("§e功法传承功能正在建设中，敬请期待!");
        }), 5, 4);

        // 遗忘功法按钮
        ItemStack forgetButton = new ItemBuilder(Material.REDSTONE)
                .name("§c§l遗忘功法")
                .lore(
                        "§7遗忘不需要的功法",
                        "",
                        "§e如何遗忘:",
                        "§7① 点击上方的功法图标",
                        "§7② 在详情页面中",
                        "§7③ 点击 §c遗忘功法 §7按钮",
                        "",
                        "§e遗忘效果:",
                        "§7- 返还部分功法点",
                        "§7- 可能返还部分灵石",
                        "§7- 自动解绑快捷键",
                        "",
                        "§c不可恢复，请谨慎操作!",
                        "",
                        "§e点击查看帮助"
                )
                .build();

        pane.addItem(new GuiItem(forgetButton, event -> {
            player.sendMessage("§e§l========== 如何遗忘功法 ==========");
            player.sendMessage("");
            player.sendMessage("§e方式一：通过GUI遗忘 §a(推荐)");
            player.sendMessage("§71. 点击上方的功法图标");
            player.sendMessage("§72. 在功法详情界面中");
            player.sendMessage("§73. 点击 §c遗忘功法 §7按钮");
            player.sendMessage("§74. 按提示输入确认命令");
            player.sendMessage("");
            player.sendMessage("§e方式二：直接使用命令");
            player.sendMessage("§7/skill forget <功法ID> confirm");
            player.sendMessage("");
            player.sendMessage("§c注意: 遗忘后有5分钟冷却，期间无法重新学习");
            player.sendMessage("§e§l================================");
        }), 7, 4);
    }

    /**
     * 打开功法详情界面
     */
    private void openSkillDetail(Player player, String skillId, int level) {
        SkillDetailInfo info = displayService.getSkillDetailInfo(skillId, level);

        ChestGui gui = new ChestGui(4, "§9§l" + info.getName() + " - 详情");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(gui, 4, Material.CYAN_STAINED_GLASS_PANE);

        StaticPane contentPane = new StaticPane(0, 0, 9, 4);

        // 功法详细信息
        ItemStack detailItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("§9§l" + info.getName())
                .lore(
                        "§e等级: §f" + info.getLevel() + "/" + info.getMaxLevel(),
                        "§e类型: §f" + info.getType(),
                        "",
                        "§e效果:",
                        "§7- 造成 §c" + info.getDamage() + " §7点伤害",
                        "§7- 额外灼烧效果 §c" + info.getBurnDuration() + "秒",
                        "§7- 攻击范围: §f" + info.getRange() + "格",
                        "",
                        "§e消耗: §f" + info.getQiCost() + " 灵气",
                        "§e冷却: §f" + info.getCooldown() + "秒",
                        "",
                        "§6升级到 " + (info.getLevel() + 1) + " 级需要:",
                        "§7- 灵石: §f" + info.getUpgradeSpiritStones(),
                        "§7- 功法点: §f" + info.getUpgradeSkillPoints()
                )
                .glow()
                .build();
        contentPane.addItem(new GuiItem(detailItem), 4, 1);

        // 升级按钮
        if (!info.isMaxLevel()) {
            ItemStack upgradeButton = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                    .name("§a§l升级功法")
                    .lore("§7提升功法等级", "", "§a点击升级")
                    .glow()
                    .build();
            contentPane.addItem(new GuiItem(upgradeButton, event -> {
                player.sendMessage("§e尝试升级功法 " + skillId + "...");
                plugin.getSkillSystem().upgradeSkill(player, skillId);
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    openSkillDetail(player, skillId, level);
                }, 1L);
            }), 2, 2);
        }

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW).name("§e返回").build();
        contentPane.addItem(new GuiItem(backButton, event -> show(player)), 4, 2);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER).name("§c关闭").build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 6, 2);

        // 遗忘功法按钮
        ItemStack forgetButton = new ItemBuilder(Material.REDSTONE)
                .name("§c§l遗忘功法")
                .lore(
                        "§7永久移除该功法",
                        "",
                        "§e返还:",
                        "§7- 部分功法点（默认50%）",
                        "§7- 可能返还部分灵石",
                        "",
                        "§e效果:",
                        "§7- 自动解绑快捷键",
                        "§7- 清除当前冷却",
                        "§7- 设置重学冷却（5分钟）",
                        "",
                        "§c不可恢复，请谨慎操作!",
                        "",
                        "§e点击遗忘"
                )
                .build();
        contentPane.addItem(new GuiItem(forgetButton, event -> {
            player.closeInventory();
            player.sendMessage("§c§l========== 遗忘功法确认 ==========");
            player.sendMessage("§e功法: §f" + skillId + " §7等级 " + level);
            player.sendMessage("");
            player.sendMessage("§c警告: 遗忘后该功法将被移除");
            player.sendMessage("§7• 将返还部分功法点");
            player.sendMessage("§7• 可能返还部分灵石（根据配置）");
            player.sendMessage("§7• 自动解绑所有快捷键");
            player.sendMessage("§7• 清除当前冷却");
            player.sendMessage("§7• 一段时间内无法重新学习");
            player.sendMessage("");
            player.sendMessage("§e如果确定要遗忘，请输入:");
            player.sendMessage("§f/skill forget " + skillId + " confirm");
            player.sendMessage("§c§l================================");
        }), 4, 3);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 静态方法打开功法详情（兼容旧调用）
     */
    public static void openSkillDetail(Player player, XianCore plugin, String skillName, int level) {
        new SkillGUI(plugin).openSkillDetail(player, skillName, level);
    }
}
