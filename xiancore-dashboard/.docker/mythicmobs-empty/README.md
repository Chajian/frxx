# MythicMobs 空目录

这是一个占位目录，当未配置 `MYTHICMOBS_MOBS_PATH` 环境变量时使用。

## 用途

- 允许 Docker 容器在没有配置 MythicMobs 路径时也能正常启动
- 作为默认挂载点，避免 volume 配置错误

## 配置 MythicMobs

如需使用 MythicMobs 功能，请：

1. 编辑项目根目录的 `.env` 文件
2. 设置 `MYTHICMOBS_MOBS_PATH` 为您的 Minecraft 服务器 MythicMobs 目录路径
3. 重启 backend 服务

示例：
```bash
# Windows
MYTHICMOBS_MOBS_PATH=D:/minecraft/server/plugins/MythicMobs/Mobs

# Linux
MYTHICMOBS_MOBS_PATH=/opt/minecraft/server/plugins/MythicMobs/Mobs
```

详细配置请参考：
- [MYTHICMOBS_SETUP.md](../../MYTHICMOBS_SETUP.md)
- [DOCKER_DEPLOYMENT.md](../../DOCKER_DEPLOYMENT.md)
