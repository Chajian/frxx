package com.xiancore.systems.sect.task;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.core.utils.InventoryUtils;
import com.xiancore.gui.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 宗门任务 GUI
 * 使用 IF (Inventory Framework) 重构
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
public class SectTaskGUI {

    private final XianCore plugin;
    private final SectTaskManager taskManager;

    public SectTaskGUI(XianCore plugin, SectTaskManager taskManager) {
        this.plugin = plugin;
        this.taskManager = taskManager;
    }

    public void openGUI(Player player) {
        openGUI(player, SectTaskType.DAILY);
    }

    public void openGUI(Player player, SectTaskType type) {
        UUID playerId = player.getUniqueId();
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            player.sendMessage("§c你还没有加入宗门!");
            return;
        }

        // 创建 GUI
        ChestGui gui = new ChestGui(6, "§6§l宗门任务 §7- " + type.getColor() + type.getDisplayName());
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 背景
        GUIUtils.addBackground(gui, 6);

        // 内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 任务类型切换按钮 (第0行)
        contentPane.addItem(new GuiItem(createTypeButton(SectTaskType.DAILY, type == SectTaskType.DAILY),
                event -> openGUI(player, SectTaskType.DAILY)), 3, 0);
        contentPane.addItem(new GuiItem(createTypeButton(SectTaskType.WEEKLY, type == SectTaskType.WEEKLY),
                event -> openGUI(player, SectTaskType.WEEKLY)), 4, 0);
        contentPane.addItem(new GuiItem(createTypeButton(SectTaskType.SPECIAL, type == SectTaskType.SPECIAL),
                event -> openGUI(player, SectTaskType.SPECIAL)), 5, 0);

        // 任务列表 (第1-4行，每行7个槽位)
        List<SectTask> tasks = taskManager.getPlayerTasksByType(playerId, type);
        displayTasks(contentPane, player, tasks, type);

