package com.xiancore.systems.boss.entity;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss刷新点定义
 * 代表一个Boss可以被刷新的点位，包含位置信息、Boss配置、刷新规则
 *
 * 职责:
 * - 存储刷新点的位置信息
 * - 管理Boss配置 (MobType、Tier)
 * - 管理刷新规则 (冷却时间、最大数量)
 * - 追踪刷新状态
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Getter
@Setter
public class BossSpawnPoint {

    // ==================== 基础信息 ====================
    /** 刷新点ID (唯一标识) */
    private String id;

    /** 刷新点名称 (显示用) */
    private String name;

    /** 刷新点描述 */
    private String description;

    // ==================== 位置信息 ====================
    /** 世界名称 */
    private String world;

    /** X坐标 */
    private int x;

    /** Y坐标 */
    private int y;

    /** Z坐标 */
    private int z;

    // ==================== Boss配置 ====================
    /** Boss类型 (MythicMobs ID) - 简化版本 */
    private String mythicMobId;

    /** Boss等级 (1-4) */
    private int tier = 1;

    // ==================== 刷新规则 ====================
    /** 冷却时间 (秒) */
    private long cooldownSeconds = 7200;  // 默认2小时

    /** 最大同时存在的Boss数量 */
    private int maxCount = 1;

    /** 是否使用随机位置 (true=随机, false=固定) */
    private boolean randomLocation = false;

    /** 随机刷新的半径 (格) */
    private int spawnRadius = 100;
    
    /** 随机偏移半径 (0=不偏移，>0=在中心点周围随机) */
    private int randomRadius = 0;
    
    /** 是否自动寻找安全的Y坐标（地面上） */
    private boolean autoFindGround = false;
    
    /** 生成模式 (fixed=固定位置, player-nearby=玩家附近随机, region=区域随机) */
    private String spawnMode = "fixed";
    
    /** 玩家附近最小距离 (仅player-nearby模式) */
    private int minDistance = 50;
    
    /** 玩家附近最大距离 (仅player-nearby模式) */
    private int maxDistance = 200;
    
    /** 区域列表 (仅region模式) - 格式: "world,x1,z1,x2,z2" */
    private List<String> regions = new ArrayList<>();
    
    // ==================== 智能位置评分配置 ====================
    /** 是否启用智能位置评分（包括生物群系、灵气、玩家密集度等） */
    private boolean enableSmartScoring = false;
    
    /** 偏好的生物群系列表（用于生物群系匹配评分） */
    private List<String> preferredBiomes = new ArrayList<>();
    
    /** 生物群系匹配权重 (0.0-1.0) */
    private double biomeWeight = 0.2;
    
    /** 灵气浓度权重 (0.0-1.0) */
    private double spiritualEnergyWeight = 0.3;
    
    /** 玩家密集度权重 (0.0-1.0) */
    private double playerDensityWeight = 0.2;
    
    /** 地形开阔度权重 (0.0-1.0) */
    private double opennessWeight = 0.3;
    
    /** 最小综合评分阈值 (0.0-1.0，低于此值不生成) */
    private double minScore = 0.4;

    // ==================== 状态信息 ====================
    /** 上次成功生成的时间 (毫秒时间戳) */
    private volatile long lastSpawnTime = 0;

    /** 当前活跃的Boss数量 */
    private volatile int currentCount = 0;

    /** 是否启用该刷新点 */
    private volatile boolean enabled = true;

    /** 生成历史 (用于统计) */
    private final List<Long> spawnHistory = Collections.synchronizedList(new ArrayList<>());

    // ==================== 构造函数 ====================

    /**
     * 默认构造函数
     */
    public BossSpawnPoint() {
    }

