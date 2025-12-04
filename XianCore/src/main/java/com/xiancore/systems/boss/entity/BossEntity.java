package com.xiancore.systems.boss.entity;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss实体包装类
 * 代表一个活跃的Boss怪物，管理其属性、伤害记录、生命周期
 *
 * 职责:
 * - 封装Bukkit的LivingEntity
 * - 记录伤害数据
 * - 管理Boss生命周期
 * - 提供属性查询
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
public class BossEntity {

    // ==================== Boss身份信息 ====================
    /** Boss UUID (唯一标识) */
    private final UUID bossUUID;

    /** Boss ID (数据库记录ID，可能为null) */
    private String bossId;

    /** MythicMobs类型 (如 "SkeletonKing") */
    private final String mythicMobType;

    /** Boss等级 (1-4) */
    private final int tier;

    // ==================== 实体信息 ====================
    /** Bukkit实体引用 */
    private final LivingEntity bukkitEntity;

    /** Boss生成时的位置 */
    private final Location spawnLocation;

    /** Boss生成时间 (毫秒时间戳) */
    private final long spawnTime;

    // ==================== 生命周期信息 ====================
    /** 最后伤害时间 */
    private volatile long lastDamageTime;

    /** Boss当前状态 */
    private volatile BossStatus status = BossStatus.ACTIVE;

    /** 最后击伤害的玩家 (可能为null) */
    private volatile Player lastDamager;

    /** 击杀者 (可能为null) */
    private volatile Player killer;

    /** 消失时间 (仅当状态为DEAD或DESPAWNED时有效) */
    private volatile long deathTime;

    // ==================== 伤害数据 ====================
    /** 玩家伤害记录 (UUID -> 伤害值) */
    private final Map<UUID, Double> playerDamage = new ConcurrentHashMap<>();

    /** 总伤害 */
    private volatile double totalDamage = 0.0;

    // ==================== 其他信息 ====================
    /** Boss显示名称 */
    private String displayName;

    /** 额外信息 (可扩展) */
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     *
     * @param bossUUID Boss UUID
     * @param bukkitEntity Bukkit实体
     * @param mythicMobType MythicMobs类型
     * @param tier Boss等级
     * @param spawnLocation 生成位置
     * @param spawnTime 生成时间
     */
    public BossEntity(UUID bossUUID, LivingEntity bukkitEntity, String mythicMobType,
                      int tier, Location spawnLocation, long spawnTime) {
        this.bossUUID = bossUUID;
        this.bukkitEntity = bukkitEntity;
        this.mythicMobType = mythicMobType;
        this.tier = tier;
        this.spawnLocation = spawnLocation.clone();
        this.spawnTime = spawnTime;
        this.lastDamageTime = spawnTime;
        this.displayName = bukkitEntity.getName();
    }

    /**
     * 获取Boss UUID
     */
    public UUID getBossUUID() {
        return bossUUID;
    }

    /**
     * 获取MythicMobs类型
     */
    public String getMythicMobType() {
        return mythicMobType;
    }

