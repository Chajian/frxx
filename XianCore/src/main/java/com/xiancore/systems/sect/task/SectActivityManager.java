package com.xiancore.systems.sect.task;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
import lombok.Data;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 宗门活跃度管理器
 * 轻量级设计，配合 Quests 插件使用
 *
 * 功能：
 * - 每日签到
 * - 活跃度统计
 * - 贡献值排行
 * - Quests 任务奖励发放
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectActivityManager {

    private final XianCore plugin;

    // 签到记录 (玩家UUID -> 签到数据)
    private final Map<UUID, CheckInData> checkInRecords;

    // 活跃度记录 (宗门ID -> 活跃度数据)
    private final Map<Integer, SectActivityData> activityRecords;

    public SectActivityManager(XianCore plugin) {
        this.plugin = plugin;
        this.checkInRecords = new ConcurrentHashMap<>();
        this.activityRecords = new ConcurrentHashMap<>();
    }

    /**
     * 初始化
     */
    public void initialize() {
        // 加载签到数据
        loadCheckInData();

        plugin.getLogger().info("  §a✓ 宗门活跃度系统初始化完成");
    }

    /**
     * 加载签到数据
     */
    private void loadCheckInData() {
        // TODO: 从数据库加载签到记录
    }

    /**
     * 玩家签到
     *
     * @param player 玩家
     * @return 签到是否成功
     */
    public boolean checkIn(Player player) {
        UUID playerId = player.getUniqueId();

        // 检查玩家是否在宗门
        Integer sectId = plugin.getSectSystem().getPlayerSects().get(playerId);
        if (sectId == null) {
            player.sendMessage("§c你还没有加入宗门!");
            return false;
        }

        CheckInData data = checkInRecords.computeIfAbsent(playerId, k -> new CheckInData());

        // 检查今天是否已签到
        if (data.hasCheckedInToday()) {
            player.sendMessage("§c你今天已经签到过了!");
            return false;
        }

        // 执行签到
        data.checkIn();

        // 计算签到奖励（连续签到奖励递增）
        int baseReward = 10;
        int bonusReward = Math.min(data.getConsecutiveDays() * 2, 50); // 最多+50
        int totalContribution = baseReward + bonusReward;
        int spiritStone = 50 + data.getConsecutiveDays() * 5;

        // 发放奖励
        PlayerData playerData = plugin.getDataManager().loadPlayerData(playerId);
        if (playerData != null) {
            playerData.addContribution(totalContribution);
            playerData.addSpiritStones(spiritStone);
            playerData.addActiveQi(5); // 签到增加活跃灵气
            plugin.getDataManager().savePlayerData(playerData);
        }

        // 增加宗门活跃度
        addSectActivity(sectId, 10);

        // 通知玩家
        player.sendMessage("§a========== 签到成功 ==========");
        player.sendMessage("§e连续签到: §6" + data.getConsecutiveDays() + " §e天");
        player.sendMessage("§e累计签到: §6" + data.getTotalDays() + " §e天");
        player.sendMessage("§e奖励:");
        player.sendMessage("§7  +§b" + totalContribution + " §7贡献值");
        player.sendMessage("§7  +§6" + spiritStone + " §7灵石");
        player.sendMessage("§7  +§a5 §7活跃灵气");
        player.sendMessage("§a===========================");

        return true;
    }

    /**
     * 增加宗门活跃度
     *
     * @param sectId 宗门ID
     * @param amount 活跃度值
     */
    public void addSectActivity(Integer sectId, int amount) {
        SectActivityData data = activityRecords.computeIfAbsent(sectId, k -> new SectActivityData());
        data.addActivity(amount);
    }

    /**
     * 处理 Quests 任务完成
     * 当玩家完成宗门任务时，发放宗门特定奖励
     *
     * @param player 玩家
     * @param questName 任务名称
     */
    public void handleQuestComplete(Player player, String questName) {
        UUID playerId = player.getUniqueId();

        // 检查是否是宗门任务（以[宗门]开头）
        if (!questName.startsWith("[宗门]")) {
            return;
        }

        Integer sectId = plugin.getSectSystem().getPlayerSects().get(playerId);
        if (sectId == null) {
            return;
        }

        // 解析奖励（从任务名称或配置）
        int contribution = parseContributionReward(questName);
        int sectExp = parseSectExpReward(questName);

        if (contribution > 0) {
            PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
            if (data != null) {
                data.addContribution(contribution);
                data.addActiveQi(15); // 完成宗门任务增加活跃灵气
                plugin.getDataManager().savePlayerData(data);
            }
        }

        if (sectExp > 0) {
            Sect sect = plugin.getSectSystem().getSects().get(sectId);
            if (sect != null) {
                sect.setExperience(sect.getExperience() + sectExp);
                plugin.getDataManager().saveSect(sect);
            }
        }

        // 增加活跃度
        addSectActivity(sectId, 20);

        // 通知玩家
        player.sendMessage("§a✓ 完成宗门任务!");
        if (contribution > 0) {
            player.sendMessage("§e  +§b" + contribution + " §e贡献值");
        }
        if (sectExp > 0) {
            player.sendMessage("§e  +§d" + sectExp + " §e宗门经验");
        }
        player.sendMessage("§e  +§a15 §e活跃灵气");
    }

    /**
     * 从任务名称解析贡献值奖励
     * 示例: "[宗门] 清理怪物 [贡献:10]"
     */
    private int parseContributionReward(String questName) {
        try {
            if (questName.contains("[贡献:")) {
                int start = questName.indexOf("[贡献:") + 5;
                int end = questName.indexOf("]", start);
                return Integer.parseInt(questName.substring(start, end));
            }
        } catch (Exception e) {
            // 解析失败，返回默认值
        }
        return 10; // 默认奖励
    }

    /**
     * 从任务名称解析宗门经验奖励
     * 示例: "[宗门] 清理怪物 [经验:50]"
     */
    private int parseSectExpReward(String questName) {
        try {
            if (questName.contains("[经验:")) {
                int start = questName.indexOf("[经验:") + 5;
                int end = questName.indexOf("]", start);
                return Integer.parseInt(questName.substring(start, end));
            }
        } catch (Exception e) {
            // 解析失败，返回默认值
        }
        return 50; // 默认经验
    }

    /**
     * 获取宗门活跃度排行
     *
     * @param limit 排行数量
     * @return 排行列表
     */
    public List<Map.Entry<Integer, SectActivityData>> getActivityRanking(int limit) {
        return activityRecords.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getTodayActivity(), a.getValue().getTodayActivity()))
                .limit(limit)
                .toList();
    }

    /**
     * 获取玩家签到数据
     */
    public CheckInData getCheckInData(UUID playerId) {
        return checkInRecords.computeIfAbsent(playerId, k -> new CheckInData());
    }

    /**
     * 每日重置（由定时任务调用）
     */
    public void dailyReset() {
        // 重置每日活跃度
        for (SectActivityData data : activityRecords.values()) {
            data.resetDaily();
        }

        plugin.getLogger().info("§a已重置宗门每日活跃度");
    }

    // ==================== 内部数据类 ====================

    /**
     * 签到数据
     */
    @Data
    public static class CheckInData {
        private String lastCheckInDate;     // 最后签到日期 (YYYY-MM-DD)
        private int consecutiveDays;        // 连续签到天数
        private int totalDays;              // 累计签到天数

        public CheckInData() {
            this.lastCheckInDate = "";
            this.consecutiveDays = 0;
            this.totalDays = 0;
        }

        /**
         * 检查今天是否已签到
         */
        public boolean hasCheckedInToday() {
            String today = LocalDate.now().toString();
            return today.equals(lastCheckInDate);
        }

        /**
         * 执行签到
         */
        public void checkIn() {
            String today = LocalDate.now().toString();
            String yesterday = LocalDate.now().minusDays(1).toString();

            if (yesterday.equals(lastCheckInDate)) {
                // 连续签到
                consecutiveDays++;
            } else if (!today.equals(lastCheckInDate)) {
                // 中断了，重新开始
                consecutiveDays = 1;
            }

            totalDays++;
            lastCheckInDate = today;
        }
    }

    /**
     * 宗门活跃度数据
     */
    @Data
    public static class SectActivityData {
        private int todayActivity;          // 今日活跃度
        private int weekActivity;           // 本周活跃度
        private int totalActivity;          // 累计活跃度

        public void addActivity(int amount) {
            todayActivity += amount;
            weekActivity += amount;
            totalActivity += amount;
        }

        public void resetDaily() {
            todayActivity = 0;
        }

        public void resetWeekly() {
            weekActivity = 0;
        }
    }
}
