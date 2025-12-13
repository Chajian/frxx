# Boss配置双模式 - 快速参考

## ✅ 功能已完成

Boss刷新系统现已支持**双模式配置存储** + **自动迁移**

---

## 🚀 快速开始

### 方式1：使用YAML模式（默认，单服推荐）

```yaml
# config.yml
boss-refresh:
  storage-type: yaml
```

重启服务器即可，配置自动保存到 `boss-refresh.yml`

### 方式2：使用MySQL模式（多服推荐）

**步骤1**：创建数据库表
```bash
mysql -u root -p xiancore < create_boss_config_tables.sql
```

**步骤2**：修改配置
```yaml
# config.yml
database:
  use-mysql: true
  # ... 数据库连接信息

boss-refresh:
  storage-type: mysql
  sync-interval: 60
```

**步骤3**：重启 + 迁移
```bash
# 游戏内执行
/xiancore migrate --dry-run      # 预览
/xiancore migrate confirm        # 执行迁移
```

---

## 📋 常用命令

### Boss管理（两种模式通用）
```bash
/boss list                           # 查看所有刷新点
/boss edit dragon_lair cooldown 7200 # 修改冷却时间（自动保存）
/boss reload                         # 重载配置
```

### 迁移命令（YAML → MySQL）
```bash
/xiancore migrate --dry-run          # 预览所有迁移
/xiancore migrate confirm            # 执行所有迁移
/xiancore migrate boss-config        # 只迁移boss配置
```

---

## 📦 实现文件

### 核心代码
- `BossConfigMigrator.java` - Boss配置迁移器 ⭐ NEW
- `BossConfigLoader.java` - 配置加载器（已扩展MySQL支持）
- `BossRefreshManager.java` - Boss管理器（已支持双模式）
- `MigrationManager.java` - 迁移管理器（已注册）

### 数据库
- `create_boss_config_tables.sql` - MySQL表结构

### 配置
- `config.yml` - 新增 `boss-refresh.storage-type` 配置项

### 文档
- `BOSS_CONFIG_DUAL_MODE_IMPLEMENTATION.md` - 完整技术实现（14KB）
- `BOSS_CONFIG_STORAGE_GUIDE.md` - 用户使用指南（8.5KB）
- `BOSS_CONFIG_MIGRATION_SUMMARY.md` - 迁移功能总结（6.6KB）
- `QUICK_REFERENCE.md` - 本快速参考（当前文档）

---

## 🔧 技术特性

✅ **双模式存储** - YAML/MySQL动态切换
✅ **自动降级** - MySQL失败自动降级到YAML
✅ **迁移集成** - 完整集成到统一迁移系统
✅ **热重载** - 配置修改自动保存
✅ **跨服同步** - MySQL模式支持多服配置共享
✅ **预览模式** - 迁移前可预览（--dry-run）

---

## 📊 迁移器列表

| 迁移器 | 说明 | 状态 |
|--------|------|------|
| player | 玩家数据迁移 | ✅ |
| sect | 宗门数据迁移 | ✅ |
| boss | Boss记录迁移 | ✅ |
| **boss-config** | **Boss配置迁移** | ✅ **NEW** |
| tribulation | 渡劫记录迁移 | ✅ |
| fate | 奇遇记录迁移 | ✅ |

---

## ⚡ 编译状态

```
[INFO] BUILD SUCCESS
[INFO] Total time:  21.233 s
[INFO] Finished at: 2025-12-10T09:51:11+08:00

JAR文件: target/XianCore-1.0.0-SNAPSHOT.jar
编译文件数: 247个Java源文件
```

---

## 💡 使用建议

| 环境 | 推荐模式 |
|------|---------|
| 开发/测试 | YAML |
| 生产单服 | YAML |
| 生产多服 | MySQL |

---

## 🔍 验证方法

### 1. 查看日志
```
[XianCore] ✓ Boss配置存储模式: MYSQL
[XianCore] ✓ 已从MySQL加载Boss配置
```

### 2. 查询数据库
```sql
SELECT * FROM xian_boss_refresh_config;
SELECT * FROM xian_boss_spawn_points;
```

### 3. 游戏内测试
```bash
/boss list
/boss edit dragon_lair cooldown 7200
/boss reload
```

---

**版本**: XianCore 1.0.0-SNAPSHOT
**完成时间**: 2025-12-10
**状态**: ✅ 完全可用
