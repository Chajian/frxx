package com.xiancore.core.data.migrate.migrators;

import com.xiancore.XianCore;
import com.xiancore.core.data.migrate.MigrationReport;
import com.xiancore.core.data.migrate.base.AbstractMigrator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 功法配置迁移器
 * 从 skill.yml 迁移功法配置到 MySQL
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class SkillConfigMigrator extends AbstractMigrator {

    private final File skillFile;

    public SkillConfigMigrator(XianCore plugin) {
        super(plugin);
        this.skillFile = new File(plugin.getDataFolder(), "skill.yml");
    }

    @Override
    public String getName() {
        return "功法配置迁移器";
    }

    @Override
    public String getDescription() {
        return "迁移功法配置（从 skill.yml 到 MySQL xian_skills 表）";
    }

    @Override
    public boolean hasDataToMigrate() {
        if (!skillFile.exists()) {
            return false;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(skillFile);
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        return skillsSection != null && !skillsSection.getKeys(false).isEmpty();
    }

    @Override
    public String getPreMigrationSummary() {
        StringBuilder sb = new StringBuilder();

        if (!skillFile.exists()) {
            sb.append("§c未找到 skill.yml 文件\n");
            return sb.toString();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(skillFile);
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        int skillCount = skillsSection != null ? skillsSection.getKeys(false).size() : 0;

        sb.append("§e功法数量: §f").append(skillCount).append(" 个\n");
        sb.append("§e文件大小: §f").append(formatFileSize(skillFile.length())).append("\n");
        sb.append("§e预计耗时: §f").append(estimateMigrationTime());

        return sb.toString();
    }

    @Override
    public MigrationReport migrate(boolean dryRun) {
        MigrationReport report = new MigrationReport();

        plugin.getLogger().info("§e开始迁移功法配置...");
        plugin.getLogger().info("§e模式: " + (dryRun ? "§6预览模式" : "§c真实迁移"));

        // 检查MySQL
        if (!isMySqlAvailable()) {
            plugin.getLogger().severe("§cMySQL未连接！");
            return report;
        }

        // 检查文件
        if (!skillFile.exists()) {
            plugin.getLogger().warning("§c未找到 skill.yml 文件");
            return report;
        }

        // 加载配置
        YamlConfiguration config = YamlConfiguration.loadConfiguration(skillFile);
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");

        if (skillsSection == null) {
            plugin.getLogger().warning("§c未找到 skills 配置节");
            return report;
        }

        // 获取所有功法ID
        var skillIds = skillsSection.getKeys(false);
        report.setTotalFiles(skillIds.size());
        plugin.getLogger().info("§a找到 " + skillIds.size() + " 个功法配置");

        // 开始迁移
        AtomicInteger processed = new AtomicInteger(0);

        for (String skillId : skillIds) {
            try {
                ConfigurationSection skillConfig = skillsSection.getConfigurationSection(skillId);
                if (skillConfig == null) {
                    report.recordFailure(skillId, skillId, "配置节为空");
                    continue;
                }

                // 检查是否已存在
                if (!dryRun && existsInDatabase(skillId)) {
                    plugin.getLogger().info("§7跳过: " + skillId + " (已存在)");
                    report.recordSkipped();
                    continue;
                }

                // 写入数据库
                if (!dryRun) {
                    saveSkillToDatabase(skillId, skillConfig);
                }

                report.recordSuccess();
                processed.incrementAndGet();
                plugin.getLogger().info("§a迁移: " + skillId);

            } catch (Exception e) {
                plugin.getLogger().warning("§c失败: " + skillId + " - " + e.getMessage());
                report.recordFailure(skillId, skillId, e.getMessage());
            }
        }

        report.complete();
        plugin.getLogger().info("§a功法配置迁移完成！");

        return report;
    }

    @Override
    protected long estimateTimeInMillis() {
        if (!skillFile.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(skillFile);
        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        int skillCount = skillsSection != null ? skillsSection.getKeys(false).size() : 0;

        return skillCount * 20L; // 假设每个功法20ms
    }

    /**
     * 检查功法是否已存在
     */
    private boolean existsInDatabase(String skillId) {
        String sql = "SELECT COUNT(*) FROM xian_skills WHERE id = ?";

        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, skillId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("§e检查数据库失败: " + e.getMessage());
        }

        return false;
    }

    /**
     * 保存功法到数据库
     */
    private void saveSkillToDatabase(String skillId, ConfigurationSection config) throws SQLException {
        String sql = """
            INSERT INTO xian_skills (
                id, name, description, type, element,
                mythic_skill_id, use_mythic,
                required_realm, required_level, required_skills_json,
                qi_cost, cooldown, damage, healing, duration, `range`,
                upgrade_cost, upgrade_skill_points, max_level,
                created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            long now = System.currentTimeMillis();

            // 基础信息
            pstmt.setString(1, skillId);
            pstmt.setString(2, stripColorCodes(config.getString("name", skillId)));
            pstmt.setString(3, config.getString("description", ""));
            pstmt.setString(4, config.getString("type", "ATTACK"));
            pstmt.setString(5, config.getString("element", "NEUTRAL"));

            // MythicMobs配置
            pstmt.setString(6, config.getString("mythic-skill-id", ""));
            pstmt.setBoolean(7, config.getBoolean("use-mythic", true));

            // 学习需求
            ConfigurationSection required = config.getConfigurationSection("required");
            pstmt.setString(8, required != null ? required.getString("realm", "炼气期") : "炼气期");
            pstmt.setInt(9, required != null ? required.getInt("level", 0) : 0);

            // 前置功法（JSON数组）
            List<String> requiredSkills = required != null ? required.getStringList("skills") : null;
            if (requiredSkills != null && !requiredSkills.isEmpty()) {
                pstmt.setString(10, "[\"" + String.join("\",\"", requiredSkills) + "\"]");
            } else {
                pstmt.setString(10, null);
            }

            // 消耗
            ConfigurationSection cost = config.getConfigurationSection("cost");
            pstmt.setInt(11, cost != null ? cost.getInt("qi", 0) : 0);
            pstmt.setInt(12, cost != null ? cost.getInt("cooldown", 0) : 0);

            // 效果
            ConfigurationSection effect = config.getConfigurationSection("effect");
            pstmt.setInt(13, effect != null ? effect.getInt("damage", 0) : 0);
            pstmt.setInt(14, effect != null ? effect.getInt("healing", 0) : 0);
            pstmt.setInt(15, effect != null ? effect.getInt("duration", 0) : 0);
            pstmt.setInt(16, effect != null ? effect.getInt("range", 0) : 0);

            // 升级
            ConfigurationSection upgrade = config.getConfigurationSection("upgrade");
            pstmt.setInt(17, upgrade != null ? upgrade.getInt("cost", 0) : 0);
            pstmt.setInt(18, upgrade != null ? upgrade.getInt("skill-points", 1) : 1);

            // 其他
            pstmt.setInt(19, config.getInt("max-level", 10));
            pstmt.setLong(20, now);
            pstmt.setLong(21, now);

            pstmt.executeUpdate();
        }
    }

    /**
     * 移除 Minecraft 颜色代码
     */
    private String stripColorCodes(String text) {
        if (text == null) return "";
        return text.replaceAll("[&§][0-9a-fk-or]", "").trim();
    }
}
