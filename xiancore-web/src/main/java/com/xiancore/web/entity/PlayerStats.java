package com.xiancore.web.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * PlayerStats 实体类
 * 记录玩家的游戏统计数据，包括击杀数、伤害统计、排名等信息
 */
@Entity
@Table(name = "player_stats", indexes = {
    @Index(name = "idx_player_id", columnList = "player_id", unique = true),
    @Index(name = "idx_kill_ranking", columnList = "kill_ranking"),
    @Index(name = "idx_wealth_ranking", columnList = "wealth_ranking")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStats implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 统计记录ID
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * 玩家ID (Minecraft UUID)
     */
    @Column(name = "player_id", length = 36, unique = true, nullable = false)
    private String playerId;

    /**
     * 玩家名称
     */
    @Column(name = "player_name", length = 100)
    private String playerName;

    /**
     * Boss击杀总数
     */
    @Column(name = "boss_kills")
    private Integer bossKills;

    /**
     * 对Boss造成的总伤害
     */
    @Column(name = "total_damage")
    private Double totalDamage;

    /**
     * 参与过的战斗总数
     */
    @Column(name = "total_battles")
    private Integer totalBattles;

    /**
     * 玩家当前余额 (游戏币)
     */
    @Column(name = "balance")
    private Double balance;

    /**
     * 玩家总收入 (游戏币)
     */
    @Column(name = "total_earned")
    private Double totalEarned;

    /**
     * 玩家总支出 (游戏币)
     */
    @Column(name = "total_spent")
    private Double totalSpent;

    /**
     * Boss击杀数排名 (1为最高)
     */
    @Column(name = "kill_ranking")
    private Integer killRanking;

    /**
     * 财富排名 (1为最富有)
     */
    @Column(name = "wealth_ranking")
    private Integer wealthRanking;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 增加玩家的Boss击杀数
     */
    public void addBossKill() {
        if (this.bossKills == null) {
            this.bossKills = 0;
        }
        this.bossKills++;
    }

    /**
     * 增加玩家的伤害统计
     */
    public void addDamage(Double damageAmount) {
        if (this.totalDamage == null) {
            this.totalDamage = 0.0;
        }
        this.totalDamage += damageAmount;
    }

    /**
     * 增加玩家参与的战斗计数
     */
    public void addBattle() {
        if (this.totalBattles == null) {
            this.totalBattles = 0;
        }
        this.totalBattles++;
    }

    /**
     * 增加玩家收入
     */
    public void addEarnings(Double amount) {
        if (this.totalEarned == null) {
            this.totalEarned = 0.0;
        }
        this.totalEarned += amount;
        addBalance(amount);
    }

    /**
     * 增加玩家支出
     */
    public void addSpending(Double amount) {
        if (this.totalSpent == null) {
            this.totalSpent = 0.0;
        }
        this.totalSpent += amount;
        subtractBalance(amount);
    }

    /**
     * 增加玩家余额
     */
    public void addBalance(Double amount) {
        if (this.balance == null) {
            this.balance = 0.0;
        }
        this.balance += amount;
    }

    /**
     * 减少玩家余额
     */
    public void subtractBalance(Double amount) {
        if (this.balance == null) {
            this.balance = 0.0;
        }
        this.balance -= amount;
    }

    /**
     * 获取平均每次战斗的伤害
     */
    public Double getAverageDamagePerBattle() {
        if (totalBattles == null || totalBattles == 0 || totalDamage == null) {
            return 0.0;
        }
        return totalDamage / totalBattles;
    }

    /**
     * 获取平均每次击杀的伤害
     */
    public Double getAverageDamagePerKill() {
        if (bossKills == null || bossKills == 0 || totalDamage == null) {
            return 0.0;
        }
        return totalDamage / bossKills;
    }

    /**
     * 获取玩家的财富等级
     */
    public String getWealthLevel() {
        if (balance == null) {
            return "贫困";
        }
        if (balance < 1000) {
            return "贫困";
        } else if (balance < 10000) {
            return "普通";
        } else if (balance < 100000) {
            return "富有";
        } else if (balance < 1000000) {
            return "非常富有";
        } else {
            return "超级富有";
        }
    }

    /**
     * 初始化新玩家的默认统计数据
     */
    public void initializeDefaults() {
        if (this.bossKills == null) {
            this.bossKills = 0;
        }
        if (this.totalDamage == null) {
            this.totalDamage = 0.0;
        }
        if (this.totalBattles == null) {
            this.totalBattles = 0;
        }
        if (this.balance == null) {
            this.balance = 0.0;
        }
        if (this.totalEarned == null) {
            this.totalEarned = 0.0;
        }
        if (this.totalSpent == null) {
            this.totalSpent = 0.0;
        }
    }
}
