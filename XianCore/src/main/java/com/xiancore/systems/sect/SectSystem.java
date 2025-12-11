package com.xiancore.systems.sect;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.integration.residence.MaintenanceFeeScheduler;
import com.xiancore.integration.residence.MaintenanceFeeGUI;
import com.xiancore.integration.residence.ResidencePermissionManager;
import com.xiancore.integration.residence.SectResidenceManager;
import com.xiancore.integration.residence.BuildingSlotManager;
import com.xiancore.integration.residence.SectBuildingCommand;
import com.xiancore.integration.residence.SectLandLeaderboard;
import com.xiancore.integration.residence.SectLandGUI;
import com.xiancore.systems.sect.facilities.SectFacilityGUI;
import com.xiancore.systems.sect.facilities.SectFacilityManager;
import com.xiancore.systems.sect.warehouse.SectWarehouseManager;
import com.xiancore.systems.sect.warehouse.SectWarehouseGUI;
import com.xiancore.systems.sect.shop.SectShopGUI;
import com.xiancore.systems.sect.listeners.SectTaskListener;
import com.xiancore.systems.sect.task.*;
import com.xiancore.systems.sect.util.SectDataSyncManager;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 宗门系统
 * 负责管理宗门的创建、加入、任务等功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SectSystem {

    private final XianCore plugin;
    private final Map<Integer, Sect> sects;              // 宗门ID -> 宗门
    private final Map<String, Integer> sectNameIndex;    // 宗门名称 -> 宗门ID
    private final Map<UUID, Integer> playerSects;        // 玩家UUID -> 宗门ID
    private final Map<UUID, Map<Integer, Long>> invitations; // 玩家UUID -> (宗门ID -> 过期时间)
    private final AtomicInteger nextSectId;
    private final SectActivityManager activityManager;   // 活跃度管理器
    private final SectTaskManager taskManager;           // 任务管理器
    private final TaskProgressTracker progressTracker;   // 进度跟踪器
    private final SectTaskListener taskListener;         // 任务监听器
    private final SectTaskGUI taskGUI;                   // 任务界面
    private final SectFacilityManager facilityManager;   // 设施管理器
    private final SectFacilityGUI facilityGUI;           // 设施界面
    private final SectWarehouseManager warehouseManager; // 仓库管理器
    private final SectWarehouseGUI warehouseGUI;         // 仓库界面
    private final SectShopGUI shopGUI;                   // 商店界面
    private final SectResidenceManager residenceManager; // 领地管理器
    private final ResidencePermissionManager permissionManager; // 权限管理器
    private final MaintenanceFeeScheduler maintenanceFeeScheduler; // 维护费调度器
    private final MaintenanceFeeGUI maintenanceFeeGUI; // 维护费GUI
    private final BuildingSlotManager buildingSlotManager; // 建筑位管理器
    private final SectBuildingCommand buildingCommand; // 建筑位命令
    private final SectLandLeaderboard landLeaderboard; // 领地排行榜
    private final SectLandGUI landGUI; // 领地管理GUI
    private SectDataSyncManager syncManager; // 数据同步管理器
    private boolean initialized = false;

    public SectSystem(XianCore plugin) {
        this.plugin = plugin;
        this.sects = new ConcurrentHashMap<>();
        this.sectNameIndex = new ConcurrentHashMap<>();
        this.playerSects = new ConcurrentHashMap<>();
        this.invitations = new ConcurrentHashMap<>();
        this.nextSectId = new AtomicInteger(1);
        this.activityManager = new SectActivityManager(plugin);
        this.taskManager = new SectTaskManager(plugin);
        this.progressTracker = new TaskProgressTracker(plugin, taskManager);
        this.taskListener = new SectTaskListener(plugin, progressTracker);
        this.taskGUI = new SectTaskGUI(plugin, taskManager);
        this.facilityManager = new SectFacilityManager(plugin);
        this.facilityGUI = new SectFacilityGUI(plugin);
        this.warehouseManager = new SectWarehouseManager(plugin);
        this.warehouseGUI = new SectWarehouseGUI(plugin);
        this.shopGUI = new SectShopGUI(plugin);
        this.residenceManager = new SectResidenceManager(new HashMap<>(), new HashMap<>());
        this.permissionManager = new ResidencePermissionManager(plugin);
        this.maintenanceFeeScheduler = new MaintenanceFeeScheduler(plugin, this, residenceManager);
        this.maintenanceFeeGUI = new MaintenanceFeeGUI(plugin, this, residenceManager, maintenanceFeeScheduler);
        this.buildingSlotManager = new BuildingSlotManager();
        this.buildingCommand = new SectBuildingCommand(plugin, this, buildingSlotManager);
        this.landLeaderboard = new SectLandLeaderboard(this);
        this.landGUI = new SectLandGUI(plugin, this, buildingSlotManager);
    }

    /**
     * 初始化宗门系统
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 初始化数据同步管理器
        this.syncManager = new SectDataSyncManager(plugin);

        // 从数据库加载宗门数据
        List<Sect> loadedSects = plugin.getDataManager().loadAllSects();
        for (Sect sect : loadedSects) {
            sects.put(sect.getId(), sect);
            sectNameIndex.put(sect.getName(), sect.getId());

            // 重建玩家-宗门映射
            for (SectMember member : sect.getMemberList()) {
                playerSects.put(member.getPlayerId(), sect.getId());
            }

            // 更新 nextSectId
            if (sect.getId() >= nextSectId.get()) {
                nextSectId.set(sect.getId() + 1);
            }
        }

        // 初始化活跃度管理器
        activityManager.initialize();

        // 初始化任务系统
        taskManager.initialize();
        taskListener.register();

        // 初始化设施管理器
        facilityManager.initialize();

        // 注册设施 GUI 事件
        plugin.getServer().getPluginManager().registerEvents(facilityGUI, plugin);

        // 初始化仓库管理器
        warehouseManager.initialize();

        // 注册仓库 GUI 事件
        plugin.getServer().getPluginManager().registerEvents(warehouseGUI, plugin);

        // SectShopGUI 使用 IF Framework，不需要手动注册 Listener

        // 注册维护费 GUI 事件
        plugin.getServer().getPluginManager().registerEvents(maintenanceFeeGUI, plugin);

        // 注册领地管理 GUI 事件
        plugin.getServer().getPluginManager().registerEvents(landGUI, plugin);

        // 启动维护费定时扣除系统
        maintenanceFeeScheduler.start();

        initialized = true;
        plugin.getLogger().info("  §a✓ 宗门系统初始化完成 (加载了 " + loadedSects.size() + " 个宗门)");

        // 验证并修复数据一致性
        validateAndFixDataConsistency();
    }

    /**
     * 创建宗门
     */
    public boolean createSect(Player player, String sectName) {
        UUID playerId = player.getUniqueId();

        // 检查玩家是否已在宗门
        if (playerSects.containsKey(playerId)) {
            player.sendMessage("§c你已经加入了宗门!");
            return false;
        }

        // 检查宗门名称是否已存在
        if (sectNameIndex.containsKey(sectName)) {
            player.sendMessage("§c宗门名称已被使用!");
            return false;
        }

        // 检查名称长度
        if (sectName.length() < 2 || sectName.length() > 10) {
            player.sendMessage("§c宗门名称长度必须在2-10个字符之间!");
            return false;
        }

        // 获取玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        // 检查灵石
        int createCost = 1000; // 创建费用
        if (data.getSpiritStones() < createCost) {
            player.sendMessage("§c灵石不足! 需要: " + createCost + " 当前: " + data.getSpiritStones());
            return false;
        }

        // 检查境界
        String requiredRealm = "筑基期";
        if (!checkRealmRequirement(data.getRealm(), requiredRealm)) {
            player.sendMessage("§c境界不足! 需要: " + requiredRealm);
            return false;
        }

        // 扣除灵石
        data.setSpiritStones(data.getSpiritStones() - createCost);

        // 创建宗门
        int sectId = nextSectId.getAndIncrement();
        Sect sect = new Sect(sectId, sectName, playerId, player.getName());

        sects.put(sectId, sect);
        sectNameIndex.put(sectName, sectId);
        playerSects.put(playerId, sectId);
        data.setSectId(sectId);
        data.setSectRank(SectRank.LEADER.name());

        // 保存到数据库
        plugin.getDataManager().saveSect(sect);
        plugin.getDataManager().savePlayerData(data);

        player.sendMessage("§a§l==================");
        player.sendMessage("§a§l成功创建宗门!");
        player.sendMessage("§e宗门名称: §f" + sectName);
        player.sendMessage("§e宗门ID: §f" + sectId);
        player.sendMessage("§e职位: §c宗主");
        player.sendMessage("§7消耗灵石: §6" + createCost);
        player.sendMessage("§a§l==================");

        return true;
    }

    /**
     * 解散宗门
     */
    public boolean disbandSect(Player player) {
        UUID playerId = player.getUniqueId();
        Integer sectId = playerSects.get(playerId);

        if (sectId == null) {
            player.sendMessage("§c你没有加入宗门!");
            return false;
        }

        Sect sect = sects.get(sectId);
        if (sect == null) {
            player.sendMessage("§c宗门不存在!");
            return false;
        }

        // 检查是否为宗主
        if (!sect.isOwner(playerId)) {
            player.sendMessage("§c只有宗主才能解散宗门!");
            return false;
        }

        // 通知所有成员
        for (SectMember member : sect.getMemberList()) {
            Player memberPlayer = plugin.getServer().getPlayer(member.getPlayerId());
            if (memberPlayer != null && memberPlayer.isOnline()) {
                memberPlayer.sendMessage("§c§l宗门 [" + sect.getName() + "] 已被解散!");
            }

            // 清除成员的宗门数据
            playerSects.remove(member.getPlayerId());
            PlayerData memberData = plugin.getDataManager().loadPlayerData(member.getPlayerId());
            if (memberData != null) {
                memberData.setSectId(null);
                memberData.setSectRank("member");
                plugin.getDataManager().savePlayerData(memberData);
            }
        }

        // 移除宗门
        sects.remove(sectId);
        sectNameIndex.remove(sect.getName());

        // 从数据库删除
        plugin.getDataManager().deleteSect(sectId);

        player.sendMessage("§a成功解散宗门!");
        return true;
    }

    /**
     * 加入宗门
     */
    public boolean joinSect(Player player, String sectName) {
        UUID playerId = player.getUniqueId();

        // 检查玩家是否已在宗门
        if (playerSects.containsKey(playerId)) {
            player.sendMessage("§c你已经加入了宗门!");
            return false;
        }

        // 查找宗门
        Integer sectId = sectNameIndex.get(sectName);
        if (sectId == null) {
            player.sendMessage("§c宗门不存在!");
            return false;
        }

        Sect sect = sects.get(sectId);
        if (sect == null) {
            player.sendMessage("§c宗门不存在!");
            return false;
        }

        // 检查是否开放招募
        if (!sect.isRecruiting()) {
            player.sendMessage("§c该宗门未开放招募!");
            return false;
        }

        // 检查是否已满员
        if (sect.isFull()) {
            player.sendMessage("§c该宗门已满员!");
            return false;
        }

        // 加入宗门
        if (!sect.addMember(playerId, player.getName())) {
            player.sendMessage("§c加入宗门失败!");
            return false;
        }

        playerSects.put(playerId, sectId);

        PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
        if (data != null) {
            data.setSectId(sectId);
            data.setSectRank(SectRank.OUTER_DISCIPLE.name());

            // 使用事务化保存，确保玩家数据和宗门数据原子性更新
            try {
                plugin.getDataManager().savePlayerAndSectAtomic(data, sect);
            } catch (RuntimeException e) {
                // 如果保存失败，回滚内存状态
                sect.removeMember(playerId);
                playerSects.remove(playerId);
                player.sendMessage("§c保存数据失败，请稍后重试!");
                plugin.getLogger().warning("玩家加入宗门保存失败: " + player.getName() + " -> " + sect.getName());
                return false;
            }
        }

        // 如果宗门有领地，为新成员设置权限
        if (sect.hasLand() && sect.getResidenceLandId() != null) {
            try {
                SectMember member = sect.getMember(playerId);
                if (member != null) {
                    // 从Residence插件获取领地
                    com.bekvon.bukkit.residence.protection.ClaimedResidence residence =
                        com.bekvon.bukkit.residence.api.ResidenceApi.getResidenceManager()
                            .getByName(sect.getResidenceLandId());

                    if (residence != null) {
                        permissionManager.addMemberPermission(sect, member, residence);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("无法为玩家 " + player.getName() + " 设置领地权限: " + e.getMessage());
            }
        }

        player.sendMessage("§a§l==================");
        player.sendMessage("§a§l成功加入宗门!");
        player.sendMessage("§e宗门: §f" + sect.getName());
        player.sendMessage("§e职位: §a外门弟子");
        player.sendMessage("§a§l==================");

        // 通知宗主
        Player owner = plugin.getServer().getPlayer(sect.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage("§e[宗门] §f" + player.getName() + " §e加入了宗门!");
        }

        return true;
    }

    /**
     * 离开宗门
     */
    public boolean leaveSect(Player player) {
        UUID playerId = player.getUniqueId();
        Integer sectId = playerSects.get(playerId);

        if (sectId == null) {
            player.sendMessage("§c你没有加入宗门!");
            return false;
        }

        Sect sect = sects.get(sectId);
        if (sect == null) {
            player.sendMessage("§c宗门不存在!");
            return false;
        }

        // 检查是否为宗主
        if (sect.isOwner(playerId)) {
            player.sendMessage("§c宗主不能直接离开宗门!");
            player.sendMessage("§7请先转让宗主或解散宗门");
            return false;
        }

        // 移除成员
        SectMember member = sect.getMember(playerId); // 先保存成员信息
        sect.removeMember(playerId);
        playerSects.remove(playerId);

        // 如果宗门有领地，移除成员的权限
        if (sect.hasLand() && sect.getResidenceLandId() != null && member != null) {
            try {
                com.bekvon.bukkit.residence.protection.ClaimedResidence residence =
                    com.bekvon.bukkit.residence.api.ResidenceApi.getResidenceManager()
                        .getByName(sect.getResidenceLandId());

                if (residence != null) {
                    permissionManager.removeMemberPermission(sect, member, residence);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("无法移除玩家 " + player.getName() + " 的领地权限: " + e.getMessage());
            }
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
        if (data != null) {
            data.setSectId(null);
            data.setSectRank("member");
            data.setContributionPoints(0); // 清空贡献

            // 使用事务化保存，确保玩家数据和宗门数据原子性更新
            try {
                plugin.getDataManager().savePlayerAndSectAtomic(data, sect);
            } catch (RuntimeException e) {
                // 如果保存失败，回滚内存状态（重新添加成员）
                sect.addMember(playerId, player.getName());
                playerSects.put(playerId, sectId);
                player.sendMessage("§c保存数据失败，请稍后重试!");
                plugin.getLogger().warning("玩家离开宗门保存失败: " + player.getName() + " <- " + sect.getName());
                return false;
            }
        }

        player.sendMessage("§c你已离开宗门 [" + sect.getName() + "]");
        player.sendMessage("§7你的宗门贡献已清空");

        // 通知宗主
        Player owner = plugin.getServer().getPlayer(sect.getOwnerId());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage("§e[宗门] §f" + player.getName() + " §c离开了宗门!");
        }

        return true;
    }

    /**
     * 邀请玩家加入宗门
     */
    public boolean invitePlayer(Player inviter, Player target) {
        UUID inviterId = inviter.getUniqueId();
        UUID targetId = target.getUniqueId();

        Integer sectId = playerSects.get(inviterId);
        if (sectId == null) {
            inviter.sendMessage("§c你没有加入宗门!");
            return false;
        }

        Sect sect = sects.get(sectId);
        if (sect == null) {
            inviter.sendMessage("§c宗门不存在!");
            return false;
        }

        // 检查权限
        SectMember inviterMember = sect.getMember(inviterId);
        if (inviterMember == null || !inviterMember.getRank().canInvite()) {
            inviter.sendMessage("§c你没有邀请权限!");
            return false;
        }

        // 检查目标是否已在宗门
        if (playerSects.containsKey(targetId)) {
            inviter.sendMessage("§c该玩家已加入宗门!");
            return false;
        }

        // 检查宗门是否已满
        if (sect.isFull()) {
            inviter.sendMessage("§c宗门已满员!");
            return false;
        }

        // 创建邀请
        invitations.putIfAbsent(targetId, new ConcurrentHashMap<>());
        long expireTime = System.currentTimeMillis() + 60000; // 1分钟过期
        invitations.get(targetId).put(sectId, expireTime);

        inviter.sendMessage("§a已向 §f" + target.getName() + " §a发送宗门邀请!");

        target.sendMessage("§e§l==================");
        target.sendMessage("§e[宗门邀请]");
        target.sendMessage("§f" + inviter.getName() + " §e邀请你加入宗门:");
        target.sendMessage("§6" + sect.getName());
        target.sendMessage("§7使用 /sect accept " + sect.getName() + " 接受");
        target.sendMessage("§7邀请将在 60 秒后过期");
        target.sendMessage("§e§l==================");

        return true;
    }

    /**
     * 接受邀请
     */
    public boolean acceptInvitation(Player player, String sectName) {
        UUID playerId = player.getUniqueId();

        // 检查是否有邀请
        Map<Integer, Long> playerInvites = invitations.get(playerId);
        if (playerInvites == null || playerInvites.isEmpty()) {
            player.sendMessage("§c你没有待处理的邀请!");
            return false;
        }

        // 查找宗门
        Integer sectId = sectNameIndex.get(sectName);
        if (sectId == null) {
            player.sendMessage("§c宗门不存在!");
            return false;
        }

        // 检查邀请是否存在且未过期
        Long expireTime = playerInvites.get(sectId);
        if (expireTime == null) {
            player.sendMessage("§c你没有该宗门的邀请!");
            return false;
        }

        if (System.currentTimeMillis() > expireTime) {
            playerInvites.remove(sectId);
            player.sendMessage("§c邀请已过期!");
            return false;
        }

        // 移除邀请
        playerInvites.remove(sectId);

        // 加入宗门
        Sect sect = sects.get(sectId);
        if (sect == null || !sect.addMember(playerId, player.getName())) {
            player.sendMessage("§c加入宗门失败!");
            return false;
        }

        playerSects.put(playerId, sectId);

        PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
        if (data != null) {
            data.setSectId(sectId);
            data.setSectRank(SectRank.OUTER_DISCIPLE.name());
            plugin.getDataManager().savePlayerData(data);
        }

        // 保存宗门数据
        plugin.getDataManager().saveSect(sect);

        player.sendMessage("§a成功加入宗门 [" + sect.getName() + "]!");

        return true;
    }

    /**
     * 踢出成员
     */
    public boolean kickMember(Player kicker, String targetName) {
        UUID kickerId = kicker.getUniqueId();
        Integer sectId = playerSects.get(kickerId);

        if (sectId == null) {
            kicker.sendMessage("§c你没有加入宗门!");
            return false;
        }

        Sect sect = sects.get(sectId);
        if (sect == null) {
            kicker.sendMessage("§c宗门不存在!");
            return false;
        }

        // 检查权限
        SectMember kickerMember = sect.getMember(kickerId);
        if (kickerMember == null || !kickerMember.getRank().canKick()) {
            kicker.sendMessage("§c你没有踢出成员的权限!");
            return false;
        }

        // 查找目标成员
        SectMember targetMember = null;
        UUID targetId = null;
        for (SectMember member : sect.getMemberList()) {
            if (member.getPlayerName().equalsIgnoreCase(targetName)) {
                targetMember = member;
                targetId = member.getPlayerId();
                break;
            }
        }

        if (targetMember == null) {
            kicker.sendMessage("§c该玩家不在宗门中!");
            return false;
        }

        // 不能踢出宗主
        if (sect.isOwner(targetId)) {
            kicker.sendMessage("§c不能踢出宗主!");
            return false;
        }

        // 不能踢出比自己职位高的成员
        if (targetMember.getRank().isHigherThan(kickerMember.getRank())) {
            kicker.sendMessage("§c不能踢出职位比你高的成员!");
            return false;
        }

        // 移除成员
        sect.removeMember(targetId);
        playerSects.remove(targetId);

        // 如果宗门有领地，移除成员的权限
        if (sect.hasLand() && sect.getResidenceLandId() != null) {
            try {
                com.bekvon.bukkit.residence.protection.ClaimedResidence residence =
                    com.bekvon.bukkit.residence.api.ResidenceApi.getResidenceManager()
                        .getByName(sect.getResidenceLandId());

                if (residence != null) {
                    permissionManager.removeMemberPermission(sect, targetMember, residence);
                }
            } catch (Exception e) {
                kicker.sendMessage("§c警告: 无法移除玩家的领地权限");
                plugin.getLogger().warning("无法移除玩家 " + targetName + " 的领地权限: " + e.getMessage());
            }
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(targetId);
        if (data != null) {
            data.setSectId(null);
            data.setSectRank("member");

            // 使用事务化保存，确保玩家数据和宗门数据原子性更新
            try {
                plugin.getDataManager().savePlayerAndSectAtomic(data, sect);
            } catch (RuntimeException e) {
                // 如果保存失败，回滚内存状态（重新添加成员）
                sect.getMembers().put(targetId, targetMember);
                playerSects.put(targetId, sectId);
                kicker.sendMessage("§c保存数据失败，请稍后重试!");
                plugin.getLogger().warning("踢出宗门成员保存失败: " + targetName + " <- " + sect.getName());
                return false;
            }
        }

        kicker.sendMessage("§a已将 §f" + targetName + " §a踢出宗门!");

        // 通知被踢出的玩家
        Player target = plugin.getServer().getPlayer(targetId);
        if (target != null && target.isOnline()) {
            target.sendMessage("§c你被踢出了宗门 [" + sect.getName() + "]!");
        }

        return true;
    }

    /**
     * 获取宗门列表
     */
    public List<Sect> getAllSects() {
        return new ArrayList<>(sects.values());
    }

    /**
     * 获取玩家的宗门
     */
    public Sect getPlayerSect(UUID playerId) {
        Integer sectId = playerSects.get(playerId);
        return sectId != null ? sects.get(sectId) : null;
    }

    /**
     * 根据名称获取宗门
     */
    public Sect getSectByName(String name) {
        Integer sectId = sectNameIndex.get(name);
        return sectId != null ? sects.get(sectId) : null;
    }

    /**
     * 根据ID获取宗门
     */
    public Sect getSect(int sectId) {
        return sects.get(sectId);
    }

    /**
     * 保存宗门数据
     */
    public void saveSect(Sect sect) {
        if (sect != null) {
            sect.touch();
            plugin.getDataManager().saveSect(sect);
        }
    }

    /**
     * 保存所有宗门数据
     */
    public void saveAll() {
        plugin.getLogger().info(String.format("正在保存 %d 个宗门的数据...", sects.size()));

        // 保存宗门基础数据
        for (Sect sect : sects.values()) {
            sect.touch();
            plugin.getDataManager().saveSect(sect);
        }

        // 保存仓库数据
        if (warehouseManager != null) {
            warehouseManager.saveAll();
        }

        // 保存设施数据
        if (facilityManager != null) {
            facilityManager.saveAll();
        }

        plugin.getLogger().info("§a所有宗门数据已保存!");
    }

    /**
     * 检查境界要求
     */
    private boolean checkRealmRequirement(String playerRealm, String requiredRealm) {
        String[] realms = {"炼气期", "筑基期", "结丹期", "元婴期", "化神期", "炼虚期", "合体期", "大乘期"};
        int playerIndex = -1;
        int requiredIndex = -1;

        for (int i = 0; i < realms.length; i++) {
            if (realms[i].equals(playerRealm)) playerIndex = i;
            if (realms[i].equals(requiredRealm)) requiredIndex = i;
        }

        return playerIndex >= requiredIndex;
    }

    /**
     * 验证并修复数据一致性
     * 在服务器启动时自动检查宗门数据和玩家数据是否一致
     * 如果发现不一致，自动修复（以宗门成员表为准）
     */
    public void validateAndFixDataConsistency() {
        plugin.getLogger().info("§e正在检查宗门数据一致性...");

        int inconsistentCount = 0;
        int fixedCount = 0;

        for (Sect sect : sects.values()) {
            for (SectMember member : sect.getMemberList()) {
                UUID playerId = member.getPlayerId();

                // 加载玩家数据
                PlayerData data = plugin.getDataManager().loadPlayerData(playerId);
                if (data == null) {
                    plugin.getLogger().warning(String.format(
                            "§c玩家数据不存在但在宗门成员表中: %s (UUID: %s, 宗门: %s)",
                            member.getPlayerName(), playerId, sect.getName()));
                    inconsistentCount++;
                    continue;
                }

                // 检查玩家的 sectId 是否与宗门 ID 一致
                Integer playerSectId = data.getSectId();
                if (playerSectId == null || !playerSectId.equals(sect.getId())) {
                    plugin.getLogger().warning(String.format(
                            "§e数据不一致: 玩家 %s 在宗门 %s 的成员表中，但玩家数据显示 sectId=%s",
                            member.getPlayerName(), sect.getName(), playerSectId));

                    // 自动修复：以宗门成员表为准
                    data.setSectId(sect.getId());
                    data.setSectRank(member.getRank().name());
                    plugin.getDataManager().savePlayerData(data);

                    plugin.getLogger().info(String.format(
                            "§a自动修复: 将玩家 %s 的 sectId 更新为 %d",
                            member.getPlayerName(), sect.getId()));

                    inconsistentCount++;
                    fixedCount++;
                }
            }
        }

        // 反向检查：玩家数据中有 sectId，但不在对应宗门的成员表中
        // 注意：这需要遍历所有玩家数据，成本较高，这里我们先跳过
        // 可以通过定时任务或手动命令来执行完整检查

        if (inconsistentCount == 0) {
            plugin.getLogger().info("§a✓ 数据一致性检查完成，未发现问题");
        } else {
            plugin.getLogger().warning(String.format(
                    "§e数据一致性检查完成: 发现 %d 处不一致，已自动修复 %d 处",
                    inconsistentCount, fixedCount));
        }
    }

    /**
     * 向宗门所有在线成员广播消息
     *
     * @param sect 目标宗门
     * @param message 要广播的消息
     * @param excludePlayerId 要排除的玩家UUID，如果为null则广播给所有成员
     */
    public void broadcastToSect(Sect sect, String message, UUID excludePlayerId) {
        if (sect == null || message == null) {
            return;
        }
        
        for (SectMember member : sect.getMemberList()) {
            UUID memberId = member.getPlayerId();
            
            // 跳过被排除的玩家
            if (excludePlayerId != null && excludePlayerId.equals(memberId)) {
                continue;
            }
            
            // 获取在线玩家并发送消息
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * 获取任务调度器
     */
    public TaskRefreshScheduler getTaskScheduler() {
        if (taskManager != null) {
            return taskManager.getTaskScheduler();
        }
        return null;
    }

    /**
     * 关闭宗门系统
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        plugin.getLogger().info("  §e正在关闭宗门系统...");

        try {
            // 保存所有数据
            saveAll();

            // 关闭任务管理器
            if (taskManager != null) {
                taskManager.shutdown();
            }

            // 关闭维护费调度器
            if (maintenanceFeeScheduler != null) {
                maintenanceFeeScheduler.stop();
            }

            // 关闭设施管理器
            if (facilityManager != null) {
                facilityManager.saveAll();
            }

            // 关闭仓库管理器
            if (warehouseManager != null) {
                warehouseManager.saveAll();
            }

            // 清理缓存
            sects.clear();
            sectNameIndex.clear();
            playerSects.clear();
            invitations.clear();

            initialized = false;
            plugin.getLogger().info("  §a✓ 宗门系统已关闭");

        } catch (Exception e) {
            plugin.getLogger().severe("  §c✗ 宗门系统关闭时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
