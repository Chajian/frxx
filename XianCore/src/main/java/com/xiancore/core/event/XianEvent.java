package com.xiancore.core.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

/**
 * XianCore 自定义事件基类
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
@Setter
public class XianEvent {

    private final String eventName;
    private final Player player;
    private final Object data;
    private final long timestamp;
    private boolean cancelled = false;

    public XianEvent(String eventName, Player player, Object data) {
        this.eventName = eventName;
        this.player = player;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 取消事件
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * 获取事件触发时间
     */
    public long getTimestamp() {
        return timestamp;
    }
}
