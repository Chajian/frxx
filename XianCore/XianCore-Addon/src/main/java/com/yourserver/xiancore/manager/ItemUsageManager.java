package com.yourserver.xiancore.manager;

import com.yourserver.xiancore.XianCoreAddon;
import com.yourserver.xiancore.model.ItemUsage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 道具使用次数管理器
 */
public class ItemUsageManager {
    
    private final XianCoreAddon plugin;
    private final Map<UUID, Map<String, ItemUsage>> cache;
    private final Map<UUID, Map<String, Long>> cooldowns;
    
    public ItemUsageManager(XianCoreAddon plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<>();
        this.cooldowns = new HashMap<>();
    }
    
    /**
     * 检查玩家是否可以使用道具
     */
    public boolean canUseItem(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        
        // 检查权限绕过
        if (player.hasPermission("xiancore.bypass.limit")) {
            return true;
        }
        
        // 检查使用次数
        if (plugin.getConfig().getBoolean("features.usage-limit", true)) {
            ItemUsage usage = getUsage(uuid, itemId);
            int maxUsage = plugin.getConfigManager().getItemMaxUsage(itemId);
            
            if (usage.getUsageCount() >= maxUsage) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("current", String.valueOf(usage.getUsageCount()));
                replacements.put("max", String.valueOf(maxUsage));
                player.sendMessage(plugin.getConfigManager().getMessage("item.usage-limit-reached", replacements));
                return false;
            }
        }
        
        // 检查冷却时间
        if (plugin.getConfig().getBoolean("features.cooldown-system", true)) {
            if (isOnCooldown(uuid, itemId)) {
                long remaining = getRemainingCooldown(uuid, itemId);
                Map<String, String> replacements = new HashMap<>();
                replacements.put("time", String.valueOf(remaining));
                player.sendMessage(plugin.getConfigManager().getMessage("item.on-cooldown", replacements));
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 记录道具使用
     */
    public void recordUsage(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        ItemUsage usage = getUsage(uuid, itemId);
        
        long now = System.currentTimeMillis() / 1000;
        usage.incrementUsage();
        usage.setLastUsed(now);
        
        if (usage.getFirstUsed() == 0) {
            usage.setFirstUsed(now);
        }
        
        // 更新缓存
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(itemId, usage);
        
        // 设置冷却
        int cooldown = plugin.getConfigManager().getItemCooldown(itemId);
        if (cooldown > 0) {
            setCooldown(uuid, itemId, cooldown);
        }
        
        // 异步保存到数据库
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().saveItemUsage(usage);
        });
        
        // 记录日志
        plugin.getDatabaseManager().logOperation(
            uuid, player.getName(), "USE_ITEM", itemId, 
            usage.getUsageCount() - 1, usage.getUsageCount(),
            "使用道具: " + itemId
        );
        
        if (plugin.getConfig().getBoolean("debug.verbose-logging", false)) {
            plugin.getLogger().info(String.format(
                "[道具使用] %s 使用了 %s (第 %d/%d 次)",
                player.getName(), itemId, usage.getUsageCount(), usage.getMaxUsage()
            ));
        }
    }
    
    /**
     * 获取道具使用记录
     */
    public ItemUsage getUsage(UUID uuid, String itemId) {
        // 先从缓存读取
        if (cache.containsKey(uuid) && cache.get(uuid).containsKey(itemId)) {
            return cache.get(uuid).get(itemId);
        }
        
        // 从数据库读取
        ItemUsage usage = plugin.getDatabaseManager().loadItemUsage(uuid, itemId);
        
        // 如果不存在，创建新记录
        if (usage == null) {
            int maxUsage = plugin.getConfigManager().getItemMaxUsage(itemId);
            usage = new ItemUsage(uuid, itemId, 0, maxUsage, 0, 0);
        }
        
        // 放入缓存
        cache.computeIfAbsent(uuid, k -> new HashMap<>()).put(itemId, usage);
        
        return usage;
    }
    
    /**
     * 获取玩家所有道具使用记录
     */
    public Map<String, ItemUsage> getAllUsage(UUID uuid) {
        if (!cache.containsKey(uuid)) {
            return new HashMap<>();
        }
        return new HashMap<>(cache.get(uuid));
    }
    
    /**
     * 重置玩家道具使用次数（管理员）
     */
    public boolean resetUsage(UUID uuid, String itemId) {
        // 从数据库删除
        plugin.getDatabaseManager().deleteItemUsage(uuid, itemId);
        
        // 清除缓存
        if (cache.containsKey(uuid)) {
            cache.get(uuid).remove(itemId);
        }
        
        // 清除冷却
        if (cooldowns.containsKey(uuid)) {
            cooldowns.get(uuid).remove(itemId);
        }
        
        return true;
    }
    
    /**
     * 重置玩家所有道具使用次数（管理员）
     */
    public boolean resetAllUsage(UUID uuid) {
        // 清除缓存
        cache.remove(uuid);
        cooldowns.remove(uuid);
        
        // TODO: 从数据库删除所有记录（需要添加数据库方法）
        
        return true;
    }
    
    /**
     * 设置冷却时间
     */
    private void setCooldown(UUID uuid, String itemId, int seconds) {
        long expireTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>()).put(itemId, expireTime);
    }
    
    /**
     * 检查是否在冷却中
     */
    private boolean isOnCooldown(UUID uuid, String itemId) {
        if (!cooldowns.containsKey(uuid) || !cooldowns.get(uuid).containsKey(itemId)) {
            return false;
        }
        
        long expireTime = cooldowns.get(uuid).get(itemId);
        if (System.currentTimeMillis() >= expireTime) {
            cooldowns.get(uuid).remove(itemId);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取剩余冷却时间（秒）
     */
    private long getRemainingCooldown(UUID uuid, String itemId) {
        if (!cooldowns.containsKey(uuid) || !cooldowns.get(uuid).containsKey(itemId)) {
            return 0;
        }
        
        long expireTime = cooldowns.get(uuid).get(itemId);
        long remaining = (expireTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }
    
    /**
     * 清除玩家缓存（玩家退出时调用）
     */
    public void clearCache(UUID uuid) {
        cache.remove(uuid);
        cooldowns.remove(uuid);
    }
}

