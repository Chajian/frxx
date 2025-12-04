package com.xiancore.core.event;

/**
 * XianCore 事件监听器接口
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public interface XianEventListener {

    /**
     * 处理事件
     *
     * @param event 事件对象
     */
    void onEvent(XianEvent event);

    /**
     * 获取监听器优先级
     * 数值越大，优先级越高
     *
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
}
