package com.xiancore.web.api.controller;

import com.xiancore.common.dto.DamageRecordDTO;
import com.xiancore.common.dto.ApiResponse;
import com.xiancore.web.api.dto.PageResponse;
import com.xiancore.web.entity.DamageRecord;
import com.xiancore.web.service.DamageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 伤害记录REST API端点
 * 处理所有伤害相关的业务操作
 */
@RestController
@RequestMapping("/api/v1/damage")
@RequiredArgsConstructor
@Slf4j
public class DamageController {

    private final DamageService damageService;

    /**
     * 记录新的伤害事件
     * POST /api/v1/damage
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DamageRecord>> recordDamage(
            @Valid @RequestBody DamageRecordDTO damageDTO) {
        log.info("Recording damage event: player={}, boss={}, damage={}",
                damageDTO.getPlayerId(), damageDTO.getBossId(), damageDTO.getDamage());
        try {
            DamageRecord record = damageService.recordDamage(damageDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(record));
        } catch (Exception e) {
            log.error("Failed to record damage", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to record damage: " + e.getMessage()));
        }
    }

    /**
     * 获取Boss的所有伤害记录
     * GET /api/v1/damage/boss/{bossId}
     */
    @GetMapping("/boss/{bossId}")
    public ResponseEntity<ApiResponse<List<DamageRecord>>> getDamageRecordsByBoss(
            @PathVariable String bossId) {
        log.info("Fetching damage records for boss: {}", bossId);
        try {
            List<DamageRecord> records = damageService.getDamageRecordsByBoss(bossId);
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("Failed to fetch damage records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage records"));
        }
    }

    /**
     * 获取Boss的伤害记录（分页）
     * GET /api/v1/damage/boss/{bossId}/paged?page=0&size=10
     */
    @GetMapping("/boss/{bossId}/paged")
    public ResponseEntity<ApiResponse<PageResponse<List<DamageRecord>>>> getDamageRecordsByBossPaged(
            @PathVariable String bossId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching paged damage records for boss: {} - page: {}, size: {}", bossId, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "damageTime"));
            Page<DamageRecord> records = damageService.getDamageRecordsByBoss(bossId, pageable);

            PageResponse<List<DamageRecord>> pageResponse = PageResponse.<List<DamageRecord>>builder()
                    .pageNumber(records.getNumber())
                    .pageSize(records.getSize())
                    .totalPages(records.getTotalPages())
                    .totalElements(records.getTotalElements())
                    .content(records.getContent())
                    .last(records.isLast())
                    .first(records.isFirst())
                    .hasNext(records.hasNext())
                    .hasPrevious(records.hasPrevious())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("Failed to fetch paged damage records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage records"));
        }
    }

    /**
     * 获取玩家的所有伤害记录
     * GET /api/v1/damage/player/{playerId}
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<ApiResponse<List<DamageRecord>>> getDamageRecordsByPlayer(
            @PathVariable String playerId) {
        log.info("Fetching damage records for player: {}", playerId);
        try {
            List<DamageRecord> records = damageService.getDamageRecordsByPlayer(playerId);
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("Failed to fetch damage records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage records"));
        }
    }

    /**
     * 获取Boss的总伤害
     * GET /api/v1/damage/boss/{bossId}/total
     */
    @GetMapping("/boss/{bossId}/total")
    public ResponseEntity<ApiResponse<Double>> getBossTotalDamage(@PathVariable String bossId) {
        log.info("Fetching total damage for boss: {}", bossId);
        try {
            Double totalDamage = damageService.getBossTotalDamage(bossId);
            return ResponseEntity.ok(ApiResponse.success(totalDamage));
        } catch (Exception e) {
            log.error("Failed to fetch total damage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch total damage"));
        }
    }

    /**
     * 获取玩家对Boss的总伤害
     * GET /api/v1/damage/boss/{bossId}/player/{playerId}
     */
    @GetMapping("/boss/{bossId}/player/{playerId}")
    public ResponseEntity<ApiResponse<Double>> getPlayerDamageTowardsBoss(
            @PathVariable String bossId,
            @PathVariable String playerId) {
        log.info("Fetching damage from player {} to boss {}", playerId, bossId);
        try {
            Double damage = damageService.getPlayerDamageTowardsBoss(bossId, playerId);
            return ResponseEntity.ok(ApiResponse.success(damage));
        } catch (Exception e) {
            log.error("Failed to fetch player damage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch player damage"));
        }
    }

    /**
     * 获取玩家的总伤害
     * GET /api/v1/damage/player/{playerId}/total
     */
    @GetMapping("/player/{playerId}/total")
    public ResponseEntity<ApiResponse<Double>> getPlayerTotalDamage(
            @PathVariable String playerId) {
        log.info("Fetching total damage for player: {}", playerId);
        try {
            Double totalDamage = damageService.getPlayerTotalDamage(playerId);
            return ResponseEntity.ok(ApiResponse.success(totalDamage));
        } catch (Exception e) {
            log.error("Failed to fetch total damage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch total damage"));
        }
    }

    /**
     * 获取Boss的伤害排名
     * GET /api/v1/damage/boss/{bossId}/ranking
     */
    @GetMapping("/boss/{bossId}/ranking")
    public ResponseEntity<ApiResponse<List<Object[]>>> getDamageRankingByBoss(
            @PathVariable String bossId) {
        log.info("Fetching damage ranking for boss: {}", bossId);
        try {
            List<Object[]> ranking = damageService.getBossDamageRanking(bossId);
            return ResponseEntity.ok(ApiResponse.success(ranking));
        } catch (Exception e) {
            log.error("Failed to fetch damage ranking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage ranking"));
        }
    }

    /**
     * 获取Boss的伤害次数
     * GET /api/v1/damage/boss/{bossId}/count
     */
    @GetMapping("/boss/{bossId}/count")
    public ResponseEntity<ApiResponse<Long>> getBossDamageCount(@PathVariable String bossId) {
        log.info("Fetching damage count for boss: {}", bossId);
        try {
            Long count = damageService.getBossDamageCount(bossId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to fetch damage count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage count"));
        }
    }

    /**
     * 获取Boss的平均伤害
     * GET /api/v1/damage/boss/{bossId}/average
     */
    @GetMapping("/boss/{bossId}/average")
    public ResponseEntity<ApiResponse<Double>> getBossAverageDamage(@PathVariable String bossId) {
        log.info("Fetching average damage for boss: {}", bossId);
        try {
            Double avgDamage = damageService.getBossAverageDamage(bossId);
            return ResponseEntity.ok(ApiResponse.success(avgDamage));
        } catch (Exception e) {
            log.error("Failed to fetch average damage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch average damage"));
        }
    }

    /**
     * 获取按伤害类型分类的伤害记录
     * GET /api/v1/damage/type/{damageType}
     */
    @GetMapping("/type/{damageType}")
    public ResponseEntity<ApiResponse<List<DamageRecord>>> getDamageRecordsByType(
            @PathVariable String damageType) {
        log.info("Fetching damage records by type: {}", damageType);
        try {
            List<DamageRecord> records = damageService.getDamageRecordsByType(damageType);
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("Failed to fetch damage records by type", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage records"));
        }
    }

    /**
     * 获取最高单次伤害记录
     * GET /api/v1/damage/highest?limit=10
     */
    @GetMapping("/highest")
    public ResponseEntity<ApiResponse<List<DamageRecord>>> getHighestDamageRecords(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching {} highest damage records", limit);
        try {
            List<DamageRecord> records = damageService.getHighestDamageRecords(PageRequest.of(0, limit)).getContent();
            return ResponseEntity.ok(ApiResponse.success(records));
        } catch (Exception e) {
            log.error("Failed to fetch highest damage records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage records"));
        }
    }

    /**
     * 清理旧的伤害记录
     * DELETE /api/v1/damage/cleanup?beforeTime=1609459200000
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldDamageRecords(
            @RequestParam Long beforeTime) {
        log.info("Cleaning up damage records before: {}", beforeTime);
        try {
            Integer deleted = damageService.cleanupOldDamageRecords(beforeTime);
            return ResponseEntity.ok(ApiResponse.success(deleted));
        } catch (Exception e) {
            log.error("Failed to cleanup damage records", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to cleanup records: " + e.getMessage()));
        }
    }
}
