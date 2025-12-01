-- XianCore 初始数据库架构脚本
-- 创建日期: 2025-11-16
-- 版本: 1.0.0

-- ============================================
-- Boss 表
-- ============================================
CREATE TABLE IF NOT EXISTS bosses (
  id VARCHAR(36) PRIMARY KEY COMMENT 'Boss唯一ID',
  name VARCHAR(100) NOT NULL COMMENT 'Boss名称',
  type VARCHAR(50) COMMENT 'Boss类型',
  status VARCHAR(20) NOT NULL COMMENT 'Boss状态: SPAWNED, ALIVE, DEAD, DESPAWNED',
  world VARCHAR(100) COMMENT '所在世界名称',
  x DOUBLE COMMENT 'X坐标',
  y DOUBLE COMMENT 'Y坐标',
  z DOUBLE COMMENT 'Z坐标',
  current_health DOUBLE COMMENT '当前血量',
  max_health DOUBLE COMMENT '最大血量',
  total_damage DOUBLE DEFAULT 0 COMMENT '受到的总伤害',
  difficulty_level INT COMMENT '难度等级 (1-5)',
  spawned_time BIGINT COMMENT 'Boss生成时间戳',
  killed_time BIGINT COMMENT 'Boss被击杀时间戳',
  killer_player_id VARCHAR(36) COMMENT '击杀者玩家ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引定义
  INDEX idx_status (status),
  INDEX idx_world (world),
  INDEX idx_spawned_time (spawned_time),
  INDEX idx_killed_time (killed_time),
  INDEX idx_killer_player_id (killer_player_id)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Boss表';

-- ============================================
-- DamageRecord 表
-- ============================================
CREATE TABLE IF NOT EXISTS damage_records (
  id VARCHAR(36) PRIMARY KEY COMMENT '伤害记录ID',
  boss_id VARCHAR(36) NOT NULL COMMENT 'Boss ID',
  player_id VARCHAR(36) NOT NULL COMMENT '玩家ID (UUID)',
  player_name VARCHAR(100) COMMENT '玩家名称',
  damage DOUBLE NOT NULL COMMENT '伤害值',
  damage_time BIGINT NOT NULL COMMENT '伤害发生时间戳',
  damage_type VARCHAR(50) COMMENT '伤害类型: PHYSICAL, MAGICAL, TRUE_DAMAGE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

  -- 外键关系
  CONSTRAINT fk_damage_boss FOREIGN KEY (boss_id) REFERENCES bosses(id) ON DELETE CASCADE,

  -- 索引定义
  INDEX idx_boss_id (boss_id),
  INDEX idx_player_id (player_id),
  INDEX idx_damage_time (damage_time),
  INDEX idx_damage_type (damage_type)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='伤害记录表';

-- ============================================
-- PlayerStats 表
-- ============================================
CREATE TABLE IF NOT EXISTS player_stats (
  id VARCHAR(36) PRIMARY KEY COMMENT '统计记录ID',
  player_id VARCHAR(36) UNIQUE NOT NULL COMMENT '玩家ID (Minecraft UUID)',
  player_name VARCHAR(100) COMMENT '玩家名称',
  boss_kills INT DEFAULT 0 COMMENT 'Boss击杀总数',
  total_damage DOUBLE DEFAULT 0 COMMENT '对Boss总伤害',
  total_battles INT DEFAULT 0 COMMENT '参与的总战斗数',
  balance DOUBLE DEFAULT 0 COMMENT '当前余额 (游戏币)',
  total_earned DOUBLE DEFAULT 0 COMMENT '总收入 (游戏币)',
  total_spent DOUBLE DEFAULT 0 COMMENT '总支出 (游戏币)',
  kill_ranking INT COMMENT 'Boss击杀数排名',
  wealth_ranking INT COMMENT '财富排名',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  -- 索引定义
  UNIQUE INDEX idx_player_id (player_id),
  INDEX idx_kill_ranking (kill_ranking),
  INDEX idx_wealth_ranking (wealth_ranking),
  INDEX idx_boss_kills (boss_kills),
  INDEX idx_total_damage (total_damage),
  INDEX idx_balance (balance)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家统计表';

-- ============================================
-- 视图定义
-- ============================================

-- Boss击杀统计视图
CREATE OR REPLACE VIEW v_boss_kill_statistics AS
SELECT
  b.id,
  b.name,
  b.type,
  b.difficulty_level,
  b.spawned_time,
  b.killed_time,
  b.killer_player_id,
  b.total_damage,
  (SELECT COUNT(*) FROM damage_records WHERE boss_id = b.id) as damage_record_count,
  (SELECT COUNT(DISTINCT player_id) FROM damage_records WHERE boss_id = b.id) as participant_count
FROM bosses b
WHERE b.status = 'DEAD'
ORDER BY b.killed_time DESC;

-- 玩家伤害贡献度视图
CREATE OR REPLACE VIEW v_player_damage_contribution AS
SELECT
  p.player_id,
  p.player_name,
  p.total_damage,
  ROUND((p.total_damage * 100.0) / (SELECT SUM(total_damage) FROM player_stats WHERE total_damage > 0), 2) as contribution_percent,
  p.boss_kills,
  p.balance,
  p.kill_ranking
FROM player_stats p
WHERE p.total_damage > 0
ORDER BY p.total_damage DESC;

-- Boss伤害排名视图
CREATE OR REPLACE VIEW v_boss_damage_ranking AS
SELECT
  d.boss_id,
  d.player_id,
  d.player_name,
  SUM(d.damage) as total_damage_to_boss,
  COUNT(*) as hit_count,
  AVG(d.damage) as average_damage,
  ROW_NUMBER() OVER (PARTITION BY d.boss_id ORDER BY SUM(d.damage) DESC) as damage_rank
FROM damage_records d
GROUP BY d.boss_id, d.player_id, d.player_name;

-- ============================================
-- 初始化基础数据 (可选)
-- ============================================
-- 这部分可在后续需要时添加示例数据
