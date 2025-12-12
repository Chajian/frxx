package com.xiancore.gui.boss;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Boss列表显示服务
 * 负责Boss列表GUI的数据处理和显示逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class BossListDisplayService {

    // 分页常量
    public static final int ITEMS_PER_PAGE = 21;

    // Tier材料映射
    private static final Material[] TIER_MATERIALS = {
            Material.SKULL_BANNER_PATTERN, // 默认
            Material.ZOMBIE_HEAD,           // Tier 1
            Material.WITHER_SKELETON_SKULL, // Tier 2
            Material.DRAGON_HEAD,           // Tier 3
            Material.NETHER_STAR            // Tier 4
    };

    // Tier颜色映射
    private static final String[] TIER_COLORS = {
            "§7", // 默认
            "§a", // Tier 1 绿色
            "§b", // Tier 2 青色
            "§e", // Tier 3 黄色
            "§c"  // Tier 4 红色
    };

    // Tier名称映射
    private static final String[] TIER_NAMES = {
            "未知",
            "普通",
            "精英",
            "世界Boss",
            "传奇"
    };

    // 状态颜色映射
    private static final String STATUS_ACTIVE = "§a";
    private static final String STATUS_DEAD = "§c";
    private static final String STATUS_DESPAWNED = "§7";
    private static final String STATUS_DEFAULT = "§8";

    /**
     * 根据Tier获取材料
     */
    public Material getMaterialForTier(int tier) {
        if (tier >= 1 && tier < TIER_MATERIALS.length) {
            return TIER_MATERIALS[tier];
        }
        return TIER_MATERIALS[0];
    }

    /**
     * 根据Tier获取颜色
     */
    public String getColorForTier(int tier) {
        if (tier >= 1 && tier < TIER_COLORS.length) {
            return TIER_COLORS[tier];
        }
        return TIER_COLORS[0];
    }

    /**
     * 获取Tier名称
     */
    public String getTierName(int tier) {
        if (tier >= 1 && tier < TIER_NAMES.length) {
            return TIER_NAMES[tier];
        }
        return TIER_NAMES[0];
    }

    /**
     * 获取状态颜色
     */
    public String getStatusColor(String status) {
        if (status == null) return STATUS_DEFAULT;
        return switch (status) {
            case "ACTIVE" -> STATUS_ACTIVE;
            case "DEAD" -> STATUS_DEAD;
            case "DESPAWNED" -> STATUS_DESPAWNED;
            default -> STATUS_DEFAULT;
        };
    }

    /**
     * 计算分页信息
     */
    public PageInfo calculatePageInfo(int totalItems, int requestedPage) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        return new PageInfo(page, totalPages, startIndex, endIndex);
    }

    /**
     * 搜索Boss
     */
    public List<BossListGUI.BossInfo> searchBosses(List<BossListGUI.BossInfo> bosses, String query) {
        if (query == null || query.isEmpty()) {
            return bosses;
        }

        String lowerQuery = query.toLowerCase();
        return bosses.stream()
                .filter(boss -> boss.type.toLowerCase().contains(lowerQuery) ||
                        boss.world.toLowerCase().contains(lowerQuery) ||
                        boss.status.toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    /**
     * 获取活跃Boss数量
     */
    public int getActiveBossCount(List<BossListGUI.BossInfo> bosses) {
        return (int) bosses.stream()
                .filter(boss -> "ACTIVE".equals(boss.status))
                .count();
    }

    /**
     * 获取特定Tier的Boss列表
     */
    public List<BossListGUI.BossInfo> getBossesByTier(List<BossListGUI.BossInfo> bosses, int tier) {
        return bosses.stream()
                .filter(boss -> boss.tier == tier)
                .collect(Collectors.toList());
    }

    /**
     * 创建Boss详情文本
     */
    public List<String> createBossDetailLines(BossListGUI.BossInfo boss) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l═══════════════════════════════");
        lines.add("§6§l  Boss详情");
        lines.add("§6§l═══════════════════════════════");
        lines.add("§e名称: §a" + boss.type);
        lines.add("§e世界: §a" + boss.world);
        lines.add("§e位置: §a" + boss.x + ", " + boss.y + ", " + boss.z);
        lines.add("§e等级: " + getColorForTier(boss.tier) + getTierName(boss.tier));
        lines.add("§e血量: §c" + String.format("%.1f", boss.health) + " / 100.0");
        lines.add("§e状态: " + getStatusColor(boss.status) + boss.status);
        lines.add("§6§l═══════════════════════════════");
        return lines;
    }

    /**
     * 分页信息
     */
    public static class PageInfo {
        private final int currentPage;
        private final int totalPages;
        private final int startIndex;
        private final int endIndex;

        public PageInfo(int currentPage, int totalPages, int startIndex, int endIndex) {
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public int getCurrentPage() { return currentPage; }
        public int getTotalPages() { return totalPages; }
        public int getStartIndex() { return startIndex; }
        public int getEndIndex() { return endIndex; }
        public boolean hasPrevious() { return currentPage > 1; }
        public boolean hasNext() { return currentPage < totalPages; }
    }
}
