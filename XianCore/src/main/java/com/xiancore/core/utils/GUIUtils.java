package com.xiancore.core.utils;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.xiancore.gui.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * GUI 工具类
 * 提供通用的 GUI 构建方法
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public final class GUIUtils {

    private GUIUtils() {
        // 工具类禁止实例化
    }

    /**
     * 创建标准背景面板（黑色玻璃板）
     *
     * @param gui ChestGui 实例
     * @param rows 行数
     */
    public static void addBackground(ChestGui gui, int rows) {
        addBackground(gui, rows, Material.BLACK_STAINED_GLASS_PANE);
    }

    /**
     * 创建标准背景面板（灰色玻璃板）
     *
     * @param gui ChestGui 实例
     * @param rows 行数
     */
    public static void addGrayBackground(ChestGui gui, int rows) {
        addBackground(gui, rows, Material.GRAY_STAINED_GLASS_PANE);
    }

    /**
     * 创建标准背景面板（指定材质）
     *
     * @param gui ChestGui 实例
     * @param rows 行数
     * @param material 背景材质
     */
    public static void addBackground(ChestGui gui, int rows, Material material) {
        OutlinePane background = new OutlinePane(0, 0, 9, rows);
        ItemStack border = new ItemBuilder(material).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(Pane.Priority.LOWEST);
        gui.addPane(background);
    }

    /**
     * 创建并返回背景面板（用于需要额外配置的场景）
     *
     * @param rows 行数
     * @param material 背景材质
     * @return OutlinePane 背景面板
     */
    public static OutlinePane createBackground(int rows, Material material) {
        OutlinePane background = new OutlinePane(0, 0, 9, rows);
        ItemStack border = new ItemBuilder(material).name("§7").build();
        background.addItem(new GuiItem(border));
        background.setRepeat(true);
        background.setPriority(Pane.Priority.LOWEST);
        return background;
    }

    /**
     * 创建关闭按钮
     *
     * @return 关闭按钮 ItemStack
     */
    public static ItemStack createCloseButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("§c关闭")
                .build();
    }

    /**
     * 创建返回按钮
     *
     * @return 返回按钮 ItemStack
     */
    public static ItemStack createBackButton() {
        return new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .build();
    }

    /**
     * 创建返回按钮（带自定义提示）
     *
     * @param lore 提示文本
     * @return 返回按钮 ItemStack
     */
    public static ItemStack createBackButton(String... lore) {
        return new ItemBuilder(Material.ARROW)
                .name("§e返回")
                .lore(lore)
                .build();
    }
}
