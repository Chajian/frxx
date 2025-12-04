package com.xiancore.systems.boss.announcement;

import com.xiancore.systems.boss.lifecycle.BossLifecycleData;
import com.xiancore.systems.boss.permission.BossPermission;
import com.xiancore.systems.boss.permission.BossPermissionManager;
import com.xiancore.boss.system.model.BossTier;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Boss全服公告管理器
 * 管理所有Boss事件的公告发送
 *
 * 职责:
 * - 创建和发送公告
 * - 管理公告模板
 * - 与BossLifecycleManager集成
 * - 异步处理和队列管理
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
public class BossAnnouncementManager {

    // ==================== 常量 ====================

    private static final long DEFAULT_SPAWN_COOLDOWN = 120;  // 120秒
    private static final long DEFAULT_KILLED_COOLDOWN = 60;  // 60秒
    private static final long DEFAULT_RAREDROP_COOLDOWN = 30; // 30秒
    private static final long DEFAULT_MILESTONE_COOLDOWN = 300; // 300秒

    // ==================== 内部状态 ====================

    /** 格式化器 */
    private final AnnouncementFormatter formatter;

    /** 调度器 */
    private final AnnouncementScheduler scheduler;

    /** 最近公告历史 (用于聚合) */
    private final Deque<BossAnnouncement> recentAnnouncements;

    /** 模板库 */
    private final Map<BossAnnouncement.AnnouncementType, String> templates;

    /** 冷却时间配置 */
    private final Map<BossAnnouncement.AnnouncementType, Long> cooldownConfig;

    /** 是否已初始化 */
    private volatile boolean initialized;

    /** 是否启用 */
    private volatile boolean enabled;

    /** 权限管理器 */
    private BossPermissionManager permissionManager;

    /** 最近公告的最大数量 */
    private static final int MAX_RECENT_ANNOUNCEMENTS = 50;

    // ==================== 构造函数 ====================

    public BossAnnouncementManager() {
        this.formatter = new AnnouncementFormatter();
        this.scheduler = new AnnouncementScheduler();
        this.recentAnnouncements = new ConcurrentLinkedDeque<>();
        this.templates = new ConcurrentHashMap<>();
        this.cooldownConfig = new ConcurrentHashMap<>();
        this.permissionManager = null;
        this.initialized = false;
        this.enabled = false;

        // 初始化冷却配置
        cooldownConfig.put(BossAnnouncement.AnnouncementType.SPAWN, DEFAULT_SPAWN_COOLDOWN);
        cooldownConfig.put(BossAnnouncement.AnnouncementType.KILLED, DEFAULT_KILLED_COOLDOWN);
        cooldownConfig.put(BossAnnouncement.AnnouncementType.RARE_DROP, DEFAULT_RAREDROP_COOLDOWN);
        cooldownConfig.put(BossAnnouncement.AnnouncementType.LEVEL_UP, DEFAULT_MILESTONE_COOLDOWN);
    }

    // ==================== 生命周期 ====================

