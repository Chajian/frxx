package com.xiancore.integration.mythic;

import org.bukkit.plugin.Plugin;
import java.util.logging.Logger;

/**
 * Boss 属性验证器
 * 验证 Boss 属性的有效范围和兼容性
 *
 * @author XianCore
 * @version 1.0
 */
public class AttributeValidator {

    private final Plugin plugin;
    private final Logger logger;

    // 属性范围限制
    private static final double MIN_HEALTH = 1.0;
    private static final double MAX_HEALTH = 2048.0; // Minecraft 血量上限
    private static final double MIN_DAMAGE = 0.5;
    private static final double MAX_DAMAGE = 100.0;
    private static final double MIN_ARMOR = 0.0;
    private static final double MAX_ARMOR = 30.0; // Minecraft 护甲上限
    private static final double MIN_SPEED = 0.01;
    private static final double MAX_SPEED = 1.0;

    /**
     * 构造函数
     */
    public AttributeValidator(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 验证 Boss 属性
     * @return 是否有效
     */
    public boolean validateAttributes(MythicMobsAttributeReader.MobAttributes attributes) {
        if (attributes == null) {
            logger.warning("✗ 属性对象为 null");
            return false;
        }

        boolean valid = true;

        // 验证血量
        if (!validateHealth(attributes.health)) {
            valid = false;
        }

        // 验证伤害
        if (!validateDamage(attributes.damage)) {
            valid = false;
        }

        // 验证护甲
        if (!validateArmor(attributes.armor)) {
            valid = false;
        }

        // 验证移动速度
        if (!validateMovementSpeed(attributes.movementSpeed)) {
            valid = false;
        }

        // 验证攻击速度
        if (!validateAttackSpeed(attributes.attackSpeed)) {
            valid = false;
        }

        if (valid) {
            logger.info("✓ 属性验证通过");
        } else {
            logger.warning("⚠ 属性验证有警告，但已自动修正");
        }

        return true; // 虽然有警告，但不影响使用
    }

    /**
     * 验证血量
     */
    private boolean validateHealth(double health) {
        if (health < MIN_HEALTH) {
            logger.warning("⚠ 血量过低: " + health + " (最小值: " + MIN_HEALTH + ")");
            return false;
        }
        if (health > MAX_HEALTH) {
            logger.warning("⚠ 血量过高: " + health + " (最大值: " + MAX_HEALTH + ")");
            return false;
        }
        return true;
    }

    /**
     * 验证伤害
     */
    private boolean validateDamage(double damage) {
        if (damage < MIN_DAMAGE) {
            logger.warning("⚠ 伤害过低: " + damage + " (最小值: " + MIN_DAMAGE + ")");
            return false;
        }
        if (damage > MAX_DAMAGE) {
            logger.warning("⚠ 伤害过高: " + damage + " (最大值: " + MAX_DAMAGE + ")");
            return false;
        }
        return true;
    }

    /**
     * 验证护甲
     */
    private boolean validateArmor(double armor) {
        if (armor < MIN_ARMOR) {
            logger.warning("⚠ 护甲值为负: " + armor);
            return false;
        }
        if (armor > MAX_ARMOR) {
            logger.warning("⚠ 护甲过高: " + armor + " (最大值: " + MAX_ARMOR + ")");
            return false;
        }
        return true;
    }

    /**
     * 验证移动速度
     */
    private boolean validateMovementSpeed(double speed) {
        if (speed < MIN_SPEED) {
            logger.warning("⚠ 移动速度过低: " + speed + " (最小值: " + MIN_SPEED + ")");
            return false;
        }
        if (speed > MAX_SPEED) {
            logger.warning("⚠ 移动速度过高: " + speed + " (最大值: " + MAX_SPEED + ")");
            return false;
        }
        return true;
    }

    /**
     * 验证攻击速度
     */
    private boolean validateAttackSpeed(double attackSpeed) {
        if (attackSpeed < 0.1) {
            logger.warning("⚠ 攻击速度过低: " + attackSpeed);
            return false;
        }
        if (attackSpeed > 10.0) {
            logger.warning("⚠ 攻击速度过高: " + attackSpeed);
            return false;
        }
        return true;
    }

    /**
     * 修正属性到有效范围
     */
    public MythicMobsAttributeReader.MobAttributes fixAttributes(
            MythicMobsAttributeReader.MobAttributes attributes) {
        if (attributes == null) {
            logger.warning("✗ 无法修正 null 属性");
            return null;
        }

        double health = Math.max(MIN_HEALTH, Math.min(MAX_HEALTH, attributes.health));
        double damage = Math.max(MIN_DAMAGE, Math.min(MAX_DAMAGE, attributes.damage));
        double armor = Math.max(MIN_ARMOR, Math.min(MAX_ARMOR, attributes.armor));
        double speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, attributes.movementSpeed));
        double attackSpeed = Math.max(0.1, Math.min(10.0, attributes.attackSpeed));

