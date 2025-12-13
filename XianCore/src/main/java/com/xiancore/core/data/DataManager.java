package com.xiancore.core.data;

import com.xiancore.XianCore;
import com.xiancore.core.data.repository.*;
import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.facilities.SectFacilityData;
import com.xiancore.systems.sect.warehouse.SectWarehouse;
import com.xiancore.systems.tribulation.Tribulation;
import lombok.Getter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据管理器 (Facade 模式)
 * 作为数据层的统一入口，协调各个 Repository
 * <p>
 * 重构说明：
 * - 原有 2000+ 行代码已拆分为多个职责单一的类
 * - DatabaseManager: 连接池管理
 * - SchemaManager: 表结构和迁移
 * - TransactionManager: 事务管理
 * - PlayerRepository: 玩家数据 CRUD
 * - SectRepository: 宗门数据 CRUD
 * - TribulationRepository: 天劫数据 CRUD
 * - WarehouseRepository: 仓库数据 CRUD
 * - FacilityRepository: 设施数据 CRUD
 * <p>
 * 此类保持原有的公开 API 不变，确保向后兼容
 *
 * @author Olivia Diaz
 * @version 2.0.0
 */
@Getter
public class DataManager {

    private final XianCore plugin;

    // ==================== 基础设施层 ====================
    private final DatabaseManager databaseManager;
    private final SchemaManager schemaManager;
    private final TransactionManager transactionManager;

    // ==================== 仓储层 ====================
    private final PlayerRepository playerRepository;
    private final SectRepository sectRepository;
    private final TribulationRepository tribulationRepository;
    private final WarehouseRepository warehouseRepository;
    private final FacilityRepository facilityRepository;

    public DataManager(XianCore plugin) {
        this.plugin = plugin;

        // 初始化基础设施
        this.databaseManager = new DatabaseManager(plugin);
        this.schemaManager = new SchemaManager(plugin, databaseManager);
        this.transactionManager = new TransactionManager(databaseManager, plugin.getLogger());

        // 初始化仓储
        this.playerRepository = new PlayerRepository(plugin, databaseManager);
        this.sectRepository = new SectRepository(plugin, databaseManager);
        this.tribulationRepository = new TribulationRepository(plugin, databaseManager);
        this.warehouseRepository = new WarehouseRepository(plugin, databaseManager);
        this.facilityRepository = new FacilityRepository(plugin, databaseManager);
    }

    /**
     * 初始化数据管理器
     */
    public void initialize() {
        plugin.getLogger().info("正在初始化数据管理器...");

        // 1. 初始化数据库连接
        databaseManager.initialize();

        // 2. 创建/迁移表结构
        schemaManager.initialize();

        plugin.getLogger().info("§a数据管理器初始化完成!");
    }

    // ==================== 玩家数据 API（保持原有接口不变） ====================

    /**
     * 加载玩家数据
     *
     * @param uuid 玩家 UUID
     * @return 玩家数据
     */
    public PlayerData loadPlayerData(UUID uuid) {
        return playerRepository.load(uuid);
    }

    /**
     * 创建新玩家数据
     *
     * @param uuid 玩家 UUID
     * @return 新创建的玩家数据
     */
    public PlayerData createPlayerData(UUID uuid) {
        return playerRepository.create(uuid);
    }

    /**
     * 保存玩家数据
     *
     * @param data 玩家数据
     */
    public void savePlayerData(PlayerData data) {
        playerRepository.save(data);
    }

    /**
     * 移除玩家数据缓存
     *
     * @param uuid 玩家 UUID
     */
    public void removePlayerData(UUID uuid) {
        playerRepository.evict(uuid);
    }

    /**
     * 获取玩家数据缓存（用于兼容）
     *
     * @return 玩家数据缓存 Map
     */
    public Map<UUID, PlayerData> getPlayerDataCache() {
        return playerRepository.getCache();
    }

    // ==================== 宗门数据 API（保持原有接口不变） ====================

    /**
     * 保存宗门数据
     *
     * @param sect 宗门
     */
    public void saveSect(Sect sect) {
        sectRepository.save(sect);
    }

