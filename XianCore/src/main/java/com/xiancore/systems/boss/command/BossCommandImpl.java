package com.xiancore.systems.boss.command;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.config.BossConfigLoader;
import com.xiancore.systems.boss.config.BossRefreshConfig;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import com.xiancore.systems.boss.gui.BossAdminGUI;
import com.xiancore.systems.boss.permission.BossPermissions;
import com.xiancore.systems.boss.permission.PermissionChecker;
import com.xiancore.systems.boss.teleport.BossTeleportManager;
import com.xiancore.systems.boss.teleport.BossTeleportManager.TeleportResult;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

/**
 * Boss主命令实现
 * 处理所有 /boss 命令
 *
 * 支持的命令:
 * - /boss help - 显示帮助
 * - /boss list - 列出所有刷新点
 * - /boss info <id> - 查看刷新点详情
 * - /boss stats - 查看系统统计
 * - /boss add <id> <location> <mob-type> - 添加刷新点
 * - /boss remove <id> - 删除刷新点
 * - /boss edit <id> <参数> <值> - 编辑刷新点
 * - /boss enable <id> - 启用刷新点
 * - /boss disable <id> - 禁用刷新点
 * - /boss spawn <id> - 手动生成Boss
 * - /boss reload - 重载配置文件
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-15
 */
public class BossCommandImpl extends BossCommand {

    private final BossRefreshManager bossManager;
    private final BossConfigLoader configLoader;
    private final BossAdminGUI adminGUI;

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param permissionChecker 权限检查器
     * @param bossManager Boss管理器
     * @param configLoader 配置加载器
     * @param adminGUI 管理GUI
     */
    public BossCommandImpl(XianCore plugin, PermissionChecker permissionChecker,
                          BossRefreshManager bossManager, BossConfigLoader configLoader,
                          BossAdminGUI adminGUI) {
        super(plugin, permissionChecker);
        this.bossManager = bossManager;
        this.configLoader = configLoader;
        this.adminGUI = adminGUI;
    }

    // ==================== 命令处理 ====================

    @Override
    protected boolean handleHelp(CommandSender sender) {
        sendTitle(sender, "Boss系统命令帮助");

        sender.sendMessage("§b查看类命令:");
        sender.sendMessage("  §7/boss list §f- 列出所有Boss刷新点");
        sender.sendMessage("  §7/boss info <id> §f- 查看刷新点详情");
        sender.sendMessage("  §7/boss stats §f- 查看系统统计信息");

        if (permissionChecker.hasPermission(sender, BossPermissions.COMMAND_ADD)) {
            sender.sendMessage("§b管理类命令:");
            sender.sendMessage("  §7/boss add <id> <location> <mob> §f- 添加刷新点");
            sender.sendMessage("  §7/boss remove <id> §f- 删除刷新点");
            sender.sendMessage("  §7/boss edit <id> <参数> <值> §f- 编辑刷新点");
            sender.sendMessage("  §7/boss enable <id> §f- 启用刷新点");
            sender.sendMessage("  §7/boss disable <id> §f- 禁用刷新点");
            sender.sendMessage("  §7/boss spawn <id> §f- 手动生成Boss");
        }

        sender.sendMessage("§b玩家类命令:");
        sender.sendMessage("  §7/boss tp <id> §f- 传送到Boss位置");

        if (permissionChecker.hasPermission(sender, BossPermissions.RELOAD)) {
            sender.sendMessage("§b系统类命令:");
            sender.sendMessage("  §7/boss reload §f- 重载配置文件");
        }

        if (sender.isOp() || sender.hasPermission("boss.admin")) {
            sender.sendMessage("§bGUI管理命令:");
            sender.sendMessage("  §7/boss gui §f- 打开管理界面 §e⭐ 推荐");
            sender.sendMessage("§b权限管理命令:");
            sender.sendMessage("  §7/boss perm list <player> §f- 查看玩家权限");
            sender.sendMessage("  §7/boss perm check <player> <perm> §f- 检查特定权限");
            sender.sendMessage("  §7/boss perm grant <player> <perm> §f- 授予权限");
            sender.sendMessage("  §7/boss perm revoke <player> <perm> §f- 撤销权限");
            sender.sendMessage("  §7/boss perm reload §f- 重载权限缓存");
        }

        sendSeparator(sender);
        sender.sendMessage("§7使用 §b/boss help <命令> §7获取更多帮助");
        return true;
    }

