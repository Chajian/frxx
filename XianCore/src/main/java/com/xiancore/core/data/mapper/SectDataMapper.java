package com.xiancore.core.data.mapper;

import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 宗门数据映射器
 * 负责 Sect 与 SQL/YAML 之间的转换
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SectDataMapper {

    /**
     * 从 ResultSet 映射到 Sect（不包含成员）
     *
     * @param rs 结果集
     * @return Sect 对象
     * @throws SQLException SQL 异常
     */
    public Sect mapFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        UUID ownerUuid = UUID.fromString(rs.getString("owner_uuid"));
        String ownerName = rs.getString("owner_name");

        // 从数据库加载时不自动添加宗主（成员会从 sect_members 表加载）
        Sect sect = new Sect(id, name, ownerUuid, ownerName, false);

        sect.setDescription(rs.getString("description"));
        sect.setLevel(rs.getInt("level"));
        sect.setExperience(rs.getLong("experience"));
        sect.setSectFunds(rs.getLong("sect_funds"));
        sect.setSectContribution(rs.getInt("sect_contribution"));
        sect.setMaxMembers(rs.getInt("max_members"));
        sect.setRecruiting(rs.getBoolean("recruiting"));
        sect.setPvpEnabled(rs.getBoolean("pvp_enabled"));
        sect.setAnnouncement(rs.getString("announcement"));
        sect.setCreatedAt(rs.getLong("created_at"));
        sect.setUpdatedAt(rs.getLong("updated_at"));

        // 加载领地相关数据
        sect.setResidenceLandId(rs.getString("residence_land_id"));
        String worldName = rs.getString("land_center_world");
        if (worldName != null && !worldName.isEmpty()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = rs.getDouble("land_center_x");
                double y = rs.getDouble("land_center_y");
                double z = rs.getDouble("land_center_z");
                sect.setLandCenter(new Location(world, x, y, z));
            }
        }

        // 加载维护费相关
        sect.setLastMaintenanceTime(rs.getLong("last_maintenance_time"));

        // 加载建筑位数据
        String buildingSlotsJson = rs.getString("building_slots_data");
        if (buildingSlotsJson != null && !buildingSlotsJson.isEmpty()) {
            try {
                Map<String, Integer> buildingSlots = new HashMap<>();
                String[] pairs = buildingSlotsJson.split(";");
                for (String pair : pairs) {
                    if (pair.contains(":")) {
                        String[] kv = pair.split(":");
                        buildingSlots.put(kv[0], Integer.parseInt(kv[1]));
                    }
                }
                sect.setBuildingSlots(buildingSlots);
            } catch (Exception ignored) {
            }
        }

        return sect;
    }

    /**
     * 绑定 Sect 到 PreparedStatement（完整版，用于保存）
     *
     * @param pstmt PreparedStatement
     * @param sect  宗门数据
     * @throws SQLException SQL 异常
     */
    public void bindForSave(PreparedStatement pstmt, Sect sect) throws SQLException {
        int i = 1;

        pstmt.setInt(i++, sect.getId());
        pstmt.setString(i++, sect.getName());
        pstmt.setString(i++, sect.getDescription());
        pstmt.setString(i++, sect.getOwnerId().toString());
        pstmt.setString(i++, sect.getOwnerName());
        pstmt.setInt(i++, sect.getLevel());
        pstmt.setLong(i++, sect.getExperience());
        pstmt.setLong(i++, sect.getSectFunds());
        pstmt.setInt(i++, sect.getSectContribution());
        pstmt.setInt(i++, sect.getMaxMembers());
        pstmt.setBoolean(i++, sect.isRecruiting());
        pstmt.setBoolean(i++, sect.isPvpEnabled());
        pstmt.setString(i++, sect.getAnnouncement());

        // 保存领地相关数据
        pstmt.setString(i++, sect.getResidenceLandId());
        if (sect.getLandCenter() != null) {
            pstmt.setString(i++, sect.getLandCenter().getWorld().getName());
            pstmt.setDouble(i++, sect.getLandCenter().getX());
            pstmt.setDouble(i++, sect.getLandCenter().getY());
            pstmt.setDouble(i++, sect.getLandCenter().getZ());
        } else {
            pstmt.setString(i++, null);
            pstmt.setDouble(i++, 0);
            pstmt.setDouble(i++, 0);
            pstmt.setDouble(i++, 0);
        }

        // 维护费相关
        pstmt.setLong(i++, sect.getLastMaintenanceTime());

        // 建筑位数据 - 使用简单的序列化格式 (key1:value1;key2:value2)
        StringBuilder buildingSlotsStr = new StringBuilder();
        for (Map.Entry<String, Integer> entry : sect.getBuildingSlots().entrySet()) {
            if (buildingSlotsStr.length() > 0) {
                buildingSlotsStr.append(";");
            }
            buildingSlotsStr.append(entry.getKey()).append(":").append(entry.getValue());
        }
        pstmt.setString(i++, buildingSlotsStr.toString());

        pstmt.setLong(i++, sect.getCreatedAt());
        pstmt.setLong(i++, sect.getUpdatedAt());
    }

    /**
     * 绑定 Sect 到 PreparedStatement（简化版，用于事务保存）
     *
     * @param pstmt PreparedStatement
     * @param sect  宗门数据
     * @throws SQLException SQL 异常
     */
    public void bindForSaveSimple(PreparedStatement pstmt, Sect sect) throws SQLException {
        int i = 1;

        pstmt.setInt(i++, sect.getId());
        pstmt.setString(i++, sect.getName());
        pstmt.setString(i++, sect.getDescription());
        pstmt.setString(i++, sect.getOwnerId().toString());
        pstmt.setString(i++, sect.getOwnerName());
        pstmt.setInt(i++, sect.getLevel());
        pstmt.setLong(i++, sect.getExperience());
        pstmt.setLong(i++, sect.getSectFunds());
        pstmt.setInt(i++, sect.getSectContribution());
        pstmt.setInt(i++, sect.getMaxMembers());
        pstmt.setBoolean(i++, sect.isRecruiting());
        pstmt.setBoolean(i++, sect.isPvpEnabled());
        pstmt.setString(i++, sect.getAnnouncement());
        pstmt.setLong(i++, sect.getCreatedAt());
        pstmt.setLong(i++, sect.getUpdatedAt());
    }

    /**
     * 从 ResultSet 映射到 SectMember
     *
     * @param rs 结果集
     * @return SectMember 对象
     * @throws SQLException SQL 异常
     */
    public SectMember mapMemberFromResultSet(ResultSet rs) throws SQLException {
        UUID playerUuid = UUID.fromString(rs.getString("player_uuid"));
        String playerName = rs.getString("player_name");

        SectMember member = new SectMember(playerUuid, playerName);
        member.setRank(SectRank.valueOf(rs.getString("rank")));
        member.setContribution(rs.getInt("contribution"));
        member.setWeeklyContribution(rs.getInt("weekly_contribution"));
        member.setJoinedAt(rs.getLong("joined_at"));
        member.setLastActiveAt(rs.getLong("last_active_at"));
        member.setTasksCompleted(rs.getInt("tasks_completed"));
        member.setDonationCount(rs.getInt("donation_count"));

        return member;
    }

    /**
     * 从 YamlConfiguration 映射到 Sect
     *
     * @param config YAML 配置
     * @return Sect 对象
     */
    public Sect mapFromYaml(YamlConfiguration config) {
        int id = config.getInt("id");
        String name = config.getString("name");
        UUID ownerUuid = UUID.fromString(config.getString("owner_uuid"));
        String ownerName = config.getString("owner_name");

        // 从文件加载时不自动添加宗主（成员会从文件中加载）
        Sect sect = new Sect(id, name, ownerUuid, ownerName, false);

        sect.setDescription(config.getString("description", ""));
        sect.setLevel(config.getInt("level", 1));
        sect.setExperience(config.getLong("experience", 0));
        sect.setSectFunds(config.getLong("sect_funds", 0));
        sect.setSectContribution(config.getInt("sect_contribution", 0));
        sect.setMaxMembers(config.getInt("max_members", 10));
        sect.setRecruiting(config.getBoolean("recruiting", true));
        sect.setPvpEnabled(config.getBoolean("pvp_enabled", false));
        sect.setAnnouncement(config.getString("announcement", ""));
        sect.setCreatedAt(config.getLong("created_at", System.currentTimeMillis()));
        sect.setUpdatedAt(config.getLong("updated_at", System.currentTimeMillis()));

        // 加载领地相关数据
        sect.setResidenceLandId(config.getString("residence_land_id", null));
        if (config.contains("land_center.world")) {
            String worldName = config.getString("land_center.world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = config.getDouble("land_center.x", 0);
                double y = config.getDouble("land_center.y", 0);
                double z = config.getDouble("land_center.z", 0);
                sect.setLandCenter(new Location(world, x, y, z));
            }
        }

        // 加载维护费相关
        sect.setLastMaintenanceTime(config.getLong("last_maintenance_time", 0));

        // 加载建筑位数据
        if (config.contains("building_slots")) {
            Map<String, Integer> buildingSlots = new HashMap<>();
            ConfigurationSection slotsSection = config.getConfigurationSection("building_slots");
            if (slotsSection != null) {
                for (String key : slotsSection.getKeys(false)) {
                    buildingSlots.put(key, config.getInt("building_slots." + key, 0));
                }
            }
            sect.setBuildingSlots(buildingSlots);
        }

        // 加载成员数据
        if (config.contains("members")) {
            ConfigurationSection membersSection = config.getConfigurationSection("members");
            if (membersSection != null) {
                for (String uuidStr : membersSection.getKeys(false)) {
                    String memberPath = "members." + uuidStr;
                    UUID playerUuid = UUID.fromString(uuidStr);
                    String playerName = config.getString(memberPath + ".name");

                    SectMember member = new SectMember(playerUuid, playerName);
                    member.setRank(SectRank.valueOf(config.getString(memberPath + ".rank", "OUTER_DISCIPLE")));
                    member.setContribution(config.getInt(memberPath + ".contribution", 0));
                    member.setWeeklyContribution(config.getInt(memberPath + ".weekly_contribution", 0));
                    member.setJoinedAt(config.getLong(memberPath + ".joined_at", System.currentTimeMillis()));
                    member.setLastActiveAt(config.getLong(memberPath + ".last_active_at", System.currentTimeMillis()));
                    member.setTasksCompleted(config.getInt(memberPath + ".tasks_completed", 0));
                    member.setDonationCount(config.getInt(memberPath + ".donation_count", 0));

                    sect.getMembers().put(playerUuid, member);
                }
            }
        }

        return sect;
    }

    /**
     * 将 Sect 映射到 YamlConfiguration
     *
     * @param config YAML 配置
     * @param sect   宗门数据
     */
    public void mapToYaml(YamlConfiguration config, Sect sect) {
        // 保存基础数据
        config.set("id", sect.getId());
        config.set("name", sect.getName());
        config.set("description", sect.getDescription());
        config.set("owner_uuid", sect.getOwnerId().toString());
        config.set("owner_name", sect.getOwnerName());
        config.set("level", sect.getLevel());
        config.set("experience", sect.getExperience());
        config.set("sect_funds", sect.getSectFunds());
        config.set("sect_contribution", sect.getSectContribution());
        config.set("max_members", sect.getMaxMembers());
        config.set("recruiting", sect.isRecruiting());
        config.set("pvp_enabled", sect.isPvpEnabled());
        config.set("announcement", sect.getAnnouncement());
        config.set("created_at", sect.getCreatedAt());
        config.set("updated_at", sect.getUpdatedAt());

        // 保存领地相关数据
        config.set("residence_land_id", sect.getResidenceLandId());
        if (sect.getLandCenter() != null) {
            config.set("land_center.world", sect.getLandCenter().getWorld().getName());
            config.set("land_center.x", sect.getLandCenter().getX());
            config.set("land_center.y", sect.getLandCenter().getY());
            config.set("land_center.z", sect.getLandCenter().getZ());
        }

        // 保存维护费相关
        config.set("last_maintenance_time", sect.getLastMaintenanceTime());
        config.set("building_slots", sect.getBuildingSlots());

        // 保存成员数据
        for (SectMember member : sect.getMemberList()) {
            String memberPath = "members." + member.getPlayerId().toString();
            config.set(memberPath + ".name", member.getPlayerName());
            config.set(memberPath + ".rank", member.getRank().name());
            config.set(memberPath + ".contribution", member.getContribution());
            config.set(memberPath + ".weekly_contribution", member.getWeeklyContribution());
            config.set(memberPath + ".joined_at", member.getJoinedAt());
            config.set(memberPath + ".last_active_at", member.getLastActiveAt());
            config.set(memberPath + ".tasks_completed", member.getTasksCompleted());
            config.set(memberPath + ".donation_count", member.getDonationCount());
        }
    }
}
