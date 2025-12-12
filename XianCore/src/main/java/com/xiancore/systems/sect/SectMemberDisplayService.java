package com.xiancore.systems.sect;

import com.xiancore.XianCore;
import com.xiancore.integration.residence.ResidencePermissionManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 宗门成员显示服务
 * 负责宗门成员GUI的业务逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectMemberDisplayService {

    private final XianCore plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public SectMemberDisplayService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取排序后的成员列表
     */
    public List<SectMember> getSortedMembers(Sect sect) {
        List<SectMember> members = new ArrayList<>(sect.getMemberList());
        members.sort(Comparator.comparing((SectMember m) -> m.getRank().getLevel()).reversed());
        return members;
    }

    /**
     * 获取成员统计信息
     */
    public MemberStatsInfo getMemberStats(Sect sect) {
        return new MemberStatsInfo(
                sect.getMemberCount(),
                sect.getMaxMembers(),
                sect.getMembersByRank(SectRank.LEADER).size(),
                sect.getMembersByRank(SectRank.ELDER).size(),
                sect.getMembersByRank(SectRank.CORE_DISCIPLE).size(),
                sect.getMembersByRank(SectRank.INNER_DISCIPLE).size(),
                sect.getMembersByRank(SectRank.OUTER_DISCIPLE).size()
        );
    }

    /**
     * 获取权限统计信息
     */
    public PermissionStatsInfo getPermissionStats(Sect sect) {
        if (!sect.hasLand()) {
            return null;
        }

        String landId = sect.getResidenceLandId();
        String[] statsLines = plugin.getSectSystem().getPermissionManager()
                .getPermissionStatistics(sect.getId()).split("\n");

        return new PermissionStatsInfo(landId, Arrays.asList(statsLines));
    }

    /**
     * 获取成员显示信息
     */
    public MemberDisplayInfo getMemberDisplayInfo(Player viewer, Sect sect, SectMember member) {
        SectRank rank = member.getRank();
        long daysSinceActive = (System.currentTimeMillis() - member.getLastActiveAt()) / (1000 * 60 * 60 * 24);

        String activityStatus;
        if (daysSinceActive > 30) {
            activityStatus = "§c长期未活跃 (" + daysSinceActive + " 天)";
        } else if (daysSinceActive > 7) {
            activityStatus = "§e最近活跃: " + daysSinceActive + " 天前";
        } else {
            activityStatus = "§a活跃成员";
        }

        String permissionInfo = null;
        String permissionDesc = null;
        if (sect.hasLand()) {
            ResidencePermissionManager.PermissionLevel permLevel =
                    plugin.getSectSystem().getPermissionManager().getPermissionLevel(rank);
            permissionInfo = permLevel.getDisplayName();
            permissionDesc = permLevel.getDescription();
        }

        SectMember viewerMember = sect.getMember(viewer.getUniqueId());
        boolean canManage = viewerMember != null && viewerMember.getRank().hasManagePermission();
        boolean isSelf = member.getPlayerId().equals(viewer.getUniqueId());
        boolean canManageTarget = canManage && !isSelf && viewerMember.getRank().isHigherThan(rank);

        return new MemberDisplayInfo(
                member.getPlayerName(),
                rank,
                member.getContribution(),
                dateFormat.format(new Date(member.getJoinedAt())),
                activityStatus,
                permissionInfo,
                permissionDesc,
                canManage,
                canManageTarget
        );
    }

    /**
     * 获取成员详情信息
     */
    public MemberDetailInfo getMemberDetailInfo(Sect sect, SectMember member) {
        List<String> permissionFlags = new ArrayList<>();
        String permLevelName = null;
        String permLevelDesc = null;

        if (sect.hasLand()) {
            ResidencePermissionManager permManager = plugin.getSectSystem().getPermissionManager();
            ResidencePermissionManager.PermissionLevel permLevel = permManager.getPermissionLevel(member.getRank());
            permLevelName = permLevel.getDisplayName();
            permLevelDesc = permLevel.getDescription();
            permissionFlags = permManager.getFlagsForLevel(permLevel);
        }

        return new MemberDetailInfo(
                member.getPlayerName(),
                member.getPlayerId().toString().substring(0, 8) + "...",
                member.getRank(),
                member.getContribution(),
                dateFormat.format(new Date(member.getJoinedAt())),
                dateFormat.format(new Date(member.getLastActiveAt())),
                sect.hasLand(),
                permLevelName,
                permLevelDesc,
                permissionFlags
        );
    }

    /**
     * 检查是否可以管理成员
     */
    public ManagePermissionResult checkManagePermission(Player viewer, Sect sect, SectMember target) {
        SectMember viewerMember = sect.getMember(viewer.getUniqueId());

        if (viewerMember == null || !viewerMember.getRank().hasManagePermission()) {
            return new ManagePermissionResult(false, "§c你没有权限管理成员!");
        }

        if (target.getPlayerId().equals(viewer.getUniqueId())) {
            return new ManagePermissionResult(false, "§c你不能管理自己!");
        }

        if (!viewerMember.getRank().isHigherThan(target.getRank())) {
            return new ManagePermissionResult(false, "§c你不能管理职位不低于你的成员!");
        }

        return new ManagePermissionResult(true, null);
    }

    /**
     * 获取职位对应的材质
     */
    public Material getMaterialForRank(SectRank rank) {
        return switch (rank) {
            case LEADER -> Material.GOLDEN_HELMET;
            case ELDER -> Material.DIAMOND_HELMET;
            case CORE_DISCIPLE -> Material.IRON_HELMET;
            case INNER_DISCIPLE -> Material.CHAINMAIL_HELMET;
            case OUTER_DISCIPLE -> Material.LEATHER_HELMET;
        };
    }

    /**
     * 检查是否可以晋升
     */
    public boolean canPromote(SectRank rank) {
        return rank != SectRank.LEADER;
    }

    /**
     * 检查是否可以降职
     */
    public boolean canDemote(SectRank rank) {
        return rank != SectRank.OUTER_DISCIPLE;
    }

    /**
     * 成员统计信息
     */
    public static class MemberStatsInfo {
        private final int memberCount;
        private final int maxMembers;
        private final int leaderCount;
        private final int elderCount;
        private final int coreCount;
        private final int innerCount;
        private final int outerCount;

        public MemberStatsInfo(int memberCount, int maxMembers, int leaderCount,
                               int elderCount, int coreCount, int innerCount, int outerCount) {
            this.memberCount = memberCount;
            this.maxMembers = maxMembers;
            this.leaderCount = leaderCount;
            this.elderCount = elderCount;
            this.coreCount = coreCount;
            this.innerCount = innerCount;
            this.outerCount = outerCount;
        }

        public int getMemberCount() { return memberCount; }
        public int getMaxMembers() { return maxMembers; }
        public int getLeaderCount() { return leaderCount; }
        public int getElderCount() { return elderCount; }
        public int getCoreCount() { return coreCount; }
        public int getInnerCount() { return innerCount; }
        public int getOuterCount() { return outerCount; }
    }

    /**
     * 权限统计信息
     */
    public static class PermissionStatsInfo {
        private final String landId;
        private final List<String> statsLines;

        public PermissionStatsInfo(String landId, List<String> statsLines) {
            this.landId = landId;
            this.statsLines = statsLines;
        }

        public String getLandId() { return landId; }
        public List<String> getStatsLines() { return statsLines; }
    }

    /**
     * 成员显示信息
     */
    public static class MemberDisplayInfo {
        private final String playerName;
        private final SectRank rank;
        private final int contribution;
        private final String joinDate;
        private final String activityStatus;
        private final String permissionLevel;
        private final String permissionDesc;
        private final boolean canViewDetails;
        private final boolean canManage;

        public MemberDisplayInfo(String playerName, SectRank rank, int contribution,
                                 String joinDate, String activityStatus,
                                 String permissionLevel, String permissionDesc,
                                 boolean canViewDetails, boolean canManage) {
            this.playerName = playerName;
            this.rank = rank;
            this.contribution = contribution;
            this.joinDate = joinDate;
            this.activityStatus = activityStatus;
            this.permissionLevel = permissionLevel;
            this.permissionDesc = permissionDesc;
            this.canViewDetails = canViewDetails;
            this.canManage = canManage;
        }

        public String getPlayerName() { return playerName; }
        public SectRank getRank() { return rank; }
        public int getContribution() { return contribution; }
        public String getJoinDate() { return joinDate; }
        public String getActivityStatus() { return activityStatus; }
        public String getPermissionLevel() { return permissionLevel; }
        public String getPermissionDesc() { return permissionDesc; }
        public boolean canViewDetails() { return canViewDetails; }
        public boolean canManage() { return canManage; }
        public boolean hasPermissionInfo() { return permissionLevel != null; }
    }

    /**
     * 成员详情信息
     */
    public static class MemberDetailInfo {
        private final String playerName;
        private final String shortUuid;
        private final SectRank rank;
        private final int contribution;
        private final String joinDate;
        private final String lastActiveDate;
        private final boolean hasLand;
        private final String permLevelName;
        private final String permLevelDesc;
        private final List<String> permissionFlags;

        public MemberDetailInfo(String playerName, String shortUuid, SectRank rank,
                                int contribution, String joinDate, String lastActiveDate,
                                boolean hasLand, String permLevelName, String permLevelDesc,
                                List<String> permissionFlags) {
            this.playerName = playerName;
            this.shortUuid = shortUuid;
            this.rank = rank;
            this.contribution = contribution;
            this.joinDate = joinDate;
            this.lastActiveDate = lastActiveDate;
            this.hasLand = hasLand;
            this.permLevelName = permLevelName;
            this.permLevelDesc = permLevelDesc;
            this.permissionFlags = permissionFlags;
        }

        public String getPlayerName() { return playerName; }
        public String getShortUuid() { return shortUuid; }
        public SectRank getRank() { return rank; }
        public int getContribution() { return contribution; }
        public String getJoinDate() { return joinDate; }
        public String getLastActiveDate() { return lastActiveDate; }
        public boolean hasLand() { return hasLand; }
        public String getPermLevelName() { return permLevelName; }
        public String getPermLevelDesc() { return permLevelDesc; }
        public List<String> getPermissionFlags() { return permissionFlags; }
    }

    /**
     * 管理权限检查结果
     */
    public static class ManagePermissionResult {
        private final boolean allowed;
        private final String errorMessage;

        public ManagePermissionResult(boolean allowed, String errorMessage) {
            this.allowed = allowed;
            this.errorMessage = errorMessage;
        }

        public boolean isAllowed() { return allowed; }
        public String getErrorMessage() { return errorMessage; }
    }
}
