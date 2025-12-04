package com.xiancore.systems.sect.task;

import com.xiancore.XianCore;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务进度跟踪器
 * 监听游戏事件并更新任务进度
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TaskProgressTracker implements Listener {

    private final XianCore plugin;
    private final SectTaskManager taskManager;

    // 在线时长跟踪
    private final Map<UUID, Long> onlineTime;
    private final Map<UUID, Long> loginTime;

    public TaskProgressTracker(XianCore plugin, SectTaskManager taskManager) {
        this.plugin = plugin;
        this.taskManager = taskManager;
        this.onlineTime = new ConcurrentHashMap<>();
        this.loginTime = new ConcurrentHashMap<>();

        // 启动在线时长定时器（每分钟检查一次）
        startOnlineTimeTracker();
    }

    // ==================== 击杀怪物 ====================

    /**
     * 监听原版怪物击杀
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // 检查击杀者是否为玩家
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }

        Player killer = (Player) event.getEntity().getKiller();
        UUID playerId = killer.getUniqueId();

        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 获取怪物类型
        EntityType entityType = event.getEntityType();
        String mobName = getMobDisplayName(entityType);

        // 更新 KILL_MOB_TYPE 任务进度
        taskManager.updateProgress(playerId, TaskObjective.KILL_MOB_TYPE, mobName, 1);
    }

    /**
     * 监听 MythicMobs 怪物击杀
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        // 检查击杀者是否为玩家
        if (!(event.getKiller() instanceof Player)) {
            return;
        }

        Player killer = (Player) event.getKiller();
        UUID playerId = killer.getUniqueId();

        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 获取 MythicMob 的内部名称
        String mobInternalName = event.getMobType().getInternalName();

        // 更新 KILL_MOB 任务进度（用于特殊任务，如击杀BOSS）
        taskManager.updateProgress(playerId, TaskObjective.KILL_MOB, mobInternalName, 1);
    }

    // ==================== 收集物品 ====================

    /**
     * 跟踪玩家物品收集
     * 通过定时检查背包实现
     */
    public void trackItemCollection(Player player, Material material, int amount) {
        UUID playerId = player.getUniqueId();

        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新 COLLECT_ITEM 任务进度
        taskManager.updateProgress(playerId, TaskObjective.COLLECT_ITEM, material.name(), amount);
    }

    // ==================== 在线时长 ====================

    /**
     * 玩家登录时记录时间
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 记录登录时间
        loginTime.put(playerId, System.currentTimeMillis());

        // 初始化在线时长
        onlineTime.putIfAbsent(playerId, 0L);

        // 初始化玩家的任务
        if (taskManager != null) {
            taskManager.onPlayerLogin(player);
        }
    }

    /**
     * 玩家退出时保存在线时长
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        // 计算本次在线时长
        Long login = loginTime.remove(playerId);
        if (login != null) {
            long sessionTime = (System.currentTimeMillis() - login) / 1000 / 60; // 转换为分钟
            long totalTime = onlineTime.getOrDefault(playerId, 0L) + sessionTime;
            onlineTime.put(playerId, totalTime);
        }
    }

    /**
     * 启动在线时长定时器
     */
    private void startOnlineTimeTracker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID playerId = player.getUniqueId();

                    // 检查玩家是否在宗门
                    if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
                        continue;
                    }

                    // 计算本次检查周期的在线时长（1分钟）
                    Long login = loginTime.get(playerId);
                    if (login != null) {
                        long minutes = (currentTime - login) / 1000 / 60;

                        if (minutes >= 1) {
                            // 更新在线时长任务进度（每分钟）
                            taskManager.updateProgress(playerId, TaskObjective.ONLINE_TIME, null, 1);

                            // 重置登录时间为当前时间（避免重复计算）
                            loginTime.put(playerId, currentTime);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * 60, 20L * 60); // 每60秒运行一次
    }

    // ==================== 修炼 ====================

    /**
     * 跟踪修炼时长
     * 由 CultivationSystem 调用
     */
    public void trackCultivation(UUID playerId, int minutes) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新修炼任务进度
        taskManager.updateProgress(playerId, TaskObjective.CULTIVATE, null, minutes);
    }

    // ==================== 捐献灵石 ====================

    /**
     * 跟踪灵石捐献
     * 由 SectSystem 调用
     */
    public void trackDonation(UUID playerId, int amount) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新捐献任务进度
        taskManager.updateProgress(playerId, TaskObjective.DONATE_SPIRIT_STONE, null, amount);
    }

    // ==================== 境界突破 ====================

    /**
     * 跟踪境界突破
     * 由 CultivationSystem 调用
     */
    public void trackBreakthrough(UUID playerId) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新突破任务进度
        taskManager.updateProgress(playerId, TaskObjective.BREAKTHROUGH, null, 1);
    }

    // ==================== 炼制装备 ====================

    /**
     * 跟踪装备炼制
     * 由 ForgeSystem 调用
     */
    public void trackForge(UUID playerId, int count) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新炼制任务进度
        taskManager.updateProgress(playerId, TaskObjective.FORGE_EQUIPMENT, null, count);
    }

    // ==================== 使用功法 ====================

    /**
     * 跟踪功法使用
     * 由 SkillSystem 调用
     */
    public void trackSkillUse(UUID playerId) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新使用功法任务进度
        taskManager.updateProgress(playerId, TaskObjective.USE_SKILL, null, 1);
    }

    // ==================== 宗门活动 ====================

    /**
     * 跟踪宗门活动参与
     * 由 SectSystem 调用
     */
    public void trackSectEvent(UUID playerId) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新宗门活动任务进度
        taskManager.updateProgress(playerId, TaskObjective.ATTEND_EVENT, null, 1);
    }

    // ==================== 完成任务 ====================

    /**
     * 跟踪 Quests 任务完成
     * 由 QuestsIntegrationListener 调用
     */
    public void trackQuestComplete(UUID playerId) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新完成任务进度
        taskManager.updateProgress(playerId, TaskObjective.COMPLETE_QUEST, null, 1);
    }

    // ==================== 组队副本 ====================

    /**
     * 跟踪组队副本完成
     * 由副本系统调用（如果实现）
     */
    public void trackTeamDungeon(UUID playerId) {
        // 检查玩家是否在宗门
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            return;
        }

        // 更新组队副本任务进度
        taskManager.updateProgress(playerId, TaskObjective.TEAM_DUNGEON, null, 1);
    }

    // ==================== 工具方法 ====================

    /**
     * 获取怪物显示名称（中文）
     */
    private String getMobDisplayName(EntityType type) {
        return switch (type) {
            case ZOMBIE -> "僵尸";
            case SKELETON -> "骷髅";
            case SPIDER -> "蜘蛛";
            case CREEPER -> "苦力怕";
            case ENDERMAN -> "末影人";
            case WITCH -> "女巫";
            case BLAZE -> "烈焰人";
            case GHAST -> "恶魂";
            case SLIME -> "史莱姆";
            case MAGMA_CUBE -> "岩浆怪";
            case WITHER_SKELETON -> "凋灵骷髅";
            case PIGLIN -> "猪灵";
            case HOGLIN -> "疣猪兽";
            case ZOGLIN -> "僵尸疣猪兽";
            case PIGLIN_BRUTE -> "猪灵蛮兵";
            case EVOKER -> "唤魔者";
            case VINDICATOR -> "卫道士";
            case PILLAGER -> "掠夺者";
            case RAVAGER -> "劫掠兽";
            case VEX -> "恼鬼";
            case GUARDIAN -> "守卫者";
            case ELDER_GUARDIAN -> "远古守卫者";
            case SHULKER -> "潜影贝";
            case PHANTOM -> "幻翼";
            case DROWNED -> "溺尸";
            case HUSK -> "尸壳";
            case STRAY -> "流浪者";
            case SILVERFISH -> "蠹虫";
            case CAVE_SPIDER -> "洞穴蜘蛛";
            default -> type.name();
        };
    }

    /**
     * 获取玩家在线时长（分钟）
     */
    public long getOnlineTime(UUID playerId) {
        long baseTime = onlineTime.getOrDefault(playerId, 0L);
        Long login = loginTime.get(playerId);
        if (login != null) {
            long currentSession = (System.currentTimeMillis() - login) / 1000 / 60;
            return baseTime + currentSession;
        }
        return baseTime;
    }
}
