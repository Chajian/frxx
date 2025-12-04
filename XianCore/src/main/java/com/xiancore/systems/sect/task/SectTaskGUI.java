package com.xiancore.systems.sect.task;

import com.xiancore.XianCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SectTaskGUI implements Listener {

    private final XianCore plugin;
    private final SectTaskManager taskManager;
    private final Map<UUID, SectTaskType> playerViewingType;

    public SectTaskGUI(XianCore plugin, SectTaskManager taskManager) {
        this.plugin = plugin;
        this.taskManager = taskManager;
        this.playerViewingType = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        openGUI(player, SectTaskType.DAILY);
    }

    public void openGUI(Player player, SectTaskType type) {
        UUID playerId = player.getUniqueId();
        if (plugin.getSectSystem().getPlayerSect(playerId) == null) {
            player.sendMessage("\u00a7c\u4f60\u8fd8\u6ca1\u6709\u52a0\u5165\u5b97\u95e8!");
            return;
        }
        playerViewingType.put(playerId, type);
        Inventory gui = createTaskInventory(player, type);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    private Inventory createTaskInventory(Player player, SectTaskType type) {
        UUID playerId = player.getUniqueId();
        Inventory gui = Bukkit.createInventory(null, 54, "\u00a76\u00a7l\u5b97\u95e8\u4efb\u52a1 \u00a77- " + type.getColor() + type.getDisplayName());
        List<SectTask> allTasks = taskManager.getPlayerTasksByType(playerId, type);

        ItemStack borderPane = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderPane);
        }

        gui.setItem(3, createTypeButton(SectTaskType.DAILY, type == SectTaskType.DAILY));
        gui.setItem(4, createTypeButton(SectTaskType.WEEKLY, type == SectTaskType.WEEKLY));
        gui.setItem(5, createTypeButton(SectTaskType.SPECIAL, type == SectTaskType.SPECIAL));

        int slot = 10;
        for (SectTask task : allTasks) {
            if (slot > 43) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot++;
            gui.setItem(slot, createTaskItem(task));
            slot++;
        }

        if (allTasks.isEmpty()) {
            gui.setItem(22, createItem(Material.BARRIER, "\u00a7c\u6682\u65e0\u4efb\u52a1",
                    Arrays.asList("\u00a77\u70b9\u51fb\u4e0b\u65b9\u6309\u94ae\u751f\u6210\u4efb\u52a1")));
        }

        for (int i = 45; i < 54; i++) {
            gui.setItem(i, borderPane);
        }

        gui.setItem(49, createItem(Material.EMERALD, "\u00a7a\u00a7l\u751f\u6210\u65b0\u4efb\u52a1",
                Arrays.asList(
                        "\u00a77\u70b9\u51fb\u4e3a\u4f60\u751f\u6210 " + type.getColor() + type.getDisplayName(),
                        "\u00a7e\u6bcf\u4e2a\u7c7b\u578b\u7684\u4efb\u52a1\u4f1a\u81ea\u52a8\u5b9a\u671f\u5237\u65b0"
                )));

        gui.setItem(53, createItem(Material.BARRIER, "\u00a7c\u00a7l\u5173\u95ed", null));
        return gui;
    }

    private ItemStack createTypeButton(SectTaskType type, boolean selected) {
        Material material = switch (type) {
            case DAILY -> Material.LIME_STAINED_GLASS_PANE;
            case WEEKLY -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case SPECIAL -> Material.MAGENTA_STAINED_GLASS_PANE;
        };

        String name = type.getColor() + "\u00a7l" + type.getDisplayName();
        if (selected) {
            name = "\u00a7e\u00a7l\u279e " + name;
        }

        List<String> lore = new ArrayList<>();
        lore.add("\u00a77\u70b9\u51fb\u5207\u6362\u5230 " + type.getColor() + type.getDisplayName());
        return createItem(material, name, lore);
    }

    private ItemStack createTaskItem(SectTask task) {
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

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String status = task.getStatus().getDisplayName();
            meta.setDisplayName(task.getObjective().getIcon() + " \u00a7f" + task.getName() + " " + status);

            List<String> lore = new ArrayList<>();
            lore.add("\u00a77\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501");
            lore.add("\u00a7e\u76ee\u6807: \u00a7f" + task.getDescription());
            lore.add("\u00a7e\u8fdb\u5ea6: \u00a7f" + task.getProgressText() + " \u00a77(" + String.format("%.1f", task.getProgressPercentage()) + "%)");
            lore.add(createProgressBar(task.getProgressPercentage()));
            
            // 如果是物品收集任务，显示背包中的物品数量
            if (task.getObjective() == TaskObjective.COLLECT_ITEM && !task.isCompleted() && !task.isExpired()) {
                Player viewer = Bukkit.getPlayer(task.getOwnerId());
                if (viewer != null) {
                    int inventoryCount = getItemCountInInventory(viewer, task.getTarget());
                    if (inventoryCount >= task.getTargetAmount()) {
                        lore.add("\u00a7a\u80cc\u5305\u4e2d: \u00a7f" + inventoryCount + " \u00a77\u4e2a \u00a7a\u2714");
                    } else {
                        lore.add("\u00a7e\u80cc\u5305\u4e2d: \u00a7f" + inventoryCount + " \u00a77\u4e2a");
                    }
                }
            }
            
            lore.add("\u00a77\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501");
            lore.add("\u00a76\u5956\u52b1:");
            if (task.getContributionReward() > 0) {
                lore.add("\u00a77  +\u00a7b" + task.getContributionReward() + " \u00a77\u8d21\u732e\u503c");
            }
            if (task.getSpiritStoneReward() > 0) {
                lore.add("\u00a77  +\u00a76" + task.getSpiritStoneReward() + " \u00a77\u7075\u77f3");
            }
            if (task.getActivityReward() > 0) {
                lore.add("\u00a77  +\u00a7a" + task.getActivityReward() + " \u00a77\u6d3b\u8dc3\u7075\u6c14");
            }
            if (task.getSectExpReward() > 0) {
                lore.add("\u00a77  +\u00a7d" + task.getSectExpReward() + " \u00a77\u5b97\u95e8\u7ecf\u9a8c");
            }
            lore.add("\u00a77\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501");

            if (task.isCompleted() && !task.isRewardClaimed()) {
                lore.add("\u00a7a\u00a7l\u2714 \u4efb\u52a1\u5df2\u5b8c\u6210!");
                lore.add("\u00a7e\u70b9\u51fb\u9886\u53d6\u5956\u52b1");
            } else if (task.isRewardClaimed()) {
                lore.add("\u00a77\u5956\u52b1\u5df2\u9886\u53d6");
            } else if (task.isExpired()) {
                lore.add("\u00a7c\u4efb\u52a1\u5df2\u8fc7\u671f");
            } else {
                // 区分物品收集任务和其他任务
                if (task.getObjective() == TaskObjective.COLLECT_ITEM) {
                    lore.add("\u00a7a\u70b9\u51fb\u63d0\u4ea4\u4efb\u52a1");
                    lore.add("\u00a77(\u9700\u8981\u80cc\u5305\u4e2d\u6709\u8db3\u591f\u7269\u54c1)");
                } else {
                    lore.add("\u00a77\u7ee7\u7eed\u5b8c\u6210\u4efb\u52a1\u76ee\u6807");
                }
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private String createProgressBar(double percentage) {
        int total = 20;
        int filled = (int) (percentage / 100.0 * total);
        StringBuilder bar = new StringBuilder("\u00a77[");
        for (int i = 0; i < total; i++) {
            if (i < filled) {
                bar.append("\u00a7a\u2589");
            } else {
                bar.append("\u00a78\u2589");
            }
        }
        bar.append("\u00a77]");
        return bar.toString();
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith("\u00a76\u00a7l\u5b97\u95e8\u4efb\u52a1")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        UUID playerId = player.getUniqueId();
        SectTaskType currentType = playerViewingType.getOrDefault(playerId, SectTaskType.DAILY);

        if (event.getSlot() == 3) {
            openGUI(player, SectTaskType.DAILY);
            return;
        } else if (event.getSlot() == 4) {
            openGUI(player, SectTaskType.WEEKLY);
            return;
        } else if (event.getSlot() == 5) {
            openGUI(player, SectTaskType.SPECIAL);
            return;
        }

        if (event.getSlot() == 49) {
            taskManager.generateTasksForPlayer(player, currentType);
            openGUI(player, currentType);
            return;
        }

        if (event.getSlot() == 53) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 0.8f);
            return;
        }

        if (event.getSlot() >= 10 && event.getSlot() <= 43) {
            List<SectTask> tasks = taskManager.getPlayerTasksByType(playerId, currentType);
            int index = getTaskIndexFromSlot(event.getSlot());
            if (index >= 0 && index < tasks.size()) {
                SectTask task = tasks.get(index);
                
                // 已完成未领奖：领取奖励
                if (task.isCompleted() && !task.isRewardClaimed()) {
                    taskManager.claimReward(player, task.getTaskId());
                    openGUI(player, currentType);
                }
                // 物品收集任务：提交任务
                else if (task.getObjective() == TaskObjective.COLLECT_ITEM && 
                         !task.isCompleted() && !task.isExpired()) {
                    boolean success = taskManager.submitCollectTask(player, task.getTaskId());
                    if (success) {
                        // 刷新界面
                        openGUI(player, currentType);
                    }
                }
            }
        }
    }
    
    /**
     * 获取背包中指定物品的数量
     */
    private int getItemCountInInventory(Player player, String materialName) {
        try {
            Material target = Material.valueOf(materialName);
            int total = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == target) {
                    total += item.getAmount();
                }
            }
            return total;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private int getTaskIndexFromSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (col == 0 || col == 8) {
            return -1;
        }
        int effectiveRow = row - 1;
        int effectiveCol = col - 1;
        return effectiveRow * 7 + effectiveCol;
    }

    public void onPlayerCloseGUI(Player player) {
        playerViewingType.remove(player.getUniqueId());
    }
}
