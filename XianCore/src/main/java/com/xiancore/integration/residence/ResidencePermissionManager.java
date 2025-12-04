package com.xiancore.integration.residence;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagState;
import com.bekvon.bukkit.residence.containers.Flags;
import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Residence 权限管理器
 * 负责 SectRank 与 Residence 权限的映射和管理
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class ResidencePermissionManager {

    private final XianCore plugin;

    // 权限配置缓存（宗门ID -> 权限记录）
    private final Map<Integer, SectPermissionRecord> permissionRecords = new ConcurrentHashMap<>();

    // 权限审计日志系统
    private final PermissionAuditLog auditLog = new PermissionAuditLog();

    /**
     * 构造函数
     */
    public ResidencePermissionManager(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 为宗门设置权限
     * 将宗门成员的权限添加到 Residence 领地中
     */
    public void setupSectPermissions(Sect sect, ClaimedResidence residence) {
        if (sect == null || residence == null) {
            return;
        }

        try {
            // 创建权限记录
            SectPermissionRecord record = new SectPermissionRecord(sect.getId(), residence.getName());

            // 为宗主添加完全权限
            addLeaderPermissions(sect, residence, record);

            // 为长老添加高级权限
            addElderPermissions(sect, residence, record);

            // 为核心弟子添加管理权限
            addCoreDisciplePermissions(sect, residence, record);

            // 为内门弟子添加基础权限
            addInnerDisciplePermissions(sect, residence, record);

            // 为外门弟子添加访问权限
            addOuterDisciplePermissions(sect, residence, record);

            // 缓存权限记录
            permissionRecords.put(sect.getId(), record);

            // 记录审计日志
            auditLog.logPermissionsBatchSet(sect.getId(), sect.getMemberList().size());

            plugin.getLogger().info(String.format(
                "§a✓ 宗门权限已设置: %s (ID: %d, 领地: %s)",
                sect.getName(), sect.getId(), residence.getName()
            ));

        } catch (Exception e) {
            plugin.getLogger().warning("§c权限设置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加宗主权限（完全管理权）
     */
    private void addLeaderPermissions(Sect sect, ClaimedResidence residence, SectPermissionRecord record) {
        for (SectMember member : sect.getMemberList()) {
            if (member.getRank() == SectRank.LEADER) {
                try {
                    // 添加管理员权限
                    addResidencePermission(residence, member, PermissionLevel.ADMIN);
                    record.addPermission(member.getPlayerId().toString(), PermissionLevel.ADMIN, member.getPlayerName());

                    // 记录审计日志
                    auditLog.logPermissionGranted(sect.getId(), member.getPlayerId().toString(),
                        member.getPlayerName(), PermissionLevel.ADMIN.getDisplayName());
                } catch (Exception e) {
                    plugin.getLogger().warning("§c无法为宗主 " + member.getPlayerName() + " 设置权限");
                }
            }
        }
    }

    /**
     * 添加长老权限（高级管理权）
     */
    private void addElderPermissions(Sect sect, ClaimedResidence residence, SectPermissionRecord record) {
        for (SectMember member : sect.getMemberList()) {
            if (member.getRank() == SectRank.ELDER) {
                try {
                    // 添加编辑权限（可以管理部分功能，但不能删除或转让）
                    addResidencePermission(residence, member, PermissionLevel.MANAGER);
                    record.addPermission(member.getPlayerId().toString(), PermissionLevel.MANAGER, member.getPlayerName());
                } catch (Exception e) {
                    plugin.getLogger().warning("§c无法为长老 " + member.getPlayerName() + " 设置权限");
                }
            }
        }
    }

    /**
     * 添加核心弟子权限（建筑权）
     */
    private void addCoreDisciplePermissions(Sect sect, ClaimedResidence residence, SectPermissionRecord record) {
        for (SectMember member : sect.getMemberList()) {
            if (member.getRank() == SectRank.CORE_DISCIPLE) {
                try {
                    // 添加建筑权
                    addResidencePermission(residence, member, PermissionLevel.BUILDER);
                    record.addPermission(member.getPlayerId().toString(), PermissionLevel.BUILDER, member.getPlayerName());
                } catch (Exception e) {
                    plugin.getLogger().warning("§c无法为核心弟子 " + member.getPlayerName() + " 设置权限");
                }
            }
        }
    }

    /**
     * 添加内门弟子权限（基础访问权）
     */
    private void addInnerDisciplePermissions(Sect sect, ClaimedResidence residence, SectPermissionRecord record) {
        for (SectMember member : sect.getMemberList()) {
            if (member.getRank() == SectRank.INNER_DISCIPLE) {
                try {
                    // 添加访问权（可以进入和使用，但不能建筑）
                    addResidencePermission(residence, member, PermissionLevel.MEMBER);
                    record.addPermission(member.getPlayerId().toString(), PermissionLevel.MEMBER, member.getPlayerName());
                } catch (Exception e) {
                    plugin.getLogger().warning("§c无法为内门弟子 " + member.getPlayerName() + " 设置权限");
                }
            }
        }
    }

    /**
     * 添加外门弟子权限（有限访问权）
     */
    private void addOuterDisciplePermissions(Sect sect, ClaimedResidence residence, SectPermissionRecord record) {
        for (SectMember member : sect.getMemberList()) {
            if (member.getRank() == SectRank.OUTER_DISCIPLE) {
                try {
                    // 添加有限访问权
                    addResidencePermission(residence, member, PermissionLevel.GUEST);
                    record.addPermission(member.getPlayerId().toString(), PermissionLevel.GUEST, member.getPlayerName());
                } catch (Exception e) {
                    plugin.getLogger().warning("§c无法为外门弟子 " + member.getPlayerName() + " 设置权限");
                }
            }
        }
    }

    /**
     * 添加 Residence 权限
     * 根据权限等级调用 Residence API 为玩家设置权限标志
     *
     * @param residence Residence领地对象
     * @param member 宗门成员
     * @param level 权限等级
     */
    private void addResidencePermission(ClaimedResidence residence, SectMember member, PermissionLevel level) {
        try {
            if (residence == null || member == null) {
                return;
            }

            String playerName = member.getPlayerName();

            // 获取Residence权限管理器
            var perms = residence.getPermissions();
            if (perms == null) {
                plugin.getLogger().warning("§c无法获取Residence权限管理器: " + residence.getName());
                return;
            }

            // ==================== 根据权限等级设置标志 ====================
            switch (level) {
                case ADMIN:
                    // 管理员权限：完全控制
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    plugin.getLogger().info("§a✓ 权限设置: " + playerName + " → 管理员 (领地: " + residence.getName() + ")");
                    break;

                case MANAGER:
                    // 管理者权限：高级管理
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.FALSE);
                    plugin.getLogger().info("§a✓ 权限设置: " + playerName + " → 管理者 (领地: " + residence.getName() + ")");
                    break;

                case BUILDER:
                    // 建筑者权限：可建造和破坏
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.FALSE);
                    plugin.getLogger().info("§a✓ 权限设置: " + playerName + " → 建筑者 (领地: " + residence.getName() + ")");
                    break;

                case MEMBER:
                    // 成员权限：基础访问和使用
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.FALSE);
                    plugin.getLogger().info("§a✓ 权限设置: " + playerName + " → 成员 (领地: " + residence.getName() + ")");
                    break;

                case GUEST:
                    // 访客权限：仅允许进入和移动
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.FALSE);
                    plugin.getLogger().info("§a✓ 权限设置: " + playerName + " → 访客 (领地: " + residence.getName() + ")");
                    break;

                default:
                    plugin.getLogger().warning("§c未知的权限等级: " + level);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("§c权限设置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 添加新成员权限
     * 当玩家加入宗门时调用
     */
    public void addMemberPermission(Sect sect, SectMember member, ClaimedResidence residence) {
        if (sect == null || member == null || residence == null) {
            return;
        }

        try {
            PermissionLevel level = getPermissionLevel(member.getRank());
            addResidencePermission(residence, member, level);

            // 更新权限记录
            SectPermissionRecord record = permissionRecords.get(sect.getId());
            if (record != null) {
                record.addPermission(member.getPlayerId().toString(), level, member.getPlayerName());
            }

            // 记录审计日志
            auditLog.logPermissionGranted(sect.getId(), member.getPlayerId().toString(),
                member.getPlayerName(), level.getDisplayName());

            plugin.getLogger().info(String.format(
                "§a✓ 为成员 %s (%s) 添加了权限: %s",
                member.getPlayerName(), member.getPlayerId(), level.getDisplayName()
            ));

        } catch (Exception e) {
            plugin.getLogger().warning("§c添加成员权限失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 移除成员权限
     * 当玩家离开宗门时调用
     */
    public void removeMemberPermission(Sect sect, SectMember member, ClaimedResidence residence) {
        if (sect == null || member == null || residence == null) {
            return;
        }

        try {
            String playerName = member.getPlayerName();

            // 获取Residence权限管理器
            var perms = residence.getPermissions();
            if (perms != null) {
                // 移除所有权限标志
                perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.FALSE);
                perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.FALSE);
                perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.FALSE);
                perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.FALSE);
                perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.FALSE);
                perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.FALSE);

                plugin.getLogger().info("§a✓ 权限已移除: " + playerName + " (领地: " + residence.getName() + ")");
            }

            // 更新权限记录
            SectPermissionRecord record = permissionRecords.get(sect.getId());
            if (record != null) {
                PermissionLevel previousLevel = null;
                SectPermissionRecord.PermissionEntry entry = record.getPermission(member.getPlayerId().toString());
                if (entry != null) {
                    previousLevel = entry.getLevel();
                }

                record.removePermission(member.getPlayerId().toString());

                // 记录审计日志
                if (previousLevel != null) {
                    auditLog.logPermissionRevoked(sect.getId(), member.getPlayerId().toString(),
                        member.getPlayerName(), previousLevel.getDisplayName());
                }
            }

            plugin.getLogger().info(String.format(
                "§a✓ 已移除成员 %s 的权限",
                member.getPlayerName()
            ));

        } catch (Exception e) {
            plugin.getLogger().warning("§c移除成员权限失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新成员权限（当职位变更时）
     */
    public void updateMemberPermission(Sect sect, SectMember member, SectRank oldRank, ClaimedResidence residence) {
        if (sect == null || member == null || residence == null) {
            return;
        }

        try {
            // 先移除旧权限
            removeMemberPermission(sect, member, residence);

            // 再添加新权限
            addMemberPermission(sect, member, residence);

            plugin.getLogger().info(String.format(
                "§a✓ 已更新成员 %s 的权限: %s → %s",
                member.getPlayerName(), oldRank.getDisplayName(), member.getRank().getDisplayName()
            ));

        } catch (Exception e) {
            plugin.getLogger().warning("§c更新成员权限失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 清除宗门所有权限（删除领地时）
     */
    public void clearSectPermissions(Sect sect) {
        if (sect == null) {
            return;
        }

        try {
            // 移除权限记录
            permissionRecords.remove(sect.getId());

            // 记录审计日志
            auditLog.logPermissionsCleared(sect.getId(), "领地被删除");

            plugin.getLogger().info(String.format(
                "§a✓ 已清除宗门 %s 的所有权限",
                sect.getName()
            ));

        } catch (Exception e) {
            plugin.getLogger().warning("§c清除权限失败: " + e.getMessage());
        }
    }

    /**
    /**
     * 根据权限等级获取对应的 Residence flags
     */
    public List<String> getFlagsForLevel(PermissionLevel level) {
        return switch (level) {
            case ADMIN -> Arrays.asList("admin", "owner", "allow", "deny", "flags", "remove");
            case MANAGER -> Arrays.asList("allow", "flags", "container");
            case BUILDER -> Arrays.asList("allow", "container", "build");
            case MEMBER -> Arrays.asList("allow", "container", "move");
            case GUEST -> Arrays.asList("allow", "move");
        };
    }

    /**
     * 根据职位获取权限等级
     */
    public PermissionLevel getPermissionLevel(SectRank rank) {
        return switch (rank) {
            case LEADER -> PermissionLevel.ADMIN;
            case ELDER -> PermissionLevel.MANAGER;
            case CORE_DISCIPLE -> PermissionLevel.BUILDER;
            case INNER_DISCIPLE -> PermissionLevel.MEMBER;
            case OUTER_DISCIPLE -> PermissionLevel.GUEST;
        };
    }

    /**
     * 验证玩家权限
     */
    public boolean hasPermission(Player player, Sect sect, PermissionLevel requiredLevel) {
        SectMember member = sect.getMember(player.getUniqueId());
        if (member == null) {
            return false;
        }

        PermissionLevel memberLevel = getPermissionLevel(member.getRank());
        return memberLevel.getLevel() >= requiredLevel.getLevel();
    }

    /**
     * 权限等级枚举
     */
    public enum PermissionLevel {
        ADMIN(4, "§c管理员", "完全管理权限"),
        MANAGER(3, "§6管理者", "高级管理权限"),
        BUILDER(2, "§e建筑者", "建筑权限"),
        MEMBER(1, "§a成员", "基础访问权限"),
        GUEST(0, "§7访客", "有限访问权限");

        private final int level;
        private final String displayName;
        private final String description;

        PermissionLevel(int level, String displayName, String description) {
            this.level = level;
            this.displayName = displayName;
            this.description = description;
        }

        public int getLevel() {
            return level;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 宗门权限记录
     */
    public static class SectPermissionRecord {
        private final int sectId;
        private final String residenceId;
        private final long createdTime;
        private final Map<String, PermissionEntry> permissions = new ConcurrentHashMap<>();

        public SectPermissionRecord(int sectId, String residenceId) {
            this.sectId = sectId;
            this.residenceId = residenceId;
            this.createdTime = System.currentTimeMillis();
        }

        public void addPermission(String playerUuid, PermissionLevel level, String playerName) {
            permissions.put(playerUuid, new PermissionEntry(playerUuid, playerName, level, System.currentTimeMillis()));
        }

        public void removePermission(String playerUuid) {
            permissions.remove(playerUuid);
        }

        public PermissionEntry getPermission(String playerUuid) {
            return permissions.get(playerUuid);
        }

        public Collection<PermissionEntry> getAllPermissions() {
            return new ArrayList<>(permissions.values());
        }

        public int getSectId() {
            return sectId;
        }

        public String getResidenceId() {
            return residenceId;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public int getPermissionCount() {
            return permissions.size();
        }

        /**
         * 权限条目
         */
        public static class PermissionEntry {
            private final String playerUuid;
            private final String playerName;
            private final PermissionLevel level;
            private final long grantedTime;

            public PermissionEntry(String playerUuid, String playerName, PermissionLevel level, long grantedTime) {
                this.playerUuid = playerUuid;
                this.playerName = playerName;
                this.level = level;
                this.grantedTime = grantedTime;
            }

            public String getPlayerUuid() {
                return playerUuid;
            }

            public String getPlayerName() {
                return playerName;
            }

            public PermissionLevel getLevel() {
                return level;
            }

            public long getGrantedTime() {
                return grantedTime;
            }
        }
    }

    /**
     * 获取权限审计日志信息
     */
    public String getAuditLogStatistics(int sectId) {
        return auditLog.getLogStatistics(sectId);
    }

    /**
     * 获取宗门最近的权限日志
     */
    public List<PermissionAuditLog.PermissionAuditEvent> getRecentAuditLogs(int sectId, int count) {
        return auditLog.getRecentLogs(sectId, count);
    }

    /**
     * 获取玩家的权限日志
     */
    public List<PermissionAuditLog.PermissionAuditEvent> getPlayerAuditLogs(int sectId, String playerUuid) {
        return auditLog.getPlayerLogs(sectId, playerUuid);
    }

    /**
     * 获取宗门权限统计
     */
    public String getPermissionStatistics(int sectId) {
        SectPermissionRecord record = permissionRecords.get(sectId);
        if (record == null) {
            return "未找到权限记录";
        }

        int adminCount = (int) record.getAllPermissions().stream()
            .filter(p -> p.getLevel() == PermissionLevel.ADMIN).count();
        int managerCount = (int) record.getAllPermissions().stream()
            .filter(p -> p.getLevel() == PermissionLevel.MANAGER).count();
        int builderCount = (int) record.getAllPermissions().stream()
            .filter(p -> p.getLevel() == PermissionLevel.BUILDER).count();

        return String.format(
            "权限统计 (宗门ID: %d, 领地: %s):\n" +
            "  管理员: %d 人\n" +
            "  管理者: %d 人\n" +
            "  建筑者: %d 人\n" +
            "  总权限数: %d",
            record.getSectId(), record.getResidenceId(),
            adminCount, managerCount, builderCount, record.getPermissionCount()
        );
    }

    /**
     * 诊断玩家权限 - 验证玩家在Residence中的权限是否与预期相符
     *
     * @param player 玩家对象
     * @param sect 宗门对象
     * @param residence 领地对象
     * @return 诊断报告字符串
     */
    public String diagnoseMemberPermissions(Player player, Sect sect, ClaimedResidence residence) {
        StringBuilder report = new StringBuilder();
        report.append("========== 权限诊断报告 ==========\n");
        report.append("玩家: ").append(player.getName()).append("\n");
        report.append("宗门: ").append(sect.getName()).append("\n");
        report.append("领地: ").append(residence.getName()).append("\n");

        try {
            // 获取成员和权限信息
            SectMember member = sect.getMember(player.getUniqueId());
            if (member == null) {
                report.append("§c[错误] 玩家不在该宗门中\n");
                return report.toString();
            }

            report.append("职位: ").append(member.getRank().getDisplayName()).append("\n");

            // 获取预期权限等级
            PermissionLevel expectedLevel = getPermissionLevel(member.getRank());
            report.append("预期权限等级: ").append(expectedLevel.getDisplayName()).append("\n");

            // 检查Residence权限
            var perms = residence.getPermissions();
            if (perms == null) {
                report.append("§c[错误] 无法获取领地权限管理器\n");
                return report.toString();
            }

            String playerName = player.getName();
            report.append("\n========== 实际权限状态 ==========\n");

            // 检查每个关键权限标志
            boolean hasBuild = perms.playerHas(player, Flags.build, false);
            boolean hasDestroy = perms.playerHas(player, Flags.destroy, false);
            boolean hasAdmin = perms.playerHas(player, Flags.admin, false);
            boolean hasUse = perms.playerHas(player, Flags.use, false);
            boolean hasContainer = perms.playerHas(player, Flags.container, false);
            boolean hasMove = perms.playerHas(player, Flags.move, false);

            report.append("build: ").append(hasBuild ? "§a✓" : "§c✗").append("\n");
            report.append("destroy: ").append(hasDestroy ? "§a✓" : "§c✗").append("\n");
            report.append("admin: ").append(hasAdmin ? "§a✓" : "§c✗").append("\n");
            report.append("use: ").append(hasUse ? "§a✓" : "§c✗").append("\n");
            report.append("container: ").append(hasContainer ? "§a✓" : "§c✗").append("\n");
            report.append("move: ").append(hasMove ? "§a✓" : "§c✗").append("\n");

            // 验证权限是否符合预期
            report.append("\n========== 验证结果 ==========\n");
            boolean isConsistent = verifyPermissionConsistency(expectedLevel, hasBuild, hasDestroy,
                hasAdmin, hasUse, hasContainer, hasMove);

            if (isConsistent) {
                report.append("§a✓ 权限配置正确\n");
            } else {
                report.append("§c✗ 权限配置有误，需要重新同步\n");
                report.append("建议: 执行晋升/降职命令重新同步权限\n");
            }

            report.append("====================================");

        } catch (Exception e) {
            report.append("§c[错误] 诊断失败: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }

        return report.toString();
    }

    /**
     * 验证权限等级是否与实际权限标志一致
     */
    private boolean verifyPermissionConsistency(PermissionLevel level, boolean build, boolean destroy,
                                               boolean admin, boolean use, boolean container, boolean move) {
        return switch (level) {
            case ADMIN -> build && destroy && admin && use && container && move;
            case MANAGER -> build && destroy && !admin && use && container && move;
            case BUILDER -> build && destroy && !admin && use && container && move;
            case MEMBER -> !build && !destroy && !admin && use && container && move;
            case GUEST -> !build && !destroy && !admin && !use && !container && move;
        };
    }
}
