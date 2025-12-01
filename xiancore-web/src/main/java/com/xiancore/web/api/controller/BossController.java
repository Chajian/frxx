package com.xiancore.web.api.controller;

import com.xiancore.common.dto.BossDTO;
import com.xiancore.web.api.dto.ApiResponse;
import com.xiancore.web.api.dto.PageResponse;
import com.xiancore.web.entity.Boss;
import com.xiancore.web.service.BossService;
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
 * Boss管理REST API端点
 * 处理Boss的所有业务操作
 */
@RestController
@RequestMapping("/api/v1/bosses")
@RequiredArgsConstructor
@Slf4j
public class BossController {

    private final BossService bossService;

    /**
     * 创建新的Boss
     * POST /api/v1/bosses
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Boss>> createBoss(@Valid @RequestBody BossDTO bossDTO) {
        log.info("Creating new boss: {}", bossDTO.getBossName());
        try {
            Boss boss = bossService.createBoss(bossDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(boss));
        } catch (Exception e) {
            log.error("Failed to create boss", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to create boss: " + e.getMessage()));
        }
    }

    /**
     * 获取所有活跃的Boss
     * GET /api/v1/bosses/alive
     */
    @GetMapping("/alive")
    public ResponseEntity<ApiResponse<List<Boss>>> getAllAliveBosses() {
        log.info("Fetching all alive bosses");
        try {
            List<Boss> bosses = bossService.getAllAliveBosses();
            return ResponseEntity.ok(ApiResponse.success(bosses));
        } catch (Exception e) {
            log.error("Failed to fetch alive bosses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch alive bosses"));
        }
    }

    /**
     * 获取指定世界的活跃Boss
     * GET /api/v1/bosses/world/{world}
     */
    @GetMapping("/world/{world}")
    public ResponseEntity<ApiResponse<List<Boss>>> getAliveBossesByWorld(
            @PathVariable String world) {
        log.info("Fetching alive bosses in world: {}", world);
        try {
            List<Boss> bosses = bossService.getAliveBossesByWorld(world);
            return ResponseEntity.ok(ApiResponse.success(bosses));
        } catch (Exception e) {
            log.error("Failed to fetch bosses for world: {}", world, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch bosses"));
        }
    }

    /**
     * 按状态分页查询Boss
     * GET /api/v1/bosses?status=ALIVE&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<List<Boss>>>> getBossesByStatus(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "spawnedTime") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        log.info("Fetching bosses - status: {}, page: {}, size: {}", status, page, size);
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            Page<Boss> bosses = (status != null)
                    ? bossService.getBossesByStatus(status, pageable)
                    : bossService.getBossesByStatus("ALIVE", pageable);

            PageResponse<List<Boss>> pageResponse = PageResponse.<List<Boss>>builder()
                    .pageNumber(bosses.getNumber())
                    .pageSize(bosses.getSize())
                    .totalPages(bosses.getTotalPages())
                    .totalElements(bosses.getTotalElements())
                    .content(bosses.getContent())
                    .last(bosses.isLast())
                    .first(bosses.isFirst())
                    .hasNext(bosses.hasNext())
                    .hasPrevious(bosses.hasPrevious())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("Failed to fetch bosses by status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch bosses"));
        }
    }

    /**
     * 获取Boss详细信息
     * GET /api/v1/bosses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Boss>> getBossById(@PathVariable String id) {
        log.info("Fetching boss with id: {}", id);
        try {
            return bossService.getBossById(id)
                    .map(boss -> ResponseEntity.ok(ApiResponse.success(boss)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(404, "Boss not found")));
        } catch (Exception e) {
            log.error("Failed to fetch boss", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch boss"));
        }
    }

    /**
     * 更新Boss信息
     * PUT /api/v1/bosses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boss>> updateBoss(
            @PathVariable String id,
            @Valid @RequestBody BossDTO bossDTO) {
        log.info("Updating boss with id: {}", id);
        try {
            Boss boss = bossService.updateBoss(id, bossDTO);
            return ResponseEntity.ok(ApiResponse.success(boss));
        } catch (Exception e) {
            log.error("Failed to update boss", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to update boss: " + e.getMessage()));
        }
    }

    /**
     * 删除Boss
     * DELETE /api/v1/bosses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBoss(@PathVariable String id) {
        log.info("Deleting boss with id: {}", id);
        try {
            bossService.deleteBoss(id);
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to delete boss", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to delete boss: " + e.getMessage()));
        }
    }

    /**
     * 记录Boss伤害
     * PUT /api/v1/bosses/{id}/damage
     */
    @PutMapping("/{id}/damage")
    public ResponseEntity<ApiResponse<Boss>> recordDamage(
            @PathVariable String id,
            @RequestParam Double damage) {
        log.info("Recording damage {} for boss: {}", damage, id);
        try {
            Boss boss = bossService.recordDamage(id, damage);
            return ResponseEntity.ok(ApiResponse.success(boss));
        } catch (Exception e) {
            log.error("Failed to record damage", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to record damage: " + e.getMessage()));
        }
    }

    /**
     * 标记Boss被击杀
     * PUT /api/v1/bosses/{id}/kill
     */
    @PutMapping("/{id}/kill")
    public ResponseEntity<ApiResponse<Boss>> markBossAsKilled(
            @PathVariable String id,
            @RequestParam String playerId) {
        log.info("Marking boss {} as killed by player: {}", id, playerId);
        try {
            Boss boss = bossService.markBossAsKilled(id, playerId);
            return ResponseEntity.ok(ApiResponse.success(boss));
        } catch (Exception e) {
            log.error("Failed to mark boss as killed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to mark boss as killed: " + e.getMessage()));
        }
    }

    /**
     * 标记Boss为生成
     * PUT /api/v1/bosses/{id}/spawn
     */
    @PutMapping("/{id}/spawn")
    public ResponseEntity<ApiResponse<Boss>> markBossAsSpawned(@PathVariable String id) {
        log.info("Marking boss {} as spawned", id);
        try {
            Boss boss = bossService.markBossAsSpawned(id);
            return ResponseEntity.ok(ApiResponse.success(boss));
        } catch (Exception e) {
            log.error("Failed to mark boss as spawned", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to mark boss as spawned: " + e.getMessage()));
        }
    }

    /**
     * 获取Boss血量百分比
     * GET /api/v1/bosses/{id}/health-percentage
     */
    @GetMapping("/{id}/health-percentage")
    public ResponseEntity<ApiResponse<Double>> getBossHealthPercentage(@PathVariable String id) {
        log.info("Fetching health percentage for boss: {}", id);
        try {
            Double percentage = bossService.getBossHealthPercentage(id);
            return ResponseEntity.ok(ApiResponse.success(percentage));
        } catch (Exception e) {
            log.error("Failed to fetch health percentage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch health percentage"));
        }
    }

    /**
     * 检查Boss是否活跃
     * GET /api/v1/bosses/{id}/is-alive
     */
    @GetMapping("/{id}/is-alive")
    public ResponseEntity<ApiResponse<Boolean>> isBossAlive(@PathVariable String id) {
        log.info("Checking if boss {} is alive", id);
        try {
            Boolean alive = bossService.isBossAlive(id);
            return ResponseEntity.ok(ApiResponse.success(alive));
        } catch (Exception e) {
            log.error("Failed to check if boss is alive", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to check boss status"));
        }
    }

    /**
     * 获取最近击杀的Boss
     * GET /api/v1/bosses/recent-killed?limit=10
     */
    @GetMapping("/recent-killed")
    public ResponseEntity<ApiResponse<List<Boss>>> getRecentlyKilledBosses(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching {} recently killed bosses", limit);
        try {
            List<Boss> bosses = bossService.getRecentlyKilledBosses(PageRequest.of(0, limit)).getContent();
            return ResponseEntity.ok(ApiResponse.success(bosses));
        } catch (Exception e) {
            log.error("Failed to fetch recently killed bosses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch recently killed bosses"));
        }
    }

    /**
     * 统计击杀的Boss总数
     * GET /api/v1/bosses/count/killed
     */
    @GetMapping("/count/killed")
    public ResponseEntity<ApiResponse<Long>> countKilledBosses() {
        log.info("Counting killed bosses");
        try {
            Long count = bossService.countKilledBosses();
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to count killed bosses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to count killed bosses"));
        }
    }

    /**
     * 统计玩家击杀的Boss数量
     * GET /api/v1/bosses/count/player/{playerId}
     */
    @GetMapping("/count/player/{playerId}")
    public ResponseEntity<ApiResponse<Long>> countBossesKilledByPlayer(
            @PathVariable String playerId) {
        log.info("Counting bosses killed by player: {}", playerId);
        try {
            Long count = bossService.countBossesKilledByPlayer(playerId);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to count bosses killed by player", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to count bosses"));
        }
    }
}
