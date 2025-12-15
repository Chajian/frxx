import request from '@/utils/request';

/**
 * 技能信息
 */
export interface MythicSkillInfo {
  raw: string;
  mechanic?: string;
  trigger?: string;
  conditions?: string[];
}

/**
 * 掉落物信息
 */
export interface MythicDropInfo {
  raw: string;
  item?: string;
  amount?: string;
  chance?: number;
}

/**
 * 装备信息
 */
export interface MythicEquipmentInfo {
  slot: string;
  item: string;
}

/**
 * MythicMob 怪物基础信息
 */
export interface MythicMobInfo {
  id: string;
  displayName: string;
  type: string;
  health: number;
  damage: number;
  armor: number;
  movementSpeed?: number;
  knockbackResistance?: number;
  skills?: string[];
  options?: Record<string, any>;
  fileName: string;
}

/**
 * MythicMob 怪物详细信息
 */
export interface MythicMobDetailInfo extends MythicMobInfo {
  rawConfig: Record<string, any>;
  parsedSkills: MythicSkillInfo[];
  drops: MythicDropInfo[];
  dropsTable?: string;
  equipment: MythicEquipmentInfo[];
  aiGoals?: string[];
  aiTargets?: string[];
  disguise?: string;
  levelModifiers?: Record<string, any>;
  faction?: string;
  bossBar?: {
    enabled: boolean;
    title?: string;
    color?: string;
    style?: string;
  };
  hearingRange?: number;
  followRange?: number;
  preventOtherDrops?: boolean;
  preventRandomEquipment?: boolean;
  preventLeashing?: boolean;
  preventSunburn?: boolean;
}

/**
 * Boss 刷新点
 */
export interface BossSpawnPoint {
  id: string;
  name: string;
  description: string | null;
  world: string;
  x: number;
  y: number;
  z: number;
  mythicMobId: string;
  mythicMobInfo: MythicMobInfo | null;
  tier: number;
  cooldownSeconds: string; // BigInt as string
  maxCount: number;
  randomLocation: boolean;
  spawnRadius: number;
  randomRadius: number;
  spawnMode: string;
  enabled: boolean;
  preSpawnWarning: number;
  spawnMessage: string | null;
  killMessage: string | null;
  lastSpawnTime: string;
  currentCount: number;
  totalSpawns: number;
  createdAt: string | null;
  updatedAt: string | null;
  remainingCooldown: number;
  isReadyToSpawn: boolean;
}

/**
 * 创建刷新点的输入数据
 */
export interface CreateSpawnPointInput {
  id: string;
  name: string;
  description?: string;
  world: string;
  x: number;
  y: number;
  z: number;
  mythicMobId: string;
  tier?: number;
  cooldownSeconds?: number;
  maxCount?: number;
  randomLocation?: boolean;
  spawnRadius?: number;
  randomRadius?: number;
  spawnMode?: string;
  enabled?: boolean;
  preSpawnWarning?: number;
  spawnMessage?: string;
  killMessage?: string;
}

/**
 * 更新刷新点的输入数据
 */
export interface UpdateSpawnPointInput {
  name?: string;
  description?: string;
  world?: string;
  x?: number;
  y?: number;
  z?: number;
  mythicMobId?: string;
  tier?: number;
  cooldownSeconds?: number;
  maxCount?: number;
  randomLocation?: boolean;
  spawnRadius?: number;
  randomRadius?: number;
  spawnMode?: string;
  enabled?: boolean;
  preSpawnWarning?: number;
  spawnMessage?: string;
  killMessage?: string;
}

/**
 * Boss 统计数据
 */
export interface BossStats {
  totalSpawnPoints: number;
  enabledSpawnPoints: number;
  disabledSpawnPoints: number;
  activeBosses: number;
  totalKills: number;
  todayKills: number;
  tierDistribution: Record<number, number>;
}

/**
 * Boss 击杀历史
 */
export interface BossKillHistory {
  id: number;
  bossUuid: string;
  spawnPointId: string;
  mythicMobId: string;
  tier: number;
  killerUuid: string | null;
  killerName: string | null;
  totalDamage: number;
  aliveDuration: string;
  participantCount: number;
  participantsJson: string | null;
  spawnTime: string;
  killTime: string;
  spawnPoint?: { name: string };
}

/**
 * MythicMobs 状态
 */
export interface MythicMobsStatus {
  configured: boolean;
  path: string;
  cacheSize: number;
  cacheAge: number;
}

/**
 * Boss API
 */
