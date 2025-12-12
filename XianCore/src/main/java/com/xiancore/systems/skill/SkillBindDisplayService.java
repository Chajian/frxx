package com.xiancore.systems.skill;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 功法绑定显示服务
 * 负责功法绑定GUI的业务逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillBindDisplayService {

    private final XianCore plugin;

    public SkillBindDisplayService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取玩家所有绑定
     */
    public Map<Integer, String> getAllBindings(Player player) {
        return plugin.getSkillSystem().getBindManager().getAllBindings(player);
    }

    /**
     * 获取玩家学习的技能
     */
    public Map<String, Integer> getLearnedSkills(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        return data != null ? data.getSkills() : Map.of();
    }

    /**
     * 获取技能冷却时间
     */
    public int getRemainingCooldown(Player player, String skillId) {
        return plugin.getSkillSystem().getCooldownManager().getRemainingCooldown(player, skillId);
    }

    /**
     * 绑定技能
     */
    public boolean bindSkill(Player player, int slot, String skillId) {
        return plugin.getSkillSystem().getBindManager().bindSkill(player, slot, skillId);
    }

    /**
     * 解绑技能
     */
    public void unbindSkill(Player player, int slot) {
        plugin.getSkillSystem().getBindManager().unbindSkill(player, slot);
    }

    /**
     * 获取技能信息
     */
    public Skill getSkill(String skillId) {
        return plugin.getSkillSystem().getSkill(skillId);
    }

    /**
     * 获取技能绑定的槽位列表
     */
    public List<Integer> getSkillBoundSlots(Player player, String skillId) {
        Map<Integer, String> bindings = getAllBindings(player);
        List<Integer> boundSlots = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : bindings.entrySet()) {
            if (skillId.equals(entry.getValue())) {
                boundSlots.add(entry.getKey());
            }
        }
        return boundSlots;
    }

    /**
     * 创建槽位显示信息
     */
    public SlotDisplayInfo getSlotDisplayInfo(Player player, int slot, String boundSkillId) {
        if (boundSkillId != null) {
            Skill skill = getSkill(boundSkillId);
            String skillName = skill != null ? skill.getName() : boundSkillId;
            int cooldown = getRemainingCooldown(player, boundSkillId);

            String typeName = skill != null ? skill.getType().getDisplayName() : null;
            String elementName = skill != null && skill.getElement() != null ?
                    skill.getElement().getColoredName() : null;

            return new SlotDisplayInfo(
                    slot,
                    true,
                    skillName,
                    typeName,
                    elementName,
                    cooldown,
                    cooldown <= 0
            );
        } else {
            return new SlotDisplayInfo(slot, false, null, null, null, 0, false);
        }
    }

    /**
     * 创建技能显示信息
     */
    public SkillDisplayInfo getSkillDisplayInfo(Player player, String skillId, int level, boolean isSelected) {
        Skill skill = getSkill(skillId);
        if (skill == null) {
            return new SkillDisplayInfo(skillId, skillId, level, 10, null, null, null, List.of(), isSelected);
        }

        String typeName = skill.getType().getDisplayName();
        String elementName = skill.getElement() != null ? skill.getElement().getColoredName() : null;
        String description = skill.getDescription();
        List<Integer> boundSlots = getSkillBoundSlots(player, skillId);

        return new SkillDisplayInfo(
                skillId,
                skill.getName(),
                level,
                skill.getMaxLevel(),
                typeName,
                elementName,
                description,
                boundSlots,
                isSelected
        );
    }

    /**
     * 槽位显示信息
     */
    public static class SlotDisplayInfo {
        private final int slot;
        private final boolean bound;
        private final String skillName;
        private final String typeName;
        private final String elementName;
        private final int cooldown;
        private final boolean ready;

        public SlotDisplayInfo(int slot, boolean bound, String skillName, String typeName,
                               String elementName, int cooldown, boolean ready) {
            this.slot = slot;
            this.bound = bound;
            this.skillName = skillName;
            this.typeName = typeName;
            this.elementName = elementName;
            this.cooldown = cooldown;
            this.ready = ready;
        }

        public int getSlot() { return slot; }
        public boolean isBound() { return bound; }
        public String getSkillName() { return skillName; }
        public String getTypeName() { return typeName; }
        public String getElementName() { return elementName; }
        public int getCooldown() { return cooldown; }
        public boolean isReady() { return ready; }

        public Material getMaterial() {
            return bound ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        }
    }

    /**
     * 技能显示信息
     */
    public static class SkillDisplayInfo {
        private final String skillId;
        private final String name;
        private final int level;
        private final int maxLevel;
        private final String typeName;
        private final String elementName;
        private final String description;
        private final List<Integer> boundSlots;
        private final boolean selected;

        public SkillDisplayInfo(String skillId, String name, int level, int maxLevel,
                                String typeName, String elementName, String description,
                                List<Integer> boundSlots, boolean selected) {
            this.skillId = skillId;
            this.name = name;
            this.level = level;
            this.maxLevel = maxLevel;
            this.typeName = typeName;
            this.elementName = elementName;
            this.description = description;
            this.boundSlots = boundSlots;
            this.selected = selected;
        }

        public String getSkillId() { return skillId; }
        public String getName() { return name; }
        public int getLevel() { return level; }
        public int getMaxLevel() { return maxLevel; }
        public String getTypeName() { return typeName; }
        public String getElementName() { return elementName; }
        public String getDescription() { return description; }
        public List<Integer> getBoundSlots() { return boundSlots; }
        public boolean isSelected() { return selected; }
        public boolean hasBoundSlots() { return !boundSlots.isEmpty(); }

        public Material getMaterial() {
            return selected ? Material.ENCHANTED_BOOK : Material.BOOK;
        }

        public String getBoundSlotsDisplay() {
            return boundSlots.toString().replaceAll("[\\[\\]]", "");
        }
    }
}
