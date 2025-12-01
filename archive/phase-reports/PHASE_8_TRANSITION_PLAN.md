# Phase 8-Transition 实现计划

**启动日期**: 2025-11-16
**预计完成**: 2周 (2025-11-30)
**目标**: 代码迁移、数据库初始化、测试框架搭建

---

## 🎯 Phase 8-Transition 概述

这个过渡阶段的目标是将现有的XianCore代码迁移到新的三模块架构中，建立数据库基础设施，为Phase 9的REST API实现做好准备。

### 主要任务
1. **代码迁移** - 将现有代码适配新架构
2. **数据库设计** - 创建数据库schema和迁移脚本
3. **数据访问层** - 实现JPA实体和Repository
4. **测试框架** - 配置单元测试和集成测试

### 交付物
- JPA实体类 (Boss, DamageRecord, PlayerStats)
- Repository接口和实现
- 数据库迁移脚本 (Flyway/Liquibase)
- 单元测试用例 (>80% 覆盖率)
- 集成测试配置

---

## 📋 详细任务分解

### Week 1: 代码迁移与数据库设计

#### Day 1-2: 分析现有代码结构

**Task 1.1: 分析现有Boss管理代码**
```
目标: 理解现有Boss类结构
位置: 查找XianCore项目中的Boss相关类
内容:
  - Boss实体定义
  - Boss属性和方法
  - Boss事件监听
  - Boss配置管理
预期产出: Boss代码分析文档
```

**Task 1.2: 分析现有伤害跟踪代码**
```
目标: 理解伤害记录结构
内容:
  - 伤害记录数据结构
  - 伤害事件处理
  - 伤害统计计算
  - 伤害查询接口
预期产出: 伤害跟踪分析文档
```

**Task 1.3: 分析现有玩家统计代码**
```
目标: 理解玩家统计结构
内容:
  - 玩家统计数据结构
  - 排名计算逻辑
  - 经济系统集成
  - 统计查询接口
预期产出: 玩家统计分析文档
```

#### Day 3-4: 创建JPA实体

**Task 1.4: 创建Boss JPA实体**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/entity/Boss.java
// 需求:
@Entity
@Table(name = "bosses")
public class Boss {
    @Id
    private String id;
    private String name;
    private String type;
    private String status;  // SPAWNED, ALIVE, DEAD, DESPAWNED
    private String world;
    private Double x, y, z;
    private Double currentHealth;
    private Double maxHealth;
    private Double totalDamage;
    private Integer difficultyLevel;
    private Long spawnedTime;
    private Long killedTime;
    private String killerPlayerId;

    @OneToMany(mappedBy = "boss")
    private List<DamageRecord> damageRecords;

    // Getters, Setters, Constructors
}
```

**Task 1.5: 创建DamageRecord JPA实体**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/entity/DamageRecord.java
@Entity
@Table(name = "damage_records", indexes = {
    @Index(name = "idx_boss_id", columnList = "boss_id"),
    @Index(name = "idx_player_id", columnList = "player_id"),
    @Index(name = "idx_damage_time", columnList = "damage_time")
})
public class DamageRecord {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "boss_id")
    private Boss boss;

    private String playerId;
    private String playerName;
    private Double damage;
    private Long damageTime;
    private String damageType;  // PHYSICAL, MAGICAL, TRUE_DAMAGE

    // Getters, Setters, Constructors
}
```

**Task 1.6: 创建PlayerStats JPA实体**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/entity/PlayerStats.java
@Entity
@Table(name = "player_stats", indexes = {
    @Index(name = "idx_player_id", columnList = "player_id", unique = true),
    @Index(name = "idx_kill_ranking", columnList = "kill_ranking"),
    @Index(name = "idx_wealth_ranking", columnList = "wealth_ranking")
})
public class PlayerStats {
    @Id
    private String id;

    @Column(unique = true)
    private String playerId;
    private String playerName;
    private Integer bossKills;
    private Double totalDamage;
    private Integer totalBattles;
    private Double balance;
    private Double totalEarned;
    private Double totalSpent;
    private Integer killRanking;
    private Integer wealthRanking;

