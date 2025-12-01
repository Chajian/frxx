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
import java.util.List;

/**
 * Boss 实体类
 * 代表游戏中的Boss对象，包含Boss的所有属性和状态信息
 */
@Entity
@Table(name = "bosses", indexes = {
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_world", columnList = "world"),
    @Index(name = "idx_spawned_time", columnList = "spawned_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boss implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Boss唯一ID
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * Boss名称
     */
    @Column(length = 100, nullable = false)
    private String name;

    /**
     * Boss类型 (例如: DRAGON, SKELETON_KING等)
     */
    @Column(length = 50)
    private String type;

    /**
     * Boss状态: SPAWNED(生成), ALIVE(存活), DEAD(已死), DESPAWNED(消失)
     */
    @Column(length = 20, nullable = false)
    private String status;

    /**
     * Boss所在世界
     */
    @Column(length = 100)
    private String world;

    /**
     * Boss X坐标
     */
    @Column(name = "x")
    private Double coordX;

    /**
     * Boss Y坐标
     */
    @Column(name = "y")
    private Double coordY;

    /**
     * Boss Z坐标
     */
    @Column(name = "z")
    private Double coordZ;

    /**
     * Boss当前血量
     */
    @Column(name = "current_health")
    private Double currentHealth;

    /**
     * Boss最大血量
     */
    @Column(name = "max_health")
    private Double maxHealth;

    /**
     * Boss受到的总伤害
     */
    @Column(name = "total_damage")
    private Double totalDamage;

    /**
     * 难度等级: 1-5 (简单到绝望)
     */
    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    /**
     * Boss生成时间戳
     */
    @Column(name = "spawned_time")
    private Long spawnedTime;

    /**
     * Boss被击杀时间戳
     */
    @Column(name = "killed_time")
    private Long killedTime;

    /**
     * 击杀者玩家ID
     */
    @Column(name = "killer_player_id", length = 36)
    private String killerPlayerId;

    /**
     * 该Boss关联的伤害记录
     */
    @OneToMany(mappedBy = "boss", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DamageRecord> damageRecords;

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
     * 获取Boss是否活跃
     */
    public boolean isAlive() {
        return "ALIVE".equals(status);
    }

    /**
     * 获取Boss是否已击杀
     */
    public boolean isKilled() {
        return "DEAD".equals(status);
    }

    /**
     * 获取Boss剩余血量百分比
     */
    public Double getHealthPercentage() {
        if (maxHealth == null || maxHealth == 0) {
            return 100.0;
        }
        return (currentHealth / maxHealth) * 100;
    }

    /**
     * 更新Boss状态为击杀
     */
    public void markAsKilled(String playerId) {
        this.status = "DEAD";
        this.killedTime = System.currentTimeMillis();
        this.killerPlayerId = playerId;
        this.currentHealth = 0.0;
    }
}
