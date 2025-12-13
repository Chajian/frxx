import prisma from '@/lib/prisma';

/**
 * 宗门服务
 */
export class SectService {
  /**
   * 获取所有宗门
   */
  async getAllSects() {
    return await prisma.xianSect.findMany({
      include: {
        _count: {
          select: { members: true },
        },
      },
      orderBy: {
        level: 'desc',
      },
    });
  }

  /**
   * 根据 ID 获取宗门详情
   */
  async getSectById(id: number) {
    return await prisma.xianSect.findUnique({
      where: { id },
      include: {
        members: {
          orderBy: {
            contribution: 'desc',
          },
        },
        facilities: true,
        warehouse: true,
      },
    });
  }

  /**
   * 获取宗门排行榜
   */
  async getSectRanking(limit = 10) {
    return await prisma.xianSect.findMany({
      take: limit,
      orderBy: {
        level: 'desc',
      },
      select: {
        id: true,
        name: true,
        level: true,
        experience: true,
        ownerName: true,
        _count: {
          select: { members: true },
        },
      },
    });
  }
}

export default new SectService();
