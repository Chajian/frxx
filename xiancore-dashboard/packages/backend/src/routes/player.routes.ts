import { Router, Request, Response } from 'express';
import playerService from '@/services/player.service';
import { success, error } from '@/lib/response';

const router = Router();

/**
 * GET /api/players
 * 获取所有玩家
 */
router.get('/', async (req: Request, res: Response) => {
  try {
    const players = await playerService.getAllPlayers();
    return success(res, players);
  } catch (err: any) {
    return error(res, err.message);
  }
});

/**
 * GET /api/players/ranking
 * 获取玩家排行榜
 */
router.get('/ranking', async (req: Request, res: Response) => {
  try {
    const limit = Number(req.query.limit) || 10;
    const ranking = await playerService.getPlayerRanking(limit);
    return success(res, ranking);
  } catch (err: any) {
    return error(res, err.message);
  }
});

/**
 * GET /api/players/:uuid
 * 获取玩家详情
 */
router.get('/:uuid', async (req: Request, res: Response) => {
  try {
    const { uuid } = req.params;
    const player = await playerService.getPlayerByUuid(uuid);

    if (!player) {
      return error(res, '玩家不存在', 404);
    }

    return success(res, player);
  } catch (err: any) {
    return error(res, err.message);
  }
});

/**
 * PUT /api/players/:uuid
 * 更新玩家信息
 */
router.put('/:uuid', async (req: Request, res: Response) => {
  try {
    const { uuid } = req.params;
    const updatedPlayer = await playerService.updatePlayer(uuid, req.body);
    return success(res, updatedPlayer);
  } catch (err: any) {
    return error(res, err.message);
  }
});

export default router;
