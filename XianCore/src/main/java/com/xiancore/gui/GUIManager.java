package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.xiancore.XianCore;
import lombok.Getter;

/**
 * GUI 管理器
 * 使用 IF (Inventory Framework) 管理所有界面
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class GUIManager {

    private final XianCore plugin;
    private ActiveQiShopGUI activeQiShopGUI;

    public GUIManager(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化 GUI 管理器
     */
    public void initialize() {
        try {
            // 初始化活跃灵气商店 GUI
            activeQiShopGUI = new ActiveQiShopGUI(plugin);
            // IF Framework 自动处理事件，不需要手动注册 Listener

            plugin.getLogger().info("§a✓ GUI 系统初始化完成 (IF Framework)");
        } catch (Exception e) {
            plugin.getLogger().severe("§c✗ GUI 系统初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 关闭 GUI 管理器
     */
    public void shutdown() {
        plugin.getLogger().info("§a✓ GUI 系统已关闭");
    }
}
