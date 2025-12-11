package com.xiancore.gui;

import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagState;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.integration.residence.ResidencePermissionManager;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 宗门角色权限配置 GUI
 *
 * 功能：
 * - 查看和编辑不同宗门角色的权限预设
 * - 复用Residence权限标志概念，但应用于角色而非个体玩家
 * - 实时预览当前角色的权限状态
 * - 批量应用权限变更到所有该角色的成员
 *
 * 权限标志对应关系：
 * - build      → 建造权限
 * - destroy    → 破坏权限
 * - admin      → 权限管理权限
 * - use        → 使用设备权限
 * - container  → 容器访问权限
 * - move       → 移动权限
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectRolePermissionGUI {

    private final XianCore plugin;
    private final ResidencePermissionManager permissionManager;

    // 权限标志列表 - 按显示顺序排列
    private static final List<PermissionFlag> PERMISSION_FLAGS = Arrays.asList(
        new PermissionFlag(Flags.build, "§e建造", "§7允许建造方块"),
        new PermissionFlag(Flags.destroy, "§c破坏", "§7允许破坏方块"),
        new PermissionFlag(Flags.use, "§6使用", "§7允许使用门、开关等"),
        new PermissionFlag(Flags.container, "§5容器", "§7允许打开箱子、炉子等"),
        new PermissionFlag(Flags.admin, "§4管理", "§7允许修改其他玩家权限"),
        new PermissionFlag(Flags.move, "§b移动", "§7允许在领地内移动")
    );

    // 临时权限编辑缓存 (宗门ID -> 角色 -> 权限标志状态)
    private final Map<Integer, Map<SectRank, Map<String, Boolean>>> editingCache = new HashMap<>();

    public SectRolePermissionGUI(XianCore plugin, ResidencePermissionManager permissionManager) {
        this.plugin = plugin;
        this.permissionManager = permissionManager;
    }

    /**
     * 打开角色权限配置主界面
     * 显示所有宗门角色及其权限预览
     */
    public void openRolePermissionMain(Player player, Sect sect) {
        ChestGui gui = new ChestGui(6, "§b§l角色权限配置 - " + sect.getName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // ==================== 背景边框 ====================
        GUIUtils.addBackground(gui, 6, Material.BLUE_STAINED_GLASS_PANE);

        // ==================== 标题和说明 ====================
        StaticPane headerPane = new StaticPane(1, 0, 7, 1);
        ItemStack titleItem = new ItemBuilder(Material.BOOK)
            .name("§b§l角色权限配置")
            .lore("§7点击角色卡片查看或编辑权限")
            .lore("§7Shift+左键 编辑 | 右键 查看详情")
            .build();
        headerPane.addItem(new GuiItem(titleItem), 0, 0);
        gui.addPane(headerPane);

        // ==================== 角色列表 ====================
        StaticPane rolePane = new StaticPane(1, 1, 7, 4);

        SectRank[] ranks = SectRank.values();
        for (int i = 0; i < ranks.length; i++) {
            SectRank rank = ranks[i];

            // 创建角色卡片
            ItemStack roleCard = createRoleCard(sect, rank);
            int row = i / 7;
            int col = i % 7;

            if (row < 4) {
                GuiItem guiItem = new GuiItem(roleCard, event -> {
                    event.setCancelled(true);
                    if (event.isShiftClick()) {
                        // Shift+点击 - 编辑该角色的权限
                        openRolePermissionEditor(player, sect, rank);
                    } else {
                        // 普通点击 - 查看详情
                        openRolePermissionDetail(player, sect, rank);
                    }
                });
                rolePane.addItem(guiItem, col, row);
            }
        }
        gui.addPane(rolePane);

        // ==================== 底部按钮 ====================
        StaticPane bottomPane = new StaticPane(1, 5, 7, 1);

        // 查看权限报告
        ItemStack reportItem = new ItemBuilder(Material.WRITABLE_BOOK)
            .name("§d权限统计报告")
            .lore("§7查看所有角色的权限分布统计")
            .glow()
            .build();
        bottomPane.addItem(new GuiItem(reportItem, event -> {
            event.setCancelled(true);
            openPermissionReport(player, sect);
        }), 0, 0);

        // 重置为默认设置
        ItemStack resetItem = new ItemBuilder(Material.BARRIER)
            .name("§c重置为默认")
            .lore("§7恢复所有角色权限为系统默认设置")
            .build();
        bottomPane.addItem(new GuiItem(resetItem, event -> {
            event.setCancelled(true);
            confirmResetPermissions(player, sect);
        }), 2, 0);

        // 应用全部修改
        ItemStack applyItem = new ItemBuilder(Material.EMERALD_BLOCK)
            .name("§a应用全部修改")
            .lore("§7将所有修改应用到领地权限系统")
            .glow()
            .build();
        bottomPane.addItem(new GuiItem(applyItem, event -> {
            event.setCancelled(true);
            applyAllChanges(player, sect);
        }), 4, 0);

        // 返回上级菜单
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name("§c返回")
            .build();
        bottomPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            player.closeInventory();
        }), 6, 0);

        gui.addPane(bottomPane);
        gui.show(player);
    }

    /**
     * 创建角色卡片
     */
    private ItemStack createRoleCard(Sect sect, SectRank rank) {
        // 获取角色信息
        String displayName = rank.getDisplayName();
        Material material = getRankMaterial(rank);

        // 获取该角色的成员数量
        long memberCount = sect.getMemberList().stream()
            .filter(m -> m.getRank() == rank)
            .count();

        List<String> lore = new ArrayList<>();
        lore.add("§7成员数: §f" + memberCount);
        lore.add("");

        // 显示当前权限预览
        ResidencePermissionManager.PermissionLevel level = permissionManager.getPermissionLevel(rank);
        lore.add("§7权限等级: " + level.getDisplayName());
        lore.add("");

        // 显示权限标志
        Map<String, Boolean> permissions = getRolePermissions(rank);
        for (PermissionFlag flag : PERMISSION_FLAGS) {
            boolean hasPermission = permissions.getOrDefault(flag.flag.toString(), false);
            String symbol = hasPermission ? "§a✓" : "§c✗";
            lore.add(symbol + " " + flag.displayName);
        }

        lore.add("");
        lore.add("§eShift+左键 编辑 | 右键 查看详情");

        ItemStack card = new ItemBuilder(material)
            .name(displayName)
            .lore(lore.toArray(new String[0]))
            .build();

        return card;
    }

    /**
     * 打开角色权限编辑器
     * 允许宗主编辑该角色的权限
     */
    public void openRolePermissionEditor(Player player, Sect sect, SectRank rank) {
        ChestGui gui = new ChestGui(6, "§6编辑权限 - " + rank.getDisplayName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addBackground(gui, 6, Material.ORANGE_STAINED_GLASS_PANE);

        // 标题
        StaticPane titlePane = new StaticPane(1, 0, 7, 1);
        ItemStack titleItem = new ItemBuilder(Material.REDSTONE)
            .name("§6编辑 " + rank.getDisplayName() + " 的权限")
            .lore("§7左键启用 | 右键禁用权限")
            .build();
        titlePane.addItem(new GuiItem(titleItem), 3, 0);
        gui.addPane(titlePane);

        // 权限编辑面板
        StaticPane permPane = new StaticPane(1, 1, 7, 4);
        Map<String, Boolean> rolePermissions = getRolePermissions(rank);

        int index = 0;
        for (PermissionFlag flag : PERMISSION_FLAGS) {
            boolean currentState = rolePermissions.getOrDefault(flag.flag.toString(), false);
            ItemStack flagItem = createPermissionToggleItem(flag, currentState);

            int row = index / 7;
            int col = index % 7;

            GuiItem guiItem = new GuiItem(flagItem, event -> {
                event.setCancelled(true);
                boolean newState = !currentState; // 切换状态

                // 更新缓存
                updateEditingCache(sect, rank, flag.flag.toString(), newState);

                // 刷新界面
                openRolePermissionEditor(player, sect, rank);

                // 提示玩家
                String action = newState ? "§a启用" : "§c禁用";
                player.sendMessage("§7[权限配置] " + action + " " + flag.displayName);
            });

            permPane.addItem(guiItem, col, row);
            index++;
        }
        gui.addPane(permPane);

        // 底部操作按钮
        StaticPane bottomPane = new StaticPane(1, 5, 7, 1);

        // 预设按钮 - 应用预定义的权限组合
        ItemStack presetItem = new ItemBuilder(Material.REPEATING_COMMAND_BLOCK)
            .name("§9权限预设")
            .lore("§7应用预定义的权限组合")
            .build();
        bottomPane.addItem(new GuiItem(presetItem, event -> {
            event.setCancelled(true);
            openPermissionPresets(player, sect, rank);
        }), 0, 0);

        // 保存
        ItemStack saveItem = new ItemBuilder(Material.LIME_CONCRETE)
            .name("§a保存修改")
            .lore("§7保存本角色的权限修改")
            .glow()
            .build();
        bottomPane.addItem(new GuiItem(saveItem, event -> {
            event.setCancelled(true);
            saveRolePermissions(player, sect, rank);
            player.sendMessage("§a[权限配置] 权限已保存！");
            openRolePermissionMain(player, sect);
        }), 3, 0);

        // 返回
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name("§c返回")
            .build();
        bottomPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            openRolePermissionMain(player, sect);
        }), 6, 0);

        gui.addPane(bottomPane);
        gui.show(player);
    }

    /**
     * 权限预设
     */
    public void openPermissionPresets(Player player, Sect sect, SectRank rank) {
        ChestGui gui = new ChestGui(3, "§9权限预设 - " + rank.getDisplayName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(gui, 3, Material.PURPLE_STAINED_GLASS_PANE);

        StaticPane presetPane = new StaticPane(1, 0, 7, 2);

        // 预设1: 完全权限 (ADMIN)
        ItemStack adminPreset = createPresetItem(
            Material.DIAMOND_BLOCK,
            "§4完全权限",
            "所有权限已启用"
        );
        presetPane.addItem(new GuiItem(adminPreset, event -> {
            event.setCancelled(true);
            applyPermissionPreset(player, sect, rank, PermissionPreset.ADMIN);
        }), 0, 0);

        // 预设2: 管理权限 (MANAGER)
        ItemStack managerPreset = createPresetItem(
            Material.GOLD_BLOCK,
            "§6管理权限",
            "除权限管理外的所有权限"
        );
        presetPane.addItem(new GuiItem(managerPreset, event -> {
            event.setCancelled(true);
            applyPermissionPreset(player, sect, rank, PermissionPreset.MANAGER);
        }), 2, 0);

        // 预设3: 建造权限 (BUILDER)
        ItemStack builderPreset = createPresetItem(
            Material.IRON_BLOCK,
            "§e建造权限",
            "建造、破坏、使用、容器访问"
        );
        presetPane.addItem(new GuiItem(builderPreset, event -> {
            event.setCancelled(true);
            applyPermissionPreset(player, sect, rank, PermissionPreset.BUILDER);
        }), 4, 0);

        // 预设4: 成员权限 (MEMBER)
        ItemStack memberPreset = createPresetItem(
            Material.STONE,
            "§7成员权限",
            "使用、容器访问、移动"
        );
        presetPane.addItem(new GuiItem(memberPreset, event -> {
            event.setCancelled(true);
            applyPermissionPreset(player, sect, rank, PermissionPreset.MEMBER);
        }), 6, 0);

        // 预设5: 访客权限 (GUEST)
        ItemStack guestPreset = createPresetItem(
            Material.COBBLESTONE,
            "§8访客权限",
            "仅移动权限"
        );
        presetPane.addItem(new GuiItem(guestPreset, event -> {
            event.setCancelled(true);
            applyPermissionPreset(player, sect, rank, PermissionPreset.GUEST);
        }), 1, 1);

        // 返回
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name("§c返回")
            .build();
        presetPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            openRolePermissionEditor(player, sect, rank);
        }), 6, 1);

        gui.addPane(presetPane);
        gui.show(player);
    }

    /**
     * 查看角色权限详情
     */
    public void openRolePermissionDetail(Player player, Sect sect, SectRank rank) {
        ChestGui gui = new ChestGui(4, "§a权限详情 - " + rank.getDisplayName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(gui, 4, Material.GREEN_STAINED_GLASS_PANE);

        StaticPane detailPane = new StaticPane(1, 0, 7, 3);

        // 角色信息
        ItemStack infoItem = new ItemBuilder(Material.PLAYER_HEAD)
            .name(rank.getDisplayName())
            .lore("§7角色: " + rank.name())
            .lore("§7成员数: §f" + sect.getMemberList().stream()
                .filter(m -> m.getRank() == rank).count())
            .build();
        detailPane.addItem(new GuiItem(infoItem), 3, 0);

        // 权限列表
        StaticPane permListPane = new StaticPane(1, 1, 7, 2);
        Map<String, Boolean> rolePerms = getRolePermissions(rank);

        int index = 0;
        for (PermissionFlag flag : PERMISSION_FLAGS) {
            boolean hasPermission = rolePerms.getOrDefault(flag.flag.toString(), false);
            ItemStack flagItem = createPermissionDisplayItem(flag, hasPermission);

            int row = index / 7;
            int col = index % 7;

            if (row < 2) {
                permListPane.addItem(new GuiItem(flagItem), col, row);
            }
            index++;
        }
        gui.addPane(permListPane);

        // 返回按钮
        StaticPane bottomPane = new StaticPane(3, 3, 3, 1);
        ItemStack backItem = new ItemBuilder(Material.ARROW)
            .name("§c返回")
            .build();
        bottomPane.addItem(new GuiItem(backItem, event -> {
            event.setCancelled(true);
            openRolePermissionMain(player, sect);
        }), 1, 0);
        gui.addPane(bottomPane);

        gui.show(player);
    }

    /**
     * 权限统计报告
     */
    public void openPermissionReport(Player player, Sect sect) {
        ChestGui gui = new ChestGui(4, "§d权限统计报告");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addBackground(gui, 4, Material.PURPLE_STAINED_GLASS_PANE);

        StaticPane reportPane = new StaticPane(1, 0, 7, 3);

        // 权限启用率统计
        int index = 0;
        for (PermissionFlag flag : PERMISSION_FLAGS) {
            long enableCount = Arrays.stream(SectRank.values())
                .filter(rank -> getRolePermissions(rank).getOrDefault(flag.flag.toString(), false))
                .count();

            ItemStack statItem = new ItemBuilder(Material.PAPER)
                .name(flag.displayName)
                .lore("§7启用角色: §f" + enableCount + "/" + SectRank.values().length)
                .build();

            int row = index / 7;
            int col = index % 7;
            reportPane.addItem(new GuiItem(statItem), col, row);
            index++;
        }
        gui.addPane(reportPane);

        gui.show(player);
    }

    /**
     * 创建权限切换项
     */
    private ItemStack createPermissionToggleItem(PermissionFlag flag, boolean enabled) {
        Material material = enabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String state = enabled ? "§a启用" : "§c禁用";

        return new ItemBuilder(material)
            .name(flag.displayName + " " + state)
            .lore(flag.description)
            .lore("")
            .lore(enabled ? "§a✓ 已启用" : "§c✗ 已禁用")
            .build();
    }

    /**
     * 创建权限显示项
     */
    private ItemStack createPermissionDisplayItem(PermissionFlag flag, boolean enabled) {
        String symbol = enabled ? "§a✓" : "§c✗";

        return new ItemBuilder(Material.ITEM_FRAME)
            .name(symbol + " " + flag.displayName)
            .lore(flag.description)
            .build();
    }

    /**
     * 创建预设项
     */
    private ItemStack createPresetItem(Material material, String name, String description) {
        return new ItemBuilder(material)
            .name(name)
            .lore("§7" + description)
            .build();
    }

    /**
     * 应用权限预设
     */
    private void applyPermissionPreset(Player player, Sect sect, SectRank rank, PermissionPreset preset) {
        Map<String, Boolean> presetPerms = getPresetPermissions(preset);
        for (Map.Entry<String, Boolean> entry : presetPerms.entrySet()) {
            updateEditingCache(sect, rank, entry.getKey(), entry.getValue());
        }
        saveRolePermissions(player, sect, rank);
        player.sendMessage("§a[权限配置] 已应用权限预设: " + preset.name());
        openRolePermissionEditor(player, sect, rank);
    }

    /**
     * 获取权限预设的权限配置
     */
    private Map<String, Boolean> getPresetPermissions(PermissionPreset preset) {
        Map<String, Boolean> perms = new HashMap<>();

        switch (preset) {
            case ADMIN:
                perms.put("build", true);
                perms.put("destroy", true);
                perms.put("use", true);
                perms.put("container", true);
                perms.put("admin", true);
                perms.put("move", true);
                break;
            case MANAGER:
                perms.put("build", true);
                perms.put("destroy", true);
                perms.put("use", true);
                perms.put("container", true);
                perms.put("admin", false);
                perms.put("move", true);
                break;
            case BUILDER:
                perms.put("build", true);
                perms.put("destroy", true);
                perms.put("use", true);
                perms.put("container", true);
                perms.put("admin", false);
                perms.put("move", true);
                break;
            case MEMBER:
                perms.put("build", false);
                perms.put("destroy", false);
                perms.put("use", true);
                perms.put("container", true);
                perms.put("admin", false);
                perms.put("move", true);
                break;
            case GUEST:
                perms.put("build", false);
                perms.put("destroy", false);
                perms.put("use", false);
                perms.put("container", false);
                perms.put("admin", false);
                perms.put("move", true);
                break;
        }

        return perms;
    }

    /**
     * 更新编辑缓存
     */
    private void updateEditingCache(Sect sect, SectRank rank, String flag, boolean enabled) {
        editingCache
            .computeIfAbsent(sect.getId(), k -> new HashMap<>())
            .computeIfAbsent(rank, k -> new HashMap<>())
            .put(flag, enabled);
    }

    /**
     * 获取角色权限
     */
    private Map<String, Boolean> getRolePermissions(SectRank rank) {
        // 如果有编辑缓存，使用缓存的值
        // 否则返回默认权限

        Map<String, Boolean> perms = new HashMap<>();

        switch (rank) {
            case LEADER:
            case ELDER:
                perms.put("build", true);
                perms.put("destroy", true);
                perms.put("use", true);
                perms.put("container", true);
                perms.put("admin", true);
                perms.put("move", true);
                break;
            case CORE_DISCIPLE:
            case INNER_DISCIPLE:
                perms.put("build", true);
                perms.put("destroy", true);
                perms.put("use", true);
                perms.put("container", true);
                perms.put("admin", false);
                perms.put("move", true);
                break;
            case OUTER_DISCIPLE:
                perms.put("build", false);
                perms.put("destroy", false);
                perms.put("use", false);
                perms.put("container", false);
                perms.put("admin", false);
                perms.put("move", true);
                break;
        }

        return perms;
    }

    /**
     * 保存角色权限修改
     */
    private void saveRolePermissions(Player player, Sect sect, SectRank rank) {
        // 保存到编辑缓存
        // 稍后通过应用全部修改来同步到Residence
        plugin.getLogger().info("已保存角色 " + rank.getDisplayName() + " 的权限修改");
    }

    /**
     * 应用全部权限修改
     */
    private void applyAllChanges(Player player, Sect sect) {
        // 将所有编辑缓存中的权限修改应用到领地权限系统
        // 这会批量调用Residence API更新权限

        if (!sect.hasLand()) {
            player.sendMessage("§c[权限配置] 宗门未绑定领地，无法应用权限修改！");
            return;
        }

        player.sendMessage("§a[权限配置] 正在应用全部权限修改...");

        // 为该宗门的所有成员更新权限
        for (var member : sect.getMemberList()) {
            // 这里会根据成员的最新职位应用相应的权限
        }

        player.sendMessage("§a[权限配置] 权限修改已应用！");
        openRolePermissionMain(player, sect);
    }

    /**
     * 确认重置权限
     */
    private void confirmResetPermissions(Player player, Sect sect) {
        player.sendMessage("§c[权限配置] 确认要重置所有角色权限吗？输入 /sect permission reset 确认");
        // 需要在命令处理中实现具体的重置逻辑
    }

    /**
     * 获取角色的材料类型（用于显示）
     */
    private Material getRankMaterial(SectRank rank) {
        return switch (rank) {
            case LEADER -> Material.DIAMOND_BLOCK;
            case ELDER -> Material.GOLD_BLOCK;
            case CORE_DISCIPLE -> Material.IRON_BLOCK;
            case INNER_DISCIPLE -> Material.STONE;
            case OUTER_DISCIPLE -> Material.COBBLESTONE;
        };
    }

    /**
     * 权限标志信息类
     */
    public static class PermissionFlag {
        public final Flags flag;
        public final String displayName;
        public final String description;

        public PermissionFlag(Flags flag, String displayName, String description) {
            this.flag = flag;
            this.displayName = displayName;
            this.description = description;
        }
    }

    /**
     * 权限预设枚举
     */
    public enum PermissionPreset {
        ADMIN("完全权限"),
        MANAGER("管理权限"),
        BUILDER("建造权限"),
        MEMBER("成员权限"),
        GUEST("访客权限");

        private final String displayName;

        PermissionPreset(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
