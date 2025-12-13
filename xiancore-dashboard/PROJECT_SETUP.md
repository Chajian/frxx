# XianCore Dashboard 项目搭建完成

## 项目概览

已成功搭建 XianCore 管理后台的完整基础框架，包括前端和后端的基础结构。

## 技术栈

### 后端
- ✅ Express + TypeScript
- ✅ Prisma ORM (MySQL)
- ✅ 分层架构（Routes → Services → Prisma）
- ✅ 统一响应格式
- ✅ 错误处理中间件

### 前端
- ✅ Vue 3 + TypeScript
- ✅ Element Plus UI 组件库
- ✅ Pinia 状态管理
- ✅ Vue Router 路由
- ✅ Axios 请求封装
- ✅ Vite 构建工具

## 项目结构

```
xiancore-dashboard/
├── packages/
│   ├── backend/                    # 后端服务
│   │   ├── prisma/
│   │   │   └── schema.prisma       # Prisma 数据模型（已映射所有 XianCore 表）
│   │   ├── src/
│   │   │   ├── lib/
│   │   │   │   ├── prisma.ts       # Prisma Client 实例
│   │   │   │   └── response.ts     # 统一响应格式
│   │   │   ├── services/           # 业务逻辑层
│   │   │   │   ├── player.service.ts
│   │   │   │   └── sect.service.ts
│   │   │   ├── routes/             # 路由层
│   │   │   │   ├── index.ts
│   │   │   │   ├── player.routes.ts
│   │   │   │   └── sect.routes.ts
│   │   │   └── index.ts            # 入口文件
│   │   ├── .env                    # 环境变量
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
│       │   │   └── request.ts      # Axios 请求封装
│       │   ├── views/              # 页面组件
│       │   │   ├── Dashboard.vue   # 仪表盘
│       │   │   ├── Players.vue     # 玩家管理
│       │   │   └── Sects.vue       # 宗门管理
│       │   ├── router/
│       │   │   └── index.ts        # 路由配置
│       │   ├── App.vue
│       │   └── main.ts
│       ├── index.html
│       ├── vite.config.ts
│       ├── package.json
│       └── tsconfig.json
│
├── package.json                    # Monorepo 配置
├── pnpm-workspace.yaml
└── README.md
```

## 已实现的功能

### 后端 API

#### 玩家相关
- `GET /api/players` - 获取所有玩家
- `GET /api/players/ranking?limit=10` - 获取玩家排行榜
- `GET /api/players/:uuid` - 获取玩家详情

#### 宗门相关
- `GET /api/sects` - 获取所有宗门
- `GET /api/sects/ranking?limit=10` - 获取宗门排行榜
- `GET /api/sects/:id` - 获取宗门详情

#### 系统
- `GET /health` - 健康检查

### 前端页面

#### 1. 仪表盘 (`/dashboard`)
- 统计卡片：玩家总数、宗门总数、在线玩家、最高境界
- 玩家等级排行榜（Top 10）
- 宗门等级排行榜（Top 10）

#### 2. 玩家管理 (`/players`)
- 玩家列表展示
- 搜索功能
- 查看详情（待实现）

#### 3. 宗门管理 (`/sects`)
- 宗门列表展示
- 查看详情（待实现）

#### 4. 排行榜 (`/ranking`)
- 暂时复用仪表盘页面

### 核心特性

✅ **分层架构**
- Controller (Routes) → Service → Prisma
- 职责清晰，易于维护

✅ **TypeScript 全栈**
- 类型安全
- 开发体验好

✅ **统一响应格式**
```typescript
{
  code: 0,      // 0 = 成功，其他 = 错误码
  message: string,
  data?: T
}
```

✅ **错误处理**
- 全局错误拦截
- 友好的错误提示

✅ **自动化工具**
- Element Plus 组件自动导入
- Vue API 自动导入
- 热重载

## 启动项目

### 1. 安装依赖
```bash
cd D:/workspace/java/mc/frxx/xiancore-dashboard
pnpm install
```

