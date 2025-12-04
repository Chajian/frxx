package com.xiancore.integration.residence;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectSystem;
import com.xiancore.gui.SectRoleCompletePermissionGUIWithPagination;
import com.xiancore.gui.SectRolePermissionGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

// Residence API
import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

import java.util.ArrayList;
import java.util.List;

public class SectLandGUI implements Listener {

    private final XianCore plugin;
    private final SectSystem sectSystem;
    private final BuildingSlotManager buildingSlotManager;

    private static final String TITLE_PREFIX = "§a§l宗门领地管理 - ";

    // 布局槽位
    private static final int SLOT_INFO = 10;
    private static final int SLOT_TP = 12;
    private static final int SLOT_MAINTENANCE = 14;
    private static final int SLOT_SIMPLE_PERM = 21;
    private static final int SLOT_COMPLETE_PERM = 23;
    private static final int SLOT_REFRESH = 49;
    private static final int SLOT_CLOSE = 53;

    public SectLandGUI(XianCore plugin, SectSystem sectSystem, BuildingSlotManager buildingSlotManager) {
        this.plugin = plugin;
        this.sectSystem = sectSystem;
        this.buildingSlotManager = buildingSlotManager;
    }

    public void openMainGUI(Player player, Sect sect) {
        if (sect == null) {
            player.sendMessage("§c未找到你的宗门数据！");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + sect.getName());

        // 背景装饰
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // 核心功能按钮
        inv.setItem(SLOT_INFO, createInfoItem(sect));
        inv.setItem(SLOT_TP, createItem(Material.ENDER_PEARL, "§b§l领地传送", lore(
                "§7传送至宗门领地中心",
                sect.getLandCenter() == null ? "§c未设置中心点" : "§7位置: §f" + sect.getLandCenter().getBlockX() + ", " + sect.getLandCenter().getBlockY() + ", " + sect.getLandCenter().getBlockZ()
        )));
        inv.setItem(SLOT_MAINTENANCE, createItem(Material.GOLD_INGOT, "§6§l维护费管理", lore(
                "§7查看维护费状态并进行缴纳"
        )));
        inv.setItem(SLOT_SIMPLE_PERM, createItem(Material.BOOK, "§a§l简化权限配置", lore(
                "§7按角色快速配置常用权限"
        )));
        inv.setItem(SLOT_COMPLETE_PERM, createItem(Material.WRITABLE_BOOK, "§d§l完整权限配置", lore(
                "§7分页查看并编辑所有权限标志"
        )));

        // 底部按钮
        inv.setItem(SLOT_REFRESH, createItem(Material.SUNFLOWER, "§e刷新", lore("§7刷新界面显示")));
        inv.setItem(SLOT_CLOSE, createItem(Material.BARRIER, "§c关闭", lore("§7关闭界面")));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        if (title == null) return;
        if (!title.startsWith(TITLE_PREFIX)) return;

        event.setCancelled(true);

        Sect sect = sectSystem.getPlayerSect(player.getUniqueId());
        if (sect == null) {
            player.sendMessage("§c未找到你的宗门数据！");
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        switch (slot) {
            case SLOT_INFO:
                sendSectInfo(player, sect);
                break;
            case SLOT_TP:
                handleTeleport(player, sect);
                break;
            case SLOT_MAINTENANCE:
                sectSystem.getMaintenanceFeeGUI().openMaintenanceGUI(player, sect);
                break;
            case SLOT_SIMPLE_PERM:
                new SectRolePermissionGUI(plugin, sectSystem.getPermissionManager()).openRolePermissionMain(player, sect);
                break;
            case SLOT_COMPLETE_PERM:
                openCompletePermissionGUI(player, sect);
                break;
            case SLOT_REFRESH:
                openMainGUI(player, sect);
                break;
            case SLOT_CLOSE:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void handleTeleport(Player player, Sect sect) {
        if (sect.getLandCenter() == null) {
            player.sendMessage("§c未设置领地中心点，无法传送！");
            return;
        }
        player.teleport(sect.getLandCenter());
        player.sendMessage("§a已传送至宗门领地中心。");
    }

    private void openCompletePermissionGUI(Player player, Sect sect) {
        String landId = sect.getResidenceLandId();
        if (landId == null || landId.isEmpty()) {
            player.sendMessage("§c宗门未绑定Residence领地，无法打开完整权限配置！");
            return;
        }

        ClaimedResidence residence = ResidenceApi.getResidenceManager().getByName(landId);
        if (residence == null) {
            player.sendMessage("§c未找到对应的Residence领地: " + landId);
            return;
        }

        SectRoleCompletePermissionGUIWithPagination gui = new SectRoleCompletePermissionGUIWithPagination(
                plugin, sect, residence, sectSystem.getPermissionManager());
        gui.openMainMenu(player);
    }

    private void sendSectInfo(Player player, Sect sect) {
        player.sendMessage("§a§l========== 领地信息 ==========");
        player.sendMessage("§7宗门: §f" + sect.getName());
        player.sendMessage("§7领地ID: §f" + (sect.getResidenceLandId() == null ? "未绑定" : sect.getResidenceLandId()));
        if (sect.getLandCenter() != null) {
            player.sendMessage("§7中心点: §f" + sect.getLandCenter().getWorld().getName() + " "
                    + sect.getLandCenter().getBlockX() + ", " + sect.getLandCenter().getBlockY() + ", " + sect.getLandCenter().getBlockZ());
        }
        player.sendMessage("§7建筑位数量: §f" + (sect.getBuildingSlots() == null ? 0 : sect.getBuildingSlots().size()));
        player.sendMessage("§a§l===========================");
    }

    private ItemStack createInfoItem(Sect sect) {
        List<String> lore = new ArrayList<>();
        lore.add("§7领地ID: §f" + (sect.getResidenceLandId() == null ? "未绑定" : sect.getResidenceLandId()));
        if (sect.getLandCenter() != null) {
            lore.add("§7中心点: §f" + sect.getLandCenter().getBlockX() + ", " + sect.getLandCenter().getBlockY() + ", " + sect.getLandCenter().getBlockZ());
        }
        lore.add("§7建筑位: §f" + (sect.getBuildingSlots() == null ? 0 : sect.getBuildingSlots().size()));
        lore.add(" ");
        lore.add("§7点击查看详细信息");
        return createItem(Material.PAPER, "§a§l领地信息", lore);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> lore(String... lines) {
        List<String> list = new ArrayList<>();
        for (String s : lines) list.add(s);
        return list;
    }
}
