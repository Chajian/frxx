package com.xiancore.gui;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.xiancore.XianCore;
import com.xiancore.core.utils.GUIUtils;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.utils.ItemBuilder;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 宗门界面
 * 提供宗门管理、成员查看、任务等功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectGUI {

    private final XianCore plugin;

    public SectGUI(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开宗门主界面
     */
    public static void open(Player player, XianCore plugin) {
        new SectGUI(plugin).show(player);
    }

    private void show(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        // 创建6行的GUI
        ChestGui gui = new ChestGui(6, "§d§l宗门系统");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 创建边框面板
        GUIUtils.addBackground(gui, 6, Material.PURPLE_STAINED_GLASS_PANE);

        // 创建内容面板
        StaticPane contentPane = new StaticPane(0, 0, 9, 6);

        // 判断玩家是否已加入宗门
        Integer sectId = data.getSectId();
        if (sectId == null) {
            // 未加入宗门 - 显示创建/加入界面
            displayNoSectInterface(player, data, contentPane);
        } else {
            // 已加入宗门 - 显示宗门管理界面
            displaySectInterface(player, data, contentPane);
        }

        // 关闭按钮
        ItemStack closeButton = new ItemBuilder(Material.BARRIER).name("§c关闭").build();
        contentPane.addItem(new GuiItem(closeButton, event -> player.closeInventory()), 4, 5);

        gui.addPane(contentPane);
        gui.show(player);
    }

    /**
     * 显示未加入宗门的界面
     */
    private void displayNoSectInterface(Player player, PlayerData data, StaticPane pane) {
        // 标题提示
        ItemStack infoItem = new ItemBuilder(Material.PAPER)
                .name("§e§l宗门系统")
                .lore(
                        "§7你还未加入任何宗门",
                        "",
                        "§e宗门可以提供:",
                        "§7- 修炼加成",
                        "§7- 宗门任务奖励",
                        "§7- 成员互助",
                        "§7- 宗门战争",
                        "",
                        "§6创建宗门或加入其他宗门开始你的征程!"
                )
                .build();
        pane.addItem(new GuiItem(infoItem), 4, 1);

        // 创建宗门按钮
        ItemStack createButton = new ItemBuilder(Material.EMERALD)
                .name("§a§l创建宗门")
                .lore(
                        "§7创建一个新的宗门",
                        "",
                        "§e需要条件:",
                        "§7- 境界: §f筑基期以上",
                        "§7- 灵石: §f10000",
                        "§7- 未加入其他宗门",
                        "",
                        "§a点击创建宗门"
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(createButton, event -> {
            // 检查条件
            if (!data.getRealm().equals("炼气期")) {
                player.sendMessage("§e正在创建宗门，请稍候...");
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    player.performCommand("sect create");
                }, 1L);
            } else {
                player.sendMessage("§c你的境界不足! 需要筑基期以上才能创建宗门。");
            }
        }), 2, 3);

        // 加入宗门按钮
        ItemStack joinButton = new ItemBuilder(Material.WRITABLE_BOOK)
                .name("§b§l加入宗门")
                .lore(
                        "§7查看并加入已有的宗门",
                        "",
                        "§e功能:",
                        "§7- 查看宗门列表",
                        "§7- 查看宗门详情",
                        "§7- 申请加入",
                        "",
                        "§b点击查看宗门列表"
                )
                .build();

        pane.addItem(new GuiItem(joinButton, event -> {
            player.sendMessage("§e正在查询宗门列表...");
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.performCommand("sect list");
            }, 1L);
        }), 4, 3);

        // 宗门排行按钮
        ItemStack rankButton = new ItemBuilder(Material.GOLDEN_APPLE)
                .name("§6§l宗门排行")
                .lore(
                        "§7查看宗门实力排行",
                        "",
                        "§e排名依据:",
                        "§7- 宗门总实力",
                        "§7- 成员数量",
                        "§7- 任务完成度",
                        "",
                        "§6点击查看排行榜"
                )
                .build();

        pane.addItem(new GuiItem(rankButton, event -> {
            player.sendMessage("§e正在查询宗门排行...");
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.performCommand("sect rank");
            }, 1L);
        }), 6, 3);
    }

    /**
     * 显示已加入宗门的界面
     */
    private void displaySectInterface(Player player, PlayerData data, StaticPane pane) {
        // 从宗门系统获取宗门信息
        com.xiancore.systems.sect.Sect sect = plugin.getSectSystem().getPlayerSect(player.getUniqueId());

        // 获取职位和成员数量
        String playerRank = SectRank.getColoredDisplayName(data.getSectRank());
        int memberCount = sect != null ? sect.getMemberList().size() : 1;
        int maxMembers = sect != null ? sect.getMaxMembers() : 10;

        // 宗门信息
        ItemStack sectInfoItem = new ItemBuilder(Material.WHITE_BANNER)
                .name("§d§l我的宗门")
                .lore(
                        "§e宗门ID: §f" + data.getSectId(),
                        "§e我的职位: §f" + playerRank,
                        "§e成员数量: §f" + memberCount + "§7/§f" + maxMembers,
                        "",
                        "§7点击查看详细信息"
                )
                .glow()
                .build();
        pane.addItem(new GuiItem(sectInfoItem, event -> {
            player.sendMessage("§e正在查询宗门信息...");
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.performCommand("sect info");
            }, 1L);
        }), 4, 1);

        // 成员管理
        ItemStack membersButton = new ItemBuilder(Material.PLAYER_HEAD)
                .name("§a§l成员管理")
                .lore(
                        "§7管理宗门成员",
                        "",
                        "§e功能:",
                        "§7- 查看成员列表",
                        "§7- 邀请新成员",
                        "§7- 踢出成员",
                        "§7- 提升/降低职位",
                        "",
                        "§a点击管理成员"
                )
                .build();

        pane.addItem(new GuiItem(membersButton, event -> {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (sect != null) {
                    new SectMemberGUI(plugin).openMemberList(player, sect);
                } else {
                    player.sendMessage("§c无法获取宗门信息!");
                }
            });
        }), 1, 3);

        // 宗门任务
        ItemStack questButton = new ItemBuilder(Material.BOOK)
                .name("§e§l宗门任务")
                .lore(
                        "§7接取并完成宗门任务",
                        "",
                        "§e奖励:",
                        "§7- 宗门贡献点",
                        "§7- 修炼资源",
                        "§7- 特殊道具",
                        "",
                        "§e点击查看任务"
                )
                .build();

        pane.addItem(new GuiItem(questButton, event -> {
            player.closeInventory();
            // 打开宗门任务GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getSectSystem().getTaskGUI().openGUI(player);
            });
        }), 3, 3);

        // 宗门仓库
        ItemStack warehouseButton = new ItemBuilder(Material.CHEST)
                .name("§6§l宗门仓库")
                .lore(
                        "§7存取宗门共享资源",
                        "",
                        "§e功能:",
                        "§7- 存入物品",
                        "§7- 取出物品",
                        "§7- 查看仓库",
                        "",
                        "§6点击打开仓库"
                )
                .build();

        pane.addItem(new GuiItem(warehouseButton, event -> {
            player.closeInventory();
            // 打开宗门仓库GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getSectSystem().getWarehouseGUI().open(player);
            });
        }), 5, 3);

        // 宗门商店
        ItemStack shopButton = new ItemBuilder(Material.EMERALD)
                .name("§2§l宗门商店")
                .lore(
                        "§7使用贡献点兑换物品",
                        "",
                        "§e可兑换:",
                        "§7- 修炼丹药",
                        "§7- 功法秘籍",
                        "§7- 仙家胚胎",
                        "",
                        "§2点击打开商店"
                )
                .build();

        pane.addItem(new GuiItem(shopButton, event -> {
            player.closeInventory();
            // 打开宗门商店GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getSectSystem().getShopGUI().open(player);
            });
        }), 7, 3);

        // 宗门设施
        ItemStack facilityButton = new ItemBuilder(Material.BEACON)
                .name("§d§l宗门设施")
                .lore(
                        "§7管理和升级宗门设施",
                        "",
                        "§e可升级设施:",
                        "§7- 灵脉（修炼加成）",
                        "§7- 炼器台（炼制加成）",
                        "§7- 藏经阁（功法解锁）",
                        "§7- 宗门仓库（容量扩展）",
                        "§7- 宗门商店（开启商店）",
                        "",
                        "§d点击管理设施"
                )
                .glow()
                .build();

        pane.addItem(new GuiItem(facilityButton, event -> {
            player.closeInventory();
            // 打开宗门设施GUI
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getSectSystem().getFacilityGUI().open(player);
            });
        }), 4, 3);

        // 宗门设置 (仅宗主和长老)
        ItemStack settingsButton = new ItemBuilder(Material.COMPARATOR)
                .name("§c§l宗门设置")
                .lore(
                        "§7管理宗门配置",
                        "",
                        "§e功能:",
                        "§7- 修改宗门名称",
                        "§7- 修改宗门公告",
                        "§7- 权限设置",
                        "§7- 解散宗门",
                        "",
                        "§c仅宗主可用"
                )
                .build();

        pane.addItem(new GuiItem(settingsButton, event -> {
            // 检查权限：只有宗主可以访问设置
            SectRank sectRank = SectRank.fromRankString(data.getSectRank());
            if (sectRank != SectRank.LEADER) {
                player.sendMessage("§c只有宗主才能访问宗门设置!");
                return;
            }

            player.sendMessage("§b========== 宗门设置 ==========");
            player.sendMessage("§e可用操作:");
            player.sendMessage("§7- 使用命令修改宗门信息");
            player.sendMessage("§7- 使用命令管理宗门成员");
            player.sendMessage("§7- 使用 §a/sect disband §7解散宗门");
            player.sendMessage("§e相关命令:");
            player.sendMessage("§7- §a/sect info §7查看宗门信息");
            player.sendMessage("§7- §a/sect invite <玩家> §7邀请成员");
            player.sendMessage("§7- §a/sect kick <玩家> §7踢出成员");
            player.sendMessage("§b===========================");
        }), 1, 4);

        // 领地管理 (仅宗主)
        ItemStack landButton = new ItemBuilder(Material.GRASS_BLOCK)
                .name("§a§l领地管理")
                .lore(
                        "§7管理宗门领地",
                        "",
                        "§e功能:",
                        "§7- 绑定/解绑领地",
                        "§7- 查看领地信息",
                        "§7- 配置领地权限",
                        "§7- 领地升级",
                        "",
                        "§a仅宗主可用"
                )
                .build();

        pane.addItem(new GuiItem(landButton, event -> {
            // 检查权限：只有宗主可以访问领地管理
            SectRank sectRank = SectRank.fromRankString(data.getSectRank());
            if (sectRank != SectRank.LEADER) {
                player.sendMessage("§c只有宗主才能管理领地!");
                return;
            }

            player.closeInventory();
            // 打开领地管理GUI（已整合所有功能）
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.performCommand("sect land gui");
            }, 1L);
        }), 3, 4);

        // 退出宗门
        ItemStack leaveButton = new ItemBuilder(Material.IRON_DOOR)
                .name("§c§l退出宗门")
                .lore(
                        "§7离开当前宗门",
                        "",
                        "§c警告:",
                        "§7- 将失去所有宗门权益",
                        "§7- 贡献点不会保留",
                        "§7- 宗主无法直接退出",
                        "",
                        "§c点击退出宗门"
                )
                .build();

        pane.addItem(new GuiItem(leaveButton, event -> {
            player.sendMessage("§e正在退出宗门...");
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                player.performCommand("sect leave");
            }, 1L);
        }), 7, 4);
    }
}
