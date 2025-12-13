import { Router, Request, Response } from 'express';
import sectService from '@/services/sect.service';
import { success, error } from '@/lib/response';

const router = Router();

/**
 * GET /api/sects
 * 获取所有宗门
 */
router.get('/', async (req: Request, res: Response) => {
  try {
    const sects = await sectService.getAllSects();
    return success(res, sects);
  } catch (err: any) {
    return error(res, err.message);
  }
});

/**
 * GET /api/sects/ranking
 * 获取宗门排行榜
 */
router.get('/ranking', async (req: Request, res: Response) => {
  try {
    const limit = Number(req.query.limit) || 10;
    const ranking = await sectService.getSectRanking(limit);
    return success(res, ranking);
  } catch (err: any) {
    return error(res, err.message);
  }
});

/**
 * GET /api/sects/:id
 * 获取宗门详情
 */
router.get('/:id', async (req: Request, res: Response) => {
  try {
    const id = Number(req.params.id);
    const sect = await sectService.getSectById(id);

    if (!sect) {
      return error(res, '宗门不存在', 404);
    }

    return success(res, sect);
  } catch (err: any) {
    return error(res, err.message);
  }
});

export default router;
