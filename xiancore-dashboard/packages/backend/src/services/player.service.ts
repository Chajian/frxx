import prisma from '@/lib/prisma';

/**
 * 玩家服务
 */
export class PlayerService {
  /**
   * 获取所有玩家
   */
  async getAllPlayers() {
    return await prisma.xianPlayer.findMany({
      orderBy: {
        playerLevel: 'desc',
      },
    });
  }

  /**
   * 根据 UUID 获取玩家
   */
  async getPlayerByUuid(uuid: string) {
    return await prisma.xianPlayer.findUnique({
      where: { uuid },
      include: {
        skills: true,
        equipment: true,
        skillBinds: true,
      },
    });
  }

  /**
   * 获取玩家排行榜
   */
  async getPlayerRanking(limit = 10) {
    return await prisma.xianPlayer.findMany({
      take: limit,
      orderBy: {
        playerLevel: 'desc',
      },
      select: {
        uuid: true,
        name: true,
        realm: true,
        realmStage: true,
        playerLevel: true,
        qi: true,
      },
    });
  }
}

export default new PlayerService();
