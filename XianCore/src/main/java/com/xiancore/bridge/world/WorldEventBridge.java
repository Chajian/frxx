package com.xiancore.bridge.world;

import com.xiancore.XianCore;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 世界事件桥接系统
 * 负责跨服事件广播和数据同步（基于 Redis）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class WorldEventBridge {

    private final XianCore plugin;
    private JedisPool jedisPool;
    private boolean initialized = false;
    private boolean useRedis = false;

    public WorldEventBridge(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化世界事件桥接
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 检查是否启用 Redis
        useRedis = plugin.getConfig().getBoolean("redis.enabled", false);

        if (useRedis) {
            setupRedis();
        } else {
            plugin.getLogger().info("  §e! Redis 未启用，跨服功能将被禁用");
        }

        initialized = true;
        plugin.getLogger().info("  §a✓ 世界事件桥接初始化完成");
    }

    /**
     * 设置 Redis 连接
     */
    private void setupRedis() {
        try {
            String host = plugin.getConfig().getString("redis.host", "localhost");
            int port = plugin.getConfig().getInt("redis.port", 6379);
            String password = plugin.getConfig().getString("redis.password", "");

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);

            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000);
            }

            // 测试连接
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                plugin.getLogger().info("  §a✓ Redis 连接成功!");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("  §c✗ Redis 连接失败: " + e.getMessage());
            useRedis = false;
        }
    }

    /**
     * 广播事件到所有服务器
     *
     * @param channel 频道
     * @param message 消息
     */
    public void broadcast(String channel, String message) {
        if (!useRedis || jedisPool == null) {
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, message);
        } catch (Exception e) {
            plugin.getLogger().severe("广播消息失败: " + e.getMessage());
        }
    }

    /**
     * 关闭 Redis 连接
     */
    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            plugin.getLogger().info("  §a✓ Redis 连接已关闭");
        }
    }
}
