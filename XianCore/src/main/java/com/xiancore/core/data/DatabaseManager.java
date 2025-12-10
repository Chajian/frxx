package com.xiancore.core.data;

import com.xiancore.XianCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接管理器
 * 负责 HikariCP 连接池的初始化、获取连接、关闭
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class DatabaseManager {

    private final XianCore plugin;
    private HikariDataSource dataSource;

    @Getter
    private boolean useMySql = false;

    public DatabaseManager(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化数据库连接
     */
    public void initialize() {
        FileConfiguration config = plugin.getConfig();
        useMySql = config.getBoolean("database.use-mysql", false);

        if (useMySql) {
            setupHikariCP(config);
        } else {
            plugin.getLogger().info("使用本地文件存储（YAML）");
        }
    }

    /**
     * 设置 HikariCP 连接池
     */
    private void setupHikariCP(FileConfiguration config) {
        plugin.getLogger().info("正在连接到 MySQL 数据库...");

        String host = config.getString("database.host", "localhost");
        int port = config.getInt("database.port", 3306);
        String database = config.getString("database.database", "xiancore");
        String username = config.getString("database.username", "root");
        String password = config.getString("database.password", "password");

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
                host, port, database
        );

        plugin.getLogger().info("§e数据库配置:");
        plugin.getLogger().info("§e  主机: " + host + ":" + port);
        plugin.getLogger().info("§e  数据库: " + database);
        plugin.getLogger().info("§e  用户: " + username);
        plugin.getLogger().info("§e  URL: " + jdbcUrl);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        // 连接池配置
        hikariConfig.setMaximumPoolSize(config.getInt("database.pool-size", 10));
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        // 性能优化
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("§a✓ MySQL 数据库连接成功!");
        } catch (Exception e) {
            plugin.getLogger().severe("§c✗ MySQL 数据库连接失败!");
            e.printStackTrace();
            plugin.getLogger().warning("§e切换到本地文件存储（YAML）");
            useMySql = false;
        }
    }

    /**
     * 获取数据库连接
     *
     * @return 数据库连接
     * @throws SQLException 如果获取连接失败
     */
    public Connection getConnection() throws SQLException {
        if (!useMySql || dataSource == null) {
            throw new SQLException("MySQL 未启用或连接池未初始化");
        }
        return dataSource.getConnection();
    }

    /**
     * 检查数据库连接是否可用
     *
     * @return 是否可用
     */
    public boolean isAvailable() {
        if (!useMySql || dataSource == null) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭数据库连接池
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("§a数据库连接池已关闭");
        }
    }
}
