package com.xiancore.core.data.migrate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据迁移报告
 * 记录迁移过程中的统计信息和错误
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class MigrationReport {
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalFiles;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private List<MigrationError> errors;
    private long totalDataSize;
    
    public MigrationReport() {
        this.startTime = LocalDateTime.now();
        this.errors = new ArrayList<>();
        this.totalFiles = 0;
        this.successCount = 0;
        this.failedCount = 0;
        this.skippedCount = 0;
        this.totalDataSize = 0;
    }
    
    /**
     * 记录成功迁移
     */
    public void recordSuccess() {
        this.successCount++;
    }
    
    /**
     * 记录失败
     */
    public void recordFailure(String playerName, String uuid, String errorMessage) {
        this.failedCount++;
        this.errors.add(new MigrationError(playerName, uuid, errorMessage));
    }
    
    /**
     * 记录跳过
     */
    public void recordSkipped() {
        this.skippedCount++;
    }
    
    /**
     * 设置总文件数
     */
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    /**
     * 增加数据大小
     */
    public void addDataSize(long size) {
        this.totalDataSize += size;
    }
    
    /**
     * 完成迁移
     */
    public void complete() {
        this.endTime = LocalDateTime.now();
    }
    
    /**
     * 获取迁移耗时（秒）
     */
    public long getElapsedSeconds() {
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end).getSeconds();
    }
    
    /**
     * 获取迁移耗时（毫秒）
     */
    public long getElapsedMillis() {
        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();
        return Duration.between(startTime, end).toMillis();
    }
    
    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        if (totalFiles == 0) return 0.0;
        return (double) successCount / totalFiles * 100.0;
    }
    
    /**
     * 是否有错误
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * 生成文本报告
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("§b========================================\n");
        sb.append("§e§l       数据迁移完成报告\n");
        sb.append("§b========================================\n");
        sb.append("\n");
        
        // 时间信息
        sb.append("§e开始时间: §f").append(startTime).append("\n");
        sb.append("§e结束时间: §f").append(endTime != null ? endTime : "进行中").append("\n");
        sb.append("§e总耗时: §f").append(getElapsedSeconds()).append(" 秒\n");
        sb.append("\n");
        
        // 统计信息
        sb.append("§b======== 统计信息 ========\n");
        sb.append("§e总文件数: §f").append(totalFiles).append("\n");
        sb.append("§a成功迁移: §f").append(successCount).append("\n");
        sb.append("§c失败数量: §f").append(failedCount).append("\n");
        sb.append("§7跳过数量: §f").append(skippedCount).append("\n");
        sb.append("§e成功率: §f").append(String.format("%.2f", getSuccessRate())).append("%\n");
        sb.append("§e数据大小: §f").append(formatFileSize(totalDataSize)).append("\n");
        sb.append("\n");
        
        // 性能信息
        if (successCount > 0 && getElapsedSeconds() > 0) {
            double speed = (double) successCount / getElapsedSeconds();
            sb.append("§e迁移速度: §f").append(String.format("%.2f", speed)).append(" 个/秒\n");
            sb.append("\n");
        }
        
        // 错误详情
        if (hasErrors()) {
            sb.append("§c======== 错误详情 ========\n");
            int displayCount = Math.min(errors.size(), 10);
            for (int i = 0; i < displayCount; i++) {
                MigrationError error = errors.get(i);
                sb.append("§c").append(i + 1).append(". §f").append(error.playerName)
                  .append(" §7(").append(error.uuid).append(")\n");
                sb.append("   §7错误: ").append(error.errorMessage).append("\n");
            }
            if (errors.size() > 10) {
                sb.append("§7... 还有 ").append(errors.size() - 10).append(" 个错误未显示\n");
            }
            sb.append("\n");
        }
        
        // 总结
        sb.append("§b========================================\n");
        if (failedCount == 0) {
            sb.append("§a§l✓ 迁移完全成功！所有数据已安全迁移到MySQL\n");
        } else if (successCount > 0) {
            sb.append("§e§l⚠ 迁移部分成功，请检查失败的记录\n");
        } else {
            sb.append("§c§l✗ 迁移失败，未能迁移任何数据\n");
        }
        sb.append("§b========================================\n");
        
        return sb.toString();
    }
    
    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
    
    // Getters
    public int getTotalFiles() { return totalFiles; }
    public int getSuccessCount() { return successCount; }
    public int getFailedCount() { return failedCount; }
    public int getSkippedCount() { return skippedCount; }
    public List<MigrationError> getErrors() { return errors; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    
    /**
     * 迁移错误记录
     */
    public static class MigrationError {
        private final String playerName;
        private final String uuid;
        private final String errorMessage;
        private final LocalDateTime timestamp;
        
        public MigrationError(String playerName, String uuid, String errorMessage) {
            this.playerName = playerName;
            this.uuid = uuid;
            this.errorMessage = errorMessage;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getPlayerName() { return playerName; }
        public String getUuid() { return uuid; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
