package com.yourserver.xiancore.manager;

import com.yourserver.xiancore.XianCoreAddon;
import com.yourserver.xiancore.model.AttributeBuff;
import com.yourserver.xiancore.model.ItemUsage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 数据库管理器
 */
public class DatabaseManager {
    
    private final XianCoreAddon plugin;
    private HikariDataSource dataSource;
    private final String dbType;
    
    public DatabaseManager(XianCoreAddon plugin) {
        this.plugin = plugin;
        this.dbType = plugin.getConfig().getString("database.type", "sqlite");
    }
    
    /**
     * 初始化数据库
     */
    public void initialize() {
        try {
            setupDataSource();
            createTables();
            plugin.getLogger().info("数据库初始化成功！类型: " + dbType);
        } catch (Exception e) {
            plugin.getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 设置数据源
     */
    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        
        if (dbType.equalsIgnoreCase("mysql")) {
            // MySQL 配置
            String host = plugin.getConfig().getString("database.mysql.host");
            int port = plugin.getConfig().getInt("database.mysql.port");
            String database = plugin.getConfig().getString("database.mysql.database");
            String username = plugin.getConfig().getString("database.mysql.username");
            String password = plugin.getConfig().getString("database.mysql.password");
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                            "?useSSL=false&serverTimezone=UTC&characterEncoding=utf8");
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.mysql.pool-size", 10));
            
        } else {
            // SQLite 配置
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            String dbFile = plugin.getConfig().getString("database.sqlite.file", "database.db");
            File databaseFile = new File(dataFolder, dbFile);
            
            config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            config.setMaximumPoolSize(1); // SQLite 不支持多连接
        }
        
        config.setConnectionTimeout(plugin.getConfig().getLong("database.mysql.connection-timeout", 10000));
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
    }
    
    /**
     * 创建表
     */
    private void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // 创建道具使用记录表
            String createItemUsageTable = 
                "CREATE TABLE IF NOT EXISTS item_usage (" +
                "uuid VARCHAR(36) NOT NULL," +
                "item_id VARCHAR(50) NOT NULL," +
                "usage_count INTEGER DEFAULT 0," +
                "max_usage INTEGER NOT NULL," +
                "first_used INTEGER," +
                "last_used INTEGER," +
                "PRIMARY KEY (uuid, item_id)" +
                ")";
            
