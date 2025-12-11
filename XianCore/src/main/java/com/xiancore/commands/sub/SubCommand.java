package com.xiancore.commands.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 子命令接口
 * 定义子命令的统一契约
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public interface SubCommand {

    /**
     * 获取命令名称
     *
     * @return 命令名（如 "reload"）
     */
    String getName();

    /**
     * 获取命令别名（支持中文）
     *
     * @return 别名数组（如 ["重载"]）
     */
    String[] getAliases();

    /**
     * 获取权限节点
     *
     * @return 权限节点（如 "xiancore.reload"），null 表示无需权限
     */
    String getPermission();

    /**
     * 获取用法说明
     *
     * @return 用法字符串
     */
    String getUsage();

    /**
     * 获取命令描述
     *
     * @return 描述字符串
     */
    String getDescription();

    /**
     * 执行命令
     *
     * @param sender 命令发送者
     * @param args   命令参数（不包含子命令名本身）
     */
    void execute(CommandSender sender, String[] args);

    /**
     * Tab 补全
     *
     * @param sender 命令发送者
     * @param args   当前参数
     * @return 补全建议列表
     */
    List<String> tabComplete(CommandSender sender, String[] args);

    /**
     * 是否需要玩家执行
     *
     * @return true 需要玩家，false 控制台也可以
     */
    default boolean requiresPlayer() {
        return false;
    }
}
