package com.xiancore.boss.system.lifecycle;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Boss 生命周期数据
 * 记录 Boss 的状态和生命周期信息
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class BossLifecycleData {

    private UUID bossUUID;
    private String bossType;
    private String status; // SPAWNED, FIGHTING, DYING, DEAD, DESPAWNED
    private int tier;
    private double currentHealth;
    private double maxHealth;
    private long spawnTime;
    private long deathTime;
    private int participantCount;
    private Map<String, Object> metadata;

    public BossLifecycleData() {
        this.metadata = new HashMap<>();
        this.spawnTime = System.currentTimeMillis();
    }

    public BossLifecycleData(@NotNull UUID bossUUID, @NotNull String bossType, int tier) {
        this();
        this.bossUUID = bossUUID;
        this.bossType = bossType;
        this.tier = tier;
        this.status = "SPAWNED";
    }

    // Getters and Setters
    public UUID getBossUUID() {
        return bossUUID;
    }

    public void setBossUUID(UUID bossUUID) {
        this.bossUUID = bossUUID;
    }

    public String getBossType() {
        return bossType;
    }

    public void setBossType(String bossType) {
        this.bossType = bossType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(double currentHealth) {
        this.currentHealth = currentHealth;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public long getSpawnTime() {
        return spawnTime;
    }

    public void setSpawnTime(long spawnTime) {
        this.spawnTime = spawnTime;
    }

    public long getDeathTime() {
        return deathTime;
    }

    public void setDeathTime(long deathTime) {
        this.deathTime = deathTime;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 获取 Boss 存活时间（毫秒）
     */
    public long getAliveTime() {
        return System.currentTimeMillis() - spawnTime;
    }

    /**
     * 检查 Boss 是否已死亡
     */
    public boolean isDead() {
        return "DEAD".equals(status) || "DESPAWNED".equals(status);
    }
}
