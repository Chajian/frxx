# XianCore Dashboard

<div align="center">

**XianCore 修仙插件管理后台**

基于 Vue 3 + Element Plus + Express + Prisma 构建的全栈 TypeScript 项目

[快速开始](#快速开始) • [功能特性](#功能特性) • [开发指南](#开发指南) • [API 文档](#api-文档)

</div>

---

## 📋 目录

- [项目简介](#项目简介)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [前置要求](#前置要求)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [可用命令](#可用命令)
- [API 文档](#api-文档)
- [开发指南](#开发指南)
- [常见问题](#常见问题)
- [故障排除](#故障排除)

---

## 🎯 项目简介

XianCore Dashboard 是为 Minecraft 修仙插件 XianCore 开发的 Web 管理后台，提供：

- 📊 **实时数据展示** - 玩家数据、宗门信息、排行榜
- 👥 **玩家管理** - 查看玩家详情、等级、境界、灵根等信息
- 🏛️ **宗门管理** - 管理宗门成员、设施、仓库
- 🏆 **排行榜系统** - 玩家等级榜、宗门等级榜
- 💾 **数据库直连** - 无需 Minecraft 服务器在线，直接读取 MySQL 数据库

## ✨ 功能特性

- ✅ **全栈 TypeScript** - 类型安全，开发体验好
- ✅ **Monorepo 架构** - 前后端代码统一管理
- ✅ **现代化 UI** - Element Plus 组件库，响应式设计
- ✅ **ORM 支持** - Prisma ORM，类型安全的数据库访问
- ✅ **热重载** - 开发时自动刷新，提升开发效率
- ✅ **API 文档** - RESTful API 设计，清晰的接口定义

## 🛠️ 技术栈

### 前端
- **框架**: Vue 3 (Composition API)
- **UI 库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **构建工具**: Vite 6
- **HTTP 客户端**: Axios
- **语言**: TypeScript

### 后端
- **运行时**: Node.js (>=20.10.0)
- **框架**: Express.js
- **ORM**: Prisma
- **数据库**: MySQL
- **语言**: TypeScript

### 开发工具
- **包管理器**: pnpm (>=9.0.0)
- **代码规范**: TypeScript Strict Mode

## 📋 前置要求

在开始之前，请确保你的开发环境满足以下要求：

### 必需
- **Node.js**: >= 20.10.0 ([下载地址](https://nodejs.org/))
- **pnpm**: >= 9.0.0
- **MySQL**: 运行中的 MySQL 数据库（XianCore 插件使用的数据库）

### 推荐
- **IDE**: VSCode / WebStorm
- **浏览器**: Chrome / Edge（最新版）

### 安装 pnpm

```bash
npm install -g pnpm
```

### 验证安装

```bash
node --version   # 应该显示 >= v20.10.0
pnpm --version   # 应该显示 >= 9.0.0
```

## 🚀 快速开始

### 1️⃣ 克隆或进入项目目录

```bash
cd D:/workspace/java/mc/frxx/xiancore-dashboard
```

### 2️⃣ 安装依赖

```bash
pnpm install
```

这将安装项目根目录以及 `packages/backend` 和 `packages/frontend` 的所有依赖。

### 3️⃣ 配置数据库连接

后端的环境变量文件已经创建在 `packages/backend/.env`：

```env
# 数据库连接字符串
DATABASE_URL="mysql://用户名:密码@主机:端口/数据库名"

# 示例（根据实际情况修改）
DATABASE_URL="mysql://securityuser:security123@localhost:3306/xiancore"

# 服务器端口
PORT=8400

# 运行环境
NODE_ENV=development

# CORS 允许的来源
CORS_ORIGIN=http://localhost:5173
```

**重要**：请根据你的 MySQL 配置修改 `DATABASE_URL`

### 4️⃣ 生成 Prisma Client

```bash
pnpm prisma:generate
```

这将根据 `packages/backend/prisma/schema.prisma` 生成 Prisma 客户端代码。

### 5️⃣ 启动开发服务器

#### 方式一：同时启动前后端（推荐）

```bash
pnpm dev
```

#### 方式二：分别启动

```bash
# 终端 1 - 启动后端
pnpm dev:backend

# 终端 2 - 启动前端
pnpm dev:frontend
```

### 6️⃣ 访问应用

- **前端页面**: http://localhost:5173
- **后端 API**: http://localhost:8400
- **健康检查**: http://localhost:8400/health

## 📁 项目结构

```
xiancore-dashboard/
├── packages/
│   ├── backend/                    # 后端服务
│   │   ├── prisma/
│   │   │   └── schema.prisma       # Prisma 数据模型定义
│   │   ├── src/
│   │   │   ├── lib/
│   │   │   │   ├── prisma.ts       # Prisma Client 实例
│   │   │   │   └── response.ts     # 统一响应格式
│   │   │   ├── services/           # 业务逻辑层
│   │   │   │   ├── player.service.ts
│   │   │   │   └── sect.service.ts
│   │   │   ├── routes/             # 路由层
│   │   │   │   ├── index.ts        # 路由汇总
│   │   │   │   ├── player.routes.ts
│   │   │   │   └── sect.routes.ts
│   │   │   └── index.ts            # 入口文件
│   │   ├── .env                    # 环境变量（已创建）
│   │   ├── package.json
│   │   └── tsconfig.json
│   │
│   └── frontend/                   # 前端应用
│       ├── src/
│       │   ├── api/                # API 接口封装
│       │   │   ├── player.ts
│       │   │   └── sect.ts
│       │   ├── layouts/            # 布局组件
│       │   │   └── BasicLayout.vue
│       │   ├── utils/              # 工具函数
│       │   │   └── request.ts      # Axios 封装
│       │   ├── views/              # 页面组件
│       │   │   ├── Dashboard.vue   # 仪表盘
│       │   │   ├── Players.vue     # 玩家管理
│       │   │   └── Sects.vue       # 宗门管理
│       │   ├── router/
│       │   │   └── index.ts        # 路由配置
│       │   ├── App.vue             # 根组件
│       │   └── main.ts             # 入口文件
│       ├── index.html
│       ├── vite.config.ts          # Vite 配置
│       ├── package.json
│       └── tsconfig.json
│
├── package.json                    # Monorepo 根配置
├── pnpm-workspace.yaml             # pnpm workspace 配置
└── README.md
```

## 📜 可用命令

### 根目录命令

```bash
# 开发
pnpm dev              # 同时启动前后端开发服务器
pnpm dev:backend      # 仅启动后端
pnpm dev:frontend     # 仅启动前端

# 构建
pnpm build            # 构建前后端生产版本
pnpm build:backend    # 仅构建后端
pnpm build:frontend   # 仅构建前端

# 类型检查
pnpm typecheck        # 运行 TypeScript 类型检查

# 清理
pnpm clean            # 清理所有构建产物和 node_modules

# Prisma 相关
pnpm prisma:generate  # 生成 Prisma Client
pnpm prisma:migrate   # 运行数据库迁移
pnpm prisma:studio    # 打开 Prisma Studio（数据库可视化工具）
```

### 后端命令

```bash
cd packages/backend

pnpm dev              # 启动开发服务器（热重载）
pnpm build            # 构建生产版本
pnpm start            # 启动生产服务器
pnpm typecheck        # 类型检查
```

### 前端命令

```bash
cd packages/frontend

pnpm dev              # 启动开发服务器
pnpm build            # 构建生产版本
pnpm preview          # 预览生产构建
pnpm typecheck        # 类型检查
```

## 🔌 API 文档

### 基础信息

- **Base URL**: `http://localhost:8400/api`
- **响应格式**: JSON
- **字符编码**: UTF-8

### 统一响应格式

```typescript
{
  code: number;      // 0 = 成功，其他 = 错误码
  message: string;   // 响应消息
  data?: any;        // 响应数据（可选）
}
```

### 玩家相关 API

#### 获取所有玩家
```
GET /api/players
```

**响应示例**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "name": "玩家名称",
      "realm": "金丹期",
      "realmStage": 3,
      "playerLevel": 50,
      "qi": 100000,
      "spiritualRoot": 0.85,
      "spiritualRootType": "天灵根"
    }
  ]
}
```

#### 获取玩家排行榜
```
GET /api/players/ranking?limit=10
```

**参数**:
- `limit` (可选): 返回数量，默认 10

#### 获取玩家详情
```
GET /api/players/:uuid
```

**参数**:
- `uuid`: 玩家 UUID

### 宗门相关 API

#### 获取所有宗门
```
GET /api/sects
```

#### 获取宗门排行榜
```
GET /api/sects/ranking?limit=10
```

**参数**:
- `limit` (可选): 返回数量，默认 10

#### 获取宗门详情
```
GET /api/sects/:id
```

**参数**:
- `id`: 宗门 ID

### 系统 API

#### 健康检查
```
GET /health
```

**响应示例**:
```json
{
  "status": "ok",
  "timestamp": 1765606937754
}
```

## 👨‍💻 开发指南

### 代码风格

#### 命名规范
- **文件名**: kebab-case (`player-service.ts`)
- **组件名**: PascalCase (`BasicLayout.vue`)
- **变量/函数**: camelCase (`fetchPlayers`)
- **类型/接口**: PascalCase (`Player`, `ApiResponse`)
- **常量**: UPPER_SNAKE_CASE (`API_BASE_URL`)

#### 组件规范
- 使用 `<script setup lang="ts">` 语法
- 使用 Composition API
- Props 和 Emits 需要定义类型

#### API 请求规范
- 使用 async/await
- 统一使用 `@/api` 下的接口
- 错误处理由 Axios 拦截器统一处理

### 添加新功能

#### 1. 后端添加新 API

```typescript
// 1. 在 services/ 创建 Service
// packages/backend/src/services/example.service.ts
import prisma from '@/lib/prisma';

export class ExampleService {
  async getData() {
    return await prisma.example.findMany();
  }
}

export default new ExampleService();

// 2. 在 routes/ 创建 Routes
// packages/backend/src/routes/example.routes.ts
import { Router } from 'express';
import exampleService from '@/services/example.service';
import { success, error } from '@/lib/response';

const router = Router();

router.get('/', async (req, res) => {
  try {
    const data = await exampleService.getData();
    return success(res, data);
  } catch (err: any) {
    return error(res, err.message);
  }
});

export default router;

// 3. 在 routes/index.ts 注册路由
import exampleRoutes from './example.routes';
router.use('/examples', exampleRoutes);
```

#### 2. 前端添加新页面

```vue
<!-- packages/frontend/src/views/Example.vue -->
<template>
  <div class="example-container">
    <el-card>
      <template #header>
        <span>示例页面</span>
      </template>
      <!-- 页面内容 -->
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

const data = ref([]);

const fetchData = async () => {
  // 调用 API
};

onMounted(() => {
  fetchData();
});
</script>
```

```typescript
// packages/frontend/src/router/index.ts
{
  path: 'example',
  name: 'Example',
  component: () => import('@/views/Example.vue'),
  meta: {
    title: '示例页面',
  },
}
```

### 数据库操作

使用 Prisma Studio 可视化管理数据库：

```bash
pnpm prisma:studio
```

浏览器会自动打开 http://localhost:5555

### 调试技巧

#### 后端调试
- 使用 `console.log` 输出到终端
- 请求日志会自动打印（包含时间、方法、路径）

#### 前端调试
- 使用 Vue DevTools 浏览器插件
- 在浏览器控制台查看 Network 请求

## ❓ 常见问题

### Q1: 端口被占用怎么办？

**后端端口 (8400) 被占用**:
```bash
# Windows
netstat -ano | findstr ":8400"
taskkill //F //PID <进程ID>

# 或者修改 packages/backend/.env
PORT=8401
```

**前端端口 (5173) 被占用**:
```bash
# 修改 packages/frontend/vite.config.ts
server: {
  port: 5174,
}
```

### Q2: Prisma Client 生成失败？

```bash
# 清理缓存后重试
rm -rf node_modules/.pnpm/@prisma
pnpm install
pnpm prisma:generate
```

### Q3: 数据库连接失败？

检查以下几点：
1. MySQL 服务是否运行
2. `.env` 中的 `DATABASE_URL` 是否正确
3. 数据库用户是否有权限
4. 防火墙是否阻止连接

### Q4: 前端无法访问后端 API？

1. 检查后端是否正常启动（访问 http://localhost:8400/health）
2. 检查浏览器控制台的 Network 请求
3. 确认 Vite 代理配置正确（`vite.config.ts`）

### Q5: 如何重置数据库？

```bash
# ⚠️ 警告：这将删除所有数据！
pnpm prisma:migrate reset
```

## 🔧 故障排除

### 问题：pnpm install 速度慢

**解决方案**:
```bash
# 设置国内镜像
pnpm config set registry https://registry.npmmirror.com
```

### 问题：TypeScript 报错但代码能运行

**解决方案**:
```bash
# 重新生成类型声明
pnpm prisma:generate

# 重启 IDE 的 TypeScript 服务器
# VSCode: Ctrl+Shift+P -> "TypeScript: Restart TS Server"
```

### 问题：前端页面空白

**解决方案**:
1. 打开浏览器控制台查看错误
2. 检查后端 API 是否正常
3. 清除浏览器缓存后刷新

### 问题：Element Plus 组件未自动导入

**解决方案**:
```bash
# 删除自动生成的类型文件
rm -rf packages/frontend/src/types/auto-imports.d.ts
rm -rf packages/frontend/src/types/components.d.ts

# 重启开发服务器
pnpm dev:frontend
```

## 📚 数据库表说明

项目使用 Prisma ORM 映射 XianCore 插件的 MySQL 数据库表：

| 表名 | 说明 | Prisma Model |
|-----|------|--------------|
| xian_players | 玩家基础数据 | XianPlayer |
| xian_player_skills | 玩家功法 | XianPlayerSkill |
| xian_player_equipment | 玩家装备 | XianPlayerEquipment |
| xian_player_skill_binds | 玩家技能绑定 | XianPlayerSkillBind |
| xian_sects | 宗门数据 | XianSect |
| xian_sect_members | 宗门成员 | XianSectMember |
| xian_sect_facilities | 宗门设施 | XianSectFacility |
| xian_sect_warehouses | 宗门仓库 | XianSectWarehouse |
| xian_tribulations | 天劫数据 | XianTribulation |

详细的表结构定义请查看 `packages/backend/prisma/schema.prisma`

## 🚢 生产部署

### 构建生产版本

```bash
# 构建前后端
pnpm build

# 构建产物位置
# 后端: packages/backend/dist/
# 前端: packages/frontend/dist/
```

### 启动生产服务器

```bash
# 后端
cd packages/backend
node dist/index.js

# 前端需要使用 Nginx 或其他 Web 服务器托管 dist/ 目录
```

### 环境变量（生产）

```env
DATABASE_URL="mysql://用户名:密码@主机:端口/数据库名"
PORT=8400
NODE_ENV=production
CORS_ORIGIN=https://你的域名.com
```

## 📄 License

MIT

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

在提交代码前，请确保：
- ✅ 代码通过 TypeScript 类型检查
- ✅ 遵循项目代码规范
- ✅ 测试新功能正常工作

---

**项目创建日期**: 2025-12-13
**维护者**: XianCore Team
