package com.xiancore.integration.residence;

import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectSystem;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 宗门领地排行榜系统
 * 统计和展示宗门领地相关的排行信息
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SectLandLeaderboard {

    private final SectSystem sectSystem;

    /**
     * 领地排行条目
     */
    @Getter
    public static class LeaderboardEntry {
        private final String sectName;
        private final int sectId;
        private final int value;
        private final String unit;
        private final long timestamp;

        public LeaderboardEntry(String sectName, int sectId, int value, String unit) {
            this.sectName = sectName;
            this.sectId = sectId;
            this.value = value;
            this.unit = unit;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "§f" + sectName + " §7- §6" + value + " " + unit;
        }
    }

    /**
     * 构造函数
     */
    public SectLandLeaderboard(SectSystem sectSystem) {
        this.sectSystem = sectSystem;
    }

    /**
     * 获取领地规模排行榜（前10名）
     */
    public List<LeaderboardEntry> getLandSizeLeaderboard() {
        return sectSystem.getSects().values().stream()
            .filter(Sect::hasLand)
            .map(sect -> new LeaderboardEntry(
                sect.getName(),
                sect.getId(),
                sect.getBuildingSlots().size(),
                "建筑位"
            ))
            .sorted((a, b) -> Integer.compare(b.value, a.value))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取资金排行榜（前10名）
     */
    public List<LeaderboardEntry> getFundsLeaderboard() {
        return sectSystem.getSects().values().stream()
            .map(sect -> new LeaderboardEntry(
                sect.getName(),
                sect.getId(),
                (int) sect.getSectFunds(),
                "灵石"
            ))
            .sorted((a, b) -> Integer.compare(b.value, a.value))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取成员排行榜（前10名）
     */
    public List<LeaderboardEntry> getMemberCountLeaderboard() {
        return sectSystem.getSects().values().stream()
            .map(sect -> new LeaderboardEntry(
                sect.getName(),
                sect.getId(),
                sect.getMemberCount(),
                "成员"
            ))
            .sorted((a, b) -> Integer.compare(b.value, a.value))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取维护评分排行榜（前10名）
     */
    public List<LeaderboardEntry> getMaintenanceScoreLeaderboard() {
        return sectSystem.getSects().values().stream()
            .filter(Sect::hasLand)
            .map(sect -> {
                // 计算维护评分
                long daysSinceMaintenance = (System.currentTimeMillis() - sect.getLastMaintenanceTime()) / (1000 * 60 * 60 * 24);
                int score = calculateMaintenanceScore(daysSinceMaintenance);
                return new LeaderboardEntry(
                    sect.getName(),
                    sect.getId(),
                    score,
                    "分"
                );
            })
            .sorted((a, b) -> Integer.compare(b.value, a.value))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取综合排行榜（前10名）
     * 基于建筑位数量、资金、成员数的综合评分
     */
    public List<LeaderboardEntry> getOverallLeaderboard() {
        return sectSystem.getSects().values().stream()
            .filter(Sect::hasLand)
            .map(sect -> {
                // 综合评分计算
                int buildingSlots = sect.getBuildingSlots().size();
                long funds = sect.getSectFunds();
                int members = sect.getMemberCount();

                // 建筑位评分：每个建筑位10分
                int slotScore = buildingSlots * 10;

                // 资金评分：每1000灵石1分
                int fundsScore = (int) (Math.min(funds, 100000) / 1000);

                // 成员评分：每个成员5分
                int memberScore = members * 5;

                // 维护评分
                long daysSinceMaintenance = (System.currentTimeMillis() - sect.getLastMaintenanceTime()) / (1000 * 60 * 60 * 24);
                int maintenanceScore = calculateMaintenanceScore(daysSinceMaintenance);

                int totalScore = slotScore + fundsScore + memberScore + maintenanceScore;

                return new LeaderboardEntry(
                    sect.getName(),
                    sect.getId(),
                    totalScore,
                    "分"
                );
            })
            .sorted((a, b) -> Integer.compare(b.value, a.value))
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 获取特定宗门的排名
     */
    public int getRankForSect(List<LeaderboardEntry> leaderboard, int sectId) {
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).sectId == sectId) {
                return i + 1;
            }
        }
        return -1; // 未在排行榜中
    }

    /**
     * 计算维护评分
     * 30天满分100，逾期扣分
     */
    private int calculateMaintenanceScore(long daysSinceMaintenance) {
        if (daysSinceMaintenance <= 0) {
            return 100;
        } else if (daysSinceMaintenance <= 30) {
            return 100 - (int) ((daysSinceMaintenance / 30.0) * 50);
        } else if (daysSinceMaintenance <= 60) {
            return 50 - (int) (((daysSinceMaintenance - 30) / 30.0) * 50);
        } else {
            return 0;
        }
    }

    /**
     * 格式化排行榜输出
     */
    public String formatLeaderboard(String title, List<LeaderboardEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("§a§l========== ").append(title).append(" ==========\"\n");

        if (entries.isEmpty()) {
            sb.append("§7暂无数据\n");
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                String rank = "§6#" + (i + 1) + "§7 ";
                if (i == 0) {
                    rank = "§e★ #1★§7 ";
                } else if (i == 1) {
                    rank = "§f✦ #2✦§7 ";
                } else if (i == 2) {
                    rank = "§d◆ #3◆§7 ";
                }
                sb.append(rank).append(entry).append("\n");
            }
        }

        sb.append("§a§l=============================\"");
        return sb.toString();
    }
}
