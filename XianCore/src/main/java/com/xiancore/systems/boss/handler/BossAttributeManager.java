package com.xiancore.systems.boss.handler;

import com.xiancore.XianCore;
import com.xiancore.integration.mythic.MythicIntegration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.logging.Logger;

/**
 * Boss属性管理器 - 管理Boss的属性(HP、伤害、掉落等)
 *
 * 职责:
 * - 应用Boss等级修饰符
 * - 管理Boss血量倍数
 * - 管理Boss伤害倍数
 * - 应用掉落表规则
 * - 存储和检索Boss属性
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-16
 */
public class BossAttributeManager {

    private final XianCore plugin;
    private final Logger logger;
    private final MythicIntegration mythicIntegration;

    // 属性乘数配置
    private static final double[] TIER_HEALTH_MULTIPLIERS = {
        1.0,    // Tier 1: 基础血量
        1.5,    // Tier 2: 1.5倍
        2.0,    // Tier 3: 2.0倍
        2.5     // Tier 4: 2.5倍
    };

    private static final double[] TIER_DAMAGE_MULTIPLIERS = {
        1.0,    // Tier 1: 基础伤害
        1.3,    // Tier 2: 1.3倍
        1.6,    // Tier 3: 1.6倍
        2.0     // Tier 4: 2.0倍
    };

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param mythicIntegration MythicIntegration集成
     */
    public BossAttributeManager(XianCore plugin, MythicIntegration mythicIntegration) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.mythicIntegration = mythicIntegration;
    }

    /**
     * 应用Boss属性修饰符（根据等级）
     *
     * @param entity Boss实体
     * @param tier Boss等级 (1-4)
     */
    public void applyBossAttributesByTier(LivingEntity entity, int tier) {
        try {
            if (tier < 1 || tier > 4) {
                logger.warning("✗ 无效的Boss等级: " + tier + ", 使用默认值1");
                tier = 1;
            }

            // 获取倍数
            double healthMultiplier = TIER_HEALTH_MULTIPLIERS[tier - 1];
            double damageMultiplier = TIER_DAMAGE_MULTIPLIERS[tier - 1];

            // 应用血量
            applyHealthMultiplier(entity, healthMultiplier);

            // 应用伤害
            applyDamageMultiplier(entity, damageMultiplier);

            // 保存属性到PDC
            saveBossAttributesToPDC(entity, tier, healthMultiplier, damageMultiplier);

            logger.info("✓ 应用Boss属性: Tier " + tier +
                    " (血量倍数: " + String.format("%.1f", healthMultiplier) +
                    "x, 伤害倍数: " + String.format("%.1f", damageMultiplier) + "x)");

        } catch (Exception e) {
            logger.warning("✗ 应用Boss属性失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用自定义血量倍数
     *
     * @param entity Boss实体
     * @param multiplier 血量倍数
     */
    public void applyHealthMultiplier(LivingEntity entity, double multiplier) {
        try {
            double baseHealth = entity.getMaxHealth();
            double newHealth = baseHealth * multiplier;

            // 限制最大血量（防止溢出）
            newHealth = Math.min(newHealth, 2048); // Minecraft最大血量限制

            entity.setMaxHealth(newHealth);
            entity.setHealth(newHealth);

            logger.fine("✓ 设置Boss血量: " + String.format("%.1f", newHealth) +
                    " (倍数: " + String.format("%.1f", multiplier) + "x)");

        } catch (Exception e) {
            logger.warning("✗ 设置血量失败: " + e.getMessage());
        }
    }

    /**
     * 应用自定义伤害倍数
     * 通过PDC存储，由MythicMobs配置使用
     *
     * @param entity Boss实体
     * @param multiplier 伤害倍数
     */
    public void applyDamageMultiplier(LivingEntity entity, double multiplier) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            NamespacedKey damageKey = new NamespacedKey(plugin, "boss_damage_multiplier");

            pdc.set(damageKey, PersistentDataType.DOUBLE, multiplier);

            logger.fine("✓ 设置Boss伤害倍数: " + String.format("%.1f", multiplier) + "x");

        } catch (Exception e) {
            logger.warning("✗ 设置伤害倍数失败: " + e.getMessage());
        }
    }

    /**
     * 保存Boss属性到PDC
     *
     * @param entity Boss实体
     * @param tier 等级
     * @param healthMultiplier 血量倍数
     * @param damageMultiplier 伤害倍数
     */
    private void saveBossAttributesToPDC(LivingEntity entity, int tier,
                                        double healthMultiplier, double damageMultiplier) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();

            // 保存等级
            NamespacedKey tierKey = new NamespacedKey(plugin, "boss_tier");
            pdc.set(tierKey, PersistentDataType.INTEGER, tier);

            // 保存血量倍数
            NamespacedKey healthKey = new NamespacedKey(plugin, "boss_health_multiplier");
            pdc.set(healthKey, PersistentDataType.DOUBLE, healthMultiplier);

            // 保存伤害倍数
            NamespacedKey damageKey = new NamespacedKey(plugin, "boss_damage_multiplier");
            pdc.set(damageKey, PersistentDataType.DOUBLE, damageMultiplier);

        } catch (Exception e) {
            logger.warning("✗ 保存Boss属性到PDC失败: " + e.getMessage());
        }
    }

    /**
     * 从PDC读取Boss属性
     *
     * @param entity Boss实体
     * @return Boss属性信息字符串
     */
    public String readBossAttributesFromPDC(LivingEntity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();

            NamespacedKey tierKey = new NamespacedKey(plugin, "boss_tier");
            NamespacedKey healthKey = new NamespacedKey(plugin, "boss_health_multiplier");
            NamespacedKey damageKey = new NamespacedKey(plugin, "boss_damage_multiplier");

            Integer tier = pdc.get(tierKey, PersistentDataType.INTEGER);
            Double healthMult = pdc.get(healthKey, PersistentDataType.DOUBLE);
            Double damageMult = pdc.get(damageKey, PersistentDataType.DOUBLE);

            if (tier == null) tier = 1;
            if (healthMult == null) healthMult = 1.0;
            if (damageMult == null) damageMult = 1.0;

            return String.format("Tier:%d, 血量倍数:%.1fx, 伤害倍数:%.1fx",
                    tier, healthMult, damageMult);

        } catch (Exception e) {
            logger.warning("✗ 读取Boss属性失败: " + e.getMessage());
            return "Unknown";
        }
    }

    /**
     * 获取Boss等级
     *
     * @param entity Boss实体
     * @return 等级 (1-4)
     */
    public int getBossTier(LivingEntity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            NamespacedKey tierKey = new NamespacedKey(plugin, "boss_tier");
            Integer tier = pdc.get(tierKey, PersistentDataType.INTEGER);
            return tier != null ? tier : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 获取Boss血量倍数
     *
     * @param entity Boss实体
     * @return 血量倍数
     */
    public double getBossHealthMultiplier(LivingEntity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            NamespacedKey healthKey = new NamespacedKey(plugin, "boss_health_multiplier");
            Double mult = pdc.get(healthKey, PersistentDataType.DOUBLE);
            return mult != null ? mult : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }

    /**
     * 获取Boss伤害倍数
     *
     * @param entity Boss实体
     * @return 伤害倍数
     */
    public double getBossDamageMultiplier(LivingEntity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            NamespacedKey damageKey = new NamespacedKey(plugin, "boss_damage_multiplier");
            Double mult = pdc.get(damageKey, PersistentDataType.DOUBLE);
            return mult != null ? mult : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }

    /**
     * 清除Boss属性
     *
     * @param entity Boss实体
     */
    public void clearBossAttributes(LivingEntity entity) {
        try {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();

            NamespacedKey tierKey = new NamespacedKey(plugin, "boss_tier");
            NamespacedKey healthKey = new NamespacedKey(plugin, "boss_health_multiplier");
            NamespacedKey damageKey = new NamespacedKey(plugin, "boss_damage_multiplier");

            pdc.remove(tierKey);
            pdc.remove(healthKey);
            pdc.remove(damageKey);

            logger.fine("✓ Boss属性已清除");

        } catch (Exception e) {
            logger.warning("✗ 清除Boss属性失败: " + e.getMessage());
        }
    }

    // ==================== 静态工具方法 ====================

    /**
     * 获取指定等级的血量倍数
     *
     * @param tier 等级 (1-4)
     * @return 血量倍数
     */
    public static double getHealthMultiplierForTier(int tier) {
        if (tier < 1 || tier > 4) return 1.0;
        return TIER_HEALTH_MULTIPLIERS[tier - 1];
    }

    /**
     * 获取指定等级的伤害倍数
     *
     * @param tier 等级 (1-4)
     * @return 伤害倍数
     */
    public static double getDamageMultiplierForTier(int tier) {
        if (tier < 1 || tier > 4) return 1.0;
        return TIER_DAMAGE_MULTIPLIERS[tier - 1];
    }
}