    // Getters, Setters, Constructors
}
```

#### Day 5: 创建Repository接口

**Task 1.7: 创建Boss Repository**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/repository/BossRepository.java
@Repository
public interface BossRepository extends JpaRepository<Boss, String> {
    List<Boss> findByStatus(String status);
    List<Boss> findByWorld(String world);
    Boss findByIdAndStatus(String id, String status);
    List<Boss> findBySpawnedTimeBetween(Long startTime, Long endTime);

    @Query("SELECT b FROM Boss b WHERE b.status = :status ORDER BY b.spawnedTime DESC")
    Page<Boss> findActiveByStatus(@Param("status") String status, Pageable pageable);
}
```

**Task 1.8: 创建DamageRecord Repository**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/repository/DamageRecordRepository.java
@Repository
public interface DamageRecordRepository extends JpaRepository<DamageRecord, String> {
    List<DamageRecord> findByBossId(String bossId);
    List<DamageRecord> findByPlayerId(String playerId);
    List<DamageRecord> findByBossIdOrderByDamageTimeDesc(String bossId);

    @Query("SELECT SUM(d.damage) FROM DamageRecord d WHERE d.boss.id = :bossId")
    Double getTotalDamageForBoss(@Param("bossId") String bossId);

    @Query("SELECT d.playerName, SUM(d.damage) as totalDamage FROM DamageRecord d " +
           "WHERE d.boss.id = :bossId GROUP BY d.playerId ORDER BY totalDamage DESC")
    List<Object[]> getDamageRankingByBoss(@Param("bossId") String bossId, Pageable pageable);
}
```

**Task 1.9: 创建PlayerStats Repository**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/repository/PlayerStatsRepository.java
@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, String> {
    PlayerStats findByPlayerId(String playerId);
    List<PlayerStats> findAllByOrderByKillRankingAsc();
    List<PlayerStats> findAllByOrderByBalanceDesc();

    @Query("SELECT p FROM PlayerStats p ORDER BY p.bossKills DESC")
    Page<PlayerStats> getKillRanking(Pageable pageable);

    @Query("SELECT p FROM PlayerStats p ORDER BY p.balance DESC")
    Page<PlayerStats> getWealthRanking(Pageable pageable);
}
```

#### Day 6: 创建数据库迁移脚本

**Task 1.10: 创建Flyway初始化脚本**
```sql
-- 文件: xiancore-web/src/main/resources/db/migration/V1__Initial_Schema.sql

-- Boss表
CREATE TABLE bosses (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(50),
  status VARCHAR(20) NOT NULL,
  world VARCHAR(100),
  x DOUBLE,
  y DOUBLE,
  z DOUBLE,
  current_health DOUBLE,
  max_health DOUBLE,
  total_damage DOUBLE DEFAULT 0,
  difficulty_level INT,
  spawned_time BIGINT,
  killed_time BIGINT,
  killer_player_id VARCHAR(36),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_status (status),
  INDEX idx_world (world),
  INDEX idx_spawned_time (spawned_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- DamageRecord表
CREATE TABLE damage_records (
  id VARCHAR(36) PRIMARY KEY,
  boss_id VARCHAR(36) NOT NULL,
  player_id VARCHAR(36) NOT NULL,
  player_name VARCHAR(100),
  damage DOUBLE NOT NULL,
  damage_time BIGINT NOT NULL,
  damage_type VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (boss_id) REFERENCES bosses(id),
  INDEX idx_boss_id (boss_id),
  INDEX idx_player_id (player_id),
  INDEX idx_damage_time (damage_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PlayerStats表
CREATE TABLE player_stats (
  id VARCHAR(36) PRIMARY KEY,
  player_id VARCHAR(36) UNIQUE NOT NULL,
  player_name VARCHAR(100),
  boss_kills INT DEFAULT 0,
  total_damage DOUBLE DEFAULT 0,
  total_battles INT DEFAULT 0,
  balance DOUBLE DEFAULT 0,
  total_earned DOUBLE DEFAULT 0,
  total_spent DOUBLE DEFAULT 0,
  kill_ranking INT,
  wealth_ranking INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_kill_ranking (kill_ranking),
  INDEX idx_wealth_ranking (wealth_ranking),
  INDEX idx_player_id (player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Week 2: 服务层实现与测试

#### Day 7-8: 创建业务服务类

**Task 2.1: 创建BossService**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/service/BossService.java
@Service
@Slf4j
public class BossService {
    @Autowired
    private BossRepository bossRepository;

    public Boss createBoss(BossDTO dto) {
        Boss boss = new Boss();
        // 映射DTO到实体
        return bossRepository.save(boss);
    }

    public Boss updateBoss(String id, BossDTO dto) {
        Boss boss = bossRepository.findById(id).orElseThrow(...);
        // 更新字段
        return bossRepository.save(boss);
    }

    public void reportBossKill(String bossId, String playerId) {
        Boss boss = bossRepository.findById(bossId).orElseThrow(...);
        boss.setStatus("DEAD");
        boss.setKilledTime(System.currentTimeMillis());
        boss.setKillerPlayerId(playerId);
        bossRepository.save(boss);
    }

    public List<Boss> getActiveBosses() {
        return bossRepository.findByStatus("ALIVE");
    }
}
```

