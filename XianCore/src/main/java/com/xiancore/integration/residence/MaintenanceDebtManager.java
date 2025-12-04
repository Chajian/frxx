package com.xiancore.integration.residence;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 宗门维护费欠费管理器
 * 负责处理维护费不足时的锁定和删除逻辑
 *
 * 欠费流程:
 * - 0天: 欠费开始，发送警告消息
 * - 1天: 继续欠费，再次警告
 * - 3天: 欠费3天，冻结领地权限（不能建筑）
 * - 7天: 欠费7天，删除领地（丧失所有权）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class MaintenanceDebtManager {

    private static final Logger logger = Logger.getLogger("XianCore");
    private final XianCore plugin;

    // 欠费记录: 宗门ID -> 欠费开始时间戳
    @Getter
    private final Map<Integer, Long> debtStartTime = new ConcurrentHashMap<>();

    // 欠费金额: 宗门ID -> 欠费金额
    @Getter
    private final Map<Integer, Long> debtAmount = new ConcurrentHashMap<>();

    // 欠费警告记录: 宗门ID -> 最后警告时间戳
    private final Map<Integer, Long> lastWarningTime = new ConcurrentHashMap<>();

    // 冻结状态: 宗门ID -> 是否冻结
    @Getter
    private final Map<Integer, Boolean> frozenTerritories = new ConcurrentHashMap<>();

    // 警告阈值（毫秒）
    private static final long DEBT_WARNING_INTERVAL = 24 * 60 * 60 * 1000L;    // 1天警告一次
    private static final long DEBT_FREEZE_THRESHOLD = 3 * 24 * 60 * 60 * 1000L; // 3天冻结
    private static final long DEBT_DELETE_THRESHOLD = 7 * 24 * 60 * 60 * 1000L; // 7天删除

    public MaintenanceDebtManager(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 记录维护费欠费
     *
     * @param sect 欠费的宗门
     * @param dueAmount 欠费金额
     */
    public void recordDebt(Sect sect, long dueAmount) {
        int sectId = sect.getId();

        // 如果是首次欠费，记录时间
        if (!debtStartTime.containsKey(sectId)) {
            debtStartTime.put(sectId, System.currentTimeMillis());
            logger.info("宗门 " + sect.getName() + " 开始欠费: " + dueAmount + " 灵石");
        }

        // 更新欠费金额
        debtAmount.put(sectId, dueAmount);
    }

    /**
     * 处理欠费宗门
     * 根据欠费时长应用不同的处理策略
     *
     * @param sect 欠费宗门
     * @return 是否需要删除领地
     */
    public boolean handleDebt(Sect sect) {
        int sectId = sect.getId();

        // 如果没有欠费记录，直接返回
        if (!debtStartTime.containsKey(sectId)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long debtTime = debtStartTime.get(sectId);
        long elapsedTime = currentTime - debtTime;
        long dueAmount = debtAmount.getOrDefault(sectId, 0L);

        // ==================== 1天内: 发送警告 ====================
        if (elapsedTime < DEBT_FREEZE_THRESHOLD) {
            handleDebtWarning(sect, dueAmount, elapsedTime);
            return false;
        }

        // ==================== 3天: 冻结领地 ====================
        if (elapsedTime >= DEBT_FREEZE_THRESHOLD && elapsedTime < DEBT_DELETE_THRESHOLD) {
            freezeTerritory(sect, dueAmount, elapsedTime);
            return false;
        }

        // ==================== 7天: 删除领地 ====================
        if (elapsedTime >= DEBT_DELETE_THRESHOLD) {
            deleteTerritory(sect, dueAmount);
            return true;
        }

        return false;
    }

    /**
     * 发送欠费警告
     *
     * @param sect 欠费宗门
     * @param dueAmount 欠费金额
     * @param elapsedTime 欠费时长（毫秒）
     */
    private void handleDebtWarning(Sect sect, long dueAmount, long elapsedTime) {
        int sectId = sect.getId();
        long currentTime = System.currentTimeMillis();

        // 检查是否需要发送警告（最多每天一次）
        long lastWarning = lastWarningTime.getOrDefault(sectId, 0L);
        if (currentTime - lastWarning < DEBT_WARNING_INTERVAL) {
            return; // 避免频繁警告
        }

        // 更新最后警告时间
        lastWarningTime.put(sectId, currentTime);

        // 计算剩余时间
        long remainingTime = DEBT_FREEZE_THRESHOLD - elapsedTime;
        long daysRemaining = remainingTime / (24 * 60 * 60 * 1000L);

        // 发送警告消息
        String warningMsg = String.format(
            "§c【宗门财务警告】\n" +
            "§c宗门领地维护费欠缴: §6%d 灵石\n" +
            "§c距离领地冻结还有: §e%d 天\n" +
            "§c请及时缴纳维护费，否则领地将被冻结！",
            dueAmount,
            Math.max(0, daysRemaining)
        );

        sect.broadcastMessage(warningMsg);

        // 记录日志
        logger.warning("宗门 " + sect.getName() + " 欠费警告: " + dueAmount + " 灵石, " +
                     "欠费时长: " + formatTime(elapsedTime));
    }

    /**
     * 冻结领地（禁止建筑）
     *
     * @param sect 欠费宗门
     * @param dueAmount 欠费金额
     * @param elapsedTime 欠费时长（毫秒）
     */
    private void freezeTerritory(Sect sect, long dueAmount, long elapsedTime) {
        int sectId = sect.getId();

        // 检查是否已冻结
        if (frozenTerritories.getOrDefault(sectId, false)) {
            return; // 已冻结，无需重复处理
        }

        // 标记为冻结
        frozenTerritories.put(sectId, true);

        // 计算剩余时间
        long remainingTime = DEBT_DELETE_THRESHOLD - elapsedTime;
        long daysRemaining = remainingTime / (24 * 60 * 60 * 1000L);

        // 发送冻结通知
        String freezeMsg = String.format(
            "§c【宗门领地冻结】\n" +
            "§c由于维护费欠缴 §6%d 灵石，\n" +
            "§c宗门领地已被冻结！\n" +
            "§c§l禁止一切建筑活动\n" +
            "§c距离领地删除还有: §e%d 天\n" +
            "§c立即缴纳维护费可解除冻结！",
            dueAmount,
            Math.max(0, daysRemaining)
        );

        sect.broadcastMessage(freezeMsg);

        // 记录日志
        logger.warning("宗门 " + sect.getName() + " 领地已冻结: " + dueAmount + " 灵石欠费");
    }

    /**
     * 删除领地（完全清理）
     * 执行领地删除的全部清理操作，包括：
     * - 通知宗门成员
     * - 清除所有领地关联数据
     * - 清除维护费记录
     * - 清除冻结状态
     * - 从Residence插件中删除领地（当实现时）
     * - 记录详细日志
     *
     * @param sect 欠费宗门
     * @param dueAmount 欠费金额
     */
    private void deleteTerritory(Sect sect, long dueAmount) {
        int sectId = sect.getId();
        String landId = sect.getResidenceLandId();

        try {
            // ==================== 第一步：通知宗门成员 ====================
            String deleteMsg = String.format(
                "§c【领地删除通知】\n" +
                "§c由于长期拖欠维护费 (§6%d 灵石)，\n" +
                "§c宗门领地已被系统删除！\n" +
                "§c宗门丧失对该领地的一切权限\n" +
                "§c如有异议，请联系管理员申诉",
                dueAmount
            );
            sect.broadcastMessage(deleteMsg);

            // ==================== 第二步：Residence插件中删除领地 ====================
            // TODO: 当 Residence API 完整实现时，取消注释以下代码
            // try {
            //     ResidenceApi.getResidenceManager().removeResidence(landId);
            //     logger.info("已从 Residence 删除领地: " + landId);
            // } catch (Exception e) {
            //     logger.warning("从 Residence 删除领地失败: " + e.getMessage());
            // }

            // ==================== 第三步：清除所有领地关联数据 ====================
            // 3.1 清除基本领地信息
            sect.setResidenceLandId(null);
            sect.setLandCenter(null);
            sect.setLastMaintenanceTime(0);

            // 3.2 清除建筑位信息
            sect.clearBuildingSlots();
            logger.info("已清除所有建筑位信息: " + sectId);

            // 3.3 重置维护费相关时间戳
            // (确保下次重新圈地后能正确计算维护费)
            sect.touch();

            // ==================== 第四步：清除维护费欠费记录 ====================
            debtStartTime.remove(sectId);
            debtAmount.remove(sectId);
            lastWarningTime.remove(sectId);
            frozenTerritories.remove(sectId);
            logger.info("已清除所有欠费记录: " + sectId);

            // ==================== 第五步：保存宗门数据到数据库 ====================
            plugin.getSectSystem().saveSect(sect);
            logger.info("已保存宗门数据到数据库: " + sect.getName());

            // ==================== 第六步：记录详细日志 ====================
            logger.warning(String.format(
                "§c宗门领地删除完成 - 宗门: %s (ID: %d), 领地ID: %s, 欠费: %d灵石",
                sect.getName(), sectId, landId, dueAmount
            ));

        } catch (Exception e) {
            logger.severe("领地删除过程中发生异常 - 宗门: " + sect.getName() + ", 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 缴纳维护费，清除欠费状态
     *
     * @param sect 宗门
     * @param paidAmount 缴纳金额
     * @return 是否成功清除欠费
     */
    public boolean payDebt(Sect sect, long paidAmount) {
        int sectId = sect.getId();
        long dueAmount = debtAmount.getOrDefault(sectId, 0L);

        // 检查是否有欠费
        if (!debtStartTime.containsKey(sectId) || dueAmount <= 0) {
            return false; // 没有欠费
        }

        // 缴纳金额不足
        if (paidAmount < dueAmount) {
            return false;
        }

        // 清除欠费记录
        clearDebt(sect);

        // 解除冻结状态
        frozenTerritories.remove(sectId);

        // 发送清除通知
        sect.broadcastMessage("§a【维护费已缴】\n" +
                            "§a宗门维护费已缴纳，欠费已清除\n" +
                            "§a领地可恢复正常使用");

        // 记录日志
        logger.info("宗门 " + sect.getName() + " 已缴纳欠费: " + dueAmount + " 灵石");

        return true;
    }

    /**
     * 清除欠费记录
     *
     * @param sect 宗门
     */
    public void clearDebt(Sect sect) {
        int sectId = sect.getId();
        debtStartTime.remove(sectId);
        debtAmount.remove(sectId);
        lastWarningTime.remove(sectId);
    }

    /**
     * 检查领地是否被冻结
     *
     * @param sect 宗门
     * @return 是否冻结
     */
    public boolean isTerritoryFrozen(Sect sect) {
        return frozenTerritories.getOrDefault(sect.getId(), false);
    }

    /**
     * 获取宗门的欠费信息
     *
     * @param sect 宗门
     * @return 欠费信息字符串
     */
    public String getDebtInfo(Sect sect) {
        int sectId = sect.getId();

        // 没有欠费
        if (!debtStartTime.containsKey(sectId)) {
            return "§a无欠费";
        }

        long currentTime = System.currentTimeMillis();
        long debtTime = debtStartTime.get(sectId);
        long elapsedTime = currentTime - debtTime;
        long dueAmount = debtAmount.getOrDefault(sectId, 0L);

        String status;
        if (elapsedTime < DEBT_FREEZE_THRESHOLD) {
            status = "§c警告";
        } else if (elapsedTime < DEBT_DELETE_THRESHOLD) {
            status = "§4冻结";
        } else {
            status = "§8待删除";
        }

        return String.format("%s (欠费: %d灵石, 时长: %s)",
            status,
            dueAmount,
            formatTime(elapsedTime)
        );
    }

    /**
     * 格式化时间显示
     *
     * @param millis 毫秒数
     * @return 格式化后的时间字符串
     */
    private String formatTime(long millis) {
        long days = millis / (24 * 60 * 60 * 1000L);
        long hours = (millis % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L);

        if (days > 0) {
            return days + "天" + hours + "小时";
        } else {
            return hours + "小时";
        }
    }

    /**
     * 获取欠费宗门列表
     *
     * @return 欠费宗门ID列表
     */
    public List<Integer> getDebtedSects() {
        return new ArrayList<>(debtStartTime.keySet());
    }

    /**
     * 生成欠费报告
     *
     * @return 报告字符串
     */
    public String generateDebtReport() {
        if (debtStartTime.isEmpty()) {
            return "§a当前无任何欠费宗门";
        }

        StringBuilder report = new StringBuilder();
        report.append("§b========== 宗门欠费报告 ==========\n");

        for (int sectId : debtStartTime.keySet()) {
            Sect sect = plugin.getSectSystem().getSect(sectId);
            if (sect != null) {
                report.append("§e").append(sect.getName()).append(": ")
                      .append(getDebtInfo(sect)).append("\n");
            }
        }

        report.append("§b====================================");
        return report.toString();
    }
}
