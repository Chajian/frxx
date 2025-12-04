package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.skill.Skill;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 功法快捷键绑定GUI
 * 提供可视化的功法绑定界面
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillBindGUI {

    private final XianCore plugin;
    private final Player player;
    private ChestGui gui;
    
    // 选择模式：玩家点击功法后进入选择模式，等待选择槽位
    private String selectedSkillId = null;

    public SkillBindGUI(XianCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
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
        // 创建6行的GUI
        gui = new ChestGui(6, "§9§l功法快捷键绑定");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 创建背景
        createBackground();

        // 显示内容
        renderGUI();

        gui.show(player);
    }

    /**
     * 创建背景
     */
    private void createBackground() {
        OutlinePane background = new OutlinePane(0, 0, 9, 6);
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(OutlinePane.Priority.LOWEST);
        gui.addPane(background);
    }

    /**
     * 渲染GUI内容
     */
    private void renderGUI() {
        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 第1行：说明
        displayInstructions(contentPane);

        // 第2行：9个槽位按钮
        displaySlots(contentPane);

        // 第3-5行：已学功法列表
        displaySkillList(contentPane);

        // 第6行：功能按钮
        displayActionButtons(contentPane);

        gui.addPane(contentPane);
    }

    /**
     * 显示说明
     */
    private void displayInstructions(StaticPane pane) {
        List<String> lore = new ArrayList<>();
        
        if (selectedSkillId != null) {
            // 选择模式提示
            Skill skill = plugin.getSkillSystem().getSkill(selectedSkillId);
            String skillName = skill != null ? skill.getName() : selectedSkillId;
            lore.add("§e§l已选择功法: §f" + skillName);
            lore.add("");
            lore.add("§a点击下方槽位进行绑定");
            lore.add("§7或点击其他功法切换选择");
        } else {
            // 正常模式提示
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
        Map<Integer, String> bindings = plugin.getSkillSystem().getBindManager().getAllBindings(player);

        for (int slot = 1; slot <= 9; slot++) {
            ItemStack slotItem = createSlotItem(slot, bindings.get(slot));
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
    private ItemStack createSlotItem(int slot, String boundSkillId) {
        Material material;
        List<String> lore = new ArrayList<>();
        String name;

        if (boundSkillId != null) {
            // 已绑定
            Skill skill = plugin.getSkillSystem().getSkill(boundSkillId);
            String skillName = skill != null ? skill.getName() : boundSkillId;
            
            // 检查冷却
            int cooldown = plugin.getSkillSystem().getCooldownManager().getRemainingCooldown(player, boundSkillId);
            
            material = Material.LIME_STAINED_GLASS_PANE;
            name = "§a§l槽位 " + slot + ": " + skillName;
            
            lore.add("§e当前绑定: §f" + skillName);
            if (skill != null) {
                lore.add("§7类型: §f" + skill.getType().getDisplayName());
                if (skill.getElement() != null) {
                    lore.add("§7属性: " + skill.getElement().getColoredName());
                }
            }
            lore.add("");
            
            if (cooldown > 0) {
                lore.add("§c冷却中: " + cooldown + "秒");
            } else {
                lore.add("§a✓ 准备就绪");
            }
            
            lore.add("");
            lore.add("§e左键 §7- 重新绑定");
            lore.add("§c右键 §7- 解除绑定");
        } else {
            // 未绑定
            material = Material.RED_STAINED_GLASS_PANE;
            name = "§7槽位 " + slot + ": §8(未绑定)";
            
            lore.add("§7此槽位尚未绑定功法");
            lore.add("");
            
            if (selectedSkillId != null) {
                lore.add("§a点击绑定选中的功法");
            } else {
                lore.add("§7先选择下方的功法");
                lore.add("§7然后点击此槽位绑定");
            }
        }

        return new ItemBuilder(material)
                .name(name)
                .lore(lore)
                .build();
    }

    /**
     * 显示已学功法列表
     */
    private void displaySkillList(StaticPane pane) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }

        Map<String, Integer> learnedSkills = data.getSkills();
        
        if (learnedSkills.isEmpty()) {
            // 没有学习任何功法
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

        // 显示功法列表（最多21个，3行x7列）
        int index = 0;
        for (Map.Entry<String, Integer> entry : learnedSkills.entrySet()) {
            if (index >= 21) break; // 最多显示21个

            String skillId = entry.getKey();
            int level = entry.getValue();

            int row = 2 + (index / 7); // 第3-5行
            int col = 1 + (index % 7); // 列1-7

            ItemStack skillItem = createSkillItem(skillId, level);
            pane.addItem(new GuiItem(skillItem, event -> handleSkillClick(skillId)), col, row);

            index++;
        }
    }

    /**
     * 创建功法图标
     */
    private ItemStack createSkillItem(String skillId, int level) {
        Skill skill = plugin.getSkillSystem().getSkill(skillId);
        if (skill == null) {
            return new ItemBuilder(Material.PAPER).name("§c未知功法: " + skillId).build();
        }

        // 根据是否选中显示不同的材料
        Material material = selectedSkillId != null && selectedSkillId.equals(skillId) 
                ? Material.ENCHANTED_BOOK 
                : Material.BOOK;

        List<String> lore = new ArrayList<>();
        lore.add("§e等级: §f" + level + "/" + skill.getMaxLevel());
        lore.add("§e类型: §f" + skill.getType().getDisplayName());
        
        if (skill.getElement() != null) {
            lore.add("§e属性: " + skill.getElement().getColoredName());
        }
        
        lore.add("");
        lore.add("§7" + skill.getDescription());
        lore.add("");

        // 显示当前绑定状态
        Map<Integer, String> bindings = plugin.getSkillSystem().getBindManager().getAllBindings(player);
        List<Integer> boundSlots = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : bindings.entrySet()) {
            if (skillId.equals(entry.getValue())) {
                boundSlots.add(entry.getKey());
            }
        }

        if (!boundSlots.isEmpty()) {
            lore.add("§a已绑定到槽位: §f" + boundSlots.toString().replaceAll("[\\[\\]]", ""));
        } else {
            lore.add("§7尚未绑定到任何槽位");
        }

        lore.add("");
        
        if (selectedSkillId != null && selectedSkillId.equals(skillId)) {
            lore.add("§a§l✓ 已选中");
            lore.add("§7点击槽位进行绑定");
        } else {
            lore.add("§e点击选择此功法");
        }

        ItemBuilder builder = new ItemBuilder(material)
                .name("§b§l" + skill.getName())
                .lore(lore);

        // 选中的功法发光
        if (selectedSkillId != null && selectedSkillId.equals(skillId)) {
            builder.glow();
        }

        return builder.build();
    }

    /**
     * 显示功能按钮
     */
    private void displayActionButtons(StaticPane pane) {
        // 清除选择按钮
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

        // 刷新按钮
        ItemStack refreshItem = new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name("§a刷新界面")
                .lore("§7点击刷新界面显示")
                .build();
        pane.addItem(new GuiItem(refreshItem, event -> {
            refreshGUI();
            player.sendMessage("§a界面已刷新");
        }), 4, 5);

        // 关闭按钮
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

        // 绑定功法
        boolean success = plugin.getSkillSystem().getBindManager().bindSkill(player, slot, selectedSkillId);
        
        if (success) {
            // 成功后清除选择并刷新
            selectedSkillId = null;
            refreshGUI();
        }
    }

    /**
     * 处理槽位解绑（右键）
     */
    private void handleSlotUnbind(int slot) {
        plugin.getSkillSystem().getBindManager().unbindSkill(player, slot);
        refreshGUI();
    }

    /**
     * 处理功法点击
     */
    private void handleSkillClick(String skillId) {
        if (selectedSkillId != null && selectedSkillId.equals(skillId)) {
            // 再次点击同一功法，取消选择
            selectedSkillId = null;
            player.sendMessage("§7已取消选择");
        } else {
            // 选择功法
            selectedSkillId = skillId;
            Skill skill = plugin.getSkillSystem().getSkill(skillId);
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
        // 清空所有面板
        gui.getPanes().clear();
        
        // 重新创建
        createBackground();
        renderGUI();
        
        // 更新显示
        gui.update();
    }
}

