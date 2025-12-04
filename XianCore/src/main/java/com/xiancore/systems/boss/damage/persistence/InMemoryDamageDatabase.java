package com.xiancore.systems.boss.damage.persistence;

import com.xiancore.systems.boss.damage.DamageHistory;
import com.xiancore.systems.boss.damage.DamageStatistics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 内存数据库实现
 * 将伤害历史数据存储在内存中
 *
 * 特性:
 * - 快速的读写性能
 * - 线程安全
 * - 服务器重启后数据丢失
 * - 适合中小型服务器
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-13
 */
public class InMemoryDamageDatabase implements DamageDatabase {

    // ==================== 数据存储 ====================
    /** 历史记录存储 */
    private final List<DamageHistory> histories = new CopyOnWriteArrayList<>();

    /** 按Boss UUID索引 */
    private final Map<UUID, List<DamageHistory>> bossByUUID = new ConcurrentHashMap<>();

    /** 按玩家UUID索引 */
    private final Map<UUID, List<DamageHistory>> playerByUUID = new ConcurrentHashMap<>();

    /** 按记录ID索引 */
    private final Map<String, DamageHistory> byId = new ConcurrentHashMap<>();

    /** 连接状态 */
    private volatile boolean connected = false;

    /** 记录ID生成器 */
    private volatile long idGenerator = 0;

    // ==================== 初始化和关闭 ====================

    @Override
    public void initialize() throws Exception {
        if (connected) {
            return;
        }

        try {
            connected = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化内存数据库失败", e);
        }
    }

