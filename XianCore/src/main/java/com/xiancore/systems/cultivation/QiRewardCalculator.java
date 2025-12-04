package com.xiancore.systems.cultivation;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 修为奖励计算器
 * 根据怪物境界、玩家境界差距和特殊修饰符计算修为奖励
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class QiRewardCalculator {

    private final XianCore plugin;
    private FileConfiguration rewardConfig;
    
    // 冷却管理
    private final Map<String, Long> killCooldowns = new ConcurrentHashMap<>();
    
    // 每日限额追踪 (玩家UUID -> 今日已获得修为)
    private final Map<UUID, Long> dailyQiGains = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastResetDate = new ConcurrentHashMap<>();
    
    // 区域击杀追踪 (区块坐标 -> 击杀记录)
    private final Map<String, AreaKillRecord> areaKillRecords = new ConcurrentHashMap<>();

    public QiRewardCalculator(XianCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        plugin.saveResource("cultivation_rewards.yml", false);
        rewardConfig = plugin.getConfigManager().getConfig("cultivation_rewards");
        
        if (rewardConfig == null) {
            plugin.getLogger().severe("无法加载 cultivation_rewards.yml 配置文件!");
            return;
        }
        
        plugin.getLogger().info("  §a✓ 修为奖励配置已加载");
    }

    /**
     * 计算击杀怪物获得的修为奖励
     *
     * @param player       击杀玩家
     * @param killedEntity 被击杀的怪物
     * @return QiRewardResult 奖励结果（包含修为数量和详细信息）
     */
    public QiRewardResult calculateQiReward(Player player, LivingEntity killedEntity) {
        if (rewardConfig == null) {
            return new QiRewardResult(0, "配置文件未加载");
        }

        PlayerData playerData = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (playerData == null) {
            return new QiRewardResult(0, "玩家数据未找到");
        }

        // 1. 检查冷却时间
        String cooldownKey = getCooldownKey(player.getUniqueId(), killedEntity);
        if (isOnCooldown(cooldownKey)) {
            long remainingTime = getRemainingCooldown(cooldownKey);
            return new QiRewardResult(0, "冷却中", remainingTime);
        }

        // 2. 解析怪物境界
        String mobRealm = parseEntityRealm(killedEntity);
        
        // 3. 获取基础奖励
        long baseReward = getBaseReward(mobRealm);
        if (baseReward <= 0) {
            // 使用备用奖励
            return calculateFallbackReward(killedEntity);
        }

        // 4. 计算境界差距乘数
        double realmGapMultiplier = calculateRealmGapMultiplier(
            playerData.getRealm(), mobRealm);

        // 5. 应用特殊修饰符
        double specialMultiplier = calculateSpecialMultipliers(killedEntity, player);

        // 6. 检查区域递减奖励
        double areaMultiplier = calculateAreaMultiplier(player.getLocation());

        // 7. 最终计算
        double totalMultiplier = realmGapMultiplier * specialMultiplier * areaMultiplier;
        long finalReward = Math.round(baseReward * totalMultiplier);
        finalReward = Math.max(1, finalReward); // 最少1点修为

        // 8. 检查每日限额
        String today = getTodayString();
        checkDailyReset(player.getUniqueId(), today);
        
        long currentDailyGain = dailyQiGains.getOrDefault(player.getUniqueId(), 0L);
        long dailyLimit = getDailyLimit(playerData.getRealm());
        
        if (currentDailyGain >= dailyLimit) {
            return new QiRewardResult(0, "今日修为获得已达上限", 
                String.format("(%d/%d)", currentDailyGain, dailyLimit));
        }
        
        // 确保不超过每日限额
        if (currentDailyGain + finalReward > dailyLimit) {
            finalReward = dailyLimit - currentDailyGain;
        }

        // 9. 记录冷却和每日获得量
        setCooldown(cooldownKey, mobRealm);
        dailyQiGains.put(player.getUniqueId(), currentDailyGain + finalReward);
        recordAreaKill(player.getLocation());

        // 10. 生成奖励结果
        String bonusInfo = generateBonusInfo(realmGapMultiplier, specialMultiplier, areaMultiplier);
        
        QiRewardResult result = new QiRewardResult(finalReward, bonusInfo);
        result.setMobRealm(mobRealm);
        result.setPlayerRealm(playerData.getRealm());
        result.setBaseReward(baseReward);
        result.setTotalMultiplier(totalMultiplier);
        
        return result;
    }

    /**
     * 解析实体境界（支持多种来源）
     */
    private String parseEntityRealm(LivingEntity entity) {
        // 优先级1: MythicMobs 自定义名称
        if (plugin.getMythicIntegration() != null && plugin.getMythicIntegration().isEnabled()) {
            String mythicName = getMythicMobDisplayName(entity);
            if (mythicName != null) {
                String realm = RealmParser.parseRealmFromName(mythicName);
                if (realm != null) {
                    return realm;
                }
            }
        }

        // 优先级2: 直接解析实体境界
        return RealmParser.parseEntityRealm(entity);
    }

    /**
     * 获取MythicMobs怪物的显示名称
     */
    private String getMythicMobDisplayName(LivingEntity entity) {
        try {
            if (plugin.getMythicIntegration().isEnabled()) {
                // 使用MythicMobs API获取显示名称
                var mythicMobs = io.lumine.mythic.bukkit.MythicBukkit.inst();
                if (mythicMobs != null && mythicMobs.getMobManager() != null) {
                    var activeMobOptional = mythicMobs.getMobManager().getActiveMob(entity.getUniqueId());
                    if (activeMobOptional.isPresent()) {
                        var activeMob = activeMobOptional.get();
                        return activeMob.getDisplayName();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("获取MythicMobs显示名称失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取基础修为奖励
     */
    private long getBaseReward(String realm) {
        ConfigurationSection baseRewards = rewardConfig.getConfigurationSection("base-rewards");
        if (baseRewards == null) {
            return 0;
        }
        
        return baseRewards.getLong(realm, 0);
    }

    /**
     * 计算境界差距乘数
     */
    private double calculateRealmGapMultiplier(String playerRealm, String mobRealm) {
        int realmGap = RealmParser.calculateRealmGap(playerRealm, mobRealm);
        
        ConfigurationSection multipliers = rewardConfig.getConfigurationSection("realm-gap-multipliers");
        if (multipliers == null) {
            return 1.0;
        }
        
        if (realmGap == 0) {
            return multipliers.getDouble("same-realm", 1.0);
        } else if (realmGap > 0) {
            // 怪物境界更高
            if (realmGap == 1) return multipliers.getDouble("one-higher", 1.5);
            else if (realmGap == 2) return multipliers.getDouble("two-higher", 2.0);
            else if (realmGap == 3) return multipliers.getDouble("three-higher", 2.5);
            else return multipliers.getDouble("four-or-more", 3.0);
        } else {
            // 玩家境界更高
            int absGap = Math.abs(realmGap);
            if (absGap == 1) return multipliers.getDouble("one-lower", 0.7);
            else if (absGap == 2) return multipliers.getDouble("two-lower", 0.4);
            else if (absGap == 3) return multipliers.getDouble("three-lower", 0.2);
            else return multipliers.getDouble("four-or-more", 0.1);
        }
    }

    /**
     * 计算特殊修饰符
     */
    private double calculateSpecialMultipliers(LivingEntity entity, Player player) {
        ConfigurationSection specialMultipliers = rewardConfig.getConfigurationSection("special-multipliers");
        if (specialMultipliers == null) {
            return 1.0;
        }
        
        double multiplier = 1.0;
        
        // Boss 乘数
        if (isBossEntity(entity)) {
            multiplier *= specialMultipliers.getDouble("boss-multiplier", 3.0);
        }
        
        // 精英乘数
        if (isEliteEntity(entity)) {
            multiplier *= specialMultipliers.getDouble("elite-multiplier", 2.0);
        }
        
        // MythicMobs 乘数
        if (isMythicMobsEntity(entity)) {
            multiplier *= specialMultipliers.getDouble("mythic-multiplier", 1.5);
        }
        
        // 组队击杀惩罚
        if (isGroupKill(player, entity)) {
            multiplier *= specialMultipliers.getDouble("group-kill-penalty", 0.8);
        }
        
        return multiplier;
    }

    /**
     * 计算区域递减奖励乘数
     */
    private double calculateAreaMultiplier(Location location) {
        ConfigurationSection areaConfig = rewardConfig.getConfigurationSection("anti-exploit.area-diminishing");
        if (areaConfig == null || !areaConfig.getBoolean("enabled", true)) {
            return 1.0;
        }
        
        String chunkKey = getChunkKey(location);
        AreaKillRecord record = areaKillRecords.get(chunkKey);
        
        if (record == null) {
            return 1.0;
        }
        
        long timeWindow = areaConfig.getLong("time-window", 300) * 1000; // 转换为毫秒
        int maxKills = areaConfig.getInt("max-kills-per-chunk", 10);
        double penalty = areaConfig.getDouble("same-chunk-penalty", 0.5);
        
        // 清理过期记录
        record.cleanupOldKills(timeWindow);
        
        if (record.getKillCount() >= maxKills) {
            return penalty;
        }
        
        return 1.0;
    }

    /**
     * 计算备用奖励（当怪物没有境界信息时）
     */
    private QiRewardResult calculateFallbackReward(LivingEntity entity) {
        ConfigurationSection fallbackConfig = rewardConfig.getConfigurationSection("fallback-rewards");
        if (fallbackConfig == null || !fallbackConfig.getBoolean("enabled", true)) {
            return new QiRewardResult(0, "无境界信息且备用奖励已禁用");
        }
        
        long baseReward = fallbackConfig.getLong("base-reward", 10);
        long finalReward = baseReward;
        
        // 基于血量的额外奖励
        if (fallbackConfig.getBoolean("health-based-bonus", true)) {
            double healthBonusRatio = fallbackConfig.getDouble("health-bonus-ratio", 0.5);
            long healthBonus = Math.round(entity.getMaxHealth() * healthBonusRatio);
            finalReward += healthBonus;
        }
        
        // 应用上限
        long maxReward = fallbackConfig.getLong("max-fallback-reward", 100);
        finalReward = Math.min(finalReward, maxReward);
        
        return new QiRewardResult(finalReward, "备用奖励");
    }

    // ========== 辅助方法 ==========

    private boolean isBossEntity(LivingEntity entity) {
        // 检查原版Boss
        EntityType type = entity.getType();
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || 
            type == EntityType.ELDER_GUARDIAN) {
            return true;
        }
        
        // 检查名称中的Boss标识
        String customName = entity.getCustomName();
        return RealmParser.isBossFromName(customName);
    }

    private boolean isEliteEntity(LivingEntity entity) {
        String customName = entity.getCustomName();
        return RealmParser.isEliteFromName(customName);
    }

    private boolean isMythicMobsEntity(LivingEntity entity) {
        if (plugin.getMythicIntegration() == null || !plugin.getMythicIntegration().isEnabled()) {
            return false;
        }
        
        try {
            var mythicMobs = io.lumine.mythic.bukkit.MythicBukkit.inst();
            return mythicMobs != null && mythicMobs.getMobManager().isActiveMob(entity.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isGroupKill(Player player, LivingEntity entity) {
        // 简单的组队检测：检查附近是否有其他玩家
        return player.getNearbyEntities(10.0, 10.0, 10.0)
                .stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .anyMatch(p -> !p.equals(player));
    }

    private String getCooldownKey(UUID playerUUID, LivingEntity entity) {
        String mobRealm = parseEntityRealm(entity);
        return playerUUID.toString() + ":" + mobRealm;
    }

    private boolean isOnCooldown(String cooldownKey) {
        Long lastKillTime = killCooldowns.get(cooldownKey);
        if (lastKillTime == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastKillTime < getCooldownTime(cooldownKey);
    }

    private long getRemainingCooldown(String cooldownKey) {
        Long lastKillTime = killCooldowns.get(cooldownKey);
        if (lastKillTime == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - lastKillTime;
        long cooldownTime = getCooldownTime(cooldownKey);
        
        return Math.max(0, (cooldownTime - elapsed) / 1000);
    }

    private long getCooldownTime(String cooldownKey) {
        String realm = cooldownKey.split(":")[1];
        ConfigurationSection cooldownConfig = rewardConfig.getConfigurationSection("anti-exploit.kill-cooldowns");
        if (cooldownConfig == null) {
            return 0;
        }
        
        return cooldownConfig.getLong(realm, 5) * 1000; // 转换为毫秒
    }

    private void setCooldown(String cooldownKey, String mobRealm) {
        killCooldowns.put(cooldownKey, System.currentTimeMillis());
    }

    private long getDailyLimit(String playerRealm) {
        ConfigurationSection dailyLimits = rewardConfig.getConfigurationSection("anti-exploit.daily-limits");
        if (dailyLimits == null) {
            return Long.MAX_VALUE;
        }
        
        return dailyLimits.getLong(playerRealm, 1000);
    }

    private String getTodayString() {
        return String.valueOf(System.currentTimeMillis() / (1000 * 60 * 60 * 24));
    }

    private void checkDailyReset(UUID playerUUID, String today) {
        String lastDate = lastResetDate.get(playerUUID);
        if (!today.equals(lastDate)) {
            dailyQiGains.remove(playerUUID);
            lastResetDate.put(playerUUID, today);
        }
    }

    private String getChunkKey(Location location) {
        return location.getWorld().getName() + ":" + 
               location.getChunk().getX() + "," + 
               location.getChunk().getZ();
    }

    private void recordAreaKill(Location location) {
        String chunkKey = getChunkKey(location);
        AreaKillRecord record = areaKillRecords.computeIfAbsent(chunkKey, k -> new AreaKillRecord());
        record.addKill(System.currentTimeMillis());
    }

    private String generateBonusInfo(double realmGap, double special, double area) {
        StringBuilder info = new StringBuilder();
        
        if (realmGap != 1.0) {
            if (realmGap > 1.0) {
                info.append("境界优势: +").append(Math.round((realmGap - 1.0) * 100)).append("%");
            } else {
                info.append("境界劣势: ").append(Math.round((realmGap - 1.0) * 100)).append("%");
            }
        }
        
        if (special > 1.0) {
            if (info.length() > 0) info.append(", ");
            info.append("特殊加成: +").append(Math.round((special - 1.0) * 100)).append("%");
        }
        
        if (area < 1.0) {
            if (info.length() > 0) info.append(", ");
            info.append("区域惩罚: ").append(Math.round((area - 1.0) * 100)).append("%");
        }
        
        return info.toString();
    }

    /**
     * 区域击杀记录
     */
    private static class AreaKillRecord {
        private final Map<Long, Integer> killTimes = new HashMap<>();
        
        public void addKill(long timestamp) {
            killTimes.put(timestamp, killTimes.getOrDefault(timestamp, 0) + 1);
        }
        
        public void cleanupOldKills(long timeWindow) {
            long cutoff = System.currentTimeMillis() - timeWindow;
            killTimes.entrySet().removeIf(entry -> entry.getKey() < cutoff);
        }
        
        public int getKillCount() {
            return killTimes.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /**
     * 修为奖励结果
     */
    public static class QiRewardResult {
        private final long qiAmount;
        private final String reason;
        private final String extraInfo;
        private final Long remainingCooldown;
        
        private String mobRealm;
        private String playerRealm;
        private long baseReward;
        private double totalMultiplier;

        public QiRewardResult(long qiAmount, String reason) {
            this(qiAmount, reason, null, null);
        }

        public QiRewardResult(long qiAmount, String reason, String extraInfo) {
            this(qiAmount, reason, extraInfo, null);
        }

        public QiRewardResult(long qiAmount, String reason, Long remainingCooldown) {
            this(qiAmount, reason, null, remainingCooldown);
        }

        public QiRewardResult(long qiAmount, String reason, String extraInfo, Long remainingCooldown) {
            this.qiAmount = qiAmount;
            this.reason = reason;
            this.extraInfo = extraInfo;
            this.remainingCooldown = remainingCooldown;
        }

        // Getters
        public long getQiAmount() { return qiAmount; }
        public String getReason() { return reason; }
        public String getExtraInfo() { return extraInfo; }
        public Long getRemainingCooldown() { return remainingCooldown; }
        public String getMobRealm() { return mobRealm; }
        public String getPlayerRealm() { return playerRealm; }
        public long getBaseReward() { return baseReward; }
        public double getTotalMultiplier() { return totalMultiplier; }

        // Setters
        public void setMobRealm(String mobRealm) { this.mobRealm = mobRealm; }
        public void setPlayerRealm(String playerRealm) { this.playerRealm = playerRealm; }
        public void setBaseReward(long baseReward) { this.baseReward = baseReward; }
        public void setTotalMultiplier(double totalMultiplier) { this.totalMultiplier = totalMultiplier; }

        public boolean isSuccess() {
            return qiAmount > 0;
        }
    }
}
