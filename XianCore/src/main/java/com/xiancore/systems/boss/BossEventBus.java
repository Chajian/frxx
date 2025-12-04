package com.xiancore.systems.boss;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Boss系统事件总线
 * 用于管理Boss相关的事件发送和订阅
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class BossEventBus {

    // ==================== 事件监听器 ====================

    private final List<Consumer<BossEvent>> globalListeners = new CopyOnWriteArrayList<>();
    private final Map<Class<? extends BossEvent>, List<Consumer<BossEvent>>> typeListeners = new HashMap<>();

    // ==================== 事件发送 ====================

    /**
     * 发送事件给所有监听器
     *
     * @param event 要发送的事件
     */
    public void publishEvent(BossEvent event) {
        emit(event);
    }

    /**
     * 发送事件给所有监听器
     *
     * @param event 要发送的事件
     */
    public void emit(BossEvent event) {
        if (event == null) {
            return;
        }

        // 发送给全局监听器
        for (Consumer<BossEvent> listener : globalListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 发送给类型特定的监听器
        List<Consumer<BossEvent>> listeners = typeListeners.get(event.getClass());
        if (listeners != null) {
            for (Consumer<BossEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ==================== 事件订阅 ====================

    /**
     * 订阅所有事件类型
     *
     * @param listener 监听器
     */
    public void subscribe(Consumer<BossEvent> listener) {
        if (listener != null) {
            globalListeners.add(listener);
        }
    }

    /**
     * 订阅特定事件类型
     *
     * @param eventType 事件类型
     * @param listener 监听器
     */
    @SuppressWarnings("unchecked")
    public <T extends BossEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        if (eventType == null || listener == null) {
            return;
        }

        typeListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add((Consumer<BossEvent>) listener);
    }

    // ==================== 事件取消订阅 ====================

    /**
     * 取消订阅
     *
     * @param listener 监听器
     */
    public void unsubscribe(Consumer<BossEvent> listener) {
        if (listener != null) {
            globalListeners.remove(listener);
        }
    }

    /**
     * 取消订阅特定事件类型
     *
     * @param eventType 事件类型
     * @param listener 监听器
     */
    @SuppressWarnings("unchecked")
    public <T extends BossEvent> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        if (eventType == null || listener == null) {
            return;
        }

        List<Consumer<BossEvent>> listeners = typeListeners.get(eventType);
        if (listeners != null) {
            listeners.remove((Consumer<BossEvent>) listener);
        }
    }

    // ==================== 清理 ====================

    /**
     * 清空所有监听器
     */
    public void clear() {
        globalListeners.clear();
        typeListeners.clear();
    }

    /**
     * 获取监听器数量
     *
     * @return 监听器数量
     */
    public int getListenerCount() {
        return globalListeners.size() + typeListeners.values().stream().mapToInt(List::size).sum();
    }

    // ==================== 基础事件类 ====================

    /**
     * Boss事件基类
     */
    public static abstract class BossEvent {
        private final long timestamp = System.currentTimeMillis();

        public long getTimestamp() {
            return timestamp;
        }
    }
}
