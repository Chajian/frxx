package com.xiancore.web.service;

import com.xiancore.common.dto.BossDTO;
import com.xiancore.web.entity.Boss;
import com.xiancore.web.repository.BossRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Boss 业务服务类
 * 处理所有与Boss相关的业务逻辑
 */
@Service
@Slf4j
@Transactional
public class BossService {

    @Autowired
    private BossRepository bossRepository;

    /**
     * 创建新的Boss
     */
    public Boss createBoss(BossDTO dto) {
        log.info("创建新Boss: {}", dto.getBossName());

        Boss boss = Boss.builder()
                .id(UUID.randomUUID().toString())
                .name(dto.getBossName())
                .type(dto.getBossType())
                .status("SPAWNED")
                .world(dto.getWorld())
                .coordX(dto.getX())
                .coordY(dto.getY())
                .coordZ(dto.getZ())
                .currentHealth(dto.getMaxHealth())
                .maxHealth(dto.getMaxHealth())
                .totalDamage(0.0)
                .difficultyLevel(parseDifficultyLevel(dto.getDifficultyLevel()))
                .spawnedTime(System.currentTimeMillis())
                .build();

        Boss saved = bossRepository.save(boss);
        log.info("Boss创建成功: {}", saved.getId());
        return saved;
    }

    /**
     * 解析难度等级 (支持String或Number类型)
     */
    private Integer parseDifficultyLevel(Object difficultyLevel) {
        if (difficultyLevel == null) {
            return 1; // 默认难度
        }
        if (difficultyLevel instanceof String) {
            try {
                return Integer.parseInt((String) difficultyLevel);
            } catch (NumberFormatException e) {
                log.warn("无法解析难度等级: {}, 使用默认值", difficultyLevel);
                return 1;
            }
        }
        if (difficultyLevel instanceof Number) {
            return ((Number) difficultyLevel).intValue();
        }
        return 1;
    }

    /**
     * 获取Boss详情
     */
    public Optional<Boss> getBossById(String id) {
        log.debug("查询Boss: {}", id);
        return bossRepository.findById(id);
    }

