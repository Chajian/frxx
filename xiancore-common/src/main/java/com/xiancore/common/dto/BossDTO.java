package com.xiancore.common.dto;

import lombok.*;

/**
 * Boss数据传输对象 - 用于插件和Web服务之间的数据交换
 * 包含Boss的所有重要信息，用于REST API和WebSocket通信
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class BossDTO {

    /** Boss唯一ID */
    private String bossId;

    /** Boss名称 */
    private String bossName;

    /** Boss类型 */
    private String bossType;

    /** Boss当前状态 (SPAWNED, ALIVE, DEAD, DESPAWNED) */
    private String status;

    /** 所在世界 */
    private String world;

    /** X坐标 */
    private Double x;

    /** Y坐标 */
    private Double y;

    /** Z坐标 */
    private Double z;

    /** 当前血量 */
    private Double currentHealth;

    /** 最大血量 */
    private Double maxHealth;

    /** 伤害数值 */
    private Double totalDamage;

    /** 难度等级 */
    private String difficultyLevel;

    /** 生成时间戳 */
    private Long spawnedTime;

    /** 击杀时间戳 */
    private Long killedTime;

    /** 击杀玩家ID */
    private String killerPlayerId;

    /** 创建时间 */
    private Long createdAt;

    /** 更新时间 */
    private Long updatedAt;
}
