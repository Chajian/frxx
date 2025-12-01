package com.xiancore.web.service;

import com.xiancore.common.dto.DamageRecordDTO;
import com.xiancore.web.entity.Boss;
import com.xiancore.web.entity.DamageRecord;
import com.xiancore.web.repository.BossRepository;
import com.xiancore.web.repository.DamageRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 伤害 业务服务类
 * 处理所有与伤害记录相关的业务逻辑
 */
@Service
@Slf4j
@Transactional
public class DamageService {

    @Autowired
    private DamageRecordRepository damageRepository;

    @Autowired
    private BossRepository bossRepository;

    /**
     * 记录伤害事件
     */
    public DamageRecord recordDamage(DamageRecordDTO dto) {
        log.info("记录伤害: Boss:{} Player:{} Damage:{}", dto.getBossId(), dto.getPlayerId(), dto.getDamage());

        Boss boss = bossRepository.findById(dto.getBossId())
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + dto.getBossId()));

        DamageRecord record = DamageRecord.builder()
                .id(UUID.randomUUID().toString())
                .boss(boss)
                .playerId(dto.getPlayerId())
                .playerName(dto.getPlayerName())
                .damage(dto.getDamage())
                .damageTime(System.currentTimeMillis())
                .damageType(dto.getDamageType() != null ? dto.getDamageType() : "PHYSICAL")
                .build();

        DamageRecord saved = damageRepository.save(record);
        log.debug("伤害记录保存成功: {}", saved.getId());

        // 更新Boss的总伤害
        boss.setTotalDamage(boss.getTotalDamage() + dto.getDamage());
        bossRepository.save(boss);

        return saved;
    }

    /**
     * 获取Boss的所有伤害记录
     */
    public List<DamageRecord> getDamageRecordsByBoss(String bossId) {
        log.debug("查询Boss的伤害记录: {}", bossId);
        return damageRepository.findByBossIdOrderByDamageTimeDesc(bossId);
    }

    /**
     * 分页查询Boss的伤害记录
     */
    public Page<DamageRecord> getDamageRecordsByBoss(String bossId, Pageable pageable) {
        log.debug("分页查询Boss的伤害记录: {}", bossId);
        return damageRepository.findByBossIdOrderByDamageTimeDesc(bossId, pageable);
    }

    /**
     * 获取玩家造成的所有伤害记录
     */
    public List<DamageRecord> getDamageRecordsByPlayer(String playerId) {
        log.debug("查询玩家的伤害记录: {}", playerId);
        return damageRepository.findByPlayerId(playerId);
    }

    /**
     * 获取玩家对特定Boss的伤害记录
     */
    public List<DamageRecord> getDamageRecordsByBossAndPlayer(String bossId, String playerId) {
        log.debug("查询玩家对Boss的伤害: Boss:{} Player:{}", bossId, playerId);
        return damageRepository.findByBossIdAndPlayerId(bossId, playerId);
    }

    /**
     * 获取Boss的总伤害值
     */
    public Double getBossTotalDamage(String bossId) {
        log.debug("获取Boss总伤害: {}", bossId);
        Double totalDamage = damageRepository.getTotalDamageForBoss(bossId);
        return totalDamage != null ? totalDamage : 0.0;
    }

    /**
     * 获取玩家对Boss的总伤害值
     */
    public Double getPlayerDamageTowardsBoss(String bossId, String playerId) {
        log.debug("获取玩家对Boss的总伤害: Boss:{} Player:{}", bossId, playerId);
        Double totalDamage = damageRepository.getTotalDamageByPlayer(bossId, playerId);
        return totalDamage != null ? totalDamage : 0.0;
    }

    /**
     * 获取Boss的伤害排名
     */
    public List<Object[]> getBossDamageRanking(String bossId) {
        log.debug("获取Boss的伤害排名: {}", bossId);
        return damageRepository.getDamageRankingByBoss(bossId);
    }

    /**
     * 分页获取Boss的伤害排名
     */
    public Page<Object[]> getBossDamageRanking(String bossId, Pageable pageable) {
        log.debug("分页获取Boss的伤害排名: {}", bossId);
        return damageRepository.getDamageRankingByBoss(bossId, pageable);
    }

    /**
     * 获取玩家的总伤害值
     */
    public Double getPlayerTotalDamage(String playerId) {
        log.debug("获取玩家总伤害: {}", playerId);
        Double totalDamage = damageRepository.getTotalDamageByPlayerId(playerId);
        return totalDamage != null ? totalDamage : 0.0;
    }

    /**
     * 统计Boss被伤害的次数
     */
    public Long getBossDamageCount(String bossId) {
        log.debug("统计Boss被伤害次数: {}", bossId);
        return damageRepository.countDamageRecordsByBoss(bossId);
    }

    /**
     * 统计玩家参与击杀的Boss数量
     */
    public Long countBossesDamagedByPlayer(String playerId) {
        log.debug("统计玩家参与击杀的Boss数量: {}", playerId);
        return damageRepository.countBossesDamagedByPlayer(playerId);
    }

    /**
     * 获取最高单次伤害记录
     */
    public Page<DamageRecord> getHighestDamageRecords(Pageable pageable) {
        log.debug("获取最高单次伤害记录");
        return damageRepository.findHighestDamageRecords(pageable);
    }

    /**
     * 获取指定伤害类型的记录
     */
    public List<DamageRecord> getDamageRecordsByType(String damageType) {
        log.debug("查询指定伤害类型的记录: {}", damageType);
        return damageRepository.findByDamageType(damageType);
    }

    /**
     * 获取时间范围内的伤害记录
     */
    public List<DamageRecord> getDamageRecordsByTimeRange(Long startTime, Long endTime) {
        log.debug("查询时间范围内的伤害记录: {} - {}", startTime, endTime);
        return damageRepository.findByDamageTimeBetween(startTime, endTime);
    }

    /**
     * 获取玩家对所有Boss的伤害统计
     */
    public List<Object[]> getPlayerDamageStatistics(String playerId) {
        log.debug("获取玩家的伤害统计: {}", playerId);
        return damageRepository.getPlayerDamageStatistics(playerId);
    }

    /**
     * 验证伤害记录的有效性
     */
    public Boolean isValidDamageRecord(DamageRecordDTO dto) {
        if (dto.getDamage() == null || dto.getDamage() <= 0) {
            log.warn("无效的伤害值: {}", dto.getDamage());
            return false;
        }

        if (dto.getPlayerId() == null || dto.getPlayerId().isEmpty()) {
            log.warn("无效的玩家ID");
            return false;
        }

        if (!bossRepository.existsById(dto.getBossId())) {
            log.warn("Boss不存在: {}", dto.getBossId());
            return false;
        }

        return true;
    }

    /**
     * 获取Boss的平均伤害
     */
    public Double getBossAverageDamage(String bossId) {
        log.debug("获取Boss的平均伤害: {}", bossId);
        Long damageCount = damageRepository.countDamageRecordsByBoss(bossId);
        if (damageCount == 0) {
            return 0.0;
        }
        Double totalDamage = damageRepository.getTotalDamageForBoss(bossId);
        return totalDamage / damageCount;
    }

    /**
     * 获取玩家对所有Boss的平均伤害
     */
    public Double getPlayerAverageDamagePerBoss(String playerId) {
        log.debug("获取玩家对所有Boss的平均伤害: {}", playerId);
        Long bossCount = damageRepository.countBossesDamagedByPlayer(playerId);
        if (bossCount == 0) {
            return 0.0;
        }
        Double totalDamage = damageRepository.getTotalDamageByPlayerId(playerId);
        return totalDamage / bossCount;
    }

    /**
     * 清除指定时间之前的伤害记录 (数据清理)
     */
    public Integer cleanupOldDamageRecords(Long beforeTime) {
        log.info("清理时间戳早于 {} 的伤害记录", beforeTime);
        List<DamageRecord> oldRecords = damageRepository.findByDamageTimeBetween(0L, beforeTime);
        if (!oldRecords.isEmpty()) {
            damageRepository.deleteAll(oldRecords);
            log.info("已删除 {} 条旧伤害记录", oldRecords.size());
            return oldRecords.size();
        }
        return 0;
    }
}