    /**
     * 更新Boss信息
     */
    public Boss updateBoss(String id, BossDTO dto) {
        log.info("更新Boss: {}", id);

        Boss boss = bossRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + id));

        if (dto.getBossName() != null) {
            boss.setName(dto.getBossName());
        }
        if (dto.getX() != null && dto.getY() != null && dto.getZ() != null) {
            boss.setCoordX(dto.getX());
            boss.setCoordY(dto.getY());
            boss.setCoordZ(dto.getZ());
        }
        if (dto.getMaxHealth() != null) {
            boss.setMaxHealth(dto.getMaxHealth());
        }

        Boss updated = bossRepository.save(boss);
        log.info("Boss更新成功: {}", id);
        return updated;
    }

    /**
     * 删除Boss
     */
    public void deleteBoss(String id) {
        log.info("删除Boss: {}", id);
        bossRepository.deleteById(id);
    }

    /**
     * 记录Boss受到的伤害
     */
    public Boss recordDamage(String bossId, Double damageAmount) {
        log.debug("记录Boss伤害: {} - {}", bossId, damageAmount);

        Boss boss = bossRepository.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + bossId));

        boss.setTotalDamage(boss.getTotalDamage() + damageAmount);
        boss.setCurrentHealth(Math.max(0, boss.getCurrentHealth() - damageAmount));

        // 如果Boss血量为0，标记为死亡
        if (boss.getCurrentHealth() <= 0 && !"DEAD".equals(boss.getStatus())) {
            boss.setStatus("DEAD");
            boss.setKilledTime(System.currentTimeMillis());
            log.info("Boss已被击杀: {}", bossId);
        }

        return bossRepository.save(boss);
    }

    /**
     * 标记Boss为被击杀
     */
    public Boss markBossAsKilled(String bossId, String playerId) {
        log.info("Boss被击杀: {} - 击杀者: {}", bossId, playerId);

        Boss boss = bossRepository.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + bossId));

        boss.markAsKilled(playerId);
        return bossRepository.save(boss);
    }

    /**
     * 标记Boss为生成
     */
    public Boss markBossAsSpawned(String bossId) {
        log.info("Boss已生成: {}", bossId);

        Boss boss = bossRepository.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + bossId));

        boss.setStatus("SPAWNED");
        boss.setSpawnedTime(System.currentTimeMillis());
        return bossRepository.save(boss);
    }

    /**
     * 获取所有存活的Boss
     */
    public List<Boss> getAllAliveBosses() {
        log.debug("查询所有存活的Boss");
        return bossRepository.findAllAliveBosses();
    }

    /**
     * 获取指定世界的所有存活Boss
     */
    public List<Boss> getAliveBossesByWorld(String world) {
        log.debug("查询世界 {} 的所有存活Boss", world);
        return bossRepository.findAllAliveBossesByWorld(world);
    }

    /**
     * 分页查询指定状态的Boss
     */
    public Page<Boss> getBossesByStatus(String status, Pageable pageable) {
        log.debug("分页查询Boss - 状态: {}", status);
        return bossRepository.findActiveByStatus(status, pageable);
    }

    /**
     * 获取最近被击杀的Boss
     */
    public Page<Boss> getRecentlyKilledBosses(Pageable pageable) {
        log.debug("查询最近被击杀的Boss");
        return bossRepository.findRecentlyKilledBosses(pageable);
    }

    /**
     * 获取受伤害最多的Boss
     */
    public Page<Boss> getMostDamagedBosses(Pageable pageable) {
        log.debug("查询受伤害最多的Boss");
        return bossRepository.findMostDamagedBosses(pageable);
    }

    /**
     * 统计已击杀的Boss总数
     */
    public Long countKilledBosses() {
        log.debug("统计已击杀的Boss总数");
        return bossRepository.countKilledBosses();
    }

    /**
     * 统计玩家击杀的Boss数量
     */
    public Long countBossesKilledByPlayer(String playerId) {
        log.debug("统计玩家击杀的Boss数量: {}", playerId);
        return bossRepository.countBossesKilledByPlayer(playerId);
    }

    /**
     * 获取玩家击杀的所有Boss
     */
    public List<Boss> getBossesKilledByPlayer(String playerId) {
        log.debug("获取玩家击杀的Boss列表: {}", playerId);
        return bossRepository.findBossesKilledByPlayer(playerId);
    }

    /**
     * 获取特定难度等级的Boss
     */
    public List<Boss> getBossesByDifficulty(Integer difficulty) {
        log.debug("查询难度为 {} 的Boss", difficulty);
        return bossRepository.findByDifficultyLevel(difficulty);
    }

    /**
     * 获取特定类型的Boss
     */
    public List<Boss> getBossesByType(String type) {
        log.debug("查询类型为 {} 的Boss", type);
        return bossRepository.findByType(type);
    }

    /**
     * 获取Boss的血量百分比
     */
    public Double getBossHealthPercentage(String bossId) {
        Boss boss = bossRepository.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + bossId));
        return boss.getHealthPercentage();
    }

    /**
     * 检查Boss是否还活着
     */
    public Boolean isBossAlive(String bossId) {
        Boss boss = bossRepository.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + bossId));
        return boss.isAlive();
    }

    /**
     * 重置Boss状态 (用于Boss复活)
     */
    public Boss resetBoss(String bossId) {
        log.info("重置Boss状态: {}", bossId);

        Boss boss = bossRepository.findById(bossId)
                .orElseThrow(() -> new IllegalArgumentException("Boss不存在: " + bossId));

        boss.setStatus("ALIVE");
        boss.setCurrentHealth(boss.getMaxHealth());
        boss.setTotalDamage(0.0);
        boss.setKilledTime(null);
        boss.setKillerPlayerId(null);

        return bossRepository.save(boss);
    }
}
