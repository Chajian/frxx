package com.xiancore.systems.sect.task;

import com.xiancore.XianCore;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 任务配置验证器
 * 在加载配置前验证配置文件的正确性
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class TaskConfigValidator {

    private final XianCore plugin;
    private static final String REQUIRED_VERSION = "1.0";

    public TaskConfigValidator(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 验证配置文件
     *
     * @param config 配置文件
     * @return 验证结果
     */
    public ValidationResult validate(FileConfiguration config) {
        ValidationResult result = new ValidationResult();

        // 1. 验证配置版本
        validateVersion(config, result);

        // 2. 验证全局设置
        validateSettings(config, result);

        // 3. 验证任务模板
        validateTemplates(config, result);

        // 4. 验证境界配置
        validateRealms(config, result);

        return result;
    }

    /**
     * 验证配置版本
     */
    private void validateVersion(FileConfiguration config, ValidationResult result) {
        String version = config.getString("config-version", "unknown");
        if (!REQUIRED_VERSION.equals(version)) {
            result.addWarning("配置版本不匹配: " + version + " (需要: " + REQUIRED_VERSION + ")");
        }
    }

    /**
     * 验证全局设置
     */
    private void validateSettings(FileConfiguration config, ValidationResult result) {
        if (!config.contains("settings")) {
            result.addError("缺少 settings 配置节");
            return;
        }

        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings == null) {
            result.addError("settings 配置节为空");
            return;
        }

        // 验证任务数量
        if (settings.contains("task-counts")) {
            ConfigurationSection counts = settings.getConfigurationSection("task-counts");
            if (counts != null) {
                validateTaskCount(counts, "daily", result);
                validateTaskCount(counts, "weekly", result);
                validateTaskCount(counts, "special", result);
            }
        } else {
            result.addWarning("缺少 task-counts 配置，将使用默认值");
        }

        // 验证难度系数
        if (settings.contains("difficulty")) {
            ConfigurationSection difficulty = settings.getConfigurationSection("difficulty");
            if (difficulty != null) {
                validateMultiplier(difficulty, "target-multiplier", 0.0, 1.0, result);
                validateMultiplier(difficulty, "reward-multiplier", 0.0, 2.0, result);
            }
        }
    }

    /**
     * 验证任务数量
     */
    private void validateTaskCount(ConfigurationSection section, String key, ValidationResult result) {
        if (!section.contains(key)) {
            result.addWarning("缺少 " + key + " 任务数量配置");
            return;
        }

        int count = section.getInt(key, 0);
        if (count <= 0) {
            result.addError(key + " 任务数量必须大于 0，当前值: " + count);
        } else if (count > 10) {
            result.addWarning(key + " 任务数量过多 (" + count + ")，可能影响性能");
        }
    }

    /**
     * 验证倍率系数
     */
    private void validateMultiplier(ConfigurationSection section, String key, double min, double max, ValidationResult result) {
        if (!section.contains(key)) {
            result.addWarning("缺少 " + key + " 配置");
            return;
        }

        double value = section.getDouble(key, 0.0);
        if (value < min || value > max) {
            result.addError(key + " 超出有效范围 [" + min + ", " + max + "]，当前值: " + value);
        }
    }

    /**
     * 验证任务模板
     */
    private void validateTemplates(FileConfiguration config, ValidationResult result) {
        if (!config.contains("templates")) {
            result.addError("缺少 templates 配置节");
            return;
        }

        ConfigurationSection templates = config.getConfigurationSection("templates");
        if (templates == null) {
            result.addError("templates 配置节为空");
            return;
        }

        // 验证每种类型的任务模板
        for (SectTaskType type : SectTaskType.values()) {
            String typeName = type.name().toLowerCase();
            if (!templates.contains(typeName)) {
                result.addError("缺少 " + typeName + " 类型的任务模板");
                continue;
            }

            ConfigurationSection typeSection = templates.getConfigurationSection(typeName);
            if (typeSection == null || typeSection.getKeys(false).isEmpty()) {
                result.addError(typeName + " 类型的任务模板为空");
                continue;
            }

            // 验证每个任务模板
            int validCount = 0;
            for (String taskId : typeSection.getKeys(false)) {
                ConfigurationSection taskSection = typeSection.getConfigurationSection(taskId);
                if (validateTaskTemplate(taskId, taskSection, result)) {
                    validCount++;
                }
            }

            if (validCount == 0) {
                result.addError(typeName + " 类型没有有效的任务模板");
            } else {
                result.addInfo(typeName + " 类型: " + validCount + " 个有效任务模板");
            }
        }
    }

    /**
     * 验证单个任务模板
     */
    private boolean validateTaskTemplate(String taskId, ConfigurationSection section, ValidationResult result) {
        if (section == null) {
            result.addError("任务 " + taskId + " 配置为空");
            return false;
        }

        boolean isValid = true;
        String prefix = "任务 " + taskId + ": ";

        // 检查必需字段
        if (!section.contains("name") || section.getString("name", "").isEmpty()) {
            result.addError(prefix + "缺少 name 字段");
            isValid = false;
        }

        if (!section.contains("objective")) {
            result.addError(prefix + "缺少 objective 字段");
            isValid = false;
        } else {
            // 验证 objective 是否有效
            String objectiveStr = section.getString("objective");
            try {
                TaskObjective.valueOf(objectiveStr);
            } catch (IllegalArgumentException e) {
                result.addError(prefix + "无效的 objective 值: " + objectiveStr);
                isValid = false;
            }
        }

        // 验证 base-amount
        if (!section.contains("base-amount")) {
            result.addError(prefix + "缺少 base-amount 字段");
            isValid = false;
        } else {
            int baseAmount = section.getInt("base-amount", 0);
            if (baseAmount <= 0) {
                result.addError(prefix + "base-amount 必须大于 0，当前值: " + baseAmount);
                isValid = false;
            } else if (baseAmount > 10000) {
                result.addWarning(prefix + "base-amount 过大 (" + baseAmount + ")，可能不合理");
            }
        }

        // 验证 min-realm
        if (!section.contains("min-realm")) {
            result.addWarning(prefix + "缺少 min-realm 字段，将使用默认值 1");
        } else {
            int minRealm = section.getInt("min-realm", 1);
            if (minRealm < 1 || minRealm > 8) {
                result.addError(prefix + "min-realm 必须在 1-8 之间，当前值: " + minRealm);
                isValid = false;
            }
        }

        // 验证 target 字段（针对特定任务类型）
        String objectiveStr = section.getString("objective", "");
        if (objectiveStr.equals("COLLECT_ITEM")) {
            String target = section.getString("target", "");
            if (target.isEmpty()) {
                result.addError(prefix + "COLLECT_ITEM 类型必须指定 target");
                isValid = false;
            } else {
                // 验证材料是否有效
                try {
                    Material.valueOf(target.toUpperCase());
                } catch (IllegalArgumentException e) {
                    result.addError(prefix + "无效的材料类型: " + target);
                    isValid = false;
                }
            }
        }

        // 验证奖励配置
        if (!section.contains("rewards")) {
            result.addWarning(prefix + "缺少 rewards 配置");
        } else {
            ConfigurationSection rewards = section.getConfigurationSection("rewards");
            if (rewards != null) {
                validateRewards(taskId, rewards, result);
            }
        }

        // 验证权重
        if (section.contains("weight")) {
            int weight = section.getInt("weight", 10);
            if (weight < 0 || weight > 100) {
                result.addWarning(prefix + "weight 建议在 0-100 之间，当前值: " + weight);
            }
        }

        return isValid;
    }

    /**
     * 验证奖励配置
     */
    private void validateRewards(String taskId, ConfigurationSection rewards, ValidationResult result) {
        String prefix = "任务 " + taskId + " 奖励: ";

        // 检查是否至少有一种奖励
        boolean hasReward = false;
        if (rewards.getInt("contribution", 0) > 0) hasReward = true;
        if (rewards.getInt("spirit-stone", 0) > 0) hasReward = true;
        if (rewards.getInt("activity", 0) > 0) hasReward = true;
        if (rewards.getInt("sect-exp", 0) > 0) hasReward = true;

        if (!hasReward) {
            result.addWarning(prefix + "没有配置任何奖励");
        }

        // 验证奖励数值范围
        validateRewardValue(rewards, "contribution", 0, 1000, prefix, result);
        validateRewardValue(rewards, "spirit-stone", 0, 10000, prefix, result);
        validateRewardValue(rewards, "activity", 0, 500, prefix, result);
        validateRewardValue(rewards, "sect-exp", 0, 1000, prefix, result);
    }

    /**
     * 验证奖励数值
     */
    private void validateRewardValue(ConfigurationSection section, String key, int min, int max, String prefix, ValidationResult result) {
        if (section.contains(key)) {
            int value = section.getInt(key, 0);
            if (value < min || value > max) {
                result.addWarning(prefix + key + " 建议在 " + min + "-" + max + " 之间，当前值: " + value);
            }
        }
    }

    /**
     * 验证境界配置
     */
    private void validateRealms(FileConfiguration config, ValidationResult result) {
        if (!config.contains("realms")) {
            result.addWarning("缺少 realms 配置，将使用默认境界列表");
            return;
        }

        List<?> realms = config.getList("realms");
        if (realms == null || realms.isEmpty()) {
            result.addWarning("realms 配置为空");
            return;
        }

        Set<Integer> levels = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (int i = 0; i < realms.size(); i++) {
            Object realmObj = realms.get(i);
            if (!(realmObj instanceof ConfigurationSection)) {
                continue;
            }

            ConfigurationSection realm = (ConfigurationSection) realmObj;
            String name = realm.getString("name");
            int level = realm.getInt("level", -1);

            if (name == null || name.isEmpty()) {
                result.addError("境界 #" + i + " 缺少 name");
            } else if (names.contains(name)) {
                result.addError("境界名称重复: " + name);
            } else {
                names.add(name);
            }

            if (level < 1) {
                result.addError("境界 " + name + " 的 level 无效: " + level);
            } else if (levels.contains(level)) {
                result.addError("境界等级重复: " + level);
            } else {
                levels.add(level);
            }
        }

        if (levels.size() < 3) {
            result.addWarning("境界数量过少 (" + levels.size() + ")，建议至少配置 3 个");
        }
    }

    /**
     * 验证结果类
     */
    @Getter
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addInfo(String info) {
            infos.add(info);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getWarningCount() {
            return warnings.size();
        }

        /**
         * 打印验证结果
         */
        public void printResults(XianCore plugin) {
            if (!infos.isEmpty()) {
                plugin.getLogger().info("§b配置信息:");
                for (String info : infos) {
                    plugin.getLogger().info("  §7" + info);
                }
            }

            if (hasWarnings()) {
                plugin.getLogger().warning("§e配置警告 (" + warnings.size() + " 个):");
                for (String warning : warnings) {
                    plugin.getLogger().warning("  §e⚠ " + warning);
                }
            }

            if (!isValid()) {
                plugin.getLogger().severe("§c配置错误 (" + errors.size() + " 个):");
                for (String error : errors) {
                    plugin.getLogger().severe("  §c✗ " + error);
                }
            }
        }

        /**
         * 生成摘要
         */
        public String getSummary() {
            if (isValid() && !hasWarnings()) {
                return "§a✓ 配置验证通过";
            } else if (isValid()) {
                return "§e⚠ 配置验证通过，但有 " + warnings.size() + " 个警告";
            } else {
                return "§c✗ 配置验证失败: " + errors.size() + " 个错误, " + warnings.size() + " 个警告";
            }
        }
    }
}





