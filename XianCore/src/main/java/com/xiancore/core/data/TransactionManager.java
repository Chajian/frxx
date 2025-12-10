package com.xiancore.core.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * 事务管理器
 * 提供统一的事务执行模板，确保原子性
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TransactionManager {

    private final DatabaseManager databaseManager;
    private final Logger logger;

    public TransactionManager(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }

    /**
     * 在事务中执行操作（有返回值）
     *
     * @param callback 事务回调
     * @param <T>      返回值类型
     * @return 操作结果
     * @throws DataAccessException 如果事务执行失败
     */
    public <T> T executeInTransaction(TransactionCallback<T> callback) {
        if (!databaseManager.isUseMySql()) {
            throw new IllegalStateException("事务仅支持 MySQL 模式");
        }

        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            T result = callback.execute(conn);

            conn.commit();
            return result;

        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new DataAccessException("事务执行失败", e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * 在事务中执行操作（无返回值）
     *
     * @param callback 事务回调
     * @throws DataAccessException 如果事务执行失败
     */
    public void executeInTransactionVoid(TransactionVoidCallback callback) {
        executeInTransaction(conn -> {
            callback.execute(conn);
            return null;
        });
    }

    /**
     * 静默回滚事务
     */
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                logger.warning("§c事务已回滚");
            } catch (SQLException e) {
                logger.severe("§c事务回滚失败: " + e.getMessage());
            }
        }
    }

    /**
     * 静默关闭连接
     */
    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * 事务回调接口（有返回值）
     */
    @FunctionalInterface
    public interface TransactionCallback<T> {
        T execute(Connection conn) throws SQLException;
    }

    /**
     * 事务回调接口（无返回值）
     */
    @FunctionalInterface
    public interface TransactionVoidCallback {
        void execute(Connection conn) throws SQLException;
    }
}
