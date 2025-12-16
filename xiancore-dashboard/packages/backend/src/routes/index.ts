import { Router } from 'express';
import playerRoutes from './player.routes.js';
import sectRoutes from './sect.routes.js';
import bossRoutes from './boss.routes.js';
import skillRoutes from './skill.routes.js';

const router = Router();

// 注册路由
router.use('/players', playerRoutes);
router.use('/sects', sectRoutes);
router.use('/boss', bossRoutes);
router.use('/skills', skillRoutes);

export default router;
