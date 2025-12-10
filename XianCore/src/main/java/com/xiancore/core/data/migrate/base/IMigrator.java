package com.xiancore.core.data.migrate.base;

import com.xiancore.core.data.migrate.MigrationReport;

/**
 * 数据迁移器接口
 * 所有具体的迁移器都需要实现此接口
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface IMigrator {
    
    /**
     * 获取迁移器名称
     * @return 迁移器名称（用于日志和报告）
     */
    String getName();
    
    /**
     * 获取迁移器描述
     * @return 迁移器描述
     */
    String getDescription();
    
    /**
     * 检查是否有数据需要迁移
     * @return 如果有数据需要迁移返回 true
     */
    boolean hasDataToMigrate();
    
    /**
     * 获取迁移前摘要信息
     * @return 格式化的摘要字符串
     */
    String getPreMigrationSummary();
    
    /**
     * 执行迁移
     * @param dryRun 如果为 true，只预览不实际写入
     * @return 迁移报告
     */
    MigrationReport migrate(boolean dryRun);
    
    /**
     * 估算迁移时间
     * @return 估算的时间字符串（如 "约 30 秒"）
     */
    String estimateMigrationTime();
}
