# 🔧 SQL语法错误修复 - rank 关键字问题

## ❌ 错误信息

```
1064 - You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'rank VARCHAR(20) NOT NULL DEFAULT 'OUTER_DISCIPLE',
    contribution INT DEFAUL' at line 6
```

## 🔍 问题原因

**`rank` 是 MySQL 的保留关键字**，不能直接作为列名使用。

MySQL 保留关键字包括：
- `rank`
- `order`
- `group`
- `table`
- `select`
- 等等...

## ✅ 解决方案

使用**反引号**（`` ` ``）包裹关键字：

### 错误写法 ❌
```sql
rank VARCHAR(20) NOT NULL DEFAULT 'OUTER_DISCIPLE'
```

### 正确写法 ✅
```sql
`rank` VARCHAR(20) NOT NULL DEFAULT 'OUTER_DISCIPLE'
```

## 📝 完整正确的 SQL

```sql
CREATE TABLE IF NOT EXISTS xian_sect_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sect_id INT NOT NULL COMMENT '宗门ID',
    player_uuid VARCHAR(36) NOT NULL COMMENT '玩家UUID',
    player_name VARCHAR(50) NOT NULL COMMENT '玩家名称',
    `rank` VARCHAR(20) NOT NULL DEFAULT 'OUTER_DISCIPLE' COMMENT '职位',  -- ✅ 使用反引号
    contribution INT DEFAULT 0 COMMENT '贡献值',
    weekly_contribution INT DEFAULT 0 COMMENT '本周贡献值',
    joined_at BIGINT NOT NULL COMMENT '加入时间',
    last_active_at BIGINT NOT NULL COMMENT '最后活跃时间',
    tasks_completed INT DEFAULT 0 COMMENT '完成任务数',
    donation_count INT DEFAULT 0 COMMENT '捐献次数',
    UNIQUE KEY uk_sect_player (sect_id, player_uuid),
    INDEX idx_sect_id (sect_id),
    INDEX idx_player_uuid (player_uuid),
    FOREIGN KEY (sect_id) REFERENCES xian_sects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宗门成员表';
```

## 🚀 立即执行

**所有 SQL 文件已修复，请重新执行：**

```sql
-- 连接数据库
mysql -u securityuser -p xiancore

-- 执行修复后的 SQL
source d:/workspace/java/mc/frxx/XianCore/REQUIRED_TABLES_SETUP.sql

-- 或手动执行
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

-- 验证表已创建
DESC xian_sect_members;
SHOW CREATE TABLE xian_sect_members;
```

## ✅ 验证结果

执行成功后应该看到：

```
Query OK, 0 rows affected (0.XX sec)
```

查看表结构：
```
+---------------------+--------------+------+-----+------------------+
| Field               | Type         | Null | Key | Default          |
+---------------------+--------------+------+-----+------------------+
| id                  | int          | NO   | PRI | NULL             |
| sect_id             | int          | NO   | MUL | NULL             |
| player_uuid         | varchar(36)  | NO   |     | NULL             |
| player_name         | varchar(50)  | NO   |     | NULL             |
| rank                | varchar(20)  | NO   |     | OUTER_DISCIPLE   |
| contribution        | int          | YES  |     | 0                |
| ...                 | ...          | ...  | ... | ...              |
+---------------------+--------------+------+-----+------------------+
```

## 📚 已修复的文件

所有相关 SQL 文件已自动修复：

- ✅ `REQUIRED_TABLES_SETUP.sql`
- ✅ `MIGRATION_QUICK_FIX.md`
- ✅ `MIGRATION_FINAL_SUMMARY.md`
- ✅ `database_migration_tables.sql`

**现在可以直接使用这些文件，不会再遇到语法错误！** 🎉

## 💡 MySQL 反引号使用规则

| 情况 | 是否需要反引号 | 示例 |
|------|----------------|------|
| 普通列名 | ❌ 不需要 | `player_name VARCHAR(50)` |
| 保留关键字 | ✅ 需要 | `` `rank` VARCHAR(20)`` |
| 含特殊字符 | ✅ 需要 | `` `player-id` INT`` |
| 含空格 | ✅ 需要 | `` `player name` VARCHAR(50)`` |

**建议：** 尽量避免使用保留关键字作为列名，但如果必须使用，记得加反引号！
