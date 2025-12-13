-- XianCore 缺失表创建脚本
-- 用于手动创建迁移所需的新表（移除外键约束以避免兼容性问题）

USE xiancore;

-- 1. 玩家技能绑定表（如果已存在则删除）
DROP TABLE IF EXISTS xian_player_skill_binds;
CREATE TABLE xian_player_skill_binds (
    player_uuid VARCHAR(36) NOT NULL,
    slot INT NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (player_uuid, slot),
    INDEX idx_player (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 2. 宗门设施表（如果已存在则删除）
DROP TABLE IF EXISTS xian_sect_facilities;
CREATE TABLE xian_sect_facilities (
    sect_id INT NOT NULL,
    facility_type VARCHAR(32) NOT NULL,
    level INT DEFAULT 1,
    upgraded_at BIGINT,
    PRIMARY KEY (sect_id, facility_type),
    INDEX idx_sect (sect_id),
    INDEX idx_type_level (facility_type, level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 3. 宗门仓库表（如果已存在则删除）
DROP TABLE IF EXISTS xian_sect_warehouses;
CREATE TABLE xian_sect_warehouses (
    sect_id INT PRIMARY KEY,
    capacity INT DEFAULT 54,
    items_json LONGTEXT,
    last_modified BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 验证表已创建
SELECT 'Tables created successfully!' AS status;
SHOW TABLES LIKE 'xian_%';
