# Boss刷新配置双模式持久化 - 使用指南

## 功能概述

Boss刷新系统现在支持**两种配置存储方式**：
- **YAML模式**：配置存储在 `boss-refresh.yml` 文件中（单服推荐）
- **MySQL模式**：配置存储在 MySQL 数据库中（多服推荐，可实现配置共享）

---

## 快速开始

### 1. 数据库准备（仅MySQL模式需要）

如果您选择MySQL模式，请先执行建表脚本：

```bash
mysql -u securityuser -psecurity123 xiancore < XianCore/create_boss_config_tables.sql
```

这会创建2张表：
- `xian_boss_refresh_config` - 全局配置表
- `xian_boss_spawn_points` - Boss刷新点表

### 2. 配置模式选择

编辑 `plugins/XianCore/config.yml`：

```yaml
boss-refresh:
  # 选择存储模式
  storage-type: yaml  # 或 mysql

  # MySQL模式下的同步间隔（秒）
  sync-interval: 60
```

### 3. 重启服务器

修改配置后重启服务器，系统会自动使用您选择的存储方式。

---

## 模式对比

| 特性 | YAML模式 | MySQL模式 |
|------|---------|----------|
| **存储位置** | `boss-refresh.yml` 文件 | MySQL数据库 |
| **适用场景** | 单服务器 | 多服务器（跨服配置共享） |
| **配置管理** | 手动编辑文件 | 命令或数据库 |
| **性能** | 启动时一次性加载 | 启动时从数据库加载 |
| **版本控制** | 支持Git管理 | 需要数据库备份 |
| **跨服同步** | 需要手动分发 | 自动同步 |
| **依赖** | 无 | 需要MySQL连接 |

---

## 使用示例

### 场景1：单服务器（推荐YAML）

```yaml
# config.yml
boss-refresh:
  storage-type: yaml
```

**优势：**
- 配置直观，可以直接编辑YAML文件
- 无数据库依赖，性能更好
- 支持Git版本控制

**操作：**
```bash
# 查看Boss配置
/boss list

# 修改刷新间隔
/boss edit dragon_lair cooldown 7200

# 配置自动保存到 boss-refresh.yml ✅
```

### 场景2：多服务器（推荐MySQL）

```yaml
# config.yml - 所有服务器统一配置
database:
  use-mysql: true
  host: 192.168.1.100
  database: xiancore
  username: securityuser
  password: security123

boss-refresh:
  storage-type: mysql  # ← 使用MySQL模式
  sync-interval: 60    # 每60秒同步配置
```

**优势：**
- 配置集中存储，一处修改全服生效
- 支持配置管理后台
- 自动跨服同步

**操作：**
```bash
# 在主服务器修改配置
/boss edit dragon_lair cooldown 7200

# 配置保存到MySQL ✅
# 60秒后，所有服务器自动同步新配置 ✅
```

---

## 模式切换

### YAML → MySQL 迁移

**方式1：使用迁移系统（推荐）** ⭐

```bash
# 1. 执行建表脚本
mysql -u securityuser -psecurity123 xiancore < XianCore/create_boss_config_tables.sql

# 2. 修改 config.yml
```

```yaml
boss-refresh:
  storage-type: mysql  # 切换到MySQL模式
```

```bash
# 3. 重启服务器

# 4. 执行迁移命令
/xiancore migrate --dry-run

# 输出示例：
# [XianCore] [4/6] 开始迁移: Boss刷新配置迁移器
# [XianCore]   → 找到 3 个刷新点配置
# [XianCore]   [预览] 将迁移以下配置:
# [XianCore]     - 全局配置: 检查间隔=30秒
# [XianCore]     - 刷新点数: 3 个
# [XianCore]       * dragon_lair (EnderDragon, Tier 4)
# [XianCore]       * spider_nest (CaveSpider, Tier 2)
# [XianCore]       * zombie_horde (Zombie, Tier 1)

# 5. 确认无误后执行真实迁移
/xiancore migrate confirm

# [XianCore] ✓ Boss配置迁移完成！
```

**方式2：手动迁移**

```bash
# 1. 备份现有YAML配置
cp plugins/XianCore/boss-refresh.yml boss-refresh.yml.backup

# 2. 执行建表脚本
mysql -u root -p xiancore < XianCore/create_boss_config_tables.sql

# 3. 修改config.yml
```

```yaml
boss-refresh:
  storage-type: mysql  # 从yaml改为mysql
```

```bash
# 4. 重启服务器（会从YAML加载配置）

# 5. 触发保存到MySQL（任选其一）
/boss reload
# 或
/boss edit dragon_lair cooldown 7200
```

配置现在已保存到MySQL ✅

### MySQL → YAML 迁移

