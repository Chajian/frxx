package com.xiancore.integration.residence;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectSystem;
import lombok.Getter;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 宗门领地维护费定时扣除系统
 * 负责定期检查和扣除宗门的领地维护费
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class MaintenanceFeeScheduler {

    private final XianCore plugin;
    private final SectSystem sectSystem;
    private final SectResidenceManager residenceManager;

    // 定时任务相关
    private BukkitTask maintenanceTask;
    private boolean enabled = true;

    // 维护费扣除周期（毫秒）：默认7天
    private long maintenancePeriod = 7 * 24 * 60 * 60 * 1000L;

    // 时区配置
    private ZoneId timezone = ZoneId.systemDefault();

    // 扣费时间配置（HH:mm）
    private String maintenanceTime = "00:00";

    // 最后扣费时间记录（宗门ID -> 时间戳）
    private final Map<Integer, Long> lastMaintenanceCheck = new ConcurrentHashMap<>();

    // 维护费记录
    @Getter
    private final MaintenanceFeeRecord maintenanceFeeRecord = new MaintenanceFeeRecord();

    // 欠费管理器
    @Getter
    private final MaintenanceDebtManager debtManager;

    /**
     * 构造函数
     */
    public MaintenanceFeeScheduler(XianCore plugin, SectSystem sectSystem, SectResidenceManager residenceManager) {
        this.plugin = plugin;
        this.sectSystem = sectSystem;
        this.residenceManager = residenceManager;
        this.debtManager = new MaintenanceDebtManager(plugin);

        // 从配置加载设置
        loadConfiguration();
    }

    /**
     * 加载配置
     */
    private void loadConfiguration() {
        if (plugin.getConfig().contains("residence.maintenance")) {
            maintenancePeriod = plugin.getConfig().getLong("residence.maintenance.period-days", 7) * 24 * 60 * 60 * 1000L;
            maintenanceTime = plugin.getConfig().getString("residence.maintenance.time", "00:00");
            String timezoneStr = plugin.getConfig().getString("residence.maintenance.timezone", "UTC");
            try {
                timezone = ZoneId.of(timezoneStr);
            } catch (Exception e) {
                plugin.getLogger().warning("无效的时区配置: " + timezoneStr + "，使用默认时区");
                timezone = ZoneId.systemDefault();
            }
        }
    }

    /**
     * 启动维护费定时任务
     */
    public void start() {
        if (maintenanceTask != null && !maintenanceTask.isCancelled()) {
            plugin.getLogger().warning("维护费定时任务已在运行！");
            return;
        }

        // 每小时检查一次维护费状态（降低性能影响）
        maintenanceTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::checkAndProcessMaintenanceFees,
            20L,  // 1秒后开始
            20 * 60 * 60L  // 每小时运行一次
        );

        plugin.getLogger().info("§a✓ 维护费定时扣除系统已启动");
    }

    /**
     * 停止维护费定时任务
     */
    public void stop() {
        if (maintenanceTask != null && !maintenanceTask.isCancelled()) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
        plugin.getLogger().info("§a✓ 维护费定时扣除系统已停止");
    }

    /**
     * 检查并处理维护费
     */
    private void checkAndProcessMaintenanceFees() {
        if (!enabled) {
            return;
        }

        try {
            List<Sect> allSects = sectSystem.getAllSects();

            for (Sect sect : allSects) {
                // 检查是否需要扣费
                if (shouldProcessMaintenance(sect)) {
                    processSectMaintenance(sect);
                }

                // 检查维护费状态变化
                checkMaintenanceStatusTransition(sect);
            }

            maintenanceFeeRecord.incrementCheckCount();
        } catch (Exception e) {
            plugin.getLogger().warning("§c维护费检查处理失败: " + e.getMessage());
            e.printStackTrace();
            maintenanceFeeRecord.recordError(new MaintenanceFeeRecord.ErrorRecord(
                "维护费检查失败",
                e.getMessage(),
                System.currentTimeMillis()
            ));
        }
    }

    /**
     * 判断是否应该处理维护费
     */
    private boolean shouldProcessMaintenance(Sect sect) {
        // 没有领地不需要处理
        if (!sect.hasLand()) {
            return false;
        }

        long lastMaintenance = sect.getLastMaintenanceTime();
        long now = System.currentTimeMillis();
        long timeSinceMaintenance = now - lastMaintenance;

        // 超过维护期（或首次设置）就需要扣费
        return timeSinceMaintenance >= maintenancePeriod;
    }

    /**
     * 处理单个宗门的维护费
     */
    private void processSectMaintenance(Sect sect) {
        // 获取领地大小（暂时用1作为占位符，实际需要从Residence API获取）
        int landSize = 1; // TODO: 从 SectResidenceManager 获取实际领地大小

        // 计算维护费
        long maintenanceCost = residenceManager.calculateMaintenanceCost(landSize);

        // 检查宗门资金
        if (sect.getSectFunds() < maintenanceCost) {
            // 资金不足，触发逾期警告
            handleMaintenanceOverdue(sect, maintenanceCost);
            maintenanceFeeRecord.recordOverdueEvent(sect.getId(), maintenanceCost);
            return;
        }

        // 扣除维护费
        if (sect.removeFunds(maintenanceCost)) {
            sect.setLastMaintenanceTime(System.currentTimeMillis());
            sectSystem.saveSect(sect);

            // 通知宗门
            sect.broadcastMessage("§a§l[领地维护] 维护费已成功扣除");
            sect.broadcastMessage("§a费用: §6" + maintenanceCost + " 灵石");
            sect.broadcastMessage("§a下次扣费时间: §f" + getNextMaintenanceTime());

            // 记录维护费事件
            maintenanceFeeRecord.recordSuccessfulPayment(sect.getId(), maintenanceCost);

            plugin.getLogger().info(String.format(
                "§a宗门 %s (ID: %d) 维护费已扣除: %d 灵石",
                sect.getName(), sect.getId(), maintenanceCost
            ));
        }
    }

    /**
     * 检查维护费状态转变
     */
    private void checkMaintenanceStatusTransition(Sect sect) {
        if (!sect.hasLand()) {
            return;
        }

        SectResidenceManager.MaintenanceStatus status = residenceManager.getMaintenanceStatus(sect);

        switch (status) {
            case OVERDUE_WARNING:
                // 逾期警告状态
                sect.broadcastMessage("§e§l[警告] 宗门领地维护费即将逾期！");
                sect.broadcastMessage("§e请尽快缴纳维护费，否则领地功能将被冻结");
                break;

            case FROZEN:
                // 冻结状态 - 限制使用
                sect.broadcastMessage("§c§l[冻结通知] 宗门领地维护费已逾期");
                sect.broadcastMessage("§c领地功能已被冻结，请立即缴纳维护费");
                sect.broadcastMessage("§7使用 /sect land pay 缴纳维护费");
                break;

            case AUTO_RELEASING:
                // 自动释放状态 - 执行自动释放
                handleAutoRelease(sect);
                break;

            default:
                break;
        }
    }


    /**
     * 处理维护费逾期（资金不足）
     * 集成欠费管理机制
     */
    private void handleMaintenanceOverdue(Sect sect, long requiredAmount) {
        long shortage = requiredAmount - sect.getSectFunds();

        // ==================== 记录欠费 ====================
        debtManager.recordDebt(sect, requiredAmount);

        // ==================== 检查欠费状态 ====================
        boolean shouldDelete = debtManager.handleDebt(sect);

        // 如果需要删除领地
        if (shouldDelete) {
            return; // debtManager 已处理删除
        }

        // ==================== 发送通知 ====================
        sect.broadcastMessage("§c§l[维护费不足] 无法扣除维护费");
        sect.broadcastMessage("§c需要: §6" + requiredAmount + " §c灵石");
        sect.broadcastMessage("§c当前: §6" + sect.getSectFunds() + " §c灵石");
        sect.broadcastMessage("§c缺少: §6" + shortage + " §c灵石");
        sect.broadcastMessage("§e欠费状态: " + debtManager.getDebtInfo(sect));
        sect.broadcastMessage("§7使用 /sect land pay 缴纳维护费");

        // ==================== 记录日志 ====================
        plugin.getLogger().warning(String.format(
            "宗门 %s (ID: %d) 维护费不足，需要 %d 但仅有 %d",
            sect.getName(), sect.getId(), requiredAmount, sect.getSectFunds()
        ));
    }

    /**
     * 处理自动释放领地
     */
    private void handleAutoRelease(Sect sect) {
        if (!sect.hasLand()) {
            return;
        }

        // 通知宗门即将释放
        sect.broadcastMessage("§c§l[最后警告] 宗门领地将在 24 小时内自动释放！");
        sect.broadcastMessage("§c请立即缴纳维护费以保留领地");
        sect.broadcastMessage("§7使用 /sect land pay 缴纳维护费");

        // 记录自动释放事件（但暂不执行自动删除，由管理员手动处理）
        maintenanceFeeRecord.recordAutoReleaseEvent(sect.getId());

        plugin.getLogger().warning(String.format(
            "宗门 %s (ID: %d) 维护费严重逾期，即将自动释放",
            sect.getName(), sect.getId()
        ));
    }

    /**
     * 手动处理维护费支付
     * 用于 /sect land pay 命令
     */
    public boolean processManualPayment(Sect sect, long paymentAmount) {
        if (!sect.hasLand()) {
            return false;
        }

        // 检查是否有欠费需要先缴清
        long dueAmount = debtManager.getDebtAmount().getOrDefault(sect.getId(), 0L);

        // 如果有欠费
        if (dueAmount > 0) {
            if (paymentAmount < dueAmount) {
                sect.broadcastMessage("§c缴纳金额不足以清除欠费");
                sect.broadcastMessage("§c需要缴纳: §6" + dueAmount + " §c灵石");
                sect.broadcastMessage("§c您缴纳: §6" + paymentAmount + " §c灵石");
                return false;
            }

            // 清除欠费状态
            if (debtManager.payDebt(sect, paymentAmount)) {
                sect.setLastMaintenanceTime(System.currentTimeMillis());
                sectSystem.saveSect(sect);
                maintenanceFeeRecord.recordSuccessfulPayment(sect.getId(), paymentAmount);
                return true;
            }
            return false;
        }

        // 正常维护费缴纳
        // TODO: 从 SectResidenceManager 获取实际领地大小
        int landSize = 1;
        long maintenanceCost = residenceManager.calculateMaintenanceCost(landSize);

        if (paymentAmount < maintenanceCost) {
            sect.broadcastMessage("§c缴纳金额不足");
            sect.broadcastMessage("§c需要: §6" + maintenanceCost + " §c灵石");
            sect.broadcastMessage("§c您缴纳: §6" + paymentAmount + " §c灵石");
            return false;
        }

        if (sect.removeFunds(paymentAmount)) {
            sect.setLastMaintenanceTime(System.currentTimeMillis());
            sectSystem.saveSect(sect);

            sect.broadcastMessage("§a§l维护费缴纳成功");
            sect.broadcastMessage("§a缴纳金额: §6" + paymentAmount + " §a灵石");
            sect.broadcastMessage("§a下次扣费时间: §f" + getNextMaintenanceTime());

            maintenanceFeeRecord.recordSuccessfulPayment(sect.getId(), paymentAmount);

            plugin.getLogger().info("宗门 " + sect.getName() + " 缴纳维护费: " + paymentAmount + " 灵石");
            return true;
        }

        return false;
    }

    /**
     * 获取下次维护费扣除时间
     */
    public String getNextMaintenanceTime() {
        long nextTime = System.currentTimeMillis() + maintenancePeriod;
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(nextTime));
    }

    /**
     * 启用/禁用维护费系统
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        String status = enabled ? "§a已启用" : "§c已禁用";
        plugin.getLogger().info("§e维护费系统 " + status);
    }

    /**
     * 维护费记录类
     */
    public static class MaintenanceFeeRecord {
        private long lastCheckTime = 0;
        private long totalChecks = 0;
        private long successfulPayments = 0;
        private long overdueEvents = 0;
        private long autoReleaseEvents = 0;
        private final List<ErrorRecord> errors = Collections.synchronizedList(new ArrayList<>());

        public void incrementCheckCount() {
            this.lastCheckTime = System.currentTimeMillis();
            this.totalChecks++;
        }

        public void recordSuccessfulPayment(int sectId, long amount) {
            this.successfulPayments++;
        }

        public void recordOverdueEvent(int sectId, long requiredAmount) {
            this.overdueEvents++;
        }

        public void recordAutoReleaseEvent(int sectId) {
            this.autoReleaseEvents++;
        }

        public void recordError(ErrorRecord error) {
            this.errors.add(error);
            // 只保留最近100条错误记录
            if (this.errors.size() > 100) {
                this.errors.remove(0);
            }
        }

        public long getLastCheckTime() {
            return lastCheckTime;
        }

        public long getTotalChecks() {
            return totalChecks;
        }

        public long getSuccessfulPayments() {
            return successfulPayments;
        }

        public long getOverdueEvents() {
            return overdueEvents;
        }

        public long getAutoReleaseEvents() {
            return autoReleaseEvents;
        }

        public List<ErrorRecord> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<ErrorRecord> getRecentErrors() {
            int start = Math.max(0, errors.size() - 10);
            return new ArrayList<>(errors.subList(start, errors.size()));
        }

        /**
         * 错误记录
         */
        public static class ErrorRecord {
            private final String description;
            private final String message;
            private final long timestamp;

            public ErrorRecord(String description, String message, long timestamp) {
                this.description = description;
                this.message = message;
                this.timestamp = timestamp;
            }

            public String getDescription() {
                return description;
            }

            public String getErrorMessage() {
                return message;
            }

            public long getTimestamp() {
                return timestamp;
            }
        }
    }

    /**
     * 获取维护费系统统计信息
     */
    public String getStatistics() {
        return String.format(
            "维护费系统统计:\n" +
            "  最后检查: %s\n" +
            "  总检查次数: %d\n" +
            "  成功缴费: %d\n" +
            "  逾期事件: %d\n" +
            "  自动释放: %d\n" +
            "  错误记录: %d",
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(maintenanceFeeRecord.getLastCheckTime())),
            maintenanceFeeRecord.getTotalChecks(),
            maintenanceFeeRecord.getSuccessfulPayments(),
            maintenanceFeeRecord.getOverdueEvents(),
            maintenanceFeeRecord.getAutoReleaseEvents(),
            maintenanceFeeRecord.getErrors().size()
        );
    }
}
