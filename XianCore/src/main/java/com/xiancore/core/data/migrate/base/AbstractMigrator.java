package com.xiancore.core.data.migrate.base;

import com.xiancore.XianCore;
import com.xiancore.core.data.DataManager;

import java.io.File;

/**
 * 抽象迁移器基类
 * 提供公共的迁移功能实现
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public abstract class AbstractMigrator implements IMigrator {
    
    protected final XianCore plugin;
    protected final DataManager dataManager;
    
    public AbstractMigrator(XianCore plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }
    
    @Override
    public String estimateMigrationTime() {
        long estimatedMs = estimateTimeInMillis();
        long estimatedSeconds = estimatedMs / 1000;
        
        if (estimatedSeconds < 1) {
            return "约 1 秒";
        } else if (estimatedSeconds < 60) {
            return "约 " + estimatedSeconds + " 秒";
        } else if (estimatedSeconds < 3600) {
            return "约 " + (estimatedSeconds / 60) + " 分钟";
        } else {
            return "约 " + (estimatedSeconds / 3600) + " 小时";
        }
    }
    
    /**
     * 估算迁移时间（毫秒）
     * 子类可重写以提供更准确的估算
     */
    protected abstract long estimateTimeInMillis();
    
    /**
     * 格式化文件大小
     */
    protected String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
    
    /**
     * 计算文件夹的总大小
     */
    protected long calculateTotalSize(File dir, String extension) {
        if (!dir.exists() || !dir.isDirectory()) {
            return 0;
        }
        
        File[] files = dir.listFiles((d, name) -> name.endsWith(extension));
        if (files == null) {
            return 0;
        }
        
        long total = 0;
        for (File file : files) {
            total += file.length();
        }
        return total;
    }
    
    /**
     * 检查MySQL是否可用
     */
    protected boolean isMySqlAvailable() {
        boolean available = dataManager.isMySqlAvailable();
        if (!available) {
            plugin.getLogger().warning("§eMySQL不可用 - useMySql: " + dataManager.isUsingMySql());
        }
        return available;
    }
}
