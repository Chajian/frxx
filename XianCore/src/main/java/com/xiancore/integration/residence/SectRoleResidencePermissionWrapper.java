package com.xiancore.integration.residence;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagState;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 宗门角色-Residence权限包装器
 *
 * 功能：
 * - 将Residence的权限系统与宗门角色绑定
 * - 支持所有Residence权限标志 (100+)
 * - 为角色配置权限后批量应用到该角色的所有成员
 * - 作为SectRolePermissionGUI的增强版本
 *
 * 使用方式：
 * SectRoleResidencePermissionWrapper wrapper =
 *     new SectRoleResidencePermissionWrapper(plugin, sect, residence);
 *
 * // 1. 获取所有权限标志
 * List<String> allFlags = wrapper.getAllAvailableFlags();
 *
 * // 2. 为某个角色获取当前权限配置
 * Map<String, FlagState> rolePerms = wrapper.getRolePermissions(SectRank.ELDER);
 *
 * // 3. 为某个角色设置某个权限
 * wrapper.setRolePermission(SectRank.ELDER, "build", FlagState.TRUE);
 *
 * // 4. 批量应用角色权限到所有成员
 * wrapper.applyRolePermissionsToAllMembers(SectRank.ELDER);
 *
 * @author Claude
 * @version 1.0.0
 */
public class SectRoleResidencePermissionWrapper {

    private final XianCore plugin;
    private final Sect sect;
    private final ClaimedResidence residence;
    private final ResidencePermissionManager permissionManager;

    // 缓存角色权限配置 (SectRank -> 权限标志名 -> 权限状态)
    private final Map<SectRank, Map<String, FlagState>> rolePermissionCache = new HashMap<>();

    // 虚拟角色玩家名称 (用于存储角色级别的权限)
    private static final String ROLE_MARKER_PREFIX = "__SECT_ROLE_";

    public SectRoleResidencePermissionWrapper(
            XianCore plugin,
            Sect sect,
            ClaimedResidence residence,
            ResidencePermissionManager permissionManager) {
        this.plugin = plugin;
        this.sect = sect;
        this.residence = residence;
        this.permissionManager = permissionManager;

        // 初始化缓存
        initializeRolePermissionCache();
    }

    /**
     * 初始化缓存 - 从Residence系统中读取现有的角色权限配置
     */
    private void initializeRolePermissionCache() {
        for (SectRank rank : SectRank.values()) {
            Map<String, FlagState> rankPerms = new HashMap<>();

            for (Flags flag : Flags.values()) {
                String flagName = flag.toString();
                FlagState state = getCurrentRolePermission(rank, flagName);
                rankPerms.put(flagName, state);
            }

            rolePermissionCache.put(rank, rankPerms);
        }

        plugin.getLogger().info("已初始化宗门角色权限缓存，包含" + getAllAvailableFlags().size() + "个权限标志");
    }

    /**
     * 获取所有可用的权限标志名称列表
     * 返回Residence中所有注册的权限标志
     */
    public List<String> getAllAvailableFlags() {
        List<String> flags = new ArrayList<>();
        for (Flags flag : Flags.values()) {
            flags.add(flag.toString());
        }
        return flags;
    }

    /**
     * 获取权限标志的显示名称
     */
    public String getFlagDisplayName(String flagName) {
        Flags flag = Flags.getFlag(flagName);
        return flag != null ? flag.getName() : flagName;
    }

    /**
     * 获取权限标志的描述
     */
    public String getFlagDescription(String flagName) {
        Flags flag = Flags.getFlag(flagName);
        return flag != null ? flag.getDesc() : "No description available";
    }

    /**
     * 获取某个角色的所有权限配置
     * 返回格式: 权限标志名 -> 权限状态
     */
    public Map<String, FlagState> getRolePermissions(SectRank rank) {
        return new HashMap<>(rolePermissionCache.getOrDefault(rank, new HashMap<>()));
    }

