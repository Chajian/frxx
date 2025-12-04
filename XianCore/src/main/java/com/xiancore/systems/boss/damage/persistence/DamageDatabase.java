package com.xiancore.systems.boss.damage.persistence;

import com.xiancore.systems.boss.damage.DamageHistory;
import com.xiancore.systems.boss.damage.DamageStatistics;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 伤害数据库接口
 * 定义伤害数据的持久化操作
 *
 * 支持多种实现:
 * - 内存存储 (速度快, 重启丢失)
 * - 文件存储 (YAML/JSON, 便于编辑)
 * - SQL数据库 (大规模数据)
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public interface DamageDatabase {

    // ==================== 初始化和关闭 ====================

    /**
     * 初始化数据库连接和表结构
     */
    void initialize() throws Exception;

    /**
     * 关闭数据库连接
     */
    void shutdown() throws Exception;

    /**
     * 是否已连接
     *
     * @return 连接状态
     */
    boolean isConnected();

    // ==================== 保存操作 ====================

    /**
     * 保存伤害历史记录
     *
     * @param history 伤害历史
     * @return 是否保存成功
     */
    boolean saveHistory(DamageHistory history);

    /**
     * 批量保存伤害历史记录
     *
     * @param histories 历史记录列表
     * @return 成功保存的数量
     */
    int saveHistories(List<DamageHistory> histories);

    /**
     * 异步保存伤害历史 (非阻塞)
     *
     * @param history 伤害历史
     * @return 异步操作结果
     */
    CompletableFuture<Boolean> saveHistoryAsync(DamageHistory history);

    /**
     * 更新伤害历史记录
     *
     * @param history 伤害历史
     * @return 是否更新成功
     */
    boolean updateHistory(DamageHistory history);

    // ==================== 查询操作 ====================

    /**
     * 查询特定Boss的所有历史记录
     *
     * @param bossUUID Boss UUID
     * @return 历史记录列表
     */
    List<DamageHistory> queryByBossUUID(UUID bossUUID);

    /**
     * 查询特定玩家的所有历史记录
     *
     * @param playerUUID 玩家UUID
     * @return 历史记录列表
     */
    List<DamageHistory> queryByPlayerUUID(UUID playerUUID);

    /**
     * 查询时间范围内的历史记录
     *
     * @param startTime 开始时间 (毫秒)
     * @param endTime 结束时间 (毫秒)
     * @return 历史记录列表
     */
    List<DamageHistory> queryByTimeRange(long startTime, long endTime);

    /**
     * 查询所有历史记录
     *
     * @return 所有历史记录列表
     */
    List<DamageHistory> queryAll();

    /**
     * 查询所有历史记录 (分页)
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 分页结果
     */
    List<DamageHistory> queryAll(int offset, int limit);

    /**
     * 查询指定ID的历史记录
     *
     * @param id 记录ID
     * @return 历史记录，不存在则返回null
     */
    DamageHistory queryById(String id);

    /**
     * 获取历史记录总数
     *
     * @return 记录总数
     */
    long getTotalCount();

    /**
     * 获取特定Boss的历史记录数
     *
     * @param bossUUID Boss UUID
     * @return 记录数
     */
    long getCountByBossUUID(UUID bossUUID);

    // ==================== 统计操作 ====================

    /**
     * 获取玩家的总伤害统计
     *
     * @param playerUUID 玩家UUID
     * @return 总伤害值
     */
    double getTotalDamageForPlayer(UUID playerUUID);

    /**
     * 获取玩家的排行榜 (按总伤害)
     *
     * @param limit 前N名
     * @return 玩家UUID -> 总伤害的映射
     */
    Map<UUID, Double> getTopDamagers(int limit);

    /**
     * 获取Boss的杀死次数
     *
     * @param bossUUID Boss UUID
     * @return 杀死次数
     */
    int getBossKillCount(UUID bossUUID);

    /**
     * 获取平均伤害统计
     *
     * @return 平均伤害值
     */
    double getAverageDamage();

    // ==================== 删除操作 ====================

    /**
     * 删除特定Boss的所有历史
     *
     * @param bossUUID Boss UUID
     * @return 删除的记录数
     */
    int deleteByBossUUID(UUID bossUUID);

    /**
     * 删除特定玩家的所有历史
     *
     * @param playerUUID 玩家UUID
     * @return 删除的记录数
     */
    int deleteByPlayerUUID(UUID playerUUID);

    /**
     * 删除指定时间前的所有历史
     *
     * @param beforeTime 时间戳 (毫秒)
     * @return 删除的记录数
     */
    int deleteBeforeTime(long beforeTime);

    /**
     * 删除指定ID的历史记录
     *
     * @param id 记录ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);

    /**
     * 清空所有数据
     */
    void clearAll();

    // ==================== 导入导出 ====================

    /**
     * 导出所有数据为JSON字符串
     *
     * @return JSON数据
     */
    String exportAsJson();

    /**
     * 从JSON字符串导入数据
     *
     * @param json JSON数据
     * @return 导入的记录数
     */
    int importFromJson(String json);

    /**
     * 导出所有数据为YAML字符串
     *
     * @return YAML数据
     */
    String exportAsYaml();

    /**
     * 从YAML字符串导入数据
     *
     * @param yaml YAML数据
     * @return 导入的记录数
     */
    int importFromYaml(String yaml);

    // ==================== 性能和优化 ====================

    /**
     * 清理数据库 (删除过期数据)
     *
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     */
    int cleanup(int daysToKeep);

    /**
     * 优化数据库 (索引重建等)
     */
    void optimize();

    /**
     * 获取数据库统计信息
     *
     * @return 统计信息 (JSON格式)
     */
    String getStatistics();

    /**
     * 备份数据库
     *
     * @param backupPath 备份路径
     * @return 是否备份成功
     */
    boolean backup(String backupPath);

    /**
     * 从备份恢复数据库
     *
     * @param backupPath 备份路径
     * @return 是否恢复成功
     */
    boolean restore(String backupPath);
}
