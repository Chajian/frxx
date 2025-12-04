package com.xiancore.core.data;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家数据类
 * 存储玩家的修仙相关数据
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class PlayerData {

    // 基础信息
    private UUID uuid;
    private String name;

    // 修炼数据
    private String realm = "炼气期";              // 境界
    private int realmStage = 1;                    // 境界阶段（初、中、后期）
    private long qi = 0;                           // 灵力值
    private double spiritualRoot = 0.5;            // 灵根数值 (L) - 0.0-1.0
    private SpiritualRootType spiritualRootType = null;  // 灵根类型（五行属性）
    private double comprehension = 0.5;            // 悟性 (G)
    private double techniqueAdaptation = 0.6;      // 功法适配度 (P)

    // 资源数据
    private long spiritStones = 0;                 // 灵石
    private int contributionPoints = 0;            // 宗门贡献点
    private int skillPoints = 0;                   // 功法点
    private int playerLevel = 1;                   // 玩家等级

    // 装备数据
    private Map<String, String> equipment = new HashMap<>();  // 装备槽位 -> 装备UUID

    // 宗门数据
    private Integer sectId = null;                 // 宗门ID
    private String sectRank = "member";            // 宗门职位

    // 功法数据
    private Map<String, Integer> learnedSkills = new HashMap<>();  // 功法ID -> 等级
    private Map<Integer, String> skillBindings = new HashMap<>();  // 槽位(1-9) -> 功法ID（快捷键绑定）

    // 统计数据
    private long lastLogin;                        // 最后登录时间
    private long createdAt;                        // 创建时间
    private long updatedAt;                        // 更新时间
    private int breakthroughAttempts = 0;          // 突破尝试次数
    private int successfulBreakthroughs = 0;       // 成功突破次数

    // 天劫数据
    private int tribulationCount = 0;              // 渡劫总次数
    private int successfulTribulations = 0;        // 成功渡劫次数

    // 奇遇数据
    private long activeQi = 0;                     // 活跃灵气值（用于奇遇触发）
    private long lastFateTime = 0;                 // 上次触发奇遇时间
    private int fateCount = 0;                     // 触发奇遇总数

    // 修炼状态
    private boolean cultivating = false;           // 是否正在打坐修炼（被动增益）

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
        this.lastLogin = createdAt;
    }

    /**
     * 获取境界完整名称
     * 例如：炼气期·初期
     */
    public String getFullRealmName() {
        String stage = switch (realmStage) {
            case 1 -> "初期";
            case 2 -> "中期";
            case 3 -> "后期";
            default -> "未知";
        };
        return realm + "·" + stage;
    }

    /**
     * 增加灵力
     */
    public void addQi(long amount) {
        this.qi += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 减少灵力
     */
    public boolean removeQi(long amount) {
        if (this.qi >= amount) {
            this.qi -= amount;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * 增加灵石
     */
    public void addSpiritStones(long amount) {
        this.spiritStones += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 减少灵石
     */
    public boolean removeSpiritStones(long amount) {
        if (this.spiritStones >= amount) {
            this.spiritStones -= amount;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * 增加活跃灵气值
     */
    public void addActiveQi(long amount) {
        this.activeQi += amount;
        // 每日最大值100点
        if (this.activeQi > 100) {
            this.activeQi = 100;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 减少活跃灵气值
     */
    public boolean removeActiveQi(long amount) {
        if (this.activeQi >= amount) {
            this.activeQi -= amount;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * 增加功法点
     */
    public void addSkillPoints(int amount) {
        this.skillPoints += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 减少功法点
     */
    public boolean removeSkillPoints(int amount) {
        if (this.skillPoints >= amount) {
            this.skillPoints -= amount;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * 增加宗门贡献值
     */
    public void addContribution(int amount) {
        this.contributionPoints += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 减少宗门贡献值
     */
    public boolean removeContribution(int amount) {
        if (this.contributionPoints >= amount) {
            this.contributionPoints -= amount;
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * 增加玩家等级
     */
    public void addLevel(int amount) {
        this.playerLevel += amount;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 获取玩家等级
     */
    public int getLevel() {
        return this.playerLevel;
    }

    /**
     * 衰减活跃灵气值（每日）
     */
    public void decayActiveQi() {
        this.activeQi = (long) (this.activeQi * 0.85);
    }

    /**
     * 学习功法
     */
    public void learnSkill(String skillId) {
        this.learnedSkills.put(skillId, 1);
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 升级功法
     */
    public boolean upgradeSkill(String skillId) {
        if (this.learnedSkills.containsKey(skillId)) {
            int currentLevel = this.learnedSkills.get(skillId);
            this.learnedSkills.put(skillId, currentLevel + 1);
            this.updatedAt = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * 获取功法等级
     */
    public int getSkillLevel(String skillId) {
        return this.learnedSkills.getOrDefault(skillId, 0);
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLogin() {
        this.lastLogin = System.currentTimeMillis();
        this.updatedAt = this.lastLogin;
    }

    /**
     * 记录突破尝试
     */
    public void recordBreakthroughAttempt(boolean success) {
        this.breakthroughAttempts++;
        if (success) {
            this.successfulBreakthroughs++;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 获取突破成功率
     */
    public double getBreakthroughSuccessRate() {
        if (breakthroughAttempts == 0) {
            return 0.0;
        }
        return (double) successfulBreakthroughs / breakthroughAttempts;
    }

    /**
     * 获取突破尝试次数
     */
    public int getBreakthroughAttemptCount() {
        return this.breakthroughAttempts;
    }

    /**
     * 获取突破成功次数
     */
    public int getBreakthroughSuccessCount() {
        return this.successfulBreakthroughs;
    }

    /**
     * 获取已学习的功法列表
     */
    public Map<String, Integer> getSkills() {
        return new HashMap<>(this.learnedSkills);
    }
    
    /**
     * 获取灵根显示名称（带颜色）
     */
    public String getSpiritualRootDisplay() {
        if (spiritualRootType != null) {
            return spiritualRootType.getColoredFullName();
        }
        // 兼容旧数据
        return getSpiritualRootGradeOld(spiritualRoot);
    }
    
    /**
     * 获取灵根等级名称（带颜色）
     */
    public String getSpiritualRootGrade() {
        if (spiritualRootType != null) {
            return spiritualRootType.getColoredGradeName();
        }
        // 兼容旧数据
        return getSpiritualRootGradeOld(spiritualRoot);
    }
    
    /**
     * 获取灵根五行属性显示
     */
    public String getSpiritualRootElements() {
        if (spiritualRootType != null) {
            return spiritualRootType.getElementsDisplay();
        }
        return "§7未知";
    }
    
    /**
     * 获取灵根品质描述
     */
    public String getSpiritualRootDescription() {
        if (spiritualRootType != null) {
            return spiritualRootType.getQualityDescription();
        }
        // 兼容旧数据
        if (spiritualRoot >= 0.9) return "§d§l传说中的天灵根！修炼之路将一帆风顺！";
        if (spiritualRoot >= 0.8) return "§5§l罕见的异灵根！注定成为一方强者！";
        if (spiritualRoot >= 0.7) return "§b§l不错的真灵根！勤加修炼，前途无量！";
        if (spiritualRoot >= 0.6) return "§e§l上品灵根！努力修炼，终将有所成就！";
        if (spiritualRoot >= 0.5) return "§a虽然是中品灵根，但机缘造化靠自己！";
        if (spiritualRoot >= 0.4) return "§7虽然是下品灵根，但坚持必有收获！";
        return "§7虽然是伪灵根，但凡人亦可逆天改命！";
    }
    
    /**
     * 旧版灵根品质获取（兼容）
     */
    private String getSpiritualRootGradeOld(double root) {
        if (root >= 0.9) return "§d§l天灵根";
        if (root >= 0.8) return "§5§l异灵根";
        if (root >= 0.7) return "§b§l真灵根";
        if (root >= 0.6) return "§e§l上品灵根";
        if (root >= 0.5) return "§a§l中品灵根";
        if (root >= 0.4) return "§7§l下品灵根";
        return "§8§l伪灵根";
    }

    // ==================== 显式 Setter 方法 ====================

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setSpiritualRootType(SpiritualRootType spiritualRootType) {
        this.spiritualRootType = spiritualRootType;
    }

    public void setSpiritualRoot(double spiritualRoot) {
        this.spiritualRoot = spiritualRoot;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public void setRealmStage(int realmStage) {
        this.realmStage = realmStage;
    }

    public void setQi(long qi) {
        this.qi = qi;
    }

    public void setComprehension(double comprehension) {
        this.comprehension = comprehension;
    }

    public void setTechniqueAdaptation(double techniqueAdaptation) {
        this.techniqueAdaptation = techniqueAdaptation;
    }

    public void setSpiritStones(long spiritStones) {
        this.spiritStones = spiritStones;
    }

    public void setContributionPoints(int contributionPoints) {
        this.contributionPoints = contributionPoints;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setEquipment(Map<String, String> equipment) {
        this.equipment = equipment;
    }

    public void setSectId(Integer sectId) {
        this.sectId = sectId;
    }

    public void setSectRank(String sectRank) {
        this.sectRank = sectRank;
    }

    public void setLearnedSkills(Map<String, Integer> learnedSkills) {
        this.learnedSkills = learnedSkills;
    }

    public void setSkillBindings(Map<Integer, String> skillBindings) {
        this.skillBindings = skillBindings;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setBreakthroughAttempts(int breakthroughAttempts) {
        this.breakthroughAttempts = breakthroughAttempts;
    }

    public void setSuccessfulBreakthroughs(int successfulBreakthroughs) {
        this.successfulBreakthroughs = successfulBreakthroughs;
    }

    public void setTribulationCount(int tribulationCount) {
        this.tribulationCount = tribulationCount;
    }

    public void setSuccessfulTribulations(int successfulTribulations) {
        this.successfulTribulations = successfulTribulations;
    }

    public void setActiveQi(long activeQi) {
        this.activeQi = activeQi;
    }

    public void setLastFateTime(long lastFateTime) {
        this.lastFateTime = lastFateTime;
    }

    public void setFateCount(int fateCount) {
        this.fateCount = fateCount;
    }

    public void setCultivating(boolean cultivating) {
        this.cultivating = cultivating;
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public void setPlayerLevel(int playerLevel) {
        this.playerLevel = playerLevel;
    }

    // ==================== 显式 Getter 方法 ====================

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getRealm() {
        return realm;
    }

    public int getRealmStage() {
        return realmStage;
    }

    public long getQi() {
        return qi;
    }

    public double getSpiritualRoot() {
        return spiritualRoot;
    }

    public SpiritualRootType getSpiritualRootType() {
        return spiritualRootType;
    }

    public double getComprehension() {
        return comprehension;
    }

    public double getTechniqueAdaptation() {
        return techniqueAdaptation;
    }

    public long getSpiritStones() {
        return spiritStones;
    }

    public int getContributionPoints() {
        return contributionPoints;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public int getPlayerLevel() {
        return playerLevel;
    }

    public Map<String, String> getEquipment() {
        return equipment;
    }

    public Integer getSectId() {
        return sectId;
    }

    public String getSectRank() {
        return sectRank;
    }

    public Map<String, Integer> getLearnedSkills() {
        return learnedSkills;
    }

    public Map<Integer, String> getSkillBindings() {
        return skillBindings;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public int getBreakthroughAttempts() {
        return breakthroughAttempts;
    }

    public int getSuccessfulBreakthroughs() {
        return successfulBreakthroughs;
    }

    public int getTribulationCount() {
        return tribulationCount;
    }

    public int getSuccessfulTribulations() {
        return successfulTribulations;
    }

    public long getActiveQi() {
        return activeQi;
    }

    public long getLastFateTime() {
        return lastFateTime;
    }

    public int getFateCount() {
        return fateCount;
    }

    public boolean isCultivating() {
        return cultivating;
    }
}
