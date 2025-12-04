package com.xiancore.systems.tribulation;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 天劫系统
 * 负责管理天劫触发、生成、奖励等功能
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class TribulationSystem {

    private final XianCore plugin;
    private final Map<UUID, Tribulation> activeTribulations;  // 激活的天劫
    private final Map<UUID, Long> tribulationCooldowns;       // 天劫冷却
    private boolean initialized = false;
    private BukkitRunnable tribulationTask;

    public TribulationSystem(XianCore plugin) {
        this.plugin = plugin;
        this.activeTribulations = new ConcurrentHashMap<>();
        this.tribulationCooldowns = new ConcurrentHashMap<>();
    }

    /**
     * 初始化天劫系统
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        // 加载所有在线玩家的活跃天劫
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Tribulation tribulation = plugin.getDataManager().loadActiveTribulation(player.getUniqueId());
            if (tribulation != null) {
                activeTribulations.put(player.getUniqueId(), tribulation);
                plugin.getLogger().info("§7加载玩家 " + player.getName() + " 的天劫数据");
            }
        }

        // 启动天劫处理任务
        startTribulationTask();

        initialized = true;
        plugin.getLogger().info("  §a✓ 天劫系统初始化完成");
    }

    /**
     * 启动天劫处理任务
     */
    private void startTribulationTask() {
        tribulationTask = new BukkitRunnable() {
            @Override
            public void run() {
                processTribulations();
            }
        };

        // 每秒检查一次
        tribulationTask.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * 处理所有激活的天劫
     */
    private void processTribulations() {
        Iterator<Map.Entry<UUID, Tribulation>> iterator = activeTribulations.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Tribulation> entry = iterator.next();
            Tribulation tribulation = entry.getValue();

            // 检查是否可以触发下一波
            if (tribulation.canTriggerNextWave()) {
                Player player = plugin.getServer().getPlayer(tribulation.getPlayerId());

                if (player == null || !player.isOnline()) {
                    // 玩家离线,保存天劫状态
                    plugin.getDataManager().saveTribulation(tribulation);
                    continue;
                }

                // 检查玩家是否在范围内
                if (!tribulation.isPlayerInRange(player.getLocation())) {
                    // 玩家离开范围,失败
                    tribulation.fail();
                    player.sendMessage("§c你离开了天劫范围,渡劫失败!");
                    handleTribulationEnd(tribulation);
                    iterator.remove();
                    continue;
                }

                // 触发下一波劫雷
                triggerLightningWave(player, tribulation);

                // 保存天劫进度
                plugin.getDataManager().saveTribulation(tribulation);
            }

            // 检查是否完成或失败
            if (tribulation.isCompleted() || tribulation.isFailed()) {
                handleTribulationEnd(tribulation);
                iterator.remove();
            }
        }
    }

    /**
     * 开始天劫
     */
    public boolean startTribulation(Player player, TribulationType type) {
        UUID playerId = player.getUniqueId();

        // 检查是否已在渡劫
        if (activeTribulations.containsKey(playerId)) {
            player.sendMessage("§c你已经在渡劫中!");
            return false;
        }

        // 检查冷却
        if (isOnCooldown(playerId)) {
            long remaining = getRemainingCooldown(playerId);
            player.sendMessage("§c天劫冷却中! 剩余: §f" + remaining + " §c秒");
            return false;
        }

        // 创建天劫
        Tribulation tribulation = new Tribulation(player, type);
        tribulation.start();

        // 添加到激活列表
        activeTribulations.put(playerId, tribulation);

        // 保存到数据库
        plugin.getDataManager().saveTribulation(tribulation);

        // 通知玩家
        player.sendMessage("§6§l==================");
        player.sendMessage("§c§l天劫降临!");
        player.sendMessage("§e劫数: §f" + type.getDisplayName());
        player.sendMessage("§e波数: §f" + type.getWaves());
        player.sendMessage("§e范围: §f" + tribulation.getRange() + "格");
        player.sendMessage("§c请不要离开天劫范围!");
        player.sendMessage("§6§l==================");

        // 播放音效和效果
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        player.getWorld().strikeLightningEffect(tribulation.getLocation());

        return true;
    }

    /**
     * 触发劫雷波
     */
    private void triggerLightningWave(Player player, Tribulation tribulation) {
        if (!tribulation.nextWave()) {
            return;
        }

        Location loc = player.getLocation();
        double damage = tribulation.getCurrentWaveDamage();

        // 记录玩家受伤前的血量
        double healthBefore = player.getHealth();

        // 在玩家周围随机位置降下劫雷
        Random random = new Random();
        int lightningCount = 1 + (tribulation.getCurrentWave() / 3); // 越往后劫雷越多
        lightningCount = Math.min(lightningCount, 5); // 最多5个劫雷

        for (int i = 0; i < lightningCount; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 10;
            double offsetZ = (random.nextDouble() - 0.5) * 10;

            Location strikeLoc = loc.clone().add(offsetX, 0, offsetZ);
            strikeLoc.setY(strikeLoc.getWorld().getHighestBlockYAt(strikeLoc));

            // 降下劫雷
            LightningStrike lightning = (LightningStrike) strikeLoc.getWorld().spawnEntity(
                    strikeLoc, EntityType.LIGHTNING);

            // 对玩家造成伤害
            if (player.getLocation().distance(strikeLoc) < 5.0) {
                player.damage(damage);
                tribulation.addDamage(damage);

                // 添加粒子效果
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
            }
        }

        // 记录玩家受伤后的血量
        double healthAfter = player.getHealth();
        tribulation.recordHealthLoss(healthBefore, healthAfter);

        // 播放音效
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

        // 显示进度
        int current = tribulation.getCurrentWave();
        int total = tribulation.getTotalWaves();
        player.sendMessage("§e第 §c" + current + "§e/§c" + total + " §e波劫雷! §7(伤害: §c" +
                String.format("%.1f", damage) + "§7)");

        // 每5波显示详细进度
        if (current % 5 == 0 && current < total) {
            displayProgress(player, tribulation);
        }

        // 检查玩家是否死亡
        if (player.isDead() || player.getHealth() <= 0) {
            tribulation.fail();
            player.sendMessage("§c§l你在天劫中身死道消!");
        }

        // 检查是否完成
        if (tribulation.isLastWave() && !tribulation.isFailed()) {
            tribulation.complete();
        }
    }

    /**
     * 处理天劫结束
     */
    private void handleTribulationEnd(Tribulation tribulation) {
        Player player = plugin.getServer().getPlayer(tribulation.getPlayerId());

        if (player == null || !player.isOnline()) {
            return;
        }

        if (tribulation.isCompleted()) {
            // 成功渡劫
            handleTribulationSuccess(player, tribulation);
        } else if (tribulation.isFailed()) {
            // 渡劫失败
            handleTribulationFailure(player, tribulation);
        }

        // 设置冷却
        setCooldown(tribulation.getPlayerId(), 300); // 5分钟冷却

        // 从数据库删除天劫数据
        plugin.getDataManager().deleteTribulation(tribulation.getTribulationId());
    }

    /**
     * 处理渡劫成功
     */
    private void handleTribulationSuccess(Player player, Tribulation tribulation) {
        // 计算评级
        String rating = tribulation.calculateRating();
        String ratingColor = tribulation.getRatingColor();
        double rewardMultiplier = tribulation.getRewardMultiplier();

        player.sendMessage("§a§l=======================");
        player.sendMessage("§a§l渡劫成功!");
        player.sendMessage("§e评级: " + ratingColor + rating + " §7(" + tribulation.getRatingDescription() + ")");
        player.sendMessage("§e历时: §f" + tribulation.getDuration() + " 秒");
        player.sendMessage("§e劫雷: §f" + tribulation.getLightningStrikes() + " 次");
        player.sendMessage("§e死亡: §f" + tribulation.getDeaths() + " 次");
        player.sendMessage("§e最低血量: §c" + String.format("%.1f", tribulation.getMinHealth()) + " §7/ 20.0");
        player.sendMessage("§a§l=======================");

        // 播放成功音效和粒子
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation(), 100, 1, 1, 1, 0.1);

        // 额外粒子效果（基于评级）
        if (rating.equals("S")) {
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 200, 2, 2, 2, 0.2);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }

        // 给予奖励
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data != null) {
            // 基础奖励
            long baseExpReward = (long) (10000 * tribulation.getType().getDifficultyMultiplier());
            int baseStonesReward = (int) (100 * tribulation.getType().getTier());
            int baseSkillPoints = tribulation.getType().getTier();
            int baseActiveQi = 25 + (tribulation.getType().getTier() * 5);

            // 应用评级加成
            long expReward = (long) (baseExpReward * rewardMultiplier);
            int stonesReward = baseStonesReward;
            int skillPointReward = baseSkillPoints;
            int activeQiGain = baseActiveQi;

            // 额外评级奖励
            int bonusStones = 0;
            int bonusSkillPoints = 0;
            int bonusActiveQi = 0;

            switch (rating) {
                case "S":
                    bonusStones = 500;
                    bonusSkillPoints = 3;
                    bonusActiveQi = 50;
                    player.sendMessage("§d§l★ 完美通关奖励!");
                    break;
                case "A":
                    bonusStones = 300;
                    bonusSkillPoints = 2;
                    bonusActiveQi = 30;
                    player.sendMessage("§6§l★ 优秀通关奖励!");
                    break;
                case "B":
                    bonusStones = 150;
                    bonusSkillPoints = 1;
                    bonusActiveQi = 15;
                    break;
                case "C":
                    bonusStones = 50;
                    bonusActiveQi = 5;
                    break;
            }

            stonesReward += bonusStones;
            skillPointReward += bonusSkillPoints;
            activeQiGain += bonusActiveQi;

            // 增加修为
            data.addQi(expReward);

            // 增加灵石
            data.addSpiritStones(stonesReward);

            // 增加功法点
            data.addSkillPoints(skillPointReward);

            // 增加活跃灵气
            data.addActiveQi(activeQiGain);

            // 保存数据
            plugin.getDataManager().savePlayerData(data);

            player.sendMessage("");
            player.sendMessage("§e奖励:");
            player.sendMessage("§7- 修为: §b+" + expReward + " §7(基础×" + String.format("%.1f", rewardMultiplier) + ")");
            player.sendMessage("§7- 灵石: §6+" + stonesReward + (bonusStones > 0 ? " §7(含评级奖励+" + bonusStones + ")" : ""));
            player.sendMessage("§7- 功法点: §d+" + skillPointReward + (bonusSkillPoints > 0 ? " §7(含评级奖励+" + bonusSkillPoints + ")" : ""));
            player.sendMessage("§7- 活跃灵气: §a+" + activeQiGain + (bonusActiveQi > 0 ? " §7(含评级奖励+" + bonusActiveQi + ")" : ""));

            // 全服广播（S/A评级）
            if (rating.equals("S") || rating.equals("A")) {
                String broadcast = "§6§l[天劫] §f" + player.getName() + " §e以 " +
                    ratingColor + rating + " §e评级成功渡过了 §c" +
                    tribulation.getType().getDisplayName() + "§e!";
                plugin.getServer().broadcastMessage(broadcast);
            }
        }
    }

    /**
     * 处理渡劫失败
     */
    private void handleTribulationFailure(Player player, Tribulation tribulation) {
        player.sendMessage("§c§l==================");
        player.sendMessage("§c§l渡劫失败!");
        player.sendMessage("§e历时: §f" + tribulation.getDuration() + " 秒");
        player.sendMessage("§e已承受: §f" + tribulation.getCurrentWave() + "/" +
                tribulation.getTotalWaves() + " 波");
        player.sendMessage("§c§l==================");

        // 惩罚
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data != null) {
            // 损失修为
            long qiPenalty = data.getQi() / 10; // 损失10%修为
            data.setQi(Math.max(0, data.getQi() - qiPenalty));

            // 即使失败也增加少量活跃灵气（鼓励尝试）
            data.addActiveQi(8);

            // 保存数据
            plugin.getDataManager().savePlayerData(data);

            player.sendMessage("§c惩罚:");
            player.sendMessage("§7- 修为: §c-" + qiPenalty);
            player.sendMessage("§7但你获得了 §a+8 §7活跃灵气（挑战勇气）");
        }
    }

    /**
     * 显示渡劫进度
     */
    private void displayProgress(Player player, Tribulation tribulation) {
        int current = tribulation.getCurrentWave();
        int total = tribulation.getTotalWaves();
        double progress = tribulation.getProgress();

        player.sendMessage("");
        player.sendMessage("§b§l========== 渡劫进度 ==========");
        player.sendMessage("§e已完成: §a" + current + "§e/§c" + total + " §e波");
        player.sendMessage("§e进度: §6" + String.format("%.1f%%", progress));
        player.sendMessage("§e受到伤害: §c" + String.format("%.1f", tribulation.getTotalDamage()));
        player.sendMessage("§e死亡次数: §c" + tribulation.getDeaths());
        player.sendMessage("§e当前血量: " + getHealthBar(player) + " §c" +
            String.format("%.1f", player.getHealth()) + "§7/20.0");
        player.sendMessage("§b§l============================");
        player.sendMessage("");

        // 播放提示音效
        player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 0.5f, 1.5f);

        // 显示粒子效果
        player.getWorld().spawnParticle(Particle.END_ROD,
            player.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * 获取血量条
     */
    private String getHealthBar(Player player) {
        double health = player.getHealth();
        double maxHealth = 20.0;
        double percent = health / maxHealth;

        int barLength = 20;
        int filled = (int) (barLength * percent);

        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                if (percent > 0.6) {
                    bar.append("§a█");
                } else if (percent > 0.3) {
                    bar.append("§e█");
                } else {
                    bar.append("§c█");
                }
            } else {
                bar.append("§8█");
            }
        }
        bar.append("§7]");

        return bar.toString();
    }

    /**
     * 获取玩家的天劫
     */
    public Tribulation getTribulation(UUID playerId) {
        return activeTribulations.get(playerId);
    }

    /**
     * 取消天劫
     */
    public boolean cancelTribulation(UUID playerId) {
        Tribulation tribulation = activeTribulations.remove(playerId);
        if (tribulation != null) {
            tribulation.cancel();
            return true;
        }
        return false;
    }

    /**
     * 检查是否在冷却中
     */
    private boolean isOnCooldown(UUID playerId) {
        Long endTime = tribulationCooldowns.get(playerId);
        if (endTime == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now >= endTime) {
            tribulationCooldowns.remove(playerId);
            return false;
        }

        return true;
    }

    /**
     * 获取剩余冷却时间(秒)
     */
    private long getRemainingCooldown(UUID playerId) {
        Long endTime = tribulationCooldowns.get(playerId);
        if (endTime == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long remaining = endTime - now;
        return Math.max(0, remaining / 1000);
    }

    /**
     * 设置冷却
     */
    private void setCooldown(UUID playerId, int seconds) {
        long endTime = System.currentTimeMillis() + (seconds * 1000L);
        tribulationCooldowns.put(playerId, endTime);
    }

    /**
     * 关闭系统
     */
    public void shutdown() {
        if (tribulationTask != null) {
            tribulationTask.cancel();
        }

        // 取消所有激活的天劫
        activeTribulations.values().forEach(Tribulation::cancel);
        activeTribulations.clear();
    }
}
