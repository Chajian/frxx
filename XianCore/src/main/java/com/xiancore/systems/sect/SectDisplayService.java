package com.xiancore.systems.sect;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 宗门显示服务
 * 负责宗门GUI相关的数据获取和权限检查
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectDisplayService {

    private final XianCore plugin;

    // 创建宗门所需的最低境界（炼气期以上）
    private static final String REQUIRED_REALM_FOR_CREATE = "炼气期";

    public SectDisplayService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 检查玩家是否已加入宗门
     */
    public boolean hasSect(PlayerData data) {
        return data.getSectId() != null;
    }

    /**
     * 获取玩家的宗门
     */
    public Sect getPlayerSect(UUID playerUUID) {
        return plugin.getSectSystem().getPlayerSect(playerUUID);
    }

    /**
     * 检查玩家境界是否满足创建宗门条件
     */
    public boolean canCreateSect(PlayerData data) {
        // 炼气期不能创建，需要筑基期以上
        return !REQUIRED_REALM_FOR_CREATE.equals(data.getRealm());
    }

    /**
     * 检查玩家是否是宗主
     */
    public boolean isLeader(PlayerData data) {
        SectRank rank = SectRank.fromRankString(data.getSectRank());
        return rank == SectRank.LEADER;
    }

    /**
     * 检查玩家是否是长老或宗主
     */
    public boolean isElderOrAbove(PlayerData data) {
        SectRank rank = SectRank.fromRankString(data.getSectRank());
        return rank == SectRank.LEADER || rank == SectRank.ELDER;
    }

    /**
     * 获取宗门显示信息
     */
    public SectDisplayInfo getSectDisplayInfo(Player player, PlayerData data) {
        if (!hasSect(data)) {
            return SectDisplayInfo.noSect(canCreateSect(data));
        }

        Sect sect = getPlayerSect(player.getUniqueId());
        if (sect == null) {
            return SectDisplayInfo.noSect(canCreateSect(data));
        }

        String coloredRank = SectRank.getColoredDisplayName(data.getSectRank());
        int memberCount = sect.getMemberList().size();
        int maxMembers = sect.getMaxMembers();
        boolean isLeader = isLeader(data);
        boolean isElderOrAbove = isElderOrAbove(data);

        return new SectDisplayInfo(
                true,
                data.getSectId(),
                coloredRank,
                memberCount,
                maxMembers,
                isLeader,
                isElderOrAbove,
                canCreateSect(data)
        );
    }

    /**
     * 宗门显示信息
     */
    public static class SectDisplayInfo {
        private final boolean inSect;
        private final Integer sectId;
        private final String coloredRank;
        private final int memberCount;
        private final int maxMembers;
        private final boolean isLeader;
        private final boolean isElderOrAbove;
        private final boolean canCreateSect;

        public SectDisplayInfo(boolean inSect, Integer sectId, String coloredRank,
                               int memberCount, int maxMembers, boolean isLeader,
                               boolean isElderOrAbove, boolean canCreateSect) {
            this.inSect = inSect;
            this.sectId = sectId;
            this.coloredRank = coloredRank;
            this.memberCount = memberCount;
            this.maxMembers = maxMembers;
            this.isLeader = isLeader;
            this.isElderOrAbove = isElderOrAbove;
            this.canCreateSect = canCreateSect;
        }

        /**
         * 创建未加入宗门的信息
         */
        public static SectDisplayInfo noSect(boolean canCreateSect) {
            return new SectDisplayInfo(false, null, null, 0, 0, false, false, canCreateSect);
        }

        public boolean isInSect() { return inSect; }
        public Integer getSectId() { return sectId; }
        public String getColoredRank() { return coloredRank; }
        public int getMemberCount() { return memberCount; }
        public int getMaxMembers() { return maxMembers; }
        public boolean isLeader() { return isLeader; }
        public boolean isElderOrAbove() { return isElderOrAbove; }
        public boolean canCreateSect() { return canCreateSect; }

        public String getMemberCountDisplay() {
            return memberCount + "§7/§f" + maxMembers;
        }
    }
}
