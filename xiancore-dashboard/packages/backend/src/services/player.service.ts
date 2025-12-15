import prisma from '@/lib/prisma.js';

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

  /**
   * 更新玩家信息
   */
  async updatePlayer(uuid: string, data: Record<string, any>) {
    return await prisma.xianPlayer.update({
      where: { uuid },
      data: {
        ...(data.realm && { realm: data.realm }),
        ...(data.qi !== undefined && { qi: BigInt(data.qi) }),
        ...(data.spiritualRoot !== undefined && { spiritualRoot: data.spiritualRoot }),
        ...(data.spiritualRootType && { spiritualRootType: data.spiritualRootType }),
        ...(data.spiritStones !== undefined && { spiritStones: BigInt(data.spiritStones) }),
        ...(data.contributionPoints !== undefined && { contributionPoints: data.contributionPoints }),
        ...(data.playerLevel !== undefined && { playerLevel: data.playerLevel }),
        ...(data.activeQi !== undefined && { activeQi: BigInt(data.activeQi) }),
      },
      include: {
        skills: true,
        equipment: true,
        skillBinds: true,
      },
    });
  }
}

export default new PlayerService();