1. **导出MySQL配置到YAML（可选）**
```bash
# 如果需要保留当前MySQL配置，可以：
# 1. 在MySQL模式下先备份一次
# 2. 或手动从数据库导出到YAML
```

2. **修改config.yml**
```yaml
boss-refresh:
  storage-type: yaml  # 从mysql改为yaml
```

3. **重启服务器**

后续所有配置修改将保存到 `boss-refresh.yml` 文件。

---

## 高级功能

### 自动降级（容错机制）

如果MySQL模式下数据库连接失败，系统会自动降级到YAML模式：

```
[XianCore] ✓ Boss配置存储模式: MYSQL
[XianCore] ✗ MySQL加载失败，降级到YAML模式
[XianCore] ✓ Boss 配置已加载: 3 个刷新点
```

### 配置同步（MySQL模式）

多服务器环境下，每60秒（可配置）自动同步最新配置：

```yaml
boss-refresh:
  storage-type: mysql
  sync-interval: 60  # 同步间隔（秒）
```

**工作流程：**
1. 管理员在服务器A修改配置 → 保存到MySQL
2. 服务器B每60秒检查数据库
3. 发现配置更新 → 自动重载
4. 无需手动重启 ✅

---

## 命令参考

### 配置管理命令

所有命令在两种模式下都完全相同：

```bash
# 查看配置
/boss list
/boss info dragon_lair

# 修改配置（自动保存）
/boss edit dragon_lair cooldown 7200
/boss enable dragon_lair
/boss disable dragon_lair

# 重载配置
/boss reload
```

**区别：**
- **YAML模式**：修改后保存到 `boss-refresh.yml`
- **MySQL模式**：修改后保存到数据库

### 迁移命令（新增）

```bash
# 预览迁移（不写入数据库）
/xiancore migrate --dry-run

# 执行真实迁移
/xiancore migrate confirm

# 只迁移Boss配置
/xiancore migrate boss-config --dry-run
/xiancore migrate boss-config confirm
```

**迁移系统支持：**
- ✅ player - 玩家数据迁移
- ✅ sect - 宗门数据迁移
- ✅ boss - Boss记录迁移
- ✅ **boss-config** - **Boss配置迁移（新增）**
- ✅ tribulation - 渡劫记录迁移
- ✅ fate - 奇遇记录迁移

---

## 常见问题

### Q: 如何确认当前使用的模式？

**A:** 查看服务器启动日志：

```
[XianCore] ✓ Boss配置存储模式: YAML
# 或
[XianCore] ✓ Boss配置存储模式: MYSQL
[XianCore] ✓ 已从MySQL加载Boss配置
```

### Q: MySQL模式下配置没有保存到数据库？

**A:** 检查以下几点：
1. `database.use-mysql` 是否为 `true`
2. MySQL连接是否正常（查看启动日志）
3. 是否执行了建表脚本
4. 用户是否有写入权限

### Q: 多服务器配置不同步？

**A:** 检查：
1. 所有服务器的 `storage-type` 都是 `mysql`
2. 所有服务器连接同一个数据库
3. `sync-interval` 设置是否合理（建议60秒）

### Q: 想从MySQL导出配置到YAML文件？

**A:** 临时切换到MySQL模式，执行一次 `/boss reload`，然后切回YAML模式。

---

## 数据库表结构

### xian_boss_refresh_config（全局配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 固定为1（单行配置） |
| check_interval_seconds | INT | 检查间隔（秒） |
| max_active_bosses | INT | 最大活跃Boss数 |
| min_online_players | INT | 最少在线玩家数 |
| enabled | BOOLEAN | 是否启用系统 |
| updated_at | BIGINT | 最后更新时间 |

### xian_boss_spawn_points（刷新点配置）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 刷新点ID |
| world_name | VARCHAR(64) | 世界名称 |
| x, y, z | DOUBLE | 坐标 |
| mythic_mob_id | VARCHAR(128) | MythicMobs ID |
| tier | INT | Boss等级(1-4) |
| cooldown_seconds | BIGINT | 冷却时间（秒） |
| max_count | INT | 最大Boss数量 |
| current_count | INT | 当前Boss数量 |
| last_spawn_time | BIGINT | 最后刷新时间 |
| enabled | BOOLEAN | 是否启用 |
| spawn_mode | VARCHAR(32) | 刷新模式 |
| ... | ... | 其他配置字段 |

---

## 总结

✅ **单服务器** → 使用 **YAML模式**（简单、高效）
✅ **多服务器** → 使用 **MySQL模式**（集中管理、自动同步）
✅ **灵活切换** → 支持随时切换模式，无需迁移数据
✅ **自动降级** → MySQL失败自动降级到YAML，确保系统可用

**推荐配置：**
- 开发/测试环境：`storage-type: yaml`
- 生产单服：`storage-type: yaml`
- 生产多服：`storage-type: mysql`
