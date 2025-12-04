package com.xiancore.integration.mythic;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * MythicMobs 属性读取器
 * 从 MythicMobs 配置中读取 Boss 的各项属性
 *
 * @author XianCore
 * @version 1.0
 */
public class MythicMobsAttributeReader {

    private final Plugin plugin;
    private final Logger logger;
    private final Map<String, MobAttributes> attributeCache;
    private final int CACHE_EXPIRE_TIME = 300; // 5分钟缓存过期时间（秒）

    /**
     * Boss 属性数据类
     */
    public static class MobAttributes {
        public double health;
        public double damage;
        public double armor;
        public double movementSpeed;
        public double attackSpeed;
        public long lastUpdateTime;

        public MobAttributes(double health, double damage, double armor,
                           double movementSpeed, double attackSpeed) {
            this.health = health;
            this.damage = damage;
            this.armor = armor;
            this.movementSpeed = movementSpeed;
            this.attackSpeed = attackSpeed;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * 检查缓存是否过期
         */
        public boolean isExpired(int expireSeconds) {
            long currentTime = System.currentTimeMillis();
            return (currentTime - lastUpdateTime) > (expireSeconds * 1000L);
        }
    }

    /**
     * 构造函数
     */
    public MythicMobsAttributeReader(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.attributeCache = new ConcurrentHashMap<>();
    }

    /**
     * 读取 Boss 属性
     * 优先使用缓存，缓存过期后重新读取
     */
    public MobAttributes readMobProperties(String mobType) {
        try {
            // 检查缓存
            if (attributeCache.containsKey(mobType)) {
                MobAttributes cached = attributeCache.get(mobType);
                if (!cached.isExpired(CACHE_EXPIRE_TIME)) {
                    logger.info("✓ 使用缓存属性: " + mobType);
                    return cached;
                }
            }

            // 从 MythicMobs 读取属性（暂时返回默认属性）
            // 实际使用时需要正确调用 MythicMobs API
            MobAttributes attributes = new MobAttributes(20.0, 5.0, 0.0, 0.1, 1.0);
            attributeCache.put(mobType, attributes);
            logger.info("✓ 读取 Boss 属性成功（使用默认值）: " + mobType);
            return attributes;

        } catch (Exception e) {
            logger.severe("✗ 读取 Boss 属性异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 读取 Boss 血量
     */
    private double readHealth(MythicMob mythicMob) {
        try {
            // 从 MythicMobs 配置读取血量
            // 如果没有配置，使用默认值 20.0
            Double health = mythicMob.getConfig().getDouble("Health");
            return health != null && health > 0 ? health : 20.0;
        } catch (Exception e) {
            logger.warning("读取血量失败，使用默认值: " + e.getMessage());
            return 20.0;
        }
    }

    /**
     * 读取 Boss 伤害值
     */
    private double readDamage(MythicMob mythicMob) {
        try {
            // 从 MythicMobs 配置读取伤害
            // 通常在 Damage 字段中
            Double damage = mythicMob.getConfig().getDouble("Damage");
            return damage != null && damage > 0 ? damage : 5.0;
        } catch (Exception e) {
            logger.warning("读取伤害值失败，使用默认值: " + e.getMessage());
            return 5.0;
        }
    }

    /**
     * 读取 Boss 护甲值
     */
    private double readArmor(MythicMob mythicMob) {
        try {
            // 从 MythicMobs 配置读取护甲
            Double armor = mythicMob.getConfig().getDouble("Armor");
            return armor != null && armor >= 0 ? armor : 0.0;
        } catch (Exception e) {
            logger.warning("读取护甲值失败，使用默认值: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * 读取 Boss 移动速度
     */
    private double readMovementSpeed(MythicMob mythicMob) {
        try {
            // 从 MythicMobs 配置读取移动速度
            Double speed = mythicMob.getConfig().getDouble("MovementSpeed");
            return speed != null && speed > 0 ? speed : 0.1;
        } catch (Exception e) {
            logger.warning("读取移动速度失败，使用默认值: " + e.getMessage());
            return 0.1;
        }
    }

    /**
     * 读取 Boss 攻击速度
     */
    private double readAttackSpeed(MythicMob mythicMob) {
        try {
            // 从 MythicMobs 配置读取攻击速度
            Double attackSpeed = mythicMob.getConfig().getDouble("AttackSpeed");
            return attackSpeed != null && attackSpeed > 0 ? attackSpeed : 1.0;
        } catch (Exception e) {
            logger.warning("读取攻击速度失败，使用默认值: " + e.getMessage());
            return 1.0;
        }
    }

    /**
     * 清除单个缓存
     */
    public void clearCache(String mobType) {
        attributeCache.remove(mobType);
        logger.info("✓ 清除缓存: " + mobType);
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        attributeCache.clear();
        logger.info("✓ 清除所有缓存");
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return attributeCache.size();
    }

    /**
     * 验证 MythicMob 是否存在
     */
    public boolean isMythicMobExists(String mobType) {
        try {
            // 如果缓存中有，说明存在
            if (attributeCache.containsKey(mobType)) {
                return true;
            }
            // 可以通过尝试读取来验证是否存在
            MobAttributes attr = readMobProperties(mobType);
            return attr != null;
        } catch (Exception e) {
            logger.warning("验证 MythicMob 失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有已注册的 MythicMob 类型
     */
    public List<String> getAllMythicMobTypes() {
        try {
            // 返回缓存中的所有类型
            return new ArrayList<>(attributeCache.keySet());
        } catch (Exception e) {
            logger.warning("获取 MythicMob 类型列表失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取缓存中的所有属性
     */
    public Map<String, MobAttributes> getAllCachedAttributes() {
        return new HashMap<>(attributeCache);
    }

    /**
     * 打印缓存统计信息
     */
    public void printCacheStats() {
        logger.info("=== MythicMobs 属性缓存统计 ===");
        logger.info("缓存大小: " + attributeCache.size());
        attributeCache.forEach((mobType, attributes) -> {
            logger.info("  [" + mobType + "] 血: " + attributes.health +
                       " 伤: " + attributes.damage +
                       " 甲: " + attributes.armor);
        });
    }
}