    /**
     * 基础构造函数
     */
    public BossSpawnPoint(String id, String world, int x, int y, int z) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = id;
    }

    /**
     * 完整构造函数
     */
    public BossSpawnPoint(String id, String world, int x, int y, int z, String mythicMobId) {
        this(id, world, x, y, z);
        this.mythicMobId = mythicMobId;
    }

    /**
     * 完整构造函数 (带Tier)
     */
    public BossSpawnPoint(String id, String world, int x, int y, int z, String mythicMobId, int tier) {
        this(id, world, x, y, z, mythicMobId);
        this.tier = tier;
    }

    /**
     * 获取刷新点ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取Boss等级
     */
    public int getTier() {
        return tier;
    }

    /**
     * 获取最大同时存在的Boss数量
     */
    public int getMaxCount() {
        return maxCount;
    }

    // ==================== 位置相关方法 ====================

    /**
     * 获取刷新点位置
     *
     * @param worldObj 世界对象
     * @return Location对象
     */
    public Location getLocation(World worldObj) {
        return new Location(worldObj, x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * 获取刷新点位置 (使用存储的世界名称)
     * 注: 需要调用者自行处理世界不存在的情况
     *
     * @return Location对象，或null如果世界不存在
     */
    public Location getLocation() {
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }
        return new Location(w, x + 0.5, y + 0.5, z + 0.5);
    }

    /**
     * 设置位置
     */
    public void setLocation(Location location) {
        this.world = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    /**
     * 获取坐标字符串 (格式: world,x,y,z)
     */
    public String getLocationString() {
        return String.format("%s,%d,%d,%d", world, x, y, z);
    }

    // ==================== 刷新规则检查 ====================

    /**
     * 检查是否准备好刷新
     * 检查条件:
     * - 刷新点是否启用
     * - 冷却时间是否已过
     * - 当前Boss数 < 最大数量
     *
     * @return true如果准备好，false否则
     */
    public boolean isReadyToSpawn() {
        // 1. 检查是否启用
        if (!enabled) {
            return false;
        }

        // 2. 检查冷却时间是否已过
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSpawn = currentTime - lastSpawnTime;
        long cooldownMillis = cooldownSeconds * 1000L;

        if (timeSinceLastSpawn < cooldownMillis) {
            return false;
        }

        // 3. 检查当前Boss数量
        if (currentCount >= maxCount) {
            return false;
        }

        return true;
    }

    /**
     * 获取距离下次刷新还需等待的时间 (秒)
     *
     * @return 剩余秒数，如果准备好则为0
     */
    public long getRemainingCooldown() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSpawn = currentTime - lastSpawnTime;
        long cooldownMillis = cooldownSeconds * 1000L;

        if (timeSinceLastSpawn >= cooldownMillis) {
            return 0;
        }

        return (cooldownMillis - timeSinceLastSpawn) / 1000;
    }

    /**
     * 获取冷却进度 (0.0-1.0)
     * 0.0表示刚刚生成，1.0表示冷却完成
     */
    public double getCooldownProgress() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSpawn = currentTime - lastSpawnTime;
        long cooldownMillis = cooldownSeconds * 1000L;

        if (timeSinceLastSpawn >= cooldownMillis) {
            return 1.0;
        }

        return (double) timeSinceLastSpawn / cooldownMillis;
    }

    // ==================== Boss数量管理 ====================

    /**
     * 记录一次生成事件
     * 更新上次生成时间和当前计数
     */
    public void recordSpawn(UUID bossUUID) {
        this.lastSpawnTime = System.currentTimeMillis();
        this.currentCount++;
        this.spawnHistory.add(lastSpawnTime);
    }

    /**
     * 减少当前Boss数量
     * 当Boss被击杀或消失时调用
     */
    public void decrementCount() {
        if (currentCount > 0) {
            currentCount--;
        }
    }

    /**
     * 重置当前Boss数量
     */
    public void resetCount() {
        this.currentCount = 0;
    }

    /**
     * 获取当前活跃Boss数量
     */
    public int getCurrentCount() {
        return currentCount;
    }

    /**
     * 更新当前Boss数量
     *
     * @param count 新的数量
     */
    public void updateCurrentCount(int count) {
        this.currentCount = Math.max(0, count);
    }

    // ==================== Boss类型管理 ====================

    /**
     * 获取Boss类型 (MythicMobs ID)
     */
    public String getMythicMobType() {
        return mythicMobId;
    }

    /**
     * 设置Boss类型 (MythicMobs ID)
     */
    public void setMythicMobType(String mythicMobId) {
        this.mythicMobId = mythicMobId;
    }

    /**
     * 兼容性方法: 获取所有Boss类型 (返回单元素列表)
     */
    public List<String> getMobTypes() {
        List<String> list = new ArrayList<>();
        if (mythicMobId != null && !mythicMobId.isEmpty()) {
            list.add(mythicMobId);
        }
        return list;
    }

    /**
     * 兼容性方法: 添加Boss类型
     */
    public void addMobType(String mobType) {
        this.mythicMobId = mobType;
    }

    /**
     * 兼容性方法: 设置Boss类型列表
     */
    public void setMobTypes(List<String> mobTypes) {
        if (mobTypes != null && !mobTypes.isEmpty()) {
            this.mythicMobId = mobTypes.get(0);
        }
    }

    /**
     * 获取当前应使用的Boss类型
     */
    public String getActiveMobType() {
        return mythicMobId;
    }

    // ==================== 统计信息 ====================

    /**
     * 获取生成历史
     */
    public List<Long> getSpawnHistory() {
        return new ArrayList<>(spawnHistory);
    }

    /**
     * 获取总生成次数
     */
    public int getTotalSpawns() {
        return spawnHistory.size();
    }

    /**
     * 清除生成历史 (用于数据管理)
     */
    public void clearHistory() {
        spawnHistory.clear();
    }

    /**
     * 获取最近N次的生成情况
     */
    public List<Long> getRecentSpawns(int count) {
        int size = spawnHistory.size();
        int startIndex = Math.max(0, size - count);
        return new ArrayList<>(spawnHistory.subList(startIndex, size));
    }

    // ==================== 验证方法 ====================

    /**
     * 验证刷新点的完整性
     *
     * @return true如果配置有效，false否则
     */
    public boolean isValid() {
        // 检查基础信息
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (world == null || world.isEmpty()) {
            return false;
        }

        // 检查Boss配置
        if (mythicMobId == null || mythicMobId.isEmpty()) {
            return false;
        }

        // 检查等级
        if (tier < 1 || tier > 4) {
            return false;
        }

        // 检查冷却时间
        if (cooldownSeconds < 60) {
            return false;  // 至少60秒
        }

        // 检查最大数量
        if (maxCount < 1) {
            return false;
        }

        return true;
    }

    /**
     * 获取验证错误信息
     *
     * @return 错误消息列表，如果有效则为空列表
     */
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();

        if (id == null || id.isEmpty()) {
            errors.add("ID不能为空");
        }
        if (world == null || world.isEmpty()) {
            errors.add("世界名称不能为空");
        }
        if (mythicMobId == null || mythicMobId.isEmpty()) {
            errors.add("MythicMobs ID 不能为空");
        }
        if (tier < 1 || tier > 4) {
            errors.add("Boss等级必须在1-4之间");
        }
        if (cooldownSeconds < 60) {
            errors.add("冷却时间必须至少60秒");
        }
        if (maxCount < 1) {
            errors.add("最大数量必须至少为1");
        }

        return errors;
    }

    // ==================== 工具方法 ====================

    /**
     * 获取简要信息
     */
    public String getSimpleInfo() {
        return String.format("SpawnPoint{id=%s, location=%s, tier=%d, mob=%s, enabled=%s}",
            id, getLocationString(), tier, mythicMobId, enabled);
    }

    /**
     * 获取详细信息
     */
    public String getDetailedInfo() {
        return String.format(
            "BossSpawnPoint{\n" +
            "  ID: %s\n" +
            "  名称: %s\n" +
            "  位置: %s\n" +
            "  Boss类型: %s\n" +
            "  等级: %d\n" +
            "  冷却: %d秒 (剩余: %d秒)\n" +
            "  最大数量: %d\n" +
            "  当前数量: %d\n" +
            "  是否启用: %s\n" +
            "  总生成次数: %d\n" +
            "  随机位置: %s (半径: %d)\n" +
            "}",
            id, name, getLocationString(), mythicMobId, tier,
            cooldownSeconds, getRemainingCooldown(),
            maxCount, currentCount, enabled, getTotalSpawns(),
            randomLocation, spawnRadius
        );
    }

    @Override
    public String toString() {
        return getSimpleInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BossSpawnPoint that = (BossSpawnPoint) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
