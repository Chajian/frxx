package com.xiancore.systems.skill;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元素属性管理器
 * 管理实体的元素属性，支持缓存和持久化
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ElementalAttribute {

    private static XianCore plugin;
    private static NamespacedKey elementKey;

    // 元素属性缓存 (UUID -> Element)
    private static final Map<UUID, SkillElement> elementCache = new ConcurrentHashMap<>();

    // 相克系数缓存 (预计算表)
    private static final Map<String, Double> multiplierCache = new HashMap<>();

    /**
     * 初始化元素属性系统
     */
    public static void initialize(XianCore pluginInstance) {
        plugin = pluginInstance;
        elementKey = new NamespacedKey(plugin, "element");

        // 预计算所有相克系数
        precomputeMultipliers();

        plugin.getLogger().info("  §a✓ 元素属性系统已初始化");
    }

    /**
     * 预计算相克系数表
     */
    private static void precomputeMultipliers() {
        for (SkillElement skill : SkillElement.values()) {
            for (SkillElement target : SkillElement.values()) {
                String key = skill.name() + ":" + target.name();
                multiplierCache.put(key, skill.getDamageMultiplier(target));
            }
        }
    }

    /**
     * 获取实体的元素属性
     *
     * @param entity 实体
     * @return 元素属性，如果没有则返回null或默认值
     */
    public static SkillElement getEntityElement(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();

        // 1. 先查缓存
        SkillElement cached = elementCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        // 2. 尝试从NBT读取
        SkillElement nbtElement = readFromNBT(entity);
        if (nbtElement != null) {
            elementCache.put(uuid, nbtElement);
            return nbtElement;
        }

        // 3. 如果是玩家，从装备或PlayerData获取
        if (entity instanceof Player player) {
            SkillElement playerElement = getPlayerElement(player);
            if (playerElement != null) {
                elementCache.put(uuid, playerElement);
                return playerElement;
            }
        }

        // 4. 尝试从MythicMobs配置读取
        SkillElement mythicElement = readFromMythicMobs(entity);
        if (mythicElement != null) {
            elementCache.put(uuid, mythicElement);
            return mythicElement;
        }

        // 5. 使用默认元素分配
        SkillElement defaultElement = getDefaultElement(entity);
        elementCache.put(uuid, defaultElement);
        return defaultElement;
    }

    /**
     * 设置实体的元素属性
     *
     * @param entity  实体
     * @param element 元素属性
     */
    public static void setEntityElement(LivingEntity entity, SkillElement element) {
        // 写入NBT
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(elementKey, PersistentDataType.STRING, element.name());

        // 更新缓存
        elementCache.put(entity.getUniqueId(), element);
    }

    /**
     * 从NBT读取元素属性
     */
    private static SkillElement readFromNBT(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        String elementStr = pdc.get(elementKey, PersistentDataType.STRING);

        if (elementStr != null) {
            try {
                return SkillElement.valueOf(elementStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * 获取玩家的元素属性
     * 优先级：当前使用的功法 > 装备 > 境界
     */
    private static SkillElement getPlayerElement(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return null;
        }

        // TODO: 可以根据玩家最常用的功法元素来确定
        // 或者根据装备的元素来确定
        // 这里暂时返回null，使用默认规则

        return null;
    }

    /**
     * 从MythicMobs配置读取元素
     */
    private static SkillElement readFromMythicMobs(LivingEntity entity) {
        // 检查是否为MythicMobs怪物
        if (plugin.getMythicIntegration() != null && 
            plugin.getMythicIntegration().isEnabled()) {
            
            try {
                // 尝试获取MythicMobs怪物的元素标签
                String elementTag = plugin.getMythicIntegration()
                        .getMobElement(entity);
                
                if (elementTag != null) {
                    return SkillElement.fromString(elementTag);
                }
            } catch (Exception e) {
                // MythicMobs集成失败，忽略
            }
        }

        return null;
    }

    /**
     * 根据实体类型分配默认元素
     */
    private static SkillElement getDefaultElement(LivingEntity entity) {
        EntityType type = entity.getType();
        
        // 火系
        if (type == EntityType.BLAZE || type == EntityType.MAGMA_CUBE) {
            return SkillElement.FIRE;
        }
        
        // 水系
        if (type == EntityType.DROWNED || type == EntityType.GUARDIAN || 
            type == EntityType.ELDER_GUARDIAN || type == EntityType.SQUID) {
            return SkillElement.WATER;
        }
        
        // 土/地系
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || 
            type == EntityType.ZOMBIE_VILLAGER) {
            return SkillElement.EARTH;
        }
        
        // 冰系
        if (type == EntityType.STRAY || type == EntityType.POLAR_BEAR) {
            return SkillElement.ICE;
        }
        
        // 雷系
        if (type == EntityType.CREEPER) {
            return SkillElement.THUNDER;
        }
        
        // 木系
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER || 
            type == EntityType.SILVERFISH || type == EntityType.ENDERMITE) {
            return SkillElement.WOOD;
        }
        
        // 金系
        if (type == EntityType.SKELETON || type == EntityType.IRON_GOLEM) {
            return SkillElement.METAL;
        }
        
        // 风系
        if (type == EntityType.PHANTOM || type == EntityType.VEX) {
            return SkillElement.WIND;
        }
        
        // 暗系
        if (type == EntityType.WITHER || type == EntityType.WITHER_SKELETON || 
            type == EntityType.ENDERMAN) {
            return SkillElement.DARK;
        }
        
        // 光系（1.19+才有ALLAY，需要检查）
        try {
            if (type == EntityType.valueOf("ALLAY")) {
                return SkillElement.LIGHT;
            }
        } catch (IllegalArgumentException e) {
            // ALLAY不存在于当前版本，忽略
        }
        
        // 默认无属性
        return SkillElement.NEUTRAL;
    }

    /**
     * 获取相克系数（使用缓存）
     *
     * @param skillElement  功法元素
     * @param targetElement 目标元素
     * @param isPVP         是否为PVP
     * @return 伤害倍率
     */
    public static double getDamageMultiplier(SkillElement skillElement, 
                                            SkillElement targetElement, 
                                            boolean isPVP) {
        if (skillElement == null || targetElement == null) {
            return 1.0;
        }

        // 从缓存获取基础倍率
        String key = skillElement.name() + ":" + targetElement.name();
        Double baseMultiplier = multiplierCache.get(key);

        if (baseMultiplier == null) {
            baseMultiplier = 1.0;
        }

        // PVP时减半相克效果
        if (isPVP && plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.elemental.full-pvp-multiplier", false) == false) {
            
            if (baseMultiplier > 1.0) {
                // 相克：1.5 → 1.25
                return 1.0 + (baseMultiplier - 1.0) * 0.5;
            } else if (baseMultiplier < 1.0) {
                // 相生：0.7 → 0.85
                return 1.0 - (1.0 - baseMultiplier) * 0.5;
            }
        }

        return baseMultiplier;
    }

    /**
     * 清除实体缓存（在实体死亡时调用）
     *
     * @param entityUUID 实体UUID
     */
    public static void clearCache(UUID entityUUID) {
        elementCache.remove(entityUUID);
    }

    /**
     * 清除所有缓存
     */
    public static void clearAllCache() {
        elementCache.clear();
    }

    /**
     * 获取缓存大小（调试用）
     */
    public static int getCacheSize() {
        return elementCache.size();
    }
}


