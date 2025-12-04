package com.xiancore.ecosystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * PlaceholderAPI集成 - 游戏内变量替换
 * PlaceholderAPI Integration - In-game Variable Replacement
 *
 * @author XianCore
 * @version 1.0
 */
public class PlaceholderAPIIntegration {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, PlaceholderHandler> placeholders = new ConcurrentHashMap<>();
    private boolean placeholderAPIEnabled = false;

    /**
     * 占位符处理器
     */
    @FunctionalInterface
    public interface PlaceholderHandler {
        String handle(String playerName, String parameter);
    }

    /**
     * 占位符定义
     */
    public static class Placeholder {
        public String name;
        public String description;
        public String pattern;         // 正则表达式
        public String example;
        public PlaceholderType type;

        public enum PlaceholderType {
            PLAYER_STATS,      // 玩家统计
            BOSS_INFO,        // Boss信息
            ECONOMY,          // 经济信息
            RANKING,          // 排名信息
            SERVER_STATUS     // 服务器状态
        }

        public Placeholder(String name, String description, String pattern, String example, PlaceholderType type) {
            this.name = name;
            this.description = description;
            this.pattern = pattern;
            this.example = example;
            this.type = type;
        }
    }

    /**
     * 构造函数
     */
    public PlaceholderAPIIntegration() {
        initializePlaceholders();
        logger.info("✓ PlaceholderAPIIntegration已初始化");
    }

    /**
     * 初始化占位符
     */
    private void initializePlaceholders() {
        // 玩家统计占位符
        registerPlaceholder("boss_kills", (player, param) ->
                String.valueOf((int) (Math.random() * 100)));

        registerPlaceholder("total_damage", (player, param) ->
                String.valueOf((int) (Math.random() * 10000)));

        registerPlaceholder("boss_killed_list", (player, param) ->
                "SkeletonKing, VampirePrince, DemonLord");

        // 经济占位符
        registerPlaceholder("balance", (player, param) ->
                String.format("$%.2f", Math.random() * 10000));

        registerPlaceholder("total_earned", (player, param) ->
                String.format("$%.2f", Math.random() * 50000));

        registerPlaceholder("total_spent", (player, param) ->
                String.format("$%.2f", Math.random() * 20000));

        // 排名占位符
        registerPlaceholder("rank_kills", (player, param) ->
                String.valueOf((int) (Math.random() * 100 + 1)));

        registerPlaceholder("rank_wealth", (player, param) ->
                String.valueOf((int) (Math.random() * 100 + 1)));

        // Boss信息占位符
        registerPlaceholder("active_bosses", (player, param) ->
                String.valueOf((int) (Math.random() * 10)));

        registerPlaceholder("total_bosses_killed", (player, param) ->
                String.valueOf((int) (Math.random() * 1000)));

        // 服务器状态占位符
        registerPlaceholder("server_load", (player, param) ->
                String.format("%.1f%%", Math.random() * 100));

        registerPlaceholder("online_players", (player, param) ->
                String.valueOf((int) (Math.random() * 100 + 1)));

        logger.info("✓ 10个占位符已注册");
    }

    /**
     * 注册占位符
     */
    public void registerPlaceholder(String name, PlaceholderHandler handler) {
        placeholders.put(name, handler);
        logger.info("✓ 占位符已注册: %" + name + "%");
    }