### 2. 配置环境变量
后端的 `.env` 文件已创建：
```
DATABASE_URL="mysql://securityuser:security123@localhost:3306/xiancore"
PORT=8400
NODE_ENV=development
CORS_ORIGIN=http://localhost:5173
```

### 3. 生成 Prisma Client
```bash
pnpm prisma:generate
```

### 4. 启动开发服务器
```bash
# 同时启动前后端
pnpm dev

# 或分别启动
pnpm dev:backend    # http://localhost:8400
pnpm dev:frontend   # http://localhost:5173
```

### 5. 访问应用
- 前端：http://localhost:5173
- 后端 API：http://localhost:8400
- Prisma Studio：`pnpm prisma:studio`

## 数据库表映射

Prisma Schema 已映射所有 XianCore 插件的数据库表：

| 表名 | 说明 | Prisma Model |
|-----|------|--------------|
| xian_players | 玩家数据 | XianPlayer |
| xian_player_skills | 玩家功法 | XianPlayerSkill |
| xian_player_equipment | 玩家装备 | XianPlayerEquipment |
| xian_player_skill_binds | 玩家技能绑定 | XianPlayerSkillBind |
| xian_sects | 宗门数据 | XianSect |
| xian_sect_members | 宗门成员 | XianSectMember |
| xian_sect_facilities | 宗门设施 | XianSectFacility |
| xian_sect_warehouses | 宗门仓库 | XianSectWarehouse |
| xian_tribulations | 天劫数据 | XianTribulation |

## 下一步开发建议

### 短期（基础完善）
1. ✅ 完成玩家详情页面
2. ✅ 完成宗门详情页面
3. ✅ 添加数据刷新功能
4. ✅ 添加分页功能
5. ✅ 优化移动端适配

### 中期（功能扩展）
1. ✅ 添加 Boss 管理模块
2. ✅ 添加天劫数据展示
3. ✅ 添加数据可视化图表
4. ✅ 添加导出功能
5. ✅ 读取 YML 配置文件

### 长期（高级功能）
1. ✅ 用户认证与权限
2. ✅ 操作日志记录
3. ✅ 数据备份与恢复
4. ✅ 性能优化与缓存
5. ✅ Docker 部署

## 开发规范

### 代码风格
- 使用 TypeScript 严格模式
- 遵循 ESLint 规则
- 组件使用 `<script setup>` 语法
- API 使用 async/await

### 命名规范
- 文件名：kebab-case (如: `player-service.ts`)
- 组件名：PascalCase (如: `BasicLayout.vue`)
- 变量/函数：camelCase (如: `fetchPlayers`)
- 类型/接口：PascalCase (如: `Player`, `ApiResponse`)

### Git 提交规范
- `feat: 新功能`
- `fix: 修复bug`
- `docs: 文档更新`
- `style: 代码格式调整`
- `refactor: 重构`
- `test: 测试`
- `chore: 构建/工具链`

## 常见问题

### Q: Prisma Client 报错？
A: 运行 `pnpm prisma:generate` 生成客户端

### Q: 前端无法访问后端？
A: 检查 Vite 代理配置和后端 CORS 设置

### Q: 数据库连接失败？
A: 检查 `.env` 中的 `DATABASE_URL` 配置

### Q: 端口被占用？
A: 修改 `.env` 中的 `PORT` 或前端 `vite.config.ts` 中的 `server.port`

## 项目状态

🎉 **基础框架已完成！**

✅ 后端 API 框架
✅ 前端页面框架
✅ 数据库映射
✅ 路由配置
✅ 请求封装
✅ 布局组件
✅ 基础页面

🚧 **待开发功能**
- 详情页面
- 更多 API 接口
- 数据图表
- 权限管理
- 部署配置

---

**创建日期**: 2025-12-13
**作者**: Claude Code
**项目位置**: D:/workspace/java/mc/frxx/xiancore-dashboard
