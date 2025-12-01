package com.xiancore.web.api.controller;

import com.xiancore.web.api.dto.ApiResponse;
import com.xiancore.web.api.dto.PageResponse;
import com.xiancore.web.entity.PlayerStats;
import com.xiancore.web.service.PlayerStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家统计REST API端点
 * 处理所有玩家相关的统计和经济操作
 */
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final PlayerStatsService playerStatsService;

    /**
     * 获取或创建玩家统计
     * POST /api/v1/stats/{playerId}?playerName=xxx
     */
    @PostMapping("/{playerId}")
    public ResponseEntity<ApiResponse<PlayerStats>> getOrCreatePlayerStats(
            @PathVariable String playerId,
            @RequestParam String playerName) {
        log.info("Getting or creating stats for player: {} - {}", playerId, playerName);
        try {
            PlayerStats stats = playerStatsService.getOrCreatePlayerStats(playerId, playerName);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to get or create player stats", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to get or create stats: " + e.getMessage()));
        }
    }

    /**
     * 获取玩家统计信息
     * GET /api/v1/stats/{playerId}
     */
    @GetMapping("/{playerId}")
    public ResponseEntity<ApiResponse<PlayerStats>> getPlayerStats(@PathVariable String playerId) {
        log.info("Fetching stats for player: {}", playerId);
        try {
            return playerStatsService.getPlayerStats(playerId)
                    .map(stats -> ResponseEntity.ok(ApiResponse.success(stats)))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error(404, "Player stats not found")));
        } catch (Exception e) {
            log.error("Failed to fetch player stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch player stats"));
        }
    }

    /**
     * 增加Boss击杀
     * PUT /api/v1/stats/{playerId}/kill?reward=500
     */
    @PutMapping("/{playerId}/kill")
    public ResponseEntity<ApiResponse<PlayerStats>> addBossKill(
            @PathVariable String playerId,
            @RequestParam Double reward) {
        log.info("Adding boss kill for player: {} - reward: {}", playerId, reward);
        try {
            PlayerStats stats = playerStatsService.addBossKill(playerId, reward);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to add boss kill", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to add boss kill: " + e.getMessage()));
        }
    }

    /**
     * 增加伤害统计
     * PUT /api/v1/stats/{playerId}/damage?damage=150
     */
    @PutMapping("/{playerId}/damage")
    public ResponseEntity<ApiResponse<PlayerStats>> addPlayerDamage(
            @PathVariable String playerId,
            @RequestParam Double damage) {
        log.info("Adding damage for player: {} - damage: {}", playerId, damage);
        try {
            PlayerStats stats = playerStatsService.addPlayerDamage(playerId, damage);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to add player damage", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to add damage: " + e.getMessage()));
        }
    }

    /**
     * 增加战斗次数
     * PUT /api/v1/stats/{playerId}/battle
     */
    @PutMapping("/{playerId}/battle")
    public ResponseEntity<ApiResponse<PlayerStats>> addBattle(@PathVariable String playerId) {
        log.info("Adding battle for player: {}", playerId);
        try {
            PlayerStats stats = playerStatsService.addBattle(playerId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to add battle", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to add battle: " + e.getMessage()));
        }
    }

    /**
     * 增加收入
     * PUT /api/v1/stats/{playerId}/earnings?amount=1000
     */
    @PutMapping("/{playerId}/earnings")
    public ResponseEntity<ApiResponse<PlayerStats>> addEarnings(
            @PathVariable String playerId,
            @RequestParam Double amount) {
        log.info("Adding earnings for player: {} - amount: {}", playerId, amount);
        try {
            PlayerStats stats = playerStatsService.addEarnings(playerId, amount);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to add earnings", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to add earnings: " + e.getMessage()));
        }
    }

    /**
     * 增加支出
     * PUT /api/v1/stats/{playerId}/spending?amount=300
     */
    @PutMapping("/{playerId}/spending")
    public ResponseEntity<ApiResponse<PlayerStats>> addSpending(
            @PathVariable String playerId,
            @RequestParam Double amount) {
        log.info("Adding spending for player: {} - amount: {}", playerId, amount);
        try {
            PlayerStats stats = playerStatsService.addSpending(playerId, amount);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to add spending", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to add spending: " + e.getMessage()));
        }
    }

    /**
     * 增加余额
     * PUT /api/v1/stats/{playerId}/balance?amount=500
     */
    @PutMapping("/{playerId}/balance")
    public ResponseEntity<ApiResponse<PlayerStats>> addBalance(
            @PathVariable String playerId,
            @RequestParam Double amount) {
        log.info("Adding balance for player: {} - amount: {}", playerId, amount);
        try {
            PlayerStats stats = playerStatsService.addBalance(playerId, amount);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to add balance", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to add balance: " + e.getMessage()));
        }
    }

    /**
     * 减少余额
     * PUT /api/v1/stats/{playerId}/balance-subtract?amount=200
     */
    @PutMapping("/{playerId}/balance-subtract")
    public ResponseEntity<ApiResponse<PlayerStats>> subtractBalance(
            @PathVariable String playerId,
            @RequestParam Double amount) {
        log.info("Subtracting balance for player: {} - amount: {}", playerId, amount);
        try {
            PlayerStats stats = playerStatsService.subtractBalance(playerId, amount);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to subtract balance", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to subtract balance: " + e.getMessage()));
        }
    }

    /**
     * 获取击杀排名
     * GET /api/v1/stats/rankings/kills?page=0&size=10
     */
    @GetMapping("/rankings/kills")
    public ResponseEntity<ApiResponse<PageResponse<List<PlayerStats>>>> getKillRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching kill ranking - page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PlayerStats> ranking = playerStatsService.getKillRanking(pageable);

            PageResponse<List<PlayerStats>> pageResponse = PageResponse.<List<PlayerStats>>builder()
                    .pageNumber(ranking.getNumber())
                    .pageSize(ranking.getSize())
                    .totalPages(ranking.getTotalPages())
                    .totalElements(ranking.getTotalElements())
                    .content(ranking.getContent())
                    .last(ranking.isLast())
                    .first(ranking.isFirst())
                    .hasNext(ranking.hasNext())
                    .hasPrevious(ranking.hasPrevious())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("Failed to fetch kill ranking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch kill ranking"));
        }
    }

    /**
     * 获取财富排名
     * GET /api/v1/stats/rankings/wealth?page=0&size=10
     */
    @GetMapping("/rankings/wealth")
    public ResponseEntity<ApiResponse<PageResponse<List<PlayerStats>>>> getWealthRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching wealth ranking - page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PlayerStats> ranking = playerStatsService.getWealthRanking(pageable);

            PageResponse<List<PlayerStats>> pageResponse = PageResponse.<List<PlayerStats>>builder()
                    .pageNumber(ranking.getNumber())
                    .pageSize(ranking.getSize())
                    .totalPages(ranking.getTotalPages())
                    .totalElements(ranking.getTotalElements())
                    .content(ranking.getContent())
                    .last(ranking.isLast())
                    .first(ranking.isFirst())
                    .hasNext(ranking.hasNext())
                    .hasPrevious(ranking.hasPrevious())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("Failed to fetch wealth ranking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch wealth ranking"));
        }
    }

    /**
     * 获取伤害排名
     * GET /api/v1/stats/rankings/damage?page=0&size=10
     */
    @GetMapping("/rankings/damage")
    public ResponseEntity<ApiResponse<PageResponse<List<PlayerStats>>>> getDamageRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching damage ranking - page: {}, size: {}", page, size);
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<PlayerStats> ranking = playerStatsService.getDamageRanking(pageable);

            PageResponse<List<PlayerStats>> pageResponse = PageResponse.<List<PlayerStats>>builder()
                    .pageNumber(ranking.getNumber())
                    .pageSize(ranking.getSize())
                    .totalPages(ranking.getTotalPages())
                    .totalElements(ranking.getTotalElements())
                    .content(ranking.getContent())
                    .last(ranking.isLast())
                    .first(ranking.isFirst())
                    .hasNext(ranking.hasNext())
                    .hasPrevious(ranking.hasPrevious())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("Failed to fetch damage ranking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch damage ranking"));
        }
    }

    /**
     * 获取新玩家列表
     * GET /api/v1/stats/new-players?limit=10
     */
    @GetMapping("/new-players")
    public ResponseEntity<ApiResponse<List<PlayerStats>>> getNewPlayers(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching {} new players", limit);
        try {
            List<PlayerStats> players = playerStatsService.getNewPlayers(PageRequest.of(0, limit)).getContent();
            return ResponseEntity.ok(ApiResponse.success(players));
        } catch (Exception e) {
            log.error("Failed to fetch new players", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch new players"));
        }
    }

    /**
     * 获取富人榜
     * GET /api/v1/stats/richest-players?limit=10
     */
    @GetMapping("/richest-players")
    public ResponseEntity<ApiResponse<List<PlayerStats>>> getRichestPlayers(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching {} richest players", limit);
        try {
            List<PlayerStats> players = playerStatsService.getRichestPlayers(PageRequest.of(0, limit)).getContent();
            return ResponseEntity.ok(ApiResponse.success(players));
        } catch (Exception e) {
            log.error("Failed to fetch richest players", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch richest players"));
        }
    }

    /**
     * 获取活跃玩家列表
     * GET /api/v1/stats/active-players?limit=10
     */
    @GetMapping("/active-players")
    public ResponseEntity<ApiResponse<List<PlayerStats>>> getMostActivePlayers(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching {} most active players", limit);
        try {
            List<PlayerStats> players = playerStatsService.getMostActivePlayers(PageRequest.of(0, limit)).getContent();
            return ResponseEntity.ok(ApiResponse.success(players));
        } catch (Exception e) {
            log.error("Failed to fetch active players", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch active players"));
        }
    }

    /**
     * 按名称搜索玩家
     * GET /api/v1/stats/search?keyword=xxx&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<List<PlayerStats>>>> searchPlayerByName(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Searching players by name: {} - page: {}, size: {}", keyword, page, size);
        try {
            List<PlayerStats> results = playerStatsService.searchPlayerByName(keyword);

            // Manual pagination of the results
            int totalElements = results.size();
            int totalPages = (totalElements + size - 1) / size;
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);

            List<PlayerStats> pageContent = startIndex < totalElements ?
                    results.subList(startIndex, endIndex) : new ArrayList<>();

            PageResponse<List<PlayerStats>> pageResponse = PageResponse.<List<PlayerStats>>builder()
                    .pageNumber(page)
                    .pageSize(size)
                    .totalPages(totalPages)
                    .totalElements(totalElements)
                    .content(pageContent)
                    .last(page >= totalPages - 1)
                    .first(page == 0)
                    .hasNext(page < totalPages - 1)
                    .hasPrevious(page > 0)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } catch (Exception e) {
            log.error("Failed to search players", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to search players"));
        }
    }

    /**
     * 获取玩家财富等级
     * GET /api/v1/stats/{playerId}/wealth-level
     */
    @GetMapping("/{playerId}/wealth-level")
    public ResponseEntity<ApiResponse<String>> getPlayerWealthLevel(@PathVariable String playerId) {
        log.info("Fetching wealth level for player: {}", playerId);
        try {
            String level = playerStatsService.getPlayerWealthLevel(playerId);
            return ResponseEntity.ok(ApiResponse.success(level));
        } catch (Exception e) {
            log.error("Failed to fetch wealth level", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch wealth level"));
        }
    }

    /**
     * 获取玩家平均伤害/战斗
     * GET /api/v1/stats/{playerId}/average-damage-per-battle
     */
    @GetMapping("/{playerId}/average-damage-per-battle")
    public ResponseEntity<ApiResponse<Double>> getPlayerAverageDamagePerBattle(
            @PathVariable String playerId) {
        log.info("Fetching average damage per battle for player: {}", playerId);
        try {
            Double avgDamage = playerStatsService.getPlayerAverageDamagePerBattle(playerId);
            return ResponseEntity.ok(ApiResponse.success(avgDamage));
        } catch (Exception e) {
            log.error("Failed to fetch average damage per battle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch average damage"));
        }
    }

    /**
     * 获取玩家平均伤害/击杀
     * GET /api/v1/stats/{playerId}/average-damage-per-kill
     */
    @GetMapping("/{playerId}/average-damage-per-kill")
    public ResponseEntity<ApiResponse<Double>> getPlayerAverageDamagePerKill(
            @PathVariable String playerId) {
        log.info("Fetching average damage per kill for player: {}", playerId);
        try {
            Double avgDamage = playerStatsService.getPlayerAverageDamagePerKill(playerId);
            return ResponseEntity.ok(ApiResponse.success(avgDamage));
        } catch (Exception e) {
            log.error("Failed to fetch average damage per kill", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch average damage"));
        }
    }

    /**
     * 统计总玩家数
     * GET /api/v1/stats/count/total
     */
    @GetMapping("/count/total")
    public ResponseEntity<ApiResponse<Long>> countTotalPlayers() {
        log.info("Counting total players");
        try {
            Long count = playerStatsService.countTotalPlayers();
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to count total players", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to count players"));
        }
    }

    /**
     * 统计击杀玩家数
     * GET /api/v1/stats/count/with-kills
     */
    @GetMapping("/count/with-kills")
    public ResponseEntity<ApiResponse<Long>> countPlayersWithKills() {
        log.info("Counting players with kills");
        try {
            Long count = playerStatsService.countPlayersWithKills();
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to count players with kills", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to count players"));
        }
    }

    /**
     * 获取所有玩家总伤害
     * GET /api/v1/stats/total-damage
     */
    @GetMapping("/total-damage")
    public ResponseEntity<ApiResponse<Double>> getTotalDamageAcrossAllPlayers() {
        log.info("Fetching total damage across all players");
        try {
            Double totalDamage = playerStatsService.getTotalDamageAcrossAllPlayers();
            return ResponseEntity.ok(ApiResponse.success(totalDamage));
        } catch (Exception e) {
            log.error("Failed to fetch total damage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch total damage"));
        }
    }

    /**
     * 获取平均Boss击杀数
     * GET /api/v1/stats/average-kills
     */
    @GetMapping("/average-kills")
    public ResponseEntity<ApiResponse<Double>> getAverageBossKills() {
        log.info("Fetching average boss kills");
        try {
            Double avgKills = playerStatsService.getAverageBossKills();
            return ResponseEntity.ok(ApiResponse.success(avgKills));
        } catch (Exception e) {
            log.error("Failed to fetch average kills", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch average kills"));
        }
    }

    /**
     * 获取最高余额
     * GET /api/v1/stats/max-balance
     */
    @GetMapping("/max-balance")
    public ResponseEntity<ApiResponse<Double>> getMaxBalance() {
        log.info("Fetching max balance");
        try {
            Double maxBalance = playerStatsService.getMaxBalance();
            return ResponseEntity.ok(ApiResponse.success(maxBalance));
        } catch (Exception e) {
            log.error("Failed to fetch max balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Failed to fetch max balance"));
        }
    }

    /**
     * 更新所有玩家排名
     * POST /api/v1/stats/update-rankings
     */
    @PostMapping("/update-rankings")
    public ResponseEntity<ApiResponse<Void>> updateAllPlayerRankings() {
        log.info("Updating all player rankings");
        try {
            playerStatsService.updateAllPlayerRankings();
            return ResponseEntity.ok(ApiResponse.success());
        } catch (Exception e) {
            log.error("Failed to update rankings", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(400, "Failed to update rankings: " + e.getMessage()));
        }
    }
}
