# XianCore MySQL 迁移使用指南

本指南说明如何将现有的 YAML 数据迁移到 MySQL 数据库。

---

## 📋 目录
1. [前置准备](#前置准备)
2. [启用 MySQL](#启用-mysql)
3. [数据迁移](#数据迁移)
4. [验证迁移](#验证迁移)
5. [故障排查](#故障排查)

---

## 前置准备

### 1. 备份数据（重要！）
```bash
# 备份整个插件目录
cp -r plugins/XianCore plugins/XianCore.backup

# 或者只备份数据文件
tar -czf xiancore-data-backup-$(date +%Y%m%d).tar.gz \
  plugins/XianCore/players \
  plugins/XianCore/sects \
  plugins/XianCore/boss
```

### 2. 确认 MySQL 配置
编辑 `plugins/XianCore/config.yml`:
```yaml
database:
  use-mysql: true
  host: localhost
  port: 3306
  database: xiancore
  username: root
  password: your_password
  pool-size: 10
```

### 3. 确认 MySQL 连接
重启服务器，检查日志：
```
[XianCore] ✓ MySQL 连接成功
[XianCore] ✓ 数据表创建/检查完成（包含3张新表）
```

---

## 启用 MySQL

### 方法 1: 配置文件（推荐）
```yaml
# config.yml
database:
  use-mysql: true  # 改为 true
```

### 方法 2: 运行时切换
```
/xian db switch mysql
```

重启服务器使配置生效。

---

## 数据迁移

### 支持的数据类型

| 数据类型 | 迁移器 | YML文件 | 表名 |
|---------|--------|---------|------|
| 玩家数据 | player | players/*.yml | xian_players |
| 技能绑定 | player | players/*.yml | xian_player_skill_binds |
| 宗门基础 | sect | sects/[数字].yml | xian_sects |
| 宗门设施 | facility | sects/*_facilities.yml | xian_sect_facilities |
| 宗门仓库 | warehouse | sects/*_warehouse.yml | xian_sect_warehouses |
| Boss记录 | boss | boss/*.yml | xian_bosses |
| 渡劫数据 | tribulation | tribulations/*.yml | xian_tribulations |
| 奇遇数据 | fate | fates/*.yml | xian_fates |

### 1. 预览迁移（推荐先执行）
```
/xian migrate all --dry-run
```

这会显示：
- 将要迁移的文件数量
- 数据大小
- 预计耗时
- **不会实际写入数据库**

### 2. 执行完整迁移
```
/xian migrate all
```

控制台输出示例：
```
[XianCore] ========================================
[XianCore]        开始完整数据迁移
[XianCore] ========================================
[XianCore] 模式: 真实迁移
[XianCore] 迁移器数量: 8
[XianCore]
[XianCore] [1/8] 开始迁移: 玩家数据迁移器
[XianCore] 找到 150 个玩家文件
[XianCore] 进度: 20% (30/150)
[XianCore] 进度: 40% (60/150)
[XianCore] ...
[XianCore] [1/8] 玩家数据迁移器 迁移完成！
[XianCore]
[XianCore] [2/8] 开始迁移: 宗门数据迁移器
[XianCore] 找到 5 个宗门文件
[XianCore] 迁移宗门: 天道宗 (ID: 1)
[XianCore] ...
```

### 3. 单独迁移特定类型
```
# 只迁移玩家数据
/xian migrate player

# 只迁移宗门设施
/xian migrate facility

# 只迁移宗门仓库
/xian migrate warehouse

# 查看所有迁移器
/xian migrate list
```

### 4. 迁移行为说明

#### 自动跳过已存在数据
如果数据库中已存在记录，迁移器会自动跳过：
```
[XianCore] 跳过: player-uuid.yml (已存在)
[XianCore] 跳过: 1_facilities.yml (已存在)
```

这意味着：
- ✅ 可以安全地多次运行迁移
- ✅ 中断后可以继续
- ⚠️ 不会更新已存在的数据

#### 保留 YML 文件
迁移**不会删除** YML 文件，它们作为备份保留。

#### 错误容错
单个文件迁移失败不影响其他文件：
```
[XianCore] ✓ 成功: 100  ✗ 失败: 2  ○ 跳过: 10
[XianCore] 失败详情:
  - player-abc.yml: 无法解析UUID
  - sect-999.yml: 缺少必需字段
```

---

## 验证迁移

### 1. 检查表记录数
连接到 MySQL：
```sql
USE xiancore;

-- 玩家数据
SELECT COUNT(*) FROM xian_players;
SELECT COUNT(*) FROM xian_player_skill_binds;

-- 宗门数据
SELECT COUNT(*) FROM xian_sects;
SELECT COUNT(*) FROM xian_sect_facilities;
SELECT COUNT(*) FROM xian_sect_warehouses;

-- Boss数据
SELECT COUNT(*) FROM xian_bosses;
```

### 2. 对比文件数量
```bash
# 玩家文件数
ls plugins/XianCore/players/*.yml | wc -l

# 宗门主文件数（排除设施和仓库）
ls plugins/XianCore/sects/[0-9]*.yml | wc -l

# 设施文件数
ls plugins/XianCore/sects/*_facilities.yml | wc -l

# 仓库文件数
ls plugins/XianCore/sects/*_warehouse.yml | wc -l
```

### 3. 游戏内测试
- 登录玩家检查数据（境界、灵石、功法）
- 检查技能快捷键绑定
- 访问宗门设施
- 查看宗门仓库物品

---

## 故障排查

### 问题 1: MySQL 连接失败
```
[XianCore] ✗ MySQL连接失败！
```

**解决方案**:
1. 检查 MySQL 服务是否运行
2. 验证 config.yml 中的连接信息
3. 确认防火墙允许连接
4. 测试连接：`mysql -h localhost -u root -p`

### 问题 2: 表不存在
```
[XianCore] SQLException: Table 'xiancore.xian_sect_facilities' doesn't exist
```

**解决方案**:
```
/xian db init     # 重新创建表
```

或手动执行 SQL：
```sql
-- 查看 SchemaManager.java 中的 CREATE TABLE 语句
```

### 问题 3: 迁移卡住不动
```
[XianCore] 进度: 20% (30/150)
[然后停止]
```

**解决方案**:
1. 检查控制台是否有错误日志
2. 检查 MySQL 慢查询日志
3. 增大 `pool-size` 配置
4. 分批迁移：
   ```
   /xian migrate player --dry-run  # 先测试
   /xian migrate player            # 再执行
   ```

### 问题 4: 数据丢失部分字段
```
[XianCore] 警告: 加载仓库物品失败 (宗门=1, 槽位=5)
```

**解决方案**:
1. 检查物品是否使用了不支持的 NBT 数据
2. 查看完整错误堆栈：`/xian log level DEBUG`
3. 手动检查 YML 文件是否损坏
4. 如果是物品序列化问题，可能需要特殊处理

### 问题 5: 重复迁移如何处理
**情况**: 已经迁移过，想重新迁移

**方案 A - 清空表后重新迁移**:
```sql
-- ⚠️ 危险操作！会删除所有数据！
TRUNCATE TABLE xian_players;
TRUNCATE TABLE xian_player_skill_binds;
TRUNCATE TABLE xian_sect_facilities;
TRUNCATE TABLE xian_sect_warehouses;
```

**方案 B - 删除特定记录**:
```sql
-- 删除特定玩家
DELETE FROM xian_players WHERE uuid = 'xxx-xxx-xxx';

-- 删除特定宗门的设施
DELETE FROM xian_sect_facilities WHERE sect_id = 1;
```

然后重新运行迁移。

---

## 性能优化

### 大数据量迁移（500+ 玩家）

**1. 调整 MySQL 配置** (`my.cnf`):
```ini
[mysqld]
max_allowed_packet = 64M
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
```

**2. 增加连接池**:
```yaml
# config.yml
database:
  pool-size: 20  # 默认10，可增加到20
```

**3. 关闭服务器迁移**:
```bash
# 停止服务器
stop

# 后台执行迁移（通过控制台命令）
/xian migrate all

# 等待完成后重启
```

**4. 分批迁移**:
```
# 先迁移核心数据
/xian migrate player
/xian migrate sect

# 再迁移扩展数据
/xian migrate facility
/xian migrate warehouse
/xian migrate boss
```

---

## 回滚到 YAML 模式

如果需要暂时回退：

### 1. 禁用 MySQL
```yaml
# config.yml
database:
  use-mysql: false
```

### 2. 重启服务器
服务器会自动切换回 YAML 文件读写。

### 3. 数据同步注意
⚠️ **重要**: MySQL 和 YAML 不会自动同步！

- 如果在 MySQL 模式下修改了数据，切回 YAML 后这些修改不会反映
- 如果在 YAML 模式下修改了数据，切回 MySQL 后这些修改不会反映

**建议**: 选择一种模式长期使用，避免频繁切换。

---

## 常见问题 FAQ

### Q1: 迁移需要多长时间？
**A**: 取决于数据量：
- 100 个玩家：约 3-5 秒
- 500 个玩家：约 15-20 秒
- 1000 个玩家：约 30-40 秒

### Q2: 迁移会影响服务器性能吗？
**A**:
- 使用 `--dry-run` 不影响性能
- 真实迁移会占用少量 CPU 和 MySQL 连接
- 建议在玩家较少时执行

### Q3: 可以边运行服务器边迁移吗？
**A**:
- ✅ 可以，但不推荐
- 迁移期间新数据可能不会被迁移
- 建议在服务器启动时或玩家离线时执行

### Q4: 迁移失败会损坏数据吗？
**A**:
- ❌ 不会！原始 YML 文件不会被修改或删除
- 失败的迁移可以重新执行
- 数据库有事务保护（如果中途失败会回滚）

### Q5: 使用 MySQL 后还能改回 YAML 吗？
**A**:
- ✅ 可以随时切换
- 但需要注意数据一致性问题（见"回滚到 YAML 模式"章节）

---

## 技术细节

### ItemStack JSON 序列化
宗门仓库使用 JSON 存储物品：

```json
{
  "0": {
    "type": "DIAMOND_SWORD",
    "amount": 1,
    "meta": {
      "enchants": {"SHARPNESS": 5},
      "display-name": "§c传说之剑",
      "lore": ["§7一把强大的剑"]
    }
  }
}
```

优点：
- 人类可读
- 支持所有 Bukkit ItemStack 元数据
- 跨版本兼容

### 双模式架构
Repository 根据配置自动选择存储方式：

```java
public void save(Data data) {
    if (databaseManager.isUseMySql()) {
        saveToDatabase(data);  // MySQL
    } else {
        saveToFile(data);      // YAML
    }
}
```

这意味着：
- 无需修改业务代码
- 运行时可切换
- 完全透明

---

## 总结

✅ **迁移前**: 备份数据、配置 MySQL、测试连接
✅ **迁移中**: 先 dry-run、再真实迁移、监控日志
✅ **迁移后**: 验证数据、游戏测试、保留备份

📌 **最佳实践**:
1. 务必先备份！
2. 先预览再执行
3. 分批迁移大数据量
4. 选择一种模式长期使用
5. 保留 YML 作为备份

---

*最后更新: 2025-12-12*
*版本: XianCore 1.0.0*