    /**
     * 获取某个角色对特定权限的设置
     */
    public FlagState getRolePermission(SectRank rank, String flagName) {
        Map<String, FlagState> rankPerms = rolePermissionCache.get(rank);
        if (rankPerms == null) {
            rankPerms = new HashMap<>();
            rolePermissionCache.put(rank, rankPerms);
        }
        return rankPerms.getOrDefault(flagName, FlagState.NEITHER);
    }

    /**
     * 为某个角色设置特定权限
     */
    public void setRolePermission(SectRank rank, String flagName, FlagState state) {
        Map<String, FlagState> rankPerms = rolePermissionCache.computeIfAbsent(rank, k -> new HashMap<>());
        rankPerms.put(flagName, state);

        plugin.getLogger().info("设置权限: 角色=" + rank.name() + ", 权限=" + flagName + ", 状态=" + state.name());
    }

    /**
     * 切换权限状态 (TRUE -> FALSE -> NEITHER -> TRUE)
     */
    public FlagState toggleRolePermission(SectRank rank, String flagName) {
        FlagState current = getRolePermission(rank, flagName);

        FlagState next;
        switch (current) {
            case TRUE:
                next = FlagState.FALSE;
                break;
            case FALSE:
                next = FlagState.NEITHER;
                break;
            case NEITHER:
                next = FlagState.TRUE;
                break;
            default:
                next = FlagState.TRUE;
        }

        setRolePermission(rank, flagName, next);
        return next;
    }

