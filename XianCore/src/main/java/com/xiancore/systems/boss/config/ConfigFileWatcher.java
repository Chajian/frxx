package com.xiancore.systems.boss.config;

import com.xiancore.XianCore;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Logger;

/**
 * Boss配置文件监听器 - 实现热重载功能
 * 自动监听boss-refresh.yml文件变更并触发重载
 *
 * 功能:
 * - 文件变更监听 (WatchService)
 * - Debounce机制 (避免重复触发)
 * - 异步处理 (不阻塞主线程)
 * - 错误恢复 (异常处理)
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-16
 */
@Getter
public class ConfigFileWatcher {

    private final XianCore plugin;
    private final Logger logger;
    private final File configFile;
    private final Runnable reloadCallback;

    private WatchService watchService;
    private WatchKey watchKey;
    private Thread watcherThread;
    private volatile boolean watching = false;

    // 防止重复触发的debounce机制
    private volatile long lastModifiedTime = 0;
    private static final long DEBOUNCE_DELAY_MS = 1000; // 1秒防抖延迟

    /**
     * 构造函数
     *
     * @param plugin 插件实例
     * @param configFile 配置文件
     * @param reloadCallback 文件变更时的回调函数
     */
    public ConfigFileWatcher(XianCore plugin, File configFile, Runnable reloadCallback) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = configFile;
        this.reloadCallback = reloadCallback;
    }

    /**
     * 启动文件监听器
     */
    public void start() {
        if (watching) {
            logger.warning("ConfigFileWatcher already started!");
            return;
        }

        try {
            // 创建WatchService
            watchService = FileSystems.getDefault().newWatchService();

            // 注册监听目录
            Path configDir = configFile.getParentFile().toPath();
            watchKey = configDir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE
            );

            watching = true;

            // 在后台线程中运行监听器
            watcherThread = new Thread(this::watchConfigFile);
            watcherThread.setName("Boss-ConfigFileWatcher");
            watcherThread.setDaemon(true);
            watcherThread.start();

            logger.info("✓ Boss配置文件监听器已启动");
            logger.info("  - 监听文件: " + configFile.getAbsolutePath());
            logger.info("  - 防抖延迟: " + DEBOUNCE_DELAY_MS + "ms");

        } catch (IOException e) {
            logger.severe("✗ 启动配置文件监听器失败: " + e.getMessage());
            e.printStackTrace();
            watching = false;
        }
    }

    /**
     * 停止文件监听器
     */
    public void stop() {
        if (!watching) {
            return;
        }

        try {
            watching = false;

            if (watchKey != null) {
                watchKey.cancel();
            }

            if (watchService != null) {
                watchService.close();
            }

            if (watcherThread != null) {
                watcherThread.interrupt();
                // 等待最多5秒
                watcherThread.join(5000);
            }

            logger.info("✓ Boss配置文件监听器已停止");

        } catch (Exception e) {
            logger.warning("✗ 停止配置文件监听器时出错: " + e.getMessage());
        }
    }

    /**
     * 文件监听主循环
     * 在后台线程中运行
     */
    private void watchConfigFile() {
        try {
            String configFileName = configFile.getName();

            while (watching) {
                try {
                    // 等待文件事件 (timeout: 500ms)
                    WatchKey key = watchService.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);

                    if (key == null) {
                        continue;
                    }

                    // 处理所有事件
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context() == null) {
                            continue;
                        }

                        String eventFileName = event.context().toString();

                        // 只关注配置文件
                        if (!eventFileName.equals(configFileName)) {
                            continue;
                        }

                        // Debounce: 检查是否在防抖延迟内
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastModifiedTime < DEBOUNCE_DELAY_MS) {
                            continue;
                        }

                        lastModifiedTime = currentTime;

                        logger.info("✓ 检测到配置文件变更: " + configFileName);
                        logger.info("  - 事件类型: " + event.kind());

                        // 在主线程中执行重载回调
                        if (reloadCallback != null) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    logger.info("  - 正在自动重载配置...");
                                    reloadCallback.run();
                                    logger.info("  - 配置重载成功");
                                } catch (Exception e) {
                                    logger.severe("  - 配置重载失败: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        }
                    }

                    // 重置WatchKey
                    boolean valid = key.reset();
                    if (!valid) {
                        logger.warning("✗ WatchKey无效，停止监听");
                        break;
                    }

                } catch (InterruptedException e) {
                    // 线程被中断，正常退出
                    break;
                } catch (Exception e) {
                    logger.warning("✗ 监听文件事件时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            logger.severe("✗ 配置文件监听器异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            watching = false;
        }
    }

    /**
     * 检查监听器是否正在运行
     *
     * @return 是否正在监听
     */
    public boolean isWatching() {
        return watching;
    }

    /**
     * 获取防抖延迟时间
     *
     * @return 延迟时间(毫秒)
     */
    public static long getDebounceDelayMs() {
        return DEBOUNCE_DELAY_MS;
    }
}
