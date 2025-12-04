package com.xiancore.systems.sect;

import lombok.Data;
import org.bukkit.Location;

import java.util.*;

/**
 * 宗门数据类
 * 存储宗门的基本信息和成员数据
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class Sect {

    // 基本信息
    private Integer id;                    // 宗门ID (唯一标识)
    private String name;                   // 宗门名称
    private String description;            // 宗门描述
    private UUID ownerId;                  // 宗主UUID
    private String ownerName;              // 宗主名称

    // 等级相关
    private int level = 1;                 // 宗门等级
    private long experience = 0;           // 宗门经验值

    // 资源相关
    private long sectFunds = 0;            // 宗门资金(灵石)
    private int sectContribution = 0;      // 宗门总贡献值

    // 成员相关
    private Map<UUID, SectMember> members = new HashMap<>();  // 成员列表
    private int maxMembers = 10;           // 最大成员数

    // 状态相关
    private boolean recruiting = true;     // 是否开放招募
    private boolean pvpEnabled = false;    // 是否开启PVP
    private String announcement = "";      // 宗门公告

    // 领地相关 (Residence Integration)
    private String residenceLandId = null;          // 关联的 Residence 领地ID
    private Location landCenter = null;             // 领地中心坐标
    private long lastMaintenanceTime = 0;           // 最后缴费时间
    private Map<String, Integer> buildingSlots = new HashMap<>();  // 建筑位使用情况

    // 时间相关
    private long createdAt;                // 创建时间
    private long updatedAt;                // 更新时间

    /**
     * 构造函数 - 用于创建新宗门
     */
    public Sect(Integer id, String name, UUID ownerId, String ownerName) {
        this(id, name, ownerId, ownerName, true);
    }

    /**
     * 构造函数 - 完整版本
     * @param autoAddOwner 是否自动添加宗主到成员列表（创建新宗门时为true，从数据库加载时为false）
     */
    public Sect(Integer id, String name, UUID ownerId, String ownerName, boolean autoAddOwner) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;

        // 只有在创建新宗门时才自动添加宗主
        if (autoAddOwner) {
            SectMember owner = new SectMember(ownerId, ownerName);
            owner.setRank(SectRank.LEADER);
            owner.setJoinedAt(createdAt);
            members.put(ownerId, owner);
        }
    }

    /**
     * 获取成员数量
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * 检查是否为宗主
     */
    public boolean isOwner(UUID playerId) {
        return ownerId.equals(playerId);
    }

    /**
     * 检查是否为成员
     */
    public boolean isMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    /**
     * 获取成员
     */
    public SectMember getMember(UUID playerId) {
        return members.get(playerId);
    }

    /**
     * 添加成员
     */
    public boolean addMember(UUID playerId, String playerName) {
        if (members.size() >= maxMembers) {
            return false;
        }

        if (members.containsKey(playerId)) {
            return false;
        }

        SectMember member = new SectMember(playerId, playerName);
        member.setJoinedAt(System.currentTimeMillis());
        members.put(playerId, member);
        this.updatedAt = System.currentTimeMillis();

        return true;
    }

    /**
     * 移除成员
     */
    public boolean removeMember(UUID playerId) {
        // 不能移除宗主
        if (isOwner(playerId)) {
            return false;
        }

        boolean removed = members.remove(playerId) != null;
        if (removed) {
            this.updatedAt = System.currentTimeMillis();
        }

        return removed;
    }

    /**
     * 转让宗主
     */
    public boolean transferOwnership(UUID newOwnerId) {
        if (!members.containsKey(newOwnerId)) {
            return false;
        }

        // 将旧宗主降为长老
        SectMember oldOwner = members.get(ownerId);
        if (oldOwner != null) {
            oldOwner.setRank(SectRank.ELDER);
        }

        // 设置新宗主
        SectMember newOwner = members.get(newOwnerId);
        newOwner.setRank(SectRank.LEADER);

        this.ownerId = newOwnerId;
        this.ownerName = newOwner.getPlayerName();
        this.updatedAt = System.currentTimeMillis();

        return true;
    }

    /**
     * 降职成员
     */
    public boolean demoteMember(UUID playerId) {
        SectMember member = members.get(playerId);
        if (member == null || member.getRank() == SectRank.OUTER_DISCIPLE) {
            return false;
        }

        SectRank currentRank = member.getRank();
        int currentLevel = currentRank.getLevel();

        // 找到下一个更低等级的职位（按等级排序，而非数组位置）
        SectRank lowerRank = null;
        int lowerLevel = Integer.MIN_VALUE;

        for (SectRank rank : SectRank.values()) {
            int rankLevel = rank.getLevel();
            // 找低于当前等级的职位中，等级最大的那个（即下一级）
            if (rankLevel < currentLevel && rankLevel > lowerLevel) {
                lowerRank = rank;
                lowerLevel = rankLevel;
            }
        }

        // 如果找不到更低的职位
        if (lowerRank == null) {
            return false;
        }

        // 执行降职
        member.setRank(lowerRank);
        this.updatedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 晋升成员 - 返回详细结果
     * @param playerId 目标玩家UUID
     * @return 晋升结果，包含成功标志和原因
     */
    public PromotionResult promoteMemberWithDiagnosis(UUID playerId) {
        SectMember member = members.get(playerId);

        // 检查成员是否存在
        if (member == null) {
            return PromotionResult.failure(
                "成员不存在",
                "目标成员在宗门的成员列表中不存在"
            );
        }

        // 检查是否是宗主
        if (member.getRank() == SectRank.LEADER) {
            return PromotionResult.failure(
                "已是最高职位",
                "目标成员已经是宗主（最高职位），无法再晋升"
            );
        }

        SectRank currentRank = member.getRank();
        int currentLevel = currentRank.getLevel();

        // 找到下一个更高等级的职位（按等级排序，而非数组位置）
        SectRank nextRank = null;
        int nextLevel = Integer.MAX_VALUE;

        for (SectRank rank : SectRank.values()) {
            int rankLevel = rank.getLevel();
            // 找高于当前等级的职位中，等级最小的那个（即下一级）
            if (rankLevel > currentLevel && rankLevel < nextLevel) {
                nextRank = rank;
                nextLevel = rankLevel;
            }
        }

        // 如果找不到更高的职位
        if (nextRank == null) {
            return PromotionResult.failure(
                "已是最高职位",
                "无法在职位列表中找到更高的职位"
            );
        }

        // 执行晋升
        member.setRank(nextRank);
        this.updatedAt = System.currentTimeMillis();

        return PromotionResult.success(
            currentRank.getDisplayName() + " → " + nextRank.getDisplayName(),
            "晋升成功"
        );
    }

    /**
     * 晋升结果类 - 提供结构化的晋升操作诊断结果
     */
    public static class PromotionResult {
        private final boolean success;           // 操作是否成功
        private final String shortMessage;       // 简短的消息标题（如："职位限制"）
        private final String detailedMessage;    // 详细的原因说明

        private PromotionResult(boolean success, String shortMessage, String detailedMessage) {
            this.success = success;
            this.shortMessage = shortMessage;
            this.detailedMessage = detailedMessage;
        }

        /**
         * 创建成功的晋升结果
         */
        public static PromotionResult success(String shortMessage, String detailedMessage) {
            return new PromotionResult(true, shortMessage, detailedMessage);
        }

        /**
         * 创建失败的晋升结果
         */
        public static PromotionResult failure(String shortMessage, String detailedMessage) {
            return new PromotionResult(false, shortMessage, detailedMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getShortMessage() {
            return shortMessage;
        }

        public String getDetailedMessage() {
            return detailedMessage;
        }

        @Override
        public String toString() {
            return "PromotionResult{" +
                    "success=" + success +
                    ", shortMessage='" + shortMessage + '\'' +
                    ", detailedMessage='" + detailedMessage + '\'' +
                    '}';
        }
    }

    /**
     * 增加宗门资金
     */
    public void addFunds(long amount) {
        this.sectFunds += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 扣除宗门资金
     */
    public boolean removeFunds(long amount) {
        if (this.sectFunds < amount) {
            return false;
        }

        this.sectFunds -= amount;
        this.updatedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 增加宗门经验
     */
    public void addExperience(long exp) {
        this.experience += exp;
        checkLevelUp();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 检查是否可以升级
     */
    private void checkLevelUp() {
        long requiredExp = calculateRequiredExp(level);
        while (experience >= requiredExp && level < 100) {
            experience -= requiredExp;
            level++;
            // 每升一级增加2个成员上限
            maxMembers += 2;
            requiredExp = calculateRequiredExp(level);
        }
    }

    /**
     * 计算升级所需经验
     */
    public long calculateRequiredExp(int level) {
        return (long) (1000 * Math.pow(1.5, level - 1));
    }

    /**
     * 获取成员列表
     */
    public List<SectMember> getMemberList() {
        return new ArrayList<>(members.values());
    }

    /**
     * 根据职位获取成员列表
     */
    public List<SectMember> getMembersByRank(SectRank rank) {
        return members.values().stream()
                .filter(member -> member.getRank() == rank)
                .toList();
    }

    /**
     * 检查是否已满员
     */
    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    /**
     * 更新时间戳
     */
    public void touch() {
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 向所有在线成员广播消息
     */
    public void broadcastMessage(String message) {
        for (SectMember member : members.values()) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(member.getPlayerId());
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * 领地相关方法 (Residence Integration)
     * ═══════════════════════════════════════════════════════════════
     */

    /**
     * 检查宗门是否拥有领地
     */
    public boolean hasLand() {
        return residenceLandId != null && !residenceLandId.isEmpty();
    }

    /**
     * 设置领地ID
     */
    public void setResidenceLandId(String landId) {
        this.residenceLandId = landId;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 获取领地ID
     */
    public String getResidenceLandId() {
        return residenceLandId;
    }

    /**
     * 设置领地中心坐标
     */
    public void setLandCenter(Location center) {
        this.landCenter = center;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 获取领地中心坐标
     */
    public Location getLandCenter() {
        return landCenter;
    }

    /**
     * 支付维护费
     * @param cost 维护费金额
     * @return 是否支付成功
     */
    public boolean payMaintenance(long cost) {
        if (!removeFunds(cost)) {
            return false;
        }
        this.lastMaintenanceTime = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 获取最后缴费时间
     */
    public long getLastMaintenanceTime() {
        return lastMaintenanceTime;
    }

    /**
     * 设置最后缴费时间
     */
    public void setLastMaintenanceTime(long time) {
        this.lastMaintenanceTime = time;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 获取可用建筑位数量
     * @param maxSlots 最大建筑位数量（由领地大小决定）
     */
    public int getAvailableSlots(int maxSlots) {
        int usedSlots = buildingSlots.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        return Math.max(0, maxSlots - usedSlots);
    }

    /**
     * 使用建筑位
     * @param facilityType 设施类型
     * @param slotsNeeded 需要的建筑位数量
     * @return 是否使用成功
     */
    public boolean useBuildingSlot(String facilityType, int slotsNeeded) {
        buildingSlots.put(facilityType, buildingSlots.getOrDefault(facilityType, 0) + slotsNeeded);
        this.updatedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 释放建筑位
     * @param facilityType 设施类型
     * @param slotsToFree 需要释放的建筑位数量
     * @return 是否释放成功
     */
    public boolean freeBuildingSlot(String facilityType, int slotsToFree) {
        int currentSlots = buildingSlots.getOrDefault(facilityType, 0);
        if (currentSlots < slotsToFree) {
            return false;
        }

        if (currentSlots == slotsToFree) {
            buildingSlots.remove(facilityType);
        } else {
            buildingSlots.put(facilityType, currentSlots - slotsToFree);
        }

        this.updatedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 获取特定设施类型已使用的建筑位
     * @param facilityType 设施类型
     * @return 已使用的建筑位数量
     */
    public int getUsedSlots(String facilityType) {
        return buildingSlots.getOrDefault(facilityType, 0);
    }

    /**
     * 获取所有建筑位使用情况
     */
    public Map<String, Integer> getBuildingSlots() {
        return new HashMap<>(buildingSlots);
    }

    /**
     * 清空所有建筑位
     */
    public void clearBuildingSlots() {
        buildingSlots.clear();
        this.updatedAt = System.currentTimeMillis();
    }

    // ==================== 显式 Getter 方法 ====================

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getLevel() {
        return level;
    }

    public long getExperience() {
        return experience;
    }

    public long getSectFunds() {
        return sectFunds;
    }

    public int getSectContribution() {
        return sectContribution;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public boolean isRecruiting() {
        return recruiting;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public String getAnnouncement() {
        return announcement;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Map<UUID, SectMember> getMembers() {
        return members;
    }

    // ==================== 显式 Setter 方法 ====================

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setLevel(int level) {
        this.level = level;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setExperience(long experience) {
        this.experience = experience;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setSectFunds(long sectFunds) {
        this.sectFunds = sectFunds;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setSectContribution(int sectContribution) {
        this.sectContribution = sectContribution;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setRecruiting(boolean recruiting) {
        this.recruiting = recruiting;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
        this.updatedAt = System.currentTimeMillis();
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setBuildingSlots(Map<String, Integer> buildingSlots) {
        this.buildingSlots = buildingSlots;
        this.updatedAt = System.currentTimeMillis();
    }
}
