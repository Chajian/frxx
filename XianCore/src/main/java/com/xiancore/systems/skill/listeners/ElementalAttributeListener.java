package com.xiancore.systems.skill.listeners;

import com.xiancore.XianCore;
import com.xiancore.systems.skill.ElementalAttribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * 元素属性监听器
 * 负责清理元素缓存，防止内存泄漏
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ElementalAttributeListener implements Listener {

    private final XianCore plugin;

    public ElementalAttributeListener(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 实体死亡时清除缓存
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        ElementalAttribute.clearCache(event.getEntity().getUniqueId());
    }

    /**
     * 区块卸载时清理该区块的实体缓存
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        // 清理该区块内所有实体的缓存
        org.bukkit.entity.Entity[] entities = event.getChunk().getEntities();
        for (org.bukkit.entity.Entity entity : entities) {
            ElementalAttribute.clearCache(entity.getUniqueId());
        }
    }
}


