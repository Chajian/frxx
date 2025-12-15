import prisma from '@/lib/prisma.js';
import mythicMobsService, { type MythicMobInfo, type MythicMobDetailInfo } from './mythicmobs.service.js';

/**
 * Boss 刷新点数据（带 MythicMob 信息）
 */
export interface SpawnPointWithMob {
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
  cooldownSeconds: bigint;
  maxCount: number;
  randomLocation: boolean;
  spawnRadius: number;
  randomRadius: number;
  spawnMode: string;
  enabled: boolean;
  preSpawnWarning: number;
  spawnMessage: string | null;
  killMessage: string | null;
  lastSpawnTime: bigint;
  currentCount: number;
  totalSpawns: number;
  createdAt: bigint | null;
  updatedAt: bigint | null;
  // 计算字段
  remainingCooldown: number;
  isReadyToSpawn: boolean;
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
 * Boss 服务
 * 管理 Boss 刷新点、击杀历史、奖励配置等
 */
export class BossService {
  /**
   * 获取所有刷新点
   */
  async getAllSpawnPoints(): Promise<SpawnPointWithMob[]> {
    const spawnPoints = await prisma.bossSpawnPoint.findMany({
      orderBy: [{ tier: 'desc' }, { name: 'asc' }],
    });

    // 获取所有 MythicMob 信息
    const mobs = await mythicMobsService.getAllMobs();
    const mobMap = new Map(mobs.map(m => [m.id, m]));

    return spawnPoints.map(sp => this.enrichSpawnPoint(sp, mobMap));
  }

  /**
   * 根据 ID 获取刷新点
   */
  async getSpawnPointById(id: string): Promise<SpawnPointWithMob | null> {
    const spawnPoint = await prisma.bossSpawnPoint.findUnique({
      where: { id },
    });

    if (!spawnPoint) {
      return null;
    }

    const mobInfo = await mythicMobsService.getMobById(spawnPoint.mythicMobId);
    return this.enrichSpawnPoint(spawnPoint, new Map(mobInfo ? [[mobInfo.id, mobInfo]] : []));
  }

  /**
   * 创建刷新点
   */
  async createSpawnPoint(data: CreateSpawnPointInput) {
    const now = BigInt(Date.now());

    return await prisma.bossSpawnPoint.create({
      data: {
        id: data.id,
        name: data.name,
        description: data.description || null,
        world: data.world,
        x: data.x,
        y: data.y,
        z: data.z,
        mythicMobId: data.mythicMobId,
        tier: data.tier ?? 1,
        cooldownSeconds: BigInt(data.cooldownSeconds ?? 7200),
        maxCount: data.maxCount ?? 1,
        randomLocation: data.randomLocation ?? false,
        spawnRadius: data.spawnRadius ?? 100,
        randomRadius: data.randomRadius ?? 0,
        spawnMode: data.spawnMode ?? 'fixed',
        enabled: data.enabled ?? true,
        preSpawnWarning: data.preSpawnWarning ?? 30,
        spawnMessage: data.spawnMessage || null,
        killMessage: data.killMessage || null,
        createdAt: now,
        updatedAt: now,
      },
    });
  }

  /**
   * 更新刷新点
   */
  async updateSpawnPoint(id: string, data: UpdateSpawnPointInput) {
    const updateData: any = {
      updatedAt: BigInt(Date.now()),
    };

    if (data.name !== undefined) updateData.name = data.name;
    if (data.description !== undefined) updateData.description = data.description;
    if (data.world !== undefined) updateData.world = data.world;
    if (data.x !== undefined) updateData.x = data.x;
    if (data.y !== undefined) updateData.y = data.y;
    if (data.z !== undefined) updateData.z = data.z;
    if (data.mythicMobId !== undefined) updateData.mythicMobId = data.mythicMobId;
    if (data.tier !== undefined) updateData.tier = data.tier;
    if (data.cooldownSeconds !== undefined) updateData.cooldownSeconds = BigInt(data.cooldownSeconds);
    if (data.maxCount !== undefined) updateData.maxCount = data.maxCount;
    if (data.randomLocation !== undefined) updateData.randomLocation = data.randomLocation;
    if (data.spawnRadius !== undefined) updateData.spawnRadius = data.spawnRadius;
    if (data.randomRadius !== undefined) updateData.randomRadius = data.randomRadius;
    if (data.spawnMode !== undefined) updateData.spawnMode = data.spawnMode;
    if (data.enabled !== undefined) updateData.enabled = data.enabled;
    if (data.preSpawnWarning !== undefined) updateData.preSpawnWarning = data.preSpawnWarning;
    if (data.spawnMessage !== undefined) updateData.spawnMessage = data.spawnMessage;
    if (data.killMessage !== undefined) updateData.killMessage = data.killMessage;

    return await prisma.bossSpawnPoint.update({
      where: { id },
      data: updateData,
    });
  }

