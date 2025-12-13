# 🎯 数据迁移系统 - 最终配置总结

## 📌 当前状况

### 现有数据库表
```
✅ xian_players
✅ xian_player_equipment
✅ xian_player_skills
✅ xian_sects
```

### 问题诊断
❌ **宗门成员数据无法同步** - 缺少 `xian_sect_members` 表

---

## 🔧 解决方案

### 方案 1: 最小配置（推荐）⭐

**适用场景：** 只需要迁移玩家和宗门数据

#### Step 1: 创建必需的表

```sql
-- 宗门成员表（必需！）
CREATE TABLE IF NOT EXISTS xian_sect_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sect_id INT NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(50) NOT NULL,
    `rank` VARCHAR(20) NOT NULL DEFAULT 'OUTER_DISCIPLE',
    contribution INT DEFAULT 0,
    weekly_contribution INT DEFAULT 0,
    joined_at BIGINT NOT NULL,
    last_active_at BIGINT NOT NULL,
    tasks_completed INT DEFAULT 0,
    donation_count INT DEFAULT 0,
    UNIQUE KEY uk_sect_player (sect_id, player_uuid),
    INDEX idx_sect_id (sect_id),
    INDEX idx_player_uuid (player_uuid),
    FOREIGN KEY (sect_id) REFERENCES xian_sects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### Step 2: 调整 MigrationManager

编辑 `MigrationManager.java`，注释掉不需要的迁移器：

```java
private void registerMigrators() {
    // ✅ 保留
    registerMigrator("player", new PlayerDataMigrator(plugin));
    registerMigrator("sect", new SectDataMigrator(plugin));
    
    // ❌ 注释掉（如果没有对应数据）
    // registerMigrator("boss", new BossDataMigrator(plugin));
    // registerMigrator("tribulation", new TribulationDataMigrator(plugin));
    // registerMigrator("fate", new FateDataMigrator(plugin));
}
```

**或者直接使用：** `MigrationManager_MINIMAL.java.example`
```bash
# 备份原文件
cp MigrationManager.java MigrationManager.java.bak

# 使用最小配置
cp MigrationManager_MINIMAL.java.example MigrationManager.java
```

---

### 方案 2: 完整配置

**适用场景：** 需要迁移所有类型数据（渡劫、奇遇、Boss）

#### Step 1: 创建所有表

```bash
# 连接MySQL
mysql -u securityuser -p xiancore

# 执行SQL脚本
source REQUIRED_TABLES_SETUP.sql
```

这会创建：
- `xian_sect_members`
- `xian_tribulation_records`
- `xian_fate_records`
- `xian_boss_records`

#### Step 2: 保持 MigrationManager 配置

保持当前配置不变，所有迁移器都启用。

---

## 📁 相关文件说明

| 文件 | 用途 |
|------|------|
| `REQUIRED_TABLES_SETUP.sql` | 创建所有必需表的SQL脚本 |
| `MIGRATION_QUICK_FIX.md` | 快速修复指南（详细） |
| `MigrationManager_MINIMAL.java.example` | 最小配置示例 |
| `MIGRATION_EXTENSIONS_COMPLETE.md` | 扩展迁移器完整文档 |

---

## ✅ 推荐执行流程

### 1. 创建数据库表

```bash
cd d:/workspace/java/mc/frxx/XianCore

# 登录MySQL
mysql -u securityuser -p xiancore

# 至少执行这个创建宗门成员表
CREATE TABLE IF NOT EXISTS xian_sect_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sect_id INT NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(50) NOT NULL,
    `rank` VARCHAR(20) NOT NULL DEFAULT 'OUTER_DISCIPLE',
    contribution INT DEFAULT 0,
    weekly_contribution INT DEFAULT 0,
    joined_at BIGINT NOT NULL,
    last_active_at BIGINT NOT NULL,
    tasks_completed INT DEFAULT 0,
    donation_count INT DEFAULT 0,
    UNIQUE KEY uk_sect_player (sect_id, player_uuid),
    INDEX idx_sect_id (sect_id),
    INDEX idx_player_uuid (player_uuid),
    FOREIGN KEY (sect_id) REFERENCES xian_sects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# 验证
SHOW TABLES LIKE 'xian_%';
```

### 2. 调整迁移器配置

选择**方案1**或**方案2**

### 3. 编译插件

```bash
mvn clean package -DskipTests
```

### 4. 部署测试

```bash
# 复制JAR到服务器
cp target/XianCore-*.jar /path/to/server/plugins/

# 重启服务器
```

### 5. 执行迁移

```bash
# 在服务器控制台或游戏内执行

# 1. 先查看迁移信息
/xiancore migrate --info

# 2. 预览迁移（不实际写入）
/xiancore migrate --dry-run

# 3. 确认后执行真实迁移
/xiancore migrate confirm
```

### 6. 验证结果

```sql
-- 检查宗门成员数据
SELECT s.name, COUNT(m.id) as member_count
FROM xian_sects s
LEFT JOIN xian_sect_members m ON s.id = m.sect_id
GROUP BY s.id, s.name;

-- 应该能看到每个宗门的成员数量
```

---

## 🎯 当前迁移器状态

| 迁移器 | 状态 | 依赖表 | 建议 |
|--------|------|--------|------|
| PlayerDataMigrator | ✅ 可用 | xian_players, xian_player_equipment, xian_player_skills | 保留 |
| SectDataMigrator | ⚠️ 需要表 | xian_sects, xian_sect_members | **必须创建xian_sect_members** |
| BossDataMigrator | ⚠️ 需要表 | xian_boss_records | 按需启用 |
| TribulationDataMigrator | ⚠️ 需要表 | xian_tribulation_records | 按需启用 |
| FateDataMigrator | ⚠️ 需要表 | xian_fate_records | 按需启用 |

---

## ❗ 关键注意事项

1. **xian_sect_members 表必须创建**，否则宗门成员数据无法保存
2. 不需要的迁移器建议注释掉，避免错误
3. 先使用 `--dry-run` 预览，确认无误后再执行
4. 迁移不会删除原YML文件，可作为备份

---

## 📞 问题排查

### 宗门成员数据依然为空？

1. 检查 `xian_sect_members` 表是否存在
   ```sql
   DESC xian_sect_members;
   ```

2. 检查迁移日志
   ```bash
   # 查看服务器日志中是否有SQL错误
   ```

3. 手动验证YML数据
   ```bash
   # 检查 plugins/XianCore/sects/ 目录
   # 确认YML文件中有members节点
   ```

### 迁移器报错？

1. 查看完整错误日志
2. 确认所有依赖表已创建
3. 检查数据库连接配置

---

## ✨ 总结

**立即执行（必需）：**
1. ✅ 创建 `xian_sect_members` 表
2. ✅ 调整 `MigrationManager` 注册
3. ✅ 重新编译并部署

**可选执行：**
- 根据需求创建其他表（渡劫、奇遇、Boss）
- 启用对应的迁移器

**执行后你将获得：**
- ✅ 玩家数据完整迁移
- ✅ 宗门信息完整迁移
- ✅ **宗门成员数据正常同步** 🎉

---

**祝迁移顺利！** 如有问题请查看详细文档或日志排查。
