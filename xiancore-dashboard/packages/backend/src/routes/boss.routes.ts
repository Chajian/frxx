import { Router, Request, Response } from 'express';
import bossService from '@/services/boss.service';
import { success, error } from '@/lib/response';

const router = Router();

// ==================== 刷新点管理 ====================

/**
 * 获取所有刷新点
 * GET /api/boss/spawn-points
 */
router.get('/spawn-points', async (req: Request, res: Response) => {
  try {
    const spawnPoints = await bossService.getAllSpawnPoints();
    return success(res, spawnPoints);
  } catch (err: any) {
    console.error('获取刷新点列表失败:', err);
    return error(res, err.message || '获取刷新点列表失败');
  }
});

/**
 * 获取单个刷新点
 * GET /api/boss/spawn-points/:id
 */
router.get('/spawn-points/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const spawnPoint = await bossService.getSpawnPointById(id);

    if (!spawnPoint) {
      return error(res, '刷新点不存在', 404);
    }

    return success(res, spawnPoint);
  } catch (err: any) {
    console.error('获取刷新点失败:', err);
    return error(res, err.message || '获取刷新点失败');
  }
});

/**
 * 创建刷新点
 * POST /api/boss/spawn-points
 */
router.post('/spawn-points', async (req: Request, res: Response) => {
  try {
    const data = req.body;

    // 基本验证
    if (!data.id || !data.name || !data.world || !data.mythicMobId) {
      return error(res, '缺少必填字段: id, name, world, mythicMobId', 400);
    }

    if (data.x === undefined || data.y === undefined || data.z === undefined) {
      return error(res, '缺少坐标: x, y, z', 400);
    }

    // ID 格式验证
    if (!/^[a-zA-Z0-9_-]+$/.test(data.id)) {
      return error(res, 'ID 只能包含字母、数字、下划线和连字符', 400);
    }

    // 检查 ID 是否已存在
    const existing = await bossService.getSpawnPointById(data.id);
    if (existing) {
      return error(res, '刷新点 ID 已存在', 400);
    }

    const spawnPoint = await bossService.createSpawnPoint(data);
    return success(res, spawnPoint, '创建成功');
  } catch (err: any) {
    console.error('创建刷新点失败:', err);
    return error(res, err.message || '创建刷新点失败');
  }
});

/**
 * 更新刷新点
 * PUT /api/boss/spawn-points/:id
 */
router.put('/spawn-points/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const data = req.body;

    // 检查是否存在
    const existing = await bossService.getSpawnPointById(id);
    if (!existing) {
      return error(res, '刷新点不存在', 404);
    }

    const spawnPoint = await bossService.updateSpawnPoint(id, data);
    return success(res, spawnPoint, '更新成功');
  } catch (err: any) {
    console.error('更新刷新点失败:', err);
    return error(res, err.message || '更新刷新点失败');
  }
});

/**
 * 删除刷新点
 * DELETE /api/boss/spawn-points/:id
 */
router.delete('/spawn-points/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    // 检查是否存在
    const existing = await bossService.getSpawnPointById(id);
    if (!existing) {
      return error(res, '刷新点不存在', 404);
    }

    await bossService.deleteSpawnPoint(id);
    return success(res, null, '删除成功');
  } catch (err: any) {
    console.error('删除刷新点失败:', err);
    return error(res, err.message || '删除刷新点失败');
  }
});

/**
 * 切换刷新点启用状态
 * PATCH /api/boss/spawn-points/:id/toggle
 */
router.patch('/spawn-points/:id/toggle', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const spawnPoint = await bossService.toggleSpawnPoint(id);
    return success(res, spawnPoint, spawnPoint.enabled ? '已启用' : '已禁用');
  } catch (err: any) {
    console.error('切换刷新点状态失败:', err);
    return error(res, err.message || '切换状态失败');
  }
});

/**
 * 批量切换刷新点启用状态
 * PATCH /api/boss/spawn-points/batch-toggle
 */
router.patch('/spawn-points/batch-toggle', async (req: Request, res: Response) => {
  try {
    const { ids, enabled } = req.body;

    if (!Array.isArray(ids) || ids.length === 0) {
      return error(res, '请提供刷新点 ID 列表', 400);
    }

    if (typeof enabled !== 'boolean') {
      return error(res, '请提供 enabled 状态', 400);
    }

    const result = await bossService.batchToggleSpawnPoints(ids, enabled);
    return success(res, result, `已${enabled ? '启用' : '禁用'} ${result.count} 个刷新点`);
  } catch (err: any) {
    console.error('批量切换状态失败:', err);
    return error(res, err.message || '批量切换状态失败');
  }
});

// ==================== 击杀历史 ====================

/**
 * 获取击杀历史
 * GET /api/boss/history
 */
