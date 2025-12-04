# 🌟 XianCore-Addon - 属性提升道具系统

> 基于 MMOCore 扩展的修仙服属性提升道具系统

## 📋 项目信息

- **版本**: 1.0.0
- **作者**: YourServer Team
- **开发日期**: 2025-11-02
- **适用版本**: Minecraft 1.16.5+
- **许可证**: MIT License

---

## 🎯 功能特性

✅ **14 种属性提升道具** - 洗髓丹、悟道茶、化灵丹等  
✅ **使用次数限制** - 防止属性无限叠加  
✅ **临时 Buff 系统** - 破境丹提供 30 分钟属性加成  
✅ **SQLite/MySQL 双数据库** - 灵活的存储方案  
✅ **完整的命令系统** - 玩家查询、管理员管理  
✅ **ActionBar Buff 显示** - 实时显示 Buff 剩余时间  
✅ **自动数据备份** - 定时备份防止数据丢失  
✅ **性能优化** - 异步数据库操作、内存缓存

---

## 📦 依赖插件

### 必需

- **Spigot/Paper** 1.16.5+
- **MMOCore** 1.12+
- **MMOItems** 6.9.4+
- **MythicLib** 1.6+

### 可选

- **PlaceholderAPI** 2.11+ (推荐)

---

## 🚀 快速开始

### 1. 编译插件

```bash
# 克隆项目
git clone <repository-url>
cd XianCore-Addon

# Maven 编译
mvn clean package

# 输出: target/XianCore-Addon-1.0.0.jar
```

### 2. 安装插件

```bash
# 复制到服务器
cp target/XianCore-Addon-1.0.0.jar /path/to/server/plugins/

# 确保已安装依赖
plugins/
├── MMOCore-1.12.1.jar
├── MMOItems-6.9.4.jar
├── MythicLib-1.6.2.jar
└── XianCore-Addon-1.0.0.jar
```

### 3. 配置 MMOCore

编辑 `plugins/MMOCore/stats.yml`，添加自定义属性：

```yaml
# 灵根
spiritual-root:
  name: "&b灵根"
  type: BASIC
  max: 1.0
  min: 0.0
  default: 0.5

# 悟性
comprehension:
  name: "&d悟性"
  type: BASIC
  max: 1.0
  min: 0.0
  default: 0.5

# 功法适配度
technique-adaptation:
  name: "&e功法适配度"
  type: BASIC
  max: 1.0
  min: 0.0
  default: 0.5

# 活跃灵气
active-qi:
  name: "&a活跃灵气"
  type: BASIC
  max: 10000
  min: 0
  default: 0
```

### 4. 启动服务器

```bash
# 启动服务器
java -Xms4G -Xmx4G -jar paper-1.16.5.jar nogui

# 查看日志
tail -f logs/latest.log | grep XianCore
```

---

## 📖 使用教程

### 玩家命令

```bash
# 查看道具使用记录
/xiancore items history

# 查看当前Buff
/xiancore buff list

# 查看帮助
/xiancore help
```

### 管理员命令

```bash
# 重载配置
/xiancore reload

# 重置玩家道具使用次数
/xiancore items reset <玩家名> [道具ID]

# 查看玩家属性
/xiancore attribute get <玩家名> <属性名>

# 设置玩家属性
/xiancore attribute set <玩家名> <属性名> <数值>

# 添加Buff
/xiancore buff add <玩家名> <属性名> <数值> <持续时间>

# 备份数据库
/xiancore admin backup

# 优化数据库
/xiancore admin optimize
```

### 道具 ID 列表

| 道具 ID                         | 名称         | 效果                                 | 使用限制                      |
| ------------------------------- | ------------ | ------------------------------------ | ----------------------------- |
| `SPIRITUAL_ROOT_PILL_COMMON`    | 洗髓丹[普通] | 灵根+0.05                            | 终生 3 次                     |
| `SPIRITUAL_ROOT_PILL_RARE`      | 洗髓丹[上品] | 灵根+0.10                            | 终生 2 次                     |
| `SPIRITUAL_ROOT_PILL_EPIC`      | 洗髓丹[极品] | 灵根+0.20 + 活跃灵气+50              | 终生 1 次                     |
| `SPIRITUAL_ROOT_PILL_LEGENDARY` | 洗髓丹[天品] | 灵根+0.30 + 活跃灵气+100 + 功法点+10 | 终生唯一                      |
| `COMPREHENSION_TEA_*`           | 悟道茶系列   | 悟性提升                             | 同上                          |
| `TECHNIQUE_ADAPTATION_PILL_*`   | 化灵丹系列   | 功法适配提升                         | 普通 5 次/上品 3 次/极品 1 次 |
| `HUNYUAN_PILL`                  | 混元丹       | 全属性提升                           | 终生唯一                      |
| `TIANDAO_FRUIT`                 | 天道果       | 随机属性提升                         | 终生 3 次                     |
| `POJING_PILL`                   | 破境丹       | 临时全属性+0.10 (30 分钟)            | 无限制                        |

---

## ⚙️ 配置说明

### config.yml - 主配置

```yaml
# 数据库类型: sqlite 或 mysql
database:
  type: sqlite

# 功能开关
features:
  usage-limit: true # 使用次数限制
  buff-system: true # Buff系统
  audit-log: true # 审计日志

# 自动备份
backup:
  enabled: true
  interval: 3600 # 秒
  keep-days: 7
```

### items.yml - 道具配置

```yaml
items:
  SPIRITUAL_ROOT_PILL_COMMON:
    name: "洗髓丹[普通]"
    max-usage: 3
    cooldown: 300
    broadcast: false
    unique: false
```