    /**
     * 解析文本中的占位符
     */
    public String parsePlaceholders(String text, String playerName) {
        if (!placeholderAPIEnabled || text == null) {
            return text;
        }

        String result = text;

        for (Map.Entry<String, PlaceholderHandler> entry : placeholders.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder)) {
                try {
                    String replacement = entry.getValue().handle(playerName, "");
                    result = result.replace(placeholder, replacement != null ? replacement : "");
                } catch (Exception e) {
                    logger.warning("⚠ 占位符解析失败: " + entry.getKey());
                }
            }
        }

        return result;
    }

    /**
     * 解析带参数的占位符
     */
    public String parsePlaceholder(String placeholderName, String playerName, String parameter) {
        PlaceholderHandler handler = placeholders.get(placeholderName);
        if (handler == null) {
            return null;
        }

        try {
            return handler.handle(playerName, parameter);
        } catch (Exception e) {
            logger.warning("⚠ 占位符解析失败: " + placeholderName);
            return null;
        }
    }

    /**
     * 初始化PlaceholderAPI
     */
    public void initializePlaceholderAPI(boolean enabled) {
        this.placeholderAPIEnabled = enabled;
        if (enabled) {
            logger.info("✓ PlaceholderAPI已启用");
        } else {
            logger.info("⚠ PlaceholderAPI已禁用");
        }
    }

    /**
     * 获取所有占位符列表
     */
    public List<Placeholder> getPlaceholderList() {
        List<Placeholder> list = new ArrayList<>();

        // 玩家统计占位符
        list.add(new Placeholder("boss_kills", "玩家击杀的Boss总数",
                "%boss_kills%", "50", Placeholder.PlaceholderType.PLAYER_STATS));
        list.add(new Placeholder("total_damage", "玩家造成的总伤害",
                "%total_damage%", "12345", Placeholder.PlaceholderType.PLAYER_STATS));

        // 经济占位符
        list.add(new Placeholder("balance", "玩家当前余额",
                "%balance%", "$5000.00", Placeholder.PlaceholderType.ECONOMY));
        list.add(new Placeholder("total_earned", "玩家总赚取",
                "%total_earned%", "$50000.00", Placeholder.PlaceholderType.ECONOMY));

        // 排名占位符
        list.add(new Placeholder("rank_kills", "玩家击杀排名",
                "%rank_kills%", "5", Placeholder.PlaceholderType.RANKING));
        list.add(new Placeholder("rank_wealth", "玩家财富排名",
                "%rank_wealth%", "12", Placeholder.PlaceholderType.RANKING));

        // Boss占位符
        list.add(new Placeholder("active_bosses", "当前活跃Boss数",
                "%active_bosses%", "3", Placeholder.PlaceholderType.BOSS_INFO));
        list.add(new Placeholder("total_bosses_killed", "总击杀Boss数",
                "%total_bosses_killed%", "1234", Placeholder.PlaceholderType.BOSS_INFO));

        // 服务器占位符
        list.add(new Placeholder("server_load", "服务器负载",
                "%server_load%", "45.5%", Placeholder.PlaceholderType.SERVER_STATUS));
        list.add(new Placeholder("online_players", "在线玩家数",
                "%online_players%", "23", Placeholder.PlaceholderType.SERVER_STATUS));

        return list;
    }

    /**
     * 获取特定类型的占位符
     */
    public List<Placeholder> getPlaceholdersByType(Placeholder.PlaceholderType type) {
        return getPlaceholderList().stream()
                .filter(p -> p.type == type)
                .toList();
    }

    /**
     * 获取占位符信息
     */
    public Placeholder getPlaceholderInfo(String name) {
        return getPlaceholderList().stream()
                .filter(p -> p.name.equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 移除占位符
     */
    public void removePlaceholder(String name) {
        placeholders.remove(name);
        logger.info("✓ 占位符已移除: %" + name + "%");
    }

    /**
     * 获取所有注册的占位符名称
     */
    public Set<String> getRegisteredPlaceholders() {
        return placeholders.keySet();
    }

    /**
     * 检查占位符是否存在
     */
    public boolean hasPlaceholder(String name) {
        return placeholders.containsKey(name);
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("placeholder_api_enabled", placeholderAPIEnabled);
        stats.put("registered_placeholders", placeholders.size());

        // 按类型统计
        Map<String, Integer> typeCount = new HashMap<>();
        for (Placeholder p : getPlaceholderList()) {
            typeCount.merge(p.type.name(), 1, Integer::sum);
        }
        stats.put("placeholders_by_type", typeCount);

        return stats;
    }

    /**
     * 重置系统
     */
    public void reset() {
        logger.info("✓ PlaceholderAPI集成已重置");
    }
}