        // 生成任务按钮 (第5行)
        ItemStack generateButton = new ItemBuilder(Material.EMERALD)
                .name("§a§l生成新任务")
                .lore(
                        "§7点击为你生成 " + type.getColor() + type.getDisplayName(),
                        "§e每个类型的任务会自动定期刷新"
                )
                .glow()
                .build();
        contentPane.addItem(new GuiItem(generateButton, event -> {
            taskManager.generateTasksForPlayer(player, type);
            openGUI(player, type);
        }), 4, 5);

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER)
                .name("§c§l关闭")
                .build();
        contentPane.addItem(new GuiItem(closeButton, event -> {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
        }), 8, 5);

        gui.addPane(contentPane);
        gui.show(player);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    /**
     * 显示任务列表
     */
    private void displayTasks(StaticPane pane, Player player, List<SectTask> tasks, SectTaskType type) {
        if (tasks.isEmpty()) {
            ItemStack noTaskItem = new ItemBuilder(Material.BARRIER)
                    .name("§c暂无任务")
                    .lore("§7点击下方按钮生成任务")
                    .build();
            pane.addItem(new GuiItem(noTaskItem), 4, 2);
            return;
        }

        // 任务槽位布局 (中间区域 7x4)
        int[][] slots = {
                {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {6, 1}, {7, 1},
                {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2}, {7, 2},
                {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3},
                {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4}, {7, 4}
        };

        for (int i = 0; i < tasks.size() && i < slots.length; i++) {
            SectTask task = tasks.get(i);
            ItemStack taskItem = createTaskItem(task, player);

            pane.addItem(new GuiItem(taskItem, event -> {
                handleTaskClick(player, task, type);
            }), slots[i][0], slots[i][1]);
        }
    }

    /**
     * 创建任务类型切换按钮
     */
    private ItemStack createTypeButton(SectTaskType type, boolean selected) {
        Material material = switch (type) {
            case DAILY -> Material.LIME_STAINED_GLASS_PANE;
            case WEEKLY -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case SPECIAL -> Material.MAGENTA_STAINED_GLASS_PANE;
        };

        String name = type.getColor() + "§l" + type.getDisplayName();
        if (selected) {
            name = "§e§l➤ " + name;
        }

        return new ItemBuilder(material)
                .name(name)
                .lore("§7点击切换到 " + type.getColor() + type.getDisplayName())
                .build();
    }

    /**
     * 创建任务显示物品
     */
    private ItemStack createTaskItem(SectTask task, Player player) {
        Material material;
        if (task.isRewardClaimed()) {
            material = Material.GRAY_STAINED_GLASS_PANE;
        } else if (task.isCompleted()) {
            material = Material.LIME_STAINED_GLASS_PANE;
        } else if (task.isExpired()) {
            material = Material.RED_STAINED_GLASS_PANE;
        } else {
            material = Material.YELLOW_STAINED_GLASS_PANE;
        }

        String status = task.getStatus().getDisplayName();
        String displayName = task.getObjective().getIcon() + " §f" + task.getName() + " " + status;

        List<String> lore = new ArrayList<>();
        lore.add("§7━━━━━━━━━━━━━━━");
        lore.add("§e目标: §f" + task.getDescription());
        lore.add("§e进度: §f" + task.getProgressText() + " §7(" + String.format("%.1f", task.getProgressPercentage()) + "%)");
        lore.add(createProgressBar(task.getProgressPercentage()));

        // 如果是物品收集任务，显示背包中的物品数量
        if (task.getObjective() == TaskObjective.COLLECT_ITEM && !task.isCompleted() && !task.isExpired()) {
            try {
                Material targetMaterial = Material.valueOf(task.getTarget());
                int inventoryCount = InventoryUtils.countItems(player, targetMaterial);
                if (inventoryCount >= task.getTargetAmount()) {
                    lore.add("§a背包中: §f" + inventoryCount + " §7个 §a✔");
                } else {
                    lore.add("§e背包中: §f" + inventoryCount + " §7个");
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        lore.add("§7━━━━━━━━━━━━━━━");
        lore.add("§6奖励:");
        if (task.getContributionReward() > 0) {
            lore.add("§7  +§b" + task.getContributionReward() + " §7贡献值");
        }
        if (task.getSpiritStoneReward() > 0) {
            lore.add("§7  +§6" + task.getSpiritStoneReward() + " §7灵石");
        }
        if (task.getActivityReward() > 0) {
            lore.add("§7  +§a" + task.getActivityReward() + " §7活跃灵气");
        }
        if (task.getSectExpReward() > 0) {
            lore.add("§7  +§d" + task.getSectExpReward() + " §7宗门经验");
        }
        lore.add("§7━━━━━━━━━━━━━━━");

        if (task.isCompleted() && !task.isRewardClaimed()) {
            lore.add("§a§l✔ 任务已完成!");
            lore.add("§e点击领取奖励");
        } else if (task.isRewardClaimed()) {
            lore.add("§7奖励已领取");
        } else if (task.isExpired()) {
            lore.add("§c任务已过期");
        } else {
            if (task.getObjective() == TaskObjective.COLLECT_ITEM) {
                lore.add("§a点击提交任务");
                lore.add("§7(需要背包中有足够物品)");
            } else {
                lore.add("§7继续完成任务目标");
            }
        }

        return new ItemBuilder(material)
                .name(displayName)
                .lore(lore)
                .build();
    }

    /**
     * 处理任务点击
     */
    private void handleTaskClick(Player player, SectTask task, SectTaskType type) {
        // 已完成未领奖：领取奖励
        if (task.isCompleted() && !task.isRewardClaimed()) {
            taskManager.claimReward(player, task.getTaskId());
            openGUI(player, type);
        }
        // 物品收集任务：提交任务
        else if (task.getObjective() == TaskObjective.COLLECT_ITEM &&
                !task.isCompleted() && !task.isExpired()) {
            boolean success = taskManager.submitCollectTask(player, task.getTaskId());
            if (success) {
                openGUI(player, type);
            }
        }
    }

    /**
     * 创建进度条
     */
    private String createProgressBar(double percentage) {
        int total = 20;
        int filled = (int) (percentage / 100.0 * total);
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§8█");
            }
        }
        bar.append("§7]");
        return bar.toString();
    }
}
