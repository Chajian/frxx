package com.xiancore.integration.residence;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectSystem;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;
import org.bukkit.Material;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 维护费管理GUI系统
 * 提供友好的图形界面来查看和管理宗门领地维护费
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class MaintenanceFeeGUI implements Listener {

    private final XianCore plugin;
    private final SectSystem sectSystem;
    private final SectResidenceManager residenceManager;
    private final MaintenanceFeeScheduler maintenanceFeeScheduler;

    // 用户正在查看的宗门ID（玩家UUID -> 宗门ID）
    private final Map<String, Integer> viewingSectId = new HashMap<>();

    // GUI标识符
    private static final String GUI_TITLE_PREFIX = "§6维护费管理";
    private static final String GUI_DETAILS_PREFIX = "§6维护费详情";

    /**
     * 构造函数
     */
    public MaintenanceFeeGUI(XianCore plugin, SectSystem sectSystem, SectResidenceManager residenceManager,
                             MaintenanceFeeScheduler maintenanceFeeScheduler) {
        this.plugin = plugin;
        this.sectSystem = sectSystem;
        this.residenceManager = residenceManager;
        this.maintenanceFeeScheduler = maintenanceFeeScheduler;
    }

    /**
     * 打开维护费管理GUI
     */
    public void openMaintenanceGUI(Player player, Sect sect) {
        if (sect == null || !sect.hasLand()) {
            player.sendMessage("§c该宗门没有领地！");
            return;
        }

        viewingSectId.put(player.getUniqueId().toString(), sect.getId());
        Inventory gui = createMaintenanceInventory(sect);
        player.openInventory(gui);
    }

    /**
     * 创建维护费管理主界面
     */
    private Inventory createMaintenanceInventory(Sect sect) {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_TITLE_PREFIX + " - " + sect.getName());

        // 获取维护费状态
        SectResidenceManager.MaintenanceStatus status = residenceManager.getMaintenanceStatus(sect);

        // 显示维护费状态
        ItemStack statusItem = new ItemStack(getStatusMaterial(status));
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            statusMeta.setDisplayName("§e维护费状态");
            List<String> lore = new ArrayList<>();
            lore.add("§7" + getStatusDescription(status));

            long daysSinceMaintenance = (System.currentTimeMillis() - sect.getLastMaintenanceTime()) / (1000 * 60 * 60 * 24);
            lore.add("§7距上次缴费: §f" + daysSinceMaintenance + " §7天");

            lore.add("");
            lore.add("§7点击查看详细信息");
            statusMeta.setLore(lore);
            statusItem.setItemMeta(statusMeta);
        }
        inventory.setItem(11, statusItem);

        // 显示宗门资金
        ItemStack fundsItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta fundsMeta = fundsItem.getItemMeta();
        if (fundsMeta != null) {
            fundsMeta.setDisplayName("§6宗门资金");
            List<String> lore = new ArrayList<>();
            lore.add("§7当前资金: §f" + sect.getSectFunds() + " §7灵石");

            long maintenanceCost = maintenanceFeeScheduler.getMaintenanceFeeRecord().getTotalChecks();
            lore.add("§7成功缴费: §f" + maintenanceFeeScheduler.getMaintenanceFeeRecord().getSuccessfulPayments() + " §7次");

            lore.add("");
            lore.add("§7点击查看资金详情");
            fundsMeta.setLore(lore);
            fundsItem.setItemMeta(fundsMeta);
        }
        inventory.setItem(13, fundsItem);

        // 显示领地信息
        ItemStack landItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta landMeta = landItem.getItemMeta();
        if (landMeta != null) {
            landMeta.setDisplayName("§a领地信息");
            List<String> lore = new ArrayList<>();
            lore.add("§7领地ID: §f" + sect.getResidenceLandId());
            if (sect.getLandCenter() != null) {
                lore.add("§7领地中心: §f" + sect.getLandCenter().getBlockX() + ", "
                    + sect.getLandCenter().getBlockY() + ", " + sect.getLandCenter().getBlockZ());
            }
            lore.add("§7建筑位: §f" + sect.getBuildingSlots().size());
            lore.add("");
            lore.add("§7点击查看领地详情");
            landMeta.setLore(lore);
            landItem.setItemMeta(landMeta);
        }
        inventory.setItem(15, landItem);

        // 缴纳维护费按钮
        ItemStack payItem = new ItemStack(Material.EMERALD);
        ItemMeta payMeta = payItem.getItemMeta();
        if (payMeta != null) {
            payMeta.setDisplayName("§a缴纳维护费");
            List<String> lore = new ArrayList<>();
            long maintenanceCost = maintenanceFeeScheduler.getMaintenanceFeeRecord().getTotalChecks() > 0 ? 100 : 100;
            lore.add("§7费用: §6" + maintenanceCost + " §7灵石");
            lore.add("");
            lore.add("§e点击缴纳维护费");
            payMeta.setLore(lore);
            payItem.setItemMeta(payMeta);
        }
        inventory.setItem(20, payItem);

        // 权限记录按钮
        ItemStack permItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta permMeta = permItem.getItemMeta();
        if (permMeta != null) {
            permMeta.setDisplayName("§b权限记录");
            List<String> lore = new ArrayList<>();
            lore.add("§7查看成员权限信息");
            lore.add("");
            lore.add("§e点击查看权限记录");
            permMeta.setLore(lore);
            permItem.setItemMeta(permMeta);
        }
        inventory.setItem(22, permItem);

        // 返回按钮（第26格）
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c关闭");
            backMeta.setLore(Collections.singletonList("§7点击关闭界面"));
            backItem.setItemMeta(backMeta);
        }
        inventory.setItem(26, backItem);

        // 装饰物品
        ItemStack decorItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorItem.getItemMeta();
        if (decorMeta != null) {
            decorMeta.setDisplayName(" ");
            decorItem.setItemMeta(decorMeta);
        }
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 17, 18, 19, 21, 23, 24, 25}) {
            inventory.setItem(i, decorItem);
        }

        return inventory;
    }

    /**
     * 创建维护费详情界面
     */
    private Inventory createMaintenanceDetailsInventory(Sect sect) {
        Inventory inventory = Bukkit.createInventory(null, 27, GUI_DETAILS_PREFIX + " - " + sect.getName());

        // 维护费状态详情
        SectResidenceManager.MaintenanceStatus status = residenceManager.getMaintenanceStatus(sect);
        ItemStack statusItem = new ItemStack(getStatusMaterial(status));
        ItemMeta statusMeta = statusItem.getItemMeta();
        if (statusMeta != null) {
            statusMeta.setDisplayName("§e维护费状态详情");
            List<String> lore = new ArrayList<>();
            lore.add("§7当前状态: " + getStatusColoredDescription(status));

            long daysSinceMaintenance = (System.currentTimeMillis() - sect.getLastMaintenanceTime()) / (1000 * 60 * 60 * 24);
            lore.add("§7距上次缴费: §f" + daysSinceMaintenance + " §7天");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            lore.add("§7上次缴费时间: §f" + sdf.format(new Date(sect.getLastMaintenanceTime())));

            lore.add("");
            if (status == SectResidenceManager.MaintenanceStatus.PAID) {
                lore.add("§a宗门领地维护费已缴纳");
                lore.add("§a状态: 正常");
            } else if (status == SectResidenceManager.MaintenanceStatus.OVERDUE_WARNING) {
                lore.add("§e宗门领地维护费即将逾期");
                lore.add("§e请尽快缴纳维护费");
            } else if (status == SectResidenceManager.MaintenanceStatus.FROZEN) {
                lore.add("§c宗门领地维护费已逾期");
                lore.add("§c领地功能已被冻结");
                lore.add("§c请立即缴纳维护费");
            } else if (status == SectResidenceManager.MaintenanceStatus.AUTO_RELEASING) {
                lore.add("§c宗门领地将在24小时内自动释放");
                lore.add("§c请立即缴纳维护费");
            }

            statusMeta.setLore(lore);
            statusItem.setItemMeta(statusMeta);
        }
        inventory.setItem(11, statusItem);

        // 缴费记录
        ItemStack recordItem = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta recordMeta = recordItem.getItemMeta();
        if (recordMeta != null) {
            recordMeta.setDisplayName("§b维护费记录");
            List<String> lore = new ArrayList<>();
            MaintenanceFeeScheduler.MaintenanceFeeRecord record = maintenanceFeeScheduler.getMaintenanceFeeRecord();
            lore.add("§7成功缴费: §f" + record.getSuccessfulPayments() + " §7次");
            lore.add("§7逾期事件: §f" + record.getOverdueEvents() + " §7次");
            lore.add("§7自动释放: §f" + record.getAutoReleaseEvents() + " §7次");
            lore.add("§7总检查: §f" + record.getTotalChecks() + " §7次");

            if (record.getLastCheckTime() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                lore.add("§7最后检查: §f" + sdf.format(new Date(record.getLastCheckTime())));
            }

            recordMeta.setLore(lore);
            recordItem.setItemMeta(recordMeta);
        }
        inventory.setItem(15, recordItem);

        // 返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c返回");
            backMeta.setLore(Collections.singletonList("§7点击返回主菜单"));
            backItem.setItemMeta(backMeta);
        }
        inventory.setItem(26, backItem);

        // 装饰物品
        ItemStack decorItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorItem.getItemMeta();
        if (decorMeta != null) {
            decorMeta.setDisplayName(" ");
            decorItem.setItemMeta(decorMeta);
        }
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25}) {
            inventory.setItem(i, decorItem);
        }

        return inventory;
    }

    /**
     * 获取维护费状态对应的物品
     */
    private Material getStatusMaterial(SectResidenceManager.MaintenanceStatus status) {
        return switch (status) {
            case PAID -> Material.GREEN_CONCRETE;
            case OVERDUE_WARNING -> Material.YELLOW_CONCRETE;
            case FROZEN -> Material.ORANGE_CONCRETE;
            case AUTO_RELEASING -> Material.RED_CONCRETE;
            default -> Material.GRAY_CONCRETE;
        };
    }

    /**
     * 获取维护费状态描述
     */
    private String getStatusDescription(SectResidenceManager.MaintenanceStatus status) {
        return switch (status) {
            case PAID -> "已缴费 ✓";
            case OVERDUE_WARNING -> "逾期警告 ⚠";
            case FROZEN -> "已冻结 ✗";
            case AUTO_RELEASING -> "自动释放中 ⚡";
            case NO_LAND -> "无领地";
        };
    }

    /**
     * 获取带颜色的维护费状态描述
     */
    private String getStatusColoredDescription(SectResidenceManager.MaintenanceStatus status) {
        return switch (status) {
            case PAID -> "§a已缴费";
            case OVERDUE_WARNING -> "§e逾期警告";
            case FROZEN -> "§c已冻结";
            case AUTO_RELEASING -> "§4自动释放中";
            case NO_LAND -> "§7无领地";
        };
    }

    /**
     * 处理GUI点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("维护费")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String playerUuid = player.getUniqueId().toString();
        Integer sectId = viewingSectId.get(playerUuid);

        if (sectId == null) {
            player.closeInventory();
            return;
        }

        Sect sect = sectSystem.getSect(sectId);
        if (sect == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();

        // 主菜单处理
        if (event.getView().getTitle().contains("维护费管理")) {
            switch (slot) {
                case 11: // 维护费状态
                    player.openInventory(createMaintenanceDetailsInventory(sect));
                    break;

                case 13: // 宗门资金
                    player.sendMessage("§a§l========== 宗门资金信息 ==========");
                    player.sendMessage("§a当前资金: §6" + sect.getSectFunds() + " §a灵石");
                    player.sendMessage("§a成功缴费次数: §6" +
                        maintenanceFeeScheduler.getMaintenanceFeeRecord().getSuccessfulPayments());
                    player.sendMessage("§a§l==============================");
                    break;

                case 15: // 领地信息
                    player.sendMessage("§a§l========== 领地信息 ==========");
                    player.sendMessage("§a领地ID: §f" + sect.getResidenceLandId());
                    if (sect.getLandCenter() != null) {
                        player.sendMessage("§a领地中心: §f" +
                            sect.getLandCenter().getBlockX() + ", " +
                            sect.getLandCenter().getBlockY() + ", " +
                            sect.getLandCenter().getBlockZ());
                    }
                    player.sendMessage("§a建筑位: §f" + sect.getBuildingSlots().size());
                    player.sendMessage("§a§l=========================");
                    break;

                case 20: // 缴纳维护费
                    player.closeInventory();
                    player.sendMessage("§a正在处理维护费缴纳...");
                    // 这里会调用实际的缴纳逻辑
                    // 通过 /sect land pay 命令实现
                    player.performCommand("sect land pay");
                    break;

                case 22: // 权限记录
                    player.openInventory(createPermissionInventory(sect));
                    break;

                case 26: // 关闭
                    player.closeInventory();
                    viewingSectId.remove(playerUuid);
                    break;
            }
        }
        // 详情菜单处理
        else if (event.getView().getTitle().contains("维护费详情")) {
            switch (slot) {
                case 26: // 返回
                    player.openInventory(createMaintenanceInventory(sect));
                    break;
            }
        }
        // 权限菜单处理
        else if (event.getView().getTitle().contains("权限记录")) {
            switch (slot) {
                case 26: // 返回
                    player.openInventory(createMaintenanceInventory(sect));
                    break;
            }
        }
    }

    /**
     * 创建权限记录界面
     */
    private Inventory createPermissionInventory(Sect sect) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§b权限记录 - " + sect.getName());

        // 权限统计
        ItemStack statsItem = new ItemStack(Material.BOOK);
        ItemMeta statsMeta = statsItem.getItemMeta();
        if (statsMeta != null) {
            statsMeta.setDisplayName("§a权限统计");
            List<String> lore = new ArrayList<>();
            lore.add("§7宗门成员: §f" + sect.getMemberCount());
            lore.add("§7管理员: " + countPermissionLevel(sect, "§c管理员§7"));
            lore.add("§7管理者: " + countPermissionLevel(sect, "§6管理者§7"));
            lore.add("§7建筑者: " + countPermissionLevel(sect, "§e建筑者§7"));
            lore.add("§7成员: " + countPermissionLevel(sect, "§a成员§7"));
            lore.add("§7访客: " + countPermissionLevel(sect, "§7访客§7"));
            statsMeta.setLore(lore);
            statsItem.setItemMeta(statsMeta);
        }
        inventory.setItem(11, statsItem);

        // 权限审计
        ItemStack auditItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta auditMeta = auditItem.getItemMeta();
        if (auditMeta != null) {
            auditMeta.setDisplayName("§b权限审计日志");
            List<String> lore = new ArrayList<>();
            lore.add("§7查看权限变更历史");
            lore.add("§7包括权限设置、移除、更新等");
            lore.add("");
            lore.add("§e点击查看详情");
            auditMeta.setLore(lore);
            auditItem.setItemMeta(auditMeta);
        }
        inventory.setItem(15, auditItem);

        // 返回按钮
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c返回");
            backMeta.setLore(Collections.singletonList("§7点击返回主菜单"));
            backItem.setItemMeta(backMeta);
        }
        inventory.setItem(26, backItem);

        // 装饰物品
        ItemStack decorItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta decorMeta = decorItem.getItemMeta();
        if (decorMeta != null) {
            decorMeta.setDisplayName(" ");
            decorItem.setItemMeta(decorMeta);
        }
        for (int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25}) {
            inventory.setItem(i, decorItem);
        }

        return inventory;
    }

    /**
     * 计算权限等级数量（简化版）
     */
    private String countPermissionLevel(Sect sect, String levelName) {
        return "§f0"; // 这里需要从权限管理器获取实际数据
    }
}
