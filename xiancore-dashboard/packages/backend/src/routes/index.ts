import { Router } from 'express';
import playerRoutes from './player.routes';
import sectRoutes from './sect.routes';
import bossRoutes from './boss.routes';

const router = Router();

// 注册路由
router.use('/players', playerRoutes);
router.use('/sects', sectRoutes);
router.use('/boss', bossRoutes);

export default router;
