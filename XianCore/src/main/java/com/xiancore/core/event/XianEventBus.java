package com.xiancore.core.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * XianCore 事件总线
 * 提供自定义事件系统，支持异步回调和事件优先级
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class XianEventBus {

    private static final Logger LOGGER = Logger.getLogger("XianCore");

    // 事件监听器映射: 事件名 -> 监听器列表
    private final Map<String, List<XianEventListener>> listeners = new ConcurrentHashMap<>();

    // 事件统计
    private final Map<String, Long> eventStats = new ConcurrentHashMap<>();

    /**
     * 注册事件监听器
     *
     * @param eventName 事件名称
     * @param listener  监听器
     */
    public void registerListener(String eventName, XianEventListener listener) {
        listeners.computeIfAbsent(eventName, k -> new ArrayList<>()).add(listener);
        LOGGER.info(String.format("注册事件监听器: %s", eventName));
    }

    /**
     * 注册简单的事件处理器（玩家事件）
     *
     * @param eventName 事件名称
     * @param handler   处理器
     */
    public void registerEvent(String eventName, BiConsumer<Player, Object> handler) {
        registerListener(eventName, new XianEventListener() {
            @Override
            public void onEvent(XianEvent event) {
                if (event.getPlayer() != null) {
                    handler.accept(event.getPlayer(), event.getData());
                }
            }

            @Override
            public int getPriority() {
                return 0;
            }
        });
    }

    /**
     * 触发事件
     *
     * @param eventName 事件名称
     * @param event     事件对象
     */
    public void fireEvent(String eventName, XianEvent event) {
        List<XianEventListener> eventListeners = listeners.get(eventName);
        if (eventListeners == null || eventListeners.isEmpty()) {
            return;
        }

        // 按优先级排序
        eventListeners.sort(Comparator.comparingInt(XianEventListener::getPriority).reversed());

        // 执行监听器
        for (XianEventListener listener : eventListeners) {
            try {
                listener.onEvent(event);
                if (event.isCancelled()) {
                    break;
                }
            } catch (Exception e) {
                LOGGER.severe(String.format("执行事件监听器时发生错误: %s - %s", eventName, e.getMessage()));
                e.printStackTrace();
            }
        }

        // 更新统计
        eventStats.merge(eventName, 1L, Long::sum);
    }

    /**
     * 触发简单事件（玩家 + 数据）
     *
     * @param eventName 事件名称
     * @param player    玩家
     * @param data      数据
     */
    public void fireEvent(String eventName, Player player, Object data) {
        XianEvent event = new XianEvent(eventName, player, data);
        fireEvent(eventName, event);
    }

    /**
     * 取消注册事件监听器
     *
     * @param eventName 事件名称
     */
    public void unregisterListeners(String eventName) {
        listeners.remove(eventName);
        LOGGER.info(String.format("取消注册事件监听器: %s", eventName));
    }

    /**
     * 获取事件统计信息
     *
     * @return 事件统计
     */
    public Map<String, Long> getEventStats() {
        return new HashMap<>(eventStats);
    }

    /**
     * 清空事件统计
     */
    public void clearStats() {
        eventStats.clear();
    }

    /**
     * 获取已注册的事件列表
     *
     * @return 事件名称列表
     */
    public Set<String> getRegisteredEvents() {
        return new HashSet<>(listeners.keySet());
    }
}