    public int getTier() {
        return tier;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    /**
     * 获取生成时间戳
     */
    public long getSpawnTime() {
        return spawnTime;
    }

    // ==================== 伤害相关方法 ====================

    /**
     * 记录玩家伤害
     *
     * @param player 玩家
     * @param damage 伤害值
     */
    public void recordDamage(Player player, double damage) {
        recordDamage(player.getUniqueId(), damage, player);
    }

    /**
     * 记录伤害
     *
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     */
    public void recordDamage(UUID playerUUID, double damage) {
        recordDamage(playerUUID, damage, null);
    }

    /**
     * 记录伤害 (内部方法)
     *
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     * @param player 玩家对象 (可选)
     */
    private void recordDamage(UUID playerUUID, double damage, Player player) {
        if (damage <= 0) {
            return;
        }

        // 记录伤害
        playerDamage.merge(playerUUID, damage, Double::sum);
        totalDamage += damage;
        lastDamageTime = System.currentTimeMillis();

        // 记录最后伤害者
        if (player != null) {
            lastDamager = player;
        }
    }

    /**
     * 记录治疗 (按50%计入伤害)
     *
     * @param player 治疗者
     * @param healing 治疗值
     */
    public void recordHealing(Player player, double healing) {
        double damageEquivalent = healing * 0.5;
        recordDamage(player, damageEquivalent);
    }

    /**
     * 获取特定玩家的伤害
     *
     * @param playerUUID 玩家UUID
     * @return 伤害值
     */
    public double getPlayerDamage(UUID playerUUID) {
        return playerDamage.getOrDefault(playerUUID, 0.0);
    }

    /**
     * 获取特定玩家的伤害百分比
     *
     * @param playerUUID 玩家UUID
     * @return 百分比 (0.0-1.0)
     */
    public double getPlayerDamagePercentage(UUID playerUUID) {
        if (totalDamage <= 0) {
            return 0.0;
        }
        return getPlayerDamage(playerUUID) / totalDamage;
    }

    /**
     * 获取总伤害
     */
    public double getTotalDamage() {
        return totalDamage;
    }

    /**
     * 获取伤害最高的前N名玩家
     *
     * @param limit 限制数量
     * @return 玩家UUID列表，按伤害从高到低排序
     */
    public List<UUID> getTopDamagers(int limit) {
        return playerDamage.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * 获取所有参与伤害的玩家
     */
    public Set<UUID> getParticipants() {
        return new HashSet<>(playerDamage.keySet());
    }

    /**
     * 清除伤害数据
     */
    public void clearDamageData() {
        playerDamage.clear();
        totalDamage = 0.0;
    }

    // ==================== 生命周期方法 ====================

    /**
     * 检查Boss是否还有效 (实体是否存在)
     */
    public boolean isValid() {
        return bukkitEntity != null && !bukkitEntity.isDead();
    }

    /**
     * 获取当前血量百分比 (0.0-1.0)
     */
    public double getHealthPercentage() {
        if (bukkitEntity == null || bukkitEntity.getMaxHealth() == 0) {
            return 0.0;
        }
        return bukkitEntity.getHealth() / bukkitEntity.getMaxHealth();
    }

    /**
     * 获取当前血量
     */
    public double getCurrentHealth() {
        return bukkitEntity != null ? bukkitEntity.getHealth() : 0.0;
    }

    /**
     * 获取最大血量
     */
    public double getMaxHealth() {
        return bukkitEntity != null ? bukkitEntity.getMaxHealth() : 0.0;
    }

    /**
     * 标记为已击杀
     *
     * @param killer 击杀者
     */
    public void markAsKilled(Player killer) {
        this.killer = killer;
        this.status = BossStatus.DEAD;
        this.deathTime = System.currentTimeMillis();
    }

    /**
     * 标记为已消失 (自然消失或超时)
     */
    public void markAsDespawned() {
        this.status = BossStatus.DESPAWNED;
        this.deathTime = System.currentTimeMillis();
    }

    /**
     * 获取Boss存活时间 (毫秒)
     */
    public long getAliveDuration() {
        long endTime = status == BossStatus.ACTIVE ? System.currentTimeMillis() : deathTime;
        return endTime - spawnTime;
    }

    /**
     * 获取Boss存活时间 (秒)
     */
    public long getAliveDurationSeconds() {
        return getAliveDuration() / 1000;
    }

    /**
     * 获取自最后伤害以来的时间 (秒)
     */
    public long getIdleTimeSeconds() {
        return (System.currentTimeMillis() - lastDamageTime) / 1000;
    }

    /**
     * 获取Boss状态
     */
    public BossStatus getStatus() {
        return status;
    }

    /**
     * Boss是否已死亡
     */
    public boolean isDead() {
        return status == BossStatus.DEAD || status == BossStatus.DESPAWNED;
    }

    // ==================== 属性查询方法 ====================

    /**
     * 获取Boss的属性信息
     */
    public BossAttributes getAttributes() {
        return new BossAttributes(
            mythicMobType,
            tier,
            displayName,
            getCurrentHealth(),
            getMaxHealth(),
            getParticipants().size(),
            getTotalDamage()
        );
    }

    /**
     * 获取Boss的位置
     */
    public Location getLocation() {
        return bukkitEntity != null ? bukkitEntity.getLocation() : spawnLocation;
    }

    /**
     * 获取Boss到某个位置的距离
     */
    public double getDistance(Location location) {
        Location bossLoc = getLocation();
        if (bossLoc == null || location == null) {
            return Double.MAX_VALUE;
        }
        return bossLoc.distance(location);
    }

    /**
     * Boss是否在某个范围内
     */
    public boolean isInRange(Location location, double range) {
        return getDistance(location) <= range;
    }

    // ==================== 元数据方法 ====================

    /**
     * 设置元数据
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 获取元数据 (带默认值)
     */
    public Object getMetadata(String key, Object defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    /**
     * 移除元数据
     */
    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    /**
     * 清除所有元数据
     */
    public void clearMetadata() {
        metadata.clear();
    }

    // ==================== 信息方法 ====================

    /**
     * 获取简要信息
     */
    public String getSimpleInfo() {
        return String.format(
            "Boss{uuid=%s, type=%s, tier=%d, health=%.1f/%.1f, damage=%.1f}",
            bossUUID.toString().substring(0, 8),
            mythicMobType,
            tier,
            getCurrentHealth(),
            getMaxHealth(),
            getTotalDamage()
        );
    }

    /**
     * 获取详细信息
     */
    public String getDetailedInfo() {
        return String.format(
            "BossEntity{\n" +
            "  UUID: %s\n" +
            "  类型: %s\n" +
            "  等级: %d\n" +
            "  显示名: %s\n" +
            "  血量: %.1f/%.1f (%.1f%%)\n" +
            "  伤害: %.1f 来自%d个玩家\n" +
            "  存活时间: %d秒\n" +
            "  空闲时间: %d秒\n" +
            "  状态: %s\n" +
            "  位置: %s\n" +
            "}",
            bossUUID,
            mythicMobType,
            tier,
            displayName,
            getCurrentHealth(),
            getMaxHealth(),
            getHealthPercentage() * 100,
            getTotalDamage(),
            getParticipants().size(),
            getAliveDurationSeconds(),
            getIdleTimeSeconds(),
            status,
            getLocation()
        );
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BossEntity that = (BossEntity) o;
        return Objects.equals(bossUUID, that.bossUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bossUUID);
    }

    // ==================== 内部类 ====================

    /**
     * Boss状态枚举
     */
    public enum BossStatus {
        PREPARING("准备中"),
        ACTIVE("活跃"),
        INJURED("受伤"),
        CRITICAL("濒死"),
        DEAD("已死亡"),
        DESPAWNED("已消失");

        private final String displayName;

        BossStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Boss属性信息
     */
    @Getter
    public static class BossAttributes {
        private final String type;
        private final int tier;
        private final String displayName;
        private final double currentHealth;
        private final double maxHealth;
        private final int participantCount;
        private final double totalDamage;

        public BossAttributes(String type, int tier, String displayName,
                            double currentHealth, double maxHealth,
                            int participantCount, double totalDamage) {
            this.type = type;
            this.tier = tier;
            this.displayName = displayName;
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.participantCount = participantCount;
            this.totalDamage = totalDamage;
        }

        /**
         * 获取血量百分比
         */
        public double getHealthPercentage() {
            return maxHealth > 0 ? currentHealth / maxHealth : 0.0;
        }

        /**
         * 获取简要描述
         */
        public String getDescription() {
            return String.format("%s (Lv.%d) - HP: %.1f/%.1f (%.1f%%) - 伤害: %.1f",
                displayName, tier, currentHealth, maxHealth,
                getHealthPercentage() * 100, totalDamage);
        }
    }
}
