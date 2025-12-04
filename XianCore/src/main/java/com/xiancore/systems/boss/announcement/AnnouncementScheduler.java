package com.xiancore.systems.boss.announcement;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;

/**
 * 公告调度器
 * 管理公告的定时发送、优先级队列、冷却时间等
 *
 * 核心功能:
 * - 优先级队列管理
 * - 定时任务调度
 * - 冷却时间管理
 * - 异步处理
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
public class AnnouncementScheduler {

    // ==================== 配置常量 ====================

    /** 队列最大大小 */
    private static final int MAX_QUEUE_SIZE = 1000;

    // ==================== 内部状态 ====================

    /** 优先级队列 (按优先级降序排列) */
    private final PriorityQueue<BossAnnouncement> queue;

    /** 冷却时间映射 */
    private final Map<UUID, Long> cooldownMap;

    /** 定时任务映射 */
    private final Map<String, ScheduledFuture<?>> scheduledTasks;

    /** 线程池执行器 */
    private final ExecutorService executor;

    /** 定时任务执行器 */
    private final ScheduledExecutorService scheduledExecutor;

    /** 是否已启用 */
    private volatile boolean enabled;

    // ==================== 统计信息 ====================

    /** 已处理的公告数量 */
    private volatile long totalProcessed;

    /** 已发送的公告数量 */
    private volatile long totalSent;

    // ==================== 构造函数 ====================

    public AnnouncementScheduler() {
        // 创建优先级队列，按优先级降序排列
        this.queue = new PriorityQueue<>((a, b) -> {
            // 优先级高的先出列
            int priorityCompare = Integer.compare(b.getCombinedPriority(), a.getCombinedPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 同优先级则按创建时间升序 (先创建的先处理)
            return Long.compare(a.getCreatedTime(), b.getCreatedTime());
        });

        this.cooldownMap = new ConcurrentHashMap<>();
        this.scheduledTasks = new ConcurrentHashMap<>();

        // 创建线程池
        this.executor = Executors.newFixedThreadPool(2,
            r -> {
                Thread t = new Thread(r, "BossAnnouncementWorker-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            });

        this.scheduledExecutor = Executors.newScheduledThreadPool(1,
            r -> {
                Thread t = new Thread(r, "BossAnnouncementScheduler");
                t.setDaemon(true);
                return t;
            });

        this.enabled = false;
        this.totalProcessed = 0;
        this.totalSent = 0;
    }

    // ==================== 生命周期管理 ====================

    /**
     * 启用调度器
     */
    public void start() {
        if (enabled) {
            return;
        }

        enabled = true;

        // 启动队列处理任务
        scheduledExecutor.scheduleAtFixedRate(
            this::processQueueAsync,
            0,
            100,  // 每100毫秒处理一次
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 停止调度器
     */
    public void shutdown() {
        if (!enabled) {
            return;
        }

        enabled = false;

        // 取消所有定时任务
        scheduledTasks.values().forEach(future -> future.cancel(false));
        scheduledTasks.clear();

        // 关闭线程池
        executor.shutdown();
        scheduledExecutor.shutdown();

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        queue.clear();
    }

    // ==================== 队列管理 ====================

    /**
     * 将公告加入队列
     *
     * @param announcement 公告对象
     * @return 是否成功加入
     */
    public boolean enqueue(BossAnnouncement announcement) {
        if (!enabled || announcement == null) {
            return false;
        }

        if (queue.size() >= MAX_QUEUE_SIZE) {
            return false;
        }

        return queue.offer(announcement);
    }

    /**
     * 从队列获取下一个公告
     *
     * @return 公告对象，如果队列为空返回null
     */
    public BossAnnouncement nextAnnouncement() {
        return queue.poll();
    }

    /**
     * 获取队列中的公告数量
     *
     * @return 数量
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * 清空队列
     */
    public void clearQueue() {
        queue.clear();
    }

    /**
     * 获取队列中的所有公告
     *
     * @return 公告列表
     */
    public List<BossAnnouncement> getAllAnnouncements() {
        return new ArrayList<>(queue);
    }

    // ==================== 处理方法 ====================

    /**
     * 异步处理队列中的公告
     */
    private void processQueueAsync() {
        if (!enabled || queue.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            try {
                BossAnnouncement announcement = nextAnnouncement();
                if (announcement != null) {
                    processAnnouncement(announcement);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理单个公告
     */
    private void processAnnouncement(BossAnnouncement announcement) {
        if (announcement == null) {
            return;
        }

        // 检查冷却
        if (isInCooldown(announcement.getBossUUID())) {
            // 重新加入队列以后处理
            enqueue(announcement);
            return;
        }

        // 标记为已发送
        announcement.markAsSent();

        // 更新统计
        totalProcessed++;
        if (announcement.isSent()) {
            totalSent++;
        }
    }

    // ==================== 冷却管理 ====================

    /**
     * 添加冷却
     *
     * @param bossUUID Boss UUID
     * @param cooldownSeconds 冷却秒数
     */
    public void addCooldown(UUID bossUUID, long cooldownSeconds) {
        if (bossUUID == null) {
            return;
        }

        long cooldownEndTime = System.currentTimeMillis() + (cooldownSeconds * 1000);
        cooldownMap.put(bossUUID, cooldownEndTime);
    }

    /**
     * 判断是否在冷却中
     *
     * @param bossUUID Boss UUID
     * @return 是否在冷却中
     */
    public boolean isInCooldown(UUID bossUUID) {
        if (bossUUID == null) {
            return false;
        }

        Long cooldownEndTime = cooldownMap.get(bossUUID);
        if (cooldownEndTime == null) {
            return false;
        }

        if (System.currentTimeMillis() >= cooldownEndTime) {
            cooldownMap.remove(bossUUID);
            return false;
        }

        return true;
    }

    /**
     * 获取剩余冷却时间 (秒)
     *
     * @param bossUUID Boss UUID
     * @return 秒数，0表示无冷却
     */
    public long getRemainingCooldown(UUID bossUUID) {
        if (bossUUID == null) {
            return 0;
        }

        Long cooldownEndTime = cooldownMap.get(bossUUID);
        if (cooldownEndTime == null) {
            return 0;
        }

        long remaining = cooldownEndTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldownMap.remove(bossUUID);
            return 0;
        }

        return remaining / 1000;
    }

    /**
     * 移除冷却
     *
     * @param bossUUID Boss UUID
     */
    public void removeCooldown(UUID bossUUID) {
        if (bossUUID != null) {
            cooldownMap.remove(bossUUID);
        }
    }

    // ==================== 定时任务 ====================

    /**
     * 定时调度公告
     *
     * @param taskId 任务ID
     * @param announcement 公告对象
     * @param delayMillis 延迟 (毫秒)
     */
    public void scheduleAnnouncement(String taskId, BossAnnouncement announcement, long delayMillis) {
        if (!enabled || taskId == null || announcement == null) {
            return;
        }

        ScheduledFuture<?> future = scheduledExecutor.schedule(
            () -> {
                try {
                    enqueue(announcement);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            delayMillis,
            TimeUnit.MILLISECONDS
        );

        scheduledTasks.put(taskId, future);
    }

    /**
     * 定时周期性公告
     *
     * @param taskId 任务ID
     * @param announcement 公告对象
     * @param intervalMillis 间隔 (毫秒)
     */
    public void schedulePeriodicAnnouncement(String taskId, BossAnnouncement announcement, long intervalMillis) {
        if (!enabled || taskId == null || announcement == null) {
            return;
        }

        ScheduledFuture<?> future = scheduledExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    // 创建新的公告实例以避免重复发送
                    BossAnnouncement newAnnouncement = new BossAnnouncement();
                    newAnnouncement.setType(announcement.getType());
                    newAnnouncement.setTemplate(announcement.getTemplate());
                    newAnnouncement.setParams(announcement.getAllParams());
                    enqueue(newAnnouncement);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            0,
            intervalMillis,
            TimeUnit.MILLISECONDS
        );

        scheduledTasks.put(taskId, future);
    }

    /**
     * 取消定时任务
     *
     * @param taskId 任务ID
     */
    public void cancelTask(String taskId) {
        if (taskId == null) {
            return;
        }

        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * 获取所有定时任务ID
     *
     * @return ID列表
     */
    public Set<String> getAllTaskIds() {
        return new HashSet<>(scheduledTasks.keySet());
    }

    // ==================== 统计方法 ====================

    /**
     * 获取已处理的公告数量
     *
     * @return 数量
     */
    public long getTotalProcessed() {
        return totalProcessed;
    }

    /**
     * 获取已发送的公告数量
     *
     * @return 数量
     */
    public long getTotalSent() {
        return totalSent;
    }

    /**
     * 获取统计信息
     *
     * @return 统计字符串
     */
    public String getStatistics() {
        return String.format(
            "公告调度器统计: 已处理%d个, 已发送%d个, 队列中%d个, 冷却中%d个, 定时任务%d个",
            totalProcessed,
            totalSent,
            queue.size(),
            cooldownMap.size(),
            scheduledTasks.size()
        );
    }

    /**
     * 重置统计数据
     */
    public void resetStatistics() {
        totalProcessed = 0;
        totalSent = 0;
    }
}
