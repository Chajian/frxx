package com.xiancore.systems.sect.task;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 宗门任务管理器
 * 管理所有玩家的宗门任务
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SectTaskManager {

    private final XianCore plugin;
    private final SectTaskGenerator taskGenerator;

    // 玩家任务存储 (玩家UUID -> 任务列表)
    private final Map<UUID, List<SectTask>> playerTasks;

    // 任务ID索引 (任务ID -> 任务对象)
    private final Map<String, SectTask> taskIndex;

    public SectTaskManager(XianCore plugin) {
        this.plugin = plugin;
        this.taskGenerator = new SectTaskGenerator(plugin);
        this.playerTasks = new ConcurrentHashMap<>();
        this.taskIndex = new ConcurrentHashMap<>();
    }

    /**
     * 初始化任务系统
     */
    public void initialize() {
        // 加载任务配置
        taskGenerator.loadTaskTemplates();

        // 从数据库加载玩家任务
        loadPlayerTasks();

        plugin.getLogger().info("  §a✓ 宗门任务系统初始化完成");
    }

    /**
     * 从数据库加载玩家任务
     */
    private void loadPlayerTasks() {
        // TODO: 从数据库加载任务数据
        plugin.getLogger().info("  §7  (任务数据将在玩家登录时动态加载)");
    }

    /**
     * 为玩家生成任务
     *
     * @param player 玩家
     * @param type   任务类型
     */
    public void generateTasksForPlayer(Player player, SectTaskType type) {
        UUID playerId = player.getUniqueId();

        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 获取玩家的任务列表
        List<SectTask> tasks = playerTasks.computeIfAbsent(playerId, k -> new ArrayList<>());

        // 移除已过期的任务
        tasks.removeIf(SectTask::isExpired);

        // 检查是否已有该类型的任务
        boolean hasTypeTask = tasks.stream()
                .anyMatch(task -> task.getType() == type && task.getStatus() != SectTask.TaskStatus.COMPLETED);

        if (!hasTypeTask) {
            // 生成新任务
            List<SectTask> newTasks = taskGenerator.generateTasks(player, type, getTaskCount(type));

            // 分配给玩家
            for (SectTask task : newTasks) {
                task.setOwnerId(playerId);
                task.setStatus(SectTask.TaskStatus.IN_PROGRESS);
                tasks.add(task);
                taskIndex.put(task.getTaskId(), task);
            }

            player.sendMessage("§a[宗门任务] 为你生成了 " + newTasks.size() + " 个" + type.getDisplayName());
        }
    }

    /**
     * 获取玩家的任务列表
     *
     * @param playerId 玩家UUID
     * @return 任务列表
     */
    public List<SectTask> getPlayerTasks(UUID playerId) {
        return playerTasks.getOrDefault(playerId, new ArrayList<>());
    }

    /**
     * 获取玩家指定类型的任务
     *
     * @param playerId 玩家UUID
     * @param type     任务类型
     * @return 任务列表
     */
    public List<SectTask> getPlayerTasksByType(UUID playerId, SectTaskType type) {
        return getPlayerTasks(playerId).stream()
                .filter(task -> task.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 根据任务ID获取任务
     *
     * @param taskId 任务ID
     * @return 任务对象
     */
    public SectTask getTask(String taskId) {
        return taskIndex.get(taskId);
    }

    /**
     * 更新任务进度
     *
     * @param playerId 玩家UUID
     * @param objective 任务目标
     * @param target    目标对象
     * @param amount    增加的数量
     */
    public void updateProgress(UUID playerId, TaskObjective objective, String target, int amount) {
        List<SectTask> tasks = getPlayerTasks(playerId);

        for (SectTask task : tasks) {
            // 检查任务是否匹配
            if (task.getObjective() == objective &&
                    (target == null || target.equals(task.getTarget())) &&
                    task.getStatus() == SectTask.TaskStatus.IN_PROGRESS) {

                boolean completed = task.addProgress(amount);

                if (completed) {
                    // 任务完成通知
                    Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.sendMessage("§a§l✓ 任务完成!");
                        player.sendMessage("§e任务: §f" + task.getName());
                        player.sendMessage("§7使用 /sect task 查看并领取奖励");
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }

                    // 保存数据
                    saveTask(task);
                }
            }
        }
    }

    /**
     * 领取任务奖励
     *
     * @param player 玩家
     * @param taskId 任务ID
     * @return 是否成功领取
     */
    public boolean claimReward(Player player, String taskId) {
        UUID playerId = player.getUniqueId();
        SectTask task = getTask(taskId);

        if (task == null) {
            player.sendMessage("§c任务不存在!");
            return false;
        }

        if (!task.getOwnerId().equals(playerId)) {
            player.sendMessage("§c这不是你的任务!");
            return false;
        }

        if (!task.isCompleted()) {
            player.sendMessage("§c任务尚未完成!");
            return false;
        }

        if (task.isRewardClaimed()) {
            player.sendMessage("§c你已经领取过这个任务的奖励了!");
            return false;
        }

        // 发放奖励
        PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 贡献值
        if (task.getContributionReward() > 0) {
            data.addContribution(task.getContributionReward());
        }

        // 灵石
        if (task.getSpiritStoneReward() > 0) {
            data.addSpiritStones(task.getSpiritStoneReward());
        }

        // 活跃灵气
        if (task.getActivityReward() > 0) {
            data.addActiveQi(task.getActivityReward());
        }

        // 宗门经验
        if (task.getSectExpReward() > 0) {
            Integer sectId = plugin.getSectSystem().getPlayerSects().get(playerId);
            if (sectId != null) {
                Sect sect = plugin.getSectSystem().getSects().get(sectId);
                if (sect != null) {
                    sect.setExperience(sect.getExperience() + task.getSectExpReward());
                    plugin.getDataManager().saveSect(sect);
                }
            }
        }

        // 标记奖励已领取
        task.claimReward();

        // 保存数据
        plugin.getDataManager().savePlayerData(data);
        saveTask(task);

        // 通知玩家
        player.sendMessage("§a§l========== 领取奖励 ==========");
        player.sendMessage("§e任务: §f" + task.getName());
        if (task.getContributionReward() > 0) {
            player.sendMessage("§7  +§b" + task.getContributionReward() + " §7贡献值");
        }
        if (task.getSpiritStoneReward() > 0) {
            player.sendMessage("§7  +§6" + task.getSpiritStoneReward() + " §7灵石");
        }
        if (task.getActivityReward() > 0) {
            player.sendMessage("§7  +§a" + task.getActivityReward() + " §7活跃灵气");
        }
        if (task.getSectExpReward() > 0) {
            player.sendMessage("§7  +§d" + task.getSectExpReward() + " §7宗门经验");
        }
        player.sendMessage("§a§l============================");

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);

        return true;
    }

    /**
     * 放弃任务
     *
     * @param player 玩家
     * @param taskId 任务ID
     * @return 是否成功放弃
     */
    public boolean abandonTask(Player player, String taskId) {
        UUID playerId = player.getUniqueId();
        SectTask task = getTask(taskId);

        if (task == null || !task.getOwnerId().equals(playerId)) {
            player.sendMessage("§c任务不存在!");
            return false;
        }

        if (task.isCompleted()) {
            player.sendMessage("§c无法放弃已完成的任务!");
            return false;
        }

        // 移除任务
        List<SectTask> tasks = getPlayerTasks(playerId);
        tasks.remove(task);
        taskIndex.remove(taskId);

        player.sendMessage("§7已放弃任务: §f" + task.getName());
        return true;
    }

    /**
     * 刷新任务（定时任务调用）
     *
     * @param type 任务类型
     */
    public void refreshTasks(SectTaskType type) {
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<UUID, List<SectTask>> entry : playerTasks.entrySet()) {
            UUID playerId = entry.getKey();
            List<SectTask> tasks = entry.getValue();

            // 移除该类型的旧任务
            tasks.removeIf(task -> task.getType() == type);

            // 为在线玩家生成新任务
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                generateTasksForPlayer(player, type);
            }
        }

        plugin.getLogger().info("§a已刷新 " + type.getDisplayName());
    }

    /**
     * 保存任务数据
     *
     * @param task 任务
     */
    private void saveTask(SectTask task) {
        // TODO: 保存到数据库
    }

    /**
     * 获取每种类型的任务数量
     */
    private int getTaskCount(SectTaskType type) {
        return switch (type) {
            case DAILY -> 3;    // 每日3个任务
            case WEEKLY -> 2;   // 每周2个任务
            case SPECIAL -> 1;  // 特殊任务1个
        };
    }

    /**
     * 玩家登录时初始化任务
     *
     * @param player 玩家
     */
    public void onPlayerLogin(Player player) {
        UUID playerId = player.getUniqueId();

        // 检查是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 如果没有任务，生成日常任务
        List<SectTask> tasks = getPlayerTasks(playerId);
        if (tasks.isEmpty()) {
            generateTasksForPlayer(player, SectTaskType.DAILY);
        }
    }

    /**
     * 获取玩家完成的任务数量
     *
     * @param playerId 玩家UUID
     * @param type     任务类型
     * @return 完成数量
     */
    public int getCompletedCount(UUID playerId, SectTaskType type) {
        return (int) getPlayerTasksByType(playerId, type).stream()
                .filter(SectTask::isCompleted)
                .count();
    }

    /**
     * 提交收集任务
     *
     * @param player 玩家
     * @param taskId 任务ID
     * @return 是否成功提交
     */
    public boolean submitCollectTask(Player player, String taskId) {
        SectTask task = getTask(taskId);
        if (task == null) {
            return false;
        }

        // 检查任务是否为收集类型
        if (task.getObjective() != TaskObjective.COLLECT_ITEM) {
            return false;
        }

        // 检查任务是否属于该玩家
        if (!task.getOwnerId().equals(player.getUniqueId())) {
            return false;
        }

        // 检查任务是否已完成
        if (task.getStatus() != SectTask.TaskStatus.IN_PROGRESS) {
            return false;
        }

        // 检查玩家背包中是否有足够的物品
        String materialName = task.getTarget();
        int requiredAmount = task.getTargetAmount() - task.getCurrentProgress();
        
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            int playerAmount = 0;
            
            // 计算玩家背包中的物品数量
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    playerAmount += item.getAmount();
                }
            }
            
            if (playerAmount < requiredAmount) {
                player.sendMessage("§c物品不足! 需要: " + requiredAmount + " 个 " + materialName + "，你有: " + playerAmount + " 个");
                return false;
            }
            
            // 扣除物品
            int remainingToRemove = requiredAmount;
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material && remainingToRemove > 0) {
                    int removeAmount = Math.min(item.getAmount(), remainingToRemove);
                    item.setAmount(item.getAmount() - removeAmount);
                    remainingToRemove -= removeAmount;
                    
                    if (item.getAmount() <= 0) {
                        item.setType(Material.AIR);
                    }
                }
            }
            
            // 完成任务
            task.setProgress(task.getTargetAmount());
            task.setStatus(SectTask.TaskStatus.COMPLETED);
            
            player.sendMessage("§a任务完成! 已提交 " + requiredAmount + " 个 " + materialName);
            return true;
            
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c无效的物品类型: " + materialName);
            return false;
        }
    }

    /**
     * 重载任务配置
     */
    public void reloadTaskConfig() {
        try {
            taskGenerator.loadTaskTemplates();
            plugin.getLogger().info("§a任务配置已重载");
        } catch (Exception e) {
            plugin.getLogger().severe("§c重载任务配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证任务配置
     *
     * @param player 执行验证的玩家（用于发送反馈消息）
     */
    public void validateTaskConfig(Player player) {
        try {
            // 这里可以添加配置验证逻辑
            // 目前简单地尝试重载配置来验证
            taskGenerator.loadTaskTemplates();
            
            player.sendMessage("§a任务配置验证通过");
            plugin.getLogger().info("§a任务配置验证通过");
            
        } catch (Exception e) {
            player.sendMessage("§c任务配置验证失败: " + e.getMessage());
            plugin.getLogger().severe("§c任务配置验证失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 带保护机制的任务刷新
     *
     * @param type 任务类型
     * @param protectHighProgress 是否保护高进度任务
     * @param progressThreshold 进度阈值
     * @param protectCompleted 是否保护已完成任务
     * @param enableCompensation 是否启用补偿
     * @param compensationRate 补偿率
     * @return 刷新的玩家数量
     */
    public int refreshTasksWithProtection(SectTaskType type, boolean protectHighProgress, 
                                        int progressThreshold, boolean protectCompleted, 
                                        boolean enableCompensation, double compensationRate) {
        int refreshedPlayers = 0;
        
        for (UUID playerId : playerTasks.keySet()) {
            List<SectTask> tasks = getPlayerTasksByType(playerId, type);
            boolean needsRefresh = false;
            
            for (SectTask task : tasks) {
                // 检查是否需要保护
                if (protectCompleted && task.isCompleted()) {
                    continue;
                }
                
                if (protectHighProgress && task.getProgressPercentage() >= progressThreshold) {
                    continue;
                }
                
                needsRefresh = true;
                break;
            }
            
            if (needsRefresh) {
                // 如果启用补偿，给予一些奖励
                if (enableCompensation) {
                    org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
                    if (player != null) {
                        com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
                        if (data != null) {
                            int compensation = (int) (50 * compensationRate); // 基础补偿50灵石
                            data.addSpiritStones(compensation);
                            plugin.getDataManager().savePlayerData(data);
                            player.sendMessage("§e任务刷新补偿: +" + compensation + " 灵石");
                        }
                    }
                }
                
                // 刷新任务
                refreshTasks(type);
                refreshedPlayers++;
            }
        }
        
        return refreshedPlayers;
    }

    /**
     * 获取任务调度器
     */
    public TaskRefreshScheduler getTaskScheduler() {
        // 这里应该返回任务调度器实例
        // 需要在 SectTaskManager 中添加调度器字段
        return null; // 临时返回null，需要在构造函数中初始化
    }

    /**
     * 关闭任务管理器
     */
    public void shutdown() {
        try {
            // 保存所有任务数据
            for (SectTask task : taskIndex.values()) {
                saveTask(task);
            }
            
            // 清理缓存
            playerTasks.clear();
            taskIndex.clear();
            
            plugin.getLogger().info("  §a✓ 宗门任务管理器已关闭");
            
        } catch (Exception e) {
            plugin.getLogger().severe("  §c✗ 宗门任务管理器关闭时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
