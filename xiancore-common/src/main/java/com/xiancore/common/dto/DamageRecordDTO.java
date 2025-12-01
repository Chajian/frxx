package com.xiancore.common.dto;

import lombok.*;

/**
 * 伤害记录传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DamageRecordDTO {

    /** 唯一ID */
    private String recordId;

    /** Boss ID */
    private String bossId;

    /** 玩家ID */
    private String playerId;

    /** 玩家名称 */
    private String playerName;

    /** 伤害数值 */
    private Double damage;

    /** 伤害时间戳 */
    private Long damageTime;

    /** 伤害类型 (PHYSICAL, MAGICAL, TRUE_DAMAGE) */
    private String damageType;

    /** 创建时间 */
    private Long createdAt;
}