    @Override
    protected boolean handleList(CommandSender sender, String[] args) {
        if (!permissionChecker.hasPermission(sender, BossPermissions.COMMAND_LIST)) {
            return permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_LIST);
        }

        List<BossSpawnPoint> spawnPoints = bossManager.getAllSpawnPoints();

        if (spawnPoints.isEmpty()) {
            sendInfo(sender, "当前没有配置任何Boss刷新点");
            return true;
        }

        sendTitle(sender, "Boss刷新点列表 (" + spawnPoints.size() + ")");

        for (BossSpawnPoint point : spawnPoints) {
            String enabled = point.isEnabled() ? "§a启用" : "§c禁用";
            String status = point.getCurrentCount() > 0 ? "§e生成中" : "§7待命";

            sender.sendMessage(String.format(
                "  §b%s §7| 位置: §f%s §7| Boss: §f%s §7| 等级: §f%d §7| %s §7| %s",
                point.getId(),
                point.getLocationString(),
                point.getMythicMobId(),
                point.getTier(),
                enabled,
                status
            ));
        }

        sendSeparator(sender);
        sender.sendMessage("§7使用 §b/boss info <id> §7查看详细信息");
        return true;
    }

    @Override
    protected boolean handleInfo(CommandSender sender, String[] args) {
        if (!permissionChecker.hasPermission(sender, BossPermissions.COMMAND_INFO)) {
            return permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_INFO);
        }

        if (args.length < 2) {
            sendUsage(sender, "/boss info <id>");
            return false;
        }

        String id = args[1];
        BossSpawnPoint point = bossManager.getSpawnPoint(id);

        if (point == null) {
            sendError(sender, "找不到ID为 '" + id + "' 的刷新点");
            return false;
        }

        sendTitle(sender, "刷新点详情 - " + id);
        sender.sendMessage(point.getDetailedInfo());
        sendSeparator(sender);
        return true;
    }

    @Override
    protected boolean handleStats(CommandSender sender, String[] args) {
        if (!permissionChecker.hasPermission(sender, BossPermissions.COMMAND_STATS)) {
            return permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_STATS);
        }

        sendTitle(sender, "系统统计信息");

        sender.sendMessage(String.format("§7总刷新点数: §b%d", bossManager.getAllSpawnPoints().size()));
        sender.sendMessage(String.format("§7启用的刷新点: §b%d", bossManager.getEnabledPoints().size()));
        sender.sendMessage(String.format("§7当前活跃Boss数: §b%d", bossManager.getActiveBosses().size()));
        sender.sendMessage(String.format("§7总生成Boss数: §b%d", bossManager.getTotalBossesSpawned()));
        sender.sendMessage(String.format("§7已击杀Boss数: §b%d", bossManager.getTotalBossesKilled()));
        sender.sendMessage(String.format("§7系统运行时间: §b正常"));

        sendSeparator(sender);
        return true;
    }

    @Override
    protected boolean handleAdd(CommandSender sender, String[] args) {
        if (!permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_ADD)) {
            return false;
        }

        if (args.length < 4) {
            sendUsage(sender, "/boss add <id> <location> <mob-type>");
            sender.sendMessage("§7例: /boss add dragon_lair world,100,64,200 EnderDragon");
            sender.sendMessage("§7location格式: world,x,y,z");
            return false;
        }

        String id = args[1];
        String location = args[2];
        String mobType = args[3];

        try {
            // 1. 验证ID是否已存在
            if (bossManager.getSpawnPoint(id) != null) {
                sendError(sender, "刷新点ID '" + id + "' 已存在");
                return false;
            }

            // 2. 验证位置格式
            String[] coords = location.split(",");
            if (coords.length != 4) {
                sendError(sender, "位置格式错误: 应为 world,x,y,z");
                return false;
            }

            String world = coords[0].trim();
            int x, y, z;
            try {
                x = Integer.parseInt(coords[1].trim());
                y = Integer.parseInt(coords[2].trim());
                z = Integer.parseInt(coords[3].trim());
            } catch (NumberFormatException e) {
                sendError(sender, "坐标必须是整数");
                return false;
            }

            // 3. 验证MythicMobs ID格式
            if (!mobType.matches("[a-zA-Z0-9_]+")) {
                sendError(sender, "Boss ID 只能包含字母、数字和下划线");
                return false;
            }

            // 4. 创建新的刷新点
            BossSpawnPoint point = new BossSpawnPoint(id, world, x, y, z, mobType);

            // 5. 添加到管理器
            // 注意: 需要使用正确的API
            sendError(sender, "此功能需要通过配置文件或重新加载来实现");
            sendInfo(sender, "请使用GUI界面 (/boss gui) 或编辑配置文件");
            return false;

        } catch (Exception e) {
            sendError(sender, "添加刷新点失败: " + e.getMessage());
            logger.severe("添加刷新点异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean handleRemove(CommandSender sender, String[] args) {
        if (!permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_REMOVE)) {
            return false;
        }

        if (args.length < 2) {
            sendUsage(sender, "/boss remove <id>");
            return false;
        }

        String id = args[1];

        try {
            // 1. 查找刷新点
            BossSpawnPoint point = bossManager.getSpawnPoint(id);
            if (point == null) {
                sendError(sender, "找不到ID为 '" + id + "' 的刷新点");
                return false;
            }

            // 2. 检查是否有活跃的Boss
            if (point.getCurrentCount() > 0) {
                sendError(sender, "刷新点上还有 " + point.getCurrentCount() + " 个活跃的Boss，无法删除");
                sender.sendMessage("§7请等待这些Boss被击杀后再删除，或使用 /boss disable 禁用该刷新点");
                return false;
            }

            // 3. 删除刷新点
            // 注意: 需要使用正确的API
            sendError(sender, "此功能需要通过配置文件来实现");
            sendInfo(sender, "请直接编辑配置文件并使用 /boss reload 重载");
            return false;

        } catch (Exception e) {
            sendError(sender, "删除刷新点失败: " + e.getMessage());
            logger.severe("删除刷新点异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean handleEdit(CommandSender sender, String[] args) {
        if (!permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_EDIT)) {
            return false;
        }

        if (args.length < 4) {
            sendUsage(sender, "/boss edit <id> <参数> <值>");
            sender.sendMessage("§7可编辑参数: tier, cooldown, max-count, location");
            sender.sendMessage("§7例子:");
            sender.sendMessage("  §7/boss edit dragon_lair tier 3");
            sender.sendMessage("  §7/boss edit dragon_lair cooldown 7200");
            return false;
        }

        String id = args[1];
        String parameter = args[2].toLowerCase();
        String value = args[3];

        try {
            // 1. 查找刷新点
            BossSpawnPoint point = bossManager.getSpawnPoint(id);
            if (point == null) {
                sendError(sender, "找不到ID为 '" + id + "' 的刷新点");
                return false;
            }

            // 2. 编辑对应参数
            switch (parameter) {
                case "tier" -> {
                    try {
                        int tier = Integer.parseInt(value);
                        if (tier < 1 || tier > 4) {
                            sendError(sender, "Boss等级必须在 1-4 之间");
                            return false;
                        }
                        point.setTier(tier);
                        sendSuccess(sender, "已更新 " + id + " 的等级为: " + tier);
                    } catch (NumberFormatException e) {
                        sendError(sender, "等级必须是整数");
                        return false;
                    }
                }

                case "cooldown" -> {
                    try {
                        long cooldown = Long.parseLong(value);
                        if (cooldown < 60) {
                            sendError(sender, "冷却时间必须至少60秒");
                            return false;
                        }
                        point.setCooldownSeconds(cooldown);
                        sendSuccess(sender, "已更新 " + id + " 的冷却时间为: " + cooldown + "秒");
                    } catch (NumberFormatException e) {
                        sendError(sender, "冷却时间必须是整数");
                        return false;
                    }
                }

                case "max-count" -> {
                    try {
                        int maxCount = Integer.parseInt(value);
                        if (maxCount < 1) {
                            sendError(sender, "最大数量必须至少为1");
                            return false;
                        }
                        point.setMaxCount(maxCount);
                        sendSuccess(sender, "已更新 " + id + " 的最大数量为: " + maxCount);
                    } catch (NumberFormatException e) {
                        sendError(sender, "最大数量必须是整数");
                        return false;
                    }
                }

                case "location" -> {
                    if (args.length < 4) {
                        sendUsage(sender, "/boss edit " + id + " location world,x,y,z");
                        return false;
                    }
                    String location = args[3];
                    String[] coords = location.split(",");
                    if (coords.length != 4) {
                        sendError(sender, "位置格式错误: 应为 world,x,y,z");
                        return false;
                    }
                    try {
                        String world = coords[0].trim();
                        int x = Integer.parseInt(coords[1].trim());
                        int y = Integer.parseInt(coords[2].trim());
                        int z = Integer.parseInt(coords[3].trim());
                        point.setWorld(world);
                        point.setX(x);
                        point.setY(y);
                        point.setZ(z);
                        sendSuccess(sender, "已更新 " + id + " 的位置为: " + location);
                    } catch (NumberFormatException e) {
                        sendError(sender, "坐标必须是整数");
                        return false;
                    }
                }

                default -> {
                    sendError(sender, "未知参数: " + parameter);
                    sender.sendMessage("§7可用参数: tier, cooldown, max-count, location");
                    return false;
                }
            }

            // 记录日志
            if (sender instanceof Player player) {
                logger.info(String.format("玩家 %s 编辑了刷新点 %s 的参数 %s = %s",
                    player.getName(), id, parameter, value));
            }

            // 保存配置
            if (bossManager.saveCurrentConfig()) {
                sendInfo(sender, "配置已自动保存");
            } else {
                sendError(sender, "警告: 配置保存失败，请手动重载");
            }

            return true;

        } catch (Exception e) {
            sendError(sender, "编辑刷新点失败: " + e.getMessage());
            logger.severe("编辑刷新点异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean handleEnable(CommandSender sender, String[] args) {
        if (!permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_ENABLE)) {
            return false;
        }

        if (args.length < 2) {
            sendUsage(sender, "/boss enable <id>");
            return false;
        }

        String id = args[1];
        BossSpawnPoint point = bossManager.getSpawnPoint(id);

        if (point == null) {
            sendError(sender, "找不到ID为 '" + id + "' 的刷新点");
            return false;
        }

        if (point.isEnabled()) {
            sendInfo(sender, "刷新点 '" + id + "' 已经是启用状态");
            return true;
        }

        point.setEnabled(true);
        sendSuccess(sender, "已启用刷新点: " + id);

        // 保存配置
        if (bossManager.saveCurrentConfig()) {
            sendInfo(sender, "配置已自动保存");
        } else {
            sendError(sender, "警告: 配置保存失败，请手动重载");
        }

        // 记录日志
        if (sender instanceof Player player) {
            logger.info(String.format("玩家 %s 启用了刷新点: %s", player.getName(), id));
        }

        return true;
    }

    @Override
    protected boolean handleDisable(CommandSender sender, String[] args) {
        if (!permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_ENABLE)) {
            return false;
        }

        if (args.length < 2) {
            sendUsage(sender, "/boss disable <id>");
            return false;
        }

        String id = args[1];
        BossSpawnPoint point = bossManager.getSpawnPoint(id);

        if (point == null) {
            sendError(sender, "找不到ID为 '" + id + "' 的刷新点");
            return false;
        }

        if (!point.isEnabled()) {
            sendInfo(sender, "刷新点 '" + id + "' 已经是禁用状态");
            return true;
        }

        point.setEnabled(false);
        sendSuccess(sender, "已禁用刷新点: " + id);

        // 保存配置
        if (bossManager.saveCurrentConfig()) {
            sendInfo(sender, "配置已自动保存");
        } else {
            sendError(sender, "警告: 配置保存失败，请手动重载");
        }

        // 记录日志
        if (sender instanceof Player player) {
            logger.info(String.format("玩家 %s 禁用了刷新点: %s", player.getName(), id));
        }

        return true;
    }

    @Override
    protected boolean handleSpawn(CommandSender sender, String[] args) {
        if (!permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.COMMAND_SPAWN)) {
            return false;
        }

        if (args.length < 2) {
            sendUsage(sender, "/boss spawn <id>");
            return false;
        }

        String id = args[1];

        try {
            // 1. 查找刷新点
            BossSpawnPoint point = bossManager.getSpawnPoint(id);
            if (point == null) {
                sendError(sender, "找不到ID为 '" + id + "' 的刷新点");
                return false;
            }

            // 2. 检查刷新点是否启用
            if (!point.isEnabled()) {
                sendError(sender, "刷新点已被禁用，无法生成Boss");
                return false;
            }

            // 3. 检查当前活跃Boss数
            if (point.getCurrentCount() >= point.getMaxCount()) {
                sendError(sender, "该刷新点已有 " + point.getCurrentCount() + " 个活跃的Boss");
                return false;
            }

            // 4. 尝试生成Boss
            Location spawnLocation = point.getLocation();
            if (spawnLocation == null) {
                sendError(sender, "刷新点的位置无效（世界可能不存在）");
                return false;
            }

            String mobType = point.getMythicMobId();
            if (mobType == null || mobType.isEmpty()) {
                sendError(sender, "刷新点未配置Boss类型");
                return false;
            }

            // 5. 通过MossRefreshManager生成Boss
            try {
                bossManager.spawnBossAtLocation(spawnLocation, mobType, point.getTier());
                point.recordSpawn(java.util.UUID.randomUUID());

                sendSuccess(sender, "Boss生成成功: " + mobType);
                sendInfo(sender, "刷新点: " + id + ", 等级: " + point.getTier());

                // 记录日志
                if (sender instanceof Player player) {
                    logger.info(String.format("玩家 %s 手动生成了Boss: %s (%s)", player.getName(), id, mobType));
                }

                return true;
            } catch (Exception e) {
                sendError(sender, "Boss生成失败: " + e.getMessage());
                logger.severe("Boss生成异常: " + e.getMessage());
                e.printStackTrace();
                return false;
            }

        } catch (Exception e) {
            sendError(sender, "手动刷新Boss失败: " + e.getMessage());
            logger.severe("手动刷新Boss异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean handleReload(CommandSender sender, String[] args) {
        if (!permissionChecker.checkPermissionOrSendMessage(sender, BossPermissions.RELOAD)) {
            return false;
        }

        try {
            sendInfo(sender, "正在重载Boss配置...");

            // 重新加载配置
            File configFile = new File(plugin.getDataFolder(), "boss-refresh.yml");
            BossRefreshConfig config = configLoader.loadConfig(configFile);

            // 重新初始化Boss管理器
            bossManager.disable();
            bossManager.initialize();
            bossManager.enable();

            sendSuccess(sender, "Boss配置已重载");

            // 记录日志
            if (sender instanceof Player player) {
                logger.info(String.format("玩家 %s 重载了Boss配置", player.getName()));
            } else {
                logger.info("Boss配置已重载 (由控制台执行)");
            }

            return true;
        } catch (Exception e) {
            sendError(sender, "重载配置失败: " + e.getMessage());
            logger.severe("重载Boss配置时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected boolean handleTeleport(CommandSender sender, String[] args) {
        // 只有玩家可以传送
        if (!(sender instanceof Player)) {
            sendError(sender, "只有玩家可以使用传送功能");
            return true;
        }

        Player player = (Player) sender;

        // 检查参数
        if (args.length < 2) {
            sendError(sender, "用法: /boss tp <刷新点ID>");
            return false;
        }

        String pointId = args[1];
        BossSpawnPoint point = bossManager.getSpawnPoint(pointId);

        if (point == null) {
            sendError(sender, "刷新点不存在: " + pointId);
            return true;
        }

        // 获取传送管理器
        BossTeleportManager teleportManager = bossManager.getTeleportManager();
        if (teleportManager == null) {
            sendError(sender, "传送系统未启用");
            return true;
        }

        // 执行传送
        sendInfo(sender, "正在传送到 " + pointId + "...");
        
        TeleportResult result = teleportManager.teleportPlayer(player, pointId);

        // 处理结果
        if (result.isSuccess()) {
            sendSuccess(sender, "传送成功！");
            logger.info(String.format("玩家 %s 传送到刷新点 %s", player.getName(), pointId));
        } else {
            sendError(sender, "传送失败: " + result.description);
            logger.warning(String.format("玩家 %s 传送到刷新点 %s 失败: %s", 
                player.getName(), pointId, result.description));
        }

        return true;
    }

    // ==================== 自动补全 ====================

    @Override
    protected List<String> getSpawnPointNames() {
        return bossManager.getEnabledPoints();
    }

    @Override
    protected boolean handlePermission(CommandSender sender, String[] args) {
        // 只有管理员可以使用权限命令
        if (!sender.isOp() && !sender.hasPermission("boss.admin")) {
            sendError(sender, "你没有权限使用此命令");
            return false;
        }

        if (args.length < 2) {
            sendUsage(sender, "/boss perm <list|check|grant|revoke|reload>");
            return false;
        }

        String subCmd = args[1].toLowerCase();

        return switch (subCmd) {
            case "list" -> handlePermList(sender, args);
            case "check" -> handlePermCheck(sender, args);
            case "grant" -> handlePermGrant(sender, args);
            case "revoke" -> handlePermRevoke(sender, args);
            case "reload" -> handlePermReload(sender, args);
            default -> {
                sendError(sender, "未知的权限子命令: " + subCmd);
                sendUsage(sender, "/boss perm <list|check|grant|revoke|reload>");
                yield false;
            }
        };
    }

    /**
     * 列出玩家的权限
     * /boss perm list <player>
     */
    private boolean handlePermList(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendUsage(sender, "/boss perm list <player>");
            return false;
        }

        String playerName = args[2];
        Player target = plugin.getServer().getPlayer(playerName);

        if (target == null) {
            sendError(sender, "玩家 " + playerName + " 不在线");
            return false;
        }

        sendTitle(sender, "玩家 " + playerName + " 的Boss权限");

        // 检查所有Boss权限
        boolean hasAny = false;
        sender.sendMessage("§a拥有的权限:");
        for (com.xiancore.systems.boss.permission.BossPermission perm : 
             com.xiancore.systems.boss.permission.BossPermission.values()) {
            if (target.hasPermission(perm.getNode())) {
                sender.sendMessage("  §2✓ §7" + perm.getNode() + " §f- " + perm.getDescription());
                hasAny = true;
            }
        }

        if (!hasAny) {
            sender.sendMessage("  §7(无)");
        }

        sendSeparator(sender);
        return true;
    }

    /**
     * 检查玩家的特定权限
     * /boss perm check <player> <permission>
     */
    private boolean handlePermCheck(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendUsage(sender, "/boss perm check <player> <permission>");
            return false;
        }

        String playerName = args[2];
        String permNode = args[3];

        Player target = plugin.getServer().getPlayer(playerName);
        if (target == null) {
            sendError(sender, "玩家 " + playerName + " 不在线");
            return false;
        }

        boolean hasPerm = target.hasPermission(permNode);
        
        if (hasPerm) {
            sendSuccess(sender, playerName + " 拥有权限: " + permNode);
        } else {
            sendError(sender, playerName + " 没有权限: " + permNode);
        }

        return true;
    }

    /**
     * 授予玩家权限（需要权限插件支持）
     * /boss perm grant <player> <permission>
     */
    private boolean handlePermGrant(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendUsage(sender, "/boss perm grant <player> <permission>");
            return false;
        }

        sendError(sender, "此功能需要LuckPerms等权限插件支持");
        sendInfo(sender, "请使用权限插件的命令来授予权限:");
        sendInfo(sender, "  /lp user <player> permission set " + args[3]);
        
        return true;
    }

    /**
     * 撤销玩家权限（需要权限插件支持）
     * /boss perm revoke <player> <permission>
     */
    private boolean handlePermRevoke(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendUsage(sender, "/boss perm revoke <player> <permission>");
            return false;
        }

        sendError(sender, "此功能需要LuckPerms等权限插件支持");
        sendInfo(sender, "请使用权限插件的命令来撤销权限:");
        sendInfo(sender, "  /lp user <player> permission unset " + args[3]);
        
        return true;
    }

    /**
     * 重载权限缓存
     * /boss perm reload
     */
    private boolean handlePermReload(CommandSender sender, String[] args) {
        sendSuccess(sender, "Boss权限系统已重载");
        sendInfo(sender, "注意: 实际权限由权限插件管理");
        return true;
    }

    @Override
    protected boolean handleGUI(CommandSender sender, String[] args) {
        // 只有玩家可以打开GUI
        if (!(sender instanceof Player)) {
            sendError(sender, "只有玩家可以打开GUI界面");
            return false;
        }

        Player player = (Player) sender;

        // 权限检查
        if (!player.isOp() && !player.hasPermission("boss.admin")) {
            sendError(sender, "你没有权限使用管理界面");
            return false;
        }

        // 打开管理GUI
        adminGUI.openMainMenu(player);
        sendSuccess(sender, "已打开Boss管理界面");
        return true;
    }
}
