package com.xiancore.systems.forge.listeners;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.forge.items.EquipmentType;
import com.xiancore.systems.forge.items.MythicItemAdapter;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 装备属性应用监听器
 * 负责处理饰品和自定义属性（灵力值）的生效逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class EquipmentAttributeListener implements Listener {

    private final XianCore plugin;

    // 缓存玩家装备加成
    private final Map<UUID, EquipmentBonus> playerBonusCache = new HashMap<>();

    public EquipmentAttributeListener(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 装备加成数据结构
     */
    private static class EquipmentBonus {
        int attack = 0;
        int defense = 0;
        int hp = 0;
        int qi = 0;

        void reset() {
            attack = 0;
            defense = 0;
            hp = 0;
            qi = 0;
        }

        void add(EquipmentBonus other) {
            this.attack += other.attack;
            this.defense += other.defense;
            this.hp += other.hp;
            this.qi += other.qi;
        }
    }

    /**
     * 玩家加入时刷新装备属性
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        recalculatePlayerBonus(event.getPlayer());
    }

    /**
     * 玩家退出时清理缓存
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerBonusCache.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 玩家切换手持物品时刷新属性
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        // 延迟1tick刷新，确保物品已切换
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            recalculatePlayerBonus(event.getPlayer());
        }, 1L);
    }

    /**
     * 玩家造成伤害时应用攻击力加成
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        EquipmentBonus bonus = playerBonusCache.get(attacker.getUniqueId());
        if (bonus == null || bonus.attack == 0) {
            return;
        }

        // 增加基础伤害
        double originalDamage = event.getDamage();
        double attackBonus = bonus.attack * 0.5;  // 攻击力转换系数：每点+0.5伤害
        event.setDamage(originalDamage + attackBonus);

        plugin.getLogger().fine(attacker.getName() + " 攻击力加成: +" + bonus.attack + " (伤害+" + attackBonus + ")");
    }

    /**
     * 玩家受到伤害时应用防御力减免
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }

        EquipmentBonus bonus = playerBonusCache.get(defender.getUniqueId());
        if (bonus == null || bonus.defense == 0) {
            return;
        }

        // 减免伤害：防御力转换系数
        // 公式：减免率 = defense / (defense + 100)
        // 示例：100防御 = 50%减免，200防御 = 66.7%减免
        double originalDamage = event.getDamage();
        double reductionRate = bonus.defense / (double)(bonus.defense + 100);
        double reduction = originalDamage * reductionRate;
        
        event.setDamage(Math.max(0, originalDamage - reduction));

        plugin.getLogger().fine(defender.getName() + " 防御力加成: " + bonus.defense + " (减免: " + String.format("%.1f", reduction) + ")");
    }

    /**
     * 重新计算玩家的装备加成
     */
    private void recalculatePlayerBonus(Player player) {
        EquipmentBonus totalBonus = new EquipmentBonus();

        // 扫描所有装备槽位
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        // 计算每件装备的加成
        addItemBonus(totalBonus, mainHand);
        addItemBonus(totalBonus, offHand);
        addItemBonus(totalBonus, helmet);
        addItemBonus(totalBonus, chestplate);
        addItemBonus(totalBonus, leggings);
        addItemBonus(totalBonus, boots);

        // 更新缓存
        playerBonusCache.put(player.getUniqueId(), totalBonus);

        // 应用生命值和灵力值加成到玩家数据
        applyHealthAndQi(player, totalBonus);

        plugin.getLogger().fine(player.getName() + " 装备加成: 攻击+" + totalBonus.attack + " 防御+" + totalBonus.defense + " 生命+" + totalBonus.hp + " 灵力+" + totalBonus.qi);
    }

    /**
     * 计算单件装备的加成
     */
    private void addItemBonus(EquipmentBonus totalBonus, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        // 检查是否为仙家装备
        if (!MythicItemAdapter.isXianEquipment(plugin, item)) {
            return;
        }

        // 读取装备类型
        EquipmentType type = MythicItemAdapter.getEquipmentType(plugin, item);
        if (type == null) {
            return;
        }

        // 读取品质和强化等级
        String quality = MythicItemAdapter.getQuality(plugin, item);
        int enhanceLevel = MythicItemAdapter.getEnhanceLevel(plugin, item);

        // 根据装备类型、品质、强化等级和实际属性值计算属性加成
        EquipmentBonus itemBonus = calculateItemBonus(type, quality, enhanceLevel, item);
        totalBonus.add(itemBonus);
    }

    /**
     * 根据装备类型、品质、强化等级计算属性
     * 
     * 优先从PDC读取实际属性值（新装备），如果PDC为null（旧装备），则使用固定公式
     * 
     * @param type 装备类型
     * @param quality 品质
     * @param enhanceLevel 强化等级
     * @param item 物品（用于读取PDC属性值）
     * @return 装备加成
     */
    private EquipmentBonus calculateItemBonus(EquipmentType type, String quality, int enhanceLevel, ItemStack item) {
        EquipmentBonus bonus = new EquipmentBonus();

        // 尝试从PDC读取实际属性值（新装备）
        Integer baseAttack = MythicItemAdapter.getBaseAttack(plugin, item);
        Integer baseDefense = MythicItemAdapter.getBaseDefense(plugin, item);
        Integer baseHp = MythicItemAdapter.getBaseHp(plugin, item);
        Integer baseQi = MythicItemAdapter.getBaseQi(plugin, item);

        if (baseAttack != null && baseDefense != null && baseHp != null && baseQi != null) {
            // 新装备：使用实际属性值
            // 强化加成：每级+5%基础值
            double enhanceMultiplier = 1.0 + (enhanceLevel * 0.05);
            
            if (type.isWeapon()) {
                bonus.attack = (int)(baseAttack * enhanceMultiplier);
                bonus.qi = (int)(baseQi * enhanceMultiplier);
            } else if (type.isArmor()) {
                bonus.defense = (int)(baseDefense * enhanceMultiplier);
                bonus.hp = (int)(baseHp * enhanceMultiplier);
                bonus.qi = (int)(baseQi * enhanceMultiplier);
            } else if (type.isAccessory()) {
                // 饰品：全属性加成
                bonus.attack = (int)(baseAttack * enhanceMultiplier);
                bonus.defense = (int)(baseDefense * enhanceMultiplier);
                bonus.hp = (int)(baseHp * enhanceMultiplier);
                bonus.qi = (int)(baseQi * enhanceMultiplier);
            }
            
            plugin.getLogger().fine("使用实际属性值计算: 攻击=" + baseAttack + ", 防御=" + baseDefense + 
                    ", 生命=" + baseHp + ", 灵力=" + baseQi + ", 强化等级=" + enhanceLevel);
        } else {
            // 旧装备：使用固定公式（兼容处理）
            // 基础属性值（根据品质）
            // ⚠️ 已减半：考虑到服务器安装了 AuraSkills，避免属性过高
            int baseValue = switch (quality) {
                case "神品" -> 50;   // 原 100，减半
                case "仙品" -> 30;   // 原 60，减半
                case "宝品" -> 18;   // 原 35，减半
                case "灵品" -> 10;   // 原 20，减半
                default -> 5;        // 原 10，减半（凡品）
            };

            // 强化加成：每级+5%基础值
            double enhanceMultiplier = 1.0 + (enhanceLevel * 0.05);
            baseValue = (int)(baseValue * enhanceMultiplier);

            // 根据装备类型分配属性
            if (type.isWeapon()) {
                bonus.attack = baseValue * 2;
                bonus.qi = baseValue * 4;
            } else if (type.isArmor()) {
                bonus.defense = baseValue * 2;
                bonus.hp = baseValue * 10;
                bonus.qi = baseValue * 2;
            } else if (type.isAccessory()) {
                // 饰品：全属性加成
                bonus.attack = (int)(baseValue * 0.8);
                bonus.defense = (int)(baseValue * 0.5);
                bonus.hp = baseValue * 4;
                bonus.qi = baseValue * 3;
            }
            
            plugin.getLogger().fine("使用固定公式计算（旧装备）: 品质=" + quality + ", baseValue=" + baseValue + 
                    ", 强化等级=" + enhanceLevel);
        }

        return bonus;
    }

    /**
     * 应用生命值和灵力值加成
     */
    private void applyHealthAndQi(Player player, EquipmentBonus bonus) {
        // 应用生命值加成到原版血量上限
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            // 基础血量 + 装备加成（每点HP = 1血量）
            double baseHealth = 20.0;  // 默认20血
            double totalHealth = baseHealth + (bonus.hp * 0.1);  // 每点HP加成+0.1血（可调整）
            totalHealth = Math.min(2048, totalHealth);  // 限制最大值

            maxHealthAttr.setBaseValue(totalHealth);
            
            // 确保当前血量不超过上限
            if (player.getHealth() > totalHealth) {
                player.setHealth(totalHealth);
            }
        }

        // 应用灵力值加成到 PlayerData
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data != null) {
            // 假设 PlayerData 有灵力值字段（如果没有则需要添加）
            // data.setMaxQi(baseQi + bonus.qi);
            // data.setCurrentQi(Math.min(data.getCurrentQi(), data.getMaxQi()));
            // plugin.getDataManager().savePlayerData(data);
        }
    }

    /**
     * 手动触发玩家属性刷新（供外部调用，如强化/装备变更后）
     */
    public void refreshPlayer(Player player) {
        recalculatePlayerBonus(player);
    }

    /**
     * 获取玩家当前装备加成（供外部查询）
     */
    public int getPlayerAttackBonus(Player player) {
        EquipmentBonus bonus = playerBonusCache.get(player.getUniqueId());
        return bonus != null ? bonus.attack : 0;
    }

    public int getPlayerDefenseBonus(Player player) {
        EquipmentBonus bonus = playerBonusCache.get(player.getUniqueId());
        return bonus != null ? bonus.defense : 0;
    }

    public int getPlayerHpBonus(Player player) {
        EquipmentBonus bonus = playerBonusCache.get(player.getUniqueId());
        return bonus != null ? bonus.hp : 0;
    }

    public int getPlayerQiBonus(Player player) {
        EquipmentBonus bonus = playerBonusCache.get(player.getUniqueId());
        return bonus != null ? bonus.qi : 0;
    }
}


