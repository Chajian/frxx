package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.SpiritualRootType;
import com.xiancore.systems.forge.items.Embryo;
import com.xiancore.systems.forge.items.EmbryoFactory;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.items.SkillBookFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * XianCore 主命令处理器
 * 处理 /xiancore 命令
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class XianCoreCommand extends BaseCommand {

    public XianCoreCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        // 如果没有参数，显示帮助
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
            case "帮助":
                showHelp(sender);
                break;

            case "reload":
            case "重载":
                handleReload(sender);
                break;

            case "info":
            case "信息":
                handleInfo(sender);
                break;

            case "debug":
            case "调试":
                handleDebug(sender);
                break;

            case "give":
            case "给予":
                handleGive(sender, args);
                break;

            case "shop":
            case "activeqi":
            case "商店":
            case "活跃灵气":
                handleShop(sender);
                break;

            case "fixlevel":
            case "修复等级":
                handleFixLevel(sender, args);
                break;

            case "fixsect":
            case "修复宗门":
                handleFixSect(sender, args);
                break;

            case "player":
            case "玩家":
                handlePlayer(sender, args);
                break;

            case "migrate":
            case "迁移":
                handleMigrate(sender, args);
                break;

            default:
                sendError(sender, "未知的子命令: " + subCommand);
                sendInfo(sender, "使用 /xiancore help 查看帮助");
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 第一层子命令
            completions.add("help");
            completions.add("reload");
            completions.add("info");
            completions.add("debug");
            completions.add("give");
            completions.add("shop");
            completions.add("fixlevel");
            completions.add("fixsect");
            completions.add("player");
            completions.add("migrate");
            return filterTabComplete(completions, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // give 命令的玩家列表
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            return filterTabComplete(completions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // give 命令的物品类型
            completions.add("embryo");
            completions.add("skillbook");
            return filterTabComplete(completions, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give") && args[2].equalsIgnoreCase("embryo")) {
            // embryo 的品质
            completions.addAll(Arrays.asList("凡品", "灵品", "宝品", "仙品", "神品", "random"));
            return filterTabComplete(completions, args[3]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give") && args[2].equalsIgnoreCase("skillbook")) {
            // skillbook 的功法ID
            completions.addAll(plugin.getSkillSystem().getAllSkills().keySet());
            return filterTabComplete(completions, args[3]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            // player 命令的操作
            completions.addAll(Arrays.asList("info", "set", "reset", "list"));
            return filterTabComplete(completions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("player") &&
            (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("reset"))) {
            // player 命令的玩家列表（支持离线玩家）
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            return filterTabComplete(completions, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("player") && args[1].equalsIgnoreCase("set")) {
            // player set 命令的字段
            completions.addAll(Arrays.asList("qi", "realm", "spiritStones", "level", "contribution", "spiritualRoot", "rootType"));
            return filterTabComplete(completions, args[3]);
        }
        
        if (args.length == 5 && args[0].equalsIgnoreCase("player") && args[1].equalsIgnoreCase("set") && 
            args[3].equalsIgnoreCase("rootType")) {
            // rootType 的灵根类型列表
            for (SpiritualRootType type : SpiritualRootType.values()) {
                completions.add(type.name());
                completions.add(type.getFullName());
            }
            return filterTabComplete(completions, args[4]);
        }

        return completions;
    }

    @Override
    protected boolean requiresPlayer() {
        return false; // 主命令控制台也可以执行
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== XianCore 命令帮助 ==========");
        sendInfo(sender, "§e/xiancore help §7- 显示此帮助信息");
        sendInfo(sender, "§e/xiancore reload §7- 重载配置文件");
        sendInfo(sender, "§e/xiancore info §7- 显示插件信息");
        sendInfo(sender, "§e/xiancore debug §7- 切换调试模式");
        sendInfo(sender, "§e/xiancore shop §7- 打开活跃灵气商店");
        sendInfo(sender, "§e/xiancore give <玩家> embryo <品质> §7- 给予仙家胚胎");
        sendInfo(sender, "§e/xiancore give <玩家> skillbook <功法ID> §7- 给予功法秘籍");
        sendInfo(sender, "§e/xiancore fixsect [玩家] §7- 修复宗门数据不一致");
        sendInfo(sender, "§e/xiancore player <操作> §7- 管理玩家数据");
        sendInfo(sender, "§e/xiancore migrate [--dry-run|--info] §7- YML迁移到MySQL");
        sendInfo(sender, "§b=====================================");
    }

    /**
     * 处理重载命令
     */
    private void handleReload(CommandSender sender) {
        if (!hasPermission(sender, "xiancore.reload")) {
            return;
        }

        try {
            long start = System.currentTimeMillis();
            plugin.reloadConfigs();
            long elapsed = System.currentTimeMillis() - start;

            sendSuccess(sender, "配置已重载! 耗时: " + elapsed + "ms");
            plugin.getLogger().info(sender.getName() + " 重载了配置文件");
        } catch (Exception e) {
            sendError(sender, "配置重载失败: " + e.getMessage());
            plugin.getLogger().severe("配置重载失败: " + e.getMessage());
        }
    }

    /**
     * 处理信息命令
     */
    private void handleInfo(CommandSender sender) {
        sendInfo(sender, "§b========== XianCore 插件信息 ==========");
        sendInfo(sender, "§e版本: §f" + plugin.getDescription().getVersion());
        sendInfo(sender, "§e作者: §f" + plugin.getDescription().getAuthors());
        sendInfo(sender, "§e描述: §f" + plugin.getDescription().getDescription());

        // 显示各系统状态
        sendInfo(sender, "§b========== 系统状态 ==========");
        sendInfo(sender, "§e修炼系统: §a已加载");
        sendInfo(sender, "§e炼器系统: §a已加载");
        sendInfo(sender, "§e奇遇系统: §a已加载");
        sendInfo(sender, "§e宗门系统: §a已加载");
        sendInfo(sender, "§e功法系统: §a已加载");
        sendInfo(sender, "§e天劫系统: §a已加载");

        // 显示集成状态
        sendInfo(sender, "§b========== 集成状态 ==========");
        sendInfo(sender, "§eMythicMobs: " + (Bukkit.getPluginManager().getPlugin("MythicMobs") != null ? "§a已连接" : "§c未安装"));
        sendInfo(sender, "§eVault: " + (Bukkit.getPluginManager().getPlugin("Vault") != null ? "§a已连接" : "§c未安装"));
        sendInfo(sender, "§b=====================================");
    }

    /**
     * 处理调试命令
     */
    private void handleDebug(CommandSender sender) {
        if (!hasPermission(sender, "xiancore.debug")) {
            return;
        }

        // 切换调试模式
        boolean currentDebugMode = plugin.isDebugMode();
        boolean newDebugMode = !currentDebugMode;

        plugin.setDebugMode(newDebugMode);

        if (newDebugMode) {
            sendSuccess(sender, "§a调试模式已§l启用§r");
            sendInfo(sender, "§7日志级别已提升至 DEBUG");
            sendInfo(sender, "§7将显示详细的系统日志和性能信息");
        } else {
            sendSuccess(sender, "§c调试模式已§l禁用§r");
            sendInfo(sender, "§7日志级别已恢复至 INFO");
            sendInfo(sender, "§7将隐藏详细日志");
        }

        // 全管理员广播
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("xiancore.debug")) {
                player.sendMessage("§6[调试模式]§r " + sender.getName() + " 已" + (newDebugMode ? "启用" : "禁用") + "调试模式");
            }
        }
    }

    /**
     * 处理商店命令
     */
    private void handleShop(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendError(sender, "此命令只能由玩家执行!");
            return;
        }

        if (!hasPermission(sender, "xiancore.shop")) {
            return;
        }

        Player player = (Player) sender;
        plugin.getGuiManager().getActiveQiShopGUI().open(player);
    }

    /**
     * 处理给予物品命令
     * /xiancore give <玩家> embryo <品质>
     * /xiancore give <玩家> skillbook <功法ID>
     */
    private void handleGive(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "xiancore.give")) {
            return;
        }

        if (args.length < 4) {
            sendError(sender, "用法: /xiancore give <玩家> <类型> <参数>");
            sendInfo(sender, "类型: embryo, skillbook");
            sendInfo(sender, "示例: /xiancore give Player embryo 仙品");
            sendInfo(sender, "示例: /xiancore give Player skillbook fireball");
            return;
        }

        String playerName = args[1];
        String itemType = args[2];
        String param = args[3];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sendError(sender, "玩家 " + playerName + " 不在线或不存在!");
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
            org.bukkit.inventory.ItemStack skillBook = SkillBookFactory.createSkillBook(skill);

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

    /**
     * 修复玩家等级
     * 根据境界和境界阶段计算正确的玩家等级
     */
    private void handleFixLevel(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("xiancore.admin.fixlevel")) {
            sendError(sender, "你没有权限使用此命令!");
            return;
        }

        Player target;

        if (args.length < 2) {
            // 修复自己
            if (!(sender instanceof Player)) {
                sendError(sender, "控制台必须指定玩家名称!");
                sendInfo(sender, "用法: /xiancore fixlevel <玩家名>");
                return;
            }
            target = (Player) sender;
        } else {
            // 修复指定玩家
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendError(sender, "玩家不在线: " + args[1]);
                return;
            }
        }

        // 加载玩家数据
        com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(target.getUniqueId());
        if (data == null) {
            sendError(sender, "加载玩家数据失败!");
            return;
        }

        // 计算正确的等级
        int correctLevel = calculateLevelByRealm(data.getRealm(), data.getRealmStage());
        int oldLevel = data.getLevel();

        // 更新等级
        data.setPlayerLevel(correctLevel);
        plugin.getDataManager().savePlayerData(data);

        // 成功消息
        sendSuccess(sender, "§a§l========== 等级修复成功 ==========");
        sendSuccess(sender, "§e玩家: §f" + target.getName());
        sendSuccess(sender, "§e境界: §f" + data.getFullRealmName());
        sendSuccess(sender, "§e等级: §f" + oldLevel + " §a→ §f" + correctLevel);
        sendSuccess(sender, "§a§l================================");

        if (target != sender) {
            sendSuccess(target, "§a你的等级已被修复为 §e" + correctLevel + " §a级 (境界: " + data.getFullRealmName() + ")");
        }

        plugin.getLogger().info(sender.getName() + " 修复了 " + target.getName() + " 的等级: " + oldLevel + " → " + correctLevel);
    }

    /**
     * 根据境界和境界阶段计算应有的玩家等级
     *
     * 等级计算规则：
     * - 起始等级：1
     * - 小境界突破（初→中，中→后）：+5级
     * - 大境界突破（后→下一境界初）：+15级
     *
     * @param realm 境界
     * @param realmStage 境界阶段 (1=初期, 2=中期, 3=后期)
     * @return 应有的等级
     */
    private int calculateLevelByRealm(String realm, int realmStage) {
        // 基础等级 = 1
        int level = 1;

        // 计算大境界的等级
        switch (realm) {
            case "炼气期" -> level = 1;
            case "筑基期" -> level = 1 + 5 + 5 + 15;      // 炼气初→中(+5)→后(+5)→筑基初(+15) = 26
            case "结丹期" -> level = 26 + 5 + 5 + 15;     // +筑基中(+5)→后(+5)→结丹初(+15) = 51
            case "元婴期" -> level = 51 + 5 + 5 + 15;     // +结丹中(+5)→后(+5)→元婴初(+15) = 76
            case "化神期" -> level = 76 + 5 + 5 + 15;     // +元婴中(+5)→后(+5)→化神初(+15) = 101
            case "炼虚期" -> level = 101 + 5 + 5 + 15;    // +化神中(+5)→后(+5)→炼虚初(+15) = 126
            case "合体期" -> level = 126 + 5 + 5 + 15;    // +炼虚中(+5)→后(+5)→合体初(+15) = 151
            case "大乘期" -> level = 151 + 5 + 5 + 15;    // +合体中(+5)→后(+5)→大乘初(+15) = 176
            default -> level = 1;
        }

        // 加上小境界阶段的等级
        // 初期 = 0, 中期 = +5, 后期 = +10
        level += (realmStage - 1) * 5;

        return level;
    }

    /**
     * 修复宗门数据不一致问题
     * 当玩家数据库中的 sectId 与内存中的宗门系统不一致时使用此命令
     */
    private void handleFixSect(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("xiancore.admin.fixsect")) {
            sendError(sender, "你没有权限使用此命令!");
            return;
        }

        // 检查是否为批量操作
        if (args.length >= 2 && args[1].equalsIgnoreCase("--all")) {
            handleFixSectAll(sender);
            return;
        }

        // 检查是否为检查模式（不修复，只检查）
        if (args.length >= 2 && args[1].equalsIgnoreCase("--check")) {
            handleCheckSectConsistency(sender);
            return;
        }

        Player target;

        if (args.length < 2) {
            // 修复自己
            if (!(sender instanceof Player)) {
                sendError(sender, "控制台必须指定玩家名称!");
                sendInfo(sender, "用法: /xiancore fixsect <玩家名>");
                sendInfo(sender, "      /xiancore fixsect --all   (批量修复所有玩家)");
                sendInfo(sender, "      /xiancore fixsect --check (仅检查不修复)");
                return;
            }
            target = (Player) sender;
        } else {
            // 修复指定玩家
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendError(sender, "玩家不在线: " + args[1]);
                return;
            }
        }

        // 加载玩家数据
        com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(target.getUniqueId());
        if (data == null) {
            sendError(sender, "加载玩家数据失败!");
            return;
        }

        // 从内存中获取玩家的宗门
        com.xiancore.systems.sect.Sect memSect = plugin.getSectSystem().getPlayerSect(target.getUniqueId());
        Integer dbSectId = data.getSectId();

        sendInfo(sender, "§b========== 宗门数据检测 ==========");
        sendInfo(sender, "§e玩家: §f" + target.getName());
        sendInfo(sender, "§e数据库中的宗门ID: §f" + (dbSectId != null ? dbSectId : "无"));
        sendInfo(sender, "§e内存中的宗门: §f" + (memSect != null ? memSect.getName() + " (ID: " + memSect.getId() + ")" : "无"));

        // 检查是否不一致
        if (memSect == null && dbSectId == null) {
            sendInfo(sender, "§a数据一致：玩家未加入任何宗门");
            sendInfo(sender, "§b================================");
            return;
        }

        if (memSect != null && dbSectId != null && memSect.getId() == dbSectId) {
            sendInfo(sender, "§a数据一致：玩家已加入宗门 " + memSect.getName());
            sendInfo(sender, "§b================================");
            return;
        }

        // 数据不一致，需要修复
        sendWarning(sender, "§c检测到数据不一致!");

        if (memSect == null && dbSectId != null) {
            // 内存中没有宗门，但数据库有 sectId
            sendInfo(sender, "§7问题：数据库中有宗门记录，但内存中找不到");
            sendInfo(sender, "§7正在清理数据库中的宗门数据...");

            data.setSectId(null);
            data.setSectRank("member");
            data.setContributionPoints(0);
            plugin.getDataManager().savePlayerData(data);

            sendSuccess(sender, "§a已清除玩家的宗门数据");
        } else if (memSect != null && dbSectId == null) {
            // 内存中有宗门，但数据库没有 sectId
            sendInfo(sender, "§7问题：内存中有宗门记录，但数据库没有");
            sendInfo(sender, "§7正在同步数据到数据库...");

            data.setSectId(memSect.getId());
            com.xiancore.systems.sect.SectMember member = memSect.getMember(target.getUniqueId());
            if (member != null) {
                data.setSectRank(member.getRank().name());
            }
            plugin.getDataManager().savePlayerData(data);

            sendSuccess(sender, "§a已同步宗门数据到数据库");
        } else {
            // 宗门ID不匹配
            sendInfo(sender, "§7问题：宗门ID不匹配");
            sendInfo(sender, "§7正在同步内存数据到数据库...");

            data.setSectId(memSect.getId());
            com.xiancore.systems.sect.SectMember member = memSect.getMember(target.getUniqueId());
            if (member != null) {
                data.setSectRank(member.getRank().name());
            }
            plugin.getDataManager().savePlayerData(data);

            sendSuccess(sender, "§a已同步宗门数据");
        }

        sendSuccess(sender, "§a§l========== 修复完成 ==========");
        sendSuccess(sender, "§e最终状态:");
        sendSuccess(sender, "§e  宗门ID: §f" + data.getSectId());
        sendSuccess(sender, "§e  职位: " + com.xiancore.systems.sect.SectRank.getColoredDisplayName(data.getSectRank()));
        sendSuccess(sender, "§a§l============================");

        if (target != sender) {
            sendSuccess(target, "§a你的宗门数据已被管理员修复");
        }

        plugin.getLogger().info(sender.getName() + " 修复了 " + target.getName() + " 的宗门数据不一致问题");
    }

    /**
     * 批量修复所有玩家的宗门数据
     */
    private void handleFixSectAll(CommandSender sender) {
        sendInfo(sender, "§e§l========== 批量修复宗门数据 ==========");
        sendInfo(sender, "§7正在扫描所有宗门成员数据...");

        int totalChecked = 0;
        int fixedCount = 0;
        int errorCount = 0;

        // 遍历所有宗门的所有成员
        for (com.xiancore.systems.sect.Sect sect : plugin.getSectSystem().getAllSects()) {
            for (com.xiancore.systems.sect.SectMember member : sect.getMemberList()) {
                totalChecked++;

                try {
                    // 加载玩家数据
                    com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(member.getPlayerId());
                    if (data == null) {
                        sendWarning(sender, "§c玩家数据不存在: " + member.getPlayerName() + " (UUID: " + member.getPlayerId() + ")");
                        errorCount++;
                        continue;
                    }

                    // 检查是否不一致
                    Integer dbSectId = data.getSectId();
                    if (dbSectId == null || !dbSectId.equals(sect.getId())) {
                        sendInfo(sender, "§e修复: " + member.getPlayerName() + " (宗门: " + sect.getName() + ")");

                        // 修复数据
                        data.setSectId(sect.getId());
                        data.setSectRank(member.getRank().name());
                        plugin.getDataManager().savePlayerData(data);

                        fixedCount++;
                    }
                } catch (Exception e) {
                    sendError(sender, "§c处理失败: " + member.getPlayerName() + " - " + e.getMessage());
                    errorCount++;
                }
            }
        }

        sendInfo(sender, "§a§l========== 批量修复完成 ==========");
        sendInfo(sender, "§e总计检查: §f" + totalChecked + " §e个成员");
        sendInfo(sender, "§a成功修复: §f" + fixedCount + " §a条记录");
        if (errorCount > 0) {
            sendWarning(sender, "§c错误数量: §f" + errorCount);
        }
        sendInfo(sender, "§a§l=================================");

        plugin.getLogger().info(sender.getName() + " 执行了批量宗门数据修复，修复了 " + fixedCount + " 条记录");
    }

    /**
     * 检查宗门数据一致性（不修复）
     */
    private void handleCheckSectConsistency(CommandSender sender) {
        sendInfo(sender, "§e§l========== 宗门数据一致性检查 ==========");
        sendInfo(sender, "§7正在检查所有宗门成员数据...");

        int totalChecked = 0;
        int inconsistentCount = 0;
        java.util.List<String> inconsistentPlayers = new java.util.ArrayList<>();

        // 遍历所有宗门的所有成员
        for (com.xiancore.systems.sect.Sect sect : plugin.getSectSystem().getAllSects()) {
            for (com.xiancore.systems.sect.SectMember member : sect.getMemberList()) {
                totalChecked++;

                try {
                    // 加载玩家数据
                    com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(member.getPlayerId());
                    if (data == null) {
                        inconsistentPlayers.add("§c" + member.getPlayerName() + " §7(数据不存在)");
                        inconsistentCount++;
                        continue;
                    }

                    // 检查是否不一致
                    Integer dbSectId = data.getSectId();
                    if (dbSectId == null || !dbSectId.equals(sect.getId())) {
                        String issue = "§e" + member.getPlayerName() + " §7(宗门: " + sect.getName() + ", DB宗门ID: " + dbSectId + ")";
                        inconsistentPlayers.add(issue);
                        inconsistentCount++;
                    }
                } catch (Exception e) {
                    inconsistentPlayers.add("§c" + member.getPlayerName() + " §7(检查失败: " + e.getMessage() + ")");
                    inconsistentCount++;
                }
            }
        }

        sendInfo(sender, "§a§l========== 检查完成 ==========");
        sendInfo(sender, "§e总计检查: §f" + totalChecked + " §e个成员");

        if (inconsistentCount == 0) {
            sendSuccess(sender, "§a✓ 所有数据一致，未发现问题");
        } else {
            sendWarning(sender, "§c发现 " + inconsistentCount + " 个数据不一致:");
            for (String player : inconsistentPlayers) {
                sendInfo(sender, "  " + player);
            }
            sendInfo(sender, "");
            sendInfo(sender, "§e使用 §f/xiancore fixsect --all §e来批量修复");
        }
        sendInfo(sender, "§a§l==============================");
    }

    /**
     * 处理玩家数据管理
     * /xiancore player <操作> [玩家] [参数...]
     */
    private void handlePlayer(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("xiancore.admin.player")) {
            sendError(sender, "你没有权限使用此命令!");
            return;
        }

        if (args.length < 2) {
            sendError(sender, "用法: /xiancore player <操作> [参数]");
            sendInfo(sender, "操作:");
            sendInfo(sender, "  §einfo <玩家> §7- 查看玩家数据");
            sendInfo(sender, "  §eset <玩家> <字段> <值> §7- 设置玩家数据");
            sendInfo(sender, "  §ereset <玩家> §7- 重置玩家数据");
            sendInfo(sender, "  §elist §7- 列出所有在线玩家");
            return;
        }

        String operation = args[1].toLowerCase();

        switch (operation) {
            case "info":
                handlePlayerInfo(sender, args);
                break;
            case "set":
                handlePlayerSet(sender, args);
                break;
            case "reset":
                handlePlayerReset(sender, args);
                break;
            case "list":
                handlePlayerList(sender);
                break;
            default:
                sendError(sender, "未知操作: " + operation);
                break;
        }
    }

    /**
     * 查看玩家信息
     */
    private void handlePlayerInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "用法: /xiancore player info <玩家>");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sendError(sender, "玩家不在线: " + args[2]);
            return;
        }

        com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(target.getUniqueId());
        if (data == null) {
            sendError(sender, "加载玩家数据失败!");
            return;
        }

        sendInfo(sender, "§b========== 玩家数据 ==========");
        sendInfo(sender, "§e玩家: §f" + target.getName());
        sendInfo(sender, "§eUUID: §7" + target.getUniqueId());
        sendInfo(sender, "§e境界: §f" + data.getFullRealmName());
        sendInfo(sender, "§e修为: §f" + data.getQi());
        sendInfo(sender, "§e灵根: " + data.getSpiritualRootDisplay());
        sendInfo(sender, "§e五行: " + data.getSpiritualRootElements());
        sendInfo(sender, "§e灵根值: §f" + String.format("%.3f", data.getSpiritualRoot()));
        sendInfo(sender, "§e灵石: §6" + data.getSpiritStones());
        sendInfo(sender, "§e等级: §f" + data.getLevel());
        sendInfo(sender, "§e宗门ID: §f" + (data.getSectId() != null ? data.getSectId() : "无"));
        sendInfo(sender, "§e职位: " + com.xiancore.systems.sect.SectRank.getColoredDisplayName(data.getSectRank()));
        sendInfo(sender, "§e贡献: §f" + data.getContributionPoints());
        sendInfo(sender, "§e活跃灵气: §b" + data.getActiveQi());
        sendInfo(sender, "§b============================");
    }

    /**
     * 设置玩家数据
     */
    private void handlePlayerSet(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sendError(sender, "用法: /xiancore player set <玩家> <字段> <值>");
            sendInfo(sender, "可用字段: qi, realm, spiritStones, level, contribution, activeQi, spiritualRoot, rootType");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sendError(sender, "玩家不在线: " + args[2]);
            return;
        }

        com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(target.getUniqueId());
        if (data == null) {
            sendError(sender, "加载玩家数据失败!");
            return;
        }

        String field = args[3].toLowerCase();
        String value = args[4];

        try {
            switch (field) {
                case "qi":
                case "修为":
                    long qi = Long.parseLong(value);
                    data.setQi(qi);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的修为为: " + qi);
                    break;

                case "realm":
                case "境界":
                    data.setRealm(value);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的境界为: " + value);
                    break;

                case "spiritstones":
                case "spiritstone":
                case "灵石":
                    long stones = Long.parseLong(value);
                    data.setSpiritStones(stones);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的灵石为: " + stones);
                    break;

                case "level":
                case "等级":
                    int level = Integer.parseInt(value);
                    data.setPlayerLevel(level);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的等级为: " + level);
                    break;

                case "contribution":
                case "贡献":
                    int contribution = Integer.parseInt(value);
                    data.setContributionPoints(contribution);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的贡献为: " + contribution);
                    break;

                case "activeqi":
                case "活跃灵气":
                    long activeQi = Long.parseLong(value);
                    data.setActiveQi(activeQi);
                    sendSuccess(sender, "已设置 " + target.getName() + " 的活跃灵气为: " + activeQi);
                    break;
                    
                case "spiritualroot":
                case "灵根值":
                    double spiritualRoot = Double.parseDouble(value);
                    if (spiritualRoot < 0.0 || spiritualRoot > 1.0) {
                        sendError(sender, "灵根值必须在 0.0 到 1.0 之间");
                        return;
                    }
                    data.setSpiritualRoot(spiritualRoot);
                    // 根据新的灵根值更新灵根类型
                    data.setSpiritualRootType(SpiritualRootType.fromValue(spiritualRoot));
                    sendSuccess(sender, "已设置 " + target.getName() + " 的灵根值为: " + String.format("%.3f", spiritualRoot));
                    sendInfo(sender, "灵根类型: " + data.getSpiritualRootDisplay());
                    break;
                    
                case "roottype":
                case "灵根类型":
                    SpiritualRootType rootType = SpiritualRootType.fromName(value);
                    if (rootType == null) {
                        // 尝试通过枚举名称查找
                        try {
                            rootType = SpiritualRootType.valueOf(value.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            sendError(sender, "未知的灵根类型: " + value);
                            sendInfo(sender, "可用类型示例: HEAVENLY_FIRE, VARIANT_METAL_WOOD, TRUE_THREE_1, MIXED_FIVE");
                            sendInfo(sender, "或使用中文名称: 纯火灵根, 金木双灵根, 金木水三灵根, 杂灵根");
                            return;
                        }
                    }
                    
                    data.setSpiritualRootType(rootType);
                    // 同时设置灵根值为该类型的中间值
                    double newRootValue = (rootType.getMinValue() + rootType.getMaxValue()) / 2.0;
                    data.setSpiritualRoot(newRootValue);
                    
                    sendSuccess(sender, "§a已设置 " + target.getName() + " 的灵根类型");
                    sendInfo(sender, "灵根: " + data.getSpiritualRootDisplay());
                    sendInfo(sender, "五行属性: " + data.getSpiritualRootElements());
                    sendInfo(sender, "灵根值: " + String.format("%.3f", newRootValue));
                    
                    // 通知玩家
                    if (target != sender) {
                        target.sendMessage("§b========================================");
                        target.sendMessage("§6§l你的灵根已被管理员修改！");
                        target.sendMessage("");
                        target.sendMessage("§e新灵根: " + data.getSpiritualRootDisplay());
                        target.sendMessage("§e五行属性: " + data.getSpiritualRootElements());
                        target.sendMessage("§e灵根值: §f" + String.format("%.1f%%", newRootValue * 100));
                        target.sendMessage("");
                        target.sendMessage(data.getSpiritualRootDescription());
                        target.sendMessage("§b========================================");
                    }
                    break;

                default:
                    sendError(sender, "未知字段: " + field);
                    sendInfo(sender, "可用字段: qi, realm, spiritStones, level, contribution, activeQi, spiritualRoot, rootType");
                    return;
            }

            // 保存数据
            plugin.getDataManager().savePlayerData(data);
            plugin.getLogger().info(sender.getName() + " 设置了 " + target.getName() + " 的 " + field + " 为 " + value);

        } catch (NumberFormatException e) {
            sendError(sender, "无效的数值: " + value);
        }
    }

    /**
     * 重置玩家数据
     */
    private void handlePlayerReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "用法: /xiancore player reset <玩家>");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sendError(sender, "玩家不在线: " + args[2]);
            return;
        }

        // 确认操作
        sendWarning(sender, "§c警告：此操作将重置 " + target.getName() + " 的所有数据!");
        sendWarning(sender, "§c如需确认，请再次执行此命令（功能待完善）");

        // TODO: 实现确认机制和重置逻辑
        sendInfo(sender, "§7此功能暂未完全实现");
    }

    /**
     * 列出所有在线玩家
     */
    private void handlePlayerList(CommandSender sender) {
        java.util.Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        sendInfo(sender, "§b========== 在线玩家列表 ==========");
        sendInfo(sender, "§e总计: §f" + players.size() + " 人");
        sendInfo(sender, "");

        int count = 0;
        for (Player player : players) {
            com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
            if (data != null) {
                count++;
                sendInfo(sender, String.format("§f%d. §e%s §7- §f%s §7灵石: §6%d",
                    count,
                    player.getName(),
                    data.getFullRealmName(),
                    data.getSpiritStones()
                ));
            }
        }

        sendInfo(sender, "§b================================");
    }

    /**
     * 处理数据迁移命令
     * /xiancore migrate [--dry-run|--info]
     */
    private void handleMigrate(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("xiancore.admin.migrate")) {
            sendError(sender, "你没有权限使用此命令！");
            return;
        }

        // 创建迁移工具
        com.xiancore.core.data.DataMigrationTool migrationTool = 
            new com.xiancore.core.data.DataMigrationTool(plugin);

        // 解析参数
        final boolean dryRun;
        boolean showInfo = false;

        if (args.length >= 2) {
            String param = args[1].toLowerCase();
            if (param.equals("--dry-run") || param.equals("-d")) {
                dryRun = true;
            } else if (param.equals("--info") || param.equals("-i")) {
                showInfo = true;
                dryRun = false;
            } else {
                dryRun = false;
            }
        } else {
            dryRun = false;
        }

        // 显示信息模式
        if (showInfo) {
            String summary = migrationTool.getPreMigrationSummary();
            for (String line : summary.split("\n")) {
                sender.sendMessage(line);
            }
            return;
        }

        // 确认提示
        if (!dryRun && args.length < 2) {
            sendWarning(sender, "§e§l警告: 此操作将把YML数据迁移到MySQL！");
            sendInfo(sender, "§7使用 §f/xiancore migrate --info §7查看详情");
            sendInfo(sender, "§7使用 §f/xiancore migrate --dry-run §7预览迁移");
            sendInfo(sender, "§7使用 §f/xiancore migrate confirm §7确认并执行迁移");
            return;
        }

        boolean confirmed = args.length >= 2 && args[1].equalsIgnoreCase("confirm");
        if (!dryRun && !confirmed) {
            sendError(sender, "请使用 --dry-run 预览或 confirm 确认执行");
            return;
        }

        // 执行迁移
        if (dryRun) {
            sendInfo(sender, "§e正在执行预览迁移（不写入数据库）...");
        } else {
            sendWarning(sender, "§c§l正在执行真实迁移，请勿关闭服务器！");
        }

        // 异步执行迁移
        migrationTool.migrateAsync(dryRun).thenAccept(report -> {
            // 在主线程显示结果
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String reportText = report.generateReport();
                for (String line : reportText.split("\n")) {
                    sender.sendMessage(line);
                }

                // 如果是预览模式，提示如何执行真实迁移
                if (dryRun && report.getSuccessCount() > 0) {
                    sendInfo(sender, "");
                    sendSuccess(sender, "§a预览完成！使用以下命令执行真实迁移:");
                    sendInfo(sender, "§e/xiancore migrate confirm");
                }
            });
        }).exceptionally(throwable -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                sendError(sender, "迁移过程发生错误: " + throwable.getMessage());
                plugin.getLogger().severe("数据迁移失败:");
                throwable.printStackTrace();
            });
            return null;
        });

        sendInfo(sender, "§e迁移任务已在后台启动，请稍候...");
    }
}