### messages.yml - 消息配置

```yaml
prefix: "&6[XianCore] "

item:
  usage-limit-reached: "&c该道具已达使用上限！"
  on-cooldown: "&c该道具冷却中，请等待 %time% 秒！"
```

---

## 🧪 测试指南

### 基础功能测试

```bash
# 1. 给予道具
/mmoitems give CONSUMABLE SPIRITUAL_ROOT_PILL_COMMON <你的名字> 1

# 2. 使用道具（右键）

# 3. 查看属性变化
/mmocore stats

# 4. 查看使用记录
/xiancore items history

# 5. 测试使用次数限制（使用3次后应该无法再用）
```

### Buff 系统测试

```bash
# 1. 给予破境丹
/mmoitems give CONSUMABLE POJING_PILL <你的名字> 1

# 2. 使用破境丹

# 3. 检查Buff显示（ActionBar应该显示剩余时间）

# 4. 查看Buff列表
/xiancore buff list

# 5. 等待30分钟后Buff应该自动消失
```

### 数据库测试

```bash
# 1. 使用道具

# 2. 重启服务器

# 3. 再次查看使用记录（数据应该保留）
/xiancore items history

# 4. 检查数据库文件
ls -lh plugins/XianCore-Addon/database.db
```

---

## 🐛 常见问题

### Q1: 使用道具后属性没有变化？

**A**: 检查以下几点：

1. MMOCore 的 `stats.yml` 中是否配置了自定义属性
2. 服务器日志是否有错误信息
3. 使用 `/xiancore attribute get <玩家名> spiritual_root` 查看实际属性值

### Q2: 数据库连接失败？

**A**:

- SQLite: 检查 `plugins/XianCore-Addon/` 目录权限
- MySQL: 检查 `config.yml` 中的连接信息是否正确

### Q3: 使用次数限制不生效？

**A**:

- 检查 `config.yml` 中 `features.usage-limit` 是否为 `true`
- 使用 `/xiancore items history` 查看当前使用次数
- 检查是否有 `xiancore.bypass.limit` 权限

---

## 📊 数据库结构

```sql
-- 道具使用记录表
CREATE TABLE item_usage (
    uuid VARCHAR(36) NOT NULL,
    item_id VARCHAR(50) NOT NULL,
    usage_count INTEGER DEFAULT 0,
    max_usage INTEGER NOT NULL,
    first_used INTEGER,
    last_used INTEGER,
    PRIMARY KEY (uuid, item_id)
);

-- 临时Buff表
CREATE TABLE active_buffs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(36) NOT NULL,
    buff_type VARCHAR(50) NOT NULL,
    attribute_name VARCHAR(50) NOT NULL,
    bonus_value REAL NOT NULL,
    start_time INTEGER NOT NULL,
    expire_time INTEGER NOT NULL
);

-- 操作日志表
CREATE TABLE operation_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(16),
    operation_type VARCHAR(20) NOT NULL,
    item_id VARCHAR(50),
    old_value REAL,
    new_value REAL,
    details TEXT,
    timestamp INTEGER NOT NULL
);
```

---

## 🛠️ 开发说明

### 项目结构

```
src/main/java/com/yourserver/xiancore/
├── XianCoreAddon.java              # 主插件类
├── command/                        # 命令系统
│   ├── XianCoreCommand.java
│   ├── SubCommand.java
│   └── subcommand/
│       ├── AttributeCommand.java
│       ├── BuffCommand.java
│       ├── ItemsCommand.java
│       ├── ReloadCommand.java
│       └── AdminCommand.java
├── config/                         # 配置管理
│   └── ConfigManager.java
├── listener/                       # 事件监听
│   ├── ItemUseListener.java
│   └── PlayerDataListener.java
├── manager/                        # 管理器
│   ├── DatabaseManager.java
│   ├── AttributeManager.java
│   ├── ItemUsageManager.java
│   └── BuffManager.java
└── model/                          # 数据模型
    ├── ItemUsage.java
    ├── AttributeBuff.java
    └── ItemConfig.java
```

### 扩展开发

**添加新道具**：

1. 在 `items.yml` 中添加配置
2. 在 `ItemUseListener.java` 的 `handleItemEffect()` 中添加处理逻辑
3. 重载配置

**添加新属性**：

1. 在 MMOCore 的 `stats.yml` 中定义属性
2. 在 `ConfigManager.java` 中添加显示名称映射
3. 更新属性上限配置

---

## 📝 更新日志

### v1.0.0 (2025-11-02)

- ✨ 初始版本发布
- ✅ 14 种属性提升道具
- ✅ 使用次数限制系统
- ✅ 临时 Buff 系统
- ✅ SQLite/MySQL 双数据库支持
- ✅ 完整的命令系统
- ✅ 自动数据备份

---

## 📞 技术支持

- **GitHub Issues**: [提交问题](https://github.com/yourserver/XianCore-Addon/issues)
- **Discord**: [加入 Discord 服务器](#)
- **QQ 群**: [您的 QQ 群号]
- **邮箱**: support@yourserver.com

---

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

## 🙏 致谢

感谢以下项目：

- [MMOCore](https://www.spigotmc.org/resources/mmocore.70575/) - RPG 核心系统
- [MMOItems](https://www.spigotmc.org/resources/mmoitems.39267/) - 自定义物品系统
- [MythicLib](https://mythiccraft.io/index.php?resources/mythiclib.2/) - 底层库
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - 数据库连接池

---

**开发完成于** 2025-11-02 🎉
