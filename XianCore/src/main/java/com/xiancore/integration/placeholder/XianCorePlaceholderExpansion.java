package com.xiancore.integration.placeholder;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.sect.Sect;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

/**
 * XianCore PlaceholderAPI 扩展
 * 提供所有 XianCore 相关的占位符支持
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class XianCorePlaceholderExpansion extends PlaceholderExpansion {

    private final XianCore plugin;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,###");
    private final DecimalFormat percentFormat = new DecimalFormat("#0.0");

    public XianCorePlaceholderExpansion(XianCore plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "xiancore";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // 插件重载后继续保持注册
    }

    @Override
    @Nullable
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // 加载玩家数据
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return "未初始化";
        }

        // 处理占位符
        return processPlaceholder(data, params);
    }

    /**
     * 处理占位符请求
     */
    private String processPlaceholder(PlayerData data, String params) {
        // 修炼系统占位符
        if (params.startsWith("realm")) {
            return handleRealmPlaceholders(data, params);
        }

        // 资源系统占位符
        if (params.startsWith("stones") || params.startsWith("spirit_stones")) {
            return handleStonesPlaceholders(data, params);
        }

        if (params.startsWith("skill_points")) {
            return handleSkillPointsPlaceholders(data, params);
        }

        if (params.startsWith("contribution")) {
            return handleContributionPlaceholders(data, params);
        }

        // 宗门系统占位符
        if (params.startsWith("sect")) {
            return handleSectPlaceholders(data, params);
        }

        // 属性占位符
        if (params.startsWith("qi")) {
            return handleQiPlaceholders(data, params);
        }

        if (params.startsWith("spiritual_root") || params.startsWith("root")) {
            return handleSpiritualRootPlaceholders(data, params);
        }

        if (params.startsWith("comprehension") || params.startsWith("comp")) {
            return handleComprehensionPlaceholders(data, params);
        }

        if (params.startsWith("adaptation") || params.startsWith("adapt")) {
            return handleAdaptationPlaceholders(data, params);
        }

        // 其他占位符
        if (params.equals("player_level") || params.equals("level")) {
            return String.valueOf(data.getPlayerLevel());
        }

        if (params.equals("active_qi")) {
            return decimalFormat.format(data.getActiveQi());
        }

        if (params.equals("fate_count")) {
            return String.valueOf(data.getFateCount());
        }

        if (params.equals("breakthrough_attempts")) {
            return String.valueOf(data.getBreakthroughAttempts());
        }

        if (params.equals("successful_breakthroughs")) {
            return String.valueOf(data.getSuccessfulBreakthroughs());
        }

        if (params.equals("breakthrough_success_rate")) {
            int total = data.getBreakthroughAttempts();
            if (total == 0) return "0.0%";
            double rate = (double) data.getSuccessfulBreakthroughs() / total * 100;
            return percentFormat.format(rate) + "%";
        }

        if (params.equals("breakthrough_rate")) {
            // 计算当前突破成功率（简化版本）
            double rate = calculateSimpleBreakthroughRate(data);
            return percentFormat.format(rate * 100) + "%";
        }

        // 天劫系统占位符
        if (params.equals("tribulation_count")) {
            return String.valueOf(data.getTribulationCount());
        }

        if (params.equals("tribulation_success")) {
            return String.valueOf(data.getSuccessfulTribulations());
        }

        if (params.equals("next_tribulation")) {
            return getNextTribulationType(data);
        }

        // 功法系统占位符
        if (params.equals("learned_skills")) {
            return String.valueOf(data.getLearnedSkills().size());
        }

        if (params.equals("max_skill_level")) {
            int maxLevel = data.getLearnedSkills().values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
            return String.valueOf(maxLevel);
        }

        // 未知占位符
        return null;
    }

    /**
     * 处理境界相关占位符
     */
    private String handleRealmPlaceholders(PlayerData data, String params) {
        String stageName = getRealmStageName(data.getRealmStage());

        switch (params) {
            case "realm":
                return data.getRealm();

            case "realm_stage":
                return stageName;

            case "realm_full":
                return data.getRealm() + "·" + stageName;

            case "realm_color":
                return getRealmColor(data.getRealm()) + data.getRealm();

            case "realm_stage_color":
                return getStageColor(stageName) + stageName;

            case "realm_full_color":
                return getRealmColor(data.getRealm()) + data.getRealm() +
                       "§7·" + getStageColor(stageName) + stageName;

            default:
                return null;
        }
    }

    /**
     * 处理修为相关占位符
     */
    private String handleQiPlaceholders(PlayerData data, String params) {
        switch (params) {
            case "qi":
                return decimalFormat.format(data.getQi());

            case "qi_max":
                long maxQi = calculateMaxQi(data.getRealm(), data.getRealmStage());
                return decimalFormat.format(maxQi);

            case "qi_percent":
                long max = calculateMaxQi(data.getRealm(), data.getRealmStage());
                if (max == 0) return "0.0%";
                double percent = (double) data.getQi() / max * 100;
                return percentFormat.format(percent) + "%";

            case "qi_bar":
                return generateProgressBar(data.getQi(),
                    calculateMaxQi(data.getRealm(), data.getRealmStage()));

            default:
                return null;
        }
    }

    /**
     * 处理灵根相关占位符
     */
    private String handleSpiritualRootPlaceholders(PlayerData data, String params) {
        double root = data.getSpiritualRoot();

        switch (params) {
            case "spiritual_root":
            case "root":
                return percentFormat.format(root * 100) + "%";

            case "spiritual_root_raw":
            case "root_raw":
                return String.valueOf(root);

            case "spiritual_root_grade":
            case "root_grade":
                return getRootGrade(root);

            case "spiritual_root_color":
            case "root_color":
                return getRootGradeColor(root) + getRootGrade(root);

            default:
                return null;
        }
    }

    /**
     * 处理悟性相关占位符
     */
    private String handleComprehensionPlaceholders(PlayerData data, String params) {
        double comp = data.getComprehension();

        switch (params) {
            case "comprehension":
            case "comp":
                return percentFormat.format(comp * 100) + "%";

            case "comprehension_raw":
            case "comp_raw":
                return String.valueOf(comp);

            case "comprehension_grade":
            case "comp_grade":
                return getComprehensionGrade(comp);

            case "comprehension_color":
            case "comp_color":
                return getComprehensionGradeColor(comp) + getComprehensionGrade(comp);

            default:
                return null;
        }
    }

    /**
     * 处理功法适配度相关占位符
     */
    private String handleAdaptationPlaceholders(PlayerData data, String params) {
        double adapt = data.getTechniqueAdaptation();

        switch (params) {
            case "adaptation":
            case "adapt":
                return percentFormat.format(adapt * 100) + "%";

            case "adaptation_raw":
            case "adapt_raw":
                return String.valueOf(adapt);

            default:
                return null;
        }
    }

    /**
     * 处理灵石相关占位符
     */
    private String handleStonesPlaceholders(PlayerData data, String params) {
        switch (params) {
            case "stones":
            case "spirit_stones":
                return decimalFormat.format(data.getSpiritStones());

            case "stones_raw":
            case "spirit_stones_raw":
                return String.valueOf(data.getSpiritStones());

            default:
                return null;
        }
    }

    /**
     * 处理功法点相关占位符
     */
    private String handleSkillPointsPlaceholders(PlayerData data, String params) {
        switch (params) {
            case "skill_points":
                return String.valueOf(data.getSkillPoints());

            default:
                return null;
        }
    }

    /**
     * 处理贡献点相关占位符
     */
    private String handleContributionPlaceholders(PlayerData data, String params) {
        switch (params) {
            case "contribution":
                return decimalFormat.format(data.getContributionPoints());

            case "contribution_raw":
                return String.valueOf(data.getContributionPoints());

            default:
                return null;
        }
    }

    /**
     * 处理宗门相关占位符
     */
    private String handleSectPlaceholders(PlayerData data, String params) {
        Integer sectId = data.getSectId();

        if (sectId == null) {
            switch (params) {
                case "sect_name":
                    return "无宗门";
                case "sect_rank":
                    return "散修";
                case "sect_level":
                case "sect_members":
                case "sect_max_members":
                case "sect_funds":
                    return "0";
                default:
                    return "N/A";
            }
        }

        Sect sect = plugin.getSectSystem().getSects().get(sectId);
        if (sect == null) {
            return "宗门已解散";
        }

        switch (params) {
            case "sect_name":
                return sect.getName();

            case "sect_rank":
                return data.getSectRank();

            case "sect_level":
                return String.valueOf(sect.getLevel());

            case "sect_members":
                return String.valueOf(sect.getMembers().size());

            case "sect_max_members":
                return String.valueOf(sect.getMaxMembers());

            case "sect_funds":
                return decimalFormat.format(sect.getSectFunds());

            case "sect_owner":
                return sect.getOwnerName();

            case "sect_recruiting":
                return sect.isRecruiting() ? "招募中" : "不招募";

            default:
                return null;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 将境界阶段数字转换为名称
     */
    private String getRealmStageName(int stage) {
        return switch (stage) {
            case 1 -> "初期";
            case 2 -> "中期";
            case 3 -> "后期";
            default -> "未知";
        };
    }

    /**
     * 计算修为上限
     * 根据境界和阶段返回修为上限
     */
    private long calculateMaxQi(String realm, int stage) {
        long baseQi = switch (realm) {
            case "炼气期" -> 10000L;
            case "筑基期" -> 50000L;
            case "金丹期", "结丹期" -> 200000L;
            case "元婴期" -> 1000000L;
            case "化神期" -> 5000000L;
            case "炼虚期" -> 20000000L;
            case "合体期" -> 100000000L;
            case "大乘期" -> 500000000L;
            default -> 10000L;
        };

        // 根据阶段调整
        return switch (stage) {
            case 1 -> baseQi;           // 初期
            case 2 -> baseQi * 2;       // 中期
            case 3 -> baseQi * 4;       // 后期
            default -> baseQi;
        };
    }

    /**
     * 计算简化的突破成功率
     * 基于玩家当前属性的简化计算
     */
    private double calculateSimpleBreakthroughRate(PlayerData data) {
        double L = data.getSpiritualRoot();           // 灵根
        double P = data.getTechniqueAdaptation();     // 功法适配度
        double G = data.getComprehension();           // 悟性
        double D = getRealmDifficulty(data.getRealm()); // 境界难度

        // 简化公式：基础成功率 * 属性加成 / 难度
        double baseRate = 0.3; // 30% 基础成功率
        double attributeBonus = (L + P + G) / 3.0; // 平均属性

        double rate = baseRate * attributeBonus / D;

        // 限制在 5% 到 95% 之间
        return Math.max(0.05, Math.min(0.95, rate));
    }

    /**
     * 获取境界难度系数
     */
    private double getRealmDifficulty(String realm) {
        return switch (realm) {
            case "炼气期" -> 1.0;
            case "筑基期" -> 1.5;
            case "金丹期", "结丹期" -> 2.5;
            case "元婴期" -> 4.0;
            case "化神期" -> 6.0;
            case "炼虚期" -> 8.0;
            case "合体期" -> 10.0;
            case "大乘期" -> 15.0;
            default -> 1.0;
        };
    }

    /**
     * 获取下次天劫类型
     * 根据玩家当前境界判断下一次渡劫类型
     */
    private String getNextTribulationType(PlayerData data) {
        String realm = data.getRealm();
        int stage = data.getRealmStage();

        // 如果是后期（第3阶段），下次天劫是大境界突破的天劫
        if (stage == 3) {
            return switch (realm) {
                case "炼气期" -> "筑基劫";
                case "筑基期" -> "金丹劫";
                case "金丹期", "结丹期" -> "元婴劫";
                case "元婴期" -> "化神劫";
                case "化神期" -> "炼虚劫";
                case "炼虚期" -> "合体劫";
                case "合体期" -> "大乘劫";
                case "大乘期" -> "飞升劫";
                default -> "无";
            };
        } else {
            // 小境界突破不需要渡劫
            return "无需渡劫";
        }
    }

    /**
     * 获取境界颜色
     */
    private String getRealmColor(String realm) {
        switch (realm) {
            case "炼气期": return "§7";
            case "筑基期": return "§a";
            case "金丹期": return "§e";
            case "元婴期": return "§6";
            case "化神期": return "§c";
            case "炼虚期": return "§5";
            case "合体期": return "§d";
            case "大乘期": return "§b";
            default: return "§f";
        }
    }

    /**
     * 获取阶段颜色
     */
    private String getStageColor(String stage) {
        switch (stage) {
            case "初期": return "§7";
            case "中期": return "§a";
            case "后期": return "§e";
            default: return "§f";
        }
    }

    /**
     * 获取灵根等级
     */
    private String getRootGrade(double root) {
        if (root >= 0.9) return "天灵根";
        if (root >= 0.8) return "异灵根";
        if (root >= 0.7) return "真灵根";
        if (root >= 0.6) return "上品灵根";
        if (root >= 0.5) return "中品灵根";
        if (root >= 0.4) return "下品灵根";
        return "伪灵根";
    }

    /**
     * 获取灵根等级颜色
     */
    private String getRootGradeColor(double root) {
        if (root >= 0.9) return "§d"; // 天灵根 - 粉色
        if (root >= 0.8) return "§5"; // 异灵根 - 紫色
        if (root >= 0.7) return "§b"; // 真灵根 - 青色
        if (root >= 0.6) return "§e"; // 上品灵根 - 黄色
        if (root >= 0.5) return "§a"; // 中品灵根 - 绿色
        if (root >= 0.4) return "§7"; // 下品灵根 - 灰色
        return "§8"; // 伪灵根 - 深灰色
    }

    /**
     * 获取悟性等级
     */
    private String getComprehensionGrade(double comp) {
        if (comp >= 0.9) return "绝世";
        if (comp >= 0.8) return "超凡";
        if (comp >= 0.7) return "优秀";
        if (comp >= 0.6) return "良好";
        if (comp >= 0.5) return "普通";
        if (comp >= 0.4) return "较差";
        return "愚钝";
    }

    /**
     * 获取悟性等级颜色
     */
    private String getComprehensionGradeColor(double comp) {
        if (comp >= 0.9) return "§d"; // 绝世 - 粉色
        if (comp >= 0.8) return "§5"; // 超凡 - 紫色
        if (comp >= 0.7) return "§b"; // 优秀 - 青色
        if (comp >= 0.6) return "§e"; // 良好 - 黄色
        if (comp >= 0.5) return "§a"; // 普通 - 绿色
        if (comp >= 0.4) return "§7"; // 较差 - 灰色
        return "§8"; // 愚钝 - 深灰色
    }

    /**
     * 生成进度条
     */
    private String generateProgressBar(long current, long max) {
        if (max == 0) return "§7[§8----------§7]";

        int barLength = 10;
        double percent = (double) current / max;
        int filled = (int) (percent * barLength);

        StringBuilder bar = new StringBuilder("§7[");

        // 填充部分 (绿色)
        for (int i = 0; i < filled; i++) {
            bar.append("§a█");
        }

        // 未填充部分 (灰色)
        for (int i = filled; i < barLength; i++) {
            bar.append("§8█");
        }

        bar.append("§7]");
        return bar.toString();
    }
}
