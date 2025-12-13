import { Router } from 'express';
import playerRoutes from './player.routes';
import sectRoutes from './sect.routes';

const router = Router();

// 注册路由
router.use('/players', playerRoutes);
router.use('/sects', sectRoutes);

export default router;
