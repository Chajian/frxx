package com.xiancore.systems.tribulation;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 天劫数据类
 * 存储单次天劫的状态和进度
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class Tribulation {

    // 基本信息
    private UUID tribulationId;           // 天劫唯一ID
    private UUID playerId;                // 渡劫玩家UUID
    private String playerName;            // 渡劫玩家名称
    private TribulationType type;         // 天劫类型

    // 位置信息
    private Location location;            // 天劫中心位置
    private double range = 50.0;          // 天劫影响范围

    // 进度信息
    private int currentWave = 0;          // 当前波数
    private int totalWaves;               // 总波数
    private boolean active = false;       // 是否激活
    private boolean completed = false;    // 是否完成
    private boolean failed = false;       // 是否失败

    // 时间信息
    private long startTime;               // 开始时间
    private long lastWaveTime;            // 上次劫雷时间
    private long waveInterval = 3000;     // 劫雷间隔(毫秒)
    private long endTime;                 // 结束时间

    // 统计信息
    private int lightningStrikes = 0;     // 劫雷次数
    private double totalDamage = 0;       // 总伤害
    private int deaths = 0;               // 死亡次数
    private double healthLost = 0;        // 损失的生命值
    private double minHealth = 20.0;      // 最低生命值
    private boolean isPerfect = true;     // 是否完美（无伤）

    /**
     * 构造函数
     */
    public Tribulation(Player player, TribulationType type) {
        this.tribulationId = UUID.randomUUID();
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.type = type;
        this.location = player.getLocation().clone();
        this.totalWaves = type.getWaves();
        this.startTime = System.currentTimeMillis();
        this.lastWaveTime = startTime;
    }

    /**
     * 数据加载构造函数
     */
    public Tribulation(UUID tribulationId, UUID playerId, TribulationType type, Location location) {
        this.tribulationId = tribulationId;
        this.playerId = playerId;
        this.type = type;
        this.location = location;
        this.totalWaves = type.getWaves();
    }

    /**
     * 开始天劫
     */
    public void start() {
        this.active = true;
        this.startTime = System.currentTimeMillis();
        this.lastWaveTime = startTime;
    }

    /**
     * 下一波劫雷
     */
    public boolean nextWave() {
        if (!active || completed || failed) {
            return false;
        }

        if (currentWave >= totalWaves) {
            complete();
            return false;
        }

        currentWave++;
        lastWaveTime = System.currentTimeMillis();
        lightningStrikes++;

        return true;
    }

    /**
     * 检查是否可以触发下一波
     */
    public boolean canTriggerNextWave() {
        if (!active || completed || failed) {
            return false;
        }

        if (currentWave >= totalWaves) {
            return false;
        }

        long now = System.currentTimeMillis();
        return (now - lastWaveTime) >= waveInterval;
    }

    /**
     * 完成天劫
     */
    public void complete() {
        this.active = false;
        this.completed = true;
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 失败天劫
     */
    public void fail() {
        this.active = false;
        this.failed = true;
        this.endTime = System.currentTimeMillis();
        this.deaths++;
    }

    /**
     * 取消天劫
     */
    public void cancel() {
        this.active = false;
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 记录伤害
     */
    public void addDamage(double damage) {
        this.totalDamage += damage;
    }

    /**
     * 获取进度百分比
     */
    public double getProgress() {
        if (totalWaves == 0) {
            return 0;
        }
        return (double) currentWave / totalWaves * 100;
    }

    /**
     * 获取持续时间(秒)
     */
    public long getDuration() {
        if (endTime > 0) {
            return (endTime - startTime) / 1000;
        }
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * 计算当前波的伤害
     */
    public double getCurrentWaveDamage() {
        return type.getLightningDamage(currentWave);
    }

    /**
     * 检查玩家是否在范围内
     */
    public boolean isPlayerInRange(Location playerLoc) {
        if (location == null || playerLoc == null) {
            return false;
        }

        if (!location.getWorld().equals(playerLoc.getWorld())) {
            return false;
        }

        return location.distance(playerLoc) <= range;
    }

    /**
     * 获取剩余波数
     */
    public int getRemainingWaves() {
        return totalWaves - currentWave;
    }

    /**
     * 是否为最后一波
     */
    public boolean isLastWave() {
        return currentWave == totalWaves;
    }

    /**
     * 记录生命值损失
     */
    public void recordHealthLoss(double healthBefore, double healthAfter) {
        double loss = healthBefore - healthAfter;
        if (loss > 0) {
            this.healthLost += loss;
            this.isPerfect = false;
            this.minHealth = Math.min(this.minHealth, healthAfter);
        }
    }

    /**
     * 计算渡劫评级
     * S: 无死亡，血量保持80%以上
     * A: 无死亡，血量保持50%以上
     * B: 死亡1次或血量低于50%
     * C: 死亡2次以上
     */
    public String calculateRating() {
        if (!completed) {
            return "F";  // 失败或未完成
        }

        // 死亡2次以上 = C
        if (deaths >= 2) {
            return "C";
        }

        // 死亡1次 = B
        if (deaths == 1) {
            return "B";
        }

        // 无死亡，根据最低血量判断
        double healthPercent = minHealth / 20.0; // Minecraft满血20

        if (healthPercent >= 0.8 && isPerfect) {
            return "S";  // 完美通关
        } else if (healthPercent >= 0.5) {
            return "A";  // 优秀
        } else {
            return "B";  // 良好
        }
    }

    /**
     * 获取评级颜色
     */
    public String getRatingColor() {
        String rating = calculateRating();
        return switch (rating) {
            case "S" -> "§d§l";
            case "A" -> "§6§l";
            case "B" -> "§e§l";
            case "C" -> "§7§l";
            default -> "§c§l";
        };
    }

    /**
     * 获取评级描述
     */
    public String getRatingDescription() {
        String rating = calculateRating();
        return switch (rating) {
            case "S" -> "完美通关";
            case "A" -> "优秀";
            case "B" -> "良好";
            case "C" -> "勉强";
            default -> "失败";
        };
    }

    /**
     * 计算奖励倍率（基于评级）
     */
    public double getRewardMultiplier() {
        String rating = calculateRating();
        return switch (rating) {
            case "S" -> 2.0;
            case "A" -> 1.5;
            case "B" -> 1.2;
            case "C" -> 1.0;
            default -> 0.0;
        };
    }

    // ==================== 显式 Getter 方法 ====================

    public UUID getTribulationId() {
        return tribulationId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public TribulationType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public double getRange() {
        return range;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public int getTotalWaves() {
        return totalWaves;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isFailed() {
        return failed;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getLastWaveTime() {
        return lastWaveTime;
    }

    public long getWaveInterval() {
        return waveInterval;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getLightningStrikes() {
        return lightningStrikes;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public int getDeaths() {
        return deaths;
    }

    public double getHealthLost() {
        return healthLost;
    }

    public double getMinHealth() {
        return minHealth;
    }

    public boolean isPerfect() {
        return isPerfect;
    }

    // ==================== 显式 Setter 方法 ====================

    public void setTribulationId(UUID tribulationId) {
        this.tribulationId = tribulationId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setType(TribulationType type) {
        this.type = type;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public void setTotalWaves(int totalWaves) {
        this.totalWaves = totalWaves;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setLastWaveTime(long lastWaveTime) {
        this.lastWaveTime = lastWaveTime;
    }

    public void setWaveInterval(long waveInterval) {
        this.waveInterval = waveInterval;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setLightningStrikes(int lightningStrikes) {
        this.lightningStrikes = lightningStrikes;
    }

    public void setTotalDamage(double totalDamage) {
        this.totalDamage = totalDamage;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void setHealthLost(double healthLost) {
        this.healthLost = healthLost;
    }

    public void setMinHealth(double minHealth) {
        this.minHealth = minHealth;
    }

    public void setPerfect(boolean perfect) {
        this.isPerfect = perfect;
    }
}
