package com.xiancore.listeners;

import com.xiancore.XianCore;
import com.xiancore.systems.skill.Skill;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功法快捷键监听器
 * 监听玩家按F键(副手切换)事件，触发功法施放
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillKeybindListener implements Listener {

    private final XianCore plugin;
    
    // 防抖机制：记录每个玩家上次触发时间
    private final Map<UUID, Long> lastCastTime = new ConcurrentHashMap<>();
    
    // 防抖时间：100毫秒（可配置）
    private static final long DEBOUNCE_TIME = 100L;

    public SkillKeybindListener(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听副手切换事件（F键）
     * 
     * @param event 事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 检查配置：是否启用快捷键系统
        boolean keybindEnabled = plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.keybind.enabled", true);

        if (!keybindEnabled) {
            return; // 未启用，不处理
        }

        // 1. 防抖检查：防止快速连击导致的多个事件
        long now = System.currentTimeMillis();
        Long lastTime = lastCastTime.get(uuid);
        if (lastTime != null && (now - lastTime) < DEBOUNCE_TIME) {
            // 忽略过快的事件，取消事件但不处理
            event.setCancelled(true);
            return;
        }
        lastCastTime.put(uuid, now);

        // 2. 立即捕获槽位值（快照），避免后续读取时值不一致
        // 获取玩家当前快捷栏位置 (0-8) 转换为 (1-9)
        final int slot = player.getInventory().getHeldItemSlot() + 1;

        // 3. 获取绑定的功法ID（使用快照的 slot 值）
        String skillId = plugin.getSkillSystem().getBindManager().getBinding(player, slot);

        // 如果没有绑定功法，不处理（让原版F键功能继续）
        if (skillId == null) {
            // 可选：显示提示
            boolean showHint = plugin.getConfigManager().getConfig("config")
                    .getBoolean("skill.keybind.show-empty-slot-hint", false);
            
            if (showHint) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                    new TextComponent("§7槽位 " + slot + " 未绑定功法"));
            }
            return;
        }

        // 取消副手切换事件（拦截F键）
        event.setCancelled(true);

        // 检查是否允许手持物品时施放
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
            boolean allowWithItem = plugin.getConfigManager().getConfig("config")
                    .getBoolean("skill.keybind.allow-with-item", true);

            if (!allowWithItem) {
                player.sendMessage("§c手持物品时无法施放功法! 请切换到空手");
                return;
            }
        }

        // 检查功法是否存在
        Skill skill = plugin.getSkillSystem().getSkill(skillId);
        if (skill == null) {
            player.sendMessage("§c绑定的功法不存在: " + skillId);
            player.sendMessage("§7请重新绑定功法");
            return;
        }

        // 施放功法
        boolean success = plugin.getSkillSystem().castSkill(player, skillId);

        if (success) {
            // 显示施放反馈（ActionBar）
            String message = plugin.getConfigManager().getConfig("config")
                    .getString("skill.keybind.messages.cast-success", "§a施放: §e{skill}");
            message = message.replace("{skill}", skill.getName());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
        // 失败消息已在 castSkill 方法中处理
    }

    /**
     * 检查物品是否为 MMOCore 物品（避免冲突）
     * 
     * @param item 物品
     * @return 是否为 MMOCore 物品
     */
    @SuppressWarnings("unused")
    private boolean isMMOCoreItem(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return false;
        }

        // 检查物品是否有 MMOCore 的 NBT 标记
        if (!item.hasItemMeta()) {
            return false;
        }

        // 这里可以添加更详细的检查逻辑
        // 例如检查 PDC (PersistentDataContainer) 中的 MMOCore 标记
        // 目前简单返回 false
        return false;
    }
}