  /**
   * 删除刷新点
   */
  async deleteSpawnPoint(id: string) {
    return await prisma.bossSpawnPoint.delete({
      where: { id },
    });
  }

  /**
   * 切换刷新点启用状态
   */
  async toggleSpawnPoint(id: string) {
    const current = await prisma.bossSpawnPoint.findUnique({
      where: { id },
      select: { enabled: true },
    });

    if (!current) {
      throw new Error('刷新点不存在');
    }

    return await prisma.bossSpawnPoint.update({
      where: { id },
      data: {
        enabled: !current.enabled,
        updatedAt: BigInt(Date.now()),
      },
    });
  }

  /**
   * 批量切换刷新点启用状态
   */
  async batchToggleSpawnPoints(ids: string[], enabled: boolean) {
    return await prisma.bossSpawnPoint.updateMany({
      where: { id: { in: ids } },
      data: {
        enabled,
        updatedAt: BigInt(Date.now()),
      },
    });
  }

  /**
   * 获取击杀历史
   */
  async getKillHistory(options?: {
    spawnPointId?: string;
    killerUuid?: string;
    startTime?: number;
    endTime?: number;
    limit?: number;
    offset?: number;
  }) {
    const where: any = {};

    if (options?.spawnPointId) {
      where.spawnPointId = options.spawnPointId;
    }
    if (options?.killerUuid) {
      where.killerUuid = options.killerUuid;
    }
    if (options?.startTime || options?.endTime) {
      where.killTime = {};
      if (options.startTime) {
        where.killTime.gte = BigInt(options.startTime);
      }
      if (options.endTime) {
        where.killTime.lte = BigInt(options.endTime);
      }
    }

    const [records, total] = await Promise.all([
      prisma.bossKillHistory.findMany({
        where,
        orderBy: { killTime: 'desc' },
        take: options?.limit ?? 50,
        skip: options?.offset ?? 0,
        include: {
          spawnPoint: {
            select: { name: true },
          },
        },
      }),
      prisma.bossKillHistory.count({ where }),
    ]);

    return { records, total };
  }

  /**
   * 获取当前活跃 Boss
   */
  async getActiveBosses() {
    return await prisma.bossActive.findMany({
      orderBy: { spawnTime: 'desc' },
    });
  }

  /**
   * 获取统计数据
   */
  async getStats(): Promise<BossStats> {
    const now = Date.now();
    const todayStart = new Date();
    todayStart.setHours(0, 0, 0, 0);

    const [
      totalSpawnPoints,
      enabledSpawnPoints,
      activeBosses,
      totalKills,
      todayKills,
      tierCounts,
    ] = await Promise.all([
      prisma.bossSpawnPoint.count(),
      prisma.bossSpawnPoint.count({ where: { enabled: true } }),
      prisma.bossActive.count(),
      prisma.bossKillHistory.count(),
      prisma.bossKillHistory.count({
        where: { killTime: { gte: BigInt(todayStart.getTime()) } },
      }),
      prisma.bossSpawnPoint.groupBy({
        by: ['tier'],
        _count: { tier: true },
      }),
    ]);

    const tierDistribution: Record<number, number> = {};
    for (const tc of tierCounts) {
      tierDistribution[tc.tier] = tc._count.tier;
    }

    return {
      totalSpawnPoints,
      enabledSpawnPoints,
      disabledSpawnPoints: totalSpawnPoints - enabledSpawnPoints,
      activeBosses,
      totalKills,
      todayKills,
      tierDistribution,
    };
  }

  /**
   * 获取奖励配置
   */
  async getRewardConfigs() {
    return await prisma.bossRewardConfig.findMany({
      orderBy: [{ tier: 'asc' }, { rank: 'asc' }],
    });
  }

  /**
   * 更新奖励配置
   */
  async upsertRewardConfig(tier: number, rank: number, multiplier: number, rewardsJson: string) {
    return await prisma.bossRewardConfig.upsert({
      where: {
        tier_rank: { tier, rank },
      },
      update: {
        multiplier,
        rewardsJson,
      },
      create: {
        tier,
        rank,
        multiplier,
        rewardsJson,
      },
    });
  }

  /**
   * 获取 MythicMobs 服务状态
   */
  getMythicMobsStatus() {
    return mythicMobsService.getStatus();
  }

