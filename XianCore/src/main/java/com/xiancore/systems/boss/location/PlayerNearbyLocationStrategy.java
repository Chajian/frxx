package com.xiancore.systems.boss.location;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 玩家附近位置策略
 * 在随机玩家附近的指定距离范围内生成
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class PlayerNearbyLocationStrategy extends AbstractLocationStrategy {

    public static final String NAME = "player-nearby";

    public PlayerNearbyLocationStrategy(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<Location> generateCandidates(BossSpawnPoint point, int maxCandidates) {
        List<Location> candidates = new ArrayList<>();

        // 1. 获取所有在线玩家
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) {
            logger.warning("[玩家附近策略] 没有在线玩家，无法生成候选位置");
            return candidates;
        }

        // 2. 随机选择一个玩家
        Player randomPlayer = onlinePlayers.get(ThreadLocalRandom.current().nextInt(onlinePlayers.size()));
        Location playerLoc = randomPlayer.getLocation();

        logger.info("[玩家附近策略] 基于玩家 " + randomPlayer.getName() + " 寻找生成位置...");

        // 3. 获取距离配置
        int minDist = point.getMinDistance();
        int maxDist = point.getMaxDistance();

        // 4. 生成候选位置
        for (int attempt = 0; attempt < maxCandidates; attempt++) {
            // 随机距离和角度
            double distance = minDist + (Math.random() * (maxDist - minDist));
            double angle = Math.random() * 2 * Math.PI;

            int offsetX = (int) (distance * Math.cos(angle));
            int offsetZ = (int) (distance * Math.sin(angle));

            Location candidateLoc = playerLoc.clone().add(offsetX, 0, offsetZ);

            // 加载区块
            candidateLoc.getChunk().load();

            // 寻找安全地面
            if (point.isAutoFindGround()) {
                Location safeLoc = findSafeGroundLocation(candidateLoc);
                if (safeLoc != null) {
                    candidates.add(safeLoc);
                }
            } else {
                candidates.add(candidateLoc);
            }
        }

        logger.info("[玩家附近策略] 生成了 " + candidates.size() + " 个候选位置");
        return candidates;
    }
}
