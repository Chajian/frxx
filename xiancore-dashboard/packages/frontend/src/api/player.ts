import request from '@/utils/request';

// 玩家数据类型
export interface Player {
  uuid: string;
  name: string;
  // 修炼数据
  realm: string;
  realmStage: number;
  qi: number;
  maxQi?: number;
  spiritualRoot: number;
  spiritualRootType?: string;
  comprehension?: number;
  techniqueAdaptation?: number;
  physique?: number;
  perception?: number;
  // 资源数据
  spiritStones?: number;
  contributionPoints?: number;
  skillPoints?: number;
  playerLevel: number;
  // 宗门数据
  sectId?: number;
  sectRank?: string;
  // 统计数据
  lastLogin?: number;
  createdAt?: number;
  updatedAt?: number;
  breakthroughAttempts?: number;
  successfulBreakthroughs?: number;
  // 天劫数据
  tribulationCount?: number;
  successfulTribulations?: number;
  // 奇遇数据
  activeQi?: number;
  lastFateTime?: number;
  fateCount?: number;
  // 修炼状态
  cultivating?: boolean;
  // 关联数据
  skills?: PlayerSkill[];
  equipment?: PlayerEquipment[];
  skillBinds?: PlayerSkillBind[];
}

export interface PlayerSkill {
  id: number;
  playerUuid: string;
  skillId: string;
  level: number;
  proficiency?: number;
}

export interface PlayerEquipment {
  id: number;
  playerUuid: string;
  slot: string;
  itemId: string;
  itemData?: string;
}

export interface PlayerSkillBind {
  id: number;
  playerUuid: string;
  slot: number;
  skillId: string;
}

// 玩家 API
export const playerApi = {
  // 获取所有玩家
  getAll() {
    return request.get<Player[]>('/players');
  },

  // 获取玩家详情
  getByUuid(uuid: string) {
    return request.get<Player>(`/players/${uuid}`);
  },

  // 更新玩家信息
  update(uuid: string, data: Partial<Player>) {
    return request.put<Player>(`/players/${uuid}`, data);
  },

  // 获取玩家排行榜
  getRanking(limit = 10) {
    return request.get<Player[]>('/players/ranking', { params: { limit } });
  },
};