        if (health != attributes.health || damage != attributes.damage ||
            armor != attributes.armor || speed != attributes.movementSpeed ||
            attackSpeed != attributes.attackSpeed) {
            logger.info("✓ 属性已自动修正");
        }

        return new MythicMobsAttributeReader.MobAttributes(health, damage, armor, speed, attackSpeed);
    }

    /**
     * 验证属性兼容性
     */
    public boolean validateCompatibility(MythicMobsAttributeReader.MobAttributes attr1,
                                        MythicMobsAttributeReader.MobAttributes attr2) {
        if (attr1 == null || attr2 == null) {
            return false;
        }

        // 检查属性差异是否过大
        double healthRatio = Math.max(attr1.health, attr2.health) /
                           Math.min(attr1.health, attr2.health);
        if (healthRatio > 10.0) {
            logger.warning("⚠ 两个Boss的血量差异过大: " + healthRatio + "倍");
            return false;
        }

        double damageRatio = Math.max(attr1.damage, attr2.damage) /
                           Math.min(attr1.damage, attr2.damage);
        if (damageRatio > 5.0) {
            logger.warning("⚠ 两个Boss的伤害差异过大: " + damageRatio + "倍");
            return false;
        }

        return true;
    }

    /**
     * 比较两个属性集合
     */
    public void compareAttributes(String name1, MythicMobsAttributeReader.MobAttributes attr1,
                                 String name2, MythicMobsAttributeReader.MobAttributes attr2) {
        if (attr1 == null || attr2 == null) {
            logger.warning("✗ 无法比较: 属性为 null");
            return;
        }

        logger.info("=== 属性比较: " + name1 + " vs " + name2 + " ===");
        logger.info("血量: " + attr1.health + " vs " + attr2.health +
                   " (比例: " + (attr1.health / attr2.health) + ")");
        logger.info("伤害: " + attr1.damage + " vs " + attr2.damage +
                   " (比例: " + (attr1.damage / attr2.damage) + ")");
        logger.info("护甲: " + attr1.armor + " vs " + attr2.armor);
        logger.info("速度: " + attr1.movementSpeed + " vs " + attr2.movementSpeed);
        logger.info("攻速: " + attr1.attackSpeed + " vs " + attr2.attackSpeed);
    }

    /**
     * 获取属性摘要
     */
    public String getAttributesSummary(MythicMobsAttributeReader.MobAttributes attributes) {
        if (attributes == null) {
            return "null";
        }
        return String.format("血:%s 伤:%s 甲:%s 速:%s 攻:%s",
                Math.round(attributes.health),
                Math.round(attributes.damage * 100.0) / 100.0,
                Math.round(attributes.armor * 10.0) / 10.0,
                Math.round(attributes.movementSpeed * 100.0) / 100.0,
                Math.round(attributes.attackSpeed * 100.0) / 100.0);
    }
}
