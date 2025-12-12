package com.xiancore.systems.skill;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 功法显示服务
 * 负责功法显示相关的业务逻辑，与 GUI 分离
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillDisplayService {

    private final XianCore plugin;

    // 等级对应的材料
    private static final Map<Integer, Material> LEVEL_MATERIALS = Map.of(
            1, Material.BOOK,
            2, Material.BOOK,
            3, Material.ENCHANTED_BOOK,
            4, Material.ENCHANTED_BOOK,
            5, Material.WRITABLE_BOOK,
            6, Material.WRITABLE_BOOK
    );

    // 等级对应的颜色代码
    private static final Map<Integer, String> LEVEL_COLORS = Map.of(
            1, "§f",
            2, "§f",
            3, "§a",
            4, "§a",
            5, "§b",
            6, "§b",
            7, "§d",
            8, "§d"
    );

    // 功法槽位使用率阈值
    private static final double SLOT_WARNING_THRESHOLD = 0.8;

    public SkillDisplayService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取功法对应的材料
     */
    public Material getSkillMaterial(int level, boolean isOnCooldown) {
        if (isOnCooldown) {
            return Material.GRAY_STAINED_GLASS_PANE;
        }
        return LEVEL_MATERIALS.getOrDefault(level, Material.KNOWLEDGE_BOOK);
    }

    /**
     * 获取功法等级对应的颜色
     */
    public String getLevelColor(int level) {
        if (level >= 9) return "§6";
        return LEVEL_COLORS.getOrDefault(level, "§f");
    }

    /**
     * 获取功法类型显示名
     */
    public String getSkillType(String skillId) {
        Skill skill = plugin.getSkillSystem().getSkill(skillId);
        if (skill != null) {
            return skill.getType().getDisplayName();
        }

        // 备用方案：根据名称推测
        if (skillId.contains("剑")) return "剑诀";
        if (skillId.contains("掌")) return "掌法";
        if (skillId.contains("火")) return "火系法术";
        if (skillId.contains("水")) return "水系法术";
        if (skillId.contains("御")) return "防御术";
        return "通用功法";
    }

    /**
     * 获取功法描述
     */
    public String getSkillDescription(String skillId) {
        Skill skill = plugin.getSkillSystem().getSkill(skillId);
        if (skill != null && skill.getDescription() != null && !skill.getDescription().isEmpty()) {
            return skill.getDescription();
        }
        return "凝聚灵气施展强大的攻击";
    }

    /**
     * 获取功法冷却时间
     */
    public int getSkillCooldown(String skillId) {
        Skill skill = plugin.getSkillSystem().getSkill(skillId);
        if (skill != null) {
            return skill.getBaseCooldown();
        }
        return 10;
    }

    /**
     * 获取功法灵气消耗
     */
    public int getSkillQiCost(String skillId, int level) {
        Skill skill = plugin.getSkillSystem().getSkill(skillId);
        if (skill != null) {
            return skill.calculateQiCost(level);
        }
        return 50 + level * 10;
    }

    /**
     * 获取剩余冷却时间
     */
    public int getRemainingCooldown(Player player, String skillId) {
        return plugin.getSkillSystem().getCooldownManager().getRemainingCooldown(player, skillId);
    }

    /**
     * 检查功法是否在冷却中
     */
    public boolean isOnCooldown(Player player, String skillId) {
        return getRemainingCooldown(player, skillId) > 0;
    }

    /**
     * 格式化冷却时间
     */
    public String formatCooldownTime(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return String.format("%d分%d秒", minutes, secs);
        }
        return seconds + "秒";
    }

    /**
     * 获取功法槽位信息
     */
    public SlotInfo getSlotInfo(Player player, PlayerData data) {
        Map<String, Integer> skills = data.getSkills();
        int skillCount = skills != null ? skills.size() : 0;
        int maxSlots = plugin.getSkillSystem().getMaxSkillSlots(player, data);

        SlotStatus status;
        if (skillCount >= maxSlots) {
            status = SlotStatus.FULL;
        } else if (skillCount >= maxSlots * SLOT_WARNING_THRESHOLD) {
            status = SlotStatus.NEAR_FULL;
        } else {
            status = SlotStatus.AVAILABLE;
        }

        return new SlotInfo(skillCount, maxSlots, status);
    }

    /**
     * 获取功法详情信息
     */
    public SkillDetailInfo getSkillDetailInfo(String skillId, int level) {
        Skill skill = plugin.getSkillSystem().getSkill(skillId);

        String name = skill != null ? skill.getName() : skillId;
        String type = getSkillType(skillId);
        String description = getSkillDescription(skillId);
        int cooldown = getSkillCooldown(skillId);
        int qiCost = getSkillQiCost(skillId, level);
        int maxLevel = skill != null ? skill.getMaxLevel() : 10;

        // 计算升级消耗
        int upgradeSpiritStones = 1000 * level;
        int upgradeSkillPoints = level * 5;

        // 计算效果数值（示例）
        int damage = 100 + level * 50;
        int burnDuration = level * 2;
        int range = 3 + level;

        return new SkillDetailInfo(
                name, type, description, level, maxLevel,
                cooldown, qiCost, damage, burnDuration, range,
                upgradeSpiritStones, upgradeSkillPoints
        );
    }

    /**
     * 获取Skill对象
     */
    public Skill getSkill(String skillId) {
        return plugin.getSkillSystem().getSkill(skillId);
    }

    /**
     * 槽位状态枚举
     */
    public enum SlotStatus {
        AVAILABLE("§f", ""),
        NEAR_FULL("§e", " §e(接近上限)"),
        FULL("§c", " §c(已满)");

        private final String color;
        private final String suffix;

        SlotStatus(String color, String suffix) {
            this.color = color;
            this.suffix = suffix;
        }

        public String getColor() { return color; }
        public String getSuffix() { return suffix; }
    }

    /**
     * 槽位信息
     */
    public static class SlotInfo {
        private final int currentCount;
        private final int maxSlots;
        private final SlotStatus status;

        public SlotInfo(int currentCount, int maxSlots, SlotStatus status) {
            this.currentCount = currentCount;
            this.maxSlots = maxSlots;
            this.status = status;
        }

        public int getCurrentCount() { return currentCount; }
        public int getMaxSlots() { return maxSlots; }
        public SlotStatus getStatus() { return status; }

        public String getDisplayText() {
            return "§e功法槽位: " + status.getColor() + currentCount + "/" + maxSlots + status.getSuffix();
        }
    }

    /**
     * 功法详情信息
     */
    public static class SkillDetailInfo {
        private final String name;
        private final String type;
        private final String description;
        private final int level;
        private final int maxLevel;
        private final int cooldown;
        private final int qiCost;
        private final int damage;
        private final int burnDuration;
        private final int range;
        private final int upgradeSpiritStones;
        private final int upgradeSkillPoints;

        public SkillDetailInfo(String name, String type, String description,
                               int level, int maxLevel, int cooldown, int qiCost,
                               int damage, int burnDuration, int range,
                               int upgradeSpiritStones, int upgradeSkillPoints) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.level = level;
            this.maxLevel = maxLevel;
            this.cooldown = cooldown;
            this.qiCost = qiCost;
            this.damage = damage;
            this.burnDuration = burnDuration;
            this.range = range;
            this.upgradeSpiritStones = upgradeSpiritStones;
            this.upgradeSkillPoints = upgradeSkillPoints;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public String getDescription() { return description; }
        public int getLevel() { return level; }
        public int getMaxLevel() { return maxLevel; }
        public int getCooldown() { return cooldown; }
        public int getQiCost() { return qiCost; }
        public int getDamage() { return damage; }
        public int getBurnDuration() { return burnDuration; }
        public int getRange() { return range; }
        public int getUpgradeSpiritStones() { return upgradeSpiritStones; }
        public int getUpgradeSkillPoints() { return upgradeSkillPoints; }
        public boolean isMaxLevel() { return level >= maxLevel; }
    }
}
