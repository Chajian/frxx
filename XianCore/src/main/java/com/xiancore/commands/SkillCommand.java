package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.SkillGUI;
import com.xiancore.gui.SkillBindGUI;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillSystem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 功法命令处理器
 * 处理 /skill 命令
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillCommand extends BaseCommand {

    public SkillCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // 如果没有参数，打开功法GUI
        if (args.length == 0) {
            SkillGUI.open(player, plugin);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "gui":
                SkillGUI.open(player, plugin);
                break;

            case "list":
            case "列表":
                showSkillList(player);
                break;

            case "learn":
            case "学习":
                handleLearn(player, args);
                break;

            case "upgrade":
            case "升级":
                handleUpgrade(player, args);
                break;

            case "info":
            case "信息":
                handleInfo(player, args);
                break;

            case "cast":
            case "施放":
                handleCast(player, args);
                break;

            case "bind":
            case "绑定":
                handleBind(player, args);
                break;

            case "unbind":
            case "解绑":
                handleUnbind(player, args);
                break;

            case "binds":
            case "绑定列表":
                handleBinds(player);
                break;

            case "bindgui":
            case "绑定界面":
                handleBindGUI(player);
                break;

            case "forget":
            case "遗忘":
                handleForget(player, args);
                break;

            case "help":
            case "帮助":
                showHelp(sender);
                break;

            default:
                sendError(sender, "未知的子命令: " + subCommand);
                sendInfo(sender, "使用 /skill help 查看帮助");
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(Arrays.asList("list", "learn", "upgrade", "info", "cast", "bind", "unbind", "binds", "bindgui", "forget", "help"), args[0]);
        }
        
        // bind 命令的tab补全
        if (args.length == 2 && "bind".equalsIgnoreCase(args[0])) {
            // 第二个参数：槽位 (1-9)
            return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");
        }
        
        // bind 和 learn 的第三个参数：功法ID
        if (args.length == 3 && ("bind".equalsIgnoreCase(args[0]) || "learn".equalsIgnoreCase(args[0]))) {
            SkillSystem skillSystem = plugin.getSkillSystem();
            if (sender instanceof Player) {
                return skillSystem.getAllSkills().keySet().stream()
                        .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        // forget 命令的tab补全：已学功法ID
        if (args.length == 2 && "forget".equalsIgnoreCase(args[0])) {
            SkillSystem skillSystem = plugin.getSkillSystem();
            if (sender instanceof Player player) {
                return skillSystem.getPlayerSkills(player).keySet().stream()
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        // unbind 命令的tab补全：槽位
        if (args.length == 2 && "unbind".equalsIgnoreCase(args[0])) {
            return Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");
        }
        
        return List.of();
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== 功法系统命令帮助 ==========");
        sendInfo(sender, "§e/skill §7- 查看已学功法");
        sendInfo(sender, "§e/skill list §7- 查看已学功法");
        sendInfo(sender, "§e/skill learn <功法> §7- 学习新功法");
        sendInfo(sender, "§e/skill upgrade <功法> §7- 升级功法");
        sendInfo(sender, "§e/skill forget <功法> §7- 遗忘功法（返还部分资源）");
        sendInfo(sender, "§e/skill info <功法> §7- 查看功法信息");
        sendInfo(sender, "§e/skill cast <功法> §7- 施放功法");
        sendInfo(sender, "");
        sendInfo(sender, "§6§l快捷键绑定:");
        sendInfo(sender, "§e/skill bind <槽位> <功法> §7- 绑定功法到快捷栏");
        sendInfo(sender, "§e/skill unbind <槽位> §7- 解除槽位绑定");
        sendInfo(sender, "§e/skill binds §7- 查看当前绑定");
        sendInfo(sender, "§e/skill bindgui §7- 打开绑定GUI界面");
        sendInfo(sender, "§e/skill help §7- 显示此帮助");
        sendInfo(sender, "§b=====================================");
    }

    /**
     * 显示功法列表
     */
    private void showSkillList(Player player) {
        if (!hasPermission(player, "xiancore.skill.use")) {
            return;
        }

        SkillSystem skillSystem = plugin.getSkillSystem();
        Map<String, Integer> playerSkills = skillSystem.getPlayerSkills(player);

        sendInfo(player, "§b========== 已学功法 ==========");

        if (playerSkills.isEmpty()) {
            sendWarning(player, "你还没有学习任何功法!");
            sendInfo(player, "使用 /skill learn <功法ID> 学习新功法");
        } else {
            for (Map.Entry<String, Integer> entry : playerSkills.entrySet()) {
                String skillId = entry.getKey();
                int level = entry.getValue();
                Skill skill = skillSystem.getSkill(skillId);

                if (skill != null) {
                    sendInfo(player, "§e" + skill.getName() + " §7[" + skillId + "] - Lv." + level + "/" + skill.getMaxLevel());
                } else {
                    sendInfo(player, "§e" + skillId + " §7- 等级 " + level);
                }
            }
        }

        sendInfo(player, "§b=============================");
        sendInfo(player, "§7功法总数: §f" + playerSkills.size());
    }

    /**
     * 处理学习功法
     */
    private void handleLearn(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.skill.learn")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要学习的功法ID!");
            sendInfo(player, "用法: /skill learn <功法ID>");
            return;
        }

        String skillId = args[1];
        SkillSystem skillSystem = plugin.getSkillSystem();

        // 调用 SkillSystem 的学习功能
        boolean success = skillSystem.learnSkill(player, skillId);

        if (success) {
            sendSuccess(player, "成功学习功法!");
        }
    }

    /**
     * 处理升级功法
     */
    private void handleUpgrade(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.skill.upgrade")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要升级的功法ID!");
            sendInfo(player, "用法: /skill upgrade <功法ID>");
            return;
        }

        String skillId = args[1];
        SkillSystem skillSystem = plugin.getSkillSystem();

        // 调用 SkillSystem 的升级功能
        boolean success = skillSystem.upgradeSkill(player, skillId);

        if (success) {
            sendSuccess(player, "成功升级功法!");
        }
    }

    /**
     * 处理功法信息查看
     */
    private void handleInfo(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.skill.use")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要查看的功法ID!");
            sendInfo(player, "用法: /skill info <功法ID>");
            return;
        }

        String skillId = args[1];
        SkillSystem skillSystem = plugin.getSkillSystem();
        Skill skill = skillSystem.getSkill(skillId);

        if (skill == null) {
            sendError(player, "功法不存在: " + skillId);
            return;
        }

        int playerLevel = skillSystem.getPlayerSkillLevel(player, skillId);

        sendInfo(player, "§b========== 功法信息 ==========");
        sendInfo(player, "§e功法名称: §f" + skill.getName());
        sendInfo(player, "§e功法ID: §7" + skill.getId());
        sendInfo(player, "§e类型: §f" + skill.getType().getDisplayName());

        if (skill.getElement() != null) {
            sendInfo(player, "§e属性: " + skill.getElement().getColoredName());
        }

        if (playerLevel > 0) {
            sendInfo(player, "§e当前等级: §f" + playerLevel + "/" + skill.getMaxLevel());
            sendInfo(player, "");
            sendInfo(player, "§7== 当前等级效果 ==");

            if (skill.getBaseDamage() > 0) {
                sendInfo(player, "§c伤害: §f" + String.format("%.1f", skill.calculateDamage(playerLevel)));
            }
            if (skill.getBaseHealing() > 0) {
                sendInfo(player, "§a治疗: §f" + String.format("%.1f", skill.calculateHealing(playerLevel)));
            }
            if (skill.getBaseRange() > 0) {
                sendInfo(player, "§e范围: §f" + String.format("%.1f", skill.calculateRange(playerLevel)) + "格");
            }

            sendInfo(player, "§b消耗: §f" + skill.calculateQiCost(playerLevel) + " 灵气");
            sendInfo(player, "§e冷却: §f" + skill.calculateCooldown(playerLevel) + "秒");
        } else {
            sendInfo(player, "§7你还未学习此功法");
            sendInfo(player, "");
            sendInfo(player, "§7== 需求 ==");
            if (skill.getRequiredRealm() != null) {
                sendInfo(player, "§7境界: §f" + skill.getRequiredRealm());
            }
            if (skill.getRequiredLevel() > 0) {
                sendInfo(player, "§7等级: §f" + skill.getRequiredLevel());
            }
        }

        sendInfo(player, "");
        sendInfo(player, "§7" + skill.getDescription());
        sendInfo(player, "§b=============================");
    }

    /**
     * 处理施放功法
     */
    private void handleCast(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.skill.use")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要施放的功法ID!");
            sendInfo(player, "用法: /skill cast <功法ID>");
            return;
        }

        String skillId = args[1];
        SkillSystem skillSystem = plugin.getSkillSystem();

        // 使用新的自动目标选择方法
        skillSystem.castSkill(player, skillId);
    }

    /**
     * 处理绑定功法
     */
    private void handleBind(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.skill.bind")) {
            return;
        }

        if (args.length < 3) {
            sendError(player, "用法: /skill bind <槽位> <功法ID>");
            sendInfo(player, "槽位范围: 1-9");
            sendInfo(player, "示例: /skill bind 1 fireball");
            return;
        }

        // 解析槽位
        int slot;
        try {
            slot = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendError(player, "槽位必须是数字 (1-9)");
            return;
        }

        String skillId = args[2];
        
        // 调用绑定管理器
        boolean success = plugin.getSkillSystem().getBindManager().bindSkill(player, slot, skillId);
        
        if (success) {
            sendInfo(player, "");
            sendInfo(player, "§7提示: 切换快捷栏到槽位 §f" + slot + " §7然后按 §eF键 §7即可施放");
        }
    }

    /**
     * 处理解绑功法
     */
    private void handleUnbind(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.skill.bind")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "用法: /skill unbind <槽位>");
            sendInfo(player, "槽位范围: 1-9");
            sendInfo(player, "示例: /skill unbind 1");
            return;
        }

        // 解析槽位
        int slot;
        try {
            slot = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendError(player, "槽位必须是数字 (1-9)");
            return;
        }

        // 调用绑定管理器
        plugin.getSkillSystem().getBindManager().unbindSkill(player, slot);
    }

    /**
     * 显示所有绑定
     */
    private void handleBinds(Player player) {
        if (!hasPermission(player, "xiancore.skill.bind")) {
            return;
        }

        Map<Integer, String> bindings = plugin.getSkillSystem().getBindManager().getAllBindings(player);
        
        sendInfo(player, "§b========== 快捷键绑定 ==========");
        
        if (bindings.isEmpty()) {
            sendInfo(player, "§7你还没有绑定任何功法");
            sendInfo(player, "");
            sendInfo(player, "§e使用 §f/skill bind <槽位> <功法ID> §e进行绑定");
            sendInfo(player, "§7示例: §f/skill bind 1 fireball");
        } else {
            sendInfo(player, "");
            
            // 显示所有9个槽位的绑定状态
            for (int slot = 1; slot <= 9; slot++) {
                String skillId = bindings.get(slot);
                
                if (skillId != null) {
                    Skill skill = plugin.getSkillSystem().getSkill(skillId);
                    String skillName = skill != null ? skill.getName() : skillId;
                    
                    // 检查冷却
                    int cooldown = plugin.getSkillSystem().getCooldownManager().getRemainingCooldown(player, skillId);
                    String cooldownStr = cooldown > 0 ? " §c(" + cooldown + "秒)" : " §a✓";
                    
                    sendInfo(player, "  §e槽位 " + slot + ": §f" + skillName + cooldownStr);
                } else {
                    sendInfo(player, "  §7槽位 " + slot + ": §8(未绑定)");
                }
            }
            
            sendInfo(player, "");
            sendInfo(player, "§7提示: 切换快捷栏到对应槽位，按 §eF键 §7施放");
        }
        
        sendInfo(player, "§b===============================");
    }

    /**
     * 打开绑定GUI
     */
    private void handleBindGUI(Player player) {
        if (!hasPermission(player, "xiancore.skill.bind")) {
            return;
        }

        SkillBindGUI.open(player, plugin);
    }

    /**
     * 处理遗忘功法（带二次确认）
     */
    private void handleForget(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.skill.forget")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请指定要遗忘的功法ID!");
            sendInfo(player, "用法: /skill forget <功法ID>");
            sendInfo(player, "§c警告: 遗忘功法将移除该功法，并返还部分资源");
            return;
        }

        String skillId = args[1];
        SkillSystem skillSystem = plugin.getSkillSystem();
        Skill skill = skillSystem.getSkill(skillId);

        if (skill == null) {
            sendError(player, "功法不存在: " + skillId);
            return;
        }

        // 检查是否已学习
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null || !data.getSkills().containsKey(skillId)) {
            sendError(player, "你还没有学习这个功法!");
            return;
        }

        int currentLevel = data.getSkills().get(skillId);

        // 二次确认机制
        boolean forceConfirm = args.length >= 3 && "confirm".equalsIgnoreCase(args[2]);
        
        if (!forceConfirm) {
            // 显示确认提示
            sendWarning(player, "§c§l========== 遗忘功法确认 ==========");
            sendWarning(player, "§e功法: §f" + skill.getName() + " §7[" + skillId + "]");
            sendWarning(player, "§e等级: §f" + currentLevel);
            sendWarning(player, "");
            sendWarning(player, "§c警告: 遗忘后该功法将被移除");
            sendWarning(player, "§7• 将返还部分功法点");
            sendWarning(player, "§7• 可能返还部分灵石（根据配置）");
            sendWarning(player, "§7• 自动解绑所有快捷键");
            sendWarning(player, "§7• 清除当前冷却");
            sendWarning(player, "§7• 一段时间内无法重新学习");
            sendWarning(player, "");
            sendWarning(player, "§e如果确定要遗忘，请输入:");
            sendWarning(player, "§f/skill forget " + skillId + " confirm");
            sendWarning(player, "§c§l================================");
            return;
        }

        // 执行遗忘
        boolean success = skillSystem.forgetSkill(player, skillId);

        if (!success) {
            // 错误信息已在 forgetSkill 中显示
            return;
        }

        // 成功消息已在 forgetSkill 中显示
        sendSuccess(player, "功法遗忘操作已完成");
    }

    /**
     * 获取玩家视线中的实体
     *
     * @param player 玩家
     * @param range  范围
     * @return 目标实体，如果没有则返回null
     */
    private LivingEntity getTargetEntity(Player player, double range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                entity -> entity instanceof LivingEntity && entity != player
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }

        return null;
    }
}
