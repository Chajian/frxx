package com.xiancore.systems.sect.listeners;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.task.TaskProgressTracker;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/**
 * Sect Task Listener
 * Registers all task-related event listeners
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectTaskListener implements Listener {

    private final XianCore plugin;
    private final TaskProgressTracker progressTracker;

    public SectTaskListener(XianCore plugin, TaskProgressTracker progressTracker) {
        this.plugin = plugin;
        this.progressTracker = progressTracker;
    }

    /**
     * Register all task-related event listeners
     */
    public void register() {
        // Register progress tracker (TaskProgressTracker implements Listener)
        Bukkit.getPluginManager().registerEvents(progressTracker, plugin);
        plugin.getLogger().info("  \u00a7a\u2713 \u5b97\u95e8\u4efb\u52a1\u76d1\u542c\u5668\u5df2\u6ce8\u518c");
    }

    /**
     * Get progress tracker
     */
    public TaskProgressTracker getProgressTracker() {
        return progressTracker;
    }
}
