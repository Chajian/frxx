package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.cultivation.QiRewardCalculator;
import com.xiancore.systems.cultivation.RealmParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 修为奖励系统调试命令
 * 用于测试和管理基于境界的修为奖励系统
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class QiRewardDebugCommand extends BaseCommand {

    private final QiRewardCalculator rewardCalculator;

    public QiRewardDebugCommand(XianCore plugin) {
        super(plugin);
        this.rewardCalculator = new QiRewardCalculator(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "xiancore.admin")) {
            return;
        }

        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "test":
                handleTest(sender, args);
                break;
            case "simulate":
                handleSimulate(sender, args);
                break;
            case "realm":
                handleRealmTest(sender, args);
                break;
            case "target":
                handleTargetTest(sender);
                break;
            case "config":
                handleConfig(sender, args);
                break;
            case "stats":
                handleStats(sender, args);
                break;
            case "help":
            default:
                showHelp(sender);
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(Arrays.asList("reload", "test", "simulate", "realm", "target", "config", "stats", "help"), args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "test", "simulate", "stats":
                    return filterTabComplete(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
                case "realm":
                    return filterTabComplete(Arrays.asList(RealmParser.getAllRealms()), args[1]);
                case "config":
                    return filterTabComplete(Arrays.asList("show", "set"), args[1]);
            }
        }
        if (args.length == 3) {
            if ("simulate".equals(args[0].toLowerCase())) {
                return filterTabComplete(Arrays.asList(RealmParser.getAllRealms()), args[2]);
            }
        }
        return List.of();
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== 修为奖励调试命令帮助 ==========");
        sendInfo(sender, "§e/qireward reload §7- 重载修为奖励配置文件");
        sendInfo(sender, "§e/qireward test [玩家] §7- 测试玩家击杀目标怪物的修为计算");
        sendInfo(sender, "§e/qireward simulate <玩家> <怪物境界> [Boss|Elite] §7- 模拟击杀计算");
        sendInfo(sender, "§e/qireward realm <境界> §7- 测试境界解析功能");
        sendInfo(sender, "§e/qireward target §7- 分析你看向的实体境界信息");
        sendInfo(sender, "§e/qireward config show §7- 显示当前配置摘要");
        sendInfo(sender, "§e/qireward stats [玩家] §7- 查看玩家今日击杀统计");
        sendInfo(sender, "§b=========================================");
    }

    /**
     * 重载配置文件
     */
    private void handleReload(CommandSender sender) {
        try {
            rewardCalculator.loadConfig();
            plugin.getConfigManager().reloadConfig("cultivation_rewards");
            sendSuccess(sender, "修为奖励配置已重载！");
        } catch (Exception e) {
            sendError(sender, "重载配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试玩家击杀目标的修为计算
     */
    private void handleTest(CommandSender sender, String[] args) {
        Player targetPlayer = getTargetPlayer(sender, args, 1);
        if (targetPlayer == null) {
            return;
        }

        if (!(sender instanceof Player player)) {
            sendError(sender, "此命令只能由玩家执行！");
            return;
        }

        // 获取玩家视线中的实体
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            20.0,
            entity -> entity instanceof LivingEntity && entity != player
        );

        if (rayTrace == null || !(rayTrace.getHitEntity() instanceof LivingEntity target)) {
            sendError(sender, "请看向一个生物实体！");
            return;
        }

        // 计算修为奖励
        QiRewardCalculator.QiRewardResult result = rewardCalculator.calculateQiReward(targetPlayer, target);

        // 显示结果
        sendInfo(sender, "§b========== 修为奖励计算测试 ==========");
        sendInfo(sender, "§e测试玩家: §f" + targetPlayer.getName());
        sendInfo(sender, "§e目标实体: §f" + getEntityDisplayName(target));
        sendInfo(sender, "§e实体类型: §f" + target.getType().name());
        
        PlayerData playerData = plugin.getDataManager().loadPlayerData(targetPlayer.getUniqueId());
        if (playerData != null) {
            sendInfo(sender, "§e玩家境界: §f" + playerData.getRealm());
        }

        String mobRealm = RealmParser.parseEntityRealm(target);
        sendInfo(sender, "§e怪物境界: §f" + mobRealm);

        if (result.isSuccess()) {
            sendSuccess(sender, "§a获得修为: §f" + result.getQiAmount());
            if (result.getExtraInfo() != null && !result.getExtraInfo().isEmpty()) {
                sendInfo(sender, "§e加成信息: §f" + result.getExtraInfo());
            }
            if (result.getBaseReward() > 0) {
                sendInfo(sender, "§e基础奖励: §f" + result.getBaseReward());
                sendInfo(sender, "§e总乘数: §f" + String.format("%.2f", result.getTotalMultiplier()));
            }
        } else {
            sendWarning(sender, "§c获得失败: §f" + result.getReason());
            if (result.getExtraInfo() != null) {
                sendInfo(sender, "§e额外信息: §f" + result.getExtraInfo());
            }
            if (result.getRemainingCooldown() != null) {
                sendInfo(sender, "§e剩余冷却: §f" + result.getRemainingCooldown() + "秒");
            }
        }
        sendInfo(sender, "§b====================================");
    }

    /**
     * 模拟击杀计算
     */
    private void handleSimulate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "用法: /qireward simulate <玩家> <怪物境界> [Boss|Elite]");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sendError(sender, "玩家不在线或不存在！");
            return;
        }

        String mobRealm = args[2];
        boolean isBoss = args.length > 3 && args[3].equalsIgnoreCase("Boss");
        boolean isElite = args.length > 3 && args[3].equalsIgnoreCase("Elite");

        // 创建模拟实体数据
        PlayerData playerData = plugin.getDataManager().loadPlayerData(targetPlayer.getUniqueId());
        if (playerData == null) {
            sendError(sender, "无法加载玩家数据！");
            return;
        }

        sendInfo(sender, "§b========== 模拟击杀计算 ==========");
        sendInfo(sender, "§e玩家: §f" + targetPlayer.getName() + " §7(§f" + playerData.getRealm() + "§7)");
        sendInfo(sender, "§e怪物境界: §f" + mobRealm);
        sendInfo(sender, "§e怪物类型: §f" + (isBoss ? "Boss" : isElite ? "Elite" : "普通"));

        // 计算基础奖励
        long baseReward = plugin.getConfigManager().getConfig("cultivation_rewards")
                .getLong("base-rewards." + mobRealm, 0);

        if (baseReward <= 0) {
            sendError(sender, "未找到境界 " + mobRealm + " 的基础奖励配置！");
            return;
        }

        // 计算境界差距乘数
        int realmGap = RealmParser.calculateRealmGap(playerData.getRealm(), mobRealm);
        double realmMultiplier = calculateRealmMultiplier(realmGap);

        // 计算特殊乘数
        double specialMultiplier = 1.0;
        if (isBoss) specialMultiplier *= 3.0;
        if (isElite) specialMultiplier *= 2.0;

        long finalReward = Math.round(baseReward * realmMultiplier * specialMultiplier);

        sendInfo(sender, "§e基础奖励: §f" + baseReward);
        sendInfo(sender, "§e境界差距: §f" + realmGap + " §7(乘数: §f" + String.format("%.2f", realmMultiplier) + "§7)");
        sendInfo(sender, "§e特殊乘数: §f" + String.format("%.2f", specialMultiplier));
        sendInfo(sender, "§e计算过程: §f" + baseReward + " × " + String.format("%.2f", realmMultiplier) + " × " + String.format("%.2f", specialMultiplier));
        sendSuccess(sender, "§a最终奖励: §f" + finalReward + " 修为");
        sendInfo(sender, "§b==============================");
    }

    /**
     * 测试境界解析
     */
    private void handleRealmTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "用法: /qireward realm <境界名称>");
            return;
        }

        String testRealm = args[1];
        
        sendInfo(sender, "§b========== 境界解析测试 ==========");
        sendInfo(sender, "§e测试境界: §f" + testRealm);
        
        // 测试境界索引
        int realmIndex = RealmParser.getRealmIndex(testRealm);
        sendInfo(sender, "§e境界索引: §f" + (realmIndex >= 0 ? realmIndex : "未找到"));
        
        if (realmIndex >= 0) {
            sendInfo(sender, "§e境界有效: §a是");
            
            // 显示与其他境界的差距
            sendInfo(sender, "§e与其他境界差距:");
            for (String otherRealm : RealmParser.getAllRealms()) {
                int gap = RealmParser.calculateRealmGap(testRealm, otherRealm);
                String gapText = gap > 0 ? "高" + gap + "级" : gap < 0 ? "低" + Math.abs(gap) + "级" : "相同";
                sendInfo(sender, "  §7- " + otherRealm + ": §f" + gapText);
            }
        } else {
            sendWarning(sender, "§e境界有效: §c否");
        }
        sendInfo(sender, "§b===============================");
    }

    /**
     * 分析目标实体
     */
    private void handleTargetTest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendError(sender, "此命令只能由玩家执行！");
            return;
        }

        // 获取玩家视线中的实体
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            20.0,
            entity -> entity instanceof LivingEntity
        );

        if (rayTrace == null || !(rayTrace.getHitEntity() instanceof LivingEntity target)) {
            sendError(sender, "请看向一个生物实体！");
            return;
        }

        sendInfo(sender, "§b========== 目标实体分析 ==========");
        sendInfo(sender, "§e实体类型: §f" + target.getType().name());
        sendInfo(sender, "§e显示名称: §f" + getEntityDisplayName(target));
        sendInfo(sender, "§e自定义名称: §f" + (target.getCustomName() != null ? target.getCustomName() : "无"));
        
        String parsedRealm = RealmParser.parseEntityRealm(target);
        sendInfo(sender, "§e解析境界: §f" + parsedRealm);
        
        String defaultRealm = RealmParser.getDefaultRealmForEntityType(target.getType());
        sendInfo(sender, "§e默认境界: §f" + defaultRealm);
        
        boolean isBoss = RealmParser.isBossFromName(target.getCustomName());
        boolean isElite = RealmParser.isEliteFromName(target.getCustomName());
        sendInfo(sender, "§e特殊类型: §f" + (isBoss ? "Boss" : isElite ? "Elite" : "普通"));
        
        sendInfo(sender, "§e生命值: §f" + target.getHealth() + "/" + target.getMaxHealth());
        sendInfo(sender, "§b===============================");
    }

    /**
     * 显示配置信息
     */
    private void handleConfig(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "用法: /qireward config show");
            return;
        }

        if ("show".equalsIgnoreCase(args[1])) {
            sendInfo(sender, "§b========== 修为奖励配置摘要 ==========");
            
            boolean enabled = plugin.getConfig().getBoolean("qi-from-kills.enabled", true);
            sendInfo(sender, "§e系统启用: " + (enabled ? "§a是" : "§c否"));
            
            boolean pvpEnabled = plugin.getConfig().getBoolean("qi-from-kills.pvp-enabled", false);
            sendInfo(sender, "§ePVP奖励: " + (pvpEnabled ? "§a启用" : "§c禁用"));
            
            var rewardConfig = plugin.getConfigManager().getConfig("cultivation_rewards");
            if (rewardConfig != null) {
                sendInfo(sender, "§e基础奖励配置:");
                for (String realm : RealmParser.getAllRealms()) {
                    long reward = rewardConfig.getLong("base-rewards." + realm, 0);
                    sendInfo(sender, "  §7- " + realm + ": §f" + reward + " 修为");
                }
                
                sendInfo(sender, "§e防刷机制:");
                boolean cooldownEnabled = rewardConfig.getBoolean("anti-exploit.kill-cooldowns", true);
                sendInfo(sender, "  §7- 冷却限制: " + (cooldownEnabled ? "§a启用" : "§c禁用"));
                
                boolean dailyLimitEnabled = rewardConfig.getBoolean("anti-exploit.daily-limits", true);
                sendInfo(sender, "  §7- 每日限额: " + (dailyLimitEnabled ? "§a启用" : "§c禁用"));
            }
            sendInfo(sender, "§b===================================");
        }
    }

    /**
     * 查看玩家统计
     */
    private void handleStats(CommandSender sender, String[] args) {
        Player targetPlayer = getTargetPlayer(sender, args, 1);
        if (targetPlayer == null) {
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(targetPlayer.getUniqueId());
        if (data == null) {
            sendError(sender, "无法加载玩家数据！");
            return;
        }

        sendInfo(sender, "§b========== 玩家击杀统计 ==========");
        sendInfo(sender, "§e玩家: §f" + targetPlayer.getName());
        sendInfo(sender, "§e当前境界: §f" + data.getRealm());
        sendInfo(sender, "§e当前修为: §f" + data.getQi());
        
        // 这里可以扩展显示更多统计信息
        // 比如今日击杀数量、获得修为等
        // 需要在 QiRewardCalculator 中添加统计功能
        
        sendInfo(sender, "§7注: 详细击杀统计功能将在后续版本中添加");
        sendInfo(sender, "§b===============================");
    }

    // ========== 辅助方法 ==========

    private Player getTargetPlayer(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            if (sender instanceof Player) {
                return (Player) sender;
            } else {
                sendError(sender, "请指定一个玩家！");
                return null;
            }
        }
        Player targetPlayer = Bukkit.getPlayer(args[index]);
        if (targetPlayer == null) {
            sendError(sender, "玩家不在线或不存在！");
            return null;
        }
        return targetPlayer;
    }

    private String getEntityDisplayName(LivingEntity entity) {
        String customName = entity.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        return entity.getType().name();
    }

    private double calculateRealmMultiplier(int realmGap) {
        if (realmGap == 0) return 1.0;
        else if (realmGap == 1) return 1.5;
        else if (realmGap == 2) return 2.0;
        else if (realmGap == 3) return 2.5;
        else if (realmGap >= 4) return 3.0;
        else if (realmGap == -1) return 0.7;
        else if (realmGap == -2) return 0.4;
        else if (realmGap == -3) return 0.2;
        else return 0.1; // -4 or less
    }
}










