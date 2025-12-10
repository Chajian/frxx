package com.xiancore.core.data.mapper;

import com.xiancore.core.data.PlayerData;
import com.xiancore.core.data.SpiritualRootType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * 玩家数据映射器
 * 负责 PlayerData 与 SQL/YAML 之间的转换
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class PlayerDataMapper {

    /**
     * 从 ResultSet 映射到 PlayerData
     *
     * @param rs   结果集
     * @param uuid 玩家 UUID
     * @return PlayerData 对象
     * @throws SQLException SQL 异常
     */
    public PlayerData mapFromResultSet(ResultSet rs, UUID uuid) throws SQLException {
        PlayerData data = new PlayerData(uuid);

        data.setName(rs.getString("name"));
        data.setRealm(rs.getString("realm"));
        data.setRealmStage(rs.getInt("realm_stage"));
        data.setQi(rs.getLong("qi"));
        data.setSpiritualRoot(rs.getDouble("spiritual_root"));

        // 灵根类型
        String rootTypeStr = rs.getString("spiritual_root_type");
        if (rootTypeStr != null && !rootTypeStr.isEmpty()) {
            try {
                data.setSpiritualRootType(SpiritualRootType.valueOf(rootTypeStr));
            } catch (IllegalArgumentException e) {
                data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
            }
        } else {
            data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
        }

        data.setComprehension(rs.getDouble("comprehension"));
        data.setTechniqueAdaptation(rs.getDouble("technique_adaptation"));
        data.setSpiritStones(rs.getLong("spirit_stones"));
        data.setContributionPoints(rs.getInt("contribution_points"));
        data.setSkillPoints(rs.getInt("skill_points"));
        data.setPlayerLevel(rs.getInt("player_level"));
        data.setSectId(rs.getObject("sect_id", Integer.class));
        data.setSectRank(rs.getString("sect_rank"));
        data.setLastLogin(rs.getLong("last_login"));
        data.setCreatedAt(rs.getLong("created_at"));
        data.setUpdatedAt(rs.getLong("updated_at"));
        data.setBreakthroughAttempts(rs.getInt("breakthrough_attempts"));
        data.setSuccessfulBreakthroughs(rs.getInt("successful_breakthroughs"));
        data.setActiveQi(rs.getLong("active_qi"));
        data.setLastFateTime(rs.getLong("last_fate_time"));
        data.setFateCount(rs.getInt("fate_count"));

        return data;
    }

    /**
     * 绑定 PlayerData 到 PreparedStatement（用于保存）
     *
     * @param pstmt PreparedStatement
     * @param data  玩家数据
     * @throws SQLException SQL 异常
     */
    public void bindForSave(PreparedStatement pstmt, PlayerData data) throws SQLException {
        int i = 1;

        pstmt.setString(i++, data.getUuid().toString());
        pstmt.setString(i++, data.getName());
        pstmt.setString(i++, data.getRealm());
        pstmt.setInt(i++, data.getRealmStage());
        pstmt.setLong(i++, data.getQi());
        pstmt.setDouble(i++, data.getSpiritualRoot());

        // 灵根类型（可空）
        if (data.getSpiritualRootType() != null) {
            pstmt.setString(i++, data.getSpiritualRootType().name());
        } else {
            pstmt.setNull(i++, Types.VARCHAR);
        }

        pstmt.setDouble(i++, data.getComprehension());
        pstmt.setDouble(i++, data.getTechniqueAdaptation());
        pstmt.setLong(i++, data.getSpiritStones());
        pstmt.setInt(i++, data.getContributionPoints());
        pstmt.setInt(i++, data.getSkillPoints());
        pstmt.setInt(i++, data.getPlayerLevel());

        // 宗门ID（可空）
        if (data.getSectId() != null) {
            pstmt.setInt(i++, data.getSectId());
        } else {
            pstmt.setNull(i++, Types.INTEGER);
        }

        pstmt.setString(i++, data.getSectRank());
        pstmt.setLong(i++, data.getLastLogin());
        pstmt.setLong(i++, data.getCreatedAt());
        pstmt.setLong(i++, System.currentTimeMillis()); // updated_at
        pstmt.setInt(i++, data.getBreakthroughAttempts());
        pstmt.setInt(i++, data.getSuccessfulBreakthroughs());
        pstmt.setLong(i++, data.getActiveQi());
        pstmt.setLong(i++, data.getLastFateTime());
        pstmt.setInt(i++, data.getFateCount());
    }

    /**
     * 从 YamlConfiguration 映射到 PlayerData
     *
     * @param config YAML 配置
     * @param uuid   玩家 UUID
     * @return PlayerData 对象
     */
    public PlayerData mapFromYaml(YamlConfiguration config, UUID uuid) {
        PlayerData data = new PlayerData(uuid);

        data.setName(config.getString("name", ""));
        data.setRealm(config.getString("realm", "炼气期"));
        data.setRealmStage(config.getInt("realm_stage", 1));
        data.setQi(config.getLong("qi", 0));
        data.setSpiritualRoot(config.getDouble("spiritual_root", 0.5));

        // 灵根类型
        String rootTypeStr = config.getString("spiritual_root_type");
        if (rootTypeStr != null && !rootTypeStr.isEmpty()) {
            try {
                data.setSpiritualRootType(SpiritualRootType.valueOf(rootTypeStr));
            } catch (IllegalArgumentException e) {
                data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
            }
        } else {
            data.setSpiritualRootType(SpiritualRootType.fromValue(data.getSpiritualRoot()));
        }

        data.setComprehension(config.getDouble("comprehension", 0.5));
        data.setTechniqueAdaptation(config.getDouble("technique_adaptation", 0.6));
        data.setSpiritStones(config.getLong("spirit_stones", 0));
        data.setContributionPoints(config.getInt("contribution_points", 0));
        data.setSkillPoints(config.getInt("skill_points", 0));
        data.setPlayerLevel(config.getInt("player_level", 1));

        if (config.contains("sect_id")) {
            data.setSectId(config.getInt("sect_id"));
        }

        data.setSectRank(config.getString("sect_rank", "member"));
        data.setLastLogin(config.getLong("last_login", System.currentTimeMillis()));
        data.setCreatedAt(config.getLong("created_at", System.currentTimeMillis()));
        data.setUpdatedAt(config.getLong("updated_at", System.currentTimeMillis()));
        data.setBreakthroughAttempts(config.getInt("breakthrough_attempts", 0));
        data.setSuccessfulBreakthroughs(config.getInt("successful_breakthroughs", 0));
        data.setActiveQi(config.getLong("active_qi", 0));
        data.setLastFateTime(config.getLong("last_fate_time", 0));
        data.setFateCount(config.getInt("fate_count", 0));

        // 加载功法
        if (config.contains("learned_skills")) {
            ConfigurationSection skillsSection = config.getConfigurationSection("learned_skills");
            if (skillsSection != null) {
                for (String skillId : skillsSection.getKeys(false)) {
                    data.getLearnedSkills().put(skillId, config.getInt("learned_skills." + skillId, 1));
                }
            }
        }

        // 加载技能绑定
        if (config.contains("skill_bindings")) {
            ConfigurationSection bindingsSection = config.getConfigurationSection("skill_bindings");
            if (bindingsSection != null) {
                for (String slotStr : bindingsSection.getKeys(false)) {
                    try {
                        data.getSkillBindings().put(Integer.parseInt(slotStr),
                                config.getString("skill_bindings." + slotStr));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 加载装备
        if (config.contains("equipment")) {
            ConfigurationSection equipSection = config.getConfigurationSection("equipment");
            if (equipSection != null) {
                for (String slot : equipSection.getKeys(false)) {
                    data.getEquipment().put(slot, config.getString("equipment." + slot));
                }
            }
        }

        return data;
    }

    /**
     * 将 PlayerData 映射到 YamlConfiguration
     *
     * @param config YAML 配置
     * @param data   玩家数据
     */
    public void mapToYaml(YamlConfiguration config, PlayerData data) {
        config.set("name", data.getName());
        config.set("realm", data.getRealm());
        config.set("realm_stage", data.getRealmStage());
        config.set("qi", data.getQi());
        config.set("spiritual_root", data.getSpiritualRoot());

        if (data.getSpiritualRootType() != null) {
            config.set("spiritual_root_type", data.getSpiritualRootType().name());
        }

        config.set("comprehension", data.getComprehension());
        config.set("technique_adaptation", data.getTechniqueAdaptation());
        config.set("spirit_stones", data.getSpiritStones());
        config.set("contribution_points", data.getContributionPoints());
        config.set("skill_points", data.getSkillPoints());
        config.set("player_level", data.getPlayerLevel());

        if (data.getSectId() != null) {
            config.set("sect_id", data.getSectId());
        }

        config.set("sect_rank", data.getSectRank());
        config.set("last_login", data.getLastLogin());
        config.set("created_at", data.getCreatedAt());
        config.set("updated_at", System.currentTimeMillis());
        config.set("breakthrough_attempts", data.getBreakthroughAttempts());
        config.set("successful_breakthroughs", data.getSuccessfulBreakthroughs());
        config.set("active_qi", data.getActiveQi());
        config.set("last_fate_time", data.getLastFateTime());
        config.set("fate_count", data.getFateCount());

        // 功法
        for (var entry : data.getLearnedSkills().entrySet()) {
            config.set("learned_skills." + entry.getKey(), entry.getValue());
        }

        // 技能绑定
        for (var entry : data.getSkillBindings().entrySet()) {
            config.set("skill_bindings." + entry.getKey(), entry.getValue());
        }

        // 装备
        for (var entry : data.getEquipment().entrySet()) {
            config.set("equipment." + entry.getKey(), entry.getValue());
        }
    }
}
