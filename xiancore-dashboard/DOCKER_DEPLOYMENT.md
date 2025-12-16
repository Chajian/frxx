# Docker 部署指南

## 📦 项目容器化说明

XianCore Dashboard 已完整支持 Docker 容器化部署，包含以下服务：

- **MySQL 8.0**: 数据库服务
- **Backend**: Express + Prisma + TypeScript API 服务
- **Frontend**: Vue 3 + Nginx 静态资源服务

## 🚀 快速开始

### 方式一：使用一键部署脚本（推荐）

我们提供了三种一键部署脚本，可以极大简化部署流程：

#### Windows 用户

**选项 1：批处理脚本（适合所有 Windows 版本）**

直接双击运行 `deploy.bat`，或在命令提示符中执行：

```cmd
deploy.bat
```

**选项 2：PowerShell 脚本（推荐，功能更强大）**

右键点击 `deploy.ps1` 选择"使用 PowerShell 运行"，或在 PowerShell 中执行：

```powershell
.\deploy.ps1
```

如果遇到执行策略错误，请以管理员身份运行 PowerShell 并执行：

```powershell
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
```

PowerShell 脚本支持命令行参数：

```powershell
.\deploy.ps1 -Action start     # 启动服务
.\deploy.ps1 -Action stop      # 停止服务
.\deploy.ps1 -Action restart   # 重启服务
.\deploy.ps1 -Action status    # 查看状态
.\deploy.ps1 -Action backup    # 备份数据库
```

#### Linux/Mac 用户

添加执行权限并运行：

```bash
chmod +x deploy.sh
./deploy.sh
```

#### 一键脚本功能

所有脚本提供以下功能：

1. **首次部署** - 自动检查环境、配置文件、构建镜像并启动服务
2. **启动服务** - 启动所有 Docker 容器
3. **停止服务** - 停止所有运行中的容器
4. **重启服务** - 重启所有容器
5. **查看状态** - 显示所有容器的运行状态
6. **查看日志** - 实时查看服务日志（支持按服务筛选）
7. **重新构建** - 清理并重新构建所有镜像
8. **清理数据** - 停止并删除所有容器、网络和数据卷
9. **备份数据库** - 导出数据库到 SQL 文件
10. **恢复数据库** - 从备份文件恢复数据库（仅 PowerShell 版本）
11. **进入容器** - 进入容器内部进行调试
12. **健康检查** - 检查服务的健康状态（仅 PowerShell 版本）

#### 首次部署步骤

1. 运行对应平台的部署脚本
2. 选择 `1. 首次部署`
3. 脚本会自动：
   - 检查 Docker 是否安装
   - 创建 `.env` 配置文件（如果不存在）
   - 构建 Docker 镜像
   - 启动所有服务
4. 访问 http://localhost 即可使用

### 方式二：手动部署

如果你更喜欢手动控制部署过程，可以使用以下命令：

#### 前置要求

- Docker 20.10+
- Docker Compose 2.0+

#### 1. 配置环境变量

复制环境变量示例文件并根据需要修改：

```bash
cp .env.docker.example .env
```

编辑 `.env` 文件，配置以下关键参数：

```env
# MySQL 配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=xiancore
MYSQL_USER=securityuser
MYSQL_PASSWORD=your_secure_password

# 服务端口
BACKEND_PORT=8400
FRONTEND_PORT=80

# CORS 配置（生产环境请修改为实际域名）
CORS_ORIGIN=http://your-domain.com

# 可选：MythicMobs 配置（详细配置见下方说明）
MYTHICMOBS_MOBS_PATH=/path/to/minecraft/server/plugins/MythicMobs/Mobs
```

