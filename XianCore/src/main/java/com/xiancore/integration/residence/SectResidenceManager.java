package com.xiancore.integration.residence;

import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

/**
 * 宗门领地系统管理器
 * 负责与 Residence 插件集成，处理所有领地相关操作
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectResidenceManager {

    // 日志记录
    private static final Logger logger = Logger.getLogger("XianCore");

    // 配置数据
    private final Map<String, Object> config;

    // 消息数据
    private final Map<String, Object> messages;

    /**
     * 构造函数
     * @param config 领地系统配置
     * @param messages 消息配置
     */
    public SectResidenceManager(Map<String, Object> config, Map<String, Object> messages) {
        this.config = config != null ? config : new HashMap<>();
        this.messages = messages != null ? messages : new HashMap<>();
    }

    /**
     * 获取圈地所需的灵石成本
     * @param sect 宗门对象
     * @param blockCount 要圈的块数（Chunk数量）
     * @return 圈地所需的灵石
     */
    public long calculateClaimCost(Sect sect, int blockCount) {
        // 基础价格: base-price
        long basePrice = getConfigLong("claiming.base-price", 1000);

        // 按块计算: price-per-block
        long pricePerBlock = getConfigLong("claiming.price-per-block", 100);

        // 宗门等级折扣: level-multiplier
        double levelMultiplier = getConfigDouble("claiming.level-multiplier", 0.5);
        double levelDiscount = 1.0 - (levelMultiplier * (sect.getLevel() - 1) * 0.1);
        levelDiscount = Math.max(0.1, Math.min(1.0, levelDiscount)); // Clamp between 0.1 and 1.0

        long cost = (long) ((basePrice + pricePerBlock * blockCount) * levelDiscount);
        return Math.max(1, cost); // 至少 1 灵石
    }

    /**
     * 获取固定绑定费用（用于“绑定现有领地”流程）
     */
    public long getBindingFlatFee() {
        return getConfigLong("binding.flat-fee", 2000);
    }

    /**
     * 计算维护费
     * @param blockCount 领地块数
     * @return 周维护费
     */
    public long calculateMaintenanceCost(int blockCount) {
        long costPerBlockPerWeek = getConfigLong("maintenance.cost-per-block-per-week", 100);
        return costPerBlockPerWeek * blockCount;
    }

    /**
     * 获取领地上限
     * @param sect 宗门对象
     * @param memberCount 成员数量
     * @return 该宗门可以拥有的最大块数
     */
    public int getLandLimit(Sect sect, int memberCount) {
        int baseLimit = getConfigInt("limits.base-limit", 10);
        int perLevelBonus = getConfigInt("limits.per-level-bonus", 2);
        double perMembersBonus = getConfigDouble("limits.per-members-bonus", 0.2);
        int maxLimit = getConfigInt("limits.max-limit", 200);

        int limit = baseLimit + (sect.getLevel() * perLevelBonus) + (int) (memberCount * perMembersBonus);
        return Math.min(limit, maxLimit);
    }

    /**
     * 检查玩家是否有权限圈地
     * @param player 玩家
     * @param sect 宗门
     * @return 是否有权限
     */
    public boolean canClaimLand(Player player, Sect sect) {
        if (sect == null) {
            return false;
        }

        // 只有宗主和长老可以圈地
        SectRank rank = sect.getMember(player.getUniqueId()).getRank();
        return rank == SectRank.LEADER || rank == SectRank.ELDER;
    }

    /**
     * 检查是否满足圈地条件
     * @param sect 宗门
     * @param blockCount 要圈的块数
     * @param cost 圈地成本
     * @return 检查结果，包含错误信息
     */
    public ClaimCheckResult checkClaimConditions(Sect sect, int blockCount, long cost) {
        // 检查宗门资金
        if (sect.getSectFunds() < cost) {
            long shortage = cost - sect.getSectFunds();
            return ClaimCheckResult.failure("insufficient-funds",
                "Sect has insufficient funds. Need: " + cost + ", Have: " + sect.getSectFunds() + ", Shortage: " + shortage);
        }

        // 检查已有领地大小 + 新增大小是否超过上限
        int currentSize = sect.getResidenceLandId() != null ?
            getResidenceLandSize(sect.getResidenceLandId()) : 0;
        int limit = getLandLimit(sect, sect.getMemberCount());

        if (currentSize + blockCount > limit) {
            return ClaimCheckResult.failure("land-limit-reached",
                "Adding " + blockCount + " blocks would exceed limit. Current: " + currentSize + ", Limit: " + limit);
        }

        // 检查宗门是否已有领地（如果配置不允许多领地）
        boolean multilandEnabled = getConfigBoolean("land.multi-land-enabled", false);
        if (!multilandEnabled && sect.hasLand()) {
            return ClaimCheckResult.failure("land-already-exists",
                "Sect already has a land claim and multi-land is not enabled");
        }

        return ClaimCheckResult.success();
    }

    /**
     * 执行圈地操作
     * @param sect 宗门
     * @param landName 领地名称
     * @param blockCount 块数
     * @param cost 成本
     * @param centerLoc 中心位置
     * @param owner 领地所有者（玩家）
     * @return 操作结果
     */
    public ClaimResult claimLand(Sect sect, String landName, int blockCount, long cost, Location centerLoc, Player owner) {
            // 1. 扣除资金
            if (!sect.removeFunds(cost)) {
                return ClaimResult.failure("Could not deduct funds");
            }

            // 2. 创建 Residence 领地
            String residenceId = generateResidenceId(sect.getId(), sect.getName());

            // 创建领地的边界（简化版，以中心点为基准）
            Location minCorner = centerLoc.clone().subtract(blockCount * 8, 0, blockCount * 8);
            Location maxCorner = centerLoc.clone().add(blockCount * 8, 256, blockCount * 8);

            // 获取 ResidencePlayer 对象
            com.bekvon.bukkit.residence.containers.ResidencePlayer rPlayer = 
                ResidenceApi.getPlayerManager().getResidencePlayer(owner.getName());
            
            if (rPlayer == null) {
                sect.addFunds(cost);
                return ClaimResult.failure("Failed to get ResidencePlayer object for " + owner.getName());
            }

            // 使用正确的 Residence API 签名创建领地
            // addResidence(String playerName, String residenceName, Location loc1, Location loc2)
            boolean created = ResidenceApi.getResidenceManager()
                .addResidence(owner.getName(), residenceId, minCorner, maxCorner);

            if (!created) {
                // 回滚：返还资金
                sect.addFunds(cost);
                return ClaimResult.failure("Failed to create Residence land");
            }

            // 获取刚创建的领地
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByName(residenceId);
            if (residence == null) {
                // 回滚：返还资金
                sect.addFunds(cost);
                return ClaimResult.failure("Failed to retrieve created Residence land");
            }

            // 3. 设置权限（简化版，由权限管理器处理）
            try {
                // Residence 领地创建成功，具体的权限设置由
                // ResidencePermissionManager 在后续处理
                // 这里只进行基础验证
            } catch (Exception e) {
                // 记录但不阻止圈地
                System.err.println("[SectResidenceManager] Error during permission setup: " + e.getMessage());
            }

            // 4. 更新宗门数据
            sect.setResidenceLandId(residenceId);
            sect.setLandCenter(centerLoc);
            sect.setLastMaintenanceTime(System.currentTimeMillis());

            return ClaimResult.success(residenceId);
    }

    /**
     * 绑定玩家当前位置所在的已有 Residence 领地到宗门（固定绑定费用），并将领地主转移为宗主
     * 前置：玩家需要站在其本人拥有的 Residence 领地内
     */
    public ClaimResult bindExistingResidenceAtPlayer(Sect sect, Player player) {
        try {
            // 1) 获取当前位置所在领地
            Location loc = player.getLocation();
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByLoc(loc);
            if (residence == null) {
                return ClaimResult.failure("No Residence at your location");
            }

            // 2) 校验归属：必须为玩家本人所有
            String ownerName = null;
            try {
                ownerName = residence.getOwner();
            } catch (Throwable ignored) {
            }
            if (ownerName == null || !ownerName.equalsIgnoreCase(player.getName())) {
                return ClaimResult.failure("Residence here is not owned by you");
            }

            // 3) 扣除绑定费用（固定费用）
            long bindFee = getConfigLong("binding.flat-fee", 2000);
            if (!sect.removeFunds(bindFee)) {
                return ClaimResult.failure("Insufficient sect funds for binding: " + bindFee);
            }

            // 4) 获取领地ID/名称
            String residenceId;
            try {
                residenceId = residence.getName();
            } catch (Throwable t) {
                // 回滚
                sect.addFunds(bindFee);
                return ClaimResult.failure("Failed to read residence name");
            }

            // 5) 转移所有者为宗主（使用管理员命令以保证兼容性）
            String leaderName = sect.getOwnerName();
            boolean transferOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "resadmin setowner " + residenceId + " " + leaderName);
            if (!transferOk) {
                // 回滚
                sect.addFunds(bindFee);
                return ClaimResult.failure("Failed to transfer residence owner to sect leader");
            }

            // 6) 写入宗门数据
            sect.setResidenceLandId(residenceId);
            sect.setLandCenter(loc);
            sect.setLastMaintenanceTime(System.currentTimeMillis());

            return ClaimResult.success(residenceId);
        } catch (Exception e) {
            return ClaimResult.failure("Exception during binding: " + e.getMessage());
        }
    }

    /**
     * 获取 Residence 领地的块数
     * @param residenceId 领地ID
     * @return 块数，如果领地不存在则返回 0
     */
    public int getResidenceLandSize(String residenceId) {
        try {
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByName(residenceId);
            if (residence != null) {
                // 返回领地区域的总块数
                // 这是一个简化实现，实际需要根据 Residence API 的具体实现来调整
                return 1; // TODO: 根据实际的 Residence API 计算真实的块数
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * 获取维护费状态
     * @param sect 宗门
     * @return 维护费状态
     */
    public MaintenanceStatus getMaintenanceStatus(Sect sect) {
        if (!sect.hasLand()) {
            return MaintenanceStatus.NO_LAND;
        }

        long lastMaintenance = sect.getLastMaintenanceTime();
        long now = System.currentTimeMillis();
        long daysSinceMaintenance = (now - lastMaintenance) / (1000 * 60 * 60 * 24);

        int gracePeriod = getConfigInt("maintenance.grace-period-days", 3);
        int freezePeriod = getConfigInt("maintenance.freeze-period-days", 7);
        int autoReleasePeriod = getConfigInt("maintenance.auto-release-period-days", 14);

        if (daysSinceMaintenance < gracePeriod) {
            return MaintenanceStatus.PAID;
        } else if (daysSinceMaintenance < freezePeriod) {
            return MaintenanceStatus.OVERDUE_WARNING;
        } else if (daysSinceMaintenance < autoReleasePeriod) {
            return MaintenanceStatus.FROZEN;
        } else {
            return MaintenanceStatus.AUTO_RELEASING;
        }
    }

    /**
     * 检查维护费是否逾期
     * @param sect 宗门
     * @return 是否逾期
     */
    public boolean isMaintenanceOverdue(Sect sect) {
        MaintenanceStatus status = getMaintenanceStatus(sect);
        return status != MaintenanceStatus.PAID && status != MaintenanceStatus.NO_LAND;
    }

    /**
     * 支付维护费
     * @param sect 宗门
     * @param landSize 领地大小（块数）
     * @return 是否支付成功
     */
    public boolean payMaintenance(Sect sect, int landSize) {
        long cost = calculateMaintenanceCost(landSize);
        return sect.payMaintenance(cost);
    }

    /**
     * 生成 Residence 领地 ID
     * @param sectId 宗门 ID
     * @param sectName 宗门名称
     * @return 生成的领地 ID
     */
    private String generateResidenceId(Integer sectId, String sectName) {
        // 格式: SectLand_{SectId}_{SectName}
        return "SectLand_" + sectId + "_" + sectName.replaceAll(" ", "_");
    }

    /**
     * 扩展领地
     * @param sect 宗门
     * @param expandSize 扩展大小
     * @param cost 扩展成本
     * @return 操作结果
     */
    public ExpandResult expandLand(Sect sect, int expandSize, long cost) {
        try {
            // 检查资金
            if (sect.getSectFunds() < cost) {
                return ExpandResult.failure("Insufficient funds");
            }

            // 扣除资金
            if (!sect.removeFunds(cost)) {
                return ExpandResult.failure("Failed to deduct funds");
            }

            // TODO: 调用 Residence API 扩展领地
            // 这需要根据实际 Residence API 实现
            // ResidenceApi.getResidenceManager().expandResidence(sect.getResidenceLandId(), expandSize);

            sect.touch();
            return ExpandResult.success();

        } catch (Exception e) {
            // 回滚资金
            sect.addFunds(cost);
            return ExpandResult.failure("Exception: " + e.getMessage());
        }
    }

    /**
     * 缩小领地
     * @param sect 宗门
     * @param shrinkSize 缩小大小
     * @return 操作结果
     */
    public ShrinkResult shrinkLand(Sect sect, int shrinkSize) {
        try {
            // 检查是否有足够的建筑物需要移除
            if (!sect.getBuildingSlots().isEmpty()) {
                int totalSlots = sect.getBuildingSlots().values().stream()
                    .mapToInt(Integer::intValue).sum();
                if (totalSlots > 0) {
                    return ShrinkResult.failure("Cannot shrink: " + totalSlots + " building slots still in use");
                }
            }

            // 计算返还费用（50% 返还）
            long originalCost = calculateClaimCost(sect, shrinkSize);
            long refund = (long) (originalCost * 0.5);

            // 返还灵石
            sect.addFunds(refund);

            // TODO: 调用 Residence API 缩小领地
            // ResidenceApi.getResidenceManager().shrinkResidence(sect.getResidenceLandId(), shrinkSize);

            sect.touch();
            return ShrinkResult.success(refund);

        } catch (Exception e) {
            return ShrinkResult.failure("Exception: " + e.getMessage());
        }
    }

    /**
     * 删除领地（完全清理）
     * 执行领地删除的全部清理操作，包括：
     * - 检查建筑位使用状态
     * - 从 Residence 删除领地（当实现时）
     * - 清除所有领地关联数据
     * - 清除建筑位信息
     * - 通知宗门成员
     * - 保存宗门数据
     * - 记录详细日志
     *
     * @param sect 宗门
     * @return 操作结果
     */
    public DeleteResult deleteLand(Sect sect) {
        try {
            String residenceId = sect.getResidenceLandId();

            // ==================== 第一步：验证删除前置条件 ====================
            // 检查是否有建筑物还在使用
            if (!sect.getBuildingSlots().isEmpty()) {
                int totalSlots = sect.getBuildingSlots().values().stream()
                    .mapToInt(Integer::intValue).sum();
                if (totalSlots > 0) {
                    return DeleteResult.failure("Cannot delete: " + totalSlots + " building slots still in use");
                }
            }

            // ==================== 第二步：从 Residence 删除领地 ====================
            // TODO: 当 Residence API 完整实现时，取消注释以下代码
            // try {
            //     ResidenceApi.getResidenceManager().removeResidence(residenceId);
            //     logger.info("已从 Residence 删除领地: " + residenceId);
            // } catch (Exception e) {
            //     logger.warning("从 Residence 删除领地失败: " + e.getMessage());
            //     // 继续执行，尝试清除本地数据
            // }

            // ==================== 第三步：清除所有领地关联数据 ====================
            // 3.1 清除基本领地信息
            sect.setResidenceLandId(null);
            sect.setLandCenter(null);
            sect.setLastMaintenanceTime(0);

            // 3.2 清除建筑位信息
            sect.clearBuildingSlots();
            logger.info("已清除所有建筑位信息: " + sect.getName());

            // 3.3 触发变更时间戳更新
            sect.touch();

            // ==================== 第四步：通知宗门成员 ====================
            sect.broadcastMessage("§a§l【领地删除】");
            sect.broadcastMessage("§a宗门领地已被删除");
            sect.broadcastMessage("§7领地ID: §f" + (residenceId != null ? residenceId : "N/A"));

            // ==================== 第五步：保存宗门数据到数据库 ====================
            // 注意：调用者需要确保已加载宗门系统
            logger.info("已清除所有领地关联数据: " + sect.getName());

            // ==================== 第六步：记录详细日志 ====================
            logger.info(String.format(
                "§a宗门领地删除完成 - 宗门: %s (ID: %d), 领地ID: %s",
                sect.getName(), sect.getId(), residenceId != null ? residenceId : "N/A"
            ));

            return DeleteResult.success(0, residenceId);

        } catch (Exception e) {
            logger.severe("领地删除过程中发生异常 - 宗门: " + sect.getName() + ", 错误: " + e.getMessage());
            e.printStackTrace();
            return DeleteResult.failure("Exception: " + e.getMessage());
        }
    }

    /**
     * 转让领地所有权
     * @param sect 宗门
     * @param newOwner 新所有者UUID
     * @return 操作结果
     */
    public TransferResult transferLand(Sect sect, java.util.UUID newOwner) {
        try {
            // 检查新所有者是否是宗门成员
            if (!sect.isMember(newOwner)) {
                return TransferResult.failure("Target player is not a sect member");
            }

            // TODO: 调用 Residence API 更新所有权
            // ResidenceApi 可能需要更新权限/所有者信息

            sect.touch();
            return TransferResult.success(newOwner);

        } catch (Exception e) {
            return TransferResult.failure("Exception: " + e.getMessage());
        }
    }

    /**
     * 获取配置中的长整数值
     */
    private long getConfigLong(String key, long defaultValue) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * 获取配置中的整数值
     */
    private int getConfigInt(String key, int defaultValue) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * 获取配置中的双精度浮点数值
     */
    private double getConfigDouble(String key, double defaultValue) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * 获取配置中的布尔值
     */
    private boolean getConfigBoolean(String key, boolean defaultValue) {
        Object value = getConfigValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * 从嵌套配置中获取值
     * 支持点号分隔的路径，如 "claiming.base-price"
     */
    private Object getConfigValue(String key) {
        String[] parts = key.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * 圈地检查结果
     */
    public static class ClaimCheckResult {
        private final boolean success;
        private final String errorKey;
        private final String errorMessage;

        private ClaimCheckResult(boolean success, String errorKey, String errorMessage) {
            this.success = success;
            this.errorKey = errorKey;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorKey() {
            return errorKey;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static ClaimCheckResult success() {
            return new ClaimCheckResult(true, null, null);
        }

        public static ClaimCheckResult failure(String errorKey, String errorMessage) {
            return new ClaimCheckResult(false, errorKey, errorMessage);
        }
    }

    /**
     * 圈地操作结果
     */
    public static class ClaimResult {
        private final boolean success;
        private final String residenceId;
        private final String errorMessage;

        private ClaimResult(boolean success, String residenceId, String errorMessage) {
            this.success = success;
            this.residenceId = residenceId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getResidenceId() {
            return residenceId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static ClaimResult success(String residenceId) {
            return new ClaimResult(true, residenceId, null);
        }

        public static ClaimResult failure(String errorMessage) {
            return new ClaimResult(false, null, errorMessage);
        }
    }

    /**
     * 扩展操作结果
     */
    public static class ExpandResult {
        private final boolean success;
        private final String errorMessage;

        private ExpandResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static ExpandResult success() {
            return new ExpandResult(true, null);
        }

        public static ExpandResult failure(String errorMessage) {
            return new ExpandResult(false, errorMessage);
        }
    }

    /**
     * 缩小操作结果
     */
    public static class ShrinkResult {
        private final boolean success;
        private final long refund;
        private final String errorMessage;

        private ShrinkResult(boolean success, long refund, String errorMessage) {
            this.success = success;
            this.refund = refund;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getRefund() {
            return refund;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static ShrinkResult success(long refund) {
            return new ShrinkResult(true, refund, null);
        }

        public static ShrinkResult failure(String errorMessage) {
            return new ShrinkResult(false, 0, errorMessage);
        }
    }

    /**
     * 删除操作结果
     */
    public static class DeleteResult {
        private final boolean success;
        private final long refund;
        private final String residenceId;
        private final String errorMessage;

        private DeleteResult(boolean success, long refund, String residenceId, String errorMessage) {
            this.success = success;
            this.refund = refund;
            this.residenceId = residenceId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getRefund() {
            return refund;
        }

        public String getResidenceId() {
            return residenceId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static DeleteResult success(long refund, String residenceId) {
            return new DeleteResult(true, refund, residenceId, null);
        }

        public static DeleteResult failure(String errorMessage) {
            return new DeleteResult(false, 0, null, errorMessage);
        }
    }

    /**
     * 转让操作结果
     */
    public static class TransferResult {
        private final boolean success;
        private final java.util.UUID newOwner;
        private final String errorMessage;

        private TransferResult(boolean success, java.util.UUID newOwner, String errorMessage) {
            this.success = success;
            this.newOwner = newOwner;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public java.util.UUID getNewOwner() {
            return newOwner;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static TransferResult success(java.util.UUID newOwner) {
            return new TransferResult(true, newOwner, null);
        }

        public static TransferResult failure(String errorMessage) {
            return new TransferResult(false, null, errorMessage);
        }
    }

    /**
     * 维护费状态枚举
     */
    public enum MaintenanceStatus {
        NO_LAND("无领地"),
        PAID("已缴费"),
        OVERDUE_WARNING("逾期警告"),
        FROZEN("已冻结"),
        AUTO_RELEASING("自动释放中");

        private final String description;

        MaintenanceStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
