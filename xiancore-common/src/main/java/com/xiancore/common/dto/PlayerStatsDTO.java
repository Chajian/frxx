package com.xiancore.common.dto;

import lombok.*;

/**
 * 玩家统计数据传输对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PlayerStatsDTO {

    /** 玩家UUID */
    private String playerId;

    /** 玩家名称 */
    private String playerName;

    /** 击杀Boss数量 */
    private Integer bossKills;

    /** 总伤害输出 */
    private Double totalDamage;

    /** 总参与战斗数 */
    private Integer totalBattles;

    /** 当前余额 */
    private Double balance;

    /** 总赚取 */
    private Double totalEarned;

    /** 总消费 */
    private Double totalSpent;

    /** 击杀排名 */
    private Integer killRanking;

    /** 财富排名 */
    private Integer wealthRanking;

    /** 最后更新时间 */
    private Long lastUpdated;

    /** 加入时间 */
    private Long joinedAt;
}
