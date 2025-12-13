import request from '@/utils/request';

// 宗门数据类型
export interface Sect {
  id: number;
  name: string;
  level: number;
  experience: bigint;
  ownerName: string;
  _count?: {
    members: number;
  };
}

// 宗门 API
export const sectApi = {
  // 获取所有宗门
  getAll() {
    return request.get<Sect[]>('/sects');
  },

  // 获取宗门详情
  getById(id: number) {
    return request.get<Sect>(`/sects/${id}`);
  },

  // 获取宗门排行榜
  getRanking(limit = 10) {
    return request.get<Sect[]>('/sects/ranking', { params: { limit } });
  },
};
