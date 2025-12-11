package com.xiancore.core.realm;

import java.util.List;

/**
 * 境界数据类
 * 表示一个修仙境界的所有属性
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class Realm {

    private final String id;                    // 境界ID (如 "lianqi")
    private final String name;                  // 境界名称 (如 "炼气期")
    private final int order;                    // 排序顺序 (1-8)
    private final long baseBreakthroughQi;      // 突破所需基础修为
    private final double difficulty;            // 突破难度系数
    private final int levelGainSmall;           // 小境界升级增加的等级
    private final int levelGainBig;             // 大境界升级增加的等级
    private final int skillPointReward;         // 突破成功获得的功法点
    private final String nextRealmId;           // 下一境界ID (null表示最高境界)
    private final List<String> keywords;        // 用于解析怪物名称的关键词

    /**
     * 构造境界对象
     */
    public Realm(String id, String name, int order, long baseBreakthroughQi,
                 double difficulty, int levelGainSmall, int levelGainBig,
                 int skillPointReward, String nextRealmId, List<String> keywords) {
        this.id = id;
        this.name = name;
        this.order = order;
        this.baseBreakthroughQi = baseBreakthroughQi;
        this.difficulty = difficulty;
        this.levelGainSmall = levelGainSmall;
        this.levelGainBig = levelGainBig;
        this.skillPointReward = skillPointReward;
        this.nextRealmId = nextRealmId;
        this.keywords = keywords != null ? List.copyOf(keywords) : List.of();
    }

    // ==================== Getters ====================

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public long getBaseBreakthroughQi() {
        return baseBreakthroughQi;
    }

    public double getDifficulty() {
        return difficulty;
    }

    public int getLevelGainSmall() {
        return levelGainSmall;
    }

    public int getLevelGainBig() {
        return levelGainBig;
    }

    public int getSkillPointReward() {
        return skillPointReward;
    }

    public String getNextRealmId() {
        return nextRealmId;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * 是否为最高境界
     */
    public boolean isMaxRealm() {
        return nextRealmId == null || nextRealmId.isEmpty();
    }

    /**
     * 计算指定阶段的突破所需修为
     *
     * @param stage 阶段 (1=初期, 2=中期, 3=后期)
     * @return 突破所需修为
     */
    public long getBreakthroughQiForStage(int stage) {
        // 基础值 * 阶段系数 (初期x1, 中期x1.5, 后期x2)
        double multiplier = 1.0 + (stage - 1) * 0.5;
        return (long) (baseBreakthroughQi * multiplier);
    }

    /**
     * 计算指定小阶段对应的玩家等级
     *
     * @param stage 阶段 (1=初期, 2=中期, 3=后期)
     * @return 玩家等级
     */
    public int calculateLevel(int stage) {
        // 基础等级 = 1 + (境界顺序-1) * (小境界升级*2 + 大境界升级)
        int baseLevel = 1;
        if (order > 1) {
            // 前面境界贡献的等级
            // 每个境界: 初→中(+5) + 中→后(+5) + 后→下一境界初(+15) = 25
            baseLevel = 1 + (order - 1) * (levelGainSmall * 2 + levelGainBig);
        }
        // 加上当前境界的小阶段等级
        return baseLevel + (stage - 1) * levelGainSmall;
    }

    @Override
    public String toString() {
        return "Realm{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", order=" + order +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Realm realm = (Realm) o;
        return id.equals(realm.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private int order;
        private long baseBreakthroughQi = 1000L;
        private double difficulty = 1.0;
        private int levelGainSmall = 5;
        private int levelGainBig = 15;
        private int skillPointReward = 0;
        private String nextRealmId;
        private List<String> keywords;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder baseBreakthroughQi(long qi) {
            this.baseBreakthroughQi = qi;
            return this;
        }

        public Builder difficulty(double difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder levelGainSmall(int levelGainSmall) {
            this.levelGainSmall = levelGainSmall;
            return this;
        }

        public Builder levelGainBig(int levelGainBig) {
            this.levelGainBig = levelGainBig;
            return this;
        }

        public Builder skillPointReward(int reward) {
            this.skillPointReward = reward;
            return this;
        }

        public Builder nextRealmId(String nextRealmId) {
            this.nextRealmId = nextRealmId;
            return this;
        }

        public Builder keywords(List<String> keywords) {
            this.keywords = keywords;
            return this;
        }

        public Realm build() {
            if (id == null || name == null) {
                throw new IllegalStateException("Realm id and name are required");
            }
            return new Realm(id, name, order, baseBreakthroughQi, difficulty,
                    levelGainSmall, levelGainBig, skillPointReward, nextRealmId, keywords);
        }
    }
}
