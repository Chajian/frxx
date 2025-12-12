# XianCore - 修仙体系插件

[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-17-orange.svg)]()

基于**凡人修仙传**世界观的 Minecraft 服务器插件，提供完整的修仙体系。

## 功能特性

### 核心系统
- **修炼系统** - 境界突破、修为积累、资质评定
- **功法系统** - 功法学习、升级、战斗技能释放
- **炼器系统** - 装备炼制、强化、融合
- **宗门系统** - 创建宗门、成员管理、宗门任务、设施升级
- **天劫系统** - 渡劫考验、劫难奖励
- **奇遇系统** - 随机奇遇触发、奖励发放
- **Boss 系统** - 定时刷新、伤害统计、奖励分配

### 技术特性
- **双存储模式** - 支持 YAML/MySQL 动态切换
- **数据迁移** - 完整的 YAML → MySQL 迁移工具
- **跨服同步** - 基于 MySQL 的多服数据共享
- **PlaceholderAPI** - 支持变量占位符
- **MythicMobs 集成** - Boss 实体管理

## 快速开始

### 环境要求
- Minecraft Server: 1.20.4 (Purpur/Paper)
- Java: 17+
- MySQL: 5.7+ (可选，推荐用于生产环境)

### 依赖插件
| 插件 | 版本 | 必需 | 说明 |
|------|------|------|------|
| MythicMobs | 5.6.1+ | ✅ | Boss 实体管理 |
| Vault | 1.7.3+ | ✅ | 经济系统 |
| PlaceholderAPI | 2.11+ | ⚠️ | 占位符支持（推荐） |
| Residence | 5.1+ | ⚠️ | 宗门领地功能（可选） |

### 安装步骤

1. **下载插件**
   ```bash
   # 将 XianCore-1.0.0-SNAPSHOT.jar 放到 plugins 文件夹
   ```

2. **首次启动**（自动生成配置）
   ```bash
   # 启动服务器，插件会自动创建配置文件
   # 配置文件位置: plugins/XianCore/config.yml
   ```

3. **配置数据库**（可选，推荐生产环境）
   ```yaml
   # config.yml
   database:
     use-mysql: true
     host: localhost
     port: 3306
     database: xiancore
     username: root
     password: your_password
   ```

4. **执行数据库初始化**
   ```bash
   mysql -u root -p xiancore < plugins/XianCore/schema.sql
   ```

5. **重启服务器**
   ```bash
   # 重启服务器以加载配置
   ```

## 架构概览

```
XianCore
├── core/              核心层：配置、数据、事件、工具
├── systems/           系统层：各功能模块（boss, sect, skill...）
├── gui/               表现层：IF 框架 GUI + DisplayService
├── commands/          命令层：命令模式实现
├── integration/       集成层：外部插件集成
└── bridge/            桥接层：经济、世界事件桥接
```

### 核心设计模式
- **Service 层模式** - 业务逻辑与表现层分离
- **DisplayService 模式** - GUI 数据获取与展示分离
- **策略模式** - Boss 位置选择、刷新策略
- **命令模式** - 统一的命令处理框架
- **工厂模式** - MobSpawner 接口解耦

详细架构设计请参阅 [ARCHITECTURE.md](ARCHITECTURE.md)

## 开发约定

### GUI 开发
```java
// ❌ 错误：GUI 中包含业务逻辑
public class ForgeGUI {
    private void handleCraft() {
        // 计算成功率
        double rate = calculateRate(...);
        // 扣除材料
        removeMaterials(...);
    }
}

// ✅ 正确：业务逻辑委托给 Service
public class ForgeGUI {
    private final CraftingService craftingService;

    private void handleCraft() {
        CraftingResult result = craftingService.craft(player, recipe);
        // GUI 只负责展示结果
        displayResult(result);
    }
}
```

### 核心原则
1. **GUI 类禁止包含业务逻辑** - 使用 DisplayService/BusinessService
2. **使用 GUIUtils 创建背景** - 避免重复代码
3. **统一使用 InventoryFramework** - 不再使用原生 Bukkit Inventory
4. **Service 类必须可单元测试** - 不依赖 Bukkit API

## 配置文件

| 文件 | 说明 |
|------|------|
| `config.yml` | 主配置：数据库、系统开关 |
| `cultivation.yml` | 修炼系统配置 |
| `skill.yml` | 功法配置 |
| `skill_shop.yml` | 功法商店配置 |
| `forge.yml` | 炼器配置 |
| `sect.yml` | 宗门配置 |
| `sect_task.yml` | 宗门任务配置 |
| `fate.yml` | 奇遇配置 |
| `messages.yml` | 消息配置 |

