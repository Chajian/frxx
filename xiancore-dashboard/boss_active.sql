/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3307
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3307
 Source Schema         : xiancore

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 15/12/2025 20:09:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for _prisma_migrations
-- ----------------------------
DROP TABLE IF EXISTS `_prisma_migrations`;
CREATE TABLE `_prisma_migrations`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `finished_at` datetime(3) NULL DEFAULT NULL,
  `migration_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `logs` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `rolled_back_at` datetime(3) NULL DEFAULT NULL,
  `started_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `applied_steps_count` int UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for boss_active
-- ----------------------------
DROP TABLE IF EXISTS `boss_active`;
CREATE TABLE `boss_active`  (
  `boss_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `spawn_point_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `mythic_mob_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tier` int NOT NULL,
  `world` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `x` int NOT NULL,
  `y` int NOT NULL,
  `z` int NOT NULL,
  `spawn_time` bigint NOT NULL,
  `current_health` double NULL DEFAULT NULL,
  `max_health` double NULL DEFAULT NULL,
  PRIMARY KEY (`boss_uuid`) USING BTREE,
  INDEX `idx_active_spawn_point`(`spawn_point_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for boss_kill_history
-- ----------------------------
DROP TABLE IF EXISTS `boss_kill_history`;
CREATE TABLE `boss_kill_history`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `boss_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `spawn_point_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `mythic_mob_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tier` int NOT NULL,
  `killer_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `killer_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `total_damage` double NOT NULL DEFAULT 0,
  `alive_duration` bigint NOT NULL DEFAULT 0,
  `participant_count` int NOT NULL DEFAULT 0,
  `participants_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `spawn_time` bigint NOT NULL,
  `kill_time` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_spawn_point`(`spawn_point_id` ASC) USING BTREE,
  INDEX `idx_killer`(`killer_uuid` ASC) USING BTREE,
  INDEX `idx_kill_time`(`kill_time` ASC) USING BTREE,
  INDEX `idx_mythic_mob_history`(`mythic_mob_id` ASC) USING BTREE,
  CONSTRAINT `boss_kill_history_spawn_point_id_fkey` FOREIGN KEY (`spawn_point_id`) REFERENCES `boss_spawn_points` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for boss_reward_configs
-- ----------------------------
DROP TABLE IF EXISTS `boss_reward_configs`;
CREATE TABLE `boss_reward_configs`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `tier` int NOT NULL,
  `rank` int NOT NULL,
  `multiplier` double NOT NULL DEFAULT 1,
  `rewards_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `boss_reward_configs_tier_rank_key`(`tier` ASC, `rank` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for boss_spawn_points
-- ----------------------------
DROP TABLE IF EXISTS `boss_spawn_points`;
CREATE TABLE `boss_spawn_points`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `world` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `x` int NOT NULL,
  `y` int NOT NULL,
  `z` int NOT NULL,
  `mythic_mob_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tier` int NOT NULL DEFAULT 1,
  `cooldown_seconds` bigint NOT NULL DEFAULT 7200,
  `max_count` int NOT NULL DEFAULT 1,
  `random_location` tinyint(1) NOT NULL DEFAULT 0,
  `spawn_radius` int NOT NULL DEFAULT 100,
  `random_radius` int NOT NULL DEFAULT 0,
  `spawn_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'fixed',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `pre_spawn_warning` int NOT NULL DEFAULT 30,
  `spawn_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `kill_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `last_spawn_time` bigint NOT NULL DEFAULT 0,
  `current_count` int NOT NULL DEFAULT 0,
  `total_spawns` int NOT NULL DEFAULT 0,
  `created_at` bigint NULL DEFAULT NULL,
  `updated_at` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mythic_mob`(`mythic_mob_id` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_tier`(`tier` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_boss_refresh_config
-- ----------------------------
DROP TABLE IF EXISTS `xian_boss_refresh_config`;
CREATE TABLE `xian_boss_refresh_config`  (
  `id` int NOT NULL DEFAULT 1,
  `updated_at` bigint NULL DEFAULT NULL,
  `check_interval_seconds` int NOT NULL DEFAULT 300,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `max_active_bosses` int NOT NULL DEFAULT 10,
  `min_online_players` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_boss_spawn_points
-- ----------------------------
DROP TABLE IF EXISTS `xian_boss_spawn_points`;
CREATE TABLE `xian_boss_spawn_points`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `world_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `x` double NOT NULL,
  `y` double NOT NULL,
  `z` double NOT NULL,
  `mythic_mob_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tier` int NOT NULL DEFAULT 1,
  `cooldown_seconds` bigint NOT NULL DEFAULT 7200,
  `max_count` int NOT NULL DEFAULT 1,
  `current_count` int NOT NULL DEFAULT 0,
  `last_spawn_time` bigint NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `spawn_mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'fixed',
  `random_radius` int NOT NULL DEFAULT 0,
  `auto_find_ground` tinyint(1) NOT NULL DEFAULT 0,
  `min_distance` int NOT NULL DEFAULT 50,
  `max_distance` int NOT NULL DEFAULT 200,
  `regions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `enable_smart_scoring` tinyint(1) NOT NULL DEFAULT 0,
  `preferred_biomes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `biome_weight` double NOT NULL DEFAULT 0.2,
  `spiritual_energy_weight` double NOT NULL DEFAULT 0.3,
  `player_density_weight` double NOT NULL DEFAULT 0.2,
  `openness_weight` double NOT NULL DEFAULT 0.3,
  `min_score` double NOT NULL DEFAULT 0.4,
  `created_at` bigint NULL DEFAULT NULL,
  `updated_at` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_player_equipment
-- ----------------------------
DROP TABLE IF EXISTS `xian_player_equipment`;
CREATE TABLE `xian_player_equipment`  (
  `player_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `slot` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `equipment_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`player_uuid`, `slot`) USING BTREE,
  INDEX `idx_player`(`player_uuid` ASC) USING BTREE,
  CONSTRAINT `xian_player_equipment_player_uuid_fkey` FOREIGN KEY (`player_uuid`) REFERENCES `xian_players` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_player_skill_binds
-- ----------------------------
DROP TABLE IF EXISTS `xian_player_skill_binds`;
CREATE TABLE `xian_player_skill_binds`  (
  `player_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `slot` int NOT NULL,
  `skill_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`player_uuid`, `slot`) USING BTREE,
  INDEX `idx_player`(`player_uuid` ASC) USING BTREE,
  CONSTRAINT `xian_player_skill_binds_player_uuid_fkey` FOREIGN KEY (`player_uuid`) REFERENCES `xian_players` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_player_skills
-- ----------------------------
DROP TABLE IF EXISTS `xian_player_skills`;
CREATE TABLE `xian_player_skills`  (
  `player_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `skill_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `skill_level` int NOT NULL DEFAULT 1,
  PRIMARY KEY (`player_uuid`, `skill_id`) USING BTREE,
  INDEX `idx_player`(`player_uuid` ASC) USING BTREE,
  CONSTRAINT `xian_player_skills_player_uuid_fkey` FOREIGN KEY (`player_uuid`) REFERENCES `xian_players` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_players
-- ----------------------------
DROP TABLE IF EXISTS `xian_players`;
CREATE TABLE `xian_players`  (
  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `realm` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '炼气期',
  `realm_stage` int NOT NULL DEFAULT 1,
  `qi` bigint NOT NULL DEFAULT 0,
  `spiritual_root` double NOT NULL DEFAULT 0.5,
  `spiritual_root_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `comprehension` double NOT NULL DEFAULT 0.5,
  `technique_adaptation` double NOT NULL DEFAULT 0.6,
  `spirit_stones` bigint NOT NULL DEFAULT 0,
  `contribution_points` int NOT NULL DEFAULT 0,
  `skill_points` int NOT NULL DEFAULT 0,
  `player_level` int NOT NULL DEFAULT 1,
  `sect_id` int NULL DEFAULT NULL,
  `sect_rank` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'member',
  `last_login` bigint NULL DEFAULT NULL,
  `created_at` bigint NULL DEFAULT NULL,
  `updated_at` bigint NULL DEFAULT NULL,
  `breakthrough_attempts` int NOT NULL DEFAULT 0,
  `successful_breakthroughs` int NOT NULL DEFAULT 0,
  `active_qi` bigint NOT NULL DEFAULT 0,
  `last_fate_time` bigint NOT NULL DEFAULT 0,
  `fate_count` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`uuid`) USING BTREE,
  INDEX `idx_sect`(`sect_id` ASC) USING BTREE,
  INDEX `idx_realm`(`realm` ASC) USING BTREE,
  INDEX `idx_last_login`(`last_login` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_sect_facilities
-- ----------------------------
DROP TABLE IF EXISTS `xian_sect_facilities`;
CREATE TABLE `xian_sect_facilities`  (
  `sect_id` int NOT NULL,
  `facility_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `level` int NOT NULL DEFAULT 1,
  `upgraded_at` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`sect_id`, `facility_type`) USING BTREE,
  INDEX `idx_sect`(`sect_id` ASC) USING BTREE,
  INDEX `idx_type_level`(`facility_type` ASC, `level` ASC) USING BTREE,
  CONSTRAINT `xian_sect_facilities_sect_id_fkey` FOREIGN KEY (`sect_id`) REFERENCES `xian_sects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_sect_members
-- ----------------------------
DROP TABLE IF EXISTS `xian_sect_members`;
CREATE TABLE `xian_sect_members`  (
  `sect_id` int NOT NULL,
  `player_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `player_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `rank` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OUTER_DISCIPLE',
  `contribution` int NOT NULL DEFAULT 0,
  `weekly_contribution` int NOT NULL DEFAULT 0,
  `joined_at` bigint NULL DEFAULT NULL,
  `last_active_at` bigint NULL DEFAULT NULL,
  `tasks_completed` int NOT NULL DEFAULT 0,
  `donation_count` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`sect_id`, `player_uuid`) USING BTREE,
  INDEX `idx_sect`(`sect_id` ASC) USING BTREE,
  INDEX `idx_player`(`player_uuid` ASC) USING BTREE,
  CONSTRAINT `xian_sect_members_sect_id_fkey` FOREIGN KEY (`sect_id`) REFERENCES `xian_sects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_sect_warehouses
-- ----------------------------
DROP TABLE IF EXISTS `xian_sect_warehouses`;
CREATE TABLE `xian_sect_warehouses`  (
  `sect_id` int NOT NULL,
  `capacity` int NOT NULL DEFAULT 54,
  `items_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `last_modified` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`sect_id`) USING BTREE,
  CONSTRAINT `xian_sect_warehouses_sect_id_fkey` FOREIGN KEY (`sect_id`) REFERENCES `xian_sects` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_sects
-- ----------------------------
DROP TABLE IF EXISTS `xian_sects`;
CREATE TABLE `xian_sects`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `owner_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `level` int NOT NULL DEFAULT 1,
  `experience` bigint NOT NULL DEFAULT 0,
  `sect_funds` bigint NOT NULL DEFAULT 0,
  `sect_contribution` int NOT NULL DEFAULT 0,
  `max_members` int NOT NULL DEFAULT 10,
  `recruiting` tinyint(1) NOT NULL DEFAULT 1,
  `pvp_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `announcement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `residence_land_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `land_center_world` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `land_center_x` double NULL DEFAULT 0,
  `land_center_y` double NULL DEFAULT 0,
  `land_center_z` double NULL DEFAULT 0,
  `last_maintenance_time` bigint NOT NULL DEFAULT 0,
  `building_slots_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `created_at` bigint NULL DEFAULT NULL,
  `updated_at` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_owner`(`owner_uuid` ASC) USING BTREE,
  INDEX `idx_residence`(`residence_land_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for xian_tribulations
-- ----------------------------
DROP TABLE IF EXISTS `xian_tribulations`;
CREATE TABLE `xian_tribulations`  (
  `tribulation_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `player_uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `world_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `x` double NOT NULL,
  `y` double NOT NULL,
  `z` double NOT NULL,
  `current_wave` int NOT NULL DEFAULT 0,
  `total_waves` int NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT 0,
  `completed` tinyint(1) NOT NULL DEFAULT 0,
  `failed` tinyint(1) NOT NULL DEFAULT 0,
  `start_time` bigint NULL DEFAULT NULL,
  `end_time` bigint NULL DEFAULT NULL,
  `last_wave_time` bigint NULL DEFAULT NULL,
  `total_damage_dealt` double NOT NULL DEFAULT 0,
  `total_damage_taken` double NOT NULL DEFAULT 0,
  PRIMARY KEY (`tribulation_uuid`) USING BTREE,
  INDEX `idx_player`(`player_uuid` ASC) USING BTREE,
  INDEX `idx_active`(`active` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
