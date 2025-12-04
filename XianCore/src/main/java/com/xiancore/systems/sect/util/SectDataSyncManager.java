package com.xiancore.systems.sect.util;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;
import com.bekvon.bukkit.residence.api.ResidenceApi;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions.FlagState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * 宗门数据同步管理器 - 维护Sect和PlayerData之间的一致性
 *
 * 目的：
 * - 当Sect中成员职位改变时，自动同步到PlayerData
 * - 当PlayerData中职位改变时，验证与Sect一致性
 * - 确保两个数据源始终保持同步
 * - 职位变更时同步更新 Residence 权限
 *
 * 设计模式：观察者模式 + 单向同步
 * - Sect是主数据源（真实来源）
 * - PlayerData是缓存数据源（会被同步更新）
 * - Residence权限是从属数据源（由职位决定）
 * - 所有修改必须通过SectDataSyncManager进行
 *
 * @author Olivia Diaz
 * @version 1.1.0
 */
public class SectDataSyncManager {

    private static final Logger logger = Logger.getLogger("XianCore");
    private final XianCore plugin;

    public SectDataSyncManager(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 晋升成员 - 同步版本
     * 自动将职位变更同步到PlayerData和Residence权限
     *
     * @param sect 宗门对象
     * @param targetId 目标玩家UUID
     * @return 是否晋升成功
     */
    public Sect.PromotionResult promoteAndSync(Sect sect, UUID targetId) {
        // 第一步：执行晋升（仅修改sect中的数据）
        Sect.PromotionResult result = sect.promoteMemberWithDiagnosis(targetId);

        // 第二步：如果成功，进行完整同步
        if (result.isSuccess()) {
            // 2.1 同步职位到 PlayerData
            syncMemberRankToPlayerData(targetId, sect);

            // 2.2 同步权限到 Residence（如果宗门有领地）
            if (sect.hasLand()) {
                syncMemberPermissionsToResidence(targetId, sect);
            }
        }

        return result;
    }

    /**
     * 降职成员 - 同步版本
     * 自动将职位变更同步到PlayerData和Residence权限
     *
     * @param sect 宗门对象
     * @param targetId 目标玩家UUID
     * @return 是否降职成功
     */
    public boolean demoteAndSync(Sect sect, UUID targetId) {
        // 第一步：执行降职
        boolean success = sect.demoteMember(targetId);

        // 第二步：如果成功，进行完整同步
        if (success) {
            // 2.1 同步职位到 PlayerData
            syncMemberRankToPlayerData(targetId, sect);

            // 2.2 同步权限到 Residence（如果宗门有领地）
            if (sect.hasLand()) {
                syncMemberPermissionsToResidence(targetId, sect);
            }
        }

        return success;
    }

    /**
     * 核心同步方法：将sect中的职位同步到PlayerData
     *
     * @param playerId 玩家UUID
     * @param sect 宗门对象
     */
    public void syncMemberRankToPlayerData(UUID playerId, Sect sect) {
        try {
            // 步骤1：从sect获取最新职位
            SectMember member = sect.getMember(playerId);
            if (member == null) {
                logger.warning("无法同步职位: 玩家" + playerId + "不在宗门" + sect.getName() + "中");
                return;
            }

            SectRank newRank = member.getRank();

            // 步骤2：加载PlayerData
            PlayerData playerData = plugin.getDataManager().loadPlayerData(playerId);
            if (playerData == null) {
                logger.warning("无法同步职位: 玩家数据不存在 - " + playerId);
                return;
            }

            // 步骤3：比较旧职位和新职位
            String oldRank = playerData.getSectRank();
            String newRankString = newRank.name();

            // 步骤4：如果职位发生变化，进行同步
            if (!oldRank.equals(newRankString)) {
                playerData.setSectRank(newRankString);

                // 步骤5：保存到数据库
                plugin.getDataManager().savePlayerData(playerData);

                // 步骤6：记录日志
                logger.info("职位同步成功: " + member.getPlayerName() +
                        " (" + playerId + ") 在宗门 " + sect.getName() +
                        " 中的职位从 " + oldRank + " 变更为 " + newRankString);
            }
        } catch (Exception e) {
            logger.warning("职位同步失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 同步成员权限到Residence
     * 根据职位等级更新Residence领地权限
     *
     * @param playerId 玩家UUID
     * @param sect 宗门对象
     */
    public void syncMemberPermissionsToResidence(UUID playerId, Sect sect) {
        try {
            // 步骤1：验证前提条件
            if (!sect.hasLand()) {
                logger.warning("无法同步权限: 宗门 " + sect.getName() + " 没有领地");
                return;
            }

            String residenceLandId = sect.getResidenceLandId();
            if (residenceLandId == null || residenceLandId.isEmpty()) {
                logger.warning("无法同步权限: 宗门 " + sect.getName() + " 领地ID为空");
                return;
            }

            // 步骤2：获取目标成员信息
            SectMember member = sect.getMember(playerId);
            if (member == null) {
                logger.warning("无法同步权限: 玩家 " + playerId + " 不在宗门 " + sect.getName() + " 中");
                return;
            }

            SectRank rank = member.getRank();

            // 步骤3：从Residence获取领地对象
            ClaimedResidence residence = ResidenceApi.getResidenceManager().getByName(residenceLandId);
            if (residence == null) {
                logger.warning("无法同步权限: Residence领地 " + residenceLandId + " 不存在");
                return;
            }

            // 步骤4：根据职位等级设置权限
            String playerName = member.getPlayerName();

            switch (rank) {
                case LEADER:
                case ELDER:
                    // 最高权限：完全控制
                    // 注意：具体权限方法实现依赖于 Residence API 的具体版本
                    // 参考文件：ResidencePermissionManager.java 中的权限管理实现
                    setResidencePermissionByRank(residence, playerName, rank);
                    logger.info("权限同步: " + playerName + " 在 " + sect.getName() +
                            " 领地获得完全权限 (职位: " + rank.name() + ")");
                    break;

                case CORE_DISCIPLE:
                case INNER_DISCIPLE:
                    // 中等权限：可建造和破坏
                    setResidencePermissionByRank(residence, playerName, rank);
                    logger.info("权限同步: " + playerName + " 在 " + sect.getName() +
                            " 领地获得建造权限 (职位: " + rank.name() + ")");
                    break;

                case OUTER_DISCIPLE:
                    // 最低权限：仅允许进入
                    setResidencePermissionByRank(residence, playerName, rank);
                    logger.info("权限同步: " + playerName + " 在 " + sect.getName() +
                            " 领地仅允许进入 (职位: " + rank.name() + ")");
                    break;

                default:
                    logger.warning("未知的职位等级: " + rank.name());
                    break;
            }

        } catch (Exception e) {
            logger.warning("权限同步失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据职位等级设置Residence权限的辅助方法
     * 调用Residence API为玩家设置相应的权限标志
     *
     * @param residence Residence领地对象
     * @param playerName 玩家名称
     * @param rank 职位等级
     */
    private void setResidencePermissionByRank(ClaimedResidence residence, String playerName, SectRank rank) {
        try {
            if (residence == null || playerName == null || playerName.isEmpty()) {
                logger.warning("权限设置失败：缺少必要参数");
                return;
            }

            // 获取Residence权限管理器
            var perms = residence.getPermissions();
            if (perms == null) {
                logger.warning("无法获取Residence权限管理器: " + residence.getName());
                return;
            }

            // ==================== 根据职位等级设置权限 ====================
            switch (rank) {
                case LEADER:
                case ELDER:
                    // 最高权限：管理员权限
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    logger.info("[权限同步] " + playerName + " → 管理员权限 (职位: " + rank.name() + ")");
                    break;

                case CORE_DISCIPLE:
                case INNER_DISCIPLE:
                    // 中等权限：建造者权限
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.FALSE);
                    logger.info("[权限同步] " + playerName + " → 建造者权限 (职位: " + rank.name() + ")");
                    break;

                case OUTER_DISCIPLE:
                    // 最低权限：访客权限
                    perms.setPlayerFlag(playerName, Flags.build.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.destroy.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.use.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.container.toString(), FlagState.FALSE);
                    perms.setPlayerFlag(playerName, Flags.move.toString(), FlagState.TRUE);
                    perms.setPlayerFlag(playerName, Flags.admin.toString(), FlagState.FALSE);
                    logger.info("[权限同步] " + playerName + " → 访客权限 (职位: " + rank.name() + ")");
                    break;

                default:
                    logger.warning("未知的职位等级: " + rank.name());
                    break;
            }

        } catch (Exception e) {
            logger.warning("权限设置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 验证数据一致性
     * 检查PlayerData中的职位是否与Sect一致
     *
     * @param playerId 玩家UUID
     * @param sect 宗门对象
     * @return 是否一致
     */
    public boolean verifyRankConsistency(UUID playerId, Sect sect) {
        try {
            // 从sect获取职位
            SectMember member = sect.getMember(playerId);
            if (member == null) {
                return true;  // 不在宗门中，视为一致
            }

            // 从PlayerData获取职位
            PlayerData playerData = plugin.getDataManager().loadPlayerData(playerId);
            if (playerData == null) {
                return false;  // 数据缺失，视为不一致
            }

            // 比较职位
            String sectRank = member.getRank().name();
            String playerDataRank = playerData.getSectRank();

            return sectRank.equals(playerDataRank);
        } catch (Exception e) {
            logger.warning("验证数据一致性失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 修复数据不一致
     * 如果两个数据源不一致，以sect为准修复PlayerData
     *
     * @param playerId 玩家UUID
     * @param sect 宗门对象
     * @return 是否修复成功
     */
    public boolean fixRankInconsistency(UUID playerId, Sect sect) {
        try {
            // 检查是否需要修复
            if (verifyRankConsistency(playerId, sect)) {
                logger.info("职位已一致，无需修复: " + playerId);
                return true;
            }

            // 以sect为准进行修复
            syncMemberRankToPlayerData(playerId, sect);

            // 验证修复结果
            if (verifyRankConsistency(playerId, sect)) {
                logger.info("职位不一致已修复: " + playerId);
                return true;
            } else {
                logger.warning("职位修复失败: " + playerId);
                return false;
            }
        } catch (Exception e) {
            logger.warning("修复职位不一致失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 添加成员到宗门 - 同步版本
     * 确保新成员的职位数据在sect和PlayerData中都被正确设置
     *
     * @param sect 宗门对象
     * @param playerId 玩家UUID
     * @param playerName 玩家名称
     * @return 是否添加成功
     */
    public boolean addMemberAndSync(Sect sect, UUID playerId, String playerName) {
        try {
            // 第一步：添加到sect
            boolean added = sect.addMember(playerId, playerName);
            if (!added) {
                logger.warning("无法添加成员到宗门: " + playerName);
                return false;
            }

            // 第二步：同步职位到PlayerData（新成员默认为外门弟子）
            syncMemberRankToPlayerData(playerId, sect);

            logger.info("成员添加成功: " + playerName + " 加入宗门 " + sect.getName());
            return true;
        } catch (Exception e) {
            logger.warning("添加成员失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除成员从宗门 - 同步版本
     * 确保成员被从sect和PlayerData中正确移除
     *
     * @param sect 宗门对象
     * @param playerId 玩家UUID
     * @return 是否移除成功
     */
    public boolean removeMemberAndSync(Sect sect, UUID playerId) {
        try {
            SectMember member = sect.getMember(playerId);
            if (member == null) {
                logger.warning("无法移除成员: 成员不在宗门中");
                return false;
            }

            String playerName = member.getPlayerName();

            // 第一步：从sect移除
            boolean removed = sect.removeMember(playerId);
            if (!removed) {
                logger.warning("无法从sect中移除成员: " + playerName);
                return false;
            }

            // 第二步：从PlayerData中移除职位关联
            PlayerData playerData = plugin.getDataManager().loadPlayerData(playerId);
            if (playerData != null) {
                playerData.setSectRank("member");  // 重置为默认值
                playerData.setSectId(null);        // 清除宗门ID
                plugin.getDataManager().savePlayerData(playerData);
            }

            logger.info("成员移除成功: " + playerName + " 已离开宗门 " + sect.getName());
            return true;
        } catch (Exception e) {
            logger.warning("移除成员失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 全量同步检查
     * 对宗门中的所有成员进行一致性检查和修复
     * 建议定期运行（例如每小时一次）
     *
     * @param sect 宗门对象
     * @return 发现的不一致数量
     */
    public int fullSyncCheck(Sect sect) {
        int inconsistencyCount = 0;

        try {
            logger.info("开始进行宗门数据一致性检查: " + sect.getName());

            for (SectMember member : sect.getMemberList()) {
                UUID playerId = member.getPlayerId();

                if (!verifyRankConsistency(playerId, sect)) {
                    inconsistencyCount++;
                    logger.warning("发现职位不一致: " + member.getPlayerName() +
                            " (ID: " + playerId + ")");

                    // 自动修复
                    if (!fixRankInconsistency(playerId, sect)) {
                        logger.warning("职位修复失败: " + member.getPlayerName());
                    }
                }
            }

            logger.info("宗门数据一致性检查完成: " + sect.getName() +
                    " (发现 " + inconsistencyCount + " 个不一致)");

            return inconsistencyCount;
        } catch (Exception e) {
            logger.warning("全量同步检查失败: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 统计同步状态
     * 返回一份同步状态报告
     *
     * @param sect 宗门对象
     * @return 同步状态报告字符串
     */
    public String getSyncStatusReport(Sect sect) {
        StringBuilder report = new StringBuilder();
        report.append("========== 宗门数据同步状态 ==========\n");
        report.append("宗门名称: ").append(sect.getName()).append("\n");
        report.append("总成员数: ").append(sect.getMemberList().size()).append("\n");

        int consistentCount = 0;
        int inconsistentCount = 0;

        for (SectMember member : sect.getMemberList()) {
            if (verifyRankConsistency(member.getPlayerId(), sect)) {
                consistentCount++;
            } else {
                inconsistentCount++;
            }
        }

        report.append("数据一致: ").append(consistentCount).append("\n");
        report.append("数据不一致: ").append(inconsistentCount).append("\n");

        if (inconsistentCount > 0) {
            report.append("\n需要修复的成员:\n");
            for (SectMember member : sect.getMemberList()) {
                if (!verifyRankConsistency(member.getPlayerId(), sect)) {
                    report.append("  - ").append(member.getPlayerName())
                            .append(" (").append(member.getPlayerId()).append(")\n");
                }
            }
        }

        report.append("=====================================");
        return report.toString();
    }
}