export const bossApi = {
  // ==================== 刷新点 ====================

  /** 获取所有刷新点 */
  getSpawnPoints(): Promise<BossSpawnPoint[]> {
    return request.get('/boss/spawn-points');
  },

  /** 获取单个刷新点 */
  getSpawnPoint(id: string): Promise<BossSpawnPoint> {
    return request.get(`/boss/spawn-points/${id}`);
  },

  /** 创建刷新点 */
  createSpawnPoint(data: CreateSpawnPointInput): Promise<BossSpawnPoint> {
    return request.post('/boss/spawn-points', data);
  },

  /** 更新刷新点 */
  updateSpawnPoint(id: string, data: UpdateSpawnPointInput): Promise<BossSpawnPoint> {
    return request.put(`/boss/spawn-points/${id}`, data);
  },

  /** 删除刷新点 */
  deleteSpawnPoint(id: string): Promise<void> {
    return request.delete(`/boss/spawn-points/${id}`);
  },

  /** 切换刷新点启用状态 */
  toggleSpawnPoint(id: string): Promise<BossSpawnPoint> {
    return request.patch(`/boss/spawn-points/${id}/toggle`);
  },

  /** 批量切换刷新点启用状态 */
  batchToggleSpawnPoints(ids: string[], enabled: boolean): Promise<{ count: number }> {
    return request.patch('/boss/spawn-points/batch-toggle', { ids, enabled });
  },

  // ==================== 统计 ====================

  /** 获取统计数据 */
  getStats(): Promise<BossStats> {
    return request.get('/boss/stats');
  },

  // ==================== 击杀历史 ====================

  /** 获取击杀历史 */
  getHistory(params?: {
    spawnPointId?: string;
    killerUuid?: string;
    startTime?: number;
    endTime?: number;
    limit?: number;
    offset?: number;
  }): Promise<{ records: BossKillHistory[]; total: number }> {
    return request.get('/boss/history', { params });
  },

  // ==================== 活跃 Boss ====================

  /** 获取当前活跃 Boss */
  getActiveBosses(): Promise<any[]> {
    return request.get('/boss/active');
  },

  // ==================== MythicMobs ====================

  /** 获取 MythicMobs 状态 */
  getMythicMobsStatus(): Promise<MythicMobsStatus> {
    return request.get('/boss/mythicmobs/status');
  },

  /** 获取所有 MythicMobs 怪物 */
  getMythicMobs(): Promise<MythicMobInfo[]> {
    return request.get('/boss/mythicmobs');
  },

  /** 搜索 MythicMobs 怪物 */
  searchMythicMobs(keyword: string): Promise<MythicMobInfo[]> {
    return request.get('/boss/mythicmobs/search', { params: { keyword } });
  },

  /** 刷新 MythicMobs 缓存 */
  refreshMythicMobsCache(): Promise<{ count: number }> {
    return request.post('/boss/mythicmobs/refresh');
  },

  /** 获取单个 MythicMobs 怪物详情 */
  getMythicMobDetail(id: string): Promise<MythicMobDetailInfo> {
    return request.get(`/boss/mythicmobs/${id}`);
  },

  /** 获取怪物类型统计 */
  getMythicMobTypeStats(): Promise<Record<string, number>> {
    return request.get('/boss/mythicmobs-stats');
  },

  // ==================== 奖励配置 ====================

  /** 获取奖励配置 */
  getRewards(): Promise<any[]> {
    return request.get('/boss/rewards');
  },

  /** 更新奖励配置 */
  updateReward(data: { tier: number; rank: number; multiplier: number; rewardsJson: string }): Promise<any> {
    return request.put('/boss/rewards', data);
  },
};

/**
 * Boss 等级配置
 */
export const BOSS_TIERS = [
  { value: 1, label: '普通', color: '#909399', description: '适合新手玩家' },
  { value: 2, label: '精英', color: '#67c23a', description: '小队挑战(3-5人)' },
  { value: 3, label: '首领', color: '#e6a23c', description: '团队挑战(5-10人)' },
  { value: 4, label: '传说', color: '#f56c6c', description: '多团队挑战(20+人)' },
];

/**
 * 刷新模式配置
 */
export const SPAWN_MODES = [
  { value: 'fixed', label: '固定位置', description: '在指定坐标生成' },
  { value: 'random', label: '随机偏移', description: '在中心点周围随机偏移' },
  { value: 'player-nearby', label: '玩家附近', description: '在附近玩家周围生成' },
  { value: 'region', label: '区域随机', description: '在指定区域内随机生成' },
];

/**
 * 格式化冷却时间
 */
export function formatCooldown(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}秒`;
  }
  if (seconds < 3600) {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return secs > 0 ? `${minutes}分${secs}秒` : `${minutes}分钟`;
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return minutes > 0 ? `${hours}小时${minutes}分` : `${hours}小时`;
}

/**
 * 获取等级颜色
 */
export function getTierColor(tier: number): string {
  const tierConfig = BOSS_TIERS.find(t => t.value === tier);
  return tierConfig?.color || '#909399';
}

/**
 * 获取等级标签
 */
export function getTierLabel(tier: number): string {
  const tierConfig = BOSS_TIERS.find(t => t.value === tier);
  return tierConfig?.label || '未知';
}