    @Override
    public void shutdown() throws Exception {
        connected = false;
        // 内存数据库不需要关闭连接
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    // ==================== 保存操作 ====================

    @Override
    public synchronized boolean saveHistory(DamageHistory history) {
        if (history == null || !connected) {
            return false;
        }

        try {
            // 生成唯一ID
            String id = generateId();
            history.getMetadata().put("_id", id);
            history.getMetadata().put("_saveTime", System.currentTimeMillis());

            // 添加到主列表
            histories.add(history);

            // 添加到索引
            byId.put(id, history);

            // 按Boss UUID索引
            bossByUUID.computeIfAbsent(history.getBossUUID(), k -> new CopyOnWriteArrayList<>())
                .add(history);

            // 按玩家UUID索引
            for (UUID playerUUID : history.getParticipants()) {
                playerByUUID.computeIfAbsent(playerUUID, k -> new CopyOnWriteArrayList<>())
                    .add(history);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public synchronized int saveHistories(List<DamageHistory> histories) {
        int count = 0;
        for (DamageHistory history : histories) {
            if (saveHistory(history)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public CompletableFuture<Boolean> saveHistoryAsync(DamageHistory history) {
        return CompletableFuture.supplyAsync(() -> saveHistory(history));
    }

    @Override
    public synchronized boolean updateHistory(DamageHistory history) {
        if (history == null || !connected) {
            return false;
        }

        try {
            String id = (String) history.getMetadata().get("_id");
            if (id == null) {
                return saveHistory(history);
            }

            DamageHistory existing = byId.get(id);
            if (existing == null) {
                return false;
            }

            // 更新元数据
            existing.getMetadata().putAll(history.getMetadata());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 查询操作 ====================

    @Override
    public List<DamageHistory> queryByBossUUID(UUID bossUUID) {
        if (!connected) {
            return new ArrayList<>();
        }
        return new ArrayList<>(bossByUUID.getOrDefault(bossUUID, new ArrayList<>()));
    }

    @Override
    public List<DamageHistory> queryByPlayerUUID(UUID playerUUID) {
        if (!connected) {
            return new ArrayList<>();
        }
        return new ArrayList<>(playerByUUID.getOrDefault(playerUUID, new ArrayList<>()));
    }

    @Override
    public List<DamageHistory> queryByTimeRange(long startTime, long endTime) {
        if (!connected) {
            return new ArrayList<>();
        }

        return histories.stream()
            .filter(h -> {
                long time = h.getRecordTime();
                return time >= startTime && time <= endTime;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<DamageHistory> queryAll() {
        if (!connected) {
            return new ArrayList<>();
        }
        return new ArrayList<>(histories);
    }

    @Override
    public List<DamageHistory> queryAll(int offset, int limit) {
        if (!connected) {
            return new ArrayList<>();
        }

        return histories.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public DamageHistory queryById(String id) {
        if (!connected) {
            return null;
        }
        return byId.get(id);
    }

    @Override
    public long getTotalCount() {
        return connected ? histories.size() : 0;
    }

    @Override
    public long getCountByBossUUID(UUID bossUUID) {
        if (!connected) {
            return 0;
        }
        return bossByUUID.getOrDefault(bossUUID, new ArrayList<>()).size();
    }

    // ==================== 统计操作 ====================

    @Override
    public double getTotalDamageForPlayer(UUID playerUUID) {
        if (!connected) {
            return 0.0;
        }

        return playerByUUID.getOrDefault(playerUUID, new ArrayList<>())
            .stream()
            .mapToDouble(h -> h.getPlayerDamage(playerUUID))
            .sum();
    }

    @Override
    public Map<UUID, Double> getTopDamagers(int limit) {
        if (!connected) {
            return new HashMap<>();
        }

        Map<UUID, Double> damageMap = new HashMap<>();

        for (DamageHistory history : histories) {
            for (UUID playerUUID : history.getParticipants()) {
                double damage = history.getPlayerDamage(playerUUID);
                damageMap.put(playerUUID, damageMap.getOrDefault(playerUUID, 0.0) + damage);
            }
        }

        return damageMap.entrySet()
            .stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (e1, e2) -> e1, LinkedHashMap::new));
    }

    @Override
    public int getBossKillCount(UUID bossUUID) {
        if (!connected) {
            return 0;
        }
        return (int) bossByUUID.getOrDefault(bossUUID, new ArrayList<>()).size();
    }

    @Override
    public double getAverageDamage() {
        if (!connected || histories.isEmpty()) {
            return 0.0;
        }

        return histories.stream()
            .mapToDouble(DamageHistory::getTotalDamage)
            .average()
            .orElse(0.0);
    }

    // ==================== 删除操作 ====================

    @Override
    public synchronized int deleteByBossUUID(UUID bossUUID) {
        if (!connected) {
            return 0;
        }

        List<DamageHistory> toDelete = bossByUUID.remove(bossUUID);
        if (toDelete == null) {
            return 0;
        }

        int count = 0;
        for (DamageHistory history : toDelete) {
            if (histories.remove(history)) {
                count++;
                byId.remove((String) history.getMetadata().get("_id"));
            }
        }

        return count;
    }

    @Override
    public synchronized int deleteByPlayerUUID(UUID playerUUID) {
        if (!connected) {
            return 0;
        }

        List<DamageHistory> toDelete = playerByUUID.remove(playerUUID);
        if (toDelete == null) {
            return 0;
        }

        int count = 0;
        for (DamageHistory history : toDelete) {
            if (histories.remove(history)) {
                count++;
                byId.remove((String) history.getMetadata().get("_id"));
            }
        }

        return count;
    }

    @Override
    public synchronized int deleteBeforeTime(long beforeTime) {
        if (!connected) {
            return 0;
        }

        List<DamageHistory> toDelete = histories.stream()
            .filter(h -> h.getRecordTime() < beforeTime)
            .collect(Collectors.toList());

        int count = 0;
        for (DamageHistory history : toDelete) {
            if (histories.remove(history)) {
                count++;
                byId.remove((String) history.getMetadata().get("_id"));

                // 清理索引
                bossByUUID.computeIfPresent(history.getBossUUID(), (k, v) -> {
                    v.remove(history);
                    return v.isEmpty() ? null : v;
                });
            }
        }

        return count;
    }

    @Override
    public synchronized boolean deleteById(String id) {
        if (!connected) {
            return false;
        }

        DamageHistory history = byId.remove(id);
        if (history == null) {
            return false;
        }

        histories.remove(history);
        return true;
    }

    @Override
    public synchronized void clearAll() {
        if (connected) {
            histories.clear();
            bossByUUID.clear();
            playerByUUID.clear();
            byId.clear();
        }
    }

    // ==================== 导入导出 ====================

    @Override
    public String exportAsJson() {
        if (!connected) {
            return "[]";
        }

        // 简单的JSON导出 (生产环境应使用Gson或Jackson)
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < histories.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(historyToJson(histories.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int importFromJson(String json) {
        // 简单的JSON导入 (生产环境应使用Gson或Jackson)
        return 0;
    }

    @Override
    public String exportAsYaml() {
        if (!connected) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (DamageHistory history : histories) {
            sb.append(historyToYaml(history)).append("\n---\n");
        }
        return sb.toString();
    }

    @Override
    public int importFromYaml(String yaml) {
        // 简单的YAML导入 (生产环境应使用SnakeYAML)
        return 0;
    }

    // ==================== 性能和优化 ====================

    @Override
    public synchronized int cleanup(int daysToKeep) {
        if (!connected) {
            return 0;
        }

        long beforeTime = System.currentTimeMillis() - (long) daysToKeep * 24 * 60 * 60 * 1000;
        return deleteBeforeTime(beforeTime);
    }

    @Override
    public void optimize() {
        // 内存数据库无需优化
    }

    @Override
    public String getStatistics() {
        if (!connected) {
            return "{}";
        }

        return String.format(
            "{\"total\": %d, \"bosses\": %d, \"players\": %d}",
            histories.size(),
            bossByUUID.size(),
            playerByUUID.size()
        );
    }

    @Override
    public boolean backup(String backupPath) {
        // 内存数据库无法备份 (内存中的数据)
        return false;
    }

    @Override
    public boolean restore(String backupPath) {
        // 内存数据库无法恢复
        return false;
    }

    // ==================== 内部方法 ====================

    /**
     * 生成唯一ID
     */
    private synchronized String generateId() {
        return "dmg_" + System.currentTimeMillis() + "_" + (idGenerator++);
    }

    /**
     * 将历史记录转换为JSON
     */
    private String historyToJson(DamageHistory history) {
        return String.format(
            "{\"boss\":\"%s\",\"totalDamage\":%f,\"participants\":%d}",
            history.getBossUUID(),
            history.getTotalDamage(),
            history.getParticipantCount()
        );
    }

    /**
     * 将历史记录转换为YAML
     */
    private String historyToYaml(DamageHistory history) {
        return String.format(
            "boss_uuid: %s\ntotal_damage: %.2f\nparticipants: %d\ntime: %d",
            history.getBossUUID(),
            history.getTotalDamage(),
            history.getParticipantCount(),
            history.getRecordTime()
        );
    }
}
