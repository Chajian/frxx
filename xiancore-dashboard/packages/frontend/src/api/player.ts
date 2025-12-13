import request from '@/utils/request';

// 玩家数据类型
export interface Player {
  uuid: string;
  name: string;
  realm: string;
  realmStage: number;
  playerLevel: number;
  qi: bigint;
  spiritualRoot: number;
  spiritualRootType?: string;
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

  // 获取玩家排行榜
  getRanking(limit = 10) {
    return request.get<Player[]>('/players/ranking', { params: { limit } });
  },
};
