package com.xiancore.web.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DamageRecord 实体类
 * 记录游戏中玩家对Boss造成的伤害
 */
@Entity
@Table(name = "damage_records", indexes = {
    @Index(name = "idx_boss_id", columnList = "boss_id"),
    @Index(name = "idx_player_id", columnList = "player_id"),
    @Index(name = "idx_damage_time", columnList = "damage_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DamageRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 伤害记录ID
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * 关联的Boss
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boss_id", nullable = false)
    private Boss boss;

    /**
     * 造成伤害的玩家ID
     */
    @Column(name = "player_id", length = 36, nullable = false)
    private String playerId;

    /**
     * 造成伤害的玩家名称
     */
    @Column(name = "player_name", length = 100)
    private String playerName;

    /**
     * 造成的伤害值
     */
    @Column(nullable = false)
    private Double damage;

    /**
     * 伤害发生时间戳
     */
    @Column(name = "damage_time", nullable = false)
    private Long damageTime;

    /**
     * 伤害类型: PHYSICAL(物理), MAGICAL(魔法), TRUE_DAMAGE(真实伤害)
     */
    @Column(name = "damage_type", length = 50)
    private String damageType;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 获取伤害发生距离现在的时间(秒)
     */
    public long getSecondsSinceDamage() {
        return (System.currentTimeMillis() - damageTime) / 1000;
    }

    /**
     * 检查伤害是否有效
     */
    public boolean isValid() {
        return damage > 0 && playerId != null && !playerId.isEmpty() && boss != null;
    }

    /**
     * 获取伤害类型的中文描述
     */
    public String getDamageTypeDescription() {
        if ("PHYSICAL".equals(damageType)) {
            return "物理伤害";
        } else if ("MAGICAL".equals(damageType)) {
            return "魔法伤害";
        } else if ("TRUE_DAMAGE".equals(damageType)) {
            return "真实伤害";
        }
        return "未知伤害";
    }
}
