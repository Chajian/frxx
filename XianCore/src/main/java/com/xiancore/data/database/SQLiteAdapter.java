package com.xiancore.data.database;

import org.bukkit.plugin.Plugin;
import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * SQLite 数据库适配器
 * 本地 SQLite 数据库实现
 *
 * @author XianCore
 * @version 1.0
 */
public class SQLiteAdapter implements IDatabaseAdapter {

    private final Plugin plugin;
    private final Logger logger;
    private final String databasePath;
    private Connection connection;

    /**
     * 构造函数
     */
    public SQLiteAdapter(Plugin plugin, String databasePath) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.databasePath = databasePath;
        this.connection = null;
    }

    /**
     * 连接数据库
     */
    @Override
    public boolean connect() {
        try {
            // 确保文件夹存在
            File dbFolder = new File(databasePath).getParentFile();
            if (!dbFolder.exists()) {
                dbFolder.mkdirs();
            }

            // 加载 JDBC 驱动
            Class.forName("org.sqlite.JDBC");

            // 连接数据库
            String url = "jdbc:sqlite:" + databasePath;
            connection = DriverManager.getConnection(url);

            logger.info("✓ SQLite 数据库连接成功: " + databasePath);
            return true;

        } catch (ClassNotFoundException e) {
            logger.severe("✗ SQLite JDBC 驱动不存在: " + e.getMessage());
            return false;
        } catch (SQLException e) {
            logger.severe("✗ 数据库连接失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 断开连接
     */
    @Override
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("✓ SQLite 数据库连接已关闭");
            }
        } catch (SQLException e) {
            logger.warning("断开数据库连接异常: " + e.getMessage());
        }
    }

    /**
     * 检查连接状态
     */
    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 执行查询
     */
    @Override
    public List<Map<String, Object>> query(String sql) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (!isConnected()) {
            logger.warning("✗ 数据库未连接");
            return results;
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

            logger.info("✓ 查询成功: 返回 " + results.size() + " 行数据");

        } catch (SQLException e) {
            logger.severe("✗ 执行查询异常: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * 执行更新/插入/删除
     */
    @Override
    public boolean execute(String sql) {
        if (!isConnected()) {
            logger.warning("✗ 数据库未连接");
            return false;
        }

        try (Statement statement = connection.createStatement()) {
            int rowsAffected = statement.executeUpdate(sql);
            logger.info("✓ 执行成功: 影响 " + rowsAffected + " 行");
            return true;

        } catch (SQLException e) {
            logger.severe("✗ 执行异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 执行预处理语句（查询）
     */
    @Override
    public List<Map<String, Object>> preparedQuery(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (!isConnected()) {
            logger.warning("✗ 数据库未连接");
            return results;
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // 设置参数
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }

            logger.info("✓ 预处理查询成功: 返回 " + results.size() + " 行数据");

        } catch (SQLException e) {
            logger.severe("✗ 预处理查询异常: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * 执行预处理语句（更新）
     */
    @Override
    public boolean preparedExecute(String sql, Object... params) {
        if (!isConnected()) {
            logger.warning("✗ 数据库未连接");
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // 设置参数
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            int rowsAffected = statement.executeUpdate();
            logger.info("✓ 预处理执行成功: 影响 " + rowsAffected + " 行");
            return true;

        } catch (SQLException e) {
            logger.severe("✗ 预处理执行异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 执行事务
     */
    @Override
    public boolean executeTransaction(List<String> sqlList) {
        if (!isConnected()) {
            logger.warning("✗ 数据库未连接");
            return false;
        }

        try {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlList) {
                    statement.executeUpdate(sql);
                }
            }

            connection.commit();
            connection.setAutoCommit(true);
            logger.info("✓ 事务执行成功: " + sqlList.size() + " 个语句");
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackError) {
                logger.severe("回滚事务失败: " + rollbackError.getMessage());
            }
            logger.severe("✗ 事务执行失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 创建表
     */
    @Override
    public boolean createTable(String sql) {
        return execute(sql);
    }

    /**
     * 删除表
     */
    @Override
    public boolean dropTable(String tableName) {
        return execute("DROP TABLE IF EXISTS " + tableName);
    }

    /**
     * 获取数据库类型
     */
    @Override
    public String getDatabaseType() {
        return "SQLite";
    }

    /**
     * 获取连接信息
     */
    @Override
    public String getConnectionInfo() {
        return "SQLite - " + databasePath;
    }

    /**
     * 创建备份
     */
    public boolean createBackup(String backupPath) {
        try {
            File sourceFile = new File(databasePath);
            File backupFile = new File(backupPath);

            if (!sourceFile.exists()) {
                logger.warning("✗ 源数据库文件不存在");
                return false;
            }

            // 简单的文件复制备份
            byte[] buffer = new byte[1024];
            int bytesRead;

            try (java.io.FileInputStream fis = new java.io.FileInputStream(sourceFile);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(backupFile)) {
                while ((bytesRead = fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            logger.info("✓ 数据库备份成功: " + backupPath);
            return true;

        } catch (Exception e) {
            logger.severe("✗ 创建备份失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取表列表
     */
    public List<String> listTables() {
        List<String> tables = new ArrayList<>();

        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, "%", new String[]{"TABLE"});

            while (resultSet.next()) {
                tables.add(resultSet.getString("TABLE_NAME"));
            }
            resultSet.close();

            logger.info("✓ 获取表列表成功: " + tables.size() + " 个表");

        } catch (SQLException e) {
            logger.warning("获取表列表失败: " + e.getMessage());
        }

        return tables;
    }

    /**
     * 获取数据库统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("type", getDatabaseType());
            stats.put("path", databasePath);
            stats.put("connected", isConnected());
            stats.put("tables", listTables());

            File dbFile = new File(databasePath);
            if (dbFile.exists()) {
                stats.put("size", dbFile.length());
                stats.put("lastModified", new java.util.Date(dbFile.lastModified()));
            }

            logger.info("✓ 获取数据库统计信息成功");

        } catch (Exception e) {
            logger.warning("获取统计信息失败: " + e.getMessage());
        }

        return stats;
    }
}
