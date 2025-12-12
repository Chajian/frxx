package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillBindDisplayService;
import com.xiancore.systems.skill.SkillBindDisplayService.SkillDisplayInfo;
import com.xiancore.systems.skill.SkillBindDisplayService.SlotDisplayInfo;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 功法快捷键绑定GUI
 * 提供可视化的功法绑定界面
 * 业务逻辑委托给 SkillBindDisplayService
 *
 * @author Olivia Diaz
 * @version 2.0.0 - 使用 Service 层分离业务逻辑
 */
public class SkillBindGUI {

    private final XianCore plugin;
    private final Player player;
    private final SkillBindDisplayService displayService;
    private ChestGui gui;

    // 选择模式：玩家点击功法后进入选择模式，等待选择槽位
    private String selectedSkillId = null;

    public SkillBindGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.displayService = new SkillBindDisplayService(plugin);
    }

    /**
     * 打开绑定GUI
     */
    public static void open(Player player, XianCore plugin) {
        new SkillBindGUI(plugin, player).show();
    }

    /**
     * 显示GUI
     */
    private void show() {
        gui = new ChestGui(6, "§9§l功法快捷键绑定");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        createBackground();
        renderGUI();

        gui.show(player);
    }

    /**
     * 创建背景
     */
    private void createBackground() {
        GUIUtils.addGrayBackground(gui, 6);
    }

    /**
     * 渲染GUI内容
     */
    private void renderGUI() {
        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        displayInstructions(contentPane);
        displaySlots(contentPane);
        displaySkillList(contentPane);
        displayActionButtons(contentPane);

        gui.addPane(contentPane);
    }

    /**
     * 显示说明
     */
    private void displayInstructions(StaticPane pane) {
        List<String> lore = new ArrayList<>();

        if (selectedSkillId != null) {
            Skill skill = displayService.getSkill(selectedSkillId);
            String skillName = skill != null ? skill.getName() : selectedSkillId;
            lore.add("§e§l已选择功法: §f" + skillName);
            lore.add("");
            lore.add("§a点击下方槽位进行绑定");
            lore.add("§7或点击其他功法切换选择");
        } else {
            lore.add("§7点击功法 → 点击槽位绑定");
            lore.add("§7右键槽位解除绑定");
            lore.add("");
            lore.add("§e切换快捷栏到对应槽位");
            lore.add("§e然后按 §fF键 §e即可施放功法");
        }

        ItemStack infoItem = new ItemBuilder(Material.BOOK)
                .name("§e§l快捷键绑定说明")
                .lore(lore)
                .build();

        pane.addItem(new GuiItem(infoItem), 4, 0);
    }

    /**
     * 显示9个槽位
     */
    private void displaySlots(StaticPane pane) {
        Map<Integer, String> bindings = displayService.getAllBindings(player);

        for (int slot = 1; slot <= 9; slot++) {
            SlotDisplayInfo info = displayService.getSlotDisplayInfo(player, slot, bindings.get(slot));
            ItemStack slotItem = createSlotItem(info);
            final int finalSlot = slot;

            pane.addItem(new GuiItem(slotItem, event -> {
                if (event.isLeftClick()) {
                    handleSlotClick(finalSlot);
                } else if (event.isRightClick()) {
                    handleSlotUnbind(finalSlot);
                }
            }), slot - 1, 1);
        }
    }

    /**
     * 创建槽位图标
     */
    private ItemStack createSlotItem(SlotDisplayInfo info) {
        List<String> lore = new ArrayList<>();
        String name;

        if (info.isBound()) {
            name = "§a§l槽位 " + info.getSlot() + ": " + info.getSkillName();

            lore.add("§e当前绑定: §f" + info.getSkillName());
            if (info.getTypeName() != null) {
                lore.add("§7类型: §f" + info.getTypeName());
            }
            if (info.getElementName() != null) {
                lore.add("§7属性: " + info.getElementName());
            }
            lore.add("");

            if (info.getCooldown() > 0) {
                lore.add("§c冷却中: " + info.getCooldown() + "秒");
            } else {
                lore.add("§a✓ 准备就绪");
            }

            lore.add("");
            lore.add("§e左键 §7- 重新绑定");
            lore.add("§c右键 §7- 解除绑定");
        } else {
            name = "§7槽位 " + info.getSlot() + ": §8(未绑定)";

            lore.add("§7此槽位尚未绑定功法");
            lore.add("");

            if (selectedSkillId != null) {
                lore.add("§a点击绑定选中的功法");
            } else {
                lore.add("§7先选择下方的功法");
                lore.add("§7然后点击此槽位绑定");
            }
        }

        return new ItemBuilder(info.getMaterial())
                .name(name)
                .lore(lore)
                .build();
    }

    /**
     * 显示已学功法列表
     */
    private void displaySkillList(StaticPane pane) {
        Map<String, Integer> learnedSkills = displayService.getLearnedSkills(player);

        if (learnedSkills.isEmpty()) {
            ItemStack noSkillItem = new ItemBuilder(Material.BARRIER)
                    .name("§c未学习任何功法")
                    .lore(
                            "§7你还没有学习任何功法",
                            "",
                            "§e使用 §f/skill §e打开功法界面",
                            "§7或通过功法商店购买"
                    )
                    .build();
            pane.addItem(new GuiItem(noSkillItem), 4, 3);
            return;
        }

        int index = 0;
        for (Map.Entry<String, Integer> entry : learnedSkills.entrySet()) {
            if (index >= 21) break;

            String skillId = entry.getKey();
            int level = entry.getValue();

            int row = 2 + (index / 7);
            int col = 1 + (index % 7);

            boolean isSelected = selectedSkillId != null && selectedSkillId.equals(skillId);
            SkillDisplayInfo info = displayService.getSkillDisplayInfo(player, skillId, level, isSelected);
            ItemStack skillItem = createSkillItem(info);
            pane.addItem(new GuiItem(skillItem, event -> handleSkillClick(skillId)), col, row);

            index++;
        }
    }

    /**
     * 创建功法图标
     */
    private ItemStack createSkillItem(SkillDisplayInfo info) {
        List<String> lore = new ArrayList<>();
        lore.add("§e等级: §f" + info.getLevel() + "/" + info.getMaxLevel());

        if (info.getTypeName() != null) {
            lore.add("§e类型: §f" + info.getTypeName());
        }
        if (info.getElementName() != null) {
            lore.add("§e属性: " + info.getElementName());
        }

        lore.add("");
        if (info.getDescription() != null) {
            lore.add("§7" + info.getDescription());
            lore.add("");
        }

        if (info.hasBoundSlots()) {
            lore.add("§a已绑定到槽位: §f" + info.getBoundSlotsDisplay());
        } else {
            lore.add("§7尚未绑定到任何槽位");
        }

        lore.add("");

        if (info.isSelected()) {
            lore.add("§a§l✓ 已选中");
            lore.add("§7点击槽位进行绑定");
        } else {
            lore.add("§e点击选择此功法");
        }

        ItemBuilder builder = new ItemBuilder(info.getMaterial())
                .name("§b§l" + info.getName())
                .lore(lore);

        if (info.isSelected()) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * 显示功能按钮
     */
    private void displayActionButtons(StaticPane pane) {
        if (selectedSkillId != null) {
            ItemStack clearItem = new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                    .name("§e取消选择")
                    .lore("§7点击取消当前选择的功法")
                    .build();
            pane.addItem(new GuiItem(clearItem, event -> {
                selectedSkillId = null;
                refreshGUI();
                player.sendMessage("§7已取消选择");
            }), 2, 5);
        }

        ItemStack refreshItem = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name("§a刷新界面")
                .lore("§7点击刷新界面显示")
                .build();
        pane.addItem(new GuiItem(refreshItem, event -> {
            refreshGUI();
            player.sendMessage("§a界面已刷新");
        }), 4, 5);

        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
        pane.addItem(new GuiItem(closeItem, event -> {
            player.closeInventory();
        }), 6, 5);
    }

    /**
     * 处理槽位点击（左键）
     */
    private void handleSlotClick(int slot) {
        if (selectedSkillId == null) {
            player.sendMessage("§c请先选择一个功法!");
            return;
        }

        boolean success = displayService.bindSkill(player, slot, selectedSkillId);

        if (success) {
            selectedSkillId = null;
            refreshGUI();
        }
    }

    /**
     * 处理槽位解绑（右键）
     */
    private void handleSlotUnbind(int slot) {
        displayService.unbindSkill(player, slot);
        refreshGUI();
    }

    /**
     * 处理功法点击
     */
    private void handleSkillClick(String skillId) {
        if (selectedSkillId != null && selectedSkillId.equals(skillId)) {
            selectedSkillId = null;
            player.sendMessage("§7已取消选择");
        } else {
            selectedSkillId = skillId;
            Skill skill = displayService.getSkill(skillId);
            String skillName = skill != null ? skill.getName() : skillId;
            player.sendMessage("§a已选择功法: §e" + skillName);
            player.sendMessage("§7点击上方槽位进行绑定");
        }
        refreshGUI();
    }

    /**
     * 刷新GUI
     */
    private void refreshGUI() {
        gui.getPanes().clear();
        createBackground();
        renderGUI();
        gui.update();
    }
}
