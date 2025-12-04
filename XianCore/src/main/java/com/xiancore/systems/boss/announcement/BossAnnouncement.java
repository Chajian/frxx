package com.xiancore.systems.boss.announcement;

import com.xiancore.boss.system.model.BossTier;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.*;

/**
 * Boss公告数据类
 * 表示一个单个的Boss事件公告
 *
 * 支持以下公告类型:
 * - SPAWN: Boss生成
 * - KILLED: Boss被击杀
 * - RARE_DROP: 稀有掉落
 * - LEVEL_UP: 击杀次数增加 (里程碑)
 * - PERIODIC: 周期性提醒
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
@Setter
public class BossAnnouncement {

    // ==================== 公告类型定义 ====================

    /**
     * 公告类型枚举
     */
    public enum AnnouncementType {
        SPAWN(1),           // Boss生成 (优先级1, 最低)
        PERIODIC(2),        // 周期提醒 (优先级2)
        RARE_DROP(3),       // 稀有掉落 (优先级3)
        LEVEL_UP(4),        // 击杀里程碑 (优先级4)
        KILLED(5);          // Boss击杀 (优先级5, 最高)

        public final int priority;

        AnnouncementType(int priority) {
            this.priority = priority;
        }
    }

    /**
     * 公告优先级枚举
     */
    public enum AnnouncementPriority {
        LOW(1),        // 低优先级
        NORMAL(2),     // 普通
        HIGH(3),       // 高优先级
        CRITICAL(4);   // 紧急

        public final int level;

        AnnouncementPriority(int level) {
            this.level = level;
        }
    }

    // ==================== 基本信息 ====================

    /** 公告唯一ID */
    private UUID announcementId;

    /** 公告类型 */
    private AnnouncementType type;

    /** 公告优先级 */
    private AnnouncementPriority priority;

    /** 创建时间 (毫秒) */
    private long createdTime;

    // ==================== Boss信息 ====================

    /** Boss UUID */
    private UUID bossUUID;

    /** Boss名称 */
    private String bossName;

    /** Boss类型 */
    private String bossType;

    /** Boss等级 */
    private BossTier bossTier;

    /** Boss位置 */
    private Location location;

    // ==================== 事件信息 ====================

    /** 触发玩家UUID (击杀者或发现者) */
    private UUID triggerPlayer;

    /** 触发玩家名称 */
    private String triggerPlayerName;

    /** 战斗时长 (毫秒) */
    private long duration;

    /** 参与人数 */
    private int participantCount;

    /** 死亡人数 */
    private int deathCount;

    /** 品质分数 (0-100) */
    private int qualityScore;

    /** 品质等级 */
    private String qualityLevel;

    /** 经验奖励 */
    private double experienceReward;

    // ==================== 稀有掉落信息 ====================

    /** 稀有掉落名称 */
    private String rareDropName;

    /** 稀有掉落稀有度 */
    private double rarity;

    /** 获得掉落的玩家 */
    private UUID luckyPlayer;

    private String luckyPlayerName;

    // ==================== 里程碑信息 ====================

    /** 击杀次数 */
    private int killCount;

    /** 里程碑描述 */
    private String milestoneDescription;

    // ==================== 模板信息 ====================

    /** 使用的模板 */
    private String template;

    /** 模板参数映射 */
    private Map<String, String> params;

    // ==================== 聚合信息 ====================

    /** 是否已聚合 */
    private boolean aggregated;

    /** 相关的其他公告 */
    private List<UUID> relatedAnnouncementIds;

    // ==================== 冷却信息 ====================

    /** 上次公告时间 */
    private long lastAnnouncedTime;

    /** 冷却秒数 */
    private long cooldownSeconds;

    /** 是否在冷却中 */
    private boolean inCooldown;

    // ==================== 显示信息 ====================

    /** 是否已发送 */
    private boolean sent;

    /** 发送时间 */
    private long sentTime;

    /** 发送次数 */
    private int sendCount;

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public BossAnnouncement() {
        this.announcementId = UUID.randomUUID();
        this.createdTime = System.currentTimeMillis();
        this.sent = false;
        this.sendCount = 0;
        this.aggregated = false;
        this.inCooldown = false;
        this.params = new HashMap<>();
        this.relatedAnnouncementIds = new ArrayList<>();
        this.priority = AnnouncementPriority.NORMAL;
    }

    /**
     * Boss生成公告构造函数
     */
    public BossAnnouncement(UUID bossUUID, String bossName, String bossType,
                           BossTier bossTier, Location location, int participantCount) {
        this();
        this.bossUUID = bossUUID;
        this.bossName = bossName;
        this.bossType = bossType;
        this.bossTier = bossTier;
        this.location = location;
        this.participantCount = participantCount;
        this.type = AnnouncementType.SPAWN;
        setPriorityByTier(bossTier);
    }

    /**
     * Boss击杀公告构造函数
     */
    public BossAnnouncement(UUID bossUUID, String bossName, UUID killerUUID,
                           String killerName, int qualityScore, String qualityLevel,
                           double experienceReward, long duration, int deathCount) {
        this();
        this.bossUUID = bossUUID;
        this.bossName = bossName;
        this.triggerPlayer = killerUUID;
        this.triggerPlayerName = killerName;
        this.qualityScore = qualityScore;
        this.qualityLevel = qualityLevel;
        this.experienceReward = experienceReward;
        this.duration = duration;
        this.deathCount = deathCount;
        this.type = AnnouncementType.KILLED;
        this.priority = AnnouncementPriority.CRITICAL;
    }

    // ==================== 业务方法 ====================

    /**
     * 根据Boss等级设置优先级
     */
    public void setPriorityByTier(BossTier tier) {
        if (tier == null) {
            this.priority = AnnouncementPriority.NORMAL;
            return;
        }

        switch (tier.getLevel()) {
            case 4 -> this.priority = AnnouncementPriority.CRITICAL; // 传奇
            case 3 -> this.priority = AnnouncementPriority.HIGH;     // Boss
            case 2 -> this.priority = AnnouncementPriority.NORMAL;   // Elite
            default -> this.priority = AnnouncementPriority.LOW;     // Normal
        }
    }

    /**
     * 获取公告的综合优先级 (类型 + 用户优先级)
     * 用于队列排序
     */
    public int getCombinedPriority() {
        return type.priority * 10 + priority.level;
    }

    /**
     * 判断是否在冷却期内
     */
    public boolean isInCooldown() {
        if (!inCooldown) {
            return false;
        }

        long timePassed = System.currentTimeMillis() - lastAnnouncedTime;
        return timePassed < (cooldownSeconds * 1000);
    }

    /**
     * 获取冷却剩余时间 (秒)
     */
    public long getRemainingCooldown() {
        if (!inCooldown) {
            return 0;
        }

        long timePassed = System.currentTimeMillis() - lastAnnouncedTime;
        long remaining = cooldownSeconds - (timePassed / 1000);
        return Math.max(0, remaining);
    }

    /**
     * 记录公告已发送
     */
    public void markAsSent() {
        this.sent = true;
        this.sentTime = System.currentTimeMillis();
        this.sendCount++;
        this.lastAnnouncedTime = System.currentTimeMillis();
        this.inCooldown = true;
    }

    /**
     * 是否应该聚合这两个公告
     * 聚合规则: 同一Boss的多个公告在短时间内合并
     */
    public boolean shouldAggregatWith(BossAnnouncement other) {
        if (other == null) {
            return false;
        }

        // 同一Boss
        if (!this.bossUUID.equals(other.bossUUID)) {
            return false;
        }

        // 时间在2秒内
        long timeDiff = Math.abs(this.createdTime - other.createdTime);
        if (timeDiff > 2000) {
            return false;
        }

        // 相同类型的公告可以聚合
        return this.type == other.type;
    }

    /**
     * 添加相关公告
     */
    public void addRelatedAnnouncement(UUID announcementId) {
        if (!relatedAnnouncementIds.contains(announcementId)) {
            relatedAnnouncementIds.add(announcementId);
        }
    }

    /**
     * 设置模板参数
     */
    public void setParam(String key, String value) {
        params.put(key, value);
    }

    /**
     * 批量设置参数
     */
    public void setParams(Map<String, String> parameters) {
        if (parameters != null) {
            params.putAll(parameters);
        }
    }

    /**
     * 获取所有参数
     */
    public Map<String, String> getAllParams() {
        return new HashMap<>(params);
    }

    /**
     * 获取公告的完整描述信息
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 公告详情 ===\n");
        sb.append(String.format("类型: %s\n", type.name()));
        sb.append(String.format("优先级: %s\n", priority.name()));
        sb.append(String.format("Boss: %s (等级%s)\n", bossName, bossTier != null ? bossTier.getTier() : "?"));

        if (type == AnnouncementType.KILLED) {
            sb.append(String.format("击杀者: %s\n", triggerPlayerName));
            sb.append(String.format("品质: %s (%d分)\n", qualityLevel, qualityScore));
            sb.append(String.format("战斗时长: %d秒\n", duration / 1000));
        } else if (type == AnnouncementType.RARE_DROP) {
            sb.append(String.format("掉落: %s\n", rareDropName));
            sb.append(String.format("获得者: %s\n", luckyPlayerName));
            sb.append(String.format("稀有度: %.2f%%\n", rarity * 100));
        } else if (type == AnnouncementType.SPAWN) {
            sb.append(String.format("位置: %s\n", location != null ?
                String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ())
                : "未知"));
            sb.append(String.format("参与人数: %d\n", participantCount));
        }

        sb.append(String.format("发送状态: %s\n", sent ? "已发送" : "未发送"));
        if (sent) {
            sb.append(String.format("发送次数: %d\n", sendCount));
        }

        return sb.toString();
    }

    /**
     * 转换为Map格式 (用于序列化)
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("announcementId", announcementId.toString());
        map.put("type", type.name());
        map.put("priority", priority.name());
        map.put("createdTime", createdTime);

        map.put("bossUUID", bossUUID != null ? bossUUID.toString() : null);
        map.put("bossName", bossName);
        map.put("bossType", bossType);
        map.put("bossTier", bossTier != null ? bossTier.name() : null);

        map.put("triggerPlayerName", triggerPlayerName);
        map.put("qualityLevel", qualityLevel);
        map.put("qualityScore", qualityScore);

        map.put("rareDropName", rareDropName);
        map.put("luckyPlayerName", luckyPlayerName);

        map.put("sent", sent);
        map.put("sendCount", sendCount);

        return map;
    }
}
