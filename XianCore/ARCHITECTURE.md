# XianCore 架构设计文档

## 目录
1. [整体架构](#整体架构)
2. [分层设计](#分层设计)
3. [核心模块](#核心模块)
4. [设计模式](#设计模式)
5. [数据流](#数据流)
6. [扩展性设计](#扩展性设计)

---

## 整体架构

XianCore 采用**分层架构** + **模块化设计**，遵循**关注点分离**原则。

```
┌─────────────────────────────────────────────────────────┐
│                     用户交互层                          │
│  Commands (命令处理) + GUI (界面展示)                  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   DisplayService 层                      │
│  数据获取、权限检查、格式化展示                         │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  BusinessService 层                      │
│  核心业务逻辑、计算、状态管理                           │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                      系统层 (Systems)                    │
│  boss, sect, skill, cultivation, forge, tribulation...  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                      数据层 (Core/Data)                  │
│  Repository, Mapper, DAO + 配置管理                     │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   存储层 (MySQL/YAML)                    │
└─────────────────────────────────────────────────────────┘
```

---

## 分层设计

### 1. 用户交互层

#### Commands (命令处理)
- **位置**: `com.xiancore.commands`
- **职责**: 处理玩家命令输入、参数解析、权限校验
- **模式**: 命令模式 + BaseCommand 基类

**示例**:
```java
public class SectCommand extends BaseCommand {
    @Override
    protected void execute(CommandSender sender, String[] args) {
        // 仅处理命令分发，不包含业务逻辑
        if (args[0].equals("create")) {
            handleCreate(player, args);
        }
    }
}
```

#### GUI (图形界面)
- **位置**: `com.xiancore.gui`
- **职责**: 使用 InventoryFramework 展示界面
- **框架**: InventoryFramework (IF)
- **约束**:
  - ❌ 禁止包含业务逻辑
  - ❌ 禁止直接访问数据库
  - ✅ 只负责界面展示和事件响应

**示例**:
```java
public class ForgeGUI {
    private final ForgeDisplayService displayService;

    public void open(Player player) {
        ChestGui gui = new ChestGui(6, "§6§l炼器");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        // 使用 GUIUtils 统一创建背景
        GUIUtils.addGrayBackground(gui, 6);

        // 委托 DisplayService 获取数据
        ForgeInfo info = displayService.getForgeInfo(player);

        // 只负责展示
        displayForgeItems(gui, info);
        gui.show(player);
    }
}
```

---

### 2. DisplayService 层

- **位置**: `com.xiancore.gui.*/`, `com.xiancore.systems/*/`
- **职责**:
  - 数据获取与格式化
  - 权限检查
  - 为 GUI 提供展示用数据对象
- **命名**: `*DisplayService`

**核心 DisplayService**:
| DisplayService | 服务对象 | 职责 |
|----------------|---------|------|
| `BossListDisplayService` | BossListGUI | Boss 列表数据 |
| `StatsDisplayService` | StatsGUI | 玩家统计数据 |
| `ActiveQiShopDisplayService` | ActiveQiShopGUI | 活跃灵气商店 |
| `SkillBindDisplayService` | SkillBindGUI | 技能绑定数据 |
| `ItemSelectionService` | 胚胎/装备选择 | 物品选择数据 |
| `SectDisplayService` | SectGUI | 宗门信息 |
| `SectMemberDisplayService` | SectMemberGUI | 成员列表 |
| `ForgeDisplayService` | ForgeGUI | 炼器界面数据 |
| `SkillDisplayService` | SkillGUI | 功法界面数据 |
| `TribulationDisplayService` | TribulationGUI | 渡劫数据 |

**示例**:
```java
public class ForgeDisplayService {
    private final XianCore plugin;

    // 返回封装的数据对象
    public ForgeInfo getForgeInfo(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());

        return new ForgeInfo(
            data.getForgeLevel(),
            data.getForgeExperience(),
            canCraft(player),
            canEnhance(player)
        );
    }

    // 权限检查
    public boolean canCraft(Player player) {
        return player.hasPermission("xiancore.forge.craft");
    }

    // 内部数据对象
    public static class ForgeInfo {
        private final int level;
        private final int experience;
        private final boolean canCraft;
        private final boolean canEnhance;

        // ... getters
    }
}
```

---

### 3. BusinessService 层

- **位置**: `com.xiancore.systems/*/`
- **职责**:
  - 核心业务逻辑
  - 复杂计算
  - 状态管理
  - 并发控制
- **命名**: `*Service`

**核心 BusinessService**:
| Service | 职责 | 关键方法 |
|---------|------|---------|
| `EnhanceService` | 装备强化 | `enhance()`, `calculateRate()` |
| `CraftingService` | 装备炼制 | `craft()`, `matchRecipe()` |
| `CultivationService` | 修炼系统 | `breakthrough()`, `calculateProgress()` |

**示例**:
```java
public class EnhanceService {
    // 业务常量
    private static final double BASE_SUCCESS_RATE = 0.6;
    private static final int MAX_LEVEL = 15;

    // 并发控制
    private final Map<UUID, Long> enhancingPlayers = new ConcurrentHashMap<>();

    /**
     * 强化装备
     * @return EnhanceResult 强化结果对象
     */
    public EnhanceResult enhance(Player player, ItemStack item, int targetLevel) {
        // 1. 并发检查
        if (isEnhancing(player.getUniqueId())) {
            return EnhanceResult.failure("正在强化中，请稍候");
        }

        // 2. 业务规则检查
        if (targetLevel > MAX_LEVEL) {
            return EnhanceResult.failure("超过最大强化等级");
        }

        // 3. 计算成功率
        double rate = calculateSuccessRate(player, item, targetLevel);

        // 4. 执行强化逻辑
        boolean success = Math.random() < rate;

        // 5. 返回结果对象
        return success
            ? EnhanceResult.success(targetLevel, rate)
            : EnhanceResult.failure("强化失败", rate);
    }

    /**
     * 计算成功率（核心算法）
     */
    private double calculateSuccessRate(Player player, ItemStack item, int level) {
        double rate = BASE_SUCCESS_RATE;

        // 活跃灵气加成
        if (hasActiveQiBoost(player)) {
            rate += 0.03;
        }

        // 等级惩罚
        rate -= (level - 1) * 0.05;

        return Math.max(0.1, Math.min(0.95, rate));
    }

    /**
     * 结果对象（封装返回值）
     */
    public static class EnhanceResult {
        private final boolean success;
        private final String message;
        private final int level;
        private final double rate;

        public static EnhanceResult success(int level, double rate) {
            return new EnhanceResult(true, "强化成功", level, rate);
        }

        public static EnhanceResult failure(String reason) {
            return new EnhanceResult(false, reason, 0, 0);
        }

        // ... getters
    }
}
```

---

### 4. 系统层 (Systems)

每个系统都是独立的功能模块，包含完整的业务逻辑。

#### Boss 系统
**路径**: `com.xiancore.systems.boss`

```
boss/
├── BossRefreshManager.java         # Boss 刷新管理器
├── announcement/                    # 广播系统
│   └── BossAnnouncement.java
├── config/                          # 配置加载
│   └── BossConfigLoader.java
├── damage/                          # 伤害统计
│   └── DamageStatisticsManager.java
├── entity/                          # 实体管理
│   └── BossEntity.java
├── event/                           # 自定义事件
│   ├── BossSpawnEvent.java
│   └── BossDeathEvent.java
├── gui/                             # GUI 界面
│   ├── BossGUI.java
│   └── BossAdminGUI.java
├── listener/                        # 事件监听
│   ├── BossEventListener.java
│   └── BossCombatListener.java
├── location/                        # 位置策略
│   ├── LocationStrategy.java       # 策略接口
│   ├── BaritoneStrategy.java       # Baritone 实现
│   └── VanillaStrategy.java        # 原版实现
├── reward/                          # 奖励系统
│   └── RewardDistributor.java
└── spawner/                         # 生成器
    ├── MobSpawner.java              # 生成器接口
    └── MythicMobsSpawner.java       # MM 实现
```

**核心设计**:
- ✅ 策略模式 - 位置选择策略
- ✅ 工厂模式 - MobSpawner 接口解耦 MythicMobs
- ✅ 事件驱动 - 自定义 Boss 事件
- ✅ 双模式存储 - YAML/MySQL 动态切换

#### Sect (宗门) 系统
**路径**: `com.xiancore.systems.sect`

```
sect/
├── SectSystem.java                  # 宗门系统主类
├── Sect.java                        # 宗门实体
├── SectMember.java                  # 成员实体
├── SectRank.java                    # 职位枚举
├── achievement/                     # 成就系统
├── facilities/                      # 设施管理
│   ├── SectFacility.java
│   └── SectFacilityGUI.java
├── shop/                            # 宗门商店
│   └── SectShopGUI.java
├── task/                            # 任务系统
│   ├── SectTask.java
│   ├── SectTaskManager.java
│   ├── SectTaskGUI.java
│   └── TaskRefreshScheduler.java   # 定时刷新
└── warehouse/                       # 仓库系统
    └── SectWarehouseGUI.java
```

#### Skill (功法) 系统
**路径**: `com.xiancore.systems.skill`

```
skill/
├── SkillSystem.java                 # 功法系统主类
├── Skill.java                       # 功法实体
├── effects/                         # 技能效果
│   ├── DamageEffect.java
│   └── HealEffect.java
├── events/                          # 技能事件
│   └── SkillCastEvent.java
├── items/                           # 功法物品
│   └── SkillBook.java
├── listeners/                       # 监听器
│   ├── SkillBookListener.java
│   └── ElementalAttributeListener.java
└── shop/                            # 功法商店
    ├── SkillShopConfig.java
    └── SkillShopGUI.java
```

---

### 5. 核心层 (Core)

#### 配置管理
**路径**: `com.xiancore.core.config`

```java
public class ConfigManager {
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public void loadConfigs() {
        loadConfig("config.yml");
        loadConfig("cultivation.yml");
        loadConfig("skill.yml");
        // ... 其他配置
    }

    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }
}
```

#### 数据层
**路径**: `com.xiancore.core.data`

```
data/
├── DataManager.java                 # 数据管理器
├── PlayerData.java                  # 玩家数据实体
├── mapper/                          # SQL 映射
│   ├── PlayerDataMapper.java
│   └── SectMapper.java
├── migrate/                         # 数据迁移
│   ├── MigrationManager.java
│   └── migrators/
│       ├── PlayerDataMigrator.java
│       └── BossConfigMigrator.java
└── repository/                      # 仓储模式
    ├── PlayerRepository.java
    └── SectRepository.java
```

**Repository 模式示例**:
```java
public class PlayerRepository {
    private final DataSource dataSource;

    public PlayerData findByUUID(UUID uuid) {
        // YAML 模式
        if (!useMySql) {
            return loadFromYaml(uuid);
        }

        // MySQL 模式
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM xian_player_data WHERE uuid = ?";
            // ... JDBC 查询
            return mapToPlayerData(resultSet);
        }
    }

    public void save(PlayerData data) {
        if (!useMySql) {
            saveToYaml(data);
        } else {
            saveToMysql(data);
        }
    }
}
```

#### 工具类
**路径**: `com.xiancore.core.utils`

| 工具类 | 职责 |
|--------|------|
| `GUIUtils` | GUI 背景统一创建 |
| `ColorUtils` | 颜色处理 |
| `ItemBuilder` | 物品构建器（流式API） |

**GUIUtils 示例**:
```java
public class GUIUtils {
    /**
     * 添加灰色玻璃背景
     * 消除了 20+ 个 GUI 中的重复代码
     */
    public static void addGrayBackground(ChestGui gui, int rows) {
        StaticPane background = new StaticPane(0, 0, 9, rows);
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 9 * rows; i++) {
            background.addItem(new GuiItem(glass), i % 9, i / 9);
        }

        gui.addPane(background);
    }
}
```

---

## 设计模式

### 1. Service 层模式
**意图**: 分离业务逻辑与表现层

**应用场景**: 所有 GUI、Command

**收益**:
- ✅ GUI 代码减少 30-40%
- ✅ Service 可独立单元测试
- ✅ 业务逻辑复用
- ✅ 关注点分离

### 2. 策略模式 (Strategy Pattern)
**意图**: 定义一系列算法，让它们可以互换

**应用**: Boss 位置选择
```java
public interface LocationStrategy {
    Location selectLocation(Boss boss);
}

public class BaritoneStrategy implements LocationStrategy {
    @Override
    public Location selectLocation(Boss boss) {
        // 使用 Baritone 路径查找
    }
}

public class VanillaStrategy implements LocationStrategy {
    @Override
    public Location selectLocation(Boss boss) {
        // 使用原版随机位置
    }
}
```

### 3. 工厂模式 (Factory Pattern)
**意图**: 解耦对象创建

**应用**: MobSpawner 接口
```java
public interface MobSpawner {
    void spawn(String mobType, Location location);
}

public class MythicMobsSpawner implements MobSpawner {
    @Override
    public void spawn(String mobType, Location location) {
        MythicMobs.inst().getAPIHelper().spawnMythicMob(mobType, location);
    }
}
```

### 4. 命令模式 (Command Pattern)
**意图**: 将请求封装为对象

**应用**: 命令系统
```java
public abstract class BaseCommand implements CommandExecutor {
    protected abstract void execute(CommandSender sender, String[] args);
    protected abstract void showHelp(CommandSender sender);
}
```

### 5. Repository 模式
**意图**: 抽象数据访问层

**应用**: 数据存储
```java
public interface PlayerRepository {
    PlayerData findByUUID(UUID uuid);
    void save(PlayerData data);
    List<PlayerData> findAll();
}
```

---

## 数据流

### 玩家打开 GUI 的完整流程

```
1. 玩家执行命令
   /sect

2. SectCommand 接收
   └─> execute(sender, args)

3. 调用 GUI
   └─> SectGUI.open(player, plugin)

4. GUI 委托 DisplayService
   └─> SectDisplayService.getSectDisplayInfo(player, data)

5. DisplayService 调用系统层
   └─> SectSystem.getPlayerSect(uuid)

6. 系统层访问数据层
   └─> DataManager.loadPlayerData(uuid)

7. 数据层查询存储
   └─> PlayerRepository.findByUUID(uuid)
       ├─> YAML 模式: 读取 playerdata/{uuid}.yml
       └─> MySQL 模式: SELECT FROM xian_player_data

8. 数据返回
   PlayerData → SectSystem → DisplayService → GUI

9. GUI 展示
   ChestGui.show(player)
```

### 业务操作的完整流程（以炼制装备为例）

```
1. 玩家点击 GUI 按钮
   ForgeGUI → craftButton.onClick()

2. GUI 委托 Service
   └─> CraftingService.craft(player, recipe)

3. Service 执行业务逻辑
   ├─> 检查材料: hasMaterials()
   ├─> 计算成功率: calculateRate()
   ├─> 扣除材料: removeMaterials()
   ├─> 执行炼制: performCraft()
   └─> 返回结果: CraftingResult

4. GUI 展示结果
   └─> displayResult(result)
       ├─> 成功: 给予物品 + 成功音效
       └─> 失败: 显示失败消息
```

---

## 扩展性设计

### 1. 新增系统模块

**步骤**:
1. 在 `com.xiancore.systems` 下创建包
2. 创建 `*System.java` 主类
3. 在 `XianCore.java` 中注册

```java
// XianCore.java
private void initializeGameSystems() {
    // ... 现有系统

    // 新系统
    newSystem = new NewSystem(this);
    newSystem.initialize();
}
```

### 2. 新增 GUI

**约束**:
- ✅ 必须使用 InventoryFramework
- ✅ 必须创建对应的 DisplayService
- ✅ 必须使用 GUIUtils 创建背景

```java
public class NewGUI {
    private final XianCore plugin;
    private final NewDisplayService displayService;

    public void open(Player player) {
        ChestGui gui = new ChestGui(6, "§a§l新功能");
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        GUIUtils.addGrayBackground(gui, 6);

        // 委托 DisplayService
        NewInfo info = displayService.getInfo(player);
        displayContent(gui, info);

        gui.show(player);
    }
}
```

### 3. 新增外部集成

**步骤**:
1. 在 `com.xiancore.integration` 下创建包
2. 实现集成类
3. 在 `initializeBridges()` 中注册

```java
// XianCore.java
private void initializeBridges() {
    // ... 现有集成

    if (Bukkit.getPluginManager().getPlugin("NewPlugin") != null) {
        newIntegration = new NewIntegration(this);
        newIntegration.initialize();
    }
}
```

---

## 性能考量

### 1. 异步操作
- ✅ 数据保存: 每 5 分钟异步保存
- ✅ 数据库查询: 使用连接池（HikariCP）
- ✅ Boss 伤害统计: 缓存计算结果

### 2. 缓存策略
```java
// Boss 伤害统计缓存
private final Map<UUID, DamageRecord> damageCache = new ConcurrentHashMap<>();
```

### 3. 定时任务
- ✅ 活跃灵气衰减: 每天凌晨 4 点
- ✅ 宗门数据一致性: 每小时检查
- ✅ 数据自动保存: 每 5 分钟

---

## 架构演进

### 重构前 (v0.8)
```
GUI
└─> 包含所有业务逻辑、数据访问、UI 渲染
    (单个文件 500-800 行)
```

### 重构后 (v1.0)
```
GUI (表现层，200-350 行)
└─> DisplayService (数据层，150-250 行)
    └─> BusinessService (业务层，300-400 行)
        └─> System (系统层)
            └─> DataManager (数据层)
```

**改进**:
- ✅ 代码行数减少 30-40%
- ✅ 职责清晰，易于维护
- ✅ Service 可独立测试
- ✅ 代码复用率提升

---

## 总结

XianCore 的架构设计遵循以下核心原则：

1. **分层清晰** - GUI → DisplayService → BusinessService → System → Data
2. **关注点分离** - 每层只负责自己的职责
3. **设计模式应用** - 合理使用设计模式解决问题
4. **可扩展性** - 易于新增系统和功能
5. **可测试性** - Service 层可独立测试
6. **性能优化** - 异步、缓存、定时任务

通过系统化的重构，项目代码质量得到显著提升，为后续开发奠定了坚实基础。
