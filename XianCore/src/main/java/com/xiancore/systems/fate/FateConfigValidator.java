package com.xiancore.systems.fate;

import com.xiancore.XianCore;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 奇遇配置验证工具
 * 在启动时验证配置文件的完整性和正确性
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class FateConfigValidator {

    private final XianCore plugin;

    public FateConfigValidator(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 验证整个配置文件
     *
     * @param config 配置文件
     * @return 错误列表（空列表表示无错误）
     */
    public List<String> validate(FileConfiguration config) {
        List<String> errors = new ArrayList<>();

        if (config == null) {
            errors.add("配置文件为 null");
            return errors;
        }

        // 检查 fate-types 配置节
        if (!config.contains("fate-types")) {
            errors.add("缺少 fate-types 配置节");
            return errors;
        }

        ConfigurationSection fateTypesSection = config.getConfigurationSection("fate-types");
        if (fateTypesSection == null) {
            errors.add("fate-types 配置节无效");
            return errors;
        }

        // 验证四种奇遇类型
        errors.addAll(validateFateType(config, "small", "小奇遇"));
        errors.addAll(validateFateType(config, "medium", "中奇遇"));
        errors.addAll(validateFateType(config, "large", "大奇遇"));
        errors.addAll(validateFateType(config, "destiny", "命运奇遇"));

        return errors;
    }

    /**
     * 验证单个奇遇类型配置
     */
    private List<String> validateFateType(FileConfiguration config, String typeKey, String typeName) {
        List<String> errors = new ArrayList<>();
        String path = "fate-types." + typeKey;

        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            errors.add(typeName + " 配置节不存在: " + path);
            return errors;
        }

        // 检查 weight 字段
        if (!section.contains("weight")) {
            errors.add(typeName + " 缺少 weight 字段");
        } else {
            try {
                double weight = section.getDouble("weight");
                if (weight < 0) {
                    errors.add(typeName + " 的 weight 不能为负数: " + weight);
                }
            } catch (Exception e) {
                errors.add(typeName + " 的 weight 格式错误");
            }
        }

        // 检查 rewards 列表
        if (!section.contains("rewards")) {
            errors.add(typeName + " 缺少 rewards 配置");
        } else {
            List<?> rewardsList = section.getList("rewards");
            if (rewardsList == null || rewardsList.isEmpty()) {
                errors.add(typeName + " 的 rewards 列表为空");
            } else {
                // 验证每个奖励配置
                int rewardIndex = 0;
                for (Object rewardObj : rewardsList) {
                    rewardIndex++;
                    if (rewardObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> rewardMap = (Map<String, Object>) rewardObj;
                        errors.addAll(validateReward(rewardMap, typeName, rewardIndex));
                    } else {
                        errors.add(typeName + " 的第 " + rewardIndex + " 个奖励格式错误");
                    }
                }
            }
        }

        return errors;
    }

    /**
     * 验证单个奖励配置
     */
    private List<String> validateReward(Map<String, Object> rewardMap, String typeName, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = typeName + " 的第 " + index + " 个奖励";

        // 检查 type 字段
        if (!rewardMap.containsKey("type")) {
            errors.add(prefix + " 缺少 type 字段");
            return errors;
        }

        String type = (String) rewardMap.get("type");
        if (type == null || type.isEmpty()) {
            errors.add(prefix + " 的 type 字段为空");
            return errors;
        }

        // 根据奖励类型验证特定字段
        switch (type.toLowerCase()) {
            case "qi":
            case "spirit-stones":
            case "skill-points":
            case "contribution":
            case "active-qi":
            case "level":
                // 数值奖励：检查 amount 相关字段
                errors.addAll(validateNumericReward(rewardMap, prefix));
                break;

            case "item":
                // 物品奖励：检查 material 和 amount
                errors.addAll(validateItemReward(rewardMap, prefix));
                break;

            case "skill-book":
                // 功法书奖励：检查 skill-id 或 skill-pool
                errors.addAll(validateSkillBookReward(rewardMap, prefix));
                break;

            case "equipment":
                // 装备奖励：检查 quality 和 level
                errors.addAll(validateEquipmentReward(rewardMap, prefix));
                break;

            case "command":
                // 命令奖励：检查 command 字段
                errors.addAll(validateCommandReward(rewardMap, prefix));
                break;

            default:
                plugin.getLogger().warning(prefix + " 使用了未知的奖励类型: " + type);
                break;
        }

        // 验证 chance 字段（可选）
        if (rewardMap.containsKey("chance")) {
            try {
                Object chanceObj = rewardMap.get("chance");
                double chance;
                if (chanceObj instanceof Number) {
                    chance = ((Number) chanceObj).doubleValue();
                } else if (chanceObj instanceof String) {
                    chance = Double.parseDouble((String) chanceObj);
                } else {
                    errors.add(prefix + " 的 chance 字段类型无效");
                    return errors;
                }

                if (chance < 0.0 || chance > 1.0) {
                    errors.add(prefix + " 的 chance 超出范围 [0-1]: " + chance);
                }
            } catch (Exception e) {
                errors.add(prefix + " 的 chance 格式错误");
            }
        }

        return errors;
    }

    /**
     * 验证数值奖励
     */
    private List<String> validateNumericReward(Map<String, Object> map, String prefix) {
        List<String> errors = new ArrayList<>();

        // 必须有 amount 或 (min-amount + max-amount)
        if (!map.containsKey("amount") && !map.containsKey("min-amount") && !map.containsKey("max-amount")) {
            errors.add(prefix + " 缺少 amount 或 min-amount/max-amount 字段");
        }

        return errors;
    }

    /**
     * 验证物品奖励
     */
    private List<String> validateItemReward(Map<String, Object> map, String prefix) {
        List<String> errors = new ArrayList<>();

        // 检查 material 字段
        if (!map.containsKey("material")) {
            errors.add(prefix + " 缺少 material 字段");
        } else {
            String material = (String) map.get("material");
            try {
                Material.valueOf(material.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(prefix + " 的 material 无效: " + material);
            }
        }

        return errors;
    }

    /**
     * 验证功法书奖励
     */
    private List<String> validateSkillBookReward(Map<String, Object> map, String prefix) {
        List<String> errors = new ArrayList<>();

        // 必须有 skill-id 或 skill-pool
        if (!map.containsKey("skill-id") && !map.containsKey("skill-pool")) {
            errors.add(prefix + " 缺少 skill-id 或 skill-pool 字段");
        }

        // 验证功法ID是否存在（如果提供了固定ID）
        if (map.containsKey("skill-id")) {
            String skillId = (String) map.get("skill-id");
            if (plugin.getSkillSystem() != null && plugin.getSkillSystem().getSkill(skillId) == null) {
                plugin.getLogger().warning(prefix + " 引用的功法不存在: " + skillId);
            }
        }

        // 验证功法池（如果提供了）
        if (map.containsKey("skill-pool")) {
            Object poolObj = map.get("skill-pool");
            if (poolObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> skillPool = (List<String>) poolObj;
                
                if (skillPool.isEmpty()) {
                    errors.add(prefix + " 的 skill-pool 为空");
                }
                
                // 验证池中的功法ID
                if (plugin.getSkillSystem() != null) {
                    for (String skillId : skillPool) {
                        if (plugin.getSkillSystem().getSkill(skillId) == null) {
                            plugin.getLogger().warning(prefix + " 的功法池中引用了不存在的功法: " + skillId);
                        }
                    }
                }
            }
        }

        return errors;
    }

    /**
     * 验证装备奖励
     */
    private List<String> validateEquipmentReward(Map<String, Object> map, String prefix) {
        List<String> errors = new ArrayList<>();

        // 检查 quality 字段（可选）
        if (map.containsKey("quality")) {
            String quality = (String) map.get("quality");
            List<String> validQualities = List.of("common", "rare", "epic", "legendary", "random");
            if (!validQualities.contains(quality.toLowerCase())) {
                errors.add(prefix + " 的 quality 无效: " + quality);
            }
        }

        return errors;
    }

    /**
     * 验证命令奖励
     */
    private List<String> validateCommandReward(Map<String, Object> map, String prefix) {
        List<String> errors = new ArrayList<>();

        // 检查 command 字段
        if (!map.containsKey("command")) {
            errors.add(prefix + " 缺少 command 字段");
        }

        return errors;
    }

    /**
     * 验证配置并打印结果
     *
     * @param config 配置文件
     * @return 是否有错误
     */
    public boolean validateAndLog(FileConfiguration config) {
        List<String> errors = validate(config);

        if (errors.isEmpty()) {
            plugin.getLogger().info("  §a✓ 奇遇配置验证通过");
            return true;
        } else {
            plugin.getLogger().warning("§e[奇遇系统] 配置文件存在 " + errors.size() + " 个问题:");
            for (String error : errors) {
                plugin.getLogger().warning("  §e- " + error);
            }
            return false;
        }
    }
}

