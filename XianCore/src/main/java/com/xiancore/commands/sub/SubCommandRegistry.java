package com.xiancore.commands.sub;

import java.util.*;

/**
 * 子命令注册器
 * 管理所有子命令的注册和查找
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class SubCommandRegistry {

    /**
     * 命令名 -> 子命令映射
     */
    private final Map<String, SubCommand> commands = new HashMap<>();

    /**
     * 别名 -> 命令名映射
     */
    private final Map<String, String> aliases = new HashMap<>();

    /**
     * 注册子命令
     *
     * @param command 子命令实例
     */
    public void register(SubCommand command) {
        String name = command.getName().toLowerCase();
        commands.put(name, command);

        // 注册别名
        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), name);
        }
    }

    /**
     * 获取子命令
     *
     * @param nameOrAlias 命令名或别名
     * @return 子命令实例，未找到返回 null
     */
    public SubCommand get(String nameOrAlias) {
        if (nameOrAlias == null) {
            return null;
        }

        String lowerName = nameOrAlias.toLowerCase();

        // 先尝试直接匹配
        SubCommand command = commands.get(lowerName);
        if (command != null) {
            return command;
        }

        // 再尝试别名匹配
        String realName = aliases.get(lowerName);
        if (realName != null) {
            return commands.get(realName);
        }

        return null;
    }

    /**
     * 获取所有命令名（用于 Tab 补全）
     *
     * @return 命令名列表
     */
    public List<String> getCommandNames() {
        return new ArrayList<>(commands.keySet());
    }

    /**
     * 获取所有已注册的子命令
     *
     * @return 子命令集合
     */
    public Collection<SubCommand> getAllCommands() {
        return commands.values();
    }

    /**
     * 检查命令是否存在
     *
     * @param nameOrAlias 命令名或别名
     * @return true 存在
     */
    public boolean exists(String nameOrAlias) {
        return get(nameOrAlias) != null;
    }

    /**
     * 获取注册的命令数量
     *
     * @return 命令数量
     */
    public int size() {
        return commands.size();
    }
}
