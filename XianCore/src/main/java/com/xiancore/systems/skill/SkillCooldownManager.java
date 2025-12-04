package com.xiancore.systems.skill;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功法冷却管理器
 * 管理玩家功法的冷却时间
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillCooldownManager {

    // 玩家 -> (功法ID -> 冷却结束时间)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    // 遗忘锁：玩家 -> (功法ID -> 可重学时间)
    private final Map<UUID, Map<String, Long>> forgetLocks = new ConcurrentHashMap<>();

    /**
     * 设置功法冷却
     *
     * @param player   玩家
     * @param skillId  功法ID
     * @param cooldown 冷却时间(秒)
     */
    public void setCooldown(Player player, String skillId, int cooldown) {
        UUID uuid = player.getUniqueId();
        cooldowns.putIfAbsent(uuid, new ConcurrentHashMap<>());

        long endTime = System.currentTimeMillis() + (cooldown * 1000L);
        cooldowns.get(uuid).put(skillId, endTime);
    }

    /**
     * 检查功法是否在冷却中
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 是否在冷却中
     */
    public boolean isOnCooldown(Player player, String skillId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null || !playerCooldowns.containsKey(skillId)) {
            return false;
        }

        long endTime = playerCooldowns.get(skillId);
        long now = System.currentTimeMillis();

        if (now >= endTime) {
            // 冷却结束，移除记录
            playerCooldowns.remove(skillId);
            return false;
        }

        return true;
    }

    /**
     * 获取剩余冷却时间
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 剩余冷却时间(秒)，如果不在冷却中返回0
     */
    public int getRemainingCooldown(Player player, String skillId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns == null || !playerCooldowns.containsKey(skillId)) {
            return 0;
        }

        long endTime = playerCooldowns.get(skillId);
        long now = System.currentTimeMillis();
        long remaining = endTime - now;

        if (remaining <= 0) {
            playerCooldowns.remove(skillId);
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * 清除玩家的所有冷却
     *
     * @param player 玩家
     */
    public void clearCooldowns(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    /**
     * 清除玩家指定功法的冷却
     *
     * @param player  玩家
     * @param skillId 功法ID
     */
    public void clearCooldown(Player player, String skillId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns != null) {
            playerCooldowns.remove(skillId);
        }
    }

    /**
     * 减少功法冷却时间
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @param seconds 减少的秒数
     */
    public void reduceCooldown(Player player, String skillId, int seconds) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);

        if (playerCooldowns != null && playerCooldowns.containsKey(skillId)) {
            long endTime = playerCooldowns.get(skillId);
            long newEndTime = endTime - (seconds * 1000L);
            long now = System.currentTimeMillis();

            if (newEndTime <= now) {
                // 冷却结束
                playerCooldowns.remove(skillId);
            } else {
                playerCooldowns.put(skillId, newEndTime);
            }
        }
    }

    /**
     * 获取玩家所有在冷却中的功法
     *
     * @param player 玩家
     * @return 功法ID -> 剩余冷却时间(秒)
     */
    public Map<String, Integer> getAllCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        Map<String, Integer> result = new HashMap<>();

        if (playerCooldowns == null) {
            return result;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : playerCooldowns.entrySet()) {
            long remaining = entry.getValue() - now;
            if (remaining > 0) {
                result.put(entry.getKey(), (int) Math.ceil(remaining / 1000.0));
            }
        }

        return result;
    }

    /**
     * 清理所有过期的冷却记录
     * 应该定期调用以释放内存
     */
    public void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();

        for (Map<String, Long> playerCooldowns : cooldowns.values()) {
            playerCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
        }

        // 移除空的玩家记录
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // 清理过期的遗忘锁
        for (Map<String, Long> playerLocks : forgetLocks.values()) {
            playerLocks.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        forgetLocks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * 设置遗忘锁（防止立即重学绕过冷却）
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @param seconds 锁定时间(秒)
     */
    public void setForgetLock(Player player, String skillId, int seconds) {
        UUID uuid = player.getUniqueId();
        forgetLocks.putIfAbsent(uuid, new ConcurrentHashMap<>());

        long unlockTime = System.currentTimeMillis() + (seconds * 1000L);
        forgetLocks.get(uuid).put(skillId, unlockTime);
    }

    /**
     * 检查是否在遗忘锁定期内
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 是否被锁定
     */
    public boolean isForgetLocked(Player player, String skillId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerLocks = forgetLocks.get(uuid);

        if (playerLocks == null || !playerLocks.containsKey(skillId)) {
            return false;
        }

        long unlockTime = playerLocks.get(skillId);
        long now = System.currentTimeMillis();

        if (now >= unlockTime) {
            playerLocks.remove(skillId);
            return false;
        }

        return true;
    }

    /**
     * 获取遗忘锁剩余时间
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 剩余时间(秒)，未锁定返回0
     */
    public int getRemainingForgetLock(Player player, String skillId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerLocks = forgetLocks.get(uuid);

        if (playerLocks == null || !playerLocks.containsKey(skillId)) {
            return 0;
        }

        long unlockTime = playerLocks.get(skillId);
        long now = System.currentTimeMillis();
        long remaining = unlockTime - now;

        if (remaining <= 0) {
            playerLocks.remove(skillId);
            return 0;
        }

        return (int) Math.ceil(remaining / 1000.0);
    }

    /**
     * 清除遗忘锁
     *
     * @param player  玩家
     * @param skillId 功法ID
     */
    public void clearForgetLock(Player player, String skillId) {
        UUID uuid = player.getUniqueId();
        Map<String, Long> playerLocks = forgetLocks.get(uuid);

        if (playerLocks != null) {
            playerLocks.remove(skillId);
        }
    }
}