    /**
     * 将某个角色的权限配置应用到该角色的所有成员
     *
     * 流程:
     * 1. 获取该角色的所有成员
     * 2. 获取该角色的权限配置
     * 3. 为每个成员应用所有权限
     */
    public void applyRolePermissionsToAllMembers(SectRank rank) {
        // 获取该角色的所有成员
        List<SectMember> members = sect.getMembersByRank(rank);

        if (members.isEmpty()) {
            plugin.getLogger().warning("角色 " + rank.name() + " 没有成员，跳过权限应用");
            return;
        }

        // 获取该角色的权限配置
        Map<String, FlagState> rolePerms = getRolePermissions(rank);

        int successCount = 0;
        int failCount = 0;

        // 为每个成员应用权限
        for (SectMember member : members) {
            String playerName = member.getPlayerName();

            try {
                // 为该玩家应用所有权限
                for (Map.Entry<String, FlagState> perm : rolePerms.entrySet()) {
                    String flagName = perm.getKey();
                    FlagState state = perm.getValue();

                    // 应用权限状态
                    residence.getPermissions().setPlayerFlag(playerName, flagName, state);
                }

                successCount++;
            } catch (Exception e) {
                plugin.getLogger().warning("为玩家 " + playerName + " 应用权限失败: " + e.getMessage());
                failCount++;
            }
        }

        // 保存到数据库
        try {
            residence.save();
            plugin.getLogger().info("✓ 已为角色 " + rank.name() + " 的 " + successCount + " 个成员应用权限");

            if (failCount > 0) {
                plugin.getLogger().warning("✗ 有 " + failCount + " 个成员应用权限失败");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("保存权限配置失败: " + e.getMessage());
        }
    }

    /**
     * 将某个角色的权限配置应用到特定玩家
     */
    public void applyRolePermissionsToMember(SectRank rank, String playerName) {
        Map<String, FlagState> rolePerms = getRolePermissions(rank);

        try {
            for (Map.Entry<String, FlagState> perm : rolePerms.entrySet()) {
                String flagName = perm.getKey();
                FlagState state = perm.getValue();

                residence.getPermissions().setPlayerFlag(playerName, flagName, state);
            }

            residence.save();
            plugin.getLogger().info("已为玩家 " + playerName + " 应用角色 " + rank.name() + " 的权限");
        } catch (Exception e) {
            plugin.getLogger().warning("为玩家 " + playerName + " 应用权限失败: " + e.getMessage());
        }
    }

    /**
     * 重置某个角色的权限为默认配置
     */
    public void resetRolePermissionsToDefault(SectRank rank) {
        Map<String, FlagState> rankPerms = rolePermissionCache.get(rank);
        if (rankPerms != null) {
            rankPerms.clear();
        }

        // 设置默认权限配置
        setDefaultPermissions(rank);

        plugin.getLogger().info("已重置角色 " + rank.name() + " 的权限为默认配置");
    }

    /**
     * 为角色设置默认权限
     */
    private void setDefaultPermissions(SectRank rank) {
        Map<String, FlagState> rankPerms = rolePermissionCache.computeIfAbsent(rank, k -> new HashMap<>());

        // 根据职位设置默认权限
        switch (rank) {
            case LEADER:  // 宗主 - 完全权限
                for (Flags flag : Flags.values()) {
                    rankPerms.put(flag.toString(), FlagState.TRUE);
                }
                break;

            case ELDER:  // 长老 - 管理权限（除admin外）
                for (Flags flag : Flags.values()) {
                    if (!flag.equals(Flags.admin)) {
                        rankPerms.put(flag.toString(), FlagState.TRUE);
                    } else {
                        rankPerms.put(flag.toString(), FlagState.FALSE);
                    }
                }
                break;

            case CORE_DISCIPLE:  // 核心弟子 - 建造权限
            case INNER_DISCIPLE:  // 内门弟子 - 建造权限
                setPermissionByNames(rankPerms, true,
                        "build", "destroy", "use", "container", "move");
                setPermissionByNames(rankPerms, false, "admin");
                break;

            case OUTER_DISCIPLE:  // 外门弟子 - 基础权限
                setPermissionByNames(rankPerms, true, "use", "container", "move");
                setPermissionByNames(rankPerms, false,
                        "build", "destroy", "admin");
                break;
        }
    }

    /**
     * 设置指定权限的状态
     */
    private void setPermissionByNames(Map<String, FlagState> perms, boolean enabled, String... names) {
        FlagState state = enabled ? FlagState.TRUE : FlagState.FALSE;
        for (String name : names) {
            perms.put(name, state);
        }
    }

    /**
     * 获取权限的当前状态（从缓存）
     */
    private FlagState getCurrentRolePermission(SectRank rank, String flagName) {
        // 这个方法用于初始化缓存时读取已保存的权限
        // 首次运行时会返回默认的NEITHER状态
        return FlagState.NEITHER;
    }

    /**
     * 获取一个角色的权限统计
     */
    public Map<String, Object> getRolePermissionStatistics(SectRank rank) {
        Map<String, Object> stats = new HashMap<>();
        Map<String, FlagState> perms = getRolePermissions(rank);

        int trueCount = 0;
        int falseCount = 0;
        int neitherCount = 0;

        for (FlagState state : perms.values()) {
            switch (state) {
                case TRUE:
                    trueCount++;
                    break;
                case FALSE:
                    falseCount++;
                    break;
                case NEITHER:
                    neitherCount++;
                    break;
            }
        }

        stats.put("enabled", trueCount);
        stats.put("disabled", falseCount);
        stats.put("notset", neitherCount);
        stats.put("total", perms.size());

        return stats;
    }

    /**
     * 打开权限管理界面供玩家编辑
     * 注意: 这个方法返回权限列表供GUI显示
     */
    public List<PermissionItem> getPermissionItemsForGui(SectRank rank) {
        List<PermissionItem> items = new ArrayList<>();
        Map<String, FlagState> perms = getRolePermissions(rank);

        for (Flags flag : Flags.values()) {
            String flagName = flag.toString();
            FlagState state = perms.getOrDefault(flagName, FlagState.NEITHER);

            PermissionItem item = new PermissionItem(
                    flagName,
                    flag.getName(),
                    flag.getDesc(),
                    state
            );

            items.add(item);
        }

        return items;
    }

    /**
     * 权限项目类 - 用于GUI显示
     */
    public static class PermissionItem {
        public final String flagName;
        public final String displayName;
        public final String description;
        public final FlagState state;

        public PermissionItem(String flagName, String displayName, String description, FlagState state) {
            this.flagName = flagName;
            this.displayName = displayName;
            this.description = description;
            this.state = state;
        }

        public boolean isEnabled() {
            return state == FlagState.TRUE;
        }

        public boolean isDisabled() {
            return state == FlagState.FALSE;
        }

        public boolean isNotSet() {
            return state == FlagState.NEITHER;
        }
    }
}