    /**
     * 初始化管理器
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            // 启动调度器
            scheduler.start();
            enabled = true;
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭管理器
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        try {
            enabled = false;
            scheduler.shutdown();
            recentAnnouncements.clear();
            templates.clear();
            initialized = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== Boss事件公告 ====================

    /**
     * 公告Boss生成
     *
     * @param bossData Boss生命周期数据
     * @param location Boss生成位置
     * @param participantCount 参与人数
     */
    public void announceBossSpawn(BossLifecycleData bossData, Location location, int participantCount) {
        if (!enabled || bossData == null) {
            return;
        }

        try {
            BossAnnouncement announcement = new BossAnnouncement(
                bossData.getBossUUID(),
                bossData.getBossType(),
                bossData.getBossType(),
                bossData.getBossTier(),
                location,
                participantCount
            );

            announcement.setType(BossAnnouncement.AnnouncementType.SPAWN);
            announcement.setCooldownSeconds(cooldownConfig.get(BossAnnouncement.AnnouncementType.SPAWN));

            // 设置模板参数
            if (location != null) {
                announcement.setParam("world", location.getWorld().getName());
                announcement.setParam("x", String.valueOf(location.getBlockX()));
                announcement.setParam("y", String.valueOf(location.getBlockY()));
                announcement.setParam("z", String.valueOf(location.getBlockZ()));
            }
            announcement.setParam("boss_name", bossData.getBossType());
            announcement.setParam("boss_tier", String.valueOf(bossData.getBossTier().getTier()));
            announcement.setParam("player_count", String.valueOf(participantCount));

            // 设置模板
            String template = templates.get(BossAnnouncement.AnnouncementType.SPAWN);
            if (template != null) {
                announcement.setTemplate(template);
            }

            // 加入队列
            scheduler.enqueue(announcement);
            recordAnnouncement(announcement);

            // 添加冷却
            scheduler.addCooldown(bossData.getBossUUID(), cooldownConfig.get(BossAnnouncement.AnnouncementType.SPAWN));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 公告Boss击杀
     *
     * @param bossData Boss生命周期数据
     * @param killerUUID 击杀者UUID
     * @param killerName 击杀者名称
     * @param qualityScore 品质分数
     * @param qualityLevel 品质等级
     * @param experienceReward 经验奖励
     * @param duration 战斗时长 (毫秒)
     * @param deathCount 死亡人数
     */
    public void announceBossKilled(BossLifecycleData bossData, UUID killerUUID, String killerName,
                                   int qualityScore, String qualityLevel, double experienceReward,
                                   long duration, int deathCount) {
        if (!enabled || bossData == null) {
            return;
        }

        try {
            BossAnnouncement announcement = new BossAnnouncement(
                bossData.getBossUUID(),
                bossData.getBossType(),
                killerUUID,
                killerName,
                qualityScore,
                qualityLevel,
                experienceReward,
                duration,
                deathCount
            );

            announcement.setType(BossAnnouncement.AnnouncementType.KILLED);
            announcement.setPriority(BossAnnouncement.AnnouncementPriority.CRITICAL);
            announcement.setCooldownSeconds(cooldownConfig.get(BossAnnouncement.AnnouncementType.KILLED));

            // 设置模板参数
            announcement.setParam("boss_name", bossData.getBossType());
            announcement.setParam("killer", killerName);
            announcement.setParam("quality", qualityLevel);
            announcement.setParam("quality_score", String.valueOf(qualityScore));
            announcement.setParam("exp_reward", String.format("%.0f", experienceReward));
            announcement.setParam("duration", String.valueOf(duration / 1000));

            // 设置模板
            String template = templates.get(BossAnnouncement.AnnouncementType.KILLED);
            if (template != null) {
                announcement.setTemplate(template);
            }

            // 加入队列
            scheduler.enqueue(announcement);
            recordAnnouncement(announcement);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 公告稀有掉落
     *
     * @param bossUUID Boss UUID
     * @param dropName 掉落物品名称
     * @param luckyPlayerUUID 幸运玩家UUID
     * @param luckyPlayerName 幸运玩家名称
     * @param rarity 稀有度
     */
    public void announceRareDrop(UUID bossUUID, String dropName, UUID luckyPlayerUUID,
                                String luckyPlayerName, double rarity) {
        if (!enabled || bossUUID == null) {
            return;
        }

        try {
            BossAnnouncement announcement = new BossAnnouncement();
            announcement.setType(BossAnnouncement.AnnouncementType.RARE_DROP);
            announcement.setPriority(BossAnnouncement.AnnouncementPriority.HIGH);
            announcement.setBossUUID(bossUUID);
            announcement.setRareDropName(dropName);
            announcement.setLuckyPlayer(luckyPlayerUUID);
            announcement.setLuckyPlayerName(luckyPlayerName);
            announcement.setRarity(rarity);
            announcement.setCooldownSeconds(cooldownConfig.get(BossAnnouncement.AnnouncementType.RARE_DROP));

            // 设置模板参数
            announcement.setParam("drop_name", dropName);
            announcement.setParam("lucky_player", luckyPlayerName);
            announcement.setParam("rarity", String.format("%.2f%%", rarity * 100));

            // 设置模板
            String template = templates.get(BossAnnouncement.AnnouncementType.RARE_DROP);
            if (template != null) {
                announcement.setTemplate(template);
            }

            scheduler.enqueue(announcement);
            recordAnnouncement(announcement);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 公告击杀里程碑
     *
     * @param bossType Boss类型
     * @param killCount 击杀次数
     */
    public void announceKillCountMilestone(String bossType, int killCount) {
        if (!enabled || bossType == null || killCount <= 0) {
            return;
        }

        try {
            BossAnnouncement announcement = new BossAnnouncement();
            announcement.setType(BossAnnouncement.AnnouncementType.LEVEL_UP);
            announcement.setPriority(BossAnnouncement.AnnouncementPriority.HIGH);
            announcement.setBossType(bossType);
            announcement.setKillCount(killCount);
            announcement.setMilestoneDescription(String.format("%s已被击杀%d次", bossType, killCount));
            announcement.setCooldownSeconds(cooldownConfig.get(BossAnnouncement.AnnouncementType.LEVEL_UP));

            // 设置模板参数
            announcement.setParam("boss_type", bossType);
            announcement.setParam("kill_count", String.valueOf(killCount));

            // 设置模板
            String template = templates.get(BossAnnouncement.AnnouncementType.LEVEL_UP);
            if (template != null) {
                announcement.setTemplate(template);
            }

            scheduler.enqueue(announcement);
            recordAnnouncement(announcement);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 通用公告方法 ====================

    /**
     * 发送公告给全服所有玩家
     *
     * @param message 消息
     * @param priority 优先级
     */
    public void announceToAll(String message, BossAnnouncement.AnnouncementPriority priority) {
        if (!enabled || message == null || message.isEmpty()) {
            return;
        }

        try {
            // 应用颜色
            String formatted = formatter.applyColors(message);

            // 发送给所有在线玩家
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(formatted);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送公告给附近的玩家
     *
     * @param location 位置
     * @param message 消息
     * @param radius 半径
     */
    public void announceToNearby(Location location, String message, double radius) {
        if (!enabled || location == null || message == null || message.isEmpty()) {
            return;
        }

        try {
            String formatted = formatter.applyColors(message);

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(location.getWorld())) {
                    if (player.getLocation().distance(location) <= radius) {
                        player.sendMessage(formatted);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送公告给指定玩家
     *
     * @param players 玩家列表
     * @param message 消息
     */
    public void announceToPlayers(List<Player> players, String message) {
        if (!enabled || players == null || players.isEmpty() || message == null || message.isEmpty()) {
            return;
        }

        try {
            String formatted = formatter.applyColors(message);

            for (Player player : players) {
                if (player.isOnline()) {
                    player.sendMessage(formatted);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 模板管理 ====================

    /**
     * 设置公告模板
     *
     * @param type 公告类型
     * @param template 模板文本
     */
    public void setTemplate(BossAnnouncement.AnnouncementType type, String template) {
        if (type != null && template != null) {
            templates.put(type, template);
        }
    }

    /**
     * 获取模板
     *
     * @param type 公告类型
     * @return 模板文本
     */
    public String getTemplate(BossAnnouncement.AnnouncementType type) {
        return templates.get(type);
    }

    // ==================== 冷却管理 ====================

    /**
     * 设置冷却时间
     *
     * @param type 公告类型
     * @param cooldownSeconds 冷却秒数
     */
    public void setCooldown(BossAnnouncement.AnnouncementType type, long cooldownSeconds) {
        if (type != null && cooldownSeconds > 0) {
            cooldownConfig.put(type, cooldownSeconds);
        }
    }

    /**
     * 获取冷却时间
     *
     * @param type 公告类型
     * @return 冷却秒数
     */
    public long getCooldown(BossAnnouncement.AnnouncementType type) {
        return cooldownConfig.getOrDefault(type, 60L);
    }

    // ==================== 内部方法 ====================

    /**
     * 记录公告到历史
     */
    private void recordAnnouncement(BossAnnouncement announcement) {
        recentAnnouncements.addFirst(announcement);

        // 限制历史大小
        while (recentAnnouncements.size() > MAX_RECENT_ANNOUNCEMENTS) {
            recentAnnouncements.removeLast();
        }
    }

    // ==================== 查询方法 ====================

    /**
     * 获取最近的公告
     *
     * @param count 数量
     * @return 公告列表
     */
    public List<BossAnnouncement> getRecentAnnouncements(int count) {
        List<BossAnnouncement> result = new ArrayList<>();
        int i = 0;
        for (BossAnnouncement announcement : recentAnnouncements) {
            if (i >= count) break;
            result.add(announcement);
            i++;
        }
        return result;
    }

    /**
     * 获取指定类型的最近公告
     *
     * @param type 公告类型
     * @return 公告列表
     */
    public List<BossAnnouncement> getAnnouncementsByType(BossAnnouncement.AnnouncementType type) {
        List<BossAnnouncement> result = new ArrayList<>();
        for (BossAnnouncement announcement : recentAnnouncements) {
            if (announcement.getType() == type) {
                result.add(announcement);
            }
        }
        return result;
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        return String.format(
            "公告系统: 最近公告%d个, 队列%d个, %s",
            recentAnnouncements.size(),
            scheduler.getQueueSize(),
            scheduler.getStatistics()
        );
    }

    // ==================== 权限管理 ====================

    /**
     * 设置权限管理器
     *
     * @param permissionManager 权限管理器
     */
    public void setPermissionManager(BossPermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    /**
     * 获取所有有权限接收公告的在线玩家
     *
     * @return 玩家列表
     */
    public List<Player> getPlayersWithAnnouncePermission() {
        List<Player> players = new ArrayList<>();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 如果没有权限管理器，或者玩家有公告权限，则添加到列表
            if (permissionManager == null || 
                permissionManager.hasPermission(player, BossPermission.ANNOUNCE)) {
                players.add(player);
            }
        }
        
        return players;
    }

    /**
     * 检查玩家是否有接收公告权限
     *
     * @param player 玩家
     * @return 是否有权限
     */
    public boolean canReceiveAnnouncement(Player player) {
        if (player == null) {
            return false;
        }
        
        // 如果没有权限管理器，默认所有人都能接收
        if (permissionManager == null) {
            return true;
        }
        
        return permissionManager.hasPermission(player, BossPermission.ANNOUNCE);
    }
}
