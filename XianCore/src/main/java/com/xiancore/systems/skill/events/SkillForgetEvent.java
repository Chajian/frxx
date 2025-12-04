package com.xiancore.systems.skill.events;

import com.xiancore.systems.skill.Skill;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * 功法遗忘事件
 * 在玩家遗忘功法时触发，可被取消
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
@Setter
public class SkillForgetEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    
    private final Skill skill;
    private final int skillLevel;
    private int refundedSkillPoints;
    private long refundedSpiritStones;
    private boolean cancelled;
    private String cancelReason;

    /**
     * 构造函数
     *
     * @param player               玩家
     * @param skill                被遗忘的功法
     * @param skillLevel           功法等级
     * @param refundedSkillPoints  返还的功法点
     * @param refundedSpiritStones 返还的灵石
     */
    public SkillForgetEvent(Player player, Skill skill, int skillLevel, 
                           int refundedSkillPoints, long refundedSpiritStones) {
        super(player);
        this.skill = skill;
        this.skillLevel = skillLevel;
        this.refundedSkillPoints = refundedSkillPoints;
        this.refundedSpiritStones = refundedSpiritStones;
        this.cancelled = false;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * 设置取消原因
     *
     * @param reason 原因
     */
    public void setCancelled(boolean cancelled, String reason) {
        this.cancelled = cancelled;
        this.cancelReason = reason;
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public Skill getSkill() {
        return skill;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public int getRefundedSkillPoints() {
        return refundedSkillPoints;
    }

    public long getRefundedSpiritStones() {
        return refundedSpiritStones;
    }

    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }

    public void setRefundedSkillPoints(int refundedSkillPoints) {
        this.refundedSkillPoints = refundedSkillPoints;
    }

    public void setRefundedSpiritStones(long refundedSpiritStones) {
        this.refundedSpiritStones = refundedSpiritStones;
    }
}

