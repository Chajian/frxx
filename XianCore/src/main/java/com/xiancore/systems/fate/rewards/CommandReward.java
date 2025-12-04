package com.xiancore.systems.fate.rewards;

import com.xiancore.XianCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 命令奖励
 * 执行控制台命令作为奖励
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class CommandReward extends FateReward {

    private String command;

    public CommandReward(XianCore plugin, String command) {
        super(plugin, "command");
        this.command = command;
    }

    @Override
    public String give(Player player) {
        // 概率判定
        if (!shouldGive()) {
            return null;
        }

        try {
            if (command == null || command.isEmpty()) {
                plugin.getLogger().warning("[奇遇系统] 命令为空，无法执行");
                return null;
            }

            // 替换变量
            String finalCommand = replaceVariables(command, player);

            // 执行命令
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);

            if (success) {
                if (plugin.isDebugMode()) {
                    plugin.getLogger().fine("[奇遇系统] 执行命令成功: " + finalCommand);
                }
                return "§a特殊奖励已发放";
            } else {
                plugin.getLogger().warning("[奇遇系统] 命令执行失败: " + finalCommand);
                return null;
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 执行命令奖励失败: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 替换命令中的变量
     *
     * @param command 原始命令
     * @param player  玩家
     * @return 替换后的命令
     */
    private String replaceVariables(String command, Player player) {
        return command
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{world}", player.getWorld().getName());
    }

    /**
     * 从配置Map创建命令奖励
     *
     * @param map    配置Map
     * @param plugin 插件实例
     * @return 命令奖励对象
     */
    public static CommandReward fromMap(Map<String, Object> map, XianCore plugin) {
        // 读取命令（必需）
        String command = (String) map.get("command");
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("命令奖励缺少 command 字段");
        }

        return new CommandReward(plugin, command);
    }

    /**
     * 获取命令（用于调试和验证）
     */
    public String getCommand() {
        return command;
    }

    /**
     * 设置命令
     */
    public void setCommand(String command) {
        this.command = command;
    }
}


