package com.xiancore.systems.skill.listeners;

import com.xiancore.XianCore;
import com.xiancore.systems.skill.items.SkillBook;
import com.xiancore.systems.skill.items.SkillBookFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 功法秘籍监听器
 * 监听玩家使用功法秘籍学习功法
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillBookListener implements Listener {

    private final XianCore plugin;

    public SkillBookListener(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 监听玩家右键使用功法秘籍
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerUseSkillBook(PlayerInteractEvent event) {
        // 只处理右键交互
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // 检查物品是否为空
        if (item == null || item.getType().isAir()) {
            return;
        }

        // 检查是否为功法秘籍
        if (!SkillBookFactory.isSkillBook(item)) {
            return;
        }

        // 取消事件，防止放置方块等默认行为
        event.setCancelled(true);

        // 解析功法秘籍信息
        SkillBook skillBook = SkillBookFactory.fromItemStack(item);
        if (skillBook == null) {
            player.sendMessage("§c功法秘籍数据损坏！");
            plugin.getLogger().warning("玩家 " + player.getName() + " 使用了损坏的功法秘籍");
            return;
        }

        String skillId = skillBook.getSkillId();
        int skillLevel = skillBook.getSkillLevel();

        // 调用功法系统学习功法
        boolean success = plugin.getSkillSystem().learnSkill(player, skillId);

        if (success) {
            // 学习成功，移除物品
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // 播放学习成功音效
            player.playSound(player.getLocation(), 
                org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            
            // 显示粒子效果
            player.getWorld().spawnParticle(
                org.bukkit.Particle.ENCHANTMENT_TABLE, 
                player.getLocation().add(0, 1, 0), 
                50, 0.5, 0.5, 0.5, 0.1
            );

            plugin.getLogger().info("玩家 " + player.getName() + " 通过秘籍学习了功法: " + skillId);
        } else {
            // 学习失败（原因已在SkillSystem中提示）
            player.playSound(player.getLocation(), 
                org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
