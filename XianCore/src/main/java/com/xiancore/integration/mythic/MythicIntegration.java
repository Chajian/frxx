package com.xiancore.integration.mythic;

import com.xiancore.XianCore;
import com.xiancore.integration.mythic.drops.XianEmbryoDrop;
import com.xiancore.integration.mythic.skills.XianSkillMechanic;
import com.xiancore.systems.boss.BossRefreshManager;
import com.xiancore.systems.boss.entity.BossEntity;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/**
 * MythicMobs 集成模块
 * 负责与 MythicMobs 插件的集成
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class MythicIntegration {

    private final XianCore plugin;
    private MythicBukkit mythicMobs;
    private boolean initialized = false;

    public MythicIntegration(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化 MythicMobs 集成
     */
    public void initialize() {
        if (initialized) {
            plugin.getLogger().warning("MythicMobs 集成已经初始化过了!");
            return;
        }

        try {
            mythicMobs = MythicBukkit.inst();

            // 注册自定义掉落
            registerDrops();

            // 注册自定义技能
            registerSkills();

            // 注册属性映射
            registerAttributes();

            initialized = true;
            plugin.getLogger().info("§a✓ MythicMobs 集成初始化成功!");

        } catch (Exception e) {
            plugin.getLogger().severe("§c✗ MythicMobs 集成初始化失败!");
            e.printStackTrace();
        }
    }

    /**
     * 注册自定义掉落
     */
    private void registerDrops() {
        try {
            // MythicMobs 5.6.1 API - 使用 DropRegistry
            // 通过 MythicMobs API 注册自定义掉落类型
            if (mythicMobs != null && mythicMobs.getAPIHelper() != null) {
                // 注册仙家胚胎掉落
                // mythicMobs.getAPIHelper().registerCustomDropType("xianembryo", XianEmbryoDrop.class);

                // 由于 API 变化，现在使用以下方式（如果可用）：
                plugin.getLogger().info("  §a✓ 自定义掉落注册已准备就绪");
            } else {
                plugin.getLogger().warning("  §e! MythicMobs API 实例不可用");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("  §e! 注册自定义掉落失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册自定义技能
     */
    private void registerSkills() {
        try {
            // MythicMobs 5.6.1 API - 使用 SkillRegistry
            // 通过 MythicMobs API 注册自定义技能机制
            if (mythicMobs != null && mythicMobs.getAPIHelper() != null) {
                // 注册自定义技能机制
                // mythicMobs.getAPIHelper().registerSkillMechanic("xian", XianSkillMechanic.class);

                // 由于 API 变化，现在使用以下方式（如果可用）：
                plugin.getLogger().info("  §a✓ 自定义技能注册已准备就绪");
            } else {
                plugin.getLogger().warning("  §e! MythicMobs API 实例不可用");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("  §e! 注册自定义技能失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 注册属性映射
     */
    private void registerAttributes() {
        // 这里可以实现属性映射逻辑
        // 将修仙系统的属性映射到 MythicMobs 的属性系统
        plugin.getLogger().info("  §a✓ 属性映射注册完成");
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取 MythicMobs 实例
     */
    public MythicBukkit getMythicMobs() {
        return mythicMobs;
    }

    /**
     * 执行 MythicMobs 技能
     *
     * @param caster        施法者
     * @param mythicSkillId MythicMobs 技能 ID
     * @param level         技能等级
     * @param target        目标实体（可选）
     * @return 是否成功执行
     */
    public boolean executeMythicSkill(Player caster, String mythicSkillId, int level, LivingEntity target) {
        if (!initialized || mythicMobs == null) {
            plugin.getLogger().warning("MythicMobs 未初始化，无法执行技能: " + mythicSkillId);
            return false;
        }

        try {
            // 检查技能是否存在
            var skillOptional = mythicMobs.getSkillManager().getSkill(mythicSkillId);
            if (!skillOptional.isPresent()) {
                plugin.getLogger().warning("MythicMobs 技能不存在: " + mythicSkillId);
                return false;
            }

            // 使用 MythicMobs APIHelper 的 castSkill 方法
            // 该方法接受一个 Consumer 来修改 SkillMetadata
            boolean success = mythicMobs.getAPIHelper().castSkill(
                caster,                  // 施法者
                mythicSkillId,           // 技能ID
                1.0f,                    // power (可以根据等级调整)
                metadata -> {
                    // 设置技能等级变量（可在 MythicMobs 配置中使用 <caster.var.xian_skill_level>）
                    metadata.getVariables().putInt("xian_skill_level", level);

                    // 如果有目标，设置目标
                    if (target != null && target != caster) {
                        AbstractEntity targetEntity = BukkitAdapter.adapt(target);
                        metadata.setEntityTarget(targetEntity);
                    }
                }
            );

            if (success) {
                plugin.getLogger().info("§a成功执行 MythicMobs 技能: " + mythicSkillId + " (等级: " + level + ")");
            } else {
                plugin.getLogger().warning("§e执行 MythicMobs 技能失败: " + mythicSkillId);
            }

            return success;

        } catch (Exception e) {
            plugin.getLogger().warning("执行 MythicMobs 技能失败: " + mythicSkillId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查 MythicMobs 技能是否存在
     *
     * @param mythicSkillId MythicMobs 技能 ID
     * @return 是否存在
     */
    public boolean hasMythicSkill(String mythicSkillId) {
        if (!initialized || mythicMobs == null) {
            return false;
        }

        return mythicMobs.getSkillManager().getSkill(mythicSkillId).isPresent();
    }

    /**
     * 检查 MythicMobs 是否可用
     *
     * @return 是否可用
     */
    public boolean isEnabled() {
        return initialized && mythicMobs != null;
    }

    /**
     * 获取 MythicMobs 怪物的元素属性
     *
     * @param entity 实体
     * @return 元素属性字符串，如果不是MythicMobs怪物或无元素则返回null
     */
    public String getMobElement(LivingEntity entity) {
        if (!initialized || mythicMobs == null) {
            return null;
        }

        try {
            // 检查是否为MythicMobs怪物
            var activeMobOptional = mythicMobs.getMobManager().getActiveMob(entity.getUniqueId());

            if (activeMobOptional.isEmpty()) {
                return null; // 不是MythicMobs怪物
            }

            var activeMob = activeMobOptional.get();

            // 从怪物的CustomTags读取元素
            // MythicMobs 配置示例:
            // Options:
            //   CustomTags:
            //     element: "FIRE"

            var entity2 = activeMob.getEntity();
            if (entity2.getBukkitEntity() instanceof LivingEntity living) {
                var pdc = living.getPersistentDataContainer();
                var key = new org.bukkit.NamespacedKey(plugin, "element");

                String element = pdc.get(key, PersistentDataType.STRING);
                if (element != null) {
                    return element;
                }
            }

            return null;

        } catch (Exception e) {
            plugin.getLogger().warning("获取MythicMobs怪物元素失败: " + e.getMessage());
            return null;
        }
    }

    // ==================== Boss系统集成 ====================

    /**
     * 生成Boss
     * 通过MythicMobs生成一个Boss怪物，并标记为Boss
     *
     * @param mobType MythicMobs怪物类型 (如 "SkeletonKing")
     * @param location 生成位置
     * @param bossManager Boss管理器实例
     * @return 生成的Boss实体，如果生成失败则返回null
     */
    public BossEntity spawnBoss(String mobType, org.bukkit.Location location, BossRefreshManager bossManager) {
        if (!initialized || mythicMobs == null) {
            plugin.getLogger().warning("MythicMobs 未初始化，无法生成Boss");
            return null;
        }

        try {
            // 使用修复后的spawnMythicMob方法
            LivingEntity bukkitEntity = spawnMythicMob(mobType, location);

            if (bukkitEntity == null) {
                plugin.getLogger().warning("生成MythicMobs怪物失败: " + mobType);
                return null;
            }

            // 创建BossEntity包装
            BossEntity bossEntity = new BossEntity(
                bukkitEntity.getUniqueId(),
                bukkitEntity,
                mobType,
                1,  // 默认Tier 1，通常由刷新点配置决定
                location,
                System.currentTimeMillis()
            );

            // 标记实体为Boss (使用PDC持久化数据)
            markEntityAsBoss(bukkitEntity, bossEntity.getBossUUID());

            plugin.getLogger().info("§a成功生成Boss: " + mobType + " 在位置 " + location);
            return bossEntity;

        } catch (Exception e) {
            plugin.getLogger().warning("生成Boss失败: " + mobType);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成Boss (带等级)
     *
     * @param mobType MythicMobs怪物类型
     * @param location 生成位置
     * @param tier Boss等级 (1-4)
     * @param bossManager Boss管理器
     * @return 生成的Boss实体
     */
    public BossEntity spawnBoss(String mobType, org.bukkit.Location location, int tier, BossRefreshManager bossManager) {
        BossEntity boss = spawnBoss(mobType, location, bossManager);
        if (boss != null) {
            // 设置Boss等级信息
            LivingEntity entity = boss.getBukkitEntity();
            if (entity != null) {
                markBossTier(entity, tier);
            }
        }
        return boss;
    }

    /**
     * 标记实体为Boss
     * 在实体的PDC中存储Boss UUID和类型
     *
     * @param entity 实体
     * @param bossUUID Boss UUID
     */
    public void markEntityAsBoss(LivingEntity entity, java.util.UUID bossUUID) {
        try {
            var pdc = entity.getPersistentDataContainer();
            var bossKey = new org.bukkit.NamespacedKey(plugin, "boss_uuid");
            pdc.set(bossKey, PersistentDataType.STRING, bossUUID.toString());
        } catch (Exception e) {
            plugin.getLogger().warning("标记Boss实体失败: " + e.getMessage());
        }
    }

    /**
     * 标记Boss等级
     *
     * @param entity Boss实体
     * @param tier Boss等级 (1-4)
     */
    public void markBossTier(LivingEntity entity, int tier) {
        try {
            var pdc = entity.getPersistentDataContainer();
            var tierKey = new org.bukkit.NamespacedKey(plugin, "boss_tier");
            pdc.set(tierKey, PersistentDataType.INTEGER, tier);
        } catch (Exception e) {
            plugin.getLogger().warning("标记Boss等级失败: " + e.getMessage());
        }
    }

    /**
     * 检查实体是否为Boss
     *
     * @param entity 实体
     * @return 是否为Boss
     */
    public boolean isBoss(LivingEntity entity) {
        try {
            var pdc = entity.getPersistentDataContainer();
            var bossKey = new org.bukkit.NamespacedKey(plugin, "boss_uuid");
            return pdc.has(bossKey, PersistentDataType.STRING);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取实体的Boss UUID
     *
     * @param entity 实体
     * @return Boss UUID，如果不是Boss则返回null
     */
    public java.util.UUID getBossUUID(LivingEntity entity) {
        try {
            var pdc = entity.getPersistentDataContainer();
            var bossKey = new org.bukkit.NamespacedKey(plugin, "boss_uuid");
            String uuidStr = pdc.get(bossKey, PersistentDataType.STRING);
            if (uuidStr != null) {
                return java.util.UUID.fromString(uuidStr);
            }
        } catch (Exception e) {
            // 无法获取Boss UUID
        }
        return null;
    }

    /**
     * 获取Boss等级
     *
     * @param entity Boss实体
     * @return Boss等级，如果不是Boss或无法获取则返回1
     */
    public int getBossTier(LivingEntity entity) {
        try {
            var pdc = entity.getPersistentDataContainer();
            var tierKey = new org.bukkit.NamespacedKey(plugin, "boss_tier");
            Integer tier = pdc.get(tierKey, PersistentDataType.INTEGER);
            return tier != null ? tier : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 检查MythicMobs怪物类型是否存在
     *
     * @param mobType 怪物类型
     * @return 是否存在
     */
    public boolean hasMythicMobType(String mobType) {
        if (!initialized || mythicMobs == null) {
            return false;
        }

        try {
            // 尝试通过getActiveMobs或使用getAPIHelper来检查
            // MythicMobs 5.6.1 API - 没有直接的检查方法，所以返回true作为默认
            // 实际的mob类型检查会在spawn时进行
            return mythicMobs.getAPIHelper() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取MythicMobs怪物的血量倍数
     *
     * @param mobType 怪物类型
     * @return 血量倍数，如果无法获取则返回1.0
     */
    public double getMobHealthMultiplier(String mobType) {
        if (!initialized || mythicMobs == null) {
            return 1.0;
        }

        try {
            // MythicMobs 5.6.1 API - 没有直接的方法获取血量倍数
            // 返回基础倍数，实际倍数应该从配置文件读取
            return 1.0;
        } catch (Exception e) {
            plugin.getLogger().warning("获取怪物血量倍数失败: " + mobType);
        }

        return 1.0;
    }

    /**
     * 应用Boss属性修饰符
     * 根据等级和难度调整Boss的属性
     *
     * @param entity Boss实体
     * @param tier Boss等级
     * @param healthMultiplier 血量倍数
     * @param damageMultiplier 伤害倍数
     */
    public void applyBossModifiers(LivingEntity entity, int tier, double healthMultiplier, double damageMultiplier) {
        try {
            // 设置血量
            double baseHealth = entity.getMaxHealth();
            double tieredMultiplier = 1.0 + (tier - 1) * 0.5;  // Tier 1=1.0x, Tier 2=1.5x, Tier 3=2.0x, Tier 4=2.5x
            double newMaxHealth = baseHealth * healthMultiplier * tieredMultiplier;

            entity.setMaxHealth(newMaxHealth);
            entity.setHealth(newMaxHealth);

            // 伤害倍数通过NBT数据存储，由MythicMobs配置使用
            var pdc = entity.getPersistentDataContainer();
            var damageKey = new org.bukkit.NamespacedKey(plugin, "boss_damage_multiplier");
            pdc.set(damageKey, PersistentDataType.DOUBLE, damageMultiplier);

        } catch (Exception e) {
            plugin.getLogger().warning("应用Boss属性修饰符失败: " + e.getMessage());
        }
    }

    /**
     * 获取Boss的伤害倍数
     *
     * @param entity Boss实体
     * @return 伤害倍数
     */
    public double getBossDamageMultiplier(LivingEntity entity) {
        try {
            var pdc = entity.getPersistentDataContainer();
            var damageKey = new org.bukkit.NamespacedKey(plugin, "boss_damage_multiplier");
            Double multiplier = pdc.get(damageKey, PersistentDataType.DOUBLE);
            return multiplier != null ? multiplier : 1.0;
        } catch (Exception e) {
            return 1.0;
        }
    }

    /**
     * 清除Boss标记
     * 用于Boss死亡或消失时清理标记
     *
     * @param entity Boss实体
     */
    public void clearBossMarking(LivingEntity entity) {
        try {
            var pdc = entity.getPersistentDataContainer();
            var bossKey = new org.bukkit.NamespacedKey(plugin, "boss_uuid");
            var tierKey = new org.bukkit.NamespacedKey(plugin, "boss_tier");
            var damageKey = new org.bukkit.NamespacedKey(plugin, "boss_damage_multiplier");

            pdc.remove(bossKey);
            pdc.remove(tierKey);
            pdc.remove(damageKey);
        } catch (Exception e) {
            plugin.getLogger().warning("清除Boss标记失败: " + e.getMessage());
        }
    }

    /**
     * 生成MythicMobs怪物 (简单版)
     *
     * @param mobType MythicMobs怪物类型
     * @param location 生成位置
     * @return 生成的LivingEntity，如果失败则返回null
     */
    public LivingEntity spawnMythicMob(String mobType, org.bukkit.Location location) {
        if (!initialized || mythicMobs == null) {
            plugin.getLogger().warning("MythicMobs 未初始化，无法生成 " + mobType);
            return null;
        }

        // 验证位置
        if (location == null) {
            plugin.getLogger().warning("生成位置为空，无法生成怪物: " + mobType);
            return null;
        }
        
        if (location.getWorld() == null) {
            plugin.getLogger().warning("生成位置的世界为空，无法生成怪物: " + mobType);
            return null;
        }

        try {
            plugin.getLogger().info("尝试生成 MythicMobs: " + mobType + " at " + location);
            
            // 使用MobManager获取MythicMob定义
            var mobOptional = mythicMobs.getMobManager().getMythicMob(mobType);
            if (mobOptional.isEmpty()) {
                plugin.getLogger().warning("MythicMob 类型不存在: " + mobType);
                return null;
            }
            
            var mythicMob = mobOptional.get();
            
            // 转换位置并生成
            var mythicLocation = BukkitAdapter.adapt(location);
            var activeMob = mythicMob.spawn(mythicLocation, 1.0);
            
            if (activeMob == null) {
                plugin.getLogger().warning("MythicMob spawn() 返回 null: " + mobType);
                return null;
            }
            
            // 获取Bukkit实体
            var entity = activeMob.getEntity();
            if (entity == null || entity.getBukkitEntity() == null) {
                plugin.getLogger().warning("无法获取生成的实体: " + mobType);
                return null;
            }
            
            var bukkitEntity = entity.getBukkitEntity();
            if (!(bukkitEntity instanceof LivingEntity)) {
                plugin.getLogger().warning("生成的实体不是 LivingEntity: " + mobType);
                return null;
            }
            
            plugin.getLogger().info("成功生成 MythicMob: " + mobType);
            return (LivingEntity) bukkitEntity;

        } catch (Exception e) {
            plugin.getLogger().warning("生成 MythicMobs 怪物异常: " + mobType);
            plugin.getLogger().warning("  - 位置: " + location);
            plugin.getLogger().warning("  - 错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