**重要提示：**
- 如果需要使用 MythicMobs 可视化功能，请参考下方 [MythicMobs 配置](#mythicmobs-配置) 章节
- 如果不需要此功能，请保持该值为空或注释掉相关配置

### 2. 启动所有服务

```bash
docker-compose up -d
```

这将启动以下服务：
- **MySQL**: `localhost:3306`
- **Backend API**: `localhost:8400`
- **Frontend**: `localhost:80`

### 3. 查看服务状态

```bash
# 查看所有容器状态
docker-compose ps

# 查看服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

### 4. 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止服务并删除数据卷（⚠️ 会删除数据库数据）
docker-compose down -v
```

## 🔧 开发与调试

### 单独构建服务

```bash
# 构建 Backend
docker-compose build backend

# 构建 Frontend
docker-compose build frontend

# 构建所有服务
docker-compose build
```

### 重启单个服务

```bash
# 重启 Backend
docker-compose restart backend

# 重启 Frontend
docker-compose restart frontend
```

### 进入容器调试

```bash
# 进入 Backend 容器
docker-compose exec backend sh

# 进入 Frontend 容器
docker-compose exec frontend sh

# 进入 MySQL 容器
docker-compose exec mysql bash
```

### 数据库操作

```bash
# 连接到 MySQL
docker-compose exec mysql mysql -u securityuser -p xiancore

# 备份数据库
docker-compose exec mysql mysqldump -u root -p xiancore > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -u root -p xiancore < backup.sql
```

### 运行 Prisma 命令

```bash
# 生成 Prisma Client
docker-compose exec backend pnpm prisma:generate

# 运行数据库迁移
docker-compose exec backend pnpm prisma:migrate

# 打开 Prisma Studio
docker-compose exec backend pnpm prisma:studio
```

## 📊 健康检查

所有服务都配置了健康检查：

- **Backend**: `http://localhost:8400/api/health`
- **Frontend**: `http://localhost/health`
- **MySQL**: 自动 ping 检查

查看健康状态：

```bash
docker-compose ps
```

## 🎮 MythicMobs 配置

如果您的项目需要使用 MythicMobs 可视化功能，需要将 Minecraft 服务器上的 MythicMobs 目录挂载到 Docker 容器中。

### 配置步骤

#### 1. 设置环境变量

编辑 `.env` 文件，设置 MythicMobs 目录的完整路径：

```bash
# Windows 路径示例（使用正斜杠）
MYTHICMOBS_MOBS_PATH=D:/minecraft/server/plugins/MythicMobs/Mobs

# Linux 路径示例
MYTHICMOBS_MOBS_PATH=/opt/minecraft/server/plugins/MythicMobs/Mobs
```

**注意事项：**
- Windows 路径必须使用正斜杠 `/` 而不是反斜杠 `\`
- 必须是绝对路径
- 确保 Docker 有权限访问该目录

#### 2. 验证配置

重启服务后，检查配置是否生效：

```bash
# 重启后端服务
docker-compose restart backend

# 查看日志确认路径
docker logs xiancore-backend | grep MythicMobs
# 应该看到: 📁 MythicMobs Path: /app/mythicmobs

# 测试 API
curl http://localhost:8400/api/boss/mythicmobs
```

#### 3. 验证文件挂载

```bash
# 检查容器内是否能访问文件
docker exec xiancore-backend ls -la /app/mythicmobs

# 查看具体文件
docker exec xiancore-backend cat /app/mythicmobs/example_mob.yml
```

### 不使用 MythicMobs 功能

如果不需要此功能：

1. **方法一**：保持环境变量为空
   ```bash
   MYTHICMOBS_MOBS_PATH=
   ```
   同时注释 `docker-compose.yml` 中的 volume 挂载：
   ```yaml
   volumes:
     # - ${MYTHICMOBS_MOBS_PATH}:/app/mythicmobs:ro
     - backend_logs:/app/packages/backend/logs
   ```

2. **方法二**：完全移除相关配置
   - 从 `.env` 中删除 `MYTHICMOBS_MOBS_PATH` 行
   - 从 `docker-compose.yml` 中删除对应的 volume 挂载

### 故障排除

#### 容器无法启动

```bash
# 检查挂载配置
docker inspect xiancore-backend | grep -A 10 Mounts

# 确认路径是否正确
ls -la "$(grep MYTHICMOBS_MOBS_PATH .env | cut -d= -f2)"
```

#### API 返回空数据

```bash
# 检查文件权限
docker exec xiancore-backend ls -la /app/mythicmobs

# 查看后端日志
docker logs xiancore-backend --tail 50
```

### 相关 API 端点

- `GET /api/boss/mythicmobs` - 获取所有怪物列表
- `GET /api/boss/mythicmobs/:id` - 获取怪物详情
- `GET /api/boss/mythicmobs/:id/detail` - 获取完整配置
- `POST /api/boss/mythicmobs/refresh` - 刷新缓存

更多详细信息请参考 [MYTHICMOBS_SETUP.md](./MYTHICMOBS_SETUP.md)

## 🔒 生产部署建议

### 1. 安全配置

- ✅ 修改所有默认密码
- ✅ 使用环境变量管理敏感信息
- ✅ 配置防火墙规则
- ✅ 启用 HTTPS（使用 Nginx 反向代理 + Let's Encrypt）

### 2. 性能优化

```yaml
# 在 docker-compose.yml 中为服务添加资源限制
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. 数据持久化

数据卷已配置：
- `mysql_data`: MySQL 数据库文件
- `backend_logs`: Backend 日志文件

备份这些卷以防止数据丢失：

```bash
# 备份数据卷
docker run --rm -v xiancore-dashboard_mysql_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/mysql_backup_$(date +%Y%m%d).tar.gz /data
```

### 4. 使用 HTTPS

创建 `docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  nginx-proxy:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx-prod.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - frontend
```

### 5. 监控和日志

配置日志轮转：

```yaml
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

## 🌐 网络配置

所有服务运行在独立的 Docker 网络 `xiancore-network` 中：

- Frontend -> Backend: 通过 `http://backend:8400`
- Backend -> MySQL: 通过 `mysql://mysql:3306`

## 📁 目录结构

```
xiancore-dashboard/
├── docker-compose.yml          # Docker Compose 配置
├── .env.docker.example         # 环境变量示例
├── .env                        # 环境变量（需创建）
├── .dockerignore               # Docker 忽略文件
├── deploy.bat                  # Windows 批处理部署脚本
├── deploy.sh                   # Linux/Mac Shell 部署脚本
├── deploy.ps1                  # Windows PowerShell 部署脚本（增强版）
├── packages/
│   ├── backend/
│   │   ├── Dockerfile         # Backend Dockerfile
│   │   └── ...
│   └── frontend/
│       ├── Dockerfile         # Frontend Dockerfile
│       ├── nginx.conf         # Nginx 配置
│       └── ...
└── DOCKER_DEPLOYMENT.md       # 本文档
```

## 🐛 故障排除

### 1. 容器启动失败

```bash
# 查看详细日志
docker-compose logs -f [service-name]

# 重新构建并启动
docker-compose up -d --build
```

### 2. 数据库连接失败

- 检查 MySQL 容器是否健康：`docker-compose ps`
- 检查环境变量配置是否正确
- 确保 Backend 在 MySQL 完全启动后才启动（已配置 `depends_on`）

### 3. Frontend 无法访问 Backend

- 检查 Nginx 配置中的代理设置
- 确保 Backend 服务运行正常
- 查看 Frontend 容器日志

### 4. 端口冲突

修改 `.env` 文件中的端口配置：

```env
BACKEND_PORT=8401
FRONTEND_PORT=8080
MYSQL_PORT=3307
```

### 5. 权限问题

```bash
# 重置文件权限
sudo chown -R $USER:$USER .

# 清理并重建
docker-compose down -v
docker-compose up -d --build
```

## 📚 更多资源

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Prisma Docker 指南](https://www.prisma.io/docs/guides/deployment/deployment-guides/deploying-to-docker)

## 🤝 贡献

如有问题或建议，请提交 Issue 或 Pull Request。

## 📄 许可证

与项目主许可证一致。
