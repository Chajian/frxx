package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import com.xiancore.core.data.migrate.MigrationManager;
import com.xiancore.core.data.migrate.MigrationReport;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据迁移命令
 * /xiancore migrate [--dry-run|--info|confirm]
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class MigrateCommand extends AbstractSubCommand {

    public MigrateCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "migrate";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"迁移"};
    }

    @Override
    public String getPermission() {
        return "xiancore.admin.migrate";
    }

    @Override
    public String getUsage() {
        return "/xiancore migrate [--dry-run|--info|confirm]";
    }

    @Override
    public String getDescription() {
        return "将YML数据迁移到MySQL";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }

        MigrationManager migrationManager = plugin.getMigrationManager();
        if (migrationManager == null) {
            sendError(sender, "迁移管理器未初始化！");
            return;
        }

        // 解析参数
        final boolean dryRun;
        boolean showInfo = false;

        if (args.length >= 1) {
            String param = args[0].toLowerCase();
            if (param.equals("--dry-run") || param.equals("-d")) {
                dryRun = true;
            } else if (param.equals("--info") || param.equals("-i")) {
                showInfo = true;
                dryRun = false;
            } else if (param.equals("confirm")) {
                dryRun = false;
            } else {
                dryRun = false;
            }
        } else {
            dryRun = false;
        }

        // 显示信息模式
        if (showInfo) {
            String summary = migrationManager.getPreMigrationSummary();
            for (String line : summary.split("\n")) {
                sender.sendMessage(line);
            }
            return;
        }

        // 确认提示
        if (!dryRun && args.length < 1) {
            sendWarning(sender, "§e§l警告: 此操作将把YML数据迁移到MySQL！");
            sendInfo(sender, "§7使用 §f/xiancore migrate --info §7查看详情");
            sendInfo(sender, "§7使用 §f/xiancore migrate --dry-run §7预览迁移");
            sendInfo(sender, "§7使用 §f/xiancore migrate confirm §7确认并执行迁移");
            return;
        }

        boolean confirmed = args.length >= 1 && args[0].equalsIgnoreCase("confirm");
        if (!dryRun && !confirmed) {
            sendError(sender, "请使用 --dry-run 预览或 confirm 确认执行");
            return;
        }

        // 执行迁移
        if (dryRun) {
            sendInfo(sender, "§e正在执行预览迁移（不写入数据库）...");
        } else {
            sendWarning(sender, "§c§l正在执行真实迁移，请勿关闭服务器！");
        }

        // 异步执行所有迁移器
        migrationManager.migrateAll(dryRun).thenAccept(reports -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                displayMigrationResult(sender, reports, dryRun);
            });
        }).exceptionally(throwable -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sendError(sender, "迁移过程发生错误: " + throwable.getMessage());
                plugin.getLogger().severe("数据迁移失败:");
                throwable.printStackTrace();
            });
            return null;
        });

        sendInfo(sender, "§e迁移任务已在后台启动，请稍候...");
    }

    /**
     * 显示迁移结果
     */
    private void displayMigrationResult(CommandSender sender, Map<String, MigrationReport> reports, boolean dryRun) {
        sendInfo(sender, "§b========================================");
        sendInfo(sender, "§e§l         迁移总结报告");
        sendInfo(sender, "§b========================================");
        sendInfo(sender, "");

        int totalSuccess = 0;
        int totalFailed = 0;
        int totalSkipped = 0;

        for (Map.Entry<String, MigrationReport> entry : reports.entrySet()) {
            String type = entry.getKey();
            MigrationReport report = entry.getValue();

            sender.sendMessage(String.format("§e%s: §a成功:%d §c失败:%d §7跳过:%d",
                    type, report.getSuccessCount(), report.getFailedCount(), report.getSkippedCount()));

            totalSuccess += report.getSuccessCount();
            totalFailed += report.getFailedCount();
            totalSkipped += report.getSkippedCount();
        }

        sendInfo(sender, "");
        sendInfo(sender, String.format("§e总计: §a成功:%d §c失败:%d §7跳过:%d",
                totalSuccess, totalFailed, totalSkipped));
        sendInfo(sender, "§b========================================");

        if (dryRun && totalSuccess > 0) {
            sendInfo(sender, "");
            sendSuccess(sender, "§a预览完成！使用以下命令执行真实迁移:");
            sendInfo(sender, "§e/xiancore migrate confirm");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(Arrays.asList("--dry-run", "--info", "confirm"), args[0]);
        }
        return new ArrayList<>();
    }
}