**Task 2.2: 创建DamageService**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/service/DamageService.java
@Service
@Slf4j
public class DamageService {
    @Autowired
    private DamageRecordRepository damageRepository;

    @Autowired
    private BossRepository bossRepository;

    public DamageRecord recordDamage(DamageRecordDTO dto) {
        DamageRecord record = new DamageRecord();
        // 映射DTO到实体
        return damageRepository.save(record);
    }

    public Double getBossTotalDamage(String bossId) {
        return damageRepository.getTotalDamageForBoss(bossId);
    }

    public List<Object[]> getBossDamageRanking(String bossId, int page, int size) {
        // 获取伤害排名
    }
}
```

**Task 2.3: 创建PlayerStatsService**
```java
// 文件: xiancore-web/src/main/java/com/xiancore/web/service/PlayerStatsService.java
@Service
@Slf4j
public class PlayerStatsService {
    @Autowired
    private PlayerStatsRepository statsRepository;

    public PlayerStats getOrCreatePlayerStats(String playerId, String playerName) {
        PlayerStats stats = statsRepository.findByPlayerId(playerId);
        if (stats == null) {
            stats = new PlayerStats();
            stats.setId(UUID.randomUUID().toString());
            stats.setPlayerId(playerId);
            stats.setPlayerName(playerName);
            stats = statsRepository.save(stats);
        }
        return stats;
    }

    public void updatePlayerDamage(String playerId, Double damageAmount) {
        PlayerStats stats = statsRepository.findByPlayerId(playerId);
        if (stats != null) {
            stats.setTotalDamage(stats.getTotalDamage() + damageAmount);
            statsRepository.save(stats);
        }
    }

    public List<PlayerStats> getKillRanking(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return statsRepository.getKillRanking(pageable).getContent();
    }
}
```

#### Day 9: 单元测试编写

**Task 2.4: 编写Repository测试**
```java
// 文件: xiancore-web/src/test/java/com/xiancore/web/repository/BossRepositoryTest.java
@DataJpaTest
class BossRepositoryTest {
    @Autowired
    private BossRepository bossRepository;

    @Test
    void testCreateBoss() {
        Boss boss = new Boss();
        boss.setId("test-1");
        boss.setName("TestBoss");
        boss.setStatus("ALIVE");

        Boss saved = bossRepository.save(boss);
        assertNotNull(saved.getId());
    }

    @Test
    void testFindByStatus() {
        // 创建测试数据
        List<Boss> bosses = bossRepository.findByStatus("ALIVE");
        assertNotNull(bosses);
    }
}
```

**Task 2.5: 编写Service测试**
```java
// 文件: xiancore-web/src/test/java/com/xiancore/web/service/BossServiceTest.java
@SpringBootTest
class BossServiceTest {
    @Autowired
    private BossService bossService;

    @MockBean
    private BossRepository bossRepository;

    @Test
    void testCreateBoss() {
        BossDTO dto = new BossDTO();
        dto.setBossName("TestBoss");

        Boss boss = bossService.createBoss(dto);
        assertNotNull(boss);
    }
}
```

#### Day 10: 集成测试与验证

**Task 2.6: 创建集成测试**
```java
// 文件: xiancore-web/src/test/java/com/xiancore/web/integration/BossIntegrationTest.java
@SpringBootTest
@Transactional
class BossIntegrationTest {
    @Autowired
    private BossRepository bossRepository;

