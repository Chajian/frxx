package com.xiancore.data.database;

import org.bukkit.plugin.Plugin;
import java.util.List;
import java.util.Map;

/**
 * 数据库适配器接口
 * 定义数据库操作的标准接口
 *
 * @author XianCore
 * @version 1.0
 */
public interface IDatabaseAdapter {

    /**
     * 连接数据库
     */
    boolean connect();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 检查连接状态
     */
    boolean isConnected();

    /**
     * 执行 SQL 查询
     */
    List<Map<String, Object>> query(String sql);

    /**
     * 执行 SQL 更新/插入/删除
     */
    boolean execute(String sql);

    /**
     * 执行预处理语句（查询）
     */
    List<Map<String, Object>> preparedQuery(String sql, Object... params);

    /**
     * 执行预处理语句（更新）
     */
    boolean preparedExecute(String sql, Object... params);

    /**
     * 执行事务
     */
    boolean executeTransaction(List<String> sqlList);

    /**
     * 创建表
     */
    boolean createTable(String sql);

    /**
     * 删除表
     */
    boolean dropTable(String tableName);

    /**
     * 获取数据库类型
     */
    String getDatabaseType();

    /**
     * 获取连接信息
     */
    String getConnectionInfo();
}
