package com.xiancore.commands.sub.impl;

import com.xiancore.XianCore;
import com.xiancore.commands.sub.AbstractSubCommand;
import com.xiancore.systems.forge.items.Embryo;
import com.xiancore.systems.forge.items.EmbryoFactory;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.items.SkillBookFactory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 给予物品命令
 * /xiancore give <玩家> <类型> <参数>
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class GiveCommand extends AbstractSubCommand {

    public GiveCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"给予"};
    }

    @Override
    public String getPermission() {
        return "xiancore.give";
    }

    @Override
    public String getUsage() {
        return "/xiancore give <玩家> <类型> <参数>";
    }

    @Override
    public String getDescription() {
        return "给予玩家物品";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!checkPermission(sender)) {
            return;
        }

        // args[0] = 玩家名, args[1] = 类型, args[2] = 参数
        if (args.length < 3) {
            sendError(sender, "用法: " + getUsage());
            sendInfo(sender, "类型: embryo, skillbook");
            sendInfo(sender, "示例: /xiancore give Player embryo 仙品");
            sendInfo(sender, "示例: /xiancore give Player skillbook fireball");
            return;
        }

        String playerName = args[0];
        String itemType = args[1];
        String param = args[2];

        Player target = getOnlinePlayer(sender, playerName);
        if (target == null) {
            return;
        }

        switch (itemType.toLowerCase()) {
            case "embryo":
            case "胚胎":
                giveEmbryo(sender, target, param);
                break;

            case "skillbook":
            case "秘籍":
                giveSkillBook(sender, target, param);
                break;

            default:
                sendError(sender, "未知的物品类型: " + itemType);
                sendInfo(sender, "可用类型: embryo, skillbook");
                break;
        }
    }

    /**
     * 给予仙家胚胎
     */
    private void giveEmbryo(CommandSender sender, Player target, String quality) {
        try {
            // 生成仙家胚胎
            Embryo embryo;
            if (quality.equalsIgnoreCase("random")) {
                embryo = EmbryoFactory.randomGenerate();
            } else {
                embryo = EmbryoFactory.generateByQuality(quality);
            }

            // 给予玩家
            target.getInventory().addItem(embryo.toItemStack());

            sendSuccess(sender, "已给予 " + target.getName() + " 一个 " + embryo.getQuality() + " 品质的仙家胚胎");
            sendSuccess(target, "你获得了一个 " + embryo.getQuality() + " 品质的仙家胚胎!");

            plugin.getLogger().info(sender.getName() + " 给予 " + target.getName() + " 仙家胚胎 (品质: " + embryo.getQuality() + ")");
        } catch (Exception e) {
            sendError(sender, "生成胚胎失败: " + e.getMessage());
            plugin.getLogger().severe("生成仙家胚胎失败: " + e.getMessage());
        }
    }

    /**
     * 给予功法秘籍
     */
    private void giveSkillBook(CommandSender sender, Player target, String skillId) {
        try {
            // 获取功法
            Skill skill = plugin.getSkillSystem().getSkill(skillId);
            if (skill == null) {
                sendError(sender, "功法不存在: " + skillId);
                sendInfo(sender, "使用 /skill list 查看可用功法");
                return;
            }

            // 生成功法秘籍
            ItemStack skillBook = SkillBookFactory.createSkillBook(skill);

            // 给予玩家
            target.getInventory().addItem(skillBook);

            sendSuccess(sender, "已给予 " + target.getName() + " §r功法秘籍: " + skill.getName());
            sendSuccess(target, "你获得了功法秘籍: " + skill.getName());

            plugin.getLogger().info(sender.getName() + " 给予 " + target.getName() + " 功法秘籍 (" + skill.getName() + ")");
        } catch (Exception e) {
            sendError(sender, "生成秘籍失败: " + e.getMessage());
            plugin.getLogger().severe("生成功法秘籍失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 玩家名
            return filterTabComplete(getOnlinePlayerNames(), args[0]);
        }

        if (args.length == 2) {
            // 物品类型
            return filterTabComplete(Arrays.asList("embryo", "skillbook"), args[1]);
        }

        if (args.length == 3) {
            String itemType = args[1].toLowerCase();
            if (itemType.equals("embryo") || itemType.equals("胚胎")) {
                // 胚胎品质
                return filterTabComplete(Arrays.asList("凡品", "灵品", "仙品", "神品", "random"), args[2]);
            } else if (itemType.equals("skillbook") || itemType.equals("秘籍")) {
                // 功法ID
                List<String> skillIds = new ArrayList<>();
                for (Skill skill : plugin.getSkillSystem().getAllSkills().values()) {
                    skillIds.add(skill.getId());
                }
                return filterTabComplete(skillIds, args[2]);
            }
        }

        return new ArrayList<>();
    }
}