    /**
     * 加载所有宗门数据
     *
     * @return 宗门列表
     */
    public List<Sect> loadAllSects() {
        return sectRepository.loadAll();
    }

    /**
     * 删除宗门数据
     *
     * @param sectId 宗门 ID
     */
    public void deleteSect(int sectId) {
        sectRepository.delete(sectId);
    }

    // ==================== 仓库/设施 API（保持原有接口不变） ====================

    /**
     * 保存仓库数据到文件
     *
     * @param warehouse 仓库
     */
    public void saveWarehouseToFile(SectWarehouse warehouse) {
        warehouseRepository.save(warehouse);
    }

    /**
     * 从文件加载仓库数据
     *
     * @param sectId 宗门 ID
     * @return 仓库数据
     */
    public SectWarehouse loadWarehouseFromFile(int sectId) {
        return warehouseRepository.load(sectId);
    }

    /**
     * 保存设施数据到文件
     *
     * @param data 设施数据
     */
    public void saveFacilityDataToFile(SectFacilityData data) {
        facilityRepository.save(data);
    }

    /**
     * 从文件加载设施数据
     *
     * @param sectId 宗门 ID
     * @return 设施数据
     */
    public SectFacilityData loadFacilityDataFromFile(int sectId) {
        return facilityRepository.load(sectId);
    }

    // ==================== 天劫数据 API（保持原有接口不变） ====================

    /**
     * 保存天劫数据
     *
     * @param tribulation 天劫
     */
    public void saveTribulation(Tribulation tribulation) {
        tribulationRepository.save(tribulation);
    }

    /**
     * 加载玩家的活跃天劫
     *
     * @param playerId 玩家 UUID
     * @return 活跃的天劫
     */
    public Tribulation loadActiveTribulation(UUID playerId) {
        return tribulationRepository.loadActive(playerId);
    }

    /**
     * 删除天劫数据
     *
     * @param tribulationId 天劫 UUID
     */
    public void deleteTribulation(UUID tribulationId) {
        tribulationRepository.delete(tribulationId);
    }

    // ==================== 事务 API（保持原有接口不变） ====================

    /**
     * 原子性保存玩家数据和宗门数据
     * 使用数据库事务确保两个操作要么都成功，要么都失败
     *
     * @param playerData 玩家数据
     * @param sect       宗门数据
     * @throws RuntimeException 如果保存失败，事务会回滚
     */
    public void savePlayerAndSectAtomic(PlayerData playerData, Sect sect) {
        if (!databaseManager.isUseMySql()) {
            // 降级为普通保存
            savePlayerData(playerData);
            saveSect(sect);
            return;
        }

        transactionManager.executeInTransactionVoid(conn -> {
            playerRepository.saveWithConnection(conn, playerData);
            sectRepository.saveWithConnection(conn, sect);
        });

        plugin.getLogger().fine(String.format("§a事务提交成功: 玩家=%s, 宗门=%s",
                playerData.getUuid(), sect.getName()));
    }

    // ==================== 工具方法（保持原有接口不变） ====================

    /**
     * 保存所有数据
     */
    public void saveAll() {
        playerRepository.saveAll();

        if (plugin.getSectSystem() != null) {
            plugin.getSectSystem().saveAll();
        }
    }

    /**
     * 关闭数据管理器
     */
    public void shutdown() {
        saveAll();
        databaseManager.shutdown();
    }

    /**
     * 获取数据库连接
     *
     * @return 数据库连接
     * @throws SQLException SQL 异常
     */
    public Connection getConnection() throws SQLException {
        return databaseManager.getConnection();
    }

    /**
     * 检查是否使用 MySQL
     *
     * @return 是否使用 MySQL
     */
    public boolean isUsingMySql() {
        return databaseManager.isUseMySql();
    }

    /**
     * 检查 MySQL 连接是否可用
     *
     * @return 是否可用
     */
    public boolean isMySqlAvailable() {
        return databaseManager.isAvailable();
    }

    // ==================== 兼容性方法（保留原有字段访问方式） ====================

    /**
     * 检查是否使用 MySQL（兼容原有字段访问）
     *
     * @return 是否使用 MySQL
     */
    public boolean isUseMySql() {
        return databaseManager.isUseMySql();
    }
}
