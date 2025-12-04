package com.xiancore.systems.forge;

import com.xiancore.XianCore;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 炼器系统
 * 负责管理装备炼制、强化、融合等功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class ForgeSystem {

    private final XianCore plugin;
    private final RecipeManager recipeManager;
    private final Map<UUID, String> pendingEquipmentNames;  // 玩家UUID -> 待设置的装备名称
    private boolean initialized = false;

    public ForgeSystem(XianCore plugin) {
        this.plugin = plugin;
        this.recipeManager = new RecipeManager(plugin);
        this.pendingEquipmentNames = new ConcurrentHashMap<>();
    }

    /**
     * 初始化炼器系统
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 加载炼制配方
        recipeManager.loadRecipes();

        initialized = true;
        plugin.getLogger().info("  §a✓ 炼器系统初始化完成");
    }

    /**
     * 设置玩家的待命名装备名称
     */
    public void setPendingEquipmentName(UUID playerId, String name) {
        if (name == null || name.isEmpty()) {
            pendingEquipmentNames.remove(playerId);
        } else {
            pendingEquipmentNames.put(playerId, name);
        }
    }

    /**
     * 获取并清除玩家的待命名装备名称
     */
    public String getPendingEquipmentName(UUID playerId) {
        return pendingEquipmentNames.remove(playerId);
    }
}
