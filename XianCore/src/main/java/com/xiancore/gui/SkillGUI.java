package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 功法界面
 * 提供功法学习、升级、施放等功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillGUI {

    private final XianCore plugin;

    public SkillGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开功法主界面
     */
    public static void open(Player player, XianCore plugin) {
        new SkillGUI(plugin).show(player);
    }

    private void show(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        // 创建6行的GUI
        ChestGui gui = new ChestGui(6, "§9§l功法系统");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 创建边框面板
        GUIUtils.addBackground(gui, 6, Material.CYAN_STAINED_GLASS_PANE);

        // 创建内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 显示玩家信息
        displayPlayerInfo(player, data, contentPane);

        // 显示已学功法
        displayLearnedSkills(player, data, contentPane);

        // 功能按钮
        displayFunctionButtons(player, data, contentPane);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER).name("§c关闭").build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 4, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示玩家信息
     */
    private void displayPlayerInfo(Player player, PlayerData data, StaticPane pane) {
        Map<String, Integer> skills = data.getSkills();
        int skillCount = skills != null ? skills.size() : 0;

        // 获取真实的槽位限制
        int maxSlots = plugin.getSkillSystem().getMaxSkillSlots(player, data);
        String slotInfo = "§e功法槽位: §f" + skillCount + "/" + maxSlots;

        // 根据使用率改变颜色
        if (skillCount >= maxSlots) {
            slotInfo = "§e功法槽位: §c" + skillCount + "/" + maxSlots + " §c(已满)";
        } else if (skillCount >= maxSlots * 0.8) {
            slotInfo = "§e功法槽位: §e" + skillCount + "/" + maxSlots + " §e(接近上限)";
        }

        ItemStack infoItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("§9§l我的功法")
                .lore(
                        "§e境界: §f" + data.getFullRealmName(),
                        "§e已学功法: §f" + skillCount + " 本",
                        slotInfo,
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
            // 没有学习任何功法
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
            // 显示已学习的功法列表
            int slot = 0;
            int row = 2;
            int col = 1;

            for (Map.Entry<String, Integer> entry : skills.entrySet()) {
                if (slot >= 7) break; // 最多显示7个

                String skillName = entry.getKey();
                int level = entry.getValue();

                ItemStack skillItem = createSkillItem(player, skillName, level);
                pane.addItem(new GuiItem(skillItem, event -> {
                    if (event.isLeftClick()) {
                        // 左键 - 查看详情（打开详情窗口，保持原GUI在后台）
                        openSkillDetail(player, plugin, skillName, level);
                    } else if (event.isRightClick()) {
                        // 右键 - 升级功法
                        player.sendMessage("§e尝试升级功法 " + skillName + "...");
                        // 升级逻辑可以通过命令或直接调用系统来实现
                        plugin.getSkillSystem().upgradeSkill(player, skillName);
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
    private ItemStack createSkillItem(Player player, String skillName, int level) {
        // 检查是否在冷却中
        int remainingCooldown = plugin.getSkillSystem().getCooldownManager().getRemainingCooldown(player, skillName);
        boolean isOnCooldown = remainingCooldown > 0;

        // 根据功法等级和冷却状态选择材料
        Material material;
        if (isOnCooldown) {
            // 冷却中使用灰色玻璃板
            material = Material.GRAY_STAINED_GLASS_PANE;
        } else {
            material = switch (level) {
                case 1, 2 -> Material.BOOK;
                case 3, 4 -> Material.ENCHANTED_BOOK;
                case 5, 6 -> Material.WRITABLE_BOOK;
                default -> Material.KNOWLEDGE_BOOK;
            };
        }

        // 根据等级显示不同颜色
        String color = switch (level) {
            case 1, 2 -> "§f";
            case 3, 4 -> "§a";
            case 5, 6 -> "§b";
            case 7, 8 -> "§d";
            default -> "§6";
        };

        List<String> lore = new ArrayList<>();
        lore.add("§e等级: " + color + level + "/10");
        lore.add("§e类型: §f" + getSkillType(skillName));
        lore.add("");
        lore.add("§7" + getSkillDescription(skillName));
        lore.add("");

        // 显示冷却状态
        if (isOnCooldown) {
            // 使用格式化方法
            String formattedTime = formatCooldownTime(remainingCooldown);
            lore.add("§c§l冷却中: §f" + formattedTime);
        } else {
            lore.add("§e冷却时间: §f" + getSkillCooldown(skillName) + "秒");
        }

        lore.add("§e消耗灵气: §f" + getSkillCost(skillName, level));
        lore.add("");

        if (level < 10) {
            lore.add("§a左键 §7- 查看详情");
            lore.add("§a右键 §7- 升级功法");
        } else {
            lore.add("§6已满级!");
            lore.add("§a左键 §7- 查看详情");
        }

        // 冷却状态提示
        if (isOnCooldown) {
            lore.add("");
            lore.add("§c§l✗ 冷却中，无法使用");
        } else {
            lore.add("");
            lore.add("§a§l✓ 准备就绪");
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name(color + "§l" + skillName)
                .lore(lore);

        // 冷却中不发光，准备好时发光
        if (level >= 5 && !isOnCooldown) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * 获取功法类型
     */
    private String getSkillType(String skillName) {
        // 从功法系统获取实际类型
        com.xiancore.systems.skill.Skill skill = plugin.getSkillSystem().getSkill(skillName);
        if (skill != null) {
            return skill.getType().name();
        }

        // 备用方案：根据名称推测
        if (skillName.contains("剑")) return "剑诀";
        if (skillName.contains("掌")) return "掌法";
        if (skillName.contains("火")) return "火系法术";
        if (skillName.contains("水")) return "水系法术";
        if (skillName.contains("御")) return "防御术";
        return "通用功法";
    }

    /**
     * 获取功法描述
     */
    private String getSkillDescription(String skillName) {
        // 从功法系统获取实际描述
        com.xiancore.systems.skill.Skill skill = plugin.getSkillSystem().getSkill(skillName);
        if (skill != null && skill.getDescription() != null && !skill.getDescription().isEmpty()) {
            return skill.getDescription();
        }
        return "凝聚灵气施展强大的攻击";
    }

    /**
     * 获取功法冷却时间
     */
    private int getSkillCooldown(String skillName) {
        // 从功法系统获取实际冷却时间
        com.xiancore.systems.skill.Skill skill = plugin.getSkillSystem().getSkill(skillName);
        if (skill != null) {
            return skill.getBaseCooldown();
        }
        return 10;
    }

    /**
     * 获取功法消耗
     */
    private int getSkillCost(String skillName, int level) {
        // 从功法系统获取实际消耗
        com.xiancore.systems.skill.Skill skill = plugin.getSkillSystem().getSkill(skillName);
        if (skill != null) {
            return skill.calculateQiCost(level);
        }
        return 50 + level * 10;
    }

    /**
     * 格式化冷却时间
     */
    private String formatCooldownTime(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return String.format("%d分%d秒", minutes, secs);
        }
        return seconds + "秒";
    }

    /**
     * 显示功能按钮
     */
    private void displayFunctionButtons(Player player, PlayerData data, StaticPane pane) {
        // ========== 第一行按钮（第4行）==========
        
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

        // 快捷键绑定按钮 ⭐ 新增
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
                .glow()  // 重要功能，加发光效果
                .build();

        pane.addItem(new GuiItem(bindButton, event -> {
            player.closeInventory();
            // 延迟1tick打开，确保当前GUI完全关闭
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
            // 打开功法商店
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

        // 遗忘功法按钮（引导到详情页面）
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
    public static void openSkillDetail(Player player, XianCore plugin, String skillName, int level) {
        ChestGui gui = new ChestGui(4, "§9§l" + skillName + " - 详情");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 创建边框
        GUIUtils.addBackground(gui, 4, Material.CYAN_STAINED_GLASS_PANE);

        StaticPane contentPane = new StaticPane(0, 0, 9, 4);

        // 功法详细信息
        ItemStack detailItem = new ItemBuilder(Material.ENCHANTED_BOOK)
                .name("§9§l" + skillName)
                .lore(
                        "§e等级: §f" + level + "/10",
                        "§e类型: §f攻击类",
                        "",
                        "§e效果:",
                        "§7- 造成 §c" + (100 + level * 50) + " §7点伤害",
                        "§7- 额外灼烧效果 §c" + (level * 2) + "秒",
                        "§7- 攻击范围: §f" + (3 + level) + "格",
                        "",
                        "§e消耗: §f" + (50 + level * 10) + " 灵气",
                        "§e冷却: §f10秒",
                        "",
                        "§6升级到 " + (level + 1) + " 级需要:",
                        "§7- 灵石: §f" + (1000 * level),
                        "§7- 功法点: §f" + (level * 5)
                )
                .glow()
                .build();
        contentPane.addItem(new GuiItem(detailItem), 4, 1);

        // 升级按钮
        if (level < 10) {
            ItemStack upgradeButton = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                    .name("§a§l升级功法")
                    .lore(
                            "§7提升功法等级",
                            "",
                            "§a点击升级"
                    )
                    .glow()
                    .build();
            contentPane.addItem(new GuiItem(upgradeButton, event -> {
                player.sendMessage("§e尝试升级功法 " + skillName + "...");
                plugin.getSkillSystem().upgradeSkill(player, skillName);
                // 升级后更新详情界面
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    openSkillDetail(player, plugin, skillName, level);
                }, 1L);
            }), 2, 2);
        }

        // 返回按钮
        ItemStack backButton = new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .build();
        contentPane.addItem(new GuiItem(backButton, event -> {
            open(player, plugin);
        }), 4, 2);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
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
            // 显示遗忘确认信息
            player.sendMessage("§c§l========== 遗忘功法确认 ==========");
            player.sendMessage("§e功法: §f" + skillName + " §7等级 " + level);
            player.sendMessage("");
            player.sendMessage("§c警告: 遗忘后该功法将被移除");
            player.sendMessage("§7• 将返还部分功法点");
            player.sendMessage("§7• 可能返还部分灵石（根据配置）");
            player.sendMessage("§7• 自动解绑所有快捷键");
            player.sendMessage("§7• 清除当前冷却");
            player.sendMessage("§7• 一段时间内无法重新学习");
            player.sendMessage("");
            player.sendMessage("§e如果确定要遗忘，请输入:");
            player.sendMessage("§f/skill forget " + skillName + " confirm");
            player.sendMessage("§c§l================================");
        }), 4, 3);

        gui.addPane(contentPane);
        gui.show(player);
    }
}
