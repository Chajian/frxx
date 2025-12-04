package com.xiancore.integration.residence;

import com.bekvon.bukkit.residence.event.ResidenceDeleteEvent;
import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Residence 相关事件监听器
 * 处理与 Residence 领地系统的交互事件
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ResidenceEventListener implements Listener {

    private final XianCore plugin;
    private final SectSystem sectSystem;
    private final SectResidenceManager residenceManager;

    /**
     * 构造函数
     */
    public ResidenceEventListener(XianCore plugin, SectSystem sectSystem, SectResidenceManager residenceManager) {
        this.plugin = plugin;
        this.sectSystem = sectSystem;
        this.residenceManager = residenceManager;
    }

    /**
     * 监听玩家移动事件，判断是否进出宗门领地
     * 这是一个简化实现，实际可能需要更高效的方式
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // 只处理玩家移动到不同的块时
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // 同一块内，无需处理
        }

        // 获取玩家所在宗门
        Sect playerSect = sectSystem.getPlayerSect(player.getUniqueId());
        if (playerSect == null || !playerSect.hasLand()) {
            return; // 玩家不在宗门或宗门无领地
        }

        // TODO: 集成 Residence API 来判断玩家是否进入/离开领地
        // 这需要检查 event.getTo() 是否在 playerSect.getResidenceLandId() 的领地范围内
        // 实现方式可能需要缓存上一次的位置和当前位置的领地信息
    }

    /**
     * 监听领地删除事件
     * 当 Residence 领地被删除时，同步更新宗门数据并进行完全清理
     *
     * 处理流程：
     * 1. 查找拥有该领地的宗门
     * 2. 清除所有领地关联数据
     * 3. 清除维护费记录（如果适用）
     * 4. 通知宗门成员
     * 5. 保存宗门数据到数据库
     * 6. 记录详细日志
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResidenceDelete(ResidenceDeleteEvent event) {
        String deletedLandId = event.getResidence().getName();

        try {
            // ==================== 第一步：查找拥有这个领地的宗门 ====================
            Sect sect = findSectByLandId(deletedLandId);
            if (sect == null) {
                return; // 不是宗门领地
            }

            plugin.getLogger().info("检测到宗门领地被删除: " + deletedLandId + " (宗门: " + sect.getName() + ")");

            // ==================== 第二步：清除所有领地关联数据 ====================
            // 2.1 清除基本领地信息
            sect.setResidenceLandId(null);
            sect.setLandCenter(null);
            sect.setLastMaintenanceTime(0);

            // 2.2 清除建筑位信息
            sect.clearBuildingSlots();
            plugin.getLogger().info("已清除所有建筑位信息: " + sect.getName());

            // 2.3 触发变更时间戳更新
            sect.touch();

            // ==================== 第三步：清除维护费记录（如果有欠费） ====================
            // 注意：在正常情况下，欠费删除应由 MaintenanceDebtManager 处理
            // 这里只是额外的安全措施，防止数据残留
            // (如果需要访问 MaintenanceDebtManager，可以通过 plugin.getMaintenanceFeeScheduler().getDebtManager())

            // ==================== 第四步：通知宗门成员 ====================
            sect.broadcastMessage("§c§l【领地删除】");
            sect.broadcastMessage("§c宗门领地已被删除！");
            sect.broadcastMessage("§7领地ID: §f" + deletedLandId);
            sect.broadcastMessage("§7原因: 在 Residence 插件中被删除");

            // ==================== 第五步：保存宗门数据到数据库 ====================
            sectSystem.saveSect(sect);
            plugin.getLogger().info("已保存宗门数据到数据库: " + sect.getName());

            // ==================== 第六步：记录详细日志 ====================
            plugin.getLogger().warning(String.format(
                "§c宗门领地删除完成 - 宗门: %s (ID: %d), 领地ID: %s, 事件来源: Residence 删除事件",
                sect.getName(), sect.getId(), deletedLandId
            ));

        } catch (Exception e) {
            plugin.getLogger().severe("处理领地删除事件时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据领地ID查找宗门
     * @param landId 领地ID
     * @return 宗门对象，如果未找到返回 null
     */
    private Sect findSectByLandId(String landId) {
        // 遍历所有宗门，查找匹配的领地ID
        for (Sect sect : sectSystem.getAllSects()) {
            if (sect.hasLand() && sect.getResidenceLandId().equals(landId)) {
                return sect;
            }
        }
        return null;
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * 额外的事件处理（可在后续完善）
     * ═══════════════════════════════════════════════════════════════
     */

    /**
     * 处理维护费逾期事件（可选实现）
     * 这需要与维护费系统的定时任务配合
     */
    public void handleMaintenanceOverdue(Sect sect) {
        if (sect == null || !sect.hasLand()) {
            return;
        }

        SectResidenceManager.MaintenanceStatus status = residenceManager.getMaintenanceStatus(sect);

        switch (status) {
            case OVERDUE_WARNING:
                sect.broadcastMessage("§e§l[警告] 宗门领地维护费即将逾期！");
                break;

            case FROZEN:
                sect.broadcastMessage("§c§l[冻结通知] 宗门领地维护费逾期，领地功能已被冻结！");
                break;

            case AUTO_RELEASING:
                sect.broadcastMessage("§c§l[释放通知] 宗门领地维护费逾期过久，部分领地已自动释放！");
                break;

            default:
                break;
        }
    }

    /**
     * 处理建筑位不足事件
     * @param sect 宗门
     * @param facilityType 设施类型
     * @param needed 需要的建筑位
     * @param available 可用的建筑位
     */
    public void handleInsufficientBuildingSlots(Sect sect, String facilityType, int needed, int available) {
        sect.broadcastMessage("§c建筑位不足！设施类型: " + facilityType + "，需要: " + needed + "，可用: " + available);
    }

    /**
     * 获取监听器名称
     */
    @Override
    public String toString() {
        return "ResidenceEventListener{" +
                "plugin=" + plugin.getName() +
                ", sectSystem=" + sectSystem.getClass().getSimpleName() +
                '}';
    }
}
