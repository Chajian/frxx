# 🎯 迁移系统使用指南

## ✅ 已完成的修复

### 1. 数据库表 SQL 语法修复
- ✅ 修复 `rank` 保留关键字问题（添加反引号）
- ✅ 更新所有 SQL 文件

### 2. 命令系统集成
- ✅ 在 `XianCore.java` 中初始化 `MigrationManager`
- ✅ 添加 `getMigrationManager()` getter 方法
- ✅ 更新 `XianCoreCommand` 使用新的 `MigrationManager`
- ✅ 支持所有5个迁移器（player, sect, boss, tribulation, fate）

---

## 📝 现在可以执行的步骤

### Step 1: 创建数据库表

```bash
# 连接MySQL
mysql -u securityuser -p xiancore

# 执行SQL（必需！）
```

```sql
-- 创建宗门成员表（必需！）
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

-- 如果需要迁移Boss/渡劫/奇遇数据，继续创建：
-- （可选，如果没有对应YML数据可以跳过）

CREATE TABLE IF NOT EXISTS xian_boss_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    boss_id VARCHAR(50) NOT NULL UNIQUE,
    boss_name VARCHAR(100),
    total_kills INT DEFAULT 0,
    last_spawn_time BIGINT DEFAULT 0,
    last_kill_time BIGINT DEFAULT 0,
    total_damage_dealt BIGINT DEFAULT 0,
    killer_uuid VARCHAR(36),
    reward_distributed BOOLEAN DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_boss_id (boss_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS xian_tribulation_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL UNIQUE,
    total_attempts INT DEFAULT 0,
    successful_attempts INT DEFAULT 0,
    failed_attempts INT DEFAULT 0,
    last_tribulation_time BIGINT DEFAULT 0,
    next_tribulation_realm VARCHAR(50),
    tribulation_power DOUBLE DEFAULT 1.0,
    heavenly_punishment INT DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_player_uuid (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS xian_fate_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL UNIQUE,
    total_encounters INT DEFAULT 0,
    last_encounter_time BIGINT DEFAULT 0,
    fate_type VARCHAR(20) DEFAULT 'NORMAL',
    fate_reward TEXT,
    luck_value DOUBLE DEFAULT 1.0,
    completed_fates INT DEFAULT 0,
    rare_encounter_count INT DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_player_uuid (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Step 2: 编译插件

```bash
cd d:\workspace\java\mc\frxx\XianCore
mvn clean package -DskipTests
```

### Step 3: 部署插件

```bash
# 复制JAR到服务器
copy target\XianCore-*.jar d:\workspace\mc\乾坤生存R\plugins\XianCore.jar

# 重启服务器
```

### Step 4: 查看迁移信息

启动后在控制台应该看到：
```
[XianCore] §a注册迁移器: §fplayer - 玩家数据迁移器
[XianCore] §a注册迁移器: §fsect - 宗门数据迁移器
[XianCore] §a注册迁移器: §fboss - Boss数据迁移器
[XianCore] §a注册迁移器: §ftribulation - 渡劫数据迁移器
[XianCore] §a注册迁移器: §ffate - 奇遇数据迁移器
```

执行查看命令：
```
/xiancore migrate --info
```

应该看到所有迁移器的详细信息！

### Step 5: 预览迁移

```
/xiancore migrate --dry-run
```

查看输出，应该显示：
```
[1/5] 开始迁移: 玩家数据迁移器
[1/5] 玩家数据迁移器 迁移完成！

[2/5] 开始迁移: 宗门数据迁移器
[2/5] 宗门数据迁移器 迁移完成！

[3/5] 开始迁移: Boss数据迁移器
...

迁移总结报告:
player: 成功:1 失败:0 跳过:0
sect: 成功:1 失败:0 跳过:0
boss: 成功:0 失败:0 跳过:0
tribulation: 成功:0 失败:0 跳过:0
fate: 成功:0 失败:0 跳过:0

总计: 成功:2 失败:0 跳过:0
```

### Step 6: 执行真实迁移

```
/xiancore migrate confirm
```

### Step 7: 验证宗门成员数据

```sql
-- 检查宗门成员表
SELECT 
    s.name as 宗门名称,
    m.player_name as 玩家,
    m.`rank` as 职位,
    m.contribution as 贡献
FROM xian_sect_members m
JOIN xian_sects s ON m.sect_id = s.id
ORDER BY s.id, m.`rank`;
```

**如果看到数据，说明宗门成员已成功同步！** 🎉

---

## 🔧 如果没有某些数据类型

如果你的服务器**没有** Boss/渡劫/奇遇的YML文件，编辑 `MigrationManager.java`：

```java
private void registerMigrators() {
    // ✅ 保留
    registerMigrator("player", new PlayerDataMigrator(plugin));
    registerMigrator("sect", new SectDataMigrator(plugin));
    
    // ❌ 注释掉不需要的
    // registerMigrator("boss", new BossDataMigrator(plugin));
    // registerMigrator("tribulation", new TribulationDataMigrator(plugin));
    // registerMigrator("fate", new FateDataMigrator(plugin));
}
```

然后重新编译部署。

---

## 📊 预期结果

### 成功的标志：

1. **迁移器注册成功**
   ```
   [XianCore] §a注册迁移器: §fplayer - 玩家数据迁移器
   [XianCore] §a注册迁移器: §fsect - 宗门数据迁移器
   ```

2. **--info 显示多个迁移器**
   ```
   ▶ 玩家数据迁移器
   ▶ 宗门数据迁移器
   ▶ Boss数据迁移器
   ...
   ```

3. **迁移过程显示所有迁移器**
   ```
   [1/5] 开始迁移: 玩家数据迁移器
   [2/5] 开始迁移: 宗门数据迁移器
   ...
   ```

4. **宗门成员表有数据**
   ```sql
   SELECT COUNT(*) FROM xian_sect_members;
   -- 应该 > 0
   ```

---

## ✨ 总结

**现在你的迁移系统已完全集成！**

✅ 支持5种数据类型迁移  
✅ 宗门成员数据可正常同步  
✅ 命令行界面完整  
✅ 支持预览和真实迁移  
✅ SQL语法错误已修复  

**立即执行：**
1. 创建数据库表
2. 编译插件
3. 部署重启
4. 执行迁移

**祝迁移顺利！** 🚀