            // 创建临时Buff表
            String createBuffsTable = 
                "CREATE TABLE IF NOT EXISTS active_buffs (" +
                "id INTEGER PRIMARY KEY " + (dbType.equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + "," +
                "uuid VARCHAR(36) NOT NULL," +
                "buff_type VARCHAR(50) NOT NULL," +
                "attribute_name VARCHAR(50) NOT NULL," +
                "bonus_value REAL NOT NULL," +
                "start_time INTEGER NOT NULL," +
                "expire_time INTEGER NOT NULL" +
                ")";
            
            // 创建操作日志表
            String createLogsTable = 
                "CREATE TABLE IF NOT EXISTS operation_logs (" +
                "id INTEGER PRIMARY KEY " + (dbType.equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + "," +
                "uuid VARCHAR(36) NOT NULL," +
                "player_name VARCHAR(16)," +
                "operation_type VARCHAR(20) NOT NULL," +
                "item_id VARCHAR(50)," +
                "old_value REAL," +
                "new_value REAL," +
                "details TEXT," +
                "timestamp INTEGER NOT NULL" +
                ")";
            
            Statement stmt = conn.createStatement();
            stmt.execute(createItemUsageTable);
            stmt.execute(createBuffsTable);
            stmt.execute(createLogsTable);
            
            // 创建索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_buffs_uuid ON active_buffs(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_buffs_expire ON active_buffs(expire_time)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_uuid ON operation_logs(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_time ON operation_logs(timestamp)");
            
            stmt.close();
            
            plugin.getLogger().info("数据库表创建成功！");
        }
    }
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * 保存道具使用记录
     */
    public void saveItemUsage(ItemUsage usage) {
        String sql = "INSERT OR REPLACE INTO item_usage " +
                    "(uuid, item_id, usage_count, max_usage, first_used, last_used) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        // MySQL 使用不同的语法
        if (dbType.equalsIgnoreCase("mysql")) {
            sql = "INSERT INTO item_usage " +
                  "(uuid, item_id, usage_count, max_usage, first_used, last_used) " +
                  "VALUES (?, ?, ?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE " +
                  "usage_count = VALUES(usage_count), " +
                  "max_usage = VALUES(max_usage), " +
                  "first_used = VALUES(first_used), " +
                  "last_used = VALUES(last_used)";
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usage.getUuid().toString());
            stmt.setString(2, usage.getItemId());
            stmt.setInt(3, usage.getUsageCount());
            stmt.setInt(4, usage.getMaxUsage());
            stmt.setLong(5, usage.getFirstUsed());
            stmt.setLong(6, usage.getLastUsed());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("保存道具使用记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载道具使用记录
     */
    public ItemUsage loadItemUsage(UUID uuid, String itemId) {
        String sql = "SELECT * FROM item_usage WHERE uuid = ? AND item_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, itemId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ItemUsage(
                    uuid,
                    itemId,
                    rs.getInt("usage_count"),
                    rs.getInt("max_usage"),
                    rs.getLong("first_used"),
                    rs.getLong("last_used")
                );
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("加载道具使用记录失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 删除道具使用记录
     */
    public void deleteItemUsage(UUID uuid, String itemId) {
        String sql = "DELETE FROM item_usage WHERE uuid = ? AND item_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            stmt.setString(2, itemId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("删除道具使用记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存Buff
     */
    public void saveBuff(AttributeBuff buff) {
        String sql = "INSERT INTO active_buffs " +
                    "(uuid, buff_type, attribute_name, bonus_value, start_time, expire_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, buff.getUuid().toString());
            stmt.setString(2, buff.getBuffType());
            stmt.setString(3, buff.getAttributeName());
            stmt.setDouble(4, buff.getBonusValue());
            stmt.setLong(5, buff.getStartTime());
            stmt.setLong(6, buff.getExpireTime());
            
            stmt.executeUpdate();
            
            // 获取生成的ID
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                buff.setId(rs.getInt(1));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("保存Buff失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载玩家所有Buff
     */
    public List<AttributeBuff> loadBuffs(UUID uuid) {
        List<AttributeBuff> buffs = new ArrayList<>();
        String sql = "SELECT * FROM active_buffs WHERE uuid = ? AND expire_time > ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, System.currentTimeMillis() / 1000);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                buffs.add(new AttributeBuff(
                    rs.getInt("id"),
                    uuid,
                    rs.getString("buff_type"),
                    rs.getString("attribute_name"),
                    rs.getDouble("bonus_value"),
                    rs.getLong("start_time"),
                    rs.getLong("expire_time")
                ));
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("加载Buff失败: " + e.getMessage());
        }
        
        return buffs;
    }
    
    /**
     * 删除Buff
     */
    public void deleteBuff(int buffId) {
        String sql = "DELETE FROM active_buffs WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, buffId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("删除Buff失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期Buff
     */
    public void cleanupExpiredBuffs() {
        String sql = "DELETE FROM active_buffs WHERE expire_time < ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis() / 1000);
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                plugin.getLogger().info("清理了 " + deleted + " 个过期Buff");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("清理过期Buff失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录操作日志
     */
    public void logOperation(UUID uuid, String playerName, String operationType, 
                            String itemId, double oldValue, double newValue, String details) {
        if (!plugin.getConfig().getBoolean("features.audit-log", true)) {
            return;
        }
        
        String sql = "INSERT INTO operation_logs " +
                    "(uuid, player_name, operation_type, item_id, old_value, new_value, details, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, playerName);
                stmt.setString(3, operationType);
                stmt.setString(4, itemId);
                stmt.setDouble(5, oldValue);
                stmt.setDouble(6, newValue);
                stmt.setString(7, details);
                stmt.setLong(8, System.currentTimeMillis() / 1000);
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().severe("记录操作日志失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 备份数据库
     */
    public void backupDatabase() {
        if (!plugin.getConfig().getBoolean("backup.enabled", true)) {
            return;
        }
        
        if (!dbType.equalsIgnoreCase("sqlite")) {
            plugin.getLogger().warning("只有SQLite支持自动备份");
            return;
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File dataFolder = plugin.getDataFolder();
                String dbFile = plugin.getConfig().getString("database.sqlite.file", "database.db");
                File sourceFile = new File(dataFolder, dbFile);
                
                if (!sourceFile.exists()) {
                    return;
                }
                
                // 创建备份目录
                String backupPath = plugin.getConfig().getString("backup.path", "backups/");
                File backupFolder = new File(dataFolder, backupPath);
                if (!backupFolder.exists()) {
                    backupFolder.mkdirs();
                }
                
                // 生成备份文件名
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String timestamp = sdf.format(new Date());
                File backupFile = new File(backupFolder, "database_" + timestamp + ".db");
                
                // 复制文件
                Files.copy(sourceFile.toPath(), backupFile.toPath(), 
                          StandardCopyOption.REPLACE_EXISTING);
                
                plugin.getLogger().info("数据库备份成功: " + backupFile.getName());
                
                // 清理旧备份
                cleanupOldBackups(backupFolder);
                
            } catch (IOException e) {
                plugin.getLogger().severe("数据库备份失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 清理旧备份
     */
    private void cleanupOldBackups(File backupFolder) {
        int keepDays = plugin.getConfig().getInt("backup.keep-days", 7);
        long cutoffTime = System.currentTimeMillis() - (keepDays * 24L * 60 * 60 * 1000);
        
        File[] files = backupFolder.listFiles((dir, name) -> name.startsWith("database_") && name.endsWith(".db"));
        if (files != null) {
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        plugin.getLogger().info("删除旧备份: " + file.getName());
                    }
                }
            }
        }
    }
    
    /**
     * 优化数据库
     */
    public void optimizeDatabase() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                
                if (dbType.equalsIgnoreCase("sqlite")) {
                    stmt.execute("VACUUM");
                    stmt.execute("ANALYZE");
                } else {
                    stmt.execute("OPTIMIZE TABLE item_usage, active_buffs, operation_logs");
                }
                
                plugin.getLogger().info("数据库优化完成！");
                
            } catch (SQLException e) {
                plugin.getLogger().severe("数据库优化失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 关闭数据库连接
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("数据库连接已关闭");
        }
    }
}

