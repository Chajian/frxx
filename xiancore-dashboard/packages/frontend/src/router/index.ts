import { createRouter, createWebHistory } from 'vue-router';
import BasicLayout from '@/layouts/BasicLayout.vue';

const routes = [
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: {
          title: '仪表盘',
        },
      },
      {
        path: 'players',
        name: 'Players',
        component: () => import('@/views/Players.vue'),
        meta: {
          title: '玩家管理',
        },
      },
      {
        path: 'sects',
        name: 'Sects',
        component: () => import('@/views/Sects.vue'),
        meta: {
          title: '宗门管理',
        },
      },
      {
        path: 'boss',
        name: 'Boss',
        component: () => import('@/views/Boss.vue'),
        meta: {
          title: 'Boss管理',
        },
      },
      {
        path: 'ranking',
        name: 'Ranking',
        component: () => import('@/views/Dashboard.vue'), // 暂时复用 Dashboard
        meta: {
          title: '排行榜',
        },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