## 数据迁移

从 YAML 迁移到 MySQL：
```bash
# 游戏内命令
/xiancore migrate --dry-run      # 预览迁移
/xiancore migrate confirm        # 执行迁移
/xiancore migrate player         # 仅迁移玩家数据
```

详细迁移指南请参阅 [MIGRATION_USAGE_GUIDE.md](MIGRATION_USAGE_GUIDE.md)

## 命令列表

### 玩家命令
| 命令 | 说明 |
|------|------|
| `/cultivation` | 打开修炼界面 |
| `/skill` | 打开功法界面 |
| `/forge` | 打开炼器界面 |
| `/sect` | 打开宗门界面 |
| `/tribulation` | 查看渡劫信息 |
| `/boss` | 查看 Boss 列表 |

### 管理员命令
| 命令 | 说明 | 权限 |
|------|------|------|
| `/xiancore reload` | 重载配置 | `xiancore.admin` |
| `/xiancore migrate` | 数据迁移 | `xiancore.admin` |
| `/xiancore debug` | 调试模式 | `xiancore.admin` |
| `/boss admin` | Boss 管理面板 | `xiancore.boss.admin` |

## 权限节点

| 权限 | 说明 | 默认 |
|------|------|------|
| `xiancore.use` | 使用基础功能 | true |
| `xiancore.admin` | 管理员命令 | op |
| `xiancore.sect.create` | 创建宗门 | true |
| `xiancore.boss.admin` | Boss 管理 | op |

## 性能优化

- ✅ 异步数据保存（每 5 分钟）
- ✅ Boss 伤害统计缓存
- ✅ 宗门数据一致性定时检查
- ✅ 活跃灵气每日衰减（凌晨 4 点）
- ✅ 数据库连接池（HikariCP）

## 开发者文档

- [架构设计](ARCHITECTURE.md) - 分层架构、模块说明
- [重构历史](REFACTORING_LOG.md) - P0-P2 重构记录
- [迁移指南](MIGRATION_USAGE_GUIDE.md) - 数据迁移详解
- [Boss 配置](BOSS_CONFIG_STORAGE_GUIDE.md) - Boss 双模式存储

## 编译构建

```bash
# 编译项目
mvn clean package

# 跳过测试编译
mvn clean package -DskipTests

# 编译并部署到服务器
mvn clean package && cp target/XianCore-1.0.0-SNAPSHOT.jar /path/to/server/plugins/
```

## 项目统计

- **Java 文件**: 300+ 个
- **系统模块**: 7 大核心系统
- **代码行数**: 约 20,000 行（含注释）
- **Service 类**: 15+ 个业务服务
- **GUI 类**: 20+ 个界面

## 重构历程

| 版本 | 重构内容 | 代码变化 |
|------|---------|---------|
| v0.9 | P0 消除重复代码 | -480 行 |
| v0.9.5 | P1 Service 层分离 | +1133 行, -657 行 |
| v1.0 | P1 统一 GUI 框架 | +3321 行, -2198 行 |

详细重构历史请参阅 [REFACTORING_LOG.md](REFACTORING_LOG.md)

## 技术栈

- **核心框架**: Spigot API 1.20.4
- **数据库**: MySQL 5.7+ / YAML
- **GUI 框架**: InventoryFramework 0.10.11
- **依赖注入**: 构造器注入
- **数据访问**: Repository 模式
- **并发控制**: ConcurrentHashMap
- **日志**: SLF4J + Bukkit Logger

## 贡献指南

### 分支策略
- `master` - 稳定版本
- `develop` - 开发分支
- `feature/*` - 功能分支
- `hotfix/*` - 紧急修复

### 提交规范
```
feat: 新功能
fix: Bug 修复
refactor: 重构
docs: 文档
perf: 性能优化
test: 测试
```

示例：
```bash
git commit -m "feat: 新增宗门战争系统"
git commit -m "refactor: P1 GUI类职责分离 - 创建Service层"
```

## 支持与反馈

- **作者**: Olivia Diaz
- **贡献者**: xieyanglin
- **版本**: 1.0.0-SNAPSHOT
- **许可证**: Proprietary

## 更新日志

### v1.0.0 (2025-12-12)
- ✅ 完成 P0-P2 代码重构
- ✅ 统一 GUI 框架为 InventoryFramework
- ✅ 创建 Service 层分离业务逻辑
- ✅ 新增数据迁移功能
- ✅ Boss 配置支持 YAML/MySQL 双模式

---

**⚠️ 注意**: 此插件仍在积极开发中，部分功能可能不稳定。建议在生产环境前充分测试。
