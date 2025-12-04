package com.xiancore.commands;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.gui.CraftingGUI;
import com.xiancore.gui.EmbryoSelectionGUI;
import com.xiancore.gui.EquipmentCraftGUI;
import com.xiancore.gui.EquipmentSelectionGUI;
import com.xiancore.gui.ForgeGUI;
import com.xiancore.systems.forge.items.Embryo;
import com.xiancore.systems.forge.items.EmbryoFactory;
import com.xiancore.systems.forge.items.EmbryoParser;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 炼器命令处理器
 * 处理 /forge 命令
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ForgeCommand extends BaseCommand {

    public ForgeCommand(XianCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // 如果没有参数，打开炼器GUI
        if (args.length == 0) {
            openForgeGUI(player);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "make":
            case "制作":
                handleMake(player);
                break;

            case "craft":
            case "炼制":
                handleCraft(player);
                break;

            case "refine":
            case "精炼":
                handleRefine(player);
                break;

            case "enhance":
            case "强化":
                handleEnhance(player);
                break;

            case "fuse":
            case "融合":
                handleFuse(player);
                break;

            case "info":
            case "信息":
                handleInfo(player);
                break;

            case "name":
            case "命名":
                handleName(player, args);
                break;

            case "help":
            case "帮助":
                showHelp(sender);
                break;

            default:
                sendError(sender, "未知的子命令: " + subCommand);
                sendInfo(sender, "使用 /forge help 查看帮助");
                break;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabComplete(Arrays.asList("make", "craft", "refine", "enhance", "fuse", "info", "name", "help"), args[0]);
        }
        return List.of();
    }

    @Override
    protected void showHelp(CommandSender sender) {
        sendInfo(sender, "§b========== 炼器系统命令帮助 ==========");
        sendInfo(sender, "§e/forge §7- 打开炼器界面");
        sendInfo(sender, "§e/forge make §7- 打开配方炼制界面（使用1-4个材料）");
        sendInfo(sender, "§7  支持多种材料组合炼制装备");
        sendInfo(sender, "§7  自动匹配配方并显示成功率");
        sendInfo(sender, "§e/forge craft §7- 炼制胚胎（需要背包中有矿石材料）");
        sendInfo(sender, "§7  材料价值: §c下界合金(500) §b钻石(100) §a绿宝石(80)");
        sendInfo(sender, "§7            §6金矿(20) §7铁矿(5)");
        sendInfo(sender, "§7  品质要求: §d神品≥5000 §6仙品≥2000 §5宝品≥800");
        sendInfo(sender, "§7            §b灵品≥300 §f凡品<300");
        sendInfo(sender, "§e/forge refine §7- 精炼装备（手持胚胎，选择装备类型）");
        sendInfo(sender, "§e/forge enhance §7- 强化手中的装备");
        sendInfo(sender, "§e/forge fuse §7- 融合装备（主副手各持一件同品质胚胎）");
        sendInfo(sender, "§e/forge info §7- 查看手中物品信息");
        sendInfo(sender, "§e/forge help §7- 显示此帮助");
        sendInfo(sender, "§b====================================");
    }

    /**
     * 打开炼器GUI
     */
    private void openForgeGUI(Player player) {
        if (!hasPermission(player, "xiancore.forge.craft")) {
            return;
        }

        ForgeGUI.open(player, plugin);
    }

    /**
     * 处理制作（打开配方炼制GUI）
     */
    private void handleMake(Player player) {
        if (!hasPermission(player, "xiancore.forge.make")) {
            return;
        }

        CraftingGUI.open(player, plugin);
    }

    /**
     * 处理精炼（将胚胎炼制成装备）
     */
    private void handleRefine(Player player) {
        if (!hasPermission(player, "xiancore.forge.refine")) {
            return;
        }

        // 直接打开胚胎选择GUI
        EmbryoSelectionGUI.open(player, plugin);
    }

    /**
     * 处理炼制
     */
    private void handleCraft(Player player) {
        if (!hasPermission(player, "xiancore.forge.craft")) {
            return;
        }

        // 获取玩家数据
        com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            sendError(player, "数据加载失败!");
            return;
        }

        // 检测玩家背包中的材料
        java.util.Map<Material, Integer> materials = collectMaterials(player);

        if (materials.isEmpty()) {
            sendError(player, "§c背包中没有可用的炼制材料!");
            sendInfo(player, "§e可用材料: §7铁矿石、金矿石、钻石、绿宝石、下界合金");
            return;
        }

        // 计算材料品质和数量
        CraftResult result = calculateCraftQuality(materials);
        String targetQuality = result.quality;
        int materialCount = result.materialCount;
        int baseCost = result.cost;

        // 检查灵石是否足够
        if (data.getSpiritStones() < baseCost) {
            sendError(player, "§c灵石不足! 需要: " + baseCost + " 当前: " + data.getSpiritStones());
            return;
        }

        // 检查并应用活跃灵气加成
        double forgeBoost = plugin.getActiveQiManager().getForgeBoost(player.getUniqueId());
        int finalSuccessRate = result.successRate;
        if (forgeBoost > 0) {
            finalSuccessRate += (int)(forgeBoost * 100); // +3%
            finalSuccessRate = Math.min(100, finalSuccessRate); // 最高100%
        }

        // 检查并应用宗门设施加成
        PlayerData playerData = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (playerData != null && playerData.getSectId() != null) {
            double sectBonus = plugin.getSectSystem().getFacilityManager()
                    .getForgeSuccessBonus(playerData.getSectId());
            if (sectBonus > 0) {
                finalSuccessRate += (int) sectBonus;
                finalSuccessRate = Math.min(100, finalSuccessRate);
            }
        }

        // 显示炼制信息
        player.sendMessage("§b========== 炼制装备 ==========");
        player.sendMessage("§e检测到材料:");
        for (var entry : materials.entrySet()) {
            player.sendMessage("§7  - " + getMaterialName(entry.getKey()) + " x" + entry.getValue());
        }
        player.sendMessage("");
        player.sendMessage("§e炼制品质: " + getQualityColor(targetQuality) + targetQuality);
        player.sendMessage("§e成功率: §a" + finalSuccessRate + "%");
        if (forgeBoost > 0) {
            player.sendMessage("§a§l⚡ 活跃灵气加成: +3% 成功率!");
        }
        if (playerData != null && playerData.getSectId() != null) {
            double sectBonus = plugin.getSectSystem().getFacilityManager()
                    .getForgeSuccessBonus(playerData.getSectId());
            if (sectBonus > 0) {
                player.sendMessage("§a§l⚒ 宗门设施加成: +" + (int)sectBonus + "% 成功率!");
            }
        }
        player.sendMessage("§e消耗灵石: §6" + baseCost);
        player.sendMessage("§b===========================");

        // 执行炼制
        java.util.Random random = new java.util.Random();
        if (random.nextInt(100) < finalSuccessRate) {
            // 炼制成功
            consumeMaterials(player, materials);
            data.removeSpiritStones(baseCost);

            // 生成胚胎
            Embryo embryo = EmbryoFactory.generateByQuality(targetQuality);
            ItemStack embryoItem = embryo.toItemStack();

            // 给予玩家
            player.getInventory().addItem(embryoItem);

            // 增加活跃灵气（根据品质给予不同奖励）
            int activeQiGain = switch (targetQuality) {
                case "神品" -> 20;
                case "仙品" -> 15;
                case "宝品" -> 10;
                case "灵品" -> 8;
                default -> 5;
            };
            data.addActiveQi(activeQiGain);

            player.sendMessage("§a✓ 炼制成功!");
            player.sendMessage("§e获得了 " + getQualityColor(targetQuality) + targetQuality + " §e品质的仙家胚胎!");
            player.sendMessage("§7UUID: " + embryo.getUuid());
            player.sendMessage("§7活跃灵气 +" + activeQiGain);

        } else {
            // 炼制失败
            consumeMaterials(player, materials);
            data.removeSpiritStones(baseCost / 2);  // 返还一半灵石

            player.sendMessage("§c✗ 炼制失败!");
            player.sendMessage("§7材料被毁坏了...");
            player.sendMessage("§7返还了 " + (baseCost / 2) + " 灵石");
        }

        // 消耗活跃灵气加成（无论成功失败）
        if (forgeBoost > 0) {
            plugin.getActiveQiManager().consumeBoost(player.getUniqueId(),
                    com.xiancore.systems.activeqi.ActiveQiManager.ActiveQiBoostType.FORGE);
        }

        plugin.getDataManager().savePlayerData(data);
    }

    /**
     * 收集玩家背包中的炼器材料
     */
    private java.util.Map<Material, Integer> collectMaterials(Player player) {
        java.util.Map<Material, Integer> materials = new java.util.HashMap<>();

        // 定义可用材料
        Material[] validMaterials = {
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND, Material.EMERALD,
            Material.NETHERITE_INGOT
        };

        for (Material material : validMaterials) {
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    count += item.getAmount();
                }
            }
            if (count > 0) {
                materials.put(material, count);
            }
        }

        return materials;
    }

    /**
     * 计算炼制品质和成功率
     */
    private CraftResult calculateCraftQuality(java.util.Map<Material, Integer> materials) {
        int totalValue = 0;
        int materialCount = 0;

        // 计算材料总价值
        for (var entry : materials.entrySet()) {
            int value = getMaterialValue(entry.getKey());
            int count = Math.min(entry.getValue(), 64); // 最多计算64个
            totalValue += value * count;
            materialCount += count;
        }

        // 根据总价值确定品质
        String quality;
        int successRate;
        int cost;

        if (totalValue >= 5000) {
            quality = "神品";
            successRate = 20;
            cost = 1000;
        } else if (totalValue >= 2000) {
            quality = "仙品";
            successRate = 40;
            cost = 500;
        } else if (totalValue >= 800) {
            quality = "宝品";
            successRate = 60;
            cost = 300;
        } else if (totalValue >= 300) {
            quality = "灵品";
            successRate = 80;
            cost = 100;
        } else {
            quality = "凡品";
            successRate = 95;
            cost = 50;
        }

        return new CraftResult(quality, successRate, cost, materialCount);
    }

    /**
     * 获取材料价值
     */
    private int getMaterialValue(Material material) {
        return switch (material) {
            case NETHERITE_INGOT -> 500;
            case DIAMOND -> 100;
            case EMERALD -> 80;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> 20;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> 5;
            default -> 1;
        };
    }

    /**
     * 获取材料名称
     */
    private String getMaterialName(Material material) {
        return switch (material) {
            case NETHERITE_INGOT -> "下界合金锭";
            case DIAMOND -> "钻石";
            case EMERALD -> "绿宝石";
            case GOLD_ORE -> "金矿石";
            case DEEPSLATE_GOLD_ORE -> "深层金矿石";
            case IRON_ORE -> "铁矿石";
            case DEEPSLATE_IRON_ORE -> "深层铁矿石";
            default -> material.name();
        };
    }

    /**
     * 消耗材料
     */
    private void consumeMaterials(Player player, java.util.Map<Material, Integer> materials) {
        for (var entry : materials.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    if (item.getAmount() <= amount) {
                        amount -= item.getAmount();
                        item.setAmount(0);
                    } else {
                        item.setAmount(item.getAmount() - amount);
                        amount = 0;
                    }
                    if (amount == 0) break;
                }
            }
        }
    }

    /**
     * 炼制结果类
     */
    private static class CraftResult {
        String quality;
        int successRate;
        int cost;
        int materialCount;

        CraftResult(String quality, int successRate, int cost, int materialCount) {
            this.quality = quality;
            this.successRate = successRate;
            this.cost = cost;
            this.materialCount = materialCount;
        }
    }

    /**
     * 处理强化
     */
    private void handleEnhance(Player player) {
        if (!hasPermission(player, "xiancore.forge.enhance")) {
            return;
        }

        // 直接打开装备选择GUI
        EquipmentSelectionGUI.open(player, plugin);
    }

    /**
     * 处理融合
     */
    private void handleFuse(Player player) {
        if (!hasPermission(player, "xiancore.forge.fuse")) {
            return;
        }

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        // 检查是否有两件物品
        if (mainHandItem.getType().isAir() || offHandItem.getType().isAir()) {
            sendError(player, "融合需要在主手和副手各持一件胚胎!");
            sendInfo(player, "§e融合规则:");
            sendInfo(player, "§7- 两件同品质胚胎可以融合");
            sendInfo(player, "§7- 融合后继承两者的属性");
            sendInfo(player, "§7- 有概率提升品质（同品质）");
            return;
        }

        // 检查是否都是仙家胚胎
        if (!isEmbryo(mainHandItem) || !isEmbryo(offHandItem)) {
            sendError(player, "只能融合仙家胚胎装备!");
            return;
        }

        // 从玩家数据中获取品质信息
        String mainQuality = extractQuality(mainHandItem);
        String offQuality = extractQuality(offHandItem);

        // 检查品质是否匹配
        if (!mainQuality.equals(offQuality)) {
            sendError(player, "融合的两件胚胎品质必须相同!");
            sendInfo(player, "主手品质: " + mainQuality);
            sendInfo(player, "副手品质: " + offQuality);
            return;
        }

        // 检查灵石是否足够
        com.xiancore.core.data.PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        int fuseCost = calculateFuseCost(mainQuality);

        if (data.getSpiritStones() < fuseCost) {
            sendError(player, "§c灵石不足! 需要 " + fuseCost + " 灵石，你有 " + data.getSpiritStones() + " 灵石");
            return;
        }

        // 检查并应用活跃灵气加成
        double forgeBoost = plugin.getActiveQiManager().getForgeBoost(player.getUniqueId());
        double upgradeChance = 0.2; // 默认20%概率提升品质
        if (forgeBoost > 0) {
            upgradeChance += forgeBoost; // +3% 提升品质概率
            player.sendMessage("§a§l⚡ 活跃灵气加成: 品质提升概率 +3%!");
        }

        // 检查并应用宗门设施加成
        PlayerData playerData3 = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (playerData3 != null && playerData3.getSectId() != null) {
            double sectBonus = plugin.getSectSystem().getFacilityManager()
                    .getForgeSuccessBonus(playerData3.getSectId());
            if (sectBonus > 0) {
                upgradeChance += (sectBonus / 100); // 转换为小数
                player.sendMessage("§a§l⚒ 宗门设施加成: 品质提升概率 +" + (int)sectBonus + "%!");
            }
        }

        // 执行融合
        boolean successUpgrade = performFusion(mainHandItem, offHandItem, upgradeChance);

        // 消耗灵石
        data.removeSpiritStones(fuseCost);

        // 清除副手物品
        player.getInventory().setItemInOffHand(new ItemStack(org.bukkit.Material.AIR));

        // 增加活跃灵气
        data.addActiveQi(12);

        if (successUpgrade) {
            sendSuccess(player, "§a融合成功! 品质已提升！");
            player.sendMessage("§e融合后的装备已保存到主手");
            player.sendMessage("§7活跃灵气 +12");
        } else {
            sendSuccess(player, "§a融合成功! 属性已继承");
            player.sendMessage("§e融合后的装备已保存到主手");
            player.sendMessage("§7活跃灵气 +12");
        }

        // 消耗活跃灵气加成
        if (forgeBoost > 0) {
            plugin.getActiveQiManager().consumeBoost(player.getUniqueId(),
                    com.xiancore.systems.activeqi.ActiveQiManager.ActiveQiBoostType.FORGE);
        }

        plugin.getDataManager().savePlayerData(data);
    }

    /**
     * 从胚胎物品中提取品质
     */
    private String extractQuality(ItemStack item) {
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            // 品质信息格式: "§d§l仙家胚胎 [品质]"
            if (displayName.contains("[") && displayName.contains("]")) {
                int start = displayName.lastIndexOf("[");
                int end = displayName.lastIndexOf("]");
                return displayName.substring(start + 1, end);
            }
        }
        return "普通";
    }

    /**
     * 计算融合消耗
     */
    private int calculateFuseCost(String quality) {
        return switch (quality) {
            case "神品" -> 1000;  // 顶级品质融合最贵
            case "仙品" -> 500;
            case "宝品" -> 300;
            case "灵品" -> 100;
            default -> 50;
        };
    }

    /**
     * 执行融合操作
     */
    private boolean performFusion(ItemStack item1, ItemStack item2, double upgradeChance) {
        // 融合逻辑：合并属性并有概率升品质
        // 获取两个胚胎的属性
        ItemStack fusedItem = item1.clone();

        if (item1.getItemMeta() != null && item2.getItemMeta() != null) {
            ItemMeta meta = fusedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
                // 简单融合：合并lore中的属性
                List<String> lore = new ArrayList<>(meta.getLore());

                // 使用传入的品质提升概率
                if (Math.random() < upgradeChance) {
                    // 提升品质
                    String displayName = meta.getDisplayName();
                    String newDisplayName = upgradeQuality(displayName);
                    meta.setDisplayName(newDisplayName);
                    return true;
                }

                meta.setLore(lore);
                fusedItem.setItemMeta(meta);
            }
        }

        return false;
    }

    /**
     * 升级品质
     */
    private String upgradeQuality(String displayName) {
        if (displayName.contains("灵品")) {
            return displayName.replace("灵品", "宝品").replace("§b", "§5");
        } else if (displayName.contains("宝品")) {
            return displayName.replace("宝品", "仙品").replace("§5", "§6§l");
        } else if (displayName.contains("仙品")) {
            return displayName.replace("仙品", "神品").replace("§6§l", "§d§l");
        }
        return displayName;
    }

    /**
     * 处理信息查看
     */
    private void handleInfo(Player player) {
        if (!hasPermission(player, "xiancore.forge.use")) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            sendError(player, "请手持要查看的物品!");
            return;
        }

        if (!isEmbryo(item)) {
            sendError(player, "该物品不是仙家胚胎!");
            return;
        }

        // 显示胚胎信息
        showEmbryoInfo(player, item);
    }

    /**
     * 处理装备命名
     */
    private void handleName(Player player, String[] args) {
        if (!hasPermission(player, "xiancore.forge.use")) {
            return;
        }

        if (args.length < 2) {
            sendError(player, "请提供装备名称!");
            sendInfo(player, "§e用法: /forge name <名称>");
            sendInfo(player, "§7示例: /forge name §c火焰之剑");
            return;
        }

        // 合并所有参数为名称（支持空格）
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) nameBuilder.append(" ");
            nameBuilder.append(args[i]);
        }
        String customName = nameBuilder.toString();

        // 保存到待命名列表
        plugin.getForgeSystem().setPendingEquipmentName(player.getUniqueId(), customName);

        player.sendMessage("§a已设置装备名称: §f" + customName);
        player.sendMessage("§7下次炼制装备时将使用此名称");
        player.sendMessage("§e提示: 使用 §a/forge make §e打开炼制界面");
    }

    /**
     * 显示胚胎信息
     */
    private void showEmbryoInfo(Player player, ItemStack item) {
        // 从物品获取胚胎信息
        String embryoName = "未知胚胎";
        String quality = "凡品";
        String element = "未知";
        int attack = 0, defense = 0, hp = 0, qi = 0;

        // 从 DisplayName 提取胚胎名称和品质
        if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            embryoName = displayName.replaceAll("§[0-9a-fklmnor]", ""); // 移除颜色代码

            // 提取品质
            if (displayName.contains("凡品")) quality = "凡品";
            else if (displayName.contains("灵品")) quality = "灵品";
            else if (displayName.contains("宝品")) quality = "宝品";
            else if (displayName.contains("仙品")) quality = "仙品";
            else if (displayName.contains("神品")) quality = "神品";
        }

        // 从 Lore 中提取详细属性信息
        if (item.getItemMeta() != null && item.getItemMeta().hasLore() && item.getItemMeta().getLore() != null) {
            for (String line : item.getItemMeta().getLore()) {
                String cleanLine = line.replaceAll("§[0-9a-fklmnor]", "");

                // 提取攻击力
                if (cleanLine.contains("攻击力:")) {
                    try {
                        attack = Integer.parseInt(cleanLine.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        attack = 0;
                    }
                }
                // 提取防御力
                else if (cleanLine.contains("防御力:")) {
                    try {
                        defense = Integer.parseInt(cleanLine.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        defense = 0;
                    }
                }
                // 提取生命值
                else if (cleanLine.contains("生命值:")) {
                    try {
                        hp = Integer.parseInt(cleanLine.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        hp = 0;
                    }
                }
                // 提取灵力值
                else if (cleanLine.contains("灵力值:")) {
                    try {
                        qi = Integer.parseInt(cleanLine.replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        qi = 0;
                    }
                }
                // 提取五行属性
                else if (cleanLine.contains("五行属性:")) {
                    element = cleanLine.replace("五行属性:", "").trim();
                }
            }
        }

        // 显示胚胎信息
        sendInfo(player, "§b========== 仙家胚胎信息 ==========");
        sendInfo(player, "§e名称: §f" + embryoName);
        sendInfo(player, "§e品质: " + getQualityColor(quality) + quality);
        sendInfo(player, "§e五行属性: §f" + element);
        sendInfo(player, "");
        sendInfo(player, "§e基础属性:");
        sendInfo(player, "§c  攻击力: " + attack);
        sendInfo(player, "§9  防御力: " + defense);
        sendInfo(player, "§a  生命值: " + hp);
        sendInfo(player, "§b  灵力值: " + qi);
        sendInfo(player, "§b=================================");
    }

    /**
     * 获取品质颜色代码
     */
    private String getQualityColor(String quality) {
        return switch (quality) {
            case "神品" -> "§d§l";
            case "仙品" -> "§6§l";
            case "宝品" -> "§5";
            case "灵品" -> "§b";
            default -> "§f";
        };
    }

    /**
     * 检查是否是仙家胚胎
     */
    private boolean isEmbryo(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        // 检查物品的 Lore 或 NBT 标记
        if (item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("仙家胚胎") || line.contains("品质:")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取物品的强化等级
     */
    private int getEnhanceLevel(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return 0;
        }

        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) {
            return 0;
        }

        for (String line : lore) {
            if (line.contains("强化等级:") || line.contains("强化:")) {
                try {
                    // 提取数字
                    String[] parts = line.split("[^0-9]");
                    for (String part : parts) {
                        if (!part.isEmpty()) {
                            return Integer.parseInt(part);
                        }
                    }
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }

        return 0;
    }

    /**
     * 设置物品的强化等级
     */
    private void setEnhanceLevel(ItemStack item, int level) {
        if (!item.hasItemMeta()) {
            return;
        }

        var meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();

        // 移除旧的强化等级信息
        lore.removeIf(line -> line.contains("强化等级:") || line.contains("强化:"));

        // 添加新的强化等级信息
        lore.add("§7强化等级: §6+" + level);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * 提升物品属性（基于强化等级）
     */
    private void enhanceItemStats(ItemStack item, int level) {
        if (!item.hasItemMeta()) {
            return;
        }

        var meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new java.util.ArrayList<>(meta.getLore()) : new java.util.ArrayList<>();

        // 这里可以添加更复杂的属性提升逻辑
        // 简单示例：根据强化等级增加属性提示
        if (level > 0) {
            // 移除旧的属性行
            lore.removeIf(line -> line.contains("攻击加成:") || line.contains("防御加成:"));

            // 添加新的属性
            int atkBonus = level * 10;
            int defBonus = level * 5;

            lore.add("§c攻击加成: +" + atkBonus);
            lore.add("§9防御加成: +" + defBonus);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
