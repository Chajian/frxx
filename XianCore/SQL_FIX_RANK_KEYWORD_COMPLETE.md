# MySQL关键字冲突修复 - rank字段

## 问题描述

在执行宗门数据迁移时，出现SQL语法错误：

```
You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'rank, contribution,
    weekly_contribution, joined_at, last_active_at,
    task' at line 2
```

**原因**：`rank` 是MySQL保留关键字，在SQL语句中直接使用会导致语法错误。

---

## 修复方案

### 修复位置

**文件**：`DataManager.java`

**修复内容**：在所有使用 `rank` 列名的INSERT语句中，用反引号包围该字段。

### 修复详情

#### 1. saveSectMembers() 方法（第1297-1303行）

**修复前**：
```java
String insertSql = """
        INSERT INTO xian_sect_members (
            sect_id, player_uuid, player_name, rank, contribution,
            weekly_contribution, joined_at, last_active_at,
            tasks_completed, donation_count
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
```

**修复后**：
```java
String insertSql = """
        INSERT INTO xian_sect_members (
            sect_id, player_uuid, player_name, `rank`, contribution,
            weekly_contribution, joined_at, last_active_at,
            tasks_completed, donation_count
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
```

#### 2. saveSectMembersWithConnection() 方法（第2007-2013行）

**修复前**：
```java
String insertSql = """
        INSERT INTO xian_sect_members (
            sect_id, player_uuid, player_name, rank, contribution,
            weekly_contribution, joined_at, last_active_at,
            tasks_completed, donation_count
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
```

**修复后**：
```java
String insertSql = """
        INSERT INTO xian_sect_members (
            sect_id, player_uuid, player_name, `rank`, contribution,
            weekly_contribution, joined_at, last_active_at,
            tasks_completed, donation_count
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
```

---

## MySQL保留关键字处理

### 其他已正确处理的关键字

在建表语句中，`rank` 字段已经正确使用了反引号（第224行）：

```java
`rank` VARCHAR(32) DEFAULT 'OUTER_DISCIPLE',
```

### SELECT语句无需修复

在 `loadSectMembers()` 方法（第1525行）中：

```java
String sql = "SELECT * FROM xian_sect_members WHERE sect_id = ?";
```

使用 `SELECT *` 自动包含所有列，无需显式指定列名，因此不受影响。

在读取ResultSet时（第1538行）：

```java
member.setRank(com.xiancore.systems.sect.SectRank.valueOf(rs.getString("rank")));
```

`ResultSet.getString("rank")` 中的列名不需要反引号，因为这是Java字符串参数，不是SQL语句。

---

## 编译结果

```
[INFO] BUILD SUCCESS
[INFO] Total time:  6.473 s
[INFO] Finished at: 2025-12-10T21:14:17+08:00

JAR文件: target/XianCore-1.0.0-SNAPSHOT.jar
编译文件数: 247个Java源文件
```

---

## 验证测试

### 预期结果

执行迁移命令后：

```bash
/xiancore migrate confirm
```

**修复前**：
```
sect: 成功:2 失败:6 跳过:1
[ERROR] SQL语法错误: near 'rank, contribution, ...
```

**修复后**：
```
sect: 成功:N 失败:0 跳过:M
✓ 宗门成员数据保存成功
```

### 需要重新迁移

由于之前的迁移失败，需要：

1. **重启服务器**：加载新的JAR文件
2. **重新执行迁移**：`/xiancore migrate confirm`

---

## 技术说明

### MySQL保留关键字

MySQL有一系列保留关键字，在用作标识符（表名、列名）时必须用反引号包围：

| 常见保留字 | 说明 |
|-----------|------|
| `rank` | 排名/等级 |
| `order` | 排序 |
| `group` | 分组 |
| `key` | 键 |
| `index` | 索引 |
| `table` | 表 |
| `select` | 查询 |
| `desc` | 降序 |

### 反引号使用规则

1. **建表语句**：✅ 需要反引号
   ```sql
   CREATE TABLE foo (`rank` INT)
   ```

2. **INSERT/UPDATE语句**：✅ 需要反引号
   ```sql
   INSERT INTO foo (`rank`) VALUES (?)
   UPDATE foo SET `rank` = ?
   ```

3. **SELECT语句**：✅ 需要反引号（如果显式指定列名）
   ```sql
   SELECT `rank` FROM foo
   ```

4. **SELECT * 语句**：❌ 不需要（自动包含所有列）
   ```sql
   SELECT * FROM foo  -- rank列会自动包含
   ```

5. **ResultSet读取**：❌ 不需要（Java字符串参数）
   ```java
   rs.getString("rank")  // 不需要反引号
   ```

---

## 相关文件

- **修复文件**：`DataManager.java`（第1299行、第2009行）
- **建表语句**：`DataManager.java`（第224行，已正确使用反引号）
- **编译输出**：`target/XianCore-1.0.0-SNAPSHOT.jar`

---

## 总结

| 项目 | 状态 |
|------|------|
| 问题识别 | ✅ 完成 |
| 代码修复 | ✅ 完成（2处） |
| 编译测试 | ✅ 通过 |
| JAR打包 | ✅ 成功 |
| 部署就绪 | ✅ 是 |

**修复时间**：2025-12-10
**影响范围**：宗门成员数据迁移
**兼容性**：向后兼容，不影响现有数据

---

## 后续步骤

1. ✅ 替换服务器JAR文件
2. ✅ 重启服务器
3. ⏳ 执行迁移命令
4. ⏳ 验证宗门数据迁移成功

**注意**：Boss配置迁移已在之前修复并测试通过！