  /**
   * 获取所有 MythicMobs 怪物
   */
  async getAllMythicMobs() {
    return await mythicMobsService.getAllMobs();
  }

  /**
   * 搜索 MythicMobs 怪物
   */
  async searchMythicMobs(keyword: string) {
    return await mythicMobsService.searchMobs(keyword);
  }

  /**
   * 刷新 MythicMobs 缓存
   */
  refreshMythicMobsCache() {
    mythicMobsService.clearCache();
  }

  /**
   * 获取单个 MythicMobs 怪物详情
   */
  async getMythicMobDetail(id: string): Promise<MythicMobDetailInfo | null> {
    return await mythicMobsService.getMobDetailById(id);
  }

  /**
   * 获取单个 MythicMobs 怪物详情（增强版）
   */
  async getMythicMobDetailEnhanced(id: string): Promise<MythicMobDetailInfo | null> {
    return await mythicMobsService.getMobDetailByIdEnhanced(id);
  }

  /**
   * 获取怪物类型统计
   */
  async getMythicMobTypeStats(): Promise<Record<string, number>> {
    return await mythicMobsService.getMobTypeStats();
  }

  /**
   * 获取所有掉落表
   */
  async getAllDropTables() {
    return await mythicMobsService.getAllDropTables();
  }

  /**
   * 获取指定掉落表
   */
  async getDropTable(id: string) {
    return await mythicMobsService.getDropTable(id);
  }

  /**
   * 获取所有技能组
   */
  async getAllSkillGroups() {
    return await mythicMobsService.getAllSkillGroups();
  }

  /**
   * 获取指定技能组
   */
  async getSkillGroup(id: string) {
    return await mythicMobsService.getSkillGroup(id);
  }

  /**
   * 获取怪物模板继承信息
   */
  async getTemplateInfo(mobId: string) {
    return await mythicMobsService.getTemplateInfo(mobId);
  }

  /**
   * 获取怪物继承链
   */
  async getInheritanceChain(mobId: string) {
    return await mythicMobsService.getInheritanceChain(mobId);
  }

  /**
   * 获取怪物原始 YAML 配置
   */
  async getMobRawYaml(mobId: string) {
    return await mythicMobsService.getMobRawYaml(mobId);
  }

  /**
   * 保存怪物配置
   */
  async saveMobConfig(mobId: string, config: Record<string, any>) {
    return await mythicMobsService.saveMobConfig(mobId, config);
  }

  /**
   * 验证 YAML 配置
   */
  validateYamlConfig(yamlContent: string) {
    return mythicMobsService.validateYamlConfig(yamlContent);
  }

  // ==================== MythicMobs Items ====================

  /**
   * 获取所有物品
   */
  async getAllItems() {
    return await mythicMobsService.getAllItems();
  }

  /**
   * 获取指定物品
   */
  async getItem(id: string) {
    return await mythicMobsService.getItem(id);
  }

  /**
   * 获取物品详情
   */
  async getItemDetail(id: string) {
    return await mythicMobsService.getItemDetail(id);
  }

  /**
   * 搜索物品
   */
  async searchItems(keyword: string) {
    return await mythicMobsService.searchItems(keyword);
  }

  /**
   * 获取物品材质统计
   */
  async getItemMaterialStats() {
    return await mythicMobsService.getItemMaterialStats();
  }

  /**
   * 获取物品原始 YAML 配置
   */
  async getItemRawYaml(itemId: string) {
    return await mythicMobsService.getItemRawYaml(itemId);
  }

  /**
   * 保存物品配置
   */
  async saveItemConfig(itemId: string, config: Record<string, any>) {
    return await mythicMobsService.saveItemConfig(itemId, config);
  }

  /**
   * 丰富刷新点数据（添加 MythicMob 信息和计算字段）
   */
  private enrichSpawnPoint(
    sp: any,
    mobMap: Map<string, MythicMobInfo>
  ): SpawnPointWithMob {
    const now = Date.now();
    const lastSpawnTime = Number(sp.lastSpawnTime);
    const cooldownMs = Number(sp.cooldownSeconds) * 1000;
    const timeSinceLastSpawn = now - lastSpawnTime;
    const remainingCooldown = Math.max(0, Math.ceil((cooldownMs - timeSinceLastSpawn) / 1000));
    const isReadyToSpawn = sp.enabled && remainingCooldown === 0 && sp.currentCount < sp.maxCount;

    return {
      ...sp,
      mythicMobInfo: mobMap.get(sp.mythicMobId) || null,
      remainingCooldown,
      isReadyToSpawn,
    };
  }
}

export default new BossService();
