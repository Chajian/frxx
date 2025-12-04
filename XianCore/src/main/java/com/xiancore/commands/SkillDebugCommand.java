package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.skill.ElementalAttribute;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.SkillElement;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 功法系统调试命令
 * 用于测试和调试功法系统的各项功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillDebugCommand extends BaseCommand {

    public SkillDebugCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xiancore.admin")) {
            sendError(sender, "权限不足！");
            return;
        }

        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "slots" -> handleSlots(sender, args);
            case "element" -> handleElement(sender, args);
            case "cooldown" -> handleCooldown(sender, args);
            case "cache" -> handleCache(sender, args);
            case "test" -> handleTest(sender, args);
            default -> {
                sendError(sender, "未知的子命令: " + subCommand);
                showHelp(sender);
            }
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(
                Arrays.asList("slots", "element", "cooldown", "cache", "test"), 
                args[0]
            );
        }
        return List.of();
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== 功法调试命令 ==========");
        sendInfo(sender, "§e/skilldebug slots <玩家> §7- 查看玩家槽位");
        sendInfo(sender, "§e/skilldebug element <实体选择器> §7- 查看实体元素");
        sendInfo(sender, "§e/skilldebug element cache §7- 查看元素缓存");
        sendInfo(sender, "§e/skilldebug cooldown <玩家> §7- 查看玩家冷却");
        sendInfo(sender, "§e/skilldebug cache clear §7- 清除所有缓存");
        sendInfo(sender, "§e/skilldebug test elemental §7- 测试五行相克");
        sendInfo(sender, "§b===================================");
    }

    /**
     * 处理槽位查询
     */
    private void handleSlots(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "请指定玩家名！");
            sendInfo(sender, "用法: /skilldebug slots <玩家>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendError(sender, "玩家不在线: " + args[1]);
            return;
        }

        PlayerData data = plugin.getDataManager().loadPlayerData(target.getUniqueId());
        if (data == null) {
            sendError(sender, "数据加载失败！");
            return;
        }

        int currentSlots = data.getSkills().size();
        int maxSlots = plugin.getSkillSystem().getMaxSkillSlots(target, data);

        sendInfo(sender, "§b========== 功法槽位信息 ==========");
        sendInfo(sender, "§e玩家: §f" + target.getName());
        sendInfo(sender, "§e境界: §f" + data.getRealm());
        sendInfo(sender, "§e已学功法: §f" + currentSlots + " 个");
        sendInfo(sender, "§e槽位上限: §f" + maxSlots + " 个");
        sendInfo(sender, "§e使用率: §f" + String.format("%.1f%%", (currentSlots * 100.0 / maxSlots)));
        
        if (currentSlots >= maxSlots) {
            sendWarning(sender, "槽位已满！");
        } else {
            sendSuccess(sender, "还可以学习 " + (maxSlots - currentSlots) + " 个功法");
        }
        
        sendInfo(sender, "§b=================================");
    }

    /**
     * 处理元素查询
     */
    private void handleElement(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "请指定实体选择器或'cache'！");
            sendInfo(sender, "用法: /skilldebug element <@e/@s/@p> 或 cache");
            return;
        }

        if (args[1].equalsIgnoreCase("cache")) {
            int cacheSize = ElementalAttribute.getCacheSize();
            sendInfo(sender, "§e元素缓存大小: §f" + cacheSize + " 个实体");
            return;
        }

        // 获取附近的实体
        if (!(sender instanceof Player player)) {
            sendError(sender, "此命令只能由玩家执行！");
            return;
        }

        // 获取玩家视线中的实体（使用RayTrace）
        org.bukkit.util.RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
            player.getEyeLocation(),
            player.getEyeLocation().getDirection(),
            10.0,
            entity -> entity instanceof LivingEntity && entity != player
        );
        
        if (rayTrace == null || rayTrace.getHitEntity() == null || !(rayTrace.getHitEntity() instanceof LivingEntity)) {
            sendError(sender, "请看向一个实体！");
            return;
        }
        
        Entity target = rayTrace.getHitEntity();

        LivingEntity living = (LivingEntity) target;
        SkillElement element = ElementalAttribute.getEntityElement(living);

        sendInfo(sender, "§b========== 实体元素信息 ==========");
        sendInfo(sender, "§e实体类型: §f" + living.getType());
        sendInfo(sender, "§e实体名称: §f" + living.getName());
        sendInfo(sender, "§e元素属性: " + (element != null ? element.getColoredName() : "§7无"));
        
        if (element != null) {
            sendInfo(sender, "§e描述: §7" + element.getDescription());
        }
        
        sendInfo(sender, "§b=================================");
    }

    /**
     * 处理冷却查询
     */
    private void handleCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "请指定玩家名！");
            sendInfo(sender, "用法: /skilldebug cooldown <玩家>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendError(sender, "玩家不在线: " + args[1]);
            return;
        }

        Map<String, Integer> cooldowns = plugin.getSkillSystem()
                .getCooldownManager().getAllCooldowns(target);

        sendInfo(sender, "§b========== 冷却信息 ==========");
        sendInfo(sender, "§e玩家: §f" + target.getName());
        
        if (cooldowns.isEmpty()) {
            sendInfo(sender, "§7当前无冷却中的功法");
        } else {
            sendInfo(sender, "§e冷却中的功法: §f" + cooldowns.size() + " 个");
            sendInfo(sender, "");
            
            for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
                Skill skill = plugin.getSkillSystem().getSkill(entry.getKey());
                String skillName = skill != null ? skill.getName() : entry.getKey();
                
                // 格式化时间
                String formattedTime = formatTime(entry.getValue());
                sendInfo(sender, "§7• " + skillName + ": §f" + formattedTime);
            }
        }
        
        sendInfo(sender, "§b============================");
    }

    /**
     * 处理缓存操作
     */
    private void handleCache(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "请指定操作！");
            sendInfo(sender, "用法: /skilldebug cache <clear|info>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "clear" -> {
                ElementalAttribute.clearAllCache();
                sendSuccess(sender, "已清除所有元素缓存！");
            }
            case "info" -> {
                int elementCache = ElementalAttribute.getCacheSize();
                sendInfo(sender, "§b========== 缓存信息 ==========");
                sendInfo(sender, "§e元素缓存: §f" + elementCache + " 个实体");
                sendInfo(sender, "§b============================");
            }
            default -> {
                sendError(sender, "未知的操作: " + args[1]);
                sendInfo(sender, "用法: /skilldebug cache <clear|info>");
            }
        }
    }

    /**
     * 处理测试命令
     */
    private void handleTest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendError(sender, "此命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            sendError(sender, "请指定测试类型！");
            sendInfo(sender, "用法: /skilldebug test <elemental>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "elemental", "element" -> testElemental(player);
            default -> {
                sendError(sender, "未知的测试类型: " + args[1]);
                sendInfo(sender, "可用测试: elemental");
            }
        }
    }

    /**
     * 测试五行相克系统
     */
    private void testElemental(Player player) {
        sendInfo(player, "§b========== 五行相克测试 ==========");
        
        // 测试所有相克关系
        SkillElement[] elements = {
            SkillElement.FIRE, SkillElement.WATER, SkillElement.WOOD,
            SkillElement.METAL, SkillElement.EARTH
        };

        for (SkillElement skill : elements) {
            for (SkillElement target : elements) {
                double multiplier = skill.getDamageMultiplier(target);
                
                String symbol;
                String color;
                if (multiplier > 1.0) {
                    symbol = "→克→";
                    color = "§a";
                } else if (multiplier < 1.0 && multiplier != 0.8) {
                    symbol = "→生→";
                    color = "§c";
                } else if (multiplier == 0.8) {
                    symbol = "=同=";
                    color = "§7";
                } else {
                    continue; // 跳过无关系的
                }
                
                sendInfo(player, color + skill.getDisplayName() + " " + symbol + " " + 
                         target.getDisplayName() + " §f×" + multiplier);
            }
        }
        
        sendInfo(player, "§b================================");
    }

    /**
     * 格式化时间
     */
    private String formatTime(int seconds) {
        if (seconds >= 60) {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            return String.format("%d分%d秒", minutes, secs);
        }
        return seconds + "秒";
    }
}