router.get('/history', async (req: Request, res: Response) => {
  try {
    const {
      spawnPointId,
      killerUuid,
      startTime,
      endTime,
      limit,
      offset,
    } = req.query;

    const result = await bossService.getKillHistory({
      spawnPointId: spawnPointId as string,
      killerUuid: killerUuid as string,
      startTime: startTime ? Number(startTime) : undefined,
      endTime: endTime ? Number(endTime) : undefined,
      limit: limit ? Number(limit) : undefined,
      offset: offset ? Number(offset) : undefined,
    });

    return success(res, result);
  } catch (err: any) {
    console.error('获取击杀历史失败:', err);
    return error(res, err.message || '获取击杀历史失败');
  }
});

// ==================== 活跃 Boss ====================

/**
 * 获取当前活跃 Boss
 * GET /api/boss/active
 */
router.get('/active', async (req: Request, res: Response) => {
  try {
    const activeBosses = await bossService.getActiveBosses();
    return success(res, activeBosses);
  } catch (err: any) {
    console.error('获取活跃 Boss 失败:', err);
    return error(res, err.message || '获取活跃 Boss 失败');
  }
});

// ==================== 统计数据 ====================

/**
 * 获取统计数据
 * GET /api/boss/stats
 */
router.get('/stats', async (req: Request, res: Response) => {
  try {
    const stats = await bossService.getStats();
    return success(res, stats);
  } catch (err: any) {
    console.error('获取统计数据失败:', err);
    return error(res, err.message || '获取统计数据失败');
  }
});

// ==================== 奖励配置 ====================

/**
 * 获取奖励配置
 * GET /api/boss/rewards
 */
router.get('/rewards', async (req: Request, res: Response) => {
  try {
    const rewards = await bossService.getRewardConfigs();
    return success(res, rewards);
  } catch (err: any) {
    console.error('获取奖励配置失败:', err);
    return error(res, err.message || '获取奖励配置失败');
  }
});

/**
 * 更新奖励配置
 * PUT /api/boss/rewards
 */
router.put('/rewards', async (req: Request, res: Response) => {
  try {
    const { tier, rank, multiplier, rewardsJson } = req.body;

    if (tier === undefined || rank === undefined) {
      return error(res, '缺少 tier 或 rank', 400);
    }

    const reward = await bossService.upsertRewardConfig(
      tier,
      rank,
      multiplier ?? 1.0,
      rewardsJson ?? '{}'
    );
    return success(res, reward, '更新成功');
  } catch (err: any) {
    console.error('更新奖励配置失败:', err);
    return error(res, err.message || '更新奖励配置失败');
  }
});

// ==================== MythicMobs ====================

/**
 * 获取 MythicMobs 服务状态
 * GET /api/boss/mythicmobs/status
 */
router.get('/mythicmobs/status', async (req: Request, res: Response) => {
  try {
    const status = bossService.getMythicMobsStatus();
    return success(res, status);
  } catch (err: any) {
    console.error('获取 MythicMobs 状态失败:', err);
    return error(res, err.message || '获取状态失败');
  }
});

/**
 * 获取所有 MythicMobs 怪物
 * GET /api/boss/mythicmobs
 */
router.get('/mythicmobs', async (req: Request, res: Response) => {
  try {
    const mobs = await bossService.getAllMythicMobs();
    return success(res, mobs);
  } catch (err: any) {
    console.error('获取 MythicMobs 列表失败:', err);
    return error(res, err.message || '获取列表失败');
  }
});

/**
 * 搜索 MythicMobs 怪物
 * GET /api/boss/mythicmobs/search?keyword=xxx
 */
router.get('/mythicmobs/search', async (req: Request, res: Response) => {
  try {
    const { keyword } = req.query;

    if (!keyword || typeof keyword !== 'string') {
      return error(res, '请提供搜索关键词', 400);
    }

    const mobs = await bossService.searchMythicMobs(keyword);
    return success(res, mobs);
  } catch (err: any) {
    console.error('搜索 MythicMobs 失败:', err);
    return error(res, err.message || '搜索失败');
  }
});

/**
 * 刷新 MythicMobs 缓存
 * POST /api/boss/mythicmobs/refresh
 */
router.post('/mythicmobs/refresh', async (req: Request, res: Response) => {
  try {
    bossService.refreshMythicMobsCache();
    const mobs = await bossService.getAllMythicMobs();
    return success(res, { count: mobs.length }, '缓存已刷新');
  } catch (err: any) {
    console.error('刷新 MythicMobs 缓存失败:', err);
    return error(res, err.message || '刷新失败');
  }
});

/**
 * 获取单个 MythicMobs 怪物详情
 * GET /api/boss/mythicmobs/:id
 */
router.get('/mythicmobs/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { enhanced } = req.query;

    const mobDetail = enhanced === 'true'
      ? await bossService.getMythicMobDetailEnhanced(id)
      : await bossService.getMythicMobDetail(id);

    if (!mobDetail) {
      return error(res, '怪物不存在', 404);
    }

    return success(res, mobDetail);
  } catch (err: any) {
    console.error('获取怪物详情失败:', err);
    return error(res, err.message || '获取详情失败');
  }
});

