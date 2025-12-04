package com.xiancore.systems.boss.teleport;

import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * 传送特效和动画系统
 * 在传送时播放各种视觉和听觉效果
 *
 * 支持:
 * - 粒子效果
 * - 音效播放
 * - 动画显示
 * - 倒计时显示
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
public class TeleportAnimation {

    // ==================== 动画效果 ====================

    /**
     * 播放传送前动画
     *
     * @param player 玩家
     */
    public void playPreTeleportAnimation(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            // 播放粒子效果
            playParticleEffect(player, ParticleType.TELEPORT_START);

            // 播放音效
            playSoundEffect(player, SoundType.TELEPORT_START);

            // 发送标题提示
            sendActionBar(player, "&a正在传送...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放倒计时动画
     *
     * @param player 玩家
     * @param seconds 倒计时秒数
     */
    public void playCountdownAnimation(Player player, int seconds) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            // 根据倒计时显示不同的粒子
            if (seconds == 3) {
                playParticleEffect(player, ParticleType.COUNTDOWN_3);
                playSoundEffect(player, SoundType.COUNTDOWN);
            } else if (seconds == 2) {
                playParticleEffect(player, ParticleType.COUNTDOWN_2);
                playSoundEffect(player, SoundType.COUNTDOWN);
            } else if (seconds == 1) {
                playParticleEffect(player, ParticleType.COUNTDOWN_1);
                playSoundEffect(player, SoundType.COUNTDOWN_FINAL);
            }

            // 显示倒计时数字
            sendActionBar(player, "&e传送倒计时: " + seconds + "秒");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放传送成功动画
     *
     * @param player 玩家
     */
    public void playTeleportSuccessAnimation(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            // 播放到达动画
            playParticleEffect(player, ParticleType.TELEPORT_ARRIVE);

            // 播放成功音效
            playSoundEffect(player, SoundType.TELEPORT_SUCCESS);

            // 发送成功消息
            sendActionBar(player, "&a传送成功!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 播放传送失败动画
     *
     * @param player 玩家
     * @param reason 失败原因
     */
    public void playTeleportFailureAnimation(Player player, String reason) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            // 播放失败动画
            playParticleEffect(player, ParticleType.FAILURE);

            // 播放失败音效
            playSoundEffect(player, SoundType.FAILURE);

            // 发送失败消息
            sendActionBar(player, "&c传送失败: " + reason);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 粒子效果 ====================

    /**
     * 播放粒子效果
     *
     * @param player 玩家
     * @param type 效果类型
     */
    private void playParticleEffect(Player player, ParticleType type) {
        if (player == null || player.getLocation() == null) {
            return;
        }

        try {
            switch (type) {
                case TELEPORT_START:
                    // 传送前: 蓝色粒子环
                    player.getWorld().spawnParticle(
                        Particle.ENCHANTMENT_TABLE,
                        player.getLocation(),
                        20,      // 数量
                        0.5, 0.5, 0.5  // 偏移
                    );
                    break;

                case TELEPORT_ARRIVE:
                    // 到达时: 绿色粒子环
                    player.getWorld().spawnParticle(
                        Particle.VILLAGER_HAPPY,
                        player.getLocation(),
                        30,
                        0.5, 1.0, 0.5
                    );
                    break;

                case COUNTDOWN_3:
                case COUNTDOWN_2:
                case COUNTDOWN_1:
                    // 倒计时: 黄色粒子
                    player.getWorld().spawnParticle(
                        Particle.VILLAGER_ANGRY,
                        player.getLocation().add(0, 1, 0),
                        10,
                        0.3, 0.3, 0.3
                    );
                    break;

                case FAILURE:
                    // 失败: 红色粒子
                    player.getWorld().spawnParticle(
                        Particle.FLAME,
                        player.getLocation(),
                        15,
                        0.5, 0.5, 0.5
                    );
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 音效播放 ====================

    /**
     * 播放音效
     *
     * @param player 玩家
     * @param type 音效类型
     */
    private void playSoundEffect(Player player, SoundType type) {
        if (player == null || player.getLocation() == null) {
            return;
        }

        try {
            switch (type) {
                case TELEPORT_START:
                    // 传送开始: Endereye音效
                    player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_ENDER_EYE_LAUNCH, 1.0f, 1.0f);
                    break;

                case TELEPORT_SUCCESS:
                    // 成功: Portal音效
                    player.playSound(player.getLocation(),
                        org.bukkit.Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 1.0f);
                    break;

                case COUNTDOWN:
                    // 倒计时: 单声
                    player.playSound(player.getLocation(),
                        org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 2.0f);
                    break;

                case COUNTDOWN_FINAL:
                    // 最后倒计时: 双声
                    player.playSound(player.getLocation(),
                        org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                    break;

                case FAILURE:
                    // 失败: Explosion音效
                    player.playSound(player.getLocation(),
                        org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.8f);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 消息显示 ====================

    /**
     * 发送行动栏消息
     *
     * @param player 玩家
     * @param message 消息
     */
    private void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline()) {
            return;
        }

        try {
            // 转换颜色代码
            String formattedMessage = message
                .replace("&a", "§a")
                .replace("&e", "§e")
                .replace("&c", "§c");

            // 在实际应用中会使用Spigot API或Adventure API
            player.sendMessage(formattedMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 粒子类型枚举 ====================

    /**
     * 粒子效果类型
     */
    private enum ParticleType {
        TELEPORT_START,   // 传送开始
        TELEPORT_ARRIVE,  // 传送到达
        COUNTDOWN_3,      // 倒计时3
        COUNTDOWN_2,      // 倒计时2
        COUNTDOWN_1,      // 倒计时1
        FAILURE           // 失败
    }

    /**
     * 音效类型
     */
    private enum SoundType {
        TELEPORT_START,   // 传送开始
        TELEPORT_SUCCESS, // 传送成功
        COUNTDOWN,        // 倒计时
        COUNTDOWN_FINAL,  // 最后倒计时
        FAILURE           // 失败
    }
}