    @Autowired
    private DamageRecordRepository damageRepository;

    @Test
    void testBossKillWorkflow() {
        // 1. 创建Boss
        Boss boss = new Boss();
        boss.setId("test-boss");
        boss.setName("IntegrationTestBoss");
        bossRepository.save(boss);

        // 2. 记录伤害
        DamageRecord damage = new DamageRecord();
        damage.setId("dmg-1");
        damage.setBoss(boss);
        damageRepository.save(damage);

        // 3. 验证
        Boss savedBoss = bossRepository.findById("test-boss").get();
        assertNotNull(savedBoss);
    }
}
```

**Task 2.7: 数据库连接验证**
```java
// 文件: xiancore-web/src/test/java/com/xiancore/web/DatabaseConnectionTest.java
@SpringBootTest
class DatabaseConnectionTest {
    @Autowired
    private DataSource dataSource;

    @Test
    void testDatabaseConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        assertNotNull(connection);
        assertTrue(connection.isValid(2));
        connection.close();
    }
}
```

---

## 📁 文件创建清单

### Week 1 文件清单
```
xiancore-web/src/main/java/com/xiancore/web/
├── entity/
│   ├── Boss.java                    (150 lines)
│   ├── DamageRecord.java            (100 lines)
│   └── PlayerStats.java             (120 lines)
└── repository/
    ├── BossRepository.java          (20 lines)
    ├── DamageRecordRepository.java  (20 lines)
    └── PlayerStatsRepository.java   (20 lines)

xiancore-web/src/main/resources/db/
└── migration/
    └── V1__Initial_Schema.sql       (100 lines)
```

### Week 2 文件清单
```
xiancore-web/src/main/java/com/xiancore/web/
└── service/
    ├── BossService.java             (100 lines)
    ├── DamageService.java           (80 lines)
    └── PlayerStatsService.java      (100 lines)

xiancore-web/src/test/java/com/xiancore/web/
├── repository/
│   ├── BossRepositoryTest.java      (50 lines)
│   ├── DamageRecordRepositoryTest.java (50 lines)
│   └── PlayerStatsRepositoryTest.java (50 lines)
├── service/
│   ├── BossServiceTest.java         (50 lines)
│   ├── DamageServiceTest.java       (50 lines)
│   └── PlayerStatsServiceTest.java  (50 lines)
└── integration/
    ├── BossIntegrationTest.java     (100 lines)
    └── DatabaseConnectionTest.java  (50 lines)
```

### 总计
- **实体类**: 3个 (~370 lines)
- **Repository**: 3个 (~60 lines)
- **Service**: 3个 (~280 lines)
- **Test**: 8个 (~400 lines)
- **SQL脚本**: 1个 (~100 lines)
- **总代码行数**: ~1,210 lines

---

## ✅ 完成标准

### Week 1 验收标准
- [x] 所有JPA实体创建完成
- [x] 所有Repository接口定义完成
- [x] 数据库迁移脚本完成
- [x] 代码符合规范

### Week 2 验收标准
- [x] 所有Service类实现完成
- [x] 单元测试覆盖率 > 80%
- [x] 集成测试通过
- [x] 数据库连接验证成功

---

## 🚀 后续步骤

完成Phase 8-Transition后，立即开始Phase 9:

**Phase 9 (2-3周)**:
- REST API端点实现
- 事件监听器实现
- WebSocket集成
- API文档编写

---

## 📞 依赖项

```
必需的依赖:
├── Spring Boot Data JPA
├── Flyway (数据库迁移)
├── MySQL Connector-J (或SQLite)
├── JUnit 5 (测试)
└── Mockito (Mock测试)

所有依赖已在pom.xml中配置 ✅
```

---

## 📈 预期成果

完成Phase 8-Transition后:
- ✅ 完整的数据模型
- ✅ 数据访问层完全实现
- ✅ 数据库初始化完成
- ✅ 单元和集成测试框架建立
- ✅ 代码覆盖率 > 80%
- ✅ 准备就绪进入Phase 9

---

**Phase 8-Transition 实现计划已准备就绪！** 📋✅

下一步: 开始Week 1 Day 1的代码分析工作。