/**
 * 获取怪物类型统计
 * GET /api/boss/mythicmobs-stats
 */
router.get('/mythicmobs-stats', async (req: Request, res: Response) => {
  try {
    const stats = await bossService.getMythicMobTypeStats();
    return success(res, stats);
  } catch (err: any) {
    console.error('获取怪物类型统计失败:', err);
    return error(res, err.message || '获取统计失败');
  }
});

// ==================== 掉落表 ====================

/**
 * 获取所有掉落表
 * GET /api/boss/droptables
 */
router.get('/droptables', async (req: Request, res: Response) => {
  try {
    const dropTables = await bossService.getAllDropTables();
    return success(res, dropTables);
  } catch (err: any) {
    console.error('获取掉落表列表失败:', err);
    return error(res, err.message || '获取失败');
  }
});

/**
 * 获取指定掉落表
 * GET /api/boss/droptables/:id
 */
router.get('/droptables/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const dropTable = await bossService.getDropTable(id);

    if (!dropTable) {
      return error(res, '掉落表不存在', 404);
    }

    return success(res, dropTable);
  } catch (err: any) {
    console.error('获取掉落表失败:', err);
    return error(res, err.message || '获取失败');
  }
});

// ==================== 技能组 ====================

/**
 * 获取所有技能组
 * GET /api/boss/skillgroups
 */
router.get('/skillgroups', async (req: Request, res: Response) => {
  try {
    const skillGroups = await bossService.getAllSkillGroups();
    return success(res, skillGroups);
  } catch (err: any) {
    console.error('获取技能组列表失败:', err);
    return error(res, err.message || '获取失败');
  }
});

/**
 * 获取指定技能组
 * GET /api/boss/skillgroups/:id
 */
router.get('/skillgroups/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const skillGroup = await bossService.getSkillGroup(id);

    if (!skillGroup) {
      return error(res, '技能组不存在', 404);
    }

    return success(res, skillGroup);
  } catch (err: any) {
    console.error('获取技能组失败:', err);
    return error(res, err.message || '获取失败');
  }
});

// ==================== 模板继承 ====================

/**
 * 获取怪物模板继承信息
 * GET /api/boss/mythicmobs/:id/template
 */
router.get('/mythicmobs/:id/template', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const templateInfo = await bossService.getTemplateInfo(id);

    if (!templateInfo) {
      return error(res, '模板信息不存在', 404);
    }

    return success(res, templateInfo);
  } catch (err: any) {
    console.error('获取模板信息失败:', err);
    return error(res, err.message || '获取失败');
  }
});

/**
 * 获取怪物继承链
 * GET /api/boss/mythicmobs/:id/inheritance
 */
router.get('/mythicmobs/:id/inheritance', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const chain = await bossService.getInheritanceChain(id);
    return success(res, chain);
  } catch (err: any) {
    console.error('获取继承链失败:', err);
    return error(res, err.message || '获取失败');
  }
});

// ==================== 配置编辑 ====================

/**
 * 获取怪物原始 YAML 配置
 * GET /api/boss/mythicmobs/:id/yaml
 */
router.get('/mythicmobs/:id/yaml', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const yamlContent = await bossService.getMobRawYaml(id);

    if (!yamlContent) {
      return error(res, '怪物不存在', 404);
    }

    return success(res, { yaml: yamlContent });
  } catch (err: any) {
    console.error('获取 YAML 配置失败:', err);
    return error(res, err.message || '获取失败');
  }
});

/**
 * 保存怪物配置
 * PUT /api/boss/mythicmobs/:id/config
 */
router.put('/mythicmobs/:id/config', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { config } = req.body;

    if (!config || typeof config !== 'object') {
      return error(res, '请提供有效的配置对象', 400);
    }

    const result = await bossService.saveMobConfig(id, config);

    if (!result) {
      return error(res, '保存配置失败', 500);
    }

    return success(res, null, '配置已保存');
  } catch (err: any) {
    console.error('保存配置失败:', err);
    return error(res, err.message || '保存失败');
  }
});

/**
 * 验证 YAML 配置
 * POST /api/boss/mythicmobs/validate-yaml
 */
router.post('/mythicmobs/validate-yaml', async (req: Request, res: Response) => {
  try {
    const { yaml: yamlContent } = req.body;

    if (!yamlContent || typeof yamlContent !== 'string') {
      return error(res, '请提供 YAML 内容', 400);
    }

    const result = bossService.validateYamlConfig(yamlContent);
    return success(res, result);
  } catch (err: any) {
    console.error('验证 YAML 失败:', err);
    return error(res, err.message || '验证失败');
  }
});

export default router;
