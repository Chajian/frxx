package com.xiancore.systems.sect.achievement;

import com.xiancore.systems.sect.Sect;
import lombok.Getter;
import java.util.*;

/**
 * 领地相关成就系统
 * 定义宗门领地系统的各项成就
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class LandAchievements {

    public enum Achievement {
        // 领地成就
        FIRST_LAND("首次圈地", "§a首次圈地成功", "第一次圈地领地", 100),
        LAND_MASTER("领地大师", "§b拥有超过50块领地", "领地规模达到50块", 500),
        LAND_EXPANSION("扩展能手", "§b成功扩展领地10次", "领地扩展10次", 300),

        // 维护成就
        MAINTENANCE_HERO("维护英雄", "§c连续30天保持维护费", "连续30天不逾期", 200),
        RICH_SECT("宗门富豪", "§6宗门资金超过100000灵石", "资金达100000灵石", 400),

        // 建筑成就
        BUILDER("建筑师", "§e在领地中建造10个建筑位", "建造10个建筑位", 250),
        BUILDING_MASTER("建筑大师", "§e在领地中建造50个建筑位", "建造50个建筑位", 600),

        // 权限成就
        PERMISSIONS_MANAGER("权限管理员", "§9为所有成员设置权限", "权限配置完整", 150),
        MEMBERS_ORGANIZER("成员组织者", "§9管理超过20个成员", "成员超过20", 350);

        private final String name;
        private final String displayName;
        private final String description;
        private final int rewardPoints;

        Achievement(String name, String displayName, String description, int rewardPoints) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.rewardPoints = rewardPoints;
        }
    }

    /**
     * 检查宗门是否已解锁成就
     */
    public static boolean isAchievementUnlocked(Sect sect, Achievement achievement) {
        return switch (achievement) {
            case FIRST_LAND -> sect.hasLand();
            case LAND_MASTER -> false; // 需要外部追踪
            case LAND_EXPANSION -> false; // 需要外部追踪
            case MAINTENANCE_HERO -> isMaintenanceHero(sect);
            case RICH_SECT -> sect.getSectFunds() >= 100000;
            case BUILDER -> sect.getBuildingSlots().size() >= 10;
            case BUILDING_MASTER -> sect.getBuildingSlots().size() >= 50;
            case PERMISSIONS_MANAGER -> sect.getMemberList().size() > 0;
            case MEMBERS_ORGANIZER -> sect.getMemberCount() >= 20;
        };
    }

    /**
     * 检查是否为维护英雄
     */
    private static boolean isMaintenanceHero(Sect sect) {
        if (!sect.hasLand()) {
            return false;
        }
        long daysSinceMaintenance = (System.currentTimeMillis() - sect.getLastMaintenanceTime()) / (1000 * 60 * 60 * 24);
        return daysSinceMaintenance <= 30;
    }

    /**
     * 获取成就列表
     */
    public static List<String> getUnlockedAchievements(Sect sect) {
        List<String> unlocked = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            if (isAchievementUnlocked(sect, achievement)) {
                unlocked.add(achievement.displayName);
            }
        }
        return unlocked;
    }

    /**
     * 获取成就进度
     */
    public static String getAchievementProgress(Sect sect, Achievement achievement) {
        return switch (achievement) {
            case LAND_MASTER -> "§f" + sect.getBuildingSlots().size() + "§7/50";
            case LAND_EXPANSION -> "§f0§7/10";
            case MAINTENANCE_HERO -> formatMaintenanceProgress(sect);
            case RICH_SECT -> "§f" + sect.getSectFunds() + "§7/100000";
            case BUILDER -> "§f" + sect.getBuildingSlots().size() + "§7/10";
            case BUILDING_MASTER -> "§f" + sect.getBuildingSlots().size() + "§7/50";
            case MEMBERS_ORGANIZER -> "§f" + sect.getMemberCount() + "§7/20";
            default -> "已解锁";
        };
    }

    /**
     * 格式化维护进度
     */
    private static String formatMaintenanceProgress(Sect sect) {
        if (!sect.hasLand()) {
            return "§c无领地";
        }
        long daysSinceMaintenance = (System.currentTimeMillis() - sect.getLastMaintenanceTime()) / (1000 * 60 * 60 * 24);
        return "§f" + daysSinceMaintenance + "§7/30 天";
    }
}
