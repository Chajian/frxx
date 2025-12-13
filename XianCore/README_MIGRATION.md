# 📦 XianCore 数据迁移系统

> 将 YML 数据迁移到 MySQL 数据库的完整解决方案

---

## 🎯 快速开始

### 1️⃣ 创建数据库表

```sql
-- 必需！宗门成员表
CREATE TABLE IF NOT EXISTS xian_sect_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sect_id INT NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(50) NOT NULL,
    `rank` VARCHAR(20) NOT NULL DEFAULT 'OUTER_DISCIPLE',  -- 注意反引号！
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

**详细SQL脚本见：** `REQUIRED_TABLES_SETUP.sql`

### 2️⃣ 编译插件

```bash
mvn clean package -DskipTests
```

### 3️⃣ 执行迁移

```bash
# 查看迁移信息
/xiancore migrate --info

# 预览迁移
/xiancore migrate --dry-run

# 真实迁移
/xiancore migrate confirm
```

---

## ✨ 支持的数据类型

| 迁移器 | 数据源 | 目标表 | 状态 |
|--------|--------|--------|------|
| PlayerDataMigrator | `players/*.yml` | `xian_players` | ✅ |
| SectDataMigrator | `sects/*.yml` | `xian_sects` + `xian_sect_members` | ✅ |
| BossDataMigrator | `boss/*.yml` | `xian_boss_records` | ✅ |
| TribulationDataMigrator | `tribulation/*.yml` | `xian_tribulation_records` | ✅ |
| FateDataMigrator | `fate/*.yml` | `xian_fate_records` | ✅ |

---

## 📖 详细文档

- **`MIGRATION_USAGE_GUIDE.md`** - 完整使用指南
- **`MIGRATION_FINAL_SUMMARY.md`** - 配置总结
- **`SQL_FIX_RANK_KEYWORD.md`** - SQL语法说明
- **`REQUIRED_TABLES_SETUP.sql`** - 数据库脚本

---

## 🔧 架构说明

```
命令: /xiancore migrate
    ↓
XianCoreCommand.handleMigrate()
    ↓
MigrationManager.migrateAll()
    ↓
┌─────────────────────────────────────┐
│ PlayerDataMigrator                  │ → xian_players
│ SectDataMigrator                    │ → xian_sects + xian_sect_members
│ BossDataMigrator                    │ → xian_boss_records
│ TribulationDataMigrator             │ → xian_tribulation_records
│ FateDataMigrator                    │ → xian_fate_records
└─────────────────────────────────────┘
```

---

## ⚡ 特性

- ✅ **支持多种数据类型** - 5个完整迁移器
- ✅ **干跑模式** - 预览迁移不写数据库
- ✅ **自动跳过** - 已存在数据不重复迁移
- ✅ **详细报告** - 成功/失败/跳过统计
- ✅ **异步执行** - 不阻塞服务器
- ✅ **错误处理** - 完整的异常捕获和日志

---

## 🚨 重要提示

1. **必须创建 `xian_sect_members` 表**，否则宗门成员数据无法同步
2. **`rank` 是MySQL保留字**，必须用反引号包裹
3. **先用 `--dry-run` 预览**，确认无误后再执行
4. **迁移不会删除YML文件**，作为备份保留

---

## 🆘 常见问题

### Q: 宗门成员数据没有同步？
**A:** 检查是否创建了 `xian_sect_members` 表

### Q: SQL语法错误（rank字段）？
**A:** 确保使用了反引号：`` `rank` VARCHAR(20) ``

### Q: 只想迁移部分数据？
**A:** 编辑 `MigrationManager.java`，注释掉不需要的迁移器

---

## 📞 技术支持

**文档：** 查看 `MIGRATION_USAGE_GUIDE.md`  
**SQL修复：** 查看 `SQL_FIX_RANK_KEYWORD.md`  
**完整配置：** 查看 `MIGRATION_FINAL_SUMMARY.md`

---

**版本：** 1.0.0  
**最后更新：** 2025-12-09
